package org.batfish.common.util.isp;

import javax.annotation.Nullable;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.isp_configuration.traffic_filtering.IspTrafficFiltering.Mode;

/** Implementation of {@link Mode#NONE}. */
public final class NoTrafficFilteringPolicy implements IspTrafficFilteringPolicy {
  public static NoTrafficFilteringPolicy create() {
    return INSTANCE;
  }

  @Override
  public @Nullable IpAccessList filterTrafficFromInternet() {
    return null;
  }

  @Override
  public @Nullable IpAccessList filterTrafficToInternet() {
    return null;
  }

  @Override
  public @Nullable IpAccessList filterTrafficFromNetwork() {
    return null;
  }

  @Override
  public @Nullable IpAccessList filterTrafficToNetwork() {
    return null;
  }

  private static final NoTrafficFilteringPolicy INSTANCE = new NoTrafficFilteringPolicy();

  private NoTrafficFilteringPolicy() {}
}
