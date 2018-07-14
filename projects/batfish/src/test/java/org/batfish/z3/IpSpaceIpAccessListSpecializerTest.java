package org.batfish.z3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.TrueExpr;
import org.junit.Test;

public class IpSpaceIpAccessListSpecializerTest {
  private static final IpAccessListSpecializer TRIVIAL_SPECIALIZER =
      new IpSpaceIpAccessListSpecializer(new HeaderSpace(), ImmutableMap.of());

  private static final IpAccessListSpecializer BLACKLIST_ANY_DST_SPECIALIZER =
      new IpSpaceIpAccessListSpecializer(
          HeaderSpace.builder().setNotDstIps(ImmutableSet.of(IpWildcard.ANY)).build(),
          ImmutableMap.of());

  private static final IpAccessListSpecializer BLACKLIST_ANY_SRC_SPECIALIZER =
      new IpSpaceIpAccessListSpecializer(
          HeaderSpace.builder().setNotSrcIps(ImmutableSet.of(IpWildcard.ANY)).build(),
          ImmutableMap.of());

  private static final IpAccessListSpecializer WHITELIST_ANY_DST_SPECIALIZER =
      new IpSpaceIpAccessListSpecializer(
          HeaderSpace.builder().setDstIps(ImmutableSet.of(IpWildcard.ANY)).build(),
          ImmutableMap.of());

  private static final IpAccessListSpecializer WHITELIST_ANY_SRC_SPECIALIZER =
      new IpSpaceIpAccessListSpecializer(
          HeaderSpace.builder().setSrcIps(ImmutableSet.of(IpWildcard.ANY)).build(),
          ImmutableMap.of());

  @Test
  public void testSpecializeIpAccessListLine_singleDst() {
    IpAccessListLine ipAccessListLine =
        IpAccessListLine.accepting()
            .setMatchCondition(
                new MatchHeaderSpace(
                    HeaderSpace.builder()
                        .setDstIps(new IpWildcard("1.2.3.0/24").toIpSpace())
                        .build()))
            .build();

    assertThat(
        TRIVIAL_SPECIALIZER.specialize(ipAccessListLine), equalTo(Optional.of(ipAccessListLine)));
    assertThat(
        BLACKLIST_ANY_DST_SPECIALIZER.specialize(ipAccessListLine), equalTo(Optional.empty()));
    assertThat(
        WHITELIST_ANY_DST_SPECIALIZER.specialize(ipAccessListLine),
        equalTo(Optional.of(ipAccessListLine)));
    assertThat(
        BLACKLIST_ANY_SRC_SPECIALIZER.specialize(ipAccessListLine),
        equalTo(Optional.of(ipAccessListLine)));
    assertThat(
        WHITELIST_ANY_SRC_SPECIALIZER.specialize(ipAccessListLine),
        equalTo(Optional.of(ipAccessListLine)));

    // specialize to a headerspace that whitelists part of the dstIp
    IpAccessListSpecializer specializer =
        new IpSpaceIpAccessListSpecializer(
            HeaderSpace.builder().setDstIps(ImmutableSet.of(new IpWildcard("1.2.3.4"))).build(),
            ImmutableMap.of());
    assertThat(
        specializer.specialize(ipAccessListLine),
        equalTo(
            Optional.of(
                IpAccessListLine.accepting().setMatchCondition(TrueExpr.INSTANCE).build())));

    // specialize to a headerspace that blacklists part of the dstIp
    specializer =
        new IpSpaceIpAccessListSpecializer(
            HeaderSpace.builder().setNotDstIps(new IpWildcard("1.2.3.4").toIpSpace()).build(),
            ImmutableMap.of());
    assertThat(specializer.specialize(ipAccessListLine), equalTo(Optional.of(ipAccessListLine)));
  }

  @Test
  public void testSpecializeIpAccessListLine_singleSrc() {
    IpAccessListLine ipAccessListLine =
        IpAccessListLine.acceptingHeaderSpace(
            HeaderSpace.builder().setSrcIps(new IpWildcard("1.2.3.0/24").toIpSpace()).build());

    assertThat(
        TRIVIAL_SPECIALIZER.specialize(ipAccessListLine), equalTo(Optional.of(ipAccessListLine)));
    assertThat(
        BLACKLIST_ANY_DST_SPECIALIZER.specialize(ipAccessListLine),
        equalTo(Optional.of(ipAccessListLine)));
    assertThat(
        WHITELIST_ANY_DST_SPECIALIZER.specialize(ipAccessListLine),
        equalTo(Optional.of(ipAccessListLine)));
    assertThat(
        BLACKLIST_ANY_SRC_SPECIALIZER.specialize(ipAccessListLine), equalTo(Optional.empty()));
    assertThat(
        WHITELIST_ANY_SRC_SPECIALIZER.specialize(ipAccessListLine),
        equalTo(Optional.of(ipAccessListLine)));

    // specialize to a headerspace that whitelists part of the srcIp
    IpAccessListSpecializer specializer =
        new IpSpaceIpAccessListSpecializer(
            HeaderSpace.builder().setSrcIps(ImmutableSet.of(new IpWildcard("1.2.3.4"))).build(),
            ImmutableMap.of());
    assertThat(
        specializer.specialize(ipAccessListLine),
        equalTo(Optional.of(IpAccessListLine.ACCEPT_ALL)));

    // specialize to a headerspace that blacklists part of the srcIp
    specializer =
        new IpSpaceIpAccessListSpecializer(
            HeaderSpace.builder().setNotSrcIps(ImmutableSet.of(new IpWildcard("1.2.3.4"))).build(),
            ImmutableMap.of());
    assertThat(specializer.specialize(ipAccessListLine), equalTo(Optional.of(ipAccessListLine)));
  }
}
