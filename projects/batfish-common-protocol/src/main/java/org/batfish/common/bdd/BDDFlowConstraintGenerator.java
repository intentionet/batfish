package org.batfish.common.bdd;

import static org.batfish.datamodel.PacketHeaderConstraintsUtil.DEFAULT_PACKET_LENGTH;

import com.google.common.collect.ImmutableList;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.util.List;
import net.sf.javabdd.BDD;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.NamedPort;

/** This class generates common useful flow constraints as BDDs. */
public final class BDDFlowConstraintGenerator {
  /** Allows a caller to express preferences on how packets should be retrieved. */
  public enum FlowPreference {
    /** Prefers ICMP over UDP over TCP. */
    DEBUGGING,
    /** Prefers TCP over UDP over ICMP. */
    APPLICATION,
    /**
     * Prefers TCP over UDP over ICMP. Not currently different from {@link #APPLICATION}, but may
     * change.
     */
    TESTFILTER
  }

  private final BDDPacket _bddPacket;
  private final BDDOps _bddOps;
  private final List<BDD> _icmpConstraints;
  private final List<BDD> _udpConstraints;
  private final List<BDD> _tcpConstraints;
  private final BDD _defaultPacketLength;
  private final List<BDD> _ipConstraints;

  BDDFlowConstraintGenerator(BDDPacket pkt) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("construct BDDFlowConstraintGenerator").startActive()) {
      assert span != null; // avoid unused warning
      _bddPacket = pkt;
      _bddOps = new BDDOps(pkt.getFactory());
      _defaultPacketLength = _bddPacket.getPacketLength().value(DEFAULT_PACKET_LENGTH);
      _icmpConstraints = computeICMPConstraint();
      _udpConstraints = computeUDPConstraints();
      _tcpConstraints = computeTCPConstraints();
      _ipConstraints = computeIpConstraints();
    }
  }

  private List<BDD> computeICMPConstraint() {
    BDD icmp = _bddPacket.getIpProtocol().value(IpProtocol.ICMP);
    BDDIcmpType type = _bddPacket.getIcmpType();
    BDD codeZero = _bddPacket.getIcmpCode().value(0);
    // Prefer ICMP Echo_Request, then anything with code 0, then anything ICMP/
    return ImmutableList.of(
        _bddOps.and(icmp, type.value(IcmpType.ECHO_REQUEST), codeZero),
        _bddOps.and(icmp, codeZero),
        icmp);
  }

  private BDD emphemeralPort(BDDInteger portInteger) {
    return portInteger.geq(NamedPort.EPHEMERAL_LOWEST.number());
  }

  private List<BDD> tcpPortPreferences(BDD tcp, BDDInteger tcpPort) {
    return ImmutableList.of(
        _bddOps.and(tcp, tcpPort.value(NamedPort.HTTP.number())),
        _bddOps.and(tcp, tcpPort.value(NamedPort.HTTPS.number())),
        _bddOps.and(tcp, tcpPort.value(NamedPort.SSH.number())),
        // at least not zero if possible
        _bddOps.and(tcp, tcpPort.value(0).not()));
  }

  // Get TCP packets with special named ports, trying to find cases where only one side is
  // ephemeral.
  private List<BDD> computeTCPConstraints() {
    BDDInteger dstPort = _bddPacket.getDstPort();
    BDDInteger srcPort = _bddPacket.getSrcPort();
    BDD tcp = _bddPacket.getIpProtocol().value(IpProtocol.TCP);

    BDD srcPortEphemeral = emphemeralPort(srcPort);
    BDD dstPortEphemeral = emphemeralPort(dstPort);

    return ImmutableList.<BDD>builder()
        // First, try to nudge src and dst port apart. E.g., if one is ephemeral the other is not.
        .add(_bddOps.and(tcp, srcPortEphemeral, dstPortEphemeral.not()))
        .add(_bddOps.and(tcp, srcPortEphemeral.not(), dstPortEphemeral))
        // Next, execute port preferences
        .addAll(tcpPortPreferences(tcp, srcPort))
        .addAll(tcpPortPreferences(tcp, dstPort))
        // Anything TCP.
        .add(tcp)
        .build();
  }

  private List<BDD> udpPortPreferences(BDD udp, BDDInteger tcpPort) {
    return ImmutableList.of(
        _bddOps.and(udp, tcpPort.value(NamedPort.DOMAIN.number())),
        _bddOps.and(udp, tcpPort.value(NamedPort.SNMP.number())),
        _bddOps.and(udp, tcpPort.value(NamedPort.SNMPTRAP.number())),
        // at least not zero if possible
        _bddOps.and(udp, tcpPort.value(0).not()));
  }

  // Get UDP packets with special named ports, trying to find cases where only one side is
  // ephemeral.
  private List<BDD> computeUDPConstraints() {
    BDDInteger dstPort = _bddPacket.getDstPort();
    BDDInteger srcPort = _bddPacket.getSrcPort();
    BDD udp = _bddPacket.getIpProtocol().value(IpProtocol.UDP);

    BDD srcPortEphemeral = emphemeralPort(srcPort);
    BDD dstPortEphemeral = emphemeralPort(dstPort);

    return ImmutableList.<BDD>builder()
        // Try for UDP traceroute.
        .add(
            _bddOps.and(
                udp,
                dstPort.range(33434, 33534).and(srcPort.geq(NamedPort.EPHEMERAL_LOWEST.number()))))
        // Next, try to nudge src and dst port apart. E.g., if one is ephemeral the other is not.
        .add(_bddOps.and(udp, srcPortEphemeral, dstPortEphemeral.not()))
        .add(_bddOps.and(udp, srcPortEphemeral.not(), dstPortEphemeral))
        // Next, execute port preferences
        .addAll(udpPortPreferences(udp, srcPort))
        .addAll(udpPortPreferences(udp, dstPort))
        // Anything UDP.
        .add(udp)
        .build();
  }

  private BDD isPrivateIp(BDDInteger ipInteger) {
    return _bddOps.or(
        ipInteger.range(Ip.parse("10.0.0.0").asLong(), Ip.parse("10.255.255.255").asLong()),
        ipInteger.range(Ip.parse("172.16.0.0").asLong(), Ip.parse("172.255.255.255").asLong()),
        ipInteger.range(Ip.parse("192.168.0.0").asLong(), Ip.parse("192.168.255.255").asLong()));
  }

  private List<BDD> ipPreferences(BDDInteger ipInteger) {
    return ImmutableList.of(
        // First, one of the special IPs.
        ipInteger.value(Ip.parse("8.8.8.8").asLong()),
        ipInteger.value(Ip.parse("1.1.1.1").asLong()),
        // Next, at least don't start with 0.
        ipInteger.geq(Ip.parse("1.0.0.0").asLong()),
        // Next, try to be in class A.
        ipInteger.leq(Ip.parse("126.255.255.254").asLong()));
  }

  private List<BDD> computeIpConstraints() {
    BDDInteger srcIp = _bddPacket.getSrcIp();
    BDDInteger dstIp = _bddPacket.getDstIp();
    BDD srcIpPrivate = isPrivateIp(srcIp);
    BDD dstIpPrivate = isPrivateIp(dstIp);

    return ImmutableList.<BDD>builder()
        // First, try to nudge src and dst IP apart. E.g., if one is private the other should be
        // public.
        .add(_bddOps.and(srcIpPrivate, dstIpPrivate.not()))
        .add(_bddOps.and(srcIpPrivate.not(), dstIpPrivate))
        // Next, execute IP preferences
        .addAll(ipPreferences(srcIp))
        .addAll(ipPreferences(dstIp))
        .build();
  }

  public List<BDD> generateFlowPreference(FlowPreference preference) {
    switch (preference) {
      case DEBUGGING:
        return ImmutableList.<BDD>builder()
            .addAll(_icmpConstraints)
            .addAll(_udpConstraints)
            .addAll(_tcpConstraints)
            .add(_defaultPacketLength)
            .addAll(_ipConstraints)
            .build();
      case APPLICATION:
      case TESTFILTER:
        return ImmutableList.<BDD>builder()
            .addAll(_tcpConstraints)
            .addAll(_udpConstraints)
            .addAll(_icmpConstraints)
            .add(_defaultPacketLength)
            .addAll(_ipConstraints)
            .build();
      default:
        throw new BatfishException("Not supported flow preference");
    }
  }
}
