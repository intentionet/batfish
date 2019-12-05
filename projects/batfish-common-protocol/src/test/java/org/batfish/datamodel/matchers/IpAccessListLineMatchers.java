package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.Nonnull;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.matchers.IpAccessListLineMatchersImpl.HasAction;
import org.batfish.datamodel.matchers.IpAccessListLineMatchersImpl.HasMatchCondition;
import org.hamcrest.Matcher;

public class IpAccessListLineMatchers {

  /** Provides a matcher that matches if the provided {@code subMatcher} matches the line action. */
  public static HasAction hasAction(@Nonnull Matcher<? super LineAction> subMatcher) {
    return new HasAction(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * org.batfish.datamodel.IpAccessListLine}'s match condition. dstIps.
   */
  public static HasMatchCondition hasMatchCondition(
      @Nonnull Matcher<? super AclLineMatchExpr> subMatcher) {
    return new HasMatchCondition(subMatcher);
  }

  /**
   * Provides a matcher that matches if the provided {@code expr} equals the HeaderSpace's match
   * condition.
   */
  public static HasMatchCondition hasMatchCondition(@Nonnull AclLineMatchExpr expr) {
    return new HasMatchCondition(equalTo(expr));
  }

  private IpAccessListLineMatchers() {}
}
