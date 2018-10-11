package org.batfish.datamodel.flow;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.pojo.Node;

/** Represents a hop in a particular {@link Trace} of a {@link Flow} */
public class Hop implements Serializable {

  private static final String PROP_NODE = "node";
  private static final String PROP_STEPS = "steps";

  /** */
  private static final long serialVersionUID = 1L;

  /** Name of the node for this {@link Hop} */
  private @Nullable Node _node;

  /** {@link List} of {@link Step} present for the given {@link Hop} */
  private List<Step> _steps;

  public Hop(@Nonnull Node node, @Nonnull List<Step> steps) {
    _node = node;
    _steps = steps;
  }

  @JsonCreator
  private static Hop jsonCreator(
      @JsonProperty(PROP_NODE) @Nullable Node node,
      @JsonProperty(PROP_STEPS) @Nullable List<Step> steps) {
    checkArgument(node != null, "Hop should be have a node present");
    return new Hop(node, firstNonNull(steps, ImmutableList.of()));
  }

  @JsonProperty(PROP_NODE)
  @Nullable
  public Node getNode() {
    return _node;
  }

  @JsonProperty(PROP_STEPS)
  public List<Step> getSteps() {
    return _steps;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Hop)) {
      return false;
    }
    Hop other = (Hop) o;
    return Objects.equals(_node, other._node) && Objects.equals(_steps, other._steps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_node, _steps);
  }
}
