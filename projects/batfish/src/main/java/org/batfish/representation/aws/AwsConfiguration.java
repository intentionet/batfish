package org.batfish.representation.aws;

import static org.batfish.representation.aws.InternetGateway.AWS_BACKBONE_ASN;
import static org.batfish.representation.aws.InternetGateway.AWS_BACKBONE_HUMAN_NAME;
import static org.batfish.representation.aws.InternetGateway.BACKBONE_INTERFACE_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
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
import org.batfish.datamodel.isp_configuration.IspAnnouncement;
import org.batfish.datamodel.isp_configuration.IspConfiguration;
import org.batfish.datamodel.isp_configuration.IspFilter;
import org.batfish.datamodel.isp_configuration.IspNodeInfo;
import org.batfish.vendor.VendorConfiguration;

/** The top-level class that represent AWS configuration across different accounts */
@ParametersAreNonnullByDefault
public class AwsConfiguration extends VendorConfiguration {

  public static final String DEFAULT_REGION_NAME = "us-west-2";
  static final Ip LINK_LOCAL_IP = Ip.parse("169.254.0.1");
  public static final String DEFAULT_ACCOUNT_NAME = "default";

  @Nullable private ConvertedConfiguration _convertedConfiguration;
  @Nonnull private final Map<String, Account> _accounts;

  public AwsConfiguration() {
    this(new HashMap<>());
  }

  private AwsConfiguration(Map<String, Account> accounts) {
    _accounts = accounts;
  }

  public Collection<Account> getAccounts() {
    return _accounts.values();
  }

  /** Return a stream of all VPCs, across all accounts */
  @Nonnull
  public Stream<Vpc> getAllVpc() {
    return getAccounts().stream()
        .flatMap(a -> a.getRegions().stream())
        .flatMap(r -> r.getVpcs().values().stream());
  }

  /** Return a VPC with a given ID (in any account/region) if it exists, or {@code null} */
  @Nullable
  public Vpc getVpc(String vpcId) {
    return getAllVpc().filter(v -> vpcId.equals(v.getId())).findFirst().orElse(null);
  }

  @VisibleForTesting
  @Nonnull
  Account addOrGetAccount(String accountId) {
    return _accounts.computeIfAbsent(accountId, Account::new);
  }

  /** Adds a config subtree */
  public void addConfigElement(
      String region,
      JsonNode json,
      String sourceFileName,
      ParseVendorConfigurationAnswerElement pvcae,
      String account) {
    addOrGetAccount(account).addOrGetRegion(region).addConfigElement(json, sourceFileName, pvcae);
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
    return ImmutableList.copyOf(_convertedConfiguration.getAllNodes());
  }

  private void convertConfigurations() {
    _convertedConfiguration = new ConvertedConfiguration();
    for (Account account : getAccounts()) {
      Collection<Region> regions = account.getRegions();
      for (Region region : regions) {
        try {
          region.toConfigurationNodes(_convertedConfiguration, getWarnings());
        } catch (Exception e) {
          getWarnings()
              .redFlag(
                  String.format(
                      "Failed conversion for account %s, region %s\n%s",
                      account.getId(), region.getName(), e));
        }
      }
    }
    // Vpc peerings can be both cross-region and cross-account, so we handle them here
    processVpcPeerings();
    // Transit gateways can be cross-account so we handle them here
    try {
      TransitGatewayConverter.convertTransitGateways(this, _convertedConfiguration)
          .forEach(_convertedConfiguration::addNode);
    } catch (Exception e) {
      getWarnings().redFlag(String.format("Failed to convert transit gateways", e));
    }
  }

  private void processVpcPeerings() {
    // We do this de-duplication (collecting to a set) because cross-region (or cross-account)
    // connections will show up in both regions. No need to re-create them twice in conversion.
    Set<VpcPeeringConnection> vpcPeeringConnections =
        getAccounts().stream()
            .flatMap(a -> a.getRegions().stream())
            .flatMap(r -> r.getVpcPeeringConnections().values().stream())
            .collect(ImmutableSet.toImmutableSet());
    try {
      vpcPeeringConnections.forEach(
          c -> c.createConnection(_convertedConfiguration, getWarnings()));
    } catch (Exception e) {
      getWarnings().redFlag(String.format("Failed to process VPC peerings %s", e));
    }
  }

  @Override
  @Nonnull
  public IspConfiguration getIspConfiguration() {
    List<BorderInterfaceInfo> borderInterfaces =
        getAccounts().stream()
            .flatMap(a -> a.getRegions().stream())
            .flatMap(r -> r.getInternetGateways().values().stream())
            .map(igw -> NodeInterfacePair.of(igw.getId(), BACKBONE_INTERFACE_NAME))
            .map(BorderInterfaceInfo::new)
            .collect(ImmutableList.toImmutableList());
    return new IspConfiguration(
        borderInterfaces,
        IspFilter.ALLOW_ALL,
        ImmutableList.of(
            new IspNodeInfo(
                AWS_BACKBONE_ASN,
                AWS_BACKBONE_HUMAN_NAME,
                AwsPrefixes.getPrefixes().stream()
                    .map(IspAnnouncement::new)
                    .collect(ImmutableList.toImmutableList()))));
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
