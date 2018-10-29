package org.batfish.datamodel.acl;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.FilterResult;
import org.batfish.datamodel.LineAction;

public final class DeniedByIpAccessListLine implements TerminalTraceEvent {

  private static final String PROP_INDEX = "index";

  private static final String PROP_LINE_DESCRIPTION = "lineDescription";

  private static final String PROP_NAME = "name";

  private static final long serialVersionUID = 1L;

  @JsonCreator
  private static DeniedByIpAccessListLine create(
      @Nullable @JsonProperty(PROP_DESCRIPTION) String description,
      @Nullable @JsonProperty(PROP_INDEX) Integer index,
      @Nullable @JsonProperty(PROP_LINE_DESCRIPTION) String lineDescription,
      @Nullable @JsonProperty(PROP_NAME) String name) {
    checkArgument(description != null, "Missing %s", PROP_DESCRIPTION);
    checkArgument(index != null, "Missing %s", PROP_INDEX);
    checkArgument(lineDescription != null, "Missing %s", PROP_LINE_DESCRIPTION);
    checkArgument(name != null, "Missing %s", PROP_NAME);
    return new DeniedByIpAccessListLine(description, index, lineDescription, name);
  }

  private final String _description;

  private final int _index;

  private final String _lineDescription;

  private final String _name;

  /**
   * Creates a new {@link DeniedByIpAccessListLine} trace, using the source information if provided
   * to generate the description of the trace event. Note that either both or neither of {@code
   * sourceName} and {@code sourceType} must be present.
   */
  public static DeniedByIpAccessListLine create(
      int index,
      @Nonnull String lineDescription,
      @Nonnull String name,
      @Nullable String sourceName,
      @Nullable String sourceType) {
    checkArgument(
        (sourceName == null) == (sourceType == null),
        "Expected either both or neither of sourceName (%s) and sourceType (%s) to be null",
        sourceName,
        sourceType);
    String denier;
    if (sourceName != null) {
      denier = String.format("%s named %s", sourceType, sourceName);
    } else {
      denier = String.format("filter named %s", name);
    }
    String description =
        String.format("Flow denied by %s, index %d: %s", denier, index, lineDescription);

    return new DeniedByIpAccessListLine(description, index, lineDescription, name);
  }

  private DeniedByIpAccessListLine(
      @Nonnull String description,
      int index,
      @Nonnull String lineDescription,
      @Nonnull String name) {
    _description = description;
    _index = index;
    _lineDescription = lineDescription;
    _name = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DeniedByIpAccessListLine)) {
      return false;
    }
    DeniedByIpAccessListLine rhs = (DeniedByIpAccessListLine) obj;
    return _description.equals(rhs._description)
        && _index == rhs._index
        && _lineDescription.equals(rhs._lineDescription)
        && _name.equals(rhs._name);
  }

  @Override
  public String getDescription() {
    return _description;
  }

  @JsonProperty(PROP_INDEX)
  public int getIndex() {
    return _index;
  }

  @JsonProperty(PROP_LINE_DESCRIPTION)
  public @Nonnull String getLineDescription() {
    return _lineDescription;
  }

  @JsonProperty(PROP_NAME)
  public @Nonnull String getName() {
    return _name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_description, _index, _lineDescription, _name);
  }

  @Override
  public FilterResult toFilterResult() {
    return new FilterResult(_index, LineAction.DENY);
  }

  @Override
  public String toString() {
    return _description;
  }
}
