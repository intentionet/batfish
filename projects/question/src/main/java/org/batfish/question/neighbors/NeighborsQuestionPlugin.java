package org.batfish.question.neighbors;

import com.google.auto.service.AutoService;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.QuestionPlugin;

@AutoService(Plugin.class)
public class NeighborsQuestionPlugin extends QuestionPlugin {
  @Override
  protected NeighborsAnswerer createAnswerer(Question question, IBatfish batfish) {
    return new NeighborsAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new NeighborsQuestion(null, null, null);
  }
}
