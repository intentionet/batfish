package org.batfish.datamodel;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.batfish.datamodel.visitors.IpSpaceComparator;

public class AclIpSpaceLine implements Comparable<AclIpSpaceLine> {

  public static class Builder {

    private LineAction _action;

    private IpSpace _ipSpace;

    private Builder() {
      _action = LineAction.ACCEPT;
    }

    public AclIpSpaceLine build() {
      return new AclIpSpaceLine(_ipSpace, _action);
    }

    public Builder setAction(@Nonnull LineAction action) {
      _action = action;
      return this;
    }

    public Builder setIpSpace(@Nonnull IpSpace ipSpace) {
      _ipSpace = ipSpace;
      return this;
    }
  }

  public static final AclIpSpaceLine PERMIT_ALL =
      AclIpSpaceLine.builder().setIpSpace(UniverseIpSpace.INSTANCE).build();

  public static Builder builder() {
    return new Builder();
  }

  public static AclIpSpaceLine permit(IpSpace ipSpace) {
    return builder().setIpSpace(ipSpace).build();
  }

  public static AclIpSpaceLine reject(IpSpace ipSpace) {
    return builder().setIpSpace(ipSpace).setAction(LineAction.REJECT).build();
  }

  private final LineAction _action;

  private final IpSpace _ipSpace;

  private AclIpSpaceLine(@Nonnull IpSpace ipSpace, @Nonnull LineAction action) {
    _ipSpace = ipSpace;
    _action = action;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof AclIpSpaceLine)) {
      return false;
    }
    AclIpSpaceLine rhs = (AclIpSpaceLine) o;
    return Objects.equals(_action, rhs._action) && Objects.equals(_ipSpace, rhs._ipSpace);
  }

  public LineAction getAction() {
    return _action;
  }

  public IpSpace getIpSpace() {
    return _ipSpace;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_action.ordinal(), _ipSpace);
  }

  public Builder rebuild() {
    return builder().setAction(_action).setIpSpace(_ipSpace);
  }

  @Override
  public String toString() {
    ToStringHelper helper = MoreObjects.toStringHelper(getClass());
    if (_action != LineAction.ACCEPT) {
      helper.add("action", _action);
    }
    helper.add("ipSpace", _ipSpace);
    return helper.toString();
  }

  @Override
  public int compareTo(AclIpSpaceLine o) {
    return Comparator.comparing(AclIpSpaceLine::getAction)
        .thenComparing(AclIpSpaceLine::getIpSpace, IpSpaceComparator::compare)
        .compare(this, o);
  }
}
