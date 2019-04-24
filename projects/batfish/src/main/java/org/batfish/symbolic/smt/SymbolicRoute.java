package org.batfish.symbolic.smt;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.batfish.symbolic.CommunityVar;
import org.batfish.symbolic.CommunityVar.Type;
import org.batfish.symbolic.IDeepCopy;
import org.batfish.symbolic.Protocol;

/**
 * A symbolic record of control plane message attributes. Attributes are specialized based on the
 * protocol, and which optimizations are applicable to the network.
 *
 * @author Ryan Beckett
 */
public class SymbolicRoute implements IDeepCopy<SymbolicRoute> {

  private String _name;

  private Protocol _proto;

  private EncoderSlice _enc;

  private boolean _isUsed;

  private boolean _isEnv;

  private boolean _isBest;

  private boolean _isBestOverall;

  private boolean _isExport;

  private BoolExpr _permitted;

  private ArithExpr _prefixLength;

  private ArithExpr _adminDist;

  private ArithExpr _metric;

  private ArithExpr _med;

  private ArithExpr _localPref;

  private BoolExpr _bgpInternal;

  private ArithExpr _igpMetric;

  private SymbolicOriginatorId _clientId;

  private SymbolicEnum<Long> _ospfArea;

  private SymbolicOspfType _ospfType;

  private ArithExpr _routerId;

  private SymbolicEnum<Protocol> _protocolHistory;

  private Map<CommunityVar, BoolExpr> _communities;

  SymbolicRoute(String name, Protocol proto) {
    _name = name;
    _proto = proto;
    _enc = null;
    _isUsed = false;
    _isBest = false;
    _isBestOverall = false;
    _isEnv = false;
    _isExport = false;
    _prefixLength = null;
    _metric = null;
    _adminDist = null;
    _med = null;
    _localPref = null;
    _bgpInternal = null;
    _clientId = null;
    _igpMetric = null;
    _routerId = null;
    _permitted = null;
    _ospfArea = null;
    _ospfType = null;
    _protocolHistory = null;
  }

  /*
   * Copy constructor used to create the SSA form.
   * To avoid changing values along different If branches,
   * we must create a new copy of several variables that can change.
   */
  SymbolicRoute(SymbolicRoute other) {
    _name = other._name;
    _proto = other._proto;
    _enc = other._enc;
    _isUsed = other._isUsed;
    _isBest = other._isBest;
    _isBestOverall = other._isBestOverall;
    _isEnv = other._isEnv;
    _isExport = other._isExport;
    _prefixLength = other._prefixLength;
    _metric = other._metric;
    _adminDist = other._adminDist;
    _med = other._med;
    _localPref = other._localPref;
    _bgpInternal = other._bgpInternal;
    _clientId = other._clientId;
    _igpMetric = other._igpMetric;
    _routerId = other._routerId;
    _permitted = other._permitted;
    _ospfArea = other._ospfArea;
    _ospfType = (other._ospfType == null ? null : new SymbolicOspfType(other._ospfType));
    _protocolHistory = other._protocolHistory;
    _communities =
        new HashMap<>(other._communities); // TODO: use a persistent map to avoid this penalty
  }

