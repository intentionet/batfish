package org.batfish.representation.aws;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.IkeAuthenticationAlgorithm;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeGateway;
import org.batfish.datamodel.IkePolicy;
import org.batfish.datamodel.IkeProposal;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpsecAuthenticationAlgorithm;
import org.batfish.datamodel.IpsecPolicy;
import org.batfish.datamodel.IpsecProposal;
import org.batfish.datamodel.IpsecProtocol;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class VpnConnection implements AwsVpcEntity, Serializable {

  private static final int BGP_NEIGHBOR_DEFAULT_METRIC = 0;

  private static final long serialVersionUID = 1L;

  private static DiffieHellmanGroup toDiffieHellmanGroup(String perfectForwardSecrecy) {
    switch (perfectForwardSecrecy) {
      case "group2":
        return DiffieHellmanGroup.GROUP2;
      default:
        throw new BatfishException(
            "No conversion to Diffie-Hellman group for string: \"" + perfectForwardSecrecy + "\"");
    }
  }

  private static EncryptionAlgorithm toEncryptionAlgorithm(String encryptionProtocol) {
    switch (encryptionProtocol) {
      case "aes-128-cbc":
        return EncryptionAlgorithm.AES_128_CBC;
      default:
        throw new BatfishException(
            "No conversion to encryption algorithm for string: \"" + encryptionProtocol + "\"");
    }
  }

  private static IkeAuthenticationAlgorithm toIkeAuthenticationAlgorithm(String ikeAuthProtocol) {
    switch (ikeAuthProtocol) {
      case "sha1":
        return IkeAuthenticationAlgorithm.SHA1;

      default:
        throw new BatfishException(
            "No conversion to ike authentication algorithm for string: \""
                + ikeAuthProtocol
                + "\"");
    }
  }

  private static IpsecAuthenticationAlgorithm toIpsecAuthenticationAlgorithm(
      String ipsecAuthProtocol) {
    switch (ipsecAuthProtocol) {
      case "hmac-sha1-96":
        return IpsecAuthenticationAlgorithm.HMAC_SHA1_96;
      default:
        throw new BatfishException(
            "No conversion to ipsec authentication algorithm for string: \""
                + ipsecAuthProtocol
                + "\"");
    }
  }

  private static IpsecProtocol toIpsecProtocol(String ipsecProtocol) {
    switch (ipsecProtocol) {
      case "esp":
        return IpsecProtocol.ESP;
      default:
        throw new BatfishException(
            "No conversion to ipsec protocol for string: \"" + ipsecProtocol + "\"");
    }
  }

  private final String _customerGatewayId;

  private final List<IpsecTunnel> _ipsecTunnels;

  private final List<Prefix> _routes;

  private final boolean _staticRoutesOnly;

  private final List<VgwTelemetry> _vgwTelemetrys;

  private final String _vpnConnectionId;

  private final String _vpnGatewayId;

  public VpnConnection(JSONObject jObj, BatfishLogger logger) throws JSONException {
    _vgwTelemetrys = new LinkedList<>();
    _routes = new LinkedList<>();
    _ipsecTunnels = new LinkedList<>();
    _vpnConnectionId = jObj.getString(JSON_KEY_VPN_CONNECTION_ID);
    _customerGatewayId = jObj.getString(JSON_KEY_CUSTOMER_GATEWAY_ID);
    _vpnGatewayId = jObj.getString(JSON_KEY_VPN_GATEWAY_ID);

    logger.debugf("parsing vpnconnection id: %s\n", _vpnConnectionId);

    String cgwConfiguration = jObj.getString(JSON_KEY_CUSTOMER_GATEWAY_CONFIGURATION);
    Document document;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(cgwConfiguration));
      document = builder.parse(is);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new BatfishException(
          "Could not parse XML for CustomerGatewayConfiguration for vpn connection "
              + _vpnConnectionId
              + " "
              + e);
    }

    Element vpnConnection = (Element) document.getElementsByTagName(XML_KEY_VPN_CONNECTION).item(0);

    NodeList nodeList = document.getElementsByTagName(XML_KEY_IPSEC_TUNNEL);

    for (int index = 0; index < nodeList.getLength(); index++) {
      Element ipsecTunnel = (Element) nodeList.item(index);
      _ipsecTunnels.add(new IpsecTunnel(ipsecTunnel, vpnConnection));
    }

    if (jObj.has(JSON_KEY_ROUTES)) {
      JSONArray routes = jObj.getJSONArray(JSON_KEY_ROUTES);
      for (int index = 0; index < routes.length(); index++) {
        JSONObject childObject = routes.getJSONObject(index);
        _routes.add(Prefix.parse(childObject.getString(JSON_KEY_DESTINATION_CIDR_BLOCK)));
      }
    }

    JSONArray vgwTelemetry = jObj.getJSONArray(JSON_KEY_VGW_TELEMETRY);
    for (int index = 0; index < vgwTelemetry.length(); index++) {
      JSONObject childObject = vgwTelemetry.getJSONObject(index);
      _vgwTelemetrys.add(new VgwTelemetry(childObject, logger));
    }

    if (jObj.has(JSON_KEY_OPTIONS)) {
      JSONObject options = jObj.getJSONObject(JSON_KEY_OPTIONS);
      _staticRoutesOnly = Utils.tryGetBoolean(options, JSON_KEY_STATIC_ROUTES_ONLY, false);
    } else {
      _staticRoutesOnly = false;
    }
  }

  public void applyToVpnGateway(
      AwsConfiguration awsConfiguration, Region region, Warnings warnings) {
    if (!awsConfiguration.getConfigurationNodes().containsKey(_vpnGatewayId)) {
      warnings.redFlag(
          String.format(
              "VPN Gateway \"%s\" referred by VPN connection \"%s\" not found",
              _vpnGatewayId, _vpnConnectionId));
      return;
    }
    Configuration vpnGatewayCfgNode = awsConfiguration.getConfigurationNodes().get(_vpnGatewayId);
    for (int i = 0; i < _ipsecTunnels.size(); i++) {
      int idNum = i + 1;
      String vpnId = _vpnConnectionId + "-" + idNum;
      IpsecTunnel ipsecTunnel = _ipsecTunnels.get(i);
      if (ipsecTunnel.getCgwBgpAsn() != -1 && (_staticRoutesOnly || _routes.size() != 0)) {
        throw new BatfishException(
            "Unexpected combination of BGP and static routes for VPN connection: \""
                + _vpnConnectionId
                + "\"");
      }
      // create representation structures and add to configuration node
      IpsecVpn ipsecVpn = new IpsecVpn(vpnId, vpnGatewayCfgNode);
      vpnGatewayCfgNode.getIpsecVpns().put(vpnId, ipsecVpn);
      IpsecPolicy ipsecPolicy = new IpsecPolicy(vpnId);
      vpnGatewayCfgNode.getIpsecPolicies().put(vpnId, ipsecPolicy);
      ipsecVpn.setIpsecPolicy(ipsecPolicy);
      IpsecProposal ipsecProposal = new IpsecProposal(vpnId, -1);
      vpnGatewayCfgNode.getIpsecProposals().put(vpnId, ipsecProposal);
      ipsecPolicy.getProposals().put(vpnId, ipsecProposal);
      IkeGateway ikeGateway = new IkeGateway(vpnId);
      vpnGatewayCfgNode.getIkeGateways().put(vpnId, ikeGateway);
      ipsecVpn.setIkeGateway(ikeGateway);
      IkePolicy ikePolicy = new IkePolicy(vpnId);
      vpnGatewayCfgNode.getIkePolicies().put(vpnId, ikePolicy);
      ikeGateway.setIkePolicy(ikePolicy);
      IkeProposal ikeProposal = new IkeProposal(vpnId, -1);
      vpnGatewayCfgNode.getIkeProposals().put(vpnId, ikeProposal);
      ikePolicy.getProposals().put(vpnId, ikeProposal);
      String externalInterfaceName = "external" + idNum;
      InterfaceAddress externalInterfaceAddress =
          new InterfaceAddress(ipsecTunnel.getVgwOutsideAddress(), Prefix.MAX_PREFIX_LENGTH);
      Interface externalInterface =
          Utils.newInterface(externalInterfaceName, vpnGatewayCfgNode, externalInterfaceAddress);

      String vpnInterfaceName = "vpn" + idNum;
      InterfaceAddress vpnInterfaceAddress =
          new InterfaceAddress(
              ipsecTunnel.getVgwInsideAddress(), ipsecTunnel.getVgwInsidePrefixLength());
      Interface vpnInterface =
          Utils.newInterface(vpnInterfaceName, vpnGatewayCfgNode, vpnInterfaceAddress);

      // Set fields within representation structures

      // ipsec
      ipsecVpn.setBindInterface(vpnInterface);
      ipsecPolicy.setPfsKeyGroup(toDiffieHellmanGroup(ipsecTunnel.getIpsecPerfectForwardSecrecy()));
      ipsecProposal.setAuthenticationAlgorithm(
          toIpsecAuthenticationAlgorithm(ipsecTunnel.getIpsecAuthProtocol()));
      ipsecProposal.setEncryptionAlgorithm(
          toEncryptionAlgorithm(ipsecTunnel.getIpsecEncryptionProtocol()));
      ipsecProposal.setProtocol(toIpsecProtocol(ipsecTunnel.getIpsecProtocol()));
      ipsecProposal.setLifetimeSeconds(ipsecTunnel.getIpsecLifetime());

      // ike
      ikeGateway.setExternalInterface(externalInterface);
      ikeGateway.setAddress(ipsecTunnel.getCgwOutsideAddress());
      ikeGateway.setLocalIp(externalInterface.getAddress().getIp());
      if (ipsecTunnel.getIkePreSharedKeyHash() != null) {
        ikePolicy.setPreSharedKeyHash(ipsecTunnel.getIkePreSharedKeyHash());
        ikeProposal.setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS);
      }
      ikeProposal.setAuthenticationAlgorithm(
          toIkeAuthenticationAlgorithm(ipsecTunnel.getIkeAuthProtocol()));
      ikeProposal.setDiffieHellmanGroup(
          toDiffieHellmanGroup(ipsecTunnel.getIkePerfectForwardSecrecy()));
      ikeProposal.setEncryptionAlgorithm(
          toEncryptionAlgorithm(ipsecTunnel.getIkeEncryptionProtocol()));
      ikeProposal.setLifetimeSeconds(ipsecTunnel.getIkeLifetime());

      // bgp (if configured)
      if (ipsecTunnel.getVgwBgpAsn() != -1) {
        BgpProcess proc = vpnGatewayCfgNode.getDefaultVrf().getBgpProcess();
        if (proc == null) {
          proc = new BgpProcess();
          proc.setRouterId(ipsecTunnel.getVgwInsideAddress());
          proc.setMultipathEquivalentAsPathMatchMode(MultipathEquivalentAsPathMatchMode.EXACT_PATH);
          vpnGatewayCfgNode.getDefaultVrf().setBgpProcess(proc);
        }
        BgpNeighbor cgBgpNeighbor =
            new BgpNeighbor(ipsecTunnel.getCgwInsideAddress(), vpnGatewayCfgNode, false);
        cgBgpNeighbor.setVrf(Configuration.DEFAULT_VRF_NAME);
        proc.getNeighbors().put(cgBgpNeighbor.getPrefix(), cgBgpNeighbor);
        cgBgpNeighbor.setRemoteAs(ipsecTunnel.getCgwBgpAsn());
        cgBgpNeighbor.setLocalAs(ipsecTunnel.getVgwBgpAsn());
        cgBgpNeighbor.setLocalIp(ipsecTunnel.getVgwInsideAddress());
        cgBgpNeighbor.setDefaultMetric(BGP_NEIGHBOR_DEFAULT_METRIC);
        cgBgpNeighbor.setSendCommunity(false);

        VpnGateway vpnGateway = region.getVpnGateways().get(_vpnGatewayId);
        List<String> attachmentVpcIds = vpnGateway.getAttachmentVpcIds();
        if (attachmentVpcIds.size() != 1) {
          throw new BatfishException(
              "Not sure what routes to advertise since VPN Gateway: \""
                  + _vpnGatewayId
                  + "\" for VPN connection: \""
                  + _vpnConnectionId
                  + "\" is linked to multiple VPCs");
        }
        String vpcId = attachmentVpcIds.get(0);

        // iBGP connection to VPC
        Configuration vpcNode = awsConfiguration.getConfigurationNodes().get(vpcId);
        Ip vpcIfaceAddress = vpcNode.getInterfaces().get(_vpnGatewayId).getAddress().getIp();
        Ip vgwToVpcIfaceAddress = vpnGatewayCfgNode.getInterfaces().get(vpcId).getAddress().getIp();
        BgpNeighbor vgwToVpcBgpNeighbor =
            new BgpNeighbor(vpcIfaceAddress, vpnGatewayCfgNode, false);
        proc.getNeighbors().put(vgwToVpcBgpNeighbor.getPrefix(), vgwToVpcBgpNeighbor);
        vgwToVpcBgpNeighbor.setVrf(Configuration.DEFAULT_VRF_NAME);
        vgwToVpcBgpNeighbor.setLocalAs(ipsecTunnel.getVgwBgpAsn());
        vgwToVpcBgpNeighbor.setLocalIp(vgwToVpcIfaceAddress);
        vgwToVpcBgpNeighbor.setRemoteAs(ipsecTunnel.getVgwBgpAsn());
        vgwToVpcBgpNeighbor.setDefaultMetric(BGP_NEIGHBOR_DEFAULT_METRIC);
        vgwToVpcBgpNeighbor.setSendCommunity(true);

        // iBGP connection from VPC
        BgpNeighbor vpcToVgwBgpNeighbor = new BgpNeighbor(vgwToVpcIfaceAddress, vpcNode, false);
        BgpProcess vpcProc = new BgpProcess();
        vpcNode.getDefaultVrf().setBgpProcess(vpcProc);
        vpcProc.setMultipathEquivalentAsPathMatchMode(
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);
        vpcProc.setRouterId(vpcIfaceAddress);
        vpcProc.getNeighbors().put(vpcToVgwBgpNeighbor.getPrefix(), vpcToVgwBgpNeighbor);
        vpcToVgwBgpNeighbor.setVrf(Configuration.DEFAULT_VRF_NAME);
        vpcToVgwBgpNeighbor.setLocalAs(ipsecTunnel.getVgwBgpAsn());
        vpcToVgwBgpNeighbor.setLocalIp(vpcIfaceAddress);
        vpcToVgwBgpNeighbor.setRemoteAs(ipsecTunnel.getVgwBgpAsn());
        vpcToVgwBgpNeighbor.setDefaultMetric(BGP_NEIGHBOR_DEFAULT_METRIC);
        vpcToVgwBgpNeighbor.setSendCommunity(true);

        String rpRejectAllName = "~REJECT_ALL~";

        String rpAcceptAllEbgpAndSetNextHopSelfName = "~ACCEPT_ALL_EBGP_AND_SET_NEXT_HOP_SELF~";
        If acceptIffEbgp =
            new If(
                new MatchProtocol(RoutingProtocol.BGP),
                ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                ImmutableList.of(Statements.ExitReject.toStaticStatement()));

        RoutingPolicy vgwRpAcceptAllBgp =
            new RoutingPolicy(rpAcceptAllEbgpAndSetNextHopSelfName, vpnGatewayCfgNode);
        vpnGatewayCfgNode.getRoutingPolicies().put(vgwRpAcceptAllBgp.getName(), vgwRpAcceptAllBgp);
        vgwRpAcceptAllBgp.setStatements(
            ImmutableList.of(new SetNextHop(SelfNextHop.getInstance(), false), acceptIffEbgp));
        vgwToVpcBgpNeighbor.setExportPolicy(rpAcceptAllEbgpAndSetNextHopSelfName);
        RoutingPolicy vgwRpRejectAll = new RoutingPolicy(rpRejectAllName, vpnGatewayCfgNode);
        vpnGatewayCfgNode.getRoutingPolicies().put(rpRejectAllName, vgwRpRejectAll);
        vgwToVpcBgpNeighbor.setImportPolicy(rpRejectAllName);

        String rpAcceptAllName = "~ACCEPT_ALL~";
        RoutingPolicy vpcRpAcceptAll = new RoutingPolicy(rpAcceptAllName, vpcNode);
        vpcNode.getRoutingPolicies().put(rpAcceptAllName, vpcRpAcceptAll);
        vpcRpAcceptAll.setStatements(ImmutableList.of(Statements.ExitAccept.toStaticStatement()));
        vpcToVgwBgpNeighbor.setImportPolicy(rpAcceptAllName);
        RoutingPolicy vpcRpRejectAll = new RoutingPolicy(rpRejectAllName, vpcNode);
        vpcNode.getRoutingPolicies().put(rpRejectAllName, vpcRpRejectAll);
        vpcToVgwBgpNeighbor.setExportPolicy(rpRejectAllName);

        Vpc vpc = region.getVpcs().get(vpcId);
        String originationPolicyName = vpnId + "_origination";
        RoutingPolicy originationRoutingPolicy =
            new RoutingPolicy(originationPolicyName, vpnGatewayCfgNode);
        vpnGatewayCfgNode.getRoutingPolicies().put(originationPolicyName, originationRoutingPolicy);
        cgBgpNeighbor.setExportPolicy(originationPolicyName);
        If originationIf = new If();
        List<Statement> statements = originationRoutingPolicy.getStatements();
        statements.add(originationIf);
        statements.add(Statements.ExitReject.toStaticStatement());
        originationIf
            .getTrueStatements()
            .add(new SetOrigin(new LiteralOrigin(OriginType.IGP, null)));
        originationIf.getTrueStatements().add(Statements.ExitAccept.toStaticStatement());
        RouteFilterList originationRouteFilter = new RouteFilterList(originationPolicyName);
        vpnGatewayCfgNode.getRouteFilterLists().put(originationPolicyName, originationRouteFilter);
        vpc.getCidrBlockAssociations()
            .forEach(
                prefix -> {
                  RouteFilterLine matchOutgoingPrefix =
                      new RouteFilterLine(
                          LineAction.ACCEPT,
                          prefix,
                          new SubRange(prefix.getPrefixLength(), prefix.getPrefixLength()));
                  originationRouteFilter.addLine(matchOutgoingPrefix);
                });
        Conjunction conj = new Conjunction();
        originationIf.setGuard(conj);
        conj.getConjuncts().add(new MatchProtocol(RoutingProtocol.STATIC));
        conj.getConjuncts()
            .add(
                new MatchPrefixSet(
                    new DestinationNetwork(), new NamedPrefixSet(originationPolicyName)));
      }

      // static routes (if configured)
      for (Prefix staticRoutePrefix : _routes) {
        StaticRoute staticRoute =
            StaticRoute.builder()
                .setNetwork(staticRoutePrefix)
                .setNextHopIp(ipsecTunnel.getCgwInsideAddress())
                .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
                .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
                .build();

        vpnGatewayCfgNode.getDefaultVrf().getStaticRoutes().add(staticRoute);
      }
    }
  }

  public String getCustomerGatewayId() {
    return _customerGatewayId;
  }

  @Override
  public String getId() {
    return _vpnConnectionId;
  }

  public List<IpsecTunnel> getIpsecTunnels() {
    return _ipsecTunnels;
  }

  public List<Prefix> getRoutes() {
    return _routes;
  }

  public boolean getStaticRoutesOnly() {
    return _staticRoutesOnly;
  }

  public List<VgwTelemetry> getVgwTelemetrys() {
    return _vgwTelemetrys;
  }

  public String getVpnConnectionId() {
    return _vpnConnectionId;
  }

  public String getVpnGatewayId() {
    return _vpnGatewayId;
  }
}
