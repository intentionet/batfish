package org.batfish.specifier;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.service.AutoService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * A {@link FilterSpecifierFactory} that accepts three types of inputs:
 *
 * <ul>
 *   <li>null, which returns {@link ShorthandFilterSpecifier} that matches everything
 *   <li>inFilterOf(foo), which returns {@link InterfaceSpecifierFilterSpecifier} with foo being fed
 *       to {@link FlexibleInterfaceSpecifierFactory}
 *   <li>outFilterOf(foo), as above but for output filters
 *   <li>ref.filtergroup(foo, bar), which returns {@link ReferenceFilterGroupFilterSpecifier};
 *   <li>inputs accepted by {@link ShorthandFilterSpecifier}
 * </ul>
 */
@AutoService(FilterSpecifierFactory.class)
public class FlexibleFilterSpecifierFactory implements FilterSpecifierFactory {
  public static final String NAME = FlexibleFilterSpecifierFactory.class.getSimpleName();

  private static final Pattern IN_FILTER_OF_PATTERN =
      Pattern.compile("inFilterOf\\((.*)\\)", Pattern.CASE_INSENSITIVE);

  private static final Pattern OUT_FILTER_OF_PATTERN =
      Pattern.compile("outFilterOf\\((.*)\\)", Pattern.CASE_INSENSITIVE);

  private static final Pattern REF_PATTERN =
      Pattern.compile("ref\\.filtergroup\\((.*)\\)", Pattern.CASE_INSENSITIVE);

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public FilterSpecifier buildFilterSpecifier(@Nullable Object input) {
    if (input == null) {
      return new ShorthandFilterSpecifierFactory().buildFilterSpecifier(null);
    }
    checkArgument(input instanceof String, NAME + " requires String input");
    String str = ((String) input).trim();

    Matcher matcher = IN_FILTER_OF_PATTERN.matcher(str);
    if (matcher.find()) {
      InterfaceSpecifier specifier =
          new FlexibleInterfaceSpecifierFactory().buildInterfaceSpecifier(matcher.group(1));
      return new InterfaceSpecifierFilterSpecifier(
          InterfaceSpecifierFilterSpecifier.Type.IN_FILTER, specifier);
    }

    matcher = OUT_FILTER_OF_PATTERN.matcher(str);
    if (matcher.find()) {
      InterfaceSpecifier specifier =
          new FlexibleInterfaceSpecifierFactory().buildInterfaceSpecifier(matcher.group(1));
      return new InterfaceSpecifierFilterSpecifier(
          InterfaceSpecifierFilterSpecifier.Type.OUT_FILTER, specifier);
    }

    matcher = REF_PATTERN.matcher(str);
    if (matcher.find()) {
      return new ReferenceFilterGroupFilterSpecifierFactory()
          .buildFilterSpecifier(matcher.group(1));
    }
    return new ShorthandFilterSpecifierFactory().buildFilterSpecifier(str);
  }
}
