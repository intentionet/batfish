package org.batfish.storage;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.plugin.PluginConsumer.DEFAULT_HEADER_LENGTH_BYTES;
import static org.batfish.common.plugin.PluginConsumer.detectFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.io.Closer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.Version;
import org.batfish.common.plugin.PluginConsumer.Format;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AnalysisMetadata;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.SnapshotMetadata;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.MajorIssueConfig;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.questions.Question;
import org.batfish.identifiers.AnalysisId;
import org.batfish.identifiers.AnswerId;
import org.batfish.identifiers.IssueSettingsId;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.NodeRolesId;
import org.batfish.identifiers.QuestionId;
import org.batfish.identifiers.QuestionSettingsId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.role.NodeRolesData;

/** A utility class that abstracts the underlying file system storage used by Batfish. */
@ParametersAreNonnullByDefault
public final class FileBasedStorage implements StorageProvider {
  private final BatfishLogger _logger;
  private final BiFunction<String, Integer, AtomicInteger> _newBatch;
  private FileBasedStorageDirectoryProvider _d;

  @VisibleForTesting
  @Nonnull
  FileBasedStorageDirectoryProvider getDirectoryProvider() {
    return _d;
  }

  /**
   * Create a new {@link FileBasedStorage} instance that uses the given root path and job batch
   * provider function.
   */
  public FileBasedStorage(
      Path baseDir, BatfishLogger logger, BiFunction<String, Integer, AtomicInteger> newBatch) {
    _logger = logger;
    _newBatch = newBatch;
    _d = new FileBasedStorageDirectoryProvider(baseDir);
  }

  /**
   * Create a new {@link FileBasedStorage} instance that uses the given root path job and whose job
   * batch provider function is a NOP.
   */
  public FileBasedStorage(Path baseDir, BatfishLogger logger) {
    this(baseDir, logger, (a, b) -> new AtomicInteger());
  }

  /**
   * Returns the compressed configuration files for the given testrig. If a serialized copy of these
   * configurations is not already present, then this function returns {@code null}.
   */
  @Override
  @Nullable
  public SortedMap<String, Configuration> loadCompressedConfigurations(
      NetworkId network, SnapshotId snapshot) {
    Path testrigDir = _d.getSnapshotDir(network, snapshot);
    Path indepDir =
        testrigDir.resolve(
            Paths.get(BfConsts.RELPATH_OUTPUT, BfConsts.RELPATH_COMPRESSED_CONFIG_DIR));
    return loadConfigurations(network, snapshot, indepDir);
  }

  /**
   * Returns the configuration files for the given testrig. If a serialized copy of these
   * configurations is not already present, then this function returns {@code null}.
   */
  @Override
  @Nullable
  public SortedMap<String, Configuration> loadConfigurations(
      NetworkId network, SnapshotId snapshot) {
    Path testrigDir = _d.getSnapshotDir(network, snapshot);
    Path indepDir =
        testrigDir.resolve(
            Paths.get(BfConsts.RELPATH_OUTPUT, BfConsts.RELPATH_VENDOR_INDEPENDENT_CONFIG_DIR));
    return loadConfigurations(network, snapshot, indepDir);
  }

