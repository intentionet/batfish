package org.batfish.bddreachability;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.immutableEntry;
import static org.batfish.common.bdd.IpAccessListToBdd.toBdds;
import static org.batfish.common.util.CollectionUtil.toImmutableMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDFiniteDomain;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;

/**
 * Manages BDD variables to track potential outgoing interfaces that have {@link
 * Interface#getOutgoingOriginalFlowFilter() outgoing filters on the original flow}.
 *
 * <p>To minimize to the number of BDD variables used, we only include interfaces that are active
 * and have outgoing filters on the original flow, plus one representative value for active
 * interfaces without original flow filters if any exist.
 *
 * <p>We need these identifiers to constrain flow BDDs entering a node so that when flows eventually
 * exit a given egress interface, the BDD is already partitioned on which original flows were
 * permitted by that egress interface's {@link Interface#getOutgoingOriginalFlowFilter()
 * outgoingOriginalFlowFilter}.
 */
public final class BDDOutgoingOriginalFlowFilterManager {
  private static final String VAR_NAME = "OutgoingInterface";

  /**
   * A representative interface that is active but does not have an {@link
   * Interface#getOutgoingOriginalFlowFilter() outgoingOriginalFlowFilter}. This value (if not null)
   * will be included in the finite domain, but its corresponding BDD is not currently used.
   * Allocating this value merely enforces that the BDD for an interface with a
   * outgoingOriginalFlowFilter can't be ONE unless it is the only active interface on the node.
   *
   * <p>{@code null} when we don't need BDDs to distinguish between active interfaces with original
   * flow filters vs active interfaces without original flow filters. This is true when one or both
   * of those categories is empty.
   */
  private final @Nullable String _activeButNoOriginalFlowFilterRepresentative;

  private final BDDFiniteDomain<String> _finiteDomain;
  private final Map<String, BDD> _filterBdds;
  /**
   * BDD assignments for interfaces with {@link Interface#getOutgoingOriginalFlowFilter()
   * outgoingOriginalFlowFilters}. (Does not include entry for {@link
   * #_activeButNoOriginalFlowFilterRepresentative}.
   */
  private final Map<String, BDD> _interfaceBdds;

  /**
   * Indicates whether a flow will be permitted through an {@link
   * Interface#getOutgoingOriginalFlowFilter() outgoingOriginalFlowFilter}.
   */
  private final BDD _permitVar;

  private final BDD _falseBdd;
  private final BDD _trueBdd;
  private final Supplier<BDD> _outgoingOriginalFlowFiltersConstraint =
      Suppliers.memoize(this::computeOutgoingOriginalFlowFiltersConstraint);

