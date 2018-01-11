package org.batfish.representation.juniper;

import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;

public enum HostProtocol {
  ALL,
  BFD,
  BGP,
  DVMRP,
  IGMP,
  LDP,
  MSDP,
  NHRP,
  OSPF,
  OSPF3,
  PGM,
  PIM,
  RIP,
  RIPNG,
  ROUTER_DISCOVERY,
  RSVP,
  SAP,
  VRRP;

  private boolean _initialized;

  private List<IpAccessListLine> _lines;

  public List<IpAccessListLine> getLines() {
    init();
    return _lines;
  }

  private synchronized void init() {
    if (_initialized) {
      return;
    }
    _initialized = true;
    _lines = new ArrayList<>();
    switch (this) {
      case ALL:
        {
          for (HostProtocol other : values()) {
            if (other != ALL) {
              _lines.addAll(other.getLines());
            }
          }
          break;
        }

      case BFD:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(ImmutableSortedSet.of(IpProtocol.TCP, IpProtocol.UDP));
          line.setDstPorts(
              Collections.singleton(
                  new SubRange(NamedPort.BFD_CONTROL.number(), NamedPort.BFD_ECHO.number())));
          break;
        }

      case BGP:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.TCP));
          line.setDstPorts(
              Collections.singleton(new SubRange(NamedPort.BGP.number(), NamedPort.BGP.number())));
          break;
        }

      case DVMRP:
        {
          // TODO: DVMRP uses IGMP (an IP Protocol) type 3. need to add support
          // for IGMP types in packet headers
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.IGMP));
          break;
        }

      case IGMP:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.IGMP));
          break;
        }

      case LDP:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(ImmutableSortedSet.of(IpProtocol.TCP, IpProtocol.UDP));
          line.setDstPorts(
              Collections.singleton(new SubRange(NamedPort.LDP.number(), NamedPort.LDP.number())));
          break;
        }

      case MSDP:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.TCP));
          line.setDstPorts(
              Collections.singleton(
                  new SubRange(NamedPort.MSDP.number(), NamedPort.MSDP.number())));
          break;
        }

      case NHRP:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.NARP));
          break;
        }

      case OSPF:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.OSPF));
          break;
        }

      case OSPF3:
        {
          // TODO: OSPFv3 is an IPV6-encapsulated protocol
          break;
        }

      case PGM:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.PGM));
          break;
        }

      case PIM:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.PIM));
          break;
        }

      case RIP:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.UDP));
          line.setDstPorts(
              Collections.singleton(new SubRange(NamedPort.RIP.number(), NamedPort.RIP.number())));
          break;
        }

      case RIPNG:
        {
          // TODO: RIPng is an IPV6-encapsulated protocol
          break;
        }

      case ROUTER_DISCOVERY:
        {
          // TODO: ROUTER_DISCOVERY uses ICMP (an IP Protocol) type 9. need to
          // add support
          // for ICMP types in packet headers
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.ICMP));
          break;
        }

      case RSVP:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(ImmutableSortedSet.of(IpProtocol.RSVP, IpProtocol.RSVP_E2E_IGNORE));
          break;
        }

      case SAP:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.UDP));
          line.setDstPorts(
              Collections.singleton(new SubRange(NamedPort.SAP.number(), NamedPort.SAP.number())));
          line.setDstIps(
              Collections.singleton(new IpWildcard(Prefix.fromString("224.2.127.285/32"))));
          break;
        }

      case VRRP:
        {
          IpAccessListLine line = new IpAccessListLine();
          _lines.add(line);
          line.setIpProtocols(Collections.singleton(IpProtocol.VRRP));
          break;
        }

      default:
        {
          throw new BatfishException(
              "missing definition for host-inbound-traffic protocol: \"" + name() + "\"");
        }
    }

    for (IpAccessListLine line : _lines) {
      line.setAction(LineAction.ACCEPT);
    }
  }
}
