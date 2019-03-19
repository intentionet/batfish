package org.batfish.representation.juniper;

import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.flow.TransformationStep.TransformationType.SOURCE_NAT;
import static org.batfish.datamodel.transformation.IpField.SOURCE;
import static org.batfish.datamodel.transformation.Noop.NOOP_DEST_NAT;
import static org.batfish.datamodel.transformation.Transformation.when;
import static org.batfish.datamodel.transformation.TransformationStep.assignDestinationIp;
import static org.batfish.representation.juniper.NatPacketLocation.interfaceLocation;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDiff;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.flow.StepAction;
import org.batfish.datamodel.flow.TransformationStep;
import org.batfish.datamodel.flow.TransformationStep.TransformationStepDetail;
import org.batfish.datamodel.transformation.Noop;
import org.batfish.datamodel.transformation.PortField;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationEvaluator;
import org.batfish.datamodel.transformation.TransformationEvaluator.TransformationResult;
import org.batfish.representation.juniper.Nat.Type;
import org.junit.Test;

/** Tests for {@link NatRuleSet}. */
public class NatRuleSetTest {
  private static void setLocation(
      NatPacketLocation location, NatPacketLocation.Type type, String direction) {
    switch (type) {
      case InterfaceType:
        location.setInterface(String.format("Interface-%s", direction));
        break;
      case ZoneType:
        location.setZone(String.format("Zone-%s", direction));
        break;
      case RoutingInstanceType:
        location.setRoutingInstance(String.format("RoutingInstance-%s", direction));
        break;
      default:
        return;
    }
  }

  private static Optional<Transformation> toOutgoingTransformation(
      NatRuleSet ruleSet,
      Nat nat,
      Ip interfaceIp,
      Map<NatPacketLocation, AclLineMatchExpr> matchFromLocationExprs,
      @Nullable Transformation andThen,
      @Nullable Transformation orElse) {
    Warnings warnings = new Warnings(true, true, true);
    Optional<Transformation> transformation =
        ruleSet.toOutgoingTransformation(
            nat, interfaceIp, matchFromLocationExprs, andThen, orElse, warnings);
    assertTrue(warnings.getPedanticWarnings().isEmpty());
    assertTrue(warnings.getRedFlagWarnings().isEmpty());
    assertTrue(warnings.getUnimplementedWarnings().isEmpty());
    return transformation;
  }

  private static Optional<Transformation> toIncomingTransformation(
      NatRuleSet ruleSet,
      Nat nat,
      Ip interfaceIp,
      @Nullable Transformation andThen,
      @Nullable Transformation orElse) {
    Warnings warnings = new Warnings(true, true, true);
    Optional<Transformation> transformation =
        ruleSet.toIncomingTransformation(nat, interfaceIp, andThen, orElse, warnings);
    assertTrue(warnings.getPedanticWarnings().isEmpty());
    assertTrue(warnings.getRedFlagWarnings().isEmpty());
    assertTrue(warnings.getUnimplementedWarnings().isEmpty());
    return transformation;
  }

  @Test
  public void testComparator() {
    List<NatRuleSet> orderedRuleSetsList = new ArrayList<>();
    List<NatRuleSet> reverseOrderedRuleSetsList = new ArrayList<>();

    int i = 0;
    for (NatPacketLocation.Type toType : NatPacketLocation.Type.values()) {
      for (NatPacketLocation.Type fromType : NatPacketLocation.Type.values()) {
        NatRuleSet ruleSet = new NatRuleSet("ruleSet" + i);
        setLocation(ruleSet.getToLocation(), toType, "to" + i);
        setLocation(ruleSet.getFromLocation(), fromType, "from" + i);
        orderedRuleSetsList.add(ruleSet);
        reverseOrderedRuleSetsList.add(0, ruleSet);
        i++;
      }
    }

    reverseOrderedRuleSetsList.sort(NatRuleSet::compareTo);
    assertThat(orderedRuleSetsList, contains(reverseOrderedRuleSetsList.toArray()));
  }

