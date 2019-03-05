package org.batfish.question.ospfproperties;

import com.google.auto.service.AutoService;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.questions.OspfPropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.QuestionPlugin;
import org.batfish.specifier.AllNodesNodeSpecifier;

@AutoService(Plugin.class)
public class OspfPropertiesPlugin extends QuestionPlugin {

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new OspfPropertiesAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new OspfPropertiesQuestion(AllNodesNodeSpecifier.INSTANCE, OspfPropertySpecifier.ALL);
  }
}
