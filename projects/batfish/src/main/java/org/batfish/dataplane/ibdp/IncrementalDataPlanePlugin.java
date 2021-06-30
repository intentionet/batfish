package org.batfish.dataplane.ibdp;

import com.google.auto.service.AutoService;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.topology.GlobalBroadcastNoPointToPoint;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.LegacyL3Adjacencies;
import org.batfish.common.topology.TopologyProvider;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.answers.IncrementalBdpAnswerElement;
import org.batfish.datamodel.isis.IsisTopology;

/** A batfish plugin that registers the Incremental Batfish Data Plane (ibdp) Engine. */
@AutoService(Plugin.class)
public final class IncrementalDataPlanePlugin extends DataPlanePlugin {

  private static final Logger LOGGER = LogManager.getLogger(IncrementalDataPlanePlugin.class);

  public static final String PLUGIN_NAME = "ibdp";

  private IncrementalBdpEngine _engine;

  public IncrementalDataPlanePlugin() {}

  @Override
  public ComputeDataPlaneResult computeDataPlane(NetworkSnapshot snapshot) {
    Map<String, Configuration> configurations = _batfish.loadConfigurations(snapshot);
    Set<BgpAdvertisement> externalAdverts =
        _batfish.loadExternalBgpAnnouncements(snapshot, configurations);

    LOGGER.info("Building topology for data-plane");
    TopologyProvider topologyProvider = _batfish.getTopologyProvider();
    Optional<Layer1Topology> rawL1 = topologyProvider.getRawLayer1PhysicalTopology(snapshot);
    Optional<Layer1Topology> l1 = topologyProvider.getLayer1LogicalTopology(snapshot);
    Optional<Layer2Topology> l2 = topologyProvider.getInitialLayer2Topology(snapshot);
    TopologyContext topologyContext =
        TopologyContext.builder()
            .setIpsecTopology(topologyProvider.getInitialIpsecTopology(snapshot))
            .setIsisTopology(
                IsisTopology.initIsisTopology(
                    configurations, topologyProvider.getInitialLayer3Topology(snapshot)))
            .setLayer1LogicalTopology(l1)
            .setLayer2Topology(l2)
            .setLayer3Topology(topologyProvider.getInitialLayer3Topology(snapshot))
            .setL3Adjacencies(
                l2.isPresent()
                    ? new LegacyL3Adjacencies(
                        rawL1.orElse(Layer1Topology.EMPTY),
                        l1.orElse(Layer1Topology.EMPTY),
                        l2.get(),
                        configurations)
                    : GlobalBroadcastNoPointToPoint.instance())
            .setOspfTopology(topologyProvider.getInitialOspfTopology(snapshot))
            .setRawLayer1PhysicalTopology(rawL1)
            .setTunnelTopology(topologyProvider.getInitialTunnelTopology(snapshot))
            .build();

    ComputeDataPlaneResult answer =
        _engine.computeDataPlane(configurations, topologyContext, externalAdverts);
    _logger.infof(
        "Generated data-plane for snapshot:%s; iterations:%s",
        snapshot.getSnapshot(),
        ((IncrementalBdpAnswerElement) answer._answerElement).getDependentRoutesIterations());
    return answer;
  }

  @Override
  protected void dataPlanePluginInitialize() {
    _engine =
        new IncrementalBdpEngine(
            new IncrementalDataPlaneSettings(_batfish.getSettingsConfiguration()));
  }

  @Override
  public String getName() {
    return PLUGIN_NAME;
  }
}
