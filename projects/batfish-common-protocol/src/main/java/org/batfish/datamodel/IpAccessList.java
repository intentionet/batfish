package org.batfish.datamodel;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.NetworkFactory.NetworkFactoryBuilder;
import org.batfish.datamodel.acl.Evaluator;

/** An access-list used to filter IPV4 packets */
public class IpAccessList implements Serializable {

  public static class Builder extends NetworkFactoryBuilder<IpAccessList> {

    private List<IpAccessListLine> _lines;

    private String _name;

    private Configuration _owner;

    private String _sourceName;

    private String _sourceType;

    Builder(NetworkFactory networkFactory) {
      super(networkFactory, IpAccessList.class);
      _lines = ImmutableList.of();
    }

    @Override
    public IpAccessList build() {
      String name = _name != null ? _name : generateName();
      IpAccessList ipAccessList = new IpAccessList(name, _lines, _sourceName, _sourceType);
      if (_owner != null) {
        _owner.getIpAccessLists().put(name, ipAccessList);
      }
      return ipAccessList;
    }

    public Builder setLines(List<IpAccessListLine> lines) {
      _lines = lines;
      return this;
    }

    public Builder setName(String name) {
      _name = name;
      return this;
    }

    public Builder setOwner(Configuration owner) {
      _owner = owner;
      return this;
    }

    public Builder setSourceName(@Nullable String sourceName) {
      _sourceName = sourceName;
      return this;
    }

    public Builder setSourceType(@Nullable String sourceType) {
      _sourceType = sourceType;
      return this;
    }
  }

  private static final String PROP_LINES = "lines";
  private static final String PROP_NAME = "name";
  private static final String PROP_SOURCE_NAME = "sourceName";
  private static final String PROP_SOURCE_TYPE = "sourceType";

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

  public static Builder builder() {
    return new Builder(null);
  }

  private List<IpAccessListLine> _lines;

  @Nonnull private final String _name;

  private String _sourceName;

  private String _sourceType;

  @JsonCreator
  private IpAccessList(@Nullable @JsonProperty(PROP_NAME) String name) {
    checkArgument(name != null, "IpAccessList missing %s", PROP_NAME);
    _name = name;
  }

  public IpAccessList(
      @Nonnull String name,
      List<IpAccessListLine> lines,
      @Nullable String sourceName,
      @Nullable String sourceType) {
    _name = name;
    _lines = ImmutableList.copyOf(lines);
    _sourceName = sourceName;
    _sourceType = sourceType;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof IpAccessList)) {
      return false;
    }
    IpAccessList other = (IpAccessList) o;
    return _name.equals(other._name) && other._lines.equals(_lines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, _lines);
  }

  public FilterResult filter(
      Flow flow,
      String srcInterface,
      Map<String, IpAccessList> availableAcls,
      Map<String, IpSpace> namedIpSpaces) {
    return filter(flow, srcInterface, availableAcls, namedIpSpaces, LineAction.DENY);
  }

  public FilterResult filter(
      Flow flow,
      String srcInterface,
      Map<String, IpAccessList> availableAcls,
      Map<String, IpSpace> namedIpSpaces,
      LineAction defaultAction) {
    Evaluator evaluator = new Evaluator(flow, srcInterface, availableAcls, namedIpSpaces);
    for (int i = 0; i < _lines.size(); i++) {
      IpAccessListLine line = _lines.get(i);
      if (line.getMatchCondition().accept(evaluator)) {
        return new FilterResult(i, line.getAction());
      }
    }
    return new FilterResult(null, defaultAction);
  }

  @JsonProperty(PROP_NAME)
  @Nonnull
  public String getName() {
    return _name;
  }

  @JsonProperty(PROP_LINES)
  @JsonPropertyDescription("The lines against which to check an IPV4 packet")
  public List<IpAccessListLine> getLines() {
    return _lines;
  }

  @JsonProperty(PROP_SOURCE_NAME)
  public @Nullable String getSourceName() {
    return _sourceName;
  }

  @JsonProperty(PROP_SOURCE_TYPE)
  public @Nullable String getSourceType() {
    return _sourceType;
  }

  @JsonIgnore
  public boolean isComposite() {
    return getName().startsWith("~");
  }

  private boolean noDenyOrLastDeny(IpAccessList acl) {
    int count = 0;
    for (IpAccessListLine line : acl.getLines()) {
      if (line.getAction() == LineAction.DENY && count < acl.getLines().size() - 1) {
        return false;
      }
      count++;
    }
    return true;
  }

  @JsonProperty(PROP_LINES)
  public void setLines(List<IpAccessListLine> lines) {
    _lines = ImmutableList.copyOf(lines);
  }

  @JsonProperty(PROP_SOURCE_NAME)
  private void setSourceName(String sourceName) {
    _sourceName = sourceName;
  }

  @JsonProperty(PROP_SOURCE_TYPE)
  private void setSourceType(String sourceType) {
    _sourceType = sourceType;
  }

  @Override
  public String toString() {
    StringBuilder output =
        new StringBuilder().append(getClass().getSimpleName()).append(":").append(_name);
    for (IpAccessListLine line : _lines) {
      output.append("\n");
      output.append(line);
    }
    return output.toString();
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
