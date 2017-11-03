package org.batfish.symbolic.utils;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.questions.smt.HeaderLocationQuestion;

public class PathRegexes {

  private Pattern _dstRegex;

  private Pattern _notDstRegex;

  private Pattern _ifaceRegex;

  private Pattern _notIfaceRegex;

  private Pattern _srcRegex;

  private Pattern _notSrcRegex;

  public PathRegexes(HeaderLocationQuestion q) {
    try {
      _dstRegex = (q.getFinalNodeRegex() == null ? null : Pattern.compile(q.getFinalNodeRegex()));
      _notDstRegex =
          (q.getNotFinalNodeRegex() == null ? null : Pattern.compile(q.getNotFinalNodeRegex()));
      _ifaceRegex =
          (q.getFinalIfaceRegex() == null ? null : Pattern.compile(q.getFinalIfaceRegex()));
      _notIfaceRegex =
          (q.getNotFinalIfaceRegex() == null ? null : Pattern.compile(q.getNotFinalIfaceRegex()));
      _srcRegex =
          (q.getIngressNodeRegex() == null ? null : Pattern.compile(q.getIngressNodeRegex()));
      _notSrcRegex =
          (q.getNotIngressNodeRegex() == null ? null : Pattern.compile(q.getNotIngressNodeRegex()));
    } catch (PatternSyntaxException e) {
      throw new BatfishException("One of the supplied regexes  is not a " + "valid java regex.", e);
    }
  }

  Pattern getDstRegex() {
    return _dstRegex;
  }

  Pattern getNotDstRegex() {
    return _notDstRegex;
  }

  Pattern getIfaceRegex() {
    return _ifaceRegex;
  }

  Pattern getNotIfaceRegex() {
    return _notIfaceRegex;
  }

  Pattern getSrcRegex() {
    return _srcRegex;
  }

  Pattern getNotSrcRegex() {
    return _notSrcRegex;
  }
}
