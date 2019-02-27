package org.batfish.specifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.specifier.parboiled.ParboiledFilterSpecifierFactory;
import org.batfish.specifier.parboiled.ParboiledInterfaceSpecifierFactory;
import org.batfish.specifier.parboiled.ParboiledIpSpaceSpecifierFactory;
import org.batfish.specifier.parboiled.ParboiledLocationSpecifierFactory;
import org.batfish.specifier.parboiled.ParboiledNodeSpecifierFactory;

/**
 * This class enables a global choice of the grammar that is used by different question parameters.
 * Questions call the static functions below to directly get the specifier and never explicitly pick
 * the factory.
 */
@ParametersAreNonnullByDefault
public final class SpecifierFactories {

  public enum Version {
    V1, // original, regex-based parsing of "flexible" specifiers
    V2 // newer parboiled-based implementation
  }

  private SpecifierFactories() {}

  /** Which grammar version is currently in use */
  public static final Version ACTIVE_VERSION = Version.V2;

  public static FilterSpecifierFactory getFilterFactory(Version version) {
    switch (version) {
      case V1:
        return new FlexibleFilterSpecifierFactory();
      case V2:
        return new ParboiledFilterSpecifierFactory();
      default:
        throw new IllegalStateException("Unhandled grammar version " + version);
    }
  }

  public static InterfaceSpecifierFactory getInterfaceFactory(Version version) {
    switch (version) {
      case V1:
        return new FlexibleInterfaceSpecifierFactory();
      case V2:
        return new ParboiledInterfaceSpecifierFactory();
      default:
        throw new IllegalStateException("Unhandled grammar version " + version);
    }
  }

  public static IpSpaceSpecifierFactory getIpSpaceFactory(Version version) {
    switch (version) {
      case V1:
        return new FlexibleIpSpaceSpecifierFactory();
      case V2:
        return new ParboiledIpSpaceSpecifierFactory();
      default:
        throw new IllegalStateException("Unhandled grammar version " + version);
    }
  }

  public static LocationSpecifierFactory getLocationFactory(Version version) {
    switch (version) {
      case V1:
        return new FlexibleLocationSpecifierFactory();
      case V2:
        return new ParboiledLocationSpecifierFactory();
      default:
        throw new IllegalStateException("Unhandled grammar version " + version);
    }
  }

  public static NodeSpecifierFactory getNodeFactory(Version version) {
    switch (version) {
      case V1:
        return new FlexibleNodeSpecifierFactory();
      case V2:
        return new ParboiledNodeSpecifierFactory();
      default:
        throw new IllegalStateException("Unhandled grammar version " + version);
    }
  }

  /** Define these constants, so we don't have to keep computing them */
  private static final FilterSpecifierFactory ActiveFilterFactory =
      getFilterFactory(ACTIVE_VERSION);

  private static final InterfaceSpecifierFactory ActiveInterfaceFactory =
      getInterfaceFactory(ACTIVE_VERSION);

  private static final IpSpaceSpecifierFactory ActiveIpSpaceFactory =
      getIpSpaceFactory(ACTIVE_VERSION);

  private static final LocationSpecifierFactory ActiveLocationFactory =
      getLocationFactory(ACTIVE_VERSION);

  private static final NodeSpecifierFactory ActiveNodeFactory = getNodeFactory(ACTIVE_VERSION);

  public static FilterSpecifier getFilterSpecifierOrDefault(
      @Nullable String input, FilterSpecifier defaultSpecifier) {
    return getFilterSpecifierOrDefault(input, defaultSpecifier, ActiveFilterFactory);
  }

  public static InterfaceSpecifier getInterfaceSpecifierOrDefault(
      @Nullable String input, InterfaceSpecifier defaultSpecifier) {
    return getInterfaceSpecifierOrDefault(input, defaultSpecifier, ActiveInterfaceFactory);
  }

  public static IpSpaceSpecifier getIpSpaceSpecifierOrDefault(
      @Nullable String input, IpSpaceSpecifier defaultSpecifier) {
    return getIpSpaceSpecifierOrDefault(input, defaultSpecifier, ActiveIpSpaceFactory);
  }

  public static LocationSpecifier getLocationSpecifierOrDefault(
      @Nullable String input, LocationSpecifier defaultSpecifier) {
    return getLocationSpecifierOrDefault(input, defaultSpecifier, ActiveLocationFactory);
  }

  public static NodeSpecifier getNodeSpecifierOrDefault(
      @Nullable String input, NodeSpecifier defaultSpecifier) {
    return getNodeSpecifierOrDefault(input, defaultSpecifier, ActiveNodeFactory);
  }

  public static FilterSpecifier getFilterSpecifierOrDefault(
      @Nullable String input, FilterSpecifier defaultSpecifier, FilterSpecifierFactory factory) {
    return input == null || input.isEmpty()
        ? defaultSpecifier
        : factory.buildFilterSpecifier(input);
  }

  public static InterfaceSpecifier getInterfaceSpecifierOrDefault(
      @Nullable String input,
      InterfaceSpecifier defaultSpecifier,
      InterfaceSpecifierFactory factory) {
    return input == null || input.isEmpty()
        ? defaultSpecifier
        : factory.buildInterfaceSpecifier(input);
  }

  public static IpSpaceSpecifier getIpSpaceSpecifierOrDefault(
      @Nullable String input, IpSpaceSpecifier defaultSpecifier, IpSpaceSpecifierFactory factory) {
    return input == null || input.isEmpty()
        ? defaultSpecifier
        : factory.buildIpSpaceSpecifier(input);
  }

  public static LocationSpecifier getLocationSpecifierOrDefault(
      @Nullable String input,
      LocationSpecifier defaultSpecifier,
      LocationSpecifierFactory factory) {
    return input == null || input.isEmpty()
        ? defaultSpecifier
        : factory.buildLocationSpecifier(input);
  }

  public static NodeSpecifier getNodeSpecifierOrDefault(
      @Nullable String input, NodeSpecifier defaultSpecifier, NodeSpecifierFactory factory) {
    return input == null || input.isEmpty() ? defaultSpecifier : factory.buildNodeSpecifier(input);
  }
}
