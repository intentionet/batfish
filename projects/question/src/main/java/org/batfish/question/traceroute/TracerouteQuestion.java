package org.batfish.question.traceroute;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.questions.Question;

/**
 * A question to perform a traceroute.
 *
 * <p>This question performs a virtual traceroute in the network from ingress node. The destination
 * IP is randomly picked if not explicitly specified. Other IP headers are also randomly picked if
 * unspecified, with a bias toward generating packets similar to a real traceroute (see below).
 *
 * <p>Unlike a real traceroute, this traceroute is directional. That is, for it to succeed, the
 * reverse connectivity is not needed. This feature can help debug connectivity issues by decoupling
 * the two directions.
 */
public final class TracerouteQuestion extends Question {

  private static final String PROP_IGNORE_ACLS = "ignoreAcls";
  private static final String PROP_SOURCE_LOCATION = "startLocation";
  private static final String PROP_HEADER_CONSTRAINTS = "headers";

  private final boolean _ignoreAcls;
  private final @Nullable String _sourceLocationStr;
  private final PacketHeaderConstraints _headerConstraints;

  @JsonCreator
  TracerouteQuestion(
      @JsonProperty(PROP_IGNORE_ACLS) boolean ignoreAcls,
      @JsonProperty(PROP_SOURCE_LOCATION) @Nullable String sourceLocationStr,
      @JsonProperty(PROP_HEADER_CONSTRAINTS) @Nullable PacketHeaderConstraints headerConstraints) {
    _ignoreAcls = ignoreAcls;
    _sourceLocationStr = sourceLocationStr;
    _headerConstraints = firstNonNull(headerConstraints, PacketHeaderConstraints.unconstrained());
  }

  TracerouteQuestion() {
    this(false, null, null);
  }

  @Override
  public boolean getDataPlane() {
    return true;
  }

  @Nonnull
  @JsonProperty(PROP_HEADER_CONSTRAINTS)
  PacketHeaderConstraints getHeaderConstraints() {
    return _headerConstraints;
  }

  @JsonProperty(PROP_IGNORE_ACLS)
  public boolean getIgnoreAcls() {
    return _ignoreAcls;
  }

  @JsonProperty(PROP_SOURCE_LOCATION)
  public @Nullable String getSourceLocationStr() {
    return _sourceLocationStr;
  }

  @Override
  public String getName() {
    return "traceroute";
  }

  @Override
  public String prettyPrint() {
    try {
      StringBuilder sb = new StringBuilder();
      sb.append(String.format("traceroute %s", prettyPrintBase()));
      if (_sourceLocationStr != null) {
        sb.append(String.format(", %s=%s", PROP_SOURCE_LOCATION, _sourceLocationStr));
      }
      return sb.toString();
    } catch (Exception e) {
      try {
        return "Pretty printing failed. Printing Json\n" + toJsonString();
      } catch (BatfishException e1) {
        throw new BatfishException("Both pretty and json printing failed\n");
      }
    }
  }
}
