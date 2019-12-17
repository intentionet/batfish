package org.batfish.datamodel.flow;

import static org.batfish.datamodel.transformation.Transformation.always;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;

/** Tests for {@link FirewallSessionTraceInfo}. */
public final class FirewallSessionTraceInfoTest {
  private static final SessionMatchExpr FLOW1 =
      new SessionMatchExpr(IpProtocol.TCP, Ip.parse("1.1.1.1"), Ip.parse("2.2.2.2"), null, null);
  private static final SessionMatchExpr FLOW2 =
      new SessionMatchExpr(IpProtocol.TCP, Ip.parse("2.2.2.2"), Ip.parse("1.1.1.1"), null, null);

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            new FirewallSessionTraceInfo(
                "A", new ForwardOutInterface("B", null), ImmutableSet.of(), FLOW1, null),
            new FirewallSessionTraceInfo(
                "A", new ForwardOutInterface("B", null), ImmutableSet.of(), FLOW1, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo(
                "A1", new ForwardOutInterface("B", null), ImmutableSet.of(), FLOW1, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo(
                "A", new ForwardOutInterface("B1", null), ImmutableSet.of(), FLOW1, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo(
                "A",
                new ForwardOutInterface("B", NodeInterfacePair.of("", "")),
                ImmutableSet.of(),
                FLOW1,
                null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo(
                "A", new ForwardOutInterface("B1", null), ImmutableSet.of(""), FLOW1, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo(
                "A", new ForwardOutInterface("B1", null), ImmutableSet.of(), FLOW2, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo(
                "A",
                new ForwardOutInterface("B1", null),
                ImmutableSet.of(),
                FLOW1,
                always().build()))
        .testEquals();
  }
}
