package org.batfish.z3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.z3.Context;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import org.batfish.config.Settings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.ForwardingAction;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.TrueExpr;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Run a reachability query on a simple network with two possible origination VRFs. Test that we get
 * one flow for each origination VRF.
 */
public class NodJobChunkingTest {
  private NetworkFactory _nf;
  private SortedMap<String, Configuration> _configs;
  private DataPlane _dataPlane;
  private Configuration _dstNode;
  private Configuration _srcNode1;
  private Configuration _srcNode2;
  private Vrf _srcVrf1;
  private Vrf _srcVrf2;
  private Synthesizer _synthesizer;
  private IngressLocation _ingressLocation1;
  private IngressLocation _ingressLocation2;
  private IngressLocation _ingressLocation3;
  private String _ifaceName;

  @Before
  public void setup() throws IOException {
    setupConfigs();
    setupDataPlane();
    setupSynthesizer();
  }

  private IpAccessList mkOutgoingFilter(Configuration owner, Ip srcIp) {
    return _nf.aclBuilder()
        .setOwner(owner)
        .setLines(
            ImmutableList.of(
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setSrcIps(ImmutableList.of(new IpWildcard(srcIp)))
                        .build())))
        .build();
  }

  private void setupConfigs() {
    _nf = new NetworkFactory();
    Configuration.Builder cb =
        _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Interface.Builder ib = _nf.interfaceBuilder().setActive(true).setBandwidth(1E9d);
    Vrf.Builder vb = _nf.vrfBuilder();

    _srcNode1 = cb.build();
    _srcNode2 = cb.build();
    _srcVrf1 = vb.setOwner(_srcNode1).build();
    _srcVrf2 = vb.setOwner(_srcNode2).build();
    _ingressLocation1 = IngressLocation.vrf(_srcNode1.getHostname(), _srcVrf1.getName());
    _ingressLocation2 = IngressLocation.vrf(_srcNode2.getHostname(), _srcVrf2.getName());
    _dstNode = cb.build();
    Vrf dstVrf = vb.setOwner(_dstNode).build();

    Prefix p1 = Prefix.parse("1.0.0.0/31");
    Interface iface =
        ib.setOwner(_srcNode1)
            .setVrf(_srcVrf1)
            .setAddress(new InterfaceAddress(p1.getStartIp(), p1.getPrefixLength()))
            // require traffic srcNode1 -> dstNode to have srcIp == p1.getStartIp
            .setOutgoingFilter(mkOutgoingFilter(_srcNode1, p1.getStartIp()))
            .build();

    _ifaceName = iface.getName();
    _ingressLocation3 = IngressLocation.interfaceLink(_srcNode1.getHostname(), _ifaceName);

    ib.setOwner(_dstNode)
        .setVrf(dstVrf)
        .setAddress(new InterfaceAddress(p1.getEndIp(), p1.getPrefixLength()))
        .setSourceNats(ImmutableList.of())
        .build();

    Prefix p2 = Prefix.parse("2.0.0.0/31");
    ib.setOwner(_srcNode2)
        .setVrf(_srcVrf2)
        .setAddress(new InterfaceAddress(p2.getStartIp(), p2.getPrefixLength()))
        // require traffic srcNode2 -> dstNode to have srcIp == p2.getStartIp
        .setOutgoingFilter(mkOutgoingFilter(_srcNode2, p2.getStartIp()))
        .build();
    ib.setOwner(_dstNode)
        .setVrf(dstVrf)
        .setAddress(new InterfaceAddress(p2.getEndIp(), p2.getPrefixLength()))
        .setSourceNats(ImmutableList.of())
        .build();

    // For the destination
    Prefix pDest = Prefix.parse("3.0.0.0/32");
    ib.setOwner(_dstNode)
        .setVrf(dstVrf)
        .setAddress(new InterfaceAddress(pDest.getEndIp(), pDest.getPrefixLength()))
        .build();

    StaticRoute.Builder bld = StaticRoute.builder().setNetwork(pDest);
    _srcVrf1.getStaticRoutes().add(bld.setNextHopIp(p1.getEndIp()).build());
    _srcVrf2.getStaticRoutes().add(bld.setNextHopIp(p2.getEndIp()).build());

    _configs =
        ImmutableSortedMap.of(
            _srcNode1.getHostname(), _srcNode1,
            _srcNode2.getHostname(), _srcNode2,
            _dstNode.getHostname(), _dstNode);
  }

  private void setupDataPlane() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    Batfish batfish = BatfishTestUtils.getBatfish(_configs, tmp);
    batfish.computeDataPlane(false);
    _dataPlane = batfish.loadDataPlane();
  }

  private void setupSynthesizer() {
    _synthesizer =
        new Synthesizer(
            Batfish.computeSynthesizerInput(
                _configs,
                _dataPlane,
                new HeaderSpace(),
                IpSpaceAssignment.empty(),
                ImmutableSortedSet.of(),
                ImmutableSortedSet.of(),
                true,
                false));
  }

  private NodJob getNodJob() {
    Map<IngressLocation, BooleanExpr> srcIpConstraints =
        ImmutableMap.of(
            IngressLocation.interfaceLink(_srcNode1.getHostname(), _ifaceName),
            TrueExpr.INSTANCE,
            IngressLocation.vrf(_srcNode1.getHostname(), _srcVrf1.getName()),
            TrueExpr.INSTANCE,
            IngressLocation.vrf(_srcNode2.getHostname(), _srcVrf2.getName()),
            TrueExpr.INSTANCE);
    StandardReachabilityQuerySynthesizer querySynthesizer =
        StandardReachabilityQuerySynthesizer.builder()
            .setActions(ImmutableSet.of(ForwardingAction.ACCEPT))
            .setHeaderSpace(new HeaderSpace())
            .setSrcIpConstraints(srcIpConstraints)
            .setFinalNodes(ImmutableSet.of(_dstNode.getHostname()))
            .build();

    return new NodJob(
        new Settings(), _synthesizer, querySynthesizer, srcIpConstraints, "tag", true);
  }

  @Test
  public void testChunking() {
    NodJob nodJob = getNodJob();

    Context z3Context = new Context();
    SmtInput smtInput = nodJob.computeSmtInput(System.currentTimeMillis(), z3Context);

    Map<IngressLocation, Map<String, Long>> fieldConstraintsByIngressPoint =
        nodJob.getSolutionPerIngressLocation(z3Context, smtInput);
    assertThat(fieldConstraintsByIngressPoint.entrySet(), hasSize(3));
    assertThat(fieldConstraintsByIngressPoint, hasKey(_ingressLocation1));
    assertThat(fieldConstraintsByIngressPoint, hasKey(_ingressLocation2));
    assertThat(fieldConstraintsByIngressPoint, hasKey(_ingressLocation3));
    Map<String, Long> fieldConstraints1 = fieldConstraintsByIngressPoint.get(_ingressLocation1);
    Map<String, Long> fieldConstraints2 = fieldConstraintsByIngressPoint.get(_ingressLocation2);
    Map<String, Long> fieldConstraints3 = fieldConstraintsByIngressPoint.get(_ingressLocation3);

    assertThat(
        fieldConstraints1,
        hasEntry(IngressLocationInstrumentation.INGRESS_LOCATION_FIELD_NAME, 1L));
    assertThat(fieldConstraints1, hasEntry(Field.SRC_IP.getName(), new Ip("1.0.0.0").asLong()));
    assertThat(
        fieldConstraints2,
        hasEntry(IngressLocationInstrumentation.INGRESS_LOCATION_FIELD_NAME, 2L));
    assertThat(fieldConstraints2, hasEntry(Field.SRC_IP.getName(), new Ip("2.0.0.0").asLong()));
    assertThat(
        fieldConstraints3,
        hasEntry(IngressLocationInstrumentation.INGRESS_LOCATION_FIELD_NAME, 0L));
    assertThat(fieldConstraints3, hasEntry(Field.SRC_IP.getName(), new Ip("1.0.0.0").asLong()));
  }
}
