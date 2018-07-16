package org.batfish.main;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.collections4.map.LRUMap;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Snapshot;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.config.Settings.EnvironmentSettings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.dataplane.ibdp.IncrementalDataPlanePlugin;
import org.junit.rules.TemporaryFolder;

public class BatfishTestUtils {

  private static Cache<NetworkSnapshot, SortedMap<String, Configuration>> makeTestrigCache() {
    return CacheBuilder.newBuilder().maximumSize(5).build();
  }

  private static Map<NetworkSnapshot, SortedMap<String, BgpAdvertisementsByVrf>> makeEnvBgpCache() {
    return Collections.synchronizedMap(new LRUMap<>(4));
  }

  private static Map<NetworkSnapshot, SortedMap<String, RoutesByVrf>> makeEnvRouteCache() {
    return Collections.synchronizedMap(new LRUMap<>(4));
  }

  private static Cache<NetworkSnapshot, DataPlane> makeDataPlaneCache() {
    return CacheBuilder.newBuilder().maximumSize(2).build();
  }

  private static Batfish initBatfish(
      SortedMap<String, Configuration> configurations, @Nonnull TemporaryFolder tempFolder)
      throws IOException {
    Settings settings = new Settings(new String[] {});
    settings.setLogger(new BatfishLogger("debug", false));
    final Cache<NetworkSnapshot, SortedMap<String, Configuration>> testrigs = makeTestrigCache();
    final Cache<NetworkSnapshot, SortedMap<String, Configuration>> compressedTestrigs =
        makeTestrigCache();

    settings.setStorageBase(tempFolder.newFolder().toPath());
    settings.setContainer("tempContainer");
    if (!configurations.isEmpty()) {
      settings.setTestrig("tempTestrig");
      settings.setEnvironmentName("tempEnvironment");
      Batfish.initTestrigSettings(settings);
      settings.getBaseTestrigSettings().getEnvironmentSettings().getEnvPath().toFile().mkdirs();
      testrigs.put(
          new NetworkSnapshot("tempContainer", new Snapshot("tempTestrig", "tempEnvironment")),
          configurations);
      settings.setActiveTestrigSettings(settings.getBaseTestrigSettings());
    }
    Batfish batfish =
        new Batfish(
            settings,
            compressedTestrigs,
            testrigs,
            makeDataPlaneCache(),
            makeDataPlaneCache(),
            makeEnvBgpCache(),
            makeEnvRouteCache());
    if (!configurations.isEmpty()) {
      Batfish.serializeAsJson(
          settings.getBaseTestrigSettings().getEnvironmentSettings().getSerializedTopologyPath(),
          batfish.computeEnvironmentTopology(configurations),
          "environment topology");
    }
    registerDataPlanePlugins(batfish);
    return batfish;
  }

