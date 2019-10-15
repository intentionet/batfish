package org.batfish.representation.palo_alto;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class Vsys implements Serializable {

  public enum NamespaceType {
    /** vsys or shared-gateway */
    LEAF,
    PANORAMA,
    SHARED
  }

  private final SortedMap<String, AddressGroup> _addressGroups;

  private final SortedMap<String, AddressObject> _addressObjects;

  private final SortedMap<String, Application> _applications;

  private final SortedMap<String, ApplicationGroup> _applicationGroups;

  private String _displayName;

  private final Set<String> _importedInterfaces;

  private final Set<String> _importedVsyses;

  private final String _name;

  @Nonnull private final Rulebase _rulebase;
  // Panorama only: rules to prepend to every rulebase
  @Nonnull private final Rulebase _preRulebase;
  // Panorama only: rules to append to every rulebase
  @Nonnull private final Rulebase _postRulebase;

  private final SortedMap<String, Service> _services;

  private final SortedMap<String, ServiceGroup> _serviceGroups;

  private final SortedMap<String, SortedMap<String, SyslogServer>> _syslogServerGroups;

  private final SortedMap<String, Tag> _tags;

  private final SortedMap<String, Zone> _zones;

  private final @Nonnull NamespaceType _namespaceType;

  /**
   * Construct a {@link Vsys} with provided {@code name} and namespace type {@link
   * NamespaceType#LEAF}.
   */
  public Vsys(String name) {
    this(name, NamespaceType.LEAF);
  }

  /** Construct a {@link Vsys} with provided {@code name} and {@code namespaceType}. */
  public Vsys(String name, NamespaceType namespaceType) {
    _name = name;
    _namespaceType = namespaceType;
    _addressGroups = new TreeMap<>();
    _addressObjects = new TreeMap<>();
    _applications = new TreeMap<>();
    _applicationGroups = new TreeMap<>();
    _importedInterfaces = new HashSet<>();
    _importedVsyses = new HashSet<>();
    _rulebase = new Rulebase();
    _preRulebase = new Rulebase();
    _postRulebase = new Rulebase();
    _services = new TreeMap<>();
    _serviceGroups = new TreeMap<>();
    _syslogServerGroups = new TreeMap<>();
    _tags = new TreeMap<>();
    _zones = new TreeMap<>();
  }

  /** Returns a map of address group name to {@link AddressGroup} object */
  public SortedMap<String, AddressGroup> getAddressGroups() {
    return _addressGroups;
  }

  /** Returns a map of address object name to {@link AddressObject} object */
  public SortedMap<String, AddressObject> getAddressObjects() {
    return _addressObjects;
  }

  /** Returns a map of application name to {@link Application} object */
  public SortedMap<String, Application> getApplications() {
    return _applications;
  }

  /** Returns a map of application group name to {@link ApplicationGroup}. */
  public SortedMap<String, ApplicationGroup> getApplicationGroups() {
    return _applicationGroups;
  }

  /** Returns the display name for this vsys. */
  public @Nullable String getDisplayName() {
    return _displayName;
  }

  /** Returns the interfaces imported for this vsys. */
  public Set<String> getImportedInterfaces() {
    return _importedInterfaces;
  }

  /** Returns the sibling vsyses imported for this vsys. */
  public Set<String> getImportedVsyses() {
    return _importedVsyses;
  }

  /** Returns the name of this vsys. */
  public String getName() {
    return _name;
  }

  /** Returns a {@link Rulebase} of the rules in this vsys. */
  @Nonnull
  public Rulebase getRulebase() {
    return _rulebase;
  }

  /** Returns a {@link Rulebase} of the pre-rulebase for this vsys. */
  @Nonnull
  public Rulebase getPreRulebase() {
    return _preRulebase;
  }

  /** Returns a {@link Rulebase} of the post-rulebase for this vsys. */
  @Nonnull
  public Rulebase getPostRulebase() {
    return _postRulebase;
  }

  /** Returns a map of service name to service for the services in this vsys. */
  public SortedMap<String, Service> getServices() {
    return _services;
  }

  /** Returns a map of service group name to serviceGroup for the service groups in this vsys. */
  public SortedMap<String, ServiceGroup> getServiceGroups() {
    return _serviceGroups;
  }

  /**
   * Returns a syslog server with the specified name in the specified server group. If a matching
   * server does not exist, one is created.
   */
  public SyslogServer getSyslogServer(String serverGroupName, String serverName) {
    SortedMap<String, SyslogServer> serverGroup =
        _syslogServerGroups.computeIfAbsent(serverGroupName, g -> new TreeMap<>());
    return serverGroup.computeIfAbsent(serverName, SyslogServer::new);
  }

  /** Returns a list of all syslog server addresses. */
  SortedSet<String> getSyslogServerAddresses() {
    SortedSet<String> servers = new TreeSet<>();
    _syslogServerGroups
        .values()
        .forEach(
            g ->
                g.values()
                    .forEach(
                        s -> {
                          if (s.getAddress() != null) {
                            servers.add(s.getAddress());
                          }
                        }));
    return servers;
  }

  /** Returns a map of tag name to tag for tags in this vsys. */
  public SortedMap<String, Tag> getTags() {
    return _tags;
  }

  /** Returns a map of zone name to zone for the zones in this vsys. */
  public SortedMap<String, Zone> getZones() {
    return _zones;
  }

  /** Sets the display name for this vsys. */
  public void setDisplayName(String displayName) {
    _displayName = displayName;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(Vsys.class).add("name", _name).toString();
  }

  public @Nonnull NamespaceType getNamespaceType() {
    return _namespaceType;
  }
}
