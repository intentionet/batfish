package org.batfish.grammar.fortios;

import static org.batfish.grammar.fortios.FortiosLexer.UNQUOTED_WORD_CHARS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishListener;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.fortios.FortiosParser.Address_nameContext;
import org.batfish.grammar.fortios.FortiosParser.Address_namesContext;
import org.batfish.grammar.fortios.FortiosParser.Address_typeContext;
import org.batfish.grammar.fortios.FortiosParser.Allow_or_denyContext;
import org.batfish.grammar.fortios.FortiosParser.Bgp_asContext;
import org.batfish.grammar.fortios.FortiosParser.Bgp_neighbor_idContext;
import org.batfish.grammar.fortios.FortiosParser.Bgp_remote_asContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_editContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_renameContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_set_allow_routingContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_set_associated_interfaceContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_set_commentContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_set_end_ipContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_set_fabric_objectContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_set_interfaceContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_set_start_ipContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_set_subnetContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_set_typeContext;
import org.batfish.grammar.fortios.FortiosParser.Cfa_set_wildcardContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_append_dstaddrContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_append_dstintfContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_append_serviceContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_append_srcaddrContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_append_srcintfContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_editContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_set_actionContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_set_commentsContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_set_dstaddrContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_set_dstintfContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_set_nameContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_set_serviceContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_set_srcaddrContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_set_srcintfContext;
import org.batfish.grammar.fortios.FortiosParser.Cfp_set_statusContext;
import org.batfish.grammar.fortios.FortiosParser.Cfsc_editContext;
import org.batfish.grammar.fortios.FortiosParser.Cfsc_renameContext;
import org.batfish.grammar.fortios.FortiosParser.Cfsc_set_commentContext;
import org.batfish.grammar.fortios.FortiosParser.Cfsc_set_icmpcodeContext;
import org.batfish.grammar.fortios.FortiosParser.Cfsc_set_icmptypeContext;
import org.batfish.grammar.fortios.FortiosParser.Cfsc_set_protocolContext;
import org.batfish.grammar.fortios.FortiosParser.Cfsc_set_protocol_numberContext;
import org.batfish.grammar.fortios.FortiosParser.Cfsc_set_sctp_portrangeContext;
import org.batfish.grammar.fortios.FortiosParser.Cfsc_set_tcp_portrangeContext;
import org.batfish.grammar.fortios.FortiosParser.Cfsc_set_udp_portrangeContext;
import org.batfish.grammar.fortios.FortiosParser.Crbcn_editContext;
import org.batfish.grammar.fortios.FortiosParser.Crbcne_set_remote_asContext;
import org.batfish.grammar.fortios.FortiosParser.Crbcr_set_statusContext;
import org.batfish.grammar.fortios.FortiosParser.Crs_editContext;
import org.batfish.grammar.fortios.FortiosParser.Crs_set_deviceContext;
import org.batfish.grammar.fortios.FortiosParser.Crs_set_distanceContext;
import org.batfish.grammar.fortios.FortiosParser.Crs_set_dstContext;
import org.batfish.grammar.fortios.FortiosParser.Crs_set_gatewayContext;
import org.batfish.grammar.fortios.FortiosParser.Crs_set_sdwanContext;
import org.batfish.grammar.fortios.FortiosParser.Crs_set_statusContext;
import org.batfish.grammar.fortios.FortiosParser.Cs_replacemsgContext;
import org.batfish.grammar.fortios.FortiosParser.Csg_hostnameContext;
import org.batfish.grammar.fortios.FortiosParser.Csi_editContext;
import org.batfish.grammar.fortios.FortiosParser.Csi_set_aliasContext;
import org.batfish.grammar.fortios.FortiosParser.Csi_set_descriptionContext;
import org.batfish.grammar.fortios.FortiosParser.Csi_set_ipContext;
import org.batfish.grammar.fortios.FortiosParser.Csi_set_mtuContext;
import org.batfish.grammar.fortios.FortiosParser.Csi_set_mtu_overrideContext;
import org.batfish.grammar.fortios.FortiosParser.Csi_set_statusContext;
import org.batfish.grammar.fortios.FortiosParser.Csi_set_typeContext;
import org.batfish.grammar.fortios.FortiosParser.Csi_set_vdomContext;
import org.batfish.grammar.fortios.FortiosParser.Csi_set_vrfContext;
import org.batfish.grammar.fortios.FortiosParser.Csr_set_bufferContext;
import org.batfish.grammar.fortios.FortiosParser.Csr_unset_bufferContext;
import org.batfish.grammar.fortios.FortiosParser.Csz_append_interfaceContext;
import org.batfish.grammar.fortios.FortiosParser.Csz_editContext;
import org.batfish.grammar.fortios.FortiosParser.Csz_renameContext;
import org.batfish.grammar.fortios.FortiosParser.Csz_set_descriptionContext;
import org.batfish.grammar.fortios.FortiosParser.Csz_set_interfaceContext;
import org.batfish.grammar.fortios.FortiosParser.Csz_set_intrazoneContext;
import org.batfish.grammar.fortios.FortiosParser.Device_hostnameContext;
import org.batfish.grammar.fortios.FortiosParser.Double_quoted_stringContext;
import org.batfish.grammar.fortios.FortiosParser.Enable_or_disableContext;
import org.batfish.grammar.fortios.FortiosParser.Fortios_configurationContext;
import org.batfish.grammar.fortios.FortiosParser.Interface_aliasContext;
import org.batfish.grammar.fortios.FortiosParser.Interface_nameContext;
import org.batfish.grammar.fortios.FortiosParser.Interface_namesContext;
import org.batfish.grammar.fortios.FortiosParser.Interface_or_zone_nameContext;
import org.batfish.grammar.fortios.FortiosParser.Interface_or_zone_namesContext;
import org.batfish.grammar.fortios.FortiosParser.Interface_typeContext;
import org.batfish.grammar.fortios.FortiosParser.Ip_addressContext;
import org.batfish.grammar.fortios.FortiosParser.Ip_address_with_mask_or_prefixContext;
import org.batfish.grammar.fortios.FortiosParser.Ip_prefixContext;
import org.batfish.grammar.fortios.FortiosParser.Ip_protocol_numberContext;
import org.batfish.grammar.fortios.FortiosParser.Ip_wildcardContext;
import org.batfish.grammar.fortios.FortiosParser.Ipv6_addressContext;
import org.batfish.grammar.fortios.FortiosParser.MtuContext;
import org.batfish.grammar.fortios.FortiosParser.Policy_actionContext;
import org.batfish.grammar.fortios.FortiosParser.Policy_nameContext;
import org.batfish.grammar.fortios.FortiosParser.Policy_numberContext;
import org.batfish.grammar.fortios.FortiosParser.Policy_statusContext;
import org.batfish.grammar.fortios.FortiosParser.Port_rangeContext;
import org.batfish.grammar.fortios.FortiosParser.Replacemsg_major_typeContext;
import org.batfish.grammar.fortios.FortiosParser.Replacemsg_minor_typeContext;
import org.batfish.grammar.fortios.FortiosParser.Route_distanceContext;
import org.batfish.grammar.fortios.FortiosParser.Service_nameContext;
import org.batfish.grammar.fortios.FortiosParser.Service_namesContext;
import org.batfish.grammar.fortios.FortiosParser.Service_port_rangeContext;
import org.batfish.grammar.fortios.FortiosParser.Service_port_rangesContext;
import org.batfish.grammar.fortios.FortiosParser.Service_protocolContext;
import org.batfish.grammar.fortios.FortiosParser.Single_quoted_stringContext;
import org.batfish.grammar.fortios.FortiosParser.StrContext;
import org.batfish.grammar.fortios.FortiosParser.Subnet_maskContext;
import org.batfish.grammar.fortios.FortiosParser.Uint16Context;
import org.batfish.grammar.fortios.FortiosParser.Uint8Context;
import org.batfish.grammar.fortios.FortiosParser.Up_or_downContext;
import org.batfish.grammar.fortios.FortiosParser.VrfContext;
import org.batfish.grammar.fortios.FortiosParser.WordContext;
import org.batfish.grammar.fortios.FortiosParser.Zone_nameContext;
import org.batfish.representation.fortios.Address;
import org.batfish.representation.fortios.BatfishUUID;
import org.batfish.representation.fortios.BgpNeighbor;
import org.batfish.representation.fortios.FortiosConfiguration;
import org.batfish.representation.fortios.FortiosStructureType;
import org.batfish.representation.fortios.FortiosStructureUsage;
import org.batfish.representation.fortios.Interface;
import org.batfish.representation.fortios.Interface.Type;
import org.batfish.representation.fortios.Policy;
import org.batfish.representation.fortios.Policy.Action;
import org.batfish.representation.fortios.Policy.Status;
import org.batfish.representation.fortios.Replacemsg;
import org.batfish.representation.fortios.Service;
import org.batfish.representation.fortios.Service.Protocol;
import org.batfish.representation.fortios.StaticRoute;
import org.batfish.representation.fortios.Zone;
import org.batfish.representation.fortios.Zone.IntrazoneAction;

/**
 * Given a parse tree, builds a {@link FortiosConfiguration} that has been prepopulated with
 * metadata and defaults by {@link FortiosPreprocessor}.
 */
