package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_ACCEPTED_ROUTE_COUNT;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_OUTSIDE_IP_ADDRESS;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_STATUS;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_STATUS_MESSAGE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;

/** Telemetry data for AWS VPN connections */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public final class VgwTelemetry implements Serializable {

  private final int _acceptedRouteCount;

  @Nonnull private final Ip _outsideIpAddress;

  @Nonnull private final String _status;

  @Nonnull private final String _statusMessage;

  @JsonCreator
  private static VgwTelemetry create(
      @Nullable @JsonProperty(JSON_KEY_ACCEPTED_ROUTE_COUNT) Integer acceptedRouteCount,
      @Nullable @JsonProperty(JSON_KEY_OUTSIDE_IP_ADDRESS) Ip outsideIpAddress,
      @Nullable @JsonProperty(JSON_KEY_STATUS) String status,
      @Nullable @JsonProperty(JSON_KEY_STATUS_MESSAGE) String statusMessage) {
    checkArgument(
        acceptedRouteCount != null, "Accepted route count cannot be null in VgwTelemetry");
    checkArgument(outsideIpAddress != null, "Outside IP address cannot be null in VgwTelemetry");
    checkArgument(status != null, "Status cannot be null in VgwTelemetry");
    checkArgument(statusMessage != null, "Status message cannot be null in VgwTelemetry");

    return new VgwTelemetry(acceptedRouteCount, outsideIpAddress, status, statusMessage);
  }

  public VgwTelemetry(
      int acceptedRouteCount, Ip outsideIpAddress, String status, String statusMessage) {
    _acceptedRouteCount = acceptedRouteCount;
    _outsideIpAddress = outsideIpAddress;
    _status = status;
    _statusMessage = statusMessage;
  }

  public int getAcceptedRouteCount() {
    return _acceptedRouteCount;
  }

  @Nonnull
  public Ip getOutsideIpAddress() {
    return _outsideIpAddress;
  }

  @Nonnull
  public String getStatus() {
    return _status;
  }

  @Nonnull
  public String getStatusMessage() {
    return _statusMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VgwTelemetry that = (VgwTelemetry) o;
    return _acceptedRouteCount == that._acceptedRouteCount
        && Objects.equal(_outsideIpAddress, that._outsideIpAddress)
        && Objects.equal(_status, that._status)
        && Objects.equal(_statusMessage, that._statusMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(_acceptedRouteCount, _outsideIpAddress, _status, _statusMessage);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("_acceptedRouteCount", _acceptedRouteCount)
        .add("_outsideIpAddress", _outsideIpAddress)
        .add("_status", _status)
        .add("_statusMessage", _statusMessage)
        .toString();
  }
}
