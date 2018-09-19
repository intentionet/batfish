package org.batfish.storage;

import static org.batfish.common.plugin.PluginConsumer.DEFAULT_HEADER_LENGTH_BYTES;
import static org.batfish.common.plugin.PluginConsumer.detectFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.apache.commons.io.FileUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.Version;
import org.batfish.common.plugin.PluginConsumer.Format;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.MajorIssueConfig;

/** A utility class that abstracts the underlying file system storage used by {@link Batfish}. */
@ParametersAreNonnullByDefault
public final class FileBasedStorage implements StorageProvider {
  private final Path _baseDir;
  private final BatfishLogger _logger;
  private final BiFunction<String, Integer, AtomicInteger> _newBatch;

  /**
   * Create a new {@link FileBasedStorage} instance that uses the given root path and job batch
   * provider function.
   */
  public FileBasedStorage(
      Path baseDir, BatfishLogger logger, BiFunction<String, Integer, AtomicInteger> newBatch) {
    _baseDir = baseDir;
    _logger = logger;
    _newBatch = newBatch;
  }

  /**
   * Create a new {@link FileBasedStorage} instance that uses the given root path job and whose job
   * batch provider function is a NOP.
   */
  public FileBasedStorage(Path baseDir, BatfishLogger logger) {
    _baseDir = baseDir;
    _logger = logger;
    _newBatch = (a, b) -> new AtomicInteger();
  }

  /**
   * Returns the compressed configuration files for the given testrig. If a serialized copy of these
   * configurations is not already present, then this function returns {@code null}.
   */
  @Override
  @Nullable
  public SortedMap<String, Configuration> loadCompressedConfigurations(
      String network, String snapshot) {
    Path testrigDir = getSnapshotDir(network, snapshot);
    Path indepDir = testrigDir.resolve(BfConsts.RELPATH_COMPRESSED_CONFIG_DIR);
    return loadConfigurations(network, snapshot, indepDir);
  }

  /**
   * Returns the configuration files for the given testrig. If a serialized copy of these
   * configurations is not already present, then this function returns {@code null}.
   */
  @Override
  @Nullable
  public SortedMap<String, Configuration> loadConfigurations(String network, String snapshot) {
    Path testrigDir = getSnapshotDir(network, snapshot);
    Path indepDir = testrigDir.resolve(BfConsts.RELPATH_VENDOR_INDEPENDENT_CONFIG_DIR);
    return loadConfigurations(network, snapshot, indepDir);
  }

