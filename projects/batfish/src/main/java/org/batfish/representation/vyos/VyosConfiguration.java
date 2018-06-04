package org.batfish.representation.vyos;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.batfish.common.BatfishException;
import org.batfish.common.VendorConversionException;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IkeGateway;
import org.batfish.datamodel.IkePolicy;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpsecPolicy;
import org.batfish.datamodel.IpsecProposal;
import org.batfish.datamodel.IpsecProtocol;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.vendor.VendorConfiguration;

public class VyosConfiguration extends VendorConfiguration {

  /** */
  private static final long serialVersionUID = 1L;

  private BgpProcess _bgpProcess;

  private Configuration _c;

  private final Map<String, EspGroup> _espGroups;

  private ConfigurationFormat _format;

  protected String _hostname;

  private final Map<String, IkeGroup> _ikeGroups;

  private final Map<String, Interface> _interfaces;

  private final Set<String> _ipsecInterfaces;

  private final Map<Ip, IpsecPeer> _ipsecPeers;

  private transient Map<Ip, org.batfish.datamodel.Interface> _ipToInterfaceMap;

  private final Map<String, PrefixList> _prefixLists;

  private final Map<String, RouteMap> _routeMaps;

  private final Set<StaticNextHopRoute> _staticNextHopRoutes;

  private transient Set<String> _unimplementedFeatures;

  public VyosConfiguration() {
    _espGroups = new TreeMap<>();
    _ikeGroups = new TreeMap<>();
    _interfaces = new TreeMap<>();
    _ipsecInterfaces = new TreeSet<>();
    _ipsecPeers = new TreeMap<>();
    _prefixLists = new TreeMap<>();
    _routeMaps = new TreeMap<>();
    _staticNextHopRoutes = new HashSet<>();
  }

  private void convertInterfaces() {
    for (Entry<String, Interface> e : _interfaces.entrySet()) {
      String name = e.getKey();
      Interface iface = e.getValue();
      org.batfish.datamodel.Interface newIface = toInterface(iface);
      _c.getDefaultVrf().getInterfaces().put(name, newIface);
    }
  }

  private void convertPrefixLists() {
    for (Entry<String, PrefixList> e : _prefixLists.entrySet()) {
      String name = e.getKey();
      PrefixList prefixList = e.getValue();
      RouteFilterList routeFilterList = toRouteFilterList(prefixList);
      _c.getRouteFilterLists().put(name, routeFilterList);
    }
  }

  private void convertRouteMaps() {
    for (Entry<String, RouteMap> e : _routeMaps.entrySet()) {
      String name = e.getKey();
      RouteMap routeMap = e.getValue();
      RoutingPolicy rp = toRoutingPolicy(routeMap);
      _c.getRoutingPolicies().put(name, rp);
    }
  }

