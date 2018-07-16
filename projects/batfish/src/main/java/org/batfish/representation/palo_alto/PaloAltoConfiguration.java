package org.batfish.representation.palo_alto;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.common.VendorConversionException;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Vrf;
import org.batfish.vendor.StructureUsage;
import org.batfish.vendor.VendorConfiguration;

public final class PaloAltoConfiguration extends VendorConfiguration {

  private static final long serialVersionUID = 1L;

  public static final String DEFAULT_VSYS_NAME = "vsys1";

  public static final String SHARED_VSYS_NAME = "~SHARED_VSYS~";

  private Configuration _c;

  private String _dnsServerPrimary;

  private String _dnsServerSecondary;

  private String _hostname;

  private final SortedMap<String, Interface> _interfaces;

  private String _ntpServerPrimary;

  private String _ntpServerSecondary;

  private transient Set<String> _unimplementedFeatures;

  private ConfigurationFormat _vendor;

  private final SortedMap<String, VirtualRouter> _virtualRouters;

  private final SortedMap<String, Vsys> _virtualSystems;

  public PaloAltoConfiguration(Set<String> unimplementedFeatures) {
    _interfaces = new TreeMap<>();
    _unimplementedFeatures = unimplementedFeatures;
    _virtualRouters = new TreeMap<>();
    _virtualSystems = new TreeMap<>();
  }

  private NavigableSet<String> getDnsServers() {
    NavigableSet<String> servers = new TreeSet<>();
    if (_dnsServerPrimary != null) {
      servers.add(_dnsServerPrimary);
    }
    if (_dnsServerSecondary != null) {
      servers.add(_dnsServerSecondary);
    }
    return servers;
  }

  @Override
  public String getHostname() {
    return _hostname;
  }

  public SortedMap<String, Interface> getInterfaces() {
    return _interfaces;
  }

  private NavigableSet<String> getNtpServers() {
    NavigableSet<String> servers = new TreeSet<>();
    if (_ntpServerPrimary != null) {
      servers.add(_ntpServerPrimary);
    }
    if (_ntpServerSecondary != null) {
      servers.add(_ntpServerSecondary);
    }
    return servers;
  }

  @Override
  public Set<String> getUnimplementedFeatures() {
    return _unimplementedFeatures;
  }

  public SortedMap<String, VirtualRouter> getVirtualRouters() {
    return _virtualRouters;
  }

  public SortedMap<String, Vsys> getVirtualSystems() {
    return _virtualSystems;
  }

  public void setDnsServerPrimary(String dnsServerPrimary) {
    _dnsServerPrimary = dnsServerPrimary;
  }

  public void setDnsServerSecondary(String dnsServerSecondary) {
    _dnsServerSecondary = dnsServerSecondary;
  }

  @Override
  public void setHostname(String hostname) {
    _hostname = hostname;
  }

  public void setNtpServerPrimary(String ntpServerPrimary) {
    _ntpServerPrimary = ntpServerPrimary;
  }

  public void setNtpServerSecondary(String ntpServerSecondary) {
    _ntpServerSecondary = ntpServerSecondary;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    _vendor = format;
  }

  // Visible for testing
  /**
   * Generate unique object name (no collision across vsys namespaces) given a vsys name and
   * original object name
   */
  public static String computeObjectName(String vsysName, String objectName) {
    return String.format("%s~%s", vsysName, objectName);
  }

  /**
   * Extract object name from a name with an embedded namespace. For example: nameWithNamespace
   * might be `vsys1~SERVICE1`, where `SERVICE1` is the object name extracted and returned.
   */
  private static String extractObjectName(String nameWithNamespace) {
    String[] parts = nameWithNamespace.split("~", -1);
    return parts[parts.length - 1];
  }

  // Visible for testing
  /**
   * Generate IpAccessList name for the specified serviceGroupMemberName in the specified vsysName
   */
  public static String computeServiceGroupMemberAclName(
      String vsysName, String serviceGroupMemberName) {
    return String.format("~%s~SERVICE_GROUP_MEMBER~%s~", vsysName, serviceGroupMemberName);
  }

  /** Convert vsys components to vendor independent model */
  private void convertVirtualSystems() {
    NavigableSet<String> loggingServers = new TreeSet<>();

    for (Vsys vsys : _virtualSystems.values()) {
      loggingServers.addAll(vsys.getSyslogServerAddresses());
      String vsysName = vsys.getName();

      // Zones
      for (Entry<String, Zone> zoneEntry : vsys.getZones().entrySet()) {
        Zone zone = zoneEntry.getValue();
        String zoneName = computeObjectName(vsysName, zone.getName());
        _c.getZones().put(zoneName, toZone(zoneName, zone));
      }

      // Services
      for (Service service : vsys.getServices().values()) {
        String serviceGroupAclName = computeServiceGroupMemberAclName(vsysName, service.getName());
        _c.getIpAccessLists()
            .put(serviceGroupAclName, service.toIpAccessList(LineAction.ACCEPT, this, vsys));
      }

      // Service groups
      for (ServiceGroup serviceGroup : vsys.getServiceGroups().values()) {
        String serviceGroupAclName =
            computeServiceGroupMemberAclName(vsysName, serviceGroup.getName());
        _c.getIpAccessLists()
            .put(serviceGroupAclName, serviceGroup.toIpAccessList(LineAction.ACCEPT, this, vsys));
      }
    }
    _c.setLoggingServers(loggingServers);
  }

