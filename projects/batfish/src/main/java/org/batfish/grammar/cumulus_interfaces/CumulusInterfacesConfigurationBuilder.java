package org.batfish.grammar.cumulus_interfaces;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MacAddress;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.Cumulus_interfaces_configurationContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_addressContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_address_virtualContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_aliasContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_bond_slavesContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_bridge_accessContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_bridge_portsContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_bridge_pvidContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_bridge_vidsContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_bridge_vlan_awareContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_clag_idContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_clagd_backup_ipContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_clagd_peer_ipContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_clagd_sys_macContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_link_speedContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_vlan_idContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_vlan_raw_deviceContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_vrfContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_vrf_tableContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_vxlan_idContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.I_vxlan_local_tunnel_ipContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.Interface_nameContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.NumberContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.S_autoContext;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.S_ifaceContext;
import org.batfish.representation.cumulus.Bridge;
import org.batfish.representation.cumulus.CumulusNcluConfiguration;
import org.batfish.representation.cumulus.CumulusStructureType;
import org.batfish.representation.cumulus.CumulusStructureUsage;
import org.batfish.representation.cumulus.InterfaceClagSettings;
import org.batfish.representation.cumulus_interfaces.Converter;
import org.batfish.representation.cumulus_interfaces.Interface;
import org.batfish.representation.cumulus_interfaces.Interfaces;

/**
 * Populates an {@link Interfaces} from a parse tree from {@link
 * org.batfish.grammar.cumulus_interfaces.CumulusInterfacesCombinedParser}.
 */
