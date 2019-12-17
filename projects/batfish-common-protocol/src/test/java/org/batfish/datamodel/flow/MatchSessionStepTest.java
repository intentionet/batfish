package org.batfish.datamodel.flow;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.MatchSessionStep.MatchSessionStepDetail;
import org.junit.Test;

/** Test for {@link MatchSessionStep}. */
public final class MatchSessionStepTest {
  @Test
  public void testConstructor() {
    Set<String> incomingInterfaces = ImmutableSet.of("a");
    SessionMatchExpr matchCriteria =
        new SessionMatchExpr(IpProtocol.ICMP, Ip.parse("1.1.1.1"), Ip.parse("2.2.2.2"), null, null);
    MatchSessionStep step =
        new MatchSessionStep(
            MatchSessionStepDetail.builder()
                .setIncomingInterfaces(incomingInterfaces)
                .setSessionAction(Accept.INSTANCE)
                .setMatchCriteria(matchCriteria)
                .build());
    assertEquals(step.getAction(), StepAction.MATCHED_SESSION);
    assertEquals(step.getDetail().getIncomingInterfaces(), incomingInterfaces);
    assertEquals(step.getDetail().getSessionAction(), Accept.INSTANCE);
    assertEquals(step.getDetail().getMatchCriteria(), matchCriteria);
  }

  @Test
  public void testJsonSerialization() throws IOException {
    Set<String> incomingInterfaces = ImmutableSet.of("b");
    ForwardOutInterface forwardAction =
        new ForwardOutInterface("a", NodeInterfacePair.of("a", "b"));
    SessionMatchExpr matchCriteria =
        new SessionMatchExpr(IpProtocol.ICMP, Ip.parse("1.1.1.1"), Ip.parse("2.2.2.2"), null, null);
    MatchSessionStep step =
        new MatchSessionStep(
            MatchSessionStepDetail.builder()
                .setIncomingInterfaces(incomingInterfaces)
                .setSessionAction(forwardAction)
                .setMatchCriteria(matchCriteria)
                .build());
    MatchSessionStep clone = BatfishObjectMapper.clone(step, MatchSessionStep.class);
    assertEquals(step.getAction(), clone.getAction());
    assertThat(
        clone.getDetail().getIncomingInterfaces(),
        equalTo(step.getDetail().getIncomingInterfaces()));
    assertThat(clone.getDetail().getSessionAction(), equalTo(step.getDetail().getSessionAction()));
    assertThat(clone.getDetail().getMatchCriteria(), equalTo(step.getDetail().getMatchCriteria()));
  }
}
