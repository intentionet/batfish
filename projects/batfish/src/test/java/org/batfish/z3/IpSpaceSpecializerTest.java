package org.batfish.z3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableSortedSet;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.UniverseIpSpace;
import org.junit.Test;

public class IpSpaceSpecializerTest {

  private static final IpSpaceSpecializer trivialSpecializer =
      new IpSpaceSpecializer(ImmutableSortedSet.of(), ImmutableSortedSet.of());

  private static final IpSpaceSpecializer whitelistAnySpecializer =
      new IpSpaceSpecializer(ImmutableSortedSet.of(IpWildcard.ANY), ImmutableSortedSet.of());

  private static final IpSpaceSpecializer blacklistAnySpecializer =
      new IpSpaceSpecializer(ImmutableSortedSet.of(), ImmutableSortedSet.of(IpWildcard.ANY));

  @Test
  public void testSpecializeAclIpSpace() {
    IpSpace ipSpace =
        AclIpSpace.builder()
            .thenPermitting(Prefix.parse("0.0.1.0/24"))
            .thenRejecting(Prefix.parse("0.0.1.4/30"))
            .thenPermitting(Prefix.parse("0.0.1.6/31"))
            .build();

    assertThat(trivialSpecializer.specialize(ipSpace), equalTo(ipSpace));
    assertThat(whitelistAnySpecializer.specialize(ipSpace), equalTo(ipSpace));
    assertThat(
        blacklistAnySpecializer.specialize(ipSpace),
        equalTo(
            AclIpSpace.builder()
                .thenPermitting(EmptyIpSpace.INSTANCE)
                .thenRejecting(EmptyIpSpace.INSTANCE)
                .thenPermitting(EmptyIpSpace.INSTANCE)
                .build()));

    // headerspace is contained in all lines
    IpSpaceSpecializer specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(new IpWildcard("0.0.1.6/32")), ImmutableSortedSet.of());
    assertThat(
        specializer.specialize(ipSpace),
        equalTo(
            AclIpSpace.builder()
                .thenPermitting(UniverseIpSpace.INSTANCE)
                .thenRejecting(UniverseIpSpace.INSTANCE)
                .thenPermitting(UniverseIpSpace.INSTANCE)
                .build()));

    // headerspace is outside of all lines
    specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(new IpWildcard("1.1.1.1/32")), ImmutableSortedSet.of());
    assertThat(
        specializer.specialize(ipSpace),
        equalTo(
            AclIpSpace.builder()
                .thenPermitting(EmptyIpSpace.INSTANCE)
                .thenRejecting(EmptyIpSpace.INSTANCE)
                .thenPermitting(EmptyIpSpace.INSTANCE)
                .build()));

    // not contained in any line, and intersects the first only
    specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(new IpWildcard(new Ip(0x00000100L), new Ip(0xFF0FFF00L))),
            ImmutableSortedSet.of());
    assertThat(
        specializer.specialize(ipSpace),
        equalTo(
            AclIpSpace.builder()
                .thenPermitting(Prefix.parse("0.0.1.0/24"))
                .thenRejecting(EmptyIpSpace.INSTANCE)
                .thenPermitting(EmptyIpSpace.INSTANCE)
                .build()));
  }

  @Test
  public void testSpecializeIp() {
    Ip ip = new Ip("1.1.1.1");
    assertThat(trivialSpecializer.specialize(ip), equalTo(ip));
    assertThat(whitelistAnySpecializer.specialize(ip), equalTo(ip));
    assertThat(blacklistAnySpecializer.specialize(ip), equalTo(EmptyIpSpace.INSTANCE));
    IpSpaceSpecializer specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(), ImmutableSortedSet.of(new IpWildcard("1.1.1.0/24")));
    assertThat(specializer.specialize(ip), equalTo(EmptyIpSpace.INSTANCE));

    // blacklist takes priority
    specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(new IpWildcard("1.1.1.0/24")),
            ImmutableSortedSet.of(new IpWildcard("1.1.1.0/30")));
    assertThat(specializer.specialize(ip), equalTo(EmptyIpSpace.INSTANCE));
  }

  @Test
  public void testSpecializeIpWildcard() {
    IpWildcard ip = new IpWildcard(new Ip(0x01010001L), new Ip(0x0000FF00L));
    assertThat(trivialSpecializer.specialize(ip), equalTo(ip));
    assertThat(whitelistAnySpecializer.specialize(ip), equalTo(ip));
    assertThat(blacklistAnySpecializer.specialize(ip), equalTo(EmptyIpSpace.INSTANCE));
  }

  @Test
  public void testSpecializeIpWildcardSetIpSpace() {
    IpWildcardSetIpSpace ipSpace =
        IpWildcardSetIpSpace.builder()
            .including(new IpWildcard("1.1.1.0/24"))
            .including(new IpWildcard("1.2.0.0/24"))
            .excluding(new IpWildcard("1.1.1.4/30"))
            .build();
    assertThat(trivialSpecializer.specialize(ipSpace), equalTo(ipSpace));
    assertThat(whitelistAnySpecializer.specialize(ipSpace), equalTo(ipSpace));
    assertThat(blacklistAnySpecializer.specialize(ipSpace), equalTo(EmptyIpSpace.INSTANCE));

    IpSpaceSpecializer specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(new IpWildcard("1.1.1.0/24")), ImmutableSortedSet.of());
    assertThat(
        specializer.specialize(ipSpace),
        equalTo(
            IpWildcardSetIpSpace.builder()
                .including(IpWildcard.ANY)
                .excluding(new IpWildcard("1.1.1.4/30"))
                .build()));

    /*
     * Entire headerspace is contained in ipSpace, to specialize to UniverseIpSpace
     */
    specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(new IpWildcard("1.2.0.0/30")), ImmutableSortedSet.of());
    assertThat(specializer.specialize(ipSpace), equalTo(UniverseIpSpace.INSTANCE));

    /*
     * ipSpace contains only a portion of the headerspace, but we can remove parts of ipSpace
     * that are irrelevant.
     */
    specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(new IpWildcard("1.2.0.0/16")), ImmutableSortedSet.of());
    assertThat(
        specializer.specialize(ipSpace),
        equalTo(IpWildcardSetIpSpace.builder().including(new IpWildcard("1.2.0.0/24")).build()));
  }

  @Test
  public void testSpecializePrefix() {
    IpSpace ipSpace = Prefix.parse("1.1.1.0/24");
    assertThat(trivialSpecializer.specialize(ipSpace), equalTo(ipSpace));
    assertThat(whitelistAnySpecializer.specialize(ipSpace), equalTo(ipSpace));
    assertThat(blacklistAnySpecializer.specialize(ipSpace), equalTo(EmptyIpSpace.INSTANCE));

    /*
     * All headerspace Ips are matched by the IpSpace, so it specializes to the universe.
     */
    IpSpaceSpecializer specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(new IpWildcard("1.1.1.4/30")), ImmutableSortedSet.of());
    assertThat(specializer.specialize(ipSpace), equalTo(UniverseIpSpace.INSTANCE));

    specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(new IpWildcard("1.0.0.0/8")),
            ImmutableSortedSet.of(new IpWildcard("1.1.0.0/16")));
    assertThat(specializer.specialize(ipSpace), equalTo(EmptyIpSpace.INSTANCE));

    specializer =
        new IpSpaceSpecializer(
            ImmutableSortedSet.of(new IpWildcard("1.0.0.0/8")),
            ImmutableSortedSet.of(new IpWildcard("1.1.1.1/32")));
    assertThat(specializer.specialize(ipSpace), equalTo(ipSpace));
  }
}
