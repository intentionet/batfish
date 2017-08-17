package org.batfish.datamodel.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.questions.Question;

/**
 * The {@link Analysis Analysis} is an Object representation of the analysis for BatFish service.
 *
 * <p>Each {@link Analysis Analysis} contains a name and a mapping from question name to {@link
 * Question question} for questions in the Analysis.
 */
public class Analysis {
  private static final String PROP_NAME = "name";
  private static final String PROP_QUESTIONS = "questions";

  private String _name;
  private Map<String, String> _questions;

  public Analysis(String name) {
    this(name, new HashMap<>());
  }

  @JsonCreator
  public Analysis(
      @JsonProperty(PROP_NAME) String name,
      @JsonProperty(PROP_QUESTIONS) Map<String, String> questions) {
    this._name = name;
    this._questions = questions == null ? new HashMap<>() : questions;
  }

  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }

  @JsonProperty(PROP_QUESTIONS)
  public Map<String, String> getQuestions() {
    return _questions;
  }

  @JsonProperty(PROP_NAME)
  public void setName(String name) {
    _name = name;
  }

  @JsonProperty(PROP_QUESTIONS)
  public void setQuestions(Map<String, String> questions) {
    _questions = questions;
  }

  public void addQuestion(String questionName, String questionContent) {
    if (this._questions.containsKey(questionName)) {
      throw new BatfishException(
          "Question '" + questionName + "' already exists for analysis '" + _name + "'");
    }
    this._questions.put(questionName, questionContent);
  }

  public void deleteQuestion(String questionName) {
    if (!this._questions.containsKey(questionName)) {
      throw new BatfishException(
          "Question '" + questionName + "' does not exist for analysis '" + _name + "'");
    }
    this._questions.remove(questionName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(Analysis.class)
        .add(PROP_NAME, _name)
        .add(PROP_QUESTIONS, _questions)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Analysis)) {
      return false;
    }
    Analysis other = (Analysis) o;
    return Objects.equals(_name, other._name) && Objects.equals(_questions, other._questions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, _questions);
  }
}
