package org.batfish.common.bdd;

import static com.google.common.base.Preconditions.checkState;
import static org.batfish.common.util.CollectionUtil.toImmutableMap;
import static org.batfish.datamodel.acl.SourcesReferencedByIpAccessLists.SOURCE_ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.SourcesReferencedByIpAccessLists.referencedSources;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.OriginatingFromDevice;

/**
 * Manages BDD variables to track a packet's source: which interface it entered the node through, if
 * any. If it didn't enter through any node, it originated from the device itself.
 *
 * <p>To minize to the number of BDD variables used, we consider which sources are active and which
 * are referenced by ACLs on the node. All inactive sources are assigned the zero BDD. If a source
 * is active and referenced, then we assign it a unique identifier. All active but unreferenced
 * sources share a single identifier. We allocate enough BDD variables to distinguish between the
 * identifiers.
 *
 * <p>We need these identifiers in two contexts: to record the source of a packet as it enters a
 * node, and to check the source of a packet when an ACL includes a {@link
 * org.batfish.datamodel.acl.MatchSrcInterface} or {@link OriginatingFromDevice} expression.
 */
public final class BDDSourceManager {
  private static final String VAR_NAME = "PacketSource";

  /**
   * A source (an interface or the device itself) is "active but unreferenced" if it is active but
   * not reference by {@link MatchSrcInterface} or {@link OriginatingFromDevice}. For efficiency,
   * the finite domain doesn't track each active but unreferenced source. Instead, it tracks a
   * single representative value that represents all of them.
   *
   * <p>{@code null} when we don't need BDDs to distinguish between active and referenced sources vs
   * active but unreferenced sources. This is true when either (1) there are no active and
   * referenced sources, or (2) there are no active but unreferenced sources.
   */
  private final @Nullable String _activeButUnreferencedRepresentative;

  private final Set<String> _activeButUnreferenced;
  private final BDD _falseBDD;
  private final BDDFiniteDomain<String> _finiteDomain;
  private final BDD _trueBDD;

  private BDDSourceManager(
      BDDFiniteDomain<String> finiteDomain, Set<String> activeButUnreferenced) {
    _activeButUnreferenced = ImmutableSet.copyOf(activeButUnreferenced);
    _activeButUnreferencedRepresentative =
        finiteDomain.isEmpty()
            ? null
            : _activeButUnreferenced.stream().sorted().findFirst().orElse(null);
    _falseBDD = finiteDomain.getIsValidConstraint().getFactory().zero();
    _trueBDD = finiteDomain.getIsValidConstraint().getFactory().one();
    _finiteDomain = finiteDomain;
  }

  /** Allocates a variable for the sources and initializes SANE. */
  private static BDDSourceManager forSourcesInternal(
      BDDPacket pkt, Set<String> activeAndReferenced, Set<String> activeButNotReferenced) {
    return new BDDSourceManager(
        new BDDFiniteDomain<>(
            pkt, VAR_NAME, valuesToTrack(activeAndReferenced, activeButNotReferenced)),
        activeButNotReferenced);
  }

  /**
   * Create a {@link BDDSourceManager} that tracks the specified interfaces as sources, plus the
   * device itself.
   */
  public static BDDSourceManager forInterfaces(BDDPacket pkt, Set<String> interfaces) {
    Set<String> sources =
        ImmutableSet.<String>builder()
            .addAll(interfaces)
            .add(SOURCE_ORIGINATING_FROM_DEVICE)
            .build();
    return forSourcesInternal(pkt, sources, ImmutableSet.of());
  }

  /**
   * Create a {@link BDDSourceManager} that tracks the specified interfaces as sources, plus the
   * device itself.
   */
  public static BDDSourceManager forInterfaces(BDDInteger var, Set<String> interfaces) {
    Set<String> sources =
        ImmutableSet.<String>builder()
            .addAll(interfaces)
            .add(SOURCE_ORIGINATING_FROM_DEVICE)
            .build();
    return new BDDSourceManager(new BDDFiniteDomain<>(var, sources), ImmutableSet.of());
  }

  /** Create a {@link BDDSourceManager} for the empty set of sources. */
  public static BDDSourceManager empty(BDDPacket pkt) {
    return forSourcesInternal(pkt, ImmutableSet.of(), ImmutableSet.of());
  }

