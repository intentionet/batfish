package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpWildcard;

/** Represents an AWS security group */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
final class SecurityGroup implements AwsVpcEntity, Serializable {

  @Nonnull private final String _groupId;

  @Nonnull private final String _groupName;

  @Nonnull private final List<IpPermissions> _ipPermsEgress;

  @Nonnull private final List<IpPermissions> _ipPermsIngress;

  @Nonnull private final Set<IpWildcard> _usersIpSpace;

  @JsonCreator
  private static SecurityGroup create(
      @Nullable @JsonProperty(JSON_KEY_GROUP_ID) String groupId,
      @Nullable @JsonProperty(JSON_KEY_GROUP_NAME) String groupName,
      @Nullable @JsonProperty(JSON_KEY_IP_PERMISSIONS_EGRESS) List<IpPermissions> ipPermsEgress,
      @Nullable @JsonProperty(JSON_KEY_IP_PERMISSIONS) List<IpPermissions> ipPermsIngress) {
    checkArgument(groupId != null, "Group id cannot be null for security groups");
    checkArgument(groupName != null, "Group name cannot be null for security groups");
    checkArgument(
        ipPermsEgress != null, "Egress IP permissions list cannot be null for security groups");
    checkArgument(
        ipPermsIngress != null, "Ingress IP permissions list cannot be null for security groups");
    return new SecurityGroup(groupId, groupName, ipPermsEgress, ipPermsIngress);
  }

  SecurityGroup(
      String groupId,
      String groupName,
      List<IpPermissions> ipPermsEgress,
      List<IpPermissions> ipPermsIngress) {
    _groupId = groupId;
    _groupName = groupName;
    _ipPermsEgress = ipPermsEgress;
    _ipPermsIngress = ipPermsIngress;
    _usersIpSpace = new HashSet<>();
  }

  /** Adds any access lines for this security group to the inbound and outbound rules. */
  void addInOutAccessLines(
      List<IpAccessListLine> inboundRules, List<IpAccessListLine> outboundRules, Region region) {
    for (ListIterator<IpPermissions> it = _ipPermsIngress.listIterator(); it.hasNext(); ) {
      int seq = it.nextIndex();
      IpPermissions p = it.next();
      p.toIpAccessListLine(true, region, _groupId + " - " + _groupName + " [ingress] " + seq)
          .ifPresent(inboundRules::add);
    }
    for (ListIterator<IpPermissions> it = _ipPermsEgress.listIterator(); it.hasNext(); ) {
      int seq = it.nextIndex();
      IpPermissions p = it.next();
      p.toIpAccessListLine(false, region, _groupId + " - " + _groupName + " [egress] " + seq)
          .ifPresent(outboundRules::add);
    }
  }

  @Nonnull
  String getGroupId() {
    return _groupId;
  }

  @Nonnull
  String getGroupName() {
    return _groupName;
  }

  @Override
  public String getId() {
    return _groupId;
  }

  @Nonnull
  List<IpPermissions> getIpPermsEgress() {
    return _ipPermsEgress;
  }

  @Nonnull
  List<IpPermissions> getIpPermsIngress() {
    return _ipPermsIngress;
  }

  Set<IpWildcard> getUsersIpSpace() {
    return _usersIpSpace;
  }

  void updateConfigIps(Configuration configuration) {
    configuration.getAllInterfaces().values().stream()
        .flatMap(iface -> iface.getAllConcreteAddresses().stream())
        .map(ConcreteInterfaceAddress::getIp)
        .map(IpWildcard::create)
        .forEach(ipWildcard -> getUsersIpSpace().add(ipWildcard));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SecurityGroup)) {
      return false;
    }
    SecurityGroup that = (SecurityGroup) o;
    return Objects.equals(_groupId, that._groupId)
        && Objects.equals(_groupName, that._groupName)
        && Objects.equals(_ipPermsEgress, that._ipPermsEgress)
        && Objects.equals(_ipPermsIngress, that._ipPermsIngress)
        && Objects.equals(_usersIpSpace, that._usersIpSpace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_groupId, _groupName, _ipPermsEgress, _ipPermsIngress, _usersIpSpace);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("_groupId", _groupId)
        .add("_groupName", _groupName)
        .add("_ipPermsEgress", _ipPermsEgress)
        .add("_ipPermsIngress", _ipPermsIngress)
        .add("_usersIpSpace", _usersIpSpace)
        .toString();
  }
}
