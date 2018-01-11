package org.batfish.representation.aws;

import javax.annotation.Nullable;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkAddress;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.vendor_family.AwsFamily;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Utils {

  private static final NetworkFactory FACTORY = new NetworkFactory();

  public static Configuration newAwsConfiguration(String name) {
    Configuration c =
        FACTORY
            .configurationBuilder()
            .setHostname(name)
            .setConfigurationFormat(ConfigurationFormat.AWS)
            .setDefaultInboundAction(LineAction.ACCEPT)
            .setDefaultCrossZoneAction(LineAction.ACCEPT)
            .build();
    FACTORY.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME).setOwner(c).build();
    c.getVendorFamily().setAws(new AwsFamily());
    return c;
  }

  public static Interface newInterface(
      String name, Configuration c, NetworkAddress primaryAddress) {
    return FACTORY
        .interfaceBuilder()
        .setName(name)
        .setOwner(c)
        .setVrf(c.getDefaultVrf())
        .setActive(true)
        .setAddress(primaryAddress)
        .build();
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
