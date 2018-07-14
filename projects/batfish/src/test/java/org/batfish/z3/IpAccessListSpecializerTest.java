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
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.junit.Test;

public final class IpAccessListSpecializerTest {
  private static final IpAccessListSpecializer specializer =
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

  private static AclLineMatchExpr specialize(AclLineMatchExpr expr) {
    return specializer.visit(expr);
  }

  @Test
  public void visitAndMatchExpr() {
    assertThat(specialize(and()), equalTo(TRUE));
    assertThat(specialize(and(TRUE, TRUE, TRUE)), equalTo(TRUE));
    assertThat(specialize(and(TRUE, TRUE, FALSE)), equalTo(FALSE));
  }

  @Test
  public void visitFalseExpr() {
    assertThat(specialize(FALSE), equalTo(FALSE));
  }

  @Test
  public void visitMatchHeaderSpace_False() {
    HeaderSpace dstEmpty = HeaderSpace.builder().setDstIps(EmptyIpSpace.INSTANCE).build();
    HeaderSpace notDstUniverse =
        HeaderSpace.builder().setNotDstIps(UniverseIpSpace.INSTANCE).build();
    HeaderSpace srcEmpty = HeaderSpace.builder().setSrcIps(EmptyIpSpace.INSTANCE).build();
    HeaderSpace notSrcUniverse =
        HeaderSpace.builder().setNotSrcIps(UniverseIpSpace.INSTANCE).build();
    HeaderSpace srcOrDstEmpty = HeaderSpace.builder().setSrcOrDstIps(EmptyIpSpace.INSTANCE).build();

    assertThat(specialize(match(dstEmpty)), equalTo(FALSE));
    assertThat(specialize(match(notDstUniverse)), equalTo(FALSE));
    assertThat(specialize(match(srcEmpty)), equalTo(FALSE));
    assertThat(specialize(match(notSrcUniverse)), equalTo(FALSE));
    assertThat(specialize(match(srcOrDstEmpty)), equalTo(FALSE));
  }

  @Test
  public void visitMatchHeaderSpace() {
    HeaderSpace headerSpace =
        HeaderSpace.builder().setDstIps(new Ip("1.1.1.1").toIpSpace()).build();
    assertThat(specialize(match(headerSpace)), equalTo(match(headerSpace)));
  }

  @Test
  public void visitMatchSrcInterface() {
    AclLineMatchExpr expr = new MatchSrcInterface(ImmutableList.of("foo"));
    assertThat(specialize(expr), equalTo(expr));
  }

  @Test
  public void visitNotMatchExpr() {
    assertThat(specialize(not(TRUE)), equalTo(FALSE));
    assertThat(specialize(not(FALSE)), equalTo(TRUE));
    assertThat(specialize(not(not(TRUE))), equalTo(TRUE));
    assertThat(specialize(not(ORIGINATING_FROM_DEVICE)), equalTo(not(ORIGINATING_FROM_DEVICE)));
  }

  @Test
  public void visitOriginatingFromDevice() {
    assertThat(specialize(ORIGINATING_FROM_DEVICE), equalTo(ORIGINATING_FROM_DEVICE));
  }

  @Test
  public void visitOrMatchExpr() {
    assertThat(specialize(or()), equalTo(FALSE));
    assertThat(specialize(or(FALSE, FALSE, FALSE)), equalTo(FALSE));
    assertThat(specialize(or(FALSE, TRUE, FALSE)), equalTo(TRUE));
  }

  @Test
  public void visitPermittedByAcl() {
    PermittedByAcl permittedByAcl = new PermittedByAcl("foo");
    assertThat(specialize(permittedByAcl), equalTo(permittedByAcl));
  }

  @Test
  public void visitTrueExpr() {
    assertThat(specialize(TRUE), equalTo(TRUE));
  }
}
