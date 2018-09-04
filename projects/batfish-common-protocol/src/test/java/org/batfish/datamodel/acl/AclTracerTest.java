package org.batfish.datamodel.acl;

import static org.batfish.datamodel.matchers.DataModelMatchers.hasEvents;
import static org.batfish.datamodel.matchers.DataModelMatchers.isDefaultDeniedByIpAccessListNamed;
import static org.batfish.datamodel.matchers.DataModelMatchers.isDeniedByIpAccessListLineThat;
import static org.batfish.datamodel.matchers.DataModelMatchers.isPermittedByAclIpSpaceLineThat;
import static org.batfish.datamodel.matchers.DataModelMatchers.isPermittedByIpAccessListLineThat;
import static org.batfish.datamodel.matchers.DataModelMatchers.isPermittedByNamedIpSpace;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AclIpSpaceLine;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceMetadata;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.matchers.DeniedByIpAccessListLineMatchers;
import org.batfish.datamodel.matchers.PermittedByAclIpSpaceLineMatchers;
import org.batfish.datamodel.matchers.PermittedByIpAccessListLineMatchers;
import org.junit.Test;

public class AclTracerTest {

  private static final String ACL_IP_SPACE_NAME = "aclIpSpace";

  private static final String ACL_NAME = "acl";

  private static final Flow FLOW =
      Flow.builder().setTag("tag").setDstIp(Ip.ZERO).setIngressNode("node1").build();

  private static final String SRC_INTERFACE = null;

  private static final String TEST_ACL = "test acl";

  @Test
  public void testDefaultDeniedByIpAccessList() {
    IpAccessList acl = IpAccessList.builder().setName(ACL_NAME).build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    /* The ACL has no lines, so the only event should be a default deny */
    assertThat(trace, hasEvents(contains(isDefaultDeniedByIpAccessListNamed(ACL_NAME))));
  }

