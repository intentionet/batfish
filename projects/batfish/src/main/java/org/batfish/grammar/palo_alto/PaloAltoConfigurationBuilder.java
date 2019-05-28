package org.batfish.grammar.palo_alto;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.CATCHALL_APPLICATION_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.CATCHALL_SERVICE_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.CATCHALL_ZONE_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.DEFAULT_VSYS_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.PANORAMA_VSYS_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.SHARED_VSYS_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.computeObjectName;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_GROUP_OR_ADDRESS_OBJECT;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_GROUP_OR_ADDRESS_OBJECT_OR_NONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_OBJECT;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.APPLICATION_GROUP_OR_APPLICATION;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.RULE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SERVICE_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP_OR_NONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ZONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.ADDRESS_GROUP_STATIC;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULEBASE_SERVICE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_APPLICATION;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_DESTINATION;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_FROM_ZONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_SELF_REF;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_SOURCE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_TO_ZONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.SERVICE_GROUP_MEMBER;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.STATIC_ROUTE_INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.VIRTUAL_ROUTER_INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.ZONE_INTERFACE;

import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpRange;
import org.batfish.datamodel.IpsecAuthenticationAlgorithm;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_authenticationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_dh_groupContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_encryptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_encryption_algoContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_hashContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_lifetimeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.If_commentContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Palo_alto_configurationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Port_or_rangeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_addressContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_address_groupContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_serviceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_service_groupContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_sharedContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_vsysContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_zoneContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sa_descriptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sa_fqdnContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sa_ip_netmaskContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sa_ip_rangeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sag_descriptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sag_dynamicContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sag_staticContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sds_default_gatewayContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sds_hostnameContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sds_ip_addressContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sds_netmaskContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sds_ntp_serversContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sdsd_serversContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sdsn_ntp_server_addressContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Set_line_config_devicesContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Set_line_policy_panoramaContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sn_virtual_routerContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sni_ethernetContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sni_loopbackContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sni_tunnelContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sni_vlanContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snicp_global_protectContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snicp_ike_crypto_profilesContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snicp_ipsec_crypto_profilesContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snie_link_stateContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sniel3_ipContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sniel3_mtuContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sniel3_unitContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sniel3u_tagContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snil_unitContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snit_unitContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sniv_unitContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snvr_interfaceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snvr_routing_tableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snvrrt_admin_distContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snvrrt_destinationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snvrrt_interfaceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snvrrt_metricContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snvrrt_nexthopContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sr_securityContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Src_or_dst_list_itemContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_actionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_applicationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_descriptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_destinationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_disabledContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_fromContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_serviceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_sourceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_toContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sserv_descriptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sserv_portContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sserv_protocolContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sserv_source_portContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sservgrp_membersContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ssl_syslogContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ssls_serverContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sslss_serverContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Szn_layer3Context;
import org.batfish.grammar.palo_alto.PaloAltoParser.VariableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Variable_list_itemContext;
import org.batfish.representation.palo_alto.AddressGroup;
import org.batfish.representation.palo_alto.AddressObject;
import org.batfish.representation.palo_alto.CryptoProfile;
import org.batfish.representation.palo_alto.CryptoProfile.Type;
import org.batfish.representation.palo_alto.Interface;
import org.batfish.representation.palo_alto.PaloAltoConfiguration;
import org.batfish.representation.palo_alto.PaloAltoStructureType;
import org.batfish.representation.palo_alto.PaloAltoStructureUsage;
import org.batfish.representation.palo_alto.Rule;
import org.batfish.representation.palo_alto.RuleEndpoint;
import org.batfish.representation.palo_alto.Service;
import org.batfish.representation.palo_alto.ServiceBuiltIn;
import org.batfish.representation.palo_alto.ServiceGroup;
import org.batfish.representation.palo_alto.ServiceOrServiceGroupReference;
import org.batfish.representation.palo_alto.StaticRoute;
import org.batfish.representation.palo_alto.SyslogServer;
import org.batfish.representation.palo_alto.VirtualRouter;
import org.batfish.representation.palo_alto.Vsys;
import org.batfish.representation.palo_alto.Zone;
import org.batfish.vendor.StructureType;

public class PaloAltoConfigurationBuilder extends PaloAltoParserBaseListener {
  private PaloAltoConfiguration _configuration;

  private AddressGroup _currentAddressGroup;

  private AddressObject _currentAddressObject;

  private CryptoProfile _currentCrytoProfile;

  private String _currentDeviceName;

  private Interface _currentInterface;

  private boolean _currentNtpServerPrimary;

  private Interface _currentParentInterface;

  private Rule _currentRule;

  private Service _currentService;

  private ServiceGroup _currentServiceGroup;

  private StaticRoute _currentStaticRoute;

  private SyslogServer _currentSyslogServer;

  private String _currentSyslogServerGroupName;

  private VirtualRouter _currentVirtualRouter;

  private Vsys _currentVsys;

  private Zone _currentZone;

  private Vsys _defaultVsys;

  private PaloAltoCombinedParser _parser;

  private final String _text;

  private final Warnings _w;

