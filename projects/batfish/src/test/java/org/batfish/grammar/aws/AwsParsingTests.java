package org.batfish.grammar.aws;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedMap;
import java.util.TreeMap;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.batfish.common.BfConsts;
import org.batfish.common.util.CommonUtil;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.representation.aws_vpcs.AwsVpcConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class AwsParsingTests {

  private static String TESTCONFIGS_PREFIX = "org/batfish/grammar/aws/testconfigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testVpcPeeringConnections() throws IOException {
    String awsFilename = "VpcPeeringConnections.json";
    SortedMap<String, String> awsText = new TreeMap<>();
    String vpcPeeringConnectionsText = CommonUtil.readResource(TESTCONFIGS_PREFIX + awsFilename);
    awsText.put(awsFilename, vpcPeeringConnectionsText);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(null, awsText, null, null, null, null, _folder);
    batfish.loadConfigurations();
    Path awsVendorFile =
        batfish
            .getTestrigSettings()
            .getSerializeVendorPath()
            .resolve(BfConsts.RELPATH_AWS_VPC_CONFIGS_FILE);
    AwsVpcConfiguration awsVpcConfiguration =
        batfish
            .deserializeObjects(
                ImmutableMap.of(awsVendorFile, BfConsts.RELPATH_AWS_VPC_CONFIGS_FILE),
                AwsVpcConfiguration.class)
            .get(BfConsts.RELPATH_AWS_VPC_CONFIGS_FILE);

    /*
     * We should have an entry for the vpc peering connection with status code "active", but not
     * for the one with status code "deleted".
     */
    assertThat(awsVpcConfiguration.getVpcPeeringConnections(), hasKey("pcx-f754069e"));
    assertThat(awsVpcConfiguration.getVpcPeeringConnections(), not(hasKey("pcx-4ee8b427")));
  }
}
