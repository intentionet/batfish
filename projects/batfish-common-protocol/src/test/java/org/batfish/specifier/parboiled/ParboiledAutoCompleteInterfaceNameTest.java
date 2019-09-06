package org.batfish.specifier.parboiled;

import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_CONNECTED_TO;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_NAME_REGEX;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_PARENS;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_TYPE;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_VRF;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_ZONE;
import static org.batfish.specifier.parboiled.Anchor.Type.LOCATION_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_AND_INTERFACE_TAIL;
import static org.batfish.specifier.parboiled.Anchor.Type.OPERATOR_END;
import static org.batfish.specifier.parboiled.Anchor.Type.REFERENCE_BOOK_AND_INTERFACE_GROUP;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.batfish.common.CompletionMetadata;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.referencelibrary.ReferenceLibrary;
import org.batfish.role.NodeRolesData;
import org.batfish.specifier.parboiled.Anchor.Type;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for interface name auto completion in {@link ParboiledAutoComplete}. The testing strategy
 * is in three parts:
 *
 * <ol>
 *   <li>testFindPreceding* tests in {@link ParboiledAutoCompleteTest} check that we are correctly
 *       extracting the preceding component (by explicitly setting up PotentialMatch object based on
 *       what we expect Parboiled to do on our grammars);
 *   <li>testAutoCompleteInterfaceName* tests below check that we do the right thing when nodeInput
 *       is given
 *   <li>testE2e* tests below check that the machinery works end-to-end on different grammars and
 *       potential partial things (thus helping validate the assumptions made in testFind*)
 * </ol>
 */
public class ParboiledAutoCompleteInterfaceNameTest {

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static CompletionMetadata testCompletionMetadata =
      CompletionMetadata.builder()
          .setInterfaces(
              ImmutableSet.of(
                  NodeInterfacePair.of("n1a", "eth11"),
                  NodeInterfacePair.of("n1a", "eth12"),
                  NodeInterfacePair.of("n2a", "eth21")))
          .build();

  private static ParboiledAutoComplete getPAC(String query) {
    return getPAC(query, Grammar.INTERFACE_SPECIFIER);
  }

  private static ParboiledAutoComplete getPAC(String query, Grammar grammar) {
    return getPAC(Parser.instance(), query, grammar, testCompletionMetadata);
  }

  private static ParboiledAutoComplete getPAC(
      Parser parser, String query, Grammar grammar, CompletionMetadata completionMetadata) {
    return new ParboiledAutoComplete(
        parser,
        grammar,
        Parser.ANCHORS,
        "network",
        "snapshot",
        query,
        Integer.MAX_VALUE,
        completionMetadata,
        NodeRolesData.builder().build(),
        new ReferenceLibrary(null));
  }

