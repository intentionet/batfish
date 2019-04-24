package org.batfish.symbolic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.util.List;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.junit.Test;

/** Test of {@link CommunityVar} */
public class CommunityVarTest {
  @Test
  public void testEquals() {
    CommunityVar cv = CommunityVar.from(StandardCommunity.of(1, 1));
    new EqualsTester()
        .addEqualityGroup(cv, cv, CommunityVar.from(StandardCommunity.of(1, 1)))
        .addEqualityGroup(CommunityVar.from("1:1"))
        .addEqualityGroup(CommunityVar.other("1:1"))
        .addEqualityGroup(CommunityVar.from(StandardCommunity.of(1, 2)))
        .addEqualityGroup(CommunityVar.from("1:2"))
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testCompare() {
    List<CommunityVar> ordered =
        ImmutableList.of(
            CommunityVar.from(StandardCommunity.of(1, 1)),
            CommunityVar.from(StandardCommunity.of(1, 2)),
            CommunityVar.from("1:1"),
            CommunityVar.from("1:2"),
            CommunityVar.other("1:1"),
            CommunityVar.other("1:2"));

    for (int i = 0; i < ordered.size(); i++) {
      for (int j = 0; j < ordered.size(); j++) {
        assertThat(
            Integer.signum(ordered.get(i).compareTo(ordered.get(j))),
            equalTo(Integer.signum(i - j)));
      }
    }
  }
}
