package org.batfish.dataplane.ibdp;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.util.List;
import org.junit.Test;

/** Tests of {@link org.batfish.dataplane.ibdp.CrossVrfEdgeId} */
public final class CrossVrfEdgeIdTest {
  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(new CrossVrfEdgeId("vrf", "rib"), new CrossVrfEdgeId("vrf", "rib"))
        .addEqualityGroup(new CrossVrfEdgeId("vrf2", "rib"))
        .addEqualityGroup(new CrossVrfEdgeId("vrf", "rib2"))
        .testEquals();
  }

  @Test
  public void testComparator() {
    List<CrossVrfEdgeId> ordered =
        ImmutableList.of(
            new CrossVrfEdgeId("a", "a"),
            new CrossVrfEdgeId("a", "b"),
            new CrossVrfEdgeId("a", "c"),
            new CrossVrfEdgeId("b", "a"));
    for (int i = 0; i < ordered.size(); i++) {
      for (int j = 0; j < ordered.size(); j++) {
        assertThat(
            Integer.signum(ordered.get(i).compareTo(ordered.get(j))),
            equalTo(Integer.signum(i - j)));
      }
    }
  }
}
