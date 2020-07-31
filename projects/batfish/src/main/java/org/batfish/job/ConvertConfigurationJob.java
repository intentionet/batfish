package org.batfish.job;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.runtime.SnapshotRuntimeData;
import org.batfish.config.Settings;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.bgp.community.CommunityStructuresVerifier;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.main.Batfish;
import org.batfish.representation.host.HostConfiguration;
import org.batfish.representation.iptables.IptablesVendorConfiguration;
import org.batfish.vendor.VendorConfiguration;

public class ConvertConfigurationJob extends BatfishJob<ConvertConfigurationResult> {

  private Object _configObject;
  @Nonnull private final SnapshotRuntimeData _runtimeData;
  private String _name;

  public ConvertConfigurationJob(
      Settings settings,
      @Nullable SnapshotRuntimeData runtimeData,
      Object configObject,
      String name) {
    super(settings);
    _configObject = configObject;
    _runtimeData = firstNonNull(runtimeData, SnapshotRuntimeData.EMPTY_SNAPSHOT_RUNTIME_DATA);
    _name = name;
  }

  /**
   * Sanity checks the given map from name-of-thing to thing-with-name for name consistency. If the
   * names are not consistent, warns and does not convert them.
   *
   * <p>The created maps are sorted by key in ascending order.
   *
   * <p>Hopefully this will only happen during new parser development, and will help authors of
   * those new parsers get it right.
   */
  private static <T> ImmutableMap<String, T> verifyAndToImmutableMap(
      @Nullable Map<String, T> map, Function<T, String> keyFn, Warnings w) {
    if (map == null || map.isEmpty()) {
      return ImmutableMap.of();
    }
    return map.entrySet().stream()
        .filter(
            e -> {
              String key = keyFn.apply(e.getValue());
              if (key.equals(e.getKey())) {
                return true;
              }
              w.redFlag(
                  String.format(
                      "Batfish internal error: invalid entry %s -> %s named %s",
                      e.getKey(), e.getValue(), key));
              return false;
            })
        .sorted(Comparator.comparing(Entry::getKey)) /* ImmutableMap is insert ordered. */
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  /**
   * Converts {@link Map} to {@link ImmutableMap}. The created maps are sorted by key in ascending
   * order.
   */
  private static <T> ImmutableMap<String, T> toImmutableMap(@Nullable Map<String, T> map) {
    if (map == null || map.isEmpty()) {
      return ImmutableMap.of();
    }
    return map.entrySet().stream()
        .sorted(Comparator.comparing(Entry::getKey)) /* ImmutableMap is insert ordered. */
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  /**
   * Applies sanity checks and finishing touches to the given {@link Configuration}.
   *
   * <p>Sanity checks such as asserting that required properties hold.
   *
   * <p>Finishing touches such as converting structures to their immutable forms.
   */
  @VisibleForTesting
  static void finalizeConfiguration(Configuration c, Warnings w) {
    String hostname = c.getHostname();
    if (c.getDefaultCrossZoneAction() == null) {
      throw new BatfishException(
          "Implementation error: missing default cross-zone action for host: '" + hostname + "'");
    }
    if (c.getDefaultInboundAction() == null) {
      throw new BatfishException(
          "Implementation error: missing default inbound action for host: '" + hostname + "'");
    }
    c.simplifyRoutingPolicies();
    c.computeRoutingPolicySources(w);
    verifyInterfaces(c, w);

    c.setAsPathAccessLists(
        verifyAndToImmutableMap(c.getAsPathAccessLists(), AsPathAccessList::getName, w));
    c.setCommunityLists(verifyAndToImmutableMap(c.getCommunityLists(), CommunityList::getName, w));
    c.setCommunityMatchExprs(toImmutableMap(c.getCommunityMatchExprs()));
    c.setCommunitySetExprs(toImmutableMap(c.getCommunitySetExprs()));
    c.setCommunitySetMatchExprs(toImmutableMap(c.getCommunitySetMatchExprs()));
    c.setCommunitySets(toImmutableMap(c.getCommunitySets()));
    c.setInterfaces(verifyAndToImmutableMap(c.getAllInterfaces(), Interface::getName, w));
    c.setIpAccessLists(verifyAndToImmutableMap(c.getIpAccessLists(), IpAccessList::getName, w));
    c.setIpSpaces(toImmutableMap(c.getIpSpaces()));
    c.setIp6AccessLists(verifyAndToImmutableMap(c.getIp6AccessLists(), Ip6AccessList::getName, w));
    c.setRouteFilterLists(
        verifyAndToImmutableMap(c.getRouteFilterLists(), RouteFilterList::getName, w));
    c.setRoute6FilterLists(
        verifyAndToImmutableMap(c.getRoute6FilterLists(), Route6FilterList::getName, w));
    c.setRoutingPolicies(
        verifyAndToImmutableMap(c.getRoutingPolicies(), RoutingPolicy::getName, w));
    verifyCommunityStructures(c);
    removeInvalidAcls(c, w);
  }

  private static void verifyCommunityStructures(Configuration c) {
    // TODO: crash on undefined/circular refs (conversion is responsible for preventing them)
    CommunityStructuresVerifier.verify(c);
  }

  /** Warns on and removes interfaces with VI-invalid settings. */
  private static void verifyInterfaces(Configuration c, Warnings w) {
    Set<String> inInterfaces = ImmutableSet.copyOf(c.getAllInterfaces().keySet());
    for (String name : inInterfaces) {
      Interface i = c.getAllInterfaces().get(name);
      // VI invariant: switchport is true iff SwitchportMode is not NONE.
      boolean hasSwitchportMode = i.getSwitchportMode() != SwitchportMode.NONE;
      if (hasSwitchportMode != i.getSwitchport()) {
        w.redFlag(
            String.format(
                "Interface %s has switchport %s but switchport mode %s",
                name, i.getSwitchport(), i.getSwitchportMode()));
        c.getAllInterfaces().remove(name);
      }
    }
  }

  private static void clearInvalidFilterAndWarn(
      Map<String, IpAccessList> acls,
      @Nonnull Supplier<IpAccessList> getter,
      @Nonnull Consumer<IpAccessList> setter,
      @Nonnull String purpose,
      Warnings w) {
    @Nullable IpAccessList acl = getter.get();
    if (acl == null) {
      return;
    }
    @Nullable IpAccessList inMap = acls.get(acl.getName());
    // Deliberate == comparison.
    if (inMap == acl) {
      return;
    }
    if (inMap == null) {
      w.redFlag(
          String.format("The device ACL map does not contain %s %s.", purpose, acl.getName()));
    } else {
      w.redFlag(
          String.format(
              "The device ACL map has a different version of %s %s.", purpose, acl.getName()));
    }
    setter.accept(null);
  }

  /** Confirm assigned ACLs (e.g. interface's outgoing ACL) exist in config's IpAccessList map */
  private static void removeInvalidAcls(Configuration c, Warnings w) {
    for (Interface iface : c.getAllInterfaces().values()) {
      Map<String, IpAccessList> acls = c.getIpAccessLists();
      clearInvalidFilterAndWarn(
          acls, iface::getInboundFilter, iface::setInboundFilter, "inbound filter", w);
      clearInvalidFilterAndWarn(
          acls, iface::getIncomingFilter, iface::setIncomingFilter, "incoming filter", w);
      clearInvalidFilterAndWarn(
          acls, iface::getOutgoingFilter, iface::setOutgoingFilter, "outgoing filter", w);
      clearInvalidFilterAndWarn(
          acls,
          iface::getOutgoingOriginalFlowFilter,
          iface::setOutgoingOriginalFlowFilter,
          "outgoing filter on original flow",
          w);
      clearInvalidFilterAndWarn(
          acls,
          iface::getPostTransformationIncomingFilter,
          iface::setPostTransformationIncomingFilter,
          "post transformation incoming filter",
          w);
      clearInvalidFilterAndWarn(
          acls,
          iface::getPreTransformationOutgoingFilter,
          iface::setPreTransformationOutgoingFilter,
          "pre transformation outgoing filter",
          w);
    }
  }

  @Override
  public ConvertConfigurationResult call() {
    long startTime = System.currentTimeMillis();
    long elapsedTime;
    _logger.infof("Processing: \"%s\"", _name);
    Map<String, Configuration> configurations = new HashMap<>();
    Map<String, Warnings> warningsByHost = new HashMap<>();
    ConvertConfigurationAnswerElement answerElement = new ConvertConfigurationAnswerElement();
    Multimap<String, String> fileMap = answerElement.getFileMap();
    try {
      VendorConfiguration vendorConfiguration = (VendorConfiguration) _configObject;
      Warnings warnings = Batfish.buildWarnings(_settings);
      String filename = vendorConfiguration.getFilename();
      vendorConfiguration.setWarnings(warnings);
      vendorConfiguration.setAnswerElement(answerElement);
      vendorConfiguration.setRuntimeData(_runtimeData);
      for (Configuration configuration : vendorConfiguration.toVendorIndependentConfigurations()) {

        // get iptables if applicable
        IptablesVendorConfiguration iptablesConfig = null;
        VendorConfiguration ov = vendorConfiguration.getOverlayConfiguration();
        if (ov != null) {
          // apply overlay
          HostConfiguration oh = (HostConfiguration) ov;
          iptablesConfig = oh.getIptablesVendorConfig();
        } else if (vendorConfiguration instanceof HostConfiguration) {
          // TODO: To enable below, we need to reconcile overlay and non-overlay iptables
          // semantics.
          // HostConfiguration oh = (HostConfiguration)vendorConfiguration;
          // iptablesConfig = oh.getIptablesVendorConfig();
        }
        if (iptablesConfig != null) {
          iptablesConfig.addAsIpAccessLists(configuration, vendorConfiguration, warnings);
          iptablesConfig.applyAsOverlay(configuration, warnings);
        }

        finalizeConfiguration(configuration, warnings);

        String hostname = configuration.getHostname();
        configurations.put(hostname, configuration);
        warningsByHost.put(hostname, warnings);
        fileMap.put(filename, hostname);
      }
      _logger.info(" ...OK\n");
    } catch (Exception e) {
      String error = "Conversion error for node with hostname '" + _name + "'";
      elapsedTime = System.currentTimeMillis() - startTime;
      return new ConvertConfigurationResult(
          elapsedTime, _logger.getHistory(), _name, new BatfishException(error, e));
    } finally {
      warningsByHost.forEach((hostname, warnings) -> Batfish.logWarnings(_logger, warnings));
    }
    elapsedTime = System.currentTimeMillis() - startTime;
    return new ConvertConfigurationResult(
        elapsedTime, _logger.getHistory(), warningsByHost, _name, configurations, answerElement);
  }
}
