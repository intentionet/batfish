package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.collections.OutlierSet;
import org.batfish.datamodel.questions.AbstractRoleConsistencyQuestion;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.OutliersQuestionPlugin.OutliersAnswerElement;
import org.batfish.question.OutliersQuestionPlugin.OutliersQuestion;
import org.batfish.question.PerRoleQuestionPlugin.PerRoleAnswerElement;
import org.batfish.question.PerRoleQuestionPlugin.PerRoleQuestion;
import org.batfish.role.OutliersHypothesis;

@AutoService(Plugin.class)
public class RoleConsistencyQuestionPlugin extends QuestionPlugin {

  public static class RoleConsistencyAnswerElement extends AnswerElement {

    private static final String PROP_ANSWERS = "answers";

    private List<OutlierSet<NavigableSet<String>>> _answers;

    public RoleConsistencyAnswerElement() {
      _answers = new LinkedList<>();
    }

    @JsonProperty(PROP_ANSWERS)
    public List<OutlierSet<NavigableSet<String>>> getAnswers() {
      return _answers;
    }

    @Override
    public String prettyPrint() {

      StringBuffer sb = new StringBuffer();

      for (OutlierSet<?> answer : _answers) {
        sb.append("  Hypothesis: every node in role ");
        sb.append(answer.getRole().orElse("<unknown role>"));
        sb.append(" should have the following definition for property ");
        sb.append(answer.getName() + ": " + answer.getDefinition() + "\n");
        sb.append("  Outliers: ");
        sb.append(answer.getOutliers() + "\n");
        sb.append("  Conformers: ");
        sb.append(answer.getConformers() + "\n\n");
      }

      return sb.toString();
    }

    @JsonProperty(PROP_ANSWERS)
    public void setAnswers(List<OutlierSet<NavigableSet<String>>> answers) {
      _answers = answers;
    }
  }

  public static class RoleConsistencyAnswerer extends Answerer {

    private RoleConsistencyAnswerElement _answerElement;

    public RoleConsistencyAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public RoleConsistencyAnswerElement answer() {

      RoleConsistencyQuestion question = (RoleConsistencyQuestion) _question;
      _answerElement = new RoleConsistencyAnswerElement();

      OutliersQuestion innerQ = new OutliersQuestionPlugin().createQuestion();
      innerQ.setHypothesis(OutliersHypothesis.SAME_SERVERS);
      SortedSet<String> serverSets = new TreeSet<>();
      serverSets.add(question.getPropertyName());
      innerQ.setServerSets(serverSets);
      innerQ.setVerbose(true);

      PerRoleQuestion outerQ = new PerRoleQuestion(null, innerQ, question.getRoleDimension(), null);

      // find all outliers for protocol-specific servers, on a per-role basis
      PerRoleQuestionPlugin outerPlugin = new PerRoleQuestionPlugin();
      PerRoleAnswerElement roleAE = outerPlugin.createAnswerer(outerQ, _batfish).answer();
      List<OutlierSet<NavigableSet<String>>> answers = new LinkedList<>();
      for (Map.Entry<String, AnswerElement> entry : roleAE.getAnswers().entrySet()) {
        String role = entry.getKey();
        OutliersAnswerElement ae = (OutliersAnswerElement) entry.getValue();
        for (OutlierSet<NavigableSet<String>> answer : ae.getServerOutliers()) {
          answer.setRole(role);
          answers.add(answer);
        }
      }

      _answerElement.setAnswers(answers);

      return _answerElement;
    }
  }

  // <question_page_comment>
  /**
   * Checks a role-based consistency policy requiring that all nodes of the same role have the same
   * value for some particular configuration property (e.g., DnsServers).
   *
   * @type RoleConsistency multifile
   * @param roleDimension The name of the role dimension to use. If not specified then the primary
   *     auto-inferred role dimension.
   * @param propertyName A string representing the name of the configuration property to check.
   *     Allowed values are DnsServers, LoggingServers, NtpServers, SnmpTrapServers, TacacsServers.
   */
  public static final class RoleConsistencyQuestion extends AbstractRoleConsistencyQuestion {

    private static final String PROP_ROLE_DIMENSION = "roleDimension";

    private static final String PROP_PROPERTY_NAME = "propertyName";

    @Nullable private String _roleDimension;

    @Nullable private String _propertyName;

    @JsonCreator
    public RoleConsistencyQuestion(
        @JsonProperty(PROP_ROLE_DIMENSION) String roleDimension,
        @JsonProperty(PROP_PROPERTY_NAME) String propertyName) {
      _roleDimension = roleDimension;
      _propertyName = propertyName;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    @JsonProperty(PROP_ROLE_DIMENSION)
    public String getRoleDimension() {
      return _roleDimension;
    }

    @Override
    @JsonIgnore
    public OutliersHypothesis getHypothesis() {
      return OutliersHypothesis.SAME_SERVERS;
    }

    @Override
    public String getName() {
      return "roleConsistency";
    }

    @JsonProperty(PROP_PROPERTY_NAME)
    public String getPropertyName() {
      return _propertyName;
    }
  }

  @Override
  protected RoleConsistencyAnswerer createAnswerer(Question question, IBatfish batfish) {
    return new RoleConsistencyAnswerer(question, batfish);
  }

  @Override
  protected RoleConsistencyQuestion createQuestion() {
    return new RoleConsistencyQuestion(null, null);
  }
}
