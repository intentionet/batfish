package org.batfish.representation.aws;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.datamodel.Configuration;

/** Represents vendor independent configuration data after conversion of native AWS data. */
@ParametersAreNonnullByDefault
class ConvertedConfiguration implements Serializable {

  @Nonnull private final Map<String, Configuration> _configurationNodes;
  @Nonnull private final Set<Layer1Edge> _layer1Edges;
  /**
   * Multimap of subnet IDs to {@link Instance} in that subnet used as targets by some {@link
   * LoadBalancer}
   */
  @Nonnull private final Multimap<String, Instance> _subnetsToInstanceTargets;

  /**
   * Multimap of subnet IDs to {@link LoadBalancer} connected to that subnet that have active
   * instance targets
   */
  @Nonnull private final Multimap<String, LoadBalancer> _subnetsToNlbs;

  /** Multimap of load balancer ARNs to {@link Instance} used as targets for that load balancer */
  @Nonnull private final Multimap<String, Instance> _nlbsToInstanceTargets;

  public ConvertedConfiguration(AwsConfiguration awsConfiguration) {
    this(
        new HashMap<>(),
        new HashSet<>(),
        awsConfiguration.getSubnetsToInstanceTargets(),
        awsConfiguration.getSubnetsToNlbs(),
        awsConfiguration.getNlbsToInstanceTargets());
  }

  @VisibleForTesting
  public ConvertedConfiguration() {
    this(
        new HashMap<>(),
        new HashSet<>(),
        ImmutableMultimap.of(),
        ImmutableMultimap.of(),
        ImmutableMultimap.of());
  }

  @VisibleForTesting
  ConvertedConfiguration(Map<String, Configuration> configurationNodes) {
    this(
        configurationNodes,
        new HashSet<>(),
        ImmutableMultimap.of(),
        ImmutableMultimap.of(),
        ImmutableMultimap.of());
  }

  @VisibleForTesting
  ConvertedConfiguration(
      Map<String, Configuration> configurationNodes,
      Set<Layer1Edge> layer1Edges,
      Multimap<String, Instance> subnetsToInstanceTargets,
      Multimap<String, LoadBalancer> subnetsToNlbs,
      Multimap<String, Instance> nlbsToInstanceTargets) {
    _configurationNodes = configurationNodes;
    _layer1Edges = layer1Edges;
    _subnetsToInstanceTargets = ImmutableMultimap.copyOf(subnetsToInstanceTargets);
    _subnetsToNlbs = ImmutableMultimap.copyOf(subnetsToNlbs);
    _nlbsToInstanceTargets = ImmutableMultimap.copyOf(nlbsToInstanceTargets);
  }

  /** Return configuration nodes across all accounts */
  public Collection<Configuration> getAllNodes() {
    return _configurationNodes.values();
  }

  @Nullable
  public Configuration getNode(String hostname) {
    return _configurationNodes.get(hostname);
  }

  @Nonnull
  public Set<Layer1Edge> getLayer1Edges() {
    return _layer1Edges;
  }

  void addNode(Configuration cfgNode) {
    _configurationNodes.put(cfgNode.getHostname(), cfgNode);
  }

  void addEdge(String nodeName1, String ifaceName1, String nodeName2, String ifaceName2) {
    _layer1Edges.add(new Layer1Edge(nodeName1, ifaceName1, nodeName2, ifaceName2));
  }

  /** Subnet IDs to {@link Instance} in that subnet used as targets by some {@link LoadBalancer} */
  @VisibleForTesting
  @Nonnull
  Multimap<String, Instance> getSubnetsToInstanceTargets() {
    return _subnetsToInstanceTargets;
  }

  /**
   * Subnet IDs to {@link LoadBalancer} connected to that subnet that have active instance targets
   */
  @VisibleForTesting
  @Nonnull
  Multimap<String, LoadBalancer> getSubnetsToNlbs() {
    return _subnetsToNlbs;
  }

  /** Load balancer ARNs to {@link Instance} used as targets for that load balancer */
  @VisibleForTesting
  @Nonnull
  Multimap<String, Instance> getNlbsToInstanceTargets() {
    return _nlbsToInstanceTargets;
  }
}
