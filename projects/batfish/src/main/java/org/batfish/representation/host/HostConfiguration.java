package org.batfish.representation.host;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.batfish.common.VendorConversionException;
import org.batfish.common.Warnings;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.representation.iptables.IptablesVendorConfiguration;
import org.batfish.vendor.VendorConfiguration;

public class HostConfiguration extends VendorConfiguration {

  private static final String FILTER_FORWARD = "filter::FORWARD";

  private static final String FILTER_INPUT = "filter::INPUT";

  private static final String FILTER_OUTPUT = "filter::OUTPUT";

  private static final String MANGLE_FORWARD = "mangle::FORWARD";

  private static final String MANGLE_INPUT = "mangle::INPUT";

  private static final String MANGLE_OUTPUT = "mangle::OUTPUT";

  private static final String MANGLE_POSTROUTING = "mangle::POSTROUTING";

  private static final String MANGLE_PREROUTING = "mangle::PREROUTING";

  private static final String NAT_OUTPUT = "nat::OUTPUT";

  private static final String NAT_PREROUTING = "nat::PREROUTING";

  private static final String PROP_HOST_INTERFACES = "hostInterfaces";

  private static final String PROP_HOSTNAME = "hostname";

  private static final String PROP_IPTABLES_FILE = "iptablesFile";

  private static final String PROP_OVERLAY = "overlay";

  private static final String RAW_OUTPUT = "raw::OUTPUT";

  private static final String RAW_PREROUTING = "raw::PREROUTING";

  /** */
  private static final long serialVersionUID = 1L;

  public static HostConfiguration fromJson(String text, Warnings warnings) throws IOException {
    HostConfiguration hostConfiguration =
        BatfishObjectMapper.mapper().readValue(text, HostConfiguration.class);
    hostConfiguration._w = warnings;
    return hostConfiguration;
  }

  private Configuration _c;

  protected final SortedMap<String, HostInterface> _hostInterfaces;

  private String _hostname;

  private String _iptablesFile;

  private IptablesVendorConfiguration _iptablesVendorConfig;

  private boolean _overlay;

  protected final SortedSet<String> _roles = new TreeSet<>();

  // @JsonCreator
  // public HostConfiguration(@JsonProperty(PROP_HOSTNAME) String name) {
  // _hostname = name;
  // _interfaces = new HashMap<String, Interface>();
  // _roles = new RoleSet();
  // }

  private final Set<HostStaticRoute> _staticRoutes;

  private transient VendorConfiguration _underlayConfiguration;

  private transient Set<String> _unimplementedFeatures;

  public HostConfiguration() {
    _hostInterfaces = new TreeMap<>();
    _staticRoutes = new TreeSet<>();
  }

  @JsonProperty(PROP_HOST_INTERFACES)
  public Map<String, HostInterface> getHostInterfaces() {
    return _hostInterfaces;
  }

  @JsonProperty(PROP_HOSTNAME)
  @Override
  public String getHostname() {
    return _hostname;
  }

  @JsonProperty(PROP_IPTABLES_FILE)
  public String getIptablesFile() {
    return _iptablesFile;
  }

  public IptablesVendorConfiguration getIptablesVendorConfig() {
    return _iptablesVendorConfig;
  }

  @JsonProperty(PROP_OVERLAY)
  public boolean getOverlay() {
    return _overlay;
  }

  @JsonIgnore
  @Override
  public SortedSet<String> getRoles() {
    return _roles;
  }

  @JsonIgnore
  @Override
  public Set<String> getUnimplementedFeatures() {
    return _unimplementedFeatures;
  }

  @Override
  public void setHostname(String hostname) {
    _hostname = hostname;
  }

  public void setIptablesFile(String file) {
    _iptablesFile = file;
  }

  public void setIptablesVendorConfig(IptablesVendorConfiguration config) {
    _iptablesVendorConfig = config;
  }

  @JsonProperty(PROP_OVERLAY)
  public void setOverlay(boolean overlay) {
    _overlay = overlay;
  }

  @Override
  public void setRoles(SortedSet<String> roles) {
    _roles.addAll(roles);
  }

  @JsonIgnore
  @Override
  public void setVendor(ConfigurationFormat format) {
    throw new UnsupportedOperationException("Cannot set vendor for host configuration");
  }

