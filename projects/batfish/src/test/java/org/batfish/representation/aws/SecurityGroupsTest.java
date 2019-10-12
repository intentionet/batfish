package org.batfish.representation.aws;

import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_SECURITY_GROUPS;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link SecurityGroup}. */
public class SecurityGroupsTest {

  private List<SecurityGroup> _securityGroups;
  private IpAccessListLine _allowAllReverseOutboundRule;
  private Flow.Builder _flowBuilder;
  private Region _region;

  public static String TEST_ACL = "test_acl";

  @Before
  public void setup() throws IOException {
    JsonNode json =
        BatfishObjectMapper.mapper()
            .readTree(
                CommonUtil.readResource("org/batfish/representation/aws/SecurityGroupTest.json"));
    _securityGroups =
        BatfishObjectMapper.mapper()
            .convertValue(
                json.get(JSON_KEY_SECURITY_GROUPS), new TypeReference<List<SecurityGroup>>() {});

    _allowAllReverseOutboundRule =
        IpAccessListLine.acceptingHeaderSpace(
            HeaderSpace.builder()
                .setSrcIps(Sets.newHashSet(IpWildcard.parse("0.0.0.0/0")))
                .setTcpFlags(ImmutableSet.of(TcpFlagsMatchConditions.ACK_TCP_FLAG))
                .build());
    _region = new Region("test");
    _flowBuilder =
        Flow.builder().setIngressNode("foo").setTag("TEST").setIpProtocol(IpProtocol.TCP);
  }

  @Test
  public void testDeser() throws IOException {
    String text =
        CommonUtil.readResource("org/batfish/representation/aws/SecurityGroupTestDeser.json");

    JsonNode json = BatfishObjectMapper.mapper().readTree(text);
    ArrayNode array = (ArrayNode) json.get(JSON_KEY_SECURITY_GROUPS);
    List<SecurityGroup> securityGroups = new LinkedList<>();

    for (int index = 0; index < array.size(); index++) {
      securityGroups.add(
          BatfishObjectMapper.mapper().convertValue(array.get(index), SecurityGroup.class));
    }

    assertThat(
        securityGroups,
        Matchers.equalTo(
            ImmutableList.of(
                new SecurityGroup(
                    "sg-01a88a2ecd621d9ba",
                    "Single port ",
                    ImmutableList.of(
                        new IpPermissions(
                            "-1",
                            0,
                            65535,
                            ImmutableList.of(Prefix.parse("0.0.0.0/0")),
                            ImmutableList.of())),
                    ImmutableList.of(
                        new IpPermissions(
                            "tcp",
                            22,
                            22,
                            ImmutableList.of(Prefix.parse("1.2.3.4/32")),
                            ImmutableList.of()))))));
  }