  /** Convert Palo Alto specific interface into vendor independent model interface */
  private org.batfish.datamodel.Interface toInterface(Interface iface) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(name, _c, InterfaceType.PHYSICAL);
    Integer mtu = iface.getMtu();
    if (mtu != null) {
      newIface.setMtu(mtu);
    }
    newIface.setAddress(iface.getAddress());
    newIface.setAllAddresses(iface.getAllAddresses());
    newIface.setActive(iface.getActive());
    newIface.setDescription(iface.getComment());

    return newIface;
  }

  /** Convert Palo Alto specific virtual router into vendor independent model Vrf */
  private Vrf toVrf(VirtualRouter vr) {
    Vrf vrf = new Vrf(vr.getName());

    // Static routes
    for (Entry<String, StaticRoute> e : vr.getStaticRoutes().entrySet()) {
      StaticRoute sr = e.getValue();
      // Can only construct a static route if it has a destination
      if (sr.getDestination() != null) {
        vrf.getStaticRoutes()
            .add(
                org.batfish.datamodel.StaticRoute.builder()
                    .setNextHopInterface(sr.getNextHopInterface())
                    .setNextHopIp(sr.getNextHopIp())
                    .setAdministrativeCost(sr.getAdminDistance())
                    .setMetric(sr.getMetric())
                    .setNetwork(sr.getDestination())
                    .build());
      } else {
        _w.redFlag(
            String.format(
                "Cannot convert static route %s, as it does not have a destination.", e.getKey()));
      }
    }

    // Interfaces
    NavigableMap<String, org.batfish.datamodel.Interface> map = new TreeMap<>();
    for (String interfaceName : vr.getInterfaceNames()) {
      org.batfish.datamodel.Interface iface = _c.getInterfaces().get(interfaceName);
      if (iface != null) {
        map.put(interfaceName, iface);
      }
    }
    vrf.setInterfaces(map);

    return vrf;
  }

  /** Convert Palo Alto zone to vendor independent model zone */
  private org.batfish.datamodel.Zone toZone(String name, Zone zone) {
    org.batfish.datamodel.Zone newZone = new org.batfish.datamodel.Zone(name);
    newZone.setInterfaces(zone.getInterfaceNames());
    return newZone;
  }

  @Override
  public Configuration toVendorIndependentConfiguration() throws VendorConversionException {
    String hostname = getHostname();
    _c = new Configuration(hostname, _vendor);
    _c.setDefaultCrossZoneAction(LineAction.REJECT);
    _c.setDefaultInboundAction(LineAction.ACCEPT);
    _c.setDnsServers(getDnsServers());
    _c.setNtpServers(getNtpServers());

    for (Entry<String, Interface> i : _interfaces.entrySet()) {
      _c.getInterfaces().put(i.getKey(), toInterface(i.getValue()));
    }

    for (Entry<String, VirtualRouter> vr : _virtualRouters.entrySet()) {
      _c.getVrfs().put(vr.getKey(), toVrf(vr.getValue()));
    }

    // Handle converting items within virtual systems
    convertVirtualSystems();

    // Count and mark simple structure usages and identify undefined references
    markConcreteStructure(
        PaloAltoStructureType.INTERFACE,
        PaloAltoStructureUsage.VIRTUAL_ROUTER_INTERFACE,
        PaloAltoStructureUsage.ZONE_INTERFACE);

    // Handle marking for structures that may exist in one of a couple namespaces
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP,
        ImmutableList.of(PaloAltoStructureType.SERVICE, PaloAltoStructureType.SERVICE_GROUP),
        PaloAltoStructureUsage.SERVICE_GROUP_MEMBER);
    return _c;
  }

  /**
   * Helper method to return DefinedStructureInfo for the structure with the specified name that
   * could be any of the specified structureTypesToCheck, return null if no match is found
   */
  private @Nullable DefinedStructureInfo findDefinedStructure(
      String name, Collection<PaloAltoStructureType> structureTypesToCheck) {
    for (PaloAltoStructureType typeToCheck : structureTypesToCheck) {
      Map<String, DefinedStructureInfo> matchingDefinitions =
          _structureDefinitions.get(typeToCheck.getDescription());
      if (!matchingDefinitions.isEmpty()) {
        DefinedStructureInfo definition = matchingDefinitions.get(name);
        if (definition != null) {
          return definition;
        }
      }
    }
    return null;
  }

  /**
   * Update referrers and/or warn for undefined structures based on references to an abstract
   * structure type existing in either the reference's namespace or shared namespace
   */
  private void markAbstractStructureFromUnknownNamespace(
      PaloAltoStructureType type,
      Collection<PaloAltoStructureType> structureTypesToCheck,
      PaloAltoStructureUsage usage) {
    Map<String, SortedMap<StructureUsage, SortedMultiset<Integer>>> references =
        firstNonNull(_structureReferences.get(type), Collections.emptyMap());
    references.forEach(
        (nameWithNamespace, byUsage) -> {
          String name = extractObjectName(nameWithNamespace);
          Multiset<Integer> lines = firstNonNull(byUsage.get(usage), TreeMultiset.create());
          // Check this namespace first
          DefinedStructureInfo info =
              findDefinedStructure(nameWithNamespace, structureTypesToCheck);
          // Check shared namespace if there was no match
          if (info == null) {
            info =
                findDefinedStructure(
                    computeObjectName(SHARED_VSYS_NAME, name), structureTypesToCheck);
          }

          // Now update reference count if applicable
          if (info != null) {
            info.setNumReferrers(
                info.getNumReferrers() == DefinedStructureInfo.UNKNOWN_NUM_REFERRERS
                    ? DefinedStructureInfo.UNKNOWN_NUM_REFERRERS
                    : info.getNumReferrers() + lines.size());
          } else {
            for (int line : lines) {
              undefined(type, name, usage, line);
            }
          }
        });
  }
}
