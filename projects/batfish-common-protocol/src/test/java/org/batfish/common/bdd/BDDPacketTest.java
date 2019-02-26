package org.batfish.common.bdd;

import static org.batfish.datamodel.matchers.FlowMatchers.hasDstIp;
import static org.batfish.datamodel.matchers.FlowMatchers.hasDstPort;
import static org.batfish.datamodel.matchers.FlowMatchers.hasIcmpCode;
import static org.batfish.datamodel.matchers.FlowMatchers.hasIcmpType;
import static org.batfish.datamodel.matchers.FlowMatchers.hasIpProtocol;
import static org.batfish.datamodel.matchers.FlowMatchers.hasSrcIp;
import static org.batfish.datamodel.matchers.FlowMatchers.hasSrcPort;
import static org.batfish.datamodel.matchers.FlowMatchers.hasTcpFlagsAck;
import static org.batfish.datamodel.matchers.FlowMatchers.hasTcpFlagsCwr;
import static org.batfish.datamodel.matchers.FlowMatchers.hasTcpFlagsEce;
import static org.batfish.datamodel.matchers.FlowMatchers.hasTcpFlagsFin;
import static org.batfish.datamodel.matchers.FlowMatchers.hasTcpFlagsPsh;
import static org.batfish.datamodel.matchers.FlowMatchers.hasTcpFlagsRst;
import static org.batfish.datamodel.matchers.FlowMatchers.hasTcpFlagsUrg;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.junit.Test;

public class BDDPacketTest {
  @Test
  public void testAllocateBDDBit() {
    BDDPacket pkt = new BDDPacket();
    int varNum = pkt.getFactory().varNum();
    BDD bdd = pkt.allocateBDDBit("foo");
    assertThat(bdd, notNullValue());
    assertThat(pkt.getFactory().varNum(), equalTo(varNum + 1));
  }

  @Test
  public void testAllocateBDDInteger() {
    BDDPacket pkt = new BDDPacket();
    int varNum = pkt.getFactory().varNum();
    BDDInteger var = pkt.allocateBDDInteger("foo", 5, false);
    assertThat(var, notNullValue());
    assertThat(pkt.getFactory().varNum(), equalTo(varNum + 5));
  }

  @Test
  public void testGetFlow_empty() {
    BDDPacket pkt = new BDDPacket();
    assertThat(pkt.getFlow(pkt.getFactory().zero()), equalTo(Optional.empty()));
  }

  @Test
  public void testGetFlow() {
    BDDPacket pkt = new BDDPacket();
    Ip dstIp = Ip.parse("123.45.78.0");
    int dstPort = 0xA0;

    Ip srcIp = Ip.parse("255.255.255.255");
    int srcPort = 0xFF;

    int icmpCode = 0x00;
    int icmpType = 0x01;
    IpProtocol ipProtocol = IpProtocol.IPV6_ICMP;
    int tcpAck = 0;
    int tcpCwr = 1;
    int tcpEce = 0;
    int tcpFin = 1;
    int tcpPsh = 0;
    int tcpRst = 1;
    int tcpUrg = 0;

    BDD bdd =
        pkt.getDstIp()
            .value(dstIp.asLong())
            .and(pkt.getDstPort().value(dstPort))
            .and(pkt.getIcmpCode().value(icmpCode))
            .and(pkt.getIcmpType().value(icmpType))
            .and(pkt.getIpProtocol().value(ipProtocol.number()))
            .and(pkt.getSrcIp().value(srcIp.asLong()))
            .and(pkt.getSrcPort().value(srcPort))
            .and(pkt.getTcpAck().not())
            .and(pkt.getTcpCwr())
            .and(pkt.getTcpEce().not())
            .and(pkt.getTcpFin())
            .and(pkt.getTcpPsh().not())
            .and(pkt.getTcpRst())
            .and(pkt.getTcpUrg().not());

    Optional<Flow.Builder> flowBuilder = pkt.getFlow(bdd);
    assertTrue("Unsat", flowBuilder.isPresent());
    Flow flow = flowBuilder.get().setIngressNode("ingressNode").setTag("tag").build();

    assertThat(flow, hasDstIp(dstIp));
    assertThat(flow, hasDstPort(dstPort));
    assertThat(flow, hasIcmpCode(icmpCode));
    assertThat(flow, hasIcmpType(icmpType));
    assertThat(flow, hasIpProtocol(ipProtocol));
    assertThat(flow, hasSrcIp(srcIp));
    assertThat(flow, hasSrcPort(srcPort));
    assertThat(flow, hasTcpFlagsAck(tcpAck));
    assertThat(flow, hasTcpFlagsCwr(tcpCwr));
    assertThat(flow, hasTcpFlagsEce(tcpEce));
    assertThat(flow, hasTcpFlagsFin(tcpFin));
    assertThat(flow, hasTcpFlagsPsh(tcpPsh));
    assertThat(flow, hasTcpFlagsRst(tcpRst));
    assertThat(flow, hasTcpFlagsUrg(tcpUrg));
  }

