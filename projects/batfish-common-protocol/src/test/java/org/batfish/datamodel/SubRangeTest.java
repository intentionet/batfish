package org.batfish.datamodel;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.testing.EqualsTester;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link org.batfish.datamodel.SubRange} */
@RunWith(JUnit4.class)
public class SubRangeTest {

  @Test
  public void testClosedInterval() {
    SubRange range = new SubRange(0, 100);
    for (int i = 0; i <= 100; i++) {
      assertThat(range.includes(i), equalTo(true));
    }
  }

  @Test
  public void testEquals() {
    SubRange group1Elem1 = new SubRange(0, 100);
    SubRange group1Elem2 = new SubRange(0, 100);

    SubRange group2Elem1 = new SubRange(1, 100);
    SubRange group3Elem1 = new SubRange(0, 99);

    new EqualsTester()
        .addEqualityGroup(group1Elem1, group1Elem2)
        .addEqualityGroup(group2Elem1)
        .addEqualityGroup(group3Elem1)
        .testEquals();
  }

  @Test
  public void testEmptyRange() {
    SubRange range = new SubRange(3, 1);
    assertThat(range.isEmpty(), equalTo(true));
    assertThat(range.getStart(), equalTo(3));
    assertThat(range.getEnd(), equalTo(1));
    assertThat(range.includes(1), equalTo(false));
    assertThat(range.includes(2), equalTo(false));
    assertThat(range.includes(3), equalTo(false));
  }

  @Test
  public void testContains() {
    SubRange empty = new SubRange(2, 1);
    SubRange base = new SubRange(0, 10);
    SubRange singleton = SubRange.singleton(5);

    assertThat(base.contains(empty), equalTo(true));
    assertThat(empty.contains(empty), equalTo(true));
    assertThat(empty.contains(base), equalTo(false));
    assertThat(singleton.contains(singleton), equalTo(true));
    assertThat(singleton.contains(empty), equalTo(true));

    assertThat(base.contains(new SubRange(2, 8)), equalTo(true));
    assertThat(base.contains(new SubRange(2, 10)), equalTo(true));
    assertThat(base.contains(new SubRange(-1, 5)), equalTo(false));
    assertThat(base.contains(new SubRange(-1, 11)), equalTo(false));
    assertThat(new SubRange(-1, 11).contains(base), equalTo(true));
  }

  @Test
  public void testIntersection() {
    SubRange range0To20 = new SubRange(0, 20);
    SubRange range3To7 = new SubRange(3, 7);
    SubRange range5To7 = new SubRange(5, 7);
    SubRange range5To10 = new SubRange(5, 10);
    SubRange range6To8 = new SubRange(6, 8);
    SubRange range8To20 = new SubRange(8, 20);
    SubRange range8To10 = new SubRange(8, 10);
    SubRange range15To20 = new SubRange(15, 20);
    assertThat(range5To10.intersection(range6To8), equalTo(Optional.of(range6To8)));
    assertThat(range5To10.intersection(range8To20), equalTo(Optional.of(range8To10)));
    assertThat(range5To10.intersection(range3To7), equalTo(Optional.of(range5To7)));
    assertThat(range5To10.intersection(range0To20), equalTo(Optional.of(range5To10)));
    assertThat(range5To10.intersection(range15To20), equalTo(Optional.empty()));
  }

  @Test
  public void isSingleValue() {
    SubRange empty = new SubRange(2, 1);
    SubRange base = new SubRange(0, 10);
    SubRange singleton = SubRange.singleton(5);

    assertThat(empty.isSingleValue(), equalTo(false));
    assertThat(base.isSingleValue(), equalTo(false));
    assertThat(singleton.isSingleValue(), equalTo(true));
  }
}
