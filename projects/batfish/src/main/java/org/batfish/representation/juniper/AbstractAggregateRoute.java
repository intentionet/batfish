package org.batfish.representation.juniper;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.bgp.community.Community;

@ParametersAreNonnullByDefault
public abstract class AbstractAggregateRoute implements Serializable {

  static final int DEFAULT_AGGREGATE_ROUTE_COST = 0;

  static final int DEFAULT_AGGREGATE_ROUTE_PREFERENCE = 130;

  private static final long serialVersionUID = 1L;

  private Boolean _active;

  private AsPath _asPath;

  private Set<Community> _communities;

  private Boolean _drop;

  private Integer _metric;

  private final List<String> _policies;

  private Integer _preference;

  private final Prefix _prefix;

  private Integer _tag;

  public AbstractAggregateRoute(Prefix prefix) {
    _prefix = prefix;
    _policies = new LinkedList<>();
    _communities = new HashSet<>();
  }

  public final @Nullable Boolean getActive() {
    return _active;
  }

  public final @Nullable AsPath getAsPath() {
    return _asPath;
  }

  public final @Nonnull Set<Community> getCommunities() {
    return _communities;
  }

  public final @Nullable Boolean getDrop() {
    return _drop;
  }

  public final @Nullable Integer getMetric() {
    return _metric;
  }

  public final @Nonnull List<String> getPolicies() {
    return _policies;
  }

  public final @Nullable Integer getPreference() {
    return _preference;
  }

  public final @Nonnull Prefix getPrefix() {
    return _prefix;
  }

  public final @Nullable Integer getTag() {
    return _tag;
  }

  protected final void inheritUnsetFieldsSuper(AbstractAggregateRoute parent) {
    _active = firstNonNull(_active, parent._active);
    _asPath = _asPath != null ? _asPath : parent._asPath;
    _metric = firstNonNull(_metric, parent._metric);
    _preference = firstNonNull(_preference, parent._preference);
  }

  public final void setActive(@Nullable Boolean active) {
    _active = active;
  }

  public final void setAsPath(@Nullable AsPath asPath) {
    _asPath = asPath;
  }

  public final void setDrop(@Nullable Boolean drop) {
    _drop = drop;
  }

  public final void setMetric(@Nullable Integer metric) {
    _metric = metric;
  }

  public final void setPreference(@Nullable Integer preference) {
    _preference = preference;
  }

  public final void setTag(@Nullable Integer tag) {
    _tag = tag;
  }
}
