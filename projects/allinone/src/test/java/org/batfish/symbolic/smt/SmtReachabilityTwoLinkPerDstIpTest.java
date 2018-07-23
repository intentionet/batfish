package org.batfish.symbolic.smt;

import static java.util.Collections.singleton;
import static org.batfish.symbolic.smt.TwoNodeNetworkWithTwoLinks.DST_PREFIX_1;
import static org.batfish.symbolic.smt.TwoNodeNetworkWithTwoLinks.DST_PREFIX_2;
import static org.batfish.symbolic.smt.TwoNodeNetworkWithTwoLinks.LINK_1_NETWORK;
import static org.batfish.symbolic.smt.TwoNodeNetworkWithTwoLinks.LINK_2_NETWORK;
import static org.batfish.symbolic.smt.matchers.SmtReachabilityAnswerElementMatchers.hasVerificationResult;
import static org.batfish.symbolic.smt.matchers.VerificationResultMatchers.hasFailures;
import static org.batfish.symbolic.smt.matchers.VerificationResultMatchers.hasIsVerified;
import static org.batfish.symbolic.smt.matchers.VerificationResultMatchers.hasPacketModel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.main.Batfish;
import org.batfish.question.SmtReachabilityQuestionPlugin.ReachabilityQuestion;
import org.batfish.symbolic.answers.SmtReachabilityAnswerElement;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the smt-reachability question on a simple two-node network with two links between them.
 * Parameterized by the IPs accepted by the destination node.
 */
@RunWith(Parameterized.class)
public class SmtReachabilityTwoLinkPerDstIpTest {
  @Parameter public Ip _dstIp;

  @Rule public TemporaryFolder _temp = new TemporaryFolder();

  private Configuration _dstNode;
  private Configuration _srcNode;
  private Batfish _batfish;
  private String _failureDesc;

  @Before
  public void setup() throws IOException {
    _batfish = TwoNodeNetworkWithTwoLinks.create(_temp);
    Map<String, Configuration> configs = _batfish.loadConfigurations();
    _dstNode = configs.get(TwoNodeNetworkWithTwoLinks.DST_NODE);
    _srcNode = configs.get(TwoNodeNetworkWithTwoLinks.SRC_NODE);

    _failureDesc = String.format("link(%s,%s)", _dstNode.getHostname(), _srcNode.getHostname());
  }

  private static final List<Ip> DST_IPS =
      ImmutableList.of(
          LINK_1_NETWORK.getEndIp(),
          LINK_2_NETWORK.getEndIp(),
          DST_PREFIX_1.getStartIp(),
          DST_PREFIX_2.getStartIp());

  private static final List<String> DST_IP_STRINGS =
      DST_IPS.stream().map(Ip::toString).collect(ImmutableList.toImmutableList());

  @Parameters(name = "{index}: _dstIp = {0}")
  public static List<Ip[]> dstIps() {
    return DST_IPS.stream().map(d -> new Ip[] {d}).collect(ImmutableList.toImmutableList());
  }

  /** Verify that with no failures, source can reach the dest IP. */
  @Test
  public void testNoFailures() {
    final ReachabilityQuestion question = new ReachabilityQuestion();
    question.setIngressNodeRegex(_srcNode.getHostname());
    question.setFinalNodeRegex(_dstNode.getHostname());
    question.setDstIps(ImmutableSet.of(new IpWildcard(_dstIp)));
    final AnswerElement answer = _batfish.smtReachability(question);
    assertThat(answer, instanceOf(SmtReachabilityAnswerElement.class));

    final SmtReachabilityAnswerElement smtAnswer = (SmtReachabilityAnswerElement) answer;
    assertThat(
        smtAnswer,
        hasVerificationResult(
            allOf(hasIsVerified(true), hasFailures(nullValue()), hasPacketModel(nullValue()))));
  }

