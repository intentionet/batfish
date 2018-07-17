package org.batfish.question.multipath;

import com.google.auto.service.AutoService;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.QuestionPlugin;

/** Plugin for answer {@link MultipathConsistencyQuestion}. */
@AutoService(Plugin.class)
public class MultipathConsistencyPlugin extends QuestionPlugin {
  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new MultipathConsistencyAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new MultipathConsistencyQuestion();
  }
}
