package org.batfish.representation.fortios;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.ConcreteInterfaceAddress;

/** FortiOS datamodel component containing interface configuration */
public final class Interface implements InterfaceOrZone, Serializable {
  public enum Type {
    AGGREGATE,
    EMAC_VLAN,
    LOOPBACK,
    PHYSICAL,
    REDUNDANT,
    TUNNEL,
    VLAN,
    WL_MESH;
  }

  public enum Speed {
    AUTO,
    TEN_FULL,
    TEN_HALF,
    HUNDRED_FULL,
    HUNDRED_HALF,
    THOUSAND_FULL,
    THOUSAND_HALF,
    TEN_THOUSAND_FULL,
    TEN_THOUSAND_HALF,
    HUNDRED_GFULL,
    HUNDRED_GHALF,
  }

  public enum Status {
    UP,
    DOWN,
    UNKNOWN,
  }

  public static final int DEFAULT_INTERFACE_MTU = 1500;
  public static final boolean DEFAULT_SECONDARY_IP_ENABLED = false;
  public static final Speed DEFAULT_SPEED = Speed.AUTO;
  public static final String DEFAULT_VDOM = "root";
  public static final int DEFAULT_VRF = 0;
  public static final Type DEFAULT_TYPE = Type.VLAN;
  public static final boolean DEFAULT_STATUS = true;

  @Override
  public <T> T accept(InterfaceOrZoneVisitor<T> visitor) {
    return visitor.visitInterface(this);
  }

  @Override
  @Nonnull
  public String getName() {
    return _name;
  }

  @Nullable
  public String getAlias() {
    return _alias;
  }

  @Nullable
  public String getVdom() {
    return _vdom;
  }

  @Nullable
  public ConcreteInterfaceAddress getIp() {
    return _ip;
  }

  public @Nullable Type getType() {
    return _type;
  }

  public @Nonnull Type getTypeEffective() {
    return firstNonNull(_type, DEFAULT_TYPE);
  }

  @VisibleForTesting
  public Status getStatus() {
    return _status;
  }

  /**
   * Get the effective status of the interface, inferring the value even if not explicitly
   * configured. If {@code true}, that interface is up, if {@code false} the interface is down.
   */
  public boolean getStatusEffective() {
    return _status == Status.UNKNOWN ? DEFAULT_STATUS : _status == Status.UP;
  }

  @VisibleForTesting
  @Nullable
  public Integer getMtu() {
    return _mtu;
  }

  /**
   * Get the effective mtu of the interface, inferring the value even if not explicitly configured.
   */
  public int getMtuEffective() {
    return _mtu == null || _mtuOverride == null || !_mtuOverride ? DEFAULT_INTERFACE_MTU : _mtu;
  }

  @VisibleForTesting
  @Nullable
  public Boolean getMtuOverride() {
    return _mtuOverride;
  }

  @Nullable
  public String getDescription() {
    return _description;
  }

  @Nullable
  public String getInterface() {
    return _interface;
  }

  @Nullable
  public Boolean getSecondaryIp() {
    return _secondaryIp;
  }

  /**
   * Get the effective secondaryip enabled-status of the interface, inferring the value even if not
   * explicitly configured.
   */
  public boolean getSecondaryIpEffective() {
    return firstNonNull(_secondaryIp, DEFAULT_SECONDARY_IP_ENABLED);
  }

  @Nonnull
  public Map<String, SecondaryIp> getSecondaryip() {
    return _secondaryip;
  }

  @Nullable
  public Speed getSpeed() {
    return _speed;
  }

  @Nonnull
  public Speed getSpeedEffective() {
    return firstNonNull(_speed, DEFAULT_SPEED);
  }

  @Nullable
  public Integer getVlanid() {
    return _vlanid;
  }

  @VisibleForTesting
  @Nullable
  public Integer getVrf() {
    return _vrf;
  }

  /**
   * Get the effective vrf of the interface, inferring the value even if not explicitly configured.
   */
  public int getVrfEffective() {
    return _vrf == null ? DEFAULT_VRF : _vrf;
  }

  public void setAlias(String alias) {
    _alias = alias;
  }

  public void setVdom(String vdom) {
    _vdom = vdom;
  }

  public void setIp(ConcreteInterfaceAddress ip) {
    _ip = ip;
  }

  public void setType(Type type) {
    _type = type;
  }

  public void setStatus(Status status) {
    _status = status;
  }

  public void setMtuOverride(boolean mtuOverride) {
    _mtuOverride = mtuOverride;
  }

  public void setMtu(int mtu) {
    _mtu = mtu;
  }

  public void setDescription(String description) {
    _description = description;
  }

  public void setInterface(String iface) {
    _interface = iface;
  }

  public void setSecondaryIp(Boolean secondaryIp) {
    _secondaryIp = secondaryIp;
  }

  public void setSpeed(Speed speed) {
    _speed = speed;
  }

  public void setVlanid(int vlanid) {
    _vlanid = vlanid;
  }

  public void setVrf(int vrf) {
    _vrf = vrf;
  }

  public Interface(String name) {
    _name = name;
    _status = Status.UNKNOWN;

    _secondaryip = new HashMap<>();
  }

  @Nonnull private final String _name;
  @Nullable private String _alias;
  @Nullable private String _vdom;
  @Nullable private ConcreteInterfaceAddress _ip;
  @Nullable private Type _type;
  @Nonnull private Status _status;
  @Nullable private Boolean _mtuOverride;
  @Nullable private Integer _mtu;
  @Nullable private String _description;
  @Nullable private String _interface;
  /** Boolean indicating if secondary-IP is enabled, i.e. if secondaryip can be populated */
  @Nullable private Boolean _secondaryIp;
  /** Map of name/number to {@code SecondaryIp} */
  @Nonnull private Map<String, SecondaryIp> _secondaryip;

  @Nullable private Speed _speed;
  @Nullable private Integer _vlanid;
  @Nullable private Integer _vrf;
}
