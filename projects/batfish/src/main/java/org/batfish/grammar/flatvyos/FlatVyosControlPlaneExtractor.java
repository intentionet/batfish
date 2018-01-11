package org.batfish.grammar.flatvyos;

import java.util.Set;
import java.util.TreeSet;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkAddress;
import org.batfish.datamodel.Prefix;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.flatvyos.FlatVyosParser.Bnt_nexthop_selfContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Bnt_remote_asContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Bnt_route_map_exportContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Bnt_route_map_importContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Bt_neighborContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Esppt_encryptionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Esppt_hashContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Espt_compressionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Espt_lifetimeContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Espt_pfsContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Espt_proposalContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Flat_vyos_configurationContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Hash_algorithmContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Ikept_dh_groupContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Ikept_encryptionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Ikept_hashContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Iket_key_exchangeContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Iket_lifetimeContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Iket_proposalContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Interface_typeContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.It_addressContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.It_descriptionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Ivt_esp_groupContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Ivt_ike_groupContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Ivt_ipsec_interfacesContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Ivt_site_to_siteContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Line_actionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Plrt_actionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Plrt_descriptionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Plrt_geContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Plrt_leContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Plrt_prefixContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Plt_descriptionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Plt_ruleContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Pt_prefix_listContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Pt_route_mapContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Rmmt_ip_address_prefix_listContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Rmrt_actionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Rmrt_descriptionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Rmt_ruleContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S2sat_idContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S2sat_modeContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S2sat_pre_shared_secretContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S2sat_remote_idContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S2st_connection_typeContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S2st_descriptionContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S2st_ike_groupContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S2st_local_addressContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S2svt_bindContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S2svt_esp_groupContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S_interfacesContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.S_protocols_bgpContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Srt_next_hopContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.St_host_nameContext;
import org.batfish.grammar.flatvyos.FlatVyosParser.Statict_routeContext;
import org.batfish.representation.vyos.BgpNeighbor;
import org.batfish.representation.vyos.BgpProcess;
import org.batfish.representation.vyos.EspGroup;
import org.batfish.representation.vyos.EspProposal;
import org.batfish.representation.vyos.HashAlgorithm;
import org.batfish.representation.vyos.IkeGroup;
import org.batfish.representation.vyos.IkeProposal;
import org.batfish.representation.vyos.Interface;
import org.batfish.representation.vyos.InterfaceType;
import org.batfish.representation.vyos.IpsecPeer;
import org.batfish.representation.vyos.PfsSource;
import org.batfish.representation.vyos.PrefixList;
import org.batfish.representation.vyos.PrefixListRule;
import org.batfish.representation.vyos.RouteMap;
import org.batfish.representation.vyos.RouteMapMatch;
import org.batfish.representation.vyos.RouteMapMatchPrefixList;
import org.batfish.representation.vyos.RouteMapRule;
import org.batfish.representation.vyos.StaticNextHopRoute;
import org.batfish.representation.vyos.VyosConfiguration;
import org.batfish.vendor.VendorConfiguration;

