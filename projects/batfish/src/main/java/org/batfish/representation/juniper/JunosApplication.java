package org.batfish.representation.juniper;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.batfish.common.Warnings;
import org.batfish.common.util.DefinedStructure;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.SubRange;
import org.batfish.representation.juniper.BaseApplication.Term;

public enum JunosApplication implements Application {
  JUNOS_AOL,
  JUNOS_BGP,
  JUNOS_BIFF,
  JUNOS_BOOTPC,
  JUNOS_BOOTPS,
  JUNOS_CHARGEN,
  JUNOS_CIFS,
  JUNOS_CVSPSERVER,
  JUNOS_DHCP_CLIENT,
  JUNOS_DHCP_RELAY,
  JUNOS_DHCP_SERVER,
  JUNOS_DISCARD,
  JUNOS_DNS_TCP,
  JUNOS_DNS_UDP,
  JUNOS_ECHO,
  JUNOS_FINGER,
  JUNOS_FTP,
  JUNOS_GNUTELLA,
  JUNOS_GOPHER,
  JUNOS_GRE,
  JUNOS_GTP,
  JUNOS_H323,
  JUNOS_HTTP,
  JUNOS_HTTP_EXT,
  JUNOS_HTTPS,
  JUNOS_ICMP_ALL,
  JUNOS_ICMP_PING,
  JUNOS_ICMP6_ALL,
  JUNOS_ICMP6_DST_UNREACH_ADDR,
  JUNOS_ICMP6_DST_UNREACH_ADMIN,
  JUNOS_ICMP6_DST_UNREACH_BEYOND,
  JUNOS_ICMP6_DST_UNREACH_PORT,
  JUNOS_ICMP6_DST_UNREACH_ROUTE,
  JUNOS_ICMP6_ECHO_REPLY,
  JUNOS_ICMP6_ECHO_REQUEST,
  JUNOS_ICMP6_PACKET_TO_BIG,
  JUNOS_ICMP6_PARAM_PROB_HEADER,
  JUNOS_ICMP6_PARAM_PROB_NEXTHDR,
  JUNOS_ICMP6_PARAM_PROB_OPTION,
  JUNOS_ICMP6_TIME_EXCEED_REASSEMBLY,
  JUNOS_ICMP6_TIME_EXCEED_TRANSIT,
  JUNOS_IDENT,
  JUNOS_IKE,
  JUNOS_IKE_NAT,
  JUNOS_IMAP,
  JUNOS_IMAPS,
  JUNOS_INTERNET_LOCATOR_SERVICE,
  JUNOS_IRC,
  JUNOS_L2TP,
  JUNOS_LDAP,
  JUNOS_LDP_TCP,
  JUNOS_LDP_UDP,
  JUNOS_LPR,
  JUNOS_MAIL,
  JUNOS_MGCP,
  JUNOS_MGCP_CA,
  JUNOS_MGCP_UA,
  JUNOS_MS_RPC,
  JUNOS_MS_RPC_ANY,
  JUNOS_MS_RPC_EPM,
  JUNOS_MS_RPC_IIS_COM,
  JUNOS_MS_RPC_IIS_COM_1,
  JUNOS_MS_RPC_IIS_COM_ADMINBASE,
  JUNOS_MS_RPC_MSEXCHANGE,
  JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_NSP,
  JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_RFR,
  JUNOS_MS_RPC_MSEXCHANGE_INFO_STORE,
  JUNOS_MS_RPC_TCP,
  JUNOS_MS_RPC_UDP,
  JUNOS_MS_RPC_UUID_ANY_TCP,
  JUNOS_MS_RPC_UUID_ANY_UDP,
  JUNOS_MS_RPC_WMIC,
  JUNOS_MS_RPC_WMIC_ADMIN,
  JUNOS_MS_RPC_WMIC_ADMIN2,
  JUNOS_MS_RPC_WMIC_MGMT,
  JUNOS_MS_RPC_WMIC_WEBM_CALLRESULT,
  JUNOS_MS_RPC_WMIC_WEBM_CLASSOBJECT,
  JUNOS_MS_RPC_WMIC_WEBM_LEVEL1LOGIN,
  JUNOS_MS_RPC_WMIC_WEBM_LOGIN_CLIENTID,
  JUNOS_MS_RPC_WMIC_WEBM_LOGIN_HELPER,
  JUNOS_MS_RPC_WMIC_WEBM_OBJECTSINK,
  JUNOS_MS_RPC_WMIC_WEBM_REFRESHING_SERVICES,
  JUNOS_MS_RPC_WMIC_WEBM_REMOTE_REFRESHER,
  JUNOS_MS_RPC_WMIC_WEBM_SERVICES,
  JUNOS_MS_RPC_WMIC_WEBM_SHUTDOWN,
  JUNOS_MS_SQL,
  JUNOS_MSN,
  JUNOS_NBDS,
  JUNOS_NBNAME,
  JUNOS_NETBIOS_SESSION,
  JUNOS_NFS,
  JUNOS_NFSD_TCP,
  JUNOS_NFSD_UDP,
  JUNOS_NNTP,
  JUNOS_NS_GLOBAL,
  JUNOS_NS_GLOBAL_PRO,
  JUNOS_NSM,
  JUNOS_NTALK,
  JUNOS_NTP,
  JUNOS_OSPF,
  JUNOS_PC_ANYWHERE,
  JUNOS_PERSISTENT_NAT,
  JUNOS_PING,
  JUNOS_PINGV6,
  JUNOS_POP3,
  JUNOS_PPTP,
  JUNOS_PRINTER,
  JUNOS_R2CP,
  JUNOS_RADACCT,
  JUNOS_RADIUS,
  JUNOS_REALAUDIO,
  JUNOS_RIP,
  JUNOS_ROUTING_INBOUND,
  JUNOS_RSH,
  JUNOS_RTSP,
  JUNOS_SCCP,
  JUNOS_SCTP_ANY,
  JUNOS_SIP,
  JUNOS_SMB,
  JUNOS_SMB_SESSION,
  JUNOS_SMTP,
  JUNOS_SNMP_AGENTX,
  JUNOS_SNPP,
  JUNOS_SQL_MONITOR,
  JUNOS_SQLNET_V1,
  JUNOS_SQLNET_V2,
  JUNOS_SSH,
  JUNOS_STUN,
  JUNOS_SUN_RPC,
  JUNOS_SUN_RPC_ANY,
  JUNOS_SUN_RPC_ANY_TCP,
  JUNOS_SUN_RPC_ANY_UDP,
  JUNOS_SUN_RPC_MOUNTD,
  JUNOS_SUN_RPC_MOUNTD_TCP,
  JUNOS_SUN_RPC_MOUNTD_UDP,
  JUNOS_SUN_RPC_NFS,
  JUNOS_SUN_RPC_NFS_ACCESS,
  JUNOS_SUN_RPC_NFS_TCP,
  JUNOS_SUN_RPC_NFS_UDP,
  JUNOS_SUN_RPC_NLOCKMGR,
  JUNOS_SUN_RPC_NLOCKMGR_TCP,
  JUNOS_SUN_RPC_NLOCKMGR_UDP,
  JUNOS_SUN_RPC_PORTMAP,
  JUNOS_SUN_RPC_PORTMAP_TCP,
  JUNOS_SUN_RPC_PORTMAP_UDP,
  JUNOS_SUN_RPC_RQUOTAD,
  JUNOS_SUN_RPC_RQUOTAD_TCP,
  JUNOS_SUN_RPC_RQUOTAD_UDP,
  JUNOS_SUN_RPC_RUSERD,
  JUNOS_SUN_RPC_RUSERD_TCP,
  JUNOS_SUN_RPC_RUSERD_UDP,
  JUNOS_SUN_RPC_SADMIND,
  JUNOS_SUN_RPC_SADMIND_TCP,
  JUNOS_SUN_RPC_SADMIND_UDP,
  JUNOS_SUN_RPC_SPRAYD,
  JUNOS_SUN_RPC_SPRAYD_TCP,
  JUNOS_SUN_RPC_SPRAYD_UDP,
  JUNOS_SUN_RPC_STATUS,
  JUNOS_SUN_RPC_STATUS_TCP,
  JUNOS_SUN_RPC_STATUS_UDP,
  JUNOS_SUN_RPC_TCP,
  JUNOS_SUN_RPC_UDP,
  JUNOS_SUN_RPC_WALLD,
  JUNOS_SUN_RPC_WALLD_TCP,
  JUNOS_SUN_RPC_WALLD_UDP,
  JUNOS_SUN_RPC_YPBIND,
  JUNOS_SUN_RPC_YPBIND_TCP,
  JUNOS_SUN_RPC_YPBIND_UDP,
  JUNOS_SUN_RPC_YPSERV,
  JUNOS_SUN_RPC_YPSERV_TCP,
  JUNOS_SUN_RPC_YPSERV_UDP,
  JUNOS_SYSLOG,
  JUNOS_TACACS,
  JUNOS_TACACS_DS,
  JUNOS_TALK,
  JUNOS_TCP_ANY,
  JUNOS_TELNET,
  JUNOS_TFTP,
  JUNOS_UDP_ANY,
  JUNOS_UUCP,
  JUNOS_VDO_LIVE,
  JUNOS_VNC,
  JUNOS_WAIS,
  JUNOS_WHO,
  JUNOS_WHOIS,
  JUNOS_WINFRAME,
  JUNOS_WXCONTROL,
  JUNOS_X_WINDOWS,
  JUNOS_XNM_CLEAR_TEXT,
  JUNOS_XNM_SSL,
  JUNOS_YMSG;

