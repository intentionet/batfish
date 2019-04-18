package org.batfish.representation.f5_bigip;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.LineAction;

/** Entry in a route-map supplying match conditions and transformations for a route */
@ParametersAreNonnullByDefault
public final class RouteMapEntry implements Serializable {

  private static final long serialVersionUID = 1L;

  private @Nullable LineAction _action;
  private @Nullable MatchAccessList _matchAccessList;
  private @Nullable RouteMapMatchPrefixList _matchPrefixList;
  private final long _num;
  private @Nullable RouteMapSetCommunity _setCommunity;

  public RouteMapEntry(long num) {
    _num = num;
  }

  public @Nullable LineAction getAction() {
    return _action;
  }

  public @Nullable MatchAccessList getMatchAccessList() {
    return _matchAccessList;
  }

  /** Return stream of match statements for this entry. */
  public @Nonnull Stream<RouteMapMatch> getMatches() {
    return Stream.<RouteMapMatch>of(_matchAccessList, _matchPrefixList).filter(Objects::nonNull);
  }

  public @Nullable RouteMapMatchPrefixList getMatchPrefixList() {
    return _matchPrefixList;
  }

  public long getNum() {
    return _num;
  }

  public @Nullable RouteMapSetCommunity getSetCommunity() {
    return _setCommunity;
  }

  /** Return stream of set statements for this entry. */
  public @Nonnull Stream<RouteMapSet> getSets() {
    return Stream.<RouteMapSet>of(_setCommunity).filter(Objects::nonNull);
  }

  public void setAction(@Nullable LineAction action) {
    _action = action;
  }

  public void setMatchAccessList(@Nullable MatchAccessList matchAccessList) {
    _matchAccessList = matchAccessList;
  }

  public void setMatchPrefixList(@Nullable RouteMapMatchPrefixList matchPrefixList) {
    _matchPrefixList = matchPrefixList;
  }

  public void setSetCommunity(@Nullable RouteMapSetCommunity setCommunity) {
    _setCommunity = setCommunity;
  }
}
