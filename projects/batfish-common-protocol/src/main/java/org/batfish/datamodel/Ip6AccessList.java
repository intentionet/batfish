package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import org.batfish.common.util.ComparableStructure;

/** An access-list used to filter IPV6 packets */
public class Ip6AccessList extends ComparableStructure<String> {

  private static final String PROP_LINES = "lines";

  private static final long serialVersionUID = 1L;

  private List<Ip6AccessListLine> _lines;

  @JsonCreator
  public Ip6AccessList(@JsonProperty(PROP_NAME) String name) {
    super(name);
  }

  public Ip6AccessList(String name, List<Ip6AccessListLine> lines) {
    super(name);
    _lines = lines;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Ip6AccessList)) {
      return false;
    }
    Ip6AccessList other = (Ip6AccessList) o;
    return other._lines.equals(_lines);
  }

  public FilterResult filter(Flow6 flow) {
    for (int i = 0; i < _lines.size(); i++) {
      Ip6AccessListLine line = _lines.get(i);
      if (line.matches(flow)) {
        return new FilterResult(i, line.getAction());
      }
    }
    return new FilterResult(null, LineAction.DENY);
  }

  @JsonProperty(PROP_LINES)
  @JsonPropertyDescription("The lines against which to check an IPV6 packet")
  public List<Ip6AccessListLine> getLines() {
    return _lines;
  }

  @JsonProperty(PROP_LINES)
  public void setLines(List<Ip6AccessListLine> lines) {
    _lines = lines;
  }

  @Override
  public String toString() {
    String output = super.toString() + "\n" + "Identifier: " + _key;
    for (Ip6AccessListLine line : _lines) {
      output += "\n" + line;
    }
    return output;
  }
}