  private @Nullable SortedMap<String, Configuration> loadConfigurations(
      NetworkId network, SnapshotId snapshot, Path indepDir) {
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
      NetworkId network, SnapshotId snapshot) {
    Path ccaePath =
        _d.getSnapshotDir(network, snapshot)
            .resolve(Paths.get(BfConsts.RELPATH_OUTPUT, BfConsts.RELPATH_CONVERT_ANSWER_PATH));
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
  public @Nullable SortedSet<Edge> loadEdgeBlacklist(NetworkId network, SnapshotId snapshot) {
    Path path =
        _d.getSnapshotDir(network, snapshot)
            .resolve(Paths.get(BfConsts.RELPATH_INPUT, BfConsts.RELPATH_EDGE_BLACKLIST_FILE));
    if (!Files.exists(path)) {
      return null;
    }
    String fileText = CommonUtil.readFile(path);
    try {
      return BatfishObjectMapper.mapper()
          .readValue(fileText, new TypeReference<SortedSet<Edge>>() {});
    } catch (IOException e) {
      _logger.warnf(
          "Unexpected exception caught while loading edge blacklist for snapshot %s: %s",
          snapshot, Throwables.getStackTraceAsString(e));
      return null;
    }
  }

  @Override
  public @Nullable SortedSet<NodeInterfacePair> loadInterfaceBlacklist(
      NetworkId network, SnapshotId snapshot) {
    Path path =
        _d.getSnapshotDir(network, snapshot)
            .resolve(Paths.get(BfConsts.RELPATH_INPUT, BfConsts.RELPATH_INTERFACE_BLACKLIST_FILE));
    if (!Files.exists(path)) {
      return null;
    }
    String fileText = CommonUtil.readFile(path);
    try {
      return BatfishObjectMapper.mapper()
          .readValue(fileText, new TypeReference<SortedSet<NodeInterfacePair>>() {});
    } catch (IOException e) {
      _logger.warnf(
          "Unexpected exception caught while loading interface blacklist for snapshot %s: %s",
          snapshot, Throwables.getStackTraceAsString(e));
      return null;
    }
  }

  @Override
  public @Nullable SortedSet<String> loadNodeBlacklist(NetworkId network, SnapshotId snapshot) {
    Path path =
        _d.getSnapshotDir(network, snapshot)
            .resolve(Paths.get(BfConsts.RELPATH_INPUT, BfConsts.RELPATH_NODE_BLACKLIST_FILE));
    if (!Files.exists(path)) {
      return null;
    }
    String fileText = CommonUtil.readFile(path);
    try {
      return BatfishObjectMapper.mapper()
          .readValue(fileText, new TypeReference<SortedSet<String>>() {});
    } catch (IOException e) {
      _logger.warnf(
          "Unexpected exception caught while loading node blacklist for snapshot %s: %s",
          snapshot, Throwables.getStackTraceAsString(e));
      return null;
    }
  }

  @Override
  public @Nullable Topology loadLegacyTopology(NetworkId network, SnapshotId snapshot) {
    Path path =
        _d.getSnapshotDir(network, snapshot)
            .resolve(
                Paths.get(BfConsts.RELPATH_INPUT, BfConsts.RELPATH_TESTRIG_LEGACY_TOPOLOGY_PATH));
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
  public @Nullable Layer1Topology loadLayer1Topology(NetworkId network, SnapshotId snapshot) {
    Path path =
        _d.getSnapshotDir(network, snapshot)
            .resolve(Paths.get(BfConsts.RELPATH_INPUT, BfConsts.RELPATH_L1_TOPOLOGY_PATH));
    if (!Files.exists(path)) {
      // (deprecated)
      path =
          _d.getSnapshotDir(network, snapshot)
              .resolve(Paths.get(BfConsts.RELPATH_INPUT, "testrig_layer1_topology"));
    }
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
  @Nonnull
  public String loadWorkLog(NetworkId network, SnapshotId snapshot, String workId) {
    Path logPath =
        _d.getSnapshotDir(network, snapshot)
            .resolve(BfConsts.RELPATH_OUTPUT)
            .resolve(workId + BfConsts.SUFFIX_LOG_FILE);
    return CommonUtil.readFile(logPath);
  }

  @Override
  public @Nullable MajorIssueConfig loadMajorIssueConfig(
      NetworkId network, IssueSettingsId majorIssueType) {
    Path path = _d.getMajorIssueConfigDir(network, majorIssueType);

    if (!Files.exists(path)) {
      return null;
    }

    String majorIssueFileText = CommonUtil.readFile(path);

    try {
      return BatfishObjectMapper.mapper().readValue(majorIssueFileText, MajorIssueConfig.class);
    } catch (IOException e) {
      _logger.errorf(
          "ERROR: Could not cast file for major issue settings with ID %s in network %s to MajorIssueConfig: %s",
          majorIssueType, network, Throwables.getStackTraceAsString(e));
      return null;
    }
  }

  @Override
  public void storeMajorIssueConfig(
      NetworkId network, IssueSettingsId majorIssueType, MajorIssueConfig majorIssueConfig)
      throws IOException {
    Path path = _d.getMajorIssueConfigDir(network, majorIssueType);

    if (Files.notExists(path)) {
      Files.createDirectories(path.getParent());
    }

    CommonUtil.writeFile(path, BatfishObjectMapper.mapper().writeValueAsString(majorIssueConfig));
  }

  private @Nonnull Path getQuestionSettingsPath(
      NetworkId network, QuestionSettingsId questionSettingsId) {
    return _d.getNetworkSettingsDir(network)
        .resolve(BfConsts.RELPATH_QUESTIONS_DIR)
        .resolve(String.format("%s.json", questionSettingsId.getId()));
  }

  /**
   * Stores the configurations into the compressed config path for the given testrig. Will replace
   * any previously-stored compressed configurations.
   */
  @Override
  public void storeCompressedConfigurations(
      Map<String, Configuration> configurations, NetworkId network, SnapshotId snapshot) {
    Path snapshotDir = _d.getSnapshotDir(network, snapshot);

    if (!snapshotDir.toFile().exists() && !snapshotDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create snapshot directory '%s'", snapshotDir));
    }

    Path outputDir = _d.getCompressedConfigDir(network, snapshot);

    String batchName =
        String.format(
            "Serializing %s compressed configuration structures for snapshot %s",
            configurations.size(), snapshot);
    storeConfigurations(outputDir, batchName, configurations);
  }

  /**
   * Stores the configuration information into the given testrig. Will replace any previously-stored
   * configurations.
   */
  @Override
  public void storeConfigurations(
      Map<String, Configuration> configurations,
      ConvertConfigurationAnswerElement convertAnswerElement,
      NetworkId network,
      SnapshotId snapshot) {
    Path snapshotDir = _d.getSnapshotDir(network, snapshot);
    if (!snapshotDir.toFile().exists() && !snapshotDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create snapshot directory '%s'", snapshotDir));
    }

    // Save the convert configuration answer element.
    Path ccaePath = getConvertAnswerPath(network, snapshot);
    if (!ccaePath.toFile().exists() && !ccaePath.toFile().mkdirs()) {
      throw new BatfishException(String.format("Unable to create directory '%s'", ccaePath));
    }
    CommonUtil.deleteIfExists(ccaePath);
    serializeObject(convertAnswerElement, ccaePath);

    Path outputDir = _d.getVendorIndependentConfigDir(network, snapshot);

    String batchName =
        String.format(
            "Serializing %s vendor-independent configuration structures for snapshot %s",
            configurations.size(), snapshot);

    storeConfigurations(outputDir, batchName, configurations);
  }

  private @Nonnull Path getConvertAnswerPath(NetworkId network, SnapshotId snapshot) {
    return _d.getSnapshotDir(network, snapshot)
        .resolve(Paths.get(BfConsts.RELPATH_OUTPUT, BfConsts.RELPATH_CONVERT_ANSWER_PATH));
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

  @Override
  public void storeAnswer(String answerStr, AnswerId answerId) {
    Path answerPath = getAnswerPath(answerId);
    Path answerDir = answerPath.getParent();
    if (!answerDir.toFile().exists() && !answerDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create answer directory '%s'", answerDir));
    }
    CommonUtil.writeFile(answerPath, answerStr);
  }

  @Override
  public void storeAnswerMetadata(AnswerMetadata answerMetadata, AnswerId answerId) {
    String metricsStr;
    try {
      metricsStr = BatfishObjectMapper.writePrettyString(answerMetadata);
    } catch (JsonProcessingException e) {
      throw new BatfishException("Could not write answer metrics", e);
    }
    Path answerMetadataPath = getAnswerMetadataPath(answerId);
    Path answerDir = answerMetadataPath.getParent();
    if (!answerDir.toFile().exists() && !answerDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create answer metadata directory '%s'", answerDir));
    }
    CommonUtil.writeFile(answerMetadataPath, metricsStr);
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

  private boolean cachedConfigsAreCompatible(NetworkId network, SnapshotId snapshot) {
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

  @Override
  public @Nonnull String loadQuestion(
      NetworkId network, QuestionId question, @Nullable AnalysisId analysis) {
    return CommonUtil.readFile(getQuestionPath(network, question, analysis));
  }

  @Override
  public boolean checkQuestionExists(
      NetworkId network, QuestionId question, @Nullable AnalysisId analysis) {
    return Files.exists(getQuestionPath(network, question, analysis));
  }

  private @Nonnull Path getQuestionPath(
      NetworkId network, QuestionId question, @Nullable AnalysisId analysis) {
    return _d.getQuestionDir(network, question, analysis).resolve(BfConsts.RELPATH_QUESTION_FILE);
  }

  @Override
  public @Nonnull String loadAnswer(AnswerId answerId) throws FileNotFoundException, IOException {
    Path answerPath = getAnswerPath(answerId);
    if (!Files.exists(answerPath)) {
      throw new FileNotFoundException(String.format("Could not find answer with ID: %s", answerId));
    }
    return FileUtils.readFileToString(answerPath.toFile(), UTF_8);
  }

  @Override
  public @Nonnull AnswerMetadata loadAnswerMetadata(AnswerId answerId)
      throws FileNotFoundException, IOException {
    Path answerMetadataPath = getAnswerMetadataPath(answerId);
    if (!Files.exists(answerMetadataPath)) {
      throw new FileNotFoundException(
          String.format("Could not find answer metadata for ID: %s", answerId));
    }
    String answerMetadataStr = FileUtils.readFileToString(answerMetadataPath.toFile(), UTF_8);
    return BatfishObjectMapper.mapper()
        .readValue(answerMetadataStr, new TypeReference<AnswerMetadata>() {});
  }

  private @Nonnull Path getAnswerPath(AnswerId answerId) {
    return _d.getAnswerDir(answerId).resolve(BfConsts.RELPATH_ANSWER_JSON);
  }

  private @Nonnull Path getAnswerMetadataPath(AnswerId answerId) {
    return _d.getAnswerDir(answerId).resolve(BfConsts.RELPATH_ANSWER_METADATA);
  }

  @Override
  public void storeQuestion(
      String questionStr, NetworkId network, QuestionId question, @Nullable AnalysisId analysis) {
    Path questionPath = getQuestionPath(network, question, analysis);
    Path questionDir = questionPath.getParent();
    if (!questionDir.toFile().exists() && !questionDir.toFile().mkdirs()) {
      throw new BatfishException(
          String.format("Unable to create question directory '%s'", questionDir));
    }
    CommonUtil.writeFile(questionPath, questionStr);
  }

  @Override
  public @Nullable String loadQuestionSettings(
      NetworkId networkId, QuestionSettingsId questionSettingsId) throws IOException {
    Path questionSettingsPath = getQuestionSettingsPath(networkId, questionSettingsId);
    if (!Files.exists(questionSettingsPath)) {
      return null;
    }
    return FileUtils.readFileToString(questionSettingsPath.toFile(), UTF_8);
  }

  @Override
  public boolean checkNetworkExists(NetworkId network) {
    return Files.exists(_d.getNetworkDir(network));
  }

  @Override
  public void storeQuestionSettings(
      String settings, NetworkId network, QuestionSettingsId questionSettingsId)
      throws IOException {
    FileUtils.writeStringToFile(
        getQuestionSettingsPath(network, questionSettingsId).toFile(), settings, UTF_8);
  }

  @Override
  public boolean hasAnswerMetadata(AnswerId answerId) {
    return Files.exists(getAnswerMetadataPath(answerId));
  }

  @Override
  public String loadQuestionClassId(
      NetworkId networkId, QuestionId questionId, AnalysisId analysisId) throws IOException {
    return Question.parseQuestion(loadQuestion(networkId, questionId, analysisId)).getName();
  }

  @Override
  public boolean hasAnalysisMetadata(NetworkId networkId, AnalysisId analysisId) {
    return Files.exists(getAnalysisMetadataPath(networkId, analysisId));
  }

  private @Nonnull Path getAnalysisMetadataPath(NetworkId networkId, AnalysisId analysisId) {
    return _d.getNetworkAnalysisDir(networkId, analysisId).resolve(BfConsts.RELPATH_METADATA_FILE);
  }

  private @Nonnull Path getSnapshotMetadataPath(NetworkId networkId, SnapshotId snapshotId) {
    return _d.getSnapshotDir(networkId, snapshotId)
        .resolve(Paths.get(BfConsts.RELPATH_OUTPUT, BfConsts.RELPATH_METADATA_FILE));
  }

  @Override
  public void storeAnalysisMetadata(
      AnalysisMetadata analysisMetadata, NetworkId networkId, AnalysisId analysisId)
      throws IOException {
    FileUtils.writeStringToFile(
        getAnalysisMetadataPath(networkId, analysisId).toFile(),
        BatfishObjectMapper.writePrettyString(analysisMetadata),
        UTF_8);
  }

  @Override
  public String loadAnalysisMetadata(NetworkId networkId, AnalysisId analysisId)
      throws FileNotFoundException, IOException {
    return FileUtils.readFileToString(
        getAnalysisMetadataPath(networkId, analysisId).toFile(), UTF_8);
  }

  @Override
  public void storeSnapshotMetadata(
      SnapshotMetadata snapshotMetadata, NetworkId networkId, SnapshotId snapshotId)
      throws IOException {
    FileUtils.writeStringToFile(
        getSnapshotMetadataPath(networkId, snapshotId).toFile(),
        BatfishObjectMapper.writePrettyString(snapshotMetadata),
        UTF_8);
  }

  @Override
  public String loadSnapshotMetadata(NetworkId networkId, SnapshotId snapshotId)
      throws FileNotFoundException, IOException {
    return FileUtils.readFileToString(
        getSnapshotMetadataPath(networkId, snapshotId).toFile(), UTF_8);
  }

  @Override
  public void storeNodeRoles(NodeRolesData nodeRolesData, NodeRolesId nodeRolesId)
      throws IOException {
    FileUtils.write(
        getNodeRolesPath(nodeRolesId).toFile(),
        BatfishObjectMapper.writePrettyString(nodeRolesData),
        UTF_8);
  }

  private @Nonnull Path getNodeRolesPath(NodeRolesId nodeRolesId) {
    return _d.getNodeRolesDir().resolve(String.format("%s%s", nodeRolesId.getId(), ".json"));
  }

  @Override
  public String loadNodeRoles(NodeRolesId nodeRolesId) throws FileNotFoundException, IOException {
    return FileUtils.readFileToString(getNodeRolesPath(nodeRolesId).toFile(), UTF_8);
  }

  @Override
  public boolean hasNodeRoles(NodeRolesId nodeRolesId) {
    return Files.exists(getNodeRolesPath(nodeRolesId));
  }

  @Override
  public void initNetwork(NetworkId networkId) {
    _d.getNetworkDir(networkId).toFile().mkdirs();
  }

  @Override
  public void deleteAnswerMetadata(AnswerId answerId) throws FileNotFoundException, IOException {
    Files.delete(getAnswerMetadataPath(answerId));
  }

  @Override
  public @Nonnull InputStream loadNetworkObject(NetworkId networkId, String key)
      throws FileNotFoundException, IOException {
    Path objectPath = getNetworkObjectPath(networkId, key);
    if (!Files.exists(objectPath)) {
      throw new FileNotFoundException(String.format("Could not load: %s", objectPath));
    }
    return Files.newInputStream(objectPath);
  }

  /** {@code key} must be relative normalized path. */
  @VisibleForTesting
  static @Nonnull Path objectKeyToRelativePath(String key) {
    Path relativePathCandidate = Paths.get(FilenameUtils.separatorsToSystem(key));
    // ensure path is relative
    checkArgument(
        relativePathCandidate.getRoot() == null,
        "Key '%s' does not represent a relative path",
        key);
    // ensure path is normalized
    checkArgument(
        relativePathCandidate.equals(relativePathCandidate.normalize()),
        "Key '%s' does not represent a normalized path  (without '.', '..',  etc.)",
        key);
    return relativePathCandidate;
  }

  private @Nonnull Path getNetworkObjectPath(NetworkId networkId, String key) {
    String encodedKey = toBase64(key);
    return _d.getNetworkObjectsDir(networkId).resolve(encodedKey);
  }

  @Override
  public void storeNetworkObject(InputStream inputStream, NetworkId networkId, String key)
      throws IOException {
    Path objectPath = getNetworkObjectPath(networkId, key);
    objectPath.getParent().toFile().mkdirs();
    try {
      FileUtils.copyInputStreamToFile(inputStream, objectPath.toFile());
    } finally {
      inputStream.close();
    }
  }

  @Override
  public void deleteNetworkObject(NetworkId networkId, String key)
      throws FileNotFoundException, IOException {
    Path objectPath = getNetworkObjectPath(networkId, key);
    if (!Files.exists(objectPath)) {
      throw new FileNotFoundException(String.format("Could not delete: %s", objectPath));
    }
    Files.delete(objectPath);
  }

  private @Nonnull Path getSnapshotObjectPath(
      NetworkId networkId, SnapshotId snapshotId, String key) throws IOException {
    String encodedKey = toBase64(key);
    return _d.getSnapshotObjectsDir(networkId, snapshotId).resolve(encodedKey);
  }

  private static @Nonnull String toBase64(String key) {
    return Base64.getUrlEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public @Nonnull InputStream loadSnapshotObject(
      NetworkId networkId, SnapshotId snapshotId, String key)
      throws FileNotFoundException, IOException {
    Path objectPath = getSnapshotObjectPath(networkId, snapshotId, key);
    if (!Files.exists(objectPath)) {
      throw new FileNotFoundException(String.format("Could not load: %s", objectPath));
    }
    return Files.newInputStream(objectPath);
  }

  @Override
  public void storeSnapshotObject(
      InputStream inputStream, NetworkId networkId, SnapshotId snapshotId, String key)
      throws IOException {
    Path objectPath = getSnapshotObjectPath(networkId, snapshotId, key);
    objectPath.getParent().toFile().mkdirs();
    try {
      FileUtils.copyInputStreamToFile(inputStream, objectPath.toFile());
    } finally {
      inputStream.close();
    }
  }

  @Override
  public void deleteSnapshotObject(NetworkId networkId, SnapshotId snapshotId, String key)
      throws FileNotFoundException, IOException {
    Path objectPath = getSnapshotObjectPath(networkId, snapshotId, key);
    if (!Files.exists(objectPath)) {
      throw new FileNotFoundException(String.format("Could not delete: %s", objectPath));
    }
    Files.delete(objectPath);
  }

  @Override
  public @Nonnull InputStream loadSnapshotInputObject(
      NetworkId networkId, SnapshotId snapshotId, String key)
      throws FileNotFoundException, IOException {
    Path objectPath = getSnapshotInputObjectPath(networkId, snapshotId, key);
    if (!Files.exists(objectPath)) {
      throw new FileNotFoundException(String.format("Could not load: %s", objectPath));
    }
    return Files.newInputStream(objectPath);
  }

  private Path getSnapshotInputObjectPath(NetworkId networkId, SnapshotId snapshotId, String key)
      throws IOException {
    Path relativePath = objectKeyToRelativePath(key);
    return _d.getSnapshotInputObjectsDir(networkId, snapshotId).resolve(relativePath);
  }

  @Override
  public @Nonnull String loadPojoTopology(NetworkId networkId, SnapshotId snapshotId)
      throws IOException {
    Path path = getPojoTopologyPath(networkId, snapshotId);
    return FileUtils.readFileToString(path.toFile(), UTF_8);
  }

  private @Nonnull Path getPojoTopologyPath(NetworkId networkId, SnapshotId snapshotId) {
    return _d.getSnapshotDir(networkId, snapshotId)
        .resolve(BfConsts.RELPATH_OUTPUT)
        .resolve(BfConsts.RELPATH_TESTRIG_POJO_TOPOLOGY_PATH);
  }

  @Override
  public @Nonnull String loadTopology(NetworkId networkId, SnapshotId snapshotId)
      throws IOException {
    Path path = getEnvTopologyPath(networkId, snapshotId);
    return FileUtils.readFileToString(path.toFile(), UTF_8);
  }

  private @Nonnull Path getEnvTopologyPath(NetworkId networkId, SnapshotId snapshotId) {
    return _d.getSnapshotDir(networkId, snapshotId)
        .resolve(BfConsts.RELPATH_OUTPUT)
        .resolve(BfConsts.RELPATH_ENV_TOPOLOGY_FILE);
  }

  @Override
  public void storeTopology(Topology topology, NetworkId networkId, SnapshotId snapshotId)
      throws IOException {
    Path path = getEnvTopologyPath(networkId, snapshotId);
    path.getParent().toFile().mkdirs();
    FileUtils.writeStringToFile(
        path.toFile(), BatfishObjectMapper.writePrettyString(topology), UTF_8);
  }

  @Override
  public void storePojoTopology(
      org.batfish.datamodel.pojo.Topology topology, NetworkId networkId, SnapshotId snapshotId)
      throws IOException {
    Path path = getPojoTopologyPath(networkId, snapshotId);
    path.getParent().toFile().mkdirs();
    FileUtils.writeStringToFile(
        path.toFile(), BatfishObjectMapper.writePrettyString(topology), UTF_8);
  }
}
