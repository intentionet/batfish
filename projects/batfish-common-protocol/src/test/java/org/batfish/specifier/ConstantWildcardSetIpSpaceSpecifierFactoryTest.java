package org.batfish.specifier;

import static org.batfish.specifier.ConstantWildcardSetIpSpaceSpecifierFactory.parseIpSpace;
import static org.batfish.specifier.ConstantWildcardSetIpSpaceSpecifierFactory.parseWildcards;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConstantWildcardSetIpSpaceSpecifierFactoryTest {
  @Rule public final ExpectedException exception = ExpectedException.none();

  @Test
  public void testParseIpSpace_empty() {
    exception.expect(IllegalArgumentException.class);
    parseIpSpace("");
  }

  @Test
  public void testParseIpSpace_whitelist() {
    assertThat(
        parseIpSpace("1.0.0.0,2.0.0.0/8"),
        equalTo(
            IpWildcardSetIpSpace.builder()
                .including(ImmutableList.of(new IpWildcard("1.0.0.0"), new IpWildcard("2.0.0.0/8")))
                .build()));
  }

  @Test
  public void testParseIpSpace_blacklist() {
    assertThat(
        parseIpSpace("1.0.0.0,2.0.0.0/8 - 3.0.0.0,4.0.0.0/32"),
        equalTo(
            IpWildcardSetIpSpace.builder()
                .including(ImmutableList.of(new IpWildcard("1.0.0.0"), new IpWildcard("2.0.0.0/8")))
                .excluding(
                    ImmutableList.of(new IpWildcard("3.0.0.0"), new IpWildcard("4.0.0.0/32")))
                .build()));
  }

  @Test
  public void testParseIpSpace_blacklist_backslash() {
    assertThat(
        parseIpSpace("1.0.0.0,2.0.0.0/8 \\ 3.0.0.0,4.0.0.0/32"),
        equalTo(
            IpWildcardSetIpSpace.builder()
                .including(ImmutableList.of(new IpWildcard("1.0.0.0"), new IpWildcard("2.0.0.0/8")))
                .excluding(
                    ImmutableList.of(new IpWildcard("3.0.0.0"), new IpWildcard("4.0.0.0/32")))
                .build()));
  }

  @Test
  public void testParseIpSpace_tooManySubtractions() {
    exception.expect(IllegalArgumentException.class);
    parseIpSpace("1.0.0.0 - 2.0.0.0/8 \\ 3.0.0.0 \\ 4.0.0.0/32");
  }

  @Test
  public void testParseWildcards_zero() {
    exception.expect(IllegalArgumentException.class);
    parseWildcards("");
  }

  @Test
  public void testParseWildcards_one() {
    assertThat(parseWildcards("1.0.0.0/8"), equalTo(ImmutableList.of(new IpWildcard("1.0.0.0/8"))));
  }

  @Test
  public void testParseWildcards_two() {
    assertThat(
        parseWildcards("1.0.0.0/8, 2.0.0.0/8"),
        equalTo(ImmutableList.of(new IpWildcard("1.0.0.0/8"), new IpWildcard("2.0.0.0/8"))));
  }
}
