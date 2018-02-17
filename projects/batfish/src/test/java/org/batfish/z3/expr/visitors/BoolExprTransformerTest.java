package org.batfish.z3.expr.visitors;

import static com.google.common.collect.ImmutableList.of;
import static org.batfish.z3.expr.visitors.BitVecExprTransformer.toBitVecExpr;
import static org.batfish.z3.expr.visitors.BoolExprTransformer.toBoolExpr;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import com.google.common.collect.ImmutableSet;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Protocol;
import org.batfish.datamodel.State;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlags;
import org.batfish.z3.BasicHeaderField;
import org.batfish.z3.NodContext;
import org.batfish.z3.ReachabilityProgram;
import org.batfish.z3.SynthesizerInput;
import org.batfish.z3.TestSynthesizerInput;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BasicRuleStatement;
import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.CurrentIsOriginalExpr;
import org.batfish.z3.expr.EqExpr;
import org.batfish.z3.expr.FalseExpr;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.IfExpr;
import org.batfish.z3.expr.IntExpr;
import org.batfish.z3.expr.NotExpr;
import org.batfish.z3.expr.OrExpr;
import org.batfish.z3.expr.PrefixMatchExpr;
import org.batfish.z3.expr.RangeMatchExpr;
import org.batfish.z3.expr.SaneExpr;
import org.batfish.z3.expr.TestBooleanAtom;
import org.batfish.z3.expr.TestIntAtom;
import org.batfish.z3.expr.TransformationRuleStatement;
import org.batfish.z3.expr.TransformationStateExpr;
import org.batfish.z3.expr.TrueExpr;
import org.batfish.z3.state.Accept;
import org.batfish.z3.state.PostOutInterface;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Test transformation of batfish reachability AST BooleanExpr nodes to Z3 AST BoolExpr nodes */
public class BoolExprTransformerTest {

  private int _atomCounter;

  private BasicStateExpr _basicStateExpr;

  private Context _ctx;

  private SynthesizerInput _input;

  private NodContext _nodContext;

  private TransformationStateExpr _transformationStateExpr;

  private BooleanExpr newBooleanAtom() {
    return new TestBooleanAtom(_atomCounter++);
  }

  private IntExpr newIntAtom() {
    return new TestIntAtom(_atomCounter++, 32);
  }

  @Before
  public void setup() {
    _basicStateExpr = Accept.INSTANCE;
    _transformationStateExpr = new PostOutInterface("host1", "interface1");
    _ctx = new Context();
    _input = TestSynthesizerInput.builder().build();
    _nodContext =
        new NodContext(
            _ctx,
            ReachabilityProgram.builder()
                .setInput(_input)
                .setRules(
                    of(
                        new BasicRuleStatement(_basicStateExpr),
                        new TransformationRuleStatement(_transformationStateExpr)))
                .build());
  }

  @After
  public void tearDown() {
    _ctx.close();
  }

  @Test
  public void testVisitAndExpr() {
    BooleanExpr p1Batfish = newBooleanAtom();
    BooleanExpr p2Batfish = newBooleanAtom();
    BoolExpr p1Z3 = toBoolExpr(p1Batfish, _input, _nodContext);
    BoolExpr p2Z3 = toBoolExpr(p2Batfish, _input, _nodContext);

    assertThat(
        toBoolExpr(new AndExpr(of(p1Batfish, p2Batfish)), _input, _nodContext),
        equalTo(_ctx.mkAnd(p1Z3, p2Z3)));
  }

  @Test
  public void testVisitBasicStateExpr() {
    assertThat(toBoolExpr(_basicStateExpr, _input, _nodContext), instanceOf(BoolExpr.class));
  }

  @Test
  public void testVisitCurrentIsOriginalExpr() {
    assertThat(
        toBoolExpr(CurrentIsOriginalExpr.INSTANCE, _input, _nodContext),
        instanceOf(BoolExpr.class));
  }

  @Test
  public void testVisitEqExpr() {
    IntExpr i1Batfish = newIntAtom();
    IntExpr i2Batfish = newIntAtom();
    BitVecExpr i1Z3 = toBitVecExpr(i1Batfish, _nodContext);
    BitVecExpr i2Z3 = toBitVecExpr(i2Batfish, _nodContext);

    assertThat(
        toBoolExpr(new EqExpr(i1Batfish, i2Batfish), _input, _nodContext),
        equalTo(_ctx.mkEq(i1Z3, i2Z3)));
  }

  @Test
  public void testVisitFalseExpr() {
    assertThat(toBoolExpr(FalseExpr.INSTANCE, _input, _nodContext), equalTo(_ctx.mkFalse()));
  }

