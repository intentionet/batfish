package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Represents an {@link AbstractRoute} annotated with additional information. */
@ParametersAreNonnullByDefault
public final class AnnotatedRoute<R extends AbstractRoute>
    implements AbstractRouteDecorator, Serializable {

  private static final long serialVersionUID = 1L;

  private final R _route;
  private final String _sourceVrf;

  public AnnotatedRoute(R route, String sourceVrf) {
    _route = route;
    _sourceVrf = sourceVrf;
  }

  @Override
  public final int compareTo(AbstractRouteDecorator o) {
    int routeComparison = _route.compareTo(o.getAbstractRoute());
    if (routeComparison != 0) {
      return routeComparison;
    }
    if (!(o instanceof AnnotatedRoute)) {
      // Routes are equal; AbstractRoutes arbitrarily come before AnnotatedRoutes
      return 1;
    }
    AnnotatedRoute<?> other = (AnnotatedRoute<?>) o;
    return _sourceVrf.compareTo(other._sourceVrf);
  }

  @Override
  public AbstractRoute getAbstractRoute() {
    return _route;
  }

  @Override
  @Nonnull
  public Prefix getNetwork() {
    return _route.getNetwork();
  }

  @Nonnull
  public R getRoute() {
    return _route;
  }

  @Nonnull
  public String getSourceVrf() {
    return _sourceVrf;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof AnnotatedRoute)) {
      return false;
    }
    AnnotatedRoute<?> o = (AnnotatedRoute<?>) obj;
    return Objects.equals(_route, o._route) && Objects.equals(_sourceVrf, o._sourceVrf);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_route, _sourceVrf);
  }

  @Override
  public String toString() {
    return toStringHelper(getClass()).add("sourceVrf", _sourceVrf).add("route", _route).toString();
  }
}
