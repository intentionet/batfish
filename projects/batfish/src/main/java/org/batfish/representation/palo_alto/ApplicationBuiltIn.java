package org.batfish.representation.palo_alto;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.SubRange;

/**
 * BuiltIn applications available on PAN devices. See here for details about specific applications:
 * https://applipedia.paloaltonetworks.com/
 */
@ParametersAreNonnullByDefault
public final class ApplicationBuiltIn {
  public static final Application AOL_MESSAGEBOARD_POSTING =
      Application.builder("aol-messageboard-posting")
          .setDescription("built-in application aol-messageboard-posting")
          .addService(
              Service.builder("aol-messageboard-posting")
                  .setIpProtocol(IpProtocol.TCP)
                  .addPort(80)
                  .build())
          .build();
  public static final Application AOL_PROXY =
      Application.builder("aol-proxy")
          .setDescription("built-in application aol-proxy")
          .addService(
              Service.builder("aol-proxy")
                  .setIpProtocol(IpProtocol.TCP)
                  .addPorts(80, 5192, 5190)
                  .build())
          .build();
  public static final Application DHCP =
      Application.builder("dhcp")
          .setDescription("built-in application dhcp")
          .addService(
              Service.builder("dhcp-tcp").setIpProtocol(IpProtocol.UDP).addPorts(67, 68).build())
          .addService(
              Service.builder("dhcp-udp").setIpProtocol(IpProtocol.TCP).addPorts(67, 68).build())
          .build();

  public static final Application FINGER =
      Application.builder("finger")
          .setDescription("built-in application finger")
          .addService(
              Service.builder("finger-tcp").setIpProtocol(IpProtocol.UDP).addPort(79).build())
          .addService(
              Service.builder("finger-udp").setIpProtocol(IpProtocol.TCP).addPorts(79).build())
          .build();

  public static final Application FTP =
      Application.builder("ftp")
          .setDescription("built-in application ftp")
          .addService(Service.builder("ftp").setIpProtocol(IpProtocol.TCP).addPort(21).build())
          .build();

  public static final Application GNUTELLA =
      // No well-known ports, DPI only
      Application.builder("gnutella").setDescription("built-in application gnutella").build();

  public static final Application GOPHER =
      Application.builder("gopher")
          .setDescription("built-in application gopher")
          .addService(Service.builder("gopher").setIpProtocol(IpProtocol.TCP).addPort(70).build())
          .build();

  public static final Application ICMP =
      Application.builder("icmp")
          .setDescription("built-in application icmp")
          .addService(Service.builder("icmp").setIpProtocol(IpProtocol.ICMP).build())
          .build();
  public static final Application LDAP =
      Application.builder("ldap")
          .setDescription("built-in application ldap")
          .addService(
              Service.builder("ldap tcp")
                  .setIpProtocol(IpProtocol.TCP)
                  .addPorts(389, 636, 3268, 3269)
                  .build())
          .addService(
              Service.builder("ldap udp").setIpProtocol(IpProtocol.UDP).addPorts(389, 3268).build())
          .build();

  public static final Application MCSQL_MON =
      Application.builder("mssql-mon")
          .setDescription("built-in application mssql-mon")
          .addService(
              Service.builder("mssql-mon").setIpProtocol(IpProtocol.UDP).addPort(1434).build())
          .build();

