package org.batfish.question.routes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static java.util.Comparator.naturalOrder;
import static org.batfish.datamodel.table.TableDiff.COL_BASE_PREFIX;
import static org.batfish.datamodel.table.TableDiff.COL_DELTA_PREFIX;
import static org.batfish.question.routes.RoutesAnswerer.COL_ADMIN_DISTANCE;
import static org.batfish.question.routes.RoutesAnswerer.COL_AS_PATH;
import static org.batfish.question.routes.RoutesAnswerer.COL_CLUSTER_LIST;
import static org.batfish.question.routes.RoutesAnswerer.COL_COMMUNITIES;
import static org.batfish.question.routes.RoutesAnswerer.COL_LOCAL_PREF;
import static org.batfish.question.routes.RoutesAnswerer.COL_METRIC;
import static org.batfish.question.routes.RoutesAnswerer.COL_NETWORK;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP_INTERFACE;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP_IP;
import static org.batfish.question.routes.RoutesAnswerer.COL_NODE;
import static org.batfish.question.routes.RoutesAnswerer.COL_ORIGINATOR_ID;
import static org.batfish.question.routes.RoutesAnswerer.COL_ORIGIN_PROTOCOL;
import static org.batfish.question.routes.RoutesAnswerer.COL_ORIGIN_TYPE;
import static org.batfish.question.routes.RoutesAnswerer.COL_PROTOCOL;
import static org.batfish.question.routes.RoutesAnswerer.COL_ROUTE_DISTINGUISHER;
import static org.batfish.question.routes.RoutesAnswerer.COL_ROUTE_ENTRY_PRESENCE;
import static org.batfish.question.routes.RoutesAnswerer.COL_STATUS;
import static org.batfish.question.routes.RoutesAnswerer.COL_TAG;
import static org.batfish.question.routes.RoutesAnswerer.COL_VRF_NAME;
import static org.batfish.question.routes.RoutesAnswerer.getDiffTableMetadata;
import static org.batfish.question.routes.RoutesAnswerer.getTableMetadata;
import static org.batfish.question.routes.RoutesAnswererUtil.getMainRibRoutes;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AbstractRouteDecorator;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LocalRoute;
import org.batfish.datamodel.MockDataPlane;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.ResolutionRestriction;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.question.routes.RoutesQuestion.RibProtocol;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.RoutingProtocolSpecifier;
import org.batfish.specifier.SpecifierContext;
import org.junit.Test;

/** Tests of {@link RoutesAnswerer}. */
public class RoutesAnswererTest {

  @Test
  public void testGetMainRibRoutesWhenEmptyRib() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRouteDecorator>>> ribs =
        ImmutableSortedMap.of(
            "n1", ImmutableSortedMap.of(Configuration.DEFAULT_VRF_NAME, new MockRib<>()));

    Multiset<Row> actual =
        getMainRibRoutes(
            ribs,
            ImmutableSet.of("n1"),
            null,
            RoutingProtocolSpecifier.ALL_PROTOCOLS_SPECIFIER,
            ".*",
            null);

