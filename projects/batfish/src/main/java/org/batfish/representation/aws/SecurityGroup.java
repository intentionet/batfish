package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;

/** Represents an AWS security group */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
final class SecurityGroup implements AwsVpcEntity, Serializable {

  @Nonnull private final String _groupId;

  @Nonnull private final String _groupName;

  @Nonnull private final List<IpPermissions> _ipPermsEgress;

  @Nonnull private final List<IpPermissions> _ipPermsIngress;

  /** IPs and instance names of the instances which refer to this security group */
  @Nonnull private final Map<Ip, String> _referrerIps;

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
    _referrerIps = new HashMap<>();
  }

  /** Converts this security group's ingress or egress permission terms to List of AclLines */
  List<AclLine> toAclLines(Region region, boolean ingress, Warnings warnings) {
    ImmutableList.Builder<AclLine> aclLines = ImmutableList.builder();
    List<IpPermissions> ipPerms = ingress ? _ipPermsIngress : _ipPermsEgress;
    for (ListIterator<IpPermissions> it = ipPerms.listIterator(); it.hasNext(); ) {
      int seq = it.nextIndex();
      IpPermissions p = it.next();
      aclLines.addAll(
          p.toIpAccessListLines(
              ingress,
              region,
              String.format(
                  "%s - %s [%s] %s", _groupId, _groupName, ingress ? "ingress" : "egress", seq),
              warnings));
    }
    return aclLines.build();
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

  Map<Ip, String> getReferrerIps() {
    return _referrerIps;
  }

  private static String humanReadableInstanceName(Configuration c) {
    if (c.getHumanName() == null) {
      return c.getHostname();
    }
    return String.format("%s (%s)", c.getHumanName(), c.getHostname());
  }

  void updateConfigIps(Configuration configuration) {
    configuration.getAllInterfaces().values().stream()
        .flatMap(iface -> iface.getAllConcreteAddresses().stream())
        .map(ConcreteInterfaceAddress::getIp)
        .forEach(ip -> _referrerIps.put(ip, humanReadableInstanceName(configuration)));
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
        && Objects.equals(_referrerIps, that._referrerIps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_groupId, _groupName, _ipPermsEgress, _ipPermsIngress, _referrerIps);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("_groupId", _groupId)
        .add("_groupName", _groupName)
        .add("_ipPermsEgress", _ipPermsEgress)
        .add("_ipPermsIngress", _ipPermsIngress)
        .add("_referrerIps", _referrerIps)
        .toString();
  }
}