  public static final Application MS_DS_SMB =
      Application.builder("ms-ds-smb")
          .setDescription("built-in application ms-ds-smb")
          .addService(
              Service.builder("ms-ds-smb tcp")
                  .setIpProtocol(IpProtocol.TCP)
                  .addPorts(139, 445)
                  .build())
          .addService(
              Service.builder("ms-ds-smb udp").setIpProtocol(IpProtocol.UDP).addPorts(445).build())
          .build();
  public static final Application MSN =
      Application.builder("msn")
          .setDescription("built-in application msn")
          .addService(
              Service.builder("msn tcp")
                  .setIpProtocol(IpProtocol.TCP)
                  .addPorts(1863, 80, 7001, 443)
                  .addPorts(new SubRange(1025, 65535))
                  .build())
          .addService(
              Service.builder("msn udp")
                  .setIpProtocol(IpProtocol.UDP)
                  .addPorts(new SubRange(1025, 65535))
                  .addPorts(7001)
                  .build())
          .build();
  public static final Application MS_NETLOGON =
      Application.builder("ms-netlogon")
          .setDescription("built-in application ms-netlogon")
          .addService(
              Service.builder("ms-netlogon tcp")
                  .setIpProtocol(IpProtocol.TCP)
                  .addPorts(135, 139, 445)
                  .addPorts(new SubRange(1025, 5000))
                  .addPorts(new SubRange(49152, 65535))
                  .build())
          .addService(
              Service.builder("ms-netlogon udp")
                  .setIpProtocol(IpProtocol.UDP)
                  .addPorts(137, 138, 445)
                  .build())
          .build();
  public static final Application MS_KMS =
      Application.builder("ms-kms")
          .setDescription("built-in application ms-kms")
          .addService(
              Service.builder("ms-kms tcp").setIpProtocol(IpProtocol.TCP).addPorts(1688).build())
          .build();
  public static final Application MSRPC =
      // No well-known ports, DPI only
      Application.builder("msrpc").setDescription("built-in application msrpc").build();
  public static final Application MSSQL_DB =
      Application.builder("mssql-db")
          .addService(
              Service.builder("mssql-db tcp").setIpProtocol(IpProtocol.TCP).addPorts(1433).build())
          .addService(
              Service.builder("mssql-db udp").setIpProtocol(IpProtocol.UDP).addPorts(1433).build())
          .setDescription("built-in application mssql-db")
          .build();
  public static final Application NETBIOS_SS =
      Application.builder("netbios-ss")
          .setDescription("built-in application netbios-ss")
          .addService(
              Service.builder("netbios-ss").setIpProtocol(IpProtocol.TCP).addPorts(139).build())
          .build();
  public static final Application NTP =
      Application.builder("ntp")
          .setDescription("built-in application ntp")
          .addService(
              Service.builder("ntp tcp").setIpProtocol(IpProtocol.TCP).addPorts(123).build())
          .addService(
              Service.builder("ntp udp").setIpProtocol(IpProtocol.UDP).addPorts(123).build())
          .build();
  public static final Application PAN_DB_CLOUD =
      Application.builder("pan-db-cloud")
          .setDescription("built-in application pan-db-cloud")
          .addService(
              Service.builder("pan-db-cloud tcp")
                  .setIpProtocol(IpProtocol.TCP)
                  .addPorts(443)
                  .build())
          .build();
  public static final Application PING =
      Application.builder("ping")
          .setDescription("built-in application ping")
          .addService(Service.builder("ping").setIpProtocol(IpProtocol.ICMP).build())
          .build();
  public static final Application PC_ANYWHERE =
      Application.builder("pcanywhere")
          .setDescription("built-in application pcanywhere")
          .addService(
              Service.builder("pcanywhere-tcp")
                  .setIpProtocol(IpProtocol.TCP)
                  .addPorts(5631, 65301)
                  .build())
          .addService(
              Service.builder("pcanywhere-udp")
                  .setIpProtocol(IpProtocol.UDP)
                  .addPorts(22, 5632)
                  .build())
          .build();
  public static final Application POP3 =
      Application.builder("pop3")
          .setDescription("built-in application pop3")
          .addService(Service.builder("pop3").setIpProtocol(IpProtocol.TCP).addPort(110).build())
          .build();
  public static final Application RPC =
      // No well-known ports, DPI only
      Application.builder("rpc").setDescription("built-in application rpc").build();
  public static final Application SMTP =
      Application.builder("smtp")
          .setDescription("built-in application smtp")
          .addService(
              Service.builder("smtp").setIpProtocol(IpProtocol.TCP).addPorts(25, 587).build())
          .build();
  public static final Application SNMP =
      Application.builder("snmp")
          .setDescription("built-in application snmp")
          .addService(
              Service.builder("snmp tcp").setIpProtocol(IpProtocol.TCP).addPorts(161).build())
          .addService(
              Service.builder("snmp udp").setIpProtocol(IpProtocol.UDP).addPorts(161).build())
          .build();
  public static final Application SOAP =
      Application.builder("soap")
          .setDescription("built-in application soap")
          .addService(
              Service.builder("soap").setIpProtocol(IpProtocol.TCP).addPorts(80, 443).build())
          .build();
  public static final Application SSH =
      Application.builder("ssh")
          .setDescription("built-in application ssh")
          .addService(Service.builder("ssh").setIpProtocol(IpProtocol.TCP).addPort(22).build())
          .build();
  public static final Application SSL =
      Application.builder("ssl")
          .setDescription("built-in application ssl")
          .addService(Service.builder("ssl").setIpProtocol(IpProtocol.TCP).addPort(443).build())
          .build();
  public static final Application WEB_BROWSING =
      Application.builder("web-browsing")
          .setDescription("built-in application web-browsing")
          .addService(
              Service.builder("web-browsing").setIpProtocol(IpProtocol.TCP).addPort(80).build())
          .build();

  private static final List<Application> BUILTIN_LIST =
      ImmutableList.of(
          AOL_MESSAGEBOARD_POSTING,
          AOL_PROXY,
          DHCP,
          FINGER,
          FTP,
          GNUTELLA,
          GOPHER,
          ICMP,
          LDAP,
          MCSQL_MON,
          MS_DS_SMB,
          MS_KMS,
          MSN,
          MS_NETLOGON,
          MSSQL_DB,
          MSRPC,
          NETBIOS_SS,
          NTP,
          PAN_DB_CLOUD,
          PC_ANYWHERE,
          PING,
          POP3,
          RPC,
          SMTP,
          SNMP,
          SOAP,
          SSH,
          SSL,
          WEB_BROWSING);

  private static final Map<String, Application> BUILTINS =
      BUILTIN_LIST.stream()
          .collect(ImmutableMap.toImmutableMap(Application::getName, Function.identity()));

  /**
   * Returns the definition of the builtin corresponding to the named {@link Application}, or an
   * absent {@link Optional} if none exists (or has had its definition implemented here).
   */
  public static @Nonnull Optional<Application> getBuiltInApplication(@Nonnull String name) {
    return Optional.ofNullable(BUILTINS.get(name));
  }

  private ApplicationBuiltIn() {} // prevent instantiation of utility class
}
