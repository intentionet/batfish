package org.batfish.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.MajorIssueConfig;

/** Storage backend for loading and storing persistent data used by Batfish */
@ParametersAreNonnullByDefault
public interface StorageProvider {

  /**
   * Returns the compressed configuration files for the given snapshot. If a serialized copy of
   * these configurations is not already present, then this function returns {@code null}.
   */
  @Nullable
  SortedMap<String, Configuration> loadCompressedConfigurations(String network, String snapshot);

  /**
   * Returns the configuration files for the given snapshot. If a serialized copy of these
   * configurations is not already present, then this function returns {@code null}.
   */
  @Nullable
  SortedMap<String, Configuration> loadConfigurations(String network, String snapshot);

  /**
   * Returns the {@link ConvertConfigurationAnswerElement} that is the result of the phase that
   * converts vendor-specific configurations to vendor-independent configurations.
   *
   * @param network The name of the network
   * @param snapshot Then name of the snapshot
   */
  @Nullable
  ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElement(
      String network, String snapshot);

  /**
   * Returns the old-style combined layer-1 through layer-3 topology provided in the given snapshot
   *
   * @param network The name of the network
   * @param snapshot The name of the snapshot
   */
  @Nullable
  Topology loadLegacyTopology(String network, String snapshot);

  /**
   * Returns the layer-1 topology of the network provided in the given snapshot
   *
   * @param network The name of the network
   * @param snapshot The name of the snapshot
   */
  @Nullable
  Layer1Topology loadLayer1Topology(String network, String snapshot);

  /**
   * Returns the {@link MajorIssueConfig} for the given network and majorIssueType. If no config
   * exists, will return a valid {@link MajorIssueConfig} with an empty list of {@link
   * MinorIssueConfig}s
   */
  @Nonnull
  MajorIssueConfig loadMajorIssueConfig(String network, String majorIssueType);

  /**
   * Stores the {@link MajorIssueConfig} into the given network. Will replace any previously-stored
   * {@link MajorIssueConfig}s
   *
   * @param network The name of the network
   * @param majorIssueType The type of the {@link MajorIssueConfig}
   * @param majorIssueConfig The {@link MajorIssueConfig} to be stored
   * @throws {@link IOException} if there is an error writing writing the config
   */
  void storeMajorIssueConfig(
      String network, String majorIssueType, MajorIssueConfig majorIssueConfig) throws IOException;

  /**
   * Stores the configurations into the compressed config path for the given snapshot. Will replace
   * any previously-stored compressed configurations.
   */
  void storeCompressedConfigurations(
      Map<String, Configuration> configurations, String network, String snapshot);

  /**
   * Stores the configuration information into the given snapshot. Will replace any
   * previously-stored configurations.
   */
  void storeConfigurations(
      Map<String, Configuration> configurations,
      ConvertConfigurationAnswerElement convertAnswerElement,
      String network,
      String snapshot);

  /**
   * Store the answer to an ad-hoc or analysis question.
   *
   * @param answerStr The text of the answer
   * @param network The name of the network
   * @param snapshot The name of the base snapshot
   * @param question The name of the question
   * @param referenceSnapshot (optional) The name of the reference snapshot for a differential
   *     question, or {@code null} for a non-differential question
   * @param analysis (optional) The name of the analysis for an analysis question, or {@code null}
   *     for an ad-hoc question
   */
  void storeAnswer(
      String answerStr,
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis);

  /**
   * Store the metadata for the answer to an ad-hoc or analysis question.
   *
   * @param answerMetadata The metadata to store
   * @param network The name of the network
   * @param snapshot The name of the base snapshot
   * @param question The name of the question
   * @param referenceSnapshot (optional) The name of the reference snapshot for a differential
   *     question, or {@code null} for a non-differential question
   * @param analysis (optional) The name of the analysis for an analysis question, or {@code null}
   *     for an ad-hoc question
   */
  void storeAnswerMetadata(
      AnswerMetadata answerMetadata,
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis);

  /**
   * Load the text of a JSON-serialized ad-hoc or analysis question
   *
   * @param network The name of the network
   * @param question The name of the question
   * @param analysis (optional) The name of the analysis for an analysis question, or {@code null}
   *     for an ad-hoc question
   */
  @Nonnull
  String loadQuestion(String network, String question, @Nullable String analysis);

  /**
   * Return a list of the names of the questions associated with the given analysis of the given
   * network
   *
   * @param network The name of the network
   * @param analysis The name of the analysis
   */
  @Nonnull
  List<String> listAnalysisQuestions(String network, String analysis);