  SymbolicRoute(
      EncoderSlice slice,
      String name,
      String router,
      Protocol proto,
      Optimizations opts,
      @Nullable SymbolicEnum<Protocol> h,
      boolean isAbstract) {

    _name = name;
    _proto = proto;
    _enc = slice;
    _isUsed = true;
    _isExport = _name.contains("EXPORT");
    _isEnv = _name.contains("_ENV-");
    _isBest = _name.contains("_BEST");
    _isBestOverall = (_isBest && _name.contains("_OVERALL"));

    Context ctx = slice.getCtx();

    boolean hasOspf = slice.getProtocols().get(router).contains(Protocol.OSPF);
    boolean hasBgp = slice.getProtocols().get(router).contains(Protocol.BGP);
    boolean multipleProtos = slice.getProtocols().get(router).size() > 1;
    boolean modelAd = !_isEnv && ((_isBestOverall && multipleProtos) || opts.getKeepAdminDist());
    boolean modelIbgp = slice.isMainSlice() && opts.getNeedBgpInternal().contains(router);
    boolean modelLp =
        slice.isMainSlice()
            && !_isEnv
            && opts.getKeepLocalPref()
            && (proto.isBgp() || _isBestOverall);

    _protocolHistory = h;
    _ospfArea = null;
    _ospfType = null;
    _igpMetric = null;

    if (proto.isBest()) {
      _metric = ctx.mkIntConst(_name + "_metric");
      _localPref = (modelLp ? ctx.mkIntConst(_name + "_localPref") : null);
      _adminDist = (modelAd ? ctx.mkIntConst(_name + "_adminDist") : null);
      _med = (opts.getKeepMed() ? ctx.mkIntConst(_name + "_med") : null);
      _bgpInternal = (modelIbgp ? ctx.mkBoolConst(_name + "_bgpInternal") : null);
      _igpMetric = (modelIbgp ? ctx.mkIntConst(_name + "_igpMetric") : null);

      if (hasOspf && opts.getKeepOspfType()) {
        _ospfType = new SymbolicOspfType(slice, _name + "_ospfType");
      }

      // Set OSPF area only for best OSPF or OVERALL choice
      if (hasOspf && (_isBestOverall || _name.contains("_OSPF_"))) {
        List<Long> areaIds = new ArrayList<>(slice.getGraph().getAreaIds().get(router));
        if (areaIds.size() > 1) {
          _ospfArea = new SymbolicEnum<>(slice, areaIds, _name + "_ospfArea");
        }
      }

    } else if (proto.isConnected()) {
      _metric = null;
      _localPref = null;
      _adminDist = null;
      _med = null;
      _bgpInternal = null;
      _ospfArea = null;
      _ospfType = null;

    } else if (proto.isStatic()) {
      _metric = null;
      _localPref = null;
      _adminDist = null;
      _med = null;
      _bgpInternal = null;
      _ospfArea = null;
      _ospfType = null;

    } else if (proto.isBgp()) {
      _metric = ctx.mkIntConst(_name + "_metric");
      _localPref = (modelLp ? ctx.mkIntConst(_name + "_localPref") : null);
      _adminDist = (modelAd ? ctx.mkIntConst(_name + "_adminDist") : null);
      _med = (opts.getKeepMed() ? ctx.mkIntConst(_name + "_med") : null);
      _bgpInternal = (modelIbgp ? ctx.mkBoolConst(_name + "_bgpInternal") : null);
      _igpMetric =
          ((_isBest && modelIbgp) || (isAbstract && !_isExport)
              ? ctx.mkIntConst(_name + "_igpMetric")
              : null);
      _ospfArea = null;
      _ospfType = null;

    } else if (proto.isOspf()) {
      _metric = ctx.mkIntConst(_name + "_metric");
      _localPref = (modelLp ? ctx.mkIntConst(_name + "_localPref") : null);
      _adminDist = (modelAd ? ctx.mkIntConst(_name + "_adminDist") : null);
      _med = null;
      _bgpInternal = null;

      if (opts.getKeepOspfType()) {
        _ospfType = new SymbolicOspfType(slice, _name + "_ospfType");
      }
    }

    boolean bestAndNeedId =
        (_isBestOverall || _isBest && proto.isBgp()) && opts.getNeedRouterId().contains(router);
    if (bestAndNeedId && _enc.isMainSlice()) {
      _routerId = ctx.mkIntConst(_name + "_routerID");
    } else {
      _routerId = null;
    }

    _prefixLength = ctx.mkIntConst(_name + "_prefixLength");
    _permitted = ctx.mkBoolConst(_name + "_permitted");

    _communities = new HashMap<>();
    boolean usesBgp = (proto.isBgp() || (hasBgp && proto.isBest()));
    if (usesBgp) {
      Set<CommunityVar> allComms = slice.getAllCommunities();
      for (CommunityVar cvar : allComms) {
        // TODO: if neighbor doesn't need regex match, then don't keep it on export
        if (cvar.getType() == Type.REGEX && !_isExport) {
          continue;
        }
        String s = cvar.getRegex();
        if (cvar.getType() == CommunityVar.Type.OTHER) {
          s = s + "_OTHER";
        }
        BoolExpr var = ctx.mkBoolConst(_name + "_community_" + s);
        _communities.put(cvar, var);
      }
    }

    // client id
    if (usesBgp && opts.getNeedOriginatorIds()) {
      _clientId = new SymbolicOriginatorId(slice, _name + "_clientId");
    }

    addExprs(slice);
  }

  // TODO: depends on configuration
  /*
   * Check if a particular protocol on a router is configured
   * to use multipath routing or not.
   */
  /* private boolean isMultipath(String router, Protocol proto, boolean isIbgp) {
    Configuration conf = _enc.getGraph().getConfigurations().get(router);
    if (proto.isConnected()) {
      return true;
    } else if (proto.isStatic()) {
      return true;
    } else if (proto.isOspf()) {
      return true;
    } else if (proto.isBgp()) {
      BgpProcess p = conf.getDefaultVrf().getBgpProcess();
      if (isIbgp) {
        return p.getMultipathIbgp();
      } else {
        return p.getMultipathEbgp();
      }
    } else {
      return true;
    }
  } */

