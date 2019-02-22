package org.batfish.common.util.serialization;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Module for use by {@link com.fasterxml.jackson.databind.ObjectMapper} providing serialization
 * support for third-party classes used by Batfish.
 */
@ParametersAreNonnullByDefault
public final class BatfishThirdPartySerializationModule extends Module {

  @Override
  public @Nonnull String getModuleName() {
    return getClass().getSimpleName();
  }

  @Override
  public void setupModule(SetupContext context) {
    context.addDeserializers(new BatfishThirdPartyDeserializers());
    context.addSerializers(new BatfishThirdPartySerializers());
  }

  @Override
  public @Nonnull Version version() {
    return new Version(0, 0, 1, null, "org.batfish", "batfish-common-protocol");
  }
}
