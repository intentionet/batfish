package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.Pair;
import org.batfish.common.util.ComparableStructure;

public class RipNeighbor extends ComparableStructure<Pair<Ip, Ip>> {

  public static final class RipNeighborSummary extends ComparableStructure<String> {

    private static final String PROP_LOCAL_IP = "localIp";

    private static final String PROP_REMOTE_IP = "remoteIp";

    private static final String PROP_VRF = "vrf";

    /** */
    private static final long serialVersionUID = 1L;

    private final Ip _localIp;

    private final Ip _remoteIp;

    private final String _vrf;

    public RipNeighborSummary(RipNeighbor ripNeighbor) {
      super(ripNeighbor.getOwner().getHostname() + ":" + ripNeighbor._key);
      _localIp = ripNeighbor._key.getFirst();
      _remoteIp = ripNeighbor._key.getSecond();
      _vrf = ripNeighbor._vrf;
    }

    @JsonCreator
    public RipNeighborSummary(
        @JsonProperty(PROP_NAME) String name,
        @JsonProperty(PROP_LOCAL_IP) Ip localIp,
        @JsonProperty(PROP_REMOTE_IP) Ip remoteIp,
        @JsonProperty(PROP_VRF) String vrf) {
      super(name);
      _localIp = localIp;
      _remoteIp = remoteIp;
      _vrf = vrf;
    }

    @JsonProperty(PROP_LOCAL_IP)
    public Ip getLocalIp() {
      return _localIp;
    }

    @JsonProperty(PROP_REMOTE_IP)
    public Ip getRemoteIp() {
      return _remoteIp;
    }

    @JsonProperty(PROP_VRF)
    public String getVrf() {
      return _vrf;
    }
  }

  /** */
  private static final long serialVersionUID = 1L;

  private Interface _iface;

  private Configuration _owner;

  private transient RipNeighbor _remoteRipNeighbor;

  private String _vrf;

  public RipNeighbor(Pair<Ip, Ip> ipEdge) {
    super(ipEdge);
  }

  public Interface getIface() {
    return _iface;
  }

  public Pair<Ip, Ip> getIpEdge() {
    return _key;
  }

  @JsonIgnore
  public Ip getLocalIp() {
    return _key.getFirst();
  }

  @JsonIgnore
  public Configuration getOwner() {
    return _owner;
  }

  @JsonIgnore
  public Ip getRemoteIp() {
    return _key.getSecond();
  }

  @JsonIgnore
  public RipNeighbor getRemoteRipNeighbor() {
    return _remoteRipNeighbor;
  }

  public String getVrf() {
    return _vrf;
  }

  public void setIface(Interface iface) {
    _iface = iface;
  }

  public void setInterface(Interface iface) {
    _iface = iface;
  }

  public void setOwner(Configuration owner) {
    _owner = owner;
  }

  public void setRemoteRipNeighbor(RipNeighbor remoteRipNeighbor) {
    _remoteRipNeighbor = remoteRipNeighbor;
  }

  public void setVrf(String vrf) {
    _vrf = vrf;
  }
}
