package org.batfish.question.specifiers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.regex.Pattern;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.specifier.AllInterfaceLinksLocationSpecifier;
import org.batfish.specifier.AllInterfaceLinksLocationSpecifierFactory;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.ConstantIpSpaceSpecifier;
import org.batfish.specifier.ConstantUniverseIpSpaceSpecifierFactory;
import org.batfish.specifier.InferFromLocationIpSpaceSpecifier;
import org.batfish.specifier.NameRegexInterfaceLinkLocationSpecifier;
import org.batfish.specifier.NameRegexNodeSpecifier;
import org.batfish.specifier.NameRegexNodeSpecifierFactory;
import org.batfish.specifier.NoNodesNodeSpecifier;
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
        question.getSourceLocationSpecifier(),
        equalTo(AllInterfaceLinksLocationSpecifier.INSTANCE));
  }

  @Test
  public void getSourceLocationSpecifier_factoryNull() {
    SpecifiersReachabilityQuestion question = new SpecifiersReachabilityQuestion();
    question.setSourceLocationSpecifierInput("foo");
    assertThat(
        question.getSourceLocationSpecifier(),
        equalTo(new NameRegexInterfaceLinkLocationSpecifier(Pattern.compile("foo"))));
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
    assertThat(
        question.getForbiddenTransitNodesSpecifier(), equalTo(NoNodesNodeSpecifier.INSTANCE));
    assertThat(question.getRequiredTransitNodesSpecifier(), equalTo(NoNodesNodeSpecifier.INSTANCE));
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
