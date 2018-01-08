package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum RoutingProtocol {
  AGGREGATE("aggregate"),
  BGP("bgp"),
  CONNECTED("connected"),
  EGP("egp"),
  IBGP("ibgp"),
  IGP("igp"),
  ISIS("isis"),
  ISIS_EL1("isisEL1"),
  ISIS_EL2("isisEL2"),
  ISIS_L1("isisL1"),
  ISIS_L2("isisL2"),
  LDP("ldp"),
  LOCAL("local"),
  MSDP("msdp"),
  OSPF("ospf"),
  OSPF_E1("ospfE1"),
  OSPF_E2("ospfE2"),
  OSPF_IA("ospfIA"),
  OSPF3("ospf3"),
  RIP("rip"),
  RSVP("rsvp"),
  STATIC("static");

  private static final Map<String, RoutingProtocol> _map = buildMap();

  private static Map<String, RoutingProtocol> buildMap() {
    ImmutableMap.Builder<String, RoutingProtocol> map = ImmutableMap.builder();
    for (RoutingProtocol protocol : RoutingProtocol.values()) {
      String protocolName = protocol._protocolName.toLowerCase();
      map.put(protocolName, protocol);
    }
    return map.build();
  }

  @JsonCreator
  public static RoutingProtocol fromProtocolName(String name) {
    RoutingProtocol protocol = _map.get(name.toLowerCase());
    if (protocol == null) {
      throw new BatfishException("No routing protocol with name: \"" + name + "\"");
    }
    return protocol;
  }

  private final String _protocolName;

  RoutingProtocol(String protocolName) {
    _protocolName = protocolName;
  }

  public int getDefaultAdministrativeCost(ConfigurationFormat vendor) {
    switch (this) {
      case AGGREGATE:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            break;
          case AWS:
            break;
          case CADANT:
            break;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            break;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            break;
          case FLAT_VYOS:
          case VYOS:
            break;
          case EMPTY:
          case BLADENETWORK:
          case F5:
          case HOST:
          case IGNORED:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case BGP:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            return 200;
          case AWS:
            return 20;
          case CADANT:
            return 20;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 20;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 170;
          case FLAT_VYOS:
          case VYOS:
            return 20;
          case EMPTY:
          case IGNORED:
          case BLADENETWORK:
          case F5:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case CONNECTED:
        return 0;

      case IBGP:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            return 200;
          case AWS:
            return 200;
          case CADANT:
            return 20;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 200;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 170;
          case FLAT_VYOS:
          case VYOS:
            return 200;
          case EMPTY:
          case IGNORED:
          case BLADENETWORK:
          case F5:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case ISIS_EL1:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            break;
          case AWS:
            return 115;
          case CADANT:
            return 117;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 115;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 160;
          case FLAT_VYOS:
          case VYOS:
            return 115;
          case EMPTY:
          case IGNORED:
          case BLADENETWORK:
          case F5:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case ISIS_EL2:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            break;
          case AWS:
            return 115;
          case CADANT:
            return 118;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 115;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 165;
          case FLAT_VYOS:
          case VYOS:
            return 115;
          case EMPTY:
          case IGNORED:
          case F5:
          case BLADENETWORK:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case ISIS_L1:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            break;
          case AWS:
            return 115;
          case CADANT:
            return 115;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 115;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 15;
          case FLAT_VYOS:
          case VYOS:
            return 115;
          case EMPTY:
          case IGNORED:
          case F5:
          case BLADENETWORK:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case ISIS_L2:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            break;
          case AWS:
            return 115;
          case CADANT:
            return 116;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 115;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 18;
          case FLAT_VYOS:
          case VYOS:
            return 115;
          case EMPTY:
          case IGNORED:
          case F5:
          case BLADENETWORK:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case OSPF:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            return 110;
          case AWS:
            return 110;
          case CADANT:
            // TODO: verify. assumption due to missing information in manual.
            return 110;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 110;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 10;
          case FLAT_VYOS:
          case VYOS:
            return 110;
          case EMPTY:
          case IGNORED:
          case BLADENETWORK:
          case F5:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case OSPF_E1:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            return 110;
          case AWS:
            return 110;
          case CADANT:
            // TODO: verify. assumption based on incrementing IS-IS costs in manual.
            return 112;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 110;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 150;
          case FLAT_VYOS:
          case VYOS:
            return 110;
          case EMPTY:
          case IGNORED:
          case BLADENETWORK:
          case F5:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case OSPF_E2:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            return 110;
          case AWS:
            return 110;
          case CADANT:
            // TODO: verify. assumption based on incrementing IS-IS costs in manual.
            return 113;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 110;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 150;
          case FLAT_VYOS:
          case VYOS:
            return 110;
          case EMPTY:
          case IGNORED:
          case BLADENETWORK:
          case F5:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case OSPF_IA:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            return 110;
          case AWS:
            return 110;
          case CADANT:
            // TODO: verify. assumption based on incrementing IS-IS costs in manual.
            return 111;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 110;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 10;
          case FLAT_VYOS:
          case VYOS:
            return 110;
          case EMPTY:
          case IGNORED:
          case BLADENETWORK:
          case F5:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case RIP:
        switch (vendor) {
          case ALCATEL_AOS:
            break;
          case ARISTA:
            return 120;
          case AWS:
            return 120;
          case CADANT:
            return 120;
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 120;
          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 100;
          case FLAT_VYOS:
          case VYOS:
            return 120;
          case EMPTY:
          case IGNORED:
          case BLADENETWORK:
          case F5:
          case HOST:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
            break;
          default:
            break;
        }
        break;

      case STATIC:
        return 1;

      case EGP:
      case IGP:
      case ISIS:
      case LDP:
      case LOCAL:
      case MSDP:
      case OSPF3:
      case RSVP:
      default:
        break;
    }
    throw new BatfishException(
        "Missing default administrative cost for protocol: '"
            + _protocolName
            + "' for vendor '"
            + vendor
            + "'");
  }

  public int getSummaryAdministrativeCost(ConfigurationFormat vendor) {
    switch (this) {
      case OSPF_IA:
        switch (vendor) {
          case ARISTA:
          case CADANT:
          case CISCO_ASA:
          case CISCO_IOS:
          case CISCO_IOS_XR:
          case CISCO_NX:
          case FORCE10:
          case FOUNDRY:
            return 254;

          case FLAT_JUNIPER:
          case JUNIPER:
          case JUNIPER_SWITCH:
            return 10;

          case ALCATEL_AOS:
          case AWS:
          case BLADENETWORK:
          case EMPTY:
          case F5:
          case FLAT_VYOS:
          case HOST:
          case IGNORED:
          case IPTABLES:
          case MRV:
          case MRV_COMMANDS:
          case MSS:
          case UNKNOWN:
          case VXWORKS:
          case VYOS:
          default:
            throw new BatfishException(
                "Unsupported vendor for OSPF inter-area summary administrative cost: " + vendor);
        }

      case AGGREGATE:
      case BGP:
      case CONNECTED:
      case EGP:
      case IBGP:
      case IGP:
      case ISIS:
      case ISIS_EL1:
      case ISIS_EL2:
      case ISIS_L1:
      case ISIS_L2:
      case LDP:
      case LOCAL:
      case MSDP:
      case OSPF:
      case OSPF3:
      case OSPF_E1:
      case OSPF_E2:
      case RSVP:
      case RIP:
      case STATIC:
      default:
        throw new BatfishException(
            "Unuspported protocol for summary administrative cost: " + protocolName());
    }
  }

  @JsonValue
  public String protocolName() {
    return _protocolName;
  }
}
