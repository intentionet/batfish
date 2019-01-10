package org.batfish.datamodel.matchers;

import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.eigrp.EigrpInterfaceSettings;
import org.batfish.datamodel.hsrp.HsrpGroup;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.ospf.OspfArea;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class InterfaceMatchersImpl {

  static final class HasAccessVlan extends FeatureMatcher<Interface, Integer> {
    HasAccessVlan(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "an Interface with accessVlan:", "accessVlan");
    }

    @Override
    protected Integer featureValueOf(Interface actual) {
      return actual.getAccessVlan();
    }
  }

  static final class HasAdditionalArpIps extends FeatureMatcher<Interface, SortedSet<Ip>> {
    HasAdditionalArpIps(@Nonnull Matcher<? super SortedSet<Ip>> subMatcher) {
      super(subMatcher, "An interface with additionalArpIps:", "additionalArpIps");
    }

    @Override
    protected SortedSet<Ip> featureValueOf(Interface actual) {
      return actual.getAdditionalArpIps();
    }
  }

  static final class HasAllAddresses extends FeatureMatcher<Interface, Set<InterfaceAddress>> {
    HasAllAddresses(@Nonnull Matcher<? super Set<InterfaceAddress>> subMatcher) {
      super(subMatcher, "An Interface with allAddresses:", "allAddresses");
    }

    @Override
    protected Set<InterfaceAddress> featureValueOf(Interface actual) {
      return actual.getAllAddresses();
    }
  }

  static final class HasAllowedVlans extends FeatureMatcher<Interface, IntegerSpace> {
    HasAllowedVlans(@Nonnull Matcher<? super IntegerSpace> subMatcher) {
      super(subMatcher, "an Interface with allowedVlans:", "allowedVlans");
    }

    @Override
    protected IntegerSpace featureValueOf(Interface actual) {
      return actual.getAllowedVlans();
    }
  }

  static final class HasBandwidth extends FeatureMatcher<Interface, Double> {
    HasBandwidth(@Nonnull Matcher<? super Double> subMatcher) {
      super(subMatcher, "an Interface with bandwidth:", "bandwidth");
    }

    @Override
    protected Double featureValueOf(Interface actual) {
      return actual.getBandwidth();
    }
  }

  static final class HasDeclaredNames extends FeatureMatcher<Interface, Set<String>> {
    HasDeclaredNames(@Nonnull Matcher<? super Set<String>> subMatcher) {
      super(subMatcher, "declared names", "declared names");
    }

    @Override
    protected Set<String> featureValueOf(Interface actual) {
      return actual.getDeclaredNames();
    }
  }

  static final class HasDescription extends FeatureMatcher<Interface, String> {
    HasDescription(@Nonnull Matcher<? super String> subMatcher) {
      super(subMatcher, "An Interface with description:", "description");
    }

    @Override
    protected String featureValueOf(Interface actual) {
      return actual.getDescription();
    }
  }

  static final class HasEigrp extends FeatureMatcher<Interface, EigrpInterfaceSettings> {
    HasEigrp(@Nonnull Matcher<? super EigrpInterfaceSettings> subMatcher) {
      super(subMatcher, "An Interface with eigrp:", "eigrp");
    }

    @Override
    @Nullable
    protected EigrpInterfaceSettings featureValueOf(Interface actual) {
      return actual.getEigrp();
    }
  }

  static final class HasHsrpGroup extends FeatureMatcher<Interface, HsrpGroup> {
    private final int _number;

    HasHsrpGroup(int number, @Nonnull Matcher<? super HsrpGroup> subMatcher) {
      super(
          subMatcher,
          String.format("An Interface with hsrpGroup %d:", number),
          String.format("hsrpGroup %d", number));
      _number = number;
    }

    @Override
    protected HsrpGroup featureValueOf(Interface actual) {
      return actual.getHsrpGroups().get(_number);
    }
  }

  static final class HasHsrpVersion extends FeatureMatcher<Interface, String> {
    HasHsrpVersion(@Nonnull Matcher<? super String> subMatcher) {
      super(subMatcher, "An Interface with hsrpVersion:", "hsrpVersion");
    }

    @Override
    protected String featureValueOf(Interface actual) {
      return actual.getHsrpVersion();
    }
  }

  static final class HasIsis extends FeatureMatcher<Interface, IsisInterfaceSettings> {
    HasIsis(@Nonnull Matcher<? super IsisInterfaceSettings> subMatcher) {
      super(subMatcher, "An Interface with isis:", "isis");
    }

    @Override
    protected IsisInterfaceSettings featureValueOf(Interface actual) {
      return actual.getIsis();
    }
  }

  static final class HasMlagId extends FeatureMatcher<Interface, Integer> {
    HasMlagId(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "an Interface with MLAG ID:", "mlagId");
    }

    @Override
    @Nullable
    protected Integer featureValueOf(Interface actual) {
      return actual.getMlagId();
    }
  }

  static final class HasMtu extends FeatureMatcher<Interface, Integer> {
    HasMtu(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "an Interface with MTU:", "MTU");
    }

    @Override
    protected Integer featureValueOf(Interface actual) {
      return actual.getMtu();
    }
  }

  static final class HasName extends FeatureMatcher<Interface, String> {
    HasName(@Nonnull Matcher<? super String> subMatcher) {
      super(subMatcher, "an Interface with name:", "name");
    }

    @Override
    protected String featureValueOf(Interface actual) {
      return actual.getName();
    }
  }

  static final class HasOspfArea extends FeatureMatcher<Interface, OspfArea> {
    HasOspfArea(@Nonnull Matcher<? super OspfArea> subMatcher) {
      super(subMatcher, "an Interface with ospfArea:", "ospfArea");
    }

    @Override
    protected OspfArea featureValueOf(Interface actual) {
      return actual.getOspfArea();
    }
  }

  static final class HasOspfAreaName extends FeatureMatcher<Interface, Long> {
    HasOspfAreaName(@Nonnull Matcher<? super Long> subMatcher) {
      super(subMatcher, "an Interface with ospfAreaName:", "ospfAreaName");
    }

    @Override
    protected Long featureValueOf(Interface actual) {
      return actual.getOspfAreaName();
    }
  }

  static final class HasOspfCost extends FeatureMatcher<Interface, Integer> {
    HasOspfCost(@Nonnull Matcher<? super Integer> subMatcher) {
      super(subMatcher, "an Interface with ospfCost:", "ospfCost");
    }

    @Override
    protected Integer featureValueOf(Interface actual) {
      return actual.getOspfCost();
    }
  }

  static final class HasOspfPointToPoint extends FeatureMatcher<Interface, Boolean> {
    HasOspfPointToPoint(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "an Interface with ospfPointToPoint:", "ospfPointToPoint");
    }

    @Override
    protected Boolean featureValueOf(Interface actual) {
      return actual.getOspfPointToPoint();
    }
  }

  static final class HasIncomingFilter extends FeatureMatcher<Interface, IpAccessList> {
    HasIncomingFilter(@Nonnull Matcher<? super IpAccessList> subMatcher) {
      super(subMatcher, "an Interface with incomingFilter:", "incomingFilter");
    }

    @Override
    protected IpAccessList featureValueOf(Interface actual) {
      return actual.getIncomingFilter();
    }
  }

  static final class HasOutgoingFilter extends FeatureMatcher<Interface, IpAccessList> {
    HasOutgoingFilter(@Nonnull Matcher<? super IpAccessList> subMatcher) {
      super(subMatcher, "an Interface with outgoingFilter:", "outgoingFilter");
    }

    @Override
    protected IpAccessList featureValueOf(Interface actual) {
      return actual.getOutgoingFilter();
    }
  }

  static final class HasOutgoingFilterName extends FeatureMatcher<Interface, String> {
    HasOutgoingFilterName(@Nonnull Matcher<? super String> subMatcher) {
      super(subMatcher, "an Interface with outgoingFilterName:", "outgoingFilterName");
    }

    @Override
    protected String featureValueOf(Interface actual) {
      return actual.getOutgoingFilterName();
    }
  }

  static final class HasSwitchPortMode extends FeatureMatcher<Interface, SwitchportMode> {
    HasSwitchPortMode(@Nonnull Matcher<? super SwitchportMode> subMatcher) {
      super(subMatcher, "an Interface with switchPortMode:", "switchPortMode");
    }

    @Override
    protected SwitchportMode featureValueOf(Interface actual) {
      return actual.getSwitchportMode();
    }
  }

  static final class HasVrf extends FeatureMatcher<Interface, Vrf> {
    HasVrf(@Nonnull Matcher<? super Vrf> subMatcher) {
      super(subMatcher, "an Interface with vrf:", "vrf");
    }

    @Override
    protected Vrf featureValueOf(Interface actual) {
      return actual.getVrf();
    }
  }

  static final class HasZoneName extends FeatureMatcher<Interface, String> {
    HasZoneName(@Nonnull Matcher<? super String> subMatcher) {
      super(subMatcher, "an Interface with zoneName:", "zoneName");
    }

    @Override
    protected String featureValueOf(Interface actual) {
      return actual.getZoneName();
    }
  }

  static final class IsActive extends FeatureMatcher<Interface, Boolean> {
    IsActive(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "an Interface with active:", "active");
    }

    @Override
    protected Boolean featureValueOf(Interface actual) {
      return actual.getActive();
    }
  }

  static final class IsOspfPassive extends FeatureMatcher<Interface, Boolean> {
    IsOspfPassive(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "an Interface with ospfPassive:", "ospfPassive");
    }

    @Override
    protected Boolean featureValueOf(Interface actual) {
      return actual.getOspfPassive();
    }
  }

  static final class IsOspfPointToPoint extends FeatureMatcher<Interface, Boolean> {
    IsOspfPointToPoint(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "an Interface with ospfPointToPoint:", "ospfPointToPoint");
    }

    @Override
    protected Boolean featureValueOf(Interface actual) {
      return actual.getOspfPointToPoint();
    }
  }

  static final class IsProxyArp extends FeatureMatcher<Interface, Boolean> {
    IsProxyArp(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "an Interface with proxyArp:", "proxyArp");
    }

    @Override
    protected Boolean featureValueOf(Interface actual) {
      return actual.getProxyArp();
    }
  }

  private InterfaceMatchersImpl() {}
}
