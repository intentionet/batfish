package org.batfish.datamodel;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.function.Function;
import org.junit.Test;

public class HeaderSpaceMatchesTest {

  private static final Map<String, IpSpace> _namedIpSpaces = ImmutableMap.of();

  private static final int _dscp = 5;
  private static final Ip _dstIp = Ip.parse("2.2.2.2");
  private static final int _dstPort = 22;
  private static final int _ecn = 5;
  private static final int _fragmentOffset = 5;
  private static final int _icmpCode = 5;
  private static final int _icmpType = 5;
  private static final IpProtocol _ipProtocol = IpProtocol.TCP;
  private static final int _packetLength = 5;
  private static final Ip _srcIp = Ip.parse("1.1.1.1");
  private static final int _srcPort = 22;
  private static final FlowState _state = FlowState.NEW;
  private static final TcpFlags _tcpFlags = TcpFlags.builder().setAck(true).build();

  /** SSH {@link Flow} from 1.1.1.1 to 2.2.2.2 with arbitrary values for other fields */
  private static final Flow _flow =
      Flow.builder()
          .setIngressNode("ingressNode") // required
          .setTag("tag") // required
          .setDscp(_dscp)
          .setDstIp(_dstIp)
          .setDstPort(_dstPort)
          .setEcn(_ecn)
          .setFragmentOffset(_fragmentOffset)
          .setIcmpCode(_icmpCode)
          .setIcmpType(_icmpType)
          .setIpProtocol(_ipProtocol)
          .setPacketLength(_packetLength)
          .setSrcIp(_srcIp)
          .setSrcPort(_srcPort)
          .setState(_state)
          .setTcpFlags(_tcpFlags)
          .build();

  @Test
  public void testEmptyHeaderSpaceMatches() {
    assertThat(HeaderSpace.builder().build().matches(_flow, _namedIpSpaces), equalTo(true));
  }

  @Test
  public void testDscpsMatchers() {
    testMatches(
        dscps -> HeaderSpace.builder().setDscps(dscps).build(),
        dscps -> HeaderSpace.builder().setNotDscps(dscps).build(),
        ImmutableSet.of(_dscp, _dscp + 1),
        ImmutableSet.of(_dscp + 1, _dscp + 2));
  }

  @Test
  public void testDstIpsMatchers() {
    testMatches(
        dstIps -> HeaderSpace.builder().setDstIps(dstIps).build(),
        dstIps -> HeaderSpace.builder().setNotDstIps(dstIps).build(),
        new IpIpSpace(_dstIp),
        new IpIpSpace(Ip.parse("3.3.3.3")));
  }

  @Test
  public void testDstPortsMatchers() {
    SubRange nonMatching = new SubRange(0, _dstPort - 1);
    testMatches(
        dstPorts -> HeaderSpace.builder().setDstPorts(dstPorts).build(),
        dstPorts -> HeaderSpace.builder().setNotDstPorts(dstPorts).build(),
        ImmutableSet.of(new SubRange(0, _dstPort), nonMatching),
        ImmutableSet.of(nonMatching, nonMatching));
  }

  @Test
  public void testDstProtocolsMatchers() {
    // _flow is TCP port 22, so SSH
    testMatches(
        dstProtocols -> HeaderSpace.builder().setDstProtocols(dstProtocols).build(),
        dstProtocols -> HeaderSpace.builder().setNotDstProtocols(dstProtocols).build(),
        ImmutableSet.of(Protocol.SSH, Protocol.HTTP),
        ImmutableSet.of(Protocol.HTTP, Protocol.DNS));
  }

  @Test
  public void testEcnsMatchers() {
    testMatches(
        ecns -> HeaderSpace.builder().setEcns(ecns).build(),
        ecns -> HeaderSpace.builder().setNotEcns(ecns).build(),
        ImmutableSet.of(_ecn, _ecn + 1),
        ImmutableSet.of(_ecn + 1, _ecn + 2));
  }

