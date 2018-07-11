package org.batfish.question.routes;

import static org.batfish.datamodel.Prefix.MAX_PREFIX_LENGTH;
import static org.batfish.question.routes.RoutesAnswerer.COL_ADMIN_DISTANCE;
import static org.batfish.question.routes.RoutesAnswerer.COL_AS_PATH;
import static org.batfish.question.routes.RoutesAnswerer.COL_COMMUNITIES;
import static org.batfish.question.routes.RoutesAnswerer.COL_LOCAL_PREF;
import static org.batfish.question.routes.RoutesAnswerer.COL_METRIC;
import static org.batfish.question.routes.RoutesAnswerer.COL_NETWORK;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP_IP;
import static org.batfish.question.routes.RoutesAnswerer.COL_NODE;
import static org.batfish.question.routes.RoutesAnswerer.COL_ORIGIN_PROTOCOL;
import static org.batfish.question.routes.RoutesAnswerer.COL_PROTOCOL;
import static org.batfish.question.routes.RoutesAnswerer.COL_TAG;
import static org.batfish.question.routes.RoutesAnswerer.COL_VRF_NAME;
import static org.batfish.question.routes.RoutesAnswerer.computeNextHopNode;
import static org.batfish.question.routes.RoutesAnswerer.getMainRibRoutes;
import static org.batfish.question.routes.RoutesAnswerer.getRowsForBgpRoutes;
import static org.batfish.question.routes.RoutesAnswerer.getTableMetadata;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.SortedMap;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpRoute.Builder;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MockDataPlane;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.question.routes.RoutesQuestion.RibProtocol;
import org.junit.Test;

