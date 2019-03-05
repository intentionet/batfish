package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class InitInfoQuestionPlugin extends QuestionPlugin {

  public static class InitInfoAnswerer extends Answerer {

    public InitInfoAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public InitInfoAnswerElement answer() {
      InitInfoQuestion question = (InitInfoQuestion) _question;
      boolean b = question._environmentBgpTables;
      boolean r = question._environmentRoutes;
      if (b && r) {
        throw new BatfishException(
            String.format(
                "Only one of the following may be true: '%s' and '%s'",
                InitInfoQuestion.PROP_ENVIRONMENT_ROUTES,
                InitInfoQuestion.PROP_ENVIRONMENT_BGP_TABLES));
      }
      if (b) {
        return _batfish.initInfoBgpAdvertisements(question._summary, question._verboseError);
      } else if (r) {
        return _batfish.initInfoRoutes(question._summary, question._verboseError);
      } else {
        return _batfish.initInfo(question._summary, question._verboseError);
      }
    }
  }

  /** Outputs results of test-rig initialization. */
  public static class InitInfoQuestion extends Question {

    public static final String PROP_ENVIRONMENT_BGP_TABLES = "environmentBgpTables";

    public static final String PROP_ENVIRONMENT_ROUTES = "environmentRoutes";

    private static final String PROP_SUMMARY = "summary";

    private static final String PROP_VERBOSE_ERROR = "verboseError";

    private boolean _environmentBgpTables;

    private boolean _environmentRoutes;

    private boolean _summary;

    private boolean _verboseError;

    public InitInfoQuestion() {}

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @JsonProperty(PROP_ENVIRONMENT_BGP_TABLES)
    public boolean getEnvironmentBgpTables() {
      return _environmentBgpTables;
    }

    @JsonProperty(PROP_ENVIRONMENT_ROUTES)
    public boolean getEnvironmentRoutes() {
      return _environmentRoutes;
    }

    @Override
    public boolean getIndependent() {
      return true;
    }

    @Override
    public String getName() {
      return "initinfo";
    }

    @JsonProperty(PROP_SUMMARY)
    public boolean getSummary() {
      return _summary;
    }

    @JsonProperty(PROP_VERBOSE_ERROR)
    public boolean getVerboseError() {
      return _verboseError;
    }

    @Override
    public String prettyPrint() {
      return getName()
          + " "
          + PROP_SUMMARY
          + "="
          + _summary
          + " "
          + PROP_VERBOSE_ERROR
          + "="
          + _verboseError;
    }

    @JsonProperty(PROP_ENVIRONMENT_BGP_TABLES)
    public void setEnvironmentBgpTables(boolean environmentBgpTables) {
      _environmentBgpTables = environmentBgpTables;
    }

    @JsonProperty(PROP_ENVIRONMENT_ROUTES)
    public void setEnvironmentRoutes(boolean environmentRoutes) {
      _environmentRoutes = environmentRoutes;
    }

    @JsonProperty(PROP_SUMMARY)
    public void setSummary(boolean summary) {
      _summary = summary;
    }

    @JsonProperty(PROP_VERBOSE_ERROR)
    public void setVerboseError(boolean verboseError) {
      _verboseError = verboseError;
    }
  }

  @Override
  protected InitInfoAnswerer createAnswerer(Question question, IBatfish batfish) {
    return new InitInfoAnswerer(question, batfish);
  }

  @Override
  protected InitInfoQuestion createQuestion() {
    return new InitInfoQuestion();
  }
}