  @Test
  public void testFragmentOffsetsMatchers() {
    SubRange nonMatching = new SubRange(0, _fragmentOffset - 1);
    testMatches(
        fragOffsets -> HeaderSpace.builder().setFragmentOffsets(fragOffsets).build(),
        fragOffsets -> HeaderSpace.builder().setNotFragmentOffsets(fragOffsets).build(),
        ImmutableSet.of(new SubRange(0, _fragmentOffset), nonMatching),
        ImmutableSet.of(nonMatching, nonMatching));
  }

  @Test
  public void testIcmpCodesMatchers() {
    SubRange nonMatching = new SubRange(0, _icmpCode - 1);
    testMatches(
        icmpCodes -> HeaderSpace.builder().setIcmpCodes(icmpCodes).build(),
        icmpCodes -> HeaderSpace.builder().setNotIcmpCodes(icmpCodes).build(),
        ImmutableSet.of(new SubRange(0, _icmpCode), nonMatching),
        ImmutableSet.of(nonMatching, nonMatching));
  }

  @Test
  public void testIcmpTypesMatchers() {
    SubRange nonMatching = new SubRange(0, _icmpType - 1);
    testMatches(
        icmpTypes -> HeaderSpace.builder().setIcmpTypes(icmpTypes).build(),
        icmpTypes -> HeaderSpace.builder().setNotIcmpTypes(icmpTypes).build(),
        ImmutableSet.of(new SubRange(0, _icmpType), nonMatching),
        ImmutableSet.of(nonMatching, nonMatching));
  }

  @Test
  public void testIpProtocolsMatchers() {
    // _flow is TCP, so notMatching can be UDP and IP
    testMatches(
        protocols -> HeaderSpace.builder().setIpProtocols(protocols).build(),
        protocols -> HeaderSpace.builder().setNotIpProtocols(protocols).build(),
        ImmutableSet.of(_ipProtocol, IpProtocol.UDP),
        ImmutableSet.of(IpProtocol.UDP, IpProtocol.UDP));
  }

  @Test
  public void testPacketLengthsMatchers() {
    SubRange nonMatching = new SubRange(0, _packetLength - 1);
    testMatches(
        packetLengths -> HeaderSpace.builder().setPacketLengths(packetLengths).build(),
        packetLengths -> HeaderSpace.builder().setNotPacketLengths(packetLengths).build(),
        ImmutableSet.of(new SubRange(0, _packetLength), nonMatching),
        ImmutableSet.of(nonMatching, nonMatching));
  }

  @Test
  public void testSrcIpsMatchers() {
    testMatches(
        srcIps -> HeaderSpace.builder().setSrcIps(srcIps).build(),
        srcIps -> HeaderSpace.builder().setNotSrcIps(srcIps).build(),
        new IpIpSpace(_srcIp),
        new IpIpSpace(Ip.parse("3.3.3.3")));
  }

  @Test
  public void testSrcPortsMatchers() {
    SubRange nonMatching = new SubRange(0, _srcPort - 1);
    testMatches(
        srcPorts -> HeaderSpace.builder().setSrcPorts(srcPorts).build(),
        srcPorts -> HeaderSpace.builder().setNotSrcPorts(srcPorts).build(),
        ImmutableSet.of(new SubRange(0, _srcPort), nonMatching),
        ImmutableSet.of(nonMatching, nonMatching));
  }

  @Test
  public void testSrcProtocolsMatchers() {
    // _flow is TCP port 22, so SSH
    testMatches(
        srcProtocols -> HeaderSpace.builder().setSrcProtocols(srcProtocols).build(),
        srcProtocols -> HeaderSpace.builder().setNotSrcProtocols(srcProtocols).build(),
        ImmutableSet.of(Protocol.SSH, Protocol.HTTP),
        ImmutableSet.of(Protocol.HTTP, Protocol.HTTP));
  }

