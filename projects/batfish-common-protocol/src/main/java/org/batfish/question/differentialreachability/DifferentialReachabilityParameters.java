package org.batfish.question.differentialreachability;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.specifier.IpSpaceAssignment;

/** Parameters for differential reachability analysis */
public class DifferentialReachabilityParameters {
  private final Set<FlowDisposition> _flowDispositions;
  private final Set<String> _forbiddenTransitNodes;
  private final Set<String> _finalNodes;
  private final AclLineMatchExpr _headerSpace;
  private final boolean _ignoreAcls;
  private final IpSpaceAssignment _ipSpaceAssignment;
  private final Set<String> _requiredTransitNodes;

  public DifferentialReachabilityParameters(
      Set<FlowDisposition> flowDispositions,
      Set<String> forbiddenTransitNodes,
      Set<String> finalNodes,
      AclLineMatchExpr headerSpace,
      boolean ignoreAcls,
      IpSpaceAssignment ipSpaceAssignment,
      Set<String> requiredTransitNodes) {
    _flowDispositions = ImmutableSet.copyOf(flowDispositions);
    _forbiddenTransitNodes = ImmutableSet.copyOf(forbiddenTransitNodes);
    _finalNodes = ImmutableSet.copyOf(finalNodes);
    _headerSpace = headerSpace;
    _ignoreAcls = ignoreAcls;
    _ipSpaceAssignment = ipSpaceAssignment;
    _requiredTransitNodes = ImmutableSet.copyOf(requiredTransitNodes);
  }

  /**
   * @return The set of {@link FlowDisposition dispositions} of interest. Search for differences
   *     between the sets of {@link org.batfish.datamodel.Flow flows} that have a disposition in
   *     this set.
   */
  public Set<FlowDisposition> getFlowDispositions() {
    return _flowDispositions;
  }

  /**
   * @return The set of router hostnames that may not be transited. Search for differences between
   *     the the sets of {@link org.batfish.datamodel.Flow flows} with at least one path that does
   *     not transit any node in this set.
   */
  public Set<String> getForbiddenTransitNodes() {
    return _forbiddenTransitNodes;
  }

  /**
   * @return The set of router hostnames that must be transited. Search for differences between the
   *     the sets of {@link org.batfish.datamodel.Flow flows} with at least one path that transits
   *     any node in this set.
   */
  public Set<String> getFinalNodes() {
    return _finalNodes;
  }

  /**
   * @return The headerspace of interest. Search for differences between sets of {@link
   *     org.batfish.datamodel.Flow flows} whose headers are contained in this space.
   */
  public AclLineMatchExpr getHeaderSpace() {
    return _headerSpace;
  }

  /** @return Whether to ignore ACLs and only analyze forwarding behavior. */
  public boolean getIgnoreAcls() {
    return _ignoreAcls;
  }

  /**
   * @return The source {@link IpSpaceAssignment}. Search for differences between sets of {@link
   *     org.batfish.datamodel.Flow flows} that start at a location and with an initial source IP
   *     consistent with an entry in the assignment.
   */
  public IpSpaceAssignment getIpSpaceAssignment() {
    return _ipSpaceAssignment;
  }

  /**
   * @return The set of router hostnames where flows stop. Search for differences between the sets
   *     of {@link org.batfish.datamodel.Flow flows} with at least one path that stops at a router
   *     in this set.
   */
  public Set<String> getRequiredTransitNodes() {
    return _requiredTransitNodes;
  }
}
