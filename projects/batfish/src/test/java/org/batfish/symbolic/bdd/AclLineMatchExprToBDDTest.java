package org.batfish.symbolic.bdd;

import static org.batfish.symbolic.bdd.BDDMatchers.isOne;
import static org.batfish.symbolic.bdd.BDDMatchers.isZero;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.sf.javabdd.BDD;
import org.batfish.common.BatfishException;
import org.batfish.common.util.NonRecursiveSupplier;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.State;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AclLineMatchExprToBDDTest {
  @Rule public ExpectedException exception = ExpectedException.none();

  private AclLineMatchExprToBDD _toBDD;

  private BDDPacket _pkt;

  @Before
  public void setup() {
    _pkt = new BDDPacket();
    _toBDD =
        new AclLineMatchExprToBDD(BDDPacket.factory, _pkt, ImmutableMap.of(), ImmutableMap.of());
  }

  @Test
  public void testMatchHeaderSpace_unconstrained() {
    assertThat(_toBDD.visit(new MatchHeaderSpace(HeaderSpace.builder().build())), isOne());
  }

  @Test
  public void testMatchHeaderSpace_srcOrDstIps() {
    Ip srcOrDstIp = new Ip("1.2.3.4");
    HeaderSpace headerSpace = HeaderSpace.builder().setSrcOrDstIps(srcOrDstIp.toIpSpace()).build();
    BDD bdd = _toBDD.visit(new MatchHeaderSpace(headerSpace));

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
    bdd = _toBDD.visit(new MatchHeaderSpace(headerSpace));
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
    bdd = _toBDD.visit(new MatchHeaderSpace(headerSpace));
    assertThat(bdd, isZero());
  }

  @Test
  public void testMatchHeaderSpace_srcOrDstPorts() {
    SubRange portRange = new SubRange(10, 20);
    HeaderSpace headerSpace =
        HeaderSpace.builder().setSrcOrDstPorts(ImmutableList.of(portRange)).build();
    AclLineMatchExpr matchExpr = new MatchHeaderSpace(headerSpace);
    BDD bdd = _toBDD.visit(matchExpr);

    BDDInteger dstPort = _pkt.getDstPort();
    BDD dstPortBDD = dstPort.leq(20).and(dstPort.geq(10));
    BDDInteger srcPort = _pkt.getSrcPort();
    BDD srcPortBDD = srcPort.leq(20).and(srcPort.geq(10));
    assertThat(bdd, equalTo(dstPortBDD.or(srcPortBDD)));
  }

  @Test
  public void testPermittedByAcl() {
    Ip fooIp = new Ip("1.1.1.1");
    BDD fooIpBDD = _pkt.getSrcIp().value(fooIp.asLong());
    PermittedByAcl permittedByAcl = new PermittedByAcl("foo");
    Map<String, Supplier<BDD>> namedAclBDDs = ImmutableMap.of("foo", () -> fooIpBDD);
    AclLineMatchExprToBDD toBDD =
        new AclLineMatchExprToBDD(BDDPacket.factory, _pkt, namedAclBDDs, ImmutableMap.of());
    assertThat(permittedByAcl.accept(toBDD), equalTo(fooIpBDD));
  }

  @Test
  public void testPermittedByAcl_undefined() {
    PermittedByAcl permittedByAcl = new PermittedByAcl("foo");
    AclLineMatchExprToBDD toBDD =
        new AclLineMatchExprToBDD(BDDPacket.factory, _pkt, ImmutableMap.of(), ImmutableMap.of());
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Undefined PermittedByAcl reference: foo");
    permittedByAcl.accept(toBDD);
  }

  @Test
  public void testPermittedByAcl_circular() {
    PermittedByAcl permittedByAcl = new PermittedByAcl("foo");
    Map<String, Supplier<BDD>> namedAclBDDs = new HashMap<>();
    namedAclBDDs.put("foo", new NonRecursiveSupplier<>(() -> namedAclBDDs.get("foo").get()));
    AclLineMatchExprToBDD toBDD =
        new AclLineMatchExprToBDD(BDDPacket.factory, _pkt, namedAclBDDs, ImmutableMap.of());
    exception.expect(BatfishException.class);
    exception.expectMessage("Circular PermittedByAcl reference: foo");
    permittedByAcl.accept(toBDD);
  }

  @Test
  public void testMatchHeaderSpace_dscps() {
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setDscps(ImmutableSet.of(1, 2, 3))
            .setNotDscps(ImmutableSet.of(3, 4, 5))
            .build();
    AclLineMatchExpr matchExpr = new MatchHeaderSpace(headerSpace);
    BDD bdd = _toBDD.visit(matchExpr);

    BDDInteger dscp = _pkt.getDscp();
    BDD dscpBDD = dscp.value(1).or(dscp.value(2));
    assertThat(bdd, equalTo(dscpBDD));
  }

  @Test
  public void testMatchHeaderSpace_ecns() {
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setEcns(ImmutableSet.of(0, 1))
            .setNotEcns(ImmutableSet.of(1, 2))
            .build();
    AclLineMatchExpr matchExpr = new MatchHeaderSpace(headerSpace);
    BDD bdd = _toBDD.visit(matchExpr);

    BDDInteger ecn = _pkt.getEcn();
    BDD ecnBDD = ecn.value(0);
    assertThat(bdd, equalTo(ecnBDD));
  }

  @Test
  public void testMatchHeaderSpace_fragmentOffsets() {
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setFragmentOffsets(ImmutableSet.of(new SubRange(0, 5)))
            .setNotFragmentOffsets(ImmutableSet.of(new SubRange(2, 6)))
            .build();
    AclLineMatchExpr matchExpr = new MatchHeaderSpace(headerSpace);
    BDD bdd = _toBDD.visit(matchExpr);

    BDDInteger fragmentOffset = _pkt.getFragmentOffset();
    BDD fragmentOffsetBDD = fragmentOffset.value(0).or(fragmentOffset.value(1));
    assertThat(bdd, equalTo(fragmentOffsetBDD));
  }

  @Test
  public void testMatchHeaderSpace_state() {
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setStates(ImmutableSet.of(State.fromNum(0), State.fromNum(1)))
            .build();
    AclLineMatchExpr matchExpr = new MatchHeaderSpace(headerSpace);
    BDD bdd = _toBDD.visit(matchExpr);

    BDDInteger state = _pkt.getState();
    BDD stateBDD = state.value(0).or(state.value(1));
    assertThat(bdd, equalTo(stateBDD));
  }
}
