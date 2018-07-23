package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableList;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@JsonSchemaDescription(
    "An AsPathAccessList is used to filter e/iBGP routes according to their AS-path attribute.")
public final class AsPathAccessList implements Serializable {

  private static final String PROP_LINES = "lines";

  private static final String PROP_NAME = "name";

  private static final long serialVersionUID = 1L;

  private transient Set<AsPath> _deniedCache;

  @Nonnull private final List<AsPathAccessListLine> _lines;

  private final String _name;

  private transient Set<AsPath> _permittedCache;

  @JsonCreator
  public AsPathAccessList(
      @Nullable @JsonProperty(PROP_NAME) String name,
      @Nullable @JsonProperty(PROP_LINES) List<AsPathAccessListLine> lines) {
    _lines = firstNonNull(lines, ImmutableList.of());
    _name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof AsPathAccessList)) {
      return false;
    }
    AsPathAccessList other = (AsPathAccessList) o;
    return Objects.equals(_name, other._name) && Objects.equals(_lines, other._lines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, _lines);
  }

  @JsonProperty(PROP_LINES)
  @JsonPropertyDescription(
      "The list of lines against which a route's AS-path will be checked in order.")
  @Nonnull
  public List<AsPathAccessListLine> getLines() {
    return _lines;
  }

  @JsonProperty(PROP_NAME)
  @Nonnull
  public String getName() {
    return _name;
  }

  private boolean newPermits(AsPath asPath) {
    boolean accept = false;
    for (AsPathAccessListLine line : _lines) {
      String regex = line.getRegex();
      Pattern p = Pattern.compile(regex);
      String asPathString = asPath.getAsPathString();
      Matcher matcher = p.matcher(asPathString);
      boolean match = matcher.find();
      if (match) {
        accept = line.getAction() == LineAction.ACCEPT;
        break;
      }
    }
    if (accept) {
      _permittedCache.add(asPath);
    } else {
      _deniedCache.add(asPath);
    }
    return accept;
  }

  public boolean permits(AsPath asPath) {
    if (_deniedCache.contains(asPath)) {
      return false;
    } else if (_permittedCache.contains(asPath)) {
      return true;
    }
    return newPermits(asPath);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    _deniedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
    _permittedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }
}
