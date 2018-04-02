package org.batfish.z3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AclIpSpaceLine;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.visitors.GenericIpSpaceVisitor;

/**
 * Simplify an IpSpace. For example, there are many ways to express an empty IpSpace or the entire
 * universe of IPs. We try to detect these and convert them to EmptyIpSpace or UniverseIpSpace
 * respectively.
 */
public class IpSpaceSimplifier implements GenericIpSpaceVisitor<IpSpace> {

  private static final IpSpaceSimplifier INSTANCE = new IpSpaceSimplifier();

  private IpSpaceSimplifier() {}

  public static IpSpace simplify(IpSpace ipSpace) {
    return ipSpace.accept(INSTANCE);
  }

  @Override
  public IpSpace castToGenericIpSpaceVisitorReturnType(Object o) {
    return (IpSpace) o;
  }

  /**
   * To simplify an AclIpSpace: 1. Simplify the IpSpace of each line 2. Remove EmptyIpSpace lines 3.
   * Remove all lines after the first UniverseIpSpace line - More generally, we could remove all
   * lines whose spaces are covered by a previous line, but this is not implemented. It's also
   * probably too expensive to implement a complete IpSpace subset operation, so we'll stick to the
   * easy and most important case.
   */
  @Override
  public IpSpace visitAclIpSpace(AclIpSpace aclIpSpace) {
    List<AclIpSpaceLine> simplifiedLines = new ArrayList<>();
    for (AclIpSpaceLine line : aclIpSpace.getLines()) {
      IpSpace simplifiedLineIpSpace = line.getIpSpace().accept(this);
      if (simplifiedLineIpSpace == EmptyIpSpace.INSTANCE) {
        continue;
      }
      AclIpSpaceLine simplifiedLine = line.rebuild().setIpSpace(simplifiedLineIpSpace).build();
      simplifiedLines.add(simplifiedLine);
      if (simplifiedLineIpSpace == UniverseIpSpace.INSTANCE) {
        break;
      }
    }

    /*
     * If there is only one line, and it accepts, then simplify to the space of that line.
     */
    if (simplifiedLines.size() == 1 && simplifiedLines.get(0).getAction() == LineAction.ACCEPT) {
      return simplifiedLines.get(0).getIpSpace();
    }

    /*
     * If all lines reject (or there are no lines), simplify to EmptyIpSpace.
     */
    if (simplifiedLines.stream().allMatch(line -> line.getAction() == LineAction.REJECT)) {
      return EmptyIpSpace.INSTANCE;
    }

    /*
     * If all lines are accepts, and the last accepts UniverseIpSpace, then this can be simplified
     * to UniverseIpSpace.
     */
    if (simplifiedLines.get(simplifiedLines.size() - 1).getIpSpace() == UniverseIpSpace.INSTANCE
        && simplifiedLines.stream().allMatch(line -> line.getAction() == LineAction.ACCEPT)) {
      return UniverseIpSpace.INSTANCE;
    }

    return AclIpSpace.builder().setLines(simplifiedLines).build();
  }

  @Override
  public IpSpace visitEmptyIpSpace(EmptyIpSpace emptyIpSpace) {
    return emptyIpSpace;
  }

  @Override
  public IpSpace visitIp(Ip ip) {
    return ip;
  }

  @Override
  public IpSpace visitIpWildcard(IpWildcard ipWildcard) {
    if (ipWildcard == IpWildcard.ANY) {
      return UniverseIpSpace.INSTANCE;
    }
    return ipWildcard;
  }

  @Override
  public IpSpace visitIpWildcardSetIpSpace(IpWildcardSetIpSpace ipWildcardSetIpSpace) {
    // Remove whitelisted wildcards that are covered by blacklisted wildcards
    Set<IpWildcard> whitelist =
        ipWildcardSetIpSpace
            .getWhitelist()
            .stream()
            .filter(
                whitelistedIpWildcard ->
                    ipWildcardSetIpSpace
                        .getBlacklist()
                        .stream()
                        .noneMatch(whitelistedIpWildcard::subsetOf))
            .collect(Collectors.toSet());

    if (whitelist.isEmpty()) {
      return EmptyIpSpace.INSTANCE;
    }

    // Remove blacklisted wildcards that don't overlap with whitelisted wildcards
    Set<IpWildcard> blacklist =
        ipWildcardSetIpSpace
            .getBlacklist()
            .stream()
            .filter(
                blacklistedIpWildcard ->
                    whitelist.stream().anyMatch(blacklistedIpWildcard::intersects))
            .collect(Collectors.toSet());

    if (whitelist.contains(IpWildcard.ANY) && blacklist.isEmpty()) {
      return UniverseIpSpace.INSTANCE;
    }

    return IpWildcardSetIpSpace.builder().including(whitelist).excluding(blacklist).build();
  }

  @Override
  public IpSpace visitPrefix(Prefix prefix) {
    if (prefix.equals(Prefix.ZERO)) {
      return UniverseIpSpace.INSTANCE;
    }
    return prefix;
  }

  @Override
  public IpSpace visitUniverseIpSpace(UniverseIpSpace universeIpSpace) {
    return universeIpSpace;
  }
}
