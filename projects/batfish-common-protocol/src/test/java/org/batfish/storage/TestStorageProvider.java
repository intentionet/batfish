package org.batfish.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nonnull;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.MajorIssueConfig;

public class TestStorageProvider implements StorageProvider {

  @Override
  public SortedMap<String, Configuration> loadCompressedConfigurations(
      String network, String snapshot) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public SortedMap<String, Configuration> loadConfigurations(String network, String snapshot) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElement(
      String network, String snapshot) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public Topology loadLegacyTopology(String network, String snapshot) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public Layer1Topology loadLayer1Topology(String network, String snapshot) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public MajorIssueConfig loadMajorIssueConfig(String network, String majorIssueType) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void storeMajorIssueConfig(
      String network, String majorIssueType, MajorIssueConfig majorIssueConfig) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void storeCompressedConfigurations(
      Map<String, Configuration> configurations, String network, String snapshot) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void storeConfigurations(
      Map<String, Configuration> configurations,
      ConvertConfigurationAnswerElement convertAnswerElement,
      String network,
      String snapshot) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void storeAnswer(
      String answerStr,
      String network,
      String baseSnapshotName,
      String deltaSnapshotName,
      String analysisName,
      String questionName) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void storeAnswerMetadata(
      AnswerMetadata answerMetrics,
      String network,
      String analysisName,
      String baseSnapshotName,
      String deltaSnapshotName,
      String questionName) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public String loadQuestion(String network, String analysis, String question) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public List<String> listAnalysisQuestions(String network, String analysis) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public boolean checkQuestionExists(String network, String question, String analysis) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public String loadAnswer(
      String network, String snapshot, String question, String referenceSnapshot, String analysis)
      throws FileNotFoundException {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public AnswerMetadata loadAnswerMetadata(
      String network, String snapshot, String question, String referenceSnapshot, String analysis)
      throws FileNotFoundException, IOException {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public FileTime getQuestionLastModifiedTime(String network, String question, String analysis) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public FileTime getAnswerLastModifiedTime(
      String network, String snapshot, String question, String referenceSnapshot, String analysis) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public FileTime getAnswerMetadataLastModifiedTime(
      String network, String snapshot, String question, String referenceSnapshot, String analysis) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void storeQuestion(String questionStr, String network, String question, String analysis) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public String loadQuestionSettings(String network, String questionClass) throws IOException {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public boolean checkNetworkExists(String network) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void storeQuestionSettings(String settings, String network, String questionClass)
      throws IOException {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public @Nonnull Map<String, MajorIssueConfig> loadMajorIssueConfigs(
      String network, Set<String> majorIssueTypes) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }
}
