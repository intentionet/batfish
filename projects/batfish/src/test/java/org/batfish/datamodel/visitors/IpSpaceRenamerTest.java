package org.batfish.datamodel.visitors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpIpSpace;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardIpSpace;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.UniverseIpSpace;
import org.junit.Test;

public class IpSpaceRenamerTest {
  private static final String FOO = "foo";
  private static final IpSpaceReference FOO_REFERENCE = new IpSpaceReference(FOO);

  private static final String BAR = "bar";
  private static final IpSpaceReference BAR_REFERENCE = new IpSpaceReference(BAR);

  private static final IpIpSpace IP_IP_SPACE = Ip.parse("1.1.1.1").toIpSpace();

  private static final IpSpaceRenamer RENAMER = new IpSpaceRenamer(ImmutableMap.of(FOO, BAR)::get);
  private static final IpSpace PREFIX_IP_SPACE = Prefix.parse("1.0.0.0/8").toIpSpace();
  private static final IpWildcardIpSpace IP_WILDCARD_IP_SPACE =
      new IpWildcard("1.2.0.0/16").toIpSpace();
  private static final IpWildcardSetIpSpace IP_WILDCARD_SET_IP_SPACE =
      IpWildcardSetIpSpace.builder()
          .including(new IpWildcard("1.2.3.4"))
          .excluding(new IpWildcard("5.6.7.8"))
          .build();

  @Test
  public void testIdentities() {
    assertThat(RENAMER.apply(UniverseIpSpace.INSTANCE), equalTo(UniverseIpSpace.INSTANCE));
    assertThat(RENAMER.apply(EmptyIpSpace.INSTANCE), equalTo(EmptyIpSpace.INSTANCE));
    assertThat(RENAMER.apply(IP_IP_SPACE), equalTo(IP_IP_SPACE));
    assertThat(RENAMER.apply(PREFIX_IP_SPACE), equalTo(PREFIX_IP_SPACE));
    assertThat(RENAMER.apply(IP_WILDCARD_IP_SPACE), equalTo(IP_WILDCARD_IP_SPACE));
    assertThat(RENAMER.apply(IP_WILDCARD_SET_IP_SPACE), equalTo(IP_WILDCARD_SET_IP_SPACE));
  }

  @Test
  public void testIpSpaceReference() {
    assertThat(RENAMER.apply(FOO_REFERENCE), equalTo(BAR_REFERENCE));
  }

  @Test
  public void testAclIpSpace() {
    AclIpSpace input =
        AclIpSpace.builder()
            .thenPermitting(IP_IP_SPACE)
            .thenRejecting(PREFIX_IP_SPACE)
            .thenPermitting(FOO_REFERENCE)
            .thenRejecting(IP_WILDCARD_IP_SPACE)
            .thenPermitting(IP_WILDCARD_SET_IP_SPACE)
            .build();
    AclIpSpace output =
        AclIpSpace.builder()
            .thenPermitting(IP_IP_SPACE)
            .thenRejecting(PREFIX_IP_SPACE)
            .thenPermitting(BAR_REFERENCE)
            .thenRejecting(IP_WILDCARD_IP_SPACE)
            .thenPermitting(IP_WILDCARD_SET_IP_SPACE)
            .build();
    assertThat(RENAMER.apply(input), equalTo(output));
  }
}