/** Tests of {@link RoutesAnswerer}. */
public class RoutesAnswererTest {
  @Test
  public void testGetMainRibRoutesWhenEmptyRib() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            "n1", ImmutableSortedMap.of(Configuration.DEFAULT_VRF_NAME, new MockRib<>()));

    Multiset<Row> actual = getMainRibRoutes(ribs, ImmutableSet.of("n1"), ".*", null);

    assertThat(actual, hasSize(0));
  }

  @Test
  public void testHasNodeFiltering() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            "n1",
            ImmutableSortedMap.of(
                Configuration.DEFAULT_VRF_NAME,
                new MockRib<>(
                    ImmutableSet.of(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .setNextHopInterface("Null")
                            .build()))));

    Multiset<Row> actual = getMainRibRoutes(ribs, ImmutableSet.of("differentNode"), ".*", null);

    assertThat(actual, hasSize(0));
  }

  @Test
  public void testHasVrfFiltering() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            "n1",
            ImmutableSortedMap.of(
                Configuration.DEFAULT_VRF_NAME,
                new MockRib<>(
                    ImmutableSet.of(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .setNextHopInterface("Null")
                            .build())),
                "notDefaultVrf",
                new MockRib<>(
                    ImmutableSet.of(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("2.2.2.0/24"))
                            .setNextHopInterface("Null")
                            .build()))));

    Multiset<Row> actual = getMainRibRoutes(ribs, ImmutableSet.of("n1"), "^not.*", null);

    assertThat(actual, hasSize(1));
    assertThat(
        actual.iterator().next().getPrefix(COL_NETWORK), equalTo(Prefix.parse("2.2.2.0/24")));
  }

  @Test
  public void testHasAdminDistanceValue() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            "n1",
            ImmutableSortedMap.of(
                Configuration.DEFAULT_VRF_NAME,
                new MockRib<>(
                    ImmutableSet.of(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .setNextHopInterface("Null")
                            .setAdministrativeCost(10)
                            .build()))));

    Multiset<Row> actual = getMainRibRoutes(ribs, ImmutableSet.of("n1"), ".*", null);

    assertThat(actual.iterator().next().get(COL_ADMIN_DISTANCE, Schema.INTEGER), equalTo(10));
  }

  @Test
  public void testGetTableMetadataProtocolAll() {
    List<ColumnMetadata> columnMetadata = getTableMetadata(RibProtocol.ALL).getColumnMetadata();

    assertThat(
        columnMetadata
            .stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        contains(
            COL_NODE,
            COL_VRF_NAME,
            COL_NETWORK,
            COL_PROTOCOL,
            COL_TAG,
            COL_NEXT_HOP_IP,
            COL_NEXT_HOP,
            COL_ADMIN_DISTANCE));

    assertThat(
        columnMetadata
            .stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        contains(
            Schema.NODE,
            Schema.STRING,
            Schema.PREFIX,
            Schema.STRING,
            Schema.INTEGER,
            Schema.IP,
            Schema.STRING,
            Schema.INTEGER));
  }

  @Test
  public void testGetTableMetadataBGP() {
    List<ColumnMetadata> columnMetadata = getTableMetadata(RibProtocol.BGP).getColumnMetadata();
    ImmutableList.Builder<String> expectedBuilder = ImmutableList.builder();
    expectedBuilder.add(
        COL_NODE,
        COL_VRF_NAME,
        COL_NETWORK,
        COL_PROTOCOL,
        COL_TAG,
        COL_NEXT_HOP_IP,
        // BGP attributes
        COL_AS_PATH,
        COL_METRIC,
        COL_LOCAL_PREF,
        COL_COMMUNITIES,
        COL_ORIGIN_PROTOCOL);
    List<String> expected = expectedBuilder.build();

    assertThat(
        columnMetadata
            .stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        equalTo(expected));
  }

  @Test
  public void testGetBgpRoutesCommunities() {
    Ip ip = new Ip("1.1.1.1");
    List<Row> rows =
        getRowsForBgpRoutes(
            "node",
            "vrf",
            ImmutableSet.of(
                new Builder()
                    .setNetwork(new Prefix(ip, MAX_PREFIX_LENGTH))
                    .setOriginType(OriginType.IGP)
                    .setOriginatorIp(ip)
                    .setReceivedFromIp(ip)
                    .setCommunities(ImmutableSortedSet.of(65537L))
                    .build()));

    assertThat(
        rows.get(0).get(COL_COMMUNITIES, Schema.list(Schema.STRING)),
        equalTo(ImmutableList.of("1:1")));
  }

  @Test
  public void testComputeNextHopNode() {
    assertThat(computeNextHopNode(null, ImmutableMap.of()), nullValue());
    assertThat(computeNextHopNode(new Ip("1.1.1.1"), null), nullValue());
    assertThat(computeNextHopNode(new Ip("1.1.1.1"), ImmutableMap.of()), nullValue());
    assertThat(
        computeNextHopNode(
            new Ip("1.1.1.1"), ImmutableMap.of(new Ip("1.1.1.1"), ImmutableSet.of("n1"))),
        equalTo("n1"));
    assertThat(
        computeNextHopNode(
            new Ip("1.1.1.1"), ImmutableMap.of(new Ip("1.1.1.2"), ImmutableSet.of("n1"))),
        nullValue());
  }

  /** Run through full pipeline (create question and answerer), */
  @Test
  public void testFullAnswerPipelineAndNumResults() {
    // Setup mock data structures
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Vrf vrf = nf.vrfBuilder().setOwner(c).build();
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            c.getName(),
            ImmutableSortedMap.of(
                vrf.getName(),
                new MockRib<>(
                    ImmutableSet.of(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("1.1.1.1/32"))
                            .setNextHopInterface("Null")
                            .build()))));
    NetworkConfigurations nc = NetworkConfigurations.of(ImmutableMap.of(c.getName(), c));

    AnswerElement el =
        new RoutesAnswerer(
                new RoutesQuestion(),
                new MockBatfish(nc, MockDataPlane.builder().setRibs(ribs).build()))
            .answer();

    assert el.getSummary() != null;
    assertThat(el.getSummary().getNumResults(), equalTo(1));

    // no results for empty ribs
    el =
        new RoutesAnswerer(
                new RoutesQuestion(),
                new MockBatfish(
                    nc, MockDataPlane.builder().setRibs(ImmutableSortedMap.of()).build()))
            .answer();
    assert el.getSummary() != null;
    assertThat(el.getSummary().getNumResults(), equalTo(0));
  }
}
