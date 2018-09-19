package org.batfish.specifier;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.batfish.datamodel.questions.FiltersSpecifier;
import org.batfish.specifier.InterfaceSpecifierFilterSpecifier.Type;
import org.junit.Test;

public class FlexibleFilterSpecifierFactoryTest {
  @Test
  public void testShorthandFilterSpecifier() {
    assertThat(
        new FlexibleFilterSpecifierFactory().buildFilterSpecifier("ipv4:.*"),
        equalTo(new ShorthandFilterSpecifier(new FiltersSpecifier("ipv4:.*"))));
  }

  @Test
  public void testInFilterOf() {
    assertThat(
        new FlexibleFilterSpecifierFactory().buildFilterSpecifier("inFilterOf(a)"),
        equalTo(
            new InterfaceSpecifierFilterSpecifier(
                Type.IN_FILTER,
                new FlexibleInterfaceSpecifierFactory().buildInterfaceSpecifier("a"))));
  }

  @Test
  public void testLoad() {
    assertTrue(
        FilterSpecifierFactory.load(FlexibleFilterSpecifierFactory.NAME)
            instanceof FlexibleFilterSpecifierFactory);
  }

  @Test
  public void testNull() {
    assertThat(
        new FlexibleFilterSpecifierFactory().buildFilterSpecifier(null),
        equalTo(new ShorthandFilterSpecifierFactory().buildFilterSpecifier(null)));
  }

  @Test
  public void testOutFilterOf() {
    assertThat(
        new FlexibleFilterSpecifierFactory().buildFilterSpecifier("outFilterOf(a)"),
        equalTo(
            new InterfaceSpecifierFilterSpecifier(
                Type.OUT_FILTER,
                new FlexibleInterfaceSpecifierFactory().buildInterfaceSpecifier("a"))));
  }

  @Test
  public void testRefFilterGroup() {
    assertThat(
        new FlexibleFilterSpecifierFactory().buildFilterSpecifier("rEf.FilterGroup(a, b)"),
        equalTo(new ReferenceFilterGroupFilterSpecifier("a", "b")));
  }
}
