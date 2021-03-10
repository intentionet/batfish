package org.batfish.representation.juniper;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.batfish.datamodel.BgpAuthenticationAlgorithm;
import org.batfish.datamodel.Ip;

public class BgpGroup implements Serializable {

  public enum BgpGroupType {
    EXTERNAL,
    INTERNAL
  }

  private Boolean _advertiseExternal;
  private Boolean _advertiseInactive;
  private Boolean _advertisePeerAs;
  private BgpAuthenticationAlgorithm _authenticationAlgorithm;
  private String _authenticationKey;
  private String _authenticationKeyChainName;
  private Ip _clusterId;
  private String _description;
  @Nullable private Boolean _disable;
  private boolean _dynamic;
  private Boolean _ebgpMultihop;
  private Boolean _enforceFirstAs;
  private final List<String> _exportPolicies;
  protected String _groupName;
  private final List<String> _importPolicies;
  protected transient boolean _inherited;
  private boolean _ipv6;
  private Ip _localAddress;
  private Long _localAs;
  private Integer _loops;
  private Boolean _multipath;
  private Boolean _multipathMultipleAs;
  private Boolean _noPrependGlobalAs;
  private BgpGroup _parent;
  private Long _peerAs;
  private @Nullable Integer _preference;
  private Boolean _removePrivate;
  @Nullable private String _ribGroup;
  private BgpGroupType _type;

  public BgpGroup() {
    _exportPolicies = new LinkedList<>();
    _importPolicies = new LinkedList<>();
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
      // Deliberately do not inherit description
      if (_disable == null) {
        _disable = _parent._disable;
      }
      if (_enforceFirstAs == null) {
        _enforceFirstAs = _parent._enforceFirstAs;
      }
      if (_ebgpMultihop == null) {
        _ebgpMultihop = _parent._ebgpMultihop;
      }
      if (_exportPolicies.isEmpty()) {
        _exportPolicies.addAll(_parent._exportPolicies);
      }
      if (_groupName == null) {
        _groupName = _parent._groupName;
      }
      if (_importPolicies.isEmpty()) {
        _importPolicies.addAll(_parent._importPolicies);
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
      if (_noPrependGlobalAs == null) {
        _noPrependGlobalAs = _parent._noPrependGlobalAs;
      }
      if (_peerAs == null) {
        _peerAs = _parent._peerAs;
      }
      if (_preference == null) {
        _preference = _parent._preference;
      }
      if (_ribGroup == null) {
        _ribGroup = _parent._ribGroup;
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

  @Nullable
  public Boolean getDisable() {
    return _disable;
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

  public final List<String> getExportPolicies() {
    return _exportPolicies;
  }

  public String getGroupName() {
    return _groupName;
  }

  public final List<String> getImportPolicies() {
    return _importPolicies;
  }

  public boolean getIpv6() {
    return _ipv6;
  }

  public final Ip getLocalAddress() {
    return _localAddress;
  }

  public final Long getLocalAs() {
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

  public Long getPeerAs() {
    return _peerAs;
  }

  public @Nullable Integer getPreference() {
    return _preference;
  }

  public void setPreference(@Nullable Integer preference) {
    _preference = preference;
  }

  public Boolean getRemovePrivate() {
    return _removePrivate;
  }

  @Nullable
  public String getRibGroup() {
    return _ribGroup;
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

  public void setDisable(boolean disable) {
    _disable = disable;
  }

  public void setDynamic(boolean dynamic) {
    _dynamic = dynamic;
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

  public final void setLocalAs(@Nullable Long localAs) {
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

  public @Nullable Boolean getNoPrependGlobalAs() {
    return _noPrependGlobalAs;
  }

  public void setNoPrependGlobalAs(@Nullable Boolean noPrependGlobalAs) {
    _noPrependGlobalAs = noPrependGlobalAs;
  }

  public final void setParent(BgpGroup parent) {
    _parent = parent;
  }

  public void setPeerAs(long peerAs) {
    _peerAs = peerAs;
  }

  public void setRemovePrivate(boolean removePrivate) {
    _removePrivate = removePrivate;
  }

  public void setRibGroup(@Nullable String ribGroup) {
    _ribGroup = ribGroup;
  }

  public final void setType(BgpGroupType type) {
    _type = type;
  }
}
