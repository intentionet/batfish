package org.batfish.representation.juniper;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.batfish.datamodel.BgpAuthenticationAlgorithm;
import org.batfish.datamodel.Ip;

public class BgpGroup implements Serializable {

  public enum BgpGroupType {
    EXTERNAL,
    INTERNAL
  }

  /** */
  private static final long serialVersionUID = 1L;

  private Boolean _advertiseExternal;

  private Boolean _advertiseInactive;

  private Boolean _advertisePeerAs;

  private BgpAuthenticationAlgorithm _authenticationAlgorithm;

  private String _authenticationKey;

  private String _authenticationKeyChainName;

  private Ip _clusterId;

  private String _description;

  private boolean _dynamic;

  private Boolean _ebgpMultihop;

  private Boolean _enforceFirstAs;

  private final Map<String, Integer> _exportPolicies;

  protected String _groupName;

  private final Map<String, Integer> _importPolicies;

  protected transient boolean _inherited;

  private boolean _ipv6;

  private Ip _localAddress;

  private Integer _localAs;

  private Integer _loops;

  private Boolean _multipath;

  private Boolean _multipathMultipleAs;

  private BgpGroup _parent;

  private Integer _peerAs;

  private Boolean _removePrivate;

  private BgpGroupType _type;

  public BgpGroup() {
    _exportPolicies = new LinkedHashMap<>();
    _importPolicies = new LinkedHashMap<>();
  }

  public final void cascadeInheritance() {
    if (_inherited) {
      return;
    }
    _inherited = true;
    if (_parent != null) {
      _parent.cascadeInheritance();
      if (_advertiseExternal == null) {
        _advertiseExternal = _parent._advertiseExternal;
      }
      if (_advertiseInactive == null) {
        _advertiseInactive = _parent._advertiseInactive;
      }
      if (_advertisePeerAs == null) {
        _advertisePeerAs = _parent._advertisePeerAs;
      }
      if (_authenticationAlgorithm == null) {
        _authenticationAlgorithm = _parent._authenticationAlgorithm;
      }
      if (_authenticationKey == null) {
        _authenticationKey = _parent._authenticationKey;
      }
      if (_authenticationKeyChainName == null) {
        _authenticationKeyChainName = _parent._authenticationKeyChainName;
      }
      if (_clusterId == null) {
        _clusterId = _parent._clusterId;
      }
      if (_description == null) {
        _description = _parent._description;
      }
      if (_enforceFirstAs == null) {
        _enforceFirstAs = _parent._enforceFirstAs;
      }
      if (_ebgpMultihop == null) {
        _ebgpMultihop = _parent._ebgpMultihop;
      }
      if (_exportPolicies.size() == 0) {
        _exportPolicies.putAll(_parent._exportPolicies);
      }
      if (_groupName == null) {
        _groupName = _parent._groupName;
      }
      if (_importPolicies.size() == 0) {
        _importPolicies.putAll(_parent._importPolicies);
      }
      if (_localAs == null) {
        _localAs = _parent._localAs;
      }
      if (_loops == null) {
        _loops = _parent._loops;
      }
      if (_localAddress == null) {
        _localAddress = _parent._localAddress;
      }
      if (_multipath == null) {
        _multipath = _parent._multipath;
      }
      if (_multipathMultipleAs == null) {
        _multipathMultipleAs = _parent._multipathMultipleAs;
      }
      if (_peerAs == null) {
        _peerAs = _parent._peerAs;
      }
      if (_type == null) {
        _type = _parent._type;
      }
    }
  }

  public Boolean getAdvertiseExternal() {
    return _advertiseExternal;
  }

  public Boolean getAdvertiseInactive() {
    return _advertiseInactive;
  }

  public Boolean getAdvertisePeerAs() {
    return _advertisePeerAs;
  }

  public BgpAuthenticationAlgorithm getAuthenticationAlgorithm() {
    return _authenticationAlgorithm;
  }

  public String getAuthenticationKey() {
    return _authenticationKey;
  }

  public String getAuthenticationKeyChainName() {
    return _authenticationKeyChainName;
  }

  public Ip getClusterId() {
    return _clusterId;
  }

  public final String getDescription() {
    return _description;
  }

  public boolean getDynamic() {
    return _dynamic;
  }

  public Boolean getEbgpMultihop() {
    return _ebgpMultihop;
  }

  public Boolean getEnforceFirstAs() {
    return _enforceFirstAs;
  }

  public final Map<String, Integer> getExportPolicies() {
    return _exportPolicies;
  }

  public String getGroupName() {
    return _groupName;
  }

  public final Map<String, Integer> getImportPolicies() {
    return _importPolicies;
  }

  public boolean getIpv6() {
    return _ipv6;
  }

  public final Ip getLocalAddress() {
    return _localAddress;
  }

  public final Integer getLocalAs() {
    return _localAs;
  }

  public Integer getLoops() {
    return _loops;
  }

  public Boolean getMultipath() {
    return _multipath;
  }

  public Boolean getMultipathMultipleAs() {
    return _multipathMultipleAs;
  }

  public final BgpGroup getParent() {
    return _parent;
  }

  public Integer getPeerAs() {
    return _peerAs;
  }

  public Boolean getRemovePrivate() {
    return _removePrivate;
  }

  public final BgpGroupType getType() {
    return _type;
  }

  public void setAdvertiseExternal(boolean advertiseExternal) {
    _advertiseExternal = advertiseExternal;
  }

  public void setAdvertiseInactive(boolean advertiseInactive) {
    _advertiseInactive = advertiseInactive;
  }

  public void setAdvertisePeerAs(boolean advertisePeerAs) {
    _advertisePeerAs = advertisePeerAs;
  }

  public void setAuthenticationAlgorithm(BgpAuthenticationAlgorithm authenticationAlgorithm) {
    _authenticationAlgorithm = authenticationAlgorithm;
  }

  public void setAuthenticationKey(String authenticationKey) {
    _authenticationKey = authenticationKey;
  }

  public void setAuthenticationKeyChainName(String authenticationKeyChainName) {
    _authenticationKeyChainName = authenticationKeyChainName;
  }

  public void setClusterId(Ip clusterId) {
    _clusterId = clusterId;
  }

  public final void setDescription(String description) {
    _description = description;
  }

  public void setDynamic(boolean dynamic) {
    this._dynamic = dynamic;
  }

  public void setEbgpMultihop(boolean ebgpMultihop) {
    _ebgpMultihop = ebgpMultihop;
  }

  public void setEnforceFirstAs(Boolean enforceFirstAs) {
    _enforceFirstAs = enforceFirstAs;
  }

  public void setIpv6(boolean ipv6) {
    _ipv6 = ipv6;
  }

  public final void setLocalAddress(Ip localAddress) {
    _localAddress = localAddress;
  }

  public final void setLocalAs(int localAs) {
    _localAs = localAs;
  }

  public void setLoops(int loops) {
    _loops = loops;
  }

  public void setMultipath(Boolean multipath) {
    _multipath = multipath;
  }

  public void setMultipathMultipleAs(Boolean multipathMultipleAs) {
    _multipathMultipleAs = multipathMultipleAs;
  }

  public final void setParent(BgpGroup parent) {
    _parent = parent;
  }

  public void setPeerAs(int peerAs) {
    _peerAs = peerAs;
  }

  public void setRemovePrivate(boolean removePrivate) {
    _removePrivate = removePrivate;
  }

  public final void setType(BgpGroupType type) {
    _type = type;
  }
}
