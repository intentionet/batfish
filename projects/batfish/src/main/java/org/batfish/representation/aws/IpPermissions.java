package org.batfish.representation.aws;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_CIDR_IP;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_FROM_PORT;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_GROUP_ID;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_IP_PROTOCOL;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_IP_RANGES;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_PREFIX_LIST_ID;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_PREFIX_LIST_IDS;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_TO_PORT;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_USER_GROUP_ID_PAIRS;
import static org.batfish.representation.aws.Utils.checkNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.acl.MatchHeaderSpace;

/** IP packet permissions within AWS security groups */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
final class IpPermissions implements Serializable {

  @JsonIgnoreProperties(ignoreUnknown = true)
  @ParametersAreNonnullByDefault
  private static final class IpRange implements Serializable {

    @Nonnull private final Prefix _prefix;

    @JsonCreator
    private static IpRange create(@Nullable @JsonProperty(JSON_KEY_CIDR_IP) Prefix prefix) {
      checkArgument(prefix != null, "Prefix cannot be null in IpRange");
      return new IpRange(prefix);
    }

    IpRange(Prefix prefix) {
      _prefix = prefix;
    }

    @Nonnull
    public Prefix getPrefix() {
      return _prefix;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof IpRange)) {
        return false;
      }
      IpRange that = (IpRange) o;
      return Objects.equals(_prefix, that._prefix);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_prefix);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @ParametersAreNonnullByDefault
  private static final class PrefixListId implements Serializable {

    @Nonnull private final String _id;

    @JsonCreator
    private static PrefixListId create(@Nullable @JsonProperty(JSON_KEY_PREFIX_LIST_ID) String id) {
      checkNonNull(id, JSON_KEY_PREFIX_LIST_ID, "PrefixListIds");
      return new PrefixListId(id);
    }

    PrefixListId(String id) {
      _id = id;
    }

    @Nonnull
    public String getId() {
      return _id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PrefixListId)) {
        return false;
      }
      PrefixListId that = (PrefixListId) o;
      return _id.equals(that._id);
    }

    @Override
    public int hashCode() {
      return _id.hashCode();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @ParametersAreNonnullByDefault
  private static final class UserIdGroupPair implements Serializable {

    @Nonnull private final String _groupId;

    @JsonCreator
    private static UserIdGroupPair create(
        @Nullable @JsonProperty(JSON_KEY_GROUP_ID) String groupId) {
      checkArgument(groupId != null, "Group id cannot be null in user id group pair");
      return new UserIdGroupPair(groupId);
    }

    UserIdGroupPair(String groupId) {
      _groupId = groupId;
    }

    @Nonnull
    String getGroupId() {
      return _groupId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof UserIdGroupPair)) {
        return false;
      }
      UserIdGroupPair that = (UserIdGroupPair) o;
      return Objects.equals(_groupId, that._groupId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_groupId);
    }
  }

  private final int _fromPort;

  @Nonnull private final String _ipProtocol;

  @Nonnull private final List<Prefix> _ipRanges;

  @Nonnull private final List<String> _prefixList;

  @Nonnull private final List<String> _securityGroups;

  private int _toPort;

  @JsonCreator
  private static IpPermissions create(
      @Nullable @JsonProperty(JSON_KEY_IP_PROTOCOL) String ipProtocol,
      @Nullable @JsonProperty(JSON_KEY_FROM_PORT) Integer fromPort,
      @Nullable @JsonProperty(JSON_KEY_TO_PORT) Integer toPort,
      @Nullable @JsonProperty(JSON_KEY_IP_RANGES) List<IpRange> ipRanges,
      @Nullable @JsonProperty(JSON_KEY_PREFIX_LIST_IDS) List<PrefixListId> prefixes,
      @Nullable @JsonProperty(JSON_KEY_USER_GROUP_ID_PAIRS)
          List<UserIdGroupPair> userIdGroupPairs) {
    checkArgument(ipProtocol != null, "IP protocol cannot be null for IP permissions");
    checkArgument(ipRanges != null, "IP ranges cannot be null for IP permissions");
    checkArgument(
        userIdGroupPairs != null, "User Id groups pairs cannot be null for IP permissions");

    return new IpPermissions(
        ipProtocol,
        (fromPort == null || fromPort < 0 || fromPort > 65535) ? 0 : fromPort,
        (toPort == null || toPort < 0 || toPort > 65535) ? 65535 : toPort,
        (ipRanges.stream().map(IpRange::getPrefix).collect(ImmutableList.toImmutableList())),
        firstNonNull(prefixes, ImmutableList.<PrefixListId>of()).stream()
            .map(PrefixListId::getId)
            .collect(ImmutableList.toImmutableList()),
        userIdGroupPairs.stream()
            .map(UserIdGroupPair::getGroupId)
            .collect(ImmutableList.toImmutableList()));
  }

  IpPermissions(
      String ipProtocol,
      int fromPort,
      int toPort,
      List<Prefix> ipRanges,
      List<String> prefixList,
      List<String> securityGroups) {
    _ipProtocol = ipProtocol;
    _fromPort = fromPort;
    _toPort = toPort;
    _ipRanges = ipRanges;
    _prefixList = prefixList;
    _securityGroups = securityGroups;
  }

  private SortedSet<IpWildcard> collectIpWildCards(Region region) {
    ImmutableSortedSet.Builder<IpWildcard> ipWildcardBuilder =
        new ImmutableSortedSet.Builder<>(Comparator.naturalOrder());

    _ipRanges.stream().map(IpWildcard::create).forEach(ipWildcardBuilder::add);

    _securityGroups.stream()
        .map(sgID -> region.getSecurityGroups().get(sgID))
        .filter(Objects::nonNull)
        .flatMap(sg -> sg.getUsersIpSpace().stream())
        .forEach(ipWildcardBuilder::add);

    _prefixList.stream()
        .map(id -> region.getPrefixLists().get(id))
        .filter(Objects::nonNull)
        .flatMap(prefixList -> prefixList.getCidrs().stream())
        .forEach(pfx -> ipWildcardBuilder.add(IpWildcard.create(pfx)));

    return ipWildcardBuilder.build();
  }

  /**
   * Converts this {@link IpPermissions} to an {@link IpAccessListLine}.
   *
   * <p>Returns {@link Optional#empty()} if the security group cannot be processed, e.g., uses an
   * unsupported definition of the affected IP addresses.
   */
  Optional<IpAccessListLine> toIpAccessListLine(boolean ingress, Region region, String name) {
    Collection<IpWildcard> ips = collectIpWildCards(region);
    if (ips.isEmpty()) {
      // IPs should have been populated using either SG or IP ranges,  if not then this IpPermission
      // is incomplete.
      return Optional.empty();
    }

    HeaderSpace.Builder constraints = HeaderSpace.builder();
    IpProtocol protocol = Utils.toIpProtocol(_ipProtocol);
    if (protocol != null) {
      constraints.setIpProtocols(protocol);
    }
    // if the range isn't all ports, set it in ACL
    if (_fromPort != 0 || _toPort != 65535) {
      assert protocol == IpProtocol.TCP || protocol == IpProtocol.UDP;
      constraints.setDstPorts(ImmutableSet.of(new SubRange(_fromPort, _toPort)));
    }
    if (ingress) {
      constraints.setSrcIps(ips);
    } else {
      constraints.setDstIps(ips);
    }

    return Optional.ofNullable(
        IpAccessListLine.accepting()
            .setMatchCondition(new MatchHeaderSpace(constraints.build()))
            .setName(name)
            .build());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IpPermissions)) {
      return false;
    }
    IpPermissions that = (IpPermissions) o;
    return _fromPort == that._fromPort
        && _toPort == that._toPort
        && Objects.equals(_ipProtocol, that._ipProtocol)
        && Objects.equals(_ipRanges, that._ipRanges)
        && Objects.equals(_ipRanges, that._ipRanges)
        && Objects.equals(_prefixList, that._prefixList)
        && Objects.equals(_securityGroups, that._securityGroups);
  }

  @Override
  public int hashCode() {
    return com.google.common.base.Objects.hashCode(
        _fromPort, _ipProtocol, _ipRanges, _prefixList, _securityGroups, _toPort);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("_fromPort", _fromPort)
        .add("_ipProtocol", _ipProtocol)
        .add("_ipRanges", _ipRanges)
        .add("_prefixList", _prefixList)
        .add("_securityGroups", _securityGroups)
        .add("_toPort", _toPort)
        .toString();
  }
}
