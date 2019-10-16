package org.batfish.representation.palo_alto;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.batfish.representation.palo_alto.RuleEndpoint.Type;

/** Represents destination-translation clause in Palo Alto NAT rule */
public class DestinationTranslation implements Serializable {

  @Nullable private final RuleEndpoint _translatedAddress;

  public DestinationTranslation(@Nullable RuleEndpoint ruleEndpoint) {
    checkArgument(
        ruleEndpoint.getType() != Type.Any, "Cannot use value any for translated-address");
    _translatedAddress = ruleEndpoint;
  }

  @Nullable
  public RuleEndpoint getTranslatedAddress() {
    return _translatedAddress;
  }
}
