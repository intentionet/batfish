package org.batfish.datamodel.matchers;

import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.vendor.StructureType;
import org.batfish.vendor.StructureUsage;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

final class ConvertConfigurationAnswerElementMatchers {

  static final class HasUndefinedReference
      extends TypeSafeDiagnosingMatcher<ConvertConfigurationAnswerElement> {

    private final String _hostname;

    private final String _structureName;

    private final String _type;

    HasUndefinedReference(
        @Nonnull String hostname, @Nonnull StructureType type, @Nonnull String structureName) {
      _hostname = hostname;
      _type = type.getDescription();
      _structureName = structureName;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(
          String.format(
              "A ConvertConfigurationAnswerElement for which host '%s' has an undefined reference "
                  + "to a structure of type '%s' named '%s'",
              _hostname, _type, _structureName));
    }

    @Override
    protected boolean matchesSafely(
        ConvertConfigurationAnswerElement item, Description mismatchDescription) {
      SortedMap<String, SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>>>
          byHostname = item.getUndefinedReferences();
      if (!byHostname.containsKey(_hostname)) {
        mismatchDescription.appendText(
            String.format("Host '%s' has no undefined references", _hostname));
        return false;
      }
      SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>> byType =
          byHostname.get(_hostname);
      if (!byType.containsKey(_type)) {
        mismatchDescription.appendText(
            String.format(
                "Host '%s' has no undefined references to structures of type '%s'",
                _hostname, _type));
        return false;
      }
      SortedMap<String, SortedMap<String, SortedSet<Integer>>> byStructureName = byType.get(_type);
      if (!byStructureName.containsKey(_structureName)) {
        mismatchDescription.appendText(
            String.format(
                "Host '%s' has no undefined references to structures of type '%s' named '%s'",
                _hostname, _type, _structureName));
        return false;
      }
      return true;
    }
  }

  static final class HasUndefinedReferenceWithUsage
      extends TypeSafeDiagnosingMatcher<ConvertConfigurationAnswerElement> {

    private final String _hostname;

    private final String _structureName;

    private final String _type;

    private final String _usage;

    HasUndefinedReferenceWithUsage(
        @Nonnull String hostname,
        @Nonnull StructureType type,
        @Nonnull String structureName,
        @Nonnull StructureUsage usage) {
      _hostname = hostname;
      _type = type.getDescription();
      _structureName = structureName;
      _usage = usage.getDescription();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(
          String.format(
              "A ConvertConfigurationAnswerElement for which host '%s' has an undefined reference "
                  + "to a structure of type '%s' named '%s'",
              _hostname, _type, _structureName));
    }

    @Override
    protected boolean matchesSafely(
        ConvertConfigurationAnswerElement item, Description mismatchDescription) {
      SortedMap<String, SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>>>
          byHostname = item.getUndefinedReferences();
      if (!byHostname.containsKey(_hostname)) {
        mismatchDescription.appendText(
            String.format("Host '%s' has no undefined references", _hostname));
        return false;
      }
      SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>> byType =
          byHostname.get(_hostname);
      if (!byType.containsKey(_type)) {
        mismatchDescription.appendText(
            String.format(
                "Host '%s' has no undefined references to structures of type '%s'",
                _hostname, _type));
        return false;
      }
      SortedMap<String, SortedMap<String, SortedSet<Integer>>> byStructureName = byType.get(_type);
      if (!byStructureName.containsKey(_structureName)) {
        mismatchDescription.appendText(
            String.format(
                "Host '%s' has no undefined references to structures of type '%s' named '%s'",
                _hostname, _type, _structureName));
        return false;
      }
      SortedMap<String, SortedSet<Integer>> byUsage = byStructureName.get(_structureName);
      if (!byUsage.containsKey(_usage)) {
        mismatchDescription.appendText(
            String.format(
                "Host '%s' has no undefined references to structures of type '%s' named '%s' of "
                    + "usage '%s'",
                _hostname, _type, _structureName, _usage));
        return false;
      }
      return true;
    }
  }

  static final class HasNumReferrers
      extends TypeSafeDiagnosingMatcher<ConvertConfigurationAnswerElement> {

    private final String _hostname;

    private final int _numReferrers;

    private final String _structureName;

    private final String _type;

    HasNumReferrers(
        @Nonnull String hostname,
        @Nonnull StructureType type,
        @Nonnull String structureName,
        int numReferrers) {
      _hostname = hostname;
      _numReferrers = numReferrers;
      _type = type.getDescription();
      _structureName = structureName;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(
          String.format(
              "A ConvertConfigurationAnswerElement for which host '%s' has defined structure of "
                  + "type '%s' named '%s' with %d referrers",
              _hostname, _type, _structureName, _numReferrers));
    }

    @Override
    protected boolean matchesSafely(
        ConvertConfigurationAnswerElement item, Description mismatchDescription) {
      SortedMap<String, SortedMap<String, SortedMap<String, DefinedStructureInfo>>> byHostname =
          item.getDefinedStructures();
      if (!byHostname.containsKey(_hostname)) {
        mismatchDescription.appendText(
            String.format("Host '%s' has no defined structures", _hostname));
        return false;
      }
      SortedMap<String, SortedMap<String, DefinedStructureInfo>> byType = byHostname.get(_hostname);
      if (!byType.containsKey(_type)) {
        mismatchDescription.appendText(
            String.format("Host '%s' has no defined structures of type '%s'", _hostname, _type));
        return false;
      }
      SortedMap<String, DefinedStructureInfo> byStructureName = byType.get(_type);
      if (!byStructureName.containsKey(_structureName)) {
        mismatchDescription.appendText(
            String.format(
                "Host '%s' has no defined structures of type '%s' named '%s'",
                _hostname, _type, _structureName));
        return false;
      }
      if (byStructureName.get(_structureName).getNumReferrers() != _numReferrers) {
        mismatchDescription.appendText(
            String.format(
                "On host '%s', defined structure of type '%s' named '%s' has %d referrers",
                _hostname,
                _type,
                _structureName,
                byStructureName.get(_structureName).getNumReferrers()));
        return false;
      }
      return true;
    }
  }

  private ConvertConfigurationAnswerElementMatchers() {}
}
