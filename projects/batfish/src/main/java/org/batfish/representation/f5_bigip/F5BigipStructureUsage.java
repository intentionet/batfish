package org.batfish.representation.f5_bigip;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.vendor.StructureUsage;

/** Named structure-usage types for F5 BIG-IP device */
@ParametersAreNonnullByDefault
public enum F5BigipStructureUsage implements StructureUsage {
  BGP_ADDRESS_FAMILY_REDISTRIBUTE_KERNEL_ROUTE_MAP(
      "bgp address-family redistribute kernel route-map"),
  BGP_NEIGHBOR_IPV4_ROUTE_MAP_OUT("bgp neighbor address-family ipv4 route-map out"),
  BGP_NEIGHBOR_IPV6_ROUTE_MAP_OUT("bgp neighbor address-family ipv6 route-map out"),
  BGP_NEIGHBOR_SELF_REFERENCE("bgp neighbor self-reference"),
  BGP_NEIGHBOR_UPDATE_SOURCE("bgp neighbor update-source"),
  BGP_PROCESS_SELF_REFERENCE("bgp process self-reference"),
  INTERFACE_SELF_REFERENCE("interface self-reference"),
  MONITOR_HTTP_DEFAULTS_FROM("monitor http defaults-from"),
  MONITOR_HTTPS_DEFAULTS_FROM("monitor https defaults-from"),
  MONITOR_HTTPS_SSL_PROFILE("monitor https ssl-profile"),
  PERSISTENCE_SOURCE_ADDR_DEFAULTS_FROM("persistence source-addr defaults-from"),
  PERSISTENCE_SSL_DEFAULTS_FROM("persistence ssl defaults-from"),
  POOL_MEMBER("pool member"),
  POOL_MONITOR("pool monitor"),
  PROFILE_CLIENT_SSL_DEFAULTS_FROM("profile client-ssl defaults-from"),
  PROFILE_HTTP_DEFAULTS_FROM("profile http defaults-from"),
  PROFILE_OCSP_STAPLING_PARAMS_DEFAULTS_FROM("profile ocsp-stapling-params defaults-from"),
  PROFILE_ONE_CONNECT_DEFAULTS_FROM("profile one-connect defaults-from"),
  PROFILE_SERVER_SSL_DEFAULTS_FROM("profile server-ssl defaults-from"),
  PROFILE_TCP_DEFAULTS_FROM("profile tcp defaults-from"),
  ROUTE_MAP_MATCH_IPV4_ADDRESS_PREFIX_LIST("route-map match ipv4 address prefix-list"),
  SELF_SELF_REFERENCE("self self-reference"),
  SELF_VLAN("self vlan"),
  SNAT_SELF_REFERENCE("snat self-reference"),
  SNAT_SNATPOOL("snat snatpool"),
  SNAT_VLANS_VLAN("snat vlans vlan"),
  SNATPOOL_MEMBERS_MEMBER("snatpool members member"),
  VIRTUAL_DESTINATION("virtual destination"),
  VIRTUAL_PERSIST_PERSISTENCE("virtual persist persistence"),
  VIRTUAL_POOL("virtual pool"),
  VIRTUAL_PROFILE("virtual profile"),
  VIRTUAL_RULES_RULE("virtual rules rule"),
  VIRTUAL_SELF_REFERENCE("virtual self-reference"),
  VIRTUAL_SOURCE_ADDRESS_TRANSLATION_POOL("virtual source-address-translation pool"),
  VIRTUAL_VLANS_VLAN("virtual vlans vlan"),
  VLAN_INTERFACE("vlan interface");

  private final @Nonnull String _description;

  private F5BigipStructureUsage(String description) {
    _description = description;
  }

  @Override
  public @Nonnull String getDescription() {
    return _description;
  }
}
