package org.batfish.datamodel.flow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.io.Serializable;
import javax.annotation.Nonnull;

/**
 * Represents the detail of a {@link Step}, contains metadata about the entities involved in the
 * {@link Step}
 */
@JsonSchemaDescription("Represents the detail of a step")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class StepDetail implements Serializable {

  protected static final String PROP_NAME = "name";

  /** */
  private static final long serialVersionUID = 1L;

  private final String _name;

  @JsonCreator
  public StepDetail(@JsonProperty(PROP_NAME) @Nonnull String name) {
    _name = name;
  }

  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }
}
