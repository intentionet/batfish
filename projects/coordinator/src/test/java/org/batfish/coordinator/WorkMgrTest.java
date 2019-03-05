package org.batfish.coordinator;

import static org.batfish.common.util.CommonUtil.writeFile;
import static org.batfish.coordinator.WorkMgr.addToSerializedList;
import static org.batfish.coordinator.WorkMgr.generateFileDateString;
import static org.batfish.coordinator.WorkMgr.removeFromSerializedList;
import static org.batfish.coordinator.WorkMgrTestUtils.createSnapshot;
import static org.batfish.identifiers.NodeRolesId.DEFAULT_NETWORK_NODE_ROLES_ID;
import static org.batfish.identifiers.QuestionSettingsId.DEFAULT_QUESTION_SETTINGS_ID;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.batfish.common.AnswerRowsOptions;
import org.batfish.common.BatfishException;
import org.batfish.common.BfConsts;
import org.batfish.common.ColumnFilter;
import org.batfish.common.ColumnSortOption;
import org.batfish.common.Container;
import org.batfish.common.WorkItem;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.WorkItemBuilder;
import org.batfish.coordinator.AnalysisMetadataMgr.AnalysisType;
import org.batfish.coordinator.WorkDetails.WorkType;
import org.batfish.coordinator.id.IdManager;
import org.batfish.coordinator.resources.ForkSnapshotBean;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SnapshotMetadata;
import org.batfish.datamodel.SnapshotMetadataEntry;
import org.batfish.datamodel.acl.AclTrace;
import org.batfish.datamodel.acl.DefaultDeniedByIpAccessList;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.AnswerMetadataUtil;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.Issue;
import org.batfish.datamodel.answers.MajorIssueConfig;
import org.batfish.datamodel.answers.MinorIssueConfig;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.answers.SelfDescribingObject;
import org.batfish.datamodel.answers.StringAnswerElement;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.Hop;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.pojo.Topology;
import org.batfish.datamodel.questions.Exclusion;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.TestQuestion;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.datamodel.table.TableView;
import org.batfish.datamodel.table.TableViewRow;
import org.batfish.identifiers.AnalysisId;
import org.batfish.identifiers.AnswerId;
import org.batfish.identifiers.IssueSettingsId;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.NodeRolesId;
import org.batfish.identifiers.QuestionId;
import org.batfish.identifiers.QuestionSettingsId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.role.NodeRole;
import org.batfish.role.NodeRoleDimension;
import org.batfish.role.NodeRolesData;
import org.batfish.storage.StorageProvider;
import org.batfish.storage.StoredObjectMetadata;
import org.hamcrest.io.FileMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link WorkMgr}. */
public final class WorkMgrTest {

  private static TableMetadata MOCK_TABLE_METADATA =
      new TableMetadata(ImmutableList.of(new ColumnMetadata("col", Schema.STRING, "desc")));

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private WorkMgr _manager;

  private StorageProvider _storage;

  private IdManager _idManager;

  @Before
  public void initManager() throws Exception {
    WorkMgrTestUtils.initWorkManager(_folder);
    _manager = Main.getWorkMgr();
    _idManager = _manager.getIdManager();
    _storage = _manager.getStorage();
  }

  private void createSnapshotWithMetadata(String network, String snapshot) throws IOException {
    NetworkId networkId = _idManager.getNetworkId(network);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(snapshot, networkId, snapshotId);
    SnapshotMetadataMgr.writeMetadata(
        new SnapshotMetadata(new Date().toInstant(), null), networkId, snapshotId);
  }

  @Test
  public void testAddToSerializedList() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();

    NodeInterfacePair baseInterface = new NodeInterfacePair("n1", "iface1");
    NodeInterfacePair additionalInterface = new NodeInterfacePair("n2", "iface2");

    // Write base serialized list
    List<NodeInterfacePair> interfaces = new ArrayList<>();
    interfaces.add(baseInterface);
    writeFile(serializedListPath, BatfishObjectMapper.writePrettyString(interfaces));

    addToSerializedList(
        serializedListPath,
        ImmutableList.of(additionalInterface),
        new TypeReference<List<NodeInterfacePair>>() {});