  private boolean simple() {
    String[] aclsToCheck =
        new String[] {
          RAW_PREROUTING,
          MANGLE_PREROUTING,
          NAT_PREROUTING,
          MANGLE_INPUT,
          RAW_OUTPUT,
          MANGLE_OUTPUT,
          NAT_OUTPUT,
          MANGLE_FORWARD,
          FILTER_FORWARD,
          MANGLE_POSTROUTING
        };
    for (String aclName : aclsToCheck) {
      IpAccessList acl = _c.getIpAccessLists().get(aclName);
      if (acl != null) {
        for (IpAccessListLine line : acl.getLines()) {
          if (line.getAction() == LineAction.REJECT) {
            return false;
          }
          if (!line.unrestricted()) {
            return false;
          }
        }
      }
    }
    return true;
  }

  @Override
  public Configuration toVendorIndependentConfiguration() throws VendorConversionException {
    if (_underlayConfiguration != null) {
      _hostInterfaces.forEach(
          (name, iface) ->
              iface.setCanonicalName(_underlayConfiguration.canonicalizeInterfaceName(name)));
    } else {
      _hostInterfaces.forEach((name, iface) -> iface.setCanonicalName(name));
    }
    String hostname = getHostname();
    _c = new Configuration(hostname, ConfigurationFormat.HOST);
    _c.setDefaultCrossZoneAction(LineAction.ACCEPT);
    _c.setDefaultInboundAction(LineAction.ACCEPT);
    _c.setRoles(_roles);
    _c.getVrfs().put(Configuration.DEFAULT_VRF_NAME, new Vrf(Configuration.DEFAULT_VRF_NAME));

    // add interfaces
    _hostInterfaces
        .values()
        .forEach(
            hostInterface -> {
              String canonicalName = hostInterface.getCanonicalName();
              Interface newIface = hostInterface.toInterface(_c, _w);
              _c.getInterfaces().put(canonicalName, newIface);
              _c.getDefaultVrf().getInterfaces().put(canonicalName, newIface);
            });

    // add iptables
    if (_iptablesVendorConfig != null) {
      _iptablesVendorConfig.addAsIpAccessLists(_c, this, _w);
    }

    // apply acls to interfaces
    if (simple()) {
      for (Interface iface : _c.getDefaultVrf().getInterfaces().values()) {
        iface.setIncomingFilter(_c.getIpAccessLists().get(FILTER_INPUT));
        iface.setOutgoingFilter(_c.getIpAccessLists().get(FILTER_OUTPUT));
      }
    } else {
      _w.unimplemented("Do not support complicated iptables rules yet");
    }

    _c.getDefaultVrf()
        .getStaticRoutes()
        .addAll(_staticRoutes.stream().map(hsr -> hsr.toStaticRoute()).collect(Collectors.toSet()));
    Set<StaticRoute> staticRoutes = _c.getDefaultVrf().getStaticRoutes();
    for (HostInterface iface : _hostInterfaces.values()) {
      Ip gateway = iface.getGateway();
      if (gateway != null) {
        staticRoutes.add(
            StaticRoute.builder()
                .setNetwork(Prefix.ZERO)
                .setNextHopIp(gateway)
                .setNextHopInterface(iface.getName())
                .setAdministrativeCost(HostStaticRoute.DEFAULT_ADMINISTRATIVE_COST)
                .setTag(AbstractRoute.NO_TAG)
                .build());
        break;
      }
    }
    if (_staticRoutes.isEmpty() && staticRoutes.isEmpty() && !_c.getInterfaces().isEmpty()) {
      String ifaceName = _c.getInterfaces().values().iterator().next().getName();
      _c.getDefaultVrf()
          .getStaticRoutes()
          .add(
              StaticRoute.builder()
                  .setNetwork(Prefix.ZERO)
                  .setNextHopInterface(ifaceName)
                  .setAdministrativeCost(HostStaticRoute.DEFAULT_ADMINISTRATIVE_COST)
                  .setTag(AbstractRoute.NO_TAG)
                  .build());
    }
    return _c;
  }

  public Configuration toVendorIndependentConfiguration(VendorConfiguration underlayConfiguration) {
    _underlayConfiguration = underlayConfiguration;
    return toVendorIndependentConfiguration();
  }
}
