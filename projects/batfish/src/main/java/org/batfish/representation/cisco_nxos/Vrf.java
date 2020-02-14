package org.batfish.representation.cisco_nxos;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.ip.Prefix;

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
    _staticRoutes = HashMultimap.create();
  }

  public @Nonnull Map<AddressFamily, VrfAddressFamily> getAddressFamilies() {
    return _addressFamilies;
  }

  public @Nonnull VrfAddressFamily getAddressFamily(AddressFamily type) {
    return _addressFamilies.computeIfAbsent(type, VrfAddressFamily::new);
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
  private final @Nonnull String _name;
  private final int _id;
  private @Nullable RouteDistinguisherOrAuto _rd;
  private boolean _shutdown;
  private final Multimap<Prefix, StaticRoute> _staticRoutes;
  private @Nullable Integer _vni;
}
