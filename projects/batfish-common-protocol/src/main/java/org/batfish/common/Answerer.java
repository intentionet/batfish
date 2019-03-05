package org.batfish.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.JsonDiff;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.JsonDiffAnswerElement;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableDiff;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public abstract class Answerer {

  public static Answerer create(Question question, IBatfish batfish) {
    String questionName = question.getName();
    Answerer answerer = batfish.createAnswerer(question);
    if (answerer == null) {
      throw new BatfishException(
          "Cannot create answerer for missing question with name: " + questionName);
    }
    return answerer;
  }

  protected final IBatfish _batfish;

  protected final BatfishLogger _logger;

  /**
   * The question of this answer object, which helps the answerer figure out the context in which it
   * is called (embedded within question parameters).
   *
   * <p>In hindsight, this pattern has proved problematic. When other bits of code (e.g., other
   * answerer's) need access to this answerer's logic, they must first create a question that looks
   * similar to what a user-created question will look like (which is not good by itself, and also
   * impossible in some other cases).
   */
  protected final Question _question;

  public Answerer(Question question, IBatfish batfish) {
    _batfish = batfish;
    _logger = batfish.getLogger();
    _question = question;
  }

  public abstract AnswerElement answer();

  /**
   * The default implementation for generating differential answers.
   *
   * <p>It uses {@link TableDiff} if the answer element is a {@link TableAnswerElement}. Otherwise,
   * it uses a JSON-level diff.
   *
   * <p>Answerers that want a custom differential answer, should override this function.
   */
  public AnswerElement answerDiff() {
    _batfish.pushBaseSnapshot();
    _batfish.checkSnapshotOutputReady();
    _batfish.popSnapshot();
    _batfish.pushDeltaSnapshot();
    _batfish.checkSnapshotOutputReady();
    _batfish.popSnapshot();
    _batfish.pushBaseSnapshot();
    AnswerElement baseAnswer = create(_question, _batfish).answer();
    _batfish.popSnapshot();
    _batfish.pushDeltaSnapshot();
    AnswerElement deltaAnswer = create(_question, _batfish).answer();
    _batfish.popSnapshot();
    if (baseAnswer instanceof TableAnswerElement) {
      TableAnswerElement rawTable =
          TableDiff.diffTables(
              (TableAnswerElement) baseAnswer,
              (TableAnswerElement) deltaAnswer,
              _question.getIncludeOneTableKeys());
      TableAnswerElement finalTable = new TableAnswerElement(rawTable.getMetadata());
      finalTable.postProcessAnswer(_question, rawTable.getRows().getData());
      return finalTable;
    } else {
      try {
        String beforeJsonStr = BatfishObjectMapper.writePrettyString(baseAnswer);
        String afterJsonStr = BatfishObjectMapper.writePrettyString(deltaAnswer);
        JSONObject beforeJson = new JSONObject(beforeJsonStr);
        JSONObject afterJson = new JSONObject(afterJsonStr);
        JsonDiff diff = new JsonDiff(beforeJson, afterJson);

        return new JsonDiffAnswerElement(diff);
      } catch (JsonProcessingException | JSONException e) {
        throw new BatfishException("Could not convert diff element to json string", e);
      }
    }
  }
}
