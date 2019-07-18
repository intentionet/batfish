package org.batfish.common.topology;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.vxlan.VxlanTopology;

/**
 * Container of topologies corresponding to a convergent {@link org.batfish.datamodel.DataPlane}.
 */
public interface TopologyContainer {
  @Nonnull
  BgpTopology getBgpTopology();

  @Nonnull
  EigrpTopology getEigrpTopology();

  @Nonnull
  IsisTopology getIsisTopology();

  @Nonnull
  Optional<Layer2Topology> getLayer2Topology();

  @Nonnull
  Topology getLayer3Topology();

  @Nonnull
  OspfTopology getOspfTopology();

  @Nonnull
  VxlanTopology getVxlanTopology();

  /** See {@link TunnelTopology} */
  @Nonnull
  TunnelTopology getTunnelTopology();
}