  private static Batfish initBatfish(
      @Nonnull SortedMap<String, Configuration> baseConfigs,
      @Nonnull SortedMap<String, Configuration> deltaConfigs,
      @Nonnull TemporaryFolder tempFolder)
      throws IOException {
    Settings settings = new Settings(new String[] {});
    settings.setLogger(new BatfishLogger("debug", false));
    final Cache<NetworkSnapshot, SortedMap<String, Configuration>> testrigs = makeTestrigCache();
    final Cache<NetworkSnapshot, SortedMap<String, Configuration>> compressedTestrigs =
        makeTestrigCache();

    settings.setStorageBase(tempFolder.newFolder().toPath());
    settings.setContainer("tempContainer");
    if (!baseConfigs.isEmpty()) {
      settings.setTestrig("tempTestrig");
      settings.setEnvironmentName("tempEnvironment");
      settings.setDeltaTestrig("tempDeltaTestrig");
      settings.setDeltaEnvironmentName("tempDeltaEnvironment");
      Batfish.initTestrigSettings(settings);
      settings.getBaseTestrigSettings().getEnvironmentSettings().getEnvPath().toFile().mkdirs();
      settings.getDeltaTestrigSettings().getEnvironmentSettings().getEnvPath().toFile().mkdirs();
      testrigs.put(
          new NetworkSnapshot("tempContainer", new Snapshot("tempTestrig", "tempEnvironment")),
          baseConfigs);
      testrigs.put(
          new NetworkSnapshot(
              "tempContainer", new Snapshot("tempDeltaTestrig", "tempDeltaEnvironment")),
          deltaConfigs);
      settings.setActiveTestrigSettings(settings.getBaseTestrigSettings());
    }
    Batfish batfish =
        new Batfish(
            settings,
            compressedTestrigs,
            testrigs,
            makeDataPlaneCache(),
            makeDataPlaneCache(),
            makeEnvBgpCache(),
            makeEnvRouteCache());
    batfish.getSettings().setDiffQuestion(true);
    if (!baseConfigs.isEmpty()) {
      Batfish.serializeAsJson(
          settings.getBaseTestrigSettings().getEnvironmentSettings().getSerializedTopologyPath(),
          batfish.computeEnvironmentTopology(baseConfigs),
          "environment topology");
    }
    if (!deltaConfigs.isEmpty()) {
      batfish.pushDeltaEnvironment();
      Batfish.serializeAsJson(
          settings.getDeltaTestrigSettings().getEnvironmentSettings().getSerializedTopologyPath(),
          batfish.computeEnvironmentTopology(deltaConfigs),
          "environment topology");
      batfish.popEnvironment();
    }
    registerDataPlanePlugins(batfish);
    return batfish;
  }

  private static void registerDataPlanePlugins(Batfish batfish) {
    IncrementalDataPlanePlugin ibdpPlugin = new IncrementalDataPlanePlugin();
    ibdpPlugin.initialize(batfish);
  }

