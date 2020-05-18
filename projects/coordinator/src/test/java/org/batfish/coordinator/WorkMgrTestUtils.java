package org.batfish.coordinator;

import static org.batfish.identifiers.NodeRolesId.DEFAULT_NETWORK_NODE_ROLES_ID;
import static org.batfish.identifiers.QuestionSettingsId.DEFAULT_QUESTION_SETTINGS_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.ZipUtility;
import org.batfish.coordinator.id.IdManager;
import org.batfish.coordinator.id.StorageBasedIdManager;
import org.batfish.datamodel.SnapshotMetadata;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.AnswerMetadataUtil;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.pojo.Topology;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.TestQuestion;
import org.batfish.identifiers.AnalysisId;
import org.batfish.identifiers.AnswerId;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.NodeRolesId;
import org.batfish.identifiers.QuestionId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.role.NodeRolesData;
import org.batfish.storage.FileBasedStorage;
import org.batfish.storage.StorageProvider;
import org.junit.rules.TemporaryFolder;

public final class WorkMgrTestUtils {

  private WorkMgrTestUtils() {}

  public static void initWorkManager(TemporaryFolder folder) {
    BatfishLogger logger = new BatfishLogger("debug", false);
    Main.mainInit(new String[] {"-containerslocation", folder.getRoot().toString()});
    Main.setLogger(logger);
    Main.initAuthorizer();
    FileBasedStorage fbs = new FileBasedStorage(Main.getSettings().getContainersLocation(), logger);
    WorkMgr workMgr = new WorkMgr(Main.getSettings(), logger, new StorageBasedIdManager(fbs), fbs);
    // Setup some test version data
    Main.setWorkMgr(workMgr);
  }

  public static void initSnapshotWithTopology(String network, String snapshot, Set<String> nodes)
      throws IOException {
    IdManager idManager = Main.getWorkMgr().getIdManager();
    SnapshotMetadataMgr ssmManager = Main.getWorkMgr().getSnapshotMetadataManager();
    NetworkId networkId = idManager.getNetworkId(network);
    SnapshotId snapshotId =
        idManager.hasSnapshotId(snapshot, networkId)
            ? idManager.getSnapshotId(snapshot, networkId)
            : idManager.generateSnapshotId();
    idManager.assignSnapshot(snapshot, networkId, snapshotId);
    ssmManager.writeMetadata(
        new SnapshotMetadata(new Date().toInstant(), null), networkId, snapshotId);
    Topology pojoTopology = new Topology(snapshot);
    pojoTopology.setNodes(nodes.stream().map(Node::new).collect(Collectors.toSet()));
    Main.getWorkMgr().getStorage().storePojoTopology(pojoTopology, networkId, snapshotId);
    org.batfish.datamodel.Topology envTopology =
        new org.batfish.datamodel.Topology(ImmutableSortedSet.of());
    Main.getWorkMgr().getStorage().storeInitialTopology(envTopology, networkId, snapshotId);
  }

  public static void setSnapshotNodeRoles(
      NodeRolesData nodeRolesData, String network, String snapshot) throws IOException {
    IdManager idManager = Main.getWorkMgr().getIdManager();
    NetworkId networkId = idManager.getNetworkId(network);
    SnapshotId snapshotId = idManager.getSnapshotId(snapshot, networkId);
    NodeRolesId snapshotNodeRolesId = idManager.getSnapshotNodeRolesId(networkId, snapshotId);
    Main.getWorkMgr().getStorage().storeNodeRoles(nodeRolesData, snapshotNodeRolesId);
  }

  public static void initWorkManager(IdManager idManager, StorageProvider storage) {
    BatfishLogger logger = new BatfishLogger("debug", false);
    Main.mainInit(new String[] {});
    Main.setLogger(logger);
    Main.initAuthorizer();
    Main.setWorkMgr(new WorkMgr(Main.getSettings(), logger, idManager, storage));
  }

  public static void uploadTestSnapshot(String network, String snapshot, TemporaryFolder folder)
      throws IOException {
    uploadTestSnapshot(network, snapshot, "c1", folder);
  }

