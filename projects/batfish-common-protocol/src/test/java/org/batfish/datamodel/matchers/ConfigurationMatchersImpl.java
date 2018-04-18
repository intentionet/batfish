package org.batfish.datamodel.matchers;

import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.vendor_family.VendorFamily;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class ConfigurationMatchersImpl {

  static final class HasDefaultVrf extends FeatureMatcher<Configuration, Vrf> {
    HasDefaultVrf(@Nonnull Matcher<? super Vrf> subMatcher) {
      super(subMatcher, "A Configuration with defaultVrf:", "defaultVrf");
    }

    @Override
    protected Vrf featureValueOf(Configuration actual) {
      return actual.getDefaultVrf();
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

  private ConfigurationMatchersImpl() {}
}
