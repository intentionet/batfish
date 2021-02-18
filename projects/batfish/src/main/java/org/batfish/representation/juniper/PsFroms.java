package org.batfish.representation.juniper;

import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Represents all {@link PsFrom} statements in a single {@link PsTerm} */
public final class PsFroms implements Serializable {

  private boolean _atLeastOneFrom = false;

  private final Set<PsFromAsPath> _fromAsPaths;
  private PsFromColor _fromColor;
  private final Set<PsFromCommunity> _fromCommunities;
  private PsFromFamily _fromFamily;
  private PsFromInstance _fromInstance;
  private final Set<PsFromInterface> _fromInterfaces;
  private PsFromLocalPreference _fromLocalPreference;
  private PsFromMetric _fromMetric;
  private final Set<PsFromPolicyStatement> _fromPolicyStatements;
  private final Set<PsFromPolicyStatementConjunction> _fromPolicyStatementConjunctions;
  private final Set<PsFromPrefixList> _fromPrefixLists;
  private final Set<PsFromPrefixListFilterLonger> _fromPrefixListFilterLongers;
  private final Set<PsFromPrefixListFilterOrLonger> _fromPrefixListFilterOrLongers;
  private final Set<PsFromProtocol> _fromProtocols;
  private final Set<PsFromRouteFilter> _fromRouteFilters;
  private final Set<PsFromTag> _fromTags;
  private PsFromUnsupported _fromUnsupported;

  PsFroms() {
    _fromAsPaths = new LinkedHashSet<>();
    _fromCommunities = new LinkedHashSet<>();
    _fromInterfaces = new LinkedHashSet<>();
    _fromPolicyStatements = new LinkedHashSet<>();
    _fromPolicyStatementConjunctions = new LinkedHashSet<>();
    _fromPrefixLists = new LinkedHashSet<>();
    _fromPrefixListFilterLongers = new LinkedHashSet<>();
    _fromPrefixListFilterOrLongers = new LinkedHashSet<>();
    _fromProtocols = new LinkedHashSet<>();
    _fromRouteFilters = new LinkedHashSet<>();
    _fromTags = new LinkedHashSet<>();
  }

  public void addFromAsPath(@Nonnull PsFromAsPath fromAsPath) {
    _atLeastOneFrom = true;
    _fromAsPaths.add(fromAsPath);
  }

  public void addFromCommunity(@Nonnull PsFromCommunity fromCommunity) {
    _atLeastOneFrom = true;
    _fromCommunities.add(fromCommunity);
  }

  public void addFromInterface(@Nonnull PsFromInterface fromInterface) {
    _atLeastOneFrom = true;
    _fromInterfaces.add(fromInterface);
  }

  public void addFromPolicyStatement(@Nonnull PsFromPolicyStatement fromPolicyStatement) {
    _atLeastOneFrom = true;
    _fromPolicyStatements.add(fromPolicyStatement);
  }

  public void addFromPolicyStatementConjunction(
      @Nonnull PsFromPolicyStatementConjunction fromPolicyStatementConjunction) {
    _atLeastOneFrom = true;
    _fromPolicyStatementConjunctions.add(fromPolicyStatementConjunction);
  }

  public void addFromPrefixList(@Nonnull PsFromPrefixList fromPrefixList) {
    _atLeastOneFrom = true;
    _fromPrefixLists.add(fromPrefixList);
  }

  public void addFromPrefixListFilterLonger(
      @Nonnull PsFromPrefixListFilterLonger fromPrefixListFilterLonger) {
    _atLeastOneFrom = true;
    _fromPrefixListFilterLongers.add(fromPrefixListFilterLonger);
  }

  public void addFromPrefixListFilterOrLonger(
      @Nonnull PsFromPrefixListFilterOrLonger fromPrefixListFilterOrLonger) {
    _atLeastOneFrom = true;
    _fromPrefixListFilterOrLongers.add(fromPrefixListFilterOrLonger);
  }

  public void addFromProtocol(@Nonnull PsFromProtocol fromProtocol) {
    _atLeastOneFrom = true;
    _fromProtocols.add(fromProtocol);
  }

  public void addFromRouteFilter(@Nonnull PsFromRouteFilter fromRouteFilter) {
    _atLeastOneFrom = true;
    _fromRouteFilters.add(fromRouteFilter);
  }

  public void addFromTag(@Nonnull PsFromTag fromTag) {
    _atLeastOneFrom = true;
    _fromTags.add(fromTag);
  }

  @Nonnull
  Set<PsFromAsPath> getFromAsPaths() {
    return _fromAsPaths;
  }

  @Nullable
  PsFromColor getFromColor() {
    return _fromColor;
  }

  @Nonnull
  Set<PsFromCommunity> getFromCommunities() {
    return _fromCommunities;
  }

  @Nullable
  PsFromFamily getFromFamily() {
    return _fromFamily;
  }

  PsFromInstance getFromInstance() {
    return _fromInstance;
  }

  @Nonnull
  Set<PsFromInterface> getFromInterfaces() {
    return _fromInterfaces;
  }

  @VisibleForTesting
  public @Nullable PsFromLocalPreference getFromLocalPreference() {
    return _fromLocalPreference;
  }

  @Nullable
  PsFromMetric getFromMetric() {
    return _fromMetric;
  }

  @Nonnull
  Set<PsFromPolicyStatement> getFromPolicyStatements() {
    return _fromPolicyStatements;
  }

  @Nonnull
  Set<PsFromPolicyStatementConjunction> getFromPolicyStatementConjunctions() {
    return _fromPolicyStatementConjunctions;
  }

  @Nonnull
  Set<PsFromPrefixList> getFromPrefixLists() {
    return _fromPrefixLists;
  }

  @Nonnull
  Set<PsFromPrefixListFilterLonger> getFromPrefixListFilterLongers() {
    return _fromPrefixListFilterLongers;
  }

  @Nonnull
  Set<PsFromPrefixListFilterOrLonger> getFromPrefixListFilterOrLongers() {
    return _fromPrefixListFilterOrLongers;
  }

  @Nonnull
  Set<PsFromProtocol> getFromProtocols() {
    return _fromProtocols;
  }

  @Nonnull
  Set<PsFromRouteFilter> getFromRouteFilters() {
    return _fromRouteFilters;
  }

  @Nonnull
  Set<PsFromTag> getFromTags() {
    return _fromTags;
  }

  @Nullable
  PsFromUnsupported getFromUnsupported() {
    return _fromUnsupported;
  }

  boolean hasAtLeastOneFrom() {
    return _atLeastOneFrom;
  }

  public void setFromColor(@Nonnull PsFromColor fromColor) {
    _atLeastOneFrom = true;
    _fromColor = fromColor;
  }

  public void setFromFamily(@Nonnull PsFromFamily fromFamily) {
    _atLeastOneFrom = true;
    _fromFamily = fromFamily;
  }

  public void setFromInstance(@Nonnull PsFromInstance fromInstance) {
    _atLeastOneFrom = true;
    _fromInstance = fromInstance;
  }

  public void setFromLocalPreference(@Nonnull PsFromLocalPreference fromLocalPreference) {
    _atLeastOneFrom = true;
    _fromLocalPreference = fromLocalPreference;
  }

  public void setFromMetric(@Nonnull PsFromMetric fromMetric) {
    _atLeastOneFrom = true;
    _fromMetric = fromMetric;
  }

  public void setFromUnsupported(@Nonnull PsFromUnsupported fromUnsupported) {
    _atLeastOneFrom = true;
    _fromUnsupported = fromUnsupported;
  }
}
