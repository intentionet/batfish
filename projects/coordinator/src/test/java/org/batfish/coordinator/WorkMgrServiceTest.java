package org.batfish.coordinator;

import static org.batfish.identifiers.NodeRolesId.DEFAULT_NETWORK_NODE_ROLES_ID;
import static org.batfish.identifiers.QuestionSettingsId.DEFAULT_QUESTION_SETTINGS_ID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.batfish.common.AnswerRowsOptions;
import org.batfish.common.BatfishLogger;
import org.batfish.common.ColumnFilter;
import org.batfish.common.ColumnSortOption;
import org.batfish.common.Container;
import org.batfish.common.CoordConsts;
import org.batfish.common.WorkItem;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.coordinator.authorizer.Authorizer;
import org.batfish.coordinator.config.Settings;
import org.batfish.coordinator.id.IdManager;
import org.batfish.coordinator.id.StorageBasedIdManager;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.AnswerMetadataUtil;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.GetAnalysisAnswerMetricsAnswer;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.answers.StringAnswerElement;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.InstanceData;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.TestQuestion;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.datamodel.table.TableView;
import org.batfish.identifiers.AnalysisId;
import org.batfish.identifiers.AnswerId;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.QuestionId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.storage.FileBasedStorage;
import org.batfish.version.BatfishVersion;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link WorkMgrService}. */
public class WorkMgrServiceTest {

  private static TableMetadata MOCK_TABLE_METADATA =
      new TableMetadata(ImmutableList.of(new ColumnMetadata("col", Schema.STRING, "desc")));

  @Rule public TemporaryFolder _networksFolder = new TemporaryFolder();
  @Rule public TemporaryFolder _questionsTemplatesFolder = new TemporaryFolder();

  private WorkMgrService _service;

  private String _networkName;
  private String _snapshotName;
  private SnapshotId _snapshotId;
  private NetworkId _networkId;

  public WorkMgrServiceTest() {
    _networkName = "myNetwork";
    _networkId = new NetworkId(_networkName + "_id");
    _snapshotName = "mySnapshot";
    _snapshotId = new SnapshotId(_snapshotName + "_id");
  }

  private void initNetwork() {
    Settings settings = new Settings(new String[] {});
    BatfishLogger logger = new BatfishLogger("debug", false);
    Main.mainInit(
        new String[] {
          "-containerslocation",
          _networksFolder.getRoot().toString(),
          "-templatedirs",
          _questionsTemplatesFolder.getRoot().toString()
        });
    Main.setLogger(logger);
    Main.initAuthorizer();
    FileBasedStorage fbs = new FileBasedStorage(Main.getSettings().getContainersLocation(), logger);
    WorkMgr manager = new WorkMgr(settings, logger, new StorageBasedIdManager(fbs), fbs);
    Main.setWorkMgr(manager);
    manager.initNetwork(_networkName, null);
    manager.getIdManager().assignNetwork(_networkName, _networkId);
    _service = new WorkMgrService();
  }

  @Test
  public void getEmptyNetwork() {
    initNetwork();
    try (Response response = _service.getNetwork("100", "0.0.0", _networkName)) {
      String networkJson = response.getEntity().toString();
      assertThat(networkJson, equalTo("{\"name\":\"myNetwork\"}"));
    }
  }

  @Test
  public void getNonExistNetwork() {
    String networkName = "non-existing-folder";
    initNetwork();
    try (Response response = _service.getNetwork("100", "0.0.0", networkName)) {
      String actualMessage = response.getEntity().toString();
      String expected = "Network '" + networkName + "' not found";
      assertThat(actualMessage, equalTo(expected));
    }
  }

  @Test
  public void getNonEmptyNetwork() throws Exception {
    initNetwork();
    initSnapshot();
    try (Response response = _service.getNetwork("100", "0.0.0", _networkName)) {
      Container network =
          BatfishObjectMapper.mapper().readValue(response.getEntity().toString(), Container.class);
      Container expected =
          Container.of(_networkName, Sets.newTreeSet(Collections.singleton(_snapshotName)));
      assertThat(network, equalTo(expected));
    }
  }

