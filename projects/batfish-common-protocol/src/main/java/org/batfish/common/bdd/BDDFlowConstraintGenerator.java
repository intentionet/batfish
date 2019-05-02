package org.batfish.common.bdd;

import com.google.common.collect.ImmutableList;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.sf.javabdd.BDD;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.IcmpType;
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
    APPLICATION
  }

  private BDDPacket _bddPacket;
  private BDD _icmpFlow;
  private BDD _udpFlow;
  private BDD _tcpFlow;

  BDDFlowConstraintGenerator(BDDPacket pkt) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("construct BDDFlowConstraintGenerator").startActive()) {
      assert span != null; // avoid unused warning
      _bddPacket = pkt;
      _icmpFlow = computeICMPConstraint();
      _udpFlow = computeUDPConstraint();
      _tcpFlow = computeTCPConstraint();
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

  public List<BDD> generateFlowPreference(FlowPreference preference) {
    switch (preference) {
      case DEBUGGING:
        return ImmutableList.of(_icmpFlow, _udpFlow, _tcpFlow);
      case APPLICATION:
        return ImmutableList.of(_tcpFlow, _udpFlow, _icmpFlow);
      default:
        throw new BatfishException("Not supported flow preference");
    }
  }
}