  private @Nullable SortedMap<String, Configuration> loadConfigurations(
      String network, String snapshot, Path indepDir) {
    // If the directory that would contain these configs does not even exist, no cache exists.
    if (!Files.exists(indepDir)) {
      _logger.debugf("Unable to load configs for %s from disk: no cache directory", snapshot);
      return null;
    }

    // If the directory exists, then likely the configs exist and are useful. Still, we need to
    // confirm that they were serialized with a compatible version of Batfish first.
    if (!cachedConfigsAreCompatible(network, snapshot)) {
      _logger.debugf(
          "Unable to load configs for %s from disk: error or incompatible version", snapshot);
      return null;
    }

    _logger.info("\n*** DESERIALIZING VENDOR-INDEPENDENT CONFIGURATION STRUCTURES ***\n");
    Map<Path, String> namesByPath = new TreeMap<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(indepDir)) {
      for (Path serializedConfig : stream) {
        String name = serializedConfig.getFileName().toString();
        namesByPath.put(serializedConfig, name);
      }
    } catch (IOException e) {
      throw new BatfishException(
          "Error reading vendor-independent configs directory: '" + indepDir + "'", e);
    }
    try {
      return deserializeObjects(namesByPath, Configuration.class);
    } catch (BatfishException e) {
      return null;
    }
  }

  @Override
  public @Nullable ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElement(
      String network, String snapshot) {
    Path ccaePath = getSnapshotDir(network, snapshot).resolve(BfConsts.RELPATH_CONVERT_ANSWER_PATH);
    if (!Files.exists(ccaePath)) {
      return null;
    }
    try {
      return deserializeObject(ccaePath, ConvertConfigurationAnswerElement.class);
    } catch (BatfishException e) {
      _logger.errorf(
          "Failed to deserialize ConvertConfigurationAnswerElement: %s",
          Throwables.getStackTraceAsString(e));
      return null;
    }
  }

  @Override
  public @Nullable Topology loadLegacyTopology(String network, String snapshot) {
    Path path =
        getSnapshotDir(network, snapshot)
            .resolve(
                Paths.get(
                    BfConsts.RELPATH_TEST_RIG_DIR, BfConsts.RELPATH_TESTRIG_LEGACY_TOPOLOGY_PATH));
    if (!Files.exists(path)) {
      return null;
    }
    AtomicInteger counter = _newBatch.apply("Reading legacy topology", 1);
    String topologyFileText = CommonUtil.readFile(path);
    try {
      return BatfishObjectMapper.mapper().readValue(topologyFileText, Topology.class);
    } catch (IOException e) {
      _logger.warnf(
          "Unexpected exception caught while loading legacy testrig topology for snapshot %s: %s",
          snapshot, Throwables.getStackTraceAsString(e));
      return null;
    } finally {
      counter.incrementAndGet();
    }
  }

  @Override
  public @Nullable Layer1Topology loadLayer1Topology(String network, String snapshot) {
    Path path =
        getSnapshotDir(network, snapshot)
            .resolve(
                Paths.get(
                    BfConsts.RELPATH_TEST_RIG_DIR, BfConsts.RELPATH_TESTRIG_L1_TOPOLOGY_PATH));
    if (!Files.exists(path)) {
      return null;
    }
    AtomicInteger counter = _newBatch.apply("Reading layer-1 topology", 1);
    String topologyFileText = CommonUtil.readFile(path);
    try {
      return BatfishObjectMapper.mapper().readValue(topologyFileText, Layer1Topology.class);
    } catch (IOException e) {
      _logger.warnf(
          "Unexpected exception caught while loading layer-1 topology for snapshot %s: %s",
          snapshot, Throwables.getStackTraceAsString(e));
      return null;
    } finally {
      counter.incrementAndGet();
    }
  }

  @Override
  public @Nonnull MajorIssueConfig loadMajorIssueConfig(String network, String majorIssueType) {
    Path path = getMajorIssueConfigDir(network, majorIssueType);

    if (!Files.exists(path)) {
      return new MajorIssueConfig(majorIssueType, ImmutableMap.of());
    }

    String majorIssueFileText = CommonUtil.readFile(path);

    try {
      return BatfishObjectMapper.mapper().readValue(majorIssueFileText, MajorIssueConfig.class);
    } catch (IOException e) {
      _logger.errorf(
          "ERROR: Could not cast file for major issue %s in network %s to MajorIssueConfig: %s",
          majorIssueType, network, Throwables.getStackTraceAsString(e));
      return new MajorIssueConfig(majorIssueType, ImmutableMap.of());
    }
  }

  @Override
  public void storeMajorIssueConfig(
      String network, String majorIssueType, MajorIssueConfig majorIssueConfig) throws IOException {
    Path path = getMajorIssueConfigDir(network, majorIssueType);

    if (Files.notExists(path)) {
      Files.createDirectories(path.getParent());
    }

    CommonUtil.writeFile(path, BatfishObjectMapper.mapper().writeValueAsString(majorIssueConfig));
  }

  private @Nonnull Path getMajorIssueConfigDir(String network, String majorIssueType) {
    return getNetworkSettingsDir(network)
        .resolve(BfConsts.RELPATH_CONTAINER_SETTINGS_ISSUES)
        .resolve(majorIssueType + ".json");
  }

  private @Nonnull Path getNetworkSettingsDir(String network) {
    return getNetworkDir(network).resolve(BfConsts.RELPATH_CONTAINER_SETTINGS);
  }

  private @Nonnull Path getQuestionSettingsPath(String network, String questionClass) {
    return getNetworkSettingsDir(network)
        .resolve(BfConsts.RELPATH_QUESTIONS_DIR)
        .resolve(String.format("%s.json", questionClass));
  }

  /**
   * Stores the configurations into the compressed config path for the given testrig. Will replace
   * any previously-stored compressed configurations.
   */
  @Override
  public void storeCompressedConfigurations(
      Map<String, Configuration> configurations, String network, String snapshot) {
    Path snapshotDir = getSnapshotDir(network, snapshot);

    if (!snapshotDir.toFile().exists() && !snapshotDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create snapshot directory '%s'", snapshotDir));
    }

    Path outputDir = getCompressedConfigDir(network, snapshot);

    String batchName =
        String.format(
            "Serializing %s compressed configuration structures for snapshot %s",
            configurations.size(), snapshot);
    storeConfigurations(outputDir, batchName, configurations);
  }

  private Path getCompressedConfigDir(String network, String snapshot) {
    return getSnapshotDir(network, snapshot).resolve(BfConsts.RELPATH_COMPRESSED_CONFIG_DIR);
  }

  /**
   * Stores the configuration information into the given testrig. Will replace any previously-stored
   * configurations.
   */
  @Override
  public void storeConfigurations(
      Map<String, Configuration> configurations,
      ConvertConfigurationAnswerElement convertAnswerElement,
      String network,
      String snapshot) {
    Path snapshotDir = getSnapshotDir(network, snapshot);
    if (!snapshotDir.toFile().exists() && !snapshotDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create snapshot directory '%s'", snapshotDir));
    }

    // Save the convert configuration answer element.
    Path ccaePath = getConvertAnswerPath(network, snapshot);
    CommonUtil.deleteIfExists(ccaePath);
    serializeObject(convertAnswerElement, ccaePath);

    Path outputDir = getVendorIndependentConfigDir(network, snapshot);

    String batchName =
        String.format(
            "Serializing %s vendor-independent configuration structures for snapshot %s",
            configurations.size(), snapshot);

    storeConfigurations(outputDir, batchName, configurations);
  }

  private @Nonnull Path getVendorIndependentConfigDir(String network, String snapshot) {
    return getSnapshotDir(network, snapshot)
        .resolve(BfConsts.RELPATH_VENDOR_INDEPENDENT_CONFIG_DIR);
  }

  private @Nonnull Path getConvertAnswerPath(String network, String snapshot) {
    return getSnapshotDir(network, snapshot).resolve(BfConsts.RELPATH_CONVERT_ANSWER_PATH);
  }

  private void storeConfigurations(
      Path outputDir, String batchName, Map<String, Configuration> configurations) {
    _logger.infof("\n*** %s***\n", batchName.toUpperCase());
    AtomicInteger progressCount = _newBatch.apply(batchName, configurations.size());

    // Delete any existing output, then recreate.
    CommonUtil.deleteDirectory(outputDir);
    if (!outputDir.toFile().exists() && !outputDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create output directory '%s'", outputDir));
    }

    configurations
        .entrySet()
        .parallelStream()
        .forEach(
            e -> {
              Path currentOutputPath = outputDir.resolve(e.getKey());
              serializeObject(e.getValue(), currentOutputPath);
              progressCount.incrementAndGet();
            });
  }

  private @Nonnull Path getAnswerDir(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis) {
    return referenceSnapshot != null
        ? getDeltaAnswerDir(network, snapshot, question, referenceSnapshot, analysis)
        : getStandardAnswerDir(network, snapshot, question, analysis);
  }

  private @Nonnull Path getStandardAnswerDir(
      String network, String snapshot, String question, @Nullable String analysis) {
    Path snapshotDir = getSnapshotDir(network, snapshot);
    return analysis != null
        ? snapshotDir
            .resolve(BfConsts.RELPATH_ANALYSES_DIR)
            .resolve(analysis)
            .resolve(BfConsts.RELPATH_QUESTIONS_DIR)
            .resolve(question)
            .resolve(BfConsts.RELPATH_ENVIRONMENTS_DIR)
            .resolve(BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME)
        : snapshotDir
            .resolve(BfConsts.RELPATH_ANSWERS_DIR)
            .resolve(question)
            .resolve(BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME)
            .resolve(BfConsts.RELPATH_STANDARD_DIR);
  }

  private @Nonnull Path getDeltaAnswerDir(
      String network,
      String snapshot,
      String question,
      String referenceSnapshot,
      @Nullable String analysis) {
    Path snapshotDir = getSnapshotDir(network, snapshot);
    return analysis != null
        ? snapshotDir
            .resolve(BfConsts.RELPATH_ANALYSES_DIR)
            .resolve(analysis)
            .resolve(BfConsts.RELPATH_QUESTIONS_DIR)
            .resolve(question)
            .resolve(BfConsts.RELPATH_ENVIRONMENTS_DIR)
            .resolve(BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME)
            .resolve(BfConsts.RELPATH_DELTA)
            .resolve(referenceSnapshot)
            .resolve(BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME)
        : snapshotDir
            .resolve(BfConsts.RELPATH_ANSWERS_DIR)
            .resolve(question)
            .resolve(BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME)
            .resolve(BfConsts.RELPATH_DIFF_DIR)
            .resolve(referenceSnapshot)
            .resolve(BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME);
  }

  @Override
  public void storeAnswer(
      String answerStr,
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis) {
    Path answerPath = getAnswerPath(network, snapshot, question, referenceSnapshot, analysis);
    Path answerDir = getAnswerDir(network, snapshot, question, referenceSnapshot, analysis);
    if (!answerDir.toFile().exists() && !answerDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create answer directory '%s'", answerDir));
    }
    CommonUtil.writeFile(answerPath, answerStr);
  }

  @Override
  public void storeAnswerMetadata(
      AnswerMetadata answerMetadata,
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis) {
    String metricsStr;
    try {
      metricsStr = BatfishObjectMapper.writePrettyString(answerMetadata);
    } catch (JsonProcessingException e) {
      throw new BatfishException("Could not write answer metrics", e);
    }
    Path answerDir = getAnswerDir(network, snapshot, question, referenceSnapshot, analysis);
    if (!answerDir.toFile().exists() && !answerDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create answer metadata directory '%s'", answerDir));
    }
    Path answerMetricsPath = answerDir.resolve(BfConsts.RELPATH_ANSWER_METADATA);
    CommonUtil.writeFile(answerMetricsPath, metricsStr);
  }

  /**
   * Returns a single object of the given class deserialized from the given file. Uses the {@link
   * FileBasedStorage} default file encoding including serialization format and compression.
   */
  private static <S extends Serializable> S deserializeObject(Path inputFile, Class<S> outputClass)
      throws BatfishException {
    try (Closer closer = Closer.create()) {
      FileInputStream fis = closer.register(new FileInputStream(inputFile.toFile()));
      PushbackInputStream pbstream = new PushbackInputStream(fis, DEFAULT_HEADER_LENGTH_BYTES);
      Format f = detectFormat(pbstream);
      ObjectInputStream ois;
      if (f == Format.GZIP) {
        GZIPInputStream gis =
            closer.register(new GZIPInputStream(pbstream, 8192 /* enlarge buffer */));
        ois = new ObjectInputStream(gis);
      } else if (f == Format.LZ4) {
        LZ4FrameInputStream lis = closer.register(new LZ4FrameInputStream(pbstream));
        ois = new ObjectInputStream(lis);
      } else if (f == Format.JAVA_SERIALIZED) {
        ois = new ObjectInputStream(pbstream);
      } else {
        throw new BatfishException(
            String.format("Could not detect format of the file %s", inputFile));
      }
      closer.register(ois);
      return outputClass.cast(ois.readObject());
    } catch (IOException | ClassNotFoundException | ClassCastException e) {
      throw new BatfishException(
          String.format(
              "Failed to deserialize object of type %s from file %s",
              outputClass.getCanonicalName(), inputFile),
          e);
    }
  }

  private <S extends Serializable> SortedMap<String, S> deserializeObjects(
      Map<Path, String> namesByPath, Class<S> outputClass) {
    String outputClassName = outputClass.getName();
    AtomicInteger completed =
        _newBatch.apply(
            String.format("Deserializing objects of type '%s' from files", outputClassName),
            namesByPath.size());
    return new TreeMap<>(
        namesByPath
            .entrySet()
            .parallelStream()
            .collect(
                Collectors.toMap(
                    Entry::getValue,
                    entry -> {
                      Path inputPath = entry.getKey();
                      String name = entry.getValue();
                      _logger.debugf(
                          "Reading %s '%s' from '%s'\n", outputClassName, name, inputPath);
                      S output = deserializeObject(inputPath, outputClass);
                      completed.incrementAndGet();
                      return output;
                    })));
  }

  /**
   * Writes a single object of the given class to the given file. Uses the {@link FileBasedStorage}
   * default file encoding including serialization format and compression.
   */
  private static void serializeObject(Serializable object, Path outputFile) {
    try {
      try (OutputStream out = Files.newOutputStream(outputFile);
          LZ4FrameOutputStream gos = new LZ4FrameOutputStream(out);
          ObjectOutputStream oos = new ObjectOutputStream(gos)) {
        oos.writeObject(object);
      }
    } catch (IOException e) {
      throw new BatfishException("Failed to serialize object to output file: " + outputFile, e);
    }
  }

  private boolean cachedConfigsAreCompatible(String network, String snapshot) {
    try {
      ConvertConfigurationAnswerElement ccae =
          loadConvertConfigurationAnswerElement(network, snapshot);
      return ccae != null
          && Version.isCompatibleVersion(
              FileBasedStorage.class.getCanonicalName(),
              "Old processed configurations",
              ccae.getVersion());
    } catch (BatfishException e) {
      _logger.warnf(
          "Unexpected exception caught while deserializing configs for snapshot %s: %s",
          snapshot, Throwables.getStackTraceAsString(e));
      return false;
    }
  }

  private @Nonnull Path getNetworkAnalysisDir(String network, String analysis) {
    return getNetworkDir(network).resolve(BfConsts.RELPATH_ANALYSES_DIR).resolve(analysis);
  }

  private @Nonnull Path getAnalysisQuestionDir(String network, String question, String analysis) {
    return getAnalysisQuestionsDir(network, analysis).resolve(question);
  }

  private @Nonnull Path getAdHocQuestionDir(String network, String question) {
    return getNetworkDir(network).resolve(BfConsts.RELPATH_QUESTIONS_DIR).resolve(question);
  }

  @VisibleForTesting
  @Nonnull
  Path getNetworkDir(String network) {
    return _baseDir.resolve(network);
  }

  private @Nonnull Path getSnapshotDir(String network, String snapshot) {
    return getNetworkDir(network).resolve(BfConsts.RELPATH_TESTRIGS_DIR).resolve(snapshot);
  }

  private @Nonnull Path getQuestionDir(String network, String question, @Nullable String analysis) {
    return analysis != null
        ? getAnalysisQuestionDir(network, question, analysis)
        : getAdHocQuestionDir(network, question);
  }

  @Override
  public @Nonnull String loadQuestion(String network, String question, @Nullable String analysis) {
    return CommonUtil.readFile(getQuestionPath(network, question, analysis));
  }

  private @Nonnull Path getVendorSpecificConfigDir(String network, String snapshot) {
    return getSnapshotDir(network, snapshot).resolve(BfConsts.RELPATH_VENDOR_SPECIFIC_CONFIG_DIR);
  }

  @Override
  public @Nonnull List<String> listAnalysisQuestions(String network, String analysis) {
    Path analysisQuestionsDir = getAnalysisQuestionsDir(network, analysis);
    if (!Files.exists(analysisQuestionsDir)) {
      throw new BatfishException(
          String.format("Analysis questions dir does not exist: '%s'", analysisQuestionsDir));
    }
    try (Stream<Path> analysisQuestions = CommonUtil.list(analysisQuestionsDir)) {
      return analysisQuestions
          .map(Path::getFileName)
          .map(Object::toString)
          .collect(ImmutableList.toImmutableList());
    }
  }

  private @Nonnull Path getAnalysisQuestionsDir(String network, String analysis) {
    return getNetworkAnalysisDir(network, analysis).resolve(BfConsts.RELPATH_QUESTIONS_DIR);
  }

  @Override
  public boolean checkQuestionExists(String network, String question, @Nullable String analysis) {
    return Files.exists(getQuestionPath(network, question, analysis));
  }

  private @Nonnull Path getQuestionPath(
      String network, String question, @Nullable String analysis) {
    return getQuestionDir(network, question, analysis).resolve(BfConsts.RELPATH_QUESTION_FILE);
  }

  @Override
  public @Nonnull String loadAnswer(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws FileNotFoundException, IOException {
    Path answerPath = getAnswerPath(network, snapshot, question, referenceSnapshot, analysis);
    if (!Files.exists(answerPath)) {
      throw new FileNotFoundException(
          String.format(
              "Could not find answer for question:'%s' in network:'%s'; snapshot:'%s'; referenceSnapshot:'%s'; analysis:'%s'",
              question, network, snapshot, referenceSnapshot, analysis));
    }
    return FileUtils.readFileToString(answerPath.toFile());
  }

  @Override
  public @Nonnull AnswerMetadata loadAnswerMetadata(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws FileNotFoundException, IOException {
    Path answerMetadataPath =
        getAnswerMetadataPath(network, snapshot, question, referenceSnapshot, analysis);
    if (!Files.exists(answerMetadataPath)) {
      throw new FileNotFoundException(
          String.format(
              "Could not find answer metadata for question:'%s' in network:'%s'; snapshot:'%s'; referenceSnapshot:'%s'; analysis:'%s'",
              question, network, snapshot, referenceSnapshot, analysis));
    }
    String answerMetadataStr = FileUtils.readFileToString(answerMetadataPath.toFile());
    return BatfishObjectMapper.mapper()
        .readValue(answerMetadataStr, new TypeReference<AnswerMetadata>() {});
  }

  private @Nonnull Path getAnswerPath(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis) {
    return getAnswerDir(network, snapshot, question, referenceSnapshot, analysis)
        .resolve(BfConsts.RELPATH_ANSWER_JSON);
  }

  private @Nonnull Path getAnswerMetadataPath(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis) {
    return getAnswerDir(network, snapshot, question, referenceSnapshot, analysis)
        .resolve(BfConsts.RELPATH_ANSWER_METADATA);
  }

  @Override
  public FileTime getQuestionLastModifiedTime(
      String network, String question, @Nullable String analysis) {
    return CommonUtil.getLastModifiedTime(getQuestionPath(network, question, analysis));
  }

  @Override
  public FileTime getAnswerLastModifiedTime(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis) {
    return CommonUtil.getLastModifiedTime(
        getAnswerPath(network, snapshot, question, referenceSnapshot, analysis));
  }

  @Override
  public FileTime getAnswerMetadataLastModifiedTime(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis) {
    return CommonUtil.getLastModifiedTime(
        getAnswerMetadataPath(network, snapshot, question, referenceSnapshot, analysis));
  }

  @Override
  public void storeQuestion(
      String questionStr, String network, String question, @Nullable String analysis) {
    Path questionPath = getQuestionPath(network, question, analysis);
    Path questionDir = questionPath.getParent();
    if (!questionDir.toFile().exists() && !questionDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create question directory '%s'", questionDir));
    }
    CommonUtil.writeFile(questionPath, questionStr);
  }

  @Override
  public @Nullable String loadQuestionSettings(String network, String questionClass)
      throws IOException {
    Path questionSettingsPath = getQuestionSettingsPath(network, questionClass);
    if (!Files.exists(questionSettingsPath)) {
      return null;
    }
    return FileUtils.readFileToString(questionSettingsPath.toFile());
  }

  @Override
  public boolean checkNetworkExists(String network) {
    return Files.exists(getNetworkDir(network));
  }

  @Override
  public void storeQuestionSettings(String settings, String network, String questionClass)
      throws IOException {
    FileUtils.writeStringToFile(getQuestionSettingsPath(network, questionClass).toFile(), settings);
  }

  @Override
  public @Nonnull Map<String, MajorIssueConfig> loadMajorIssueConfigs(
      String network, Set<String> majorIssueTypes) {
    return majorIssueTypes
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Function.identity(),
                majorIssueType -> loadMajorIssueConfig(network, majorIssueType)));
  }
}
