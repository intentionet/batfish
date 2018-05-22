package org.batfish.datamodel.questions;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.batfish.datamodel.answers.AutocompleteSuggestion;
import org.batfish.datamodel.questions.NodesSpecifier.Type;
import org.batfish.role.NodeRole;
import org.batfish.role.NodeRoleDimension;
import org.batfish.role.NodeRolesData;
import org.junit.Test;

public class NodesSpecifierTest {

  private NodeRolesData initNodeRoleData() {
    NodeRoleDimension dim1 =
        new NodeRoleDimension(
            "dim10",
            new ImmutableSortedSet.Builder<NodeRole>(NodeRole::compareTo)
                .add(new NodeRole("role1", ".*"))
                .add(new NodeRole("role2", ".*"))
                .build(),
            NodeRoleDimension.Type.CUSTOM,
            null);
    NodeRoleDimension dim2 =
        new NodeRoleDimension(
            "dim20",
            new ImmutableSortedSet.Builder<NodeRole>(NodeRole::compareTo)
                .add(new NodeRole("role1", ".*"))
                .add(new NodeRole("role2", ".*"))
                .build(),
            NodeRoleDimension.Type.CUSTOM,
            null);
    SortedSet<NodeRoleDimension> roleDimensions =
        new ImmutableSortedSet.Builder<NodeRoleDimension>(NodeRoleDimension::compareTo)
            .add(dim1)
            .add(dim2)
            .build();
    return new NodeRolesData(null, null, roleDimensions);
  }

  @Test
  public void autoCompleteEmptyString() {
    Set<String> nodes = new HashSet<>();
    nodes.add("node1");
    String query = "";

    List<AutocompleteSuggestion> suggestions = NodesSpecifier.autoComplete(query, nodes, null);

    // autocomplete should yield four entries: NAME:, ROLE:, .*, node1
    assertThat(suggestions, hasSize(4));
  }

  @Test
  public void autoCompleteNothing() {
    Set<String> nodes = new HashSet<>();
    nodes.add("node1");
    String query = "matchNothing";

    List<AutocompleteSuggestion> suggestions = NodesSpecifier.autoComplete(query, nodes, null);

    // autocomplete should yield nothing
    assertThat(suggestions, hasSize(0));
  }

  @Test
  public void autoCompleteTypeAndName() {
    Set<String> nodes = new HashSet<>();
    nodes.add("nade1"); // will match
    nodes.add("node1"); // won't match
    String queryName = "na";
    String queryRole = "ro";

    List<AutocompleteSuggestion> suggestionsName =
        NodesSpecifier.autoComplete(queryName, nodes, null);
    List<AutocompleteSuggestion> suggestionsRole =
        NodesSpecifier.autoComplete(queryRole, nodes, null);

    // suggestionsName should have three elements: NAME:, na.*, and nade1
    assertThat(suggestionsName, hasSize(3));
    assertThat(suggestionsName.get(0).getText(), equalTo("NAME:"));
    assertThat(suggestionsName.get(1).getText(), equalTo("na.*"));
    assertThat(suggestionsName.get(2).getText(), equalTo("nade1"));

    // suggestionsRole should have one element: ROLE:
    assertThat(suggestionsRole, hasSize(1));
    assertThat(suggestionsRole.get(0).getText(), equalTo("ROLE:"));
  }

  @Test
  public void autoCompleteRoleAllDimensions() {
    NodeRolesData nodeRolesData = initNodeRoleData();
    String queryAllDimensions = "ROLE:";
    List<AutocompleteSuggestion> suggestions =
        NodesSpecifier.autoComplete(queryAllDimensions, null, nodeRolesData);
    Set<String> suggestionsText =
        suggestions.stream().map(suggestion -> suggestion.getText()).collect(Collectors.toSet());

    // suggestions should have two elements, one for each dimension
    assertThat(suggestions, hasSize(2));
    assertThat(suggestionsText, hasItem("ROLE:dim10:"));
    assertThat(suggestionsText, hasItem("ROLE:dim20:"));
  }

