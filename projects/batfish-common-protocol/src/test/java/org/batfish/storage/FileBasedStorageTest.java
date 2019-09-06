package org.batfish.storage;

import static org.batfish.storage.FileBasedStorage.mkdirs;
import static org.batfish.storage.FileBasedStorage.objectKeyToRelativePath;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.io.FileMatchers.anExistingDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.CompletionMetadata;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.UnzipUtility;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.MajorIssueConfig;
import org.batfish.datamodel.answers.MinorIssueConfig;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.isp_configuration.BorderInterfaceInfo;
import org.batfish.datamodel.isp_configuration.IspConfiguration;
import org.batfish.datamodel.isp_configuration.IspFilter;
import org.batfish.identifiers.IssueSettingsId;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.QuestionSettingsId;
import org.batfish.identifiers.SnapshotId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class FileBasedStorageTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private Path _containerDir;
  private BatfishLogger _logger;
  private FileBasedStorage _storage;

  @Before
  public void before() throws IOException {
    _containerDir = _folder.newFolder("container").toPath();
    _logger = new BatfishLogger(BatfishLogger.LEVELSTR_DEBUG, false);
    _storage =
        new FileBasedStorage(_containerDir.getParent(), _logger, (m, n) -> new AtomicInteger());
  }

  @Test
  public void roundTripConfigurationsSucceeds() {
    NetworkId network = new NetworkId("network");
    SnapshotId snapshot = new SnapshotId("snapshot");

    Map<String, Configuration> configs = new HashMap<>();
    configs.put("node1", new Configuration("node1", ConfigurationFormat.CISCO_IOS));

    _storage.storeConfigurations(
        configs, new ConvertConfigurationAnswerElement(), network, snapshot);
    Map<String, Configuration> deserialized = _storage.loadConfigurations(network, snapshot);
    assertThat(deserialized, not(nullValue()));
    assertThat(deserialized.keySet(), equalTo(Sets.newHashSet("node1")));
  }

  @Test
  public void loadMissingConfigurationsReturnsNull() {
    assertThat(
        _storage.loadConfigurations(new NetworkId("nonexistent"), new SnapshotId("nonexistent")),
        nullValue());
  }

  @Test
  public void testLoadIspConfiguration() throws IOException {
    NetworkId networkId = new NetworkId("network");
    SnapshotId snapshotId = new SnapshotId("snapshot");

    Path batfishConfigDir =
        _storage
            .getDirectoryProvider()
            .getSnapshotInputObjectsDir(networkId, snapshotId)
            .resolve(BfConsts.RELPATH_BATFISH_CONFIGS_DIR);
    final boolean mkdirs = batfishConfigDir.toFile().mkdirs();
    assertThat(mkdirs, equalTo(true));

    IspConfiguration ispConfiguration =
        new IspConfiguration(
            ImmutableList.of(new BorderInterfaceInfo(NodeInterfacePair.of("node", "interface"))),
            new IspFilter(
                ImmutableList.of(1L, 2L),
                ImmutableList.of(Ip.parse("1.1.1.1"), Ip.parse("2.2.2.2"))));
    BatfishObjectMapper.mapper()
        .writeValue(
            batfishConfigDir.resolve(BfConsts.RELPATH_ISP_CONFIG_FILE).toFile(), ispConfiguration);

    IspConfiguration readIspConfiguration = _storage.loadIspConfiguration(networkId, snapshotId);
    assertThat(ispConfiguration, equalTo(readIspConfiguration));
  }

  @Test
  public void testMajorIssueConfigRoundTrip() throws IOException {
    String majorIssue = "majorIssue";
    IssueSettingsId issueSettingsId = new IssueSettingsId("issueSettingsId");
    NetworkId network = new NetworkId("network");
    MinorIssueConfig minorIssueConfig = new MinorIssueConfig("minorIssue", 100, "www.google.com");
    MajorIssueConfig majorIssueConfig =
        new MajorIssueConfig(majorIssue, ImmutableList.of(minorIssueConfig));

    _storage.storeMajorIssueConfig(network, issueSettingsId, majorIssueConfig);
    assertThat(_storage.loadMajorIssueConfig(network, issueSettingsId), equalTo(majorIssueConfig));
  }

  @Test
  public void testLoadMissingMajorIssueConfig() {
    IssueSettingsId majorIssue = new IssueSettingsId("majorIssue");
    NetworkId network = new NetworkId("network");
    assertThat(_storage.loadMajorIssueConfig(network, majorIssue), nullValue());
  }

  @Test
  public void testStoreQuestionSettingsThenLoad() throws IOException {
    NetworkId network = new NetworkId("network");
    QuestionSettingsId questionSettingsId = new QuestionSettingsId("q1");
    String settings = "{}";
    _storage.storeQuestionSettings(settings, network, questionSettingsId);

    assertThat(_storage.loadQuestionSettings(network, questionSettingsId), equalTo(settings));
  }

  @Test
  public void testLoadQuestionSettingsMissing() throws IOException {
    NetworkId network = new NetworkId("network");
    QuestionSettingsId questionSettingsId = new QuestionSettingsId("q1");

    assertThat(_storage.loadQuestionSettings(network, questionSettingsId), nullValue());
  }

  @Test
  public void testCheckNetworkExistsTrue() {
    NetworkId network = new NetworkId("network");
    _storage.getDirectoryProvider().getNetworkDir(network).toFile().mkdirs();

    assertThat(_storage.checkNetworkExists(network), equalTo(true));
  }

  @Test
  public void testCheckNetworkExistsFalse() {
    NetworkId network = new NetworkId("network");

    assertThat(_storage.checkNetworkExists(network), equalTo(false));
  }

  @Test
  public void testObjectKeyToRelativePathRejectsAbsolute() {
    _thrown.expect(IllegalArgumentException.class);
    objectKeyToRelativePath("/foo/bar");
  }

  @Test
  public void testObjectKeyToRelativePathRejectsNonNormalized() {
    _thrown.expect(IllegalArgumentException.class);
    objectKeyToRelativePath("foo/../../bar");
  }

  @Test
  public void testObjectKeyToRelativePathValid() {
    // no exception should be thrown
    objectKeyToRelativePath("foo/bar");
  }

  @Test
  public void testLoadWorkLog() throws IOException {
    // setup: pretend a worker logger has written a file
    NetworkId network = new NetworkId("network");
    SnapshotId snapshot = new SnapshotId("snapshot");
    Path dir = _storage.getDirectoryProvider().getSnapshotOutputDir(network, snapshot);
    final boolean mkdirs = dir.toFile().mkdirs();
    assertThat(mkdirs, equalTo(true));
    CommonUtil.writeFile(dir.resolve("workid.log"), "testoutput");

    // Test: read log using storage API
    assertThat(_storage.loadWorkLog(network, snapshot, "workid"), equalTo("testoutput"));
  }

  @Test
  public void testLoadWorkLogMissing() throws IOException {
    // setup: pretend a worker logger has written a file
    NetworkId network = new NetworkId("network");
    SnapshotId snapshot = new SnapshotId("snapshot");

    _thrown.expect(FileNotFoundException.class);
    assertThat(_storage.loadWorkLog(network, snapshot, "workid"), equalTo("testoutput"));
  }

  @Test
  public void testMkdirs() throws IOException {
    Path dir = _folder.newFolder().toPath().resolve("parentDir").resolve("subDir");

    // Confirm mkdirs creates the non-existent dir
    mkdirs(dir);
    assertThat(dir.toFile(), anExistingDirectory());
  }

  /**
   * Run multiple threads trying to create the same dir, many times. Goal here is for mkdirs to not
   * throw an exception.
   */
  @Test
  public void testMkdirsConcurrency() throws Exception {
    int numThreads = 2;
    // Try many times, since false negatives are possible
    int numTries = 100;

    final Path dir = _folder.newFolder().toPath().resolve("testDir");
    final CyclicBarrier barrier = new CyclicBarrier(numThreads);
    final AtomicInteger exceptions = new AtomicInteger(0);
    List<Thread> threads = new ArrayList<>();

    for (int i = 0; i < numTries; i++) {
      for (int j = 0; j < numThreads; j++) {
        Thread thread =
            new Thread(
                () -> {
                  try {
                    // Wait until all threads are at the barrier
                    barrier.await();
                    mkdirs(dir);
                  } catch (Exception e) {
                    // Track exceptions with int since they are not directly surfaced
                    exceptions.addAndGet(1);
                    throw new BatfishException(e.getMessage());
                  }
                });
        threads.add(thread);
        thread.start();
      }
      for (Thread thread : threads) {
        thread.join();
      }
      Files.delete(dir);
    }

    // Confirm mkdirs runs successfully even with multiple concurrent calls
    assertThat(exceptions.get(), equalTo(0));
  }

  @Test
  public void testMkdirsExists() throws IOException {
    Path dir = _folder.newFolder().toPath();

    // Confirm mkdirs succeeds when the dir already exists
    mkdirs(dir);
    assertThat(dir.toFile(), anExistingDirectory());
  }

  @Test
  public void testMkdirsFail() throws IOException {
    File parentDir = _folder.newFolder();
    parentDir.setReadOnly();
    Path dir = parentDir.toPath().resolve("testDir");

    // Confirm mkdirs throws when creating a dir within a read-only dir
    _thrown.expectMessage(containsString("Unable to create directory"));
    mkdirs(dir);
  }

  @Test
  public void testNetworkBlobRoundTrip() throws IOException {
    NetworkId network = new NetworkId("network");
    String id = "heresanid";

    Boolean found = null;
    try {
      _storage.loadNetworkBlob(network, id);
      found = true;
    } catch (FileNotFoundException e) {
      found = false;
    }
    assertThat("Should have been set", found, notNullValue());
    assertFalse("Should not have been found", found);

    byte[] content = "here's some content".getBytes(StandardCharsets.UTF_8);
    _storage.storeNetworkBlob(new ByteArrayInputStream(content), network, id);

    byte[] loaded = ByteStreams.toByteArray(_storage.loadNetworkBlob(network, id));
    assertThat(content, equalTo(loaded));
  }

  @Test
  public void testLoadSnapshotInputObjectFile() throws IOException {
    NetworkId network = new NetworkId("network");
    SnapshotId snapshot = new SnapshotId("snapshot");
    String testSting = "what is life";

    FileUtils.copyInputStreamToFile(
        new ByteArrayInputStream(testSting.getBytes()),
        _storage.getSnapshotInputObjectPath(network, snapshot, "test").toFile());

    try (InputStream inputStream = _storage.loadSnapshotInputObject(network, snapshot, "test")) {
      assertThat(IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()), equalTo(testSting));
    }
  }

  @Test
  public void testLoadSnapshotInputObjectDirectory() throws IOException {
    NetworkId network = new NetworkId("network");
    SnapshotId snapshot = new SnapshotId("snapshot");
    String testSting = "this is life";

    Path testdir = _storage.getSnapshotInputObjectPath(network, snapshot, "testkey");
    testdir.toFile().mkdirs();
    Files.write(testdir.resolve("testfile"), testSting.getBytes());

    Path tmpzip = _folder.getRoot().toPath().resolve("tmp.zip");
    try (InputStream inputStream = _storage.loadSnapshotInputObject(network, snapshot, "testkey")) {
      FileUtils.copyInputStreamToFile(inputStream, tmpzip.toFile());
    }

    Path unzipDir = _folder.getRoot().toPath().resolve("tmp");
    UnzipUtility.unzip(tmpzip, unzipDir);

    // the top level entry in the zip should be testkey
    String[] toplevel = unzipDir.toFile().list();
    assertThat(toplevel, equalTo(new String[] {"testkey"}));

    // then, there should be testfile
    String[] secondlevel = unzipDir.resolve(toplevel[0]).toFile().list();
    assertThat(secondlevel, equalTo(new String[] {"testfile"}));

    // the content of the testfile should match what we wrote
    assertThat(
        new String(
            Files.readAllBytes(unzipDir.resolve(toplevel[0]).resolve(secondlevel[0])),
            StandardCharsets.UTF_8),
        equalTo(testSting));
  }

  @Test
  public void testGetSnapshotInputKeysNonExistentInput() throws IOException {
    NetworkId network = new NetworkId("network");
    SnapshotId snapshot = new SnapshotId("snapshot");
    _thrown.expect(FileNotFoundException.class);
    _storage.getSnapshotInputObjectsMetadata(network, snapshot);
  }

  @Test
  public void testGetSnapshotInputKeys() throws IOException {
    NetworkId network = new NetworkId("network");
    SnapshotId snapshot = new SnapshotId("snapshot");

    String dir1 = "dir1";
    String dir2 = "dir2";

    Path dir1Path = _storage.getSnapshotInputObjectPath(network, snapshot, dir1);
    dir1Path.toFile().mkdirs();

    Path dir2Path = _storage.getSnapshotInputObjectPath(network, snapshot, dir2);
    dir2Path.toFile().mkdirs();

    String file1 = "file1";
    String file1Contents = "some content";
    Files.write(dir1Path.resolve(file1), file1Contents.getBytes());

    String file2 = "file2";
    String file2Contents = "some other content";
    Files.write(dir2Path.resolve(file2), file2Contents.getBytes());

    List<StoredObjectMetadata> keys = _storage.getSnapshotInputObjectsMetadata(network, snapshot);
    assertThat(
        keys.stream().collect(ImmutableSet.toImmutableSet()),
        equalTo(
            ImmutableSet.of(
                new StoredObjectMetadata(dir2 + "/" + file2, file2Contents.getBytes().length),
                new StoredObjectMetadata(dir1 + "/" + file1, file1Contents.getBytes().length))));
  }

  @Test
  public void testGetSnapshotExtendedObjectsMetadataNonExistentInput() throws IOException {
    NetworkId network = new NetworkId("network");
    SnapshotId snapshot = new SnapshotId("snapshot");
    _thrown.expect(FileNotFoundException.class);
    _storage.getSnapshotExtendedObjectsMetadata(network, snapshot);
  }

  @Test
  public void testGetSnapshotExtendedObjectsMetadata() throws IOException {
    NetworkId network = new NetworkId("network");
    SnapshotId snapshot = new SnapshotId("snapshot");

    String key1 = "foo/bar";
    String key2 = "bat/fish";
    String content1 = "some content";
    String content2 = "some other content";

    InputStream inputStream1 = new ByteArrayInputStream(content1.getBytes());
    InputStream inputStream2 = new ByteArrayInputStream(content2.getBytes());

    _storage.storeSnapshotObject(inputStream1, network, snapshot, key1);
    _storage.storeSnapshotObject(inputStream2, network, snapshot, key2);

    List<StoredObjectMetadata> keys =
        _storage.getSnapshotExtendedObjectsMetadata(network, snapshot);
    assertThat(
        keys,
        containsInAnyOrder(
            new StoredObjectMetadata(key1, content1.getBytes().length),
            new StoredObjectMetadata(key2, content2.getBytes().length)));
  }

  @Test
  public void testCompletionMetadataRoundtrip() throws IOException {
    NetworkId networkId = new NetworkId("network");
    SnapshotId snapshotId = new SnapshotId("snapshot");

    CompletionMetadata completionMetadata =
        new CompletionMetadata(
            ImmutableSet.of("filter1"),
            ImmutableSet.of(NodeInterfacePair.of("node", "iface")),
            ImmutableSet.of("1.1.1.1"),
            ImmutableSet.of("mlag"),
            ImmutableSet.of("node"),
            ImmutableSet.of("1.1.1.1/30"),
            ImmutableSet.of("routing1"),
            ImmutableSet.of("structure1"),
            ImmutableSet.of("vrf1"),
            ImmutableSet.of("zone1"));

    _storage.storeCompletionMetadata(completionMetadata, networkId, snapshotId);

    assertThat(_storage.loadCompletionMetadata(networkId, snapshotId), equalTo(completionMetadata));
  }

  @Test
  public void testLoadCompletionMetadataMissing() throws IOException {
    NetworkId networkId = new NetworkId("network");
    SnapshotId snapshotId = new SnapshotId("snapshot");

    // if CompletionMetadata file is missing, should return a CompletionMetadata object with all
    // fields empty
    assertThat(
        _storage.loadCompletionMetadata(networkId, snapshotId), equalTo(CompletionMetadata.EMPTY));
  }

  @Test
  public void testStoreLayer2TopologyMissing() throws IOException {
    NetworkSnapshot networkSnapshot =
        new NetworkSnapshot(new NetworkId("network"), new SnapshotId("snapshot"));
    _storage.storeLayer2Topology(Optional.empty(), networkSnapshot);

    assertEquals(_storage.loadLayer2Topology(networkSnapshot), Optional.empty());
  }

  @Test
  public void testStoreLayer2TopologyPresent() throws IOException {
    NetworkSnapshot networkSnapshot =
        new NetworkSnapshot(new NetworkId("network"), new SnapshotId("snapshot"));
    _storage.storeLayer2Topology(Optional.of(Layer2Topology.EMPTY), networkSnapshot);

    assertEquals(_storage.loadLayer2Topology(networkSnapshot), Optional.of(Layer2Topology.EMPTY));
  }
}