  private void convertVpns() {
    for (Entry<Ip, IpsecPeer> ipsecPeerEntry : _ipsecPeers.entrySet()) {
      // create ipsecvpn and ikegateway to correspond roughly to vyos ipsec
      // site-to-site peer
      Ip peerAddress = ipsecPeerEntry.getKey();
      IpsecPeer ipsecPeer = ipsecPeerEntry.getValue();
      String newIpsecVpnName = peerAddress.toString();
      String newIkeGatewayName = newIpsecVpnName;
      IpsecVpn newIpsecVpn = new IpsecVpn(newIpsecVpnName, _c);
      _c.getIpsecVpns().put(newIpsecVpnName, newIpsecVpn);
      IkeGateway newIkeGateway = new IkeGateway(newIkeGatewayName);
      _c.getIkeGateways().put(newIkeGatewayName, newIkeGateway);
      newIpsecVpn.setIkeGateway(newIkeGateway);
      newIkeGateway.setLocalId(ipsecPeer.getAuthenticationId());
      newIkeGateway.setRemoteId(ipsecPeer.getAuthenticationRemoteId());
      newIkeGateway.setAddress(peerAddress);
      Ip localAddress = ipsecPeer.getLocalAddress();
      org.batfish.datamodel.Interface externalInterface = _ipToInterfaceMap.get(localAddress);
      if (externalInterface == null) {
        _w.redFlag(
            "Could not determine external interface for vpn \""
                + newIpsecVpnName
                + "\" from local-address: "
                + localAddress);
      } else {
        newIkeGateway.setExternalInterface(externalInterface);
      }

      // bind interface
      String bindInterfaceName = ipsecPeer.getBindInterface();
      org.batfish.datamodel.Interface newBindInterface =
          _c.getDefaultVrf().getInterfaces().get(bindInterfaceName);
      if (newBindInterface != null) {
        Interface bindInterface = _interfaces.get(bindInterfaceName);
        bindInterface
            .getReferers()
            .put(ipsecPeer, "bind interface for site-to-site peer \"" + newIpsecVpnName + "\"");
        newIpsecVpn.setBindInterface(newBindInterface);
      } else {
        _w.redFlag("Reference to undefined bind-interface: \"" + bindInterfaceName + "\"");
      }

      // convert the referenced ike group
      String ikeGroupName = ipsecPeer.getIkeGroup();
      IkeGroup ikeGroup = _ikeGroups.get(ikeGroupName);
      if (ikeGroup == null) {
        _w.redFlag("Reference to undefined ike-group: \"" + ikeGroupName + "\"");
      } else {
        ikeGroup
            .getReferers()
            .put(ipsecPeer, "ike group for site-to-site peer: \"" + newIpsecVpnName + "\"");
        IkePolicy newIkePolicy = new IkePolicy(ikeGroupName);
        _c.getIkePolicies().put(ikeGroupName, newIkePolicy);
        newIkeGateway.setIkePolicy(newIkePolicy);
        newIkePolicy.setPreSharedKeyHash(ipsecPeer.getAuthenticationPreSharedSecretHash());

        // convert contained ike proposals
        for (Entry<Integer, IkeProposal> ikeProposalEntry : ikeGroup.getProposals().entrySet()) {
          String newIkeProposalName =
              ikeGroupName + ":" + Integer.toString(ikeProposalEntry.getKey());
          IkeProposal ikeProposal = ikeProposalEntry.getValue();
          org.batfish.datamodel.IkeProposal newIkeProposal =
              new org.batfish.datamodel.IkeProposal(newIkeProposalName, -1);
          _c.getIkeProposals().put(newIkeProposalName, newIkeProposal);
          newIkePolicy.getProposals().put(newIkeProposalName, newIkeProposal);
          newIkeProposal.setDiffieHellmanGroup(ikeProposal.getDhGroup());
          newIkeProposal.setEncryptionAlgorithm(ikeProposal.getEncryptionAlgorithm());
          newIkeProposal.setLifetimeSeconds(ikeGroup.getLifetimeSeconds());
          newIkeProposal.setAuthenticationAlgorithm(
              ikeProposal.getHashAlgorithm().toIkeAuthenticationAlgorithm());
          newIkeProposal.setAuthenticationMethod(ipsecPeer.getAuthenticationMode());
        }
      }

      // convert the referenced esp group
      String espGroupName = ipsecPeer.getEspGroup();
      EspGroup espGroup = _espGroups.get(espGroupName);
      if (espGroup == null) {
        _w.redFlag("Reference to undefined esp-group: \"" + espGroupName + "\"");
      } else {
        espGroup
            .getReferers()
            .put(ipsecPeer, "esp-group for ipsec site-to-site peer: \"" + newIpsecVpnName + "\"");
        IpsecPolicy newIpsecPolicy = new IpsecPolicy(espGroupName);
        _c.getIpsecPolicies().put(espGroupName, newIpsecPolicy);
        newIpsecVpn.setIpsecPolicy(newIpsecPolicy);
        if (espGroup.getPfsSource() == null) {
          espGroup.setPfsSource(PfsSource.IKE_GROUP);
        }
        switch (espGroup.getPfsSource()) {
          case DISABLED:
            break;

          case ESP_GROUP:
            newIpsecPolicy.setPfsKeyGroup(espGroup.getPfsDhGroup());
            break;

          case IKE_GROUP:
            newIpsecPolicy.setPfsKeyGroupDynamicIke(true);
            break;

          default:
            throw new BatfishException("Invalid pfs source");
        }

        // convert contained esp proposals
        for (Entry<Integer, EspProposal> espProposalEntry : espGroup.getProposals().entrySet()) {
          String newIpsecProposalName =
              espGroupName + ":" + Integer.toString(espProposalEntry.getKey());
          EspProposal espProposal = espProposalEntry.getValue();
          IpsecProposal newIpsecProposal = new IpsecProposal(newIpsecProposalName, -1);
          _c.getIpsecProposals().put(newIpsecProposalName, newIpsecProposal);
          newIpsecPolicy.getProposals().put(newIpsecProposalName, newIpsecProposal);
          newIpsecProposal.setAuthenticationAlgorithm(
              espProposal.getHashAlgorithm().toIpsecAuthenticationAlgorithm());
          newIpsecProposal.setEncryptionAlgorithm(espProposal.getEncryptionAlgorithm());
          newIpsecProposal.setLifetimeSeconds(espGroup.getLifetimeSeconds());
          newIpsecProposal.setProtocol(IpsecProtocol.ESP);
        }
      }
    }
  }

