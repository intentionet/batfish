package org.batfish.representation.cisco;

import java.io.Serializable;
import org.batfish.datamodel.Ip;

public abstract class BgpPeerGroup implements Serializable {

  private static final long serialVersionUID = 1L;

  protected Boolean _active;

  protected Boolean _additionalPathsReceive;

  protected Boolean _additionalPathsSelectAll;

  protected Boolean _additionalPathsSend;

  protected Boolean _advertiseInactive;

  protected Boolean _allowAsIn;

  protected Ip _clusterId;

  protected Integer _defaultMetric;

  protected Boolean _defaultOriginate;

  protected String _defaultOriginateMap;

  protected Integer _defaultOriginateMapLine;

  protected String _description;

  protected Boolean _disablePeerAsCheck;

  protected Boolean _ebgpMultihop;

  private String _groupName;

  private int _groupNameLine;

  protected String _inboundPrefixList;

  private String _inboundRoute6Map;

  protected String _inboundRouteMap;

  private transient boolean _inherited;

  private Long _localAs;

  private Integer _maximumPaths;

  private Integer _maximumPathsEbgp;

  private Integer _maximumPathsEibgp;

  private Integer _maximumPathsIbgp;

  private Boolean _nextHopSelf;

  protected String _outboundPrefixList;

  protected String _outboundRoute6Map;

  protected String _outboundRouteMap;

  private String _peerSession;

  private int _peerSessionLine;

  protected Long _remoteAs;

  protected Boolean _removePrivateAs;

  protected Boolean _routeReflectorClient;

  protected Boolean _sendCommunity;

  private boolean _sendExtendedCommunity;

  protected Boolean _shutdown;

  protected String _updateSource;

  public BgpPeerGroup() {}

  public Boolean getActive() {
    return _active;
  }

  public Boolean getAdditionalPathsReceive() {
    return _additionalPathsReceive;
  }

  public Boolean getAdditionalPathsSelectAll() {
    return _additionalPathsSelectAll;
  }

  public Boolean getAdditionalPathsSend() {
    return _additionalPathsSend;
  }

  public Boolean getAdvertiseInactive() {
    return _advertiseInactive;
  }

  public Boolean getAllowAsIn() {
    return _allowAsIn;
  }

  public Ip getClusterId() {
    return _clusterId;
  }

  public Integer getDefaultMetric() {
    return _defaultMetric;
  }

  public Boolean getDefaultOriginate() {
    return _defaultOriginate;
  }

  public String getDefaultOriginateMap() {
    return _defaultOriginateMap;
  }

  public Integer getDefaultOriginateMapLine() {
    return _defaultOriginateMapLine;
  }

  public String getDescription() {
    return _description;
  }

  public Boolean getDisablePeerAsCheck() {
    return _disablePeerAsCheck;
  }

  public Boolean getEbgpMultihop() {
    return _ebgpMultihop;
  }

  public String getGroupName() {
    return _groupName;
  }

  public int getGroupNameLine() {
    return _groupNameLine;
  }

  public String getInboundPrefixList() {
    return _inboundPrefixList;
  }

  public String getInboundRoute6Map() {
    return _inboundRoute6Map;
  }

  public String getInboundRouteMap() {
    return _inboundRouteMap;
  }

  public boolean getInherited() {
    return _inherited;
  }

  public Long getLocalAs() {
    return _localAs;
  }

  public Integer getMaximumPaths() {
    return _maximumPaths;
  }

  public Integer getMaximumPathsEbgp() {
    return _maximumPathsEbgp;
  }

  public Integer getMaximumPathsEibgp() {
    return _maximumPathsEibgp;
  }

  public Integer getMaximumPathsIbgp() {
    return _maximumPathsIbgp;
  }

  public abstract String getName();

  public Boolean getNextHopSelf() {
    return _nextHopSelf;
  }

  public String getOutboundPrefixList() {
    return _outboundPrefixList;
  }

  public String getOutboundRoute6Map() {
    return _outboundRoute6Map;
  }

  public String getOutboundRouteMap() {
    return _outboundRouteMap;
  }

  protected final BgpPeerGroup getParentGroup(BgpProcess proc, CiscoConfiguration cv) {
    BgpPeerGroup parent = null;
    if (_groupName != null) {
      parent = proc.getNamedPeerGroups().get(_groupName);
      BgpProcess defaultProc = cv.getDefaultVrf().getBgpProcess();
      if (parent == null) {
        parent = defaultProc.getNamedPeerGroups().get(_groupName);
      }
      if (parent == null) {
        cv.undefined(
            CiscoStructureType.BGP_PEER_GROUP,
            _groupName,
            CiscoStructureUsage.BGP_INHERITED_GROUP,
            _groupNameLine);
      }
    }
    return parent;
  }

