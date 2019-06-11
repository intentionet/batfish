package org.batfish.datamodel;

import com.google.common.collect.Table;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public interface DataPlane extends Serializable {

  /** Return routes in the BGP rib for each node/VRF */
  Table<String, String, Set<Bgpv4Route>> getBgpRoutes();

  /** Return routes in the EVPN RIB on each node/VRF */
  Table<String, String, Set<EvpnRoute<?, ?>>> getEvpnRoutes();

  Map<String, Configuration> getConfigurations();

  /** Return a {@link Fib} for each node/VRF */
  Map<String, Map<String, Fib>> getFibs();

  ForwardingAnalysis getForwardingAnalysis();

  /**
   * Return the map of Vrfs that own each Ip (as computed during dataplane computation). Map
   * structure: Ip -&gt; hostname -&gt; set of Vrfs
   */
  Map<Ip, Map<String, Set<String>>> getIpVrfOwners();

  /** Return the set of all (main) RIBs. Map structure: hostname -&gt; VRF name -&gt; GenericRib */
  SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> getRibs();

  /**
   * Return the summary of route prefix propagation. Map structure: Hostname -&gt; VRF name -&gt;
   * Prefix -&gt; action taken -&gt; set of hostnames (peers).
   */
  SortedMap<String, SortedMap<String, Map<Prefix, Map<String, Set<String>>>>>
      getPrefixTracingInfoSummary();

  /**
   * Return {@link VniSettings} for each node/VRF. Returned settings are based on the vni settings
   * in a {@link Vrf}, but may include additional information obtained during dataplane computation,
   * such as updated flood lists due to EVPN route exchange.
   */
  Table<String, String, Set<VniSettings>> getVniSettings();
}
