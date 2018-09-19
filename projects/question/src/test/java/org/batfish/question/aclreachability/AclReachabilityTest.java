package org.batfish.question.aclreachability;

import static org.batfish.datamodel.IpAccessListLine.acceptingHeaderSpace;
import static org.batfish.datamodel.IpAccessListLine.rejectingHeaderSpace;
import static org.batfish.datamodel.LineAction.PERMIT;
import static org.batfish.datamodel.answers.AclReachabilityRows.COLUMN_METADATA;
import static org.batfish.datamodel.answers.AclReachabilityRows.COL_BLOCKED_LINE_ACTION;
import static org.batfish.datamodel.answers.AclReachabilityRows.COL_BLOCKED_LINE_NUM;
import static org.batfish.datamodel.answers.AclReachabilityRows.COL_BLOCKING_LINE_NUMS;
import static org.batfish.datamodel.answers.AclReachabilityRows.COL_DIFF_ACTION;
import static org.batfish.datamodel.answers.AclReachabilityRows.COL_LINES;
import static org.batfish.datamodel.answers.AclReachabilityRows.COL_MESSAGE;
import static org.batfish.datamodel.answers.AclReachabilityRows.COL_REASON;
import static org.batfish.datamodel.answers.AclReachabilityRows.COL_SOURCES;
import static org.batfish.datamodel.answers.AclReachabilityRows.Reason.BLOCKING_LINES;
import static org.batfish.datamodel.answers.AclReachabilityRows.Reason.CYCLICAL_REFERENCE;
import static org.batfish.datamodel.answers.AclReachabilityRows.Reason.UNDEFINED_REFERENCE;
import static org.batfish.datamodel.answers.AclReachabilityRows.Reason.UNMATCHABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** End-to-end tests of {@link AclReachabilityQuestion}. */
public class AclReachabilityTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private Configuration _c1;
  private Configuration _c2;

  private IpAccessList.Builder _aclb;

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _c1 = cb.setHostname("c1").build();
    _c2 = cb.setHostname("c2").build();
    _aclb = nf.aclBuilder().setOwner(_c1);
    _c1.setIpSpaces(ImmutableSortedMap.of("ipSpace", new Ip("1.2.3.4").toIpSpace()));
    _c1.setInterfaces(
        ImmutableSortedMap.of(
            "iface",
            Interface.builder().setName("iface").build(),
            "iface2",
            Interface.builder().setName("iface2").build()));
    _c2.setInterfaces(ImmutableSortedMap.of("iface", Interface.builder().setName("iface").build()));
  }

  @Test
  public void testWithIcmpType() {
    // First line accepts IP 1.2.3.4
    // Second line accepts same but only ICMP of type 8
    List<IpAccessListLine> lines =
        ImmutableList.of(
            IpAccessListLine.acceptingHeaderSpace(
                HeaderSpace.builder().setSrcIps(new Ip("1.2.3.4").toIpSpace()).build()),
            IpAccessListLine.acceptingHeaderSpace(
                HeaderSpace.builder()
                    .setSrcIps(new Ip("1.2.3.4").toIpSpace())
                    .setIpProtocols(ImmutableSet.of(IpProtocol.ICMP))
                    .setIcmpTypes(ImmutableList.of(new SubRange(8)))
                    .build()));
    _aclb.setLines(lines).setName("acl").build();
    List<String> lineNames = lines.stream().map(Object::toString).collect(Collectors.toList());

    TableAnswerElement answer = answer(new AclReachabilityQuestion());

    // Construct the expected result. First line should block second.
    Multiset<Row> expected =
        ImmutableMultiset.of(
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": acl"))
                .put(COL_LINES, lineNames)
                .put(COL_BLOCKED_LINE_NUM, 1)
                .put(COL_BLOCKED_LINE_ACTION, LineAction.PERMIT)
                .put(COL_BLOCKING_LINE_NUMS, ImmutableSet.of(0))
                .put(COL_DIFF_ACTION, false)
                .put(COL_REASON, BLOCKING_LINES)
                .put(
                    COL_MESSAGE,
                    "ACLs { c1: acl } contain an unreachable line:\n  [index 1] "
                        + lineNames.get(1)
                        + "\nBlocking line(s):\n  [index 0] "
                        + lineNames.get(0))
                .build());
    assertThat(answer.getRows().getData(), equalTo(expected));
  }

  @Test
  public void testIpWildcards() {
    // First line accepts src IPs 1.2.3.4/30
    // Second line accepts src IPs 1.2.3.4/32
    List<IpAccessListLine> lines =
        ImmutableList.of(
            IpAccessListLine.acceptingHeaderSpace(
                HeaderSpace.builder()
                    .setSrcIps(new IpWildcard(new Prefix(new Ip("1.2.3.4"), 30)).toIpSpace())
                    .build()),
            IpAccessListLine.acceptingHeaderSpace(
                HeaderSpace.builder()
                    .setSrcIps(new IpWildcard(new Prefix(new Ip("1.2.3.4"), 32)).toIpSpace())
                    .build()),
            IpAccessListLine.acceptingHeaderSpace(
                HeaderSpace.builder()
                    .setSrcIps(new IpWildcard(new Prefix(new Ip("1.2.3.4"), 28)).toIpSpace())
                    .build()));
    _aclb.setLines(lines).setName("acl").build();
    List<String> lineNames = lines.stream().map(Object::toString).collect(Collectors.toList());

    TableAnswerElement answer = answer(new AclReachabilityQuestion());

    // Construct the expected result. First line should block second.
    Multiset<Row> expected =
        ImmutableMultiset.of(
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": acl"))
                .put(COL_LINES, lineNames)
                .put(COL_BLOCKED_LINE_NUM, 1)
                .put(COL_BLOCKED_LINE_ACTION, LineAction.PERMIT)
                .put(COL_BLOCKING_LINE_NUMS, ImmutableSet.of(0))
                .put(COL_DIFF_ACTION, false)
                .put(COL_REASON, BLOCKING_LINES)
                .put(
                    COL_MESSAGE,
                    "ACLs { c1: acl } contain an unreachable line:\n  [index 1] "
                        + lineNames.get(1)
                        + "\nBlocking line(s):\n  [index 0] "
                        + lineNames.get(0))
                .build());
    assertThat(answer.getRows().getData(), equalTo(expected));
  }

  @Test
  public void testCycleAppearsOnce() {
    // acl1 permits anything acl2 permits... twice
    // acl2 permits anything acl1 permits... twice
    _aclb
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting().setMatchCondition(new PermittedByAcl("acl2")).build(),
                IpAccessListLine.accepting().setMatchCondition(new PermittedByAcl("acl2")).build()))
        .setName("acl1")
        .build();
    _aclb
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting().setMatchCondition(new PermittedByAcl("acl1")).build(),
                IpAccessListLine.accepting().setMatchCondition(new PermittedByAcl("acl1")).build()))
        .setName("acl2")
        .build();

    TableAnswerElement answer = answer(new AclReachabilityQuestion());

    // Construct the expected result. Should find only one cycle result.
    Multiset<Row> expected =
        ImmutableMultiset.of(
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": acl1, acl2"))
                .put(COL_LINES, null)
                .put(COL_BLOCKED_LINE_NUM, null)
                .put(COL_BLOCKED_LINE_ACTION, null)
                .put(COL_BLOCKING_LINE_NUMS, null)
                .put(COL_DIFF_ACTION, null)
                .put(COL_REASON, CYCLICAL_REFERENCE)
                .put(COL_MESSAGE, "Cyclic ACL references in node 'c1': acl1 -> acl2 -> acl1")
                .build());
    assertThat(answer.getRows().getData(), equalTo(expected));
  }

  @Test
  public void testCircularReferences() {
    // acl0 permits anything acl1 permits
    // acl1 permits anything acl2 permits, plus 1 other line to avoid acl3's line being unmatchable
    // acl2 permits anything acl0 permits
    // acl3 permits anything acl1 permits (not part of cycle)
    _aclb
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting().setMatchCondition(new PermittedByAcl("acl1")).build()))
        .setName("acl0")
        .build();
    _aclb
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting().setMatchCondition(new PermittedByAcl("acl2")).build(),
                acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setSrcIps(Prefix.parse("1.0.0.0/24").toIpSpace())
                        .build())))
        .setName("acl1")
        .build();
    _aclb
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting().setMatchCondition(new PermittedByAcl("acl0")).build()))
        .setName("acl2")
        .build();
    _aclb
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting().setMatchCondition(new PermittedByAcl("acl1")).build()))
        .setName("acl3")
        .build();

    TableAnswerElement answer = answer(new AclReachabilityQuestion());

    // Construct the expected result. Should find a single cycle result.
    Multiset<Row> expected =
        ImmutableMultiset.of(
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": acl0, acl1, acl2"))
                .put(COL_LINES, null)
                .put(COL_BLOCKED_LINE_NUM, null)
                .put(COL_BLOCKED_LINE_ACTION, null)
                .put(COL_BLOCKING_LINE_NUMS, null)
                .put(COL_DIFF_ACTION, null)
                .put(COL_REASON, CYCLICAL_REFERENCE)
                .put(
                    COL_MESSAGE, "Cyclic ACL references in node 'c1': acl0 -> acl1 -> acl2 -> acl0")
                .build());
    assertThat(answer.getRows().getData(), equalTo(expected));
  }

  @Test
  public void testUndefinedReference() {

    IpAccessListLine aclLine =
        IpAccessListLine.accepting().setMatchCondition(new PermittedByAcl("???")).build();
    _aclb.setLines(ImmutableList.of(aclLine)).setName("acl").build();

    TableAnswerElement answer = answer(new AclReachabilityQuestion());

    // Construct the expected result. Should find an undefined ACL result.
    Multiset<Row> expected =
        ImmutableMultiset.of(
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": acl"))
                .put(COL_LINES, ImmutableList.of(aclLine.toString()))
                .put(COL_BLOCKED_LINE_NUM, 0)
                .put(COL_BLOCKED_LINE_ACTION, PERMIT)
                .put(COL_BLOCKING_LINE_NUMS, ImmutableList.of())
                .put(COL_DIFF_ACTION, false)
                .put(COL_REASON, UNDEFINED_REFERENCE)
                .put(
                    COL_MESSAGE,
                    "ACLs { c1: acl } contain an unreachable line:\n  [index 0] IpAccessListLine{action=PERMIT,"
                        + " matchCondition=PermittedByAcl{aclName=???, defaultAccept=false}}"
                        + "\nThis line references a structure that is not defined.")
                .build());
    assertThat(answer.getRows().getData(), equalTo(expected));
  }

  @Test
  public void testIndirection() {
    /*
     Referenced ACL contains 1 line: Permit 1.0.0.0/24
     Main ACL contains 2 lines:
     0. Permit anything that referenced ACL permits
     1. Permit 1.0.0.0/24
    */
    List<IpAccessListLine> referencedAclLines =
        ImmutableList.of(
            acceptingHeaderSpace(
                HeaderSpace.builder().setSrcIps(Prefix.parse("1.0.0.0/24").toIpSpace()).build()));
    IpAccessList referencedAcl = _aclb.setLines(referencedAclLines).setName("acl1").build();

    List<IpAccessListLine> aclLines =
        ImmutableList.of(
            IpAccessListLine.accepting()
                .setMatchCondition(new PermittedByAcl(referencedAcl.getName()))
                .build(),
            acceptingHeaderSpace(
                HeaderSpace.builder().setSrcIps(Prefix.parse("1.0.0.0/24").toIpSpace()).build()));
    IpAccessList acl = _aclb.setLines(aclLines).setName("acl2").build();
    List<String> lineNames = aclLines.stream().map(Object::toString).collect(Collectors.toList());

    /*
     Runs two questions:
     1. General ACL reachability (referenced ACL won't be encoded after first NoD step)
     2. Reachability specifically for main ACL (referenced ACL won't be encoded at all)
     Will test that both give the same result.
    */
    TableAnswerElement generalAnswer = answer(new AclReachabilityQuestion());
    TableAnswerElement specificAnswer =
        answer(new AclReachabilityQuestion(null, acl.getName(), null, null));

    // Construct the expected result. Should find line 1 to be blocked by line 0 in main ACL.
    Multiset<Row> expected =
        ImmutableMultiset.of(
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": " + acl.getName()))
                .put(COL_LINES, lineNames)
                .put(COL_BLOCKED_LINE_NUM, 1)
                .put(COL_BLOCKED_LINE_ACTION, PERMIT)
                .put(COL_BLOCKING_LINE_NUMS, ImmutableList.of(0))
                .put(COL_DIFF_ACTION, false)
                .put(COL_REASON, BLOCKING_LINES)
                .put(
                    COL_MESSAGE,
                    "ACLs { c1: acl2 } contain an unreachable line:\n  [index 1] IpAccessListLine{action=PERMIT, "
                        + "matchCondition=MatchHeaderSpace{headerSpace=HeaderSpace{srcIps=PrefixIpSpace{prefix=1.0.0.0/24}}}}"
                        + "\nBlocking line(s):\n  [index 0] IpAccessListLine{action=PERMIT, "
                        + "matchCondition=PermittedByAcl{aclName=acl1, defaultAccept=false}}")
                .build());

    assertThat(generalAnswer.getRows().getData(), equalTo(expected));
    assertThat(specificAnswer.getRows().getData(), equalTo(expected));
  }

  @Test
  public void testMultipleCoveringLines() {
    List<IpAccessListLine> aclLines =
        ImmutableList.of(
            acceptingHeaderSpace(
                HeaderSpace.builder()
                    .setSrcIps(new IpWildcard("1.0.0.0:0.0.0.0").toIpSpace())
                    .build()),
            acceptingHeaderSpace(
                HeaderSpace.builder()
                    .setSrcIps(new IpWildcard("1.0.0.1:0.0.0.0").toIpSpace())
                    .build()),
            acceptingHeaderSpace(
                HeaderSpace.builder()
                    .setSrcIps(new IpWildcard("1.0.0.0:0.0.0.1").toIpSpace())
                    .build()));
    IpAccessList acl = _aclb.setLines(aclLines).setName("acl").build();
    List<String> lineNames = aclLines.stream().map(Object::toString).collect(Collectors.toList());

    TableAnswerElement answer = answer(new AclReachabilityQuestion());

    /*
     Construct the expected result. Line 2 should be blocked by both previous lines.
    */
    Multiset<Row> expected =
        ImmutableMultiset.of(
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": " + acl.getName()))
                .put(COL_LINES, lineNames)
                .put(COL_BLOCKED_LINE_NUM, 2)
                .put(COL_BLOCKED_LINE_ACTION, PERMIT)
                .put(COL_BLOCKING_LINE_NUMS, ImmutableList.of(0, 1))
                .put(COL_DIFF_ACTION, false)
                .put(COL_REASON, BLOCKING_LINES)
                .put(
                    COL_MESSAGE,
                    "ACLs { c1: acl } contain an unreachable line:\n  [index 2] IpAccessListLine{action=PERMIT, "
                        + "matchCondition=MatchHeaderSpace{headerSpace=HeaderSpace{srcIps=IpWildcardIpSpace{ipWildcard=1.0.0.0/31}}}}"
                        + "\nBlocking line(s):\n  [index 0] "
                        + lineNames.get(0)
                        + "\n  [index 1] "
                        + lineNames.get(1))
                .build());

    assertThat(answer.getRows().getData(), equalTo(expected));
  }

  @Test
  public void testIndependentlyUnmatchableLines() {
    /*
    Construct ACL with lines:
    0. Reject 1.0.0.0/24 (unblocked)
    1. Accept 1.0.0.0/24 (blocked by line 0)
    2. Accept [empty set] (unmatchable)
    3. Accept 1.0.0.0/32 (blocked by line 0)
    4. Accept 1.2.3.4/32 (unblocked)
     */
    List<IpAccessListLine> aclLines =
        ImmutableList.of(
            rejectingHeaderSpace(
                HeaderSpace.builder().setSrcIps(Prefix.parse("1.0.0.0/24").toIpSpace()).build()),
            acceptingHeaderSpace(
                HeaderSpace.builder().setSrcIps(Prefix.parse("1.0.0.0/24").toIpSpace()).build()),
            IpAccessListLine.accepting().setMatchCondition(FalseExpr.INSTANCE).build(),
            acceptingHeaderSpace(
                HeaderSpace.builder().setSrcIps(Prefix.parse("1.0.0.0/32").toIpSpace()).build()),
            acceptingHeaderSpace(
                HeaderSpace.builder().setSrcIps(Prefix.parse("1.2.3.4/32").toIpSpace()).build()));
    IpAccessList acl = _aclb.setLines(aclLines).setName("acl").build();
    List<String> lineNames = aclLines.stream().map(Object::toString).collect(Collectors.toList());

    TableAnswerElement answer = answer(new AclReachabilityQuestion());

    // Construct the expected result
    Multiset<Row> expected =
        ImmutableMultiset.of(
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": " + acl.getName()))
                .put(COL_LINES, lineNames)
                .put(COL_BLOCKED_LINE_NUM, 1)
                .put(COL_BLOCKED_LINE_ACTION, PERMIT)
                .put(COL_BLOCKING_LINE_NUMS, ImmutableList.of(0))
                .put(COL_DIFF_ACTION, true)
                .put(COL_REASON, BLOCKING_LINES)
                .put(
                    COL_MESSAGE,
                    "ACLs { c1: acl } contain an unreachable line:\n  [index 1] IpAccessListLine{action=PERMIT, "
                        + "matchCondition=MatchHeaderSpace{headerSpace=HeaderSpace{srcIps=PrefixIpSpace{prefix=1.0.0.0/24}}}}"
                        + "\nBlocking line(s):\n  [index 0] IpAccessListLine{action=DENY, "
                        + "matchCondition=MatchHeaderSpace{headerSpace=HeaderSpace{srcIps=PrefixIpSpace{prefix=1.0.0.0/24}}}}")
                .build(),
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": " + acl.getName()))
                .put(COL_LINES, lineNames)
                .put(COL_BLOCKED_LINE_NUM, 2)
                .put(COL_BLOCKED_LINE_ACTION, PERMIT)
                .put(COL_BLOCKING_LINE_NUMS, ImmutableList.of())
                .put(COL_DIFF_ACTION, false)
                .put(COL_REASON, UNMATCHABLE)
                .put(
                    COL_MESSAGE,
                    "ACLs { c1: acl } contain an unreachable line:\n"
                        + "  [index 2] IpAccessListLine{action=PERMIT, matchCondition=FalseExpr{}}\n"
                        + "This line will never match any packet, independent of preceding lines.")
                .build(),
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": " + acl.getName()))
                .put(COL_LINES, lineNames)
                .put(COL_BLOCKED_LINE_NUM, 3)
                .put(COL_BLOCKED_LINE_ACTION, PERMIT)
                .put(COL_BLOCKING_LINE_NUMS, ImmutableList.of(0))
                .put(COL_DIFF_ACTION, true)
                .put(COL_REASON, BLOCKING_LINES)
                .put(
                    COL_MESSAGE,
                    "ACLs { c1: acl } contain an unreachable line:\n  [index 3] IpAccessListLine{action=PERMIT, "
                        + "matchCondition=MatchHeaderSpace{headerSpace=HeaderSpace{srcIps=PrefixIpSpace{prefix=1.0.0.0/32}}}}"
                        + "\nBlocking line(s):\n  [index 0] IpAccessListLine{action=DENY, "
                        + "matchCondition=MatchHeaderSpace{headerSpace=HeaderSpace{srcIps=PrefixIpSpace{prefix=1.0.0.0/24}}}}")
                .build());

    assertThat(answer.getRows().getData(), equalTo(expected));
  }

  @Test
  public void testOriginalAclNotMutated() {
    // ACL that references an undefined ACL and an IpSpace; check line unchanged in original version
    IpAccessList acl =
        _aclb
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.accepting()
                        .setMatchCondition(new PermittedByAcl("???"))
                        .build(),
                    IpAccessListLine.rejecting()
                        .setMatchCondition(
                            new MatchHeaderSpace(
                                HeaderSpace.builder()
                                    .setSrcIps(new IpSpaceReference("ipSpace"))
                                    .build()))
                        .build()))
            .setName("acl")
            .build();

    answer(new AclReachabilityQuestion());

    // ACL's lines should be the same as before
    assertThat(
        acl.getLines(),
        equalTo(
            ImmutableList.of(
                IpAccessListLine.accepting().setMatchCondition(new PermittedByAcl("???")).build(),
                IpAccessListLine.rejecting()
                    .setMatchCondition(
                        new MatchHeaderSpace(
                            HeaderSpace.builder()
                                .setSrcIps(new IpSpaceReference("ipSpace"))
                                .build()))
                    .build())));

    // Config's ACL should be the same as the original version
    assertThat(_c1.getIpAccessLists().get(acl.getName()), equalTo(acl));
  }

  @Test
  public void testWithSrcInterfaceReference() {
    List<IpAccessListLine> aclLines =
        ImmutableList.of(
            IpAccessListLine.accepting()
                .setMatchCondition(new MatchSrcInterface(ImmutableList.of("iface", "iface2")))
                .build(),
            IpAccessListLine.accepting()
                .setMatchCondition(new MatchSrcInterface(ImmutableList.of("iface")))
                .build());
    IpAccessList acl = _aclb.setLines(aclLines).setName("acl").build();
    List<String> lineNames = aclLines.stream().map(Object::toString).collect(Collectors.toList());

    TableAnswerElement answer = answer(new AclReachabilityQuestion());

    /* Construct the expected result. Line 1 should be blocked by line 0. */
    Multiset<Row> expected =
        ImmutableMultiset.of(
            Row.builder(COLUMN_METADATA)
                .put(COL_SOURCES, ImmutableList.of(_c1.getHostname() + ": " + acl.getName()))
                .put(COL_LINES, lineNames)
                .put(COL_BLOCKED_LINE_NUM, 1)
                .put(COL_BLOCKED_LINE_ACTION, PERMIT)
                .put(COL_BLOCKING_LINE_NUMS, ImmutableList.of(0))
                .put(COL_DIFF_ACTION, false)
                .put(COL_REASON, BLOCKING_LINES)
                .put(
                    COL_MESSAGE,
                    "ACLs { c1: acl } contain an unreachable line:\n  [index 1] IpAccessListLine{action=PERMIT,"
                        + " matchCondition=MatchSrcInterface{srcInterfaces=[iface]}}\n"
                        + "Blocking line(s):\n  [index 0] IpAccessListLine{action=PERMIT,"
                        + " matchCondition=MatchSrcInterface{srcInterfaces=[iface, iface2]}}")
                .build());

    assertThat(answer.getRows().getData(), equalTo(expected));
  }

  private TableAnswerElement answer(AclReachabilityQuestion q) {
    IBatfish batfish =
        new IBatfishTestAdapter() {
          @Override
          public SortedMap<String, Configuration> loadConfigurations() {
            return ImmutableSortedMap.of(_c1.getHostname(), _c1, _c2.getHostname(), _c2);
          }
        };
    AclReachabilityAnswerer answerer = new AclReachabilityAnswerer(q, batfish);
    return answerer.answer();
  }
}
