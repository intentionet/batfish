package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.NodeRoleSpecifier;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.Question;
import org.batfish.role.InferRoles;

@AutoService(Plugin.class)
public class InferRolesQuestionPlugin extends QuestionPlugin {

  public static class InferRolesAnswerElement implements AnswerElement {

    private static final String PROP_ROLE_SPECIFIER = "roleSpecifier";

    private static final String PROP_ALL_NODES = "allNodes";

    private static final String PROP_ALL_NODES_COUNT = "allNodesCount";

    private static final String PROP_MATCHING_NODES_COUNT = "matchingNodesCount";

    private NodeRoleSpecifier _roleSpecifier;

    private List<String> _allNodes;

    private int _allNodesCount;

    private int _matchingNodesCount;

    public InferRolesAnswerElement() {}

    @JsonProperty(PROP_ROLE_SPECIFIER)
    public NodeRoleSpecifier getRoleSpecifier() {
      return _roleSpecifier;
    }

    @Override
    public String prettyPrint() {

      StringBuilder sb;
      sb = new StringBuilder("Results for infer roles\n");

      if (_roleSpecifier == null) {
        return sb.toString();
      }

      for (String regex : _roleSpecifier.getRoleRegexes()) {
        sb.append("Role regex inferred:  " + regex + "\n");
        sb.append("Matches " + _matchingNodesCount + " out of " + _allNodesCount + " nodes\n");
      }

      SortedMap<String, SortedSet<String>> roleNodesMap =
          _roleSpecifier.createRoleNodesMap(new TreeSet<String>(_allNodes));

      sb.append("Roles inferred:\n");
      for (Map.Entry<String, SortedSet<String>> entry : roleNodesMap.entrySet()) {
        sb.append("  " + entry + "\n");
      }

      return sb.toString();
    }

    @JsonProperty(PROP_ROLE_SPECIFIER)
    public void setRoleSpecifier(NodeRoleSpecifier roleSpecifier) {
      _roleSpecifier = roleSpecifier;
    }

    @JsonProperty(PROP_ALL_NODES)
    public void setAllNodes(List<String> allNodes) {
      _allNodes = allNodes;
    }

    @JsonProperty(PROP_ALL_NODES_COUNT)
    public void setAllNodesCount(int allNodesCount) {
      _allNodesCount = allNodesCount;
    }

    @JsonProperty(PROP_MATCHING_NODES_COUNT)
    public void setMatchingNodesCount(int matchingNodesCount) {
      _matchingNodesCount = matchingNodesCount;
    }
  }

  // Node names are parsed into a sequence of alphanumeric strings and
  // delimiters (strings of non-alphanumeric characters).
  public enum InferRolesCharClass {
    ALPHANUMERIC,
    DELIMITER;

    public static InferRolesCharClass charToCharClass(char c) {
      if (Character.isAlphabetic(c) || Character.isDigit(c)) {
        return InferRolesCharClass.ALPHANUMERIC;
      } else {
        return InferRolesCharClass.DELIMITER;
      }
    }

    public String tokenToRegex(String s) {
      switch (this) {
        case ALPHANUMERIC:
          return "\\p{Alnum}+";
        case DELIMITER:
          return Pattern.quote(s);
        default:
          throw new BatfishException("this case should be unreachable");
      }
    }
  }

  public static class InferRolesAnswerer extends Answerer {

    public InferRolesAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public InferRolesAnswerElement answer() {

      InferRolesQuestion question = (InferRolesQuestion) _question;
      InferRolesAnswerElement answerElement = new InferRolesAnswerElement();

      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      // collect relevant nodes in a list.
      List<String> nodes =
          CommonUtil.getMatchingStrings(question.getNodeRegex(), configurations.keySet());

      int allNodesCount = nodes.size();

      answerElement.setAllNodes(nodes);
      answerElement.setAllNodesCount(allNodesCount);

      NodeRoleSpecifier roleSpecifier = new InferRoles(nodes, configurations, _batfish).call();
      answerElement.setRoleSpecifier(roleSpecifier);

      SortedMap<String, SortedSet<String>> roleNodesMap =
          roleSpecifier.createRoleNodesMap(new TreeSet<>(nodes));
      SortedSet<String> matchingNodes = new TreeSet<>();
      for (SortedSet<String> nodeSet : roleNodesMap.values()) {
        matchingNodes.addAll(nodeSet);
      }

      answerElement.setMatchingNodesCount(matchingNodes.size());
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

    private String _nodeRegex;

    public InferRolesQuestion() {
      _nodeRegex = ".*";
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
    public String getNodeRegex() {
      return _nodeRegex;
    }

    @Override
    public boolean getTraffic() {
      return false;
    }

    @JsonProperty(PROP_NODE_REGEX)
    public void setNodeRegex(String regex) {
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
