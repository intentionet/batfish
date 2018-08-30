package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum NeighborType {
  EBGP("ebgp"),
  EIGRP("eigrp"),
  IBGP("ibgp"),
  ISIS("isis"),
  LAYER1("layer1"),
  LAYER2("layer2"),
  LAYER3("layer3"),
  OSPF("ospf"),
  RIP("rip");

  private static final Map<String, NeighborType> _map = buildMap();

  private static Map<String, NeighborType> buildMap() {
    ImmutableMap.Builder<String, NeighborType> map = ImmutableMap.builder();
    for (NeighborType value : NeighborType.values()) {
      String name = value._name;
      map.put(name, value);
    }
    return map.build();
  }

  @JsonCreator
  public static NeighborType fromName(String name) {
    NeighborType instance = _map.get(name.toLowerCase());
    if (instance == null) {
      throw new BatfishException(
          "No " + NeighborType.class.getSimpleName() + " with name: '" + name + "'");
    }
    return instance;
  }

  private final String _name;

  NeighborType(String name) {
    _name = name;
  }

  @JsonValue
  public String neighborTypeName() {
    return _name;
  }
}
