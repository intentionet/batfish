package org.batfish.datamodel.questions;

import static org.batfish.datamodel.questions.BgpProcessPropertySpecifier.isRouteReflector;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSortedMap;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link BgpProcessPropertySpecifier}. */
@RunWith(JUnit4.class)
public class BgpProcessPropertySpecifierTest {
  @Test
  public void testIsRouteReflector() {
    BgpProcess emptyProcess = new BgpProcess(Ip.ZERO);
    assertFalse("no rr clients", isRouteReflector(emptyProcess));

    BgpActivePeerConfig activePeerWithRRC =
        BgpActivePeerConfig.builder().setRouteReflectorClient(true).build();
    BgpActivePeerConfig activePeerWithoutRRC =
        BgpActivePeerConfig.builder().setRouteReflectorClient(false).build();
    BgpPassivePeerConfig passivePeerWithRRC =
        BgpPassivePeerConfig.builder().setRouteReflectorClient(true).build();
    BgpPassivePeerConfig passivePeerWithoutRRC =
        BgpPassivePeerConfig.builder().setRouteReflectorClient(false).build();
    Prefix p32a = Prefix.parse("1.2.3.4/32");
    Prefix p32b = Prefix.parse("1.2.3.5/32");
    Prefix p30a = Prefix.parse("1.2.3.4/30");
    Prefix p30b = Prefix.parse("1.2.3.8/30");

    // One active peer RRC
    BgpProcess hasActiveNeighbor = new BgpProcess(Ip.ZERO);
    hasActiveNeighbor.setNeighbors(ImmutableSortedMap.of(p32a, activePeerWithRRC));
    assertTrue("has active rr client", isRouteReflector(hasActiveNeighbor));

    // One passive peer RRC
    BgpProcess hasPassiveNeighbor = new BgpProcess(Ip.ZERO);
    hasPassiveNeighbor.setPassiveNeighbors(ImmutableSortedMap.of(p30a, passivePeerWithRRC));
    assertTrue("has passive rr client", isRouteReflector(hasPassiveNeighbor));

    // Mix
    BgpProcess hasNeighborMix = new BgpProcess(Ip.ZERO);
    hasNeighborMix.setNeighbors(
        ImmutableSortedMap.of(p32a, activePeerWithoutRRC, p32b, activePeerWithRRC));
    hasNeighborMix.setPassiveNeighbors(
        ImmutableSortedMap.of(p30a, passivePeerWithoutRRC, p30b, passivePeerWithRRC));
    assertTrue("has mix of active and inactive rr client", isRouteReflector(hasNeighborMix));

    // Both inactive
    BgpProcess hasAllInactive = new BgpProcess(Ip.ZERO);
    hasAllInactive.setNeighbors(ImmutableSortedMap.of(p32a, activePeerWithoutRRC));
    hasAllInactive.setPassiveNeighbors(ImmutableSortedMap.of(p30a, passivePeerWithoutRRC));
    assertFalse("has multiple inactive rr clients", isRouteReflector(hasAllInactive));
  }
}
