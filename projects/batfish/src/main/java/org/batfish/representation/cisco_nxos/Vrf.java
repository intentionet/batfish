package org.batfish.representation.cisco_nxos;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Prefix;

/** A virtual routing and forwarding instance. */
public final class Vrf implements Serializable {

  public Vrf(String name) {
    _name = name;
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
  private @Nullable RouteDistinguisherOrAuto _rd;
  private boolean _shutdown;
  private final Multimap<Prefix, StaticRoute> _staticRoutes;
  private @Nullable Integer _vni;
}
