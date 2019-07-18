package org.batfish.dataplane.ibdp;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.TopologyContainer;
import org.batfish.common.topology.TunnelTopology;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.ipsec.IpsecTopology;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.vxlan.VxlanTopology;

/** Container for various topologies used during data plane computation. */
@ParametersAreNonnullByDefault
public final class TopologyContext implements TopologyContainer {

  public static final class Builder {

    private @Nonnull BgpTopology _bgpTopology;
    private @Nonnull EigrpTopology _eigrpTopology;
    private @Nonnull IpsecTopology _ipsecTopology;
    private @Nonnull IsisTopology _isisTopology;
    private @Nonnull Optional<Layer1Topology> _layer1LogicalTopology;
    private @Nonnull Optional<Layer2Topology> _layer2Topology;
    private @Nonnull Topology _layer3Topology;
    private @Nonnull OspfTopology _ospfTopology;
    private @Nonnull Optional<Layer1Topology> _rawLayer1PhysicalTopology;
    @Nonnull private TunnelTopology _tunnelTopology;
    private @Nonnull VxlanTopology _vxlanTopology;

    public @Nonnull TopologyContext build() {
      return new TopologyContext(
          _bgpTopology,
          _eigrpTopology,
          _ipsecTopology,
          _isisTopology,
          _layer1LogicalTopology,
          _layer2Topology,
          _layer3Topology,
          _ospfTopology,
          _rawLayer1PhysicalTopology,
          _tunnelTopology,
          _vxlanTopology);
    }

    private Builder() {
      _bgpTopology = BgpTopology.EMPTY;
      _eigrpTopology = EigrpTopology.EMPTY;
      _ipsecTopology = IpsecTopology.EMPTY;
      _isisTopology = IsisTopology.EMPTY;
      _layer1LogicalTopology = Optional.empty();
      _layer2Topology = Optional.empty();
      _layer3Topology = Topology.EMPTY;
      _ospfTopology = OspfTopology.EMPTY;
      _rawLayer1PhysicalTopology = Optional.empty();
      _tunnelTopology = TunnelTopology.EMPTY;
      _vxlanTopology = VxlanTopology.EMPTY;
    }

    public @Nonnull Builder setBgpTopology(BgpTopology bgpTopology) {
      _bgpTopology = bgpTopology;
      return this;
    }

    public @Nonnull Builder setEigrpTopology(EigrpTopology eigrpTopology) {
      _eigrpTopology = eigrpTopology;
      return this;
    }

    public @Nonnull Builder setIpsecTopology(@Nonnull IpsecTopology ipsecTopology) {
      _ipsecTopology = ipsecTopology;
      return this;
    }

    public @Nonnull Builder setIsisTopology(IsisTopology isisTopology) {
      _isisTopology = isisTopology;
      return this;
    }

    public @Nonnull Builder setLayer1LogicalTopology(
        Optional<Layer1Topology> layer1LogicalTopology) {
      _layer1LogicalTopology = layer1LogicalTopology;
      return this;
    }

    public @Nonnull Builder setLayer2Topology(Optional<Layer2Topology> layer2Topology) {
      _layer2Topology = layer2Topology;
      return this;
    }

    public @Nonnull Builder setLayer3Topology(Topology layer3Topology) {
      _layer3Topology = layer3Topology;
      return this;
    }

    public @Nonnull Builder setOspfTopology(OspfTopology ospfTopology) {
      _ospfTopology = ospfTopology;
      return this;
    }

    public @Nonnull Builder setRawLayer1PhysicalTopology(
        Optional<Layer1Topology> rawLayer1PhysicalTopology) {
      _rawLayer1PhysicalTopology = rawLayer1PhysicalTopology;
      return this;
    }

    public Builder setTunnelTopology(TunnelTopology tunnelTopology) {
      _tunnelTopology = tunnelTopology;
      return this;
    }

    public @Nonnull Builder setVxlanTopology(VxlanTopology vxlanTopology) {
      _vxlanTopology = vxlanTopology;
      return this;
    }
  }

  public static @Nonnull Builder builder() {
    return new Builder();
  }

  private final @Nonnull BgpTopology _bgpTopology;
  private final @Nonnull EigrpTopology _eigrpTopology;
  private final @Nonnull IpsecTopology _ipsecTopology;
  private final @Nonnull IsisTopology _isisTopology;
  private final @Nonnull Optional<Layer1Topology> _layer1LogicalTopology;
  private final @Nonnull Optional<Layer2Topology> _layer2Topology;
  private final @Nonnull Topology _layer3Topology;
  private final @Nonnull OspfTopology _ospfTopology;
  private final @Nonnull Optional<Layer1Topology> _rawLayer1PhysicalTopology;
  @Nonnull private final TunnelTopology _tunnelTopology;
  private final @Nonnull VxlanTopology _vxlanTopology;

