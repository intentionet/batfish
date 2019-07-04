package org.batfish.representation.juniper;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LongSpace;

public final class AddressRangeAddressBookEntry extends AddressBookEntry {

  private final Ip _lowerLimit;
  private final Ip _upperLimit;

  public AddressRangeAddressBookEntry(String name, Ip lowerLimit, Ip upperLimit) {
    super(name);
    _lowerLimit = lowerLimit;
    _upperLimit = upperLimit;
  }

  @Override
  public SortedMap<String, AddressSetEntry> getEntries() {
    return ImmutableSortedMap.of();
  }

  @Override
  public SortedSet<IpWildcard> getIpWildcards(Warnings w) {
    return LongSpace.of(Range.closed(_lowerLimit.asLong(), _upperLimit.asLong()))
        .longStream()
        .mapToObj(l -> IpWildcard.create(Ip.create(l)))
        .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural()));
  }
}
