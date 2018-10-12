package org.batfish.datamodel;

import static org.batfish.datamodel.PacketHeaderConstraintsUtil.DEFAULT_PACKET_LENGTH;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.setDscpValue;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.setDstPort;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.setEcnValue;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.setFlowStates;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.setFragmentOffsets;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.setIcmpValues;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.setPacketLength;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.setSrcPort;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.setTcpFlags;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collections;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.datamodel.Flow.Builder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests of {@link PacketHeaderConstraintsUtil} */
public class PacketHeaderConstraintsUtilTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testToHeaderSpaceConversion() {
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder()
            .setSrcPorts(ImmutableSortedSet.of(new SubRange(1, 3), new SubRange(5, 6)))
            .setDstPorts(Collections.singleton(new SubRange(11, 12)))
            .setEcns(Collections.singleton(new SubRange(1, 3)))
            .setIpProtocols(Collections.singleton(IpProtocol.TCP))
            .build();

    HeaderSpace hs = PacketHeaderConstraintsUtil.toHeaderSpaceBuilder(phc).build();
    assertThat(
        hs.getSrcPorts(), equalTo(ImmutableSortedSet.of(new SubRange(1, 3), new SubRange(5, 6))));
    assertThat(hs.getDstPorts(), equalTo(Collections.singleton(new SubRange(11, 12))));
    assertThat(hs.getIpProtocols(), equalTo(Collections.singleton(IpProtocol.TCP)));
    assertThat(hs.getNotDstPorts(), empty());
  }

  @Test
  public void testToBDDConversion() {
    BDDPacket packet = new BDDPacket();
    assertThat(
        PacketHeaderConstraintsUtil.toBDD(
            PacketHeaderConstraints.unconstrained(),
            UniverseIpSpace.INSTANCE,
            UniverseIpSpace.INSTANCE),
        equalTo(packet.getFactory().one()));
  }

  @Test
  public void testSetIcmpValueMultiple() {
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder()
            .setIcmpCodes(ImmutableSet.of(new SubRange(0, 10)))
            .build();
    Builder builder = Flow.builder();
    thrown.expect(IllegalArgumentException.class);
    setIcmpValues(phc, builder);
  }

  @Test
  public void testSetDscpValueMultiple() {
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder().setDscps(ImmutableSet.of(new SubRange(0, 10))).build();
    Builder builder = Flow.builder();
    thrown.expect(IllegalArgumentException.class);
    setDscpValue(phc, builder);
  }

  @Test
  public void testSetSrcPortMultiple() {
    Builder builder = Flow.builder().setIngressNode("node").setTag("tag");
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder().setSrcPorts(ImmutableSet.of(new SubRange(1, 10))).build();
    thrown.expect(IllegalArgumentException.class);
    setSrcPort(phc, builder);
  }

  @Test
  public void testSetDstPortMultiple() {
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder().setDstPorts(ImmutableSet.of(new SubRange(1, 10))).build();
    Builder builder = Flow.builder();
    thrown.expect(IllegalArgumentException.class);
    setDstPort(phc, builder);
  }

  @Test
  public void testSetMultiplePacketLengths() {
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder()
            .setPacketLengths(ImmutableSet.of(new SubRange(1, 10)))
            .build();
    Builder builder = Flow.builder();
    thrown.expect(IllegalArgumentException.class);
    setPacketLength(phc, builder);
  }

  @Test
  public void testDefaultPacketLength() {
    Builder builder =
        Flow.builder().setIngressNode("node").setIngressInterface("iface").setTag("tag");
    PacketHeaderConstraints phc = PacketHeaderConstraints.unconstrained();
    setPacketLength(phc, builder);
    assertThat(builder.build().getPacketLength(), equalTo(DEFAULT_PACKET_LENGTH));
  }

  @Test
  public void testSetEcnValue() {
    Builder builder =
        Flow.builder().setIngressNode("node").setIngressInterface("iface").setTag("tag");
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder()
            .setEcns(Collections.singleton(new SubRange(1, 1)))
            .build();
    setEcnValue(phc, builder);
    assertThat(builder.build().getEcn(), equalTo(1));
  }

  @Test
  public void testSetEcnValuesMultiple() {
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder().setEcns(ImmutableSet.of(new SubRange(0, 3))).build();
    Builder builder = Flow.builder();
    thrown.expect(IllegalArgumentException.class);
    setEcnValue(phc, builder);
  }

  @Test
  public void testSetFragmentOffsets() {
    Builder builder =
        Flow.builder().setIngressNode("node").setIngressInterface("iface").setTag("tag");
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder()
            .setFragmentOffsets(Collections.singleton(new SubRange(2, 2)))
            .build();
    setFragmentOffsets(phc, builder);
    assertThat(builder.build().getFragmentOffset(), equalTo(2));
  }

  @Test
  public void testSetFragmentOffsetsMultiple() {
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder()
            .setFragmentOffsets(ImmutableSet.of(new SubRange(0, 10)))
            .build();
    Builder builder = Flow.builder();
    thrown.expect(IllegalArgumentException.class);
    setFragmentOffsets(phc, builder);
  }

  @Test
  public void testSetFlowStates() {
    Builder builder =
        Flow.builder().setIngressNode("node").setIngressInterface("iface").setTag("tag");
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder()
            .setFlowStates(Collections.singleton(FlowState.ESTABLISHED))
            .build();
    setFlowStates(phc, builder);
    assertThat(builder.build().getState(), equalTo(FlowState.ESTABLISHED));
  }

  @Test
  public void testSetFlowStatesMultiple() {
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder()
            .setFlowStates(ImmutableSet.of(FlowState.NEW, FlowState.ESTABLISHED))
            .build();
    Builder builder = Flow.builder();
    thrown.expect(IllegalArgumentException.class);
    setFlowStates(phc, builder);
  }

  @Test
  public void testSetTcpFlags() {
    Builder builder =
        Flow.builder().setIngressNode("node").setIngressInterface("iface").setTag("tag");
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder()
            .setTcpFlags(Collections.singleton(TcpFlagsMatchConditions.ACK_TCP_FLAG))
            .build();
    setTcpFlags(phc, builder);
    assertThat(builder.build().getTcpFlagsAck(), equalTo(1));
  }

  @Test
  public void testSetTcpFlagsMultiple() {
    PacketHeaderConstraints phc =
        PacketHeaderConstraints.builder()
            .setTcpFlags(
                ImmutableSet.of(
                    TcpFlagsMatchConditions.ACK_TCP_FLAG,
                    TcpFlagsMatchConditions.builder().build()))
            .build();
    Builder builder = Flow.builder();
    thrown.expect(IllegalArgumentException.class);
    setTcpFlags(phc, builder);
  }
}
