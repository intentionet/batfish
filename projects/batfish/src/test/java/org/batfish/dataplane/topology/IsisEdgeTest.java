package org.batfish.dataplane.topology;

import static org.batfish.common.util.CommonUtil.synthesizeTopology;
import static org.batfish.datamodel.isis.IsisLevel.LEVEL_1_2;
import static org.batfish.datamodel.isis.IsisLevel.LEVEL_2;
import static org.batfish.dataplane.topology.matchers.IsisEdgeMatchers.hasCircuitType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.isis.IsisProcess;
import org.junit.Test;

public final class IsisEdgeTest {

  private static final String AREA1 = "0001";

  private static final String AREA2 = "0002";

  private static final String SYSTEM1 = "0100.0000.0000";

  private static final String SYSTEM2 = "0100.0000.0001";

  private static IsoAddress generateAddress(@Nonnull String area, @Nonnull String system) {
    return new IsoAddress(String.format("49.%s.%s.00", area, system));
  }

  private static Set<IsisEdge> getIsisEdges(Map<String, Configuration> configurations) {
    Topology topology = synthesizeTopology(configurations);
    return topology
        .getEdges()
        .stream()
        .map(edge -> IsisEdge.edgeIfCircuit(edge, configurations))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(ImmutableSet.toImmutableSet());
  }

  private Map<String, Configuration> generateIsisConfigs(boolean sameArea, boolean sameSystem) {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    IsisLevelSettings levelSettings = IsisLevelSettings.builder().build();
    IsisProcess.Builder isb =
        IsisProcess.builder().setLevel1(levelSettings).setLevel2(levelSettings);
    IsisInterfaceLevelSettings isisInterfaceLevelSettings =
        IsisInterfaceLevelSettings.builder().setMode(IsisInterfaceMode.ACTIVE).build();
    IsisInterfaceSettings isisInterfaceSettings =
        IsisInterfaceSettings.builder()
            .setPointToPoint(true)
            .setLevel1(isisInterfaceLevelSettings)
            .setLevel2(isisInterfaceLevelSettings)
            .build();
    Interface.Builder ib = nf.interfaceBuilder().setActive(true).setIsis(isisInterfaceSettings);

    Configuration c1 = cb.build();
    Vrf v1 = vb.setOwner(c1).build();
    isb.setVrf(v1).setNetAddress(generateAddress(AREA1, SYSTEM1)).build();
    ib.setOwner(c1).setVrf(v1).setAddress(new InterfaceAddress("10.0.0.0/31")).build();

    Configuration c2 = cb.build();
    Vrf v2 = vb.setOwner(c2).build();
    isb.setVrf(v2)
        .setNetAddress(generateAddress(sameArea ? AREA1 : AREA2, sameSystem ? SYSTEM1 : SYSTEM2))
        .build();
    ib.setOwner(c2).setVrf(v2).setAddress(new InterfaceAddress("10.0.0.1/31")).build();
    return ImmutableMap.of(c1.getName(), c1, c2.getName(), c2);
  }

  @Test
  public void testSameAreaEffect() {
    Set<IsisEdge> isisEdgesSameArea = getIsisEdges(generateIsisConfigs(true, false));
    Set<IsisEdge> isisEdgesDifferentArea = getIsisEdges(generateIsisConfigs(false, false));

    // With same area, circuit should be L1L2. With different areas, circuit should just be L2.
    assertThat(
        isisEdgesSameArea,
        contains(ImmutableList.of(hasCircuitType(LEVEL_1_2), hasCircuitType(LEVEL_1_2))));
    assertThat(
        isisEdgesDifferentArea,
        contains(ImmutableList.of(hasCircuitType(LEVEL_2), hasCircuitType(LEVEL_2))));
  }

  @Test
  public void testSameSystemEffect() {
    Set<IsisEdge> isisEdgesSameSystemId = getIsisEdges(generateIsisConfigs(true, true));
    Set<IsisEdge> isisEdgesDifferentSystemId = getIsisEdges(generateIsisConfigs(true, false));

    // Circuit should be established iff system IDs differ
    assertThat(isisEdgesSameSystemId, empty());
    assertThat(
        isisEdgesDifferentSystemId,
        contains(ImmutableList.of(hasCircuitType(LEVEL_1_2), hasCircuitType(LEVEL_1_2))));
  }
}
