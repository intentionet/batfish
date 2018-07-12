package org.batfish.symbolic.bdd;

import static org.batfish.symbolic.bdd.BDDMatchers.isOne;
import static org.batfish.symbolic.bdd.BDDMatchers.isZero;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.junit.Before;
import org.junit.Test;

public class AclLineMatchExprToBDDTest {
  private AclLineMatchExprToBDD _toBDD;

  private BDDPacket _pkt;

  @Before
  public void setup() {
    _pkt = new BDDPacket();
    _toBDD = new AclLineMatchExprToBDD(BDDPacket.factory, _pkt);
  }

  @Test
  public void testMatchHeaderSpace_unconstrained() {
    assertThat(_toBDD.toBDD(new MatchHeaderSpace(HeaderSpace.builder().build())), isOne());
  }

  @Test
  public void testMatchHeaderSpace_srcOrDstIps() {
    Ip srcOrDstIp = new Ip("1.2.3.4");
    HeaderSpace headerSpace = HeaderSpace.builder().setSrcOrDstIps(srcOrDstIp.toIpSpace()).build();
    BDD bdd = _toBDD.toBDD(new MatchHeaderSpace(headerSpace));

    BDD dstIpBDD = _pkt.getDstIp().value(srcOrDstIp.asLong());
    BDD srcIpBDD = _pkt.getSrcIp().value(srcOrDstIp.asLong());
    assertThat(bdd, equalTo(dstIpBDD.or(srcIpBDD)));

    // force srcIp to be srcOrDstIp
    Ip dstIp = new Ip("1.1.1.1");
    headerSpace =
        HeaderSpace.builder()
            .setDstIps(dstIp.toIpSpace())
            .setSrcOrDstIps(srcOrDstIp.toIpSpace())
            .build();
    bdd = _toBDD.toBDD(new MatchHeaderSpace(headerSpace));
    dstIpBDD = _pkt.getDstIp().value(dstIp.asLong());
    assertThat(bdd, equalTo(srcIpBDD.and(dstIpBDD)));

    // neither can be srcOrDstIp. unsatisfiable
    Ip srcIp = new Ip("2.2.2.2");
    headerSpace =
        HeaderSpace.builder()
            .setDstIps(dstIp.toIpSpace())
            .setSrcIps(srcIp.toIpSpace())
            .setSrcOrDstIps(srcOrDstIp.toIpSpace())
            .build();
    bdd = _toBDD.toBDD(new MatchHeaderSpace(headerSpace));
    assertThat(bdd, isZero());
  }

  @Test
  public void testMatchHeaderSpace_srcOrDstPorts() {
    SubRange portRange = new SubRange(10, 20);
    HeaderSpace headerSpace =
        HeaderSpace.builder().setSrcOrDstPorts(ImmutableList.of(portRange)).build();
    AclLineMatchExpr matchExpr = new MatchHeaderSpace(headerSpace);
    BDD bdd = _toBDD.toBDD(matchExpr);

    BDDInteger dstPort = _pkt.getDstPort();
    BDD dstPortBDD = dstPort.leq(20).and(dstPort.geq(10));
    BDDInteger srcPort = _pkt.getSrcPort();
    BDD srcPortBDD = srcPort.leq(20).and(srcPort.geq(10));
    assertThat(bdd, equalTo(dstPortBDD.or(srcPortBDD)));
  }
}