  public static BDDSourceManager forIpAccessList(
      BDDPacket pkt, Configuration config, IpAccessList acl) {
    return forIpAccessList(
        pkt,
        Sets.union(ImmutableSet.of(SOURCE_ORIGINATING_FROM_DEVICE), config.activeInterfaceNames()),
        config.getIpAccessLists(),
        acl);
  }

  public static BDDSourceManager forSources(
      BDDPacket pkt, Set<String> activeSources, Set<String> referencedSources) {
    return forSourcesInternal(
        pkt,
        Sets.intersection(activeSources, referencedSources),
        Sets.difference(activeSources, referencedSources));
  }

  /**
   * Initialize a {@link BDDSourceManager} for each {@link Configuration} in a network. A single
   * variable is shared by all of them.
   */
  public static Map<String, BDDSourceManager> forNetwork(
      BDDPacket pkt, Map<String, Configuration> configs) {
    return forNetwork(pkt, configs, false);
  }

  /**
   * Initialize a {@link BDDSourceManager} for each {@link Configuration} in a network. A single
   * variable is shared by all of them.
   *
   * @param initializeSessions When true, nodes that might initialize sessions (i.e. one of their
   *     active interfaces has a {@link FirewallSessionInterfaceInfo} object) will track all
   *     interfaces, regardless of whether they are referenced by an ACL.
   */
  public static Map<String, BDDSourceManager> forNetwork(
      BDDPacket pkt, Map<String, Configuration> configs, boolean initializeSessions) {
    Map<String, Set<String>> activeSources =
        toImmutableMap(
            configs.entrySet(),
            Entry::getKey,
            entry ->
                ImmutableSet.<String>builder()
                    .add(SOURCE_ORIGINATING_FROM_DEVICE)
                    .addAll(entry.getValue().activeInterfaceNames())
                    .build());

    Map<String, Set<String>> referencedSources =
        toImmutableMap(
            configs.entrySet(),
            Entry::getKey,
            entry -> {
              Configuration config = entry.getValue();
              Set<String> active = activeSources.get(config.getHostname());

              if (initializeSessions
                  && config.getAllInterfaces().values().stream()
                      .filter(Interface::getActive)
                      .anyMatch(iface -> iface.getFirewallSessionInterfaceInfo() != null)) {
                // This node may initialize sessions -- have to track all active sources
                return active;
              }

              Set<String> sources = new HashSet<>();
              for (IpAccessList acl : config.getIpAccessLists().values()) {
                referencedSources(config.getIpAccessLists(), acl).stream()
                    .filter(active::contains)
                    .forEach(sources::add);
              }
              return sources;
            });

    Map<String, Set<String>> activeAndReferenced =
        toImmutableMap(
            activeSources,
            Entry::getKey,
            activeEntry ->
                Sets.intersection(
                    activeEntry.getValue(), referencedSources.get(activeEntry.getKey())));

    Map<String, Set<String>> activeButNotReferenced =
        toImmutableMap(
            activeSources,
            Entry::getKey,
            activeEntry ->
                Sets.difference(
                    activeEntry.getValue(), referencedSources.get(activeEntry.getKey())));

    Map<String, Set<String>> valuesToTrack =
        toImmutableMap(
            activeAndReferenced,
            Entry::getKey,
            entry -> {
              String hostname = entry.getKey();
              Set<String> hostActiveAndReferenced = entry.getValue();
              Set<String> hostActiveButNotReferenced = activeButNotReferenced.get(hostname);
              return valuesToTrack(hostActiveAndReferenced, hostActiveButNotReferenced);
            });

    Map<String, BDDFiniteDomain<String>> finiteDomains =
        BDDFiniteDomain.domainsWithSharedVariable(pkt, VAR_NAME, valuesToTrack);

    return toImmutableMap(
        finiteDomains,
        Entry::getKey,
        entry ->
            new BDDSourceManager(entry.getValue(), activeButNotReferenced.get(entry.getKey())));
  }

  /**
   * Create a {@link BDDSourceManager} for a specified {@link IpAccessList}. To minimize the number
   * of {@link BDD} bits used, it will only track interfaces referenced by the ACL.
   */
  public static BDDSourceManager forIpAccessList(
      BDDPacket pkt,
      Set<String> activeInterfaces,
      Map<String, IpAccessList> namedAcls,
      IpAccessList acl) {
    Set<String> referencedSources = referencedSources(namedAcls, acl);
    return forSources(pkt, activeInterfaces, referencedSources);
  }

