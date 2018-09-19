package org.batfish.datamodel.answers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

public class MajorIssueConfigTest {

  @Test
  public void testPut() {
    MajorIssueConfig majorConfig = new MajorIssueConfig("major", ImmutableList.of());

    // add a minor config
    MinorIssueConfig minor1Config = new MinorIssueConfig("minor1", 100, "www.cnn.com");
    majorConfig = majorConfig.put(minor1Config);
    assertThat(majorConfig, equalTo(new MajorIssueConfig("major", ImmutableList.of(minor1Config))));

    // add another minor config
    MinorIssueConfig minor2Config = new MinorIssueConfig("minor2", 9, "www");
    majorConfig = majorConfig.put(minor2Config);
    assertThat(
        majorConfig,
        equalTo(new MajorIssueConfig("major", ImmutableList.of(minor1Config, minor2Config))));

    // add a minor config that overwrite the first one
    MinorIssueConfig minor1ConfigAgain = new MinorIssueConfig("minor1", 90, "www");
    majorConfig = majorConfig.put(minor1ConfigAgain);
    assertThat(
        majorConfig,
        equalTo(new MajorIssueConfig("major", ImmutableList.of(minor2Config, minor1ConfigAgain))));
  }

  /** Test if MajorIssueConfig if properly serialized and deserialized */
  @Test
  public void testSerDeser() throws IOException {
    String config =
        "{\n"
            + " \"majorIssue\": \"major\","
            + " \"minorIssueConfigs\": [\n"
            + "     {\n"
            + "       \"minorIssue\": \"minor1\",\n"
            + "       \"severity\": 23,\n"
            + "       \"url\" : \"www.cnn.com\""
            + "     },\n"
            + "     {\n"
            + "       \"minorIssue\": \"minor2\",\n"
            + "       \"severity\": 24\n"
            + "     }\n"
            + "   ]\n"
            + "}";

    MajorIssueConfig issueConfig =
        BatfishObjectMapper.mapper().readValue(config, MajorIssueConfig.class);

    // check if the issue is properly deserialized
    assertThat(
        issueConfig,
        equalTo(
            new MajorIssueConfig(
                "major",
                ImmutableList.of(
                    new MinorIssueConfig("minor1", 23, "www.cnn.com"),
                    new MinorIssueConfig("minor2", 24, null)))));

    String configSer = BatfishObjectMapper.mapper().writeValueAsString(issueConfig);
    MajorIssueConfig after =
        BatfishObjectMapper.mapper().readValue(configSer, MajorIssueConfig.class);

    // check serialization
    assertThat(after, equalTo(issueConfig));
  }
}
