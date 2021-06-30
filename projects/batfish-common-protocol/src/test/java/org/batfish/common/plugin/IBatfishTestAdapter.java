package org.batfish.common.plugin;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishLogger;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.topology.IpOwners;
import org.batfish.common.topology.L3Adjacencies;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.TopologyProvider;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.common.topology.TunnelTopology;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.DataPlaneAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.ipsec.IpsecTopology;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.vxlan.VxlanTopology;
import org.batfish.datamodel.vxlan.VxlanTopologyUtils;
import org.batfish.grammar.BgpTableFormat;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.question.ReachabilityParameters;
import org.batfish.question.bidirectionalreachability.BidirectionalReachabilityResult;
import org.batfish.question.differentialreachability.DifferentialReachabilityParameters;
import org.batfish.question.differentialreachability.DifferentialReachabilityResult;
import org.batfish.question.multipath.MultipathConsistencyParameters;
import org.batfish.referencelibrary.ReferenceLibrary;
import org.batfish.role.NodeRoleDimension;
import org.batfish.role.NodeRolesData;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationInfo;
import org.batfish.specifier.SpecifierContext;
import org.batfish.specifier.SpecifierContextImpl;
import org.batfish.vendor.VendorConfiguration;

/**
 * A helper for tests that need an {@link IBatfish} implementation. Extend this and implement the
 * minimal methods needed.
 */
public class IBatfishTestAdapter implements IBatfish {

  public static class TopologyProviderTestAdapter implements TopologyProvider {

    protected final IBatfish _batfish;

    public TopologyProviderTestAdapter(IBatfish batfish) {
      _batfish = batfish;
    }

    @Nonnull
    @Override
    public IpOwners getIpOwners(NetworkSnapshot snapshot) {
      return new IpOwners(_batfish.loadConfigurations(snapshot));
    }

    @Override
    public Optional<Layer1Topology> getLayer1LogicalTopology(NetworkSnapshot networkSnapshot) {
      return getLayer1PhysicalTopology(networkSnapshot)
          .map(
              l1PhysicalTopology ->
                  TopologyUtil.computeLayer1LogicalTopology(
                      l1PhysicalTopology, _batfish.loadConfigurations(networkSnapshot)));
    }

    @Override
    public VxlanTopology getVxlanTopology(NetworkSnapshot snapshot) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public TunnelTopology getInitialTunnelTopology(NetworkSnapshot snapshot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Layer1Topology> getLayer1PhysicalTopology(NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Layer2Topology> getLayer2Topology(NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Topology getLayer3Topology(NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public L3Adjacencies getL3Adjacencies(NetworkSnapshot snapshot) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public OspfTopology getOspfTopology(NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public OspfTopology getInitialOspfTopology(@Nonnull NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Layer1Topology> getRawLayer1PhysicalTopology(NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Optional<Layer1Topology> getSynthesizedLayer1Topology(NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Topology getRawLayer3Topology(NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public BgpTopology getBgpTopology(NetworkSnapshot snapshot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public IpsecTopology getInitialIpsecTopology(NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Layer2Topology> getInitialLayer2Topology(NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Topology getInitialLayer3Topology(NetworkSnapshot networkSnapshot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public VxlanTopology getInitialVxlanTopology(NetworkSnapshot snapshot) {
      return VxlanTopologyUtils.computeVxlanTopology(_batfish.loadConfigurations(snapshot));
    }
  }

  @Override
  public DifferentialReachabilityResult bddDifferentialReachability(
      NetworkSnapshot snapshot,
      NetworkSnapshot reference,
      DifferentialReachabilityParameters parameters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedMap<Flow, List<Trace>> buildFlows(
      NetworkSnapshot snapshot, Set<Flow> flows, boolean ignoreFilters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataPlaneAnswerElement computeDataPlane(NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean debugFlagEnabled(String flag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<Location, LocationInfo> getLocationInfo(NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ReferenceLibrary getReferenceLibraryData() {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getNetworkObject(NetworkId networkId, String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getSnapshotInputObject(NetworkSnapshot snapshot, String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NetworkId getContainerName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataPlanePlugin getDataPlanePlugin() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Environment getEnvironment() {
    throw new UnsupportedOperationException();
  }

  @Override
  public BatfishLogger getLogger() {
    return null;
  }

  @Override
  public Optional<NodeRoleDimension> getNodeRoleDimension(String roleDimension) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NodeRolesData getNodeRolesData() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, String> getQuestionTemplates(boolean verbose) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableConfiguration getSettingsConfiguration() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getTaskId() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public TopologyProvider getTopologyProvider() {
    return new TopologyProviderTestAdapter(this);
  }

  @Override
  public InitInfoAnswerElement initInfo(
      NetworkSnapshot snapshot, boolean summary, boolean verboseError) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InitInfoAnswerElement initInfoBgpAdvertisements(
      NetworkSnapshot snapshot, boolean summary, boolean verboseError) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedMap<String, Configuration> loadConfigurations(NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, VendorConfiguration> loadVendorConfigurations(NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElementOrReparse(
      NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataPlane loadDataPlane(NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SortedMap<String, BgpAdvertisementsByVrf> loadEnvironmentBgpTables(
      NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<BgpAdvertisement> loadExternalBgpAnnouncements(
      NetworkSnapshot snapshot, Map<String, Configuration> configurations) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement(
      NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AtomicInteger newBatch(String description, int jobs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TracerouteEngine getTracerouteEngine(NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public String readExternalBgpAnnouncementsFile(NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerAnswerer(
      String questionName,
      String questionClassName,
      BiFunction<Question, IBatfish, Answerer> answererCreator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerBgpTablePlugin(BgpTableFormat format, BgpTablePlugin bgpTablePlugin) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerDataPlanePlugin(DataPlanePlugin plugin, String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerExternalBgpAdvertisementPlugin(
      ExternalBgpAdvertisementPlugin externalBgpAdvertisementPlugin) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SpecifierContext specifierContext(NetworkSnapshot networkSnapshot) {
    return new SpecifierContextImpl(this, networkSnapshot);
  }

  @Override
  public AnswerElement standard(
      NetworkSnapshot snapshot, ReachabilityParameters reachabilityParameters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Flow> bddLoopDetection(NetworkSnapshot snapshot) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Flow> bddMultipathConsistency(
      NetworkSnapshot snapshot, MultipathConsistencyParameters parameters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable Answerer createAnswerer(@Nonnull Question question) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NetworkSnapshot getSnapshot() {
    if (_snapshot == null) {
      _snapshot = new NetworkSnapshot(new NetworkId("net"), new SnapshotId("ss"));
    }
    return _snapshot;
  }

  @Override
  public NetworkSnapshot getReferenceSnapshot() {
    if (_referenceSnapshot == null) {
      _referenceSnapshot = new NetworkSnapshot(new NetworkId("net"), new SnapshotId("ref"));
    }
    return _referenceSnapshot;
  }

  @Override
  public @Nonnull BidirectionalReachabilityResult bidirectionalReachability(
      NetworkSnapshot snapshot, BDDPacket bddPacket, ReachabilityParameters parameters) {
    throw new UnsupportedOperationException();
  }

  private NetworkSnapshot _snapshot;
  private NetworkSnapshot _referenceSnapshot;
}
