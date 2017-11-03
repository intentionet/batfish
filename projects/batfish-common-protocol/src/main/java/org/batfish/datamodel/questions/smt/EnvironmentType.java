package org.batfish.datamodel.questions.smt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum EnvironmentType {
  NONE("none"),
  ANY("any"),
  SANE("sane");

  private static final Map<String, EnvironmentType> _map = buildMap();

  private static Map<String, EnvironmentType> buildMap() {
    Map<String, EnvironmentType> map = new HashMap<>();
    for (EnvironmentType value : EnvironmentType.values()) {
      String name = value._name.toLowerCase();
      map.put(name, value);
    }
    return Collections.unmodifiableMap(map);
  }

  @JsonCreator
  public static EnvironmentType fromName(String name) {
    EnvironmentType instance = _map.get(name.toLowerCase());
    if (instance == null) {
      throw new BatfishException(
          "No " + EnvironmentType.class.getSimpleName() + " with name: \"" + name + "\"");
    }
    return instance;
  }

  private final String _name;

  EnvironmentType(String name) {
    _name = name;
  }

  @JsonValue
  public String diffTypeName() {
    return _name;
  }
}
