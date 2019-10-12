package org.batfish.datamodel;

import static org.batfish.datamodel.matchers.RouteFilterListMatchers.permits;
import static org.batfish.datamodel.matchers.RouteFilterListMatchers.rejects;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

/** Test for {@link RouteFilterList} */
public class RouteFilterListTest {

  private RouteFilterList _rfAddressMask;
  private RouteFilterList _rfPrefixMoreSpecific;
  private RouteFilterList _rfPrefixExact;

  @Before
  public void setup() {
    _rfAddressMask =
        new RouteFilterList(
            "test-route-filter-mask",
            ImmutableList.of(
                new RouteFilterLine(
                    LineAction.PERMIT,
                    IpWildcard.parse("1.2.3.4:0.255.0.255"),
                    SubRange.singleton(24))));
    _rfPrefixMoreSpecific =
        new RouteFilterList(
            "test-route-filter-prefix",
            ImmutableList.of(
                new RouteFilterLine(
                    LineAction.PERMIT,
                    IpWildcard.parse("1.2.3.4:0.0.255.255"),
                    new SubRange(26, 32))));
    _rfPrefixExact =
        new RouteFilterList(
            "test-route-filter-prefix",
            ImmutableList.of(
                new RouteFilterLine(
                    LineAction.PERMIT,
                    IpWildcard.parse("1.2.3.4:0.0.255.255"),
                    SubRange.singleton(26))));
  }

  @Test
  public void testRfAddressMask() {
    Prefix acceptedPrefix1 = Prefix.parse("1.5.3.127/24");
    Prefix deniedPrefix1 = Prefix.parse("2.5.6.127/24");
    Prefix deniedPrefix2 = Prefix.parse("1.5.3.127/26");

    assertThat(_rfAddressMask, permits(acceptedPrefix1));
    assertThat(_rfAddressMask, rejects(deniedPrefix1));
    assertThat(_rfAddressMask, rejects(deniedPrefix2));
  }

  @Test
  public void testRfPrefixMoreSpecific() {
    Prefix acceptedPrefix1 = Prefix.parse("1.2.16.0/27");
    Prefix deniedPrefix1 = Prefix.parse("1.3.5.6/27");
    // matching prefix but is less specific
    Prefix deniedPrefix2 = Prefix.parse("1.2.16.0/25");

    assertThat(_rfPrefixMoreSpecific, permits(acceptedPrefix1));
    assertThat(_rfPrefixMoreSpecific, rejects(deniedPrefix1));
    assertThat(_rfPrefixMoreSpecific, rejects(deniedPrefix2));
  }

  @Test
  public void testRfPrefixExact() {
    Prefix acceptedPrefix1 = Prefix.parse("1.2.16.0/26");
    // matching prefix with non-equal prefix length
    Prefix deniedPrefix1 = Prefix.parse("1.2.16.0/27");

    assertThat(_rfPrefixExact, permits(acceptedPrefix1));
    assertThat(_rfPrefixExact, rejects(deniedPrefix1));
  }
}
