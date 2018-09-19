package org.batfish.coordinator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.batfish.common.AnswerRowsOptions;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.ColumnFilter;
import org.batfish.common.ColumnSortOption;
import org.batfish.common.Container;
import org.batfish.common.CoordConsts;
import org.batfish.common.Version;
import org.batfish.common.WorkItem;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.coordinator.config.Settings;
import org.batfish.datamodel.answers.Aggregation;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.GetAnalysisAnswerMetricsAnswer;
import org.batfish.datamodel.answers.Metrics;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.Question.InstanceData;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.storage.FileBasedStorage;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link WorkMgrService}. */
public class WorkMgrServiceTest {

  @Rule public TemporaryFolder _networksFolder = new TemporaryFolder();
  @Rule public TemporaryFolder _questionsTemplatesFolder = new TemporaryFolder();

  private WorkMgrService _service;

  private String _networkName = "myNetwork";
  private String _snapshotName = "mySnapshot";

  private void initNetworkEnvironment() throws Exception {
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
    WorkMgr manager =
        new WorkMgr(
            settings,
            logger,
            new FileBasedStorage(Main.getSettings().getContainersLocation(), logger));
    Main.setWorkMgr(manager);
    manager.initContainer(_networkName, null);
    _service = new WorkMgrService();
  }

  @Test
  public void getEmptyNetwork() throws Exception {
    initNetworkEnvironment();
    Response response = _service.getNetwork("100", "0.0.0", null, _networkName);
    String networkJson = response.getEntity().toString();
    assertThat(networkJson, equalTo("{\"name\":\"myNetwork\"}"));
  }

  @Test
  public void getNonExistNetwork() throws Exception {
    String networkName = "non-existing-folder";
    initNetworkEnvironment();
    Response response = _service.getNetwork("100", "0.0.0", null, networkName);
    String actualMessage = response.getEntity().toString();
    String expected = "Network '" + networkName + "' not found";
    assertThat(actualMessage, equalTo(expected));
  }

  @Test
  public void getNetworkWithBadVersion() throws Exception {
    initNetworkEnvironment();
    Response response = _service.getNetwork("100", "invalid version", null, _networkName);
    String actualMessage = response.getEntity().toString();
    String expected = "Illegal version 'invalid version' for Client";
    assertThat(actualMessage, equalTo(expected));
  }

  @Test
  public void getNonEmptyNetwork() throws Exception {
    initNetworkEnvironment();
    Path networkPath = _networksFolder.getRoot().toPath().resolve(_networkName);
    Path snapshotPath = networkPath.resolve(BfConsts.RELPATH_TESTRIGS_DIR).resolve(_snapshotName);
    assertThat(snapshotPath.toFile().mkdirs(), is(true));
    Response response = _service.getNetwork("100", "0.0.0", null, _networkName);
    Container network =
        BatfishObjectMapper.mapper().readValue(response.getEntity().toString(), Container.class);
    Container expected =
        Container.of(_networkName, Sets.newTreeSet(Collections.singleton(_snapshotName)));
    assertThat(network, equalTo(expected));
  }

