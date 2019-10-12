package org.batfish.representation.juniper;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.List;
import java.util.function.Supplier;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.NamedPort;
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

  private final Supplier<List<IpAccessListLine>> _lines;

  HostProtocol() {
    _lines = Suppliers.memoize(this::init);
  }

  public List<IpAccessListLine> getLines() {
    return _lines.get();
  }

  private List<IpAccessListLine> init() {
    HeaderSpace.Builder headerSpaceBuilder = HeaderSpace.builder();
    switch (this) {
      case ALL:
        {
          ImmutableList.Builder<IpAccessListLine> lines = ImmutableList.builder();
          for (HostProtocol other : values()) {
            if (other != ALL) {
              lines.addAll(other.getLines());
            }
          }
          return lines.build();
        }

      case BFD:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSortedSet.of(IpProtocol.TCP, IpProtocol.UDP))
              .setDstPorts(
                  ImmutableSet.of(
                      new SubRange(NamedPort.BFD_CONTROL.number(), NamedPort.BFD_ECHO.number())));
          break;
        }

      case BGP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.BGP.number())));
          break;
        }

      case DVMRP:
        {
          // TODO: DVMRP uses IGMP (an IP Protocol) type 3. need to add support
          // for IGMP types in packet headers
          headerSpaceBuilder.setIpProtocols(ImmutableSet.of(IpProtocol.IGMP));
          break;
        }

      case IGMP:
        {
          headerSpaceBuilder.setIpProtocols(ImmutableSet.of(IpProtocol.IGMP));
          break;
        }

      case LDP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSortedSet.of(IpProtocol.TCP, IpProtocol.UDP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.LDP.number())));
          break;
        }

      case MSDP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.MSDP.number())));
          break;
        }

      case NHRP:
        {
          headerSpaceBuilder.setIpProtocols(ImmutableSet.of(IpProtocol.NARP));
          break;
        }

      case OSPF:
        {
          headerSpaceBuilder.setIpProtocols(ImmutableSet.of(IpProtocol.OSPF));
          break;
        }

      case OSPF3:
        {
          // TODO: OSPFv3 is an IPV6-encapsulated protocol
          return ImmutableList.of();
        }

      case PGM:
        {
          headerSpaceBuilder.setIpProtocols(ImmutableSet.of(IpProtocol.PGM));
          break;
        }

      case PIM:
        {
          headerSpaceBuilder.setIpProtocols(ImmutableSet.of(IpProtocol.PIM));
          break;
        }

      case RIP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.UDP))
              .setDstPorts(
                  ImmutableSet.of(SubRange.singleton(NamedPort.EFStcp_OR_RIPudp.number())));
          break;
        }

      case RIPNG:
        {
          // TODO: RIPng is an IPV6-encapsulated protocol
          return ImmutableList.of();
        }

      case ROUTER_DISCOVERY:
        {
          // TODO: ROUTER_DISCOVERY uses ICMP (an IP Protocol) type 9. need to
          // add support
          // for ICMP types in packet headers
          headerSpaceBuilder.setIpProtocols(ImmutableSet.of(IpProtocol.ICMP));
          break;
        }

      case RSVP:
        {
          headerSpaceBuilder.setIpProtocols(
              ImmutableSortedSet.of(IpProtocol.RSVP, IpProtocol.RSVP_E2E_IGNORE));
          break;
        }

      case SAP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.UDP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.SAP.number())))
              .setDstIps(ImmutableSet.of(IpWildcard.parse("224.2.127.254/32")));
          break;
        }

      case VRRP:
        {
          headerSpaceBuilder.setIpProtocols(ImmutableSet.of(IpProtocol.VRRP));
          break;
        }

      default:
        {
          throw new BatfishException(
              "missing definition for host-inbound-traffic protocol: \"" + name() + "\"");
        }
    }
    return ImmutableList.of(IpAccessListLine.acceptingHeaderSpace(headerSpaceBuilder.build()));
  }
}
