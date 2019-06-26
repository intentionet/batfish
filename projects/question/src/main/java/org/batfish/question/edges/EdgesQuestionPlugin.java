package org.batfish.question.edges;

import com.google.auto.service.AutoService;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.QuestionPlugin;
import org.batfish.question.edges.EdgesQuestion.EdgeType;

@AutoService(Plugin.class)
public class EdgesQuestionPlugin extends QuestionPlugin {
  @Override
  protected EdgesAnswerer createAnswerer(Question question, IBatfish batfish) {
    return new EdgesAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new EdgesQuestion(null, null, EdgeType.LAYER3, false);
  }
}
