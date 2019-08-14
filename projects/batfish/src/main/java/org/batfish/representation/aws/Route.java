package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_DESTINATION_CIDR_BLOCK;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_GATEWAY_ID;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_INSTANCE_ID;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_NAT_GATEWAY_ID;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_NETWORK_INTERFACE_ID;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_STATE;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_VPC_PEERING_CONNECTION_ID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Prefix;

/** Representation of a route in AWS */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
final class Route implements Serializable {

  enum State {
    ACTIVE,
    BLACKHOLE
  }

  enum TargetType {
    Gateway,
    Instance,
    NatGateway,
    NetworkInterface,
    Unavailable,
    VpcPeeringConnection
  }

  static final int DEFAULT_STATIC_ROUTE_ADMIN = 1;

  static final int DEFAULT_STATIC_ROUTE_COST = 0;

  @Nonnull private final Prefix _destinationCidrBlock;

  @Nonnull private final State _state;
  @Nullable private final String _target;
  @Nonnull private final TargetType _targetType;

  @JsonCreator
  private static Route create(
      @Nullable @JsonProperty(JSON_KEY_DESTINATION_CIDR_BLOCK) Prefix destinationCidrBlock,
      @Nullable @JsonProperty(JSON_KEY_STATE) String stateStr,
      @Nullable @JsonProperty(JSON_KEY_VPC_PEERING_CONNECTION_ID) String vpcPeeringConnectionId,
      @Nullable @JsonProperty(JSON_KEY_GATEWAY_ID) String gatewayId,
      @Nullable @JsonProperty(JSON_KEY_NAT_GATEWAY_ID) String natGatewayId,
      @Nullable @JsonProperty(JSON_KEY_NETWORK_INTERFACE_ID) String networkInterfaceId,
      @Nullable @JsonProperty(JSON_KEY_INSTANCE_ID) String instanceId) {

    checkArgument(
        destinationCidrBlock != null, "Destination CIDR block cannot be null for a route");
    checkArgument(stateStr != null, "State cannot be null for a route");

    State state = State.valueOf(stateStr.toUpperCase());
    String target;
    TargetType targetType;

    if (vpcPeeringConnectionId != null) {
      targetType = TargetType.VpcPeeringConnection;
      target = vpcPeeringConnectionId;
    } else if (gatewayId != null) {
      targetType = TargetType.Gateway;
      target = gatewayId;
    } else if (natGatewayId != null) {
      targetType = TargetType.NatGateway;
      target = natGatewayId;
    } else if (networkInterfaceId != null) {
      targetType = TargetType.NetworkInterface;
      target = networkInterfaceId;
    } else if (instanceId != null) {
      // NOTE: so far in practice this branch is never reached after moving
      // networkInterfaceId above it!
      targetType = TargetType.Instance;
      target = instanceId;
    } else if (state == State.BLACKHOLE) {
      targetType = TargetType.Unavailable;
      target = null;
    } else {
      throw new IllegalArgumentException(
          "Unable to determine target type in route for " + destinationCidrBlock);
    }

    return new Route(destinationCidrBlock, state, target, targetType);
  }

  Route(Prefix destinationCidrBlock, State state, @Nullable String target, TargetType targetType) {
    _destinationCidrBlock = destinationCidrBlock;
    _state = state;
    _target = target;
    _targetType = targetType;
  }

  @Nonnull
  public Prefix getDestinationCidrBlock() {
    return _destinationCidrBlock;
  }

  @Nonnull
  public State getState() {
    return _state;
  }

  @Nullable
  public String getTarget() {
    return _target;
  }

  @Nonnull
  public TargetType getTargetType() {
    return _targetType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Route)) {
      return false;
    }
    Route route = (Route) o;
    return Objects.equals(_destinationCidrBlock, route._destinationCidrBlock)
        && _state == route._state
        && Objects.equals(_target, route._target)
        && _targetType == route._targetType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_destinationCidrBlock, _state, _target, _targetType);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("_destinationCidrBlock", _destinationCidrBlock)
        .add("_state", _state)
        .add("_target", _target)
        .add("_targetType", _targetType)
        .toString();
  }
}