  /**
   * A packet can enter a router from at most 1 interface, possibly none (if the packet originated
   * at the router). So for N srcInterfaces, we need N+1 distinct values (0 through N): one for each
   * interface, and one for "none of them". We use the last value (N) for "none of them".
   */
  private static Set<String> valuesToTrack(
      Set<String> activeAndReferenced, Set<String> activeButUnreferenced) {
    if (activeAndReferenced.isEmpty()) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    builder.addAll(activeAndReferenced);

    // use the min active but unreferenced source to represent any/all of them
    if (!activeButUnreferenced.isEmpty()) {
      builder.add(activeButUnreferenced.stream().min(Comparator.naturalOrder()).get());
    }

    return builder.build();
  }

  public BDDFiniteDomain<String> getFiniteDomain() {
    return _finiteDomain;
  }

  public BDD getOriginatingFromDeviceBDD() {
    return getSourceBDD(SOURCE_ORIGINATING_FROM_DEVICE);
  }

  private BDD getSourceBDD(String source) {
    if (_activeButUnreferenced.contains(source)) {
      return _activeButUnreferencedRepresentative == null
          ? _trueBDD
          : _finiteDomain.getConstraintForValue(_activeButUnreferencedRepresentative);
    } else {
      // default to false for inactive sources, which are not tracked
      return _finiteDomain.getValueBdds().getOrDefault(source, _falseBDD);
    }
  }

  public BDD getSourceInterfaceBDD(String iface) {
    return getSourceBDD(iface);
  }

  public Map<String, BDD> getSourceBDDs() {
    ImmutableMap.Builder<String, BDD> builder = ImmutableMap.builder();
    builder.putAll(_finiteDomain.getValueBdds());

    if (!_activeButUnreferenced.isEmpty()) {
      checkState(
          isTrivial() || _activeButUnreferencedRepresentative != null,
          "nontrivial source manager with active-but-unreferenced sources must have a representative");

      BDD representativeBdd =
          _activeButUnreferencedRepresentative == null
              ? _trueBDD
              : _finiteDomain.getConstraintForValue(_activeButUnreferencedRepresentative);
      _activeButUnreferenced.stream()
          .filter(src -> !src.equals(_activeButUnreferencedRepresentative))
          .forEach(src -> builder.put(src, representativeBdd));
    }

    return builder.build();
  }

  /**
   * @param bdd An assignment (i.e. only 1 possible value for each variable mentioned).
   * @return The interface of identified by the assigned value, or none if the device itself is the
   *     source.
   */
  public Optional<String> getSourceFromAssignment(BDD bdd) {
    if (_finiteDomain.isEmpty() && _activeButUnreferencedRepresentative == null) {
      // No sources
      return Optional.empty();
    }
    String value =
        _finiteDomain.isEmpty()
            ? _activeButUnreferencedRepresentative
            : _finiteDomain.getValueFromAssignment(bdd);
    return value.equals(SOURCE_ORIGINATING_FROM_DEVICE) ? Optional.empty() : Optional.of(value);
  }

  /**
   * @return true when there is nothing to track. This can happen when no {@link IpAccessList ACL}
   *     on the node uses {@link org.batfish.datamodel.acl.MatchSrcInterface} or {@link
   *     OriginatingFromDevice}.
   */
  public boolean isTrivial() {
    return _finiteDomain.isEmpty();
  }

  /**
   * @return A constraint that the source variable is assigned one of the valid values for this
   *     device.
   */
  public BDD isValidValue() {
    return _finiteDomain.getIsValidConstraint();
  }

  /** Existentially quantify the source variable. */
  public BDD existsSource(BDD bdd) {
    return _finiteDomain.existsValue(bdd);
  }

  /**
   * Test if a {@link BDD} includes the constraint that the source variable has a valid value (or a
   * stronger one that implies it).
   */
  public boolean hasIsValidConstraint(BDD bdd) {
    return bdd.and(_finiteDomain.getIsValidConstraint()).equals(bdd);
  }

  /** Test if a {@link BDD} includes a constraint on the source variable. */
  public boolean hasSourceConstraint(BDD bdd) {
    return !bdd.equals(_finiteDomain.existsValue(bdd));
  }

  /**
   * Return whether each source is tracked separately (i.e. each has its own value in the domain).
   */
  public boolean allSourcesTracked() {
    return !isTrivial() && _activeButUnreferenced.isEmpty();
  }
}
