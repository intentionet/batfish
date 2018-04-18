package org.batfish.allinone;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.question.AclReachabilityQuestionPlugin.AclReachabilityAnswerer;
import org.batfish.question.AclReachabilityQuestionPlugin.AclReachabilityQuestion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExampleTests {
  private static final String EXAMPLE_PATH = "org/batfish/allinone/networks/example";
  private static final String REF_PATH = "org/batfish/allinone/tests/basic";

  private IBatfish _batfish;

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private static String getResourceString(String resource) throws IOException {
    URL resourceURL = ClassLoader.getSystemResource(resource);
    checkNotNull(resourceURL, "Error loading classpath resource %s", resource);
    File f = new File(resourceURL.getFile());
    return Files.asCharSource(f, StandardCharsets.UTF_8).read();
  }

  private static String getString(File f) {
    try {
      return Files.asCharSource(f, StandardCharsets.UTF_8).read();
    } catch (IOException e) {
      throw new BatfishException("Unable to read classpath resource", e);
    }
  }

  private static String extractHostname(File f) {
    String name = f.getName();
    if (name.endsWith(".cfg")) {
      return name.substring(0, name.length() - ".cfg".length());
    } else if (name.endsWith(".json")) {
      return name.substring(0, name.length() - ".json".length());
    }
    return name;
  }

  private static Map<String, String> getConfigText(String resourceFolder) {
    URL resource = ClassLoader.getSystemResource(resourceFolder);
    if (resource == null) {
      return ImmutableMap.of();
    }

    File[] files = new File(resource.getFile()).listFiles();
    if (files == null) {
      return ImmutableMap.of();
    }

    return Arrays.stream(files)
        .collect(
            ImmutableMap.toImmutableMap(ExampleTests::extractHostname, ExampleTests::getString));
  }

  @Before
  public void setUp() throws IOException {
    TestrigText.Builder text = TestrigText.builder();
    text.setConfigurationText(getConfigText(EXAMPLE_PATH + "/configs"));
    text.setHostsText(getConfigText(EXAMPLE_PATH + "/hosts"));
    text.setIptablesFilesText(getConfigText(EXAMPLE_PATH + "/iptables"));

    _batfish = BatfishTestUtils.getBatfishFromTestrigText(text.build(), _folder);
  }

  @Test
  public void aclReachability() throws IOException {
    AclReachabilityQuestion q = new AclReachabilityQuestion();
    AclReachabilityAnswerer a = new AclReachabilityAnswerer(q, _batfish);
    AnswerElement e = a.answer();
    assertThat(
        BatfishObjectMapper.writePrettyString(e),
        equalTo(getResourceString(REF_PATH + "/aclReachability.ref")));
  }
}
