package org.batfish.symbolic.bdd;

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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Optional;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.junit.Test;

public class BDDPacketTest {
  @Test
  public void testAllocateBDDBit() {
    BDDPacket.factory.reset();
    BDDPacket pkt = new BDDPacket();
    int varNum = BDDPacket.factory.varNum();
    BDD bdd = pkt.allocateBDDBit("foo");
    assertThat(bdd, notNullValue());
    assertThat(BDDPacket.factory.varNum(), equalTo(varNum + 1));
  }

  @Test
  public void testAllocateBDDInteger() {
    BDDPacket.factory.reset();
    BDDPacket pkt = new BDDPacket();
    int varNum = BDDPacket.factory.varNum();
    BDDInteger var = pkt.allocateBDDInteger("foo", 5, false);
    assertThat(var, notNullValue());
    assertThat(BDDPacket.factory.varNum(), equalTo(varNum + 5));
  }

  @Test
  public void testGetFlow_empty() {
    BDDPacket pkt = new BDDPacket();
    assertThat(pkt.getFlow(BDDPacket.factory.zero()), equalTo(Optional.empty()));
  }

  @Test
  public void testGetFlow() {
    BDDPacket pkt = new BDDPacket();
    Ip dstIp = new Ip("123.456.789.0");
    int dstPort = 0xA0;

    Ip srcIp = new Ip("255.255.255.255");
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
    assertThat("Unsat", flowBuilder.isPresent());
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
}
