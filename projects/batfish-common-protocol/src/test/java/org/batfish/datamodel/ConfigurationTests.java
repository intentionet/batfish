package org.batfish.datamodel;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.CallStatement;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationTests {

  public static class HasRemoteIpsecVpn extends FeatureMatcher<IpsecVpn, IpsecVpn> {

    public HasRemoteIpsecVpn(Matcher<? super IpsecVpn> subMatcher) {
      super(subMatcher, "remoteIpsecVpn", "remoteIpsecVpn");
    }

    @Override
    protected IpsecVpn featureValueOf(IpsecVpn actual) {
      return actual.getRemoteIpsecVpn();
    }
  }

  public static HasRemoteIpsecVpn hasRemoteIpsecVpn(Matcher<? super IpsecVpn> subMatcher) {
    return new HasRemoteIpsecVpn(subMatcher);
  }

  private NetworkFactory _factory;

  @Before
  public void setup() {
    _factory = new NetworkFactory();
  }

  @Test
  public void testComputeRoutingPolicySources() {
    String bgpExportPolicyName = "bgpExportPolicy";
    String bgpImportPolicyName = "bgpImportPolicy";
    String bgpMissingExportPolicyName = "bgpMissingExportPolicy";
    String bgpMissingImportPolicyName = null;
    String generatedRouteAttributePolicyName = "generatedRouteAttributePolicy";
    String generatedRouteGenerationPolicyName = "generatedRouteGenerationPolicy";
    String ospfExportPolicyName = "ospfExportPolicy";
    String ospfExportSubPolicyName = "ospfExportSubPolicy";
    Prefix neighborPrefix = new Prefix(Ip.ZERO, Prefix.MAX_PREFIX_LENGTH);
    Prefix generatedRoutePrefix = neighborPrefix;
    Prefix neigborWithMissingPoliciesPrefix = new Prefix(Ip.MAX, Prefix.MAX_PREFIX_LENGTH);

    Configuration c = new Configuration("test", ConfigurationFormat.CISCO_IOS);
    Vrf vrf = c.getVrfs().computeIfAbsent(Configuration.DEFAULT_VRF_NAME, Vrf::new);

    // BGP
    BgpProcess bgpProcess = new BgpProcess();
    vrf.setBgpProcess(bgpProcess);
    BgpNeighbor neighbor =
        bgpProcess.getNeighbors().computeIfAbsent(neighborPrefix, BgpNeighbor::new);
    neighbor.setExportPolicy(
        c.getRoutingPolicies()
            .computeIfAbsent(bgpExportPolicyName, n -> new RoutingPolicy(n, c))
            .getName());
    neighbor.setImportPolicy(
        c.getRoutingPolicies()
            .computeIfAbsent(bgpImportPolicyName, n -> new RoutingPolicy(n, c))
            .getName());
    BgpNeighbor neighborWithMissingPolicies =
        bgpProcess
            .getNeighbors()
            .computeIfAbsent(neigborWithMissingPoliciesPrefix, BgpNeighbor::new);
    neighborWithMissingPolicies.setExportPolicy(bgpMissingExportPolicyName);
    neighborWithMissingPolicies.setImportPolicy(bgpMissingImportPolicyName);

    // Generated route
    GeneratedRoute gr =
        new GeneratedRoute.Builder()
            .setNetwork(generatedRoutePrefix)
            .setAttributePolicy(
                c.getRoutingPolicies()
                    .computeIfAbsent(
                        generatedRouteAttributePolicyName, n -> new RoutingPolicy(n, c))
                    .getName())
            .setGenerationPolicy(
                c.getRoutingPolicies()
                    .computeIfAbsent(
                        generatedRouteGenerationPolicyName, n -> new RoutingPolicy(n, c))
                    .getName())
            .build();
    vrf.getGeneratedRoutes().add(gr);

    // OSPF
    OspfProcess ospfProcess = new OspfProcess();
    vrf.setOspfProcess(ospfProcess);
    RoutingPolicy ospfExportPolicy =
        c.getRoutingPolicies().computeIfAbsent(ospfExportPolicyName, n -> new RoutingPolicy(n, c));
    ospfProcess.setExportPolicy(ospfExportPolicyName);
    ospfExportPolicy
        .getStatements()
        .add(
            new CallStatement(
                c.getRoutingPolicies()
                    .computeIfAbsent(ospfExportSubPolicyName, n -> new RoutingPolicy(n, c))
                    .getName()));

    // Compute policy sources
    Warnings w = new Warnings();
    c.computeRoutingPolicySources(w);

    // BGP tests
    assertThat(
        neighbor.getExportPolicySources(), equalTo(Collections.singleton(bgpExportPolicyName)));
    assertThat(
        neighbor.getImportPolicySources(), equalTo(Collections.singleton(bgpImportPolicyName)));
    assertThat(
        neighborWithMissingPolicies.getExportPolicySources(), equalTo(Collections.emptySet()));
    assertThat(
        neighborWithMissingPolicies.getImportPolicySources(), equalTo(Collections.emptySet()));
    // Generated route tests
    assertThat(
        gr.getAttributePolicySources(),
        equalTo(Collections.singleton(generatedRouteAttributePolicyName)));
    assertThat(
        gr.getGenerationPolicySources(),
        equalTo(Collections.singleton(generatedRouteGenerationPolicyName)));
    // OSPF tests
    assertThat(
        ospfProcess.getExportPolicySources(),
        containsInAnyOrder(ospfExportPolicyName, ospfExportSubPolicyName));
  }

  @Test
  public void testInitRemoteIpsecVpns() {
    Ip ip1to2Physical = new Ip("10.12.0.1");
    Ip ip1to2Tunnel = new Ip("10.0.12.1");
    Ip ip1to3Physical = new Ip("10.13.0.1");
    Ip ip1to4Tunnel = new Ip("10.0.14.1");

    Ip ip2to1Physical = new Ip("10.12.0.2");
    Ip ip2to1Tunnel = new Ip("10.0.12.2");

    Ip ip3to1Physical = new Ip("10.13.0.3");
    Ip ip3to4Physical = new Ip("10.34.0.3");

    Ip ip4to1Tunnel = new Ip("10.0.14.4");
    Ip ip4to3Physical = new Ip("10.34.0.4");

    Configuration.Builder cb =
        _factory.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Interface.Builder ib = _factory.interfaceBuilder().setActive(true);
    Vrf.Builder vb = _factory.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);

    Configuration c1 = cb.build();
    Vrf v1 = vb.setOwner(c1).build();
    NetworkAddress p1to2Physical = new NetworkAddress(ip1to2Physical, 24);
    NetworkAddress p1to2Tunnel = new NetworkAddress(ip1to2Tunnel, 24);
    NetworkAddress p1to3Physical = new NetworkAddress(ip1to3Physical, 24);
    NetworkAddress p1to4Tunnel = new NetworkAddress(ip1to4Tunnel, 24);
    Interface i1to2 = ib.setOwner(c1).setVrf(v1).setAddress(p1to2Physical).build();
    Interface t1to2 = ib.setAddress(p1to2Tunnel).build();
    Interface i1to3 = ib.setOwner(c1).setVrf(v1).setAddress(p1to3Physical).build();
    Interface t1to4 = ib.setAddress(p1to4Tunnel).build();
    IpsecVpn vpn1to2 = new IpsecVpn("vpn1to2", c1);
    c1.getIpsecVpns().put(vpn1to2.getName(), vpn1to2);
    vpn1to2.setBindInterface(t1to2);
    IkeGateway g1to2 = new IkeGateway("g1to2");
    c1.getIkeGateways().put(g1to2.getName(), g1to2);
    g1to2.setAddress(ip2to1Physical);
    g1to2.setLocalAddress(ip1to2Physical);
    g1to2.setExternalInterface(i1to2);
    vpn1to2.setIkeGateway(g1to2);
    IpsecVpn vpn1to4 = new IpsecVpn("vpn1to4", c1);
    c1.getIpsecVpns().put(vpn1to4.getName(), vpn1to4);
    vpn1to4.setBindInterface(t1to4);
    IkeGateway g1to4 = new IkeGateway("g1to4");
    c1.getIkeGateways().put(g1to4.getName(), g1to4);
    g1to4.setAddress(ip3to1Physical);
    g1to4.setLocalAddress(ip1to3Physical);
    g1to4.setExternalInterface(i1to3);
    vpn1to4.setIkeGateway(g1to4);

    Configuration c2 = cb.build();
    Vrf v2 = vb.setOwner(c2).build();
    NetworkAddress p2to1Physical = new NetworkAddress(ip2to1Physical, 24);
    NetworkAddress p2to1Tunnel = new NetworkAddress(ip2to1Tunnel, 24);
    Interface i2to1 = ib.setOwner(c2).setVrf(v2).setAddress(p2to1Physical).build();
    Interface t2to1 = ib.setAddress(p2to1Tunnel).build();
    IpsecVpn vpn2to1 = new IpsecVpn("vpn2to1", c2);
    c2.getIpsecVpns().put(vpn2to1.getName(), vpn2to1);
    vpn2to1.setBindInterface(t2to1);
    IkeGateway g2to1 = new IkeGateway("g2to1");
    c2.getIkeGateways().put(g2to1.getName(), g2to1);
    g2to1.setAddress(ip1to2Physical);
    g2to1.setLocalAddress(ip2to1Physical);
    g2to1.setExternalInterface(i2to1);
    vpn2to1.setIkeGateway(g2to1);

    Configuration c3 = cb.build();
    Vrf v3 = vb.setOwner(c3).build();
    NetworkAddress p3to1Physical = new NetworkAddress(ip3to1Physical, 24);
    NetworkAddress p3to4Physical = new NetworkAddress(ip3to4Physical, 24);
    Interface i3to1 = ib.setOwner(c3).setVrf(v3).setAddress(p3to1Physical).build();
    ib.setAddress(p3to4Physical).build();
    SourceNat snat3copy1 = new SourceNat();
    snat3copy1.setPoolIpFirst(ip3to1Physical);
    snat3copy1.setPoolIpLast(ip3to1Physical);
    SourceNat snat3copy2 = new SourceNat();
    snat3copy2.setPoolIpFirst(ip3to1Physical);
    snat3copy2.setPoolIpLast(ip3to1Physical);
    /* Should not crash with two source-nats aliasing the same public IP */
    i3to1.setSourceNats(ImmutableList.of(snat3copy1, snat3copy2));

    Configuration c4 = cb.build();
    Vrf v4 = vb.setOwner(c4).build();
    NetworkAddress p4to1Tunnel = new NetworkAddress(ip4to1Tunnel, 24);
    NetworkAddress p4to3Physical = new NetworkAddress(ip4to3Physical, 24);
    Interface t4to1 = ib.setOwner(c4).setVrf(v4).setAddress(p4to1Tunnel).build();
    Interface i4to3 = ib.setAddress(p4to3Physical).build();
    IpsecVpn vpn4to1 = new IpsecVpn("vpn4to1", c4);
    c4.getIpsecVpns().put(vpn4to1.getName(), vpn4to1);
    vpn4to1.setBindInterface(t4to1);
    IkeGateway g4 = new IkeGateway("g4to1");
    c4.getIkeGateways().put(g4.getName(), g4);
    g4.setAddress(ip1to3Physical);
    g4.setLocalAddress(ip4to3Physical);
    g4.setExternalInterface(i4to3);
    vpn4to1.setIkeGateway(g4);

    Map<String, Configuration> configurations =
        ImmutableMap.<String, Configuration>builder()
            .put(c1.getName(), c1)
            .put(c2.getName(), c2)
            .put(c3.getName(), c3)
            .put(c4.getName(), c4)
            .build();
    CommonUtil.initRemoteIpsecVpns(configurations);

    /*
     * vpn1to2 and vpn2to1 should connect to each other in a direct fashion
     */
    assertThat(vpn1to2, hasRemoteIpsecVpn(sameInstance(vpn2to1)));
    assertThat(vpn2to1, hasRemoteIpsecVpn(sameInstance(vpn1to2)));

    /*
     * vpn1to4 and vpn4to1 should connect over NAT. That is, vpn1to4 points to c1's interface facing
     * c3. c3's corresponding interface's ip is NAT'ed such that if c4 connects to c1, the reply is
     * translated back to the IP c4 is listening on for VPN v4to1.
     *
     */
    assertThat(vpn1to4, hasRemoteIpsecVpn(sameInstance(vpn4to1)));
    assertThat(vpn4to1, hasRemoteIpsecVpn(sameInstance(vpn1to4)));
  }
}
