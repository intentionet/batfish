package org.batfish.main;

import static org.batfish.common.util.ThrowableMatchers.hasStackTrace;
import static org.batfish.main.Batfish.postProcessInterfaceDependencies;
import static org.batfish.main.Batfish.readAllFiles;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.topology.IpOwners;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer1Node;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.TestQuestion;
import org.batfish.identifiers.AnalysisId;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.QuestionId;
import org.batfish.identifiers.QuestionSettingsId;
import org.batfish.identifiers.TestIdResolver;
import org.batfish.job.ParseVendorConfigurationResult;
import org.batfish.representation.host.HostConfiguration;
import org.batfish.storage.TestStorageProvider;
import org.batfish.vendor.VendorConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link Batfish}. */
public class BatfishTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static final BatfishLogger LOGGER = new BatfishLogger(BatfishLogger.LEVELSTR_INFO, false);

  private static final Question TEST_QUESTION =
      new TestQuestion() {
        @Override
        public String getName() {
          return "blah";
        }
      };

  @Test
  public void testAnswerBadQuestion() {
    // missing class field
    Batfish batfish =
        BatfishTestUtils.getBatfish(
            new TestStorageProvider() {
              @Override
              public String loadQuestion(
                  NetworkId network, QuestionId analysis, AnalysisId question) {
                return "{"
                    + "\"differential\": false,"
                    + "\"instance\": {"
                    + "\"description\": \"Outputs cases where undefined structures (e.g., ACL, routemaps) "
                    + "are referenced.\","
                    + "\"instanceName\": \"undefinedReferences\","
                    + "\"longDescription\": \"Such occurrences indicate configuration errors and can have"
                    + "serious consequences with some vendors.\","
                    + "\"tags\": [\"default\"],"
                    + "\"variables\": {\"nodeRegex\": {"
                    + "\"description\": \"Only check nodes whose name matches this regex\","
                    + "\"type\": \"javaRegex\","
                    + "\"value\": \".*\""
                    + "}}"
                    + "},"
                    + "\"nodeRegex\": \"${nodeRegex}\""
                    + "}";
              }
            },
            new TestIdResolver());
    Answer answer = batfish.answer();
    assertThat(answer.getQuestion(), is(nullValue()));
    assertEquals(answer.getStatus(), AnswerStatus.FAILURE);
    assertEquals(answer.getAnswerElements().size(), 1);
    assertThat(
        answer.getAnswerElements().get(0).toString(), containsString("Could not parse question"));
  }

  @Test
  public void testOverlayIptables() throws IOException {
    SortedMap<String, String> configurationsText = new TreeMap<>();
    String[] configurationNames = new String[] {"host1.cfg"};
    String testConfigsPrefix = "org/batfish/grammar/host/testrigs/router-iptables/configs/";

    SortedMap<String, String> hostsText = new TreeMap<>();
    String[] hostNames = new String[] {"host1.json"};
    String testHostsPrefix = "org/batfish/grammar/host/testrigs/router-iptables/hosts/";

    SortedMap<String, String> iptablesFilesText = new TreeMap<>();
    String[] iptablesNames = new String[] {"host1.iptables"};
    String testIptablesPrefix = "org/batfish/grammar/host/testrigs/router-iptables/iptables/";

    for (String configurationName : configurationNames) {
      String configurationText = CommonUtil.readResource(testConfigsPrefix + configurationName);
      configurationsText.put(configurationName, configurationText);
    }
    for (String hostName : hostNames) {
      String hostText = CommonUtil.readResource(testHostsPrefix + hostName);
      hostsText.put(hostName, hostText);
    }
    for (String iptablesName : iptablesNames) {
      String iptablesText = CommonUtil.readResource(testIptablesPrefix + iptablesName);
      iptablesFilesText.put(iptablesName, iptablesText);
    }
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(configurationsText)
                .setHostsText(hostsText)
                .setIptablesFilesText(iptablesFilesText)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    assertThat(
        configurations.get("host1").getAllInterfaces().get("Ethernet0").getIncomingFilterName(),
        is(notNullValue()));
  }

  @Test
  public void testInitTestrigWithDuplicateHostnames() throws IOException {
    // rtr1 and rtr2 have the same hostname
    String testrigResourcePrefix = "org/batfish/main/snapshots/duplicate_hostnames";
    List<String> configurationNames = ImmutableList.of("rtr1", "rtr2", "rtr3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(testrigResourcePrefix, configurationNames)
                .build(),
            _folder);

    // We should get all three configs, with modified hostnames for the first two
    assertThat(
        batfish.loadConfigurations().keySet(),
        containsInAnyOrder(
            ParseVendorConfigurationResult.getModifiedNameBase("rtr1", "configs/rtr1"),
            ParseVendorConfigurationResult.getModifiedNameBase("rtr1", "configs/rtr2"),
            "rtr3"));

    // hostnames are unique in rtr1 and rtr2
    String testrigResourcePrefix2 = "org/batfish/main/snapshots/duplicate_hostnames2";
    List<String> configurationNames2 = ImmutableList.of("rtr1", "rtr2");

    Batfish batfish2 =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(testrigResourcePrefix2, configurationNames2)
                .build(),
            _folder);

    // we should get only two configs, with real names -- no memory of old duplicates
    assertThat(batfish2.loadConfigurations().keySet(), equalTo(ImmutableSet.of("rtr1", "rtr2")));
  }

  @Test
  public void testInitTestrigWithLayer1Topology() throws IOException {
    String testrigResourcePrefix = "org/batfish/common/topology/testrigs/layer1";
    TestrigText.Builder testrigTextBuilder =
        TestrigText.builder()
            .setLayer1TopologyText(testrigResourcePrefix)
            .setHostsText(testrigResourcePrefix, ImmutableSet.of("c1.json", "c2.json"));
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(testrigTextBuilder.build(), _folder);

    assertThat(
        batfish.getTopologyProvider().getRawLayer3Topology(batfish.getNetworkSnapshot()).getEdges(),
        containsInAnyOrder(Edge.of("c1", "i1", "c2", "i2"), Edge.of("c2", "i2", "c1", "i1")));
  }

  @Test
  public void testInitSnapshotWithRuntimeData() throws IOException {
    /*
    Setup: Config rtr1 has interfaces Ethernet0, Ethernet1, and Ethernet2, all no shutdown.
    Runtime data says Ethernet0 is line down and Ethernet1 is line up, no entry for Ethernet2.
     */
    String snapshotResourcePrefix = "org/batfish/main/snapshots/interface_blacklist";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(snapshotResourcePrefix, "rtr1")
                .setRuntimeDataText(snapshotResourcePrefix)
                .build(),
            _folder);
    Map<String, Interface> interfaces = batfish.loadConfigurations().get("rtr1").getAllInterfaces();

    // Ethernet0 should be inactive and blacklisted
    Interface ethernet0 = interfaces.get("Ethernet0");
    assertTrue(ethernet0.getBlacklisted() && !ethernet0.getActive());

    // Ensure other interfaces are active
    assertThat(
        interfaces.entrySet().stream()
            .filter(e -> e.getValue().getActive())
            .map(Entry::getKey)
            .collect(ImmutableSet.toImmutableSet()),
        containsInAnyOrder("Ethernet1", "Ethernet2"));
  }

  @Test
  public void testLoadLayer1Topology() throws IOException {
    TestrigText.Builder testrigTextBuilder =
        TestrigText.builder().setLayer1TopologyText("org/batfish/common/topology/testrigs/layer1");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(testrigTextBuilder.build(), _folder);
    Layer1Topology layer1Topology =
        batfish
            .getTopologyProvider()
            .getRawLayer1PhysicalTopology(batfish.getNetworkSnapshot())
            .orElse(null);

    Layer1Node c1i1 = new Layer1Node("c1", "i1");
    Layer1Node c2i2 = new Layer1Node("c2", "i2");
    Layer1Node c1i3 = new Layer1Node("c1", "i3");
    Layer1Node c2i4 = new Layer1Node("c2", "i4");
    assertThat(
        layer1Topology.getGraph().edges(),
        equalTo(
            ImmutableSet.of(
                new Layer1Edge(c1i1, c2i2),
                new Layer1Edge(c2i2, c1i1),
                new Layer1Edge(c1i3, c2i4),
                new Layer1Edge(c2i4, c1i3))));
  }

  @Test
  public void testMultipleBestVrrpCandidates() throws IOException {
    String testrigResourcePrefix = "org/batfish/testrigs/vrrp_multiple_best";
    List<String> configurationNames = ImmutableList.of("r1", "r2");

    Ip vrrpAddress = Ip.parse("1.0.0.10");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(testrigResourcePrefix, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = IpOwners.computeIpNodeOwners(configurations, true);
    assertThat(ipOwners.get(vrrpAddress), equalTo(Collections.singleton("r2")));
  }

  @Test
  public void testNoFileUnderPath() throws IOException {
    Path emptyFolder = _folder.newFolder("emptyFolder").toPath();
    Map<Path, String> result = readAllFiles(emptyFolder, LOGGER);
    assertThat(result, anEmptyMap());
  }

  @Test
  public void testCheckValidTopology() {
    Map<String, Configuration> configs = new HashMap<>();
    configs.put(
        "h1", BatfishTestUtils.createTestConfiguration("h1", ConfigurationFormat.HOST, "eth0"));
    configs.put(
        "h2", BatfishTestUtils.createTestConfiguration("h2", ConfigurationFormat.HOST, "e0"));
    SortedSet<Edge> edges =
        new TreeSet<>(Collections.singletonList(Edge.of("h1", "eth0", "h2", "e0")));
    Topology topology = new Topology(edges);

    // test that checkTopology does not throw
    Batfish.checkTopology(configs, topology);
  }

  @Test
  public void testCheckTopologyInvalidNode() {
    Map<String, Configuration> configs = new HashMap<>();
    configs.put(
        "h1", BatfishTestUtils.createTestConfiguration("h1", ConfigurationFormat.HOST, "eth0"));
    SortedSet<Edge> edges =
        new TreeSet<>(Collections.singletonList(Edge.of("h1", "eth0", "h2", "e0")));
    Topology topology = new Topology(edges);

    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("Topology contains a non-existent node 'h2'");
    Batfish.checkTopology(configs, topology);
  }

  @Test
  public void testCheckTopologyInvalidInterface() {
    Map<String, Configuration> configs = new HashMap<>();
    configs.put(
        "h1", BatfishTestUtils.createTestConfiguration("h1", ConfigurationFormat.HOST, "eth0"));
    configs.put(
        "h2", BatfishTestUtils.createTestConfiguration("h2", ConfigurationFormat.HOST, "e0"));
    SortedSet<Edge> edges =
        new TreeSet<>(Collections.singletonList(Edge.of("h1", "eth1", "h2", "e0")));
    Topology topology = new Topology(edges);

    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("Topology contains a non-existent interface 'eth1' on node 'h1'");
    Batfish.checkTopology(configs, topology);
  }

  @Test
  public void testReadMissingIptableFile() throws IOException {
    HostConfiguration host1 = new HostConfiguration();
    String filename = Paths.get("iptables", "host1.iptables").toString();
    host1.setHostname("host1");
    host1.setIptablesFile(filename);
    SortedMap<String, VendorConfiguration> hostConfigurations = new TreeMap<>();
    hostConfigurations.put("host1", host1);
    SortedMap<Path, String> iptablesData = new TreeMap<>();
    Path testRigPath = _folder.newFolder("testrig").toPath();
    ParseVendorConfigurationAnswerElement answerElement =
        new ParseVendorConfigurationAnswerElement();
    answerElement.getParseStatus().put("configs/host1.cfg", ParseStatus.PASSED);
    Batfish batfish = BatfishTestUtils.getBatfish(new TreeMap<>(), _folder);
    String failureMessage =
        "Iptables file iptables"
            + File.separator
            + "host1.iptables for host host1 is not contained within the testrig";
    batfish.readIptableFiles(testRigPath, hostConfigurations, iptablesData, answerElement);
    assertThat(
        answerElement.getParseStatus().get(Paths.get("iptables", "host1.iptables").toString()),
        equalTo(ParseStatus.FAILED));
    assertThat(answerElement.getErrors().get("host1").toString(), containsString(failureMessage));
    // When host file failed, verify that error message contains both failure messages
    answerElement.getErrors().clear();
    answerElement
        .getErrors()
        .put(
            "host1",
            new BatfishException("Failed to parse host file: host1").getBatfishStackTrace());
    batfish.readIptableFiles(testRigPath, hostConfigurations, iptablesData, answerElement);
    assertThat(answerElement.getErrors().get("host1").toString(), containsString(failureMessage));
    assertThat(
        answerElement.getErrors().get("host1").toString(),
        containsString("Failed to parse host file: host1"));
    // When the haltonparseerror flag is set to true
    batfish.getSettings().setHaltOnParseError(true);
    answerElement.getErrors().clear();
    String parseErrorMessage =
        "Fatal exception due to at least one Iptables file is not contained"
            + " within the testrig";
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage(parseErrorMessage);
    batfish.readIptableFiles(testRigPath, hostConfigurations, iptablesData, answerElement);
  }

  @Test
  public void testReadNestedPath() throws IOException {
    Path nestedFolder = _folder.newFolder("nestedDirectory").toPath();
    Set<Path> expected = new HashSet<>();
    expected.add(nestedFolder.resolve("b-test.cfg"));
    expected.add(nestedFolder.resolve("d-test.cfg"));
    expected.add(nestedFolder.resolve("aDirectory").resolve("e-test.cfg"));
    expected.add(nestedFolder.resolve("eDirectory").resolve("a-test.cfg"));
    expected.add(nestedFolder.resolve("eDirectory").resolve("c-test.cfg"));
    for (Path path : expected) {
      path.getParent().toFile().mkdir();
      assertThat(path.toFile().createNewFile(), is(true));
    }
    Set<Path> actual = readAllFiles(nestedFolder, LOGGER).keySet();
    assertThat(expected, equalTo(actual));
  }

  @Test
  public void testReadStartWithDotFile() throws IOException {
    Path startWithDot = _folder.newFolder("startWithDot").toPath();
    File file = startWithDot.resolve(".cfg").toFile();
    file.getParentFile().mkdir();
    assertThat(file.createNewFile(), is(true));
    Map<Path, String> result = readAllFiles(startWithDot, LOGGER);
    assertThat(result, anEmptyMap());
  }

  @Test
  public void testReadUnNestedPath() throws IOException {
    Path unNestedFolder = _folder.newFolder("unNestedDirectory").toPath();
    Set<Path> expected = new HashSet<>();
    expected.add(unNestedFolder.resolve("test1.cfg"));
    expected.add(unNestedFolder.resolve("test2.cfg"));
    expected.add(unNestedFolder.resolve("test3.cfg"));
    for (Path path : expected) {
      path.getParent().toFile().mkdir();
      assertThat(path.toFile().createNewFile(), is(true));
    }
    Set<Path> actual = readAllFiles(unNestedFolder, LOGGER).keySet();
    assertThat(expected, equalTo(actual));
  }

  @Test
  public void testReadThrowsExceptionWithSpecificType() {
    Path nonExistPath = _folder.getRoot().toPath().resolve("nonExistent");
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("Failed to walk path: " + nonExistPath);
    readAllFiles(nonExistPath, LOGGER);
  }

  @Test
  public void testUnusableVrrpHandledCorrectly() throws Exception {
    String configurationText =
        String.join(
            "\n",
            new String[] {
              "hostname host1", "!", "interface Vlan65", "   vrrp 1 ip 1.2.3.4", "!",
            });
    SortedMap<String, String> configMap = ImmutableSortedMap.of("host1", configurationText);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder().setConfigurationText(configMap).build(), _folder);
    Map<String, Configuration> configs = batfish.loadConfigurations();

    // Assert that the config parsed successfully
    assertThat(configs, hasKey("host1"));
    assertThat(configs.get("host1").getAllInterfaces(), hasKey("Vlan65"));
    assertThat(
        configs.get("host1").getAllInterfaces().get("Vlan65").getVrrpGroups().keySet(), hasSize(1));

    // Tests that computing IP owners with such a bad interface does not crash.
    IpOwners.computeIpNodeOwners(configs, false);
  }

  @Test
  public void testReadValidIptableFile() throws IOException {
    HostConfiguration host1 = new HostConfiguration();
    host1.setHostname("host1");
    Path iptablePath = Paths.get("iptables").resolve("host1.iptables");
    host1.setIptablesFile(iptablePath.toString());
    SortedMap<String, VendorConfiguration> hostConfigurations = new TreeMap<>();
    hostConfigurations.put("host1", host1);
    SortedMap<Path, String> iptablesData = new TreeMap<>();
    Path testRigPath = _folder.newFolder("testrig").toPath();
    File iptableFile = Paths.get(testRigPath.toString(), iptablePath.toString()).toFile();
    iptableFile.getParentFile().mkdir();
    assertThat(iptableFile.createNewFile(), is(true));
    ParseVendorConfigurationAnswerElement answerElement =
        new ParseVendorConfigurationAnswerElement();
    answerElement.getParseStatus().put("host1", ParseStatus.PASSED);
    Batfish batfish = BatfishTestUtils.getBatfish(new TreeMap<>(), _folder);
    batfish.readIptableFiles(testRigPath, hostConfigurations, iptablesData, answerElement);
    assertThat(answerElement.getParseStatus().get("host1"), equalTo(ParseStatus.PASSED));
    assertThat(answerElement.getErrors().size(), is(0));
  }

  @Test
  public void testLoadQuestionSettingsPresent() {
    String questionSettings = "{}";

    Batfish batfish =
        BatfishTestUtils.getBatfish(
            new TestStorageProvider() {
              @Override
              public String loadQuestionSettings(
                  NetworkId network, QuestionSettingsId questionSettingsId) {
                return questionSettings;
              }
            },
            new TestIdResolver() {
              @Override
              public boolean hasQuestionSettingsId(String questionClassId, NetworkId networkId) {
                return true;
              }

              @Override
              public QuestionSettingsId getQuestionSettingsId(
                  String questionClassId, NetworkId networkId) {
                return new QuestionSettingsId("blah");
              }
            });

    assertThat(batfish.loadQuestionSettings(TEST_QUESTION), equalTo(questionSettings));
  }

  @Test
  public void testLoadQuestionSettingsAbsent() {
    Batfish batfish =
        BatfishTestUtils.getBatfish(
            new TestStorageProvider() {
              @Override
              public String loadQuestionSettings(
                  NetworkId networkId, QuestionSettingsId questionSettingsId) {
                return null;
              }
            },
            new TestIdResolver() {
              @Override
              public boolean hasQuestionSettingsId(String questionClassId, NetworkId networkId) {
                return false;
              }
            });

    assertThat(batfish.loadQuestionSettings(TEST_QUESTION), nullValue());
  }

  @Test
  public void testLoadQuestionSettingsError() {
    Batfish batfish =
        BatfishTestUtils.getBatfish(
            new TestStorageProvider() {
              @Override
              public String loadQuestionSettings(
                  NetworkId networkId, QuestionSettingsId questionSettingsId) throws IOException {
                throw new IOException("simulated error");
              }
            },
            new TestIdResolver() {
              @Override
              public QuestionSettingsId getQuestionSettingsId(
                  String questionClassId, NetworkId networkId) {
                return new QuestionSettingsId("foo");
              }

              @Override
              public boolean hasQuestionSettingsId(String questionClassId, NetworkId networkId) {
                return true;
              }
            });

    _thrown.expect(BatfishException.class);
    assertThat(batfish.loadQuestionSettings(TEST_QUESTION), nullValue());
  }

  @Test
  public void testCreateAnswerer() {
    Batfish batfish = BatfishTestUtils.getBatfish(new TestStorageProvider(), new TestIdResolver());
    Question testQuestion =
        new TestQuestion() {
          @Override
          public String getName() {
            return "q1";
          }
        };
    Question testQuestionMissing =
        new TestQuestion() {
          @Override
          public String getName() {
            return "q2";
          }
        };
    Answerer testAnswerer =
        new Answerer(testQuestion, batfish) {
          @Override
          public AnswerElement answer() {
            throw new UnsupportedOperationException("no implementation for generated method");
          }
        };

    batfish.registerAnswerer("q1", "q1ClassName", (q, b) -> testAnswerer);

    // should get the answerer the creator supplies
    assertThat(batfish.createAnswerer(testQuestion), equalTo(testAnswerer));

    // should get null answerer if no creator available
    assertThat(batfish.createAnswerer(testQuestionMissing), nullValue());
  }

  // all of these interfaces should not be ignored by processManagementInterfaces()
  @Test
  public void testNotIgnoredManagementInterfaces() {
    String notIgnored = "notIgnored";
    String notIgnored2 = "them0";
    String notIgnored3 = "mgt-me0";
    String notIgnored4 = "manage";
    String notIgnored5 = "Afxp0";

    Map<String, Configuration> configs = new HashMap<>();
    Configuration config1 =
        BatfishTestUtils.createTestConfiguration(
            "config1",
            ConfigurationFormat.HOST,
            notIgnored,
            notIgnored2,
            notIgnored3,
            notIgnored4,
            notIgnored5);
    config1.getAllInterfaces().get(notIgnored).setVrfName("vrf-mgmt");
    config1.getAllInterfaces().get(notIgnored2).setVrfName("Manageme");
    config1.getAllInterfaces().get(notIgnored3).setVrfName("fxp0");
    configs.put("config1", config1);

    Batfish.processManagementInterfaces(configs);

    // all of the interfaces should still be active
    assertThat(
        config1.activeInterfaceNames(),
        equalTo(ImmutableSet.of(notIgnored, notIgnored2, notIgnored3, notIgnored4, notIgnored5)));
  }

  // all of these interfaces should be ignored by processManagementInterfaces()
  @Test
  public void testIgnoredManagementInterfaces() {
    String ignoredIface1 = "ignoredIface1";
    String ignoredIface2 = "ignoredIface2";

    Map<String, Configuration> configs = new HashMap<>();
    Configuration config1 =
        BatfishTestUtils.createTestConfiguration(
            "config1",
            ConfigurationFormat.HOST,
            ignoredIface1,
            ignoredIface2,
            "mgmt0",
            "Management",
            "fxp0-0",
            "em0.0",
            "me0.10",
            "vme",
            "vme.0");
    config1.getAllInterfaces().get(ignoredIface1).setVrfName("Mgmt-intf");
    config1.getAllInterfaces().get(ignoredIface2).setVrfName("ManagementVrf");
    configs.put("config1", config1);

    Batfish.processManagementInterfaces(configs);

    // none of the interfaces should be active
    assertThat(config1.activeInterfaceNames(), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testPostProcessInterfaceDependenciesBind() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c1 =
        nf.configurationBuilder()
            .setHostname("c1")
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();
    Vrf vrf = nf.vrfBuilder().setOwner(c1).setName(Configuration.DEFAULT_VRF_NAME).build();

    Interface.Builder ib = nf.interfaceBuilder().setOwner(c1).setVrf(vrf);

    ib.setName("eth0").setActive(false).build();
    ib.setName("eth1")
        .setActive(true)
        .setDependencies(ImmutableSet.of(new Dependency("eth0", DependencyType.BIND)))
        .build();
    ib.setName("eth2")
        .setActive(true)
        .setDependencies(ImmutableSet.of(new Dependency("eth1", DependencyType.BIND)))
        .build();
    ib.setName("eth9").setActive(true).build();

    ImmutableSet<String> activeIfaces = ImmutableSet.of("eth9");
    ImmutableSet<String> inactiveIfaces = ImmutableSet.of("eth0", "eth1", "eth2");

    // Test
    postProcessInterfaceDependencies(ImmutableMap.of("c1", c1));

    activeIfaces.forEach(
        name -> assertThat(c1.getAllInterfaces().get(name).getActive(), equalTo(true)));
    inactiveIfaces.forEach(
        name -> assertThat(c1.getAllInterfaces().get(name).getActive(), equalTo(false)));
  }

  @Test
  public void testPostProcessInterfaceDependenciesAggregate() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c1 =
        nf.configurationBuilder()
            .setHostname("c1")
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();
    Vrf vrf = nf.vrfBuilder().setOwner(c1).setName(Configuration.DEFAULT_VRF_NAME).build();

    Interface.Builder ib = nf.interfaceBuilder().setOwner(c1).setVrf(vrf);

    ib.setName("eth0").setActive(false).setType(InterfaceType.PHYSICAL).build();
    ib.setName("eth1").setActive(true).setType(InterfaceType.PHYSICAL).build();
    ib.setName("eth2")
        .setActive(true)
        .setType(InterfaceType.AGGREGATED)
        .setDependencies(
            ImmutableSet.of(
                new Dependency("eth1", DependencyType.AGGREGATE),
                new Dependency("eth0", DependencyType.AGGREGATE)))
        .build();

    ib.setName("eth3").setActive(false).setType(InterfaceType.PHYSICAL).build();
    ib.setName("eth4")
        .setActive(true)
        .setType(InterfaceType.AGGREGATED)
        .setDependencies(
            ImmutableSet.of(
                new Dependency("eth0", DependencyType.AGGREGATE),
                new Dependency("eth3", DependencyType.AGGREGATE)))
        .build();

    ImmutableSet<String> activeIfaces = ImmutableSet.of("eth1", "eth2");
    ImmutableSet<String> inactiveIfaces = ImmutableSet.of("eth0", "eth3", "eth4");

    // Test
    postProcessInterfaceDependencies(ImmutableMap.of("c1", c1));

    activeIfaces.forEach(
        name -> assertThat(c1.getAllInterfaces().get(name).getActive(), equalTo(true)));
    inactiveIfaces.forEach(
        name -> assertThat(c1.getAllInterfaces().get(name).getActive(), equalTo(false)));
  }

  @Test
  public void testHaltOnParseError() throws IOException {
    String hostname = "r1";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    ImmutableMap.of(
                        hostname,
                        "!RANCID-CONTENT-TYPE: cisco\nhostname r1\ntotally-invalid-text\n"))
                .build(),
            _folder);
    batfish.getSettings().setHaltOnParseError(true);
    _thrown.expect(hasStackTrace(containsString("Error parsing configuration file")));
    batfish.loadConfigurations();
  }

  @Test
  public void testGetSnapshotInputObject() throws IOException {
    String fileName = "fileName";
    String configText = "sup dawg";

    Map<String, String> configurations = ImmutableMap.of(fileName, configText);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder().setConfigurationText(configurations).build(), _folder);

    // returns the text of the config if it exists
    assertThat(batfish.getSnapshotInputObject("configs/" + fileName), equalTo(configText));
  }

  @Test
  public void testGetSnapshotInputObjectError() throws IOException {
    String fileName = "fileName";
    String configText = "sup dawg";

    Map<String, String> configurations = ImmutableMap.of(fileName, configText);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder().setConfigurationText(configurations).build(), _folder);

    // should throw FileNotFoundException if file not found
    _thrown.expect(FileNotFoundException.class);
    batfish.getSnapshotInputObject("missing file");
  }
}
