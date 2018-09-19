package org.batfish.question.testfilters;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.datamodel.IpAccessListLine.acceptingHeaderSpace;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessLists;
import static org.batfish.datamodel.matchers.DataModelMatchers.forAll;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasEvents;
import static org.batfish.datamodel.matchers.DataModelMatchers.isDefaultDeniedByIpAccessListNamed;
import static org.batfish.datamodel.matchers.DataModelMatchers.isPermittedByIpAccessListLineThat;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.datamodel.matchers.RowsMatchers.hasSize;
import static org.batfish.datamodel.matchers.TableAnswerElementMatchers.hasRows;
import static org.batfish.question.testfilters.TestFiltersAnswerer.COL_FILTER_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.util.SortedMap;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.matchers.PermittedByIpAccessListLineMatchers;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.SpecifierContext;
import org.junit.Before;
import org.junit.Test;

public class TestFiltersAnswererTest {

  private NetworkFactory _nf = new NetworkFactory();
  private Configuration.Builder _cb;

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
  }

  private static final class MockBatfish extends IBatfishTestAdapter {

    SortedMap<String, Configuration> _configurations;
    SpecifierContext _specifierContext;

    public MockBatfish(SortedMap<String, Configuration> configurations) {
      this(configurations, null);
    }

    public MockBatfish(SortedMap<String, Configuration> configurations, SpecifierContext ctxt) {
      _configurations = configurations;
      _specifierContext = ctxt;
    }

    @Override
    public SortedMap<String, Configuration> loadConfigurations() {
      return _configurations;
    }

    @Override
    public SpecifierContext specifierContext() {
      return firstNonNull(
          _specifierContext,
          MockSpecifierContext.builder().setConfigs(loadConfigurations()).build());
    }
  }

  @Test
  public void testIndirection() throws IOException {
    Configuration c = _cb.build();
    IpAccessList.Builder aclb = _nf.aclBuilder().setOwner(c);

    /*
    Reference ACL contains 1 line: Permit 1.0.0.0/24
    Main ACL contains 2 lines:
    0. Permit anything that reference ACL permits
    1. Permit 1.0.0.0/24
     */

    IpAccessList referencedAcl =
        aclb.setLines(
                ImmutableList.of(
                    acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(Prefix.parse("1.0.0.0/32").toIpSpace())
                            .build())))
            .setName("acl1")
            .build();
    IpAccessList acl =
        aclb.setLines(
                ImmutableList.of(
                    IpAccessListLine.accepting()
                        .setMatchCondition(new PermittedByAcl(referencedAcl.getName()))
                        .build(),
                    acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(Prefix.parse("1.0.0.0/24").toIpSpace())
                            .build())))
            .setName("acl2")
            .build();

    MockBatfish batfish = new MockBatfish(ImmutableSortedMap.of(c.getHostname(), c));

    assertThat(c, hasIpAccessLists(hasEntry(referencedAcl.getName(), referencedAcl)));
    assertThat(c, hasIpAccessLists(hasEntry(acl.getName(), acl)));

    TestFiltersQuestion question =
        new TestFiltersQuestion(
            null, null, PacketHeaderConstraints.builder().setSrcIp("1.0.0.4").build(), null);
    TestFiltersAnswerer answerer = new TestFiltersAnswerer(question, batfish);
    TableAnswerElement answer = answerer.answer();

    /*
     * Trace should be present for referencing acl with one event:
     * Permitted by referencing acl
     *
     * NO event for referenced ACL since it's action was not match (default deny)
     */
    assertThat(
        answer,
        hasRows(
            forAll(
                hasColumn(COL_FILTER_NAME, equalTo(acl.getName()), Schema.STRING),
                hasColumn(
                    TestFiltersAnswerer.COL_TRACE,
                    hasEvents(
                        contains(
                            ImmutableList.of(
                                isPermittedByIpAccessListLineThat(
                                    PermittedByIpAccessListLineMatchers.hasName(acl.getName()))))),
                    Schema.ACL_TRACE))));
    /* Trace should be present for referenced acl with one event: not matching the referenced acl */
    assertThat(
        answer,
        hasRows(
            forAll(
                hasColumn(COL_FILTER_NAME, equalTo(referencedAcl.getName()), Schema.STRING),
                hasColumn(
                    TestFiltersAnswerer.COL_TRACE,
                    hasEvents(
                        contains(isDefaultDeniedByIpAccessListNamed(referencedAcl.getName()))),
                    Schema.ACL_TRACE))));
  }

  @Test
  public void testOneRowPerFilterPerConfig() throws IOException {
    IpAccessList.Builder aclb = _nf.aclBuilder();
    Configuration c1 = _cb.build();
    Configuration c2 = _cb.build();
    Configuration c3 = _cb.build();

    // Create 2 ACLs for each of 3 configs.
    aclb.setOwner(c1).build();
    aclb.build();
    aclb.setOwner(c2).build();
    aclb.build();
    aclb.setOwner(c3).build();
    aclb.build();

    IBatfish batfish =
        new MockBatfish(
            ImmutableSortedMap.of(
                c1.getHostname(), c1, c2.getHostname(), c2, c3.getHostname(), c3));
    TestFiltersQuestion question = new TestFiltersQuestion(null, null, null, null);
    TestFiltersAnswerer answerer = new TestFiltersAnswerer(question, batfish);
    TableAnswerElement answer = answerer.answer();

    // There should be 6 rows
    assertThat(answer.getRows(), hasSize(6));
  }

  @Test
  public void testOneRowPerInterface() throws IOException {
    Configuration c1 = _cb.setHostname("c1").build();
    IpAccessList.Builder aclb = _nf.aclBuilder();
    aclb.setOwner(c1).build();

    Interface iface1 =
        Interface.builder()
            .setName("iface1")
            .setOwner(c1)
            .setAddress(new InterfaceAddress("1.1.1.1/24"))
            .setVrf(new Vrf("default"))
            .build();
    Interface iface2 =
        Interface.builder()
            .setName("iface2")
            .setOwner(c1)
            .setAddress(new InterfaceAddress("2.2.2.2/24"))
            .setVrf(new Vrf("default"))
            .build();

    c1.setInterfaces(ImmutableSortedMap.of("iface1", iface1, "iface2", iface2));

    SortedMap<String, Configuration> configs = ImmutableSortedMap.of(c1.getHostname(), c1);
    IBatfish batfish =
        new MockBatfish(
            configs,
            MockSpecifierContext.builder()
                .setConfigs(configs)
                .setInterfaceOwnedIps(
                    ImmutableMap.of(
                        "c1",
                        ImmutableMap.of(
                            "iface1",
                            iface1.getAddress().getIp().toIpSpace(),
                            "iface2",
                            iface2.getAddress().getIp().toIpSpace())))
                .build());

    TestFiltersQuestion question = new TestFiltersQuestion(null, null, null, null);
    TestFiltersAnswerer answerer = new TestFiltersAnswerer(question, batfish);
    TableAnswerElement answer = answerer.answer();

    // There should be 2 rows
    assertThat(answer.getRows(), hasSize(2));
  }
}
