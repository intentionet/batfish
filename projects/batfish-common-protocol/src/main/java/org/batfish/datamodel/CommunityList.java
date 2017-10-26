package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.ComparableStructure;

@JsonSchemaDescription(
    "Represents a named access-list whose matching criteria is restricted to regexes on community "
        + "attributes sent with a bgp advertisement")
public class CommunityList extends ComparableStructure<String> {

  private static final String PROP_LINES = "lines";

  /** */
  private static final long serialVersionUID = 1L;

  private transient Set<Long> _deniedCache;

  private boolean _invertMatch;

  /**
   * The list of lines that are checked in order against the community attribute(s) of a bgp
   * advertisement
   */
  private final List<CommunityListLine> _lines;

  private transient Set<Long> _permittedCache;

  /**
   * Constructs a CommunityList with the given name for {@link #_name}, and lines for {@link
   * #_lines}
   *
   * @param name The name of the structure
   * @param lines The lines in the list
   */
  @JsonCreator
  public CommunityList(
      @JsonProperty(PROP_NAME) String name,
      @JsonProperty(PROP_LINES) List<CommunityListLine> lines) {
    super(name);
    _lines = lines;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof CommunityList)) {
      return false;
    }
    CommunityList other = (CommunityList) o;
    return other._lines.equals(_lines);
  }

  @JsonPropertyDescription(
      "Specifies whether or not lines should match the complement of their criteria (does not "
          + "change whether a line permits or denies).")
  public boolean getInvertMatch() {
    return _invertMatch;
  }

  @JsonProperty(PROP_LINES)
  @JsonPropertyDescription(
      "The list of lines that are checked in order against the community attribute(s) of a bgp "
          + "advertisement")
  public List<CommunityListLine> getLines() {
    return _lines;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_key, _lines);
  }

  private boolean newPermits(long community) {
    boolean accept = false;
    boolean match = false;
    Boolean matchingLineAccepts = null;
    for (CommunityListLine line : _lines) {
      Pattern p = Pattern.compile(line.getRegex());
      String communityStr = CommonUtil.longToCommunity(community);
      Matcher matcher = p.matcher(communityStr);
      if (matcher.find()) {
        match = true;
        matchingLineAccepts = line.getAction() == LineAction.ACCEPT;
        break;
      }
    }
    if (match) {
      if (_invertMatch) {
        accept = false;
      } else {
        accept = matchingLineAccepts;
      }
    }
    if (accept) {
      _permittedCache.add(community);
    } else {
      _deniedCache.add(community);
    }
    return accept;
  }

  public boolean permits(long community) {
    // if (_permittedCache.contains(community)) {
    // return true;
    // }
    // else if (_deniedCache.contains(community)) {
    // return false;
    // }
    return newPermits(community);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    _deniedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
    _permittedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  public void setInvertMatch(boolean invertMatch) {
    _invertMatch = invertMatch;
  }
}
