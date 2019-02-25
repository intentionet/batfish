package org.batfish.question.ospfprocess;

import com.google.auto.service.AutoService;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.QuestionPlugin;
import org.batfish.question.bgpproperties.BgpProcessConfigurationAnswerer;
import org.batfish.question.bgpproperties.BgpProcessConfigurationQuestion;

@AutoService(Plugin.class)
public class OspfProcessConfigurationQuestionPlugin extends QuestionPlugin{
  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new OspfProcessConfigurationAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new OspfProcessConfigurationQuestion(null, null);
  }
}