  private void addExprs(EncoderSlice enc) {
    Map<String, Expr> all = enc.getAllVariables();

    all.put(_permitted.toString(), _permitted);
    if (_adminDist != null) {
      all.put(_adminDist.toString(), _adminDist);
    }
    if (_med != null) {
      all.put(_med.toString(), _med);
    }
    if (_localPref != null) {
      all.put(_localPref.toString(), _localPref);
    }
    if (_metric != null) {
      all.put(_metric.toString(), _metric);
    }
    if (_prefixLength != null) {
      all.put(_prefixLength.toString(), _prefixLength);
    }
    if (_routerId != null) {
      all.put(_routerId.toString(), _routerId);
    }
    if (_bgpInternal != null) {
      all.put(_bgpInternal.toString(), _bgpInternal);
    }
    if (_ospfArea != null) {
      all.put(_ospfArea.getBitVec().toString(), _ospfArea.getBitVec());
    }
    if (_ospfType != null) {
      all.put(_ospfType.getBitVec().toString(), _ospfType.getBitVec());
    }
    if (_clientId != null) {
      all.put(_clientId.getBitVec().toString(), _clientId.getBitVec());
    }
    if (_igpMetric != null) {
      all.put(_igpMetric.toString(), _igpMetric);
    }
    for (BoolExpr var : _communities.values()) {
      all.put(var.toString(), var);
    }
  }

  boolean getIsUsed() {
    return _isUsed;
  }

  String getName() {
    return _name;
  }

  boolean isBest() {
    return _isBest;
  }

  boolean isEnv() {
    return _isEnv;
  }

  BoolExpr getPermitted() {
    return _permitted;
  }

  public void setPermitted(BoolExpr permitted) {
    this._permitted = permitted;
  }

  ArithExpr getMetric() {
    return _metric;
  }

  public void setMetric(ArithExpr metric) {
    this._metric = metric;
  }

  ArithExpr getLocalPref() {
    return _localPref;
  }

  public void setLocalPref(ArithExpr localPref) {
    this._localPref = localPref;
  }

  ArithExpr getAdminDist() {
    return _adminDist;
  }

  public void setAdminDist(ArithExpr adminDist) {
    this._adminDist = adminDist;
  }

  ArithExpr getMed() {
    return _med;
  }

  public void setMed(ArithExpr med) {
    this._med = med;
  }

  ArithExpr getRouterId() {
    return _routerId;
  }

  ArithExpr getPrefixLength() {
    return _prefixLength;
  }

  public void setPrefixLength(ArithExpr prefixLength) {
    this._prefixLength = prefixLength;
  }

  SymbolicEnum<Long> getOspfArea() {
    return _ospfArea;
  }

  public void setOspfArea(SymbolicEnum<Long> ospfArea) {
    this._ospfArea = ospfArea;
  }

  SymbolicOspfType getOspfType() {
    return _ospfType;
  }

  public void setOspfType(SymbolicOspfType ospfType) {
    this._ospfType = ospfType;
  }

  Map<CommunityVar, BoolExpr> getCommunities() {
    return _communities;
  }

  public void setCommunities(Map<CommunityVar, BoolExpr> communities) {
    this._communities = communities;
  }

  SymbolicEnum<Protocol> getProtocolHistory() {
    return _protocolHistory;
  }

  public SymbolicOriginatorId getClientId() {
    return _clientId;
  }

  public void setClientId(SymbolicOriginatorId clientId) {
    this._clientId = clientId;
  }

  BoolExpr getBgpInternal() {
    return _bgpInternal;
  }

  public void setBgpInternal(BoolExpr bgpInternal) {
    this._bgpInternal = bgpInternal;
  }

  public ArithExpr getIgpMetric() {
    return _igpMetric;
  }

  public void setIgpMetric(ArithExpr igpMetric) {
    this._igpMetric = igpMetric;
  }

  Protocol getProto() {
    return _proto;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SymbolicRoute that = (SymbolicRoute) o;

    return this._name.equals(that._name);
  }

  @Override
  public int hashCode() {
    return _name.hashCode();
  }

  public SymbolicRoute copy() {
    return new SymbolicRoute(this);
  }

  @Override
  public SymbolicRoute deepCopy() {
    return new SymbolicRoute(this);
  }
}