  @Test
  public void testToTransformation() {
    Prefix prefix1 = Prefix.parse("1.1.1.0/24");
    Prefix prefix2 = Prefix.parse("2.2.2.0/24");

    NatRule natRule1 = new NatRule("natRule1");
    natRule1.getMatches().add(new NatRuleMatchDstAddr(prefix1));
    natRule1.setThen(NatRuleThenOff.INSTANCE);

    NatRule natRule2 = new NatRule("natRule2");
    natRule2.getMatches().add(new NatRuleMatchDstAddr(prefix2));
    natRule2.setThen(new NatRuleThenPool("POOL"));

    NatRuleSet ruleSet = new NatRuleSet("ruleset");
    String fromIface = "fromLocationInterface";
    ruleSet.getFromLocation().setInterface(fromIface);
    ruleSet.getRules().add(natRule1);
    ruleSet.getRules().add(natRule2);

    NatPool pool = new NatPool();
    Ip poolStart = Ip.parse("3.0.0.0");
    Ip poolEnd = Ip.parse("4.0.0.0");
    pool.setFromAddress(poolStart);
    pool.setToAddress(poolEnd);

    // the transformation to apply after any NatRule transformation is applied.
    Transformation andThen =
        when(matchSrcInterface("and then")).apply(new Noop(SOURCE_NAT)).build();
    Transformation orElse = when(matchSrcInterface("or else")).apply(new Noop(SOURCE_NAT)).build();

    MatchSrcInterface matchFromIface = matchSrcInterface(fromIface);

    // the transformation for the rules themselves
    Transformation rulesTransformation =
        // first apply natRule1
        when(matchDst(prefix1))
            .apply(NOOP_DEST_NAT)
            .setAndThen(andThen)
            .setOrElse(
                // only apply natRule2 if natRule1 doesn't match
                when(matchDst(prefix2))
                    .apply(assignDestinationIp(poolStart, poolEnd))
                    .setAndThen(andThen)
                    .setOrElse(orElse)
                    .build())
            .build();

    Nat dnat = new Nat(Type.DESTINATION);
    dnat.getPools().put("POOL", pool);

    Ip interfaceIp = Ip.ZERO;
    assertThat(
        toOutgoingTransformation(
                ruleSet,
                dnat,
                interfaceIp,
                ImmutableMap.of(interfaceLocation(fromIface), matchFromIface),
                andThen,
                orElse)
            .get(),
        equalTo(
            // first match from location
            when(matchFromIface).setAndThen(rulesTransformation).setOrElse(orElse).build()));

    assertThat(
        toIncomingTransformation(ruleSet, dnat, interfaceIp, andThen, orElse).get(),
        equalTo(rulesTransformation));
  }

  @Test
  public void testTrace() {
    Prefix prefix1 = Prefix.parse("1.1.1.0/24");
    Prefix prefix2 = Prefix.parse("2.2.2.0/24");

    NatRule natRule1 = new NatRule("natRule1");
    natRule1.getMatches().add(new NatRuleMatchDstAddr(prefix1));
    natRule1.setThen(NatRuleThenOff.INSTANCE);

    NatRule natRule2 = new NatRule("natRule2");
    natRule2.getMatches().add(new NatRuleMatchDstAddr(prefix2));
    natRule2.setThen(new NatRuleThenPool("POOL"));

    NatRuleSet ruleSet = new NatRuleSet("ruleset");
    String fromIface = "fromLocationInterface";
    NatPacketLocation fromLocation = ruleSet.getFromLocation();
    fromLocation.setInterface(fromIface);
    ruleSet.getRules().add(natRule1);
    ruleSet.getRules().add(natRule2);

    NatPool pool = new NatPool();
    Ip poolStart = Ip.parse("3.0.0.0");
    Ip poolEnd = Ip.parse("4.0.0.0");
    pool.setFromAddress(poolStart);
    pool.setToAddress(poolEnd);

    Nat snat = new Nat(Type.SOURCE);
    snat.getPools().put("POOL", pool);

    Ip interfaceIp = Ip.parse("9.9.9.9");
    Transformation transformation =
        toOutgoingTransformation(
                ruleSet,
                snat,
                interfaceIp,
                ImmutableMap.of(fromLocation, matchSrcInterface(fromIface)),
                null,
                null)
            .get();

    Flow.Builder fb =
        Flow.builder()
            .setIngressNode("ingressNode")
            .setSrcIp(Ip.ZERO)
            .setSrcPort(0)
            .setDstPort(0)
            .setTag("tag");

    // doesn't match rule set
    TransformationResult result =
        TransformationEvaluator.eval(
            transformation,
            fb.setDstIp(prefix1.getStartIp()).build(),
            "foo",
            ImmutableMap.of(),
            ImmutableMap.of());
    assertThat(result.getTraceSteps(), empty());

    // matches rule set, matches neither rule
    result =
        TransformationEvaluator.eval(
            transformation,
            fb.setDstIp(Ip.ZERO).build(),
            fromIface,
            ImmutableMap.of(),
            ImmutableMap.of());
    assertThat(result.getTraceSteps(), empty());

    // matches rule set, matches rule1
    result =
        TransformationEvaluator.eval(
            transformation,
            fb.setDstIp(prefix1.getStartIp()).build(),
            fromIface,
            ImmutableMap.of(),
            ImmutableMap.of());
    assertThat(
        result.getTraceSteps(),
        contains(
            new TransformationStep(
                new TransformationStepDetail(SOURCE_NAT, ImmutableSortedSet.of()),
                StepAction.PERMITTED)));

    // matches rule set, matches rule2
    result =
        TransformationEvaluator.eval(
            transformation,
            fb.setDstIp(prefix2.getStartIp()).build(),
            fromIface,
            ImmutableMap.of(),
            ImmutableMap.of());
    assertThat(
        result.getTraceSteps(),
        contains(
            new TransformationStep(
                new TransformationStepDetail(
                    SOURCE_NAT,
                    ImmutableSortedSet.of(
                        FlowDiff.flowDiff(SOURCE, Ip.ZERO, poolStart),
                        FlowDiff.flowDiff(PortField.SOURCE, 0, 1024))),
                StepAction.TRANSFORMED)));
  }
}
