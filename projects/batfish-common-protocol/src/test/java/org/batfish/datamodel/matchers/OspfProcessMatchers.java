package org.batfish.datamodel.matchers;

import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.datamodel.IpLink;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.OspfNeighbor;
import org.batfish.datamodel.matchers.OspfProcessMatchersImpl.HasArea;
import org.batfish.datamodel.matchers.OspfProcessMatchersImpl.HasAreas;
import org.batfish.datamodel.matchers.OspfProcessMatchersImpl.HasOspfNeighbors;
import org.hamcrest.Matcher;

public class OspfProcessMatchers {

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the OSPF process's
   * area with specified id.
   */
  public static HasArea hasArea(long id, @Nonnull Matcher<? super OspfArea> subMatcher) {
    return new HasArea(id, subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the OSPF process's
   * areas.
   */
  public static HasAreas hasAreas(@Nonnull Matcher<? super Map<Long, OspfArea>> subMatcher) {
    return new HasAreas(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the OSPF process's
   * OSPF neighbors.
   */
  public static HasOspfNeighbors hasOspfNeighbors(
      Matcher<? super Map<IpLink, OspfNeighbor>> subMatcher) {
    return new HasOspfNeighbors(subMatcher);
  }
}
