package org.batfish.z3;

import java.util.ArrayList;
import java.util.List;

public abstract class FirstUnsatQuerySynthesizer<KeyT, ResultT> extends BaseQuerySynthesizer {

  protected final KeyT _key;

  protected final List<ResultT> _resultsByQueryIndex;

  public FirstUnsatQuerySynthesizer(KeyT key) {
    _key = key;
    _resultsByQueryIndex = new ArrayList<>();
  }

  public KeyT getKey() {
    return _key;
  }

  public List<ResultT> getResultsByQueryIndex() {
    return _resultsByQueryIndex;
  }

  public abstract ReachabilityProgram synthesizeBaseProgram(Synthesizer synthesizer);
}
