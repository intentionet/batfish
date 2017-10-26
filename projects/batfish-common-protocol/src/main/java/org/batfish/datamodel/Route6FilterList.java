package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.batfish.common.util.ComparableStructure;

@JsonSchemaDescription("An access-list used to filter IPV6 routes")
public class Route6FilterList extends ComparableStructure<String> {

  private static final String PROP_LINES = "lines";

  private static final long serialVersionUID = 1L;

  private transient Set<Prefix6> _deniedCache;

  private List<Route6FilterLine> _lines;

  private transient Set<Prefix6> _permittedCache;

  @JsonCreator
  public Route6FilterList(@JsonProperty(PROP_NAME) String name) {
    super(name);
    _lines = new ArrayList<>();
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

  @Override
  public int hashCode() {
    return Objects.hash(_key, _lines);
  }

  private boolean newPermits(Prefix6 prefix) {
    boolean accept = false;
    for (Route6FilterLine line : _lines) {
      Prefix6 linePrefix = line.getPrefix();
      int lineBits = linePrefix.getPrefixLength();
      Prefix6 truncatedLinePrefix = new Prefix6(linePrefix.getAddress(), lineBits);
      Prefix6 relevantPortion = new Prefix6(prefix.getAddress(), lineBits).getNetworkPrefix();
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

  public boolean permits(Prefix6 prefix) {
    if (_deniedCache.contains(prefix)) {
      return false;
    } else if (_permittedCache.contains(prefix)) {
      return true;
    }
    return newPermits(prefix);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    _deniedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
    _permittedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  @JsonProperty(PROP_LINES)
  public void setLines(List<Route6FilterLine> lines) {
    _lines = lines;
  }
}
