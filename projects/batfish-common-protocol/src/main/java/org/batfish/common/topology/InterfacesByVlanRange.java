package org.batfish.common.topology;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.IntegerSpace;

/**
 * Internal implementation detail of Layer2 topology computation, performed in {@link TopologyUtil}.
 * Keeps track of a mapping between VLAN ranges to a set of interfaces which allow those VLANs.
 *
 * <p>This implementation
 *
 * <ul>
 *   <li>Assumes all interfaces are on the same node (only tracks interface names)
 *   <li>Does not care about switchport modes, only overlapping VLANs
 * </ul>
 */
@ParametersAreNonnullByDefault
class InterfacesByVlanRange {

  /** Create a new mapping of VLAN ranges to interfaces */
  static InterfacesByVlanRange create() {
    return new InterfacesByVlanRange();
  }

  /** See {@link #add(Range, Collection) } */
  public void add(int vlan, String iface) {
    add(Range.singleton(vlan), iface);
  }

  /** See {@link #add(Range, Collection)} */
  public void add(Range<Integer> vlans, String iface) {
    add(vlans, ImmutableSet.of(iface));
  }

  /**
   * Add a new range with the given collection of interfaces. If the given range intersects with
   * existing ranges, new ranges will be created, with the intersection(s) being mapped to the union
   * of existing and provided interfaces.
   */
  public void add(Range<Integer> vlans, Collection<String> interfaces) {
    if (vlans.isEmpty() || interfaces.isEmpty()) {
      return;
    }
    _ranges.merge(
        vlans,
        ImmutableSet.copyOf(interfaces),
        (a, b) -> ImmutableSet.<String>builder().addAll(a).addAll(b).build());
  }

  /** Return the mapping of all ranges to sets of interfaces as an unmodifiable map */
  @Nonnull
  public Map<Range<Integer>, Set<String>> asMap() {
    return _ranges.asMapOfRanges();
  }

  /** Return the set of interfaces for a given VLAN, or an empty set. */
  @Nonnull
  public Set<String> get(int vlan) {
    return firstNonNull(_ranges.get(vlan), ImmutableSet.of());
  }

  /** Return the range matching given VLAN, or {@code null} if no match is present. */
  @Nullable
  public Range<Integer> getRange(int vlan) {
    Entry<Range<Integer>, Set<String>> entry = _ranges.getEntry(vlan);
    return entry == null ? null : entry.getKey();
  }

  /**
   * Intersect the VLAN ranges given an interface on this node with the allowed VLAN space of some
   * other interface on a (potentially different) node.
   *
   * <p>Example: Ranges 10-20 and 30-40 exist, mapped to Eth1.
   *
   * <ul>
   *   <li>Calling intersect with Eth2 as interface name will produce empty set
   *   <li>Calling intersect with Eth1 and VLAN range 50-60 will produce empty set
   *   <li>Calling intersect with Eth1 and VLAN range 15-35 will produce a set containing ranges
   *       10-15 and 30-35
   * </ul>
   *
   * @param interfaceName the name of the interface <b>that is assumed to already be in this map</b>
   * @param otherVlanSpace the VLAN range of some other, foreign interface (whose name does not
   *     matter)
   */
  @Nonnull
  public Set<Range<Integer>> intersect(String interfaceName, IntegerSpace otherVlanSpace) {
    ImmutableSet.Builder<Range<Integer>> builder = ImmutableSet.builder();
    for (Range<Integer> otherRange : otherVlanSpace.getRanges()) {
      _ranges
          .subRangeMap(otherRange)
          .asMapOfRanges()
          .forEach(
              (intersectedRange, interfaceSet) -> {
                if (interfaceSet.contains(interfaceName)) {
                  builder.add(intersectedRange);
                }
              });
    }
    return builder.build();
  }

  // Private implementation

  private final RangeMap<Integer, Set<String>> _ranges;

  private InterfacesByVlanRange() {
    _ranges = TreeRangeMap.create();
  }
}
