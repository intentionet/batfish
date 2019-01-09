package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import java.util.NavigableMap;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IkeGateway;
import org.batfish.datamodel.IkePhase1Policy;
import org.batfish.datamodel.IkePhase1Proposal;
import org.batfish.datamodel.IkeProposal;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPhase2Policy;
import org.batfish.datamodel.IpsecPhase2Proposal;
import org.batfish.datamodel.IpsecPolicy;
import org.batfish.datamodel.IpsecProposal;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.Mlag;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasConfigurationFormat;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasDefaultVrf;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasHostname;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIkeGateway;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIkePhase1Policy;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIkePhase1Proposal;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIkeProposal;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasInterface;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasInterfaces;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpAccessList;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpAccessLists;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpSpace;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpSpaces;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpsecPeerConfig;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpsecPhase2Policy;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpsecPhase2Proposal;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpsecPolicy;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpsecProposal;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasIpsecVpn;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasMlag;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasTrackingGroups;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasVendorFamily;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasVrf;
import org.batfish.datamodel.matchers.ConfigurationMatchersImpl.HasVrfs;
import org.batfish.datamodel.tracking.TrackMethod;
import org.batfish.datamodel.vendor_family.VendorFamily;
import org.hamcrest.Matcher;

public class ConfigurationMatchers {

  public static HasConfigurationFormat hasConfigurationFormat(
      @Nonnull Matcher<? super ConfigurationFormat> subMatcher) {
    return new HasConfigurationFormat(subMatcher);
  }

  /** Provides a matcher that matches if the configuration has a default VRF. */
  public static HasDefaultVrf hasDefaultVrf() {
    return new HasDefaultVrf(not(nullValue()));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * default VRF.
   */
  public static HasDefaultVrf hasDefaultVrf(@Nonnull Matcher<? super Vrf> subMatcher) {
    return new HasDefaultVrf(subMatcher);
  }

  /**
   * Provides a matcher that matches if the configuration's hostname is {@code expectedHostname}.
   */
  public static @Nonnull Matcher<Configuration> hasHostname(@Nonnull String expectedHostname) {
    return new HasHostname(equalTo(expectedHostname));
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * hostname.
   */
  public static @Nonnull Matcher<Configuration> hasHostname(
      @Nonnull Matcher<? super String> subMatcher) {
    return new HasHostname(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IKE gateway with specified name.
   */
  public static HasIkeGateway hasIkeGateway(
      @Nonnull String name, @Nonnull Matcher<? super IkeGateway> subMatcher) {
    return new HasIkeGateway(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IKE proposal with specified name.
   */
  public static HasIkeProposal hasIkeProposal(
      @Nonnull String name, @Nonnull Matcher<? super IkeProposal> subMatcher) {
    return new HasIkeProposal(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IKE phase 1 policy with specified name.
   */
  public static HasIkePhase1Policy hasIkePhase1Policy(
      @Nonnull String name, @Nonnull Matcher<? super IkePhase1Policy> subMatcher) {
    return new HasIkePhase1Policy(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IKE phase 1 proposal with specified name.
   */
  public static HasIkePhase1Proposal hasIkePhase1Proposal(
      @Nonnull String name, @Nonnull Matcher<? super IkePhase1Proposal> subMatcher) {
    return new HasIkePhase1Proposal(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * interface with specified name.
   */
  public static HasInterface hasInterface(
      @Nonnull String name, @Nonnull Matcher<? super Interface> subMatcher) {
    return new HasInterface(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * interfaces.
   */
  public static HasInterfaces hasInterfaces(
      @Nonnull Matcher<? super Map<String, Interface>> subMatcher) {
    return new HasInterfaces(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IpAccessList with specified name.
   */
  public static HasIpAccessList hasIpAccessList(
      @Nonnull String name, @Nonnull Matcher<? super IpAccessList> subMatcher) {
    return new HasIpAccessList(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * ipAccessLists.
   */
  public static HasIpAccessLists hasIpAccessLists(
      @Nonnull Matcher<? super Map<String, IpAccessList>> subMatcher) {
    return new HasIpAccessLists(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IPSec peer config with specified name.
   */
  public static HasIpsecPeerConfig hasIpsecPeerConfig(
      @Nonnull String name, @Nonnull Matcher<? super IpsecPeerConfig> subMatcher) {
    return new HasIpsecPeerConfig(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IPSec Phase2 policy with specified name.
   */
  public static HasIpsecPhase2Policy hasIpsecPhase2Policy(
      @Nonnull String name, @Nonnull Matcher<? super IpsecPhase2Policy> subMatcher) {
    return new HasIpsecPhase2Policy(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IPSec Phase2 proposal with specified name.
   */
  public static HasIpsecPhase2Proposal hasIpsecPhase2Proposal(
      @Nonnull String name, @Nonnull Matcher<? super IpsecPhase2Proposal> subMatcher) {
    return new HasIpsecPhase2Proposal(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IPSec proposal with specified name.
   */
  public static HasIpsecProposal hasIpsecProposal(
      @Nonnull String name, @Nonnull Matcher<? super IpsecProposal> subMatcher) {
    return new HasIpsecProposal(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IPSec vpn with specified name.
   */
  public static @Nonnull HasIpsecVpn hasIpsecVpn(
      @Nonnull String name, @Nonnull Matcher<? super IpsecVpn> subMatcher) {
    return new HasIpsecVpn(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * IpSpace with specified name.
   */
  public static HasIpSpace hasIpSpace(
      @Nonnull String name, @Nonnull Matcher<? super IpSpace> subMatcher) {
    return new HasIpSpace(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * ipSpaces.
   */
  public static HasIpSpaces hasIpSpaces(@Nonnull Matcher<? super Map<String, IpSpace>> subMatcher) {
    return new HasIpSpaces(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * Ipspec policy with the specified name.
   */
  public static HasIpsecPolicy hasIpsecPolicy(
      @Nonnull String name, @Nonnull Matcher<? super IpsecPolicy> subMatcher) {
    return new HasIpsecPolicy(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * MLAG configuration with the specified name.
   */
  public static HasMlag hasMlagConfig(
      @Nonnull String name, @Nonnull Matcher<? super Mlag> subMatcher) {
    return new HasMlag(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * Configuration}'s trackingGroups.
   */
  public static Matcher<Configuration> hasTrackingGroups(
      @Nonnull Matcher<? super NavigableMap<String, TrackMethod>> subMatcher) {
    return new HasTrackingGroups(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * vendorFamily.
   */
  public static HasVendorFamily hasVendorFamily(@Nonnull Matcher<? super VendorFamily> subMatcher) {
    return new HasVendorFamily(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * VRF with specified name.
   */
  public static HasVrf hasVrf(@Nonnull String name, @Nonnull Matcher<? super Vrf> subMatcher) {
    return new HasVrf(name, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the configuration's
   * vrfs.
   */
  public static HasVrfs hasVrfs(@Nonnull Matcher<? super Map<String, Vrf>> subMatcher) {
    return new HasVrfs(subMatcher);
  }

  private ConfigurationMatchers() {}
}
