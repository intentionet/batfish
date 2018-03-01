package org.batfish.datamodel;

import java.util.Map;

public interface ArpAnalysis {

  /** Mapping: hostname -> inInterface -> ipsToArpReplyTo */
  Map<String, Map<String, IpAddressAcl>> getArpReplies();

  /**
   * Mapping: hostname -> vrfName -> route -> outInterface -> arpIpChoice ->
   * dstIpsForWhichSomeoneSendsArpReply
   */
  Map<String, Map<String, Map<AbstractRoute, Map<String, Map<ArpIpChoice, IpAddressAcl>>>>>
      getArpRequests();
}
