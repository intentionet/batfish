package org.batfish.main;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.questions.InvalidReachabilityParametersException;
import org.batfish.datamodel.visitors.IpSpaceRepresentative;
import org.batfish.question.ReachabilityParameters;
import org.batfish.question.ResolvedReachabilityParameters;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.IpSpaceAssignment.Entry;
import org.batfish.specifier.Location;
import org.batfish.specifier.SpecifierContextImpl;

/**
 * Resolve a {@link ReachabilityParameters} and return a {@link ResolvedReachabilityParameters}
 * object. This involves getting the right configs and dataplane, and resolving Location, Node, and
 * IpSpace specifiers. All validation of user input is done here.
 */
public final class ReachabilityParametersResolver {
  private final SpecifierContextImpl _context;

  private final DataPlane _dataPlane;

  private final ReachabilityParameters _params;

  private final IpSpaceRepresentative _ipSpaceRepresentative;

  @VisibleForTesting
  ReachabilityParametersResolver(
      IBatfish batfish, ReachabilityParameters params, NetworkSnapshot snapshot) {
    _dataPlane = batfish.loadDataPlane();
    _params = params;
    _context = new SpecifierContextImpl(batfish, snapshot);
    _ipSpaceRepresentative = new IpSpaceRepresentative();
  }

  public static ResolvedReachabilityParameters resolveReachabilityParameters(
      IBatfish batfish, ReachabilityParameters params, NetworkSnapshot snapshot)
      throws InvalidReachabilityParametersException {

    ReachabilityParametersResolver resolver =
        new ReachabilityParametersResolver(batfish, params, snapshot);
    SpecifierContextImpl context = resolver._context;

    // validate actions
    SortedSet<FlowDisposition> actions = params.getActions();
    if (actions.isEmpty()) {
      throw new InvalidReachabilityParametersException("No actions");
    }

    // resolve and validate final nodes
    Set<String> finalNodes = params.getFinalNodesSpecifier().resolve(context);
    if (finalNodes.isEmpty()) {
      throw new InvalidReachabilityParametersException("No final nodes");
    }

    // resolve and validate transit nodes
    Set<String> forbiddenTransitNodes = params.getForbiddenTransitNodesSpecifier().resolve(context);
    Set<String> requiredTransitNodes = params.getRequiredTransitNodesSpecifier().resolve(context);
    if (!requiredTransitNodes.isEmpty()
        && forbiddenTransitNodes.containsAll(requiredTransitNodes)) {
      throw new InvalidReachabilityParametersException(
          "All required transit nodes are also forbidden");
    }
    return ResolvedReachabilityParameters.builder()
        .setActions(actions)
        .setConfigurations(resolver._context.getConfigs())
        .setDataPlane(resolver._dataPlane)
        .setFinalNodes(finalNodes)
        .setForbiddenTransitNodes(forbiddenTransitNodes)
        .setHeaderSpace(resolver.resolveHeaderSpace())
        .setIgnoreFilters(params.getIgnoreFilters())
        .setMaxChunkSize(params.getMaxChunkSize())
        .setSourceIpSpaceAssignment(resolver.resolveSourceIpSpaceAssignment())
        .setSrcNatted(params.getSrcNatted())
        .setSpecialize(params.getSpecialize())
        .setRequiredTransitNodes(requiredTransitNodes)
        .build();
  }

  private AclLineMatchExpr resolveHeaderSpace() throws InvalidReachabilityParametersException {
    AclLineMatchExpr expr =
        and(
            firstNonNull(_params.getHeaderSpace(), AclLineMatchExprs.TRUE),
            matchDst(resolveDestinationIpSpace()));
    return _params.getInvertSearch() ? AclLineMatchExprs.not(expr) : expr;
  }

  @VisibleForTesting
  IpSpace resolveDestinationIpSpace() throws InvalidReachabilityParametersException {
    IpSpace destinationIpSpace =
        AclIpSpace.union(
            _params.getDestinationIpSpaceSpecifier().resolve(ImmutableSet.of(), _context)
                .getEntries().stream()
                .map(Entry::getIpSpace)
                .collect(ImmutableList.toImmutableList()));

    /*
     * Make sure the destinationIpSpace is non-empty. Otherwise, we'll get no results and no
     * explanation why.
     */
    if (!_ipSpaceRepresentative.getRepresentative(destinationIpSpace).isPresent()) {
      throw new InvalidReachabilityParametersException("Empty destination IpSpace");
    }
    return destinationIpSpace;
  }

  /** Returns {@code true} iff the given {@link Location} is active (aka, interface is up). */
  @VisibleForTesting
  static boolean isActive(Location l, Map<String, Configuration> configs) {
    NodeInterfacePair iface;
    if (l instanceof InterfaceLocation) {
      iface =
          NodeInterfacePair.of(
              ((InterfaceLocation) l).getNodeName(), ((InterfaceLocation) l).getInterfaceName());
    } else {
      assert l instanceof InterfaceLinkLocation;
      iface =
          NodeInterfacePair.of(
              ((InterfaceLinkLocation) l).getNodeName(),
              ((InterfaceLinkLocation) l).getInterfaceName());
    }
    return configs
        .get(iface.getHostname())
        .getAllInterfaces()
        .get(iface.getInterface())
        .getActive();
  }

  @VisibleForTesting
  IpSpaceAssignment resolveSourceIpSpaceAssignment() throws InvalidReachabilityParametersException {
    Set<Location> sourceLocations =
        _params.getSourceLocationSpecifier().resolve(_context).stream()
            .filter(l -> isActive(l, _context.getConfigs()))
            .collect(ImmutableSet.toImmutableSet());
    if (sourceLocations.isEmpty()) {
      throw new InvalidReachabilityParametersException("No matching source locations");
    }

    // resolve the IpSpaceSpecifier, and filter out entries with empty IpSpaces
    IpSpaceAssignment sourceIpSpaceAssignment =
        IpSpaceAssignment.of(
            _params.getSourceIpSpaceSpecifier().resolve(sourceLocations, _context).getEntries()
                .stream()
                .filter(
                    entry ->
                        _ipSpaceRepresentative.getRepresentative(entry.getIpSpace()).isPresent())
                .collect(ImmutableList.toImmutableList()));

    if (sourceIpSpaceAssignment.getEntries().isEmpty()) {
      throw new InvalidReachabilityParametersException("All sources have empty source IpSpaces");
    }
    return sourceIpSpaceAssignment;
  }
}
