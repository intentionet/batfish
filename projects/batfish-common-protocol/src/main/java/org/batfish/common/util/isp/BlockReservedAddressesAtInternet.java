package org.batfish.common.util.isp;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.TraceElement;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.isp_configuration.traffic_filtering.IspTrafficFiltering.Mode;

/** Implementation of {@link Mode#BLOCK_RESERVED_ADDRESSES_AT_INTERNET}. */
public final class BlockReservedAddressesAtInternet implements IspTrafficFilteringPolicy {

  public static BlockReservedAddressesAtInternet create() {
    return INSTANCE;
  }

  @Override
  public @Nullable IpAccessList filterTrafficFromInternet() {
    return IpAccessList.builder()
        .setName("Block incoming traffic using reserved addresses")
        .setLines(LINES)
        .build();
  }

  @Override
  public @Nullable IpAccessList filterTrafficToInternet() {
    return IpAccessList.builder()
        .setName("Block outgoing traffic using reserved addresses")
        .setLines(LINES)
        .build();
  }

  @Override
  public @Nullable IpAccessList filterTrafficFromNetwork() {
    return null;
  }

  @Override
  public @Nullable IpAccessList filterTrafficToNetwork() {
    return null;
  }

  /** Prefix -> description for TraceElement (optional). */
  private static final Map<String, String> BLOCKED_ADDRESS =
      ImmutableMap.<String, String>builder()
          .put("0.0.0.0/8", "RFC 6890")
          .put("10.0.0.0/8", "RFC 1918 private")
          .put("127.0.0.0/8", "Loopback")
          .put("172.16.0.0/12", "RFC 1918 private")
          .put("192.0.2.0/24", "Documentation")
          .put("192.168.0.0/16", "RFC 1918 private")
          .put("198.51.100.0/24", "Documentation")
          .put("203.0.113.0/24", "Documentation")
          .put("224.0.0.0/4", "Multicast")
          .put("240.0.0.0/4", "Future use")
          .build();
  /*
   * Skipping these for now.
   *
   * .put("100.64.0.0/10", "Carrier-grade NAT")
   * .put("169.254.0.0/16", "Link-local")
   * .put("198.18.0.0/15", "Benchmarking")
   * .put("192.88.99.0/24", "Former 6-to-4")
   */

  private static final List<AclLine> LINES =
      Stream.concat(
              BLOCKED_ADDRESS.entrySet().stream()
                  .map(
                      e ->
                          ExprAclLine.rejecting(
                              TraceElement.of(
                                  String.format(
                                      "Matched reserved IP address space %s (%s)",
                                      e.getKey(), e.getValue())),
                              new MatchHeaderSpace(
                                  HeaderSpace.builder()
                                      .setSrcOrDstIps(Prefix.parse(e.getKey()).toIpSpace())
                                      .build()))),
              Stream.of(
                  ExprAclLine.accepting(
                      TraceElement.of("Permitted traffic using no reserved IP addresses"),
                      TrueExpr.INSTANCE)))
          .collect(Collectors.toList());

  private static final BlockReservedAddressesAtInternet INSTANCE =
      new BlockReservedAddressesAtInternet();

  private BlockReservedAddressesAtInternet() {}
}
