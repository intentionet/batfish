package org.batfish.z3;

import static org.batfish.datamodel.acl.AclLineMatchExprs.FALSE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.match;
import static org.batfish.datamodel.acl.AclLineMatchExprs.not;
import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.Protocol;
import org.batfish.datamodel.State;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.junit.Test;

public final class IpAccessListSpecializerTest {
  private static final IpAccessListSpecializer CONSTANT_SPECIALIZER =
      new IpAccessListSpecializer() {
        @Override
        boolean canSpecialize() {
          return true;
        }

        @Override
        HeaderSpace specialize(HeaderSpace headerSpace) {
          return HeaderSpace.builder().build();
        }
      };

  private static final IpAccessListSpecializer IDENTITY_SPECIALIZER =
      new IpAccessListSpecializer() {
        @Override
        protected boolean canSpecialize() {
          return true;
        }

        @Override
        protected HeaderSpace specialize(HeaderSpace headerSpace) {
          return headerSpace;
        }
      };

  private static AclLineMatchExpr constantSpecialize(AclLineMatchExpr expr) {
    return CONSTANT_SPECIALIZER.visit(expr);
  }

  private static AclLineMatchExpr identitySpecialize(AclLineMatchExpr expr) {
    return IDENTITY_SPECIALIZER.visit(expr);
  }

  @Test
  public void visitAndMatchExpr() {
    assertThat(identitySpecialize(and()), equalTo(TRUE));
    assertThat(identitySpecialize(and(TRUE, TRUE, TRUE)), equalTo(TRUE));
    assertThat(identitySpecialize(and(TRUE, TRUE, FALSE)), equalTo(FALSE));
  }

  @Test
  public void visitFalseExpr() {
    assertThat(identitySpecialize(FALSE), equalTo(FALSE));
  }

  @Test
  public void visitMatchHeaderSpace_EmptyIpSpace() {
    HeaderSpace dstEmpty = HeaderSpace.builder().setDstIps(EmptyIpSpace.INSTANCE).build();
    HeaderSpace notDstUniverse =
        HeaderSpace.builder().setNotDstIps(UniverseIpSpace.INSTANCE).build();
    HeaderSpace srcEmpty = HeaderSpace.builder().setSrcIps(EmptyIpSpace.INSTANCE).build();
    HeaderSpace notSrcUniverse =
        HeaderSpace.builder().setNotSrcIps(UniverseIpSpace.INSTANCE).build();
    HeaderSpace srcOrDstEmpty = HeaderSpace.builder().setSrcOrDstIps(EmptyIpSpace.INSTANCE).build();

    assertThat(identitySpecialize(match(dstEmpty)), equalTo(FALSE));
    assertThat(identitySpecialize(match(notDstUniverse)), equalTo(FALSE));
    assertThat(identitySpecialize(match(srcEmpty)), equalTo(FALSE));
    assertThat(identitySpecialize(match(notSrcUniverse)), equalTo(FALSE));
    assertThat(identitySpecialize(match(srcOrDstEmpty)), equalTo(FALSE));
  }

  @Test
  public void visitMatchHeaderSpace_EmptyDisjunction() {
    List<Integer> integerList = ImmutableList.of(0);
    List<IpProtocol> ipProtocolList = ImmutableList.of(IpProtocol.ICMP);
    List<Protocol> protocolList = ImmutableList.of(Protocol.TCP);
    List<State> stateList = ImmutableList.of(State.NEW);
    List<SubRange> subRangeList = ImmutableList.of(new SubRange(0, 0));
    List<TcpFlags> tcpFlagsList = ImmutableList.of(TcpFlags.ACK_TCP_FLAG);
    /* If the initial headerspace has non-empty disjunctions that become empty after specialization,
     * the entire match expression is FALSE.
     */
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setDscps(integerList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setEcns(integerList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setDstPorts(subRangeList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setFragmentOffsets(subRangeList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setIcmpCodes(subRangeList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setIcmpTypes(subRangeList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setIpProtocols(ipProtocolList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setSrcPorts(subRangeList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setSrcOrDstPorts(subRangeList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setSrcOrDstProtocols(protocolList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setStates(stateList).build())),
        equalTo(FALSE));
    assertThat(
        constantSpecialize(match(HeaderSpace.builder().setTcpFlags(tcpFlagsList).build())),
        equalTo(FALSE));
    // if we started with empty disjunctions, they are interpreted as "unconstrained", i.e. TRUE
    assertThat(constantSpecialize(match(HeaderSpace.builder().build())), equalTo(TRUE));
  }

  @Test
  public void visitMatchHeaderSpace() {
    HeaderSpace headerSpace =
        HeaderSpace.builder().setDstIps(new Ip("1.1.1.1").toIpSpace()).build();
    assertThat(identitySpecialize(match(headerSpace)), equalTo(match(headerSpace)));
  }

  @Test
  public void visitMatchSrcInterface() {
    AclLineMatchExpr expr = new MatchSrcInterface(ImmutableList.of("foo"));
    assertThat(identitySpecialize(expr), equalTo(expr));
  }

  @Test
  public void visitNotMatchExpr() {
    assertThat(identitySpecialize(not(TRUE)), equalTo(FALSE));
    assertThat(identitySpecialize(not(FALSE)), equalTo(TRUE));
    assertThat(identitySpecialize(not(not(TRUE))), equalTo(TRUE));
    assertThat(
        identitySpecialize(not(ORIGINATING_FROM_DEVICE)), equalTo(not(ORIGINATING_FROM_DEVICE)));
  }

  @Test
  public void visitOriginatingFromDevice() {
    assertThat(identitySpecialize(ORIGINATING_FROM_DEVICE), equalTo(ORIGINATING_FROM_DEVICE));
  }

  @Test
  public void visitOrMatchExpr() {
    assertThat(identitySpecialize(or()), equalTo(FALSE));
    assertThat(identitySpecialize(or(FALSE, FALSE, FALSE)), equalTo(FALSE));
    assertThat(identitySpecialize(or(FALSE, TRUE, FALSE)), equalTo(TRUE));
  }

  @Test
  public void visitPermittedByAcl() {
    PermittedByAcl permittedByAcl = new PermittedByAcl("foo");
    assertThat(identitySpecialize(permittedByAcl), equalTo(permittedByAcl));
  }

  @Test
  public void visitTrueExpr() {
    assertThat(identitySpecialize(TRUE), equalTo(TRUE));
  }
}
