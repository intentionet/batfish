package org.batfish.question.ipowners;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.datamodel.questions.Question;

/** Queries the computed IP owners across all nodes/VRFs/Interfaces in the network. */
public class IpOwnersQuestion extends Question {

  private static final String PROP_DUPLICATES_ONLY = "duplicatesOnly";

  private static final String PROP_EXCLUDE_INACTIVE = "excludeInactive";

  private static final String QUESTION_NAME = "ipowners";

  /** Whether to return duplicate IPs (owned by multiple nodes) only. */
  private boolean _duplicatesOnly;

  /** Whether to exclude inactive interfaces from the computation */
  private boolean _excludeInactive;

  @JsonCreator
  public IpOwnersQuestion(
      @JsonProperty(PROP_DUPLICATES_ONLY) boolean duplicatesOnly,
      @JsonProperty(PROP_EXCLUDE_INACTIVE) boolean excludeInactive) {
    _excludeInactive = excludeInactive;
    _duplicatesOnly = duplicatesOnly;
  }

  /** Initialize with default values */
  IpOwnersQuestion() {
    this(false, false);
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return QUESTION_NAME;
  }

  @JsonProperty(PROP_DUPLICATES_ONLY)
  public boolean getDuplicatesOnly() {
    return _duplicatesOnly;
  }

  @JsonProperty(PROP_EXCLUDE_INACTIVE)
  public boolean getExcludeInactive() {
    return _excludeInactive;
  }
}
