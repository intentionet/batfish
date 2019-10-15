package org.batfish.representation.palo_alto;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nonnull;

/** PAN NAT rule configuration */
public final class NatRule implements Serializable {

  // Name of the rule
  @Nonnull private final String _name;

  // Zones (both required to commit)
  @Nonnull private final SortedSet<String> _from;
  @Nonnull private final SortedSet<String> _to;

  // IPs (both required to commit)
  @Nonnull private final List<RuleEndpoint> _source;
  @Nonnull private final List<RuleEndpoint> _destination;

  public NatRule(String name) {
    _name = name;
    _from = new TreeSet<>();
    _to = new TreeSet<>();
    _source = new LinkedList<>();
    _destination = new LinkedList<>();
  }

  @Nonnull
  public List<RuleEndpoint> getDestination() {
    return _destination;
  }

  @Nonnull
  public SortedSet<String> getFrom() {
    return _from;
  }

  @Nonnull
  public String getName() {
    return _name;
  }

  @Nonnull
  public List<RuleEndpoint> getSource() {
    return _source;
  }

  @Nonnull
  public SortedSet<String> getTo() {
    return _to;
  }
}