  public BgpProcess getBgpProcess() {
    return _bgpProcess;
  }

  public Map<String, EspGroup> getEspGroups() {
    return _espGroups;
  }

  @Override
  public String getHostname() {
    return _hostname;
  }

  public Map<String, IkeGroup> getIkeGroups() {
    return _ikeGroups;
  }

  public Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public Set<String> getIpsecInterfaces() {
    return _ipsecInterfaces;
  }

  public Map<Ip, IpsecPeer> getIpsecPeers() {
    return _ipsecPeers;
  }

  public Map<String, PrefixList> getPrefixLists() {
    return _prefixLists;
  }

  public Map<String, RouteMap> getRouteMaps() {
    return _routeMaps;
  }

  public Set<StaticNextHopRoute> getStaticNextHopRoutes() {
    return _staticNextHopRoutes;
  }

  @Override
  public Set<String> getUnimplementedFeatures() {
    return _unimplementedFeatures;
  }

  public void setBgpProcess(BgpProcess bgpProcess) {
    _bgpProcess = bgpProcess;
  }

  @Override
  public void setHostname(String hostname) {
    _hostname = hostname;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    _format = format;
  }

  private org.batfish.datamodel.Interface toInterface(Interface iface) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface = new org.batfish.datamodel.Interface(name, _c);
    newIface.setDeclaredNames(ImmutableSortedSet.of(name));
    newIface.setActive(true); // TODO: may have to change
    newIface.setBandwidth(iface.getBandwidth());
    newIface.setDescription(iface.getDescription());
    InterfaceAddress address = iface.getAddress();
    if (address != null) {
      newIface.setAddress(iface.getAddress());
    }
    newIface.getAllAddresses().addAll(iface.getAllAddresses());
    for (InterfaceAddress p : newIface.getAllAddresses()) {
      _ipToInterfaceMap.put(p.getIp(), newIface);
    }
    return newIface;
  }

  private RouteFilterList toRouteFilterList(PrefixList prefixList) {
    String name = prefixList.getName();
    RouteFilterList newList = new RouteFilterList(name);
    List<RouteFilterLine> newLines =
        prefixList
            .getRules()
            .values()
            .stream()
            .map(l -> new RouteFilterLine(l.getAction(), l.getPrefix(), l.getLengthRange()))
            .collect(ImmutableList.toImmutableList());
    newList.setLines(newLines);
    return newList;
  }

  private RoutingPolicy toRoutingPolicy(RouteMap routeMap) {
    String name = routeMap.getName();
    RoutingPolicy routingPolicy = new RoutingPolicy(name, _c);
    List<Statement> statements = routingPolicy.getStatements();
    for (Entry<Integer, RouteMapRule> e : routeMap.getRules().entrySet()) {
      String ruleName = Integer.toString(e.getKey());
      RouteMapRule rule = e.getValue();
      If ifStatement = new If();
      List<Statement> trueStatements = ifStatement.getTrueStatements();
      ifStatement.setComment(ruleName);
      Conjunction conj = new Conjunction();
      for (RouteMapMatch match : rule.getMatches()) {
        conj.getConjuncts().add(match.toBooleanExpr(this, _c, _w));
      }
      ifStatement.setGuard(conj.simplify());
      switch (rule.getAction()) {
        case ACCEPT:
          trueStatements.add(Statements.ExitAccept.toStaticStatement());
          break;
        case REJECT:
          trueStatements.add(Statements.ExitReject.toStaticStatement());
          break;
        default:
          throw new BatfishException("Invalid action");
      }
      statements.add(ifStatement);
    }
    statements.add(Statements.ExitReject.toStaticStatement());
    return routingPolicy;
  }

  @Override
  public Configuration toVendorIndependentConfiguration() throws VendorConversionException {
    _ipToInterfaceMap = new HashMap<>();
    _c = new Configuration(_hostname, _format);
    _c.setDefaultCrossZoneAction(LineAction.ACCEPT);
    _c.setDefaultInboundAction(LineAction.ACCEPT);

    convertPrefixLists();
    convertRouteMaps();
    convertInterfaces();
    convertVpns();

    // TODO: convert routing processes

    warnAndDisableUnreferencedVtiInterfaces();

    return _c;
  }

  private void warnAndDisableUnreferencedVtiInterfaces() {
    for (Entry<String, Interface> ifaceEntry : _interfaces.entrySet()) {
      Interface iface = ifaceEntry.getValue();
      if (iface.getType() == InterfaceType.VTI && iface.isUnused()) {
        String name = ifaceEntry.getKey();
        _c.getDefaultVrf().getInterfaces().remove(name);
        _w.redFlag("Disabling unused VTI interface: \"" + name + "\"");
      }
    }
  }
}
