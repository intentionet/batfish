package org.batfish.z3;

import java.util.ArrayList;
import java.util.List;

public abstract class SatQuerySynthesizer<KeyT> extends BaseQuerySynthesizer {

  protected final List<KeyT> _keys;

  public SatQuerySynthesizer() {
    _keys = new ArrayList<>();
  }

  public List<KeyT> getKeys() {
    return _keys;
  }

  public abstract ReachabilityProgram synthesizeBaseProgram(Synthesizer synthesizer);
}
