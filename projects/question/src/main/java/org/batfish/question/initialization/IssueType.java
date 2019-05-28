package org.batfish.question.initialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum IssueType {
  ConvertError("Convert error"),
  ConvertWarningRedFlag("Convert warning (redflag)"),
  ConvertWarningUnimplemented("Convert warning (unimplemented)"),
  ParseError("Parse error"),
  ParseStatus("Parse status"),
  ParseWarning("Parse warning"),
  ParseWarningRedFlag("Parse warning (redflag)"),
  ParseWarningUnimplemented("Parse warning (unimplemented)");

  private static final Map<String, IssueType> _map = buildMap();

  private final String _string;

  private static Map<String, IssueType> buildMap() {
    ImmutableMap.Builder<String, IssueType> map = ImmutableMap.builder();
    for (IssueType type : IssueType.values()) {
      map.put(type._string, type);
    }
    return map.build();
  }

  IssueType(String string) {
    _string = string;
  }

  @JsonValue
  @Override
  public String toString() {
    return _string;
  }

  @JsonCreator
  public static IssueType fromString(String string) {
    IssueType type = _map.get(string);
    if (type == null) {
      throw new BatfishException(String.format("No IssueType matching: '%s'", string));
    }
    return type;
  }
}