  public PaloAltoConfigurationBuilder(
      PaloAltoCombinedParser parser, String text, Warnings warnings) {
    _configuration = new PaloAltoConfiguration();
    _parser = parser;
    _text = text;
    _w = warnings;
  }

  @SuppressWarnings("unused")
  private BatfishException convError(Class<?> type, ParserRuleContext ctx) {
    String typeName = type.getSimpleName();
    String txt = getFullText(ctx);
    return new BatfishException("Could not convert to " + typeName + ": " + txt);
  }

  private String convErrorMessage(Class<?> type, ParserRuleContext ctx) {
    return String.format("Could not convert to %s: %s", type.getSimpleName(), getFullText(ctx));
  }

  private <T, U extends T> T convProblem(
      Class<T> returnType, ParserRuleContext ctx, U defaultReturnValue) {
    _w.redFlag(convErrorMessage(returnType, ctx));
    return defaultReturnValue;
  }

  /** Mark the specified structure as defined on each line in the supplied context */
  private void defineStructure(StructureType type, String name, RuleContext ctx) {
    /* Recursively process children to find all relevant definition lines for the specified context */
    for (int i = 0; i < ctx.getChildCount(); i++) {
      ParseTree child = ctx.getChild(i);
      if (child instanceof TerminalNode) {
        _configuration.defineStructure(type, name, getLine(((TerminalNode) child).getSymbol()));
      } else if (child instanceof RuleContext) {
        defineStructure(type, name, (RuleContext) child);
      }
    }
  }