  @Test
  public void testGetFlowPreference1() {
    BDDPacket pkt = new BDDPacket();
    Ip dstIp = Ip.parse("123.45.78.0");
    Ip srcIp = Ip.parse("1.2.3.4");

    BDD bdd = pkt.getDstIp().value(dstIp.asLong()).and(pkt.getSrcIp().value(srcIp.asLong()));

    Optional<Flow.Builder> flowBuilder = pkt.getFlow(bdd);
    assertTrue("Unsat", flowBuilder.isPresent());
    Flow flow = flowBuilder.get().setIngressNode("ingressNode").setTag("tag").build();

    assertThat(flow, hasDstIp(dstIp));
    assertThat(flow, hasSrcIp(srcIp));
    assertThat(flow, hasIpProtocol(IpProtocol.ICMP));
    assertThat(flow, hasIcmpType(8));
    assertThat(flow, hasIcmpCode(0));
  }

  @Test
  public void testGetFlowPreference2() {
    BDDPacket pkt = new BDDPacket();
    Ip dstIp = Ip.parse("123.45.78.0");
    Ip srcIp = Ip.parse("1.2.3.4");

    BDD bdd = pkt.getDstIp().value(dstIp.asLong()).and(pkt.getSrcIp().value(srcIp.asLong()));

    BDD icmpbdd = pkt.getIpProtocol().value(IpProtocol.ICMP.number());

    bdd = bdd.and(icmpbdd.not());

    Optional<Flow.Builder> flowBuilder = pkt.getFlow(bdd);
    assertTrue("Unsat", flowBuilder.isPresent());
    Flow flow = flowBuilder.get().setIngressNode("ingressNode").setTag("tag").build();

    assertThat(flow, hasDstIp(dstIp));
    assertThat(flow, hasSrcIp(srcIp));
    assertThat(flow, hasIpProtocol(IpProtocol.UDP));
  }

  @Test
  public void testGetFlowPreference3() {
    BDDPacket pkt = new BDDPacket();
    Ip dstIp = Ip.parse("123.45.78.0");
    Ip srcIp = Ip.parse("1.2.3.4");

    BDD bdd = pkt.getDstIp().value(dstIp.asLong()).and(pkt.getSrcIp().value(srcIp.asLong()));

    BDD icmpbdd = pkt.getIpProtocol().value(IpProtocol.ICMP.number());

    BDD udpbdd = pkt.getIpProtocol().value(IpProtocol.UDP.number());

    bdd = bdd.and(icmpbdd.not()).and(udpbdd.not());

    Optional<Flow.Builder> flowBuilder = pkt.getFlow(bdd);
    assertTrue("Unsat", flowBuilder.isPresent());
    Flow flow = flowBuilder.get().setIngressNode("ingressNode").setTag("tag").build();

    assertThat(flow, hasDstIp(dstIp));
    assertThat(flow, hasSrcIp(srcIp));
    assertThat(flow, hasIpProtocol(IpProtocol.TCP));
    assertThat(flow, not(hasDstPort(0)));
  }

  @Test
  public void testSwapSourceAndDestinationFields() {
    BDDPacket pkt = new BDDPacket();
    BDDInteger dstIp = pkt.getDstIp();
    BDDInteger srcIp = pkt.getSrcIp();
    BDDInteger dstPort = pkt.getDstPort();
    BDDInteger srcPort = pkt.getSrcPort();

    Ip ip1 = Ip.parse("1.1.1.1");
    Ip ip2 = Ip.parse("2.2.2.2");
    Ip ip3 = Ip.parse("3.3.3.3");
    Ip ip4 = Ip.parse("4.4.4.4");

    BiFunction<BDDInteger, BDDInteger, Function<BDDInteger, BDD>> mkBdd =
        (ipVar1, ipVar2) ->
            portVar ->
                (ipVar1.value(ip1.asLong()).and(ipVar2.value(ip2.asLong())).and(portVar.value(0)))
                    .or(
                        ipVar1
                            .value(ip3.asLong())
                            .and(ipVar2.value(ip4.asLong()))
                            .and(ipVar1.value(1)));

    BDD orig = mkBdd.apply(dstIp, srcIp).apply(dstPort);
    BDD swapped = mkBdd.apply(srcIp, dstIp).apply(srcPort);
    assertThat(pkt.swapSourceAndDestinationFields(orig), equalTo(swapped));
  }
}
