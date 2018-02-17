package org.batfish.z3;

public interface QuerySynthesizer {

  boolean getNegate();

  ReachabilityProgram getReachabilityProgram(SynthesizerInput input);
}
