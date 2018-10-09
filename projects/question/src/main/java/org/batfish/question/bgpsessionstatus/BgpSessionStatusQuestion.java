package org.batfish.question.bgpsessionstatus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.questions.NodesSpecifier;

/** Based on node configurations, determines the status of IBGP and EBGP sessions. */
public class BgpSessionStatusQuestion extends BgpSessionQuestion {

  /** Create a new BGP session status question with default parameters. */
  public BgpSessionStatusQuestion() {
    super(null, null, null, null, null);
  }

  /**
   * Create a new BGP session status question.
   *
   * @param bgpGroups only look at peers that belong to a given named BGP group.
   * @param nodes Regular expression to match the nodes names for one end of the sessions. Default
   *     is '.*' (all nodes).
   * @param remoteNodes Regular expression to match the nodes names for the other end of the
   *     sessions. Default is '.*' (all nodes).
   * @param status Regular expression to match status type (see {@link
   *     BgpSessionInfo.SessionStatus})
   * @param type Regular expression to match session type (see {@link SessionType})
   */
  @JsonCreator
  public BgpSessionStatusQuestion(
      @Nullable @JsonProperty(PROP_BGP_GROUPS) SortedSet<String> bgpGroups,
      @Nullable @JsonProperty(PROP_NODES) NodesSpecifier nodes,
      @Nullable @JsonProperty(PROP_REMOTE_NODES) NodesSpecifier remoteNodes,
      @Nullable @JsonProperty(PROP_STATUS) String status,
      @Nullable @JsonProperty(PROP_TYPE) String type) {
    super(bgpGroups, nodes, remoteNodes, status, type);
  }

  @Override
  public boolean getDataPlane() {
    return true;
  }

  @JsonProperty(PROP_BGP_GROUPS)
  private SortedSet<String> getBgpGroups() {
    return _bgpGroups;
  }

  @Override
  public String getName() {
    return "bgpSessionStatusNew";
  }

  @JsonProperty(PROP_NODES)
  public NodesSpecifier getNodes() {
    return _nodes;
  }

  @JsonProperty(PROP_REMOTE_NODES)
  public NodesSpecifier getRemoteNodes() {
    return _remoteNodes;
  }

  @JsonProperty(PROP_STATUS)
  private String getStatus() {
    return _status.toString();
  }

  @JsonProperty(PROP_TYPE)
  private String getType() {
    return _type.toString();
  }
}
