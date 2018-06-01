package org.batfish.datamodel.matchers;

import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IkeProposal;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.vendor_family.VendorFamily;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class ConfigurationMatchersImpl {

  static final class HasConfigurationFormat
      extends FeatureMatcher<Configuration, ConfigurationFormat> {
    HasConfigurationFormat(@Nonnull Matcher<? super ConfigurationFormat> subMatcher) {
      super(subMatcher, "a configuration with configurationFormat", "configurationFormat");
    }

    @Override
    protected ConfigurationFormat featureValueOf(Configuration actual) {
      return actual.getConfigurationFormat();
    }
  }

  static final class HasDefaultVrf extends FeatureMatcher<Configuration, Vrf> {
    HasDefaultVrf(@Nonnull Matcher<? super Vrf> subMatcher) {
      super(subMatcher, "A Configuration with defaultVrf:", "defaultVrf");
    }

    @Override
    protected Vrf featureValueOf(Configuration actual) {
      return actual.getDefaultVrf();
    }
  }

  static final class HasIkeProposal extends FeatureMatcher<Configuration, IkeProposal> {
    private final String _name;

    HasIkeProposal(@Nonnull String name, @Nonnull Matcher<? super IkeProposal> subMatcher) {
      super(subMatcher, "A Configuration with ikeProposal " + name + ":", "ikeProposal " + name);
      _name = name;
    }

    @Override
    protected IkeProposal featureValueOf(Configuration actual) {
      return actual.getIkeProposals().get(_name);
    }
  }

  static final class HasInterface extends FeatureMatcher<Configuration, Interface> {
    private final String _name;

    HasInterface(@Nonnull String name, @Nonnull Matcher<? super Interface> subMatcher) {
      super(subMatcher, "A Configuration with interface " + name + ":", "interface " + name);
      _name = name;
    }

    @Override
    protected Interface featureValueOf(Configuration actual) {
      return actual.getInterfaces().get(_name);
    }
  }

  static final class HasInterfaces extends FeatureMatcher<Configuration, Map<String, Interface>> {
    HasInterfaces(@Nonnull Matcher<? super Map<String, Interface>> subMatcher) {
      super(subMatcher, "a configuration with interfaces", "interfaces");
    }

    @Override
    protected Map<String, Interface> featureValueOf(Configuration actual) {
      return actual.getInterfaces();
    }
  }

  static final class HasIpAccessList extends FeatureMatcher<Configuration, IpAccessList> {
    private final String _name;

    HasIpAccessList(@Nonnull String name, @Nonnull Matcher<? super IpAccessList> subMatcher) {
      super(subMatcher, "A Configuration with ipAccessList " + name + ":", "ipAccessList " + name);
      _name = name;
    }

    @Override
    protected IpAccessList featureValueOf(Configuration actual) {
      return actual.getIpAccessLists().get(_name);
    }
  }

  static final class HasIpAccessLists
      extends FeatureMatcher<Configuration, Map<String, IpAccessList>> {
    HasIpAccessLists(@Nonnull Matcher<? super Map<String, IpAccessList>> subMatcher) {
      super(subMatcher, "a configuration with ipAccessLists", "ipAccessLists");
    }

    @Override
    protected Map<String, IpAccessList> featureValueOf(Configuration actual) {
      return actual.getIpAccessLists();
    }
  }

  static final class HasIpSpace extends FeatureMatcher<Configuration, IpSpace> {
    private final String _name;

    HasIpSpace(@Nonnull String name, @Nonnull Matcher<? super IpSpace> subMatcher) {
      super(subMatcher, "A Configuration with ipSpace " + name + ":", "ipSpace " + name);
      _name = name;
    }

    @Override
    protected IpSpace featureValueOf(Configuration actual) {
      return actual.getIpSpaces().get(_name);
    }
  }

  static final class HasIpSpaces extends FeatureMatcher<Configuration, Map<String, IpSpace>> {
    HasIpSpaces(@Nonnull Matcher<? super Map<String, IpSpace>> subMatcher) {
      super(subMatcher, "a configuration with ipSpaces", "ipSpaces");
    }

    @Override
    protected Map<String, IpSpace> featureValueOf(Configuration actual) {
      return actual.getIpSpaces();
    }
  }

  static final class HasRoute6FilterList extends FeatureMatcher<Configuration, Route6FilterList> {
    private final String _name;

    HasRoute6FilterList(
        @Nonnull String name, @Nonnull Matcher<? super Route6FilterList> subMatcher) {
      super(
          subMatcher,
          "A Configuration with Route6FilterList " + name + ":",
          "Route6FilterList " + name);
      _name = name;
    }

    @Override
    protected Route6FilterList featureValueOf(Configuration actual) {
      return actual.getRoute6FilterLists().get(_name);
    }
  }

  static final class HasRouteFilterList extends FeatureMatcher<Configuration, RouteFilterList> {
    private final String _name;

    HasRouteFilterList(@Nonnull String name, @Nonnull Matcher<? super RouteFilterList> subMatcher) {
      super(
          subMatcher,
          "A Configuration with RouteFilterList " + name + ":",
          "RouteFilterList " + name);
      _name = name;
    }

    @Override
    protected RouteFilterList featureValueOf(Configuration actual) {
      return actual.getRouteFilterLists().get(_name);
    }
  }

  static final class HasVendorFamily extends FeatureMatcher<Configuration, VendorFamily> {
    HasVendorFamily(@Nonnull Matcher<? super VendorFamily> subMatcher) {
      super(subMatcher, "a configuration with vendorFamily", "vendorFamily");
    }

    @Override
    protected VendorFamily featureValueOf(Configuration actual) {
      return actual.getVendorFamily();
    }
  }

  static final class HasVrf extends FeatureMatcher<Configuration, Vrf> {
    private final String _name;

    HasVrf(@Nonnull String name, @Nonnull Matcher<? super Vrf> subMatcher) {
      super(subMatcher, "A Configuration with vrf " + name + ":", "vrf " + name);
      _name = name;
    }

    @Override
    protected Vrf featureValueOf(Configuration actual) {
      return actual.getVrfs().get(_name);
    }
  }

  static final class HasVrfs extends FeatureMatcher<Configuration, Map<String, Vrf>> {
    HasVrfs(@Nonnull Matcher<? super Map<String, Vrf>> subMatcher) {
      super(subMatcher, "a configuration with vrfs", "vrfs");
    }

    @Override
    protected Map<String, Vrf> featureValueOf(Configuration actual) {
      return actual.getVrfs();
    }
  }

  static final class HasZone extends FeatureMatcher<Configuration, Zone> {
    private final String _name;

    HasZone(@Nonnull String name, @Nonnull Matcher<? super Zone> subMatcher) {
      super(subMatcher, "A Configuration with zone " + name + ":", "zone " + name);
      _name = name;
    }

    @Override
    protected Zone featureValueOf(Configuration actual) {
      return actual.getZones().get(_name);
    }
  }

  private ConfigurationMatchersImpl() {}
}
