package org.batfish.grammar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum RoutingTableFormat {
  EMPTY("empty"),
  EOS("eos"),
  IOS("ios"),
  NXOS("nxos"),
  UNKNOWN("unknown");

  private static final Map<String, RoutingTableFormat> _map = buildMap();

  private static Map<String, RoutingTableFormat> buildMap() {
    ImmutableMap.Builder<String, RoutingTableFormat> map = ImmutableMap.builder();
    for (RoutingTableFormat value : RoutingTableFormat.values()) {
      String name = value._name;
      map.put(name, value);
    }
    return map.build();
  }

  @JsonCreator
  public static RoutingTableFormat fromName(String name) {
    RoutingTableFormat instance = _map.get(name.toLowerCase());
    if (instance == null) {
      throw new BatfishException(
          "No " + RoutingTableFormat.class.getSimpleName() + " with name: '" + name + "'");
    }
    return instance;
  }

  private final String _name;

  RoutingTableFormat(String name) {
    _name = name;
  }

  @JsonValue
  public String routingTableFormatName() {
    return _name;
  }
}
