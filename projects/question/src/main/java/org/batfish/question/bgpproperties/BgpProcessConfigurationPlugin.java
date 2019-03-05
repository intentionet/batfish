package org.batfish.question.bgpproperties;

import com.google.auto.service.AutoService;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.questions.BgpProcessPropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.QuestionPlugin;
import org.batfish.specifier.AllNodesNodeSpecifier;

@AutoService(Plugin.class)
public class BgpProcessConfigurationPlugin extends QuestionPlugin {

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new BgpProcessConfigurationAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new BgpProcessConfigurationQuestion(
        AllNodesNodeSpecifier.INSTANCE, BgpProcessPropertySpecifier.ALL);
  }
}