  @Test
  public void autoCompleteRolePartialDimension() {
    NodeRolesData nodeRolesData = initNodeRoleData();
    String queryDimensionPartial = "ROLE:dim1";
    List<AutocompleteSuggestion> suggestions =
        NodesSpecifier.autoComplete(queryDimensionPartial, null, nodeRolesData);

    // suggestions should have one element
    assertThat(suggestions, hasSize(1));
    assertThat(suggestions.get(0).getText(), equalTo("ROLE:dim10:"));
  }

  @Test
  public void autoCompleteRoleDimension() {
    NodeRolesData nodeRolesData = initNodeRoleData();
    String queryDimension = "ROLE:dim10:";
    List<AutocompleteSuggestion> suggestions =
        NodesSpecifier.autoComplete(queryDimension, null, nodeRolesData);
    Set<String> suggestionsText =
        suggestions.stream().map(suggestion -> suggestion.getText()).collect(Collectors.toSet());

    // suggestions should have three elements, one for each role and one .*
    assertThat(suggestions, hasSize(3));
    assertThat(suggestionsText, hasItem("ROLE:dim10:.*"));
    assertThat(suggestionsText, hasItem("ROLE:dim10:role1"));
    assertThat(suggestionsText, hasItem("ROLE:dim10:role2"));
  }

  @Test
  public void constructorImplicitName() {
    NodesSpecifier specifier = new NodesSpecifier("lhr-.*");
    assertThat(specifier.getType(), equalTo(Type.NAME));
    assertThat(specifier.getRegex().pattern(), equalTo(Pattern.compile("lhr-.*").pattern()));
  }

  @Test
  public void constructorExplicitName() {
    NodesSpecifier specifier = new NodesSpecifier("name:lhr-.*");
    assertThat(specifier.getType(), equalTo(Type.NAME));
    assertThat(specifier.getRegex().pattern(), equalTo(Pattern.compile("lhr-.*").pattern()));
  }

  @Test
  public void constructorRole() {
    NodesSpecifier specifier = new NodesSpecifier("role:dim:svr.*");
    assertThat(specifier.getType(), equalTo(Type.ROLE));
    assertThat(specifier.getRoleDimension(), equalTo("dim"));
    assertThat(specifier.getRegex().pattern(), equalTo(Pattern.compile("svr.*").pattern()));
  }

  @Test
  public void getMatchingNodesByName() {
    NodesSpecifier specifier = new NodesSpecifier("name:lhr-.*");

    String matchingRouter = "lhr-border1";
    String nonMatchingRouter = "svr-border1";
    Set<String> nodes =
        new ImmutableSet.Builder<String>().add(matchingRouter).add(nonMatchingRouter).build();

    Set<String> matchingNodes = specifier.getMatchingNodesByName(nodes);

    assertThat(matchingNodes, hasItem(matchingRouter));
    assertThat(matchingNodes, not(hasItem(nonMatchingRouter)));
  }

  @Test
  public void getMatchingNodesByRole() {
    NodesSpecifier specifier = new NodesSpecifier("role:dim:match.*");

    String matchingNode1 = "lhr-border-01";
    String matchingNode2 = "svr-border-02";
    String nonMatchingNode1 = "svr-core-1";
    Set<String> nodes =
        new ImmutableSet.Builder<String>()
            .add(matchingNode1)
            .add(matchingNode2)
            .add(nonMatchingNode1)
            .build();

    NodeRole role1 = new NodeRole("match1", "lhr-border.*");
    NodeRole role2 = new NodeRole("match2", "svr-border.*");
    NodeRole role3 = new NodeRole("dumb0", "lhr-core.*");
    SortedSet<NodeRole> roles =
        new ImmutableSortedSet.Builder<NodeRole>(NodeRole::compareTo)
            .add(role1)
            .add(role2)
            .add(role3)
            .build();

    NodeRoleDimension roleDimension = new NodeRoleDimension("dim", roles, null, null);

    Set<String> matchingNodes = specifier.getMatchingNodesByRole(roleDimension, nodes);

    assertThat(matchingNodes, hasItem(matchingNode1));
    assertThat(matchingNodes, hasItem(matchingNode2));
    assertThat(matchingNodes, not(hasItem(nonMatchingNode1)));
  }
}