  private BDDOutgoingOriginalFlowFilterManager(
      BDDFiniteDomain<String> finiteDomain,
      @Nullable String activeButNoOriginalFlowFilterRepresentative,
      Map<String, BDD> filterBdds,
      BDD permitVar) {
    _activeButNoOriginalFlowFilterRepresentative = activeButNoOriginalFlowFilterRepresentative;
    _falseBdd = finiteDomain.getIsValidConstraint().getFactory().zero();
    _trueBdd = finiteDomain.getIsValidConstraint().getFactory().one();
    _finiteDomain = finiteDomain;
    _filterBdds = filterBdds;
    _permitVar = permitVar;

    _interfaceBdds =
        _activeButNoOriginalFlowFilterRepresentative == null
            ? _finiteDomain.getValueBdds()
            : _finiteDomain.getValueBdds().entrySet().stream()
                .filter(e -> !e.getKey().equals(_activeButNoOriginalFlowFilterRepresentative))
                .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  private static BDD allocatePermitVar(BDDPacket pkt) {
    // Allocate permit var
    checkState(BDDPacket.FIRST_PACKET_VAR > 0, "Can't allocate permit BDD variable");
    return pkt.getFactory().ithVar(0);
  }

  /** Returns an empty {@link BDDOutgoingOriginalFlowFilterManager}. */
  public static BDDOutgoingOriginalFlowFilterManager empty(BDDPacket pkt) {
    return new BDDOutgoingOriginalFlowFilterManager(
        new BDDFiniteDomain<>(pkt, VAR_NAME, ImmutableSet.of()),
        null,
        ImmutableMap.of(),
        allocatePermitVar(pkt));
  }

  /**
   * Initialize a {@link BDDOutgoingOriginalFlowFilterManager} for each {@link Configuration} in a
   * network. A single variable is shared by all of them.
   */
  public static Map<String, BDDOutgoingOriginalFlowFilterManager> forNetwork(
      BDDPacket pkt, Map<String, Configuration> configs, Map<String, BDDSourceManager> srcMgrs) {
    // hostname -> set of interfaces that will be values for the config's finite domain
    ImmutableMap.Builder<String, Set<String>> finiteDomainValues = ImmutableMap.builder();

    // hostname -> a representative active interface without an outgoingOriginalFlowFilter (if any)
    Map<String, String> repActiveIfacesWithoutOrigFlowFilters = new HashMap<>();

    // hostname -> iface name -> BDD for flows permitted by interface's outgoingOriginalFlowFilter
    Map<String, Map<String, BDD>> filterBdds = new HashMap<>();

    for (Configuration c : configs.values()) {
      String hostname = c.getHostname();
      Set<String> activeWithFilters =
          c.getAllInterfaces().values().stream()
              .filter(iface -> iface.getActive() && iface.getOutgoingOriginalFlowFilter() != null)
              .map(Interface::getName)
              .collect(ImmutableSet.toImmutableSet());

      // Find a representative active interface without an outgoingOriginalFlowFilter. Should be
      // nonnull iff there are active interfaces both with and without outgoingOriginalFlowFilters.
      String repActiveIfaceWithoutFilter =
          activeWithFilters.isEmpty()
              ? null
              : c.getActiveInterfaces().keySet().stream()
                  .filter(iface -> !activeWithFilters.contains(iface))
                  .findAny()
                  .orElse(null);

      if (repActiveIfaceWithoutFilter != null) {
        repActiveIfacesWithoutOrigFlowFilters.put(hostname, repActiveIfaceWithoutFilter);
        finiteDomainValues.put(
            hostname,
            ImmutableSet.<String>builder()
                .addAll(activeWithFilters)
                .add(repActiveIfaceWithoutFilter)
                .build());
      } else {
        finiteDomainValues.put(hostname, activeWithFilters);
      }

      filterBdds.put(hostname, buildFilterBdds(pkt, c, srcMgrs.get(hostname)));
    }

    Map<String, BDDFiniteDomain<String>> finiteDomains =
        BDDFiniteDomain.domainsWithSharedVariable(pkt, VAR_NAME, finiteDomainValues.build());

    // Allocate permit var
    BDD permitVar = allocatePermitVar(pkt);

    return toImmutableMap(
        configs.keySet(),
        Function.identity(),
        hostname ->
            new BDDOutgoingOriginalFlowFilterManager(
                finiteDomains.get(hostname),
                repActiveIfacesWithoutOrigFlowFilters.get(hostname),
                filterBdds.get(hostname),
                permitVar));
  }

  /**
   * Builds a map of all active interfaces with {@link Interface#getOutgoingOriginalFlowFilter()
   * outgoingOriginalFlowFilters} to the permit BDD for each interface's outgoingOriginalFlowFilter.
   */
  private static Map<String, BDD> buildFilterBdds(
      BDDPacket pkt, Configuration c, BDDSourceManager srcMgr) {
    // Map of interface name -> outgoingOriginalFlowFilter for that interface.
    // Only includes active interfaces with outgoing original flow filters.
    Map<String, IpAccessList> origFlowFilters =
        c.getActiveInterfaces().values().stream()
            .map(iface -> immutableEntry(iface.getName(), iface.getOutgoingOriginalFlowFilter()))
            .filter(e -> e.getValue() != null)
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    // Map of filter name -> permit BDD for filter
    Map<String, BDD> filterBddsByAclName =
        toBdds(pkt, origFlowFilters.values(), c.getIpAccessLists(), c.getIpSpaces(), srcMgr);

    return toImmutableMap(
        origFlowFilters, Entry::getKey, e -> filterBddsByAclName.get(e.getValue().getName()));
  }

  /**
   * Returns the finite domain's BDD assignments for interfaces with {@link
   * Interface#getOutgoingOriginalFlowFilter() outgoingOriginalFlowFilters}. (Does not include entry
   * for {@code _activeButNoOriginalFlowFilterRepresentative} even if present in finite domain.)
   */
  @VisibleForTesting
  @Nonnull
  Map<String, BDD> getInterfaceBDDs() {
    return _interfaceBdds;
  }

  /**
   * Returns the BDD representing the constraint indicating the set of flows to be evaluated against
   * the outgoing filter on the given interface.
   *
   * <p>If there is no outging filter on the given interface, returns the constraint corresponding
   * to the set of flows that were not evaluated against any interface's outgoing filter.
   */
  public @Nonnull BDD outgoingInterfaceBDD(String iface) {
    if (isTrivial()) {
      return _trueBdd;
    }
    if (_activeButNoOriginalFlowFilterRepresentative == null) {
      BDD ret = _interfaceBdds.get(iface);
      assert ret != null;
      return ret;
    }
    return _interfaceBdds.getOrDefault(
        iface, _interfaceBdds.get(_activeButNoOriginalFlowFilterRepresentative));
  }

  /**
   * Returns a BDD to track which original flows would be permitted by which interfaces' outgoing
   * original flow filters. This will not eliminate any flows when applied to the original flows
   * BDD, but if the BDD is later constrained to a specific egress interface, this constraint will
   * cause {@link #_permitVar} to be true iff the original flow was permitted by that interface's
   * outgoingOriginalFlowFilter.
   *
   * <p>Always use {@link
   * org.batfish.bddreachability.transition.Transitions#addOutgoingOriginalFlowFiltersConstraint(BDDOutgoingOriginalFlowFilterManager)
   * addOutgoingOriginalFlowFiltersConstraint} to apply this constraint to edges; otherwise
   * backwards transition will not erase vars.
   */
  public BDD outgoingOriginalFlowFiltersConstraint() {
    return _outgoingOriginalFlowFiltersConstraint.get();
  }

  private BDD computeOutgoingOriginalFlowFiltersConstraint() {
    return getInterfaceBDDs().keySet().stream()
        .map(this::constraintForIface)
        .reduce(BDD::and)
        .orElse(_trueBdd); // only resorts to this if finite domain is empty
  }

  private BDD constraintForIface(String iface) {
    BDD ifaceBdd = getInterfaceBDDs().get(iface);
    if (ifaceBdd == null) {
      // this interface doesn't have an outgoingOriginalFlowFilter; no constraint to add
      return _trueBdd;
    }
    BDD permittedByFilter = _filterBdds.get(iface);
    return ifaceBdd.imp(permittedByFilter.biimp(_permitVar));
  }

  /** Existentially quantify the source variable. */
  public BDD erase(BDD bdd) {
    return _finiteDomain.existsValue(bdd).exist(_permitVar);
  }

  /**
   * BDD for having been permitted out the given interface's outgoingOriginalFlowFilter (no header
   * space constraints). If the interface has no outgoingOriginalFlowFilter, returns ONE.
   */
  public BDD permittedByOriginalFlowEgressFilter(String iface) {
    BDD ifaceBdd = getInterfaceBDDs().get(iface);
    return ifaceBdd == null ? _trueBdd : ifaceBdd.and(_permitVar);
  }

  /**
   * BDD for having been denied out the given interface's outgoingOriginalFlowFilter (no header
   * space constraints). If the interface has no outgoingOriginalFlowFilter, returns ZERO.
   */
  public BDD deniedByOriginalFlowEgressFilter(String iface) {
    BDD ifaceBdd = getInterfaceBDDs().get(iface);
    return ifaceBdd == null ? _falseBdd : ifaceBdd.and(_permitVar.not());
  }

  /**
   * @return true when there is nothing to track. This happens when no interface on the node has an
   *     {@link Interface#getOutgoingOriginalFlowFilter() outgoingOriginalFlowFilter}.
   */
  public boolean isTrivial() {
    return _finiteDomain.isEmpty();
  }
}
