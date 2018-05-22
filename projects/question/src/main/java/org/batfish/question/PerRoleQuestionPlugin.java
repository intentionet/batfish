package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.INodeRegexQuestion;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.role.NodeRoleDimension;

@AutoService(Plugin.class)
public class PerRoleQuestionPlugin extends QuestionPlugin {

  public static class PerRoleAnswerElement extends AnswerElement {

    private static final String PROP_ANSWERS = "answers";

    private SortedMap<String, AnswerElement> _answers;

    public PerRoleAnswerElement() {
      _answers = new TreeMap<>();
    }

    @JsonProperty(PROP_ANSWERS)
    public SortedMap<String, AnswerElement> getAnswers() {
      return _answers;
    }

    @Override
    public String prettyPrint() {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, AnswerElement> entry : _answers.entrySet()) {
        sb.append("Role " + entry.getKey() + ":\n");
        sb.append(entry.getValue().prettyPrint());
      }
      return sb.toString();
    }

    @JsonProperty(PROP_ANSWERS)
    public void setAnswers(SortedMap<String, AnswerElement> answers) {
      _answers = answers;
    }
  }

  public static class PerRoleAnswerer extends Answerer {

    public PerRoleAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public PerRoleAnswerElement answer() {
      PerRoleQuestion question = (PerRoleQuestion) _question;
      PerRoleAnswerElement answerElement = new PerRoleAnswerElement();

      Question innerQuestion = question.getQuestion();
      INodeRegexQuestion innerNRQuestion;
      if (innerQuestion instanceof INodeRegexQuestion) {
        innerNRQuestion = (INodeRegexQuestion) innerQuestion;
      } else {
        throw new BatfishException(
            "The question " + innerQuestion.getName() + " does not implement INodeRegexQuestion");
      }

      // collect the desired nodes in a list
      Set<String> includeNodes = question.getNodeRegex().getMatchingNodes(_batfish);

      NodeRoleDimension roleDimension =
          _batfish
              .getNodeRoleDimension(question.getRoleDimension())
              .orElseThrow(
                  () ->
                      new BatfishException(
                          "No role dimension found for " + question.getRoleDimension()));

      SortedMap<String, SortedSet<String>> roleNodeMap =
          roleDimension.createRoleNodesMap(includeNodes);

      List<String> desiredRoles = question.getRoles();
      if (desiredRoles != null) {
        // only keep the roles that the user specified
        roleNodeMap
            .entrySet()
            .removeIf(e -> !desiredRoles.contains(e.getKey()) || e.getValue() == null);
      }

      SortedMap<String, AnswerElement> results = new TreeMap<>();

      // now ask the inner question once per role
      Set<String> innerIncludeNodes = innerNRQuestion.getNodeRegex().getMatchingNodes(_batfish);
      for (Map.Entry<String, SortedSet<String>> entry : roleNodeMap.entrySet()) {
        String role = entry.getKey();
        Set<String> roleNodes = entry.getValue();
        String regex = namesToRegex(Sets.intersection(innerIncludeNodes, roleNodes));
        innerNRQuestion.setNodeRegex(new NodesSpecifier(regex));
        String innerQuestionName = innerQuestion.getName();
        Answerer innerAnswerer =
            _batfish.getAnswererCreators().get(innerQuestionName).apply(innerQuestion, _batfish);
        AnswerElement innerAnswer = innerAnswerer.answer();
        results.put(role, innerAnswer);
      }

      answerElement.setAnswers(results);

      return answerElement;
    }

    // create a regex that matches exactly the given set of names
    String namesToRegex(Set<String> names) {
      return names.stream().map(Pattern::quote).collect(Collectors.joining("|"));
    }
  }

  // <question_page_comment>
  /**
   * Answers a given question separately on each "role" within the specified dimension.
   *
   * <p>It is common for the nodes in a network to be partitioned into roles that each have a
   * specific function in the network. For example, border routers are responsible for mediating the
   * interactions between the network and its peer networks, and distribution routers are
   * responsible for delivering packets to end hosts within the network. Roles can be useful for
   * improving the precision and utility of several other questions. For example, it may only make
   * sense to run the CompareSameName question on nodes that have the same role.
   *
   * @type PerRole multifile
   * @param question The question to ask on each role. This parameter is mandatory.
   * @param roles List of the role names to include in the answer. Default is to use all role names.
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes). *
   * @param roleDimension Specifies the role dimension to use. If not provided, the default is to
   *     use the primary auto inferred role dimension.
   */
  public static final class PerRoleQuestion extends Question {

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private static final String PROP_QUESTION = "question";

    private static final String PROP_ROLES = "roles";

    private static final String PROP_ROLE_DIMENSION = "roleDimension";

    @Nonnull private NodesSpecifier _nodeRegex;

    @Nonnull private Question _question;

    private List<String> _roles;

    @Nullable private String _roleDimension;

    @JsonCreator
    public PerRoleQuestion(
        @JsonProperty(PROP_NODE_REGEX) NodesSpecifier nodeRegex,
        @JsonProperty(PROP_QUESTION) Question question,
        @JsonProperty(PROP_ROLE_DIMENSION) String roleDimension,
        @JsonProperty(PROP_ROLES) List<String> roles) {
      _nodeRegex = nodeRegex == null ? NodesSpecifier.ALL : nodeRegex;
      _question = question;
      _roleDimension = roleDimension;
      _roles = roles;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "perrole";
    }

    @JsonProperty(PROP_NODE_REGEX)
    public NodesSpecifier getNodeRegex() {
      return _nodeRegex;
    }

    @JsonProperty(PROP_QUESTION)
    public Question getQuestion() {
      return _question;
    }

    @JsonProperty(PROP_ROLES)
    public List<String> getRoles() {
      return _roles;
    }

    @JsonProperty(PROP_ROLE_DIMENSION)
    public String getRoleDimension() {
      return _roleDimension;
    }

    public void setRoleDimension(String roleDimension) {
      _roleDimension = roleDimension;
    }
  }

  @Override
  protected PerRoleAnswerer createAnswerer(Question question, IBatfish batfish) {
    return new PerRoleAnswerer(question, batfish);
  }

  @Override
  protected PerRoleQuestion createQuestion() {
    return new PerRoleQuestion(null, null, null, null);
  }
}