  @Test
  public void testSinglePort() {
    SecurityGroup sg = _securityGroups.get(0);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    assertThat(
        inboundRules,
        equalTo(
            ImmutableList.of(
                _allowAllReverseOutboundRule,
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setSrcIps(Sets.newHashSet(IpWildcard.parse("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(SubRange.singleton(22)))
                        .build()))));
  }

  @Test
  public void testBeginningHalfOpenInterval() {
    SecurityGroup sg = _securityGroups.get(1);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    assertThat(
        inboundRules,
        equalTo(
            ImmutableList.of(
                _allowAllReverseOutboundRule,
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setSrcIps(Sets.newHashSet(IpWildcard.parse("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(0, 22)))
                        .build()))));
  }

  @Test
  public void testEndHalfOpenInterval() {
    SecurityGroup sg = _securityGroups.get(2);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    assertThat(
        inboundRules,
        equalTo(
            ImmutableList.of(
                _allowAllReverseOutboundRule,
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setSrcIps(Sets.newHashSet(IpWildcard.parse("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(65530, 65535)))
                        .build()))));
  }

  @Test
  public void testFullInterval() {
    SecurityGroup sg = _securityGroups.get(3);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    assertThat(
        inboundRules,
        equalTo(
            ImmutableList.of(
                _allowAllReverseOutboundRule,
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setSrcIps(Sets.newHashSet(IpWildcard.parse("1.2.3.4/32")))
                        .build()))));
  }

  @Test
  public void testAllTrafficAllowed() {
    SecurityGroup sg = _securityGroups.get(4);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    assertThat(
        inboundRules,
        equalTo(
            ImmutableList.of(
                _allowAllReverseOutboundRule,
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setSrcIps(Sets.newHashSet(IpWildcard.parse("0.0.0.0/0")))
                        .setDstPorts(Sets.newHashSet())
                        .build()))));
  }

  @Test
  public void testClosedInterval() {
    SecurityGroup sg = _securityGroups.get(5);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    assertThat(
        inboundRules,
        equalTo(
            ImmutableList.of(
                _allowAllReverseOutboundRule,
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setSrcIps(Sets.newHashSet(IpWildcard.parse("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(45, 50)))
                        .build()))));
  }

  @Test
  public void testInvalidStartInterval() {
    SecurityGroup sg = _securityGroups.get(6);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    assertThat(
        inboundRules,
        equalTo(
            ImmutableList.of(
                _allowAllReverseOutboundRule,
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setSrcIps(Sets.newHashSet(IpWildcard.parse("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(0, 50)))
                        .build()))));
  }

  @Test
  public void testInvalidEndInterval() {
    SecurityGroup sg = _securityGroups.get(7);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    assertThat(
        inboundRules,
        equalTo(
            ImmutableList.of(
                _allowAllReverseOutboundRule,
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setSrcIps(Sets.newHashSet(IpWildcard.parse("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(30, 65535)))
                        .build()))));
  }

  @Test
  public void testStatefulTcpRules() {
    SecurityGroup sg = _securityGroups.get(8);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    assertThat(
        inboundRules,
        equalTo(
            ImmutableList.of(
                // reverse of outbound rule
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setSrcIps(Sets.newHashSet(IpWildcard.parse("5.6.7.8/32")))
                        .setSrcPorts(Sets.newHashSet(SubRange.singleton(80)))
                        .setTcpFlags(ImmutableSet.of(TcpFlagsMatchConditions.ACK_TCP_FLAG))
                        .build()),
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setSrcIps(Sets.newHashSet(IpWildcard.parse("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(SubRange.singleton(22)))
                        .build()))));
    assertThat(
        outboundRules,
        equalTo(
            ImmutableList.of(
                // reverse of inbound rule
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setDstIps(Sets.newHashSet(IpWildcard.parse("1.2.3.4/32")))
                        .setSrcPorts(Sets.newHashSet(SubRange.singleton(22)))
                        .setTcpFlags(ImmutableSet.of(TcpFlagsMatchConditions.ACK_TCP_FLAG))
                        .build()),
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setDstIps(Sets.newHashSet(IpWildcard.parse("5.6.7.8/32")))
                        .setDstPorts(Sets.newHashSet(SubRange.singleton(80)))
                        .build()))));
  }

  @Test
  public void testDeniedSynOnlyResponse() {
    SecurityGroup sg = _securityGroups.get(8);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    IpAccessList outFilter =
        IpAccessList.builder().setName(TEST_ACL).setLines(outboundRules).build();

    // flow containing SYN and ~ACK should be rejected
    _flowBuilder
        .setDstIp(Ip.parse("1.2.3.4"))
        .setSrcPort(22)
        .setDstPort(NamedPort.EPHEMERAL_LOWEST.number())
        .setTcpFlagsAck(0)
        .setTcpFlagsSyn(1);

    assertThat(
        outFilter
            .filter(_flowBuilder.build(), null, ImmutableMap.of(), ImmutableMap.of())
            .getAction(),
        equalTo(LineAction.DENY));
  }

  @Test
  public void testAllowedSynAckResponse() {
    SecurityGroup sg = _securityGroups.get(8);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    IpAccessList outFilter =
        IpAccessList.builder().setName(TEST_ACL).setLines(outboundRules).build();

    // flow containing SYN and ACK should be accepted
    _flowBuilder
        .setDstIp(Ip.parse("1.2.3.4"))
        .setSrcPort(22)
        .setDstPort(NamedPort.EPHEMERAL_LOWEST.number())
        .setTcpFlagsAck(1)
        .setTcpFlagsSyn(1);

    assertThat(
        outFilter
            .filter(_flowBuilder.build(), null, ImmutableMap.of(), ImmutableMap.of())
            .getAction(),
        equalTo(LineAction.PERMIT));
  }

  @Test
  public void testDeniedWrongIpResponse() {
    SecurityGroup sg = _securityGroups.get(8);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();

    sg.addInOutAccessLines(inboundRules, outboundRules, _region);

    IpAccessList outFilter =
        IpAccessList.builder().setName(TEST_ACL).setLines(outboundRules).build();

    // flow containing wrong destination IP should be rejected
    _flowBuilder
        .setDstIp(Ip.parse("1.2.3.5"))
        .setSrcPort(22)
        .setDstPort(NamedPort.EPHEMERAL_LOWEST.number())
        .setTcpFlagsAck(1)
        .setTcpFlagsSyn(1);

    assertThat(
        outFilter
            .filter(_flowBuilder.build(), null, ImmutableMap.of(), ImmutableMap.of())
            .getAction(),
        equalTo(LineAction.DENY));
  }
}
