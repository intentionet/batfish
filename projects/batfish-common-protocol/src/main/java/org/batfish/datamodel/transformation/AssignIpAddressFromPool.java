package org.batfish.datamodel.transformation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.flow.TransformationStep.TransformationType;

/** A {@link TransformationStep} that transforms the destination IP */
@ParametersAreNonnullByDefault
public final class AssignIpAddressFromPool implements TransformationStep, Serializable {
  /** */
  private static final long serialVersionUID = 1L;

  private static final String PROP_TRANSFORMATION_TYPE = "transformationType";
  private static final String PROP_IP_FIELD = "ipField";
  private static final String PROP_IP_RANGES = "ipRanges";

  private final TransformationType _type;
  private final IpField _ipField;
  private final RangeSet<Ip> _ipRanges;

  public AssignIpAddressFromPool(TransformationType type, IpField ipField, RangeSet<Ip> ranges) {
    checkArgument(!ranges.isEmpty(), "Pool ranges cannot be empty");
    _type = type;
    _ipField = ipField;
    _ipRanges = ImmutableRangeSet.copyOf(ranges);
  }

  public AssignIpAddressFromPool(
      TransformationType type, IpField ipField, Ip poolStart, Ip poolEnd) {
    this(type, ipField, ImmutableRangeSet.of(Range.closed(poolStart, poolEnd)));
  }

  @JsonCreator
  private static AssignIpAddressFromPool jsonCreator(
      @JsonProperty(PROP_TRANSFORMATION_TYPE) TransformationType type,
      @JsonProperty(PROP_IP_FIELD) IpField ipField,
      @JsonProperty(PROP_IP_RANGES) RangeSet<Ip> ipRanges) {
    checkNotNull(type, PROP_TRANSFORMATION_TYPE + " cannot be null");
    checkNotNull(ipField, PROP_IP_FIELD + " cannot be null");
    checkNotNull(ipRanges, PROP_IP_RANGES + " cannot be null");
    return new AssignIpAddressFromPool(type, ipField, ImmutableRangeSet.copyOf(ipRanges));
  }

  @Override
  public <T> T accept(TransformationStepVisitor<T> visitor) {
    return visitor.visitAssignIpAddressFromPool(this);
  }

  @JsonProperty(PROP_IP_FIELD)
  public IpField getIpField() {
    return _ipField;
  }

  @JsonProperty(PROP_IP_RANGES)
  public RangeSet<Ip> getIpRanges() {
    return _ipRanges;
  }

  @Override
  @JsonProperty(PROP_TRANSFORMATION_TYPE)
  public TransformationType getType() {
    return _type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AssignIpAddressFromPool)) {
      return false;
    }
    AssignIpAddressFromPool that = (AssignIpAddressFromPool) o;
    return _type == that._type
        && _ipField == that._ipField
        && Objects.equals(_ipRanges, that._ipRanges);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_type, _ipField, _ipRanges);
  }
}
