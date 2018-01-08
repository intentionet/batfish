package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum ReachabilityType {
  INCREASED("increased"),
  MULTIPATH("multipath"),
  MULTIPATH_DIFF("multipathDiff"),
  PATH_DIFF("pathDiff"),
  REDUCED_REACHABILITY("reduced"),
  STANDARD("standard");

  private static final Map<String, ReachabilityType> _map = buildMap();

  private static Map<String, ReachabilityType> buildMap() {
    ImmutableMap.Builder<String, ReachabilityType> map = ImmutableMap.builder();
    for (ReachabilityType value : ReachabilityType.values()) {
      String name = value._name.toLowerCase();
      map.put(name, value);
    }
    return map.build();
  }

  @JsonCreator
  public static ReachabilityType fromName(String name) {
    ReachabilityType instance = _map.get(name.toLowerCase());
    if (instance == null) {
      throw new BatfishException(
          "No " + ReachabilityType.class.getSimpleName() + " with name: \"" + name + "\"");
    }
    return instance;
  }

  private final String _name;

  ReachabilityType(String name) {
    _name = name;
  }

  @JsonValue
  public String reachabilityTypeName() {
    return _name;
  }
}
