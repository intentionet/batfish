package org.batfish.question.jsonpath;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum JsonPathAssertionType {
  count("count"),
  equals("equals"),
  none("none");

  private static final Map<String, JsonPathAssertionType> _map = buildMap();

  private static Map<String, JsonPathAssertionType> buildMap() {
    ImmutableMap.Builder<String, JsonPathAssertionType> map = ImmutableMap.builder();
    for (JsonPathAssertionType value : JsonPathAssertionType.values()) {
      String name = value._name.toLowerCase();
      map.put(name, value);
    }
    return map.build();
  }

  @JsonCreator
  public static JsonPathAssertionType fromName(String name) {
    JsonPathAssertionType instance = _map.get(name.toLowerCase());
    if (instance == null) {
      throw new BatfishException(
          "No " + JsonPathAssertionType.class.getSimpleName() + " with name: \"" + name + "\"");
    }
    return instance;
  }

  private final String _name;

  JsonPathAssertionType(String name) {
    _name = name;
  }

  @JsonValue
  public String jsonPathAssertionTypeName() {
    return _name;
  }
}
