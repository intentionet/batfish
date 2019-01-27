package org.batfish.dataplane.ibdp;

import com.google.common.base.MoreObjects;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Represents an edge across two Virtual routers. Routes can be leaked across the edge, going from
 * {@link #_fromVrf} to another VRF's {@link #_toRib}.
 */
@ParametersAreNonnullByDefault
public final class CrossVrfEdgeId implements Comparable<CrossVrfEdgeId> {

  private final String _fromVrf;
  private final String _toRib;

  CrossVrfEdgeId(String fromVrf, String toRib) {
    _fromVrf = fromVrf;
    _toRib = toRib;
  }

  String getFromVrf() {
    return _fromVrf;
  }

  String getToRib() {
    return _toRib;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CrossVrfEdgeId)) {
      return false;
    }
    CrossVrfEdgeId that = (CrossVrfEdgeId) o;
    return Objects.equals(_fromVrf, that._fromVrf) && Objects.equals(_toRib, that._toRib);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_fromVrf, _toRib);
  }

  @Override
  public int compareTo(CrossVrfEdgeId o) {
    return Comparator.comparing(CrossVrfEdgeId::getFromVrf)
        .thenComparing(CrossVrfEdgeId::getToRib)
        .compare(this, o);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fromVrf", _fromVrf)
        .add("toRib", _toRib)
        .toString();
  }
}
