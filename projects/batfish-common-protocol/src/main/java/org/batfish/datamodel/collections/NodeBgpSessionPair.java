package org.batfish.datamodel.collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.Pair;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.Configuration;

public class NodeBgpSessionPair extends Pair<Configuration, BgpNeighbor> {

  private static final String PROP_NODE = "node";

  private static final long serialVersionUID = 1L;

  private static final String PROP_SESSION = "session";

  @JsonCreator
  public NodeBgpSessionPair(
      @JsonProperty(PROP_NODE) Configuration t1, @JsonProperty(PROP_SESSION) BgpNeighbor t2) {
    super(t1, t2);
  }

  @JsonProperty(PROP_NODE)
  public Configuration getNode() {
    return _first;
  }

  @JsonProperty(PROP_SESSION)
  public BgpNeighbor getSession() {
    return _second;
  }
}
