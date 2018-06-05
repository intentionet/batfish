package org.batfish.question;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.acl.CanonicalAcl;
import org.batfish.datamodel.answers.AclLinesAnswerElement;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.aclreachability2.AclReachabilityAnswererUtils;

@AutoService(Plugin.class)
public class AclReachabilityQuestionPlugin extends QuestionPlugin {

  public static class AclReachabilityAnswerer extends Answerer {

    public AclReachabilityAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {
      AclReachabilityQuestion question = (AclReachabilityQuestion) _question;
      AclLinesAnswerElement answer = new AclLinesAnswerElement();

      Set<String> specifiedNodes = question.getNodeRegex().getMatchingNodes(_batfish);
      Pattern aclRegex;
      try {
        aclRegex = Pattern.compile(question.getAclNameRegex());
      } catch (PatternSyntaxException e) {
        throw new BatfishException(
            "Supplied regex for nodes is not a valid Java regex: \""
                + question.getAclNameRegex()
                + "\"",
            e);
      }
      SortedMap<String, Configuration> configurations = _batfish.loadConfigurations();
      List<CanonicalAcl> canonicalAcls =
          AclReachabilityAnswererUtils.getCanonicalAcls(
              configurations, specifiedNodes, aclRegex, answer);
      _batfish.answerAclReachability(canonicalAcls, answer);
      return answer;
    }
  }

  // <question_page_comment>

  /**
   * Identifies unreachable lines in ACLs.
   *
   * <p>Report ACLs with unreachable lines, as well as reachability of each line within the ACL.
   * Unreachable lines can indicate erroneous configuration.
   *
   * @type AclReachability onefile
   * @param aclNameRegex Regular expression for names of the ACLs to analyze. Default value is '.*'
   *     (i.e., all ACLs).
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes).
   * @example bf_answer("AclReachability", aclNameRegex='OUTSIDE_TO_INSIDE.*') Analyzes only ACLs
   *     whose names start with 'OUTSIDE_TO_INSIDE'.
   */
  public static class AclReachabilityQuestion extends Question {

    private static final String PROP_ACL_NAME_REGEX = "aclNameRegex";

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private String _aclNameRegex;

    private NodesSpecifier _nodeRegex;

    public AclReachabilityQuestion() {
      _nodeRegex = NodesSpecifier.ALL;
      _aclNameRegex = ".*";
    }

    public AclReachabilityQuestion(
        @Nullable @JsonProperty(PROP_ACL_NAME_REGEX) String aclNameRegex,
        @Nullable @JsonProperty(PROP_NODE_REGEX) NodesSpecifier nodeRegex) {
      _aclNameRegex = firstNonNull(aclNameRegex, ".*");
      _nodeRegex = firstNonNull(nodeRegex, NodesSpecifier.ALL);
    }

    @JsonProperty(PROP_ACL_NAME_REGEX)
    public String getAclNameRegex() {
      return _aclNameRegex;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "aclreachability";
    }

    @JsonProperty(PROP_NODE_REGEX)
    public NodesSpecifier getNodeRegex() {
      return _nodeRegex;
    }

    @Override
    public String prettyPrint() {
      String retString =
          String.format(
              "%s %s%s=\"%s\" %s=\"%s\"",
              getName(),
              prettyPrintBase(),
              PROP_ACL_NAME_REGEX,
              _aclNameRegex,
              PROP_NODE_REGEX,
              _nodeRegex);
      return retString;
    }

    @JsonProperty(PROP_ACL_NAME_REGEX)
    public void setAclNameRegex(String regex) {
      _aclNameRegex = regex;
    }

    @JsonProperty(PROP_NODE_REGEX)
    public void setNodeRegex(NodesSpecifier regex) {
      _nodeRegex = regex;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new AclReachabilityAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new AclReachabilityQuestion();
  }
}
