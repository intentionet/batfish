package org.batfish.datamodel.acl;

import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.acl.AclTracer.DEST_IP_DESCRIPTION;
import static org.batfish.datamodel.acl.TraceElements.deniedByAclLine;
import static org.batfish.datamodel.acl.TraceElements.permittedByAclLine;
import static org.batfish.datamodel.acl.TraceElements.permittedByNamedIpSpace;
import static org.batfish.datamodel.acl.TraceTreeMatchers.hasChildren;
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
                hasTraceElement(deniedByAclLine(acl, 0)),
                hasChildren(
                    contains(
                        allOf(
                            hasTraceElement(permittedByAclLine(aclIndirect, 0)),
                            hasChildren(empty())))))));

    AclTrace trace = new AclTrace(root);
    assertThat(
        trace,
        hasEvents(
            contains(
                TraceEvent.of(deniedByAclLine(acl, 0)),
                TraceEvent.of(permittedByAclLine(aclIndirect, 0)))));
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

    assertThat(
        root, contains(allOf(hasTraceElement(deniedByAclLine(acl, 0)), hasChildren(empty()))));

    AclTrace trace = new AclTrace(root);
    assertThat(trace, hasEvents(contains(TraceEvent.of(deniedByAclLine(acl, 0)))));
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

    assertThat(
        root, contains(allOf(hasTraceElement(permittedByAclLine(acl, 0)), hasChildren(empty()))));

    AclTrace trace = new AclTrace(root);
    assertThat(trace, hasEvents(contains(TraceEvent.of(permittedByAclLine(acl, 0)))));
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
                hasTraceElement(permittedByAclLine(acl, 0)),
                hasChildren(
                    contains(
                        allOf(
                            hasTraceElement(
                                permittedByNamedIpSpace(
                                    FLOW.getDstIp(),
                                    DEST_IP_DESCRIPTION,
                                    ipSpaceMetadata,
                                    ipSpaceName)),
                            hasChildren(empty())))))));

    AclTrace trace = new AclTrace(root);
    assertThat(
        trace,
        hasEvents(
            contains(
                TraceEvent.of(permittedByAclLine(acl, 0)),
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

    assertThat(
        root, contains(allOf(hasTraceElement(permittedByAclLine(acl, 0)), hasChildren(empty()))));

    AclTrace trace = new AclTrace(root);
    assertThat(trace, hasEvents(contains(TraceEvent.of(permittedByAclLine(acl, 0)))));
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

    assertThat(
        root, contains(allOf(hasTraceElement(permittedByAclLine(acl, 0)), hasChildren(empty()))));

    AclTrace trace = new AclTrace(root);
    assertThat(trace, hasEvents(contains(TraceEvent.of(permittedByAclLine(acl, 0)))));
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
                hasTraceElement(permittedByAclLine(acl, 0)),
                hasChildren(
                    contains(
                        allOf(
                            hasTraceElement(permittedByAclLine(aclIndirect1, 0)),
                            hasChildren(empty())),
                        allOf(
                            hasTraceElement(permittedByAclLine(aclIndirect2, 0)),
                            hasChildren(empty())))))));

    AclTrace trace = new AclTrace(root);
    assertThat(
        trace,
        hasEvents(
            contains(
                TraceEvent.of(permittedByAclLine(acl, 0)),
                TraceEvent.of(permittedByAclLine(aclIndirect1, 0)),
                TraceEvent.of(permittedByAclLine(aclIndirect2, 0)))));
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
                hasTraceElement(permittedByAclLine(testAcl, 0)),
                hasChildren(
                    contains(
                        allOf(
                            hasTraceElement(permittedByAclLine(acl, 0)), hasChildren(empty())))))));
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
                hasChildren(
                    contains(allOf(hasTraceElement(aclTraceElement), hasChildren(empty())))))));
  }
}
