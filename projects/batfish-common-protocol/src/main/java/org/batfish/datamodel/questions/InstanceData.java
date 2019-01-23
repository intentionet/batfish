package org.batfish.datamodel.questions;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BfConsts;

/**
 * Configuration and template description for a specific instance of a {@link
 * org.batfish.datamodel.questions.Question}.
 */
public final class InstanceData {

  private String _description;

  private String _instanceName;

  private String _longDescription;

  private @Nonnull ImmutableList<String> _orderedVariableNames;

  private SortedSet<String> _tags;

  private SortedMap<String, Variable> _variables;

  public InstanceData() {
    _orderedVariableNames = ImmutableList.of();
    _tags = new TreeSet<>();
    _variables = new TreeMap<>();
  }

  @JsonProperty(BfConsts.PROP_DESCRIPTION)
  public String getDescription() {
    return _description;
  }

  @JsonProperty(BfConsts.PROP_INSTANCE_NAME)
  public String getInstanceName() {
    return _instanceName;
  }

  @JsonProperty(BfConsts.PROP_LONG_DESCRIPTION)
  public String getLongDescription() {
    return _longDescription;
  }

  @JsonProperty(BfConsts.PROP_ORDERED_VARIABLE_NAMES)
  @Nonnull
  public ImmutableList<String> getOrderedVariableNames() {
    return _orderedVariableNames;
  }

  @JsonProperty(BfConsts.PROP_TAGS)
  public SortedSet<String> getTags() {
    return _tags;
  }

  @JsonProperty(BfConsts.PROP_VARIABLES)
  public SortedMap<String, Variable> getVariables() {
    return _variables;
  }

  @JsonProperty(BfConsts.PROP_DESCRIPTION)
  public void setDescription(String description) {
    _description = description;
  }

  @JsonProperty(BfConsts.PROP_INSTANCE_NAME)
  public void setInstanceName(String instanceName) {
    _instanceName = instanceName;
  }

  @JsonProperty(BfConsts.PROP_LONG_DESCRIPTION)
  public void setLongDescription(String longDescription) {
    _longDescription = longDescription;
  }

  @JsonProperty(BfConsts.PROP_ORDERED_VARIABLE_NAMES)
  public void setOrderedVariableNames(@Nullable ImmutableList<String> orderedVariableNames) {
    _orderedVariableNames = firstNonNull(orderedVariableNames, ImmutableList.of());
  }

  @JsonProperty(BfConsts.PROP_TAGS)
  public void setTags(SortedSet<String> tags) {
    _tags = tags;
  }

  @JsonProperty(BfConsts.PROP_VARIABLES)
  public void setVariables(SortedMap<String, Variable> variables) {
    _variables = variables;
  }
}
