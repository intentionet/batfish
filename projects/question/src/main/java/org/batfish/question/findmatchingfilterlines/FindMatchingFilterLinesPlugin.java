package org.batfish.question.findmatchingfilterlines;

import com.google.auto.service.AutoService;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.QuestionPlugin;

/** Plugin for getRows {@link FindMatchingFilterLinesQuestion}. */
@AutoService(Plugin.class)
public class FindMatchingFilterLinesPlugin extends QuestionPlugin {
  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new FindMatchingFilterLinesAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new FindMatchingFilterLinesQuestion();
  }
}