  /**
   * Get a new Batfish instance with given configurations, tempFolder should be present for
   * non-empty configurations
   *
   * @param testrigText Structure containing names and content of testrig input files
   * @param tempFolder Temporary folder to be used to files required for Batfish
   * @return Batfish instance pointing at new testrig comprising testrigText
   */
  public static Batfish getBatfishFromTestrigText(
      TestrigText testrigText, TemporaryFolder tempFolder) throws IOException {
    Map<String, String> awsText = testrigText.getAwsText();
    Map<String, String> bgpTablesText = testrigText.getBgpTablesText();
    Map<String, String> configurationText = testrigText.getConfigurationText();
    Map<String, String> hostsText = testrigText.getHostsText();
    Map<String, String> iptablesFilesText = testrigText.getIptablesFilesText();
    String layer1TopologyText = testrigText.getLayer1TopologyText();
    String legacyTopologyText = testrigText.getLegacyTopologyText();
    Map<String, String> routingTablesText = testrigText.getRoutingTablesText();

    Settings settings = new Settings(new String[] {});
    settings.setLogger(new BatfishLogger("debug", false));
    settings.setDisableUnrecognized(true);
    settings.setHaltOnConvertError(true);
    settings.setHaltOnParseError(true);
    settings.setThrowOnLexerError(true);
    settings.setThrowOnParserError(true);
    settings.setVerboseParse(true);
    settings.setStorageBase(tempFolder.newFolder().toPath());
    settings.setContainer("tempContainer");
    settings.setTestrig("tempTestrig");
    settings.setEnvironmentName("tempEnvironment");
    Batfish.initTestrigSettings(settings);
    Path testrigPath = settings.getBaseTestrigSettings().getTestRigPath();
    settings.setActiveTestrigSettings(settings.getBaseTestrigSettings());
    EnvironmentSettings envSettings = settings.getBaseTestrigSettings().getEnvironmentSettings();
    envSettings.getEnvPath().toFile().mkdirs();
    writeTemporaryTestrigFiles(
        configurationText, testrigPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR));
    writeTemporaryTestrigFiles(awsText, testrigPath.resolve(BfConsts.RELPATH_AWS_CONFIGS_DIR));
    writeTemporaryTestrigFiles(bgpTablesText, envSettings.getEnvironmentBgpTablesPath());
    writeTemporaryTestrigFiles(hostsText, testrigPath.resolve(BfConsts.RELPATH_HOST_CONFIGS_DIR));
    writeTemporaryTestrigFiles(iptablesFilesText, testrigPath.resolve("iptables"));
    if (layer1TopologyText != null) {
      writeTemporaryTestrigFiles(
          ImmutableMap.of(BfConsts.RELPATH_TESTRIG_L1_TOPOLOGY_PATH, layer1TopologyText),
          testrigPath);
    }
    if (legacyTopologyText != null) {
      writeTemporaryTestrigFiles(
          ImmutableMap.of(BfConsts.RELPATH_TESTRIG_LEGACY_TOPOLOGY_PATH, legacyTopologyText),
          testrigPath);
    }
    writeTemporaryTestrigFiles(routingTablesText, envSettings.getEnvironmentRoutingTablesPath());
    Batfish batfish =
        new Batfish(
            settings,
            makeTestrigCache(),
            makeTestrigCache(),
            makeDataPlaneCache(),
            makeDataPlaneCache(),
            makeEnvBgpCache(),
            makeEnvRouteCache());
    registerDataPlanePlugins(batfish);
    return batfish;
  }

  /**
   * Get a configuration object with the given interfaces
   *
   * @param nodeName Host name for the configuration
   * @param configFormat Configuration format
   * @param interfaceNames All interface names to be included
   * @return A new configuration
   */
  public static Configuration createTestConfiguration(
      String nodeName, ConfigurationFormat configFormat, String... interfaceNames) {
    Configuration config = new Configuration(nodeName, configFormat);
    for (String interfaceName : interfaceNames) {
      config.getInterfaces().put(interfaceName, new Interface(interfaceName, config));
    }
    return config;
  }

  /**
   * Get a new Batfish instance with given configurations, tempFolder should be present for
   * non-empty configurations
   *
   * @param configurations Map of all Configuration Name -> Configuration Object
   * @param tempFolder Temporary folder to be used to files required for Batfish
   * @return New Batfish instance
   */
  public static Batfish getBatfish(
      SortedMap<String, Configuration> configurations, @Nonnull TemporaryFolder tempFolder)
      throws IOException {
    return initBatfish(configurations, tempFolder);
  }

  /**
   * Get a new Batfish instance with given base and delta configurations, tempFolder should be
   * present for non-empty configurations
   *
   * @param baseConfigs Map of all Configuration Name -> Configuration Object
   * @param deltaConfigs Map of all Configuration Name -> Configuration Object
   * @param tempFolder Temporary folder to be used to files required for Batfish
   * @return New Batfish instance
   */
  public static Batfish getBatfish(
      @Nonnull SortedMap<String, Configuration> baseConfigs,
      @Nonnull SortedMap<String, Configuration> deltaConfigs,
      @Nonnull TemporaryFolder tempFolder)
      throws IOException {
    return initBatfish(baseConfigs, deltaConfigs, tempFolder);
  }

  public static Batfish getBatfishForTextConfigs(
      TemporaryFolder folder, String... configurationNames) throws IOException {
    SortedMap<String, String> configurationTextMap = new TreeMap<>();
    for (String configName : configurationNames) {
      String configurationText = CommonUtil.readResource(configName);
      configurationTextMap.put(new File(configName).getName(), configurationText);
    }
    return BatfishTestUtils.getBatfishFromTestrigText(
        TestrigText.builder().setConfigurationText(configurationTextMap).build(), folder);
  }

  public static SortedMap<String, Configuration> parseTextConfigs(
      TemporaryFolder folder, String... configurationNames) throws IOException {
    return getBatfishForTextConfigs(folder, configurationNames).loadConfigurations();
  }

  private static void writeTemporaryTestrigFiles(
      @Nullable Map<String, String> filesText, Path outputDirectory) {
    if (filesText != null) {
      filesText.forEach(
          (filename, text) -> {
            outputDirectory.toFile().mkdirs();
            CommonUtil.writeFile(outputDirectory.resolve(filename), text);
          });
    }
  }
}
