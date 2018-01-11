package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableList;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.batfish.common.BatfishException;
import org.batfish.common.util.ComparableStructure;

@JsonSchemaDescription("An access-list used to filter IPV4 routes")
public class RouteFilterList extends ComparableStructure<String> {

  private static final String PROP_LINES = "lines";

  private static final long serialVersionUID = 1L;

  private transient Set<Prefix> _deniedCache;

  private List<RouteFilterLine> _lines;

  private transient Set<Prefix> _permittedCache;

  @JsonCreator
  public RouteFilterList(@JsonProperty(PROP_NAME) String name) {
    super(name);
    _lines = Collections.emptyList();
  }

  public void addLine(RouteFilterLine r) {
    _lines = ImmutableList.<RouteFilterLine>builder().addAll(_lines).add(r).build();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof RouteFilterList)) {
      return false;
    }
    RouteFilterList other = (RouteFilterList) o;
    return other._lines.equals(_lines);
  }

  @JsonProperty(PROP_LINES)
  @JsonPropertyDescription("The lines against which to check an IPV4 route")
  public List<RouteFilterLine> getLines() {
    return _lines;
  }

  private boolean newPermits(Prefix prefix) {
    boolean accept = false;
    for (RouteFilterLine line : _lines) {
      Prefix linePrefix = line.getPrefix();
      int lineBits = linePrefix.getPrefixLength();
      Prefix truncatedLinePrefix = new Prefix(linePrefix.getAddress(), lineBits);
      Prefix relevantPortion = new Prefix(prefix.getAddress(), lineBits);
      if (relevantPortion.equals(truncatedLinePrefix)) {
        int prefixLength = prefix.getPrefixLength();
        SubRange range = line.getLengthRange();
        int min = range.getStart();
        int max = range.getEnd();
        if (prefixLength >= min && prefixLength <= max) {
          accept = line.getAction() == LineAction.ACCEPT;
          break;
        }
      }
    }
    if (accept) {
      _permittedCache.add(prefix);
    } else {
      _deniedCache.add(prefix);
    }
    return accept;
  }

  public boolean permits(Prefix prefix) {
    if (_deniedCache.contains(prefix)) {
      return false;
    } else if (_permittedCache.contains(prefix)) {
      return true;
    }
    return newPermits(prefix);
  }

  /**
   * Returns the set of {@link IpWildcard ips} that match this filter list.
   *
   * @throws BatfishException if any line in this {@link RouteFilterList} does not have an {@link
   *     LineAction#ACCEPT} when matching.
   */
  @JsonIgnore
  public List<IpWildcard> getMatchingIps() {
    return getLines()
        .stream()
        .map(
            rfLine -> {
              if (rfLine.getAction() != LineAction.ACCEPT) {
                throw new BatfishException(
                    "Expected accept action for routerfilterlist from juniper");
              } else {
                return new IpWildcard(rfLine.getPrefix());
              }
            })
        .collect(Collectors.toList());
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    _deniedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
    _permittedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  @JsonProperty(PROP_LINES)
  public void setLines(List<RouteFilterLine> lines) {
    _lines = lines;
  }
}
