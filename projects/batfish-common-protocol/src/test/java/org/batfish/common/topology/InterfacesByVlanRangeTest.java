package org.batfish.common.topology;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import org.batfish.datamodel.IntegerSpace;
import org.junit.Before;
import org.junit.Test;

/** Test of {@link InterfacesByVlanRange} */
public class InterfacesByVlanRangeTest {
  private InterfacesByVlanRange _interfacesByVlanRange;

  @Before
  public void setUp() {
    _interfacesByVlanRange = InterfacesByVlanRange.create();
  }

  @Test
  public void testStartsEmpty() {
    assertThat(_interfacesByVlanRange.asMap(), anEmptyMap());
  }

  @Test
  public void testAddToEmpty() {
    Range<Integer> range = Range.closed(1, 2);
    ImmutableSet<String> interfaces = ImmutableSet.of("iface1", "iface2");
    _interfacesByVlanRange.add(range, interfaces);
    assertThat(_interfacesByVlanRange.asMap(), hasEntry(range, interfaces));
  }

  @Test
  public void testAddWithIntersection() {
    Range<Integer> range1 = Range.closed(1, 10);
    Range<Integer> range2 = Range.closed(5, 20);
    ImmutableSet<String> set1 = ImmutableSet.of("iface1");
    ImmutableSet<String> set2 = ImmutableSet.of("iface2");
    _interfacesByVlanRange.add(range1, set1);
    _interfacesByVlanRange.add(range2, set2);
    assertThat(
        _interfacesByVlanRange.asMap(),
        allOf(
            hasEntry(Range.closedOpen(1, 5), set1),
            hasEntry(Range.closed(5, 10), ImmutableSet.of("iface1", "iface2")),
            hasEntry(Range.openClosed(10, 20), set2)));
  }

  @Test
  public void testAddDoubleIntersection() {
    Range<Integer> range1 = Range.closed(1, 10);
    Range<Integer> range2 = Range.closed(20, 30);
    Range<Integer> range3 = Range.closed(5, 25);
    ImmutableSet<String> set1 = ImmutableSet.of("iface1");
    ImmutableSet<String> set2 = ImmutableSet.of("iface2");
    ImmutableSet<String> set3 = ImmutableSet.of("iface3");
    _interfacesByVlanRange.add(range1, set1);
    _interfacesByVlanRange.add(range2, set2);
    _interfacesByVlanRange.add(range3, set3);
    assertThat(
        _interfacesByVlanRange.asMap(),
        allOf(
            hasEntry(Range.closedOpen(1, 5), set1),
            hasEntry(Range.closed(5, 10), ImmutableSet.of("iface1", "iface3")),
            hasEntry(Range.open(10, 20), set3),
            hasEntry(Range.closed(20, 25), ImmutableSet.of("iface2", "iface3")),
            hasEntry(Range.openClosed(25, 30), set2)));
  }

  @Test
  public void testAddSubset() {
    Range<Integer> range1 = Range.closed(1, 20);
    Range<Integer> range2 = Range.closed(5, 10);
    ImmutableSet<String> set1 = ImmutableSet.of("iface1");
    ImmutableSet<String> set2 = ImmutableSet.of("iface2");
    _interfacesByVlanRange.add(range1, set1);
    _interfacesByVlanRange.add(range2, set2);
    assertThat(
        _interfacesByVlanRange.asMap(),
        allOf(
            hasEntry(Range.closedOpen(1, 5), set1),
            hasEntry(Range.closed(5, 10), ImmutableSet.of("iface1", "iface2")),
            hasEntry(Range.openClosed(10, 20), set1)));
  }

  @Test
  public void testGet() {
    Range<Integer> range = Range.closed(1, 10);
    ImmutableSet<String> interfaces = ImmutableSet.of("iface1", "iface2");
    _interfacesByVlanRange.add(range, interfaces);
    for (int i = 1; i <= 10; i++) {
      assertThat(_interfacesByVlanRange.get(i), equalTo(interfaces));
    }
    assertThat(_interfacesByVlanRange.get(11), empty());
  }

  @Test
  public void testIntersect() {
    String localIface = "i1";
    _interfacesByVlanRange.add(Range.closed(10, 20), localIface);
    _interfacesByVlanRange.add(Range.closed(40, 50), localIface);
    {
      // simple intersection
      Range<Integer> remoteIfaceRange = Range.closedOpen(12, 15);
      assertThat(
          _interfacesByVlanRange.intersect(localIface, IntegerSpace.of(remoteIfaceRange)),
          contains(remoteIfaceRange));
    }
    {
      // no intersection
      assertThat(
          _interfacesByVlanRange.intersect(localIface, IntegerSpace.of(Range.closed(21, 39))),
          empty());
    }
    {
      // local ranges are subset of remote range
      assertThat(
          _interfacesByVlanRange.intersect(localIface, IntegerSpace.of(Range.closed(1, 100))),
          containsInAnyOrder(Range.closed(10, 20), Range.closed(40, 50)));
    }
    {
      // complex intersection
      assertThat(
          _interfacesByVlanRange.intersect(
              localIface,
              IntegerSpace.unionOf(
                  Range.closed(15, 25), Range.closed(39, 42), Range.singleton(49))),
          containsInAnyOrder(
              Range.closed(15, 20), Range.closedOpen(40, 43), Range.closedOpen(49, 50)));
    }
  }
}
