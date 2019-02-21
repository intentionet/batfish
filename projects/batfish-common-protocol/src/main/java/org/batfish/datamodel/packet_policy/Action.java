package org.batfish.datamodel.packet_policy;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A type of {@link Statement policy statement} that signifies an action should be taken on a
 * packet/flow
 */
public interface Action {

  /** Whether this action stops evaluation of the remaining statements */
  @JsonIgnore
  boolean isTerminal();
}