  @Test
  public void testSrcOrDestIpsMatchers() {
    // _flow goes from 1.1.1.1 to 2.2.2.2; either should work.
    HeaderSpace withSrcIpAsSrcOrDst =
        HeaderSpace.builder().setSrcOrDstIps(new IpIpSpace(_srcIp)).build();
    HeaderSpace withDstIpAsSrcOrDst =
        HeaderSpace.builder().setSrcOrDstIps(new IpIpSpace(_dstIp)).build();
    HeaderSpace withOtherIpAsSrcOrDst =
        HeaderSpace.builder().setSrcOrDstIps(new IpIpSpace(Ip.parse("3.3.3.3"))).build();
    assertThat(withSrcIpAsSrcOrDst.matches(_flow, _namedIpSpaces), equalTo(true));
    assertThat(withDstIpAsSrcOrDst.matches(_flow, _namedIpSpaces), equalTo(true));
    assertThat(withOtherIpAsSrcOrDst.matches(_flow, _namedIpSpaces), equalTo(false));
  }

  @Test
  public void testSrcOrDestPortsMatchers() {
    // Need a new flow for this because _flow has the same port for src and dst (22).
    int newDstPort = _dstPort + 1;
    Flow newDstPortFlow = _flow.toBuilder().setDstPort(newDstPort).build();
    SubRange nonMatching = new SubRange(0, _srcPort - 1);
    HeaderSpace withSrcPortAsSrcOrDst =
        HeaderSpace.builder()
            .setSrcOrDstPorts(ImmutableSet.of(new SubRange(0, _srcPort), nonMatching))
            .build();
    HeaderSpace withDstPortAsSrcOrDst =
        HeaderSpace.builder()
            .setSrcOrDstPorts(ImmutableSet.of(SubRange.singleton(newDstPort), nonMatching))
            .build();
    HeaderSpace withOtherPortAsSrcOrDst =
        HeaderSpace.builder().setSrcOrDstPorts(ImmutableSet.of(nonMatching, nonMatching)).build();
    assertThat(withSrcPortAsSrcOrDst.matches(newDstPortFlow, _namedIpSpaces), equalTo(true));
    assertThat(withDstPortAsSrcOrDst.matches(newDstPortFlow, _namedIpSpaces), equalTo(true));
    assertThat(withOtherPortAsSrcOrDst.matches(newDstPortFlow, _namedIpSpaces), equalTo(false));
  }

  @Test
  public void testSrcOrDestProtocolsMatchers() {
    // Need a new flow for this because _flow has the same protocol for src and dst (SSH).
    // Set dstPort to port number for HTTP; this should work since HTTP and SSH are both TCP.
    Flow newDstProtocolFlow = _flow.toBuilder().setDstPort(Protocol.HTTP.getPort()).build();
    HeaderSpace withSshAsSrcOrDst =
        HeaderSpace.builder()
            .setSrcOrDstProtocols(ImmutableSet.of(Protocol.SSH, Protocol.DNS))
            .build();
    HeaderSpace withHttpAsSrcOrDst =
        HeaderSpace.builder()
            .setSrcOrDstProtocols(ImmutableSet.of(Protocol.HTTP, Protocol.DNS))
            .build();
    HeaderSpace withDnsAsSrcOrDst =
        HeaderSpace.builder()
            .setSrcOrDstProtocols(ImmutableSet.of(Protocol.DNS, Protocol.DNS))
            .build();
    assertThat(withSshAsSrcOrDst.matches(newDstProtocolFlow, _namedIpSpaces), equalTo(true));
    assertThat(withHttpAsSrcOrDst.matches(newDstProtocolFlow, _namedIpSpaces), equalTo(true));
    assertThat(withDnsAsSrcOrDst.matches(newDstProtocolFlow, _namedIpSpaces), equalTo(false));
  }

