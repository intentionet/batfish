package org.batfish.question.interfaceproperties;

import static org.batfish.question.interfaceproperties.InterfacePropertiesQuestion.DEFAULT_EXCLUDE_SHUT_INTERFACES;

import com.google.auto.service.AutoService;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.questions.InterfacePropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.QuestionPlugin;
import org.batfish.specifier.AllInterfacesInterfaceSpecifier;
import org.batfish.specifier.AllNodesNodeSpecifier;

@AutoService(Plugin.class)
public class InterfacePropertiesPlugin extends QuestionPlugin {

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new InterfacePropertiesAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new InterfacePropertiesQuestion(
        AllNodesNodeSpecifier.INSTANCE,
        AllInterfacesInterfaceSpecifier.INSTANCE,
        InterfacePropertySpecifier.ALL,
        DEFAULT_EXCLUDE_SHUT_INTERFACES);
  }
}
