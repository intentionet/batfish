package org.batfish.representation.juniper;

import java.util.HashSet;
import java.util.Set;
import org.batfish.common.Warnings;
import org.batfish.datamodel.IpWildcard;

public final class AddressSetAddressBookEntry extends AddressBookEntry {

  /** */
  private static final long serialVersionUID = 1L;

  private final Set<AddressSetEntry> _entries;

  public AddressSetAddressBookEntry(String name) {
    super(name);
    _entries = new HashSet<>();
  }

  @Override
  public Set<AddressSetEntry> getEntries() {
    return _entries;
  }

  @Override
  public Set<IpWildcard> getIpWildcards(Warnings w) {
    Set<IpWildcard> prefixes = new HashSet<>();
    for (AddressSetEntry entry : _entries) {
      Set<IpWildcard> subPrefixes = entry.getIpWildcards(w);
      prefixes.addAll(subPrefixes);
    }
    return prefixes;
  }
}