  /**
   * Verify that with no failures, source can reach each dest IP. This time, we negate the check and
   * get a counterexample.
   */
  @Test
  public void testNoFailures_negate() {
    final ReachabilityQuestion question = new ReachabilityQuestion();
    question.setIngressNodeRegex(_srcNode.getHostname());
    question.setFinalNodeRegex(_dstNode.getHostname());

    // verify unreachability, which is false (we'll get a counterexample).
    question.setNegate(true);

    question.setDstIps(ImmutableSet.of(new IpWildcard(_dstIp)));
    final AnswerElement answer = _batfish.smtReachability(question);
    assertThat(answer, instanceOf(SmtReachabilityAnswerElement.class));

    final SmtReachabilityAnswerElement smtAnswer = (SmtReachabilityAnswerElement) answer;
    assertThat(
        smtAnswer,
        hasVerificationResult(
            allOf(
                hasIsVerified(false),
                hasFailures(hasSize(0)),
                hasPacketModel(hasEntry("dstIp", _dstIp.toString())))));
  }

  /**
   * Test that with one failure, both links between the two nodes are down, so no _dstIp is
   * reachable from source.
   *
   * <p>{@link Ignore Ignored} because of issue #1723: adding a dstIp constraint seems to cause
   * minesweeper to ignore the failures constraint.
   */
  @Ignore("https://github.com/batfish/batfish/issues/1723")
  @Test
  public void testOneFailure() {
    final ReachabilityQuestion question = new ReachabilityQuestion();
    question.setIngressNodeRegex(_srcNode.getHostname());
    question.setFinalNodeRegex(_dstNode.getHostname());
    question.setDstIps(ImmutableSet.of(new IpWildcard(_dstIp)));
    question.setFailures(1); // at most 1 failure

    final AnswerElement answer = _batfish.smtReachability(question);
    assertThat(answer, instanceOf(SmtReachabilityAnswerElement.class));

    final SmtReachabilityAnswerElement smtAnswer = (SmtReachabilityAnswerElement) answer;
    assertThat(
        smtAnswer,
        hasVerificationResult(allOf(hasIsVerified(false), hasFailures(singleton(_failureDesc)))));
  }

  /**
   * Negation of the previous test, so we're verifying unreachability.
   *
   * <p>{@link Ignore Ignored} because of issue #1723: adding a dstIp constraint seems to cause
   * minesweeper to ignore the failures constraint.
   */
  @Ignore("https://github.com/batfish/batfish/issues/1723")
  @Test
  public void testOneFailure_negate() {
    final ReachabilityQuestion question = new ReachabilityQuestion();
    question.setIngressNodeRegex(_srcNode.getHostname());
    question.setFinalNodeRegex(_dstNode.getHostname());
    question.setDstIps(ImmutableSet.of(new IpWildcard(_dstIp)));
    question.setFailures(1);
    question.setNegate(true);

    final AnswerElement answer = _batfish.smtReachability(question);
    assertThat(answer, instanceOf(SmtReachabilityAnswerElement.class));

    final SmtReachabilityAnswerElement smtAnswer = (SmtReachabilityAnswerElement) answer;
    assertThat(
        smtAnswer,
        hasVerificationResult(allOf(hasIsVerified(true), hasFailures(singleton(_failureDesc)))));
  }

  @Test
  public void testOneFailure_notDstIp() {
    final ReachabilityQuestion question = new ReachabilityQuestion();
    question.setIngressNodeRegex(_srcNode.getHostname());
    question.setFinalNodeRegex(_dstNode.getHostname());
    question.setNotDstIps(ImmutableSet.of(new IpWildcard(_dstIp)));
    question.setFailures(1);

    final AnswerElement answer = _batfish.smtReachability(question);
    assertThat(answer, instanceOf(SmtReachabilityAnswerElement.class));

    final SmtReachabilityAnswerElement smtAnswer = (SmtReachabilityAnswerElement) answer;
    assertThat(
        smtAnswer,
        hasVerificationResult(
            allOf(
                hasIsVerified(false),
                /*
                 * For some reason, when we use setNotDstIps we get a packetModel, whereas if we
                 * use setDstIps we don't.
                 */
                hasPacketModel(
                    hasEntry(
                        equalTo("dstIp"),
                        allOf(in(DST_IP_STRINGS), not(equalTo(_dstIp.toString()))))),
                hasFailures(singleton(_failureDesc)))));
  }
}
