package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Represents an AWS route table */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public final class RouteTable implements AwsVpcEntity, Serializable {

  @JsonIgnoreProperties(ignoreUnknown = true)
  @ParametersAreNonnullByDefault
  static class Association implements Serializable {

    private final boolean _isMain;
    @Nullable private final String _subnetId;

    @JsonCreator
    private static Association create(
        @Nullable @JsonProperty(JSON_KEY_MAIN) Boolean isMain,
        @Nullable @JsonProperty(JSON_KEY_SUBNET_ID) String subnetId) {
      checkArgument(isMain != null, "Main key must be present in route table association");
      return new Association(isMain, subnetId);
    }

    public Association(boolean isMain, @Nullable String subnetId) {
      _isMain = isMain;
      _subnetId = subnetId;
    }

    @Nullable
    public String getSubnetId() {
      return _subnetId;
    }

    public boolean isMain() {
      return _isMain;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Association that = (Association) o;
      return _isMain == that._isMain && Objects.equal(_subnetId, that._subnetId);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(_isMain, _subnetId);
    }
  }

  @Nonnull private final List<Route> _routes;

  @Nonnull private final List<Association> _associations;

  @Nonnull private final String _routeTableId;

  @Nonnull private final String _vpcId;

  @JsonCreator
  private static RouteTable create(
      @Nullable @JsonProperty(JSON_KEY_ROUTE_TABLE_ID) String routeTableId,
      @Nullable @JsonProperty(JSON_KEY_VPC_ID) String vpcId,
      @Nullable @JsonProperty(JSON_KEY_ASSOCIATIONS) List<Association> associations,
      @Nullable @JsonProperty(JSON_KEY_ROUTES) List<Route> routes) {
    checkArgument(routeTableId != null, "Route table id cannot be null");
    checkArgument(vpcId != null, "VPC id cannot be null for route table");
    checkArgument(associations != null, "Associations cannot be null for route table");
    checkArgument(routes != null, "Routes cannot be null for route table");

    return new RouteTable(routeTableId, vpcId, associations, routes);
  }

  public RouteTable(
      String routeTableId, String vpcId, List<Association> associations, List<Route> routes) {
    _routeTableId = routeTableId;
    _vpcId = vpcId;
    _associations = associations;
    _routes = routes;
  }

  @Nonnull
  public List<Association> getAssociations() {
    return _associations;
  }

  @Override
  public String getId() {
    return _routeTableId;
  }

  @Nonnull
  public List<Route> getRoutes() {
    return _routes;
  }

  @Nonnull
  public String getVpcId() {
    return _vpcId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RouteTable that = (RouteTable) o;
    return Objects.equal(_routes, that._routes)
        && Objects.equal(_associations, that._associations)
        && Objects.equal(_routeTableId, that._routeTableId)
        && Objects.equal(_vpcId, that._vpcId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(_routes, _associations, _routeTableId, _vpcId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("_routes", _routes)
        .add("_associations", _associations)
        .add("_routeTableId", _routeTableId)
        .add("_vpcId", _vpcId)
        .toString();
  }
}
