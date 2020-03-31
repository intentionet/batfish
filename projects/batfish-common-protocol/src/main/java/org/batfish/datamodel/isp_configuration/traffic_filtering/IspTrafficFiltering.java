package org.batfish.datamodel.isp_configuration.traffic_filtering;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A class that defines how an ISP will filter traffic into its network. */
public final class IspTrafficFiltering {
  /**
   * Describes how the ISP will block traffic. May be accompanied by a {@code ModeOptions
   * #getModeOptions()} function if the mode is parameterizable.
   */
  public enum Mode {
    /** The ISP will accept and forward all traffic. */
    NONE,
    /**
     * The ISP will block traffic to or from reserved IP addresses at the Internet-facing edge, in
     * both directions.
     */
    BLOCK_RESERVED_ADDRESSES_AT_INTERNET,
  }

  @JsonProperty(PROP_MODE)
  public @Nonnull Mode getMode() {
    return _mode;
  }

  public static @Nonnull IspTrafficFiltering none() {
    return new IspTrafficFiltering(Mode.NONE);
  }

  public static @Nonnull IspTrafficFiltering blockReservedAddressesAtInternet() {
    return new IspTrafficFiltering(Mode.BLOCK_RESERVED_ADDRESSES_AT_INTERNET);
  }

  // private implementation details

  // plan for extensibility:
  // - to add a new non-parameterized mode, add a new (documented) value to Mode and a new mode
  //   factory method.
  // - to add a new parameterized mode:
  //   1. add a new mode-specific FooOptions class.
  //   2. make the FooOptions class a new @Nullable field and argument of private constructor
  //   3. create a new public static mode-specific factory method taking a @Nonnull FooOptions.
  //   4. do the json work: add a new PROP_FOO_OPTIONS, add getter for it, update jsonCreator.
  private static final String PROP_MODE = "mode";

  private final Mode _mode;

  private IspTrafficFiltering(@Nonnull Mode mode) {
    _mode = mode;
  }

  @JsonCreator
  private static @Nonnull IspTrafficFiltering jsonCreator(
      @Nullable @JsonProperty(PROP_MODE) Mode mode) {
    checkArgument(mode != null, "Missing %s", PROP_MODE);
    switch (mode) {
      case NONE:
        return IspTrafficFiltering.none();
      case BLOCK_RESERVED_ADDRESSES_AT_INTERNET:
        return IspTrafficFiltering.blockReservedAddressesAtInternet();
      default:
        throw new UnsupportedOperationException("Unhandled mode " + mode);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof IspTrafficFiltering)) {
      return false;
    }
    IspTrafficFiltering that = (IspTrafficFiltering) o;
    return _mode == that._mode;
  }

  @Override
  public int hashCode() {
    return _mode.ordinal();
  }
}