  @Test
  public void testVisitHeaderSpaceMatchExpr() {
    long ipCounter = 1L;
    int intCounter = 1;
    HeaderSpace.Builder<?, ?> hb = IpAccessListLine.builder();

    BooleanExpr expr =
        new HeaderSpaceMatchExpr(
            hb.setDscps(ImmutableSet.of(intCounter++, intCounter++))
                .setDstIps(
                    ImmutableSet.of(
                        new IpWildcard(new Ip(ipCounter++)), new IpWildcard(new Ip(ipCounter++))))
                .setDstPorts(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setDstProtocols(ImmutableSet.of(Protocol.DNS, Protocol.HTTP))
                .setEcns(ImmutableSet.of(intCounter++, intCounter++))
                .setFragmentOffsets(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setIcmpCodes(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setIcmpTypes(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setIpProtocols(ImmutableSet.of(IpProtocol.AHP, IpProtocol.ARGUS))
                .setNegate(true)
                .setNotDscps(ImmutableSet.of(intCounter++, intCounter++))
                .setNotDstIps(
                    ImmutableSet.of(
                        new IpWildcard(new Ip(ipCounter++)), new IpWildcard(new Ip(ipCounter++))))
                .setNotDstPorts(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setNotDstProtocols(ImmutableSet.of(Protocol.HTTPS, Protocol.TELNET))
                .setNotEcns(ImmutableSet.of(intCounter++, intCounter++))
                .setNotFragmentOffsets(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setNotIcmpCodes(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setNotIcmpTypes(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setNotIpProtocols(ImmutableSet.of(IpProtocol.BNA, IpProtocol.XNET))
                .setNotPacketLengths(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setNotSrcIps(
                    ImmutableSet.of(
                        new IpWildcard(new Ip(ipCounter++)), new IpWildcard(new Ip(ipCounter++))))
                .setNotSrcPorts(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setNotSrcProtocols(ImmutableSet.of(Protocol.SSH, Protocol.TCP))
                .setPacketLengths(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setSrcIps(
                    ImmutableSet.of(
                        new IpWildcard(new Ip(ipCounter++)), new IpWildcard(new Ip(ipCounter++))))
                .setSrcOrDstIps(
                    ImmutableSet.of(
                        new IpWildcard(new Ip(ipCounter++)), new IpWildcard(new Ip(ipCounter++))))
                .setSrcOrDstPorts(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setSrcOrDstProtocols(ImmutableSet.of(Protocol.UDP, Protocol.HTTP))
                .setSrcPorts(
                    ImmutableSet.of(
                        new SubRange(intCounter++, intCounter++),
                        new SubRange(intCounter++, intCounter++)))
                .setSrcProtocols(ImmutableSet.of(Protocol.HTTPS, Protocol.DNS))
                .setStates(ImmutableSet.of(State.ESTABLISHED, State.NEW))
                .setTcpFlags(
                    ImmutableSet.of(
                        TcpFlags.builder().setAck(true).setUseAck(true).build(),
                        TcpFlags.builder().setUseCwr(true).build()))
                .build());

    assertThat(toBoolExpr(expr, _input, _nodContext), instanceOf(BoolExpr.class));
  }

  @Test
  public void testVisitIfExpr() {
    BooleanExpr p1Batfish = newBooleanAtom();
    BooleanExpr p2Batfish = newBooleanAtom();
    BoolExpr p1Z3 = toBoolExpr(p1Batfish, _input, _nodContext);
    BoolExpr p2Z3 = toBoolExpr(p2Batfish, _input, _nodContext);

    assertThat(
        toBoolExpr(new IfExpr(p1Batfish, p2Batfish), _input, _nodContext),
        equalTo(_ctx.mkImplies(p1Z3, p2Z3)));
  }

  @Test
  public void testVisitNotExpr() {
    BooleanExpr p1Batfish = newBooleanAtom();
    BoolExpr p1Z3 = toBoolExpr(p1Batfish, _input, _nodContext);

    assertThat(toBoolExpr(new NotExpr(p1Batfish), _input, _nodContext), equalTo(_ctx.mkNot(p1Z3)));
  }

  @Test
  public void testVisitOrExpr() {
    BooleanExpr p1Batfish = newBooleanAtom();
    BooleanExpr p2Batfish = newBooleanAtom();
    BoolExpr p1Z3 = toBoolExpr(p1Batfish, _input, _nodContext);
    BoolExpr p2Z3 = toBoolExpr(p2Batfish, _input, _nodContext);

    assertThat(
        toBoolExpr(new OrExpr(of(p1Batfish, p2Batfish)), _input, _nodContext),
        equalTo(_ctx.mkOr(p1Z3, p2Z3)));
  }

  @Test
  public void testVisitPrefixMatchExpr() {
    BooleanExpr expr = new PrefixMatchExpr(BasicHeaderField.SRC_IP, Prefix.parse("1.2.3.4/5"));

    assertThat(toBoolExpr(expr, _input, _nodContext), instanceOf(BoolExpr.class));
  }

  @Test
  public void testVisitRangeMatchExpr() {
    BooleanExpr expr =
        new RangeMatchExpr(
            BasicHeaderField.DSCP,
            BasicHeaderField.DSCP.getSize(),
            ImmutableSet.of(new SubRange(1, 3), new SubRange(5, 7)));

    assertThat(toBoolExpr(expr, _input, _nodContext), instanceOf(BoolExpr.class));
  }

  @Test
  public void testVisitSaneExpr() {
    assertThat(toBoolExpr(SaneExpr.INSTANCE, _input, _nodContext), instanceOf(BoolExpr.class));
  }

  @Test
  public void testVisitTransformationStateExpr() {
    assertThat(
        toBoolExpr(_transformationStateExpr, _input, _nodContext), instanceOf(BoolExpr.class));
  }

  @Test
  public void testVisitTrueExpr() {
    assertThat(toBoolExpr(TrueExpr.INSTANCE, _input, _nodContext), equalTo(_ctx.mkTrue()));
  }
}