public final class FortiosConfigurationBuilder extends FortiosParserBaseListener
    implements BatfishListener {

  public FortiosConfigurationBuilder(
      String text,
      FortiosCombinedParser parser,
      Warnings warnings,
      FortiosConfiguration configuration) {
    _text = text;
    _parser = parser;
    _w = warnings;
    _c = configuration;
  }

  /** Get a new, unique BatfishUUID. */
  public @Nonnull BatfishUUID getUUID() {
    return new BatfishUUID(_uuidSequenceNumber++);
  }

  @Override
  public String getInputText() {
    return _text;
  }

  @Override
  public BatfishCombinedParser<?, ?> getParser() {
    return _parser;
  }

  @Override
  public Warnings getWarnings() {
    return _w;
  }

  @Override
  public void exitFortios_configuration(Fortios_configurationContext ctx) {
    // After renaming is complete, generate object names from UUIDs
    for (Policy policy : _c.getPolicies().values()) {
      policy.setSrcAddr(
          policy.getSrcAddrUUIDs().stream()
              .map(u -> _c.getRenameableObjects().get(u).getName())
              .collect(ImmutableSet.toImmutableSet()));
      policy.setDstAddr(
          policy.getDstAddrUUIDs().stream()
              .map(u -> _c.getRenameableObjects().get(u).getName())
              .collect(ImmutableSet.toImmutableSet()));
      policy.setService(
          policy.getServiceUUIDs().stream()
              .map(u -> _c.getRenameableObjects().get(u).getName())
              .collect(ImmutableSet.toImmutableSet()));
      policy.setDstIntfZones(
          policy.getDstIntfZoneUUIDs().stream()
              .map(u -> _c.getRenameableObjects().get(u).getName())
              .collect(ImmutableSet.toImmutableSet()));
      policy.setSrcIntfZones(
          policy.getSrcIntfZoneUUIDs().stream()
              .map(u -> _c.getRenameableObjects().get(u).getName())
              .collect(ImmutableSet.toImmutableSet()));
    }
  }

  @Override
  public void exitCsg_hostname(Csg_hostnameContext ctx) {
    toString(ctx, ctx.host).ifPresent(_c::setHostname);
  }

  @Override
  public void enterCs_replacemsg(Cs_replacemsgContext ctx) {
    String majorType = toString(ctx.major_type);
    Optional<String> maybeMinorType = toString(ctx, ctx.minor_type);
    if (!maybeMinorType.isPresent()) {
      _currentReplacemsg = new Replacemsg(); // dummy
      return;
    }
    _currentReplacemsg =
        _c.getReplacemsgs()
            .computeIfAbsent(majorType, n -> new HashMap<>())
            .computeIfAbsent(maybeMinorType.get(), n -> new Replacemsg());
  }

  @Override
  public void exitCs_replacemsg(Cs_replacemsgContext ctx) {
    _currentReplacemsg = null;
  }

  @Override
  public void exitCsr_set_buffer(Csr_set_bufferContext ctx) {
    _currentReplacemsg.setBuffer(toString(ctx.buffer));
  }

  @Override
  public void exitCsr_unset_buffer(Csr_unset_bufferContext ctx) {
    _currentReplacemsg.setBuffer(null);
  }

  @Override
  public void exitCfa_rename(Cfa_renameContext ctx) {
    Optional<String> currentNameOpt = toString(ctx, ctx.current_name);
    Optional<String> newNameOpt = toString(ctx, ctx.new_name);
    assert currentNameOpt.isPresent();
    assert newNameOpt.isPresent();

    String currentName = currentNameOpt.get();
    String newName = newNameOpt.get();
    if (!_c.getAddresses().containsKey(currentName)) {
      warnRenameNonExistent(ctx, currentName, FortiosStructureType.ADDRESS);
      return;
    }
    // TODO check addrgrp as well, once that exists
    if (_c.getAddresses().containsKey(newName)) {
      // TODO handle conflicting renames
      warnRenameConflict(ctx, currentName, newName, FortiosStructureType.ADDRESS);
      return;
    }
    // Rename refs / def
    _c.renameStructure(
        currentName,
        newName,
        FortiosStructureType.ADDRESS,
        ImmutableSet.of(FortiosStructureType.ADDRESS, FortiosStructureType.ADDRGRP));
    // Rename the object itself
    Address currentAddress = _c.getAddresses().remove(currentName);
    currentAddress.setName(newName);
    _c.getAddresses().put(newName, currentAddress);
    // Add the rename as part of the def
    _c.defineStructure(FortiosStructureType.ADDRESS, newName, ctx);
  }

  @Override
  public void enterCfa_edit(FortiosParser.Cfa_editContext ctx) {
    Optional<String> name = toString(ctx, ctx.address_name());
    Address existingAddress = name.map(_c.getAddresses()::get).orElse(null);
    if (existingAddress != null) {
      // Make a clone to edit
      _currentAddress = SerializationUtils.clone(existingAddress);
    } else {
      _currentAddress = new Address(toString(ctx.address_name().str()), getUUID());
    }
    _currentAddressNameValid = name.isPresent();
  }

  @Override
  public void exitCfa_edit(Cfa_editContext ctx) {
    // If edited address is valid, add/update the entry in VS addresses map.
    String invalidReason = addressValid(_currentAddress, _currentAddressNameValid);
    if (invalidReason == null) {
      _c.defineStructure(FortiosStructureType.ADDRESS, _currentAddress.getName(), ctx);
      _c.getAddresses().put(_currentAddress.getName(), _currentAddress);
      _c.getRenameableObjects().put(_currentAddress.getBatfishUUID(), _currentAddress);
    } else {
      warn(ctx, String.format("Address edit block ignored: %s", invalidReason));
    }
    _currentAddress = null;
  }

  @Override
  public void exitCfa_set_allow_routing(Cfa_set_allow_routingContext ctx) {
    _currentAddress.setAllowRouting(toBoolean(ctx.value));
    todo(ctx);
  }

  @Override
  public void exitCfa_set_associated_interface(Cfa_set_associated_interfaceContext ctx) {
    Optional<String> optName = toString(ctx, ctx.name);
    if (!optName.isPresent()) {
      return;
    }
    // Permitted zone names are a superset of permitted interface names, so at this point we know
    // the name is a valid zone name, but it may or may not be a valid interface name.
    String name = optName.get();

    // TODO after zone support: If zone exists, set _currentAddress's associatedZone and return.

    if (!_c.getInterfaces().containsKey(name)) {
      warn(ctx, "No interface or zone named " + name);
      // TODO File undefined reference to zone, or INTERFACE_OR_ZONE if it's a valid interface name
      return;
    }

    // TODO Add structure reference for interface
    todo(ctx);
    _currentAddress.setAssociatedInterface(name);
  }

  @Override
  public void exitCfa_set_comment(Cfa_set_commentContext ctx) {
    _currentAddress.setComment(toString(ctx.comment));
  }

  @Override
  public void exitCfa_set_fabric_object(Cfa_set_fabric_objectContext ctx) {
    todo(ctx);
    _currentAddress.setFabricObject(toBoolean(ctx.value));
  }

  @Override
  public void exitCfa_set_start_ip(Cfa_set_start_ipContext ctx) {
    if (_currentAddress.getType() == Address.Type.IPRANGE) {
      _currentAddress.getTypeSpecificFields().setIp1(toIp(ctx.ip));
    } else {
      warn(ctx, "Cannot set start-ip for address type " + _currentAddress.getTypeEffective());
    }
  }

  @Override
  public void exitCfa_set_end_ip(Cfa_set_end_ipContext ctx) {
    if (_currentAddress.getType() == Address.Type.IPRANGE) {
      _currentAddress.getTypeSpecificFields().setIp2(toIp(ctx.ip));
    } else {
      warn(ctx, "Cannot set end-ip for address type " + _currentAddress.getTypeEffective());
    }
  }

  @Override
  public void exitCfa_set_interface(Cfa_set_interfaceContext ctx) {
    if (_currentAddress.getType() != Address.Type.INTERFACE_SUBNET) {
      warn(ctx, "Cannot set interface for address type " + _currentAddress.getTypeEffective());
      return;
    }
    Optional<String> name = toString(ctx, ctx.name);
    if (name.isPresent()) {
      if (_c.getInterfaces().containsKey(name.get())) {
        _currentAddress.getTypeSpecificFields().setInterface(name.get());
      } else {
        // TODO File undefined reference to interface
        warn(ctx, "No interface named " + name.get());
      }
    }
  }

  @Override
  public void exitCfa_set_subnet(Cfa_set_subnetContext ctx) {
    Address.Type currentType = _currentAddress.getTypeEffective();
    if (currentType == Address.Type.IPMASK || currentType == Address.Type.INTERFACE_SUBNET) {
      if (ctx.subnet.ip_prefix() != null) {
        Prefix prefix = toPrefix(ctx.subnet.ip_prefix());
        _currentAddress.getTypeSpecificFields().setIp1(prefix.getStartIp());
        // getPrefixWildcard returns a mask where the 1s indicate bits that DON'T matter,
        // so invert it to get the correct mask for FortiOS
        _currentAddress.getTypeSpecificFields().setIp2(prefix.getPrefixWildcard().inverted());
      } else {
        assert ctx.subnet.ip != null && ctx.subnet.mask != null;
        // Convert to wildcard to get canonicalized IP (CLI automatically zeroes out bits in the IP
        // that are zeros in the mask, even if the mask is invalid).
        IpWildcard wildcard = toIpWildcard(ctx.subnet.ip, ctx.subnet.mask);
        _currentAddress.getTypeSpecificFields().setIp1(wildcard.getIp());
        _currentAddress.getTypeSpecificFields().setIp2(toIp(ctx.subnet.mask));
      }
    } else {
      warn(ctx, "Cannot set subnet for address type " + currentType);
    }
  }

  @Override
  public void exitCfa_set_type(Cfa_set_typeContext ctx) {
    _currentAddress.setType(toAddressType(ctx.type));
  }

  @Override
  public void exitCfa_set_wildcard(Cfa_set_wildcardContext ctx) {
    if (_currentAddress.getType() == Address.Type.WILDCARD) {
      // Convert to wildcard; canonicalizes IP based on mask bits
      IpWildcard wildcard = toIpWildcard(ctx.wildcard);
      // Set IP and mask in _currentAddress. Invert mask bits because IpWildcard interprets set bits
      // as "don't matter" while FortiOS interprets unset bits as "don't matter".
      _currentAddress.getTypeSpecificFields().setIp1(wildcard.getIp());
      _currentAddress.getTypeSpecificFields().setIp2(wildcard.getWildcardMaskAsIp().inverted());
    } else {
      warn(ctx, "Cannot set wildcard for address type " + _currentAddress.getTypeEffective());
    }
  }

  @Override
  public void enterCsi_edit(Csi_editContext ctx) {
    Optional<String> name = toString(ctx, ctx.interface_name());
    Interface existing = name.map(_c.getInterfaces()::get).orElse(null);
    if (existing == null) {
      // TODO edit block validation / committing
      _currentInterface = new Interface(toString(ctx.interface_name().str()));
      return;
    }
    _currentInterface = existing;
  }

  @Override
  public void exitCsi_edit(Csi_editContext ctx) {
    // TODO better validation
    String name = _currentInterface.getName();
    if (_c.getZones().containsKey(name)) {
      warn(ctx, "Interface edit block ignored: name conflicts with a zone name");
    } else if (INTERFACE_NAME_PATTERN.matcher(name).matches()) {
      _c.defineStructure(FortiosStructureType.INTERFACE, name, ctx);
      _c.referenceStructure(
          FortiosStructureType.INTERFACE,
          name,
          FortiosStructureUsage.INTERFACE_SELF_REF,
          ctx.start.getLine());
      _c.getInterfaces().put(name, _currentInterface);
    }
    _currentInterface = null;
  }

  @Override
  public void exitCsi_set_vdom(Csi_set_vdomContext ctx) {
    _currentInterface.setVdom(toString(ctx.vdom));
  }

  @Override
  public void exitCsi_set_ip(Csi_set_ipContext ctx) {
    _currentInterface.setIp(toConcreteInterfaceAddress(ctx.ip));
  }

  @Override
  public void exitCsi_set_type(Csi_set_typeContext ctx) {
    _currentInterface.setType(toInterfaceType(ctx.type));
  }

  @Override
  public void exitCsi_set_alias(Csi_set_aliasContext ctx) {
    toString(ctx, ctx.alias).ifPresent(s -> _currentInterface.setAlias(s));
  }

  @Override
  public void exitCsi_set_status(Csi_set_statusContext ctx) {
    _currentInterface.setStatus(toStatus(ctx.status));
  }

  @Override
  public void exitCsi_set_mtu_override(Csi_set_mtu_overrideContext ctx) {
    _currentInterface.setMtuOverride(toBoolean(ctx.value));
  }

  @Override
  public void exitCsi_set_description(Csi_set_descriptionContext ctx) {
    _currentInterface.setDescription(toString(ctx.description));
  }

  @Override
  public void exitCsi_set_mtu(Csi_set_mtuContext ctx) {
    toInteger(ctx, ctx.value).ifPresent(m -> _currentInterface.setMtu(m));
  }

  @Override
  public void exitCsi_set_vrf(Csi_set_vrfContext ctx) {
    toInteger(ctx, ctx.value).ifPresent(v -> _currentInterface.setVrf(v));
  }

  @Override
  public void exitCrb_set_as(FortiosParser.Crb_set_asContext ctx) {
    toLong(ctx, ctx.bgp_as()).ifPresent(_c.getBgpProcess()::setAs);
  }

  @Override
  public void exitCrb_set_router_id(FortiosParser.Crb_set_router_idContext ctx) {
    Ip routerId = toIp(ctx.router_id);
    if (routerId.equals(Ip.ZERO)) {
      warn(ctx, "Cannot use 0.0.0.0 as BGP router-id");
    } else {
      _c.getBgpProcess().setRouterId(routerId);
    }
  }

  @Override
  public void enterCrbcn_edit(Crbcn_editContext ctx) {
    Optional<Ip> neighborId = toIp(ctx, ctx.bgp_neighbor_id());
    BgpNeighbor existing = neighborId.map(_c.getBgpProcess().getNeighbors()::get).orElse(null);
    if (existing != null) {
      // Make a clone to edit
      _currentBgpNeighbor = SerializationUtils.clone(existing);
    } else {
      // If neighbor ID can't be interpreted as an IP, make a dummy BgpNeighbor.
      // TODO: Shouldn't need a dummy once neighbor ID is strictly parsed as an IP.
      // Also, the BgpNeighbor constructor can then enforce that neighbor ID isn't 0.0.0.0.
      _currentBgpNeighbor = new BgpNeighbor(neighborId.orElse(Ip.ZERO));
    }
  }

  @Override
  public void exitCrbcn_edit(Crbcn_editContext ctx) {
    String invalidReason = bgpNeighborValid(_currentBgpNeighbor);
    if (invalidReason == null) {
      _c.getBgpProcess().getNeighbors().put(_currentBgpNeighbor.getIp(), _currentBgpNeighbor);
    } else {
      warn(ctx, String.format("BGP neighbor edit block ignored: %s", invalidReason));
    }
    _currentBgpNeighbor = null;
  }

  @Override
  public void exitCrbcne_set_remote_as(Crbcne_set_remote_asContext ctx) {
    toLong(ctx, ctx.bgp_remote_as()).ifPresent(_currentBgpNeighbor::setRemoteAs);
  }

  @Override
  public void exitCrbcr_set_status(Crbcr_set_statusContext ctx) {
    if (toBoolean(ctx.enable_or_disable())) {
      warn(ctx, "Redistribution into BGP is not yet supported");
    }
  }

  @Override
  public void enterCrs_edit(Crs_editContext ctx) {
    Optional<Long> routeNum = toLong(ctx, ctx.route_num());
    StaticRoute existing =
        routeNum.map(Object::toString).map(_c.getStaticRoutes()::get).orElse(null);
    if (existing != null) {
      // Make a clone to edit
      _currentStaticRoute = SerializationUtils.clone(existing);
    } else {
      _currentStaticRoute = new StaticRoute(toString(ctx.route_num().str()));
    }
    _currentStaticRouteNumValid = routeNum.isPresent();
  }

  @Override
  public void exitCrs_edit(Crs_editContext ctx) {
    String invalidReason = staticRouteValid(_currentStaticRoute, _currentStaticRouteNumValid);
    if (invalidReason == null) {
      _c.getStaticRoutes().put(_currentStaticRoute.getSeqNum(), _currentStaticRoute);
    } else {
      warn(ctx, String.format("Static route edit block ignored: %s", invalidReason));
    }
    _currentStaticRoute = null;
  }

  @Override
  public void exitCrs_set_device(Crs_set_deviceContext ctx) {
    toInterface(ctx, ctx.iface, FortiosStructureUsage.STATIC_ROUTE_DEVICE)
        .ifPresent(_currentStaticRoute::setDevice);
  }

  @Override
  public void exitCrs_set_distance(Crs_set_distanceContext ctx) {
    toInteger(ctx, ctx.route_distance()).ifPresent(_currentStaticRoute::setDistance);
  }

  @Override
  public void exitCrs_set_dst(Crs_set_dstContext ctx) {
    _currentStaticRoute.setDst(toPrefix(ctx.dst));
  }

  @Override
  public void exitCrs_set_gateway(Crs_set_gatewayContext ctx) {
    _currentStaticRoute.setGateway(toIp(ctx.gateway));
  }

  @Override
  public void exitCrs_set_sdwan(Crs_set_sdwanContext ctx) {
    _currentStaticRoute.setSdwanEnabled(toBoolean(ctx.enabled));
  }

  @Override
  public void exitCrs_set_status(Crs_set_statusContext ctx) {
    _currentStaticRoute.setStatus(
        toBoolean(ctx.enabled) ? StaticRoute.Status.ENABLE : StaticRoute.Status.DISABLE);
  }

  @Override
  public void enterCfp_edit(Cfp_editContext ctx) {
    Optional<Long> number = toLong(ctx, ctx.policy_number());
    Policy existing = number.map(Object::toString).map(_c.getPolicies()::get).orElse(null);
    if (existing != null) {
      // Make a clone to edit
      _currentPolicy = SerializationUtils.clone(existing);
    } else {
      _currentPolicy = new Policy(toString(ctx.policy_number().str()));
    }
    _currentPolicyValid = number.isPresent();
  }

  @Override
  public void exitCfp_edit(Cfp_editContext ctx) {
    // If edited policy is valid, add/update the entry in VS map
    String number = _currentPolicy.getNumber();
    String invalidReason = policyValid(_currentPolicy, _currentPolicyValid);
    if (invalidReason == null) { // policy is valid
      _c.defineStructure(FortiosStructureType.POLICY, number, ctx);
      _c.referenceStructure(
          FortiosStructureType.POLICY,
          number,
          FortiosStructureUsage.POLICY_SELF_REF,
          ctx.start.getLine());
      _c.getPolicies().put(number, _currentPolicy);
    } else {
      warn(ctx, String.format("Policy edit block ignored: %s", invalidReason));
    }
    _currentPolicy = null;
  }

  @Override
  public void exitCfp_set_action(Cfp_set_actionContext ctx) {
    _currentPolicy.setAction(toAction(ctx.policy_action()));
  }

  @Override
  public void exitCfp_set_comments(Cfp_set_commentsContext ctx) {
    _currentPolicy.setComments(toString(ctx.comments));
  }

  @Override
  public void exitCfp_set_name(Cfp_set_nameContext ctx) {
    toString(ctx, ctx.name).ifPresent(_currentPolicy::setName);
  }

  @Override
  public void exitCfp_set_status(Cfp_set_statusContext ctx) {
    _currentPolicy.setStatus(toStatus(ctx.status));
  }

  // List items
  @Override
  public void exitCfp_set_dstaddr(Cfp_set_dstaddrContext ctx) {
    toAddressUUIDs(ctx.addresses, FortiosStructureUsage.POLICY_DSTADDR)
        .ifPresent(
            addresses -> {
              Set<BatfishUUID> addrs = _currentPolicy.getDstAddrUUIDs();
              addrs.clear();
              addrs.addAll(addresses);
            });
  }

  @Override
  public void exitCfp_set_srcaddr(Cfp_set_srcaddrContext ctx) {
    toAddressUUIDs(ctx.addresses, FortiosStructureUsage.POLICY_SRCADDR)
        .ifPresent(
            addresses -> {
              Set<BatfishUUID> addrs = _currentPolicy.getSrcAddrUUIDs();
              addrs.clear();
              addrs.addAll(addresses);
            });
  }

  @Override
  public void exitCfp_set_dstintf(Cfp_set_dstintfContext ctx) {
    toInterfacesAndZones(ctx.interfaces, FortiosStructureUsage.POLICY_DSTINTF, false)
        .ifPresent(
            i -> {
              Set<String> ifaces = _currentPolicy.getDstIntf();
              ifaces.clear();
              ifaces.addAll(i.getInterfaces());

              Set<BatfishUUID> zones = _currentPolicy.getDstIntfZoneUUIDs();
              zones.clear();
              zones.addAll(i.getZones());
            });
  }

  @Override
  public void exitCfp_set_srcintf(Cfp_set_srcintfContext ctx) {
    toInterfacesAndZones(ctx.interfaces, FortiosStructureUsage.POLICY_SRCINTF, true)
        .ifPresent(
            i -> {
              Set<String> ifaces = _currentPolicy.getSrcIntf();
              ifaces.clear();
              ifaces.addAll(i.getInterfaces());

              Set<BatfishUUID> zones = _currentPolicy.getSrcIntfZoneUUIDs();
              zones.clear();
              zones.addAll(i.getZones());
            });
  }

  @Override
  public void exitCfp_set_service(Cfp_set_serviceContext ctx) {
    toServiceUUIDs(ctx.services, FortiosStructureUsage.POLICY_SERVICE)
        .ifPresent(
            s -> {
              Set<BatfishUUID> service = _currentPolicy.getServiceUUIDs();
              service.clear();
              service.addAll(s);
            });
  }

  @Override
  public void exitCfp_append_dstaddr(Cfp_append_dstaddrContext ctx) {
    toAddressUUIDs(ctx.addresses, FortiosStructureUsage.POLICY_DSTADDR)
        .ifPresent(
            a -> {
              Set<BatfishUUID> addrs = _currentPolicy.getDstAddrUUIDs();
              addrs.addAll(a);
            });
  }

  @Override
  public void exitCfp_append_srcaddr(Cfp_append_srcaddrContext ctx) {
    toAddressUUIDs(ctx.addresses, FortiosStructureUsage.POLICY_SRCADDR)
        .ifPresent(
            a -> {
              Set<BatfishUUID> addrs = _currentPolicy.getSrcAddrUUIDs();
              addrs.addAll(a);
            });
  }

  @Override
  public void exitCfp_append_dstintf(Cfp_append_dstintfContext ctx) {
    toInterfacesAndZones(ctx.interfaces, FortiosStructureUsage.POLICY_DSTINTF, false)
        .ifPresent(
            i -> {
              Set<String> ifaces = _currentPolicy.getDstIntf();
              ifaces.addAll(i.getInterfaces());

              Set<BatfishUUID> zones = _currentPolicy.getDstIntfZoneUUIDs();
              zones.addAll(i.getZones());
            });
  }

  @Override
  public void exitCfp_append_srcintf(Cfp_append_srcintfContext ctx) {
    toInterfacesAndZones(ctx.interfaces, FortiosStructureUsage.POLICY_SRCINTF, true)
        .ifPresent(
            i -> {
              Set<String> ifaces = _currentPolicy.getSrcIntf();
              ifaces.addAll(i.getInterfaces());

              Set<BatfishUUID> zones = _currentPolicy.getSrcIntfZoneUUIDs();
              zones.addAll(i.getZones());
            });
  }

  @Override
  public void exitCfp_append_service(Cfp_append_serviceContext ctx) {
    toServiceUUIDs(ctx.services, FortiosStructureUsage.POLICY_SERVICE)
        .ifPresent(
            s -> {
              Set<BatfishUUID> service = _currentPolicy.getServiceUUIDs();
              service.addAll(s);
            });
  }

  @Override
  public void enterCfsc_edit(Cfsc_editContext ctx) {
    Optional<String> name = toString(ctx, ctx.service_name());
    Service existing = name.map(_c.getServices()::get).orElse(null);
    if (existing != null) {
      // Make a clone to edit
      _currentService = SerializationUtils.clone(existing);
    } else {
      _currentService = new Service(toString(ctx.service_name().str()), getUUID());
    }
    _currentServiceValid = name.isPresent();
  }

  @Override
  public void exitCfsc_edit(Cfsc_editContext ctx) {
    String name = _currentService.getName();
    String invalidReason = serviceValid(_currentService, _currentServiceValid);
    if (invalidReason == null) { // service edit block is valid
      _c.getRenameableObjects().put(_currentService.getBatfishUUID(), _currentService);
      _c.defineStructure(FortiosStructureType.SERVICE_CUSTOM, name, ctx);
      _c.getServices().put(name, _currentService);
    } else {
      warn(ctx, String.format("Service edit block ignored: %s", invalidReason));
    }
    _currentService = null;
  }

  @Override
  public void exitCfsc_rename(Cfsc_renameContext ctx) {
    Optional<String> currentNameOpt = toString(ctx, ctx.current_name);
    Optional<String> newNameOpt = toString(ctx, ctx.new_name);
    assert currentNameOpt.isPresent();
    assert newNameOpt.isPresent();

    String currentName = currentNameOpt.get();
    String newName = newNameOpt.get();
    if (!_c.getServices().containsKey(currentName)) {
      warnRenameNonExistent(ctx, currentName, FortiosStructureType.SERVICE_CUSTOM);
      return;
    }
    // TODO check service group as well, once that exists
    if (_c.getServices().containsKey(newName)) {
      // TODO handle conflicting renames
      warnRenameConflict(ctx, currentName, newName, FortiosStructureType.SERVICE_CUSTOM);
      return;
    }
    // Rename refs / def
    _c.renameStructure(
        currentName,
        newName,
        FortiosStructureType.SERVICE_CUSTOM,
        ImmutableSet.of(FortiosStructureType.SERVICE_CUSTOM, FortiosStructureType.SERVICE_GROUP));
    // Rename the object itself
    Service currentService = _c.getServices().remove(currentName);
    currentService.setName(newName);
    _c.getServices().put(newName, currentService);
    // Add the rename as part of the def
    _c.defineStructure(FortiosStructureType.SERVICE_CUSTOM, newName, ctx);
  }

  @Override
  public void exitCfsc_set_comment(Cfsc_set_commentContext ctx) {
    _currentService.setComment(toString(ctx.comment));
  }

  @Override
  public void exitCfsc_set_icmpcode(Cfsc_set_icmpcodeContext ctx) {
    if (_currentService.getProtocolEffective() != Protocol.ICMP
        && _currentService.getProtocolEffective() != Protocol.ICMP6) {
      warn(
          ctx,
          String.format(
              "Cannot set ICMP code for service %s when protocol is not set to ICMP or ICMP6.",
              _currentService.getName()));
      return;
    } else if (_currentService.getIcmpType() == null) {
      warn(
          ctx,
          String.format(
              "Cannot set ICMP code for service %s when ICMP type is not set.",
              _currentService.getName()));
      return;
    }
    _currentService.setIcmpCode(toInteger(ctx.code));
  }

  @Override
  public void exitCfsc_set_icmptype(Cfsc_set_icmptypeContext ctx) {
    if (_currentService.getProtocolEffective() != Protocol.ICMP
        && _currentService.getProtocolEffective() != Protocol.ICMP6) {
      warn(
          ctx,
          String.format(
              "Cannot set ICMP type for service %s when protocol is not set to ICMP or ICMP6.",
              _currentService.getName()));
      return;
    }
    _currentService.setIcmpType(toInteger(ctx.type));
  }

  @Override
  public void exitCfsc_set_protocol(Cfsc_set_protocolContext ctx) {
    _currentService.setProtocol(toProtocol(ctx.protocol));
  }

  @Override
  public void exitCfsc_set_protocol_number(Cfsc_set_protocol_numberContext ctx) {
    if (_currentService.getProtocolEffective() != Protocol.IP) {
      warn(
          ctx,
          String.format(
              "Cannot set IP protocol number for service %s when protocol is not set to IP.",
              _currentService.getName()));
      return;
    }
    toInteger(ctx, ctx.ip_protocol_number()).ifPresent(_currentService::setProtocolNumber);
  }

  @Override
  public void exitCfsc_set_sctp_portrange(Cfsc_set_sctp_portrangeContext ctx) {
    if (_currentService.getProtocolEffective() != Protocol.TCP_UDP_SCTP) {
      warn(
          ctx,
          String.format(
              "Cannot set SCTP port range for service %s when protocol is not set to TCP/UDP/SCTP.",
              _currentService.getName()));
      return;
    }
    _currentService.setSctpPortRangeDst(toDstIntegerSpace(ctx.service_port_ranges()));
    _currentService.setSctpPortRangeSrc(toSrcIntegerSpace(ctx.service_port_ranges()).orElse(null));
  }

  @Override
  public void exitCfsc_set_tcp_portrange(Cfsc_set_tcp_portrangeContext ctx) {
    if (_currentService.getProtocolEffective() != Protocol.TCP_UDP_SCTP) {
      warn(
          ctx,
          String.format(
              "Cannot set TCP port range for service %s when protocol is not set to TCP/UDP/SCTP.",
              _currentService.getName()));
      return;
    }
    _currentService.setTcpPortRangeDst(toDstIntegerSpace(ctx.service_port_ranges()));
    _currentService.setTcpPortRangeSrc(toSrcIntegerSpace(ctx.service_port_ranges()).orElse(null));
  }

  @Override
  public void exitCfsc_set_udp_portrange(Cfsc_set_udp_portrangeContext ctx) {
    if (_currentService.getProtocolEffective() != Protocol.TCP_UDP_SCTP) {
      warn(
          ctx,
          String.format(
              "Cannot set UDP port range for service %s when protocol is not set to TCP/UDP/SCTP.",
              _currentService.getName()));
      return;
    }
    _currentService.setUdpPortRangeDst(toDstIntegerSpace(ctx.service_port_ranges()));
    _currentService.setUdpPortRangeSrc(toSrcIntegerSpace(ctx.service_port_ranges()).orElse(null));
  }

  @Override
  public void enterCsz_edit(Csz_editContext ctx) {
    Optional<String> name = toString(ctx, ctx.zone_name());
    Zone existing = name.map(_c.getZones()::get).orElse(null);
    _currentZoneNameValid = name.isPresent();
    if (existing != null) {
      // Make a clone to edit
      _currentZone = SerializationUtils.clone(existing);
    } else {
      _currentZone = new Zone(toString(ctx.zone_name().str()), getUUID());
    }
  }

  /** Returns message indicating why this zone can't be committed in the CLI, or null if it can */
  private static @Nullable String getZoneInvalidReason(
      Zone zone, boolean nameValid, Set<String> interfaceNames) {
    if (!nameValid) {
      return "name is invalid";
    } else if (interfaceNames.contains(zone.getName())) {
      return "name conflicts with a system interface name";
    } else if (zone.getInterface().isEmpty()) {
      return "interface must be set";
    }
    return null;
  }

  @Override
  public void exitCsz_edit(Csz_editContext ctx) {
    // If edited item is valid, add/update the entry in VS map
    String invalidReason =
        getZoneInvalidReason(_currentZone, _currentZoneNameValid, _c.getInterfaces().keySet());
    if (invalidReason == null) { // is valid
      String name = _currentZone.getName();
      _c.defineStructure(FortiosStructureType.ZONE, name, ctx);
      _c.referenceStructure(
          FortiosStructureType.ZONE,
          name,
          FortiosStructureUsage.ZONE_SELF_REF,
          ctx.start.getLine());
      _c.getZones().put(name, _currentZone);
      _c.getRenameableObjects().put(_currentZone.getBatfishUUID(), _currentZone);
    } else {
      warn(ctx, String.format("Zone edit block ignored: %s", invalidReason));
    }
    _currentZone = null;
  }

  @Override
  public void exitCsz_rename(Csz_renameContext ctx) {
    Optional<String> currentNameOpt = toString(ctx, ctx.current_name);
    Optional<String> newNameOpt = toString(ctx, ctx.new_name);
    if (!newNameOpt.isPresent() || !currentNameOpt.isPresent()) {
      return;
    }

    String currentName = currentNameOpt.get();
    String newName = newNameOpt.get();
    if (!_c.getZones().containsKey(currentName)) {
      warnRenameNonExistent(ctx, currentName, FortiosStructureType.ZONE);
      return;
    }
    if (_c.getZones().containsKey(newName) || _c.getInterfaces().containsKey(newName)) {
      // TODO handle conflicting renames
      warnRenameConflict(ctx, currentName, newName, FortiosStructureType.ZONE);
      return;
    }
    // Rename refs / def
    _c.renameStructure(
        currentName,
        newName,
        FortiosStructureType.ZONE,
        ImmutableSet.of(FortiosStructureType.ZONE, FortiosStructureType.INTERFACE));
    // Rename the object itself
    Zone current = _c.getZones().remove(currentName);
    current.setName(newName);
    _c.getZones().put(newName, current);
    // Add the rename as part of the def
    _c.defineStructure(FortiosStructureType.ZONE, newName, ctx);
  }

  @Override
  public void exitCsz_set_description(Csz_set_descriptionContext ctx) {
    _currentZone.setDescription(toString(ctx.description));
  }

  @Override
  public void exitCsz_set_intrazone(Csz_set_intrazoneContext ctx) {
    _currentZone.setIntrazone(toIntrazoneAction(ctx.value));
  }

  @Override
  public void exitCsz_set_interface(Csz_set_interfaceContext ctx) {
    toZoneInterfaces(ctx.interfaces)
        .ifPresent(
            newInterfaces -> {
              Set<String> ifaces = _currentZone.getInterface();
              ifaces.clear();
              ifaces.addAll(newInterfaces);
            });
  }

  @Override
  public void exitCsz_append_interface(Csz_append_interfaceContext ctx) {
    toZoneInterfaces(ctx.interfaces)
        .ifPresent(newInterfaces -> _currentZone.getInterface().addAll(newInterfaces));
  }

  private IntrazoneAction toIntrazoneAction(Allow_or_denyContext ctx) {
    if (ctx.ALLOW() != null) {
      return IntrazoneAction.ALLOW;
    }
    assert ctx.DENY() != null;
    return IntrazoneAction.DENY;
  }

  /**
   * Generate a list of service UUIDs for the supplied Service_names context. Returns {@link
   * Optional#empty()} if invalid.
   */
  private Optional<Set<BatfishUUID>> toServiceUUIDs(
      Service_namesContext ctx, FortiosStructureUsage usage) {
    int line = ctx.start.getLine();
    Map<String, Service> servicesMap = _c.getServices();
    ImmutableSet.Builder<BatfishUUID> serviceUuidsBuilder = ImmutableSet.builder();
    Set<String> services =
        ctx.service_name().stream()
            .map(n -> toString(n.str()))
            .collect(ImmutableSet.toImmutableSet());
    for (String name : services) {
      if (name.equals(Policy.ALL_SERVICE) && services.size() > 1) {
        warn(ctx, "Cannot combine 'ALL' with other services");
        return Optional.empty();
      }
      if (servicesMap.containsKey(name)) {
        serviceUuidsBuilder.add(servicesMap.get(name).getBatfishUUID());
        _c.referenceStructure(FortiosStructureType.SERVICE_CUSTOM, name, usage, line);
      } else {
        _c.undefined(FortiosStructureType.SERVICE_CUSTOM_OR_SERVICE_GROUP, name, usage, line);
        warn(
            ctx,
            String.format(
                "Service %s is undefined and cannot be added to policy %s",
                name, _currentPolicy.getNumber()));
        return Optional.empty();
      }
    }
    return Optional.of(serviceUuidsBuilder.build());
  }

  /**
   * Generate a list of address UUIDs for the supplied Address_names context. Returns {@link
   * Optional#empty()} if invalid.
   */
  private Optional<Set<BatfishUUID>> toAddressUUIDs(
      Address_namesContext ctx, FortiosStructureUsage usage) {
    int line = ctx.start.getLine();
    Map<String, Address> addressesMap = _c.getAddresses();
    ImmutableSet.Builder<BatfishUUID> addressUuidsBuilder = ImmutableSet.builder();
    Set<String> addresses =
        ctx.address_name().stream()
            .map(n -> toString(n.str()))
            .collect(ImmutableSet.toImmutableSet());
    for (String name : addresses) {
      if (name.equals(Policy.ALL_ADDRESSES) && addresses.size() > 1) {
        warn(ctx, "When 'all' is set together with other address(es), it is removed");
        continue;
      }
      if (addressesMap.containsKey(name)) {
        addressUuidsBuilder.add(addressesMap.get(name).getBatfishUUID());
        _c.referenceStructure(FortiosStructureType.ADDRESS, name, usage, line);
      } else {
        _c.undefined(FortiosStructureType.ADDRESS_OR_ADDRGRP, name, usage, line);
        warn(
            ctx,
            String.format(
                "Address %s is undefined and cannot be added to policy %s",
                name, _currentPolicy.getNumber()));
        return Optional.empty();
      }
    }
    return Optional.of(addressUuidsBuilder.build());
  }

  /**
   * Convert names in the specified context into an optional set of interface names. Returns an
   * empty optional if the line would not be accepted.
   *
   * <p>Note: we are simplifying allowed interface specification here - any valid interface not
   * already zoned is permitted. This means we are ignoring some (buggy) interface-restriction
   * applied by real devices here; specifically: some versions of fortiOS cli will prevent you from
   * using an interface you just removed.
   */
  private Optional<Set<String>> toZoneInterfaces(Interface_namesContext ctx) {
    int line = ctx.start.getLine();
    Map<String, Interface> ifacesMap = _c.getInterfaces();
    String currentZoneName = _currentZone.getName();

    ImmutableSet<String> usedIfaceNames =
        _c.getZones().values().stream()
            .filter(z -> !z.getName().equals(currentZoneName))
            .map(Zone::getInterface)
            .flatMap(Collection::stream)
            .collect(ImmutableSet.toImmutableSet());

    ImmutableSet.Builder<String> ifaceNameBuilder = ImmutableSet.builder();
    Set<String> newIfaces =
        ctx.interface_name().stream()
            .map(n -> toString(n.str()))
            .collect(ImmutableSet.toImmutableSet());

    for (String name : newIfaces) {
      if (usedIfaceNames.contains(name)) {
        warn(
            ctx,
            String.format(
                "Interface %s is already in another zone and cannot be added to zone %s",
                name, currentZoneName));
        return Optional.empty();
      } else if (ifacesMap.containsKey(name)) {
        ifaceNameBuilder.add(name);
        _c.referenceStructure(
            FortiosStructureType.INTERFACE, name, FortiosStructureUsage.ZONE_INTERFACE, line);
      } else {
        _c.undefined(
            FortiosStructureType.INTERFACE, name, FortiosStructureUsage.ZONE_INTERFACE, line);
        warn(
            ctx,
            String.format(
                "Interface %s is undefined and cannot be added to zone %s", name, currentZoneName));
        return Optional.empty();
      }
    }

    return Optional.of(ifaceNameBuilder.build());
  }

  /**
   * Convert specified interface or zone names context into interface and zone identifiers. If
   * {@code pruneAny} is true, then the special 'any' interface will be removed if specified with
   * other interfaces.
   */
  private Optional<InterfacesAndZones> toInterfacesAndZones(
      Interface_or_zone_namesContext ctx, FortiosStructureUsage usage, boolean pruneAny) {
    int line = ctx.start.getLine();
    Map<String, Interface> ifacesMap = _c.getInterfaces();
    Map<String, Zone> zonesMap = _c.getZones();
    ImmutableSet.Builder<String> ifaceNameBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<BatfishUUID> zonesUuidBuilder = ImmutableSet.builder();

    Set<String> ifaces =
        ctx.interface_or_zone_name().stream()
            .map(n -> toString(n.str()))
            .collect(ImmutableSet.toImmutableSet());
    for (String name : ifaces) {
      if (name.equals(Policy.ANY_INTERFACE)) {
        if (pruneAny && ifaces.size() > 1) {
          warn(ctx, "When 'any' is set together with other interfaces, it is removed");
          continue;
        }
        ifaceNameBuilder.add(Policy.ANY_INTERFACE);
      } else if (ifacesMap.containsKey(name)) {
        ifaceNameBuilder.add(name);
        _c.referenceStructure(FortiosStructureType.INTERFACE, name, usage, line);
      } else if (zonesMap.containsKey(name)) {
        zonesUuidBuilder.add(zonesMap.get(name).getBatfishUUID());
        _c.referenceStructure(FortiosStructureType.ZONE, name, usage, line);
      } else {
        _c.undefined(FortiosStructureType.INTERFACE_OR_ZONE, name, usage, line);
        warn(
            ctx,
            String.format(
                "Interface/zone %s is undefined and cannot be added to policy %s",
                name, _currentPolicy.getNumber()));
        return Optional.empty();
      }
    }
    return Optional.of(new InterfacesAndZones(ifaceNameBuilder.build(), zonesUuidBuilder.build()));
  }

  /**
   * Convert specified interface name context into the corresponding interface name, or return empty
   * optional if there is no such interface.
   */
  private Optional<String> toInterface(
      ParserRuleContext ctx, Interface_nameContext ifaceNameCtx, FortiosStructureUsage usage) {
    String ifaceName = toString(ifaceNameCtx.str());
    if (!_c.getInterfaces().containsKey(ifaceName)) {
      warn(ctx, String.format("Interface %s is undefined", ifaceName));
      _c.undefined(FortiosStructureType.INTERFACE, ifaceName, usage, ctx.start.getLine());
      return Optional.empty();
    }
    _c.referenceStructure(FortiosStructureType.INTERFACE, ifaceName, usage, ctx.start.getLine());
    return Optional.of(ifaceName);
  }

  /**
   * Convert specified service_port_ranges context into an IntegerSpace representing the destination
   * ports specified by the context.
   */
  private @Nonnull IntegerSpace toDstIntegerSpace(Service_port_rangesContext ctx) {
    IntegerSpace.Builder spaces = IntegerSpace.builder();
    for (Service_port_rangeContext range : ctx.service_port_range()) {
      assert range.dst_ports != null;
      spaces.including(toIntegerSpace(range.dst_ports));
    }
    return spaces.build();
  }

  /**
   * Convert specified service_port_ranges context into an optional IntegerSpace representing the
   * source ports specified by the context. An IntegerSpace is only returned if a source port space
   * is specified.
   */
  private Optional<IntegerSpace> toSrcIntegerSpace(Service_port_rangesContext ctx) {
    IntegerSpace.Builder spaces = IntegerSpace.builder();
    boolean isSet = false;
    for (Service_port_rangeContext range : ctx.service_port_range()) {
      if (range.src_ports != null) {
        isSet = true;
        spaces.including(toIntegerSpace(range.src_ports));
      }
    }
    return isSet ? Optional.of(spaces.build()) : Optional.empty();
  }

  private IntegerSpace toIntegerSpace(Port_rangeContext ctx) {
    int low = toInteger(ctx.port_low);
    if (ctx.port_high != null) {
      int high = toInteger(ctx.port_high);
      return IntegerSpace.of(Range.closed(low, high));
    }
    return IntegerSpace.of(low);
  }

  private @Nonnull Policy.Action toAction(Policy_actionContext ctx) {
    if (ctx.ACCEPT() != null) {
      return Action.ACCEPT;
    } else if (ctx.DENY() != null) {
      return Action.DENY;
    } else {
      assert ctx.IPSEC() != null;
      return Action.IPSEC;
    }
  }

  private Service.Protocol toProtocol(Service_protocolContext ctx) {
    if (ctx.ICMP() != null) {
      return Protocol.ICMP;
    } else if (ctx.ICMP6() != null) {
      return Protocol.ICMP6;
    } else if (ctx.IP_UPPER() != null) {
      return Protocol.IP;
    } else {
      assert ctx.TCP_UDP_SCTP() != null;
      return Protocol.TCP_UDP_SCTP;
    }
  }

  private boolean toBoolean(Enable_or_disableContext ctx) {
    if (ctx.ENABLE() != null) {
      return true;
    }
    assert ctx.DISABLE() != null;
    return false;
  }

  private @Nonnull Policy.Status toStatus(Policy_statusContext ctx) {
    return toBoolean(ctx.enable_or_disable()) ? Status.ENABLE : Status.DISABLE;
  }

  private Interface.Status toStatus(Up_or_downContext ctx) {
    if (ctx.UP() != null) {
      return Interface.Status.UP;
    }
    assert ctx.DOWN() != null;
    return Interface.Status.DOWN;
  }

  private Address.Type toAddressType(Address_typeContext ctx) {
    if (ctx.INTERFACE_SUBNET() != null) {
      return Address.Type.INTERFACE_SUBNET;
    } else if (ctx.IPMASK() != null) {
      return Address.Type.IPMASK;
    } else if (ctx.IPRANGE() != null) {
      return Address.Type.IPRANGE;
    } else if (ctx.WILDCARD() != null) {
      return Address.Type.WILDCARD;
    } else if (ctx.DYNAMIC() != null) {
      return Address.Type.DYNAMIC;
    } else if (ctx.FQDN() != null) {
      return Address.Type.FQDN;
    } else if (ctx.GEOGRAPHY() != null) {
      return Address.Type.GEOGRAPHY;
    } else {
      assert ctx.MAC() != null;
      return Address.Type.MAC;
    }
  }

  private Interface.Type toInterfaceType(Interface_typeContext ctx) {
    if (ctx.AGGREGATE() != null) {
      return Type.AGGREGATE;
    } else if (ctx.EMAC_VLAN() != null) {
      return Type.EMAC_VLAN;
    } else if (ctx.LOOPBACK() != null) {
      return Type.LOOPBACK;
    } else if (ctx.PHYSICAL() != null) {
      return Type.PHYSICAL;
    } else if (ctx.REDUNDANT() != null) {
      return Type.REDUNDANT;
    } else if (ctx.TUNNEL() != null) {
      return Type.TUNNEL;
    } else if (ctx.VLAN() != null) {
      return Type.VLAN;
    } else {
      assert ctx.WL_MESH() != null;
      return Type.WL_MESH;
    }
  }

  private @Nonnull ConcreteInterfaceAddress toConcreteInterfaceAddress(
      Ip_address_with_mask_or_prefixContext ctx) {
    if (ctx.ip_prefix() != null) {
      return ConcreteInterfaceAddress.parse(ctx.ip_prefix().getText());
    } else {
      assert ctx.ip_address() != null && ctx.subnet_mask() != null;
      return ConcreteInterfaceAddress.create(toIp(ctx.ip_address()), toIp(ctx.subnet_mask()));
    }
  }

  private @Nonnull Prefix toPrefix(Ip_address_with_mask_or_prefixContext ctx) {
    if (ctx.ip_prefix() != null) {
      return toPrefix(ctx.ip_prefix());
    } else {
      assert ctx.ip_address() != null && ctx.subnet_mask() != null;
      return Prefix.create(toIp(ctx.ip_address()), toIp(ctx.subnet_mask()));
    }
  }

  private @Nonnull Prefix toPrefix(Ip_prefixContext ctx) {
    return Prefix.parse(ctx.getText());
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Address_nameContext ctx) {
    return toString(messageCtx, ctx.str(), "address name", ADDRESS_NAME_PATTERN);
  }

  private @Nonnull Optional<String> toString(ParserRuleContext messageCtx, Zone_nameContext ctx) {
    return toString(messageCtx, ctx.str(), "zone name", ZONE_NAME_PATTERN);
  }

  private @Nonnull Optional<String> toString(ParserRuleContext messageCtx, Policy_nameContext ctx) {
    return toString(messageCtx, ctx.str(), "policy name", POLICY_NAME_PATTERN);
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Service_nameContext ctx) {
    return toString(messageCtx, ctx.str(), "service name", SERVICE_NAME_PATTERN);
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Interface_nameContext ctx) {
    return toString(messageCtx, ctx.str(), "interface name", INTERFACE_NAME_PATTERN);
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Interface_or_zone_nameContext ctx) {
    return toString(messageCtx, ctx.str(), "zone or interface name", ZONE_NAME_PATTERN);
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Interface_aliasContext ctx) {
    return toString(messageCtx, ctx.str(), "interface alias", INTERFACE_ALIAS_PATTERN);
  }

  private @Nonnull String toString(Replacemsg_major_typeContext ctx) {
    return ctx.getText();
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Replacemsg_minor_typeContext ctx) {
    return toString(messageCtx, ctx.word(), "replacemsg minor type");
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Device_hostnameContext ctx) {
    return toString(messageCtx, ctx.str(), "device hostname", DEVICE_HOSTNAME_PATTERN);
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, StrContext ctx, String type, Pattern pattern) {
    return toString(messageCtx, ctx, type, s -> pattern.matcher(s).matches());
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, StrContext ctx, String type, Predicate<String> predicate) {
    String text = toString(ctx);
    if (!predicate.test(text)) {
      warn(messageCtx, String.format("Illegal value for %s", type));
      return Optional.empty();
    }
    return Optional.of(text);
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, WordContext ctx, String type) {
    return toString(messageCtx, ctx.str(), type, WORD_PATTERN);
  }

  private static @Nonnull String toString(StrContext ctx) {
    /*
     * Extract the text from a str.
     *
     * A str is composed of a sequence of single-quoted strings, double-quoted strings,
     * and unquoted non-whitespace characters.
     * - single-quoted strings do not interpret any characters specially
     * - double-quoted strings recognize the following three escape sequences:
     *   \" -> "
     *   \' -> ' <---Note that single-quotes are canonically escaped in double-quotes, but need not be.
     *   \\ -> \
     *   A backslash followed by any other character is treated as a literal backslash.
     *   So e.g.
     *   \n -> \n <---The letter 'n', not newline.
     * - outside of quotes, a backslash followed by any character other than a newline is stripped.
     *   E.g.
     *   \n -> n
     *   \" -> "
     *   \(space) -> (space)
     *   A backslash followed immediately by a newline character indicates a line continuation.
     *   That is, the backslash and the newline are both stripped.
     */
    return ctx.str_content().children.stream()
        .map(
            child -> {
              if (child instanceof Double_quoted_stringContext) {
                return toString((Double_quoted_stringContext) child);
              } else if (child instanceof Single_quoted_stringContext) {
                return toString((Single_quoted_stringContext) child);
              } else {
                assert child instanceof TerminalNode;
                int type = ((TerminalNode) child).getSymbol().getType();
                assert type == UNQUOTED_WORD_CHARS;
                return ESCAPED_UNQUOTED_CHAR_PATTERN.matcher(child.getText()).replaceAll("$1");
              }
            })
        .collect(Collectors.joining(""));
  }

  private static @Nonnull String toString(Double_quoted_stringContext ctx) {
    if (ctx.text == null) {
      return "";
    }
    String quotedText = ctx.text.getText();
    return ESCAPED_DOUBLE_QUOTED_CHAR_PATTERN.matcher(quotedText).replaceAll("$1");
  }

  private static @Nonnull String toString(Single_quoted_stringContext ctx) {
    return ctx.text != null ? ctx.text.getText() : "";
  }

  private @Nonnull Optional<Long> toLong(ParserRuleContext messageCtx, Bgp_asContext ctx) {
    return toLongInSpace(messageCtx, ctx.str(), BGP_AS_SPACE, "BGP AS");
  }

  private @Nonnull Optional<Long> toLong(ParserRuleContext messageCtx, Bgp_remote_asContext ctx) {
    return toLongInSpace(messageCtx, ctx.str(), BGP_REMOTE_AS_SPACE, "BGP remote AS");
  }

  private @Nonnull Optional<Long> toLong(ParserRuleContext messageCtx, Policy_numberContext ctx) {
    return toLongInSpace(messageCtx, ctx.str(), POLICY_NUMBER_SPACE, "policy number");
  }

  private Optional<Long> toLong(ParserRuleContext ctx, FortiosParser.Route_numContext routeNum) {
    return toLongInSpace(
        ctx, routeNum.str(), STATIC_ROUTE_NUM_SPACE, "static route sequence number");
  }

  private @Nonnull Optional<Long> toLongInSpace(
      ParserRuleContext messageCtx, StrContext ctx, LongSpace space, String name) {
    return toLongInSpace(messageCtx, toString(ctx), space, name);
  }

  /**
   * Convert a {@link String} to a {@link Long} if it represents a number that is contained in the
   * provided {@code space}, or else {@link Optional#empty}.
   */
  private @Nonnull Optional<Long> toLongInSpace(
      ParserRuleContext messageCtx, String str, LongSpace space, String name) {
    Long num = Longs.tryParse(str);
    if (num == null || !space.contains(num)) {
      warn(messageCtx, String.format("Expected %s in range %s, but got '%s'", name, space, str));
      return Optional.empty();
    }
    return Optional.of(num);
  }

  private @Nonnull Optional<Integer> toIntegerInSpace(
      ParserRuleContext messageCtx, Uint8Context ctx, IntegerSpace space, String name) {
    return toIntegerInSpace(messageCtx, ctx.getText(), space, name);
  }

  private @Nonnull Optional<Integer> toIntegerInSpace(
      ParserRuleContext messageCtx, Uint16Context ctx, IntegerSpace space, String name) {
    return toIntegerInSpace(messageCtx, ctx.getText(), space, name);
  }

  /**
   * Convert a {@link String} to an {@link Integer} if it represents a number that is contained in
   * the provided {@code space}, or else {@link Optional#empty}.
   */
  private @Nonnull Optional<Integer> toIntegerInSpace(
      ParserRuleContext messageCtx, String str, IntegerSpace space, String name) {
    Integer num = Ints.tryParse(str);
    if (num == null || !space.contains(num)) {
      warn(messageCtx, String.format("Expected %s in range %s, but got '%d'", name, space, num));
      return Optional.empty();
    }
    return Optional.of(num);
  }

  /** Generate a warning for trying to rename a non-existent structure. */
  private void warnRenameNonExistent(
      ParserRuleContext ctx, String name, FortiosStructureType type) {
    warn(ctx, String.format("Cannot rename non-existent %s %s", type.getDescription(), name));
  }

  /** Generate a warning for trying to rename a structure with a name already in use. */
  private void warnRenameConflict(
      ParserRuleContext ctx, String currentName, String newName, FortiosStructureType type) {
    warn(
        ctx,
        String.format(
            "Renaming %s %s conflicts with an existing object %s, ignoring this rename operation",
            type.getDescription(), currentName, newName));
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    int line = token.getLine();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();
    _c.setUnrecognized(true);

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

  private Optional<Integer> toInteger(ParserRuleContext ctx, Ip_protocol_numberContext num) {
    return toIntegerInSpace(ctx, num.uint8(), IP_PROTOCOL_NUMBER_SPACE, "ip protocol-number");
  }

  private Optional<Integer> toInteger(ParserRuleContext ctx, MtuContext mtu) {
    return toIntegerInSpace(ctx, mtu.uint16(), MTU_SPACE, "mtu");
  }

  private Optional<Integer> toInteger(ParserRuleContext ctx, Route_distanceContext routeDistance) {
    return toIntegerInSpace(
        ctx, routeDistance.uint8(), ADMIN_DISTANCE_SPACE, "route administrative distance");
  }

  private Optional<Integer> toInteger(ParserRuleContext ctx, VrfContext vrf) {
    return toIntegerInSpace(ctx, vrf.uint8(), VRF_SPACE, "vrf");
  }

  private static int toInteger(Subnet_maskContext ctx) {
    return Ip.parse(ctx.getText()).numSubnetBits();
  }

  private static int toInteger(Uint16Context ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private static int toInteger(Uint8Context ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private @Nonnull Optional<Ip> toIp(ParserRuleContext ctx, Bgp_neighbor_idContext neighborIdCtx) {
    String ipStr = toString(neighborIdCtx.str());
    try {
      return Optional.of(Ip.parse(ipStr));
    } catch (IllegalArgumentException e) {
      warn(ctx, String.format("Cannot parse %s as an IP", ipStr));
      return Optional.empty();
    }
  }

  private static @Nonnull Ip toIp(Ip_addressContext ctx) {
    return Ip.parse(ctx.getText());
  }

  private static @Nonnull Ip toIp(Subnet_maskContext ctx) {
    return Ip.parse(ctx.getText());
  }

  /**
   * Creates an {@link IpWildcard} from the given wildcard context. Note that the context's mask is
   * assumed to be in conventional FortiOS format, i.e. 1s indicate bits that matter. The convention
   * in the {@link IpWildcard} class is the opposite, i.e. 0s in the mask indicate bits that matter.
   */
  private static @Nonnull IpWildcard toIpWildcard(Ip_wildcardContext ctx) {
    return toIpWildcard(ctx.ip, ctx.mask);
  }

  /**
   * Creates an {@link IpWildcard} from the given IP and mask. Note that the provided mask is
   * assumed to be in conventional FortiOS format, i.e. 1s indicate bits that matter. The convention
   * in the {@link IpWildcard} class is the opposite, i.e. 0s in the mask indicate bits that matter.
   */
  private static @Nonnull IpWildcard toIpWildcard(Ip_addressContext ip, Ip_addressContext mask) {
    // Invert mask because in FortiOS, bits that are set matter, whereas the opposite is true for
    // the mask in IpWildcard
    return IpWildcard.ipWithWildcardMask(toIp(ip), toIp(mask).inverted());
  }

  private static @Nonnull Ip6 toIp6(Ipv6_addressContext ctx) {
    return Ip6.parse(ctx.getText());
  }

  /** Returns message indicating why address can't be committed in the CLI, or null if it can */
  @VisibleForTesting
  public static @Nullable String addressValid(Address a, boolean nameValid) {
    if (!nameValid) {
      return "name is invalid";
    }
    switch (a.getTypeEffective()) {
      case IPMASK:
        Ip subnetMask = a.getTypeSpecificFields().getIp2Effective();
        if (!subnetMask.isValidNetmask1sLeading()) {
          return String.format("%s is not a valid subnet mask", subnetMask);
        }
        return null;
      case IPRANGE:
        Ip endIp = a.getTypeSpecificFields().getIp2Effective();
        if (endIp.equals(Ip.ZERO)) {
          // This is the warning the CLI gives if end-ip is not set
          return "end-ip cannot be 0";
        }
        Ip startIp = a.getTypeSpecificFields().getIp1Effective();
        if (endIp.asLong() < startIp.asLong()) {
          return "end-ip must be greater than start-ip";
        }
        return null;
      case WILDCARD: // Any IPs are valid for wildcard
      case INTERFACE_SUBNET: // All cases from here on are unsupported
      case DYNAMIC:
      case FQDN:
      case GEOGRAPHY:
      case MAC:
        return null;
      default:
        return String.format("address type %s is unknown", a.getTypeEffective());
    }
  }

  private static @Nullable String bgpNeighborValid(BgpNeighbor bgpNeighbor) {
    if (bgpNeighbor.getIp().equals(Ip.ZERO)) {
      return "neighbor ID is invalid";
    } else if (bgpNeighbor.getRemoteAs() == null) {
      return "remote-as must be set";
    }
    return null;
  }

  /** Returns message indicating why policy can't be committed in the CLI, or null if it can */
  @VisibleForTesting
  public static @Nullable String policyValid(Policy p, boolean valid) {
    // _valid indicates whether any invalid lines have gone into current policy that would cause the
    // CLI to pop out of its edit block
    if (!valid) {
      return "name is invalid"; // currently, only invalid name can cause valid to be false
    } else if (p.getSrcIntf().isEmpty() && p.getSrcIntfZoneUUIDs().isEmpty()) {
      return "srcintf must be set";
    } else if (p.getDstIntf().isEmpty() && p.getDstIntfZoneUUIDs().isEmpty()) {
      return "dstintf must be set";
    } else if (p.getSrcAddrUUIDs().isEmpty()) {
      return "srcaddr must be set";
    } else if (p.getDstAddrUUIDs().isEmpty()) {
      return "dstaddr must be set";
    } else if (p.getServiceUUIDs().isEmpty()) {
      return "service must be set";
    }
    // TODO "schedule" must be set to commit policy, but we don't parse it. Should we?
    return null;
  }

  /** Returns message indicating why service can't be committed in the CLI, or null if it can */
  @VisibleForTesting
  public static @Nullable String serviceValid(Service s, boolean valid) {
    // Indicates whether any invalid lines have gone into current service that would cause the
    // CLI to pop out of its edit block
    if (!valid) {
      return "name is invalid"; // currently, only invalid name can cause valid to be false
    }
    // TODO Check validity of _ipRange; it is not yet used in conversion
    switch (s.getProtocolEffective()) {
      case TCP_UDP_SCTP:
        if (s.getTcpPortRangeDst() == null
            && s.getUdpPortRangeDst() == null
            && s.getSctpPortRangeDst() == null) {
          return "TCP/UDP/SCTP portrange cannot all be empty";
        }
        return null;
      case ICMP:
      case ICMP6:
        // both ICMP type and ICMP code are allowed to be unset
      case IP:
        // protocol-number is allowed to be unset
        return null;
      default:
        return String.format("protocol %s is unknown", s.getProtocolEffective());
    }
  }

  private static @Nullable String staticRouteValid(StaticRoute staticRoute, boolean seqNumValid) {
    if (!seqNumValid) {
      return "sequence number is invalid";
    } else if (staticRoute.getDevice() == null) {
      return "device must be set";
    }
    return null;
  }

  /**
   * Class representing a set of interfaces as well as a set of zones. For use with firewall
   * policies srcintf and dstintf references.
   */
  private static final class InterfacesAndZones {
    Set<String> getInterfaces() {
      return _interfaces;
    }

    Set<BatfishUUID> getZones() {
      return _zones;
    }

    InterfacesAndZones(Set<String> interfaces, Set<BatfishUUID> zones) {
      _interfaces = ImmutableSet.copyOf(interfaces);
      _zones = ImmutableSet.copyOf(zones);
    }

    private final Set<String> _interfaces;
    private final Set<BatfishUUID> _zones;
  }

  private static final Pattern ADDRESS_NAME_PATTERN = Pattern.compile("^[^\r\n]{1,79}$");
  private static final Pattern DEVICE_HOSTNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
  private static final Pattern ESCAPED_DOUBLE_QUOTED_CHAR_PATTERN =
      Pattern.compile("\\\\(['\"\\\\])");
  private static final Pattern ESCAPED_UNQUOTED_CHAR_PATTERN = Pattern.compile("\\\\([^\\r\\n])");
  private static final Pattern INTERFACE_NAME_PATTERN = Pattern.compile("^[^\r\n]{1,15}$");
  private static final Pattern INTERFACE_ALIAS_PATTERN = Pattern.compile("^[^\r\n]{0,25}$");
  private static final Pattern POLICY_NAME_PATTERN = Pattern.compile("^[^\r\n]{1,35}$");
  private static final Pattern SERVICE_NAME_PATTERN = Pattern.compile("^[^\r\n]{1,79}$");
  private static final Pattern WORD_PATTERN = Pattern.compile("^[^ \t\r\n]+$");
  private static final Pattern ZONE_NAME_PATTERN = Pattern.compile("^[^\r\n]{1,35}$");

  private static final LongSpace BGP_AS_SPACE = LongSpace.of(Range.closed(0L, 4294967295L));
  private static final LongSpace BGP_REMOTE_AS_SPACE = LongSpace.of(Range.closed(1L, 4294967295L));
  private static final IntegerSpace IP_PROTOCOL_NUMBER_SPACE =
      IntegerSpace.of(Range.closed(0, 254));
  private static final IntegerSpace MTU_SPACE = IntegerSpace.of(Range.closed(68, 65535));
  private static final LongSpace POLICY_NUMBER_SPACE = LongSpace.of(Range.closed(0L, 4294967294L));
  private static final IntegerSpace ADMIN_DISTANCE_SPACE = IntegerSpace.of(Range.closed(1, 255));
  private static final LongSpace STATIC_ROUTE_NUM_SPACE =
      LongSpace.of(Range.closed(0L, 4294967295L));
  private static final IntegerSpace VRF_SPACE = IntegerSpace.of(Range.closed(0, 31));

  private Address _currentAddress;
  /**
   * Whether the current address has invalid lines that would prevent committing the address in CLI.
   * This field being true does not guarantee the current address is valid; use {@link
   * #addressValid(Address, boolean)}.
   */
  private boolean _currentAddressNameValid;

  private BgpNeighbor _currentBgpNeighbor;
  private Interface _currentInterface;
  private Policy _currentPolicy;
  /**
   * Whether the current policy has invalid lines that would prevent committing the policy in CLI.
   * This field being true does not guarantee the current policy is valid; use {@link
   * #policyValid(Policy, boolean)}.
   */
  private boolean _currentPolicyValid;

  private Replacemsg _currentReplacemsg;
  private Service _currentService;
  /**
   * Whether the current service has invalid lines that would prevent committing the service in CLI.
   * This field being true does not guarantee the current service is valid; use {@link
   * #serviceValid(Service, boolean)}.
   */
  private boolean _currentServiceValid;

  private StaticRoute _currentStaticRoute;
  private boolean _currentStaticRouteNumValid;
  private Zone _currentZone;
  /** Whether the current zone has an invalid name. */
  private boolean _currentZoneNameValid;

  private final @Nonnull FortiosConfiguration _c;
  private final @Nonnull FortiosCombinedParser _parser;
  private final @Nonnull String _text;
  // Internal sequence number to generate unique UUIDs for structure that may be renamed or cloned
  private int _uuidSequenceNumber = 0;
  private final @Nonnull Warnings _w;
}
