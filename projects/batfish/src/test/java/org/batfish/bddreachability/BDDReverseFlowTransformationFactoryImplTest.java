package org.batfish.bddreachability;

import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.transformation.Transformation.always;
import static org.batfish.datamodel.transformation.Transformation.when;
import static org.batfish.datamodel.transformation.TransformationStep.assignDestinationIp;
import static org.batfish.datamodel.transformation.TransformationStep.assignSourceIp;
import static org.batfish.datamodel.transformation.TransformationStep.shiftDestinationIp;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.transition.TransformationToTransition;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.common.bdd.BDDInteger;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.HeaderSpaceToBDD;
import org.batfish.common.bdd.IpAccessListToBddImpl;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.transformation.Transformation;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link BDDReverseFlowTransformationFactoryImpl}. */
public class BDDReverseFlowTransformationFactoryImplTest {
  private static final BDDPacket PKT = new BDDPacket();
  private static final BDD ONE = PKT.getFactory().one();
  private static final BDD ZERO = PKT.getFactory().zero();

  private static final String HOSTNAME = "HOSTNAME";
  private static final String IFACENAME = "INTERFACE";

  private Map<String, TransformationToTransition> _transformationToTransitions;
  private Map<String, Configuration> _configs;
  private Interface.Builder _ib;
  private HeaderSpaceToBDD _headerSpaceToBDD;

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    Configuration config =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(HOSTNAME)
            .build();
    Vrf vrf = nf.vrfBuilder().setOwner(config).build();
    _ib = nf.interfaceBuilder().setOwner(config).setVrf(vrf).setActive(true).setName(IFACENAME);
    _configs = ImmutableMap.of(HOSTNAME, config);
    _headerSpaceToBDD = new HeaderSpaceToBDD(PKT, ImmutableMap.of());
    _transformationToTransitions =
        ImmutableMap.of(
            HOSTNAME,
            new TransformationToTransition(
                PKT,
                new IpAccessListToBddImpl(
                    PKT,
                    BDDSourceManager.forInterfaces(PKT, ImmutableSet.of()),
                    _headerSpaceToBDD,
                    ImmutableMap.of())));
  }

  private BDD dstBdd(Ip ip) {
    return _headerSpaceToBDD.getDstIpSpaceToBdd().toBDD(ip);
  }

  private BDD dstBdd(Ip low, Ip high) {
    BDDInteger var = _headerSpaceToBDD.getDstIpSpaceToBdd().getBDDInteger();
    return var.geq(low.asLong()).and(var.leq(high.asLong()));
  }

  private BDD srcBdd(Ip ip) {
    return _headerSpaceToBDD.getSrcIpSpaceToBdd().toBDD(ip);
  }

  private BDD srcBdd(Prefix prefix) {
    return _headerSpaceToBDD.getSrcIpSpaceToBdd().toBDD(prefix);
  }

  private BDD srcBdd(IpWildcard ipWildcard) {
    return _headerSpaceToBDD.getSrcIpSpaceToBdd().toBDD(ipWildcard);
  }

  @Test
  public void testNoTransformation() {}

  @Test
  public void testSrcAssignToPool() {
    Ip poolStart = Ip.parse("1.1.1.1");
    Ip poolEnd = Ip.parse("1.1.1.5");
    Transformation transformation = always().apply(assignSourceIp(poolStart, poolEnd)).build();
    Interface i =
        _ib.setIncomingTransformation(transformation)
            .setOutgoingTransformation(transformation)
            .build();
    BDDReverseFlowTransformationFactoryImpl factory =
        new BDDReverseFlowTransformationFactoryImpl(_configs, _transformationToTransitions);
    Transition incomingTransformation =
        factory.reverseFlowIncomingTransformation(HOSTNAME, i.getName());

    BDD poolBdd = dstBdd(poolStart, poolEnd);
    assertTrue(
        "Anything might have been natted", incomingTransformation.transitForward(poolBdd).isOne());
    assertTrue(
        "If outside the pool, not natted",
        incomingTransformation.transitForward(poolBdd.not()).isZero());

    Transition outgoingTransformation =
        factory.reverseFlowOutgoingTransformation(HOSTNAME, i.getName());

    assertThat(
        "Anything might have been natted",
        outgoingTransformation.transitForward(poolBdd),
        equalTo(ONE));
    assertThat(
        "If outside the pool, not natted",
        outgoingTransformation.transitForward(poolBdd.not()),
        equalTo(ZERO));
  }

  @Test
  public void testMatchAndAssignSameField() {
    Ip matchIp = Ip.parse("255.255.255.255");
    Ip poolIp = Ip.parse("0.0.0.0");
    Transformation transformation =
        when(matchDst(matchIp)).apply(assignDestinationIp(poolIp, poolIp)).build();
    Interface i =
        _ib.setIncomingTransformation(transformation)
            .setOutgoingTransformation(transformation)
            .build();
    BDDReverseFlowTransformationFactoryImpl factory =
        new BDDReverseFlowTransformationFactoryImpl(_configs, _transformationToTransitions);

    BDD matchBdd = srcBdd(matchIp);
    BDD poolBdd = srcBdd(poolIp);

    Transition transition = factory.reverseFlowIncomingTransformation(HOSTNAME, i.getName());
    assertThat(
        "pool Ip may or may not have been natted.",
        transition.transitForward(poolBdd),
        equalTo(matchBdd.or(poolBdd)));
    assertThat(
        "If outside the pool, not matched",
        transition.transitForward(poolBdd.not()),
        equalTo(poolBdd.not().and(matchBdd.not())));
  }

  @Test
  public void testShiftPrefix() {
    Prefix shiftPrefix = Prefix.parse("1.1.1.0/24");
    Transformation transformation = always().apply(shiftDestinationIp(shiftPrefix)).build();

    Interface i =
        _ib.setIncomingTransformation(transformation)
            .setOutgoingTransformation(transformation)
            .build();

    BDDReverseFlowTransformationFactoryImpl factory =
        new BDDReverseFlowTransformationFactoryImpl(_configs, _transformationToTransitions);

    BDD shiftPrefixBdd = srcBdd(shiftPrefix);

    Transition transition = factory.reverseFlowIncomingTransformation(HOSTNAME, i.getName());
    assertThat(
        "everything is mapped to the shift prefix",
        transition.transitForward(shiftPrefixBdd),
        equalTo(ONE));
    assertThat(
        "nothing is mapped outside the shift prefix",
        transition.transitForward(shiftPrefixBdd.not()),
        equalTo(ZERO));

    transition = factory.reverseFlowOutgoingTransformation(HOSTNAME, i.getName());
    assertThat(
        "everything is mapped to the shift prefix",
        transition.transitForward(shiftPrefixBdd),
        equalTo(ONE));
    assertThat(
        "nothing is mapped outside the shift prefix",
        transition.transitForward(shiftPrefixBdd.not()),
        equalTo(ZERO));
  }

  @Test
  public void testAssignAndMatchSameField() {
    // match after assigning the same field.

    Ip poolIp = Ip.parse("2.2.2.2");
    Prefix shiftPrefix = Prefix.parse("1.1.1.0/24");
    Ip matchIp = Ip.parse("1.1.1.1");
    Transformation transformation =
        always()
            .apply(shiftDestinationIp(shiftPrefix))
            .setAndThen(when(matchDst(matchIp)).apply(assignDestinationIp(poolIp, poolIp)).build())
            .build();

    Interface i =
        _ib.setIncomingTransformation(transformation)
            .setOutgoingTransformation(transformation)
            .build();

    BDDReverseFlowTransformationFactoryImpl factory =
        new BDDReverseFlowTransformationFactoryImpl(_configs, _transformationToTransitions);

    BDD poolBdd = srcBdd(poolIp);
    BDD shiftPrefixBdd = srcBdd(shiftPrefix);

    IpWildcard preshiftMatchWildcard =
        new IpWildcard(Ip.create(0x00000001L), Ip.create(0xFFFFFF00L));
    BDD preshiftMatchBdd = srcBdd(preshiftMatchWildcard);

    Transition transition = factory.reverseFlowIncomingTransformation(HOSTNAME, i.getName());
    assertThat(
        "pool-natted flows were shifted to the matchIp",
        transition.transitForward(poolBdd),
        equalTo(preshiftMatchBdd));
    assertThat(
        "non-pool-natted flows are everything that doesn't shift to the matchIp",
        transition.transitForward(shiftPrefixBdd),
        equalTo(preshiftMatchBdd.not()));
    assertThat(
        "range of the forward transformation is the poolIp and the shift prefix",
        transition.transitForward(poolBdd.or(shiftPrefixBdd).not()),
        equalTo(ZERO));

    transition = factory.reverseFlowOutgoingTransformation(HOSTNAME, i.getName());
    assertThat(
        "pool-natted flows were shifted to the matchIp",
        transition.transitForward(poolBdd),
        equalTo(preshiftMatchBdd));
    assertThat(
        "non-pool-natted flows are everything that doesn't shift to the matchIp",
        transition.transitForward(shiftPrefixBdd),
        equalTo(preshiftMatchBdd.not()));
    assertThat(
        "range of the forward transformation is the poolIp and the shift prefix",
        transition.transitForward(poolBdd.or(shiftPrefixBdd).not()),
        equalTo(ZERO));
  }

  @Test
  public void testAndThen() {
    Prefix match1Prefix = Prefix.parse("1.0.0.0/8");
    Prefix match2Prefix = Prefix.parse("1.1.0.0/16");
    Ip pool1 = Ip.parse("255.255.255.255");
    Ip pool2 = Ip.parse("0.0.0.0");
    Transformation transformation =
        when(matchDst(match1Prefix))
            .apply(assignSourceIp(pool1, pool1))
            .setAndThen(when(matchDst(match2Prefix)).apply(assignSourceIp(pool2, pool2)).build())
            .build();

    Interface i =
        _ib.setIncomingTransformation(transformation)
            .setOutgoingTransformation(transformation)
            .build();

    BDDReverseFlowTransformationFactoryImpl factory =
        new BDDReverseFlowTransformationFactoryImpl(_configs, _transformationToTransitions);

    BDD pool1Bdd = dstBdd(pool1);
    BDD pool2Bdd = dstBdd(pool2);
    BDD match1Bdd = srcBdd(match1Prefix);
    BDD match2Bdd = srcBdd(match2Prefix);

    Transition transition = factory.reverseFlowIncomingTransformation(HOSTNAME, i.getName());
    assertThat(
        "flows natted to pool1 were matched by match1 but not match2",
        transition.transitForward(pool1Bdd),
        equalTo(pool1Bdd.and(match1Bdd.not()).or(match1Bdd.and(match2Bdd.not()))));
    assertThat(
        "flows natted to pool2 were matched by match1 and match2",
        transition.transitForward(pool2Bdd),
        // match2Bdd implies matched by match1 and match2
        equalTo(pool2Bdd.and(match1Bdd.not()).or(match2Bdd)));
    assertThat(
        "flows not natted were not matched by match1",
        transition.transitForward(pool1Bdd.or(pool2Bdd).not()),
        equalTo(pool1Bdd.or(pool2Bdd).not().and(match1Bdd.not())));

    transition = factory.reverseFlowOutgoingTransformation(HOSTNAME, i.getName());
    assertThat(
        "flows natted to pool1 were matched by match1 but not match2",
        transition.transitForward(pool1Bdd),
        equalTo(pool1Bdd.and(match1Bdd.not()).or(match1Bdd.and(match2Bdd.not()))));
    assertThat(
        "flows natted to pool2 were matched by match1 and match2",
        transition.transitForward(pool2Bdd),
        // match2Bdd implies matched by match1 and match2
        equalTo(pool2Bdd.and(match1Bdd.not()).or(match2Bdd)));
    assertThat(
        "flows not natted were not matched by match1",
        transition.transitForward(pool1Bdd.or(pool2Bdd).not()),
        equalTo(pool1Bdd.or(pool2Bdd).not().and(match1Bdd.not())));
  }

  @Test
  public void testOrElse() {
    Prefix matchPrefix = Prefix.parse("1.0.0.0/8");
    Ip pool1 = Ip.parse("5.5.5.5");
    Ip pool2 = Ip.parse("6.6.6.6");
    Transformation transformation =
        when(matchDst(matchPrefix))
            .apply(assignSourceIp(pool1, pool1))
            .setOrElse(always().apply(assignSourceIp(pool2, pool2)).build())
            .build();

    Interface i =
        _ib.setIncomingTransformation(transformation)
            .setOutgoingTransformation(transformation)
            .build();

    BDDReverseFlowTransformationFactoryImpl factory =
        new BDDReverseFlowTransformationFactoryImpl(_configs, _transformationToTransitions);
    Transition incomingTransformation =
        factory.reverseFlowIncomingTransformation(HOSTNAME, i.getName());

    BDD pool1Bdd = dstBdd(pool1);
    BDD pool2Bdd = dstBdd(pool2);
    BDD matchBdd = srcBdd(matchPrefix);

    assertThat(
        "matched flows are natted to pool1",
        incomingTransformation.transitForward(pool1Bdd),
        equalTo(matchBdd));
    assertThat(
        "unmatched flows are natted to pool2",
        incomingTransformation.transitForward(pool2Bdd),
        equalTo(matchBdd.not()));
    assertThat(
        "everything is natted to pool1 or pool2",
        incomingTransformation.transitForward(pool1Bdd.or(pool2Bdd).not()),
        equalTo(ZERO));

    Transition outgoingTransformation =
        factory.reverseFlowOutgoingTransformation(HOSTNAME, i.getName());

    assertThat(
        "matched flows are natted to pool1",
        outgoingTransformation.transitForward(pool1Bdd),
        equalTo(matchBdd));
    assertThat(
        "unmatched flows are natted to pool2",
        outgoingTransformation.transitForward(pool2Bdd),
        equalTo(matchBdd.not()));
    assertThat(
        "everything is natted to pool1 or pool2",
        outgoingTransformation.transitForward(pool1Bdd.or(pool2Bdd).not()),
        equalTo(ZERO));
  }
}
