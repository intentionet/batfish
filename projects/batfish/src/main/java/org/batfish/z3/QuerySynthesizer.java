package org.batfish.z3;

/** A synthesizer that generates the query portion of a Batfish reachability AST program */
public interface QuerySynthesizer {

  boolean getNegate();

  /**
   * Generate a reachability program populated only with the incremental rules and queries relevant
   * to this particular query type.
   */
  ReachabilityProgram getReachabilityProgram(SynthesizerInput input);
}