  private TopologyContext(
      BgpTopology bgpTopology,
      EigrpTopology eigrpTopology,
      IpsecTopology ipsecTopology,
      IsisTopology isisTopology,
      Optional<Layer1Topology> layer1LogicalTopology,
      Optional<Layer2Topology> layer2Topology,
      Topology layer3Topology,
      OspfTopology ospfTopology,
      Optional<Layer1Topology> rawLayer1PhysicalTopology,
      TunnelTopology tunnelTopology,
      VxlanTopology vxlanTopology) {
    _bgpTopology = bgpTopology;
    _eigrpTopology = eigrpTopology;
    _ipsecTopology = ipsecTopology;
    _isisTopology = isisTopology;
    _layer1LogicalTopology = layer1LogicalTopology;
    _layer2Topology = layer2Topology;
    _layer3Topology = layer3Topology;
    _ospfTopology = ospfTopology;
    _rawLayer1PhysicalTopology = rawLayer1PhysicalTopology;
    _vxlanTopology = vxlanTopology;
    _tunnelTopology = tunnelTopology;
  }

  @Override
  public @Nonnull BgpTopology getBgpTopology() {
    return _bgpTopology;
  }

  @Override
  public @Nonnull EigrpTopology getEigrpTopology() {
    return _eigrpTopology;
  }

  public @Nonnull IpsecTopology getIpsecTopology() {
    return _ipsecTopology;
  }

  @Override
  public @Nonnull IsisTopology getIsisTopology() {
    return _isisTopology;
  }

  public @Nonnull Optional<Layer1Topology> getLayer1LogicalTopology() {
    return _layer1LogicalTopology;
  }

  @Override
  public @Nonnull Optional<Layer2Topology> getLayer2Topology() {
    return _layer2Topology;
  }

  @Override
  public @Nonnull Topology getLayer3Topology() {
    return _layer3Topology;
  }

  @Override
  public @Nonnull OspfTopology getOspfTopology() {
    return _ospfTopology;
  }

  public @Nonnull Optional<Layer1Topology> getRawLayer1PhysicalTopology() {
    return _rawLayer1PhysicalTopology;
  }

  @Override
  public @Nonnull VxlanTopology getVxlanTopology() {
    return _vxlanTopology;
  }

  @Override
  @Nonnull
  public TunnelTopology getTunnelTopology() {
    return _tunnelTopology;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TopologyContext)) {
      return false;
    }
    TopologyContext rhs = (TopologyContext) obj;
    return _bgpTopology.equals(rhs._bgpTopology)
        && _eigrpTopology.equals(rhs._eigrpTopology)
        && _ipsecTopology.equals(rhs._ipsecTopology)
        && _isisTopology.equals(rhs._isisTopology)
        && _layer1LogicalTopology.equals(rhs._layer1LogicalTopology)
        && _layer2Topology.equals(rhs._layer2Topology)
        && _layer3Topology.equals(rhs._layer3Topology)
        && _ospfTopology.equals(rhs._ospfTopology)
        && _rawLayer1PhysicalTopology.equals(rhs._rawLayer1PhysicalTopology)
        && _tunnelTopology.equals(rhs._tunnelTopology)
        && _vxlanTopology.equals(rhs._vxlanTopology);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _bgpTopology,
        _eigrpTopology,
        _ipsecTopology,
        _isisTopology,
        _layer1LogicalTopology,
        _layer2Topology,
        _layer3Topology,
        _ospfTopology,
        _rawLayer1PhysicalTopology,
        _tunnelTopology,
        _vxlanTopology);
  }

  public @Nonnull Builder toBuilder() {
    return builder()
        .setBgpTopology(_bgpTopology)
        .setEigrpTopology(_eigrpTopology)
        .setIpsecTopology(_ipsecTopology)
        .setIsisTopology(_isisTopology)
        .setLayer1LogicalTopology(_layer1LogicalTopology)
        .setLayer2Topology(_layer2Topology)
        .setLayer3Topology(_layer3Topology)
        .setOspfTopology(_ospfTopology)
        .setRawLayer1PhysicalTopology(_rawLayer1PhysicalTopology)
        .setTunnelTopology(_tunnelTopology)
        .setVxlanTopology(_vxlanTopology);
  }
}
