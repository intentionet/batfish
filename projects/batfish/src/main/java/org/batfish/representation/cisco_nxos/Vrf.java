package org.batfish.representation.cisco_nxos;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;

/** A virtual routing and forwarding instance. */
public final class Vrf implements Serializable {

  /**
   * Offset used to compute Route Distinguisher for VNIs associated with MAC VRFs (L2 VNIs)
   * https://www.cisco.com/c/en/us/td/docs/switches/datacenter/nexus9000/sw/7-x/vxlan/configuration/guide/b_Cisco_Nexus_9000_Series_NX-OS_VXLAN_Configuration_Guide_7x/b_Cisco_Nexus_9000_Series_NX-OS_VXLAN_Configuration_Guide_7x_chapter_0100.html
   */
  public static final int MAC_VRF_OFFSET = 32767;

  public Vrf(String name, int id) {
    _name = name;
    _id = id;
    _addressFamilies = new HashMap<>();
    _nameServers = new ArrayList<>(1);
    _staticRoutes = HashMultimap.create();
    _staticRoutesV6 = HashMultimap.create();
  }

  public @Nonnull Map<AddressFamily, VrfAddressFamily> getAddressFamilies() {
    return _addressFamilies;
  }

  public @Nonnull VrfAddressFamily getAddressFamily(AddressFamily type) {
    return _addressFamilies.computeIfAbsent(type, VrfAddressFamily::new);
  }

  public @Nullable String getDescription() {
    return _description;
  }

  public void setDescription(@Nullable String description) {
    _description = description;
  }

  public @Nonnull String getName() {
    return _name;
  }

  /**
   * @return Numerical ID which represents order in which VRFs are defined. It is used in auto
   *     derived Route Distinguishers, etc.
   */
  public int getId() {
    return _id;
  }

  @Nonnull
  public List<NameServer> getNameServers() {
    return _nameServers;
  }

  public void addNameServer(@Nonnull NameServer server) {
    if (_nameServers.contains(server)) {
      // Do not add duplicates
      return;
    }
    _nameServers.add(server);
  }

  public @Nullable RouteDistinguisherOrAuto getRd() {
    return _rd;
  }

  public void setRd(@Nullable RouteDistinguisherOrAuto rd) {
    _rd = rd;
  }

  public boolean getShutdown() {
    return _shutdown;
  }

  public void setShutdown(boolean shutdown) {
    _shutdown = shutdown;
  }

  public @Nonnull Multimap<Prefix, StaticRoute> getStaticRoutes() {
    return _staticRoutes;
  }

  public @Nonnull Multimap<Prefix6, StaticRouteV6> getStaticRoutesV6() {
    return _staticRoutesV6;
  }

  public @Nullable Integer getVni() {
    return _vni;
  }

  public void setVni(@Nullable Integer vni) {
    _vni = vni;
  }

  //////////////////////////////////////////
  ///// Private implementation details /////
  //////////////////////////////////////////

  private final Map<AddressFamily, VrfAddressFamily> _addressFamilies;
  private @Nullable String _description;
  private final @Nonnull String _name;
  private final int _id;
  @Nonnull private final List<NameServer> _nameServers;
  private @Nullable RouteDistinguisherOrAuto _rd;
  private boolean _shutdown;
  private final Multimap<Prefix, StaticRoute> _staticRoutes;
  private final Multimap<Prefix6, StaticRouteV6> _staticRoutesV6;
  private @Nullable Integer _vni;
}