public final class CumulusInterfacesConfigurationBuilder
    extends CumulusInterfacesParserBaseListener {
  private final CumulusNcluConfiguration _config;
  private final Interfaces _interfaces = new Interfaces();

  private final CumulusInterfacesCombinedParser _parser;
  private final String _text;
  private final Warnings _w;
  private Interface _currentIface;

  public CumulusInterfacesConfigurationBuilder(
      CumulusNcluConfiguration config,
      CumulusInterfacesCombinedParser parser,
      String text,
      Warnings w) {
    _config = config;
    _parser = parser;
    _text = text;
    _w = w;
  }

  private String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    return _text.substring(start, end + 1);
  }

  @VisibleForTesting
  Interfaces getInterfaces() {
    return _interfaces;
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    int line = token.getLine();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();
    _config.setUnrecognized(true);

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

  // Listener methods
  @Override
  public void exitI_address(I_addressContext ctx) {
    _currentIface.addAddress(ConcreteInterfaceAddress.parse(ctx.IP_PREFIX().getText()));
  }

  @Override
  public void exitI_address_virtual(I_address_virtualContext ctx) {
    _currentIface.setAddressVirtual(
        MacAddress.parse(ctx.MAC_ADDRESS().getText()),
        ConcreteInterfaceAddress.parse(ctx.IP_PREFIX().getText()));
  }

  @Override
  public void exitI_alias(I_aliasContext ctx) {
    _currentIface.setDescription(ctx.TEXT().getText());
  }

  @Override
  public void exitI_vrf_table(I_vrf_tableContext ctx) {
    _currentIface.setVrfTable(ctx.vrf_table_name().getText());
  }

  @Override
  public void exitI_bond_slaves(I_bond_slavesContext ctx) {
    List<Interface_nameContext> interfaceNameCtxs = ctx.interface_name();
    interfaceNameCtxs.forEach(
        ifaceNameCtx ->
            _config.referenceStructure(
                CumulusStructureType.INTERFACE,
                ifaceNameCtx.getText(),
                CumulusStructureUsage.BOND_SLAVE,
                ifaceNameCtx.getStart().getLine()));

    interfaceNameCtxs.forEach(
        slaveCtx -> {
          String slave = slaveCtx.getText();
          String parent = _currentIface.getName();
          String oldParent = _interfaces.getBondSlaveParents().put(slave, parent);
          if (oldParent != null) {
            _w.getParseWarnings()
                .add(
                    new ParseWarning(
                        slaveCtx.getStart().getLine(),
                        slaveCtx.getText(),
                        ctx.getText(),
                        String.format(
                            "Interface %s cannot be the bond-slave of both %s and %s",
                            slave, parent, oldParent)));
            // keep the oldParent
            _interfaces.getBondSlaveParents().put(slave, oldParent);
          }
        });
  }

  @Override
  public void exitI_bridge_access(I_bridge_accessContext ctx) {
    _currentIface.createOrGetBridgeSettings().setAccess(Integer.parseInt(ctx.number().getText()));
  }

  @Override
  public void exitI_bridge_ports(I_bridge_portsContext ctx) {
    List<Interface_nameContext> interfaceNameCtxs = ctx.interface_name();
    interfaceNameCtxs.forEach(
        ifaceNameCtx ->
            _config.referenceStructure(
                CumulusStructureType.INTERFACE,
                ifaceNameCtx.getText(),
                CumulusStructureUsage.BRIDGE_PORT,
                ifaceNameCtx.getStart().getLine()));
    _currentIface.setBridgePorts(
        interfaceNameCtxs.stream()
            .map(RuleContext::getText)
            .collect(ImmutableSet.toImmutableSet()));
  }

  @Override
  public void exitI_bridge_pvid(I_bridge_pvidContext ctx) {
    _currentIface.createOrGetBridgeSettings().setPvid(Integer.parseInt(ctx.vlan_id().getText()));
  }

  @Override
  public void exitI_bridge_vids(I_bridge_vidsContext ctx) {
    List<NumberContext> vidCtxs = ctx.number();
    IntegerSpace vids =
        IntegerSpace.unionOf(
            vidCtxs.stream()
                .map(ParseTree::getText)
                .map(Integer::parseInt)
                .map(Range::singleton)
                .collect(ImmutableList.toImmutableList()));
    _currentIface.createOrGetBridgeSettings().setVids(vids);
  }

  @Override
  public void exitI_bridge_vlan_aware(I_bridge_vlan_awareContext ctx) {
    if (ctx.NO() != null) {
      _w.todo(ctx, getFullText(ctx), _parser);
    }
  }

  @Override
  public void exitI_clag_id(I_clag_idContext ctx) {
    _currentIface.setClagId(Integer.parseInt(ctx.number().getText()));
  }

  @Override
  public void exitI_clagd_backup_ip(I_clagd_backup_ipContext ctx) {
    InterfaceClagSettings clag = _currentIface.createOrGetClagSettings();
    clag.setBackupIp(Ip.parse(ctx.IP_ADDRESS().getText()));
    String vrf = ctx.vrf_name().getText();
    clag.setBackupIpVrf(vrf);
    _config.referenceStructure(
        CumulusStructureType.VRF,
        vrf,
        CumulusStructureUsage.INTERFACE_CLAG_BACKUP_IP_VRF,
        ctx.getStart().getLine());
  }

  @Override
  public void exitI_clagd_peer_ip(I_clagd_peer_ipContext ctx) {
    InterfaceClagSettings clag = _currentIface.createOrGetClagSettings();
    if (ctx.IP_ADDRESS() != null) {
      clag.setPeerIp(Ip.parse(ctx.IP_ADDRESS().getText()));
    } else if (ctx.LINK_LOCAL() != null) {
      clag.setPeerIpLinkLocal(true);
    } else {
      throw new IllegalStateException("clagd-peer-ip without an IP or linklocal");
    }
  }

  @Override
  public void exitI_clagd_sys_mac(I_clagd_sys_macContext ctx) {
    InterfaceClagSettings clag = _currentIface.createOrGetClagSettings();
    clag.setSysMac(MacAddress.parse(ctx.MAC_ADDRESS().getText()));
  }

  @Override
  public void exitI_link_speed(I_link_speedContext ctx) {
    _currentIface.setLinkSpeed(Integer.parseInt(ctx.number().getText()));
  }

  @Override
  public void exitI_vlan_id(I_vlan_idContext ctx) {
    String vlanId = ctx.number().getText();
    _config.defineStructure(CumulusStructureType.VLAN, vlanId, ctx);
    _currentIface.setVlanId(Integer.parseInt(vlanId));
  }

  @Override
  public void exitI_vlan_raw_device(I_vlan_raw_deviceContext ctx) {
    // intentionally not adding a reference to the raw device
    _currentIface.setVlanRawDevice(ctx.interface_name().getText());
  }

  @Override
  public void exitI_vrf(I_vrfContext ctx) {
    String vrf = ctx.vrf_name().getText();
    _currentIface.setVrf(vrf);
    _config.referenceStructure(
        CumulusStructureType.VRF,
        vrf,
        CumulusStructureUsage.INTERFACE_VRF,
        ctx.vrf_name().getStart().getLine());
  }

  @Override
  public void exitI_vxlan_id(I_vxlan_idContext ctx) {
    _currentIface.setVxlanId(Integer.parseInt(ctx.number().getText()));
  }

  @Override
  public void exitI_vxlan_local_tunnel_ip(I_vxlan_local_tunnel_ipContext ctx) {
    _currentIface.setVxlanLocalTunnelIp(Ip.parse(ctx.IP_ADDRESS().getText()));
  }

  @Override
  public void exitS_auto(S_autoContext ctx) {
    String name = ctx.interface_name().getText();
    _interfaces.setAuto(name);
  }

  @Override
  public void enterS_iface(S_ifaceContext ctx) {
    String name = ctx.interface_name().getText();
    _currentIface = _interfaces.createOrGetInterface(name);
  }

  @Override
  public void exitS_iface(S_ifaceContext ctx) {
    _config.defineStructure(_currentIface.getType(), _currentIface.getName(), ctx);
    _currentIface = null;
  }

  @Override
  public void exitCumulus_interfaces_configuration(Cumulus_interfaces_configurationContext ctxt) {
    Converter converter = new Converter(_interfaces);
    Bridge bridge = converter.convertBridge();
    _config.setBridge(bridge != null ? bridge : new Bridge());

    try {
      _config.setInterfaces(converter.convertInterfaces());
    } catch (BatfishException e) {
      _w.redFlag("Error converting interfaces to vendor-specific model");
    }

    try {
      _config.setVlans(converter.convertVlans());
    } catch (BatfishException e) {
      _w.redFlag("Error converting vlans to vendor-specific model");
    }

    try {
      _config.setVrfs(converter.convertVrfs());
    } catch (BatfishException e) {
      _w.redFlag("Error converting vrfs to vendor-specific model");
    }

    try {
      _config.setVxlans(converter.convertVxlans());
    } catch (BatfishException e) {
      _w.redFlag("Error converting vxlans to vendor-specific model");
    }
  }
}
