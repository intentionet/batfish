package org.batfish.specifier;

import static org.batfish.specifier.FlexibleLocationSpecifierFactory.parse;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link FlexibleLocationSpecifierFactory}. */
@RunWith(JUnit4.class)
public class FlexibleLocationSpecifierFactoryTest {
  @Rule public ExpectedException exception = ExpectedException.none();

  @Test
  public void parseAtomic() {
    assertThat(
        parse("foo"),
        equalTo(
            new NodeSpecifierInterfaceLocationSpecifier(
                new FlexibleNodeSpecifierFactory().buildNodeSpecifier("foo"))));
    assertThat(
        parse("ref.noderole(bar1, bar2)"),
        equalTo(
            new NodeSpecifierInterfaceLocationSpecifier(
                new FlexibleNodeSpecifierFactory()
                    .buildNodeSpecifier("ref.noderole(bar1, bar2)"))));
    assertThat(
        parse("[bar]"),
        equalTo(
            new InterfaceSpecifierInterfaceLocationSpecifier(
                new FlexibleInterfaceSpecifierFactory().buildInterfaceSpecifier("bar"))));
    assertThat(
        parse("foo[bar]"),
        equalTo(
            new IntersectionLocationSpecifier(
                new NodeSpecifierInterfaceLocationSpecifier(
                    new FlexibleNodeSpecifierFactory().buildNodeSpecifier("foo")),
                new InterfaceSpecifierInterfaceLocationSpecifier(
                    new FlexibleInterfaceSpecifierFactory().buildInterfaceSpecifier("bar")))));
  }

  @Test
  public void parseCombination() {
    LocationSpecifier leaf =
        new NodeSpecifierInterfaceLocationSpecifier(
            new FlexibleNodeSpecifierFactory().buildNodeSpecifier("foo"));
    LocationSpecifier difference = new DifferenceLocationSpecifier(leaf, leaf);
    LocationSpecifier union = new UnionLocationSpecifier(leaf, leaf);
    LocationSpecifier unionDiff = new DifferenceLocationSpecifier(union, leaf);
    assertThat(parse("foo - foo"), equalTo(difference));
    assertThat(parse("foo + foo"), equalTo(union));
    assertThat(parse("foo + foo - foo"), equalTo(unionDiff));
  }

  @Test
  public void parseFunc() {
    assertThat(
        parse("enter(foo)"),
        equalTo(
            new ToInterfaceLinkLocationSpecifier(
                new NodeSpecifierInterfaceLocationSpecifier(
                    new FlexibleNodeSpecifierFactory().buildNodeSpecifier("foo")))));
    exception.expect(UnsupportedOperationException.class);
    assertThat(
        parse("exit(foo)"),
        equalTo(
            new ToInterfaceLinkLocationSpecifier(
                new NodeSpecifierInterfaceLocationSpecifier(
                    new FlexibleNodeSpecifierFactory().buildNodeSpecifier("foo")))));
  }

  @Test
  public void parseUnbalancedBrackets() {
    exception.expect(IllegalArgumentException.class);
    parse("foo[bar");
  }

  @Test
  public void parseUnbalancedParenthesis() {
    exception.expect(IllegalArgumentException.class);
    parse("enter(foo");
  }
}