    assertThat(actual.entrySet(), hasSize(0));
  }

  @Test
  public void testHasNetworkFiltering() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRouteDecorator>>> ribs =
        ImmutableSortedMap.of(
            "n1",
            ImmutableSortedMap.of(
                Configuration.DEFAULT_VRF_NAME,
                new MockRib<>(
                    ImmutableSet.of(
                        StaticRoute.testBuilder()
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .setNextHopInterface("Null")
                            .setAdministrativeCost(1)
                            .build(),
                        StaticRoute.testBuilder()
                            .setNetwork(Prefix.parse("2.2.2.0/24"))
                            .setNextHopInterface("Null")
                            .setAdministrativeCost(1)
                            .build()))));

    Multiset<Row> actual =
        getMainRibRoutes(
            ribs,
            ImmutableSet.of("n1"),
            Prefix.create(Ip.parse("2.2.2.0"), 24),
            RoutingProtocolSpecifier.ALL_PROTOCOLS_SPECIFIER,
            ".*",
            null);

    assertThat(actual, hasSize(1));
    assertThat(
        actual.iterator().next().getPrefix(COL_NETWORK), equalTo(Prefix.parse("2.2.2.0/24")));
  }

  @Test
  public void testHasNodeFiltering() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRouteDecorator>>> ribs =
        ImmutableSortedMap.of(
            "n1",
            ImmutableSortedMap.of(
                Configuration.DEFAULT_VRF_NAME,
                new MockRib<>(
                    ImmutableSet.of(
                        StaticRoute.testBuilder()
                            .setAdministrativeCost(1)
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .setNextHopInterface("Null")
                            .build()))));

    Multiset<Row> actual =
        getMainRibRoutes(
            ribs,
            ImmutableSet.of("differentNode"),
            null,
            RoutingProtocolSpecifier.ALL_PROTOCOLS_SPECIFIER,
            ".*",
            null);

    assertThat(actual, hasSize(0));
  }

  @Test
  public void testHasProtocolFiltering() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRouteDecorator>>> ribs =
        ImmutableSortedMap.of(
            "n1",
            ImmutableSortedMap.of(
                Configuration.DEFAULT_VRF_NAME,
                new MockRib<>(
                    ImmutableSet.of(
                        StaticRoute.testBuilder()
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .setNextHopInterface("Null")
                            .setAdministrativeCost(1)
                            .build(),
                        new LocalRoute(ConcreteInterfaceAddress.parse("2.2.2.1/24"), "Null")))));

    Multiset<Row> actual =
        getMainRibRoutes(
            ribs, ImmutableSet.of("n1"), null, new RoutingProtocolSpecifier("static"), ".*", null);

    assertThat(actual, hasSize(1));
    assertThat(
        actual.iterator().next().getPrefix(COL_NETWORK), equalTo(Prefix.parse("1.1.1.0/24")));
  }

  @Test
  public void testHasVrfFiltering() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRouteDecorator>>> ribs =
        ImmutableSortedMap.of(
            "n1",
            ImmutableSortedMap.of(
                Configuration.DEFAULT_VRF_NAME,
                new MockRib<>(
                    ImmutableSet.of(
                        StaticRoute.testBuilder()
                            .setAdministrativeCost(1)
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .setNextHopInterface("Null")
                            .build())),
                "notDefaultVrf",
                new MockRib<>(
                    ImmutableSet.of(
                        StaticRoute.testBuilder()
                            .setNetwork(Prefix.parse("2.2.2.0/24"))
                            .setNextHopInterface("Null")
                            .setAdministrativeCost(1)
                            .build()))));

    Multiset<Row> actual =
        getMainRibRoutes(
            ribs,
            ImmutableSet.of("n1"),
            null,
            RoutingProtocolSpecifier.ALL_PROTOCOLS_SPECIFIER,
            "^not.*",
            null);

    assertThat(actual, hasSize(1));
    assertThat(
        actual.iterator().next().getPrefix(COL_NETWORK), equalTo(Prefix.parse("2.2.2.0/24")));
  }

  @Test
  public void testGetTableMetadataProtocolAll() {
    List<ColumnMetadata> columnMetadata = getTableMetadata(RibProtocol.MAIN).getColumnMetadata();

    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        contains(
            COL_NODE,
            COL_VRF_NAME,
            COL_NETWORK,
            COL_NEXT_HOP,
            COL_NEXT_HOP_IP,
            COL_NEXT_HOP_INTERFACE,
            COL_PROTOCOL,
            COL_METRIC,
            COL_ADMIN_DISTANCE,
            COL_TAG));

    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        contains(
            Schema.NODE,
            Schema.STRING,
            Schema.PREFIX,
            Schema.STRING,
            Schema.IP,
            Schema.STRING,
            Schema.STRING,
            Schema.LONG,
            Schema.INTEGER,
            Schema.LONG));
  }

  @Test
  public void testGetTableMetadataBGP() {
    List<String> expected =
        ImmutableList.of(
            COL_NODE,
            COL_VRF_NAME,
            COL_NETWORK,
            COL_STATUS,
            COL_NEXT_HOP_IP,
            COL_NEXT_HOP_INTERFACE,
            COL_PROTOCOL,
            // BGP attributes
            COL_AS_PATH,
            COL_METRIC,
            COL_LOCAL_PREF,
            COL_COMMUNITIES,
            COL_ORIGIN_PROTOCOL,
            COL_ORIGIN_TYPE,
            COL_ORIGINATOR_ID,
            COL_CLUSTER_LIST,
            COL_TAG);

    List<ColumnMetadata> columnMetadata = getTableMetadata(RibProtocol.BGP).getColumnMetadata();
    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        equalTo(expected));
  }

  @Test
  public void testGetTableMetadataEvpn() {
    List<String> expected =
        ImmutableList.of(
            COL_NODE,
            COL_VRF_NAME,
            COL_NETWORK,
            COL_STATUS,
            COL_ROUTE_DISTINGUISHER,
            COL_NEXT_HOP_IP,
            COL_NEXT_HOP_INTERFACE,
            COL_PROTOCOL,
            // BGP attributes
            COL_AS_PATH,
            COL_METRIC,
            COL_LOCAL_PREF,
            COL_COMMUNITIES,
            COL_ORIGIN_PROTOCOL,
            COL_ORIGIN_TYPE,
            COL_ORIGINATOR_ID,
            COL_CLUSTER_LIST,
            COL_TAG);

    List<ColumnMetadata> columnMetadata = getTableMetadata(RibProtocol.EVPN).getColumnMetadata();
    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        equalTo(expected));
  }

  @Test
  public void testGetDiffTableMetadataProtocolAll() {
    List<ColumnMetadata> columnMetadata =
        getDiffTableMetadata(RibProtocol.MAIN).getColumnMetadata();

    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        contains(
            COL_NODE,
            COL_VRF_NAME,
            COL_NETWORK,
            COL_ROUTE_ENTRY_PRESENCE,
            COL_BASE_PREFIX + COL_NEXT_HOP,
            COL_DELTA_PREFIX + COL_NEXT_HOP,
            COL_BASE_PREFIX + COL_NEXT_HOP_IP,
            COL_DELTA_PREFIX + COL_NEXT_HOP_IP,
            COL_BASE_PREFIX + COL_PROTOCOL,
            COL_DELTA_PREFIX + COL_PROTOCOL,
            COL_BASE_PREFIX + COL_NEXT_HOP_INTERFACE,
            COL_DELTA_PREFIX + COL_NEXT_HOP_INTERFACE,
            COL_BASE_PREFIX + COL_METRIC,
            COL_DELTA_PREFIX + COL_METRIC,
            COL_BASE_PREFIX + COL_ADMIN_DISTANCE,
            COL_DELTA_PREFIX + COL_ADMIN_DISTANCE,
            COL_BASE_PREFIX + COL_TAG,
            COL_DELTA_PREFIX + COL_TAG));

    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        contains(
            Schema.NODE,
            Schema.STRING,
            Schema.PREFIX,
            Schema.STRING,
            Schema.STRING,
            Schema.STRING,
            Schema.IP,
            Schema.IP,
            Schema.STRING,
            Schema.STRING,
            Schema.STRING,
            Schema.STRING,
            Schema.LONG,
            Schema.LONG,
            Schema.INTEGER,
            Schema.INTEGER,
            Schema.LONG,
            Schema.LONG));
  }

  @Test
  public void testGetDiffTableMetadataBGP() {
    ImmutableList.Builder<String> expectedBuilder = ImmutableList.builder();
    expectedBuilder.add(
        COL_NODE,
        COL_VRF_NAME,
        COL_NETWORK,
        COL_ROUTE_ENTRY_PRESENCE,
        COL_BASE_PREFIX + COL_STATUS,
        COL_DELTA_PREFIX + COL_STATUS,
        COL_BASE_PREFIX + COL_NEXT_HOP_IP,
        COL_DELTA_PREFIX + COL_NEXT_HOP_IP,
        COL_BASE_PREFIX + COL_PROTOCOL,
        COL_DELTA_PREFIX + COL_PROTOCOL,
        COL_BASE_PREFIX + COL_AS_PATH,
        COL_DELTA_PREFIX + COL_AS_PATH,
        COL_BASE_PREFIX + COL_METRIC,
        COL_DELTA_PREFIX + COL_METRIC,
        COL_BASE_PREFIX + COL_LOCAL_PREF,
        COL_DELTA_PREFIX + COL_LOCAL_PREF,
        COL_BASE_PREFIX + COL_COMMUNITIES,
        COL_DELTA_PREFIX + COL_COMMUNITIES,
        COL_BASE_PREFIX + COL_ORIGIN_PROTOCOL,
        COL_DELTA_PREFIX + COL_ORIGIN_PROTOCOL,
        COL_BASE_PREFIX + COL_ORIGIN_TYPE,
        COL_DELTA_PREFIX + COL_ORIGIN_TYPE,
        COL_BASE_PREFIX + COL_TAG,
        COL_DELTA_PREFIX + COL_TAG);

    ImmutableList.Builder<Schema> schemaBuilderList = ImmutableList.builder();
    schemaBuilderList.add(
        Schema.NODE,
        Schema.STRING,
        Schema.PREFIX,
        Schema.STRING,
        Schema.list(Schema.STRING),
        Schema.list(Schema.STRING),
        Schema.IP,
        Schema.IP,
        Schema.STRING,
        Schema.STRING,
        Schema.STRING,
        Schema.STRING,
        Schema.LONG,
        Schema.LONG,
        Schema.LONG,
        Schema.LONG,
        Schema.list(Schema.STRING),
        Schema.list(Schema.STRING),
        Schema.STRING,
        Schema.STRING,
        Schema.STRING,
        Schema.STRING,
        Schema.LONG,
        Schema.LONG);

    List<ColumnMetadata> columnMetadata = getDiffTableMetadata(RibProtocol.BGP).getColumnMetadata();
    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        equalTo(expectedBuilder.build()));

    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        equalTo(schemaBuilderList.build()));
  }

  /** Run through full pipeline (create question and answerer), */
  @Test
  public void testFullAnswerPipelineAndNumResults() {
    // Setup mock data structures
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Vrf vrf = nf.vrfBuilder().setOwner(c).build();
    SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs =
        ImmutableSortedMap.of(
            c.getHostname(),
            ImmutableSortedMap.of(
                vrf.getName(),
                new MockRib<>(
                    ImmutableSet.of(
                        new AnnotatedRoute<>(
                            StaticRoute.testBuilder()
                                .setAdministrativeCost(1)
                                .setNetwork(Prefix.parse("1.1.1.1/32"))
                                .setNextHopInterface("Null")
                                .build(),
                            vrf.getName())))));
    NetworkConfigurations nc = NetworkConfigurations.of(ImmutableMap.of(c.getHostname(), c));

    MockBatfish batfish = new MockBatfish(nc, MockDataPlane.builder().setRibs(ribs).build());
    AnswerElement el =
        new RoutesAnswerer(new RoutesQuestion(), batfish).answer(batfish.getSnapshot());

    assert el.getSummary() != null;
    assertThat(el.getSummary().getNumResults(), equalTo(1));

    // no results for empty ribs
    batfish = new MockBatfish(nc, MockDataPlane.builder().setRibs(ImmutableSortedMap.of()).build());
    el = new RoutesAnswerer(new RoutesQuestion(), batfish).answer(batfish.getSnapshot());
    assert el.getSummary() != null;
    assertThat(el.getSummary().getNumResults(), equalTo(0));
  }

  @Test
  public void testHasTextDesc() {
    String textDesc = getTableMetadata(RibProtocol.MAIN).getTextDesc();

    assertThat(textDesc, notNullValue());
    assertThat(textDesc, not(emptyString()));
  }

  static class MockBatfish extends IBatfishTestAdapter {

    private final NetworkConfigurations _configs;
    private final DataPlane _dp;

    public MockBatfish(NetworkConfigurations configs, DataPlane dp) {
      _configs = configs;
      _dp = dp;
    }

    @Override
    public SortedMap<String, Configuration> loadConfigurations(NetworkSnapshot snapshot) {
      assertThat(snapshot, equalTo(getSnapshot()));
      return _configs.getMap().entrySet().stream()
          .collect(toImmutableSortedMap(naturalOrder(), Entry::getKey, Entry::getValue));
    }

    @Override
    public DataPlane loadDataPlane(NetworkSnapshot snapshot) {
      checkArgument(snapshot.equals(getSnapshot()));
      return _dp;
    }

    @Override
    public SpecifierContext specifierContext(NetworkSnapshot snapshot) {
      return MockSpecifierContext.builder().setConfigs(loadConfigurations(snapshot)).build();
    }
  }

  /** Mock rib that only supports one operation: returning pre-set routes. */
  static class MockRib<R extends AbstractRouteDecorator> implements GenericRib<R> {

    private Set<R> _routes;

    MockRib() {
      _routes = ImmutableSet.of();
    }

    MockRib(Set<R> routes) {
      _routes = routes;
    }

    @Override
    public int comparePreference(R lhs, R rhs) {
      return 0;
    }

    @Override
    public boolean intersectsPrefixSpace(PrefixSpace prefixSpace) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsRoute(AbstractRouteDecorator route) {
      return _routes.contains(route);
    }

    @Override
    public Set<AbstractRoute> getRoutes() {
      return _routes.stream()
          .map(AbstractRouteDecorator::getAbstractRoute)
          .collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public Set<R> getTypedRoutes() {
      return _routes;
    }

    @Override
    public Set<R> getTypedBackupRoutes() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<R> longestPrefixMatch(Ip address, ResolutionRestriction<R> restriction) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<R> longestPrefixMatch(
        Ip address, int maxPrefixLength, ResolutionRestriction<R> restriction) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean mergeRoute(R route) {
      throw new UnsupportedOperationException();
    }
  }
}
