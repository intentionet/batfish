package org.batfish.common.bdd;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory.BDDOp;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.NamedPort;

/** This class generates common useful flow constraints as BDDs. */
public final class BDDFlowConstraintGenerator {
  /**
   * Difference preferences of flow constraints: DEBUGGING: 1. ICMP 2. UDP 3. TCP APPLICATION : 1.
   * TCP 2. UDP 3. ICMP
   */
  public enum FlowPreference {
    DEBUGGING,
    APPLICATION,
    TESTFILTER
  }

  private BDDPacket _bddPacket;
  private BDD _icmpFlow;
  private BDD _udpFlow;
  private BDD _tcpFlow;
  private BDD _httpFlow;

  BDDFlowConstraintGenerator(BDDPacket pkt) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("construct BDDFlowConstraintGenerator").startActive()) {
      assert span != null; // avoid unused warning
      _bddPacket = pkt;
      _icmpFlow = computeICMPConstraint();
      _udpFlow = computeUDPConstraint();
      _tcpFlow = computeTCPConstraint();
      _httpFlow = computeHTTPConstraint();
    }
  }

  public BDD getUDPFlow() {
    return _udpFlow;
  }

  public BDD getTCPFlow() {
    return _tcpFlow;
  }

  public BDD getICMPFlow() {
    return _icmpFlow;
  }

  // Get ICMP echo request packets
  BDD computeICMPConstraint() {
    return _bddPacket
        .getIpProtocol()
        .value(IpProtocol.ICMP)
        .and(
            _bddPacket
                .getIcmpType()
                .value(IcmpType.ECHO_REQUEST)
                .and(_bddPacket.getIcmpCode().value(0)));
  }

  // Get TCP packets with names ports:
  // 1. Considers both directions of a TCP flow.
  // 2. Set src (dst, respectively) port to a ephemeral port, and dst (src, respectively) port to a
  // named port
  BDD computeTCPConstraint() {
    BDDInteger dstPort = _bddPacket.getDstPort();
    BDDInteger srcPort = _bddPacket.getSrcPort();
    BDD bdd1 =
        _bddPacket
            .getFactory()
            .orAll(
                Arrays.stream(NamedPort.values())
                    .map(namedPort -> dstPort.value(namedPort.number()))
                    .collect(Collectors.toList()));
    bdd1 = bdd1.and(srcPort.geq(NamedPort.EPHEMERAL_LOWEST.number()));
    BDD bdd2 = _bddPacket.swapSourceAndDestinationFields(bdd1);
    BDD tcp = _bddPacket.getIpProtocol().value(IpProtocol.TCP);
    return tcp.and(bdd1.or(bdd2));
  }

  // Get UDP packets for traceroute:
  // 1. Considers both directions of a UDP flow.
  // 2. Set dst (src, respectively) port to the range 33434-33534 (common ports used by traceroute),
  // and src (dst, respectively) port to a ephemeral port
  BDD computeUDPConstraint() {
    BDDInteger dstPort = _bddPacket.getDstPort();
    BDDInteger srcPort = _bddPacket.getSrcPort();
    BDD bdd1 = dstPort.range(33434, 33534).and(srcPort.geq(NamedPort.EPHEMERAL_LOWEST.number()));
    BDD bdd2 = _bddPacket.swapSourceAndDestinationFields(bdd1);
    return _bddPacket.getIpProtocol().value(IpProtocol.UDP).and(bdd1.or(bdd2));
  }

  // Get HTTP packets with names ports:
  // 1. Dst Ip is 8.8.8.8
  // 2. Dst port is HTTP
  // 3. Src port is the lowest ephemeral port
  private BDD computeHTTPConstraint() {
    BDDInteger dstIp = _bddPacket.getDstIp();
    BDDInteger dstPort = _bddPacket.getDstPort();
    BDDInteger srcPort = _bddPacket.getSrcPort();
    BDDIpProtocol ipProtocol = _bddPacket.getIpProtocol();

    return dstIp
        .value(Ip.parse("8.8.8.8").asLong())
        .and(ipProtocol.value(IpProtocol.TCP))
        .and(srcPort.value(NamedPort.EPHEMERAL_LOWEST.number()))
        .and(dstPort.value(NamedPort.HTTP.number()));
  }

  private List<BDD> computeTestFilterPreference() {
    BDDInteger dstIp = _bddPacket.getDstIp();
    BDDInteger dstPort = _bddPacket.getDstPort();
    BDDInteger srcPort = _bddPacket.getSrcPort();
    BDDIpProtocol ipProtocol = _bddPacket.getIpProtocol();

    BDD dstIpBdd = dstIp.value(Ip.parse("8.8.8.8").asLong());
    BDD ipProtocolBdd = ipProtocol.value(IpProtocol.TCP);
    BDD srcPortBdd = srcPort.value(NamedPort.EPHEMERAL_LOWEST.number());
    BDD dstPortBdd = dstPort.value(NamedPort.HTTP.number());

    BDDOps bddOps = new BDDOps(_bddPacket.getFactory());
    // generate all combinations in order to enforce the following logic: when a field in the input
    // bdd contains the default value for that field, then use that value; otherwise use a value
    // in BDD of the field.
    return ImmutableList.of(
        bddOps.and(dstIpBdd, ipProtocolBdd, srcPortBdd, dstPortBdd),
        bddOps.and(ipProtocolBdd, srcPortBdd, dstPortBdd),
        bddOps.and(dstIpBdd, srcPortBdd, dstPortBdd),
        bddOps.and(dstIpBdd, ipProtocolBdd, dstPortBdd),
        bddOps.and(dstIpBdd, ipProtocolBdd, srcPortBdd),
        bddOps.and(dstIpBdd, ipProtocolBdd),
        bddOps.and(dstIpBdd, srcPortBdd),
        bddOps.and(ipProtocolBdd, srcPortBdd),
        bddOps.and(dstIpBdd, dstPortBdd),
        bddOps.and(ipProtocolBdd, dstPortBdd),
        bddOps.and(srcPortBdd, dstPortBdd),
        bddOps.and(dstIpBdd),
        bddOps.and(ipProtocolBdd),
        bddOps.and(srcPortBdd),
        bddOps.and(dstPortBdd));
  }

  public List<BDD> generateFlowPreference(FlowPreference preference) {
    switch (preference) {
      case DEBUGGING:
        return ImmutableList.of(_icmpFlow, _udpFlow, _tcpFlow);
      case APPLICATION:
        return ImmutableList.of(_tcpFlow, _udpFlow, _icmpFlow);
      case TESTFILTER:
        return computeTestFilterPreference();
      default:
        throw new BatfishException("Not supported flow preference");
    }
  }
}
