package org.batfish.grammar.flatjuniper;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Configuration.Builder;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.expr.ExplicitAsPathSet;
import org.batfish.datamodel.routing_policy.expr.RegexAsPathSetElem;
import org.junit.Test;

/**
 * Tests of {@link AsPathRegex}.
 *
 * <p>These tests are all derived from <a
 * href="https://www.juniper.net/documentation/en_US/junos/topics/usage-guidelines/policy-configuring-as-path-regular-expressions-to-use-as-routing-policy-match-conditions.html#id-10240761">Juniper
 * documentation</a>. Since the documentation only provides positive examples, negative examples are
 * home brewed.
 */
public class AsPathRegexTest {
  private static Environment buildEnvironment(AsPath path) {
    NetworkFactory nf = new NetworkFactory();
    Builder cb = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.JUNIPER);
    Configuration c = cb.build();
    c.setVrfs(
        ImmutableMap.of(Configuration.DEFAULT_VRF_NAME, new Vrf(Configuration.DEFAULT_VRF_NAME)));
    return Environment.builder(c)
        .setOriginalRoute(
            BgpRoute.builder()
                .setOriginatorIp(Ip.ZERO)
                .setOriginType(OriginType.INCOMPLETE)
                .setProtocol(RoutingProtocol.BGP)
                .setNetwork(Prefix.ZERO)
                .setAsPath(path)
                .build())
        .build();
  }

  private static void assertMatchResult(boolean expected, String regex, AsPath path) {
    String javaRegex = AsPathRegex.convertToJavaRegex(regex);
    ExplicitAsPathSet set = new ExplicitAsPathSet(new RegexAsPathSetElem(javaRegex));
    Environment testEnvironment = buildEnvironment(path);
    String expectedMsg = expected ? "should match" : "should not match";
    assertThat(
        String.format(
            "%s [converted to %s] %s %s (with string repr %s)",
            regex, javaRegex, expectedMsg, path, path.getAsPathString()),
        set.matches(testEnvironment),
        is(expected));
  }

  private static void assertDoesNotMatch(String regex, Long... asPath) {
    assertMatchResult(false, regex, AsPath.ofSingletonAsSets(asPath));
  }

  private static void assertMatches(String regex, Long... asPath) {
    assertMatchResult(true, regex, AsPath.ofSingletonAsSets(asPath));
  }

  @Test
  public void testMatchingSingleNumber() {
    String regex = "1234";
    assertMatches(regex, 1234L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 123L);
    assertDoesNotMatch(regex, 1234L, 1L);
    assertDoesNotMatch(regex, 1L, 1234L, 1L);
    assertDoesNotMatch(regex, 1L, 2L, 3L, 4L);
  }

  @Test
  public void testMatchingZeroOrMore() {
    String regex = "1234*";
    assertMatches(regex, 1234L);
    assertMatches(regex, 1234L, 1234L);
    assertMatches(regex, 1234L, 1234L, 1234L);
    assertMatches(regex);
    assertDoesNotMatch(regex, 1L);
    assertDoesNotMatch(regex, 1L, 1234L);
    assertDoesNotMatch(regex, 1234L, 1L);
    assertDoesNotMatch(regex, 1L, 1234L, 1L);
    assertDoesNotMatch(regex, 1L, 2L, 3L, 4L);
  }

  @Test
  public void testMatchingZeroOrOne() {
    for (String regex : new String[] {"1234?", "1234{0,1}"}) {
      assertMatches(regex);
      assertMatches(regex, 1234L);
      assertDoesNotMatch(regex, 1234L, 1234L);
      assertDoesNotMatch(regex, 1L);
      assertDoesNotMatch(regex, 1L, 1234L);
      assertDoesNotMatch(regex, 1234L, 1L);
      assertDoesNotMatch(regex, 1L, 1234L, 1L);
      assertDoesNotMatch(regex, 1L, 2L, 3L, 4L);
    }
  }

  @Test
  public void testMatchingOneThroughFour() {
    String regex = "1234{1,4}";
    assertMatches(regex, 1234L);
    assertMatches(regex, 1234L, 1234L);
    assertMatches(regex, 1234L, 1234L, 1234L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 1234L, 1234L, 1234L, 1234L, 1234L);
    assertDoesNotMatch(regex, 1L, 1234L);
    assertDoesNotMatch(regex, 1234L, 1L);
    assertDoesNotMatch(regex, 1L, 1234L, 1L);
    assertDoesNotMatch(regex, 1L, 2L, 3L, 4L);
  }

  @Test
  public void testMatchingOneThroughFourPlusConcrete() {
    String regex = "12{1,4} 34";
    assertMatches(regex, 12L, 34L);
    assertMatches(regex, 12L, 12L, 34L);
    assertMatches(regex, 12L, 12L, 12L, 34L);
    assertMatches(regex, 12L, 12L, 12L, 12L, 34L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 34L);
    assertDoesNotMatch(regex, 12L, 12L, 12L, 12L, 12L, 34L);
    assertDoesNotMatch(regex, 34L, 12L);
  }

  @Test
  public void testMatchingRangeSingleAsNumber() {
    String regex = "123-125";
    assertMatches(regex, 123L);
    assertMatches(regex, 124L);
    assertMatches(regex, 125L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 122L);
    assertDoesNotMatch(regex, 126L);
    assertDoesNotMatch(regex, 123L, 123L);
  }

  @Test
  public void testMatchingRangeSingleAsNumberRepeated() {
    String regex = "[123-125]*";
    assertMatches(regex);
    assertMatches(regex, 123L);
    assertMatches(regex, 124L, 124L);
    assertMatches(regex, 125L, 125L, 125L);
    assertMatches(regex, 123L, 124L, 125L, 123L);
    assertDoesNotMatch(regex, 122L);
    assertDoesNotMatch(regex, 126L);
    assertDoesNotMatch(regex, 123L, 122L);
  }

  @Test
  public void testMatchingPathSecondAsNumberMustBe56Or78() {
    for (String regex : new String[] {"(. 56) | (. 78)", ". (56 | 78)"}) {
      assertMatches(regex, 1234L, 56L);
      assertMatches(regex, 1234L, 78L);
      assertMatches(regex, 9876L, 56L);
      assertMatches(regex, 3857L, 78L);
      assertDoesNotMatch(regex);
      assertDoesNotMatch(regex, 56L);
      assertDoesNotMatch(regex, 78L);
      assertDoesNotMatch(regex, 56L, 78L, 123L);
      assertDoesNotMatch(regex, 56L, 78L, 78L);
    }
  }

  /**
   * Note that this test differs from the examples on that page ("Path whose second AS number might
   * be 56 or 78" * with regex {@code ". (56 | 78)?"} should match {@code }"1234 56 52"}, among
   * others). The page seems wrong: * the given regex should only match paths of length 1 or 2 as
   * all regular expressions have an * implicit {@code ^} and {@code $}.
   */
  @Test
  public void testMatchingPathSecondAsMightBe56Or78() {
    String regex = "123 (56 | 78)?";
    assertMatches(regex, 123L);
    assertMatches(regex, 123L, 56L);
    assertMatches(regex, 123L, 78L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 56L);
    assertDoesNotMatch(regex, 78L);
    assertDoesNotMatch(regex, 123L, 57L);
    assertDoesNotMatch(regex, 123L, 56L, 78L);
  }

  @Test
  public void testMatchingPathFirstAsFixedSecondMustBe56Or78() {
    String regex = "123 (56 | 78)";
    assertMatches(regex, 123L, 56L);
    assertMatches(regex, 123L, 78L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 56L);
    assertDoesNotMatch(regex, 78L);
    assertDoesNotMatch(regex, 123L);
    assertDoesNotMatch(regex, 123L, 57L);
    assertDoesNotMatch(regex, 123L, 56L, 78L);
  }

  @Test
  public void testMatchingPathLengthAtLeastOne() {
    for (String regex : new String[] {". .*", ". .{0,}", ".{1,}"}) {
      assertMatches(regex, 123L);
      assertMatches(regex, 123L, 123L);
      assertMatches(regex, 123L, 123L, 123L);
      assertDoesNotMatch(regex);
    }
  }

  @Test
  public void testSimplePath() {
    String regex = "1 2 3";
    assertMatches(regex, 1L, 2L, 3L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 1L);
    assertDoesNotMatch(regex, 1L, 2L);
    assertDoesNotMatch(regex, 1L, 2L, 4L);
    assertDoesNotMatch(regex, 1L, 2L, 3L, 4L);
  }

  @Test
  public void testSimplePathWithPlus() {
    String regex = "1 2 3+";
    assertMatches(regex, 1L, 2L, 3L);
    assertMatches(regex, 1L, 2L, 3L, 3L);
    assertMatches(regex, 1L, 2L, 3L, 3L, 3L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 1L);
    assertDoesNotMatch(regex, 1L, 2L);
    assertDoesNotMatch(regex, 1L, 2L, 4L);
    assertDoesNotMatch(regex, 1L, 2L, 3L, 4L);
    assertDoesNotMatch(regex, 1L, 1L, 2L, 3L);
  }

  @Test
  public void testSimplePathWithAllPlus() {
    String regex = "1+ 2+ 3+";
    assertMatches(regex, 1L, 2L, 3L);
    assertMatches(regex, 1L, 1L, 2L, 3L);
    assertMatches(regex, 1L, 1L, 2L, 2L, 3L);
    assertMatches(regex, 1L, 1L, 2L, 2L, 3L, 3L);
    assertMatches(regex, 1L, 2L, 3L, 3L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 1L);
    assertDoesNotMatch(regex, 1L, 2L);
    assertDoesNotMatch(regex, 1L, 2L, 4L);
    assertDoesNotMatch(regex, 1L, 2L, 3L, 4L);
  }

  @Test
  public void testAnyLengthWithPrefix() {
    String regex = "1 2 3 .*";
    assertMatches(regex, 1L, 2L, 3L);
    assertMatches(regex, 1L, 2L, 3L, 3L);
    assertMatches(regex, 1L, 2L, 3L, 4L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 1L);
    assertDoesNotMatch(regex, 1L, 2L);
    assertDoesNotMatch(regex, 1L, 2L, 4L);
  }

  @Test
  public void testAnyLengthWithSuffix() {
    String regex = ".* 1 2 3";
    assertMatches(regex, 1L, 2L, 3L);
    assertMatches(regex, 1L, 1L, 2L, 3L);
    assertMatches(regex, 4L, 1L, 2L, 3L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 1L);
    assertDoesNotMatch(regex, 1L, 2L);
    assertDoesNotMatch(regex, 2L, 3L);
  }

  @Test
  public void testBaldOr() {
    String regex = "5 | 12 | 18";
    assertMatches(regex, 5L);
    assertMatches(regex, 12L);
    assertMatches(regex, 18L);
    assertDoesNotMatch(regex);
    assertDoesNotMatch(regex, 1L);
    assertDoesNotMatch(regex, 5L, 2L);
    assertDoesNotMatch(regex, 2L, 5L);
  }
}
