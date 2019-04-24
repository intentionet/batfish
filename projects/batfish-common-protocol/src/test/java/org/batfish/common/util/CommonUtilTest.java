package org.batfish.common.util;

import static org.batfish.common.util.CommonUtil.asNegativeIpWildcards;
import static org.batfish.common.util.CommonUtil.asPositiveIpWildcards;
import static org.batfish.common.util.CommonUtil.toOrderedHashCode;
import static org.batfish.common.util.CommonUtil.toUnorderedHashCode;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.stream.Stream;
import org.junit.Test;

/** Tests of utility methods from {@link org.batfish.common.util.CommonUtil} */
public class CommonUtilTest {

  /** Test that asPositiveIpWildcards handles null */
  @Test
  public void testAsPositiveIpWildcards() {
    assertThat(asPositiveIpWildcards(null), nullValue());
  }

  /** Test that asNegativeIpWildcards handles null */
  @Test
  public void testAsNegativeIpWildcards() {
    assertThat(asNegativeIpWildcards(null), nullValue());
  }

  @Test
  public void testToOrderedHashCode() {
    assertThat(Stream.of().collect(toOrderedHashCode()), equalTo(ImmutableList.of().hashCode()));
    assertThat(Stream.of(1).collect(toOrderedHashCode()), equalTo(ImmutableList.of(1).hashCode()));
    assertThat(
        Stream.of(1, 2).collect(toOrderedHashCode()), equalTo(ImmutableList.of(1, 2).hashCode()));
    assertThat(
        Stream.of(2, 1).collect(toOrderedHashCode()), equalTo(ImmutableList.of(2, 1).hashCode()));
  }

  @Test
  public void testToUnorderedHashCode() {
    assertThat(Stream.of().collect(toUnorderedHashCode()), equalTo(ImmutableSet.of().hashCode()));
    assertThat(Stream.of(1).collect(toUnorderedHashCode()), equalTo(ImmutableSet.of(1).hashCode()));
    assertThat(
        Stream.of(1, 2).collect(toUnorderedHashCode()), equalTo(ImmutableSet.of(2, 1).hashCode()));
  }
}