  @Test
  public void testConfigureAnalysis() throws Exception {
    initNetworkEnvironment();
    // test init and add questions to analysis
    String analysisJsonString = "{\"question\":{\"question\":\"questionContent\"}}";
    File analysisFile = _networksFolder.newFile("analysis");
    FileUtils.writeStringToFile(analysisFile, analysisJsonString);
    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        Version.getVersion(),
        null,
        _networkName,
        "new",
        "analysis",
        new FileInputStream(analysisFile),
        "",
        null);
    Path questionPath =
        _networksFolder
            .getRoot()
            .toPath()
            .resolve(
                Paths.get(
                    _networkName,
                    BfConsts.RELPATH_ANALYSES_DIR,
                    "analysis",
                    BfConsts.RELPATH_QUESTIONS_DIR,
                    "question"));
    assertTrue(Files.exists(questionPath));
    // test delete questions
    String questionsToDelete = "[question]";
    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        Version.getVersion(),
        null,
        _networkName,
        "",
        "analysis",
        null,
        questionsToDelete,
        null);
    assertFalse(Files.exists(questionPath));
    JSONArray result =
        _service.configureAnalysis(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            null,
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
    String analysisName = "analysis1";
    String questionName = "question1";
    String questionContent = "questionContent";
    String question2Name = "question2Name";
    String answer = "answerContent";

    initNetworkEnvironment();

    String analysisJsonString =
        String.format("{\"%s\":{\"question\":\"%s\"}}", questionName, questionContent);
    File analysisFile = _networksFolder.newFile(analysisName);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        Version.getVersion(),
        null,
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);

    Path answerDir =
        _networksFolder
            .getRoot()
            .toPath()
            .resolve(
                Paths.get(
                    _networkName,
                    BfConsts.RELPATH_TESTRIGS_DIR,
                    _snapshotName,
                    BfConsts.RELPATH_ANALYSES_DIR,
                    analysisName,
                    BfConsts.RELPATH_QUESTIONS_DIR,
                    questionName,
                    BfConsts.RELPATH_ENVIRONMENTS_DIR,
                    BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME));

    Path answer1Path = answerDir.resolve(BfConsts.RELPATH_ANSWER_JSON);
    answerDir.toFile().mkdirs();
    CommonUtil.writeFile(answer1Path, answer);

    WorkItem workItem = new WorkItem(_networkName, _snapshotName);
    String workItemString = BatfishObjectMapper.mapper().writeValueAsString(workItem);

    JSONArray answer1Output =
        _service.getAnalysisAnswer(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            null,
            _networkName,
            null,
            _snapshotName,
            BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME,
            null,
            null,
            null,
            null,
            analysisName,
            questionName,
            workItemString);

    JSONArray answer2Output =
        _service.getAnalysisAnswer(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            null,
            _networkName,
            null,
            _snapshotName,
            BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME,
            null,
            null,
            null,
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
    assertThat(answerString, equalTo(answer));
  }

  @Test
  public void getConfigNonExistNetwork() throws Exception {
    initNetworkEnvironment();
    Response response =
        _service.getConfiguration(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            null,
            "nonExistNetwork",
            null,
            _snapshotName,
            "config1.cfg");
    String actualMessage = response.getEntity().toString();
    assertThat(actualMessage, equalTo("Network 'nonExistNetwork' not found"));
  }

  @Test
  public void getNonExistConfig() throws Exception {
    initNetworkEnvironment();
    Path networkDir = _networksFolder.getRoot().toPath().resolve(_networkName);
    Path snapshotPath =
        networkDir.resolve(
            Paths.get(BfConsts.RELPATH_TESTRIGS_DIR, _snapshotName, BfConsts.RELPATH_TEST_RIG_DIR));
    assertTrue(snapshotPath.toFile().mkdirs());
    Response response =
        _service.getConfiguration(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            null,
            _networkName,
            null,
            _snapshotName,
            "config.cfg");
    String actualMessage = response.getEntity().toString();
    String expected =
        String.format(
            "Configuration file config.cfg does not exist in snapshot %s for network %s",
            _snapshotName, _networkName);
    assertThat(actualMessage, equalTo(expected));
  }

  @Test
  public void getConfigContent() throws Exception {
    initNetworkEnvironment();
    Path networkPath = _networksFolder.getRoot().toPath().resolve(_networkName);
    Path configPath =
        networkPath.resolve(
            Paths.get(
                BfConsts.RELPATH_TESTRIGS_DIR,
                _snapshotName,
                BfConsts.RELPATH_TEST_RIG_DIR,
                BfConsts.RELPATH_CONFIGURATIONS_DIR));
    assertTrue(configPath.toFile().mkdirs());
    CommonUtil.writeFile(configPath.resolve("config.cfg"), "config content");
    Response response =
        _service.getConfiguration(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            null,
            _networkName,
            null,
            _snapshotName,
            "config.cfg");
    String actualMessage = response.getEntity().toString();
    assertThat(actualMessage, equalTo("config content"));
  }

  @Test
  public void getQuestionTemplates() throws Exception {
    initNetworkEnvironment();
    Question testQuestion = createTestQuestion("testquestion", "test description");
    ObjectMapper mapper = BatfishObjectMapper.mapper();
    // serializing the question in the temp questions folder
    String questionJsonString = mapper.writeValueAsString(testQuestion);
    CommonUtil.writeFile(
        _questionsTemplatesFolder.newFile("testQuestion.json").toPath(), questionJsonString);
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
  public void getEmptyContainer() throws Exception {
    initNetworkEnvironment();
    Response response = _service.getNetwork("100", "0.0.0", _networkName, null);
    String networkJson = response.getEntity().toString();
    assertThat(networkJson, equalTo("{\"name\":\"myNetwork\"}"));
  }

  @Test
  public void getNonExistContainer() throws Exception {
    String networkName = "non-existing-folder";
    initNetworkEnvironment();
    Response response = _service.getNetwork("100", "0.0.0", networkName, null);
    String actualMessage = response.getEntity().toString();
    String expected = "Network '" + networkName + "' not found";
    assertThat(actualMessage, equalTo(expected));
  }

  @Test
  public void getContainerWithBadVersion() throws Exception {
    initNetworkEnvironment();
    Response response = _service.getNetwork("100", "invalid version", _networkName, null);
    String actualMessage = response.getEntity().toString();
    String expected = "Illegal version 'invalid version' for Client";
    assertThat(actualMessage, equalTo(expected));
  }

  @Test
  public void getNonEmptyContainer() throws Exception {
    initNetworkEnvironment();
    Path networkPath = _networksFolder.getRoot().toPath().resolve(_networkName);
    Path snapshotPath = networkPath.resolve(BfConsts.RELPATH_TESTRIGS_DIR).resolve(_snapshotName);
    assertThat(snapshotPath.toFile().mkdirs(), is(true));
    Response response = _service.getNetwork("100", "0.0.0", _networkName, null);
    Container network =
        BatfishObjectMapper.mapper().readValue(response.getEntity().toString(), Container.class);
    Container expected =
        Container.of(_networkName, Sets.newTreeSet(Collections.singleton(_snapshotName)));
    assertThat(network, equalTo(expected));
  }

  @Test
  public void testConfigureAnalysisInContainer() throws Exception {
    initNetworkEnvironment();
    // test init and add questions to analysis
    String analysisJsonString = "{\"question\":{\"question\":\"questionContent\"}}";
    File analysisFile = _networksFolder.newFile("analysis");
    FileUtils.writeStringToFile(analysisFile, analysisJsonString);
    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        Version.getVersion(),
        _networkName,
        null,
        "new",
        "analysis",
        new FileInputStream(analysisFile),
        "",
        null);
    Path questionPath =
        _networksFolder
            .getRoot()
            .toPath()
            .resolve(
                Paths.get(
                    _networkName,
                    BfConsts.RELPATH_ANALYSES_DIR,
                    "analysis",
                    BfConsts.RELPATH_QUESTIONS_DIR,
                    "question"));
    assertTrue(Files.exists(questionPath));
    // test delete questions
    String questionsToDelete = "[question]";
    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        Version.getVersion(),
        _networkName,
        null,
        "",
        "analysis",
        null,
        questionsToDelete,
        null);
    assertFalse(Files.exists(questionPath));
    JSONArray result =
        _service.configureAnalysis(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            _networkName,
            null,
            "",
            "analysis",
            null,
            questionsToDelete,
            null);
    assertThat(result.getString(0), equalTo(CoordConsts.SVC_KEY_FAILURE));
  }

  @Test
  public void testGetAnalysisAnswerInContainer() throws Exception {
    String analysisName = "analysis1";
    String questionName = "question1";
    String questionContent = "questionContent";
    String question2Name = "question2Name";
    String answer = "answerContent";

    initNetworkEnvironment();

    String analysisJsonString =
        String.format("{\"%s\":{\"question\":\"%s\"}}", questionName, questionContent);
    File analysisFile = _networksFolder.newFile(analysisName);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        Version.getVersion(),
        _networkName,
        null,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);

    Path answerDir =
        _networksFolder
            .getRoot()
            .toPath()
            .resolve(
                Paths.get(
                    _networkName,
                    BfConsts.RELPATH_TESTRIGS_DIR,
                    _snapshotName,
                    BfConsts.RELPATH_ANALYSES_DIR,
                    analysisName,
                    BfConsts.RELPATH_QUESTIONS_DIR,
                    questionName,
                    BfConsts.RELPATH_ENVIRONMENTS_DIR,
                    BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME));

    Path answer1Path = answerDir.resolve(BfConsts.RELPATH_ANSWER_JSON);
    answerDir.toFile().mkdirs();
    CommonUtil.writeFile(answer1Path, answer);

    WorkItem workItem = new WorkItem(_networkName, _snapshotName);
    String workItemString = BatfishObjectMapper.mapper().writeValueAsString(workItem);

    JSONArray answer1Output =
        _service.getAnalysisAnswer(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            _networkName,
            null,
            _snapshotName,
            null,
            BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME,
            null,
            null,
            null,
            null,
            analysisName,
            questionName,
            workItemString);

    JSONArray answer2Output =
        _service.getAnalysisAnswer(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            _networkName,
            null,
            _snapshotName,
            null,
            BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME,
            null,
            null,
            null,
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
    assertThat(answerString, equalTo(answer));
  }

  @Test
  public void getConfigNonExistContainer() throws Exception {
    initNetworkEnvironment();
    Response response =
        _service.getConfiguration(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            "nonExistContainer",
            null,
            _snapshotName,
            null,
            "config1.cfg");
    String actualMessage = response.getEntity().toString();
    assertThat(actualMessage, equalTo("Network 'nonExistContainer' not found"));
  }

  @Test
  public void getNonExistConfigInContainer() throws Exception {
    initNetworkEnvironment();
    Path networkDir = _networksFolder.getRoot().toPath().resolve(_networkName);
    Path snapshotPath =
        networkDir.resolve(
            Paths.get(BfConsts.RELPATH_TESTRIGS_DIR, _snapshotName, BfConsts.RELPATH_TEST_RIG_DIR));
    assertTrue(snapshotPath.toFile().mkdirs());
    Response response =
        _service.getConfiguration(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            _networkName,
            null,
            _snapshotName,
            null,
            "config.cfg");
    String actualMessage = response.getEntity().toString();
    String expected =
        String.format(
            "Configuration file config.cfg does not exist in snapshot %s for network %s",
            _snapshotName, _networkName);
    assertThat(actualMessage, equalTo(expected));
  }

  @Test
  public void getConfigContentInContainer() throws Exception {
    initNetworkEnvironment();
    Path networkPath = _networksFolder.getRoot().toPath().resolve(_networkName);
    Path configPath =
        networkPath.resolve(
            Paths.get(
                BfConsts.RELPATH_TESTRIGS_DIR,
                _snapshotName,
                BfConsts.RELPATH_TEST_RIG_DIR,
                BfConsts.RELPATH_CONFIGURATIONS_DIR));
    assertTrue(configPath.toFile().mkdirs());
    CommonUtil.writeFile(configPath.resolve("config.cfg"), "config content");
    Response response =
        _service.getConfiguration(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            _networkName,
            null,
            _snapshotName,
            null,
            "config.cfg");
    String actualMessage = response.getEntity().toString();
    assertThat(actualMessage, equalTo("config content"));
  }

  @Test
  public void testGetAnalysisAnswersMetrics() throws Exception {
    String analysisName = "analysis1";
    String questionName = "question1";
    String questionContent = "questionContent";
    String analysisQuestionsStr =
        BatfishObjectMapper.writePrettyString(ImmutableList.of(questionName));

    initNetworkEnvironment();

    String columnName = "col";
    int value = 5;
    AnswerMetadata testAnswerMetadata =
        AnswerMetadata.builder()
            .setMetrics(
                Metrics.builder()
                    .setAggregations(
                        ImmutableMap.of(columnName, ImmutableMap.of(Aggregation.MAX, value)))
                    .setNumRows(1)
                    .build())
            .setStatus(AnswerStatus.SUCCESS)
            .build();
    String answerMetadata = BatfishObjectMapper.writePrettyString(testAnswerMetadata);

    String analysisJsonString =
        String.format("{\"%s\":{\"question\":\"%s\"}}", questionName, questionContent);
    File analysisFile = _networksFolder.newFile(analysisName);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        Version.getVersion(),
        null,
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);

    Path answerDir =
        _networksFolder
            .getRoot()
            .toPath()
            .resolve(
                Paths.get(
                    _networkName,
                    BfConsts.RELPATH_TESTRIGS_DIR,
                    _snapshotName,
                    BfConsts.RELPATH_ANALYSES_DIR,
                    analysisName,
                    BfConsts.RELPATH_QUESTIONS_DIR,
                    questionName,
                    BfConsts.RELPATH_ENVIRONMENTS_DIR,
                    BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME));

    Path answer1MetadataPath = answerDir.resolve(BfConsts.RELPATH_ANSWER_METADATA);
    answerDir.toFile().mkdirs();
    CommonUtil.writeFile(answer1MetadataPath, answerMetadata);

    JSONArray answerOutput =
        _service.getAnalysisAnswersMetrics(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            _networkName,
            _snapshotName,
            null,
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
        equalTo(
            new GetAnalysisAnswerMetricsAnswer(
                ImmutableMap.of(
                    questionName,
                    AnswerMetadata.builder()
                        .setMetrics(
                            Metrics.builder()
                                .setAggregations(
                                    ImmutableMap.of(
                                        columnName, ImmutableMap.of(Aggregation.MAX, value)))
                                .setNumRows(1)
                                .build())
                        .setStatus(AnswerStatus.SUCCESS)
                        .build()))));
  }

  @Test
  public void testGetAnalysisAnswersRows() throws Exception {
    String analysisName = "analysis1";
    String questionName = "question1";
    String questionContent = "questionContent";
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

    initNetworkEnvironment();

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

    String analysisJsonString =
        String.format("{\"%s\":{\"question\":\"%s\"}}", questionName, questionContent);
    File analysisFile = _networksFolder.newFile(analysisName);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        Version.getVersion(),
        null,
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);

    Path answerDir =
        _networksFolder
            .getRoot()
            .toPath()
            .resolve(
                Paths.get(
                    _networkName,
                    BfConsts.RELPATH_TESTRIGS_DIR,
                    _snapshotName,
                    BfConsts.RELPATH_ANALYSES_DIR,
                    analysisName,
                    BfConsts.RELPATH_QUESTIONS_DIR,
                    questionName,
                    BfConsts.RELPATH_ENVIRONMENTS_DIR,
                    BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME));

    Path answer1Path = answerDir.resolve(BfConsts.RELPATH_ANSWER_JSON);
    answerDir.toFile().mkdirs();
    CommonUtil.writeFile(answer1Path, answer);

    JSONArray answerOutput =
        _service.getAnalysisAnswersRows(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            _networkName,
            _snapshotName,
            null,
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
    String analysisName = "analysis1";
    String questionName = "question1";
    String questionContent = "questionContent";

    initNetworkEnvironment();

    String columnName = "col";
    int value = 5;
    AnswerMetadata testAnswerMetadata =
        AnswerMetadata.builder()
            .setMetrics(
                Metrics.builder()
                    .setAggregations(
                        ImmutableMap.of(columnName, ImmutableMap.of(Aggregation.MAX, value)))
                    .setNumRows(1)
                    .build())
            .setStatus(AnswerStatus.SUCCESS)
            .build();
    String answerMetadata = BatfishObjectMapper.writePrettyString(testAnswerMetadata);

    String analysisJsonString =
        String.format("{\"%s\":{\"question\":\"%s\"}}", questionName, questionContent);
    File analysisFile = _networksFolder.newFile(analysisName);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        Version.getVersion(),
        null,
        _networkName,
        "new",
        analysisName,
        new FileInputStream(analysisFile),
        "",
        null);

    Path answerDir =
        _networksFolder
            .getRoot()
            .toPath()
            .resolve(
                Paths.get(
                    _networkName,
                    BfConsts.RELPATH_TESTRIGS_DIR,
                    _snapshotName,
                    BfConsts.RELPATH_ANALYSES_DIR,
                    analysisName,
                    BfConsts.RELPATH_QUESTIONS_DIR,
                    questionName,
                    BfConsts.RELPATH_ENVIRONMENTS_DIR,
                    BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME));

    Path answer1MetadataPath = answerDir.resolve(BfConsts.RELPATH_ANSWER_METADATA);
    answerDir.toFile().mkdirs();
    CommonUtil.writeFile(answer1MetadataPath, answerMetadata);

    JSONArray answerOutput =
        _service.getAnswerMetrics(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            _networkName,
            _snapshotName,
            null,
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
    assertThat(
        structuredAnswer,
        equalTo(
            AnswerMetadata.builder()
                .setMetrics(
                    Metrics.builder()
                        .setAggregations(
                            ImmutableMap.of(columnName, ImmutableMap.of(Aggregation.MAX, value)))
                        .setNumRows(1)
                        .build())
                .setStatus(AnswerStatus.SUCCESS)
                .build()));
  }

  @Test
  public void testGetAnswerMetricsAdHoc() throws Exception {
    String questionName = "question1";
    String questionContent = "questionContent";
    String columnName = "col";
    initNetworkEnvironment();

    int value = 5;
    AnswerMetadata testAnswerMetadata =
        AnswerMetadata.builder()
            .setMetrics(
                Metrics.builder()
                    .setAggregations(
                        ImmutableMap.of(columnName, ImmutableMap.of(Aggregation.MAX, value)))
                    .setNumRows(1)
                    .build())
            .setStatus(AnswerStatus.SUCCESS)
            .build();

    Main.getWorkMgr().getStorage().storeQuestion(questionContent, _networkName, questionName, null);
    Main.getWorkMgr()
        .getStorage()
        .storeAnswerMetadata(
            testAnswerMetadata, _networkName, _snapshotName, questionName, null, null);

    JSONArray answerOutput =
        _service.getAnswerMetrics(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
            _networkName,
            _snapshotName,
            null,
            null,
            null,
            questionName,
            null);

    assertThat(answerOutput.get(0), equalTo(CoordConsts.SVC_KEY_SUCCESS));

    JSONObject answerJsonObject = (JSONObject) answerOutput.get(1);
    String answerJsonString = answerJsonObject.getString(CoordConsts.SVC_KEY_ANSWER);
    AnswerMetadata structuredAnswer =
        BatfishObjectMapper.mapper()
            .readValue(answerJsonString, new TypeReference<AnswerMetadata>() {});
    assertThat(
        structuredAnswer,
        equalTo(
            AnswerMetadata.builder()
                .setMetrics(
                    Metrics.builder()
                        .setAggregations(
                            ImmutableMap.of(columnName, ImmutableMap.of(Aggregation.MAX, value)))
                        .setNumRows(1)
                        .build())
                .setStatus(AnswerStatus.SUCCESS)
                .build()));
  }

  @Test
  public void testGetAnswerRowsAnalysis() throws Exception {
    String analysis = "analysis1";
    String question = "question1";
    String questionContent = "questionContent";
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

    initNetworkEnvironment();

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

    String analysisJsonString =
        String.format("{\"%s\":{\"question\":\"%s\"}}", question, questionContent);
    File analysisFile = _networksFolder.newFile(analysis);
    FileUtils.writeStringToFile(analysisFile, analysisJsonString);

    _service.configureAnalysis(
        CoordConsts.DEFAULT_API_KEY,
        Version.getVersion(),
        null,
        _networkName,
        "new",
        analysis,
        new FileInputStream(analysisFile),
        "",
        null);

    Main.getWorkMgr()
        .getStorage()
        .storeAnswer(answer, _networkName, _snapshotName, question, null, analysis);

    JSONArray answerOutput =
        _service.getAnswerRows(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
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

  @Test
  public void testGetAnswerRowsAdHoc() throws Exception {
    String question = "question1";
    String questionContent = "questionContent";
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

    initNetworkEnvironment();

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

    Main.getWorkMgr().getStorage().storeQuestion(questionContent, _networkName, question, null);
    Main.getWorkMgr()
        .getStorage()
        .storeAnswer(answer, _networkName, _snapshotName, question, null, null);

    JSONArray answerOutput =
        _service.getAnswerRows(
            CoordConsts.DEFAULT_API_KEY,
            Version.getVersion(),
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
}
