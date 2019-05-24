package org.batfish.question.findmatchingfilterlines;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.question.findmatchingfilterlines.FindMatchingFilterLinesAnswerer.COL_ACTION;
import static org.batfish.question.findmatchingfilterlines.FindMatchingFilterLinesAnswerer.COL_FILTER;
import static org.batfish.question.findmatchingfilterlines.FindMatchingFilterLinesAnswerer.COL_LINE;
import static org.batfish.question.findmatchingfilterlines.FindMatchingFilterLinesAnswerer.COL_LINE_INDEX;
import static org.batfish.question.findmatchingfilterlines.FindMatchingFilterLinesAnswerer.COL_NODE;
import static org.batfish.question.findmatchingfilterlines.FindMatchingFilterLinesAnswerer.getRows;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.table.Row;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.SpecifierContext;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests of {@link FindMatchingFilterLinesAnswerer} */
public class FindMatchingFilterLinesAnswererTest {
  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testThrowsIfNoFiltersMatch() {
    // Config with no ACLs; should see exception because no filters match question specifications
    Configuration c =
        new NetworkFactory()
            .configurationBuilder()
            .setHostname("c")
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();
    MockBatfish batfish = new MockBatfish(ImmutableSortedMap.of("c", c));
    FindMatchingFilterLinesAnswerer answerer =
        new FindMatchingFilterLinesAnswerer(new FindMatchingFilterLinesQuestion(), batfish);
    _thrown.expect(IllegalArgumentException.class);
    answerer.answer();
  }

  @Test
  public void testHeaderSpaceWithDstIps() {
    // Set up ACL containing two lines, make sure header constraints filter to the right line

    // First line accepts TCP to 1.1.1.0/24
    Ip ip1 = Ip.parse("1.1.1.1");
    Prefix prefix1 = Prefix.create(ip1, 24);
    Set<IpProtocol> protocols1 = ImmutableSet.of(IpProtocol.TCP);
    HeaderSpace headerSpace1 =
        HeaderSpace.builder().setDstIps(prefix1.toIpSpace()).setIpProtocols(protocols1).build();
    // Second line accepts UDP to 2.2.2.0/24
    Ip ip2 = Ip.parse("2.2.2.2");
    Prefix prefix2 = Prefix.create(ip2, 24);
    Set<IpProtocol> protocols2 = ImmutableSet.of(IpProtocol.UDP);
    HeaderSpace headerSpace2 =
        HeaderSpace.builder().setDstIps(prefix2.toIpSpace()).setIpProtocols(protocols2).build();

    // Set up config and ACL
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setHostname("c")
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();
    nf.aclBuilder()
        .setOwner(c)
        .setName("acl")
        .setLines(
            ImmutableList.of(
                IpAccessListLine.builder()
                    .setName("line1")
                    .setAction(LineAction.PERMIT)
                    .setMatchCondition(new MatchHeaderSpace(headerSpace1))
                    .build(),
                IpAccessListLine.builder()
                    .setName("line2")
                    .setAction(LineAction.PERMIT)
                    .setMatchCondition(new MatchHeaderSpace(headerSpace2))
                    .build()))
        .build();

    // Set up expected row matchers for first line
    Matcher<Row> line1Matchers =
        allOf(
            hasColumn(COL_NODE, "c", Schema.STRING),
            hasColumn(COL_FILTER, "acl", Schema.STRING),
            hasColumn(COL_LINE, "line1", Schema.STRING),
            hasColumn(COL_LINE_INDEX, 0, Schema.INTEGER),
            hasColumn(COL_ACTION, LineAction.PERMIT.toString(), Schema.STRING));
    {
      // Constrain dstIp to a superset of first line's dstIps (that still doesn't intersect prefix2)
      PacketHeaderConstraints phc =
          PacketHeaderConstraints.builder()
              .setDstIp(Prefix.create(ip1, prefix1.getPrefixLength() - 8).toString())
              .build();
      MockSpecifierContext ctxt =
          MockSpecifierContext.builder().setConfigs(ImmutableMap.of("c", c)).build();
      List<Row> rows = getRows(phc, null, ImmutableMultimap.of("c", "acl"), ctxt);
      assertThat(rows, contains(line1Matchers));
    }
    {
      // Constrain dstIp to a subset of first line's dstIps
      PacketHeaderConstraints phc =
          PacketHeaderConstraints.builder()
              .setDstIp(Prefix.create(ip1, prefix1.getPrefixLength() + 4).toString())
              .build();
      MockSpecifierContext ctxt =
          MockSpecifierContext.builder().setConfigs(ImmutableMap.of("c", c)).build();
      List<Row> rows = getRows(phc, null, ImmutableMultimap.of("c", "acl"), ctxt);
      assertThat(rows, contains(line1Matchers));
    }
    {
      // Constrain protocol to TCP
      PacketHeaderConstraints phc =
          PacketHeaderConstraints.builder().setIpProtocols(ImmutableSet.of(IpProtocol.TCP)).build();
      MockSpecifierContext ctxt =
          MockSpecifierContext.builder().setConfigs(ImmutableMap.of("c", c)).build();
      List<Row> rows = getRows(phc, null, ImmutableMultimap.of("c", "acl"), ctxt);
      assertThat(rows, contains(line1Matchers));
    }
  }