public class FlatVyosControlPlaneExtractor extends FlatVyosParserBaseListener
    implements ControlPlaneExtractor {

  private static final String F_INTERFACES_ADDRESS_DHCP = "interfaces address dhcp";

  private static LineAction toAction(Line_actionContext ctx) {
    if (ctx.DENY() != null) {
      return LineAction.REJECT;
    } else if (ctx.PERMIT() != null) {
      return LineAction.ACCEPT;
    } else {
      throw new BatfishException("invalid line_action: " + ctx.getText());
    }
  }

  private static HashAlgorithm toHashAlgorithm(Hash_algorithmContext ctx) {
    if (ctx.MD5() != null) {
      return HashAlgorithm.MD5;
    } else if (ctx.SHA1() != null) {
      return HashAlgorithm.SHA1;
    } else if (ctx.SHA256() != null) {
      return HashAlgorithm.SHA256;
    } else if (ctx.SHA384() != null) {
      return HashAlgorithm.SHA384;
    } else if (ctx.SHA512() != null) {
      return HashAlgorithm.SHA512;
    } else {
      throw new BatfishException("Invalid hash algorithm");
    }
  }

  private static int toInteger(Token t) {
    String text = t.getText();
    try {
      int result = Integer.parseInt(text);
      return result;
    } catch (NumberFormatException e) {
      throw new BatfishException("Not an integer: \"" + text + "\"", e);
    }
  }

  private static InterfaceType toInterfaceType(Interface_typeContext ctx) {
    if (ctx.BONDING() != null) {
      return InterfaceType.BONDING;
    } else if (ctx.BRIDGE() != null) {
      return InterfaceType.BRIDGE;
    } else if (ctx.DUMMY() != null) {
      return InterfaceType.DUMMY;
    } else if (ctx.ETHERNET() != null) {
      return InterfaceType.ETHERNET;
    } else if (ctx.INPUT() != null) {
      return InterfaceType.INPUT;
    } else if (ctx.L2TPV3() != null) {
      return InterfaceType.L2TPV3;
    } else if (ctx.LOOPBACK() != null) {
      return InterfaceType.LOOPBACK;
    } else if (ctx.OPENVPN() != null) {
      return InterfaceType.OPENVPN;
    } else if (ctx.PSEUDO_ETHERNET() != null) {
      return InterfaceType.PSEUDO_ETHERNET;
    } else if (ctx.TUNNEL() != null) {
      return InterfaceType.TUNNEL;
    } else if (ctx.VTI() != null) {
      return InterfaceType.VTI;
    } else if (ctx.VXLAN() != null) {
      return InterfaceType.VXLAN;
    } else if (ctx.WIRELESS() != null) {
      return InterfaceType.WIRELESS;
    } else if (ctx.WIRELESSMODEM() != null) {
      return InterfaceType.WIRELESSMODEM;
    } else {
      throw new BatfishException("Unsupported interface type: " + ctx.getText());
    }
  }

  private BgpProcess _bgpProcess;

  private VyosConfiguration _configuration;

  private BgpNeighbor _currentBgpNeighbor;

  private EspGroup _currentEspGroup;

  private EspProposal _currentEspProposal;

  private IkeGroup _currentIkeGroup;

  private IkeProposal _currentIkeProposal;

  private Interface _currentInterface;

  private IpsecPeer _currentIpsecPeer;

  private PrefixList _currentPrefixList;

  private PrefixListRule _currentPrefixListRule;

  private RouteMap _currentRouteMap;

  private RouteMapRule _currentRouteMapRule;

  private Prefix _currentStaticRoutePrefix;

  private final FlatVyosCombinedParser _parser;

  private final String _text;

  private final Set<String> _unimplementedFeatures;

  private VyosConfiguration _vendorConfiguration;

  private final Warnings _w;

  public FlatVyosControlPlaneExtractor(
      String text, FlatVyosCombinedParser parser, Warnings warnings) {
    _text = text;
    _parser = parser;
    _unimplementedFeatures = new TreeSet<>();
    _w = warnings;
  }

  @Override
  public void enterBt_neighbor(Bt_neighborContext ctx) {
    Ip neighborIp = new Ip(ctx.IP_ADDRESS().getText());
    _currentBgpNeighbor = _bgpProcess.getNeighbors().computeIfAbsent(neighborIp, BgpNeighbor::new);
  }

  @Override
  public void enterEspt_proposal(Espt_proposalContext ctx) {
    int num = toInteger(ctx.num);
    _currentEspProposal = _currentEspGroup.getProposals().computeIfAbsent(num, EspProposal::new);
  }

  @Override
  public void enterFlat_vyos_configuration(Flat_vyos_configurationContext ctx) {
    _vendorConfiguration = new VyosConfiguration();
    _configuration = _vendorConfiguration;
  }

  @Override
  public void enterIket_proposal(Iket_proposalContext ctx) {
    int num = toInteger(ctx.num);
    _currentIkeProposal = _currentIkeGroup.getProposals().computeIfAbsent(num, IkeProposal::new);
  }

  @Override
  public void enterIvt_esp_group(Ivt_esp_groupContext ctx) {
    String name = ctx.name.getText();
    _currentEspGroup = _configuration.getEspGroups().computeIfAbsent(name, EspGroup::new);
  }

  @Override
  public void enterIvt_ike_group(Ivt_ike_groupContext ctx) {
    String name = ctx.name.getText();
    _currentIkeGroup = _configuration.getIkeGroups().computeIfAbsent(name, IkeGroup::new);
  }

  @Override
  public void enterIvt_site_to_site(Ivt_site_to_siteContext ctx) {
    Ip peerAddress = new Ip(ctx.peer.getText());
    _currentIpsecPeer = _configuration.getIpsecPeers().computeIfAbsent(peerAddress, IpsecPeer::new);
  }

  @Override
  public void enterPlt_rule(Plt_ruleContext ctx) {
    int num = toInteger(ctx.num);
    _currentPrefixListRule =
        _currentPrefixList.getRules().computeIfAbsent(num, PrefixListRule::new);
  }

  @Override
  public void enterPt_prefix_list(Pt_prefix_listContext ctx) {
    String name = ctx.name.getText();
    _currentPrefixList = _configuration.getPrefixLists().computeIfAbsent(name, PrefixList::new);
  }

  @Override
  public void enterPt_route_map(Pt_route_mapContext ctx) {
    String name = ctx.name.getText();
    _currentRouteMap = _configuration.getRouteMaps().computeIfAbsent(name, RouteMap::new);
  }

  @Override
  public void enterRmt_rule(Rmt_ruleContext ctx) {
    int num = toInteger(ctx.num);
    _currentRouteMapRule = _currentRouteMap.getRules().computeIfAbsent(num, RouteMapRule::new);
  }

  @Override
  public void enterS_interfaces(S_interfacesContext ctx) {
    String name = ctx.name.getText();
    _currentInterface = _configuration.getInterfaces().get(name);
    if (_currentInterface == null) {
      _currentInterface = new Interface(name);
      _configuration.getInterfaces().put(name, _currentInterface);
      InterfaceType type = toInterfaceType(ctx.interface_type());
      double bandwidth = Interface.getDefaultBandwidth(type);
      _currentInterface.setBandwidth(bandwidth);
      _currentInterface.setType(type);
    }
  }

  @Override
  public void enterS_protocols_bgp(S_protocols_bgpContext ctx) {
    int as = toInteger(ctx.asnum);
    if (_bgpProcess == null) {
      _bgpProcess = new BgpProcess(as);
      _configuration.setBgpProcess(_bgpProcess);
    } else {
      if (_bgpProcess.getLocalAs() != as) {
        throw new BatfishException("Do not support multiple BGP processes at this time");
      }
    }
  }

  @Override
  public void enterStatict_route(Statict_routeContext ctx) {
    Prefix prefix = Prefix.fromString(ctx.IP_PREFIX().getText());
    _currentStaticRoutePrefix = prefix;
  }

  @Override
  public void exitBnt_nexthop_self(Bnt_nexthop_selfContext ctx) {
    _currentBgpNeighbor.setNextHopSelf(true);
  }

  @Override
  public void exitBnt_remote_as(Bnt_remote_asContext ctx) {
    int remoteAs = toInteger(ctx.asnum);
    _currentBgpNeighbor.setRemoteAs(remoteAs);
  }

  @Override
  public void exitBnt_route_map_export(Bnt_route_map_exportContext ctx) {
    String name = ctx.name.getText();
    _currentBgpNeighbor.setExportRouteMap(name);
  }

  @Override
  public void exitBnt_route_map_import(Bnt_route_map_importContext ctx) {
    String name = ctx.name.getText();
    _currentBgpNeighbor.setImportRouteMap(name);
  }

  @Override
  public void exitBt_neighbor(Bt_neighborContext ctx) {
    _currentBgpNeighbor = null;
  }

  @Override
  public void exitEsppt_encryption(Esppt_encryptionContext ctx) {
    if (ctx.AES128() != null) {
      _currentEspProposal.setEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC);
    } else if (ctx.AES256() != null) {
      _currentEspProposal.setEncryptionAlgorithm(EncryptionAlgorithm.AES_256_CBC);
    } else if (ctx.THREEDES() != null) {
      _currentEspProposal.setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC);
    } else {
      throw new BatfishException("Invalid encryption algorithm");
    }
  }

  @Override
  public void exitEsppt_hash(Esppt_hashContext ctx) {
    HashAlgorithm hashAlgorithm = toHashAlgorithm(ctx.hash_algorithm());
    _currentEspProposal.setHashAlgorithm(hashAlgorithm);
  }

  @Override
  public void exitEspt_compression(Espt_compressionContext ctx) {
    if (ctx.DISABLE() != null) {
      _currentEspGroup.setCompression(false);
    } else if (ctx.ENABLE() != null) {
      _currentEspGroup.setCompression(true);
    }
  }

  @Override
  public void exitEspt_lifetime(Espt_lifetimeContext ctx) {
    int lifetimeSeconds = toInteger(ctx.seconds);
    _currentEspGroup.setLifetimeSeconds(lifetimeSeconds);
  }

  @Override
  public void exitEspt_pfs(Espt_pfsContext ctx) {
    if (ctx.ENABLE() != null) {
      _currentEspGroup.setPfsSource(PfsSource.IKE_GROUP);
    } else if (ctx.DISABLE() != null) {
      _currentEspGroup.setPfsSource(PfsSource.DISABLED);
    } else {
      _currentEspGroup.setPfsSource(PfsSource.ESP_GROUP);
      if (ctx.DH_GROUP2() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP2);
      } else if (ctx.DH_GROUP5() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP5);
      } else if (ctx.DH_GROUP14() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP14);
      } else if (ctx.DH_GROUP15() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP15);
      } else if (ctx.DH_GROUP16() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP16);
      } else if (ctx.DH_GROUP17() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP17);
      } else if (ctx.DH_GROUP18() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP18);
      } else if (ctx.DH_GROUP19() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP19);
      } else if (ctx.DH_GROUP20() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP20);
      } else if (ctx.DH_GROUP21() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP21);
      } else if (ctx.DH_GROUP22() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP22);
      } else if (ctx.DH_GROUP23() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP23);
      } else if (ctx.DH_GROUP24() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP24);
      } else if (ctx.DH_GROUP25() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP25);
      } else if (ctx.DH_GROUP26() != null) {
        _currentEspGroup.setPfsDhGroup(DiffieHellmanGroup.GROUP26);
      } else {
        throw new BatfishException("Invalid Diffie-Hellman group");
      }
    }
  }

  @Override
  public void exitEspt_proposal(Espt_proposalContext ctx) {
    _currentEspProposal = null;
  }

  @Override
  public void exitIkept_dh_group(Ikept_dh_groupContext ctx) {
    int num = toInteger(ctx.num);
    DiffieHellmanGroup dhGroup = DiffieHellmanGroup.fromGroupNumber(num);
    _currentIkeProposal.setDhGroup(dhGroup);
  }

  @Override
  public void exitIkept_encryption(Ikept_encryptionContext ctx) {
    if (ctx.AES128() != null) {
      _currentIkeProposal.setEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC);
    } else if (ctx.AES256() != null) {
      _currentIkeProposal.setEncryptionAlgorithm(EncryptionAlgorithm.AES_256_CBC);
    } else if (ctx.THREEDES() != null) {
      _currentIkeProposal.setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC);
    } else {
      throw new BatfishException("Invalid encryption algorithm");
    }
  }

  @Override
  public void exitIkept_hash(Ikept_hashContext ctx) {
    HashAlgorithm hashAlgorithm = toHashAlgorithm(ctx.hash_algorithm());
    _currentIkeProposal.setHashAlgorithm(hashAlgorithm);
  }

  @Override
  public void exitIket_key_exchange(Iket_key_exchangeContext ctx) {
    if (ctx.IKEV1() != null) {
      // ..great?
    } else if (ctx.IKEV2() != null) {
      // TODO: ??
      throw new BatfishException("Not currently supported");
    }
  }

  @Override
  public void exitIket_lifetime(Iket_lifetimeContext ctx) {
    int lifetimeSeconds = toInteger(ctx.seconds);
    _currentIkeGroup.setLifetimeSeconds(lifetimeSeconds);
  }

  @Override
  public void exitIket_proposal(Iket_proposalContext ctx) {
    _currentIkeProposal = null;
  }

  @Override
  public void exitIt_address(It_addressContext ctx) {
    if (ctx.DHCP() != null) {
      todo(ctx, F_INTERFACES_ADDRESS_DHCP);
    } else if (ctx.IP_PREFIX() != null) {
      NetworkAddress address = new NetworkAddress(ctx.IP_PREFIX().getText());
      if (_currentInterface.getAddress() == null) {
        _currentInterface.setAddress(address);
      }
      _currentInterface.getAllAddresses().add(address);
    }
  }

  @Override
  public void exitIt_description(It_descriptionContext ctx) {
    String description = ctx.description().DESCRIPTION_TEXT().getText();
    _currentInterface.setDescription(description);
  }

  @Override
  public void exitIvt_esp_group(Ivt_esp_groupContext ctx) {
    _currentEspGroup = null;
  }

  @Override
  public void exitIvt_ike_group(Ivt_ike_groupContext ctx) {
    _currentIkeGroup = null;
  }

  @Override
  public void exitIvt_ipsec_interfaces(Ivt_ipsec_interfacesContext ctx) {
    String ipsecInterface = ctx.name.getText();
    _configuration.getIpsecInterfaces().add(ipsecInterface);
  }

  @Override
  public void exitIvt_site_to_site(Ivt_site_to_siteContext ctx) {
    _currentIpsecPeer = null;
  }

  @Override
  public void exitPlrt_action(Plrt_actionContext ctx) {
    LineAction action = toAction(ctx.action);
    _currentPrefixListRule.setAction(action);
  }

  @Override
  public void exitPlrt_description(Plrt_descriptionContext ctx) {
    String description = ctx.description().DESCRIPTION_TEXT().getText();
    _currentPrefixListRule.setDescription(description);
  }

  @Override
  public void exitPlrt_ge(Plrt_geContext ctx) {
    int ge = toInteger(ctx.num);
    _currentPrefixListRule.setGe(ge);
  }

  @Override
  public void exitPlrt_le(Plrt_leContext ctx) {
    int le = toInteger(ctx.num);
    _currentPrefixListRule.setLe(le);
  }

  @Override
  public void exitPlrt_prefix(Plrt_prefixContext ctx) {
    Prefix prefix = Prefix.fromString(ctx.prefix.getText());
    _currentPrefixListRule.setPrefix(prefix);
  }

  @Override
  public void exitPlt_description(Plt_descriptionContext ctx) {
    String description = ctx.description().DESCRIPTION_TEXT().getText();
    _currentPrefixList.setDescription(description);
  }

  @Override
  public void exitPlt_rule(Plt_ruleContext ctx) {
    _currentPrefixListRule = null;
  }

  @Override
  public void exitPt_prefix_list(Pt_prefix_listContext ctx) {
    _currentPrefixList = null;
  }

  @Override
  public void exitPt_route_map(Pt_route_mapContext ctx) {
    _currentRouteMap = null;
  }

  @Override
  public void exitRmmt_ip_address_prefix_list(Rmmt_ip_address_prefix_listContext ctx) {
    String name = ctx.name.getText();
    int statementLine = ctx.name.getStart().getLine();
    RouteMapMatch match = new RouteMapMatchPrefixList(name, statementLine);
    _currentRouteMapRule.getMatches().add(match);
  }

  @Override
  public void exitRmrt_action(Rmrt_actionContext ctx) {
    LineAction action = toAction(ctx.action);
    _currentRouteMapRule.setAction(action);
  }

  @Override
  public void exitRmrt_description(Rmrt_descriptionContext ctx) {
    String description = ctx.description().DESCRIPTION_TEXT().getText();
    _currentRouteMapRule.setDescription(description);
  }

  @Override
  public void exitRmt_rule(Rmt_ruleContext ctx) {
    _currentRouteMapRule = null;
  }

  @Override
  public void exitS_interfaces(S_interfacesContext ctx) {
    _currentInterface = null;
  }

  @Override
  public void exitS2sat_id(S2sat_idContext ctx) {
    String authenticationId = ctx.name.getText();
    _currentIpsecPeer.setAuthenticationId(authenticationId);
  }

  @Override
  public void exitS2sat_mode(S2sat_modeContext ctx) {
    if (ctx.PRE_SHARED_SECRET() != null) {
      _currentIpsecPeer.setAuthenticationMode(IkeAuthenticationMethod.PRE_SHARED_KEYS);
    } else if (ctx.RSA() != null) {
      _currentIpsecPeer.setAuthenticationMode(IkeAuthenticationMethod.RSA_SIGNATURES);
    } else if (ctx.X509() != null) {
      _currentIpsecPeer.setAuthenticationMode(IkeAuthenticationMethod.X509);
    }
  }

  @Override
  public void exitS2sat_pre_shared_secret(S2sat_pre_shared_secretContext ctx) {
    String secret = ctx.secret.getText();
    String saltedSecret = secret + CommonUtil.salt();
    String secretHash = CommonUtil.sha256Digest(saltedSecret);
    _currentIpsecPeer.setAuthenticationPreSharedSecretHash(secretHash);
  }

  @Override
  public void exitS2sat_remote_id(S2sat_remote_idContext ctx) {
    String authenticationRemoteId = ctx.name.getText();
    _currentIpsecPeer.setAuthenticationRemoteId(authenticationRemoteId);
  }

  @Override
  public void exitS2st_connection_type(S2st_connection_typeContext ctx) {
    if (ctx.INITIATE() != null) {
      _currentIpsecPeer.setInitiate(true);
    } else if (ctx.RESPOND() != null) {
      _currentIpsecPeer.setInitiate(false);
    } else {
      throw new BatfishException("Invalid connection type");
    }
  }

  @Override
  public void exitS2st_description(S2st_descriptionContext ctx) {
    String description = ctx.description().DESCRIPTION_TEXT().getText();
    _currentIpsecPeer.setDescription(description);
  }

  @Override
  public void exitS2st_ike_group(S2st_ike_groupContext ctx) {
    String ikeGroup = ctx.name.getText();
    _currentIpsecPeer.setIkeGroup(ikeGroup);
  }

  @Override
  public void exitS2st_local_address(S2st_local_addressContext ctx) {
    Ip localAddress = new Ip(ctx.ip.getText());
    _currentIpsecPeer.setLocalAddress(localAddress);
  }

  @Override
  public void exitS2svt_bind(S2svt_bindContext ctx) {
    String bindInterface = ctx.name.getText();
    _currentIpsecPeer.setBindInterface(bindInterface);
  }

  @Override
  public void exitS2svt_esp_group(S2svt_esp_groupContext ctx) {
    String espGroup = ctx.name.getText();
    _currentIpsecPeer.setEspGroup(espGroup);
  }

  @Override
  public void exitSrt_next_hop(Srt_next_hopContext ctx) {
    Ip nextHopIp = new Ip(ctx.nexthop.getText());
    int distance = toInteger(ctx.distance);
    StaticNextHopRoute staticRoute =
        new StaticNextHopRoute(_currentStaticRoutePrefix, nextHopIp, distance);
    _configuration.getStaticNextHopRoutes().add(staticRoute);
  }

  @Override
  public void exitSt_host_name(St_host_nameContext ctx) {
    String hostname = ctx.name.getText();
    _configuration.setHostname(hostname);
  }

  @Override
  public void exitStatict_route(Statict_routeContext ctx) {
    _currentStaticRoutePrefix = null;
  }

  @Override
  public Set<String> getUnimplementedFeatures() {
    return _unimplementedFeatures;
  }

  @Override
  public VendorConfiguration getVendorConfiguration() {
    return _vendorConfiguration;
  }

  @Override
  public void processParseTree(ParserRuleContext tree) {
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(this, tree);
  }

  private void todo(ParserRuleContext ctx, String feature) {
    _w.todo(ctx, feature, _parser, _text);
    _unimplementedFeatures.add("Vyos: " + feature);
  }
}
