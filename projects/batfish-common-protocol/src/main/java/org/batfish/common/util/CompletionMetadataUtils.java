package org.batfish.common.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.questions.NamedStructureSpecifier;

/** Various functions useful for fetching data used in the creation of CompletionMetadata */
public final class CompletionMetadataUtils {

  private CompletionMetadataUtils() {}

  @VisibleForTesting
  public static Set<String> getFilterNames(Map<String, Configuration> configurations) {
    ImmutableSet.Builder<String> filterNames = ImmutableSet.builder();
    configurations
        .values()
        .forEach(configuration -> filterNames.addAll(configuration.getIpAccessLists().keySet()));
    return filterNames.build();
  }

  @VisibleForTesting
  public static Set<NodeInterfacePair> getInterfaces(Map<String, Configuration> configurations) {
    ImmutableSet.Builder<NodeInterfacePair> interfaces = ImmutableSet.builder();
    configurations
        .values()
        .forEach(
            configuration ->
                configuration.getAllInterfaces().values().stream()
                    .map(NodeInterfacePair::new)
                    .forEach(interfaces::add));
    return interfaces.build();
  }

  @VisibleForTesting
  public static Set<String> getIps(Map<String, Configuration> configurations) {
    ImmutableSet.Builder<String> ips = ImmutableSet.builder();
    configurations
        .values()
        .forEach(
            configuration -> {
              configuration
                  .getAllInterfaces()
                  .values()
                  .forEach(
                      iface ->
                          iface.getAllAddresses().stream()
                              .map(interfaceAddress -> interfaceAddress.getIp().toString())
                              .forEach(ips::add));
              configuration
                  .getGeneratedReferenceBooks()
                  .values()
                  .forEach(
                      book ->
                          book.getAddressGroups()
                              .forEach(ag -> ag.getAddresses().forEach(ips::add)));
            });

    return ips.build();
  }

  @VisibleForTesting
  public static Set<String> getNodes(Map<String, Configuration> configurations) {
    return ImmutableSet.copyOf(configurations.keySet());
  }

  @VisibleForTesting
  public static Set<String> getPrefixes(Map<String, Configuration> configurations) {
    ImmutableSet.Builder<String> prefixes = ImmutableSet.builder();
    configurations
        .values()
        .forEach(
            configuration ->
                configuration
                    .getAllInterfaces()
                    .values()
                    .forEach(
                        iface ->
                            iface.getAllAddresses().stream()
                                .map(interfaceAddress -> interfaceAddress.getPrefix().toString())
                                .forEach(prefixes::add)));
    return prefixes.build();
  }

  @VisibleForTesting
  public static Set<String> getRoutingPolicyNames(Map<String, Configuration> configurations) {
    ImmutableSet.Builder<String> routingPolicyNames = ImmutableSet.builder();
    configurations
        .values()
        .forEach(
            configuration ->
                routingPolicyNames.addAll(configuration.getRoutingPolicies().keySet()));
    return routingPolicyNames.build();
  }

  @VisibleForTesting
  public static Set<String> getStructureNames(Map<String, Configuration> configurations) {
    ImmutableSet.Builder<String> structureNames = ImmutableSet.builder();
    configurations
        .values()
        .forEach(
            configuration ->
                NamedStructureSpecifier.JAVA_MAP
                    .values()
                    .forEach(
                        type -> {
                          // fetch names of all defined structures of given type and configuration
                          Object namedStructuresMap = type.getGetter().apply(configuration);
                          // should be an instance of a Map
                          if (namedStructuresMap instanceof Map<?, ?>) {
                            ((Map<?, ?>) namedStructuresMap)
                                .keySet()
                                .forEach(
                                    key -> {
                                      if (key instanceof String) {
                                        structureNames.add((String) key);
                                      }
                                    });
                          }
                        }));
    return structureNames.build();
  }

  @VisibleForTesting
  public static Set<String> getVrfs(Map<String, Configuration> configurations) {
    ImmutableSet.Builder<String> vrfs = ImmutableSet.builder();
    configurations
        .values()
        .forEach(
            configuration ->
                configuration.getAllInterfaces().values().stream()
                    .map(Interface::getVrfName)
                    .forEach(vrfs::add));
    return vrfs.build();
  }

  @VisibleForTesting
  public static Set<String> getZones(Map<String, Configuration> configurations) {
    ImmutableSet.Builder<String> zones = ImmutableSet.builder();
    configurations
        .values()
        .forEach(configuration -> zones.addAll(configuration.getZones().keySet()));
    return zones.build();
  }
}
