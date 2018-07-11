package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasAccessVlan;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasAdditionalArpIps;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasAllAddresses;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasAllowedVlans;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasDeclaredNames;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasDescription;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasIsis;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasMtu;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasName;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasOspfArea;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasOspfAreaName;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasOspfCost;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasOspfPointToPoint;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasSourceNats;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasSwitchPortMode;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasVrf;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.HasZoneName;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.IsActive;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.IsOspfPassive;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.IsOspfPointToPoint;
import org.batfish.datamodel.matchers.InterfaceMatchersImpl.IsProxyArp;
import org.batfish.datamodel.ospf.OspfArea;
import org.hamcrest.Matcher;

public final class InterfaceMatchers {

  /** Provides a matcher that matches if the provided value matches the interface's Access VLAN. */
  public static HasAccessVlan hasAccessVlan(int value) {
    return hasAccessVlan(equalTo(value));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * Access VLAN.
   */
  public static HasAccessVlan hasAccessVlan(Matcher<? super Integer> subMatcher) {
    return new HasAccessVlan(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * allAddresses.
   */
  public static Matcher<Interface> hasAllAddresses(
      Matcher<? super Set<InterfaceAddress>> subMatcher) {
    return new HasAllAddresses(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided value matches the interface's Allowed VLANs.
   */
  public static HasAllowedVlans hasAllowedVlans(List<SubRange> value) {
    return hasAllowedVlans(equalTo(value));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * Allowed VLANs.
   */
  public static HasAllowedVlans hasAllowedVlans(Matcher<? super List<SubRange>> subMatcher) {
    return new HasAllowedVlans(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * additional arp IPs.
   */
  public static HasAdditionalArpIps hasAdditionalArpIps(
      @Nonnull Matcher<? super SortedSet<Ip>> subMatcher) {
    return new HasAdditionalArpIps(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided the interface's declared names comprise the set
   * of unique strings in {@code expectedDeclaredNames}.
   */
  public static HasDeclaredNames hasDeclaredNames(@Nonnull Iterable<String> expectedDeclaredNames) {
    return new HasDeclaredNames(
        containsInAnyOrder(ImmutableSet.copyOf(expectedDeclaredNames).toArray()));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * declared names.
   */
  public static HasDeclaredNames hasDeclaredNames(
      @Nonnull Matcher<? super Set<String>> subMatcher) {
    return new HasDeclaredNames(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided the interface's declared names comprise the set
   * of unique strings in {@code expectedDeclaredNames}.
   */
  public static HasDeclaredNames hasDeclaredNames(@Nonnull String... expectedDeclaredNames) {
    return new HasDeclaredNames(
        containsInAnyOrder(ImmutableSet.copyOf(expectedDeclaredNames).toArray()));
  }

  /**
   * Provides a matcher that matches if the {@link Interface}'s description is {@code
   * expectedDescription}.
   */
  public static Matcher<Interface> hasDescription(String expectedDescription) {
    return new HasDescription(equalTo(expectedDescription));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * isis.
   */
  public static @Nonnull Matcher<Interface> hasIsis(
      @Nonnull Matcher<? super IsisInterfaceSettings> subMatcher) {
    return new HasIsis(subMatcher);
  }

  /** Provides a matcher that matches if the provided value matches the interface's MTU. */
  public static HasMtu hasMtu(int value) {
    return hasMtu(equalTo(value));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's MTU.
   */
  public static HasMtu hasMtu(Matcher<? super Integer> subMatcher) {
    return new HasMtu(subMatcher);
  }

  /** Provides a matcher that matches if the provided name matches the interface's name. */
  public static HasName hasName(String expectedName) {
    return new HasName(equalTo(expectedName));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's OSPF
   * area.
   */
  public static HasOspfArea hasOspfArea(Matcher<OspfArea> subMatcher) {
    return new HasOspfArea(subMatcher);
  }

  /**
   * Provides a matcher that matches if the the interface's OSPF area ID is {@link expectedArea}.
   */
  public static @Nonnull Matcher<Interface> hasOspfAreaName(long expectedArea) {
    return new HasOspfAreaName(equalTo(expectedArea));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's OSPF
   * area ID.
   */
  public static @Nonnull Matcher<Interface> hasOspfAreaName(
      @Nonnull Matcher<? super Long> subMatcher) {
    return new HasOspfAreaName(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's OSPF
   * cost.
   */
  public static HasOspfCost hasOspfCost(Matcher<Integer> subMatcher) {
    return new HasOspfCost(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's OSPF
   * point to point.
   */
  public static @Nonnull Matcher<Interface> hasOspfPointToPoint(
      @Nonnull Matcher<? super Boolean> subMatcher) {
    return new HasOspfPointToPoint(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * source NATs.
   */
  public static HasSourceNats hasSourceNats(@Nonnull Matcher<? super List<SourceNat>> subMatcher) {
    return new HasSourceNats(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided value matches the interface's Switch Port mode.
   */
  public static HasSwitchPortMode hasSwitchPortMode(SwitchportMode switchportMode) {
    return hasSwitchPortMode(equalTo(switchportMode));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * Switch Port mode.
   */
  public static HasSwitchPortMode hasSwitchPortMode(Matcher<? super SwitchportMode> subMatcher) {
    return new HasSwitchPortMode(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's VRF.
   */
  public static HasVrf hasVrf(Matcher<? super Vrf> subMatcher) {
    return new HasVrf(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's Zone
   * name.
   */
  public static HasZoneName hasZoneName(Matcher<? super String> subMatcher) {
    return new HasZoneName(subMatcher);
  }

  /** Provides a matcher that matches if the interface is active. */
  public static IsActive isActive() {
    return new IsActive(equalTo(true));
  }

  /**
   * Provides a matcher that matches if the provided active flag matches the interface's active
   * flag.
   */
  public static IsActive isActive(boolean active) {
    return new IsActive(equalTo(active));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * active flag.
   */
  public static IsActive isActive(@Nonnull Matcher<? super Boolean> subMatcher) {
    return new IsActive(subMatcher);
  }

  /** Provides a matcher that matches if the interface runs OSPF in passive mode. */
  public static IsOspfPassive isOspfPassive() {
    return isOspfPassive(equalTo(true));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * ospfPassive flag.
   */
  public static IsOspfPassive isOspfPassive(@Nonnull Matcher<? super Boolean> subMatcher) {
    return new IsOspfPassive(subMatcher);
  }

  /** Provides a matcher that matches if the interface runs OSPF in point-to-point mode. */
  public static IsOspfPointToPoint isOspfPointToPoint() {
    return new IsOspfPointToPoint(equalTo(true));
  }

  /** Provides a matcher that matches if the interface has proxy-arp enabled. */
  public static IsProxyArp isProxyArp() {
    return new IsProxyArp(equalTo(true));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the interface's
   * proxy-arp setting.
   */
  public static IsProxyArp isProxyArp(Matcher<? super Boolean> subMatcher) {
    return new IsProxyArp(subMatcher);
  }

  private InterfaceMatchers() {}
}