  private String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    String text = _text.substring(start, end + 1);
    return text;
  }

  /** Return original line number for specified token */
  private int getLine(Token t) {
    return _parser.getLine(t);
  }

  /** Return token text with enclosing quotes removed, if applicable */
  private String getText(ParserRuleContext ctx) {
    return unquote(ctx.getText());
  }

  /**
   * Helper function to add the correct service reference type for a given reference. For references
   * that may be pointing to built-in services, this is needed to make sure we don't create false
   * positive undefined references.
   */
  private void referenceService(Variable_list_itemContext var, PaloAltoStructureUsage usage) {
    String serviceName = getText(var);
    // Use constructed object name so same-named refs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), serviceName);

    if (serviceName.equals(ServiceBuiltIn.SERVICE_HTTP.getName())
        || serviceName.equals(ServiceBuiltIn.SERVICE_HTTPS.getName())
        || serviceName.equals(CATCHALL_SERVICE_NAME)) {
      // Built-in services can be overridden, so add optional object reference
      _configuration.referenceStructure(
          SERVICE_OR_SERVICE_GROUP_OR_NONE, uniqueName, usage, getLine(var.start));
    } else {
      _configuration.referenceStructure(
          SERVICE_OR_SERVICE_GROUP, uniqueName, usage, getLine(var.start));
    }
  }

  private DiffieHellmanGroup toDiffieHellmanGroup(Cp_dh_groupContext ctx) {
    if (ctx.GROUP1() != null) {
      return DiffieHellmanGroup.GROUP1;
    } else if (ctx.GROUP2() != null) {
      return DiffieHellmanGroup.GROUP2;
    } else if (ctx.GROUP5() != null) {
      return DiffieHellmanGroup.GROUP5;
    } else if (ctx.GROUP14() != null) {
      return DiffieHellmanGroup.GROUP14;
    } else if (ctx.GROUP19() != null) {
      return DiffieHellmanGroup.GROUP19;
    } else if (ctx.GROUP20() != null) {
      return DiffieHellmanGroup.GROUP20;
    } else {
      return convProblem(DiffieHellmanGroup.class, ctx, null);
    }
  }

  private EncryptionAlgorithm toEncryptionAlgo(Cp_encryption_algoContext ctx) {
    if (ctx.AES_128_CBC() != null) {
      return EncryptionAlgorithm.AES_128_CBC;
    } else if (ctx.AES_128_GCM() != null) {
      return EncryptionAlgorithm.AES_128_GCM;
    } else if (ctx.AES_192_CBC() != null) {
      return EncryptionAlgorithm.AES_192_CBC;
    } else if (ctx.AES_256_CBC() != null) {
      return EncryptionAlgorithm.AES_256_CBC;
    } else if (ctx.AES_256_GCM() != null) {
      return EncryptionAlgorithm.AES_256_GCM;
    } else if (ctx.DES() != null) {
      return EncryptionAlgorithm.DES_CBC;
    } else if (ctx.THREE_DES() != null) {
      return EncryptionAlgorithm.THREEDES_CBC;
    } else if (ctx.NULL() != null) {
      return EncryptionAlgorithm.NULL;
    }
    return convProblem(EncryptionAlgorithm.class, ctx, null);
  }

  private IkeHashingAlgorithm toIkeHashingAlgorithm(Cp_hashContext ctx) {
    if (ctx.MD5() != null) {
      return IkeHashingAlgorithm.MD5;
    } else if (ctx.SHA1() != null) {
      return IkeHashingAlgorithm.SHA1;
    } else if (ctx.SHA256() != null) {
      return IkeHashingAlgorithm.SHA_256;
    } else if (ctx.SHA384() != null) {
      return IkeHashingAlgorithm.SHA_384;
    } else if (ctx.SHA512() != null) {
      return IkeHashingAlgorithm.SHA_512;
    }
    return convProblem(IkeHashingAlgorithm.class, ctx, null);
  }

  private static int toInteger(Token t) {
    return Integer.parseInt(t.getText());
  }

  private IpsecAuthenticationAlgorithm toIpsecAuthenticationAlgorithm(
      Cp_authenticationContext ctx) {
    if (ctx.MD5() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_MD5_96;
    } else if (ctx.SHA1() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA1_96;
    } else if (ctx.SHA256() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA_256_128;
    } else if (ctx.SHA384() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA_384;
    } else if (ctx.SHA512() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA_512;
    } else if (ctx.NONE() != null) {
      return null;
    }
    return convProblem(IpsecAuthenticationAlgorithm.class, ctx, null);
  }

  /** Convert source or destination list item into an appropriate IpSpace */
  private RuleEndpoint toRuleEndpoint(Src_or_dst_list_itemContext ctx) {
    String text = ctx.getText();
    if (ctx.ANY() != null) {
      return new RuleEndpoint(RuleEndpoint.Type.Any, text);
    } else if (ctx.IP_ADDRESS() != null) {
      return new RuleEndpoint(RuleEndpoint.Type.IP_ADDRESS, text);
    } else if (ctx.IP_PREFIX() != null) {
      return new RuleEndpoint(RuleEndpoint.Type.IP_PREFIX, text);
    } else if (ctx.IP_RANGE() != null) {
      return new RuleEndpoint(RuleEndpoint.Type.IP_RANGE, text);
    }
    return new RuleEndpoint(RuleEndpoint.Type.REFERENCE, text);
  }

  private String unquote(String text) {
    if (text.length() < 2) {
      return text;
    }
    char leading = text.charAt(0);
    char trailing = text.charAt(text.length() - 1);
    if (leading == '\'' || leading == '"') {
      if (leading == trailing) {
        return text.substring(1, text.length() - 1);
      } else {
        _w.redFlag("Improperly-quoted string: " + text);
      }
    }
    return text;
  }

  @Override
  public void exitCp_authentication(Cp_authenticationContext ctx) {
    if (_currentCrytoProfile.getType() == Type.IKE) {
      _w.redFlag("'authentication' is illegal for ike-crypto-profile");
      return;
    }
    IpsecAuthenticationAlgorithm algo = toIpsecAuthenticationAlgorithm(ctx);
    if (algo != null) {
      _currentCrytoProfile.setAuthAlgorithm(algo);
    }
  }

  @Override
  public void exitCp_dh_group(Cp_dh_groupContext ctx) {
    if (_currentCrytoProfile.getType() == Type.GLOBAL_PROTECT_APP) {
      _w.redFlag("'dh-group' is illegal for global-proptect-app-crypto-profile");
      return;
    }
    DiffieHellmanGroup dhGroup = toDiffieHellmanGroup(ctx);
    if (dhGroup != null) {
      _currentCrytoProfile.setDhGroup(dhGroup);
    }
  }

  @Override
  public void exitCp_encryption(Cp_encryptionContext ctx) {
    List<EncryptionAlgorithm> algos =
        ctx.algo.stream().map(this::toEncryptionAlgo).collect(Collectors.toList());
    _currentCrytoProfile.setEncryptionAlgorithms(algos);
  }

  @Override
  public void exitCp_hash(Cp_hashContext ctx) {
    if (_currentCrytoProfile.getType() != Type.IKE) {
      _w.redFlag("'hash' is illegal for non-Ike crypto profiles");
      return;
    }
    IkeHashingAlgorithm algo = toIkeHashingAlgorithm(ctx);
    if (algo != null) {
      _currentCrytoProfile.setHashAlgorithm(algo);
    }
  }

  @Override
  public void exitCp_lifetime(Cp_lifetimeContext ctx) {
    if (_currentCrytoProfile.getType() == Type.GLOBAL_PROTECT_APP) {
      _w.redFlag("'lifetime' is illegal for global-protect-app-crypto profile");
      return;
    }
    int val = toInteger(ctx.val);
    if (ctx.DAYS() != null) {
      val *= 24 * 60 * 60;
    } else if (ctx.HOURS() != null) {
      val *= 60 * 60;
    } else if (ctx.MINUTES() != null) {
      val *= 60;
    }
    _currentCrytoProfile.setLifetimeSeconds(val);
  }

  @Override
  public void exitIf_comment(If_commentContext ctx) {
    _currentInterface.setComment(getText(ctx.text));
  }

  @Override
  public void enterPalo_alto_configuration(Palo_alto_configurationContext ctx) {
    _configuration = new PaloAltoConfiguration();
    _configuration.getVirtualSystems().computeIfAbsent(SHARED_VSYS_NAME, Vsys::new);
    _defaultVsys = _configuration.getVirtualSystems().computeIfAbsent(DEFAULT_VSYS_NAME, Vsys::new);
    _currentVsys = _defaultVsys;
  }

  @Override
  public void exitPalo_alto_configuration(Palo_alto_configurationContext ctx) {
    // Assign the appropriate zone to each interface
    for (Vsys vsys : _configuration.getVirtualSystems().values()) {
      for (Zone zone : vsys.getZones().values()) {
        for (String ifname : zone.getInterfaceNames()) {
          Interface iface = _configuration.getInterfaces().get(ifname);
          if (iface != null) {
            iface.setZone(zone);
          }
        }
      }
    }
  }

  @Override
  public void enterS_address(S_addressContext ctx) {
    String name = ctx.name.getText();
    if (_currentVsys.getAddressGroups().get(name) != null) {
      _w.redFlag(
          String.format(
              "Cannot have an address object and group with the same name '%s'. Ignoring the object definition.",
              name));
    } else {
      _currentAddressObject =
          _currentVsys.getAddressObjects().computeIfAbsent(name, AddressObject::new);

      // Use constructed name so same-named defs across vsys are unique
      String uniqueName = computeObjectName(_currentVsys.getName(), name);
      defineStructure(ADDRESS_OBJECT, uniqueName, ctx);
    }
  }

  @Override
  public void exitS_address(S_addressContext ctx) {
    _currentAddressObject = null;
  }

  @Override
  public void enterS_address_group(S_address_groupContext ctx) {
    String name = ctx.name.getText();
    if (_currentVsys.getAddressObjects().get(name) != null) {
      _w.redFlag(
          String.format(
              "Cannot have an address object and group with the same name '%s'. Ignoring the group definition.",
              name));
    } else {
      _currentAddressGroup =
          _currentVsys.getAddressGroups().computeIfAbsent(name, AddressGroup::new);

      // Use constructed name so same-named defs across vsys are unique
      String uniqueName = computeObjectName(_currentVsys.getName(), name);
      defineStructure(ADDRESS_GROUP, uniqueName, ctx);
    }
  }

  @Override
  public void exitS_address_group(S_address_groupContext ctx) {
    _currentAddressGroup = null;
  }

  @Override
  public void enterS_zone(S_zoneContext ctx) {
    String name = getText(ctx.name);
    _currentZone = _currentVsys.getZones().computeIfAbsent(name, n -> new Zone(n, _currentVsys));

    // Use constructed zone name so same-named zone defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    defineStructure(ZONE, uniqueName, ctx);
  }

  @Override
  public void exitS_zone(S_zoneContext ctx) {
    _currentZone = null;
  }

  @Override
  public void exitSa_description(Sa_descriptionContext ctx) {
    if (_currentAddressObject == null) {
      return;
    }
    _currentAddressObject.setDescription(ctx.description.getText());
  }

  @Override
  public void exitSa_fqdn(Sa_fqdnContext ctx) {
    _w.redFlag("FQDN in address objects is not currently supported: " + getFullText(ctx));
  }

  @Override
  public void exitSa_ip_netmask(Sa_ip_netmaskContext ctx) {
    if (_currentAddressObject == null) {
      return;
    }
    if (ctx.IP_ADDRESS() != null) {
      _currentAddressObject.setIpSpace(Ip.parse(ctx.IP_ADDRESS().getText()).toIpSpace());
    } else if (ctx.IP_PREFIX() != null) {
      _currentAddressObject.setIpSpace(Prefix.parse(ctx.IP_PREFIX().getText()).toIpSpace());
    } else {
      _w.redFlag("Cannot understand what follows 'ip-netmask' in " + getFullText(ctx));
    }
  }

  @Override
  public void exitSa_ip_range(Sa_ip_rangeContext ctx) {
    if (_currentAddressObject == null) {
      return;
    }
    String[] ips = ctx.IP_RANGE().getText().split("-");
    _currentAddressObject.setIpSpace(IpRange.range(Ip.parse(ips[0]), Ip.parse(ips[1])));
  }

  @Override
  public void exitSag_description(Sag_descriptionContext ctx) {
    if (_currentAddressGroup == null) {
      return;
    }
    _currentAddressGroup.setDescription(ctx.description.getText());
  }

  @Override
  public void exitSag_dynamic(Sag_dynamicContext ctx) {
    _w.redFlag("Dynamic address groups are not currently supported: " + getFullText(ctx));
  }

  @Override
  public void exitSag_static(Sag_staticContext ctx) {
    if (_currentAddressGroup == null) {
      return;
    }
    for (VariableContext var : ctx.variable()) {
      String objectName = var.getText();
      if (objectName.equals(_currentAddressGroup.getName())) {
        _w.redFlag(
            String.format(
                "The address group '%s' cannot contain itself: '%s'",
                objectName, getFullText(ctx)));
      } else {
        _currentAddressGroup.getMembers().add(objectName);

        // Use constructed name so same-named defs across vsys are unique
        String uniqueName = computeObjectName(_currentVsys.getName(), objectName);
        _configuration.referenceStructure(
            ADDRESS_GROUP_OR_ADDRESS_OBJECT, uniqueName, ADDRESS_GROUP_STATIC, getLine(var.start));
      }
    }
  }

  @Override
  public void enterSds_default_gateway(Sds_default_gatewayContext ctx) {
    _configuration.setMgmtIfaceGateway(Ip.parse(ctx.IP_ADDRESS().getText()));
  }

  @Override
  public void exitSds_hostname(Sds_hostnameContext ctx) {
    _configuration.setHostname(getText(ctx.name));
  }

  @Override
  public void enterSds_ip_address(Sds_ip_addressContext ctx) {
    _configuration.setMgmtIfaceAddress(Ip.parse(ctx.IP_ADDRESS().getText()));
  }

  @Override
  public void enterSds_netmask(Sds_netmaskContext ctx) {
    _configuration.setMgmtIfaceNetmask(Ip.parse(ctx.IP_ADDRESS().getText()));
  }

  @Override
  public void enterSds_ntp_servers(Sds_ntp_serversContext ctx) {
    _currentNtpServerPrimary = ctx.PRIMARY_NTP_SERVER() != null;
  }

  @Override
  public void exitSdsn_ntp_server_address(Sdsn_ntp_server_addressContext ctx) {
    if (_currentNtpServerPrimary) {
      _configuration.setNtpServerPrimary(ctx.address.getText());
    } else {
      _configuration.setNtpServerSecondary(ctx.address.getText());
    }
  }

  @Override
  public void exitSdsd_servers(Sdsd_serversContext ctx) {
    if (ctx.primary_name != null) {
      _configuration.setDnsServerPrimary(ctx.primary_name.getText());
    } else if (ctx.secondary_name != null) {
      _configuration.setDnsServerSecondary(ctx.secondary_name.getText());
    }
  }

  @Override
  public void enterSet_line_config_devices(Set_line_config_devicesContext ctx) {
    if (ctx.name != null) {
      String deviceName = ctx.name.getText();
      _currentDeviceName = firstNonNull(_currentDeviceName, deviceName);
      if (!_currentDeviceName.equals(deviceName)) {
        /* Do not currently handle multiple device names, which presumably happens only if multiple
         * physical devices are configured from a single config */
        _w.redFlag("Multiple devices encountered: " + deviceName);
      }
    }
  }

  @Override
  public void enterSet_line_policy_panorama(Set_line_policy_panoramaContext ctx) {
    _currentVsys =
        _configuration.getVirtualSystems().computeIfAbsent(PANORAMA_VSYS_NAME, Vsys::new);
  }

  @Override
  public void exitSet_line_policy_panorama(Set_line_policy_panoramaContext ctx) {
    _currentVsys = _defaultVsys;
  }

  @Override
  public void enterSn_virtual_router(Sn_virtual_routerContext ctx) {
    _currentVirtualRouter =
        _configuration.getVirtualRouters().computeIfAbsent(getText(ctx.name), VirtualRouter::new);
  }

  @Override
  public void exitSn_virtual_router(Sn_virtual_routerContext ctx) {
    _currentVirtualRouter = null;
  }

  @Override
  public void enterSni_ethernet(Sni_ethernetContext ctx) {
    String name = ctx.name.getText();
    _currentParentInterface = _configuration.getInterfaces().computeIfAbsent(name, Interface::new);
    _currentInterface = _currentParentInterface;
    defineStructure(INTERFACE, name, ctx);
  }

  @Override
  public void exitSni_ethernet(Sni_ethernetContext ctx) {
    _currentParentInterface = null;
    _currentInterface = null;
  }

  @Override
  public void enterSni_loopback(Sni_loopbackContext ctx) {
    String name = ctx.LOOPBACK().getText();
    _currentParentInterface = _configuration.getInterfaces().computeIfAbsent(name, Interface::new);
    _currentInterface = _currentParentInterface;
    defineStructure(INTERFACE, name, ctx);
  }

  @Override
  public void exitSni_loopback(Sni_loopbackContext ctx) {
    _currentParentInterface = null;
    _currentInterface = null;
  }

  @Override
  public void enterSni_tunnel(Sni_tunnelContext ctx) {
    String name = ctx.TUNNEL().getText();
    _currentParentInterface = _configuration.getInterfaces().computeIfAbsent(name, Interface::new);
    _currentInterface = _currentParentInterface;
    defineStructure(INTERFACE, name, ctx);
  }

  @Override
  public void exitSni_tunnel(Sni_tunnelContext ctx) {
    _currentParentInterface = null;
    _currentInterface = null;
  }

  @Override
  public void enterSni_vlan(Sni_vlanContext ctx) {
    String name = ctx.VLAN().getText();
    _currentParentInterface = _configuration.getInterfaces().computeIfAbsent(name, Interface::new);
    _currentInterface = _currentParentInterface;
    defineStructure(INTERFACE, name, ctx);
  }

  @Override
  public void exitSni_vlan(Sni_vlanContext ctx) {
    _currentParentInterface = null;
    _currentInterface = null;
  }

  @Override
  public void enterSnicp_global_protect(Snicp_global_protectContext ctx) {
    String name = ctx.name.getText();
    _currentCrytoProfile = _configuration.getCryptoProfileOrCreate(name, Type.GLOBAL_PROTECT_APP);
  }

  @Override
  public void exitSnicp_global_protect(Snicp_global_protectContext ctx) {
    _currentCrytoProfile = null;
  }

  @Override
  public void enterSnicp_ike_crypto_profiles(Snicp_ike_crypto_profilesContext ctx) {
    String name = ctx.name.getText();
    _currentCrytoProfile = _configuration.getCryptoProfileOrCreate(name, Type.IKE);
  }

  @Override
  public void exitSnicp_ike_crypto_profiles(Snicp_ike_crypto_profilesContext ctx) {
    _currentCrytoProfile = null;
  }

  @Override
  public void enterSnicp_ipsec_crypto_profiles(Snicp_ipsec_crypto_profilesContext ctx) {
    String name = ctx.name.getText();
    _currentCrytoProfile = _configuration.getCryptoProfileOrCreate(name, Type.IPSEC);
  }

  @Override
  public void exitSnicp_ipsec_crypto_profiles(Snicp_ipsec_crypto_profilesContext ctx) {
    _currentCrytoProfile = null;
  }

  @Override
  public void exitSnie_link_state(Snie_link_stateContext ctx) {
    _currentInterface.setActive((ctx.DOWN() == null));
  }

  @Override
  public void exitSniel3_ip(Sniel3_ipContext ctx) {
    InterfaceAddress address = new InterfaceAddress(ctx.address.getText());
    _currentInterface.setAddress(address);
    _currentInterface.getAllAddresses().add(address);
  }

  @Override
  public void exitSniel3_mtu(Sniel3_mtuContext ctx) {
    _currentInterface.setMtu(Integer.parseInt(ctx.mtu.getText()));
  }

  @Override
  public void enterSniel3_unit(Sniel3_unitContext ctx) {
    String name = ctx.name.getText();
    _currentInterface = _currentParentInterface.getUnits().computeIfAbsent(name, Interface::new);
    _currentInterface.setParent(_currentParentInterface);
    defineStructure(INTERFACE, name, ctx);
  }

  @Override
  public void exitSniel3_unit(Sniel3_unitContext ctx) {
    _currentInterface = _currentParentInterface;
  }

  @Override
  public void exitSniel3u_tag(Sniel3u_tagContext ctx) {
    _currentInterface.setTag(toInteger(ctx.tag));
  }

  @Override
  public void enterSnil_unit(Snil_unitContext ctx) {
    String name = ctx.name.getText();
    _currentInterface = _currentParentInterface.getUnits().computeIfAbsent(name, Interface::new);
    _currentInterface.setParent(_currentParentInterface);
    defineStructure(INTERFACE, name, ctx);
  }

  @Override
  public void exitSnil_unit(Snil_unitContext ctx) {
    _currentInterface = _currentParentInterface;
  }

  @Override
  public void enterSnit_unit(Snit_unitContext ctx) {
    String name = ctx.name.getText();
    _currentInterface = _currentParentInterface.getUnits().computeIfAbsent(name, Interface::new);
    _currentInterface.setParent(_currentParentInterface);
    defineStructure(INTERFACE, name, ctx);
  }

  @Override
  public void exitSnit_unit(Snit_unitContext ctx) {
    _currentInterface = _currentParentInterface;
  }

  @Override
  public void enterSniv_unit(Sniv_unitContext ctx) {
    String name = ctx.name.getText();
    _currentInterface = _currentParentInterface.getUnits().computeIfAbsent(name, Interface::new);
    _currentInterface.setParent(_currentParentInterface);
    defineStructure(INTERFACE, name, ctx);
  }

  @Override
  public void exitSniv_unit(Sniv_unitContext ctx) {
    _currentInterface = _currentParentInterface;
  }

  @Override
  public void enterSnvr_routing_table(Snvr_routing_tableContext ctx) {
    _currentStaticRoute =
        _currentVirtualRouter
            .getStaticRoutes()
            .computeIfAbsent(getText(ctx.name), StaticRoute::new);
  }

  @Override
  public void exitSnvr_interface(Snvr_interfaceContext ctx) {
    for (Variable_list_itemContext var : ctx.variable_list().variable_list_item()) {
      String name = var.getText();
      _currentVirtualRouter.getInterfaceNames().add(name);
      _configuration.referenceStructure(
          INTERFACE, name, VIRTUAL_ROUTER_INTERFACE, getLine(var.start));
    }
  }

  @Override
  public void exitSnvr_routing_table(Snvr_routing_tableContext ctx) {
    _currentStaticRoute = null;
  }

  @Override
  public void exitSnvrrt_admin_dist(Snvrrt_admin_distContext ctx) {
    _currentStaticRoute.setAdminDistance(Integer.parseInt(ctx.distance.getText()));
  }

  @Override
  public void exitSnvrrt_destination(Snvrrt_destinationContext ctx) {
    _currentStaticRoute.setDestination(Prefix.parse(ctx.destination.getText()));
  }

  @Override
  public void exitSnvrrt_interface(Snvrrt_interfaceContext ctx) {
    String name = ctx.iface.getText();
    _currentStaticRoute.setNextHopInterface(name);
    _configuration.referenceStructure(
        INTERFACE, name, STATIC_ROUTE_INTERFACE, getLine(ctx.iface.start));
  }

  @Override
  public void exitSnvrrt_metric(Snvrrt_metricContext ctx) {
    _currentStaticRoute.setMetric(Integer.parseInt(ctx.metric.getText()));
  }

  @Override
  public void exitSnvrrt_nexthop(Snvrrt_nexthopContext ctx) {
    _currentStaticRoute.setNextHopIp(Ip.parse(ctx.address.getText()));
  }

  @Override
  public void enterSr_security(Sr_securityContext ctx) {
    String name = getText(ctx.name);
    _currentRule = _currentVsys.getRules().computeIfAbsent(name, n -> new Rule(n, _currentVsys));

    // Use constructed name so same-named defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    defineStructure(RULE, uniqueName, ctx);
    _configuration.referenceStructure(RULE, uniqueName, RULE_SELF_REF, getLine(ctx.name.start));
  }

  @Override
  public void exitSr_security(Sr_securityContext ctx) {
    _currentRule = null;
  }

  @Override
  public void exitSrs_action(Srs_actionContext ctx) {
    if (ctx.ALLOW() != null) {
      _currentRule.setAction(LineAction.PERMIT);
    } else {
      _currentRule.setAction(LineAction.DENY);
    }
  }

  @Override
  public void exitSrs_application(Srs_applicationContext ctx) {
    for (Variable_list_itemContext var : ctx.variable_list().variable_list_item()) {
      String name = getText(var);
      if (!name.equals(CATCHALL_APPLICATION_NAME)) {
        _configuration.referenceStructure(
            APPLICATION_GROUP_OR_APPLICATION, name, RULE_APPLICATION, getLine(var.start));
      }
      _currentRule.getApplications().add(name);
    }
  }

  @Override
  public void exitSrs_description(Srs_descriptionContext ctx) {
    _currentRule.setDescription(ctx.description.getText());
  }

  @Override
  public void exitSrs_destination(Srs_destinationContext ctx) {
    for (Src_or_dst_list_itemContext var : ctx.src_or_dst_list().src_or_dst_list_item()) {
      RuleEndpoint endpoint = toRuleEndpoint(var);
      _currentRule.getDestination().add(endpoint);

      // Use constructed object name so same-named refs across vsys are unique
      String uniqueName = computeObjectName(_currentVsys.getName(), endpoint.getValue());

      // At this time, don't know if something that looks like a constant (e.g. IP address) is a
      // reference or not.  So mark a reference to a very permissive abstract structure type.
      PaloAltoStructureType type = ADDRESS_GROUP_OR_ADDRESS_OBJECT_OR_NONE;
      if (endpoint.getType() == RuleEndpoint.Type.REFERENCE) {
        // We know this reference doesn't look like a valid constant, so it must be pointing to an
        // object/group
        type = ADDRESS_GROUP_OR_ADDRESS_OBJECT;
      }
      _configuration.referenceStructure(type, uniqueName, RULE_DESTINATION, getLine(var.start));
    }
  }

  @Override
  public void exitSrs_disabled(Srs_disabledContext ctx) {
    _currentRule.setDisabled(ctx.YES() != null);
  }

  @Override
  public void exitSrs_from(Srs_fromContext ctx) {
    for (Variable_list_itemContext var : ctx.variable_list().variable_list_item()) {
      String zoneName = var.getText();
      _currentRule.getFrom().add(zoneName);

      if (!zoneName.equals(CATCHALL_ZONE_NAME)) {
        // Use constructed object name so same-named refs across vsys are unique
        String uniqueName = computeObjectName(_currentVsys.getName(), zoneName);
        _configuration.referenceStructure(ZONE, uniqueName, RULE_FROM_ZONE, getLine(var.start));
      }
    }
  }

  @Override
  public void exitSrs_service(Srs_serviceContext ctx) {
    for (Variable_list_itemContext var : ctx.variable_list().variable_list_item()) {
      String serviceName = var.getText();
      _currentRule.getService().add(new ServiceOrServiceGroupReference(serviceName));
      referenceService(var, RULEBASE_SERVICE);
    }
  }

  @Override
  public void exitSrs_source(Srs_sourceContext ctx) {
    for (Src_or_dst_list_itemContext var : ctx.src_or_dst_list().src_or_dst_list_item()) {
      RuleEndpoint endpoint = toRuleEndpoint(var);
      _currentRule.getSource().add(endpoint);
      // Use constructed object name so same-named refs across vsys are unique
      String uniqueName = computeObjectName(_currentVsys.getName(), endpoint.getValue());

      // At this time, don't know if something that looks like a constant (e.g. IP address) is a
      // reference or not.  So mark a reference to a very permissive abstract structure type.
      PaloAltoStructureType type = ADDRESS_GROUP_OR_ADDRESS_OBJECT_OR_NONE;
      if (endpoint.getType() == RuleEndpoint.Type.REFERENCE) {
        type = ADDRESS_GROUP_OR_ADDRESS_OBJECT;
      }
      _configuration.referenceStructure(type, uniqueName, RULE_SOURCE, getLine(var.start));
    }
  }

  @Override
  public void exitSrs_to(Srs_toContext ctx) {
    for (Variable_list_itemContext var : ctx.variable_list().variable_list_item()) {
      String zoneName = getText(var);
      _currentRule.getTo().add(zoneName);

      if (!zoneName.equals(CATCHALL_ZONE_NAME)) {
        // Use constructed object name so same-named refs across vsys are unique
        String uniqueName = computeObjectName(_currentVsys.getName(), zoneName);
        _configuration.referenceStructure(ZONE, uniqueName, RULE_TO_ZONE, getLine(var.start));
      }
    }
  }

  @Override
  public void enterS_service(S_serviceContext ctx) {
    String name = getText(ctx.name);
    _currentService = _currentVsys.getServices().computeIfAbsent(name, Service::new);

    // Use constructed service name so same-named defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    defineStructure(PaloAltoStructureType.SERVICE, uniqueName, ctx);
  }

  @Override
  public void exitS_service(S_serviceContext ctx) {
    _currentService = null;
  }

  @Override
  public void exitSserv_description(Sserv_descriptionContext ctx) {
    _currentService.setDescription(getText(ctx.description));
  }

  @Override
  public void exitSserv_port(Sserv_portContext ctx) {
    for (Port_or_rangeContext item : ctx.variable_port_list().port_or_range()) {
      if (item.port != null) {
        _currentService.addPort(toInteger(item.port));
      } else {
        assert item.range != null;
        _currentService.addPorts(new SubRange(item.range.getText()));
      }
    }
  }

  @Override
  public void exitSserv_protocol(Sserv_protocolContext ctx) {
    if (ctx.SCTP() != null) {
      _currentService.setProtocol(IpProtocol.SCTP);
    } else if (ctx.TCP() != null) {
      _currentService.setProtocol(IpProtocol.TCP);
    } else if (ctx.UDP() != null) {
      _currentService.setProtocol(IpProtocol.UDP);
    }
  }

  @Override
  public void exitSserv_source_port(Sserv_source_portContext ctx) {
    for (Port_or_rangeContext item : ctx.variable_port_list().port_or_range()) {
      if (item.port != null) {
        _currentService.addSourcePort(toInteger(item.port));
      } else {
        assert item.range != null;
        _currentService.addSourcePorts(new SubRange(item.range.getText()));
      }
    }
  }

  @Override
  public void enterS_service_group(S_service_groupContext ctx) {
    String name = getText(ctx.name);
    _currentServiceGroup = _currentVsys.getServiceGroups().computeIfAbsent(name, ServiceGroup::new);

    // Use constructed service-group name so same-named defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    defineStructure(SERVICE_GROUP, uniqueName, ctx);
  }

  @Override
  public void exitS_service_group(S_service_groupContext ctx) {
    _currentServiceGroup = null;
  }

  @Override
  public void exitSservgrp_members(Sservgrp_membersContext ctx) {
    for (Variable_list_itemContext var : ctx.variable_list().variable_list_item()) {
      String name = getText(var);
      _currentServiceGroup.getReferences().add(new ServiceOrServiceGroupReference(name));
      referenceService(var, SERVICE_GROUP_MEMBER);
    }
  }

  @Override
  public void enterS_shared(S_sharedContext ctx) {
    _currentVsys = _configuration.getVirtualSystems().computeIfAbsent(SHARED_VSYS_NAME, Vsys::new);
  }

  @Override
  public void exitS_shared(S_sharedContext ctx) {
    _currentVsys = _defaultVsys;
  }

  @Override
  public void enterSsl_syslog(Ssl_syslogContext ctx) {
    _currentSyslogServerGroupName = getText(ctx.name);
  }

  @Override
  public void exitSsl_syslog(Ssl_syslogContext ctx) {
    _currentSyslogServerGroupName = null;
  }

  @Override
  public void enterSsls_server(Ssls_serverContext ctx) {
    _currentSyslogServer =
        _currentVsys.getSyslogServer(_currentSyslogServerGroupName, getText(ctx.name));
  }

  @Override
  public void exitSsls_server(Ssls_serverContext ctx) {
    _currentSyslogServer = null;
  }

  @Override
  public void exitSslss_server(Sslss_serverContext ctx) {
    _currentSyslogServer.setAddress(ctx.address.getText());
  }

  @Override
  public void enterS_vsys(S_vsysContext ctx) {
    _currentVsys =
        _configuration.getVirtualSystems().computeIfAbsent(ctx.name.getText(), Vsys::new);
  }

  @Override
  public void exitS_vsys(S_vsysContext ctx) {
    _currentVsys = _defaultVsys;
  }

  @Override
  public void exitSzn_layer3(Szn_layer3Context ctx) {
    if (ctx.variable_list() != null) {
      for (Variable_list_itemContext var : ctx.variable_list().variable_list_item()) {
        String name = var.getText();
        _currentZone.getInterfaceNames().add(name);
        _configuration.referenceStructure(INTERFACE, name, ZONE_INTERFACE, getLine(var.start));
      }
    }
  }

  public PaloAltoConfiguration getConfiguration() {
    return _configuration;
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();
    int line = getLine(token);
    _configuration.setUnrecognized(true);

    if (token instanceof UnrecognizedLineToken) {
      UnrecognizedLineToken unrecToken = (UnrecognizedLineToken) token;
      _w.getParseWarnings()
          .add(
              new ParseWarning(
                  line, lineText, unrecToken.getParserContext(), "This syntax is unrecognized"));
    } else {
      String msg = String.format("Unrecognized Line: %d: %s", line, lineText);
      _w.redFlag(msg + " SUBSEQUENT LINES MAY NOT BE PROCESSED CORRECTLY");
    }
  }
}