  /** Context-sensitive completion of interface name after node name or regex */
  @Test
  public void testAutoCompleteInterfaceNameNodeNameOrRegex() {
    Set<ParboiledAutoCompleteSuggestion> n1interfaces =
        ImmutableSet.of(
            new ParboiledAutoCompleteSuggestion("eth11", 42, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth12", 42, INTERFACE_NAME));

    PathElement anchor = new PathElement(Type.INTERFACE_NAME, null, 1, 42);
    PotentialMatch pm = new PotentialMatch(anchor, "", ImmutableList.of());

    // node name
    assertThat(getPAC("dummy").autoCompleteInterfaceName(pm, "n1a"), equalTo(n1interfaces));

    // node regex
    assertThat(getPAC("dummy").autoCompleteInterfaceName(pm, "/n1/"), equalTo(n1interfaces));

    // node name does not match
    assertThat(getPAC("dummy").autoCompleteInterfaceName(pm, "nono"), equalTo(ImmutableSet.of()));
  }

  /** Fall back to all interfaces for complex node expressions */
  @Test
  public void testAutoCompleteInterfaceNameComplexNodeExpression() {
    PathElement anchor = new PathElement(Type.INTERFACE_NAME, null, 1, 42);
    PotentialMatch pm = new PotentialMatch(anchor, "", ImmutableList.of());

    assertThat(
        getPAC("dummy").autoCompleteInterfaceName(pm, "@role(a, b)"),
        containsInAnyOrder(
            new ParboiledAutoCompleteSuggestion("eth11", 42, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth12", 42, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth21", 42, INTERFACE_NAME)));
  }

  /** Context-sensitive completion of interface name when interface name prefix is present */
  @Test
  public void testAutoCompleteInterfaceNameInterfaceNamePrefix() {
    PathElement anchor = new PathElement(Type.INTERFACE_NAME, null, 1, 42);

    assertThat(
        getPAC("dummy")
            .autoCompleteInterfaceName(
                new PotentialMatch(anchor, "eth12", ImmutableList.of()), "n1a"),
        containsInAnyOrder(new ParboiledAutoCompleteSuggestion("eth12", 42, INTERFACE_NAME)));

    // now with quotes, which should be preserved
    assertThat(
        getPAC("dummy")
            .autoCompleteInterfaceName(
                new PotentialMatch(anchor, "\"eth12", ImmutableList.of()), "n1a"),
        containsInAnyOrder(new ParboiledAutoCompleteSuggestion("\"eth12\"", 42, INTERFACE_NAME)));
  }

  /**
   * The interface portion hasn't begun yet. We should suggest starting that and offer any matching
   * interface names
   */
  @Test
  public void testE2eNoTailNoMatchingInterface() {
    String query = "noeth";
    assertThat(
        getPAC(query, Grammar.INTERFACE_SPECIFIER).run(),
        containsInAnyOrder(
            new ParboiledAutoCompleteSuggestion("[", query.length(), NODE_AND_INTERFACE_TAIL),
            new ParboiledAutoCompleteSuggestion("&", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion(",", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion("\\", query.length(), INTERFACE_SET_OP)));

    // now as location specifier
    assertThat(
        getPAC(query, Grammar.LOCATION_SPECIFIER).run(),
        containsInAnyOrder(
            new ParboiledAutoCompleteSuggestion("[", query.length(), NODE_AND_INTERFACE_TAIL),
            new ParboiledAutoCompleteSuggestion("&", query.length(), LOCATION_SET_OP),
            new ParboiledAutoCompleteSuggestion(",", query.length(), LOCATION_SET_OP),
            new ParboiledAutoCompleteSuggestion("\\", query.length(), LOCATION_SET_OP)));
  }

  @Test
  public void testE2eNoTailMatchingInterface() {
    String query = "eth1"; // matching interface
    assertThat(
        getPAC(query, Grammar.INTERFACE_SPECIFIER).run(),
        containsInAnyOrder(
            new ParboiledAutoCompleteSuggestion("eth11", 0, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth12", 0, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("[", query.length(), NODE_AND_INTERFACE_TAIL),
            new ParboiledAutoCompleteSuggestion("&", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion(",", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion("\\", query.length(), INTERFACE_SET_OP)));

    // now as location specifier. interface name suggestions are missing because location grammar
    // expects node names first and tries to match those names
    assertThat(
        getPAC(query, Grammar.LOCATION_SPECIFIER).run(),
        containsInAnyOrder(
            new ParboiledAutoCompleteSuggestion("[", query.length(), NODE_AND_INTERFACE_TAIL),
            new ParboiledAutoCompleteSuggestion("&", query.length(), LOCATION_SET_OP),
            new ParboiledAutoCompleteSuggestion(",", query.length(), LOCATION_SET_OP),
            new ParboiledAutoCompleteSuggestion("\\", query.length(), LOCATION_SET_OP)));
  }

  /** The interface portion has started but no prefix yet */
  @Test
  public void testE2eNoInterfacePrefix() {
    String query = "n1a[";

    Set<ParboiledAutoCompleteSuggestion> expected =
        ImmutableSet.of(
            new ParboiledAutoCompleteSuggestion("eth11", query.length(), INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth12", query.length(), INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("(", query.length(), INTERFACE_PARENS),
            new ParboiledAutoCompleteSuggestion("/", query.length(), INTERFACE_NAME_REGEX),
            new ParboiledAutoCompleteSuggestion(
                "@connectedTo(", query.length(), INTERFACE_CONNECTED_TO),
            new ParboiledAutoCompleteSuggestion(
                "@interfaceGroup(", query.length(), REFERENCE_BOOK_AND_INTERFACE_GROUP),
            new ParboiledAutoCompleteSuggestion("@interfaceType(", query.length(), INTERFACE_TYPE),
            new ParboiledAutoCompleteSuggestion("@vrf(", query.length(), INTERFACE_VRF),
            new ParboiledAutoCompleteSuggestion("@zone(", query.length(), INTERFACE_ZONE));

    assertThat(getPAC(query, Grammar.INTERFACE_SPECIFIER).run(), equalTo(expected));

    // location specifier yields identical results
    assertThat(getPAC(query, Grammar.LOCATION_SPECIFIER).run(), equalTo(expected));
  }

  /** The interface portion has started and we also have a prefix */
  @Test
  public void testE2eInterfacePrefix() {
    String query = "n1a[eth1";

    Set<ParboiledAutoCompleteSuggestion> expected =
        ImmutableSet.of(
            new ParboiledAutoCompleteSuggestion("eth11", 4, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth12", 4, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("]", query.length(), OPERATOR_END),
            new ParboiledAutoCompleteSuggestion("&", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion(",", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion("\\", query.length(), INTERFACE_SET_OP));

    assertThat(getPAC(query, Grammar.INTERFACE_SPECIFIER).run(), equalTo(expected));

    // location specifier yields identical results
    assertThat(getPAC(query, Grammar.LOCATION_SPECIFIER).run(), equalTo(expected));
  }

  /** Test that we do generic stuff when node expression is complex */
  @Test
  public void testE2eComplexNode() {
    String query = "@role(a, b)[eth";

    Set<ParboiledAutoCompleteSuggestion> expected =
        ImmutableSet.of(
            new ParboiledAutoCompleteSuggestion("eth11", 12, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth12", 12, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth21", 12, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("]", query.length(), OPERATOR_END),
            new ParboiledAutoCompleteSuggestion("&", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion(",", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion("\\", query.length(), INTERFACE_SET_OP));

    assertThat(getPAC(query, Grammar.INTERFACE_SPECIFIER).run(), equalTo(expected));

    // location specifier yields identical results
    assertThat(getPAC(query, Grammar.LOCATION_SPECIFIER).run(), equalTo(expected));
  }

  /** The interface portion has started but no prefix yet */
  @Test
  public void testE2eLocationNoPrefix() {
    String query = "@enter(n1a[";

    Set<ParboiledAutoCompleteSuggestion> expected =
        ImmutableSet.of(
            new ParboiledAutoCompleteSuggestion("eth11", query.length(), INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth12", query.length(), INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("(", query.length(), INTERFACE_PARENS),
            new ParboiledAutoCompleteSuggestion("/", query.length(), INTERFACE_NAME_REGEX),
            new ParboiledAutoCompleteSuggestion(
                "@connectedTo(", query.length(), INTERFACE_CONNECTED_TO),
            new ParboiledAutoCompleteSuggestion(
                "@interfaceGroup(", query.length(), REFERENCE_BOOK_AND_INTERFACE_GROUP),
            new ParboiledAutoCompleteSuggestion("@interfaceType(", query.length(), INTERFACE_TYPE),
            new ParboiledAutoCompleteSuggestion("@vrf(", query.length(), INTERFACE_VRF),
            new ParboiledAutoCompleteSuggestion("@zone(", query.length(), INTERFACE_ZONE));

    assertThat(getPAC(query, Grammar.LOCATION_SPECIFIER).run(), equalTo(expected));
  }

  /** The interface portion has started and we also have a prefix */
  @Test
  public void testE2eLocationPrefix() {
    String query = "@enter(n1a[eth1";

    Set<ParboiledAutoCompleteSuggestion> expected =
        ImmutableSet.of(
            new ParboiledAutoCompleteSuggestion("eth11", 11, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth12", 11, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("])", query.length(), OPERATOR_END),
            new ParboiledAutoCompleteSuggestion("&", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion(",", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion("\\", query.length(), INTERFACE_SET_OP));

    // location specifier yields identical results
    assertThat(getPAC(query, Grammar.LOCATION_SPECIFIER).run(), equalTo(expected));
  }

  /** The interface portion has started but no prefix yet */
  @Test
  public void testE2eSpacesNoInterfacePrefix() {
    String query = " n1a [ ";

    assertThat(
        getPAC(query, Grammar.INTERFACE_SPECIFIER).run(),
        containsInAnyOrder(
            new ParboiledAutoCompleteSuggestion("eth11", query.length(), INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth12", query.length(), INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("(", query.length(), INTERFACE_PARENS),
            new ParboiledAutoCompleteSuggestion("/", query.length(), INTERFACE_NAME_REGEX),
            new ParboiledAutoCompleteSuggestion(
                "@connectedTo(", query.length(), INTERFACE_CONNECTED_TO),
            new ParboiledAutoCompleteSuggestion(
                "@interfaceGroup(", query.length(), REFERENCE_BOOK_AND_INTERFACE_GROUP),
            new ParboiledAutoCompleteSuggestion("@interfaceType(", query.length(), INTERFACE_TYPE),
            new ParboiledAutoCompleteSuggestion("@vrf(", query.length(), INTERFACE_VRF),
            new ParboiledAutoCompleteSuggestion("@zone(", query.length(), INTERFACE_ZONE)));
  }

  /** The spaces in the query shouldn't matter */
  @Test
  public void testE2eSpacesInterfacePrefix() {
    String query = " n1a [ eth1";

    Set<ParboiledAutoCompleteSuggestion> expected =
        ImmutableSet.of(
            new ParboiledAutoCompleteSuggestion("eth11", 7, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("eth12", 7, INTERFACE_NAME),
            new ParboiledAutoCompleteSuggestion("]", query.length(), OPERATOR_END),
            new ParboiledAutoCompleteSuggestion("&", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion(",", query.length(), INTERFACE_SET_OP),
            new ParboiledAutoCompleteSuggestion("\\", query.length(), INTERFACE_SET_OP));

    assertThat(getPAC(query, Grammar.INTERFACE_SPECIFIER).run(), equalTo(expected));

    // location specifier yields identical results
    assertThat(getPAC(query, Grammar.LOCATION_SPECIFIER).run(), equalTo(expected));
  }
}
