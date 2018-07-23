package org.batfish.specifier;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.IpSpace;

/**
 * An {@link IpSpaceSpecifier} that represents the {@link IpSpace} of host subnets (prefixes of
 * length less than or equal to {@length HOST_SUBNET_MAX_PREFIX_LENGTH 29} bits) connected to nodes
 * with names matching an input {@link Pattern regex}.
 */
public final class NodeNameRegexConnectedHostsIpSpaceSpecifier implements IpSpaceSpecifier {
  /**
   * /32s are loopback interfaces -- no hosts are connected.
   *
   * <p>/31s are point-to-point connections between nodes -- again, no hosts.
   *
   * <p>/30s could have hosts, but usually do not. Historically, each subnet was required to reserve
   * two addresses: one identifying the network itself, and a broadcast address. This made /31s
   * invalid, since there were no usable IPs left over. A /30 had 2 usable IPs, so was used for
   * point-to-point connections. Eventually /31s were allowed, but we assume here that any /30s are
   * hold-over point-to-point connections in the legacy model.
   */
  public static final int HOST_SUBNET_MAX_PREFIX_LENGTH = 29;

  private final Pattern _pattern;

  public NodeNameRegexConnectedHostsIpSpaceSpecifier(Pattern pattern) {
    _pattern = pattern;
  }

  @Override
  public IpSpaceAssignment resolve(Set<Location> locations, SpecifierContext ctxt) {
    Builder<IpSpace> includeBuilder = ImmutableList.builder();
    Builder<IpSpace> excludeBuilder = ImmutableList.builder();
    ctxt.getConfigs()
        .values()
        .stream()
        .filter(node -> _pattern.matcher(node.getHostname()).matches())
        .flatMap(node -> node.getInterfaces().values().stream())
        .flatMap(iface -> iface.getAllAddresses().stream())
        .filter(
            ifaceAddr -> ifaceAddr.getPrefix().getPrefixLength() <= HOST_SUBNET_MAX_PREFIX_LENGTH)
        .forEach(
            ifaceAddr -> {
              includeBuilder.add(ifaceAddr.getPrefix().toIpSpace());
              excludeBuilder.add(ifaceAddr.getIp().toIpSpace());
            });
    IpSpace include = AclIpSpace.union(includeBuilder.build());
    IpSpace exclude = AclIpSpace.union(excludeBuilder.build());
    IpSpace ipSpace = firstNonNull(AclIpSpace.difference(include, exclude), EmptyIpSpace.INSTANCE);
    return IpSpaceAssignment.builder().assign(locations, ipSpace).build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NodeNameRegexConnectedHostsIpSpaceSpecifier)) {
      return false;
    }
    NodeNameRegexConnectedHostsIpSpaceSpecifier that =
        (NodeNameRegexConnectedHostsIpSpaceSpecifier) o;
    return Objects.equals(_pattern.pattern(), that._pattern.pattern());
  }

  @Override
  public int hashCode() {
    return Objects.hash(_pattern);
  }
}
