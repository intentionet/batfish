package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.service.AutoService;
import java.util.SortedMap;
import java.util.TreeMap;
import org.batfish.common.Answerer;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.ParseTreeSentences;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class ParseTreesQuestionPlugin extends QuestionPlugin {

  public static class ParseTreesAnswerElement extends AnswerElement {

    private SortedMap<String, ParseTreeSentences> _parseTrees;

    @JsonCreator
    public ParseTreesAnswerElement() {
      _parseTrees = new TreeMap<>();
    }

    public SortedMap<String, ParseTreeSentences> getParseTrees() {
      return _parseTrees;
    }

    public void setParseTrees(SortedMap<String, ParseTreeSentences> parseTrees) {
      _parseTrees = parseTrees;
    }
  }

  public static class ParseTreesAnswerer extends Answerer {

    public ParseTreesAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public ParseTreesAnswerElement answer(NetworkSnapshot snapshot) {
      // ParseTreesQuestion question = (ParseTreesQuestion) _question;
      ParseTreesAnswerElement answerElement = new ParseTreesAnswerElement();
      ParseVendorConfigurationAnswerElement parseAnswer =
          _batfish.loadParseVendorConfigurationAnswerElement();
      answerElement._parseTrees = parseAnswer.getParseTrees();
      return answerElement;
    }
  }

  /** Outputs parse trees from snapshot initialization. */
  public static class ParseTreesQuestion extends Question {

    public ParseTreesQuestion() {}

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "parsetrees";
    }
  }

  @Override
  protected ParseTreesAnswerer createAnswerer(Question question, IBatfish batfish) {
    return new ParseTreesAnswerer(question, batfish);
  }

  @Override
  protected ParseTreesQuestion createQuestion() {
    return new ParseTreesQuestion();
  }
}
