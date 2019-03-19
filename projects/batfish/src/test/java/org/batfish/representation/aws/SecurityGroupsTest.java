package org.batfish.representation.aws;

import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_SECURITY_GROUPS;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.LinkedList;
import java.util.List;
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
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link SecurityGroup}. */
public class SecurityGroupsTest {

  private JSONArray _securityGroups;
  private IpAccessListLine _allowAllReverseOutboundRule;
  private Flow.Builder _flowBuilder;
  private Region _region;

  public static String TEST_ACL = "test_acl";

  @Before
  public void setup() throws JSONException {
    _securityGroups =
        new JSONObject(
                CommonUtil.readResource("org/batfish/representation/aws/SecurityGroupTest.json"))
            .getJSONArray(JSON_KEY_SECURITY_GROUPS);
    _allowAllReverseOutboundRule =
        IpAccessListLine.acceptingHeaderSpace(
            HeaderSpace.builder()
                .setSrcIps(Sets.newHashSet(new IpWildcard("0.0.0.0/0")))
                .setTcpFlags(ImmutableSet.of(TcpFlagsMatchConditions.ACK_TCP_FLAG))
                .build());
    _region = new Region("test");
    _flowBuilder =
        Flow.builder().setIngressNode("foo").setTag("TEST").setIpProtocol(IpProtocol.TCP);
  }

  @Test
  public void testSinglePort() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(0));

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
                        .setSrcIps(Sets.newHashSet(new IpWildcard("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(22, 22)))
                        .build()))));
  }

  @Test
  public void testBeginningHalfOpenInterval() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(1));

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
                        .setSrcIps(Sets.newHashSet(new IpWildcard("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(0, 22)))
                        .build()))));
  }

  @Test
  public void testEndHalfOpenInterval() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(2));

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
                        .setSrcIps(Sets.newHashSet(new IpWildcard("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(65530, 65535)))
                        .build()))));
  }

  @Test
  public void testFullInterval() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(3));

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
                        .setSrcIps(Sets.newHashSet(new IpWildcard("1.2.3.4/32")))
                        .build()))));
  }

  @Test
  public void testAllTrafficAllowed() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(4));

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
                        .setSrcIps(Sets.newHashSet(new IpWildcard("0.0.0.0/0")))
                        .setDstPorts(Sets.newHashSet())
                        .build()))));
  }

  @Test
  public void testClosedInterval() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(5));

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
                        .setSrcIps(Sets.newHashSet(new IpWildcard("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(45, 50)))
                        .build()))));
  }

  @Test
  public void testInvalidStartInterval() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(6));

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
                        .setSrcIps(Sets.newHashSet(new IpWildcard("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(0, 50)))
                        .build()))));
  }

  @Test
  public void testInvalidEndInterval() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(7));

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
                        .setSrcIps(Sets.newHashSet(new IpWildcard("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(30, 65535)))
                        .build()))));
  }

  @Test
  public void testStatefulTcpRules() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(8));

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
                        .setSrcIps(Sets.newHashSet(new IpWildcard("5.6.7.8/32")))
                        .setSrcPorts(Sets.newHashSet(new SubRange(80, 80)))
                        .setTcpFlags(ImmutableSet.of(TcpFlagsMatchConditions.ACK_TCP_FLAG))
                        .build()),
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setSrcIps(Sets.newHashSet(new IpWildcard("1.2.3.4/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(22, 22)))
                        .build()))));
    assertThat(
        outboundRules,
        equalTo(
            ImmutableList.of(
                // reverse of inbound rule
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setDstIps(Sets.newHashSet(new IpWildcard("1.2.3.4/32")))
                        .setSrcPorts(Sets.newHashSet(new SubRange(22, 22)))
                        .setTcpFlags(ImmutableSet.of(TcpFlagsMatchConditions.ACK_TCP_FLAG))
                        .build()),
                IpAccessListLine.acceptingHeaderSpace(
                    HeaderSpace.builder()
                        .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                        .setDstIps(Sets.newHashSet(new IpWildcard("5.6.7.8/32")))
                        .setDstPorts(Sets.newHashSet(new SubRange(80, 80)))
                        .build()))));
  }

  @Test
  public void testDeniedSynOnlyResponse() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(8));

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
  public void testAllowedSynAckResponse() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(8));

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
  public void testDeniedWrongIpResponse() throws JSONException {
    SecurityGroup sg = new SecurityGroup(_securityGroups.getJSONObject(8));

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
