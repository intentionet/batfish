package org.batfish.representation.cumulus_nclu;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A shared interfaces for the two Cumulus configuration types -- concatenated, nclu */
public interface CumulusNodeConfiguration {

  String LOOPBACK_INTERFACE_NAME = "lo";

  public static final Pattern PHYSICAL_INTERFACE_PATTERN =
      Pattern.compile("^(swp[0-9]+(s[0-9])?)|(eth[0-9]+)$");

  public static final Pattern VLAN_INTERFACE_PATTERN = Pattern.compile("^vlan([0-9]+)$");

  public static final Pattern VXLAN_INTERFACE_PATTERN = Pattern.compile("^vxlan([0-9]+)$");

  public static final Pattern SUBINTERFACE_PATTERN = Pattern.compile("^(.*)\\.([0-9]+)$");

  Map<String, IpCommunityList> getIpCommunityLists();

  Map<String, IpPrefixList> getIpPrefixLists();

  Map<String, RouteMap> getRouteMaps();

  BgpProcess getBgpProcess();

  /**
   * Returns the {@link OspfInterface} of the specified interface.
   *
   * <p>Returns Optional.empty if the interface does not exist or its OspfInterface is null.
   */
  Optional<OspfInterface> getOspfInterface(String ifaceName);

  /** Returns the vrf with the asked name. Returns null if the vrf does not exist */
  @Nullable
  Vrf getVrf(String vrfName);

  /** Returns all the vxlan Ids for this node */
  @Nonnull
  Map<String, Vxlan> getVxlans();

  /** Returns a map from interface names to clag settings, for interfaces with Clag settings */
  Map<String, InterfaceClagSettings> getClagSettings();

  OspfProcess getOspfProcess();

  /**
   * Returns the VRF for the specified access VLAN or null if the input was null or a VRF wasn't
   * configured
   */
  @Nullable
  String getVrfForVlan(@Nullable Integer bridgeAccessVlan);
}
