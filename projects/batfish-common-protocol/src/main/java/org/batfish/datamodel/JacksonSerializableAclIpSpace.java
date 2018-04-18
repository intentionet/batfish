package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.visitors.GenericIpSpaceVisitor;

public class JacksonSerializableAclIpSpace implements JacksonSerializableIpSpace {

  private static class JacksonSerializableAclIpSpaceLine {

    private static final String PROP_ACTION = "action";

    private static final String PROP_IP_SPACE = "ipSpace";

    private final LineAction _action;

    private final JacksonSerializableIpSpace _ipSpace;

    private JacksonSerializableAclIpSpaceLine(AclIpSpaceLine aclIpSpaceLine) {
      _action = aclIpSpaceLine.getAction();
      _ipSpace =
          IpSpaceToJacksonSerializableIpSpace.toJacksonSerializableIpSpace(
              aclIpSpaceLine.getIpSpace());
    }

    @JsonCreator
    private JacksonSerializableAclIpSpaceLine(
        @JsonProperty(PROP_ACTION) LineAction action,
        @JsonProperty(PROP_IP_SPACE) JacksonSerializableIpSpace ipSpace) {
      _action = action;
      _ipSpace = ipSpace;
    }

    @JsonProperty(PROP_ACTION)
    private LineAction getAction() {
      return _action;
    }

    @JsonProperty(PROP_IP_SPACE)
    private JacksonSerializableIpSpace getIpSpace() {
      return _ipSpace;
    }
  }

  private static final String PROP_LINES = "lines";

  private final List<JacksonSerializableAclIpSpaceLine> _lines;

  public JacksonSerializableAclIpSpace(AclIpSpace aclIpSpace) {
    _lines =
        aclIpSpace
            .getLines()
            .stream()
            .map(JacksonSerializableAclIpSpaceLine::new)
            .collect(ImmutableList.toImmutableList());
  }

  @JsonCreator
  private JacksonSerializableAclIpSpace(
      @JsonProperty(PROP_LINES) List<JacksonSerializableAclIpSpaceLine> lines) {
    _lines = lines;
  }

  @Override
  public <R> R accept(GenericIpSpaceVisitor<R> visitor) {
    throw new BatfishException("Should never be called");
  }

  @Override
  public IpSpace complement() {
    throw new BatfishException("Should never be called");
  }

  @Override
  public boolean containsIp(Ip ip) {
    throw new BatfishException("Should never be called");
  }

  @JsonProperty(PROP_LINES)
  public List<JacksonSerializableAclIpSpaceLine> getLines() {
    return _lines;
  }

  @Override
  public IpSpace unwrap() {
    return AclIpSpace.builder()
        .setLines(
            _lines
                .stream()
                .map(
                    wrappedLine ->
                        AclIpSpaceLine.builder()
                            .setAction(wrappedLine.getAction())
                            .setIpSpace(wrappedLine.getIpSpace().unwrap())
                            .build())
                .collect(ImmutableList.toImmutableList()))
        .build();
  }
}