  /**
   * Returns {@code true} iff the specified question exists.
   *
   * @param network The name of the network
   * @param question The name of the question
   * @param analysis (optional) The name of the analysis for an analysis question, or {@code null}
   *     for an ad-hoc question
   */
  boolean checkQuestionExists(String network, String question, @Nullable String analysis);

  /**
   * Load the JSON-serialized answer to an ad-hoc or analysis question.
   *
   * @param network The name of the network
   * @param snapshot The name of the base snapshot
   * @param question The name of the question
   * @param referenceSnapshot (optional) The name of the reference snapshot for a differential
   *     question, or {@code null} for a non-differential question
   * @param analysis (optional) The name of the analysis for an analysis question, or {@code null}
   *     for an ad-hoc question
   * @throws {@link FileNotFoundException} if answer does not exist; {@link IOException} if there is
   *     an error reading the answer.
   */
  @Nonnull
  String loadAnswer(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws FileNotFoundException, IOException;

  /**
   * Load the metadata for the answer to an ad-hoc or analysis question.
   *
   * @param network The name of the network
   * @param snapshot The name of the base snapshot
   * @param question The name of the question
   * @param referenceSnapshot (optional) The name of the reference snapshot for a differential
   *     question, or {@code null} for a non-differential question
   * @param analysis (optional) The name of the analysis for an analysis question, or {@code null}
   *     for an ad-hoc question
   * @throws {@link FileNotFoundException} if answer metadata does not exist; {@link IOException} if
   *     there is an error reading the answer metadata.
   */
  @Nonnull
  AnswerMetadata loadAnswerMetadata(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws FileNotFoundException, IOException;

  /**
   * Returns the last-modified time of the specified question.
   *
   * @param network The name of the network
   * @param question The name of the question
   * @param analysis (optional) The name of the analysis for an analysis question, or {@code null}
   *     for an ad-hoc question
   */
  @Nonnull
  FileTime getQuestionLastModifiedTime(String network, String question, @Nullable String analysis);

  /**
   * Returns the last-modified time of answer to the specified question.
   *
   * @param network The name of the network
   * @param snapshot The name of the base snapshot
   * @param question The name of the question
   * @param referenceSnapshot (optional) The name of the reference snapshot for a differential
   *     question, or {@code null} for a non-differential question
   * @param analysis (optional) The name of the analysis for an analysis question, or {@code null}
   *     for an ad-hoc question
   */
  @Nonnull
  FileTime getAnswerLastModifiedTime(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis);

  /**
   * Returns the last-modified time of metadata of the answer to the specified question.
   *
   * @param network The name of the network
   * @param snapshot The name of the base snapshot
   * @param question The name of the question
   * @param referenceSnapshot (optional) The name of the reference snapshot for a differential
   *     question, or {@code null} for a non-differential question
   * @param analysis (optional) The name of the analysis for an analysis question, or {@code null}
   *     for an ad-hoc question
   */
  @Nonnull
  FileTime getAnswerMetadataLastModifiedTime(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis);

  /**
   * Stores a question with the specified name and text.
   *
   * @param questionStr The JSON-serialized text of the question
   * @param network The name of the network
   * @param snapshot The name of the base snapshot
   * @param question The name of the question
   */
  void storeQuestion(
      String questionStr, String network, String question, @Nullable String analysis);

  /**
   * Return the JSON-serialized settings for the specified question class for the specified network,
   * or null if no custom settings exist.
   *
   * @param network The name of the network
   * @param questionClass The fully-qualified class name of the question
   * @throws IOException if there is an error trying to read the settings
   */
  @Nullable
  String loadQuestionSettings(String network, String questionClass) throws IOException;

  /** Returns {@code true} iff the specified network question exists. */
  boolean checkNetworkExists(String network);

  /**
   * Write the JSON-serialized settings for the specified question class for the specified network.
   *
   * @param network The name of the network
   * @param questionClass The fully-qualified class name of the question
   * @param settings The settings to write
   * @throws IOException if there is an error writing the settings
   */
  void storeQuestionSettings(String settings, String network, String questionClass)
      throws IOException;

  /**
   * Returns the {@link MajorIssueConfig}s for the given network and majorIssueTypes, keyed by major
   * issue type. If no config exists for a given major issue type, will return a mapping whose value
   * is a {@link MajorIssueConfig} with an empty list of {@link MinorIssueConfig}s
   *
   * @param network The name of the network
   * @param majorIssueTypes The types of the major issues whose configurations are to be loaded
   */
  @Nonnull
  Map<String, MajorIssueConfig> loadMajorIssueConfigs(String network, Set<String> majorIssueTypes);
}
