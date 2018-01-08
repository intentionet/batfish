package org.batfish.datamodel;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public final class InterfaceMatchers {
  private static final class HasDeclaredNames extends FeatureMatcher<Interface, Set<String>> {
    private HasDeclaredNames(@Nonnull Matcher<? super Set<String>> subMatcher) {
      super(subMatcher, "declared names", "declared names");
    }

    @Override
    protected Set<String> featureValueOf(Interface actual) {
      return actual.getDeclaredNames();
    }
  }

  private static final class HasSourceNats extends FeatureMatcher<Interface, List<SourceNat>> {
    private HasSourceNats(@Nonnull Matcher<? super List<SourceNat>> subMatcher) {
      super(subMatcher, "sourceNats", "sourceNats");
    }

    @Override
    protected List<SourceNat> featureValueOf(Interface actual) {
      return actual.getSourceNats();
    }
  }

  private static final class IsActive extends FeatureMatcher<Interface, Boolean> {
    private IsActive(@Nonnull Matcher<? super Boolean> subMatcher) {
      super(subMatcher, "active", "active");
    }

    @Override
    protected Boolean featureValueOf(Interface actual) {
      return actual.getActive();
    }
  }

  public static HasDeclaredNames hasDeclaredNames(@Nonnull Iterable<String> expectedDeclaredNames) {
    return new HasDeclaredNames(
        containsInAnyOrder(ImmutableSet.copyOf(expectedDeclaredNames).toArray()));
  }

  public static HasDeclaredNames hasDeclaredNames(
      @Nonnull Matcher<? super Set<String>> subMatcher) {
    return new HasDeclaredNames(subMatcher);
  }

  public static HasDeclaredNames hasDeclaredNames(@Nonnull String... expectedDeclaredNames) {
    return new HasDeclaredNames(
        containsInAnyOrder(ImmutableSet.copyOf(expectedDeclaredNames).toArray()));
  }

  public static HasSourceNats hasSourceNats(@Nonnull Matcher<? super List<SourceNat>> subMatcher) {
    return new HasSourceNats(subMatcher);
  }

  public static IsActive isActive() {
    return new IsActive(equalTo(true));
  }

  public static IsActive isActive(@Nonnull Matcher<? super Boolean> subMatcher) {
    return new IsActive(subMatcher);
  }

  private InterfaceMatchers() {}
}
