package org.batfish.dataplane.ibdp;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRouteDecorator;
import org.batfish.dataplane.rib.RibDelta;

/**
 * Interface that all routing processes must implement.
 *
 * @param <T> the topology type for determining neighbor adjacencies
 * @param <R> the route type this process produces for merging into main RIB
 */
@ParametersAreNonnullByDefault
public interface RoutingProcess<T, R extends AbstractRouteDecorator> {
  /**
   * Initialization of the routing process. Called exactly once per computation of the dataplane.
   */
  void initialize();

  /**
   * Topology update. Called every time the dataplane engine determines that a change to the
   * protocol-specific topology is available, which could result in neighbor adjacency updates.
   *
   * @param topology the updated protocol-specific topology
   */
  void updateTopology(T topology);

  /**
   * Execute one iteration of dataplane computation. Involves processing route updates from
   * neighbors and sending out any necessary routing updates.
   *
   * <p>Called exactly once per iteration (of route propagation)
   *
   * @param allNodes map of all available nodes that are participating in the computation.
   */
  void executeIteration(final Map<String, Node> allNodes);

  /**
   * Must return a {@link RibDelta} indicating which RIB updates need to be propagated to the main
   * RIB.
   *
   * <p>Called exactly once per iteration (of route propagation)
   */
  @Nonnull
  RibDelta<R> getUpdatesForMainRib();

  /**
   * Announce main RIB updates since the last iteration of the dataplane computation to the process.
   * The process may choose to redistribute these routes if configured to do so.
   *
   * <p><strong>Note:</strong> this may include updates generated by {@link #getUpdatesForMainRib()}
   *
   * <p>Called exactly once per iteration (of route propagation)
   *
   * @param mainRibDelta {@link RibDelta} containing updates to the main RIB.
   */
  void redistribute(RibDelta<? extends AbstractRouteDecorator> mainRibDelta);

  /**
   * Returns true if any outstanding computation remains (e.g., unprocessed messages or non-empty
   * {@link RibDelta deltas}
   */
  boolean isDirty();
}
