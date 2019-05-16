package org.batfish.dataplane.ibdp;

import static org.batfish.dataplane.ibdp.TestUtils.assertNoRoute;
import static org.batfish.dataplane.ibdp.TestUtils.assertRoute;

import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class BgpFilterInactiveTest {

  private static final String AS1_NAME = "as1";
  private static final String AS2_ADVERTISE_INACTIVE_NAME = "as2-advertise-inactive";
  private static final String AS2_NO_ADVERTISE_INACTIVE_NAME = "as2-no-advertise-inactive";
  private static final String AS3_NAME = "as3";
  private static final String SNAPSHOT_PATH = "org/batfish/dataplane/ibdp/bgp-filter-inactive";
  private static final Ip STATIC_NEXT_HOP = Ip.parse("10.0.12.254");

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Test
  public void testNoAdvertiseInactive() throws IOException {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    SNAPSHOT_PATH, AS1_NAME, AS2_NO_ADVERTISE_INACTIVE_NAME, AS3_NAME)
                .build(),
            _folder);
    batfish.computeDataPlane();
    IncrementalDataPlane dataplane = (IncrementalDataPlane) batfish.loadDataPlane();
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routes =
        IncrementalBdpEngine.getRoutes(dataplane);

    assertRoute(routes, RoutingProtocol.STATIC, AS1_NAME, Prefix.ZERO, 0, STATIC_NEXT_HOP);
    assertRoute(
        routes,
        RoutingProtocol.STATIC,
        AS2_NO_ADVERTISE_INACTIVE_NAME,
        Prefix.ZERO,
        0,
        STATIC_NEXT_HOP);
    assertNoRoute(routes, AS3_NAME, Prefix.ZERO);
  }

  @Test
  public void testAdvertiseInactive() throws IOException {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    SNAPSHOT_PATH, AS1_NAME, AS2_ADVERTISE_INACTIVE_NAME, AS3_NAME)
                .build(),
            _folder);
    batfish.computeDataPlane();
    IncrementalDataPlane dataplane = (IncrementalDataPlane) batfish.loadDataPlane();
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routes =
        IncrementalBdpEngine.getRoutes(dataplane);

    assertRoute(routes, RoutingProtocol.STATIC, AS1_NAME, Prefix.ZERO, 0, STATIC_NEXT_HOP);
    assertRoute(
        routes,
        RoutingProtocol.STATIC,
        AS2_ADVERTISE_INACTIVE_NAME,
        Prefix.ZERO,
        0,
        STATIC_NEXT_HOP);
    assertRoute(routes, RoutingProtocol.BGP, AS3_NAME, Prefix.ZERO, 0, Ip.parse("10.0.23.2"));
  }
}
