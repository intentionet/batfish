package org.batfish.representation.cisco_nxos;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.InterfaceAddress;

/** Describes the configuration for an IP address assigned to an {@link Interface}. */
@ParametersAreNonnullByDefault
public class InterfaceAddressWithAttributes implements Serializable {
  public InterfaceAddressWithAttributes(InterfaceAddress address) {
    _address = address;
  }

  public @Nonnull InterfaceAddress getAddress() {
    return _address;
  }

  public @Nullable Long getTag() {
    return _tag;
  }

  public void setTag(@Nullable Long tag) {
    _tag = tag;
  }

  //////////////////////////////////////////////////
  ///// Private implementation details         /////
  //////////////////////////////////////////////////

  private final @Nonnull InterfaceAddress _address;
  private @Nullable Long _tag;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InterfaceAddressWithAttributes that = (InterfaceAddressWithAttributes) o;
    return _address.equals(that._address) && Objects.equals(_tag, that._tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_address, _tag);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("address", _address)
        .add("tag", _tag)
        .toString();
  }
}
