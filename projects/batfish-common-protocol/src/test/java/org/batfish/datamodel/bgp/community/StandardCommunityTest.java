package org.batfish.datamodel.bgp.community;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;

import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests of {@link StandardCommunity} */
public class StandardCommunityTest {
  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testEquals() {
    StandardCommunity sc = StandardCommunity.of(1L);
    new EqualsTester()
        .addEqualityGroup(sc, sc, StandardCommunity.of(1L), StandardCommunity.of(0, 1))
        .addEqualityGroup(StandardCommunity.of(2))
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testBoundaryConditions() {
    assertThat(StandardCommunity.of(65535, 65535), equalTo(StandardCommunity.of(4294967295L)));
  }

  @Test
  public void testOfNegative() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.of(-1);
  }

  @Test
  public void testOfTooLarge() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.of(1L << 32);
  }

  @Test
  public void testOfTwoFirstNegative() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.of(-1, 2);
  }

  @Test
  public void testOfTwoSecondNegative() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.of(23, -1);
  }

  @Test
  public void testOfTwoFirstTooLarge() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.of(1 << 16, 1);
  }

  @Test
  public void testOfTwoSecondTooLarge() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.of(1, 1 << 16);
  }

  @Test
  public void testJavaSerialization() {
    assertThat(SerializationUtils.clone(StandardCommunity.of(1)), equalTo(StandardCommunity.of(1)));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    assertThat(
        BatfishObjectMapper.clone(StandardCommunity.of(1), StandardCommunity.class),
        equalTo(StandardCommunity.of(1)));
  }

  @Test
  public void testParse() {
    assertThat(StandardCommunity.parse("0:1"), equalTo(StandardCommunity.of(1)));
    assertThat(StandardCommunity.parse("1:1"), equalTo(StandardCommunity.of(65537)));
  }

  @Test
  public void testParseGarbage() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.parse("0:x");
  }

  @Test
  public void testParseToMany() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.parse("1:1:1");
  }

  @Test
  public void testParseFirstNegative() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.parse("-1:1");
  }

  @Test
  public void testParseSecondNegative() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.parse("1:-1");
  }

  @Test
  public void testParseFirstTooLarge() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.parse("65555:1");
  }

  @Test
  public void testParseSecondTooLarge() {
    _thrown.expect(IllegalArgumentException.class);
    StandardCommunity.parse("1:65555");
  }

  @Test
  public void testToString() {
    assertThat(StandardCommunity.of(1, 1).toString(), equalTo("1:1"));
  }

  @Test
  public void testMatchString() {
    StandardCommunity sc = StandardCommunity.parse("2:3");
    assertThat(sc.toString(), equalTo("2:3"));
    assertThat(sc.matchString(), equalTo(sc.toString()));
  }

  @Test
  public void testNotTransitive() {
    assertFalse(StandardCommunity.of(1, 1).isTransitive());
  }
}
