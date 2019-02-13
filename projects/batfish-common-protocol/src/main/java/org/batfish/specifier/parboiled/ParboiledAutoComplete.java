package org.batfish.specifier.parboiled;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.CompletionMetadata;
import org.batfish.datamodel.answers.AutoCompleteUtils;
import org.batfish.datamodel.answers.AutocompleteSuggestion;
import org.batfish.datamodel.questions.Variable.Type;
import org.batfish.referencelibrary.ReferenceLibrary;
import org.batfish.role.NodeRolesData;
import org.parboiled.Rule;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

/** A helper class that provides auto complete suggestions */
@ParametersAreNonnullByDefault
public final class ParboiledAutoComplete {

  public static final int RANK_STRING_LITERAL = 1;

  private final CommonParser _parser;
  private final Rule _expression;
  private final Map<String, Completion.Type> _completionTypes;

  private final String _network;
  private final String _snapshot;
  private final String _query;
  private final int _maxSuggestions;
  private final CompletionMetadata _completionMetadata;
  private final NodeRolesData _nodeRolesData;
  private final ReferenceLibrary _referenceLibrary;

  ParboiledAutoComplete(
      CommonParser parser,
      Rule expression,
      Map<String, Completion.Type> completionTypes,
      String network,
      String snapshot,
      String query,
      int maxSuggestions,
      CompletionMetadata completionMetadata,
      NodeRolesData nodeRolesData,
      ReferenceLibrary referenceLibrary) {
    _parser = parser;
    _expression = expression;
    _completionTypes = completionTypes;
    _network = network;
    _snapshot = snapshot;
    _query = query;
    _maxSuggestions = maxSuggestions;
    _completionMetadata = completionMetadata;
    _nodeRolesData = nodeRolesData;
    _referenceLibrary = referenceLibrary;
  }

  /** Auto completes IpSpace queries */
  public static List<AutocompleteSuggestion> autoCompleteIpSpace(
      String network,
      String snapshot,
      String query,
      int maxSuggestions,
      CompletionMetadata completionMetadata,
      NodeRolesData nodeRolesData,
      ReferenceLibrary referenceLibrary) {
    return new ParboiledAutoComplete(
            Parser.INSTANCE,
            Parser.INSTANCE.IpSpaceExpression(),
            Parser.COMPLETION_TYPES,
            network,
            snapshot,
            query,
            maxSuggestions,
            completionMetadata,
            nodeRolesData,
            referenceLibrary)
        .run();
  }

  /** This is the entry point for all auto completions */
  List<AutocompleteSuggestion> run() {

    /**
     * Before passing the query to the parser, we make it illegal by adding a funny, non-ascii
     * character (soccer ball :)). We will not get any errors backs if the string is legal.
     */
    String testQuery = _query + new String(Character.toChars(0x26bd));
    ParsingResult<AstNode> result =
        new ReportingParseRunner<AstNode>(_parser.input(_expression)).run(testQuery);
    if (result.parseErrors.isEmpty()) {
      throw new IllegalStateException("Failed to force erroneous input");
    }

    InvalidInputError error = (InvalidInputError) result.parseErrors.get(0);

    Set<PartialMatch> partialMatches = ParserUtils.getPartialMatches(error, _completionTypes);

    // first add string literals and then add others to the list. we do this because there can be
    // many suggestions based on dynamic completion (e.g., all nodes in the snapshot) and we do not
    // want them to drown everything else out
    List<AutocompleteSuggestion> suggestions =
        partialMatches.stream()
            .filter(pm -> pm.getCompletionType().equals(Completion.Type.STRING_LITERAL))
            .map(pm -> autoCompletePartialMatch(pm, error.getStartIndex()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    suggestions.addAll(
        partialMatches.stream()
            .filter(pm -> !pm.getCompletionType().equals(Completion.Type.STRING_LITERAL))
            .map(pm -> autoCompletePartialMatch(pm, error.getStartIndex()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));

    return suggestions;
  }

  @VisibleForTesting
  List<AutocompleteSuggestion> autoCompletePartialMatch(PartialMatch pm, int startIndex) {

    List<AutocompleteSuggestion> suggestions = null;
    switch (pm.getCompletionType()) {
      case STRING_LITERAL:
        return ImmutableList.of(
            new AutocompleteSuggestion(
                pm.getMatchCompletion(), true, null, RANK_STRING_LITERAL, startIndex));
      case ADDRESS_GROUP_AND_BOOK:
        suggestions =
            AutoCompleteUtils.autoComplete(
                _network,
                _snapshot,
                Type.ADDRESS_GROUP_AND_BOOK,
                pm.getMatchPrefix(),
                _maxSuggestions,
                _completionMetadata,
                _nodeRolesData,
                _referenceLibrary);
        break;
      case IP_ADDRESS:
        suggestions =
            AutoCompleteUtils.autoComplete(
                _network,
                _snapshot,
                Type.IP,
                pm.getMatchPrefix(),
                _maxSuggestions,
                _completionMetadata,
                _nodeRolesData,
                _referenceLibrary);
        break;
      case IP_PREFIX:
        suggestions =
            AutoCompleteUtils.autoComplete(
                _network,
                _snapshot,
                Type.PREFIX,
                pm.getMatchPrefix(),
                _maxSuggestions,
                _completionMetadata,
                _nodeRolesData,
                _referenceLibrary);
        break;
      case IP_RANGE: // IP_ADDRESS take care of this case
      case IP_WILDCARD: // IP_ADDRESS takes care of this case
      case EOI:
      default: // ignore things we do not know how to auto complete
        return ImmutableList.of();
    }
    return suggestions.stream()
        .map(
            s ->
                new AutocompleteSuggestion(
                    s.getText(),
                    true,
                    s.getDescription(),
                    AutocompleteSuggestion.DEFAULT_RANK,
                    startIndex))
        .collect(ImmutableList.toImmutableList());
  }
}
