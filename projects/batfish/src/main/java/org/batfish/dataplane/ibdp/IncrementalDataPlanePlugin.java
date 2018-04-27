package org.batfish.dataplane.ibdp;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.BdpAnswerElement;
import org.batfish.datamodel.collections.IbgpTopology;
import org.batfish.dataplane.TraceRouteEngineImpl;

@AutoService(Plugin.class)
public class IncrementalDataPlanePlugin extends DataPlanePlugin {

  public static final String PLUGIN_NAME = "ibdp";

  private final Map<IncrementalDataPlane, Map<Flow, Set<FlowTrace>>> _flowTraces;

  private IncrementalBdpEngine _engine;

  public IncrementalDataPlanePlugin() {
    _flowTraces = new HashMap<>();
  }

  @Override
  public ComputeDataPlaneResult computeDataPlane(boolean differentialContext) {
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Topology topology = _batfish.getEnvironmentTopology();
    return computeDataPlane(differentialContext, configurations, topology);
  }

  @Override
  public ComputeDataPlaneResult computeDataPlane(
      boolean differentialContext, Map<String, Configuration> configurations, Topology topology) {
    Answer answer = new Answer();
    BdpAnswerElement ae = new BdpAnswerElement();
    answer.addAnswerElement(ae);
    Set<BgpAdvertisement> externalAdverts = _batfish.loadExternalBgpAnnouncements(configurations);
    IncrementalDataPlane dp =
        _engine.computeDataPlane(
            differentialContext, configurations, topology, externalAdverts, ae);
    double averageRoutes =
        dp.getNodes()
            .values()
            .stream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .mapToInt(vr -> vr._mainRib.getRoutes().size())
            .average()
            .orElse(0.00d);
    _logger.infof(
        "Generated data-plane for testrig:%s; iterations:%s, avg entries per node:%.2f\n",
        _batfish.getTestrigName(), ae.getDependentRoutesIterations(), averageRoutes);
    return new ComputeDataPlaneResult(ae, dp);
  }

  @Override
  protected void dataPlanePluginInitialize() {
    _engine =
        new IncrementalBdpEngine(
            new IncrementalDataPlaneSettings(_batfish.getSettingsConfiguration()),
            _batfish.getLogger(),
            _batfish::newBatch);
  }

  @Override
  public Set<BgpAdvertisement> getAdvertisements() {
    Set<BgpAdvertisement> adverts = new LinkedHashSet<>();
    IncrementalDataPlane dp = loadDataPlane();
    for (Node node : dp._nodes.values()) {
      for (VirtualRouter vrf : node.getVirtualRouters().values()) {
        adverts.addAll(vrf._receivedBgpAdvertisements);
        adverts.addAll(vrf._sentBgpAdvertisements);
      }
    }
    return adverts;
  }

  @Override
  public List<Flow> getHistoryFlows(DataPlane dataPlane) {
    IncrementalDataPlane dp = (IncrementalDataPlane) dataPlane;
    Map<Flow, Set<FlowTrace>> traces = _flowTraces.get(dp);
    if (traces == null) {
      return ImmutableList.of();
    }
    return traces
        .entrySet()
        .stream()
        .flatMap(e -> Collections.nCopies(e.getValue().size(), e.getKey()).stream())
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<FlowTrace> getHistoryFlowTraces(DataPlane dataPlane) {
    IncrementalDataPlane dp = (IncrementalDataPlane) dataPlane;
    Map<Flow, Set<FlowTrace>> traces = _flowTraces.get(dp);
    if (traces == null) {
      return ImmutableList.of();
    }
    return traces.values().stream().flatMap(Set::stream).collect(ImmutableList.toImmutableList());
  }

  @Override
  public IbgpTopology getIbgpNeighbors() {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getRoutes(DataPlane dp) {
    return _engine.getRoutes((IncrementalDataPlane) dp);
  }

  @Override
  public void processFlows(Set<Flow> flows, DataPlane dataPlane, boolean ignoreAcls) {
    _flowTraces.put(
        (IncrementalDataPlane) dataPlane,
        TraceRouteEngineImpl.getInstance()
            .processFlows(dataPlane, flows, dataPlane.getFibs(), ignoreAcls));
  }

  private IncrementalDataPlane loadDataPlane() {
    return (IncrementalDataPlane) _batfish.loadDataPlane();
  }

  @Override
  public String getName() {
    return PLUGIN_NAME;
  }
}
