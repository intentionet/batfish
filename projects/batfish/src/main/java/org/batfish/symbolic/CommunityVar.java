package org.batfish.symbolic;

import com.google.common.base.MoreObjects;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.bgp.community.Community;

/**
 * Representation of a community variable for the symbolic encoding. Configuration languages allow
 * users match community values using either <b>exact matches</b> or <b>regular expression</b>
 * matches. For example, a regular expression match such as .*:65001 will match any community string
 * that ends with 65001.
 *
 * <p>To encode community semantics, the model introduces a single new boolean variable for every
 * exact match, and two new boolean variables for every regex match. The first variable says whether
 * there is a community value that matches the regex, but is not specified in the configuration
 * (e.g., came from a neighbor). The second variable says if the regex match is successful, which is
 * based on both the communities in the configuration as well as other communities possibly sent by
 * neighbors.
 *
 * @author Ryan Beckett
 */
@ParametersAreNonnullByDefault
public final class CommunityVar implements Comparable<CommunityVar> {

  public enum Type {
    EXACT,
    REGEX,
    OTHER
  }

  @Nonnull private final Type _type;
  @Nonnull private final String _regex;
  @Nullable private final Community _literalValue;

  private CommunityVar(Type type, String regex, @Nullable Community literalValue) {
    _type = type;
    _regex = regex;
    _literalValue = literalValue;
  }

  /** Create a community var of type {@link Type#REGEX} */
  public static CommunityVar from(String regex) {
    return new CommunityVar(Type.REGEX, regex, null);
  }

  /**
   * Create a community var of type {@link Type#EXACT} based on a literal {@link Community} value
   */
  public static CommunityVar from(Community literalCommunity) {
    return new CommunityVar(Type.EXACT, literalCommunity.matchString(), literalCommunity);
  }

  /** Create a community var of type {@link Type#OTHER} based on a REGEX community var. */
  public static CommunityVar other(String regex) {
    return new CommunityVar(Type.OTHER, regex, null);
  }

  @Nonnull
  public Type getType() {
    return _type;
  }

  @Nonnull
  public String getRegex() {
    return _regex;
  }

  @Nullable
  public Community getLiteralValue() {
    return _literalValue;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("type", _type)
        .add("regex", _regex)
        .add("literalValue", _literalValue)
        .toString();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommunityVar)) {
      return false;
    }
    CommunityVar that = (CommunityVar) o;
    return _type == that._type
        && _regex.equals(that._regex)
        && Objects.equals(_literalValue, that._literalValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_type, _regex, _literalValue);
  }

  @Override
  public int compareTo(CommunityVar that) {
    return Comparator.comparing(CommunityVar::getType)
        .thenComparing(CommunityVar::getRegex)
        .thenComparing(
            CommunityVar::getLiteralValue, Comparator.nullsLast(Comparator.naturalOrder()))
        .compare(this, that);
  }
}
