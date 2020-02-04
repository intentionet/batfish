package org.batfish.datamodel.applications;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import org.batfish.datamodel.SubRange;
import org.junit.Test;

public class IcmpTypesApplicationTest {

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            IcmpTypesApplication.ALL,
            // synonym of ALL
            new IcmpTypesApplication(ImmutableList.of(new SubRange(0, IcmpApplication.MAX_TYPE))))
        .addEqualityGroup(
            new IcmpTypesApplication(8),
            new IcmpTypesApplication(ImmutableList.of(SubRange.singleton(8))))
        .addEqualityGroup(new IcmpTypesApplication(ImmutableList.of(new SubRange(0, 8))))
        .addEqualityGroup(new IcmpTypesApplication(ImmutableList.of(new SubRange(8, 0))))
        .addEqualityGroup(
            new IcmpTypesApplication(
                ImmutableList.of(SubRange.singleton(0), SubRange.singleton(8))))
        .testEquals();
  }

  @Test
  public void testToString() {
    assertThat(IcmpTypesApplication.ALL.toString(), equalTo("icmp"));
    assertThat(new IcmpTypesApplication(8).toString(), equalTo("icmp/8"));
    assertThat(
        new IcmpTypesApplication(ImmutableList.of(new SubRange(0, 8))).toString(),
        equalTo("icmp/0-8"));
    assertThat(
        new IcmpTypesApplication(ImmutableList.of(SubRange.singleton(4), new SubRange(0, 8)))
            .toString(),
        equalTo("icmp/4,0-8"));
  }
}
