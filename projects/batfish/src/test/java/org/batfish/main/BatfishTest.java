package org.batfish.main;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.batfish.common.BatfishException;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer1Node;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.representation.host.HostConfiguration;
import org.batfish.vendor.VendorConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link Batfish}. */
public class BatfishTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testAnswerBadQuestion() throws IOException {
    // missing class field
    String badQuestionStr =
        "{"
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

    Path questionPath = _folder.newFile("testAnswerBadQuestion").toPath();
    Files.write(questionPath, badQuestionStr.getBytes(StandardCharsets.UTF_8));
    Batfish batfish = BatfishTestUtils.getBatfish(new TreeMap<>(), _folder);
    batfish.getSettings().setQuestionPath(questionPath);
    Answer answer = batfish.answer();
    assertThat(answer.getQuestion(), is(nullValue()));
    assertEquals(answer.getStatus(), AnswerStatus.FAILURE);
    assertEquals(answer.getAnswerElements().size(), 1);
    assertThat(
        answer.getAnswerElements().get(0).prettyPrint(),
        containsString("Could not parse question"));
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
        configurations.get("host1").getInterfaces().get("Ethernet0").getIncomingFilterName(),
        is(notNullValue()));
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
        batfish.computeTestrigTopology(batfish.loadConfigurations()).getEdges(),
        containsInAnyOrder(new Edge("c1", "i1", "c2", "i2"), new Edge("c2", "i2", "c1", "i1")));
  }

  @Test
  public void testInitTestrigWithLegacyTopology() throws IOException {
    String testrigResourcePrefix = "org/batfish/common/topology/testrigs/legacy";
    TestrigText.Builder testrigTextBuilder =
        TestrigText.builder()
            .setLegacyTopologyText("org/batfish/common/topology/testrigs/legacy")
            .setHostsText(testrigResourcePrefix, ImmutableSet.of("c1.json", "c2.json"));
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(testrigTextBuilder.build(), _folder);

    assertThat(
        batfish.computeTestrigTopology(batfish.loadConfigurations()).getEdges(),
        containsInAnyOrder(new Edge("c1", "i1", "c2", "i2"), new Edge("c2", "i2", "c1", "i1")));
  }

  @Test
  public void testLoadLayer1Topology() throws IOException {
    TestrigText.Builder testrigTextBuilder =
        TestrigText.builder().setLayer1TopologyText("org/batfish/common/topology/testrigs/layer1");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(testrigTextBuilder.build(), _folder);
    Layer1Topology layer1Topology = batfish.getLayer1Topology();

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
    String testrigResourcePrefix = "org/batfish/grammar/cisco/testrigs/vrrp_multiple_best";
    List<String> configurationNames = ImmutableList.of("r1", "r2");

    Ip vrrpAddress = new Ip("1.0.0.10");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(testrigResourcePrefix, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
    assertThat(ipOwners.get(vrrpAddress), equalTo(Collections.singleton("r2")));
  }

  @Test
  public void testNoFileUnderPath() throws IOException {
    Path emptyFolder = _folder.newFolder("emptyFolder").toPath();
    List<Path> result = Batfish.listAllFiles(emptyFolder);
    assertThat(result, empty());
  }

  @Test
  public void testCheckValidTopology() {
    Map<String, Configuration> configs = new HashMap<>();
    configs.put(
        "h1", BatfishTestUtils.createTestConfiguration("h1", ConfigurationFormat.HOST, "eth0"));
    configs.put(
        "h2", BatfishTestUtils.createTestConfiguration("h2", ConfigurationFormat.HOST, "e0"));
    SortedSet<Edge> edges =
        new TreeSet<>(Collections.singletonList(new Edge("h1", "eth0", "h2", "e0")));
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
        new TreeSet<>(Collections.singletonList(new Edge("h1", "eth0", "h2", "e0")));
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
        new TreeSet<>(Collections.singletonList(new Edge("h1", "eth1", "h2", "e0")));
    Topology topology = new Topology(edges);

    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("Topology contains a non-existent interface 'eth1' on node 'h1'");
    Batfish.checkTopology(configs, topology);
  }

  @Test
  public void testReadMissingIptableFile() throws IOException {
    HostConfiguration host1 = new HostConfiguration();
    host1.setHostname("host1");
    host1.setIptablesFile(Paths.get("iptables").resolve("host1.iptables").toString());
    SortedMap<String, VendorConfiguration> hostConfigurations = new TreeMap<>();
    hostConfigurations.put("host1", host1);
    SortedMap<Path, String> iptablesData = new TreeMap<>();
    Path testRigPath = _folder.newFolder("testrig").toPath();
    ParseVendorConfigurationAnswerElement answerElement =
        new ParseVendorConfigurationAnswerElement();
    answerElement.getParseStatus().put("host1", ParseStatus.PASSED);
    Batfish batfish = BatfishTestUtils.getBatfish(new TreeMap<>(), _folder);
    String failureMessage =
        "Iptables file iptables"
            + File.separator
            + "host1.iptables for host host1 is not contained within the testrig";
    batfish.readIptableFiles(testRigPath, hostConfigurations, iptablesData, answerElement);
    assertThat(answerElement.getParseStatus().get("host1"), equalTo(ParseStatus.FAILED));
    assertThat(
        answerElement.getErrors().get("host1").prettyPrint(), containsString(failureMessage));
    // When host file failed, verify that error message contains both failure messages
    answerElement.getErrors().clear();
    answerElement
        .getErrors()
        .put(
            "host1",
            new BatfishException("Failed to parse host file: host1").getBatfishStackTrace());
    batfish.readIptableFiles(testRigPath, hostConfigurations, iptablesData, answerElement);
    assertThat(
        answerElement.getErrors().get("host1").prettyPrint(), containsString(failureMessage));
    assertThat(
        answerElement.getErrors().get("host1").prettyPrint(),
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
    List<Path> expected = new ArrayList<>();
    expected.add(nestedFolder.resolve("b-test.cfg"));
    expected.add(nestedFolder.resolve("d-test.cfg"));
    expected.add(nestedFolder.resolve("aDirectory").resolve("e-test.cfg"));
    expected.add(nestedFolder.resolve("eDirectory").resolve("a-test.cfg"));
    expected.add(nestedFolder.resolve("eDirectory").resolve("c-test.cfg"));
    for (Path path : expected) {
      path.getParent().toFile().mkdir();
      assertThat(path.toFile().createNewFile(), is(true));
    }
    List<Path> actual = Batfish.listAllFiles(nestedFolder);
    Collections.sort(expected);
    assertThat(expected, equalTo(actual));
  }

  @Test
  public void testReadStartWithDotFile() throws IOException {
    Path startWithDot = _folder.newFolder("startWithDot").toPath();
    File file = startWithDot.resolve(".cfg").toFile();
    file.getParentFile().mkdir();
    assertThat(file.createNewFile(), is(true));
    List<Path> result = Batfish.listAllFiles(startWithDot);
    assertThat(result, is(empty()));
  }

  @Test
  public void testReadUnNestedPath() throws IOException {
    Path unNestedFolder = _folder.newFolder("unNestedDirectory").toPath();
    List<Path> expected = new ArrayList<>();
    expected.add(unNestedFolder.resolve("test1.cfg"));
    expected.add(unNestedFolder.resolve("test2.cfg"));
    expected.add(unNestedFolder.resolve("test3.cfg"));
    for (Path path : expected) {
      path.getParent().toFile().mkdir();
      assertThat(path.toFile().createNewFile(), is(true));
    }
    List<Path> actual = Batfish.listAllFiles(unNestedFolder);
    Collections.sort(expected);
    assertThat(expected, equalTo(actual));
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
    assertThat(configs.get("host1").getInterfaces(), hasKey("Vlan65"));
    assertThat(
        configs.get("host1").getInterfaces().get("Vlan65").getVrrpGroups().keySet(), hasSize(1));

    // Tests that computing IP owners with such a bad interface does not crash.
    CommonUtil.computeIpNodeOwners(configs, false);
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
  public void throwsExceptionWithSpecificType() {
    Path nonExistPath = _folder.getRoot().toPath().resolve("nonExistent");
    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("Failed to walk path: " + nonExistPath);
    Batfish.listAllFiles(nonExistPath);
  }
}
