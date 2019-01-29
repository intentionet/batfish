package org.batfish.datamodel.flow;

import static org.batfish.datamodel.acl.AclLineMatchExprs.FALSE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.transformation.Transformation.always;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;

/** Tests for {@link FirewallSessionTraceInfo}. */
public final class FirewallSessionTraceInfoTest {

  @Test
  public void equals() {
    new EqualsTester()
        .addEqualityGroup(
            new FirewallSessionTraceInfo("A", "B", null, ImmutableSet.of(), TRUE, null),
            new FirewallSessionTraceInfo("A", "B", null, ImmutableSet.of(), TRUE, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo("A1", "B", null, ImmutableSet.of(), TRUE, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo("A", "B1", null, ImmutableSet.of(), TRUE, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo(
                "A", "B", new NodeInterfacePair("", ""), ImmutableSet.of(), TRUE, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo("A", "B1", null, ImmutableSet.of(""), TRUE, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo("A", "B1", null, ImmutableSet.of(), FALSE, null))
        .addEqualityGroup(
            new FirewallSessionTraceInfo(
                "A", "B1", null, ImmutableSet.of(), TRUE, always().build()))
        .testEquals();
  }
}