  @Test
  public void testFullAnswer() {
    /*
    Configuration c1 has:
      - ACL acl1, with a permit-all line
      - ACL acl2, with a deny-all line
    Configuration c2 has:
      - ACL acl1, with a permit-all line
     */

    // Construct matchers for the rows corresponding to each ACL line
    Matcher<Row> c1Acl1Matcher =
        allOf(
            hasColumn(COL_NODE, "c1", Schema.STRING),
            hasColumn(COL_FILTER, "acl1", Schema.STRING),
            hasColumn(COL_LINE, IpAccessListLine.ACCEPT_ALL.getName(), Schema.STRING),
            hasColumn(COL_LINE_INDEX, 0, Schema.INTEGER),
            hasColumn(COL_ACTION, LineAction.PERMIT.toString(), Schema.STRING));
    Matcher<Row> c1Acl2Matcher =
        allOf(
            hasColumn(COL_NODE, "c1", Schema.STRING),
            hasColumn(COL_FILTER, "acl2", Schema.STRING),
            hasColumn(COL_LINE, IpAccessListLine.REJECT_ALL.getName(), Schema.STRING),
            hasColumn(COL_LINE_INDEX, 0, Schema.INTEGER),
            hasColumn(COL_ACTION, LineAction.DENY.toString(), Schema.STRING));
    Matcher<Row> c2Acl1Matcher =
        allOf(
            hasColumn(COL_NODE, "c2", Schema.STRING),
            hasColumn(COL_FILTER, "acl1", Schema.STRING),
            hasColumn(COL_LINE, IpAccessListLine.ACCEPT_ALL.getName(), Schema.STRING),
            hasColumn(COL_LINE_INDEX, 0, Schema.INTEGER),
            hasColumn(COL_ACTION, LineAction.PERMIT.toString(), Schema.STRING));

    // Build configs
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    IpAccessList.Builder aclBuilder = nf.aclBuilder();
    Configuration c1 = cb.setHostname("c1").build();
    Configuration c2 = cb.setHostname("c2").build();

    // Add acl1 in both configs
    aclBuilder.setName("acl1").setLines(ImmutableList.of(IpAccessListLine.ACCEPT_ALL));
    aclBuilder.setOwner(c1).build();
    aclBuilder.setOwner(c2).build();

    // Add acl2 in c1
    aclBuilder.setName("acl2").setLines(ImmutableList.of(IpAccessListLine.REJECT_ALL));
    aclBuilder.setOwner(c1).build();

    // Create batfish with configs
    MockBatfish mockBatfish = new MockBatfish(ImmutableSortedMap.of("c1", c1, "c2", c2));

    {
      // Unfiltered getRows should have all three lines
      FindMatchingFilterLinesAnswerer answerer =
          new FindMatchingFilterLinesAnswerer(new FindMatchingFilterLinesQuestion(), mockBatfish);
      assertThat(
          answerer.answer().getRows().getData(),
          containsInAnyOrder(c1Acl1Matcher, c1Acl2Matcher, c2Acl1Matcher));
    }
    {
      // Answerer with nodes "c1" should not give row for c2 acl1
      FindMatchingFilterLinesAnswerer answerer =
          new FindMatchingFilterLinesAnswerer(
              new FindMatchingFilterLinesQuestion("c1", null, null, null, null), mockBatfish);
      assertThat(
          answerer.answer().getRows().getData(), containsInAnyOrder(c1Acl1Matcher, c1Acl2Matcher));
    }
    {
      // Answerer with filters "acl1" should not give row for c1 acl2
      FindMatchingFilterLinesAnswerer answerer =
          new FindMatchingFilterLinesAnswerer(
              new FindMatchingFilterLinesQuestion(null, "acl1", null, null, null), mockBatfish);
      assertThat(
          answerer.answer().getRows().getData(), containsInAnyOrder(c1Acl1Matcher, c2Acl1Matcher));
    }
    {
      // Answerer with action PERMIT should not give row for c1 acl2
      FindMatchingFilterLinesAnswerer answerer =
          new FindMatchingFilterLinesAnswerer(
              new FindMatchingFilterLinesQuestion(null, null, LineAction.PERMIT, null, null),
              mockBatfish);
      assertThat(
          answerer.answer().getRows().getData(), containsInAnyOrder(c1Acl1Matcher, c2Acl1Matcher));
    }
  }

  private static class MockBatfish extends IBatfishTestAdapter {
    private final SortedMap<String, Configuration> _configs;
    private final SpecifierContext _specifierContext;

    MockBatfish(SortedMap<String, Configuration> configs) {
      _configs = configs;
      _specifierContext = MockSpecifierContext.builder().setConfigs(configs).build();
    }

    @Override
    public SortedMap<String, Configuration> loadConfigurations() {
      return _configs;
    }

    @Override
    public SpecifierContext specifierContext() {
      return _specifierContext;
    }
  }
}
