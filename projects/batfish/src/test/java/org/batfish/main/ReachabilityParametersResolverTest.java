package org.batfish.main;

import static org.batfish.datamodel.ConfigurationFormat.CISCO_IOS;
import static org.batfish.datamodel.ForwardingAction.ACCEPT;
import static org.batfish.main.ReachabilityParametersResolver.resolveReachabilityParameters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.SortedMap;
import org.batfish.common.NetworkSnapshot;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.questions.InvalidReachabilityParametersException;
import org.batfish.question.ReachabilityParameters;
import org.batfish.question.ReachabilityParameters.Builder;
import org.batfish.question.ResolvedReachabilityParameters;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.ConstantIpSpaceSpecifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReachabilityParametersResolverTest {
  @Rule public TemporaryFolder _tempFolder = new TemporaryFolder();

  private Batfish _batfish;

  private NetworkSnapshot _snapshot;

  @Before
  public void setup() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration config = nf.configurationBuilder().setConfigurationFormat(CISCO_IOS).build();
    Vrf vrf = nf.vrfBuilder().setOwner(config).build();
    nf.interfaceBuilder().setOwner(config).setVrf(vrf).build();
    SortedMap<String, Configuration> configs = ImmutableSortedMap.of(config.getHostname(), config);
    _batfish = BatfishTestUtils.getBatfish(configs, _tempFolder);
    _batfish.computeDataPlane(false);
    _snapshot = _batfish.getNetworkSnapshot();
  }

  @Test
  public void testDestinationIpSpace() throws InvalidReachabilityParametersException {
    Builder reachabilityParametersBuilder =
        ReachabilityParameters.builder()
            .setActions(ImmutableSortedSet.of(ACCEPT))
            .setFinalNodesSpecifier(AllNodesNodeSpecifier.INSTANCE);

    // test default destination IpSpace
    ReachabilityParameters reachabilityParameters = reachabilityParametersBuilder.build();
    ResolvedReachabilityParameters resolvedReachabilityParameters =
        resolveReachabilityParameters(_batfish, reachabilityParameters, _snapshot);
    assertThat(resolvedReachabilityParameters.getHeaderSpace().getNotDstIps(), nullValue());
    assertThat(
        resolvedReachabilityParameters.getHeaderSpace().getDstIps(),
        equalTo(UniverseIpSpace.INSTANCE));

    // test setting destination IpSpace
    reachabilityParameters =
        reachabilityParametersBuilder
            .setDestinationIpSpaceSpecifier(new ConstantIpSpaceSpecifier(EmptyIpSpace.INSTANCE))
            .build();
    resolvedReachabilityParameters =
        resolveReachabilityParameters(_batfish, reachabilityParameters, _snapshot);
    assertThat(resolvedReachabilityParameters.getHeaderSpace().getNotDstIps(), nullValue());
    assertThat(
        resolvedReachabilityParameters.getHeaderSpace().getDstIps(),
        equalTo(EmptyIpSpace.INSTANCE));
  }
}
