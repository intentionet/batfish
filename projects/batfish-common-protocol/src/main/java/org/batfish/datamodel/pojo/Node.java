package org.batfish.datamodel.pojo;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.DeviceType;

public final class Node extends BfObject {
  private static final String PROP_NAME = "name";
  private static final String PROP_MODEL = "model";
  private static final String PROP_TYPE = "type";

  @Nonnull private final String _name;
  @Nullable private DeviceModel _model;
  @Nullable private DeviceType _type;

  @VisibleForTesting
  Node(
      @Nonnull String name,
      @Nullable String id,
      @Nullable DeviceModel model,
      @Nullable DeviceType type) {
    super(firstNonNull(id, makeId(name)));
    _name = name.toLowerCase();
    _model = model;
    _type = type;
  }

  @JsonCreator
  private static Node jsonCreator(
      @Nullable @JsonProperty(PROP_NAME) String name,
      @Nullable @JsonProperty(PROP_ID) String id,
      @Nullable @JsonProperty(PROP_MODEL) DeviceModel model,
      @Nullable @JsonProperty(PROP_TYPE) DeviceType type) {
    checkArgument(name != null, "Missing: %s", PROP_NAME);
    return new Node(name, id, model, type);
  }

  public Node(String name) {
    this(name, null, null, null);
  }

  public Node(String name, DeviceModel model, DeviceType type) {
    this(name, null, model, type);
  }

  public static String makeId(String name) {
    return "node-" + name.toLowerCase();
  }

  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }

  @JsonProperty(PROP_MODEL)
  public DeviceModel getModel() {
    return _model;
  }

  @JsonProperty(PROP_TYPE)
  public DeviceType getType() {
    return _type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof Node)) {
      return false;
    }
    Node node = (Node) o;
    return _name.equals(node._name)
        && _model == node._model
        && _type == node._type
        && Objects.equals(getId(), node.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, getId(), _model, _type);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("id", getId())
        .add("name", _name)
        .add("model", _model)
        .add("type", _type)
        .toString();
  }
}