  protected final BgpPeerGroup getParentSession(BgpProcess proc, CiscoConfiguration cv) {
    BgpPeerGroup parent = null;
    if (_peerSession != null) {
      parent = proc.getPeerSessions().get(_peerSession);
      BgpProcess defaultProc = cv.getDefaultVrf().getBgpProcess();
      if (parent == null) {
        parent = defaultProc.getPeerSessions().get(_peerSession);
      }
      if (parent == null) {
        cv.undefined(
            CiscoStructureType.BGP_PEER_GROUP,
            _peerSession,
            CiscoStructureUsage.BGP_INHERITED_SESSION,
            _peerSessionLine);
      }
    }
    if (parent == null) {
      parent = proc.getMasterBgpPeerGroup();
    }
    return parent;
  }

  public String getPeerSession() {
    return _peerSession;
  }

  public int getPeerSessionLine() {
    return _peerSessionLine;
  }

  public Long getRemoteAs() {
    return _remoteAs;
  }

  public Boolean getRemovePrivateAs() {
    return _removePrivateAs;
  }

  public Boolean getRouteReflectorClient() {
    return _routeReflectorClient;
  }

  public Boolean getSendCommunity() {
    return _sendCommunity;
  }

  public boolean getSendExtendedCommunity() {
    return _sendExtendedCommunity;
  }

  public Boolean getShutdown() {
    return _shutdown;
  }

  public String getUpdateSource() {
    return _updateSource;
  }

  private void inheritUnsetFields(BgpPeerGroup pg) {
    if (_active == null) {
      _active = pg.getActive();
    }
    if (_additionalPathsReceive == null) {
      _additionalPathsReceive = pg.getAdditionalPathsReceive();
    }
    if (_additionalPathsSelectAll == null) {
      _additionalPathsSelectAll = pg.getAdditionalPathsSelectAll();
    }
    if (_additionalPathsSend == null) {
      _additionalPathsSend = pg.getAdditionalPathsSend();
    }
    if (_advertiseInactive == null) {
      _advertiseInactive = pg.getAdvertiseInactive();
    }
    if (_allowAsIn == null) {
      _allowAsIn = pg.getAllowAsIn();
    }
    if (_clusterId == null) {
      _clusterId = pg.getClusterId();
    }
    if (_defaultOriginate == null) {
      _defaultOriginate = pg.getDefaultOriginate();
    }
    if (_defaultOriginateMap == null) {
      _defaultOriginateMap = pg.getDefaultOriginateMap();
      _defaultOriginateMapLine = pg.getDefaultOriginateMapLine();
    }
    if (_description == null) {
      _description = pg.getDescription();
    }
    if (_disablePeerAsCheck == null) {
      _disablePeerAsCheck = pg.getDisablePeerAsCheck();
    }
    if (_ebgpMultihop == null) {
      _ebgpMultihop = pg.getEbgpMultihop();
    }
    if (_inboundPrefixList == null) {
      _inboundPrefixList = pg.getInboundPrefixList();
    }
    if (_inboundRouteMap == null) {
      _inboundRouteMap = pg.getInboundRouteMap();
    }
    if (_inboundRoute6Map == null) {
      _inboundRoute6Map = pg.getInboundRoute6Map();
    }
    if (_localAs == null) {
      _localAs = pg.getLocalAs();
    }
    if (_maximumPaths == null) {
      _maximumPaths = pg.getMaximumPaths();
    }
    if (_maximumPathsEbgp == null) {
      _maximumPathsEbgp = pg.getMaximumPathsEbgp();
    }
    if (_maximumPathsEibgp == null) {
      _maximumPathsEibgp = pg.getMaximumPathsEibgp();
    }
    if (_maximumPathsIbgp == null) {
      _maximumPathsIbgp = pg.getMaximumPathsIbgp();
    }
    if (_nextHopSelf == null) {
      _nextHopSelf = pg.getNextHopSelf();
    }
    if (_outboundPrefixList == null) {
      _outboundPrefixList = pg.getOutboundPrefixList();
    }
    if (_outboundRouteMap == null) {
      _outboundRouteMap = pg.getOutboundRouteMap();
    }
    if (_outboundRoute6Map == null) {
      _outboundRoute6Map = pg.getOutboundRoute6Map();
    }
    if (_remoteAs == null) {
      _remoteAs = pg.getRemoteAs();
    }
    if (_removePrivateAs == null) {
      _removePrivateAs = pg.getRemovePrivateAs();
    }
    if (_routeReflectorClient == null) {
      _routeReflectorClient = pg.getRouteReflectorClient();
    }
    if (_sendCommunity == null) {
      _sendCommunity = pg.getSendCommunity();
    }
    if (_shutdown == null) {
      _shutdown = pg.getShutdown();
    }
    if (_updateSource == null) {
      _updateSource = pg.getUpdateSource();
    }
  }