  @Test
  public void testStatesMatchers() {
    // Actual state is FlowState.NEW
    HeaderSpace withRightState =
        HeaderSpace.builder().setStates(ImmutableSet.of(_state, FlowState.ESTABLISHED)).build();
    HeaderSpace withWrongState =
        HeaderSpace.builder()
            .setStates(ImmutableSet.of(FlowState.ESTABLISHED, FlowState.ESTABLISHED))
            .build();
    assertThat(withRightState.matches(_flow, _namedIpSpaces), equalTo(true));
    assertThat(withWrongState.matches(_flow, _namedIpSpaces), equalTo(false));
  }

  @Test
  public void testTcpFlagsMatchers() {
    HeaderSpace withRightFlag =
        HeaderSpace.builder()
            .setTcpFlags(
                ImmutableSet.of(
                    TcpFlagsMatchConditions.builder()
                        .setTcpFlags(_tcpFlags)
                        .setUseAck(true)
                        .build()))
            .build();
    HeaderSpace withWrongFlag =
        HeaderSpace.builder()
            .setTcpFlags(ImmutableSet.of(TcpFlagsMatchConditions.builder().setUseAck(true).build()))
            .build();
    assertThat(withRightFlag.matches(_flow, _namedIpSpaces), equalTo(true));
    assertThat(withWrongFlag.matches(_flow, _namedIpSpaces), equalTo(false));
  }

  /**
   * Tests {@link HeaderSpace#matches(Flow, Map)} for matching a given property of {@link Flow}.
   * Tests that:
   *
   * <ul>
   *   <li>A HeaderSpace that is supposed to match flows with value X for the property under test
   *       does match a flow with value X for that property
   *   <li>A HeaderSpace that is supposed to match flows with value Y for the property under test
   *       does NOT match flows with value X for that property
   *   <li>A HeaderSpace that is supposed to match flows that do NOT have value X for the property
   *       under test does NOT match flows with value X for that property
   *   <li>A HeaderSpace that is supposed to match flows that do NOT have value X for the property
   *       under test does match flows with value Y for that property
   * </ul>
   *
   * @param matchingHeaderSpaceGenerator Function to generate a HeaderSpace that matches flows with
   *     a given value for the property being tested (e.g., {@code x ->
   *     HeaderSpace.builder().setDstIps(x).build()})
   * @param notMatchingHeaderSpaceGenerator Function to generate a HeaderSpace that matches flows
   *     that do NOT have a given value for the property being tested (e.g., {@code x ->
   *     HeaderSpace.builder().setNotDstIps(x).build()})
   * @param matching Value for the property under test that should match {@link #_flow}
   * @param notMatching Value for the property under test that should NOT match {@link #_flow}
   * @param <T> Type of the HeaderSpace property under test (e.g., if testing dstIps, it would be an
   *     {@link IpSpace})
   */
  private static <T> void testMatches(
      Function<T, HeaderSpace> matchingHeaderSpaceGenerator,
      Function<T, HeaderSpace> notMatchingHeaderSpaceGenerator,
      T matching,
      T notMatching) {
    // If HeaderSpace requires flow to match property that does match, matches() == true
    HeaderSpace hs = matchingHeaderSpaceGenerator.apply(matching);
    assertThat(hs.matches(_flow, _namedIpSpaces), equalTo(true));

    // If HeaderSpace requires flow to match property that does not match, matches() == false
    hs = matchingHeaderSpaceGenerator.apply(notMatching);
    assertThat(hs.matches(_flow, _namedIpSpaces), equalTo(false));

    // If HeaderSpace requires flow not to match property that does match, matches() == false
    hs = notMatchingHeaderSpaceGenerator.apply(matching);
    assertThat(hs.matches(_flow, _namedIpSpaces), equalTo(false));

    // If HeaderSpace requires flow not to match property that does not match, matches() == true
    hs = notMatchingHeaderSpaceGenerator.apply(notMatching);
    assertThat(hs.matches(_flow, _namedIpSpaces), equalTo(true));
  }
}
