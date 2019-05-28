package org.batfish.representation.palo_alto;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.IpSpace;

/** Represents a Palo Alto address object */
@ParametersAreNonnullByDefault
public final class AddressObject implements Serializable {
  private static final long serialVersionUID = 1L;

  @Nullable private String _description;

  @Nonnull private IpSpace _ipSpace;

  @Nonnull private final String _name;

  public AddressObject(String name) {
    _name = name;
    _ipSpace = EmptyIpSpace.INSTANCE;
  }

  @Nullable
  public String getDescription() {
    return _description;
  }

  @Nonnull
  public IpSpace getIpSpace() {
    return _ipSpace;
  }

  @Nonnull
  public String getName() {
    return _name;
  }

  public void setDescription(String description) {
    _description = description;
  }

  public void setIpSpace(IpSpace ipSpace) {
    _ipSpace = ipSpace;
  }
}
