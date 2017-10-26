package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.util.List;
import java.util.Objects;
import org.batfish.common.util.ComparableStructure;

@JsonSchemaDescription("An access-list used to filter IPV4 packets")
public class IpAccessList extends ComparableStructure<String> {

  private static final String PROP_LINES = "lines";

  private static final long serialVersionUID = 1L;

  static boolean bothNullOrSameName(IpAccessList a, IpAccessList b) {
    if (a == null && b == null) {
      return true;
    } else if (a != null && b != null) {
      return a.getName().equals(b.getName());
    } else {
      return false;
    }
  }

  static boolean bothNullOrUnorderedEqual(IpAccessList a, IpAccessList b) {
    if (a == null && b == null) {
      return true;
    } else if (a != null && b != null) {
      return a.unorderedEqual(b);
    } else {
      return false;
    }
  }

  private List<IpAccessListLine> _lines;

  @JsonCreator
  public IpAccessList(@JsonProperty(PROP_NAME) String name) {
    super(name);
  }

  public IpAccessList(String name, List<IpAccessListLine> lines) {
    super(name);
    _lines = lines;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof IpAccessList)) {
      return false;
    }
    IpAccessList other = (IpAccessList) o;
    return other._lines.equals(_lines);
  }

  public FilterResult filter(Flow flow) {
    for (int i = 0; i < _lines.size(); i++) {
      IpAccessListLine line = _lines.get(i);
      if (line.matches(flow)) {
        return new FilterResult(i, line.getAction());
      }
    }
    return new FilterResult(null, LineAction.REJECT);
  }

  @JsonProperty(PROP_LINES)
  @JsonPropertyDescription("The lines against which to check an IPV4 packet")
  public List<IpAccessListLine> getLines() {
    return _lines;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_key, _lines);
  }

  private boolean noDenyOrLastDeny(IpAccessList acl) {
    int count = 0;
    for (IpAccessListLine line : acl.getLines()) {
      if (line.getAction() == LineAction.REJECT && count < acl.getLines().size() - 1) {
        return false;
      }
      count++;
    }
    return true;
  }

  @JsonProperty(PROP_LINES)
  public void setLines(List<IpAccessListLine> lines) {
    _lines = lines;
  }

  @Override
  public String toString() {
    String output = super.toString() + "\n" + "Identifier: " + _key;
    for (IpAccessListLine line : _lines) {
      output += "\n" + line;
    }
    return output;
  }

  public boolean unorderedEqual(Object obj) {
    if (this == obj) {
      return true;
    }
    if (this.equals(obj)) {
      return true;
    }
    IpAccessList other = (IpAccessList) obj;
    if (this.getLines().size() != other.getLines().size()) {
      return false;
    }
    // Unordered check is valid only if there is no deny OR if there is only
    // one, at the
    // end, in both lists.
    if (!noDenyOrLastDeny(this) || !noDenyOrLastDeny(other)) {
      return false;
    }
    for (IpAccessListLine line : this.getLines()) {
      if (!other.getLines().contains(line)) {
        return false;
      }
    }
    return true;
  }
}
