package org.batfish.datamodel.isis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum IsisInterfaceMode {
  ACTIVE("active"),
  PASSIVE("passive"),
  SUPPRESSED("suppressed"),
  UNSET("unset");

  private static final Map<String, IsisInterfaceMode> _map = buildMap();

  private static Map<String, IsisInterfaceMode> buildMap() {
    ImmutableMap.Builder<String, IsisInterfaceMode> map = ImmutableMap.builder();
    for (IsisInterfaceMode value : IsisInterfaceMode.values()) {
      map.put(value._name, value);
    }
    return map.build();
  }

  @JsonCreator
  public static IsisInterfaceMode fromName(String name) {
    IsisInterfaceMode instance = _map.get(name.toLowerCase());
    if (instance == null) {
      throw new BatfishException("No IsisInterfaceMode with name: \"" + name + "\"");
    }
    return instance;
  }

  private final String _name;

  IsisInterfaceMode(String name) {
    _name = name;
  }

  @JsonValue
  public String lName() {
    return _name;
  }
}