  @Test
  public void testDefaultDeniedByNamedAclIpSpace() {
    AclIpSpace aclIpSpace = AclIpSpace.DENY_ALL;
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setDstIps(new IpSpaceReference(ACL_IP_SPACE_NAME))
                            .build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of(ACL_IP_SPACE_NAME, aclIpSpace);
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata =
        ImmutableMap.of(ACL_IP_SPACE_NAME, new IpSpaceMetadata(ACL_IP_SPACE_NAME, TEST_ACL));
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        trace, hasEvents(contains(ImmutableList.of(isDefaultDeniedByIpAccessListNamed(ACL_NAME)))));
  }

  @Test
  public void testDeniedByIndirectPermit() {
    String aclIndirectName = "aclIndirect";
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.rejecting()
                        .setMatchCondition(new PermittedByAcl(aclIndirectName))
                        .build()))
            .build();
    IpAccessList aclIndirect =
        IpAccessList.builder()
            .setName(aclIndirectName)
            .setLines(ImmutableList.of(IpAccessListLine.ACCEPT_ALL))
            .build();
    Map<String, IpAccessList> availableAcls =
        ImmutableMap.of(ACL_NAME, acl, aclIndirectName, aclIndirect);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        trace,
        hasEvents(
            contains(
                ImmutableList.of(
                    isDeniedByIpAccessListLineThat(
                        allOf(
                            DeniedByIpAccessListLineMatchers.hasName(ACL_NAME),
                            DeniedByIpAccessListLineMatchers.hasIndex(0))),
                    isPermittedByIpAccessListLineThat(
                        allOf(
                            PermittedByIpAccessListLineMatchers.hasName(aclIndirectName),
                            PermittedByIpAccessListLineMatchers.hasIndex(0)))))));
  }

  @Test
  public void testDeniedByIpAccessListLine() {
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(ImmutableList.of(IpAccessListLine.REJECT_ALL))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        trace,
        hasEvents(
            contains(
                isDeniedByIpAccessListLineThat(
                    allOf(
                        DeniedByIpAccessListLineMatchers.hasName(ACL_NAME),
                        DeniedByIpAccessListLineMatchers.hasIndex(0))))));
  }

  @Test
  public void testDeniedByNamedAclIpSpaceLine() {
    AclIpSpace aclIpSpace =
        AclIpSpace.builder().setLines(ImmutableList.of(AclIpSpaceLine.DENY_ALL)).build();
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setDstIps(new IpSpaceReference(ACL_IP_SPACE_NAME))
                            .build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of(ACL_IP_SPACE_NAME, aclIpSpace);
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata =
        ImmutableMap.of(ACL_IP_SPACE_NAME, new IpSpaceMetadata(ACL_IP_SPACE_NAME, TEST_ACL));
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        trace, hasEvents(contains(ImmutableList.of(isDefaultDeniedByIpAccessListNamed(ACL_NAME)))));
  }

  @Test
  public void testDeniedByNamedSimpleIpSpace() {
    String ipSpaceName = "aclIpSpace";

    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setDstIps(new IpSpaceReference(ipSpaceName))
                            .build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of(ipSpaceName, Ip.MAX.toIpSpace());
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata =
        ImmutableMap.of(ipSpaceName, new IpSpaceMetadata(ipSpaceName, TEST_ACL));

    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        trace, hasEvents(contains(ImmutableList.of(isDefaultDeniedByIpAccessListNamed(ACL_NAME)))));
  }

  @Test
  public void testDeniedByUnnamedAclIpSpace() {
    AclIpSpace aclIpSpace = AclIpSpace.DENY_ALL;
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.acceptingHeaderSpace(
                        HeaderSpace.builder().setDstIps(aclIpSpace).build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(trace, hasEvents(contains(isDefaultDeniedByIpAccessListNamed(ACL_NAME))));
  }

  @Test
  public void testDeniedByUnnamedSimpleIpSpace() {
    IpSpace ipSpace = EmptyIpSpace.INSTANCE;
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.acceptingHeaderSpace(
                        HeaderSpace.builder().setDstIps(ipSpace).build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(trace, hasEvents(contains(isDefaultDeniedByIpAccessListNamed(ACL_NAME))));
  }

  @Test
  public void testPermittedByIpAccessListLine() {
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(ImmutableList.of(IpAccessListLine.ACCEPT_ALL))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        trace,
        hasEvents(
            contains(
                isPermittedByIpAccessListLineThat(
                    allOf(
                        PermittedByIpAccessListLineMatchers.hasName(ACL_NAME),
                        PermittedByIpAccessListLineMatchers.hasIndex(0))))));
  }

  @Test
  public void testPermittedByNamedAclIpSpaceLine() {
    AclIpSpace aclIpSpace = AclIpSpace.PERMIT_ALL;
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setDstIps(new IpSpaceReference(ACL_IP_SPACE_NAME))
                            .build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of(ACL_IP_SPACE_NAME, aclIpSpace);
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata =
        ImmutableMap.of(ACL_IP_SPACE_NAME, new IpSpaceMetadata(ACL_IP_SPACE_NAME, TEST_ACL));
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        trace,
        hasEvents(
            contains(
                ImmutableList.of(
                    isPermittedByIpAccessListLineThat(
                        allOf(
                            PermittedByIpAccessListLineMatchers.hasName(ACL_NAME),
                            PermittedByIpAccessListLineMatchers.hasIndex(0))),
                    isPermittedByAclIpSpaceLineThat(
                        allOf(
                            PermittedByAclIpSpaceLineMatchers.hasName(ACL_IP_SPACE_NAME),
                            PermittedByAclIpSpaceLineMatchers.hasIndex(0)))))));
  }

  @Test
  public void testPermittedByNamedSimpleIpSpace() {
    String ipSpaceName = "aclIpSpace";
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setDstIps(new IpSpaceReference(ipSpaceName))
                            .build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of(ipSpaceName, Ip.ZERO.toIpSpace());
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata =
        ImmutableMap.of(ipSpaceName, new IpSpaceMetadata(ipSpaceName, TEST_ACL));
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        trace,
        hasEvents(
            contains(
                ImmutableList.of(
                    isPermittedByIpAccessListLineThat(
                        allOf(
                            PermittedByIpAccessListLineMatchers.hasName(ACL_NAME),
                            PermittedByIpAccessListLineMatchers.hasIndex(0))),
                    isPermittedByNamedIpSpace(ipSpaceName)))));
  }

  @Test
  public void testPermittedByUnnamedAclIpSpace() {
    AclIpSpace aclIpSpace = AclIpSpace.PERMIT_ALL;
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.acceptingHeaderSpace(
                        HeaderSpace.builder().setDstIps(aclIpSpace).build())))
            .build();
    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        trace,
        hasEvents(
            contains(
                ImmutableList.of(
                    isPermittedByIpAccessListLineThat(
                        allOf(
                            PermittedByIpAccessListLineMatchers.hasName(ACL_NAME),
                            PermittedByIpAccessListLineMatchers.hasIndex(0)))))));
  }

  @Test
  public void testPermittedByUnnamedSimpleIpSpace() {
    IpSpace ipSpace = UniverseIpSpace.INSTANCE;
    IpAccessList acl =
        IpAccessList.builder()
            .setName(ACL_NAME)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.acceptingHeaderSpace(
                        HeaderSpace.builder().setDstIps(ipSpace).build())))
            .build();

    Map<String, IpAccessList> availableAcls = ImmutableMap.of(ACL_NAME, acl);
    Map<String, IpSpace> namedIpSpaces = ImmutableMap.of();
    Map<String, IpSpaceMetadata> namedIpSpaceMetadata = ImmutableMap.of();
    AclTrace trace =
        AclTracer.trace(
            acl, FLOW, SRC_INTERFACE, availableAcls, namedIpSpaces, namedIpSpaceMetadata);

    assertThat(
        trace,
        hasEvents(
            contains(
                ImmutableList.of(
                    isPermittedByIpAccessListLineThat(
                        allOf(
                            PermittedByIpAccessListLineMatchers.hasName(ACL_NAME),
                            PermittedByIpAccessListLineMatchers.hasIndex(0)))))));
  }
}
