package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.role.NodeRoleDimension;

@AutoService(Plugin.class)
public class RolesQuestionPlugin extends QuestionPlugin {

  public static class RolesAnswerElement extends AnswerElement {

    private static final String PROP_ROLE_DIMENSION = "roleDimension";

    private static final String PROP_ROLE_MAP = "roleMap";

    @Nonnull private NodeRoleDimension _roleDimension;

    @Nonnull private SortedMap<String, SortedSet<String>> _roleMap;

    @JsonCreator
    public RolesAnswerElement(
        @JsonProperty(PROP_ROLE_DIMENSION) NodeRoleDimension dimension,
        @JsonProperty(PROP_ROLE_MAP) SortedMap<String, SortedSet<String>> roleMap) {
      _roleDimension = dimension;
      _roleMap = roleMap == null ? new TreeMap<>() : roleMap;
    }

    @JsonProperty(PROP_ROLE_DIMENSION)
    public NodeRoleDimension getRoleDimension() {
      return _roleDimension;
    }

    @JsonProperty(PROP_ROLE_MAP)
    public SortedMap<String, SortedSet<String>> getRoleMap() {
      return _roleMap;
    }
  }

  public static class RolesAnswerer extends Answerer {

    public RolesAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public RolesAnswerElement answer() {

      RolesQuestion question = (RolesQuestion) _question;

      // collect relevant nodes in a list.
      Set<String> nodes = question.getNodeRegex().getMatchingNodes(_batfish);

      NodeRoleDimension roleDimension =
          _batfish
              .getNodeRoleDimension(question.getRoleDimension())
              .orElseThrow(
                  () ->
                      new BatfishException(
                          "No role dimension found for " + question.getRoleDimension()));

      RolesAnswerElement answerElement =
          new RolesAnswerElement(roleDimension, roleDimension.createRoleNodesMap(nodes));

      return answerElement;
    }
  }

  // <question_page_comment>
  /**
   * List the roles of each node.
   *
   * @type Roles multifile
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes).
   * @param roleDimension Which dimension to report on. The default is the primary auto-inferred
   *     one.
   */
  public static final class RolesQuestion extends Question {

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private static final String PROP_ROLE_DIMENSION = "roleDimension";

    @Nonnull private NodesSpecifier _nodeRegex;

    @Nullable private String _roleDimension;

    @JsonCreator
    public RolesQuestion(
        @JsonProperty(PROP_NODE_REGEX) NodesSpecifier nodeRegex,
        @JsonProperty(PROP_ROLE_DIMENSION) String roleDimension) {
      _nodeRegex = nodeRegex == null ? NodesSpecifier.ALL : nodeRegex;
      _roleDimension = roleDimension;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "roles";
    }

    @JsonProperty(PROP_NODE_REGEX)
    public NodesSpecifier getNodeRegex() {
      return _nodeRegex;
    }

    @JsonProperty(PROP_ROLE_DIMENSION)
    public String getRoleDimension() {
      return _roleDimension;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new RolesAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new RolesQuestion(null, null);
  }
}