  public static void uploadTestSnapshot(
      String network, String snapshot, String fileName, TemporaryFolder folder) throws IOException {
    uploadTestSnapshot(network, snapshot, fileName, "content", folder);
  }

  public static void uploadTestSnapshot(
      String network, String snapshot, String fileName, String content, TemporaryFolder folder)
      throws IOException {
    Path tmpSnapshotZip = createSnapshotZip(snapshot, fileName, content, folder);
    try (InputStream inputStream = Files.newInputStream(tmpSnapshotZip)) {
      Main.getWorkMgr().uploadSnapshot(network, snapshot, inputStream, false);
    }
  }

  /** Creates and returns path to a snapshot dir with the specified config */
  public static Path createSnapshot(
      String snapshot, String fileName, String content, TemporaryFolder folder) {
    Path tmpSnapshotSrcDir = folder.getRoot().toPath().resolve(snapshot);
    // intentional duplication of snapshot to provide subdir
    Path tmpSnapshotConfig =
        tmpSnapshotSrcDir
            .resolve(snapshot)
            .resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR)
            .resolve(fileName);
    tmpSnapshotConfig.getParent().toFile().mkdirs();
    try {
      Files.write(tmpSnapshotConfig, content.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return tmpSnapshotSrcDir;
  }

  /** Creates a snapshot zip with the specified config and returns the path to that zip */
  public static Path createSnapshotZip(
      String snapshot, String fileName, String content, TemporaryFolder folder) {
    Path tmpSnapshotSrcDir = createSnapshot(snapshot, fileName, content, folder);

    Path tmpSnapshotZip = tmpSnapshotSrcDir.resolve(String.format("%s.zip", snapshot));
    ZipUtility.zipFiles(tmpSnapshotSrcDir.resolve(snapshot), tmpSnapshotZip);
    return tmpSnapshotZip;
  }

  public static void setupQuestionAndAnswer(
      String network,
      String snapshot,
      String questionName,
      @Nullable String analysis,
      @Nullable Answer answer)
      throws IOException {
    setupQuestionAndAnswer(network, snapshot, questionName, analysis, answer, null);
  }

  /** Setup question and optionally answer on specified network and snapshot */
  public static void setupQuestionAndAnswer(
      String network,
      String snapshot,
      String questionName,
      @Nullable String analysis,
      @Nullable Answer answer,
      @Nullable String referenceSnapshot)
      throws IOException {
    WorkMgr manager = Main.getWorkMgr();
    IdManager idManager = manager.getIdManager();
    StorageProvider storage = manager.getStorage();
    Question question = new TestQuestion();
    NetworkId networkId = idManager.getNetworkId(network);
    SnapshotId snapshotId = idManager.getSnapshotId(snapshot, networkId);
    AnalysisId analysisId = null;

    // Setup question
    if (analysis != null) {
      manager.configureAnalysis(
          network,
          true,
          analysis,
          ImmutableMap.of(questionName, BatfishObjectMapper.writeString(question)),
          Lists.newArrayList(),
          null);
      analysisId = idManager.getAnalysisId(analysis, networkId);
    } else {
      idManager.assignQuestion(questionName, networkId, idManager.generateQuestionId(), null);
    }
    QuestionId questionId = idManager.getQuestionId(questionName, networkId, analysisId);
    storage.storeQuestion(
        BatfishObjectMapper.writeString(question), networkId, questionId, analysisId);

    // Setup answer iff one was passed in
    if (answer != null) {
      SnapshotId referenceSnapshotId =
          referenceSnapshot == null ? null : idManager.getSnapshotId(referenceSnapshot, networkId);
      AnswerId answerId =
          idManager.getBaseAnswerId(
              networkId,
              snapshotId,
              questionId,
              DEFAULT_QUESTION_SETTINGS_ID,
              DEFAULT_NETWORK_NODE_ROLES_ID,
              referenceSnapshotId,
              analysisId);
      String answerStr = BatfishObjectMapper.writeString(answer);
      AnswerMetadata answerMetadata =
          AnswerMetadataUtil.computeAnswerMetadata(answer, Main.getLogger());
      storage.storeAnswer(answerStr, answerId);
      storage.storeAnswerMetadata(answerMetadata, answerId);
    }
  }
}
