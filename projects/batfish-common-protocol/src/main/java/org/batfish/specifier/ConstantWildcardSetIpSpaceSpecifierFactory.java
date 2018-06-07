package org.batfish.specifier;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import javax.annotation.Nonnull;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;

public class ConstantWildcardSetIpSpaceSpecifierFactory implements IpSpaceSpecifierFactory {
  public static final String NAME =
      ConstantWildcardSetIpSpaceSpecifierFactory.class.getSimpleName();

  @Override
  public String getName() {
    return NAME;
  }

  /**
   * Parse the following format w1,w2,...wn-wn+1,wn+2,... Wildcards to the left of - are whitelisted
   * (at least 1 required). Wildcards to the right of - (if present) are blacklisted. Can also use \
   * instead of -.
   */
  @VisibleForTesting
  static IpWildcardSetIpSpace parseIpSpace(@Nonnull String input) {
    String[] strs = input.split("-|\\\\");

    if (strs.length == 1) {
      return IpWildcardSetIpSpace.builder().including(parseWildcards(strs[0])).build();
    }
    if (strs.length == 2) {
      return IpWildcardSetIpSpace.builder()
          .including(parseWildcards(strs[0]))
          .excluding(parseWildcards(strs[1]))
          .build();
    }
    throw new IllegalArgumentException(
        "Error parsing IpWildcards: only 1 subtraction operator ('-' or '\\') allowed");
  }

  @VisibleForTesting
  static Iterable<IpWildcard> parseWildcards(@Nonnull String wildcardsStr) {
    String[] wildcardStrs = wildcardsStr.split(",");
    return Arrays.stream(wildcardStrs)
        .map(String::trim)
        .map(IpWildcard::new)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public IpSpaceSpecifier buildIpSpaceSpecifier(Object input) {
    checkArgument(input instanceof String, "String input required for " + getName());
    return new ConstantIpSpaceSpecifier(parseIpSpace((String) input));
  }
}
