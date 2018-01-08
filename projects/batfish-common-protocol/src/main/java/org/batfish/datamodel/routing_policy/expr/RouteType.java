package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum RouteType {
  INTERAREA("interarea"),
  INTERNAL("internal"),
  LEVEL_1("level-1"),
  LEVEL_2("level-2"),
  LOCAL("local"),
  OSPF_EXTERNAL_TYPE_1("ospf-external-type-1"),
  OSPF_EXTERNAL_TYPE_2("ospf-external-type-2"),
  OSPF_INTER_AREA("ospf-inter-area"),
  OSPF_INTRA_AREA("ospf-intra-area"),
  OSPF_NSSA_TYPE_1("ospf-nssa-type-1"),
  OSPF_NSSA_TYPE_2("ospf-nssa-type-2"),
  TYPE_1("type-1"),
  TYPE_2("type-2");

  private static final Map<String, RouteType> _map = buildMap();

  private static Map<String, RouteType> buildMap() {
    ImmutableMap.Builder<String, RouteType> map = ImmutableMap.builder();
    for (RouteType value : RouteType.values()) {
      String name = value._name.toLowerCase();
      map.put(name, value);
    }
    return map.build();
  }

  @JsonCreator
  public static RouteType fromName(String name) {
    RouteType instance = _map.get(name.toLowerCase());
    if (instance == null) {
      throw new BatfishException(
          "No " + RouteType.class.getSimpleName() + " with name: \"" + name + "\"");
    }
    return instance;
  }

  private final String _name;

  RouteType(String name) {
    _name = name;
  }

  @JsonValue
  public String routeTypeName() {
    return _name;
  }
}
