package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.role.InferRoles;
import org.batfish.role.NodeRoleDimension;

@AutoService(Plugin.class)
public class InferRolesQuestionPlugin extends QuestionPlugin {

  public static class InferRolesAnswerElement extends AnswerElement {

    private static final String PROP_ROLE_DIMENSIONS = "roleDimensions";

    private static final String PROP_MATCHING_NODES_COUNT = "matchingNodesCount";

    @Nonnull private final SortedSet<NodeRoleDimension> _roleDimensions;

    @Nonnull private final SortedMap<String, Integer> _matchingNodesCount;

    @JsonCreator
    public InferRolesAnswerElement(
        @JsonProperty(PROP_ROLE_DIMENSIONS) SortedSet<NodeRoleDimension> roleDimensions,
        @JsonProperty(PROP_MATCHING_NODES_COUNT) SortedMap<String, Integer> matchingNodesCount) {
      _roleDimensions = roleDimensions == null ? new TreeSet<>() : roleDimensions;
      _matchingNodesCount = matchingNodesCount == null ? new TreeMap<>() : matchingNodesCount;
    }

    @JsonProperty(PROP_MATCHING_NODES_COUNT)
    public SortedMap<String, Integer> getMatchingNodesCount() {
      return _matchingNodesCount;
    }

    @JsonProperty(PROP_ROLE_DIMENSIONS)
    public SortedSet<NodeRoleDimension> getRoleDimensions() {
      return _roleDimensions;
    }
  }

  public static class InferRolesAnswerer extends Answerer {

    public InferRolesAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public InferRolesAnswerElement answer() {

      InferRolesQuestion question = (InferRolesQuestion) _question;
      InferRolesAnswerElement answerElement = new InferRolesAnswerElement(null, null);

      // collect relevant nodes in a list.
      Set<String> nodes = question.getNodeRegex().getMatchingNodes(_batfish);

      SortedSet<NodeRoleDimension> roleDimensions =
          new InferRoles(nodes, _batfish.getEnvironmentTopology()).inferRoles();
      answerElement.getRoleDimensions().addAll(roleDimensions);

      for (NodeRoleDimension dimension : roleDimensions) {
        SortedMap<String, SortedSet<String>> roleNodesMap = dimension.createRoleNodesMap(nodes);
        SortedSet<String> matchingNodes = new TreeSet<>();
        for (SortedSet<String> nodeSet : roleNodesMap.values()) {
          matchingNodes.addAll(nodeSet);
        }

        answerElement.getMatchingNodesCount().put(dimension.getName(), matchingNodes.size());
      }
      return answerElement;
    }
  }

  // <question_page_comment>
  /**
   * Infer a regex that identifies a role from a node name.
   *
   * <p>Uses heuristics to identify a part of a node's name that represents its role.
   *
   * @type InferRoles multifile
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes).
   */
  public static final class InferRolesQuestion extends Question {

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private NodesSpecifier _nodeRegex;

    public InferRolesQuestion() {
      _nodeRegex = NodesSpecifier.ALL;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "inferroles";
    }

    @JsonProperty(PROP_NODE_REGEX)
    public NodesSpecifier getNodeRegex() {
      return _nodeRegex;
    }

    @JsonProperty(PROP_NODE_REGEX)
    public void setNodeRegex(NodesSpecifier regex) {
      _nodeRegex = regex;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new InferRolesAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new InferRolesQuestion();
  }
}