  private final Supplier<BaseApplication> _baseApplication;

  private JunosApplication() {
    _baseApplication = Suppliers.memoize(this::init);
  }

  @Override
  public void applyTo(
      JuniperConfiguration jc,
      HeaderSpace.Builder srcHeaderSpaceBuilder,
      LineAction action,
      List<IpAccessListLine> lines,
      Warnings w) {
    _baseApplication.get().applyTo(jc, srcHeaderSpaceBuilder, action, lines, w);
  }

  @Override
  public boolean getIpv6() {
    return _baseApplication.get().getIpv6();
  }

  private BaseApplication init() {
    BaseApplication baseApplication =
        new BaseApplication(name(), DefinedStructure.IGNORED_DEFINITION_LINE);
    Map<String, Term> terms = baseApplication.getTerms();
    switch (this) {
      case JUNOS_FTP:
        {
          String t1Name = "t1";
          Term t1 = new Term(t1Name);
          HeaderSpace.Builder l1 = t1.getHeaderSpace().rebuild();
          l1.setIpProtocols(ImmutableSet.of(IpProtocol.TCP));
          int portNum = NamedPort.FTP.number();
          l1.setDstPorts(ImmutableSet.of(new SubRange(portNum, portNum)));
          t1.setHeaderSpace(l1.build());
          terms.put(t1Name, t1);
          break;
        }

      case JUNOS_HTTP:
        {
          String t1Name = "t1";
          Term t1 = new Term(t1Name);
          HeaderSpace.Builder l1 = t1.getHeaderSpace().rebuild();
          l1.setIpProtocols(ImmutableSet.of(IpProtocol.TCP));
          int portNum = NamedPort.HTTP.number();
          l1.setDstPorts(ImmutableSet.of(new SubRange(portNum, portNum)));
          t1.setHeaderSpace(l1.build());
          terms.put(t1Name, t1);
          break;
        }

      case JUNOS_HTTPS:
        {
          String t1Name = "t1";
          Term t1 = new Term(t1Name);
          HeaderSpace.Builder l1 = t1.getHeaderSpace().rebuild();
          l1.setIpProtocols(ImmutableSet.of(IpProtocol.TCP));
          int portNum = NamedPort.HTTPS.number();
          l1.setDstPorts(ImmutableSet.of(new SubRange(portNum, portNum)));
          t1.setHeaderSpace(l1.build());
          terms.put(t1Name, t1);
          break;
        }

      case JUNOS_ICMP_ALL:
        {
          String t1Name = "t1";
          Term t1 = new Term(t1Name);
          HeaderSpace.Builder l1 = t1.getHeaderSpace().rebuild();
          l1.setIpProtocols(ImmutableSet.of(IpProtocol.ICMP));
          t1.setHeaderSpace(l1.build());
          terms.put(t1Name, t1);
          break;
        }

      case JUNOS_ICMP6_ALL:
        {
          baseApplication.setIpv6(true);
          // TODO
          break;
        }

      case JUNOS_NNTP:
        {
          String t1Name = "t1";
          Term t1 = new Term(t1Name);
          HeaderSpace.Builder l1 = t1.getHeaderSpace().rebuild();
          l1.setIpProtocols(ImmutableSet.of(IpProtocol.TCP));
          int portNum = NamedPort.NNTP.number();
          l1.setDstPorts(ImmutableSet.of(new SubRange(portNum, portNum)));
          t1.setHeaderSpace(l1.build());
          terms.put(t1Name, t1);
          break;
        }

      case JUNOS_NTP:
        {
          String t1Name = "t1";
          Term t1 = new Term(t1Name);
          HeaderSpace.Builder l1 = t1.getHeaderSpace().rebuild();
          l1.setIpProtocols(ImmutableSet.of(IpProtocol.UDP));
          int portNum = NamedPort.NTP.number();
          l1.setDstPorts(ImmutableSet.of(new SubRange(portNum, portNum)));
          t1.setHeaderSpace(l1.build());
          terms.put(t1Name, t1);
          break;
        }

      case JUNOS_PPTP:
        {
          String t1Name = "t1";
          Term t1 = new Term(t1Name);
          HeaderSpace.Builder l1 = t1.getHeaderSpace().rebuild();
          l1.setIpProtocols(ImmutableSet.of(IpProtocol.TCP));
          int portNum = NamedPort.PPTP.number();
          l1.setDstPorts(ImmutableSet.of(new SubRange(portNum, portNum)));
          t1.setHeaderSpace(l1.build());
          terms.put(t1Name, t1);
          break;
        }

      case JUNOS_SSH:
        {
          String t1Name = "t1";
          Term t1 = new Term(t1Name);
          HeaderSpace.Builder l1 = t1.getHeaderSpace().rebuild();
          l1.setIpProtocols(ImmutableSet.of(IpProtocol.TCP));
          int portNum = NamedPort.SSH.number();
          l1.setDstPorts(ImmutableSet.of(new SubRange(portNum, portNum)));
          t1.setHeaderSpace(l1.build());
          terms.put(t1Name, t1);
          break;
        }

        // $CASES-OMITTED$
      default:
        return null;
    }
    return baseApplication;
  }

  public boolean hasDefinition() {
    return _baseApplication.get() != null;
  }
}
