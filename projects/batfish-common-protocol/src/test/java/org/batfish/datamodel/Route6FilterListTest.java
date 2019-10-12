package org.batfish.datamodel;

import static org.batfish.datamodel.matchers.Route6FilterListMatchers.permits;
import static org.batfish.datamodel.matchers.Route6FilterListMatchers.rejects;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

/** Test for {@link RouteFilterList} */
public class Route6FilterListTest {

  private Route6FilterList _rfAddressMask;
  private Route6FilterList _rfPrefixMoreSpecific;
  private Route6FilterList _rfPrefixExact;

  @Before
  public void setup() {
    _rfAddressMask =
        new Route6FilterList(
            "test-route6-filter-mask",
            ImmutableList.of(
                new Route6FilterLine(
                    LineAction.PERMIT,
                    Ip6Wildcard.parse(
                        "2001:db8:1234:2345:3456:4567:5678:6789;0:ffff:0:0:0:ffff:ffff:ffff"),
                    SubRange.singleton(64))));
    _rfPrefixMoreSpecific =
        new Route6FilterList(
            "test-route6-filter-prefix",
            ImmutableList.of(
                new Route6FilterLine(
                    LineAction.PERMIT,
                    Ip6Wildcard.parse(
                        "2001:db8:1234:2345:3456:4567:5678:6789;0:0:0:0:ffff:ffff:ffff:ffff"),
                    new SubRange(65, 70))));
    _rfPrefixExact =
        new Route6FilterList(
            "test-route6-filter-prefix",
            ImmutableList.of(
                new Route6FilterLine(
                    LineAction.PERMIT,
                    Ip6Wildcard.parse(
                        "2001:db8:1234:2345:3456:4567:5678:6789;0:0:0:0:ffff:ffff:ffff:ffff"),
                    SubRange.singleton(64))));
  }

  @Test
  public void testRfAddressMask() {
    Prefix6 acceptedPrefix1 = Prefix6.parse("2001:db9:1234:2345:3456:8373:8728:1239/64");
    Prefix6 deniedPrefix1 = Prefix6.parse("2002:db8:2346:2347:5353:4567:5678:6789/64");
    Prefix6 deniedPrefix2 = Prefix6.parse("2001:db8:1234:2345:3456:4567:5678:6789/66");

    assertThat(_rfAddressMask, permits(acceptedPrefix1));
    assertThat(_rfAddressMask, rejects(deniedPrefix1));
    assertThat(_rfAddressMask, rejects(deniedPrefix2));
  }

  @Test
  public void testRfPrefixMoreSpecific() {
    Prefix6 acceptedPrefix1 = Prefix6.parse("2001:db8:1234:2345:5353:8373:8728:1239/66");
    Prefix6 deniedPrefix1 = Prefix6.parse("2001:db8:4567:2345:5353:8373:8728:1239/66");
    // matching prefix but is less specific
    Prefix6 deniedPrefix2 = Prefix6.parse("2001:db8:1234:2345:5353:8373:8728:1239/62");

    assertThat(_rfPrefixMoreSpecific, permits(acceptedPrefix1));
    assertThat(_rfPrefixMoreSpecific, rejects(deniedPrefix1));
    assertThat(_rfPrefixMoreSpecific, rejects(deniedPrefix2));
  }

  @Test
  public void testRfPrefixExact() {
    Prefix6 acceptedPrefix1 = Prefix6.parse("2001:db8:1234:2345:5353:8373:8728:1239/64");
    // matching prefix with non-equal prefix length
    Prefix6 deniedPrefix1 = Prefix6.parse("2001:db8:1234:2345:5353:8373:8728:1239/65");

    assertThat(_rfPrefixExact, permits(acceptedPrefix1));
    assertThat(_rfPrefixExact, rejects(deniedPrefix1));
  }
}
