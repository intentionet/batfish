package org.batfish.common.topology;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.NetworkSnapshot;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.ipsec.IpsecTopology;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.vxlan.VxlanTopology;

/**
 * A provider of various topology-type data structures (e.g., IpOwners, L3 Topology,
 * protocol-specific topology graphs) for network snapshots.
 */
@ParametersAreNonnullByDefault
public interface TopologyProvider {

  /**
   * Returns the {@link BgpTopology} corresponding to the converged {@link
   * org.batfish.datamodel.DataPlane}.
   */
  @Nonnull
  BgpTopology getBgpTopology(NetworkSnapshot snapshot);

  /**
   * Return the {@link IpsecTopology} for a given {@link NetworkSnapshot} which just contains the
   * edges based on the IPsec settings compatibility
   */
  @Nonnull
  IpsecTopology getInitialIpsecTopology(NetworkSnapshot networkSnapshot);

  /**
   * Computes the {@link Layer2Topology} for a given {@link NetworkSnapshot}. The layer-2 topology
   * is constructed from the layer-1 logical topology and switching information in the
   * configurations.
   *
   * @return computed topology, or {@link Optional#empty()} if layer-1 logical topology is absent.
   */
  @Nonnull
  Optional<Layer2Topology> getInitialLayer2Topology(NetworkSnapshot networkSnapshot);

  /**
   * Return the layer-3 {@link Topology} for a given {@link NetworkSnapshot} that is a subset of the
   * raw layer-3 topology such that edges corresponding to blacklisted entities or failed tunnels
   * have been pruned.
   */
  @Nonnull
  Topology getInitialLayer3Topology(NetworkSnapshot networkSnapshot);

  /** Return the topology representing OSPF adjacencies. */
  @Nonnull
  OspfTopology getInitialOspfTopology(@Nonnull NetworkSnapshot networkSnapshot);

  /** Return the {@link VxlanTopology} for a given {@link NetworkSnapshot}. */
  @Nonnull
  VxlanTopology getInitialVxlanTopology(NetworkSnapshot snapshot);

  /**
   * Return {@link IpOwners} computed (based on configurations only) for a given {@link
   * NetworkSnapshot}
   */
  @Nonnull
  IpOwners getIpOwners(NetworkSnapshot snapshot);

  /**
   * Computes the {@link Layer1Topology} with respect to logical layer-1 edges for a given {@link
   * NetworkSnapshot}. The logical layer-1 topology is constructed from the physical layer-1
   * topology by collapsing each set of edges betweeen physical interfaces belonging to the same
   * aggregates on each side into a single edge between the aggregate interfaces.
   *
   * <p>So for example the two edges (n1:i1,n2:i1), (n1:i2,n2:i2) would be aggregated into a single
   * edge (n1:a1,n2:a1) if on both n1 and n2 the interfaces i1 and i2 are members of an aggregate
   * interface a1.
   *
   * @return computed topology, or {@link Optional#empty()} if physical layer-1 topology is absent.
   */
  @Nonnull
  Optional<Layer1Topology> getLayer1LogicalTopology(NetworkSnapshot networkSnapshot);

  /**
   * Computes the {@link Layer1Topology} with respect to layer-1 physical edges for a given {@link
   * NetworkSnapshot}. The layer-1 physical topology is constructed from the raw layer-1 physical
   * edges input by the user by trimming the edges whose nodes do not correspond to active physical
   * interfaces, and adding the reverse edge for asymmetric edges.
   *
   * @return computed topology, or {@link Optional#empty()} if raw layer-1 physical topology is
   *     absent.
   */
  @Nonnull
  Optional<Layer1Topology> getLayer1PhysicalTopology(NetworkSnapshot networkSnapshot);

  /**
   * Returns the {@link Layer2Topology} corresponding to the converged {@link
   * org.batfish.datamodel.DataPlane}, or {@link Optional#empty()} if raw layer-1 physical topology
   * is absent.
   */
  @Nonnull
  Optional<Layer2Topology> getLayer2Topology(NetworkSnapshot snapshot);

  /**
   * Returns the layer-3 {@link Topology} corresponding to the converged {@link
   * org.batfish.datamodel.DataPlane}.
   */
  @Nonnull
  Topology getLayer3Topology(NetworkSnapshot snapshot);

  @Nonnull
  OspfTopology getOspfTopology(NetworkSnapshot networkSnapshot);

  /**
   * Return the raw {@link Layer1Topology} provided by the user in the snapshot, or {@link
   * Optional#empty()} if none provided by user.
   */
  @Nonnull
  Optional<Layer1Topology> getRawLayer1PhysicalTopology(NetworkSnapshot networkSnapshot);

  /**
   * Return the raw layer-3 {@link Topology} for a given {@link NetworkSnapshot}. The layer-3
   * topology is constructed by inferring layer-3 adjacencies via the layer-3 information in the
   * configurations, and pruning edges whose vertices are not in the same broadcast domain according
   * to the layer-2 topology. Nevertheless, pruning does NOT occur for any inferred layer-3 edge
   * where either vertex's node (i.e., hostname) does not appear in the tail of any edge of the raw
   * layer-1 topology. Note that an absent raw layer-1 topology is treated as empty, so no pruning
   * occurs in that case.
   */
  @Nonnull
  Topology getRawLayer3Topology(NetworkSnapshot networkSnapshot);

  /**
   * Returns the {@link VxlanTopology} corresponding to the converged {@link
   * org.batfish.datamodel.DataPlane}.
   */
  @Nonnull
  VxlanTopology getVxlanTopology(NetworkSnapshot snapshot);

  @Nonnull
  TunnelTopology getInitialTunnelTopology(NetworkSnapshot snapshot);
}
