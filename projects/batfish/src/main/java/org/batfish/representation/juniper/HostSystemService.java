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
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.SubRange;

public enum HostSystemService {
  ALL,
  ANY_SERVICE,
  DHCP,
  DNS,
  FINGER,
  FTP,
  HTTP,
  HTTPS,
  IDENT_RESET,
  IKE,
  LSPING,
  NETCONF,
  NTP,
  PING,
  R2CP,
  REVERSE_SSH,
  REVERSE_TELNET,
  RLOGIN,
  RPM,
  RSH,
  SIP,
  SNMP,
  SNMP_TRAP,
  SSH,
  TELNET,
  TFTP,
  TRACEROUTE,
  XNM_CLEAR_TEXT,
  XNM_SSL;

  private final Supplier<List<IpAccessListLine>> _lines;

  public List<IpAccessListLine> getLines() {
    return _lines.get();
  }

  HostSystemService() {
    _lines = Suppliers.memoize(this::init);
  }

  private List<IpAccessListLine> init() {
    HeaderSpace.Builder headerSpaceBuilder = HeaderSpace.builder();
    switch (this) {
      case ALL:
        {
          ImmutableList.Builder<IpAccessListLine> lines = ImmutableList.builder();
          for (HostSystemService other : values()) {
            if (other != ALL && other != ANY_SERVICE) {
              lines.addAll(other.getLines());
            }
          }
          return lines.build();
        }

      case ANY_SERVICE:
        {
          headerSpaceBuilder.setIpProtocols(ImmutableSortedSet.of(IpProtocol.TCP, IpProtocol.UDP));
          break;
        }

      case DHCP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.UDP))
              .setDstPorts(
                  ImmutableSet.of(
                      new SubRange(NamedPort.BOOTPS_OR_DHCP.number(), NamedPort.BOOTPC.number())));
          break;
        }

      case DNS:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSortedSet.of(IpProtocol.TCP, IpProtocol.UDP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.DOMAIN.number())));
          break;
        }

      case FINGER:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.FINGER.number())));
          break;
        }

      case FTP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.FTP.number())));
          break;
        }

      case HTTP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.HTTP.number())));
          break;
        }

      case HTTPS:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.HTTPS.number())));
          break;
        }

      case IDENT_RESET:
        {
          // TODO: ??? (Juniper documentation is opaque)
          return ImmutableList.of();
        }

      case IKE:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.UDP))
              .setDstPorts(
                  ImmutableSortedSet.of(
                      SubRange.singleton(NamedPort.ISAKMP.number()),
                      SubRange.singleton(NamedPort.NON500_ISAKMP.number())));
          break;
        }

      case LSPING:
        {
          // TODO: ??? (Juniper documentation is missing or hiding for this
          // service)
          return ImmutableList.of();
        }

      case NETCONF:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.NETCONF_SSH.number())));
          break;
        }

      case NTP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.UDP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.NTP.number())));
          break;
        }

      case PING:
        {
          headerSpaceBuilder.setIpProtocols(ImmutableSet.of(IpProtocol.ICMP));
          // TODO: PING (ECHO REQUEST) uses ICMP (an IP Protocol) type 8. need to
          // add support for ICMP types in packet headers
          break;
        }

      case R2CP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.UDP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.R2CP.number())));
          break;
        }

      case REVERSE_SSH:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.REVERSE_SSH.number())));
          break;
        }

      case REVERSE_TELNET:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.REVERSE_TELNET.number())));
          break;
        }

      case RLOGIN:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(
                  ImmutableSet.of(SubRange.singleton(NamedPort.LOGINtcp_OR_WHOudp.number())));
          break;
        }

      case RPM:
        {
          // TODO: It appears there is no default port, and the port must be
          // deduced from other settings. It can be any combination of TCP/UDP
          break;
        }

      case RSH:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(
                  ImmutableSet.of(SubRange.singleton(NamedPort.CMDtcp_OR_SYSLOGudp.number())));
          break;
        }

      case SIP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSortedSet.of(IpProtocol.TCP, IpProtocol.UDP))
              .setDstPorts(
                  ImmutableSet.of(
                      new SubRange(NamedPort.SIP_5060.number(), NamedPort.SIP_5061.number())));
          break;
        }

      case SNMP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.UDP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.SNMP.number())));
          break;
        }

      case SNMP_TRAP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.UDP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.SNMPTRAP.number())));
          break;
        }

      case SSH:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.SSH.number())));
          break;
        }

      case TELNET:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.TELNET.number())));
          break;
        }

      case TFTP:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.UDP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.TFTP.number())));
          break;
        }

      case TRACEROUTE:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.UDP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.TRACEROUTE.number())));
          break;
        }

      case XNM_CLEAR_TEXT:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.XNM_CLEAR_TEXT.number())));
          break;
        }

      case XNM_SSL:
        {
          headerSpaceBuilder
              .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
              .setDstPorts(ImmutableSet.of(SubRange.singleton(NamedPort.XNM_SSL.number())));
          break;
        }

      default:
        {
          throw new BatfishException(
              "missing definition for host-inbound-traffic system-service: \"" + name() + "\"");
        }
    }
    return ImmutableList.of(IpAccessListLine.acceptingHeaderSpace(headerSpaceBuilder.build()));
  }
}
