package org.batfish.representation.juniper;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;

/** Represents Juniper nat pools */
@ParametersAreNonnullByDefault
public final class NatPool implements Serializable {

  private static final long serialVersionUID = 1L;

  private Ip _fromAddress;

  private Ip _toAddress;

  @Nullable private String _owner;

  // null value means no pat is configured; source nat will apply pat and dest nat will not apply
  // pat by default
  @Nullable private PortAddressTranslation _pat;

  public NatPool() {
    _fromAddress = Prefix.ZERO.getStartIp();
    _toAddress = Prefix.ZERO.getEndIp();
    _pat = null;
    _owner = null;
  }

  @Nonnull
  public Ip getFromAddress() {
    return _fromAddress;
  }

  @Nonnull
  public Ip getToAddress() {
    return _toAddress;
  }

  @Nullable
  public String getOwner() {
    return _owner;
  }

  @Nullable
  public PortAddressTranslation getPortAddressTranslation() {
    return _pat;
  }

  public void setFromAddress(Ip fromAddress) {
    _fromAddress = fromAddress;
  }

  public void setToAddress(Ip toAddress) {
    _toAddress = toAddress;
  }

  public void setPortAddressTranslation(PortAddressTranslation pat) {
    _pat = pat;
  }

  public void setOwner(String routingInstance) {
    _owner = routingInstance;
  }
}
