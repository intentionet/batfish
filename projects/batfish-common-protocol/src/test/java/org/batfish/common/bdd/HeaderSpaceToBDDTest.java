package org.batfish.common.bdd;

import static org.batfish.common.bdd.BDDMatchers.isOne;
import static org.batfish.common.bdd.BDDMatchers.isZero;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.FlowState;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.junit.Before;
import org.junit.Test;

public class HeaderSpaceToBDDTest {
  private HeaderSpaceToBDD _toBDD;

  private BDDPacket _pkt;

  private IpSpaceToBDD _dstIpSpaceToBdd;

  @Before
  public void setup() {
    _pkt = new BDDPacket();
    _toBDD = new HeaderSpaceToBDD(_pkt, ImmutableMap.of());
    _dstIpSpaceToBdd = new IpSpaceToBDD(_pkt.getDstIp());
  }

  @Test
  public void test_unconstrained() {
    assertThat(_toBDD.toBDD(HeaderSpace.builder().build()), isOne());
  }

  @Test
  public void test_negate() {
    IpSpace ip = Ip.parse("1.2.3.4").toIpSpace();
    BDD ipBDD = HeaderSpaceToBDD.toBDD(ip, _dstIpSpaceToBdd);
    assertThat(
        _toBDD.toBDD(HeaderSpace.builder().setDstIps(ip).setNegate(true).build()),
        equalTo(ipBDD.not()));
  }

  @Test
  public void test_srcOrDstIps() {
    Ip srcOrDstIp = Ip.parse("1.2.3.4");
    HeaderSpace headerSpace = HeaderSpace.builder().setSrcOrDstIps(srcOrDstIp.toIpSpace()).build();
    BDD bdd = _toBDD.toBDD(headerSpace);

    BDD dstIpBDD = _pkt.getDstIp().value(srcOrDstIp.asLong());
    BDD srcIpBDD = _pkt.getSrcIp().value(srcOrDstIp.asLong());
    assertThat(bdd, equalTo(dstIpBDD.or(srcIpBDD)));

    // force srcIp to be srcOrDstIp
    Ip dstIp = Ip.parse("1.1.1.1");
    headerSpace =
        HeaderSpace.builder()
            .setDstIps(dstIp.toIpSpace())
            .setSrcOrDstIps(srcOrDstIp.toIpSpace())
            .build();
    bdd = _toBDD.toBDD(headerSpace);
    dstIpBDD = _pkt.getDstIp().value(dstIp.asLong());
    assertThat(bdd, equalTo(srcIpBDD.and(dstIpBDD)));

    // neither can be srcOrDstIp. unsatisfiable
    Ip srcIp = Ip.parse("2.2.2.2");
    headerSpace =
        HeaderSpace.builder()
            .setDstIps(dstIp.toIpSpace())
            .setSrcIps(srcIp.toIpSpace())
            .setSrcOrDstIps(srcOrDstIp.toIpSpace())
            .build();
    bdd = _toBDD.toBDD(headerSpace);
    assertThat(bdd, isZero());
  }

  @Test
  public void test_srcOrDstPorts() {
    SubRange portRange = new SubRange(10, 20);
    HeaderSpace headerSpace =
        HeaderSpace.builder().setSrcOrDstPorts(ImmutableList.of(portRange)).build();
    BDD bdd = _toBDD.toBDD(headerSpace);

    BDDInteger dstPort = _pkt.getDstPort();
    BDD dstPortBDD = dstPort.range(10, 20);
    BDDInteger srcPort = _pkt.getSrcPort();
    BDD srcPortBDD = srcPort.range(10, 20);
    assertThat(bdd, equalTo(dstPortBDD.or(srcPortBDD)));
  }

  @Test
  public void test_dscps() {
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setDscps(ImmutableSet.of(1, 2, 3))
            .setNotDscps(ImmutableSet.of(3, 4, 5))
            .build();
    BDD bdd = _toBDD.toBDD(headerSpace);

    BDDInteger dscp = _pkt.getDscp();
    BDD dscpBDD = dscp.value(1).or(dscp.value(2));
    assertThat(bdd, equalTo(dscpBDD));
  }

  @Test
  public void test_ecns() {
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setEcns(ImmutableSet.of(0, 1))
            .setNotEcns(ImmutableSet.of(1, 2))
            .build();
    BDD bdd = _toBDD.toBDD(headerSpace);

    BDDInteger ecn = _pkt.getEcn();
    BDD ecnBDD = ecn.value(0);
    assertThat(bdd, equalTo(ecnBDD));
  }

  @Test
  public void test_fragmentOffsets() {
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setFragmentOffsets(ImmutableSet.of(new SubRange(0, 5)))
            .setNotFragmentOffsets(ImmutableSet.of(new SubRange(2, 6)))
            .build();
    BDD bdd = _toBDD.toBDD(headerSpace);

    BDDInteger fragmentOffset = _pkt.getFragmentOffset();
    BDD fragmentOffsetBDD = fragmentOffset.value(0).or(fragmentOffset.value(1));
    assertThat(bdd, equalTo(fragmentOffsetBDD));
  }

  @Test
  public void test_icmpType() {
    HeaderSpace headerSpace =
        HeaderSpace.builder().setIcmpTypes(ImmutableList.of(new SubRange(8, 8))).build();
    BDD matchExprBDD = _toBDD.toBDD(headerSpace);
    BDD icmpTypeBDD = _pkt.getIcmpType().value(8);
    assertThat(matchExprBDD, equalTo(icmpTypeBDD));
  }

  @Test
  public void test_ipProtocols() {
    IpProtocol proto1 = IpProtocol.TCP;
    IpProtocol proto2 = IpProtocol.UDP;
    HeaderSpace headerSpace =
        HeaderSpace.builder().setIpProtocols(ImmutableList.of(proto1, proto2)).build();
    BDD bdd = _toBDD.toBDD(headerSpace);
    BDD protoBDD = _pkt.getIpProtocol().value(proto1).or(_pkt.getIpProtocol().value(proto2));
    assertThat(bdd, equalTo(protoBDD));
  }

  @Test
  public void test_state() {
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setStates(ImmutableSet.of(FlowState.fromNum(0), FlowState.fromNum(1)))
            .build();
    BDD bdd = _toBDD.toBDD(headerSpace);

    BDDInteger state = _pkt.getState();
    BDD stateBDD = state.value(0).or(state.value(1));
    assertThat(bdd, equalTo(stateBDD));
  }

  @Test
  public void test_tcpFlags() {
    TcpFlagsMatchConditions flags1 =
        TcpFlagsMatchConditions.builder()
            .setUseAck(true)
            .setUseEce(true)
            .setTcpFlags(TcpFlags.builder().setAck(true).setEce(false).build())
            .build();
    TcpFlagsMatchConditions flags2 =
        TcpFlagsMatchConditions.builder()
            .setUseCwr(true)
            .setUseFin(true)
            .setTcpFlags(TcpFlags.builder().setCwr(true).setFin(false).build())
            .build();
    HeaderSpace headerSpace =
        HeaderSpace.builder().setTcpFlags(ImmutableList.of(flags1, flags2)).build();
    BDD bdd = _toBDD.toBDD(headerSpace);
    BDD tcpFlagsBDD =
        _pkt.getTcpAck()
            .and(_pkt.getTcpEce().not())
            .or(_pkt.getTcpCwr().and(_pkt.getTcpFin().not()));
    assertThat(bdd, equalTo(tcpFlagsBDD));
  }
}
