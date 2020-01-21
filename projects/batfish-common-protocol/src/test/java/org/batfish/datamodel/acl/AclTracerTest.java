package org.batfish.datamodel.acl;

import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.batfish.datamodel.acl.AclTracer.DEST_IP_DESCRIPTION;
import static org.batfish.datamodel.acl.TraceElements.matchedByAclLine;
import static org.batfish.datamodel.acl.TraceElements.permittedByNamedIpSpace;
import static org.batfish.datamodel.acl.TraceTreeMatchers.hasChildren;
import static org.batfish.datamodel.acl.TraceTreeMatchers.hasNoChildren;
import static org.batfish.datamodel.acl.TraceTreeMatchers.hasTraceElement;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasEvents;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.AclAclLine;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceMetadata;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.TraceElement;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.trace.TraceTree;
import org.junit.Test;

public class AclTracerTest {

  private static final String ACL_IP_SPACE_NAME = "aclIpSpace";

  private static final String ACL_NAME = "acl";

  private static final Flow FLOW = Flow.builder().setDstIp(Ip.ZERO).setIngressNode("node1").build();

  private static final String SRC_INTERFACE = null;

  private static final String TEST_ACL = "test acl";

  private static final HeaderSpace TRUE_HEADERSPACE = HeaderSpace.builder().build();
  private static final HeaderSpace FALSE_HEADERSPACE =
      HeaderSpace.builder().setNotDstIps(UniverseIpSpace.INSTANCE).build();

  private static List<TraceTree> trace(IpAccessList acl) {
    return AclTracer.trace(
        acl, FLOW, SRC_INTERFACE, ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of());
  }

  private static List<TraceTree> trace(AclLineMatchExpr expr) {
    return AclTracer.trace(
        expr, FLOW, SRC_INTERFACE, ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of());
  }

  private static AclLineMatchExpr trueExpr(String traceElement) {
    return new MatchHeaderSpace(TRUE_HEADERSPACE, traceElement);
  }

  private static AclLineMatchExpr falseExpr(String traceElement) {
    return new MatchHeaderSpace(FALSE_HEADERSPACE, traceElement);
  }

  @Test
  public void testDefaultDeniedByIpAccessList() {
    IpAccessList acl = IpAccessList.builder().setName(ACL_NAME).build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(root, empty());
  }

  @Test
  public void testDefaultDeniedByNamedAclIpSpace() {
    IpSpace aclIpSpace =
        AclIpSpace.permitting(Ip.parse("255.255.255.255").toIpSpace())
            .thenPermitting(Ip.parse("255.255.255.254").toIpSpace())
            .build();
    assertThat(aclIpSpace, instanceOf(AclIpSpace.class));
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setDstIps(new IpSpaceReference(ACL_IP_SPACE_NAME))
                            .build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of(ACL_IP_SPACE_NAME, aclIpSpace);
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata =
        ImmutableMap.of(ACL_IP_SPACE_NAME, new IpSpaceMetadata(ACL_IP_SPACE_NAME, TEST_ACL));
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(root, empty());
  }