  public void inheritUnsetFields(BgpProcess proc, CiscoConfiguration cv) {
    if (_inherited) {
      return;
    }
    _inherited = true;
    BgpPeerGroup aa = getParentGroup(proc, cv);
    if (aa != null) {
      aa.inheritUnsetFields(proc, cv);
      inheritUnsetFields(aa);
    }
    BgpPeerGroup pg = getParentSession(proc, cv);
    if (pg != null) {
      pg.inheritUnsetFields(proc, cv);
      inheritUnsetFields(pg);
    }
  }

  public void setActive(boolean active) {
    _active = active;
  }

  public void setAdditionalPathsReceive(Boolean additionalPathsReceive) {
    _additionalPathsReceive = additionalPathsReceive;
  }

  public void setAdditionalPathsSelectAll(Boolean additionalPathsSelectAll) {
    _additionalPathsSelectAll = additionalPathsSelectAll;
  }

  public void setAdditionalPathsSend(Boolean additionalPathsSend) {
    _additionalPathsSend = additionalPathsSend;
  }

  public void setAdvertiseInactive(boolean advertiseInactive) {
    _advertiseInactive = advertiseInactive;
  }

  public void setAllowAsIn(boolean allowAsIn) {
    _allowAsIn = allowAsIn;
  }

  public void setClusterId(Ip ip) {
    _clusterId = ip;
  }

  public void setDefaultMetric(int defaultMetric) {
    _defaultMetric = defaultMetric;
  }

  public void setDefaultOriginate(boolean b) {
    _defaultOriginate = b;
  }

  public void setDefaultOriginateMap(String routeMapName) {
    _defaultOriginateMap = routeMapName;
  }

  public void setDefaultOriginateMapLine(Integer defaultOriginateMapLine) {
    _defaultOriginateMapLine = defaultOriginateMapLine;
  }

  public void setDescription(String description) {
    _description = description;
  }

  public void setDisablePeerAsCheck(boolean disablePeerAsCheck) {
    _disablePeerAsCheck = disablePeerAsCheck;
  }

  public void setEbgpMultihop(boolean ebgpMultihop) {
    _ebgpMultihop = ebgpMultihop;
  }

  public void setGroupName(String groupName) {
    _groupName = groupName;
  }

  public void setGroupNameLine(int groupNameLine) {
    _groupNameLine = groupNameLine;
  }

  public void setInboundPrefixList(String inboundPrefixList) {
    _inboundPrefixList = inboundPrefixList;
  }

  public void setInboundRoute6Map(String inboundRoute6Map) {
    _inboundRoute6Map = inboundRoute6Map;
  }

  public void setInboundRouteMap(String name) {
    _inboundRouteMap = name;
  }

  public void setLocalAs(Long localAs) {
    _localAs = localAs;
  }

  public void setMaximumPaths(Integer maximumPaths) {
    _maximumPaths = maximumPaths;
  }

  public void setMaximumPathsEbgp(Integer maximumPathsEbgp) {
    _maximumPathsEbgp = maximumPathsEbgp;
  }

  public void setMaximumPathsEibgp(Integer maximumPathsEibgp) {
    _maximumPathsEibgp = maximumPathsEibgp;
  }

  public void setMaximumPathsIbgp(Integer maximumPathsIbgp) {
    _maximumPathsIbgp = maximumPathsIbgp;
  }

  public void setNextHopSelf(boolean nextHopSelf) {
    _nextHopSelf = nextHopSelf;
  }

  public void setOutboundPrefixList(String listName) {
    _outboundPrefixList = listName;
  }

  public void setOutboundRoute6Map(String outboundRoute6Map) {
    _outboundRoute6Map = outboundRoute6Map;
  }

  public void setOutboundRouteMap(String name) {
    _outboundRouteMap = name;
  }

  public void setPeerSession(String peerSession) {
    _peerSession = peerSession;
  }

  public void setPeerSessionLine(int peerSessionLine) {
    _peerSessionLine = peerSessionLine;
  }

  public void setRemoteAs(long remoteAS) {
    _remoteAs = remoteAS;
  }

  public void setRemovePrivateAs(boolean removePrivateAs) {
    _removePrivateAs = removePrivateAs;
  }

  public void setRouteReflectorClient(boolean b) {
    _routeReflectorClient = b;
  }

  public void setSendCommunity(boolean sendCommunity) {
    _sendCommunity = sendCommunity;
  }

  public void setSendExtendedCommunity(boolean sendExtendedCommunity) {
    _sendExtendedCommunity = sendExtendedCommunity;
  }

  public void setShutdown(boolean shutdown) {
    _shutdown = shutdown;
  }

  public void setUpdateSource(String updateSource) {
    _updateSource = updateSource;
  }
}
