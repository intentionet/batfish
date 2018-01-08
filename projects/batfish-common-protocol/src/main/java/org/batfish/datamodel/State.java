package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum State {
  ESTABLISHED(1),
  INVALID(3),
  NEW(0),
  RELATED(2);

  private static final Map<Integer, State> _map = buildNumberMap();

  private static final Map<String, State> _nameMap = buildNameMap();

  private static Map<String, State> buildNameMap() {
    ImmutableMap.Builder<String, State> map = ImmutableMap.builder();
    for (State value : State.values()) {
      String name = value.name().toLowerCase();
      map.put(name, value);
    }
    return map.build();
  }

  private static Map<Integer, State> buildNumberMap() {
    ImmutableMap.Builder<Integer, State> map = ImmutableMap.builder();
    for (State value : State.values()) {
      int num = value._num;
      map.put(num, value);
    }
    return map.build();
  }

  public static State fromNum(int num) {
    State instance = _map.get(num);
    if (instance == null) {
      throw new BatfishException("Not a valid state number: '" + num + "'");
    }
    return instance;
  }

  @JsonCreator
  public static State fromString(String name) {
    State state = _nameMap.get(name.toLowerCase());
    if (state == null) {
      throw new BatfishException("No state with name: '" + name + "'");
    }
    return state;
  }

  private final int _num;

  State(int num) {
    _num = num;
  }

  public int number() {
    return _num;
  }
}
