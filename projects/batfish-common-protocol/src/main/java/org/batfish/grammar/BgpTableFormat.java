package org.batfish.grammar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;

public enum BgpTableFormat {
  EMPTY("empty"),
  EOS("eos"),
  EOS_DETAIL("eos_detail"),
  UNKNOWN("unknown");

  private static final Map<String, BgpTableFormat> _map = buildMap();

  private static Map<String, BgpTableFormat> buildMap() {
    ImmutableMap.Builder<String, BgpTableFormat> map = ImmutableMap.builder();
    for (BgpTableFormat value : BgpTableFormat.values()) {
      String name = value._name;
      map.put(name, value);
    }
    return map.build();
  }

  @JsonCreator
  public static BgpTableFormat fromName(String name) {
    BgpTableFormat instance = _map.get(name.toLowerCase());
    if (instance == null) {
      throw new BatfishException(
          "No " + BgpTableFormat.class.getSimpleName() + " with name: '" + name + "'");
    }
    return instance;
  }

  private final String _name;

  BgpTableFormat(String name) {
    _name = name;
  }

  @JsonValue
  public String bgpTableFormatName() {
    return _name;
  }
}
