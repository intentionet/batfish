package org.batfish.representation.aws;

import java.util.List;
import javax.annotation.Nullable;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.vendor_family.AwsFamily;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Utils {

  private static final NetworkFactory FACTORY = new NetworkFactory();

  public static Configuration newAwsConfiguration(String name, String domainName) {
    Configuration c =
        FACTORY
            .configurationBuilder()
            .setHostname(name)
            .setDomainName(domainName)
            .setConfigurationFormat(ConfigurationFormat.AWS)
            .setDefaultInboundAction(LineAction.ACCEPT)
            .setDefaultCrossZoneAction(LineAction.ACCEPT)
            .build();
    FACTORY.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME).setOwner(c).build();
    c.getVendorFamily().setAws(new AwsFamily());
    return c;
  }

  public static Interface newInterface(
      String name, Configuration c, InterfaceAddress primaryAddress) {
    return FACTORY
        .interfaceBuilder()
        .setName(name)
        .setOwner(c)
        .setVrf(c.getDefaultVrf())
        .setActive(true)
        .setAddress(primaryAddress)
        .build();
  }

  /**
   * Updates {@link Region}'s mapping between {@link Configuration} names and {@link SecurityGroup}
   * for a given configuration. Also updates {@link org.batfish.datamodel.Ip} of instances in {@link
   * SecurityGroup}
   *
   * @param region {@link Region} in which the configuration is in
   * @param configuration {@link Configuration} for which security groups are to be processed
   * @param securityGroupsIds {@link List} of security group IDs
   * @param warnings {@link Warnings} for the configuration
   */
  public static void processSecurityGroups(
      Region region,
      Configuration configuration,
      List<String> securityGroupsIds,
      Warnings warnings) {
    for (String sGroupId : securityGroupsIds) {
      SecurityGroup securityGroup = region.getSecurityGroups().get(sGroupId);
      if (securityGroup == null) {
        warnings.pedantic(
            String.format(
                "Security group \"%s\" for \"%s\" not found",
                sGroupId, configuration.getHostname()));
        continue;
      }
      region.updateConfigurationSecurityGroups(configuration.getHostname(), securityGroup);

      securityGroup.updateConfigIps(configuration);
    }
  }

  public static boolean tryGetBoolean(JSONObject jsonObject, String key, boolean defaultValue)
      throws JSONException {
    if (jsonObject.has(key)) {
      return jsonObject.getBoolean(key);
    }
    return defaultValue;
  }

  public static int tryGetInt(JSONObject jsonObject, String key, int defaultValue)
      throws JSONException {
    if (jsonObject.has(key)) {
      return jsonObject.getInt(key);
    }
    return defaultValue;
  }

  @Nullable
  public static String tryGetString(JSONObject jsonObject, String key) throws JSONException {
    if (jsonObject.has(key)) {
      return jsonObject.getString(key);
    }
    return null;
  }

  private Utils() {}
}
