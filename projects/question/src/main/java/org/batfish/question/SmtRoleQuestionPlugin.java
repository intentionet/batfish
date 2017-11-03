package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.IQuestion;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.smt.EquivalenceType;

@AutoService(Plugin.class)
public class SmtRoleQuestionPlugin extends QuestionPlugin {

  public static class RoleAnswerer extends Answerer {

    public RoleAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {
      RoleQuestion q = (RoleQuestion) _question;
      return _batfish.smtRoles(q.getType(), q.getNodeRegex());
    }
  }

  public static class RoleQuestion extends Question implements IQuestion {

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private static final String PROP_EQUIVALENCE_TYPE = "equivType";

    private String _nodeRegex = ".*";

    private EquivalenceType _type = EquivalenceType.NODE;

    RoleQuestion() {
      _type = EquivalenceType.NODE;
    }

    @JsonProperty(PROP_NODE_REGEX)
    public String getNodeRegex() {
      return _nodeRegex;
    }

    @JsonProperty(PROP_EQUIVALENCE_TYPE)
    public EquivalenceType getType() {
      return _type;
    }

    @JsonProperty(PROP_NODE_REGEX)
    public void setNodeRegex(String x) {
      _nodeRegex = x;
    }

    @JsonProperty(PROP_EQUIVALENCE_TYPE)
    public void setType(EquivalenceType x) {
      this._type = x;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "smt-roles";
    }

    @Override
    public boolean getTraffic() {
      return false;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new RoleAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new RoleQuestion();
  }
}