  @Test
  public void testConfigureAnalysis() throws Exception {
    initNetwork();
    // test init and add questions to analysis
    String questionName = "q1";
    String analysisName = "analysis";
    String analysisJsonString =
        String.format("{\"%s\":{\"question\":\"questionContent\"}}", questionName);
    File analysisFile = _networksFolder.newFile("analysis");
    FileUtils.writeStringToFile(analysisFile, analysisJsonString, StandardCharsets.UTF_8);
    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);
    AnalysisId analysisId = idm().getAnalysisId(analysisName, _networkId);
    assertTrue(idm().hasQuestionId(questionName, _networkId, analysisId));
    // test delete questions
    String questionsToDelete = String.format("[%s]", questionName);
    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "",
        analysisName,
        null,
        questionsToDelete,
        null);
    assertFalse(idm().hasQuestionId(questionName, _networkId, analysisId));
    JSONArray result =
        _service.configureAnalysis(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            "",
            "analysis",
            null,
            questionsToDelete,
            null);
    assertThat(result.getString(0), equalTo(CoordConsts.SVC_KEY_FAILURE));
  }

  @Test
  public void testGetAnalysisAnswer() throws Exception {
    initNetwork();
    initSnapshot();
    String analysisName = "analysis1";
    String questionName = "question1";
    Question questionObj = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(questionObj);
    String question2Name = "question2Name";

    String analysisJsonString = String.format("{\"%s\":%s}", questionName, questionContent);
    File analysisFile = _networksFolder.newFile(analysisName);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString, StandardCharsets.UTF_8);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);
    AnalysisId analysisId = idm().getAnalysisId(analysisName, _networkId);
    QuestionId questionId = idm().getQuestionId(questionName, _networkId, analysisId);
    AnswerId answerId =
        idm()
            .getBaseAnswerId(
                _networkId,
                _snapshotId,
                questionId,
                DEFAULT_QUESTION_SETTINGS_ID,
                DEFAULT_NETWORK_NODE_ROLES_ID,
                null,
                analysisId);
    Answer testAnswer = new Answer();
    testAnswer.setStatus(AnswerStatus.SUCCESS);
    TableAnswerElement table = new TableAnswerElement(MOCK_TABLE_METADATA);
    table.postProcessAnswer(questionObj, table.getRows().getData());
    testAnswer.addAnswerElement(table);
    testAnswer.setQuestion(questionObj);
    String testAnswerStr = BatfishObjectMapper.writeString(testAnswer);
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(testAnswer, Main.getLogger());
    Main.getWorkMgr().getStorage().storeAnswer(testAnswerStr, answerId);
    Main.getWorkMgr().getStorage().storeAnswerMetadata(answerMetadata, answerId);

    WorkItem workItem = new WorkItem(_networkName, _snapshotName);
    String workItemString = BatfishObjectMapper.mapper().writeValueAsString(workItem);

    JSONArray answer1Output =
        _service.getAnalysisAnswer(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            analysisName,
            questionName,
            workItemString);

    JSONArray answer2Output =
        _service.getAnalysisAnswer(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            analysisName,
            question2Name,
            null);

    assertThat(answer1Output.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));
    assertThat(answer2Output.get(0), equalTo(CoordConsts.SVC_KEY_FAILURE));

    JSONObject answerJsonObject = (JSONObject) answer1Output.get(1);
    String answerJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWER);
    String answerString =
        BatfishObjectMapper.mapper().readValue(answerJsonString, new TypeReference<String>() {});
    assertThat(
        BatfishObjectMapper.mapper().readTree(answerString),
        equalTo(BatfishObjectMapper.mapper().readTree(testAnswerStr)));
  }

  @Test
  @Deprecated
  public void getQuestionTemplates() throws Exception {
    initNetwork();
    Question testQuestion = createTestQuestion("testquestion", "test description");
    ObjectMapper mapper = BatfishObjectMapper.mapper();
    // serializing the question in the temp questions folder
    String questionJsonString = mapper.writeValueAsString(testQuestion);
    Files.write(
        _questionsTemplatesFolder.newFile("testQuestion.json").toPath(),
        questionJsonString.getBytes(StandardCharsets.UTF_8));
    JSONArray questionsResponse = _service.getQuestionTemplates(CoordConsts.DEFAULT_API_KEY);

    // testting if the response is valid and contains testquestion
    if (questionsResponse.get(0).equals(CoordConsts.SVC_KEY_SUCCESS)) {
      JSONObject questionsJsonObject = (JSONObject) questionsResponse.get(1);
      String questionsJsonString = questionsJsonObject.getString(CoordConsts.SVC_KEY_QUESTION_LIST);
      Map<String, String> questionsMap =
          mapper.readValue(questionsJsonString, new TypeReference<Map<String, String>>() {});
      if (questionsMap.containsKey("testquestion")) {
        assertThat(questionsMap.get("testquestion"), is(equalTo(questionJsonString)));
      } else {
        fail("Question not found in the response");
      }
    } else {
      fail("Service call was not successful");
    }
  }

  private Question createTestQuestion(String name, String description) {
    InstanceData instanceData = new InstanceData();
    instanceData.setDescription(description);
    instanceData.setInstanceName(name);
    Question testQuestion =
        new Question() {
          @Override
          public String getName() {
            return "test";
          }

          @Override
          public boolean getDataPlane() {
            return false;
          }

          @Override
          public boolean equals(Object obj) {
            if (obj == null) {
              return false;
            }
            if (getClass() != obj.getClass()) {
              return false;
            }
            final Question other = (Question) obj;
            return Objects.equals(
                    getInstance().getInstanceName(), other.getInstance().getInstanceName())
                && Objects.equals(
                    getInstance().getDescription(), other.getInstance().getDescription());
          }

          @Override
          public int hashCode() {
            return Objects.hash(getInstance().getInstanceName(), getInstance().getDescription());
          }
        };

    testQuestion.setInstance(instanceData);
    return testQuestion;
  }

  // TODO Remove these tests after container and testrig keys are removed

  @Test
  public void getEmptyContainer() {
    initNetwork();
    try (Response response = _service.getNetwork("100", "0.0.0", _networkName)) {
      String networkJson = response.getEntity().toString();
      assertThat(networkJson, equalTo("{\"name\":\"myNetwork\"}"));
    }
  }

  @Test
  public void getNonExistContainer() {
    String networkName = "non-existing-folder";
    initNetwork();
    try (Response response = _service.getNetwork("100", "0.0.0", networkName)) {
      String actualMessage = response.getEntity().toString();
      String expected = "Network '" + networkName + "' not found";
      assertThat(actualMessage, equalTo(expected));
    }
  }

  private void initSnapshot() {
    Main.getWorkMgr().getIdManager().assignSnapshot(_snapshotName, _networkId, _snapshotId);
  }

  @Test
  public void getNonEmptyContainer() throws Exception {
    initNetwork();
    initSnapshot();
    try (Response response = _service.getNetwork("100", "0.0.0", _networkName)) {
      String entityStr = response.getEntity().toString();
      Container network = BatfishObjectMapper.mapper().readValue(entityStr, Container.class);
      Container expected =
          Container.of(_networkName, Sets.newTreeSet(Collections.singleton(_snapshotName)));
      assertThat(network, equalTo(expected));
    }
  }

  @Test
  public void testConfigureAnalysisInContainer() throws Exception {
    initNetwork();
    // test init and add questions to analysis
    String questionName = "q1";
    String analysisName = "a1";
    String analysisJsonString =
        String.format("{\"%s\":{\"question\":\"questionContent\"}}", questionName);
    File analysisFile = _networksFolder.newFile("analysis");
    FileUtils.writeStringToFile(analysisFile, analysisJsonString, StandardCharsets.UTF_8);
    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);
    AnalysisId analysisId = idm().getAnalysisId(analysisName, _networkId);
    assertTrue(idm().hasQuestionId(questionName, _networkId, analysisId));
    // test delete questions
    String questionsToDelete = String.format("[%s]", questionName);
    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "",
        analysisName,
        null,
        questionsToDelete,
        null);
    assertFalse(idm().hasQuestionId(questionName, _networkId, analysisId));
    JSONArray result =
        _service.configureAnalysis(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            "",
            "analysis",
            null,
            questionsToDelete,
            null);
    assertThat(result.getString(0), equalTo(CoordConsts.SVC_KEY_FAILURE));
  }

  @Test
  public void testGetAnalysisAnswerInContainer() throws Exception {
    initNetwork();
    initSnapshot();
    String analysisName = "analysis1";
    String questionName = "question1";
    Question questionObj = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(questionObj);
    String question2Name = "question2Name";

    String analysisJsonString = String.format("{\"%s\":%s}", questionName, questionContent);
    File analysisFile = _networksFolder.newFile(analysisName);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString, StandardCharsets.UTF_8);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);
    AnalysisId analysisId = idm().getAnalysisId(analysisName, _networkId);
    QuestionId questionId = idm().getQuestionId(questionName, _networkId, analysisId);
    AnswerId answerId =
        idm()
            .getBaseAnswerId(
                _networkId,
                _snapshotId,
                questionId,
                DEFAULT_QUESTION_SETTINGS_ID,
                DEFAULT_NETWORK_NODE_ROLES_ID,
                null,
                analysisId);
    Answer testAnswer = new Answer();
    testAnswer.setStatus(AnswerStatus.SUCCESS);
    testAnswer.addAnswerElement(new StringAnswerElement("foo"));
    String testAnswerStr = BatfishObjectMapper.writeString(testAnswer);
    Main.getWorkMgr().getStorage().storeAnswer(testAnswerStr, answerId);
    Main.getWorkMgr()
        .getStorage()
        .storeAnswerMetadata(
            AnswerMetadataUtil.computeAnswerMetadata(testAnswer, Main.getLogger()), answerId);

    WorkItem workItem = new WorkItem(_networkName, _snapshotName);
    String workItemString = BatfishObjectMapper.mapper().writeValueAsString(workItem);

    JSONArray answer1Output =
        _service.getAnalysisAnswer(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            analysisName,
            questionName,
            workItemString);

    JSONArray answer2Output =
        _service.getAnalysisAnswer(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            analysisName,
            question2Name,
            null);

    assertThat(answer1Output.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));
    assertThat(answer2Output.get(0), equalTo(CoordConsts.SVC_KEY_FAILURE));

    JSONObject answerJsonObject = (JSONObject) answer1Output.get(1);
    String answerJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWER);
    String answerString =
        BatfishObjectMapper.mapper().readValue(answerJsonString, new TypeReference<String>() {});
    assertThat(answerString, equalTo(testAnswerStr));
  }

  @Test
  public void testGetAnalysisAnswersMetrics() throws Exception {
    initNetwork();
    initSnapshot();
    String analysisName = "analysis1";
    String questionName = "question1";
    Question questionObj = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(questionObj);
    String analysisQuestionsStr =
        BatfishObjectMapper.writePrettyString(ImmutableList.of(questionName));

    String analysisJsonString = String.format("{\"%s\":%s}", questionName, questionContent);
    File analysisFile = _networksFolder.newFile(analysisName);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString, StandardCharsets.UTF_8);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);
    AnalysisId analysisId = idm().getAnalysisId(analysisName, _networkId);
    QuestionId questionId = idm().getQuestionId(questionName, _networkId, analysisId);
    AnswerId answerId =
        idm()
            .getBaseAnswerId(
                _networkId,
                _snapshotId,
                questionId,
                DEFAULT_QUESTION_SETTINGS_ID,
                DEFAULT_NETWORK_NODE_ROLES_ID,
                null,
                analysisId);
    Answer testAnswer = new Answer();
    testAnswer.setStatus(AnswerStatus.SUCCESS);
    testAnswer.addAnswerElement(new TableAnswerElement(MOCK_TABLE_METADATA));
    String testAnswerStr = BatfishObjectMapper.writeString(testAnswer);
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(testAnswer, Main.getLogger());
    Main.getWorkMgr().getStorage().storeAnswer(testAnswerStr, answerId);
    Main.getWorkMgr().getStorage().storeAnswerMetadata(answerMetadata, answerId);

    JSONArray answerOutput =
        _service.getAnalysisAnswersMetrics(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            analysisName,
            analysisQuestionsStr,
            null);

    assertThat(answerOutput.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));

    JSONObject answerJsonObject = (JSONObject) answerOutput.get(1);
    String answerJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWER);
    GetAnalysisAnswerMetricsAnswer structuredAnswer =
        BatfishObjectMapper.mapper()
            .readValue(answerJsonString, new TypeReference<GetAnalysisAnswerMetricsAnswer>() {});
    assertThat(
        structuredAnswer,
        equalTo(new GetAnalysisAnswerMetricsAnswer(ImmutableMap.of(questionName, answerMetadata))));
  }

  @Test
  public void testGetAnalysisAnswersRows() throws Exception {
    initNetwork();
    initSnapshot();
    String analysisName = "analysis1";
    String questionName = "question1";
    Question questionObj = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(questionObj);
    String columnName = "col";
    Map<String, AnswerRowsOptions> analysisAnswersOptions =
        ImmutableMap.of(
            questionName,
            new AnswerRowsOptions(
                ImmutableSet.of(columnName),
                ImmutableList.of(new ColumnFilter(columnName, "")),
                Integer.MAX_VALUE,
                0,
                ImmutableList.of(new ColumnSortOption(columnName, false)),
                false));
    String analysisAnswersOptionsStr =
        BatfishObjectMapper.writePrettyString(analysisAnswersOptions);

    int value = 5;

    String analysisJsonString = String.format("{\"%s\":%s}", questionName, questionContent);
    File analysisFile = _networksFolder.newFile(analysisName);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString, StandardCharsets.UTF_8);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);
    AnalysisId analysisId = idm().getAnalysisId(analysisName, _networkId);
    QuestionId questionId = idm().getQuestionId(questionName, _networkId, analysisId);
    AnswerId answerId =
        idm()
            .getBaseAnswerId(
                _networkId,
                _snapshotId,
                questionId,
                DEFAULT_QUESTION_SETTINGS_ID,
                DEFAULT_NETWORK_NODE_ROLES_ID,
                null,
                analysisId);
    Answer testAnswer = new Answer();
    testAnswer.setStatus(AnswerStatus.SUCCESS);
    testAnswer.addAnswerElement(
        new TableAnswerElement(
                new TableMetadata(
                    ImmutableList.of(new ColumnMetadata(columnName, Schema.INTEGER, "foobar")),
                    new DisplayHints().getTextDesc()))
            .addRow(Row.of(columnName, value)));
    String testAnswerStr = BatfishObjectMapper.writeString(testAnswer);
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(testAnswer, Main.getLogger());
    Main.getWorkMgr().getStorage().storeAnswer(testAnswerStr, answerId);
    Main.getWorkMgr().getStorage().storeAnswerMetadata(answerMetadata, answerId);

    JSONArray answerOutput =
        _service.getAnalysisAnswersRows(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            analysisName,
            analysisAnswersOptionsStr,
            null);

    assertThat(answerOutput.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));

    JSONObject answerJsonObject = (JSONObject) answerOutput.get(1);
    String answersJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWERS);
    Map<String, Answer> structuredAnswers =
        BatfishObjectMapper.mapper()
            .readValue(answersJsonString, new TypeReference<Map<String, Answer>>() {});

    assertThat(structuredAnswers.keySet(), equalTo(ImmutableSet.of(questionName)));

    Answer processedAnswer = structuredAnswers.get(questionName);
    TableAnswerElement processedTable =
        (TableAnswerElement) processedAnswer.getAnswerElements().get(0);

    assertThat(processedTable.getRowsList(), equalTo(ImmutableList.of(Row.of(columnName, value))));
  }

  @Test
  public void testGetAnswerMetricsAnalysis() throws Exception {
    initNetwork();
    initSnapshot();
    String analysisName = "analysis1";
    String questionName = "question1";
    Question questionObj = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(questionObj);

    String analysisJsonString = String.format("{\"%s\":%s}", questionName, questionContent);
    File analysisFile = _networksFolder.newFile(analysisName);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString, StandardCharsets.UTF_8);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);
    AnalysisId analysisId = idm().getAnalysisId(analysisName, _networkId);
    QuestionId questionId = idm().getQuestionId(questionName, _networkId, analysisId);
    AnswerId answerId =
        idm()
            .getBaseAnswerId(
                _networkId,
                _snapshotId,
                questionId,
                DEFAULT_QUESTION_SETTINGS_ID,
                DEFAULT_NETWORK_NODE_ROLES_ID,
                null,
                analysisId);
    Answer testAnswer = new Answer();
    testAnswer.setStatus(AnswerStatus.SUCCESS);
    testAnswer.addAnswerElement(new TableAnswerElement(MOCK_TABLE_METADATA));
    String testAnswerStr = BatfishObjectMapper.writeString(testAnswer);
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(testAnswer, Main.getLogger());
    Main.getWorkMgr().getStorage().storeAnswer(testAnswerStr, answerId);
    Main.getWorkMgr().getStorage().storeAnswerMetadata(answerMetadata, answerId);

    JSONArray answerOutput =
        _service.getAnswerMetrics(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            analysisName,
            questionName,
            null);

    assertThat(answerOutput.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));

    JSONObject answerJsonObject = (JSONObject) answerOutput.get(1);
    String answerJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWER);
    AnswerMetadata structuredAnswer =
        BatfishObjectMapper.mapper()
            .readValue(answerJsonString, new TypeReference<AnswerMetadata>() {});
    assertThat(structuredAnswer, equalTo(answerMetadata));
  }

  @Test
  public void testGetAnswerMetricsAdHoc() throws Exception {
    initNetwork();
    initSnapshot();
    String question = "question1";
    Question questionObj = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(questionObj);

    Main.getWorkMgr().uploadQuestion(_networkName, question, questionContent);
    QuestionId questionId = idm().getQuestionId(question, _networkId, null);
    AnswerId answerId =
        idm()
            .getBaseAnswerId(
                _networkId,
                _snapshotId,
                questionId,
                DEFAULT_QUESTION_SETTINGS_ID,
                DEFAULT_NETWORK_NODE_ROLES_ID,
                null,
                null);
    Answer testAnswer = new Answer();
    testAnswer.setStatus(AnswerStatus.SUCCESS);
    testAnswer.addAnswerElement(new TableAnswerElement(MOCK_TABLE_METADATA));
    String testAnswerStr = BatfishObjectMapper.writeString(testAnswer);
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(testAnswer, Main.getLogger());
    Main.getWorkMgr().getStorage().storeAnswer(testAnswerStr, answerId);
    Main.getWorkMgr().getStorage().storeAnswerMetadata(answerMetadata, answerId);

    JSONArray answerOutput =
        _service.getAnswerMetrics(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            null,
            question,
            null);

    assertThat(answerOutput.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));

    JSONObject answerJsonObject = (JSONObject) answerOutput.get(1);
    String answerJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWER);
    AnswerMetadata structuredAnswer =
        BatfishObjectMapper.mapper()
            .readValue(answerJsonString, new TypeReference<AnswerMetadata>() {});
    assertThat(structuredAnswer, equalTo(answerMetadata));
  }

  @Test
  public void testGetAnswerRowsAnalysis() throws Exception {
    initNetwork();
    initSnapshot();
    String analysis = "analysis1";
    String question = "question1";
    Question questionObj = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(questionObj);
    String columnName = "col";
    AnswerRowsOptions answersRowsOptions =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(new ColumnFilter(columnName, "")),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(new ColumnSortOption(columnName, false)),
            false);
    String answerRowsOptionsStr = BatfishObjectMapper.writePrettyString(answersRowsOptions);
    int value = 5;
    Answer testAnswer = new Answer();
    testAnswer.addAnswerElement(
        new TableAnswerElement(
                new TableMetadata(
                    ImmutableList.of(new ColumnMetadata(columnName, Schema.INTEGER, "foobar")),
                    new DisplayHints().getTextDesc()))
            .addRow(Row.of(columnName, value)));
    testAnswer.setStatus(AnswerStatus.SUCCESS);
    String answer = BatfishObjectMapper.writePrettyString(testAnswer);

    String analysisJsonString = String.format("{\"%s\":%s}", question, questionContent);
    File analysisFile = _networksFolder.newFile(analysis);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString, StandardCharsets.UTF_8);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "new",
        analysis,
        new FileInputStream(analysisFile),
        "",
        null);
    AnalysisId analysisId = idm().getAnalysisId(analysis, _networkId);
    QuestionId questionId = idm().getQuestionId(question, _networkId, analysisId);
    AnswerId answerId =
        idm()
            .getBaseAnswerId(
                _networkId,
                _snapshotId,
                questionId,
                DEFAULT_QUESTION_SETTINGS_ID,
                DEFAULT_NETWORK_NODE_ROLES_ID,
                null,
                analysisId);

    Main.getWorkMgr().getStorage().storeAnswer(answer, answerId);
    Main.getWorkMgr()
        .getStorage()
        .storeAnswerMetadata(
            AnswerMetadataUtil.computeAnswerMetadata(testAnswer, Main.getLogger()), answerId);

    JSONArray answerOutput =
        _service.getAnswerRows(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            question,
            analysis,
            answerRowsOptionsStr,
            null);

    assertThat(answerOutput.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));

    JSONObject answerJsonObject = (JSONObject) answerOutput.get(1);
    String answersJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWER);
    Answer processedAnswer =
        BatfishObjectMapper.mapper().readValue(answersJsonString, new TypeReference<Answer>() {});

    TableAnswerElement processedTable =
        (TableAnswerElement) processedAnswer.getAnswerElements().get(0);

    assertThat(processedTable.getRowsList(), equalTo(ImmutableList.of(Row.of(columnName, value))));
  }

  private static IdManager idm() {
    return Main.getWorkMgr().getIdManager();
  }

  @Test
  public void testGetAnswerRowsAdHoc() throws Exception {
    initNetwork();
    initSnapshot();
    String question = "question1";
    QuestionId questionId = new QuestionId(question + "_id");
    Question questionObj = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(questionObj);
    String columnName = "col";
    AnswerRowsOptions answersRowsOptions =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(new ColumnFilter(columnName, "")),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(new ColumnSortOption(columnName, false)),
            false);
    String answerRowsOptionsStr = BatfishObjectMapper.writePrettyString(answersRowsOptions);

    int value = 5;
    Answer testAnswer = new Answer();
    testAnswer.addAnswerElement(
        new TableAnswerElement(
                new TableMetadata(
                    ImmutableList.of(new ColumnMetadata(columnName, Schema.INTEGER, "foobar")),
                    new DisplayHints().getTextDesc()))
            .addRow(Row.of(columnName, value)));
    testAnswer.setStatus(AnswerStatus.SUCCESS);
    String answer = BatfishObjectMapper.writePrettyString(testAnswer);

    Main.getWorkMgr().getStorage().storeQuestion(questionContent, _networkId, questionId, null);
    idm().assignQuestion(question, _networkId, questionId, null);
    AnswerId answerId =
        idm()
            .getBaseAnswerId(
                _networkId,
                _snapshotId,
                questionId,
                DEFAULT_QUESTION_SETTINGS_ID,
                DEFAULT_NETWORK_NODE_ROLES_ID,
                null,
                null);
    Main.getWorkMgr().getStorage().storeAnswer(answer, answerId);
    Main.getWorkMgr()
        .getStorage()
        .storeAnswerMetadata(
            AnswerMetadataUtil.computeAnswerMetadata(testAnswer, Main.getLogger()), answerId);

    JSONArray answerOutput =
        _service.getAnswerRows(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            question,
            null,
            answerRowsOptionsStr,
            null);

    assertThat(answerOutput.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));

    JSONObject answerJsonObject = (JSONObject) answerOutput.get(1);
    String answersJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWER);
    Answer processedAnswer =
        BatfishObjectMapper.mapper().readValue(answersJsonString, new TypeReference<Answer>() {});

    TableAnswerElement processedTable =
        (TableAnswerElement) processedAnswer.getAnswerElements().get(0);

    assertThat(processedTable.getRowsList(), equalTo(ImmutableList.of(Row.of(columnName, value))));
  }

  @Test
  public void testGetAnswerRows2Analysis() throws Exception {
    initNetwork();
    initSnapshot();
    String analysis = "analysis1";
    String question = "question1";
    Question questionObj = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(questionObj);
    String columnName = "col";
    AnswerRowsOptions answersRowsOptions =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(new ColumnFilter(columnName, "")),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(new ColumnSortOption(columnName, false)),
            false);
    String answerRowsOptionsStr = BatfishObjectMapper.writePrettyString(answersRowsOptions);
    int value = 5;
    Answer testAnswer = new Answer();
    testAnswer.addAnswerElement(
        new TableAnswerElement(
                new TableMetadata(
                    ImmutableList.of(new ColumnMetadata(columnName, Schema.INTEGER, "foobar")),
                    new DisplayHints().getTextDesc()))
            .addRow(Row.of(columnName, value)));
    testAnswer.setStatus(AnswerStatus.SUCCESS);
    String answer = BatfishObjectMapper.writePrettyString(testAnswer);

    String analysisJsonString = String.format("{\"%s\":%s}", question, questionContent);
    File analysisFile = _networksFolder.newFile(analysis);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString, StandardCharsets.UTF_8);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        _networkName,
        "new",
        analysis,
        new FileInputStream(analysisFile),
        "",
        null);
    AnalysisId analysisId = idm().getAnalysisId(analysis, _networkId);
    QuestionId questionId = idm().getQuestionId(question, _networkId, analysisId);
    AnswerId answerId =
        idm()
            .getBaseAnswerId(
                _networkId,
                _snapshotId,
                questionId,
                DEFAULT_QUESTION_SETTINGS_ID,
                DEFAULT_NETWORK_NODE_ROLES_ID,
                null,
                analysisId);

    Main.getWorkMgr().getStorage().storeAnswer(answer, answerId);
    Main.getWorkMgr()
        .getStorage()
        .storeAnswerMetadata(
            AnswerMetadataUtil.computeAnswerMetadata(testAnswer, Main.getLogger()), answerId);

    JSONArray answerOutput =
        _service.getAnswerRows2(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            question,
            analysis,
            answerRowsOptionsStr,
            null);

    assertThat(answerOutput.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));

    JSONObject answerJsonObject = (JSONObject) answerOutput.get(1);
    String answersJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWER);
    Answer processedAnswer =
        BatfishObjectMapper.mapper().readValue(answersJsonString, new TypeReference<Answer>() {});

    TableView processedTable = (TableView) processedAnswer.getAnswerElements().get(0);

    assertThat(processedTable.getInnerRows(), equalTo(ImmutableList.of(Row.of(columnName, value))));
  }

  @Test
  public void testGetAnswerRows2AdHoc() throws Exception {
    initNetwork();
    initSnapshot();
    String question = "question1";
    QuestionId questionId = new QuestionId(question + "_id");
    Question questionObj = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(questionObj);
    String columnName = "col";
    AnswerRowsOptions answersRowsOptions =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(new ColumnFilter(columnName, "")),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(new ColumnSortOption(columnName, false)),
            false);
    String answerRowsOptionsStr = BatfishObjectMapper.writePrettyString(answersRowsOptions);

    int value = 5;
    Answer testAnswer = new Answer();
    testAnswer.addAnswerElement(
        new TableAnswerElement(
                new TableMetadata(
                    ImmutableList.of(new ColumnMetadata(columnName, Schema.INTEGER, "foobar")),
                    new DisplayHints().getTextDesc()))
            .addRow(Row.of(columnName, value)));
    testAnswer.setStatus(AnswerStatus.SUCCESS);
    String answer = BatfishObjectMapper.writePrettyString(testAnswer);

    Main.getWorkMgr().getStorage().storeQuestion(questionContent, _networkId, questionId, null);
    idm().assignQuestion(question, _networkId, questionId, null);
    AnswerId answerId =
        idm()
            .getBaseAnswerId(
                _networkId,
                _snapshotId,
                questionId,
                DEFAULT_QUESTION_SETTINGS_ID,
                DEFAULT_NETWORK_NODE_ROLES_ID,
                null,
                null);
    Main.getWorkMgr().getStorage().storeAnswer(answer, answerId);
    Main.getWorkMgr()
        .getStorage()
        .storeAnswerMetadata(
            AnswerMetadataUtil.computeAnswerMetadata(testAnswer, Main.getLogger()), answerId);

    JSONArray answerOutput =
        _service.getAnswerRows2(
            CoordConsts.DEFAULT_API_KEY,
            BatfishVersion.getVersionStatic(),
            _networkName,
            _snapshotName,
            null,
            question,
            null,
            answerRowsOptionsStr,
            null);

    assertThat(answerOutput.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));

    JSONObject answerJsonObject = (JSONObject) answerOutput.get(1);
    String answersJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWER);
    Answer processedAnswer =
        BatfishObjectMapper.mapper().readValue(answersJsonString, new TypeReference<Answer>() {});

    TableView processedTable = (TableView) processedAnswer.getAnswerElements().get(0);

    assertThat(processedTable.getInnerRows(), equalTo(ImmutableList.of(Row.of(columnName, value))));
  }

  @Test
  @Deprecated
  public void testGetWorkStatus() throws InterruptedException, ExecutionException, IOException {
    initNetwork();
    WorkMgrTestUtils.uploadTestSnapshot(_networkName, _snapshotName, _networksFolder);
    final CompletableFuture<String> networkArg = new CompletableFuture<>();

    WorkItem workItem = new WorkItem(_networkName, _snapshotName);
    Main.getWorkMgr().queueWork(workItem);

    Main.setAuthorizer(
        new Authorizer() {

          @Override
          public void authorizeContainer(String apiKey, String containerName) {
            assert Boolean.TRUE;
          }

          @Override
          public boolean isAccessibleNetwork(
              String apiKey, String containerName, boolean logError) {
            networkArg.complete(containerName);
            return true;
          }

          @Override
          public boolean isValidWorkApiKey(String apiKey) {
            return true;
          }
        });
    _service.getWorkStatus(
        CoordConsts.DEFAULT_API_KEY,
        BatfishVersion.getVersionStatic(),
        workItem.getId().toString());

    // networkArg should be name, not ID
    assertTrue(networkArg.isDone());
    assertThat(networkArg.get(), equalTo(_networkName));
  }
}
