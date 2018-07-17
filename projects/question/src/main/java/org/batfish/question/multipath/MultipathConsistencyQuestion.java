package org.batfish.question.multipath;

import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.Location;

/**
 * A zero-input question to check for multipath inconsistencies. It computes all-pairs reachability
 * from any {@link Location source} to any destination, and returns traces for all detected
 * multipath inconsistencies.
 *
 * In the future, we can consider adding a flag that would stop the reachability analysis after the
 * first multipath inconsistency is found.
 */
public class MultipathConsistencyQuestion extends Question {
  @Override
  public boolean getDataPlane() {
    return true;
  }

  @Override
  public String getName() {
    return "multipath";
  }
}
