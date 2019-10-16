package org.batfish.representation.palo_alto;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.batfish.representation.palo_alto.RuleEndpoint.Type;

public class DynamicIpAndPort implements Serializable {

  @Nonnull private final List<RuleEndpoint> _translatedAddress;

  public DynamicIpAndPort() {
    _translatedAddress = new ArrayList<>();
  }

  public void addTranslatedAddress(@Nonnull RuleEndpoint ruleEndpoint) {
    checkArgument(
        ruleEndpoint.getType() != Type.Any, "Cannot use value any for translated-address");
    _translatedAddress.add(ruleEndpoint);
  }

  @Nonnull
  public List<RuleEndpoint> getTranslatedAddress() {
    return ImmutableList.copyOf(_translatedAddress);
  }
}
