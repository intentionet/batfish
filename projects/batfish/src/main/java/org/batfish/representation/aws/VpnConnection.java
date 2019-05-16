package org.batfish.representation.aws;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.IkeKeyType;
import org.batfish.datamodel.IkePhase1Key;
import org.batfish.datamodel.IkePhase1Policy;
import org.batfish.datamodel.IkePhase1Proposal;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpsecAuthenticationAlgorithm;
import org.batfish.datamodel.IpsecEncapsulationMode;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPhase2Policy;
import org.batfish.datamodel.IpsecPhase2Proposal;
import org.batfish.datamodel.IpsecProtocol;
import org.batfish.datamodel.IpsecStaticPeerConfig;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
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

@ParametersAreNonnullByDefault
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

  private static IkeHashingAlgorithm toIkeAuthenticationAlgorithm(String ikeAuthProtocol) {
    switch (ikeAuthProtocol) {
      case "sha1":
        return IkeHashingAlgorithm.SHA1;

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

  @Nullable
  private static IpsecEncapsulationMode toIpsecEncapdulationMode(
      String ipsecEncapsulationMode, Warnings warnings) {
    switch (ipsecEncapsulationMode) {
      case "tunnel":
        return IpsecEncapsulationMode.TUNNEL;
      case "transport":
        return IpsecEncapsulationMode.TRANSPORT;
      default:
        warnings.redFlag(
            String.format("No IPsec encapsulation mode for string '%s'", ipsecEncapsulationMode));
        return null;
    }
  }

  private final String _customerGatewayId;

  private final List<IpsecTunnel> _ipsecTunnels;

  private final List<Prefix> _routes;

  private final boolean _staticRoutesOnly;

  private final List<VgwTelemetry> _vgwTelemetrys;

  private final String _vpnConnectionId;

  private final String _vpnGatewayId;

  public VpnConnection(JSONObject jObj) throws JSONException {
    _vgwTelemetrys = new LinkedList<>();
    _routes = new LinkedList<>();
    _ipsecTunnels = new LinkedList<>();
    _vpnConnectionId = jObj.getString(JSON_KEY_VPN_CONNECTION_ID);
    _customerGatewayId = jObj.getString(JSON_KEY_CUSTOMER_GATEWAY_ID);
    _vpnGatewayId = jObj.getString(JSON_KEY_VPN_GATEWAY_ID);

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
      _vgwTelemetrys.add(new VgwTelemetry(childObject));
    }

    if (jObj.has(JSON_KEY_OPTIONS)) {
      JSONObject options = jObj.getJSONObject(JSON_KEY_OPTIONS);
      _staticRoutesOnly = Utils.tryGetBoolean(options, JSON_KEY_STATIC_ROUTES_ONLY, false);
    } else {
      _staticRoutesOnly = false;
    }
  }

  @Nonnull
  private static IkePhase1Proposal toIkePhase1Proposal(
      String proposalName, IpsecTunnel ipsecTunnel) {
    IkePhase1Proposal ikePhase1Proposal = new IkePhase1Proposal(proposalName);
    if (ipsecTunnel.getIkePreSharedKeyHash() != null) {
      ikePhase1Proposal.setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS);
    }
    ikePhase1Proposal.setHashingAlgorithm(
        toIkeAuthenticationAlgorithm(ipsecTunnel.getIkeAuthProtocol()));
    ikePhase1Proposal.setDiffieHellmanGroup(
        toDiffieHellmanGroup(ipsecTunnel.getIkePerfectForwardSecrecy()));
    ikePhase1Proposal.setEncryptionAlgorithm(
        toEncryptionAlgorithm(ipsecTunnel.getIkeEncryptionProtocol()));
    return ikePhase1Proposal;
  }

  @Nonnull
  private static IkePhase1Key toIkePhase1PreSharedKey(
      IpsecTunnel ipsecTunnel, Ip remoteIdentity, String localInterface) {
    IkePhase1Key ikePhase1Key = new IkePhase1Key();
    ikePhase1Key.setKeyType(IkeKeyType.PRE_SHARED_KEY);
    ikePhase1Key.setKeyHash(ipsecTunnel.getIkePreSharedKeyHash());
    ikePhase1Key.setRemoteIdentity(remoteIdentity.toIpSpace());
    ikePhase1Key.setLocalInterface(localInterface);
    return ikePhase1Key;
  }

  @Nonnull
  private static IkePhase1Policy toIkePhase1Policy(
      String vpnId,
      String ikePhase1ProposalName,
      IkePhase1Key ikePhase1Key,
      Ip remoteIdentity,
      String localInterface) {
    IkePhase1Policy ikePhase1Policy = new IkePhase1Policy(vpnId);
    ikePhase1Policy.setIkePhase1Key(ikePhase1Key);
    ikePhase1Policy.setIkePhase1Proposals(ImmutableList.of(ikePhase1ProposalName));
    ikePhase1Policy.setRemoteIdentity(remoteIdentity.toIpSpace());
    ikePhase1Policy.setLocalInterface(localInterface);
    return ikePhase1Policy;
  }

  @Nonnull
  private static IpsecPhase2Proposal toIpsecPhase2Proposal(
      IpsecTunnel ipsecTunnel, Warnings warnings) {
    IpsecPhase2Proposal ipsecPhase2Proposal = new IpsecPhase2Proposal();
    ipsecPhase2Proposal.setAuthenticationAlgorithm(
        toIpsecAuthenticationAlgorithm(ipsecTunnel.getIpsecAuthProtocol()));
    ipsecPhase2Proposal.setEncryptionAlgorithm(
        toEncryptionAlgorithm(ipsecTunnel.getIpsecEncryptionProtocol()));
    ipsecPhase2Proposal.setProtocols(
        ImmutableSortedSet.of(toIpsecProtocol(ipsecTunnel.getIpsecProtocol())));
    ipsecPhase2Proposal.setIpsecEncapsulationMode(
        toIpsecEncapdulationMode(ipsecTunnel.getIpsecMode(), warnings));
    return ipsecPhase2Proposal;
  }

  @Nonnull
  private static IpsecPhase2Policy toIpsecPhase2Policy(
      IpsecTunnel ipsecTunnel, String ipsecPhase2Proposal) {
    IpsecPhase2Policy ipsecPhase2Policy = new IpsecPhase2Policy();
    ipsecPhase2Policy.setPfsKeyGroup(
        toDiffieHellmanGroup(ipsecTunnel.getIpsecPerfectForwardSecrecy()));
    ipsecPhase2Policy.setProposals(ImmutableList.of(ipsecPhase2Proposal));
    return ipsecPhase2Policy;
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

    ImmutableSortedMap.Builder<String, IkePhase1Policy> ikePhase1PolicyMapBuilder =
        ImmutableSortedMap.naturalOrder();
    ImmutableSortedMap.Builder<String, IkePhase1Key> ikePhase1KeyMapBuilder =
        ImmutableSortedMap.naturalOrder();
    ImmutableSortedMap.Builder<String, IkePhase1Proposal> ikePhase1ProposalMapBuilder =
        ImmutableSortedMap.naturalOrder();
    ImmutableSortedMap.Builder<String, IpsecPhase2Proposal> ipsecPhase2ProposalMapBuilder =
        ImmutableSortedMap.naturalOrder();
    ImmutableSortedMap.Builder<String, IpsecPhase2Policy> ipsecPhase2PolicyMapBuilder =
        ImmutableSortedMap.naturalOrder();
    ImmutableSortedMap.Builder<String, IpsecPeerConfig> ipsecPeerConfigMapBuilder =
        ImmutableSortedMap.naturalOrder();

    for (int i = 0; i < _ipsecTunnels.size(); i++) {
      int idNum = i + 1;
      String vpnId = _vpnConnectionId + "-" + idNum;
      IpsecTunnel ipsecTunnel = _ipsecTunnels.get(i);
      if (ipsecTunnel.getCgwBgpAsn() != -1 && (_staticRoutesOnly || !_routes.isEmpty())) {
        throw new BatfishException(
            "Unexpected combination of BGP and static routes for VPN connection: \""
                + _vpnConnectionId
                + "\"");
      }
      // create representation structures and add to configuration node
      String externalInterfaceName = "external" + idNum;
      InterfaceAddress externalInterfaceAddress =
          new InterfaceAddress(ipsecTunnel.getVgwOutsideAddress(), Prefix.MAX_PREFIX_LENGTH);

      Utils.newInterface(externalInterfaceName, vpnGatewayCfgNode, externalInterfaceAddress);

      String vpnInterfaceName = "vpn" + idNum;
      InterfaceAddress vpnInterfaceAddress =
          new InterfaceAddress(
              ipsecTunnel.getVgwInsideAddress(), ipsecTunnel.getVgwInsidePrefixLength());
      Utils.newInterface(vpnInterfaceName, vpnGatewayCfgNode, vpnInterfaceAddress);

      // IPsec data-model
      ikePhase1ProposalMapBuilder.put(vpnId, toIkePhase1Proposal(vpnId, ipsecTunnel));
      IkePhase1Key ikePhase1Key =
          toIkePhase1PreSharedKey(
              ipsecTunnel, ipsecTunnel.getCgwOutsideAddress(), externalInterfaceName);
      ikePhase1KeyMapBuilder.put(vpnId, ikePhase1Key);
      ikePhase1PolicyMapBuilder.put(
          vpnId,
          toIkePhase1Policy(
              vpnId,
              vpnId,
              ikePhase1Key,
              ipsecTunnel.getCgwOutsideAddress(),
              externalInterfaceName));
      ipsecPhase2ProposalMapBuilder.put(vpnId, toIpsecPhase2Proposal(ipsecTunnel, warnings));
      ipsecPhase2PolicyMapBuilder.put(vpnId, toIpsecPhase2Policy(ipsecTunnel, vpnId));
      ipsecPeerConfigMapBuilder.put(
          vpnId,
          IpsecStaticPeerConfig.builder()
              .setTunnelInterface(vpnInterfaceName)
              .setIkePhase1Policy(vpnId)
              .setIpsecPolicy(vpnId)
              .setSourceInterface(externalInterfaceName)
              .setLocalAddress(ipsecTunnel.getVgwOutsideAddress())
              .setDestinationAddress(ipsecTunnel.getCgwOutsideAddress())
              .build());

      // bgp (if configured)
      if (ipsecTunnel.getVgwBgpAsn() != -1) {
        BgpProcess proc = vpnGatewayCfgNode.getDefaultVrf().getBgpProcess();
        if (proc == null) {
          proc = new BgpProcess(ipsecTunnel.getVgwInsideAddress());
          proc.setMultipathEquivalentAsPathMatchMode(MultipathEquivalentAsPathMatchMode.EXACT_PATH);
          vpnGatewayCfgNode.getDefaultVrf().setBgpProcess(proc);
        }

        BgpActivePeerConfig.Builder cgBgpPeerConfig =
            BgpActivePeerConfig.builder()
                .setPeerAddress(ipsecTunnel.getCgwInsideAddress())
                .setRemoteAs(ipsecTunnel.getCgwBgpAsn())
                .setBgpProcess(proc)
                .setLocalAs(ipsecTunnel.getVgwBgpAsn())
                .setLocalIp(ipsecTunnel.getVgwInsideAddress())
                .setDefaultMetric(BGP_NEIGHBOR_DEFAULT_METRIC)
                .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.instance())
                .setSendCommunity(false);

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
        Ip vpcIfaceAddress = vpcNode.getAllInterfaces().get(_vpnGatewayId).getAddress().getIp();
        Ip vgwToVpcIfaceAddress =
            vpnGatewayCfgNode.getAllInterfaces().get(vpcId).getAddress().getIp();
        BgpActivePeerConfig.Builder vgwToVpcBuilder = BgpActivePeerConfig.builder();
        vgwToVpcBuilder
            .setPeerAddress(vpcIfaceAddress)
            .setRemoteAs(ipsecTunnel.getVgwBgpAsn())
            .setBgpProcess(proc)
            .setLocalAs(ipsecTunnel.getVgwBgpAsn())
            .setLocalIp(vgwToVpcIfaceAddress)
            .setDefaultMetric(BGP_NEIGHBOR_DEFAULT_METRIC)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.instance())
            .setSendCommunity(true);

        // iBGP connection from VPC
        BgpActivePeerConfig.Builder vpcToVgwBgpPeerConfig = BgpActivePeerConfig.builder();
        vpcToVgwBgpPeerConfig.setPeerAddress(vgwToVpcIfaceAddress);
        BgpProcess vpcProc = new BgpProcess(vpcIfaceAddress);
        vpcNode.getDefaultVrf().setBgpProcess(vpcProc);
        vpcProc.setMultipathEquivalentAsPathMatchMode(
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);
        vpcToVgwBgpPeerConfig.setBgpProcess(vpcProc);
        vpcToVgwBgpPeerConfig.setLocalAs(ipsecTunnel.getVgwBgpAsn());
        vpcToVgwBgpPeerConfig.setLocalIp(vpcIfaceAddress);
        vpcToVgwBgpPeerConfig.setRemoteAs(ipsecTunnel.getVgwBgpAsn());
        vpcToVgwBgpPeerConfig.setDefaultMetric(BGP_NEIGHBOR_DEFAULT_METRIC);
        vpcToVgwBgpPeerConfig.setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.instance());
        vpcToVgwBgpPeerConfig.setSendCommunity(true);

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
        vgwToVpcBuilder.setExportPolicy(rpAcceptAllEbgpAndSetNextHopSelfName);
        RoutingPolicy vgwRpRejectAll = new RoutingPolicy(rpRejectAllName, vpnGatewayCfgNode);
        vpnGatewayCfgNode.getRoutingPolicies().put(rpRejectAllName, vgwRpRejectAll);
        vgwToVpcBuilder.setImportPolicy(rpRejectAllName);

        String rpAcceptAllName = "~ACCEPT_ALL~";
        RoutingPolicy vpcRpAcceptAll = new RoutingPolicy(rpAcceptAllName, vpcNode);
        vpcNode.getRoutingPolicies().put(rpAcceptAllName, vpcRpAcceptAll);
        vpcRpAcceptAll.setStatements(ImmutableList.of(Statements.ExitAccept.toStaticStatement()));
        vpcToVgwBgpPeerConfig.setImportPolicy(rpAcceptAllName);
        RoutingPolicy vpcRpRejectAll = new RoutingPolicy(rpRejectAllName, vpcNode);
        vpcNode.getRoutingPolicies().put(rpRejectAllName, vpcRpRejectAll);
        vpcToVgwBgpPeerConfig.setExportPolicy(rpRejectAllName);

        Vpc vpc = region.getVpcs().get(vpcId);
        String originationPolicyName = vpnId + "_origination";
        RoutingPolicy originationRoutingPolicy =
            new RoutingPolicy(originationPolicyName, vpnGatewayCfgNode);
        vpnGatewayCfgNode.getRoutingPolicies().put(originationPolicyName, originationRoutingPolicy);
        cgBgpPeerConfig.setExportPolicy(originationPolicyName);
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
                          LineAction.PERMIT,
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
                    DestinationNetwork.instance(), new NamedPrefixSet(originationPolicyName)));

        cgBgpPeerConfig.build();
        vgwToVpcBuilder.build();
        vpcToVgwBgpPeerConfig.build();
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
    vpnGatewayCfgNode.setIkePhase1Proposals(ikePhase1ProposalMapBuilder.build());
    vpnGatewayCfgNode.setIkePhase1Keys(ikePhase1KeyMapBuilder.build());
    vpnGatewayCfgNode.setIkePhase1Policies(ikePhase1PolicyMapBuilder.build());
    vpnGatewayCfgNode.setIpsecPhase2Proposals(ipsecPhase2ProposalMapBuilder.build());
    vpnGatewayCfgNode.setIpsecPhase2Policies(ipsecPhase2PolicyMapBuilder.build());
    vpnGatewayCfgNode.setIpsecPeerConfigs(ipsecPeerConfigMapBuilder.build());
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
