package org.batfish.representation.cisco;

import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.datamodel.transformation.Transformation.when;
import static org.batfish.datamodel.transformation.TransformationStep.assignDestinationIp;
import static org.batfish.datamodel.transformation.TransformationStep.assignSourceIp;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationStep;

/** Representation of a Cisco IOS dynamic NAT. */
@ParametersAreNonnullByDefault
public final class CiscoIosDynamicNat extends CiscoIosNat {

  private @Nullable String _aclName;
  private @Nullable String _natPool;

  @VisibleForTesting
  public static String computeDynamicDestinationNatAclName(@Nonnull String natAclName) {
    return String.format("~DYNAMIC_DESTINATION_NAT_INSIDE_ACL~%s~", natAclName);
  }

  @Nullable
  public String getAclName() {
    return _aclName;
  }

  public void setAclName(@Nullable String aclName) {
    _aclName = aclName;
  }

  public void setNatPool(@Nullable String natPool) {
    _natPool = natPool;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CiscoIosDynamicNat)) {
      return false;
    }
    CiscoIosDynamicNat other = (CiscoIosDynamicNat) o;
    return (getAction() == other.getAction())
        && Objects.equals(getVrf(), other.getVrf())
        && Objects.equals(_aclName, other._aclName)
        && Objects.equals(_natPool, other._natPool);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_aclName, getAction(), _natPool, getVrf());
  }

  @Override
  protected int natCompare(CiscoIosNat other) {
    return 0;
  }

  @Override
  public Optional<Transformation.Builder> toIncomingTransformation(
      Map<String, IpAccessList> ipAccessLists, Map<String, NatPool> natPools) {
    // SOURCE_OUTSIDE matches and dynamically translates source addresses on ingress.
    if (getAction() != RuleAction.SOURCE_OUTSIDE) {
      // INSIDE rules require reverse translation on ingress, which is not
      // yet supported (we need to track NAT table state for dynamic NAT).
      return Optional.empty();
    }

    if (isMalformed(natPools, ipAccessLists)) {
      return Optional.empty();
    }

    return Optional.of(makeTransformation(permittedByAcl(_aclName), natPools.get(_natPool), false));
  }

  @Override
  public Optional<Transformation.Builder> toOutgoingTransformation(
      Map<String, IpAccessList> ipAccessLists,
      Map<String, NatPool> natPools,
      Set<String> insideInterfaces,
      Configuration c) {
    /*
     * SOURCE_INSIDE matches and dynamically translates source addresses on egress
     * DESTINATION_INSIDE matches and dynamically translates destination addresses on egress
     */

    if (getAction() != RuleAction.SOURCE_INSIDE && getAction() != RuleAction.DESTINATION_INSIDE) {
      // OUTSIDE rules require reverse translation on egress, which is not
      // yet supported (we need to track NAT table state for dynamic NAT).
      return Optional.empty();
    }

    if (isMalformed(natPools, ipAccessLists)) {
      return Optional.empty();
    }
    assert _aclName != null; // invariant of isMalformed being false, to help compiler.

    IpAccessList natAcl = ipAccessLists.get(_aclName);

    if (getAction() == RuleAction.DESTINATION_INSIDE) {
      // Expect all lines to be header space matches for NAT ACL
      if (!natAcl.getLines().stream()
          .allMatch(
              l ->
                  l instanceof ExprAclLine
                      && ((ExprAclLine) l).getMatchCondition() instanceof MatchHeaderSpace)) {
        return Optional.empty();
      }

      // Create reverse acl to match destination address instead of source address
      String reverseAclName = computeDynamicDestinationNatAclName(_aclName);
      List<AclLine> lines =
          natAcl.getLines().stream()
              // Already checked that all lines are instances of ExprAclLine
              .map(ExprAclLine.class::cast)
              .map(
                  line -> {
                    HeaderSpace origHeader =
                        ((MatchHeaderSpace) line.getMatchCondition()).getHeaderspace();
                    HeaderSpace headerSpace =
                        origHeader.toBuilder()
                            .setDstIps(origHeader.getSrcIps())
                            .setSrcIps((IpSpace) null)
                            .build();
                    return ExprAclLine.builder()
                        .setAction(line.getAction())
                        .setMatchCondition(new MatchHeaderSpace(headerSpace))
                        .build();
                  })
              .collect(Collectors.toList());
      natAcl =
          IpAccessList.builder()
              .setLines(lines)
              .setName(reverseAclName)
              .setOwner(c)
              .setSourceName(_aclName)
              .setSourceType(natAcl.getSourceType())
              .build();
    }

    AclLineMatchExpr natAclExpr =
        and(permittedByAcl(natAcl.getName()), new MatchSrcInterface(insideInterfaces));
    return Optional.of(makeTransformation(natAclExpr, natPools.get(_natPool), true));
  }

  /**
   * Returns the (forward) transformation for this dynamic NAT expression using the given condition
   * on which to NAT, pool to NAT into, and the direction of traffic.
   */
  private Transformation.Builder makeTransformation(
      AclLineMatchExpr shouldNat, NatPool pool, boolean outgoing) {
    TransformationStep step =
        getAction().whatChanges(outgoing) == IpField.SOURCE
            ? assignSourceIp(pool.getFirst(), pool.getLast())
            : assignDestinationIp(pool.getFirst(), pool.getLast());
    return when(shouldNat).apply(step);
  }

  /**
   * Returns {@code true} iff this dynamic NAT configuration is invalid based on the given existing
   * NAT pools and access lists.
   */
  private boolean isMalformed(
      Map<String, NatPool> natPools, Map<String, IpAccessList> ipAccessLists) {
    // Validate that ACL is configured, present, and is a standard ACL.
    if (_aclName == null) {
      // Not configured (should be rejected by parser, but confirm).
      return true;
    }
    if (!ipAccessLists.containsKey(_aclName)) {
      // Invalid reference
      return true;
    }
    if (!CiscoStructureType.IPV4_ACCESS_LIST_STANDARD
        .getDescription()
        .equals(ipAccessLists.get(_aclName).getSourceType())) {
      // Cisco IOS only supports standard ACLs for dynamic NAT.
      return true;
    }

    // Validate that NAT pool is configured and present.
    if (_natPool == null) {
      // Parser allows this for Arista NAT (nat overload), but it is not supported
      return true;
    }
    if (!natPools.containsKey(_natPool)) {
      // Configuration has an invalid reference
      return true;
    }

    return false;
  }
}
