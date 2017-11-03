package org.batfish.role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.batfish.common.BatfishException;

// the hypotheses used to perform outlier detection
public enum OutliersHypothesis {
  // the hypothesis that same-named structures should have the same definition
  SAME_DEFINITION("sameDefinition"),
  // the hypothesis that all given nodes should define structures of the same names
  SAME_NAME("sameName"),
  // the hypothesis that all given nodes should have the same set of protocol-specific servers
  // (e.g., DNS servers, NTP servers, etc.)
  SAME_SERVERS("sameServers");

  private static final Map<String, OutliersHypothesis> _map = buildMap();

  private static synchronized Map<String, OutliersHypothesis> buildMap() {
    Map<String, OutliersHypothesis> map = new HashMap<>();
    for (OutliersHypothesis value : OutliersHypothesis.values()) {
      String name = value._name;
      map.put(name, value);
    }
    return Collections.unmodifiableMap(map);
  }

  @JsonCreator
  public static OutliersHypothesis fromName(String name) {
    OutliersHypothesis instance = _map.get(name);
    if (instance == null) {
      throw new BatfishException(
          "No " + OutliersHypothesis.class.getSimpleName() + " with name: '" + name + "'");
    }
    return instance;
  }

  private final String _name;

  private OutliersHypothesis(String name) {
    _name = name;
  }

  @JsonValue
  public String hypothesisName() {
    return _name;
  }
}
