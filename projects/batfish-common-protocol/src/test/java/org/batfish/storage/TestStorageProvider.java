package org.batfish.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.datamodel.AnalysisMetadata;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.SnapshotMetadata;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.MajorIssueConfig;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.identifiers.AnalysisId;
import org.batfish.identifiers.AnswerId;
import org.batfish.identifiers.IssueSettingsId;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.NodeRolesId;
import org.batfish.identifiers.QuestionId;
import org.batfish.identifiers.QuestionSettingsId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.role.NodeRolesData;

public class TestStorageProvider implements StorageProvider {
  @Override
  public SortedMap<String, Configuration> loadCompressedConfigurations(
      NetworkId network, SnapshotId snapshot) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public SortedMap<String, Configuration> loadConfigurations(
      NetworkId network, SnapshotId snapshot) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElement(
      NetworkId network, SnapshotId snapshot) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Nullable
  @Override
  public SortedSet<Edge> loadEdgeBlacklist(NetworkId network, SnapshotId snapshot) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Nullable
  @Override
  public SortedSet<NodeInterfacePair> loadInterfaceBlacklist(
      NetworkId network, SnapshotId snapshot) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Nullable
  @Override
  public SortedSet<String> loadNodeBlacklist(NetworkId network, SnapshotId snapshot) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public Topology loadLegacyTopology(NetworkId network, SnapshotId snapshot) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public Layer1Topology loadLayer1Topology(NetworkId network, SnapshotId snapshot) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public MajorIssueConfig loadMajorIssueConfig(NetworkId network, IssueSettingsId majorIssueType) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Nonnull
  @Override
  public String loadWorkLog(NetworkId network, SnapshotId snapshot, String workId) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void storeMajorIssueConfig(
      NetworkId network, IssueSettingsId majorIssueType, MajorIssueConfig majorIssueConfig) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void storeCompressedConfigurations(
      Map<String, Configuration> configurations, NetworkId network, SnapshotId snapshot) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void storeConfigurations(
      Map<String, Configuration> configurations,
      ConvertConfigurationAnswerElement convertAnswerElement,
      NetworkId network,
      SnapshotId snapshot) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public String loadQuestion(NetworkId network, QuestionId analysis, AnalysisId question) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public boolean checkQuestionExists(NetworkId network, QuestionId question, AnalysisId analysis) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void storeQuestion(
      String questionStr, NetworkId network, QuestionId question, AnalysisId analysis) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public String loadQuestionSettings(NetworkId network, QuestionSettingsId questionSettingsId)
      throws IOException {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public boolean checkNetworkExists(NetworkId network) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void storeQuestionSettings(
      String settings, NetworkId network, QuestionSettingsId questionSettingsId)
      throws IOException {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void storeAnswer(String answerStr, AnswerId answerId) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void storeAnswerMetadata(AnswerMetadata answerMetadata, AnswerId answerId) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public String loadAnswer(AnswerId answerId) throws FileNotFoundException, IOException {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public AnswerMetadata loadAnswerMetadata(AnswerId answerId)
      throws FileNotFoundException, IOException {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public boolean hasAnswerMetadata(AnswerId answerId) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public String loadQuestionClassId(
      NetworkId networkId, QuestionId questionId, AnalysisId analysisId) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public boolean hasAnalysisMetadata(NetworkId networkId, AnalysisId analysisId) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void storeAnalysisMetadata(
      AnalysisMetadata analysisMetadata, NetworkId networkId, AnalysisId analysisId)
      throws IOException {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public String loadAnalysisMetadata(NetworkId networkId, AnalysisId analysisId)
      throws FileNotFoundException, IOException {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void storeSnapshotMetadata(
      SnapshotMetadata snapshotMetadata, NetworkId networkId, SnapshotId snapshotId) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public String loadSnapshotMetadata(NetworkId networkId, SnapshotId snapshotId)
      throws FileNotFoundException, IOException {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void storeNodeRoles(NodeRolesData nodeRolesData, NodeRolesId nodeRolesId) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public String loadNodeRoles(NodeRolesId nodeRolesId) throws FileNotFoundException, IOException {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public boolean hasNodeRoles(NodeRolesId nodeRolesId) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public void initNetwork(NetworkId networkId) {}

  @Override
  public void deleteAnswerMetadata(AnswerId answerId) throws FileNotFoundException, IOException {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public InputStream loadNetworkObject(NetworkId networkId, String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void storeNetworkObject(InputStream inputStream, NetworkId networkId, String key)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteNetworkObject(NetworkId networkId, String key) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream loadSnapshotObject(NetworkId networkId, SnapshotId snapshotId, String key)
      throws FileNotFoundException, IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void storeSnapshotObject(
      InputStream inputStream, NetworkId networkId, SnapshotId snapshotId, String key)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteSnapshotObject(NetworkId networkId, SnapshotId snapshotId, String key)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream loadSnapshotInputObject(NetworkId networkId, SnapshotId snapshotId, String key)
      throws FileNotFoundException, IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String loadPojoTopology(NetworkId networkId, SnapshotId snapshotId) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String loadTopology(NetworkId networkId, SnapshotId snapshotId) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void storeTopology(Topology topology, NetworkId networkId, SnapshotId snapshotId)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void storePojoTopology(
      org.batfish.datamodel.pojo.Topology topology, NetworkId networkId, SnapshotId snapshotId)
      throws IOException {
    throw new UnsupportedOperationException();
  }
}
