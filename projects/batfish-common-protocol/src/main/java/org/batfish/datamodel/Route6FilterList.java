package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.batfish.common.util.ComparableStructure;

/** An access-list used to filter IPV6 routes */
public class Route6FilterList extends ComparableStructure<String> {

  private static final String PROP_LINES = "lines";

  private static final long serialVersionUID = 1L;

  private final Supplier<Set<Prefix6>> _deniedCache;

  private List<Route6FilterLine> _lines;

  private final Supplier<Set<Prefix6>> _permittedCache;

  private static class CacheSupplier implements Supplier<Set<Prefix6>>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public Set<Prefix6> get() {
      return Collections.newSetFromMap(new ConcurrentHashMap<>());
    }
  }

  public Route6FilterList(String name) {
    this(name, new ArrayList<>());
  }

  @JsonCreator
  public Route6FilterList(
      @JsonProperty(PROP_NAME) String name,
      @JsonProperty(PROP_LINES) List<Route6FilterLine> lines) {
    super(name);
    _deniedCache = Suppliers.memoize(new Route6FilterList.CacheSupplier());
    _permittedCache = Suppliers.memoize(new Route6FilterList.CacheSupplier());
    _lines = firstNonNull(lines, Collections.emptyList());
  }

  public void addLine(Route6FilterLine r) {
    _lines.add(r);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof Route6FilterList)) {
      return false;
    }

    Route6FilterList other = (Route6FilterList) o;
    return other._lines.equals(_lines);
  }

  @JsonProperty(PROP_LINES)
  @JsonPropertyDescription("The lines against which to check an IPV6 route")
  public List<Route6FilterLine> getLines() {
    return _lines;
  }

  private boolean newPermits(Prefix6 prefix) {
    boolean accept = false;
    for (Route6FilterLine line : _lines) {
      if (line.getIpWildcard().contains(prefix.getAddress())) {
        int prefixLength = prefix.getPrefixLength();
        SubRange range = line.getLengthRange();
        if (prefixLength >= range.getStart() && prefixLength <= range.getEnd()) {
          accept = line.getAction() == LineAction.PERMIT;
          break;
        }
      }
    }
    if (accept) {
      _permittedCache.get().add(prefix);
    } else {
      _deniedCache.get().add(prefix);
    }
    return accept;
  }

  public boolean permits(Prefix6 prefix) {
    if (_deniedCache.get().contains(prefix)) {
      return false;
    } else if (_permittedCache.get().contains(prefix)) {
      return true;
    }
    return newPermits(prefix);
  }

  @JsonProperty(PROP_LINES)
  public void setLines(List<Route6FilterLine> lines) {
    _lines = lines;
  }
}
