package org.batfish.question;

import static org.batfish.datamodel.acl.SourcesReferencedByIpAccessLists.SOURCE_ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.SourcesReferencedByIpAccessLists.referencedSources;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.specifier.FilterSpecifier;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.LocationSpecifier;
import org.batfish.specifier.LocationVisitor;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierContext;

/** Utilities for questions about {@link IpAccessList Filters}. */
public final class FilterQuestionUtils {
  private FilterQuestionUtils() {}

  /** Get filters specified by the given filter specifier. */
  public static Multimap<String, String> getSpecifiedFilters(
      SpecifierContext specifierContext,
      NodeSpecifier nodeSpecifier,
      FilterSpecifier filterSpecifier,
      boolean ignoreComposites) {
    Set<String> nodes = nodeSpecifier.resolve(specifierContext);
    ImmutableMultimap.Builder<String, String> filters = ImmutableMultimap.builder();
    Map<String, Configuration> configs = specifierContext.getConfigs();
    nodes.stream()
        .map(configs::get)
        .forEach(
            config ->
                filterSpecifier.resolve(config.getHostname(), specifierContext).stream()
                    .filter(f -> !ignoreComposites || !f.isComposite())
                    .forEach(filter -> filters.put(config.getHostname(), filter.getName())));
    return filters.build();
  }

  private static <T> T withDeltaSnapshot(IBatfish batfish, Supplier<T> supplier) {
    batfish.pushDeltaSnapshot();
    try {
      return supplier.get();
    } finally {
      batfish.popSnapshot();
    }
  }

  private static <T> T withBaseSnapshot(IBatfish batfish, Supplier<T> supplier) {
    batfish.pushDeltaSnapshot();
    try {
      return supplier.get();
    } finally {
      batfish.popSnapshot();
    }
  }

  /**
   * Instantiate a {@link BDDSourceManager} that tracks sources that are active and referenced on
   * both the current and reference version of a node. Further scope references to the input ACL,
   * and active sources to those specified by the input {@link LocationSpecifier}.
   */
  public static BDDSourceManager differentialBDDSourceManager(
      BDDPacket bddPacket,
      IBatfish batfish,
      String hostname,
      String aclName,
      LocationSpecifier startLocationSpecifier) {
    Configuration baseConfig =
        withBaseSnapshot(batfish, () -> batfish.loadConfigurations().get(hostname));

    Configuration deltaConfig =
        withDeltaSnapshot(batfish, () -> batfish.loadConfigurations().get(hostname));

    IpAccessList baseAcl = baseConfig.getIpAccessLists().get(aclName);
    IpAccessList deltaAcl = deltaConfig.getIpAccessLists().get(aclName);

    return differentialBDDSourceManager(
        bddPacket, batfish, baseConfig, deltaConfig, baseAcl, deltaAcl, startLocationSpecifier);
  }

  /**
   * Instantiate a {@link BDDSourceManager} that tracks sources that are active and referenced on
   * both the current and reference version of a node. Further scope references to the input ACL,
   * and active sources to those specified by the input {@link LocationSpecifier}.
   */
  public static BDDSourceManager differentialBDDSourceManager(
      BDDPacket bddPacket,
      IBatfish batfish,
      Configuration baseConfig,
      Configuration deltaConfig,
      IpAccessList baseAcl,
      IpAccessList deltaAcl,
      LocationSpecifier startLocationSpecifier) {
    String hostname = baseConfig.getHostname();

    // resolve specified source interfaces that exist in both configs.
    Set<String> commonSources =
        Sets.intersection(
            withBaseSnapshot(
                batfish,
                () -> resolveSources(batfish.specifierContext(), startLocationSpecifier, hostname)),
            withDeltaSnapshot(
                batfish,
                () ->
                    resolveSources(batfish.specifierContext(), startLocationSpecifier, hostname)));

    Set<String> inactiveInterfaces =
        Sets.union(
            Sets.difference(
                baseConfig.getAllInterfaces().keySet(), baseConfig.activeInterfaceNames()),
            Sets.difference(
                deltaConfig.getAllInterfaces().keySet(), deltaConfig.activeInterfaceNames()));

    // effectively active sources are those of interest that are active in both configs.
    Set<String> activeSources = Sets.difference(commonSources, inactiveInterfaces);
    Set<String> referencedSources =
        Sets.union(
            referencedSources(baseConfig.getIpAccessLists(), baseAcl),
            referencedSources(deltaConfig.getIpAccessLists(), deltaAcl));

    return BDDSourceManager.forSources(bddPacket, activeSources, referencedSources);
  }

  /** Return a concrete flow satisfying the input {@link BDD}, if one exists. */
  public static Optional<Flow> getFlow(
      BDDPacket pkt, BDDSourceManager bddSourceManager, String hostname, BDD bdd, String flowTag) {
    if (bdd.isZero()) {
      return Optional.empty();
    }
    BDD assignment = bdd.fullSatOne();
    return Optional.of(
        pkt.getFlowFromAssignment(assignment)
            .setTag(flowTag)
            .setIngressNode(hostname)
            .setIngressInterface(bddSourceManager.getSourceFromAssignment(assignment).orElse(null))
            .build());
  }

  /**
   * Resolve the set of filter sources (for {@link MatchSrcInterface} and {@link
   * org.batfish.datamodel.acl.OriginatingFromDevice} specified by the input {@link
   * LocationSpecifier}.
   */
  public static Set<String> resolveSources(
      SpecifierContext specifierContext, LocationSpecifier startLocationSpecifier, String node) {
    LocationVisitor<String> locationToSource =
        new LocationVisitor<String>() {
          @Override
          public String visitInterfaceLinkLocation(InterfaceLinkLocation interfaceLinkLocation) {
            return interfaceLinkLocation.getInterfaceName();
          }

          @Override
          public String visitInterfaceLocation(InterfaceLocation interfaceLocation) {
            return SOURCE_ORIGINATING_FROM_DEVICE;
          }
        };

    return startLocationSpecifier.resolve(specifierContext).stream()
        .filter(LocationVisitor.onNode(node)::visit)
        .map(locationToSource::visit)
        .collect(ImmutableSet.toImmutableSet());
  }
}
