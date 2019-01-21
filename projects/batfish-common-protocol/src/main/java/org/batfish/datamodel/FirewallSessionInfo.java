package org.batfish.datamodel;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Data that determines how to create sessions for flows that routed out interfaces. In particular,
 * sessions are needed for bidirectional traceroute and reachability, specifically for return flows.
 */
@ParametersAreNonnullByDefault
public final class FirewallSessionInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final String PROP_SESSION_INTERFACES = "sessionInterfaces";
  private static final String PROP_INCOMING_ACL_NAME = "incomingAclName";
  private static final String PROP_OUTGOING_ACL_NAME = "outgoingAclName";

  private final SortedSet<String> _sessionInterfaces;
  private final @Nullable String _incomingAclName;
  private final @Nullable String _outgoingAclName;

  public FirewallSessionInfo(
      Iterable<String> sessionInterfaces,
      @Nullable String incomingAclName,
      @Nullable String outgoingAclName) {
    _sessionInterfaces = ImmutableSortedSet.copyOf(sessionInterfaces);
    _incomingAclName = incomingAclName;
    _outgoingAclName = outgoingAclName;
  }

  @JsonCreator
  private static FirewallSessionInfo jsonCreator(
      @JsonProperty(PROP_SESSION_INTERFACES) @Nullable Set<String> sessionIntefaces,
      @JsonProperty(PROP_INCOMING_ACL_NAME) @Nullable String incomingAclName,
      @JsonProperty(PROP_OUTGOING_ACL_NAME) @Nullable String outgoingAclName) {
    checkNotNull(sessionIntefaces, PROP_SESSION_INTERFACES + " cannot be null");
    return new FirewallSessionInfo(sessionIntefaces, incomingAclName, outgoingAclName);
  }

  /** The set of interfaces through which return flows can enter. */
  @JsonProperty(PROP_SESSION_INTERFACES)
  public Set<String> getSessionInterfaces() {
    return _sessionInterfaces;
  }

  /** The name of the incoming ACL for sessions that enter this interface. */
  @JsonProperty(PROP_INCOMING_ACL_NAME)
  public @Nullable String getIncomingAclName() {
    return _incomingAclName;
  }

  /** The name of the outgoing ACL for sessions that exit this interface. */
  @JsonProperty(PROP_OUTGOING_ACL_NAME)
  public @Nullable String getOutgoingAclName() {
    return _outgoingAclName;
  }
}