    // Confirm the additional and original interfaces show up in the merged list
    assertThat(
        BatfishObjectMapper.mapper()
            .readValue(
                CommonUtil.readFile(serializedListPath),
                new TypeReference<List<NodeInterfacePair>>() {}),
        containsInAnyOrder(baseInterface, additionalInterface));
  }

  @Test
  public void testAddToSerializedListNoAddition() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();

    NodeInterfacePair baseInterface = new NodeInterfacePair("n1", "iface1");

    // Write base serialized list
    List<NodeInterfacePair> interfaces = new ArrayList<>();
    interfaces.add(baseInterface);
    writeFile(serializedListPath, BatfishObjectMapper.writePrettyString(interfaces));

    addToSerializedList(
        serializedListPath, ImmutableList.of(), new TypeReference<List<NodeInterfacePair>>() {});

    // Confirm original interface shows up in the merged list, even if there are no additions
    assertThat(
        BatfishObjectMapper.mapper()
            .readValue(
                CommonUtil.readFile(serializedListPath),
                new TypeReference<List<NodeInterfacePair>>() {}),
        containsInAnyOrder(baseInterface));
  }

  @Test
  public void testAddToSerializedListNullAddition() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();

    NodeInterfacePair baseInterface = new NodeInterfacePair("n1", "iface1");

    // Write base serialized list
    List<NodeInterfacePair> interfaces = new ArrayList<>();
    interfaces.add(baseInterface);
    writeFile(serializedListPath, BatfishObjectMapper.writePrettyString(interfaces));

    addToSerializedList(serializedListPath, null, new TypeReference<List<NodeInterfacePair>>() {});

    // Confirm original interface shows up in the merged list, even if addition is null
    assertThat(
        BatfishObjectMapper.mapper()
            .readValue(
                CommonUtil.readFile(serializedListPath),
                new TypeReference<List<NodeInterfacePair>>() {}),
        containsInAnyOrder(baseInterface));
  }

  @Test
  public void testAddToSerializedListNoList() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();
    serializedList.delete();

    NodeInterfacePair additionalInterface = new NodeInterfacePair("n2", "iface2");

    addToSerializedList(
        serializedListPath,
        ImmutableList.of(additionalInterface),
        new TypeReference<List<NodeInterfacePair>>() {});

    // Confirm the additional interface shows up in the serialized list, even if the serialized list
    // didn't exist in the first place
    assertThat(
        BatfishObjectMapper.mapper()
            .readValue(
                CommonUtil.readFile(serializedListPath),
                new TypeReference<List<NodeInterfacePair>>() {}),
        containsInAnyOrder(additionalInterface));
  }

  @Test
  public void testAddToSerializedListNoListNoAddition() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();
    serializedList.delete();

    addToSerializedList(
        serializedListPath, ImmutableList.of(), new TypeReference<List<NodeInterfacePair>>() {});

    // Confirm no file was created (since there was no list to begin with and nothing was added)
    assertThat(serializedList, not(FileMatchers.anExistingFile()));
  }

  @Test
  public void testRemoveFromSerializedList() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();

    NodeInterfacePair baseInterface1 = new NodeInterfacePair("n1", "iface1");
    NodeInterfacePair baseInterface2 = new NodeInterfacePair("n2", "iface2");

    // Write base serialized list
    List<NodeInterfacePair> interfaces = new ArrayList<>();
    interfaces.add(baseInterface1);
    interfaces.add(baseInterface2);
    writeFile(serializedListPath, BatfishObjectMapper.writePrettyString(interfaces));

    removeFromSerializedList(
        serializedListPath,
        ImmutableList.of(baseInterface1),
        new TypeReference<List<NodeInterfacePair>>() {});

    // Confirm only one interface shows up
    assertThat(
        BatfishObjectMapper.mapper()
            .readValue(
                CommonUtil.readFile(serializedListPath),
                new TypeReference<List<NodeInterfacePair>>() {}),
        contains(baseInterface2));
  }

  @Test
  public void testRemoveFromSerializedListBadSubtraction() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();

    NodeInterfacePair baseInterface1 = new NodeInterfacePair("n1", "iface1");
    NodeInterfacePair baseInterface2 = new NodeInterfacePair("n2", "iface2");
    NodeInterfacePair subtraction1 = new NodeInterfacePair("n2", "iface2");
    NodeInterfacePair subtraction2 = new NodeInterfacePair("n3", "iface3");

    // Write base serialized list
    List<NodeInterfacePair> interfaces = new ArrayList<>();
    interfaces.add(baseInterface1);
    interfaces.add(baseInterface2);
    writeFile(serializedListPath, BatfishObjectMapper.writePrettyString(interfaces));

    // Removing non-existent element should throw
    _thrown.expect(IllegalArgumentException.class);
    _thrown.expectMessage(
        String.format(
            "Existing blacklist does not contain element(s) specified for removal: '%s'",
            ImmutableList.of(subtraction2)));
    removeFromSerializedList(
        serializedListPath,
        ImmutableList.of(subtraction1, subtraction2),
        new TypeReference<List<NodeInterfacePair>>() {});
  }

  @Test
  public void testRemoveFromSerializedListBadSubtractionMissingBaseList() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();
    Files.delete(serializedListPath);

    NodeInterfacePair subtraction = new NodeInterfacePair("n2", "iface2");

    // Removing non-existent element should throw
    _thrown.expect(IllegalArgumentException.class);
    _thrown.expectMessage("Cannot remove element(s) from non-existent blacklist.");
    removeFromSerializedList(
        serializedListPath,
        ImmutableList.of(subtraction),
        new TypeReference<List<NodeInterfacePair>>() {});
  }

  @Test
  public void testRemoveFromSerializedListEmptyBaseList() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();

    // Write empty base serialized list
    List<NodeInterfacePair> interfaces = new ArrayList<>();
    writeFile(serializedListPath, BatfishObjectMapper.writePrettyString(interfaces));

    removeFromSerializedList(
        serializedListPath, ImmutableList.of(), new TypeReference<List<NodeInterfacePair>>() {});

    // Confirm no issue if base list didn't exist
    assertThat(
        BatfishObjectMapper.mapper()
            .readValue(
                CommonUtil.readFile(serializedListPath),
                new TypeReference<List<NodeInterfacePair>>() {}),
        iterableWithSize(0));
  }

  @Test
  public void testRemoveFromSerializedListMissingBaseList() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();
    Files.delete(serializedListPath);

    // Remove nothing from a non-existent list
    removeFromSerializedList(
        serializedListPath, ImmutableList.of(), new TypeReference<List<NodeInterfacePair>>() {});

    // Confirm no issue if base list didn't exist
    assertThat(serializedList, not(anExistingFile()));
  }

  @Test
  public void testRemoveFromSerializedListNoSubtraction() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    File serializedList = tmp.newFile();
    Path serializedListPath = serializedList.toPath();

    NodeInterfacePair baseInterface1 = new NodeInterfacePair("n1", "iface1");

    // Write base serialized list
    List<NodeInterfacePair> interfaces = new ArrayList<>();
    interfaces.add(baseInterface1);
    writeFile(serializedListPath, BatfishObjectMapper.writePrettyString(interfaces));

    removeFromSerializedList(
        serializedListPath, ImmutableList.of(), new TypeReference<List<NodeInterfacePair>>() {});

    // Confirm nothing changes with no subtraction
    assertThat(
        BatfishObjectMapper.mapper()
            .readValue(
                CommonUtil.readFile(serializedListPath),
                new TypeReference<List<NodeInterfacePair>>() {}),
        contains(baseInterface1));

    removeFromSerializedList(
        serializedListPath, null, new TypeReference<List<NodeInterfacePair>>() {});

    // Confirm nothing changes with null subtraction
    assertThat(
        BatfishObjectMapper.mapper()
            .readValue(
                CommonUtil.readFile(serializedListPath),
                new TypeReference<List<NodeInterfacePair>>() {}),
        contains(baseInterface1));
  }

  @Test
  public void initNetworkWithContainerName() {
    String initResult = _manager.initNetwork("container", null);
    assertThat(initResult, equalTo("container"));
  }

  @Test
  public void initNetworkWithContainerPrefix() {
    String initResult = _manager.initNetwork(null, "containerPrefix");
    assertThat(initResult, startsWith("containerPrefix"));
  }

  @Test
  public void initNetworkWithNullInput() {
    String initResult = _manager.initNetwork(null, null);
    assertThat(initResult, startsWith("null_"));
  }

  @Test
  public void initExistingContainer() {
    _manager.initNetwork("container", null);
    String expectedMessage = "Network 'container' already exists!";
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(equalTo(expectedMessage));
    _manager.initNetwork("container", null);
  }

  @Test
  public void listEmptyQuestion() {
    _manager.initNetwork("container", null);
    SortedSet<String> questions = _manager.listQuestions("container", false);
    assertThat(questions.isEmpty(), is(true));
  }

  @Test
  public void listQuestionNames() {
    String questionName = "publicquestion";
    // Leading __ means this question is an internal question
    // And should be hidden from listQuestions when verbose is false
    String internalQuestionName = "__internalquestion";
    String network = "container";
    _manager.initNetwork(network, null);
    NetworkId networkId = _idManager.getNetworkId(network);
    // Make sure the questions are assigned
    _idManager.assignQuestion(questionName, networkId, _idManager.generateQuestionId(), null);
    _idManager.assignQuestion(
        internalQuestionName, networkId, _idManager.generateQuestionId(), null);

    SortedSet<String> questionsNotVerbose = _manager.listQuestions(network, false);
    SortedSet<String> questionsVerbose = _manager.listQuestions(network, true);

    // Only the public question should show up when verbose is false
    assertThat(questionsNotVerbose, equalTo(Sets.newHashSet(questionName)));

    // Both questions should show up when verbose is true
    assertThat(questionsVerbose, equalTo(Sets.newHashSet(questionName, internalQuestionName)));
  }

  @Test
  public void listQuestionWithNonExistContainer() {
    assertThat(_manager.listQuestions("container", false), nullValue());
  }

  @Test
  public void listSortedQuestionNames() {
    String network = "container";
    _manager.initNetwork(network, null);
    NetworkId networkId = _idManager.getNetworkId(network);
    _idManager.assignQuestion("nodes", networkId, _idManager.generateQuestionId(), null);
    _idManager.assignQuestion("access", networkId, _idManager.generateQuestionId(), null);
    _idManager.assignQuestion("initinfo", networkId, _idManager.generateQuestionId(), null);
    SortedSet<String> questions = _manager.listQuestions(network, false);

    assertThat(questions, equalTo(ImmutableSet.of("access", "initinfo", "nodes")));
  }

  @Test
  public void getEmptyContainer() {
    _manager.initNetwork("container", null);
    Container container = _manager.getContainer("container");
    assertThat(container, equalTo(Container.of("container", new TreeSet<>())));
  }

  @Test
  public void getLatestTestrig() throws IOException {
    _manager.initNetwork("container", null);

    // empty should be returned if no testrigs exist
    assertThat(_manager.getLatestSnapshot("container"), equalTo(Optional.empty()));

    // create testrig1, which should be returned
    createSnapshotWithMetadata("container", "testrig1");
    assertThat(_manager.getLatestSnapshot("container"), equalTo(Optional.of("testrig1")));

    // create a second testrig, which should be returned
    createSnapshotWithMetadata("container", "testrig2");
    assertThat(_manager.getLatestSnapshot("container"), equalTo(Optional.of("testrig2")));
  }

  @Test
  public void getNodes() throws IOException {
    _manager.initNetwork("container", null);

    // create a testrig and write a topology object for it
    createSnapshotWithMetadata("container", "testrig1");
    Topology topology = new Topology("testrig1");
    topology.setNodes(ImmutableSet.of(new Node("a1"), new Node("b1")));
    CommonUtil.writeFile(
        _manager
            .getdirSnapshot("container", "testrig1")
            .resolve(
                Paths.get(BfConsts.RELPATH_OUTPUT, BfConsts.RELPATH_TESTRIG_POJO_TOPOLOGY_PATH)),
        BatfishObjectMapper.mapper().writeValueAsString(topology));

    // should get the nodes of the topology when we ask for it
    assertThat(_manager.getNodes("container", "testrig1"), equalTo(ImmutableSet.of("a1", "b1")));
  }

  @Test
  public void getNonEmptyContainer() {
    String network = "container";
    String snapshot = "testrig";
    _manager.initNetwork(network, null);
    NetworkId networkId = _idManager.getNetworkId(network);
    _idManager.assignSnapshot(snapshot, networkId, _idManager.generateSnapshotId());
    Container container = _manager.getContainer(network);
    assertThat(
        container,
        equalTo(Container.of(network, Sets.newTreeSet(Collections.singleton(snapshot)))));
  }

  @Test
  public void getNonExistContainer() {
    _thrown.expect(Exception.class);
    _thrown.expectMessage(equalTo("Network 'container' does not exist"));
    _manager.getContainer("container");
  }

  @Test
  public void testListAnalysesSuggested() {
    String containerName = "myContainer";
    _manager.initNetwork(containerName, null);

    // Create analysis1 (user analysis) and analysis2 (suggested analysis)
    _manager.configureAnalysis(
        containerName, true, "analysis1", Maps.newHashMap(), Lists.newArrayList(), false);
    _manager.configureAnalysis(
        containerName, true, "analysis2", Maps.newHashMap(), Lists.newArrayList(), true);

    // checking that we get analyses according to AnalysisType
    assertThat(
        _manager.listAnalyses(containerName, AnalysisType.ALL),
        equalTo(Sets.newHashSet("analysis1", "analysis2")));
    assertThat(
        _manager.listAnalyses(containerName, AnalysisType.USER),
        equalTo(Sets.newHashSet("analysis1")));
    assertThat(
        _manager.listAnalyses(containerName, AnalysisType.SUGGESTED),
        equalTo(Sets.newHashSet("analysis2")));
  }

  @Test
  public void testConfigureAnalysis() {
    String containerName = "myContainer";
    _manager.initNetwork(containerName, null);
    // test init and add questions to analysis
    Map<String, String> questionsToAdd = ImmutableMap.of("question1", "question1Content");
    String analysisName = "analysis";
    _manager.configureAnalysis(
        containerName, true, analysisName, questionsToAdd, Lists.newArrayList(), null);
    questionsToAdd =
        ImmutableMap.of("question2", "question2Content", "question3", "question3Content");
    _manager.configureAnalysis(
        containerName, false, analysisName, questionsToAdd, Lists.newArrayList(), null);
    NetworkId networkId = _idManager.getNetworkId(containerName);
    AnalysisId analysisId = _idManager.getAnalysisId(analysisName, networkId);
    QuestionId q1Id = _idManager.getQuestionId("question1", networkId, analysisId);
    QuestionId q2Id = _idManager.getQuestionId("question2", networkId, analysisId);
    String actual = _storage.loadQuestion(networkId, q1Id, analysisId);
    assertThat(actual, equalTo("question1Content"));
    actual = _storage.loadQuestion(networkId, q2Id, analysisId);
    assertThat(actual, equalTo("question2Content"));

    // test delete questions
    List<String> questionsToDelete = ImmutableList.of();
    _manager.configureAnalysis(
        containerName, false, analysisName, ImmutableMap.of(), questionsToDelete, null);

    assertTrue(
        _idManager.hasQuestionId("question1", networkId, analysisId)
            && _idManager.hasQuestionId("question2", networkId, analysisId)
            && _idManager.hasQuestionId("question3", networkId, analysisId));
    questionsToDelete = ImmutableList.of("question1", "question2");
    _manager.configureAnalysis(
        containerName, false, analysisName, ImmutableMap.of(), questionsToDelete, null);
    assertFalse(_idManager.hasQuestionId("question1", networkId, analysisId));
    assertFalse(_idManager.hasQuestionId("question2", networkId, analysisId));
    assertTrue(_idManager.hasQuestionId("question3", networkId, analysisId));
    _thrown.expect(IllegalArgumentException.class);
    _thrown.expectMessage(equalTo("Question 'question1' does not exist for analysis 'analysis'"));
    questionsToDelete = ImmutableList.of("question1");
    _manager.configureAnalysis(
        containerName, false, analysisName, ImmutableMap.of(), questionsToDelete, null);
  }

  @Test
  public void testConfigureAnalysisSuggested() {
    String containerName = "myContainer";
    _manager.initNetwork(containerName, null);

    // Analysis initialized with suggested = null should not be marked as suggested
    _manager.configureAnalysis(
        containerName, true, "analysis", Maps.newHashMap(), Lists.newArrayList(), null);
    assertFalse(getMetadataSuggested(containerName, "analysis"));

    // Analysis initialized with suggested = true should be marked as suggested
    _manager.configureAnalysis(
        containerName, true, "analysis2", Maps.newHashMap(), Lists.newArrayList(), true);
    assertTrue(getMetadataSuggested(containerName, "analysis2"));

    // Analysis initialized with suggested = false should not be marked as suggested
    _manager.configureAnalysis(
        containerName, true, "analysis3", Maps.newHashMap(), Lists.newArrayList(), false);
    assertFalse(getMetadataSuggested(containerName, "analysis3"));

    // Existing analysis should not change suggested if suggested arg is null
    _manager.configureAnalysis(
        containerName, false, "analysis2", Maps.newHashMap(), Lists.newArrayList(), null);
    assertTrue(getMetadataSuggested(containerName, "analysis2"));
    _manager.configureAnalysis(
        containerName, false, "analysis3", Maps.newHashMap(), Lists.newArrayList(), null);
    assertFalse(getMetadataSuggested(containerName, "analysis3"));

    // Existing analysis should update suggested if arg is not null
    _manager.configureAnalysis(
        containerName, false, "analysis2", Maps.newHashMap(), Lists.newArrayList(), false);
    assertFalse(getMetadataSuggested(containerName, "analysis2"));
    _manager.configureAnalysis(
        containerName, false, "analysis3", Maps.newHashMap(), Lists.newArrayList(), true);
    assertTrue(getMetadataSuggested(containerName, "analysis3"));
  }

  @Test
  public void testForkSnapshot() throws Exception {
    String networkName = "network";
    String snapshotBaseName = "snapshotBase";
    String snapshotNewName = "snapshotNew";

    _manager.initNetwork(networkName, null);
    uploadTestSnapshot(networkName, snapshotBaseName);
    _manager.forkSnapshot(
        networkName,
        new ForkSnapshotBean(
            snapshotBaseName, snapshotNewName, null, null, null, null, null, null, null));

    // Confirm the forked snapshot exists
    assertThat(_manager.getLatestSnapshot(networkName), equalTo(Optional.of(snapshotNewName)));
  }

  @Test
  public void testForkSnapshotBlacklists() throws Exception {
    String networkName = "network";
    String snapshotBaseName = "snapshotBase";
    String snapshotNewName1 = "snapshotNew1";
    String snapshotNewName2 = "snapshotNew2";

    List<NodeInterfacePair> interfaces = ImmutableList.of(new NodeInterfacePair("n1", "iface1"));
    List<Edge> links = ImmutableList.of(Edge.of("n2", "iface2", "n3", "iface3"));
    List<String> nodes = ImmutableList.of("n4", "n5");

    _manager.initNetwork(networkName, null);
    uploadTestSnapshot(networkName, snapshotBaseName);
    _manager.forkSnapshot(
        networkName,
        new ForkSnapshotBean(
            snapshotBaseName, snapshotNewName1, interfaces, links, nodes, null, null, null, null));
    NetworkId networkId = _idManager.getNetworkId(networkName);
    SnapshotId snapshotId1 = _idManager.getSnapshotId(snapshotNewName1, networkId);

    // Confirm the forked snapshot exists
    assertThat(_manager.getLatestSnapshot(networkName), equalTo(Optional.of(snapshotNewName1)));
    // Confirm the blacklists are correct
    assertThat(
        _storage.loadInterfaceBlacklist(networkId, snapshotId1),
        containsInAnyOrder(interfaces.toArray()));
    assertThat(
        _storage.loadEdgeBlacklist(networkId, snapshotId1), containsInAnyOrder(links.toArray()));
    assertThat(
        _storage.loadNodeBlacklist(networkId, snapshotId1), containsInAnyOrder(nodes.toArray()));

    // Remove blacklisted items from a fork of the first fork
    _manager.forkSnapshot(
        networkName,
        new ForkSnapshotBean(
            snapshotNewName1, snapshotNewName2, null, null, null, interfaces, links, nodes, null));
    SnapshotId snapshotId2 = _idManager.getSnapshotId(snapshotNewName2, networkId);

    // Confirm the forked snapshot exists
    assertThat(_manager.getLatestSnapshot(networkName), equalTo(Optional.of(snapshotNewName2)));
    // Confirm the blacklists are empty
    assertThat(_storage.loadInterfaceBlacklist(networkId, snapshotId2), iterableWithSize(0));
    assertThat(_storage.loadEdgeBlacklist(networkId, snapshotId2), iterableWithSize(0));
    assertThat(_storage.loadNodeBlacklist(networkId, snapshotId2), iterableWithSize(0));
  }

  @Test
  public void testForkSnapshotDuplicateName() throws Exception {
    String networkName = "network";
    String snapshotBaseName = "snapshotBase";
    String snapshotNewName = "snapshotNew";

    _manager.initNetwork(networkName, null);
    uploadTestSnapshot(networkName, snapshotBaseName);
    uploadTestSnapshot(networkName, snapshotNewName);

    // Fork should fail due to duplicate/conflicting new snapshot name
    _thrown.expect(IllegalArgumentException.class);
    _thrown.expectMessage(equalTo("Snapshot with name: '" + snapshotNewName + "' already exists"));
    _manager.forkSnapshot(
        networkName,
        new ForkSnapshotBean(
            snapshotBaseName, snapshotNewName, null, null, null, null, null, null, null));
  }

  @Test
  public void testForkSnapshotFileUpload() throws Exception {
    String networkName = "network";
    String snapshotBaseName = "snapshotBase";
    String snapshotNewName = "snapshotNew";
    String fileName = "file.type";
    String fileContents = "new";

    _manager.initNetwork(networkName, null);
    uploadTestSnapshot(networkName, snapshotBaseName);

    // Create zip with a new file to add to the forked snapshot
    byte[] zipFile = createSnapshotZip(snapshotNewName, fileName, fileContents);

    _manager.forkSnapshot(
        networkName,
        new ForkSnapshotBean(
            snapshotBaseName, snapshotNewName, null, null, null, null, null, null, zipFile));

    // Confirm the forked snapshot exists
    assertThat(_manager.getLatestSnapshot(networkName), equalTo(Optional.of(snapshotNewName)));

    // Confirm the new file exists in the forked snapshot, with the right contents
    String readFileContents = readSnapshotConfig(networkName, snapshotNewName, fileName);
    assertThat(readFileContents, equalTo(fileContents));
  }

  @Test
  public void testForkSnapshotFileUploadOverwrite() throws Exception {
    String networkName = "network";
    String snapshotBaseName = "snapshotBase";
    String snapshotNewName = "snapshotNew";
    String fileName = "file.type";
    String fileContents = "contents";
    String fileContentsNew = "new";

    _manager.initNetwork(networkName, null);
    // Create base snapshot with a file: fileName, containing: fileContents
    uploadTestSnapshot(networkName, snapshotBaseName, fileName, fileContents);

    // Create zip with a file to overwrite the original file in the forked snapshot
    byte[] zipFile = createSnapshotZip(snapshotNewName, fileName, fileContentsNew);

    _manager.forkSnapshot(
        networkName,
        new ForkSnapshotBean(
            snapshotBaseName, snapshotNewName, null, null, null, null, null, null, zipFile));

    // Confirm the forked snapshot exists
    assertThat(_manager.getLatestSnapshot(networkName), equalTo(Optional.of(snapshotNewName)));

    // Confirm the file was overwritten with the new contents
    String readFileContents = readSnapshotConfig(networkName, snapshotNewName, fileName);
    assertThat(readFileContents, equalTo(fileContentsNew));
  }

  private byte[] createSnapshotZip(String snapshot, String fileName, String fileContents)
      throws IOException {
    Path zipPath = WorkMgrTestUtils.createSnapshotZip(snapshot, fileName, fileContents, _folder);
    return FileUtils.readFileToByteArray(zipPath.toFile());
  }

  private String readSnapshotConfig(String network, String snapshot, String fileName)
      throws IOException {
    StringWriter writer = new StringWriter();
    try (InputStream inputStream =
        _manager.getSnapshotInputObject(
            network,
            snapshot,
            Paths.get(BfConsts.RELPATH_CONFIGURATIONS_DIR, fileName).toString())) {
      assertThat(inputStream, not(nullValue()));
      IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
      return writer.toString();
    }
  }

  @Test
  public void testForkSnapshotMissingBaseSnapshot() throws Exception {
    String networkName = "network";
    String snapshotBaseName = "snapshotBase";
    String snapshotNewName = "snapshotNew";

    _manager.initNetwork(networkName, null);

    // Fork should fail because base snapshot does not exist
    _thrown.expect(FileNotFoundException.class);
    _thrown.expectMessage(
        equalTo("Base snapshot with name: '" + snapshotBaseName + "' does not exist"));
    _manager.forkSnapshot(
        networkName,
        new ForkSnapshotBean(
            snapshotBaseName, snapshotNewName, null, null, null, null, null, null, null));
  }

  @Test
  public void testForkSnapshotMissingNetwork() throws Exception {
    String networkName = "network";
    String snapshotBaseName = "snapshotBase";
    String snapshotNewName = "snapshotNew";

    // Fork should fail because network does not exist
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(equalTo("Network '" + networkName + "' does not exist"));
    _manager.forkSnapshot(
        networkName,
        new ForkSnapshotBean(
            snapshotBaseName, snapshotNewName, null, null, null, null, null, null, null));
  }

  @Test
  public void testGetAnswerAnalysis() throws IOException {
    String containerName = "container1";
    String testrigName = "testrig1";
    String analysisName = "analysis1";
    String question1Name = "question1";
    Question question = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(question);
    String question2Name = "question2Name";
    String question3Name = "question3";

    _manager.initNetwork(containerName, null);
    Map<String, String> questionsToAdd =
        ImmutableMap.of(
            question1Name,
            questionContent,
            question2Name,
            questionContent,
            question3Name,
            questionContent);

    _manager.configureAnalysis(
        containerName, true, analysisName, questionsToAdd, Lists.newArrayList(), null);
    NetworkId networkId = _idManager.getNetworkId(containerName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(testrigName, networkId, snapshotId);
    AnalysisId analysisId = _idManager.getAnalysisId(analysisName, networkId);
    QuestionId questionId1 = _idManager.getQuestionId(question1Name, networkId, analysisId);
    QuestionId questionId2 = _idManager.getQuestionId(question2Name, networkId, analysisId);

    AnswerId baseAnswerId1 =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId1,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            analysisId);
    AnswerId baseAnswerId2 =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId2,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            analysisId);
    Answer answer1 = new Answer();
    Answer answer2 = new Answer();
    String answer1Text = "foo1";
    String answer2Text = "foo2";
    answer1.addAnswerElement(new StringAnswerElement(answer1Text));
    answer2.addAnswerElement(new StringAnswerElement(answer2Text));
    String answer1Str = BatfishObjectMapper.writeString(answer1);
    String answer2Str = BatfishObjectMapper.writeString(answer2);
    AnswerMetadata answerMetadata1 =
        AnswerMetadataUtil.computeAnswerMetadata(answer1, Main.getLogger());
    AnswerMetadata answerMetadata2 =
        AnswerMetadataUtil.computeAnswerMetadata(answer1, Main.getLogger());
    _storage.storeAnswer(answer1Str, baseAnswerId1);
    _storage.storeAnswer(answer2Str, baseAnswerId2);
    _storage.storeAnswerMetadata(answerMetadata1, baseAnswerId1);
    _storage.storeAnswerMetadata(answerMetadata2, baseAnswerId2);

    String answer1Output =
        _manager.getAnswer(containerName, testrigName, question1Name, null, analysisName);
    String answer2Output =
        _manager.getAnswer(containerName, testrigName, question2Name, null, analysisName);
    String answer3Output =
        _manager.getAnswer(containerName, testrigName, question3Name, null, analysisName);

    Answer failedAnswer = Answer.failureAnswer("Not answered", null);
    failedAnswer.setStatus(AnswerStatus.NOTFOUND);
    String failedAnswerString = BatfishObjectMapper.writePrettyString(failedAnswer);

    assertThat(
        ((StringAnswerElement)
                BatfishObjectMapper.mapper()
                    .readValue(answer1Output, Answer.class)
                    .getAnswerElements()
                    .get(0))
            .getAnswer(),
        equalTo(answer1Text));
    assertThat(
        ((StringAnswerElement)
                BatfishObjectMapper.mapper()
                    .readValue(answer2Output, Answer.class)
                    .getAnswerElements()
                    .get(0))
            .getAnswer(),
        equalTo(answer2Text));
    assertThat(answer3Output, equalTo(failedAnswerString));
  }

  @Test
  public void testGetAnalysisAnswers() throws JsonProcessingException, FileNotFoundException {
    String containerName = "container1";
    String testrigName = "testrig1";
    String analysisName = "analysis1";
    String question1Name = "question1";
    Question question = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(question);
    String question2Name = "question2Name";

    _manager.initNetwork(containerName, null);
    Map<String, String> questionsToAdd =
        ImmutableMap.of(question1Name, questionContent, question2Name, questionContent);

    _manager.configureAnalysis(
        containerName, true, analysisName, questionsToAdd, Lists.newArrayList(), null);
    NetworkId networkId = _idManager.getNetworkId(containerName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(testrigName, networkId, snapshotId);
    AnalysisId analysisId = _idManager.getAnalysisId(analysisName, networkId);
    QuestionId questionId1 = _idManager.getQuestionId(question1Name, networkId, analysisId);
    QuestionId questionId2 = _idManager.getQuestionId(question2Name, networkId, analysisId);

    AnswerId baseAnswerId1 =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId1,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            analysisId);
    AnswerId baseAnswerId2 =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId2,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            analysisId);
    Answer answer1 = new Answer();
    Answer answer2 = new Answer();
    String answer1Text = "foo1";
    String answer2Text = "foo2";
    answer1.addAnswerElement(new StringAnswerElement(answer1Text));
    answer2.addAnswerElement(new StringAnswerElement(answer2Text));
    String answer1Str = BatfishObjectMapper.writeString(answer1);
    String answer2Str = BatfishObjectMapper.writeString(answer2);
    AnswerMetadata answerMetadata1 =
        AnswerMetadataUtil.computeAnswerMetadata(answer1, Main.getLogger());
    AnswerMetadata answerMetadata2 =
        AnswerMetadataUtil.computeAnswerMetadata(answer1, Main.getLogger());
    _storage.storeAnswer(answer1Str, baseAnswerId1);
    _storage.storeAnswer(answer2Str, baseAnswerId2);
    _storage.storeAnswerMetadata(answerMetadata1, baseAnswerId1);
    _storage.storeAnswerMetadata(answerMetadata2, baseAnswerId2);

    Map<String, String> answers1 =
        _manager.getAnalysisAnswers(
            containerName, testrigName, null, analysisName, ImmutableSet.of());
    Map<String, String> answers2 =
        _manager.getAnalysisAnswers(
            containerName, testrigName, null, analysisName, ImmutableSet.of(question1Name));
    Map<String, String> answers3 =
        _manager.getAnalysisAnswers(
            containerName, testrigName, null, analysisName, ImmutableSet.of());

    assertThat(answers1.keySet(), containsInAnyOrder(question1Name, question2Name));
    assertThat(answers2.keySet(), containsInAnyOrder(question1Name));
    assertThat(answers3.keySet(), containsInAnyOrder(question1Name, question2Name));
  }

  /** Test that we return good answers when some questions are bad */
  @Test
  public void testGetAnalysisAnswersAndMetadataPartial()
      throws JsonProcessingException, FileNotFoundException {
    String containerName = "container1";
    String testrigName = "testrig1";
    String analysisName = "analysis1";

    String question1Name = "question1";
    String question1Content = BatfishObjectMapper.writeString(new TestQuestion());

    String question2Name = "question2";
    String question2Content = "bogus"; // bad content

    _manager.initNetwork(containerName, null);
    Map<String, String> questionsToAdd =
        ImmutableMap.of(question1Name, question1Content, question2Name, question2Content);

    _manager.configureAnalysis(
        containerName, true, analysisName, questionsToAdd, Lists.newArrayList(), null);
    NetworkId networkId = _idManager.getNetworkId(containerName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(testrigName, networkId, snapshotId);
    AnalysisId analysisId = _idManager.getAnalysisId(analysisName, networkId);
    QuestionId questionId1 = _idManager.getQuestionId(question1Name, networkId, analysisId);
    QuestionId questionId2 = _idManager.getQuestionId(question2Name, networkId, analysisId);

    AnswerId baseAnswerId1 =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId1,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            analysisId);
    AnswerId baseAnswerId2 =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId2,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            analysisId);
    Answer answer1 = new Answer();
    Answer answer2 = new Answer();
    String answer1Text = "foo1";
    String answer2Text = "foo2";
    answer1.addAnswerElement(new StringAnswerElement(answer1Text));
    answer2.addAnswerElement(new StringAnswerElement(answer2Text));
    String answer1Str = BatfishObjectMapper.writeString(answer1);
    String answer2Str = BatfishObjectMapper.writeString(answer2);
    AnswerMetadata answerMetadata1 =
        AnswerMetadataUtil.computeAnswerMetadata(answer1, Main.getLogger());
    AnswerMetadata answerMetadata2 =
        AnswerMetadataUtil.computeAnswerMetadata(answer1, Main.getLogger());
    _storage.storeAnswer(answer1Str, baseAnswerId1);
    _storage.storeAnswer(answer2Str, baseAnswerId2);
    _storage.storeAnswerMetadata(answerMetadata1, baseAnswerId1);
    _storage.storeAnswerMetadata(answerMetadata2, baseAnswerId2);

    assertThat(
        _manager.getAnalysisAnswers(
            containerName, testrigName, null, analysisName, ImmutableSet.of()),
        equalTo(
            ImmutableMap.of(
                question1Name,
                BatfishObjectMapper.mapper().writeValueAsString(answer1),
                question2Name,
                BatfishObjectMapper.mapper()
                    .writeValueAsString(
                        Answer.failureAnswer(
                            "Could not convert raw question text [bogus] to JSON", null)))));

    assertThat(
        _manager.getAnalysisAnswersMetadata(
            containerName, testrigName, null, analysisName, ImmutableSet.of()),
        equalTo(
            ImmutableMap.of(
                question1Name,
                answerMetadata1,
                question2Name,
                AnswerMetadata.forStatus(AnswerStatus.FAILURE))));
  }

  @Test
  public void testGetAutoWorkQueueUserAnalysis() {
    String containerName = "myContainer";
    String testrigName = "myTestrig";
    _manager.initNetwork(containerName, null);

    // user policy
    _manager.configureAnalysis(
        containerName, true, "useranalysis", Maps.newHashMap(), Lists.newArrayList(), false);

    WorkItem parseWorkItem = WorkItemBuilder.getWorkItemParse(containerName, testrigName);

    WorkItem analysisWorkItem =
        WorkItemBuilder.getWorkItemRunAnalysis(
            "useranalysis", containerName, testrigName, null, false, false);

    List<WorkItem> workQueue = _manager.getAutoWorkQueue(containerName, testrigName);

    assertThat(workQueue, hasSize(2));

    // checking that the first work item is for parse
    assertThat(workQueue.get(0).matches(parseWorkItem), equalTo(true));

    // checking run analysis workitem
    assertThat(
        "Work Queue not correct for user analyses",
        workQueue.get(1).matches(analysisWorkItem),
        equalTo(true));
  }

  @Test
  public void testGetAnswerMetadataAnalysisSuccess()
      throws JsonProcessingException, FileNotFoundException {
    String networkName = "network1";
    String snapshotName = "snapshot1";
    String analysisName = "analysis1";
    Question question = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(question);
    String questionName = "question1Name";
    _manager.initNetwork(networkName, null);
    _manager.configureAnalysis(
        networkName,
        true,
        analysisName,
        ImmutableMap.of(questionName, questionContent),
        ImmutableList.of(),
        null);
    NetworkId networkId = _idManager.getNetworkId(networkName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(snapshotName, networkId, snapshotId);
    AnalysisId analysisId = _idManager.getAnalysisId(analysisName, networkId);
    QuestionId questionId = _idManager.getQuestionId(questionName, networkId, analysisId);
    AnswerId baseAnswerId =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            analysisId);
    Answer answer = new Answer();
    answer.addAnswerElement(new TableAnswerElement(MOCK_TABLE_METADATA));
    String answerStr = BatfishObjectMapper.writeString(answer);
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(answer, Main.getLogger());
    _storage.storeAnswer(answerStr, baseAnswerId);
    _storage.storeAnswerMetadata(answerMetadata, baseAnswerId);
    AnswerMetadata answerResult =
        _manager.getAnswerMetadata(networkName, snapshotName, questionName, null, analysisName);

    assertThat(answerResult, equalTo(answerMetadata));
  }

  @Test
  public void testGetAnswerMetadataAnalysisMissingQuestion()
      throws JsonProcessingException, FileNotFoundException {
    String networkName = "network1";
    String snapshotName = "snapshot1";
    String analysisName = "analysis1";
    Question question = new TestQuestion();
    String questionName = "question1";
    _manager.initNetwork(networkName, null);
    _manager.configureAnalysis(
        networkName, true, analysisName, ImmutableMap.of(), ImmutableList.of(), null);
    NetworkId networkId = _idManager.getNetworkId(networkName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(snapshotName, networkId, snapshotId);
    QuestionSettingsId questionSettingsId = new QuestionSettingsId("blah");
    _idManager.assignQuestionSettingsId(question.getName(), networkId, questionSettingsId);

    _thrown.expect(IllegalArgumentException.class);
    _thrown.expectMessage(containsString(questionName));
    _manager.getAnswerMetadata(networkName, snapshotName, questionName, null, analysisName);
  }

  @Test
  public void testGetAnswerMetadataAnalysisMissingAnswerMetadata() throws IOException {
    String networkName = "network1";
    String snapshotName = "snapshot1";
    String analysisName = "analysis1";
    Question question = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(question);
    String questionName = "question2Name";
    _manager.initNetwork(networkName, null);
    _manager.configureAnalysis(
        networkName,
        true,
        analysisName,
        ImmutableMap.of(questionName, questionContent),
        ImmutableList.of(),
        null);
    NetworkId networkId = _idManager.getNetworkId(networkName);
    AnalysisId analysisId = _idManager.getAnalysisId(analysisName, networkId);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(snapshotName, networkId, snapshotId);
    QuestionId questionId = _idManager.getQuestionId(questionName, networkId, analysisId);
    AnswerId baseAnswerId =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            analysisId);
    Answer answer = new Answer();
    answer.addAnswerElement(new TableAnswerElement(MOCK_TABLE_METADATA));
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(answer, Main.getLogger());
    _storage.storeAnswerMetadata(answerMetadata, baseAnswerId);
    String answerStr = BatfishObjectMapper.writeString(answer);
    _storage.storeAnswer(answerStr, baseAnswerId);
    // remove answer metadata
    _storage.deleteAnswerMetadata(baseAnswerId);

    assertThat(
        _manager.getAnswerMetadata(networkName, snapshotName, questionName, null, analysisName),
        equalTo(AnswerMetadata.forStatus(AnswerStatus.NOTFOUND)));
  }

  @Test
  public void testGetAnswerMetadataAnalysisMissingAnalysis()
      throws JsonProcessingException, FileNotFoundException {
    String networkName = "network1";
    String snapshotName = "snapshot1";
    String analysisName = "analysis1";
    Question question = new TestQuestion();
    String questionName = "question1";
    _manager.initNetwork(networkName, null);
    NetworkId networkId = _idManager.getNetworkId(networkName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(snapshotName, networkId, snapshotId);
    // the analysis id is not assigned, so the analysis is effectively missing
    QuestionSettingsId questionSettingsId = new QuestionSettingsId("blah");
    _idManager.assignQuestionSettingsId(question.getName(), networkId, questionSettingsId);

    _thrown.expect(IllegalArgumentException.class);
    _thrown.expectMessage(containsString(analysisName));
    _manager.getAnswerMetadata(networkName, snapshotName, questionName, null, analysisName);
  }

  @Test
  public void testGetAnswerMetadataAdHocSuccess()
      throws JsonProcessingException, FileNotFoundException {
    String networkName = "network1";
    String snapshotName = "snapshot1";
    Question question = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(question);
    String questionName = "question2Name";
    _manager.initNetwork(networkName, null);
    _manager.uploadQuestion(networkName, questionName, questionContent, false);
    NetworkId networkId = _idManager.getNetworkId(networkName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(snapshotName, networkId, snapshotId);
    QuestionId questionId = _idManager.getQuestionId(questionName, networkId, null);
    AnswerId baseAnswerId =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            null);
    Answer answer = new Answer();
    answer.addAnswerElement(new TableAnswerElement(MOCK_TABLE_METADATA));
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(answer, Main.getLogger());
    _storage.storeAnswerMetadata(answerMetadata, baseAnswerId);
    String answerStr = BatfishObjectMapper.writeString(answer);
    _storage.storeAnswer(answerStr, baseAnswerId);
    AnswerMetadata answerResult =
        _manager.getAnswerMetadata(networkName, snapshotName, questionName, null, null);

    assertThat(answerResult, equalTo(answerMetadata));
  }

  @Test
  public void testGetAnswerMetadataAdHocMissingAnswerMetadata() throws IOException {
    String networkName = "network1";
    String snapshotName = "snapshot1";
    Question question = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(question);
    String questionName = "question2Name";
    _manager.initNetwork(networkName, null);
    _manager.uploadQuestion(networkName, questionName, questionContent, false);
    NetworkId networkId = _idManager.getNetworkId(networkName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(snapshotName, networkId, snapshotId);
    QuestionId questionId = _idManager.getQuestionId(questionName, networkId, null);
    AnswerId baseAnswerId =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            null);
    Answer answer = new Answer();
    answer.addAnswerElement(new TableAnswerElement(MOCK_TABLE_METADATA));
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(answer, Main.getLogger());
    _storage.storeAnswerMetadata(answerMetadata, baseAnswerId);
    String answerStr = BatfishObjectMapper.writeString(answer);
    _storage.storeAnswer(answerStr, baseAnswerId);
    // remove answer metadata
    _storage.deleteAnswerMetadata(baseAnswerId);

    assertThat(
        _manager.getAnswerMetadata(networkName, snapshotName, questionName, null, null),
        equalTo(AnswerMetadata.forStatus(AnswerStatus.NOTFOUND)));
  }

  @Test
  public void testGetAnswerMetadataAdHocMissingQuestion()
      throws JsonProcessingException, FileNotFoundException {
    String networkName = "network1";
    String snapshotName = "snapshot1";
    Question question = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(question);
    String questionName = "question2Name";
    _manager.initNetwork(networkName, null);
    _manager.uploadQuestion(networkName, questionName, questionContent, false);
    NetworkId networkId = _idManager.getNetworkId(networkName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(snapshotName, networkId, snapshotId);
    QuestionId questionId = _idManager.getQuestionId(questionName, networkId, null);
    QuestionSettingsId questionSettingsId = new QuestionSettingsId("blah");
    _idManager.assignQuestionSettingsId(question.getName(), networkId, questionSettingsId);
    AnswerId baseAnswerId =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            null);
    Answer answer = new Answer();
    answer.addAnswerElement(new TableAnswerElement(MOCK_TABLE_METADATA));
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(answer, Main.getLogger());
    _storage.storeAnswerMetadata(answerMetadata, baseAnswerId);
    String answerStr = BatfishObjectMapper.writeString(answer);
    _storage.storeAnswer(answerStr, baseAnswerId);
    // remove question
    _idManager.deleteQuestion(questionName, networkId, null);

    _thrown.expect(IllegalArgumentException.class);
    _thrown.expectMessage(containsString(questionName));
    _manager.getAnswerMetadata(networkName, snapshotName, questionName, null, null);
  }

  @Test
  public void testGetAutoWorkQueueSuggestedAnalysis() {
    String containerName = "myContainer";
    String testrigName = "myTestrig";
    _manager.initNetwork(containerName, null);

    // user policy
    _manager.configureAnalysis(
        containerName, true, "suggestedanalysis", Maps.newHashMap(), Lists.newArrayList(), true);

    WorkItem parseWorkItem = WorkItemBuilder.getWorkItemParse(containerName, testrigName);

    WorkItem analysisWorkItem =
        WorkItemBuilder.getWorkItemRunAnalysis(
            "suggestedanalysis", containerName, testrigName, null, false, false);

    List<WorkItem> workQueue = _manager.getAutoWorkQueue(containerName, testrigName);

    assertThat(workQueue, hasSize(2));

    // checking that the first work item is for parse
    assertThat(workQueue.get(0).matches(parseWorkItem), equalTo(true));

    // checking run analysis workitem
    assertThat(
        "Work Queue not correct for suggested analyses",
        workQueue.get(1).matches(analysisWorkItem),
        equalTo(true));
  }

  private boolean getMetadataSuggested(String containerName, String analysisName) {
    NetworkId networkId = _idManager.getNetworkId(containerName);
    AnalysisId analysisId = _idManager.getAnalysisId(analysisName, networkId);
    try {
      return AnalysisMetadataMgr.readMetadata(networkId, analysisId).getSuggested();
    } catch (IOException e) {
      throw new BatfishException("Failed to read metadata", e);
    }
  }

  @Test
  @Deprecated
  public void testGetObjectInput() throws IOException {
    String network = "network";
    String snapshot = "snapshot";
    String fileName = "test.cfg";

    _manager.initNetwork(network, null);
    uploadTestSnapshot(network, snapshot, fileName);
    // We know the config file will be written under 'configs/' in the snapshot zip
    Path object = _manager.getTestrigObject(network, snapshot, "configs/" + fileName);

    // Confirm object is found in the input directory
    assertThat(object, is(not(nullValue())));
    assertThat(object.toFile(), anExistingFile());
  }

  @Test
  @Deprecated
  public void testGetObjectOutput() throws IOException {
    String network = "network";
    String snapshot = "snapshot";
    String fileName = BfConsts.RELPATH_METADATA_FILE;

    _manager.initNetwork(network, null);
    createSnapshotWithMetadata(network, snapshot);
    Path object = _manager.getTestrigObject(network, snapshot, fileName);

    // Confirm metadata file is found (lives in the output directory)
    assertThat(object, is(not(nullValue())));
    assertThat(object.toFile(), anExistingFile());
  }

  @Test
  @Deprecated
  public void testGetObjectMissing() throws IOException {
    String network = "network";
    String snapshot = "snapshot";
    String fileName = "missing.file";

    _manager.initNetwork(network, null);
    createSnapshotWithMetadata(network, snapshot);
    Path object = _manager.getTestrigObject(network, snapshot, fileName);

    // Confirm the bogus file is not found
    assertThat(object, is(nullValue()));
  }

  @Test
  public void testGenerateDateString() {
    assertThat(
        generateFileDateString("foo", Instant.parse("2018-04-19T12:34:56Z")),
        equalTo("foo_2018-04-19T12-34-56.000"));

    assertThat(
        generateFileDateString(
            "foo",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .parse("2018-04-19T12:34:56-08:00")
                .query(Instant::from)),
        equalTo("foo_2018-04-19T20-34-56.000"));
  }

  @Test
  public void testInitSnapshot() throws Exception {
    String networkName = "network";
    String snapshotName = "snapshotName";
    String fileName = "file.type";
    String fileContents = "! empty config";

    _manager.initNetwork(networkName, null);

    // Create snapshot dir to pass into init
    Path srcDir = createSnapshot(snapshotName, fileName, fileContents, _folder);

    _manager.initSnapshot(networkName, snapshotName, srcDir, false);

    // Confirm the new snapshot exists
    assertThat(_manager.getLatestSnapshot(networkName), equalTo(Optional.of(snapshotName)));
  }

  @Test
  public void testInitSnapshotBadPackaging() throws Exception {
    String networkName = "network";
    String snapshotName = "snapshotName";
    String fileName = "file.type";
    String fileContents = "! empty config";

    _manager.initNetwork(networkName, null);

    // Create snapshot dir to pass into init
    Path srcDir = createSnapshot(snapshotName, fileName, fileContents, _folder);

    // Init should fail with improperly formatted source dir
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(containsString("Unexpected packaging of snapshot"));

    // Pass in path to subdir so init sees improperly formatted dir
    // i.e. sees dir containing 'configs/' instead of 'snapshotName/configs/'
    _manager.initSnapshot(networkName, snapshotName, srcDir.resolve(snapshotName), false);
  }

  @Test
  public void testProcessAnalysisAnswers() throws IOException {
    String questionName = "q";
    String columnName = "issue";
    int maxRows = 1;
    int rowOffset = 0;

    TableAnswerElement table =
        new TableAnswerElement(
            new TableMetadata(
                ImmutableList.of(new ColumnMetadata(columnName, Schema.ISSUE, "foobar"))));
    table.addRow(Row.of(columnName, new Issue("blah", 5, new Issue.Type("m", "n"))));
    Answer answer = new Answer();
    answer.addAnswerElement(table);
    answer.setStatus(AnswerStatus.SUCCESS);
    String answerStr = BatfishObjectMapper.writePrettyString(answer);
    Map<String, String> rawAnswers = ImmutableMap.of(questionName, answerStr);
    AnswerRowsOptions options =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            maxRows,
            rowOffset,
            ImmutableList.of(new ColumnSortOption(columnName, true)),
            false);
    Map<String, AnswerRowsOptions> analysisAnswersOptions = ImmutableMap.of(questionName, options);

    Map<String, Answer> processedAnswers =
        _manager.processAnalysisAnswers(rawAnswers, analysisAnswersOptions);
    List<Row> processedRows =
        ((TableAnswerElement) processedAnswers.get(questionName).getAnswerElements().get(0))
            .getRowsList();

    assertThat(processedRows, equalTo(table.getRowsList()));
  }

  @Test
  public void testProcessAnswerRows() throws IOException {
    String columnName = "issue";
    int maxRows = 1;
    int rowOffset = 0;
    TableAnswerElement table =
        new TableAnswerElement(
            new TableMetadata(
                ImmutableList.of(new ColumnMetadata(columnName, Schema.ISSUE, "foobar"))));
    table.addRow(Row.of(columnName, new Issue("blah", 5, new Issue.Type("m", "n"))));
    Answer answer = new Answer();
    answer.addAnswerElement(table);
    answer.setStatus(AnswerStatus.SUCCESS);
    String answerStr = BatfishObjectMapper.writePrettyString(answer);
    AnswerRowsOptions options =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            maxRows,
            rowOffset,
            ImmutableList.of(new ColumnSortOption(columnName, true)),
            false);

    List<Row> processedRows =
        ((TableAnswerElement)
                _manager.processAnswerRows(answerStr, options).getAnswerElements().get(0))
            .getRowsList();

    assertThat(processedRows, equalTo(table.getRowsList()));
  }

  @Test
  public void testProcessAnswerRows2() throws IOException {
    String columnName = "issue";
    int maxRows = 1;
    int rowOffset = 0;
    TableAnswerElement table =
        new TableAnswerElement(
            new TableMetadata(
                ImmutableList.of(new ColumnMetadata(columnName, Schema.ISSUE, "foobar"))));
    table.addRow(Row.of(columnName, new Issue("blah", 5, new Issue.Type("m", "n"))));
    Answer answer = new Answer();
    answer.addAnswerElement(table);
    answer.setStatus(AnswerStatus.SUCCESS);
    String answerStr = BatfishObjectMapper.writePrettyString(answer);
    AnswerRowsOptions options =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            maxRows,
            rowOffset,
            ImmutableList.of(new ColumnSortOption(columnName, true)),
            false);

    List<Row> processedRows =
        ((TableView) _manager.processAnswerRows2(answerStr, options).getAnswerElements().get(0))
            .getInnerRows();

    assertThat(processedRows, equalTo(table.getRowsList()));
  }

  @Test
  public void testProcessAnswerRowsFailure() throws IOException {
    String columnName = "issue";
    int maxRows = 1;
    int rowOffset = 0;
    AnswerRowsOptions options =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            maxRows,
            rowOffset,
            ImmutableList.of(new ColumnSortOption(columnName, true)),
            false);
    Answer badInput = new Answer();
    badInput.setStatus(AnswerStatus.SUCCESS);
    String rawAnswerStr = BatfishObjectMapper.writePrettyString(badInput);
    Answer processedAnswer = _manager.processAnswerRows(rawAnswerStr, options);

    assertThat(processedAnswer.getStatus(), equalTo(AnswerStatus.FAILURE));
  }

  @Test
  public void testProcessAnswerRowsNotFound() throws IOException {
    String columnName = "issue";
    int maxRows = 1;
    int rowOffset = 0;
    AnswerRowsOptions options =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            maxRows,
            rowOffset,
            ImmutableList.of(new ColumnSortOption(columnName, true)),
            false);
    Answer processedAnswer = _manager.processAnswerRows(null, options);

    assertThat(processedAnswer.getStatus(), equalTo(AnswerStatus.NOTFOUND));
  }

  @Test
  public void testProcessAnswerRowsStale() throws IOException {
    String columnName = "issue";
    int maxRows = 1;
    int rowOffset = 0;
    AnswerRowsOptions options =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            maxRows,
            rowOffset,
            ImmutableList.of(new ColumnSortOption(columnName, true)),
            false);
    Answer badInput = new Answer();
    badInput.setStatus(AnswerStatus.STALE);
    String rawAnswerStr = BatfishObjectMapper.writePrettyString(badInput);
    Answer processedAnswer = _manager.processAnswerRows(rawAnswerStr, options);

    assertThat(processedAnswer.getStatus(), equalTo(AnswerStatus.STALE));
  }

  @Test
  public void testProcessAnswerTableSorting() {
    String columnName = "val";
    TableAnswerElement table =
        new TableAnswerElement(
            new TableMetadata(
                ImmutableList.of(new ColumnMetadata(columnName, Schema.INTEGER, "foobar"))));
    Row row1 = Row.of(columnName, 1);
    Row row2 = Row.of(columnName, 2);
    table.addRow(row1);
    table.addRow(row2);
    AnswerRowsOptions optionsSorting =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(new ColumnSortOption(columnName, false)),
            false);
    AnswerRowsOptions optionsSortingReverse =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(new ColumnSortOption(columnName, true)),
            false);

    assertThat(
        _manager.processAnswerTable(table, optionsSorting).getRowsList(),
        equalTo(ImmutableList.of(row1, row2)));
    assertThat(
        _manager.processAnswerTable(table, optionsSortingReverse).getRowsList(),
        equalTo(ImmutableList.of(row2, row1)));
  }

  @Test
  public void testProcessAnswerTableOffset() {
    String columnName = "val";
    TableAnswerElement table =
        new TableAnswerElement(
            new TableMetadata(
                ImmutableList.of(new ColumnMetadata(columnName, Schema.INTEGER, "foobar"))));
    Row row1 = Row.of(columnName, 1);
    Row row2 = Row.of(columnName, 2);
    table.addRow(row1);
    table.addRow(row2);
    AnswerRowsOptions optionsNoOffset =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(),
            false);
    AnswerRowsOptions optionsOffset =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            Integer.MAX_VALUE,
            1,
            ImmutableList.of(),
            false);

    assertThat(
        _manager.processAnswerTable(table, optionsNoOffset).getRowsList(),
        equalTo(ImmutableList.of(row1, row2)));
    assertThat(
        _manager.processAnswerTable(table, optionsOffset).getRowsList(),
        equalTo(ImmutableList.of(row2)));
  }

  @Test
  public void testProcessAnswerTableFiltered() {
    String columnName = "val";
    TableAnswerElement table =
        new TableAnswerElement(
            new TableMetadata(
                ImmutableList.of(new ColumnMetadata(columnName, Schema.STRING, "foobar"))));
    String whitelistedValue = "hello";
    Row row1 = Row.of(columnName, whitelistedValue);
    Row row2 = Row.of(columnName, "goodbye");
    table.addRow(row1);
    table.addRow(row2);
    AnswerRowsOptions optionsNotFiltered =
        new AnswerRowsOptions(
            ImmutableSet.of(), ImmutableList.of(), Integer.MAX_VALUE, 0, ImmutableList.of(), false);
    AnswerRowsOptions optionsFiltered =
        new AnswerRowsOptions(
            ImmutableSet.of(),
            ImmutableList.of(new ColumnFilter(columnName, whitelistedValue)),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(),
            false);

    TableAnswerElement notFiltered = _manager.processAnswerTable(table, optionsNotFiltered);
    TableAnswerElement filtered = _manager.processAnswerTable(table, optionsFiltered);

    assertThat(notFiltered.getRowsList(), equalTo(ImmutableList.of(row1, row2)));
    assertThat(filtered.getRowsList(), equalTo(ImmutableList.of(row1)));

    assertThat(notFiltered.getSummary().getNumResults(), equalTo(2));
    assertThat(filtered.getSummary().getNumResults(), equalTo(1));
  }

  @Test
  public void testProcessAnswerTable2Filtered() {
    String columnName = "val";
    TableAnswerElement table =
        new TableAnswerElement(
            new TableMetadata(
                ImmutableList.of(new ColumnMetadata(columnName, Schema.STRING, "foobar"))));
    String whitelistedValue = "hello";
    Row row1 = Row.of(columnName, whitelistedValue);
    Row row2 = Row.of(columnName, "goodbye");
    table.addRow(row1);
    table.addRow(row2);
    AnswerRowsOptions optionsNotFiltered =
        new AnswerRowsOptions(
            ImmutableSet.of(), ImmutableList.of(), Integer.MAX_VALUE, 0, ImmutableList.of(), false);
    AnswerRowsOptions optionsFiltered =
        new AnswerRowsOptions(
            ImmutableSet.of(),
            ImmutableList.of(new ColumnFilter(columnName, whitelistedValue)),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(),
            false);

    TableView notFiltered = _manager.processAnswerTable2(table, optionsNotFiltered);
    TableView filtered = _manager.processAnswerTable2(table, optionsFiltered);

    assertThat(notFiltered.getInnerRows(), equalTo(ImmutableList.of(row1, row2)));
    assertThat(filtered.getInnerRows(), equalTo(ImmutableList.of(row1)));

    assertThat(notFiltered.getSummary().getNumResults(), equalTo(2));
    assertThat(filtered.getSummary().getNumResults(), equalTo(1));
  }

  @Test
  public void testProcessAnswerTableMaxRows() {
    String columnName = "val";
    TableAnswerElement table =
        new TableAnswerElement(
            new TableMetadata(
                ImmutableList.of(new ColumnMetadata(columnName, Schema.INTEGER, "foobar"))));
    Row row1 = Row.of(columnName, 1);
    Row row2 = Row.of(columnName, 2);
    table.addRow(row1);
    table.addRow(row2);
    AnswerRowsOptions optionsNoLimit =
        new AnswerRowsOptions(
            ImmutableSet.of(), ImmutableList.of(), Integer.MAX_VALUE, 0, ImmutableList.of(), false);
    AnswerRowsOptions optionsLimit =
        new AnswerRowsOptions(
            ImmutableSet.of(), ImmutableList.of(), 1, 0, ImmutableList.of(), false);

    assertThat(
        _manager.processAnswerTable(table, optionsNoLimit).getRowsList(),
        equalTo(ImmutableList.of(row1, row2)));
    assertThat(
        _manager.processAnswerTable(table, optionsLimit).getRowsList(),
        equalTo(ImmutableList.of(row1)));
  }

  @Test
  public void testProcessAnswerTableProject() {
    String columnName = "val";
    String otherColumnName = "val2";
    TableMetadata originalMetadata =
        new TableMetadata(
            ImmutableList.of(
                new ColumnMetadata(columnName, Schema.INTEGER, "foobar"),
                new ColumnMetadata(otherColumnName, Schema.INTEGER, "foobaz")));
    TableAnswerElement table = new TableAnswerElement(originalMetadata);
    Row row1 = Row.of(columnName, 1, otherColumnName, 3);
    Row row2 = Row.of(columnName, 2, otherColumnName, 4);
    table.addRow(row1);
    table.addRow(row2);
    AnswerRowsOptions optionsNoProject =
        new AnswerRowsOptions(
            ImmutableSet.of(), ImmutableList.of(), Integer.MAX_VALUE, 0, ImmutableList.of(), false);
    AnswerRowsOptions optionsProject =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(),
            false);

    Row row1Projected = Row.of(columnName, 1);
    Row row2Projected = Row.of(columnName, 2);

    assertThat(
        _manager.processAnswerTable(table, optionsNoProject).getRowsList(),
        equalTo(ImmutableList.of(row1, row2)));

    List<Row> projectedRows = _manager.processAnswerTable(table, optionsProject).getRowsList();

    assertThat(projectedRows, equalTo(ImmutableList.of(row1Projected, row2Projected)));
  }

  @Test
  public void testProcessAnswerTable2Project() {
    String columnName = "val";
    String otherColumnName = "val2";
    TableMetadata originalMetadata =
        new TableMetadata(
            ImmutableList.of(
                new ColumnMetadata(columnName, Schema.INTEGER, "foobar"),
                new ColumnMetadata(otherColumnName, Schema.INTEGER, "foobaz")));
    TableAnswerElement table = new TableAnswerElement(originalMetadata);
    Row row1 = Row.of(columnName, 1, otherColumnName, 3);
    Row row2 = Row.of(columnName, 2, otherColumnName, 4);
    table.addRow(row1);
    table.addRow(row2);
    AnswerRowsOptions optionsNoProject =
        new AnswerRowsOptions(
            ImmutableSet.of(), ImmutableList.of(), Integer.MAX_VALUE, 0, ImmutableList.of(), false);
    AnswerRowsOptions optionsProject =
        new AnswerRowsOptions(
            ImmutableSet.of(columnName),
            ImmutableList.of(),
            Integer.MAX_VALUE,
            0,
            ImmutableList.of(),
            false);

    TableViewRow row1Projected = new TableViewRow(0, Row.of(columnName, 1));
    TableViewRow row2Projected = new TableViewRow(1, Row.of(columnName, 2));

    assertThat(
        _manager.processAnswerTable(table, optionsNoProject).getRowsList(),
        equalTo(ImmutableList.of(row1, row2)));

    List<TableViewRow> projectedRows =
        _manager.processAnswerTable2(table, optionsProject).getRows();

    assertThat(projectedRows, equalTo(ImmutableList.of(row1Projected, row2Projected)));
    assertThat(projectedRows.get(0).getId(), equalTo(0));
    assertThat(projectedRows.get(1).getId(), equalTo(1));
  }

  @Test
  public void testProcessAnswerTableUniqueRows() {
    String columnName = "val";
    TableAnswerElement table =
        new TableAnswerElement(
            new TableMetadata(
                ImmutableList.of(new ColumnMetadata(columnName, Schema.INTEGER, "foobar"))));
    Row row1 = Row.of(columnName, 1);
    Row row2 = Row.of(columnName, 1);
    table.addRow(row1);
    table.addRow(row2);
    AnswerRowsOptions optionsNonUnique =
        new AnswerRowsOptions(
            ImmutableSet.of(), ImmutableList.of(), Integer.MAX_VALUE, 0, ImmutableList.of(), false);
    AnswerRowsOptions optionsUnique =
        new AnswerRowsOptions(
            ImmutableSet.of(), ImmutableList.of(), Integer.MAX_VALUE, 0, ImmutableList.of(), true);

    assertThat(
        _manager.processAnswerTable(table, optionsNonUnique).getRowsList(),
        equalTo(ImmutableList.of(row1, row2)));
    assertThat(
        _manager.processAnswerTable(table, optionsUnique).getRowsList(),
        equalTo(ImmutableList.of(row1)));
  }

  @Test
  public void testBuildComparator() {
    String col1 = "col1";
    String col2 = "col2";
    Map<String, ColumnMetadata> rawColumnMap =
        ImmutableMap.of(
            col1,
            new ColumnMetadata(col1, Schema.INTEGER, "blah"),
            col2,
            new ColumnMetadata(col2, Schema.INTEGER, "bloop"));
    Comparator<Row> comCol1 =
        _manager.buildComparator(rawColumnMap, ImmutableList.of(new ColumnSortOption(col1, false)));
    Comparator<Row> comCol1Reversed =
        _manager.buildComparator(rawColumnMap, ImmutableList.of(new ColumnSortOption(col1, true)));
    Comparator<Row> comCol2 =
        _manager.buildComparator(rawColumnMap, ImmutableList.of(new ColumnSortOption(col2, false)));
    Comparator<Row> comCol2Reversed =
        _manager.buildComparator(rawColumnMap, ImmutableList.of(new ColumnSortOption(col2, true)));
    Comparator<Row> comCol1Then2 =
        _manager.buildComparator(
            rawColumnMap,
            ImmutableList.of(new ColumnSortOption(col1, false), new ColumnSortOption(col2, false)));
    Comparator<Row> comCol2Then1 =
        _manager.buildComparator(
            rawColumnMap,
            ImmutableList.of(new ColumnSortOption(col2, false), new ColumnSortOption(col1, false)));

    Row row1 = Row.of(col1, 1, col2, 10);
    Row row2 = Row.of(col1, 1, col2, 20);
    Row row3 = Row.of(col1, 2, col2, 10);
    Row row4 = Row.of(col1, 2, col2, 20);

    assertThat(comCol1.compare(row1, row2), equalTo(0));
    assertThat(comCol1.compare(row1, row3), equalTo(-1));
    assertThat(comCol1.compare(row1, row4), equalTo(-1));
    assertThat(comCol1.compare(row2, row3), equalTo(-1));
    assertThat(comCol1.compare(row2, row4), equalTo(-1));
    assertThat(comCol1.compare(row3, row4), equalTo(0));

    assertThat(comCol1Reversed.compare(row1, row2), equalTo(0));
    assertThat(comCol1Reversed.compare(row1, row3), equalTo(1));
    assertThat(comCol1Reversed.compare(row1, row4), equalTo(1));
    assertThat(comCol1Reversed.compare(row2, row3), equalTo(1));
    assertThat(comCol1Reversed.compare(row2, row4), equalTo(1));
    assertThat(comCol1Reversed.compare(row3, row4), equalTo(0));

    assertThat(comCol2.compare(row1, row2), equalTo(-1));
    assertThat(comCol2.compare(row1, row3), equalTo(0));
    assertThat(comCol2.compare(row1, row4), equalTo(-1));
    assertThat(comCol2.compare(row2, row3), equalTo(1));
    assertThat(comCol2.compare(row2, row4), equalTo(0));
    assertThat(comCol2.compare(row3, row4), equalTo(-1));

    assertThat(comCol2Reversed.compare(row1, row2), equalTo(1));
    assertThat(comCol2Reversed.compare(row1, row3), equalTo(0));
    assertThat(comCol2Reversed.compare(row1, row4), equalTo(1));
    assertThat(comCol2Reversed.compare(row2, row3), equalTo(-1));
    assertThat(comCol2Reversed.compare(row2, row4), equalTo(0));
    assertThat(comCol2Reversed.compare(row3, row4), equalTo(1));

    assertThat(comCol1Then2.compare(row1, row2), equalTo(-1));
    assertThat(comCol1Then2.compare(row1, row3), equalTo(-1));
    assertThat(comCol1Then2.compare(row1, row4), equalTo(-1));
    assertThat(comCol1Then2.compare(row2, row3), equalTo(-1));
    assertThat(comCol1Then2.compare(row2, row4), equalTo(-1));
    assertThat(comCol1Then2.compare(row3, row4), equalTo(-1));

    assertThat(comCol2Then1.compare(row1, row2), equalTo(-1));
    assertThat(comCol2Then1.compare(row1, row3), equalTo(-1));
    assertThat(comCol2Then1.compare(row1, row4), equalTo(-1));
    assertThat(comCol2Then1.compare(row2, row3), equalTo(1));
    assertThat(comCol2Then1.compare(row2, row4), equalTo(-1));
    assertThat(comCol2Then1.compare(row3, row4), equalTo(-1));
  }

  @Test
  public void testColumnComparator() {
    String colInteger = "colInteger";
    String colIssue = "colIssue";
    String colString = "colString";

    ColumnMetadata columnMetadataInteger =
        new ColumnMetadata(colInteger, Schema.INTEGER, "colIntegerDesc");
    ColumnMetadata columnMetadataIssue = new ColumnMetadata(colIssue, Schema.ISSUE, "colIssueDesc");
    ColumnMetadata columnMetadataString =
        new ColumnMetadata(colString, Schema.STRING, "colStringDesc");

    Comparator<Row> comInteger = _manager.columnComparator(columnMetadataInteger);
    Comparator<Row> comIssue = _manager.columnComparator(columnMetadataIssue);
    Comparator<Row> comString = _manager.columnComparator(columnMetadataString);

    Row r1 =
        Row.of(
            colInteger,
            1,
            colIssue,
            new Issue("blah", 1, new Issue.Type("major", "minor")),
            colString,
            "a");
    Row r2 =
        Row.of(
            colInteger,
            2,
            colIssue,
            new Issue("blah", 2, new Issue.Type("major", "minor")),
            colString,
            "b");

    assertThat(comInteger.compare(r1, r2), lessThan(0));
    assertThat(comIssue.compare(r1, r2), lessThan(0));
    assertThat(comString.compare(r1, r2), lessThan(0));
  }

  @Test
  public void testColumnComparatorAclTrace() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.ACL_TRACE, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 =
        Row.of(col, new AclTrace(ImmutableList.of(new DefaultDeniedByIpAccessList("a", "a", "a"))));
    Row r2 =
        Row.of(col, new AclTrace(ImmutableList.of(new DefaultDeniedByIpAccessList("b", "b", "b"))));

    assertThat(comparator.compare(r1, r2), lessThan(0));
  }

  @Test
  public void testColumnComparatorBoolean() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.BOOLEAN, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, false);
    Row r2 = Row.of(col, true);

    assertThat(comparator.compare(r1, r2), lessThan(0));
  }

  @Test
  public void testColumnComparatorFlow() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.FLOW, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, Flow.builder().setDstIp(Ip.ZERO).setIngressNode("a").setTag("a").build());
    Row r2 = Row.of(col, Flow.builder().setDstIp(Ip.MAX).setIngressNode("a").setTag("a").build());

    assertThat(comparator.compare(r1, r2), lessThan(0));
  }

  @Test
  public void testColumnComparatorInteger() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.INTEGER, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, 0);
    Row r2 = Row.of(col, 1);

    assertThat(comparator.compare(r1, r2), lessThan(0));
  }

  @Test
  public void testColumnComparatorInterface() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.INTERFACE, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, new NodeInterfacePair("a", "a"));
    Row r2 = Row.of(col, new NodeInterfacePair("a", "b"));
    Row r3 = Row.of(col, new NodeInterfacePair("b", "a"));

    assertThat(comparator.compare(r1, r2), lessThan(0));
    assertThat(comparator.compare(r1, r3), lessThan(0));
    assertThat(comparator.compare(r2, r3), lessThan(0));
  }

  @Test
  public void testColumnComparatorIp() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.IP, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, Ip.ZERO);
    Row r2 = Row.of(col, Ip.MAX);

    assertThat(comparator.compare(r1, r2), lessThan(0));
  }

  @Test
  public void testColumnComparatorIssue() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.ISSUE, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, new Issue("a", 1, new Issue.Type("major", "minor")));
    Row r2 = Row.of(col, new Issue("a", 2, new Issue.Type("major", "minor")));
    Row r3 = Row.of(col, new Issue("b", 1, new Issue.Type("major", "minor")));

    assertThat(comparator.compare(r1, r2), lessThan(0));
    assertThat(comparator.compare(r1, r3), equalTo(0));
    assertThat(comparator.compare(r2, r3), greaterThan(0));
  }

  @Test
  public void testColumnComparatorList() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.list(Schema.STRING), "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, ImmutableList.of());
    Row r2 = Row.of(col, ImmutableList.of("a"));
    Row r3 = Row.of(col, ImmutableList.of("a", "b"));
    Row r4 = Row.of(col, ImmutableList.of("b"));
    Row r5 = Row.of(col, ImmutableList.of("b", "a"));

    assertThat(comparator.compare(r1, r2), lessThan(0));
    assertThat(comparator.compare(r1, r3), lessThan(0));
    assertThat(comparator.compare(r1, r4), lessThan(0));
    assertThat(comparator.compare(r1, r5), lessThan(0));
    assertThat(comparator.compare(r2, r3), lessThan(0));
    assertThat(comparator.compare(r2, r4), lessThan(0));
    assertThat(comparator.compare(r2, r5), lessThan(0));
    assertThat(comparator.compare(r3, r4), lessThan(0));
    assertThat(comparator.compare(r3, r5), lessThan(0));
    assertThat(comparator.compare(r4, r5), lessThan(0));
  }

  @Test
  public void testColumnComparatorLong() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.LONG, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, 0L);
    Row r2 = Row.of(col, 1L);
    Row r3 = Row.of(col, Long.MAX_VALUE);

    assertThat(comparator.compare(r1, r2), lessThan(0));
    assertThat(comparator.compare(r1, r3), lessThan(0));
    assertThat(comparator.compare(r2, r3), lessThan(0));
  }

  @Test
  public void testColumnComparatorNode() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.NODE, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, new Node("a"));
    Row r2 = Row.of(col, new Node("b"));

    assertThat(comparator.compare(r1, r2), lessThan(0));
  }

  @Test
  public void testColumnComparatorPrefix() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.PREFIX, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, Prefix.parse("1.1.2.1/32"));
    Row r2 = Row.of(col, Prefix.parse("1.1.11.1/32"));

    assertThat(comparator.compare(r1, r2), lessThan(0));
  }

  @Test
  public void testColumnComparatorSet() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.set(Schema.STRING), "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r0 = Row.of(col, null);
    Row r1 = Row.of(col, ImmutableSet.of());
    Row r2 = Row.of(col, ImmutableSet.of("a"));
    Row r3 = Row.of(col, ImmutableSet.of("b"));
    Row r4 = Row.of(col, ImmutableSet.of("a", "b"));
    Row r5 = Row.of(col, ImmutableSet.of("b", "a"));
    Row r6 = Row.of(col, Collections.singleton(null));

    assertThat(comparator.compare(r0, r1), lessThan(0));
    assertThat(comparator.compare(r1, r2), lessThan(0));
    assertThat(comparator.compare(r1, r3), lessThan(0));
    assertThat(comparator.compare(r1, r4), lessThan(0));
    assertThat(comparator.compare(r1, r5), lessThan(0));
    assertThat(comparator.compare(r2, r3), lessThan(0));
    assertThat(comparator.compare(r2, r4), not(equalTo(0)));
    assertThat(comparator.compare(r2, r5), not(equalTo(0)));
    assertThat(comparator.compare(r6, r2), lessThan(0));
    assertThat(comparator.compare(r3, r4), not(equalTo(0)));
    assertThat(comparator.compare(r3, r5), not(equalTo(0)));
    // sets in r4 and r5 might end up in same order, so no guarantee on comparison order
  }

  @Test
  public void testColumnComparatorString() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.STRING, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, "a");
    Row r2 = Row.of(col, "b");

    assertThat(comparator.compare(r1, r2), lessThan(0));
  }

  @Test
  public void testColumnComparatorSelfDescribingObject() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.SELF_DESCRIBING, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, new SelfDescribingObject(Schema.STRING, "a"));
    Row r2 = Row.of(col, new SelfDescribingObject(Schema.STRING, "b"));

    assertThat(comparator.compare(r1, r2), lessThan(0));
  }

  @Test
  public void testColumnComparatorTrace() {
    String col = "col1";
    ColumnMetadata columnMetadata = new ColumnMetadata(col, Schema.TRACE, "colDesc");
    Comparator<Row> comparator = _manager.columnComparator(columnMetadata);
    Row r1 = Row.of(col, new Trace(FlowDisposition.ACCEPTED, ImmutableList.of()));
    Row r2 =
        Row.of(
            col,
            new Trace(
                FlowDisposition.ACCEPTED,
                ImmutableList.of(new Hop(new Node("a"), ImmutableList.of()))));
    Row r3 = Row.of(col, new Trace(FlowDisposition.DELIVERED_TO_SUBNET, ImmutableList.of()));

    assertThat(comparator.compare(r1, r2), lessThan(0));
    assertThat(comparator.compare(r1, r3), lessThan(0));
    assertThat(comparator.compare(r2, r3), lessThan(0));
  }

  @Test
  public void testApplyIssuesConfiguration() throws IOException {
    String network = "network1";
    NetworkId networkId = new NetworkId(network);
    SnapshotId snapshotId = new SnapshotId("snapshot1");
    String question = "question2Name";
    QuestionId questionId = new QuestionId(question);
    SnapshotId referenceSnapshotId = null;
    AnalysisId analysisId = null;
    Answer oldAnswer = new Answer();
    String col = "Issue";
    String major = "maj";
    String minor = "min";
    int newSeverity = 6;
    oldAnswer.setStatus(AnswerStatus.SUCCESS);
    TableAnswerElement oldTable =
        new TableAnswerElement(
            new TableMetadata(ImmutableList.of(new ColumnMetadata(col, Schema.ISSUE, "desc"))));
    oldTable.addRow(Row.of(col, new Issue("blah", 1, new Issue.Type(major, minor))));
    oldTable.addExcludedRow(
        Row.of(col, new Issue("blorp", 1, new Issue.Type(major, minor))), "exc");
    oldAnswer.setAnswerElements(ImmutableList.of(oldTable));
    AnswerMetadata oldAnswerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(oldAnswer, _manager.getLogger());
    _manager.initNetwork(network, null);
    Question testQuestion = new TestQuestion();
    testQuestion.setExclusions(
        ImmutableList.of(
            new Exclusion(
                "exc",
                BatfishObjectMapper.mapper()
                    .valueToTree(
                        Row.of(col, new Issue("blorp", 1, new Issue.Type(major, minor)))))));
    String questionContent = BatfishObjectMapper.writeString(testQuestion);
    _storage.storeQuestion(questionContent, networkId, questionId, analysisId);
    _idManager.assignQuestion(question, networkId, questionId, null);
    AnswerId answerId =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            null);
    _storage.storeAnswer(BatfishObjectMapper.writeString(oldAnswer), answerId);
    _storage.storeAnswerMetadata(oldAnswerMetadata, answerId);
    Map<String, MajorIssueConfig> majorIssueConfigs =
        ImmutableMap.of(
            major,
            new MajorIssueConfig(
                major,
                ImmutableList.of(new MinorIssueConfig(minor, newSeverity, "http://example.com"))));
    AnswerId finalAnswerId = new AnswerId("finalAnswerId");
    _manager.applyIssuesConfiguration(
        majorIssueConfigs,
        networkId,
        snapshotId,
        questionId,
        answerId,
        finalAnswerId,
        referenceSnapshotId,
        analysisId);
    AnswerMetadata newAnswerMetadata = _storage.loadAnswerMetadata(finalAnswerId);

    // The answer metadata now on disk should have changed
    assertThat(newAnswerMetadata, not(equalTo(oldAnswerMetadata)));

    Answer newAnswer =
        BatfishObjectMapper.mapper()
            .readValue(_storage.loadAnswer(finalAnswerId), new TypeReference<Answer>() {});
    TableAnswerElement newTable = (TableAnswerElement) newAnswer.getAnswerElements().get(0);

    // The answer's rows should have changed.
    assertThat(newTable.getRowsList(), not(equalTo(oldTable.getRowsList())));

    // The answer should no longer have excluded rows.
    assertThat(newTable.getExcludedRows(), emptyIterable());
  }

  @Test
  public void testApplyRowIssuesConfigurationNonIssue() {
    String col = "col";
    Row oldRow = Row.of(col, "foo");
    Set<String> issueColumns = ImmutableSet.of();
    Map<String, MajorIssueConfig> issueConfigs = ImmutableMap.of();

    assertThat(
        _manager.applyRowIssuesConfiguration(oldRow, issueColumns, issueConfigs), equalTo(oldRow));
  }

  @Test
  public void testApplyRowIssuesConfigurationNoMatchMajorIssue() {
    String col = "col";
    String major = "major";
    String otherMajor = "otherMajor";
    String minor = "minor";
    Row oldRow = Row.of(col, new Issue("explanation", 5, new Issue.Type(major, minor)));
    Set<String> issueColumns = ImmutableSet.of(col);
    Map<String, MajorIssueConfig> issueConfigs =
        ImmutableMap.of(
            otherMajor,
            new MajorIssueConfig(
                otherMajor, ImmutableList.of(new MinorIssueConfig(minor, 6, "example.com"))));

    assertThat(
        _manager.applyRowIssuesConfiguration(oldRow, issueColumns, issueConfigs), equalTo(oldRow));
  }

  @Test
  public void testApplyRowIssuesConfigurationNoMatchMinorIssue() {
    String col = "col";
    String major = "major";
    String otherMinor = "otherMinor";
    String minor = "minor";
    Row oldRow = Row.of(col, new Issue("explanation", 5, new Issue.Type(major, minor)));
    Set<String> issueColumns = ImmutableSet.of(col);
    Map<String, MajorIssueConfig> issueConfigs =
        ImmutableMap.of(
            major,
            new MajorIssueConfig(
                major, ImmutableList.of(new MinorIssueConfig(otherMinor, 6, "example.com"))));

    assertThat(
        _manager.applyRowIssuesConfiguration(oldRow, issueColumns, issueConfigs), equalTo(oldRow));
  }

  @Test
  public void testApplyRowIssuesConfigurationMatch() {
    String col = "col";
    String major = "major";
    String minor = "minor";
    Issue oldIssue = new Issue("explanation", 5, new Issue.Type(major, minor));
    Row oldRow = Row.of(col, oldIssue);
    int newSeverity = 6;
    String newUrl = "example.com";
    Issue newIssue = new Issue(oldIssue.getExplanation(), newSeverity, oldIssue.getType(), newUrl);
    Set<String> issueColumns = ImmutableSet.of(col);
    Map<String, MajorIssueConfig> issueConfigs =
        ImmutableMap.of(
            major,
            new MajorIssueConfig(major, ImmutableList.of(new MinorIssueConfig(minor, 6, newUrl))));

    assertThat(
        _manager.applyRowIssuesConfiguration(oldRow, issueColumns, issueConfigs),
        equalTo(Row.of(col, newIssue)));
  }

  @Test
  public void testAnswerIssueConfigMatchesConfiguredIssuesMissingMinorMatches() {
    String major = "major";
    String minor = "minor";
    MajorIssueConfig answerIssueConfig =
        new MajorIssueConfig(major, ImmutableMap.of(minor, new MinorIssueConfig(minor, 1, null)));
    Map<String, MajorIssueConfig> configuredMajorIssues =
        ImmutableMap.of(major, new MajorIssueConfig(major, ImmutableMap.of()));

    assertThat(
        _manager.answerIssueConfigMatchesConfiguredIssues(answerIssueConfig, configuredMajorIssues),
        equalTo(true));
  }

  @Test
  public void testAnswerIssueConfigMatchesConfiguredIssuesDifferntMinorNoMatch() {
    String major = "major";
    String minor = "minor";
    MajorIssueConfig answerIssueConfig =
        new MajorIssueConfig(major, ImmutableMap.of(minor, new MinorIssueConfig(minor, 1, null)));
    Map<String, MajorIssueConfig> configuredMajorIssues =
        ImmutableMap.of(
            major,
            new MajorIssueConfig(
                major, ImmutableMap.of(minor, new MinorIssueConfig(minor, 2, null))));

    assertThat(
        _manager.answerIssueConfigMatchesConfiguredIssues(answerIssueConfig, configuredMajorIssues),
        equalTo(false));
  }

  @Test
  public void testDeleteSnapshotAbsent() throws IOException {
    String network = "network1";
    String snapshot = "snapshot1";
    _manager.initNetwork(network, null);

    // should not be able to delete non-existent snapshot
    assertFalse(_manager.delSnapshot(network, snapshot));
  }

  @Test
  public void testDeleteSnapshotPresent() throws IOException {
    String network = "network1";
    String snapshot = "snapshot1";
    _manager.initNetwork(network, null);
    uploadTestSnapshot(network, snapshot);

    // snapshot should exist
    assertThat(_manager.listSnapshots(network), contains(snapshot));

    _manager.delSnapshot(network, snapshot);

    // snapshot should no longer exist
    assertThat(_manager.listSnapshots(network), emptyIterable());
  }

  @Test
  public void testDeleteNetworkAbsent() throws IOException {
    String network = "network1";

    // deletion should do no work
    assertFalse(_manager.delNetwork(network));
  }

  @Test
  public void testDeleteNetworkPresent() throws IOException {
    String network = "network1";
    _manager.initNetwork(network, null);

    // network should exist
    assertThat(_manager.getNetworkNames(), contains(network));

    // deletion should be successful
    assertTrue(_manager.delNetwork(network));

    // network  should not exist
    assertThat(_manager.getNetworkNames(), emptyIterable());
  }

  @Test
  public void testGetNetworkNames() {
    String network = "network1";

    // should start out empty
    assertThat(_manager.getNetworkNames(), emptyIterable());

    _manager.initNetwork(network, null);

    // should contain initialized network
    assertThat(_manager.getNetworkNames(), contains(network));
  }

  @Test
  public void testGetNetworkNodeRolesEmpty() throws IOException {
    String network = "network1";
    _manager.initNetwork(network, null);

    // should return empty node roles
    assertThat(_manager.getNetworkNodeRoles(network), equalTo(NodeRolesData.builder().build()));
  }

  @Test
  public void testGetNetworkNodeRolesPresent() throws IOException {
    String network = "network1";
    String node = "node1";
    _manager.initNetwork(network, null);
    NodeRolesId networkNodeRolesId = new NodeRolesId("nr");
    NodeRolesData networkNodeRoles =
        NodeRolesData.builder()
            .setRoleDimensions(
                ImmutableSortedSet.of(
                    NodeRoleDimension.builder()
                        .setName("dim1")
                        .setRoles(ImmutableSet.of(new NodeRole("role1", node)))
                        .build()))
            .build();
    _manager.getStorage().storeNodeRoles(networkNodeRoles, networkNodeRolesId);
    _idManager.assignNetworkNodeRolesId(_idManager.getNetworkId(network), networkNodeRolesId);

    // inferred roles for first snapshot should have been set network-wide
    assertThat(_manager.getNetworkNodeRoles(network), equalTo(networkNodeRoles));
  }

  @Test
  public void testUploadSnapshot() throws IOException {
    String network = "network1";
    String snapshot = "snapshot1";
    _manager.initNetwork(network, null);
    uploadTestSnapshot(network, snapshot);

    // snapshot should exist
    assertThat(_manager.listSnapshots(network), contains(snapshot));

    // should be able to delete and recreate
    _manager.delSnapshot(network, snapshot);
    uploadTestSnapshot(network, snapshot);

    // snapshot should exist again
    assertThat(_manager.listSnapshots(network), contains(snapshot));

    // should not be able to upload again with same name
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(containsString(snapshot));
    uploadTestSnapshot(network, snapshot);
  }

  @Test
  public void testWriteQuestionSettings() throws IOException {
    String network = "network1";
    String questionClassId = "foo";
    _manager.initNetwork(network, null);
    NetworkId networkId = _idManager.getNetworkId(network);

    // no QuestionSettingsId for questionClassId at first
    assertFalse(_idManager.hasQuestionSettingsId(questionClassId, networkId));

    _manager.writeQuestionSettings(
        network, questionClassId, ImmutableList.of(), BatfishObjectMapper.mapper().readTree("{}"));

    // Should have QuestionSettingsId now
    assertTrue(_idManager.hasQuestionSettingsId(questionClassId, networkId));

    QuestionSettingsId questionSettingsId =
        _idManager.getQuestionSettingsId(questionClassId, networkId);
    _manager.writeQuestionSettings(
        network, questionClassId, ImmutableList.of(), BatfishObjectMapper.mapper().readTree("{}"));

    // QuestionSettingsId should change after subsequent write
    assertThat(
        _idManager.getQuestionSettingsId(questionClassId, networkId),
        not(equalTo(questionSettingsId)));
  }

  private void uploadTestSnapshot(String network, String snapshot) throws IOException {
    WorkMgrTestUtils.uploadTestSnapshot(network, snapshot, _folder);
  }

  private void uploadTestSnapshot(String network, String snapshot, String fileName)
      throws IOException {
    WorkMgrTestUtils.uploadTestSnapshot(network, snapshot, fileName, _folder);
  }

  private void uploadTestSnapshot(String network, String snapshot, String fileName, String content)
      throws IOException {
    WorkMgrTestUtils.uploadTestSnapshot(network, snapshot, fileName, content, _folder);
  }

  @Test
  public void testPutMajorIssueConfig() throws IOException {
    String network = "network1";
    String majorIssueType = "type1";
    _manager.initNetwork(network, null);
    NetworkId networkId = _idManager.getNetworkId(network);

    // no ID should exist at first
    assertFalse(_idManager.hasIssueSettingsId(majorIssueType, networkId));

    _manager.putMajorIssueConfig(
        network, majorIssueType, new MajorIssueConfig(majorIssueType, ImmutableList.of()));

    // There should be an ID now
    assertTrue(_idManager.hasIssueSettingsId(majorIssueType, networkId));

    IssueSettingsId issueSettingsId = _idManager.getIssueSettingsId(majorIssueType, networkId);

    _manager.putMajorIssueConfig(
        network,
        majorIssueType,
        new MajorIssueConfig(
            majorIssueType, ImmutableList.of(new MinorIssueConfig("foo", null, null))));

    // The ID should have changed
    assertThat(
        _idManager.getIssueSettingsId(majorIssueType, networkId), not(equalTo(issueSettingsId)));
  }

  @Test
  public void testComputeWorkDetailsAnalysisQuestion() throws IOException {
    String network = "network1";
    String snapshot = "snapshot1";
    String question = "question1";
    String analysis = "analysis1";
    _manager.initNetwork(network, null);
    uploadTestSnapshot(network, snapshot);
    _manager.configureAnalysis(
        network,
        true,
        analysis,
        ImmutableMap.of(question, BatfishObjectMapper.writeString(new TestQuestion())),
        ImmutableList.of(),
        false);
    WorkItem workItem =
        new WorkItem(
            UUID.randomUUID(),
            network,
            snapshot,
            ImmutableMap.of(
                BfConsts.COMMAND_ANSWER,
                "",
                BfConsts.ARG_QUESTION_NAME,
                question,
                BfConsts.ARG_ANALYSIS_NAME,
                analysis));
    WorkDetails workDetails = _manager.computeWorkDetails(workItem);

    assertThat(
        workDetails.getSnapshotId(),
        equalTo(_idManager.getSnapshotId(snapshot, _idManager.getNetworkId(network))));
    assertThat(workDetails.getWorkType(), equalTo(WorkType.PARSING_DEPENDENT_ANSWERING));
  }

  @Test
  public void testComputeWorkDetailsAdHocQuestion() throws IOException {
    String network = "network1";
    String snapshot = "snapshot1";
    String question = "question1";
    _manager.initNetwork(network, null);
    uploadTestSnapshot(network, snapshot);
    _manager.uploadQuestion(network, question, BatfishObjectMapper.writeString(new TestQuestion()));
    WorkItem workItem =
        new WorkItem(
            UUID.randomUUID(),
            network,
            snapshot,
            ImmutableMap.of(BfConsts.COMMAND_ANSWER, "", BfConsts.ARG_QUESTION_NAME, question));
    WorkDetails workDetails = _manager.computeWorkDetails(workItem);

    assertThat(
        workDetails.getSnapshotId(),
        equalTo(_idManager.getSnapshotId(snapshot, _idManager.getNetworkId(network))));
    assertThat(workDetails.getWorkType(), equalTo(WorkType.PARSING_DEPENDENT_ANSWERING));
  }

  @Test
  public void testGetAnswerNotFoundAfterNodeRolesUpdate() throws IOException {
    String networkName = "network1";
    String snapshotName = "snapshot1";
    Question question = new TestQuestion();
    String questionContent = BatfishObjectMapper.writeString(question);
    String questionName = "question2Name";
    _manager.initNetwork(networkName, null);
    _manager.uploadQuestion(networkName, questionName, questionContent, false);
    NetworkId networkId = _idManager.getNetworkId(networkName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();
    _idManager.assignSnapshot(snapshotName, networkId, snapshotId);
    QuestionId questionId = _idManager.getQuestionId(questionName, networkId, null);
    AnswerId baseAnswerId =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId,
            DEFAULT_QUESTION_SETTINGS_ID,
            DEFAULT_NETWORK_NODE_ROLES_ID,
            null,
            null);
    Answer answer = new Answer();
    answer.setStatus(AnswerStatus.SUCCESS);
    answer.addAnswerElement(new TableAnswerElement(MOCK_TABLE_METADATA));
    AnswerMetadata answerMetadata =
        AnswerMetadataUtil.computeAnswerMetadata(answer, Main.getLogger());
    _storage.storeAnswerMetadata(answerMetadata, baseAnswerId);
    String answerStr = BatfishObjectMapper.writeString(answer);
    _storage.storeAnswer(answerStr, baseAnswerId);
    Answer answerBeforeUpdate =
        BatfishObjectMapper.mapper()
            .readValue(
                _manager.getAnswer(networkName, snapshotName, questionName, null, null),
                Answer.class);

    // answer should be found at first
    assertThat(answerBeforeUpdate.getStatus(), equalTo(AnswerStatus.SUCCESS));

    boolean updated =
        _manager.putNetworkNodeRoles(
            NodeRolesData.builder()
                .setRoleDimensions(
                    ImmutableSortedSet.of(NodeRoleDimension.builder().setName("foo").build()))
                .build(),
            networkName);

    // updating node roles should succeed
    assertTrue(updated);

    Answer answerAfterUpdate =
        BatfishObjectMapper.mapper()
            .readValue(
                _manager.getAnswer(networkName, snapshotName, questionName, null, null),
                Answer.class);

    // answer should no longer be available since node roles input id changed
    assertThat(answerAfterUpdate.getStatus(), equalTo(AnswerStatus.NOTFOUND));
  }

  @Test
  public void testListSnapshotsWithMetadataMissingNetwork() throws IOException {
    String network = "network1";

    assertThat(Main.getWorkMgr().listSnapshotsWithMetadata(network), nullValue());
  }

  @Test
  public void testListSnapshotsWithMetadataSuccess() throws IOException {
    String network = "network1";
    String snapshot = "snapshot1";
    Main.getWorkMgr().initNetwork(network, null);
    WorkMgrTestUtils.uploadTestSnapshot(network, snapshot, _folder);

    assertThat(
        Main.getWorkMgr().listSnapshotsWithMetadata(network),
        equalTo(
            ImmutableList.of(
                new SnapshotMetadataEntry(
                    snapshot, Main.getWorkMgr().getSnapshotMetadata(network, snapshot)))));
  }

  @Test
  public void testGetSnapshotSubdirInvalid() {
    Path root = _folder.getRoot().toPath();
    Path s1Path = root.resolve("s1");
    s1Path.toFile().mkdirs();
    Path s2Path = root.resolve("s2");
    s2Path.toFile().mkdirs();
    CommonUtil.writeFile(s1Path.resolve("file1"), "content");
    CommonUtil.writeFile(s2Path.resolve("file2"), "content");

    // invalid because there are two top-level dirs
    _thrown.expect(BatfishException.class);
    WorkMgr.getSnapshotSubdir(root);
  }

  @Test
  public void testGetSnapshotSubdirValid() {
    Path root = _folder.getRoot().toPath();
    Path s1Path = root.resolve("s1");
    s1Path.toFile().mkdirs();
    Path s2Path = root.resolve("__MACOSX"); // ignored dir
    s2Path.toFile().mkdirs();
    CommonUtil.writeFile(s1Path.resolve("file1"), "content");
    CommonUtil.writeFile(s2Path.resolve("file2"), "content");

    // extra dir should be ignored, and s1Path should be considered the snapshot subdir
    assertThat(WorkMgr.getSnapshotSubdir(root), equalTo(s1Path));
  }

  @Test
  public void testGetSnapshotInputKeysInvalidNetwork() throws IOException {
    assertThat(_manager.getSnapshotInputObjectsMetadata("network", "snapshot"), nullValue());
  }

  @Test
  public void testGetSnapshotInputKeysInvalidSnapshot() throws IOException {
    String network = "network";
    NetworkId networkId = _idManager.generateNetworkId();
    _idManager.assignNetwork(network, networkId);
    assertThat(_manager.getSnapshotInputObjectsMetadata(network, "snapshot"), nullValue());
  }

  @Test
  public void testGetSnapshotInputKeysIOException() throws IOException {
    String network = "network";
    String snapshot = "snapshot";

    NetworkId networkId = _idManager.generateNetworkId();
    SnapshotId snapshotId = _idManager.generateSnapshotId();

    _idManager.assignNetwork(network, networkId);
    _idManager.assignSnapshot(snapshot, networkId, snapshotId);

    _thrown.expect(IOException.class);
    _manager.getSnapshotInputObjectsMetadata(network, snapshot);
  }

  @Test
  public void testGetSnapshotInputKeys() throws IOException {
    String network = "network";
    String snapshot = "snapshot";
    String fileName = "fileName";
    String content = "some content";

    _manager.initNetwork(network, null);
    uploadTestSnapshot(network, snapshot, fileName, content);

    assertThat(
        _manager.getSnapshotInputObjectsMetadata(network, snapshot),
        equalTo(
            ImmutableList.of(
                new StoredObjectMetadata("configs/" + fileName, content.getBytes().length))));
  }

  @Test
  public void testGetSnapshotExtendedObjectsMetadataInvalidNetwork() throws IOException {
    assertThat(_manager.getSnapshotExtendedObjectsMetadata("network", "snapshot"), nullValue());
  }

  @Test
  public void testGetSnapshotExtendedObjectsMetadataInvalidSnapshot() throws IOException {
    String network = "network";
    NetworkId networkId = _idManager.generateNetworkId();
    _idManager.assignNetwork(network, networkId);
    assertThat(_manager.getSnapshotExtendedObjectsMetadata(network, "snapshot"), nullValue());
  }

  @Test
  public void testGetSnapshotExtendedObjectsMetadataIOException() throws IOException {
    String network = "network";
    String snapshot = "snapshot";

    NetworkId networkId = _idManager.generateNetworkId();
    SnapshotId snapshotId = _idManager.generateSnapshotId();

    _idManager.assignNetwork(network, networkId);
    _idManager.assignSnapshot(snapshot, networkId, snapshotId);

    _thrown.expect(IOException.class);
    _manager.getSnapshotExtendedObjectsMetadata(network, snapshot);
  }

  @Test
  public void testGetSnapshotExtendedObjectsMetadata() throws IOException {
    String network = "network";
    String snapshot = "snapshot";
    String fileName = "fileName";
    String content = "some content";

    _manager.initNetwork(network, null);
    uploadTestSnapshot(network, snapshot);

    InputStream inputStream = new ByteArrayInputStream(content.getBytes());
    _manager.putSnapshotExtendedObject(inputStream, network, snapshot, fileName);

    assertThat(
        _manager.getSnapshotExtendedObjectsMetadata(network, snapshot),
        contains(new StoredObjectMetadata(fileName, content.getBytes().length)));
  }
}
