package org.batfish.datamodel.questions;

import org.batfish.datamodel.answers.StringAnswerElement;

public class InvalidReachabilityParametersException extends Exception {

  private StringAnswerElement _invalidParametersAnswer;

  public InvalidReachabilityParametersException(String msg) {
    super(msg);
    _invalidParametersAnswer = new StringAnswerElement(msg);
  }

  public StringAnswerElement getInvalidParametersAnswer() {
    return _invalidParametersAnswer;
  }
}
