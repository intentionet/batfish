package org.batfish.question.specifiers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.ImmutableSortedSet;
import java.util.regex.Pattern;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.Protocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.specifier.AllInterfaceLinksLocationSpecifier;
import org.batfish.specifier.AllInterfaceLinksLocationSpecifierFactory;
import org.batfish.specifier.AllInterfacesLocationSpecifier;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.ConstantIpSpaceSpecifier;
import org.batfish.specifier.ConstantUniverseIpSpaceSpecifierFactory;
import org.batfish.specifier.ConstantWildcardSetIpSpaceSpecifierFactory;
import org.batfish.specifier.FlexibleLocationIpSpaceSpecifierFactory;
import org.batfish.specifier.FlexibleNodeSpecifierFactory;
import org.batfish.specifier.InferFromLocationIpSpaceSpecifier;
import org.batfish.specifier.NameRegexNodeSpecifier;
import org.batfish.specifier.NameRegexNodeSpecifierFactory;
import org.batfish.specifier.NodeSpecifierInterfaceLocationSpecifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test how {@link SpecifiersReachabilityQuestion} constructs different fields of its {@link
 * org.batfish.question.ReachabilityParameters parameters} (defaults, etc).
 */
public class SpecifiersReachabilityQuestionTest {
  @Rule public final ExpectedException exception = ExpectedException.none();

  @Test
  public void getDestinationIpSpace_bothNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    assertThat(
        question.getDestinationIpSpaceSpecifier(),
        equalTo(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE)));
  }

  @Test
  public void getDestinationIpSpace_factoryNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setDestinationIpSpaceSpecifierInput("ofLocation(foo)");
    assertThat(
        question.getDestinationIpSpaceSpecifier(),
        equalTo(new FlexibleLocationIpSpaceSpecifierFactory().buildIpSpaceSpecifier("foo")));
  }

  @Test
  public void getDestinationIpSpace() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setDestinationIpSpaceSpecifierFactory(ConstantWildcardSetIpSpaceSpecifierFactory.NAME);
    question.setDestinationIpSpaceSpecifierInput("1.2.3.0/24 - 1.2.3.4");
    assertThat(
        question.getDestinationIpSpaceSpecifier(),
        equalTo(
            new ConstantIpSpaceSpecifier(
                IpWildcardSetIpSpace.builder()
                    .including(new IpWildcard("1.2.3.0/24"))
                    .excluding(new IpWildcard("1.2.3.4"))
                    .build())));
  }

  @Test
  public void getFinalNodes_bothNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    assertThat(question.getFinalNodesSpecifier(), equalTo(AllNodesNodeSpecifier.INSTANCE));
  }

  @Test
  public void getFinalNodes_factoryNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setFinalNodesSpecifierInput("foo");
    assertThat(
        question.getFinalNodesSpecifier(),
        equalTo(new NameRegexNodeSpecifier(Pattern.compile("foo"))));
  }

  @Test
  public void getFinalNodes_factoryNonNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setFinalNodesSpecifierFactory(NameRegexNodeSpecifierFactory.NAME);
    question.setFinalNodesSpecifierInput("bar");
    assertThat(
        question.getFinalNodesSpecifier(),
        equalTo(new NameRegexNodeSpecifier(Pattern.compile("bar"))));
  }

  @Test
  public void getFinalNodes_factoryNonNull_exception() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setFinalNodesSpecifierFactory("foo");

    exception.expect(BatfishException.class);
    exception.expectMessage("Could not find NodeSpecifierFactory with name foo");
    question.getFinalNodesSpecifier();
  }

  @Test
  public void getHeaderspace() {
    ImmutableSortedSet<SubRange> dstPorts = ImmutableSortedSet.of(new SubRange(1, 2));
    ImmutableSortedSet<Protocol> dstProtocols = ImmutableSortedSet.of(Protocol.DNS);

    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setDstPorts(dstPorts);
    question.setDstProtocols(dstProtocols);

    HeaderSpace headerSpace = question.getHeaderSpace();
    assertThat(headerSpace.getDstPorts(), equalTo(dstPorts));
    assertThat(headerSpace.getDstProtocols(), equalTo(dstProtocols));
  }

  @Test
  public void getSourceIpSpaceSpecifier_bothNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    assertThat(
        question.getSourceIpSpaceSpecifier(), equalTo(InferFromLocationIpSpaceSpecifier.INSTANCE));
  }

  @Test
  public void getSourceIpSpaceSpecifier_factoryNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setSourceIpSpaceSpecifierInput("1.2.3.0/24 \\ 1.2.3.4");
    assertThat(
        question.getSourceIpSpaceSpecifier(),
        equalTo(
            new ConstantIpSpaceSpecifier(
                IpWildcardSetIpSpace.builder()
                    .including(new IpWildcard("1.2.3.0/24"))
                    .excluding(new IpWildcard("1.2.3.4"))
                    .build())));
  }

  @Test
  public void getSourceIpSpaceSpecifier_factoryNonNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setSourceIpSpaceSpecifierFactory(ConstantUniverseIpSpaceSpecifierFactory.NAME);
    assertThat(
        question.getSourceIpSpaceSpecifier(),
        equalTo(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE)));
  }

  @Test
  public void getSourceLocationSpecifier_bothNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    assertThat(
        question.getSourceLocationSpecifier(), equalTo(AllInterfacesLocationSpecifier.INSTANCE));
  }

  @Test
  public void getSourceLocationSpecifier_factoryNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setSourceLocationSpecifierInput("foo");
    assertThat(
        question.getSourceLocationSpecifier(),
        equalTo(
            new NodeSpecifierInterfaceLocationSpecifier(
                new FlexibleNodeSpecifierFactory().buildNodeSpecifier("foo"))));
  }

  @Test
  public void getSourceLocationSpecifier_factoryNonNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setSourceLocationSpecifierFactory(AllInterfaceLinksLocationSpecifierFactory.NAME);
    assertThat(
        question.getSourceLocationSpecifier(),
        equalTo(AllInterfaceLinksLocationSpecifier.INSTANCE));
  }

  @Test
  public void getTransitNodes_bothNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    assertThat(question.getForbiddenTransitNodesSpecifier(), nullValue());
    assertThat(question.getRequiredTransitNodesSpecifier(), nullValue());
  }

  @Test
  public void getTransitNodes_factoryNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setForbiddenTransitNodesNodeSpecifierInput("foo");
    question.setRequiredTransitNodesNodeSpecifierInput("foo");
    assertThat(
        question.getForbiddenTransitNodesSpecifier(),
        equalTo(new NameRegexNodeSpecifier(Pattern.compile("foo"))));
    assertThat(
        question.getRequiredTransitNodesSpecifier(),
        equalTo(new NameRegexNodeSpecifier(Pattern.compile("foo"))));
  }

  @Test
  public void getTransitNodes_factoryNonNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setForbiddenTransitNodesNodeSpecifierFactory(NameRegexNodeSpecifierFactory.NAME);
    question.setForbiddenTransitNodesNodeSpecifierInput("bar");
    question.setRequiredTransitNodesNodeSpecifierFactory(NameRegexNodeSpecifierFactory.NAME);
    question.setRequiredTransitNodesNodeSpecifierInput("bar");
    assertThat(
        question.getForbiddenTransitNodesSpecifier(),
        equalTo(new NameRegexNodeSpecifier(Pattern.compile("bar"))));
    assertThat(
        question.getRequiredTransitNodesSpecifier(),
        equalTo(new NameRegexNodeSpecifier(Pattern.compile("bar"))));
  }
}
