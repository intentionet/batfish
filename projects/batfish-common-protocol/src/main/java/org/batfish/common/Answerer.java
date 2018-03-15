package org.batfish.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.function.BiFunction;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.JsonDiff;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.JsonDiffAnswerElement;
import org.batfish.datamodel.questions.Question;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public abstract class Answerer {

  public static Answerer create(Question question, IBatfish batfish) {
    String questionName = question.getName();
    BiFunction<Question, IBatfish, Answerer> answererCreator =
        batfish.getAnswererCreators().get(questionName);
    if (answererCreator == null) {
      throw new BatfishException(
          "Cannot create answerer for missing question with name: " + questionName);
    }
    return answererCreator.apply(question, batfish);
  }

  protected final IBatfish _batfish;

  protected final BatfishLogger _logger;

  protected final Question _question;

  public Answerer(Question question, IBatfish batfish) {
    _batfish = batfish;
    _logger = batfish.getLogger();
    _question = question;
  }

  public abstract AnswerElement answer();

  // this is the default differential answerer
  // if you want a custom one for a subclass, override this function in the
  // subclass
  public AnswerElement answerDiff() {
    _batfish.pushBaseEnvironment();
    _batfish.checkEnvironmentExists();
    _batfish.popEnvironment();
    _batfish.pushDeltaEnvironment();
    _batfish.checkEnvironmentExists();
    _batfish.popEnvironment();
    _batfish.pushBaseEnvironment();
    AnswerElement before = create(_question, _batfish).answer();
    _batfish.popEnvironment();
    _batfish.pushDeltaEnvironment();
    AnswerElement after = create(_question, _batfish).answer();
    _batfish.popEnvironment();
    ObjectWriter writer = BatfishObjectMapper.prettyWriter();
    try {
      String beforeJsonStr = writer.writeValueAsString(before);
      String afterJsonStr = writer.writeValueAsString(after);
      JSONObject beforeJson = new JSONObject(beforeJsonStr);
      JSONObject afterJson = new JSONObject(afterJsonStr);
      JsonDiff diff = new JsonDiff(beforeJson, afterJson);

      return new JsonDiffAnswerElement(diff);
    } catch (JsonProcessingException | JSONException e) {
      throw new BatfishException("Could not convert diff element to json string", e);
    }
  }
}
