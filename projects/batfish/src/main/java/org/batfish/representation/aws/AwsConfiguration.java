package org.batfish.representation.aws;

import static org.batfish.representation.aws.InternetGateway.AWS_BACKBONE_ASN;
import static org.batfish.representation.aws.InternetGateway.AWS_BACKBONE_NODE_NAME;
import static org.batfish.representation.aws.InternetGateway.BACKBONE_INTERFACE_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BfConsts;
import org.batfish.common.VendorConversionException;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.isp_configuration.BorderInterfaceInfo;
import org.batfish.datamodel.isp_configuration.IspConfiguration;
import org.batfish.datamodel.isp_configuration.IspFilter;
import org.batfish.datamodel.isp_configuration.IspNodeInfo;
import org.batfish.vendor.VendorConfiguration;

/** The top-level class that represent AWS configuration */
@ParametersAreNonnullByDefault
public class AwsConfiguration extends VendorConfiguration {

  static final Ip LINK_LOCAL_IP = Ip.parse("169.254.0.1");

  @Nullable private ConvertedConfiguration _convertedConfiguration;

  @Nonnull private final Map<String, Region> _regions;

  public AwsConfiguration() {
    this(new HashMap<>());
  }

  public AwsConfiguration(Map<String, Region> regions) {
    _regions = regions;
  }

  /** Adds a config subtree */
  public void addConfigElement(
      String region,
      JsonNode json,
      String sourceFileName,
      ParseVendorConfigurationAnswerElement pvcae) {
    _regions
        .computeIfAbsent(region, r -> new Region(region))
        .addConfigElement(json, sourceFileName, pvcae);
  }

  /**
   * Convert this AWS config to a set of VI configurations
   *
   * <p>TODO: Populate all the structure names that appear in these configs
   */
  @Nonnull
  @Override
  public List<Configuration> toVendorIndependentConfigurations() throws VendorConversionException {
    if (_convertedConfiguration == null) {
      convertConfigurations();
    }
    return ImmutableList.copyOf(_convertedConfiguration.getConfigurationNodes().values());
  }

  private void convertConfigurations() {
    _convertedConfiguration = new ConvertedConfiguration();
    for (Region region : _regions.values()) {
      region.toConfigurationNodes(_convertedConfiguration, getWarnings());
    }
    // We do this de-duplication because cross-region connections will show up in both regions
    Set<VpcPeeringConnection> vpcPeeringConnections =
        _regions.values().stream()
            .flatMap(r -> r.getVpcPeeringConnections().values().stream())
            .collect(ImmutableSet.toImmutableSet());
    vpcPeeringConnections.forEach(
        c -> c.createConnection(_regions, _convertedConfiguration, getWarnings()));
  }

  @Override
  @Nonnull
  public IspConfiguration getIspConfiguration() {
    List<BorderInterfaceInfo> borderInterfaces =
        _regions.values().stream()
            .flatMap(r -> r.getInternetGateways().values().stream())
            .map(igw -> NodeInterfacePair.of(igw.getId(), BACKBONE_INTERFACE_NAME))
            .map(BorderInterfaceInfo::new)
            .collect(ImmutableList.toImmutableList());
    return new IspConfiguration(
        borderInterfaces,
        IspFilter.ALLOW_ALL,
        ImmutableList.of(new IspNodeInfo(AWS_BACKBONE_ASN, AWS_BACKBONE_NODE_NAME)));
  }

  @Override
  public String getFilename() {
    // not a real file name but a folder
    return BfConsts.RELPATH_AWS_CONFIGS_FILE;
  }

  @Override
  public String getHostname() {
    // This hostname does not appear in the vendor independent configs that are returned
    return BfConsts.RELPATH_AWS_CONFIGS_FILE;
  }

  @Override
  @Nonnull
  public Set<Layer1Edge> getLayer1Edges() {
    if (_convertedConfiguration == null) {
      convertConfigurations();
    }
    return _convertedConfiguration.getLayer1Edges();
  }

  @Override
  public void setHostname(String hostname) {
    throw new IllegalStateException("Setting the hostname is not allowed for AWS configs");
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    throw new IllegalStateException("Setting the format is not allowed for AWS configs");
  }

  public static String vpnExternalInterfaceName(String tunnelId) {
    return "external-" + tunnelId;
  }

  public static String vpnTunnelId(String vpnConnectionId, int idNum) {
    return String.format("%s-%s", vpnConnectionId, idNum);
  }

  public static String vpnInterfaceName(String tunnelId) {
    return "vpn-" + tunnelId;
  }
}
