package org.batfish.specifier;

import static org.batfish.specifier.FlexibleIpSpaceSpecifierFactory.parse;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link FlexibleIpSpaceSpecifierFactory}. */
@RunWith(JUnit4.class)
public class FlexibleIpSpaceSpecifierFactoryTest {
  @Rule public ExpectedException exception = ExpectedException.none();

  @Test
  public void testParse() {
    assertThat(
        parse("1.2.3.4"),
        equalTo(
            new ConstantIpSpaceSpecifier(
                IpWildcardSetIpSpace.builder().including(new IpWildcard("1.2.3.4")).build())));
    assertThat(
        parse("ref.addressgroup(foo,bar)"),
        equalTo(new ReferenceAddressGroupIpSpaceSpecifier("foo", "bar")));
    assertThat(
        parse("ofLocation(foo)"),
        equalTo(new LocationIpSpaceSpecifier(FlexibleLocationSpecifierFactory.parse("foo"))));
  }
}