  @Test
  public void testDeniedByIndirectPermit() {
    String aclIndirectName = "aclIndirect";
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.rejecting()
                        .setMatchCondition(new PermittedByAcl(aclIndirectName))
                        .build()))
            .build();
    IpAccessList aclIndirect =
        IpAccessList.builder()
            .setName(aclIndirectName)
            .setLines(ImmutableList.of(ExprAclLine.ACCEPT_ALL))
            .build();
    Map<String, IpAccessList> availableAcls =
        ImmutableMap.of(ACL_NAME, acl, aclIndirectName, aclIndirect);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        root,
        contains(
            allOf(
                hasTraceElement(matchedByAclLine(acl, 0)),
                hasChildren(
                    contains(
                        allOf(
                            hasTraceElement(matchedByAclLine(aclIndirect, 0)),
                            hasNoChildren()))))));

    AclTrace trace = new AclTrace(root);
    assertThat(
        trace,
        hasEvents(
            contains(
                TraceEvent.of(matchedByAclLine(acl, 0)),
                TraceEvent.of(matchedByAclLine(aclIndirect, 0)))));
  }

  @Test
  public void testDeniedByAclLine() {
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(ImmutableList.of(ExprAclLine.REJECT_ALL))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(root, contains(allOf(hasTraceElement(matchedByAclLine(acl, 0)), hasNoChildren())));

    AclTrace trace = new AclTrace(root);
    assertThat(trace, hasEvents(contains(TraceEvent.of(matchedByAclLine(acl, 0)))));
  }

  @Test
  public void testDeniedByNamedAclIpSpaceLine() {
    IpSpace aclIpSpace =
        AclIpSpace.permitting(Ip.parse("255.255.255.255").toIpSpace())
            .thenPermitting(Ip.parse("255.255.255.254").toIpSpace())
            .build();
    assertThat(aclIpSpace, instanceOf(AclIpSpace.class));

    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setDstIps(new IpSpaceReference(ACL_IP_SPACE_NAME))
                            .build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of(ACL_IP_SPACE_NAME, aclIpSpace);
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata =
        ImmutableMap.of(ACL_IP_SPACE_NAME, new IpSpaceMetadata(ACL_IP_SPACE_NAME, TEST_ACL));
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(root, empty());
  }

  @Test
  public void testDeniedByNamedSimpleIpSpace() {
    String ipSpaceName = "aclIpSpace";

    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setDstIps(new IpSpaceReference(ipSpaceName))
                            .build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of(ipSpaceName, Ip.MAX.toIpSpace());
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata =
        ImmutableMap.of(ipSpaceName, new IpSpaceMetadata(ipSpaceName, TEST_ACL));

    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(root, empty());
  }

  @Test
  public void testDeniedByUnnamedAclIpSpace() {
    IpSpace aclIpSpace =
        AclIpSpace.permitting(Ip.parse("255.255.255.255").toIpSpace())
            .thenPermitting(Ip.parse("255.255.255.254").toIpSpace())
            .build();
    assertThat(aclIpSpace, instanceOf(AclIpSpace.class));

    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.acceptingHeaderSpace(
                        HeaderSpace.builder().setDstIps(aclIpSpace).build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(root, empty());
  }

  @Test
  public void testDeniedByUnnamedSimpleIpSpace() {
    IpSpace ipSpace = EmptyIpSpace.INSTANCE;
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.acceptingHeaderSpace(
                        HeaderSpace.builder().setDstIps(ipSpace).build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(root, empty());
  }

  @Test
  public void testPermittedByAclLine() {
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(ImmutableList.of(ExprAclLine.ACCEPT_ALL))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(root, contains(allOf(hasTraceElement(matchedByAclLine(acl, 0)), hasNoChildren())));

    AclTrace trace = new AclTrace(root);
    assertThat(trace, hasEvents(contains(TraceEvent.of(matchedByAclLine(acl, 0)))));
  }

  @Test
  public void testPermittedByNamedSimpleIpSpace() {
    String ipSpaceName = "aclIpSpace";
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setDstIps(new IpSpaceReference(ipSpaceName))
                            .build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of(ipSpaceName, Ip.ZERO.toIpSpace());
    IpSpaceMetadata ipSpaceMetadata = new IpSpaceMetadata(ipSpaceName, TEST_ACL);
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata =
        ImmutableMap.of(ipSpaceName, ipSpaceMetadata);
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        root,
        contains(
            allOf(
                hasTraceElement(matchedByAclLine(acl, 0)),
                hasChildren(
                    contains(
                        allOf(
                            hasTraceElement(
                                permittedByNamedIpSpace(
                                    FLOW.getDstIp(),
                                    DEST_IP_DESCRIPTION,
                                    ipSpaceMetadata,
                                    ipSpaceName)),
                            hasNoChildren()))))));

    AclTrace trace = new AclTrace(root);
    assertThat(
        trace,
        hasEvents(
            contains(
                TraceEvent.of(matchedByAclLine(acl, 0)),
                TraceEvent.of(
                    permittedByNamedIpSpace(
                        FLOW.getDstIp(), DEST_IP_DESCRIPTION, ipSpaceMetadata, ipSpaceName)))));
  }

  @Test
  public void testPermittedByUnnamedAclIpSpace() {
    IpSpace aclIpSpace =
        AclIpSpace.permitting(Prefix.parse("0.0.0.0/1").toIpSpace())
            .thenPermitting(Prefix.parse("1.0.0.0/1").toIpSpace())
            .build();
    assertThat(aclIpSpace, instanceOf(AclIpSpace.class));

    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.acceptingHeaderSpace(
                        HeaderSpace.builder().setDstIps(aclIpSpace).build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(root, contains(allOf(hasTraceElement(matchedByAclLine(acl, 0)), hasNoChildren())));

    AclTrace trace = new AclTrace(root);
    assertThat(trace, hasEvents(contains(TraceEvent.of(matchedByAclLine(acl, 0)))));
  }

  @Test
  public void testPermittedByUnnamedSimpleIpSpace() {
    IpSpace ipSpace = UniverseIpSpace.INSTANCE;
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.acceptingHeaderSpace(
                        HeaderSpace.builder().setDstIps(ipSpace).build())))
            .build();

    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(root, contains(allOf(hasTraceElement(matchedByAclLine(acl, 0)), hasNoChildren())));

    AclTrace trace = new AclTrace(root);
    assertThat(trace, hasEvents(contains(TraceEvent.of(matchedByAclLine(acl, 0)))));
  }

  @Test
  public void testDeniedByIndirectAndExpr() {
    String aclIndirectName1 = "aclIndirect1";
    String aclIndirectName2 = "aclIndirect2";
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.accepting()
                        .setMatchCondition(
                            new AndMatchExpr(
                                ImmutableList.of(
                                    new PermittedByAcl(aclIndirectName1),
                                    new PermittedByAcl(aclIndirectName2))))
                        .build()))
            .build();
    IpAccessList aclIndirect1 =
        IpAccessList.builder()
            .setName(aclIndirectName1)
            .setLines(ImmutableList.of(ExprAclLine.ACCEPT_ALL))
            .build();
    IpAccessList aclIndirect2 =
        IpAccessList.builder()
            .setName(aclIndirectName2)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.acceptingHeaderSpace(
                        HeaderSpace.builder().setSrcIps(Ip.ZERO.toIpSpace()).build())))
            .build();
    Map<String, IpAccessList> availableAcls =
        ImmutableMap.of(
            ACL_NAME, acl, aclIndirectName1, aclIndirect1, aclIndirectName2, aclIndirect2);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    List<TraceTree> root =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        root,
        contains(
            allOf(
                hasTraceElement(matchedByAclLine(acl, 0)),
                hasChildren(
                    contains(
                        allOf(hasTraceElement(matchedByAclLine(aclIndirect1, 0)), hasNoChildren()),
                        allOf(
                            hasTraceElement(matchedByAclLine(aclIndirect2, 0)),
                            hasNoChildren()))))));

    AclTrace trace = new AclTrace(root);
    assertThat(
        trace,
        hasEvents(
            contains(
                TraceEvent.of(matchedByAclLine(acl, 0)),
                TraceEvent.of(matchedByAclLine(aclIndirect1, 0)),
                TraceEvent.of(matchedByAclLine(aclIndirect2, 0)))));
  }

  @Test
  public void testAclAclLine() {
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(ImmutableList.of(ExprAclLine.accepting().setMatchCondition(TRUE).build()))
            .build();

    IpAccessList testAcl =
        IpAccessList.builder()
            .setName(TEST_ACL)
            .setLines(ImmutableList.of(new AclAclLine(TEST_ACL, ACL_NAME)))
            .build();

    List<TraceTree> root =
        AclTracer.trace(
            testAcl,
            FLOW,
            SRC_INTERFACE,
            ImmutableMap.of(ACL_NAME, acl),
            ImmutableMap.of(),
            ImmutableMap.of());

    assertThat(
        root,
        contains(
            allOf(
                hasTraceElement(matchedByAclLine(testAcl, 0)),
                hasChildren(
                    contains(allOf(hasTraceElement(matchedByAclLine(acl, 0)), hasNoChildren()))))));
  }

  @Test
  public void testLinesWithTraceElement() {
    TraceElement aclTraceElement = TraceElement.of("acl trace element");
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    ExprAclLine.accepting()
                        .setTraceElement(aclTraceElement)
                        .setMatchCondition(TRUE)
                        .build()))
            .build();

    TraceElement testAclTraceElement = TraceElement.of("test acl trace element");
    IpAccessList testAcl =
        IpAccessList.builder()
            .setName(TEST_ACL)
            .setLines(ImmutableList.of(new AclAclLine(TEST_ACL, ACL_NAME, testAclTraceElement)))
            .build();

    List<TraceTree> root =
        AclTracer.trace(
            testAcl,
            FLOW,
            SRC_INTERFACE,
            ImmutableMap.of(ACL_NAME, acl),
            ImmutableMap.of(),
            ImmutableMap.of());

    assertThat(
        root,
        contains(
            allOf(
                hasTraceElement(testAclTraceElement),
                hasChildren(contains(allOf(hasTraceElement(aclTraceElement), hasNoChildren()))))));
  }

  @Test
  public void testAnd_False() {
    List<TraceTree> trace = trace(and(trueExpr("a"), falseExpr("b")));
    assertThat(trace, empty());
  }

  @Test
  public void testAnd_withoutTraceElement() {
    List<TraceTree> trace = trace(and(trueExpr("a"), trueExpr("b")));
    assertThat(
        trace,
        contains(
            allOf(hasTraceElement(TraceElement.of("a")), hasNoChildren()),
            allOf(hasTraceElement(TraceElement.of("b")), hasNoChildren())));
  }

  @Test
  public void testAnd_withTraceElement() {
    List<TraceTree> trace = trace(and("and", trueExpr("a"), trueExpr("b")));
    assertThat(
        trace,
        contains(
            allOf(
                hasTraceElement(TraceElement.of("and")),
                hasChildren(
                    contains(
                        allOf(hasTraceElement(TraceElement.of("a")), hasNoChildren()),
                        allOf(hasTraceElement(TraceElement.of("b")), hasNoChildren()))))));
  }

  @Test
  public void testOr_false() {
    List<TraceTree> trace = trace(or(falseExpr("a"), falseExpr("b"), falseExpr("c")));
    assertThat(trace, empty());
  }

  @Test
  public void testOr_withoutTraceElement() {
    List<TraceTree> trace = trace(or(falseExpr("a"), trueExpr("b"), falseExpr("c")));
    assertThat(trace, contains(allOf(hasTraceElement(TraceElement.of("b")), hasNoChildren())));
  }

  @Test
  public void testOr_withTraceElement() {
    List<TraceTree> trace = trace(or("or", falseExpr("a"), trueExpr("b"), falseExpr("c")));
    assertThat(
        trace,
        contains(
            allOf(
                hasTraceElement(TraceElement.of("or")),
                hasChildren(
                    contains(allOf(hasTraceElement(TraceElement.of("b")), hasNoChildren()))))));
  }

  @Test
  public void testMatchHeaderspace_withoutTraceElement() {
    List<TraceTree> trace = trace(new MatchHeaderSpace(TRUE_HEADERSPACE));
    assertThat(trace, empty());
  }

  @Test
  public void testMatchHeaderspace_withTraceElement() {
    TraceElement a = TraceElement.of("a");
    List<TraceTree> trace = trace(new MatchHeaderSpace(TRUE_HEADERSPACE, a));
    assertThat(trace, contains(allOf(hasTraceElement(a), hasNoChildren())));
  }

  @Test
  public void testPermittedByAcl() {
    TraceElement a = TraceElement.of("a");
    String aclName = "acl";
    IpAccessList acl =
        IpAccessList.builder().setName(aclName).setLines(ExprAclLine.ACCEPT_ALL).build();
    List<TraceTree> trace =
        AclTracer.trace(
            new PermittedByAcl(aclName, a),
            FLOW,
            SRC_INTERFACE,
            ImmutableMap.of(aclName, acl),
            ImmutableMap.of(),
            ImmutableMap.of());
    assertThat(
        trace,
        contains(
            allOf(
                hasTraceElement(a),
                hasChildren(
                    contains(allOf(hasTraceElement(matchedByAclLine(acl, 0)), hasNoChildren()))))));
  }

  @Test
  public void testLineWithExprAnnotations() {
    AclLineMatchExpr expr = trueExpr("a");
    IpAccessList acl =
        IpAccessList.builder().setName("acl").setLines(ExprAclLine.accepting(expr)).build();
    List<TraceTree> trace = trace(acl);
    assertThat(
        trace,
        contains(
            allOf(
                hasTraceElement(matchedByAclLine(acl, 0)),
                hasChildren(contains(allOf(hasTraceElement("a"), hasNoChildren()))))));
  }

  @Test
  public void testMatchSrcInterface_withoutTraceElement() {
    String iface = "iface";
    List<TraceTree> trace =
        AclTracer.trace(
            matchSrcInterface(iface),
            FLOW,
            iface,
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of());
    assertThat(trace, empty());
  }

  @Test
  public void testMatchSrcInterface_withTraceElement() {
    String iface = "iface";
    TraceElement a = TraceElement.of("a");
    List<TraceTree> trace =
        AclTracer.trace(
            matchSrcInterface(a, iface),
            FLOW,
            iface,
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of());
    assertThat(trace, contains(allOf(hasTraceElement(a), hasNoChildren())));
  }
}
