package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.visitors.GenericIpSpaceVisitor;

public class IpSpaceReference extends IpSpace {

  private static final String PROP_DESCRIPTION = "description";

  private static final String PROP_NAME = "name";

  /** */
  private static final long serialVersionUID = 1L;

  private final @Nullable String _description;

  private final String _name;

  public IpSpaceReference(@Nonnull String name) {
    this(name, null);
  }

  /** A reference to a named {@link IpSpace} */
  @JsonCreator
  public IpSpaceReference(
      @JsonProperty(PROP_NAME) @Nonnull String name,
      @JsonProperty(PROP_DESCRIPTION) @Nullable String description) {
    _name = name;
    _description = description;
  }

  @Override
  public <R> R accept(GenericIpSpaceVisitor<R> visitor) {
    return visitor.visitIpSpaceReference(this);
  }

  @Override
  protected int compareSameClass(IpSpace o) {
    return _name.compareTo(((IpSpaceReference) o)._name);
  }

  @Override
  public IpSpace complement() {
    return AclIpSpace.rejecting(this).thenPermitting(UniverseIpSpace.INSTANCE).build();
  }

  @Override
  public boolean containsIp(@Nonnull Ip ip, @Nonnull Map<String, IpSpace> namedIpSpaces) {
    return namedIpSpaces.containsKey(_name)
        && namedIpSpaces.get(_name).containsIp(ip, namedIpSpaces);
  }

  @Override
  protected boolean exprEquals(Object o) {
    IpSpaceReference rhs = (IpSpaceReference) o;
    return _name.equals(rhs._name) && Objects.equals(_description, rhs._description);
  }

  @JsonProperty(PROP_DESCRIPTION)
  public @Nullable String getDescription() {
    return _description;
  }

  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, _description);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass())
        .omitNullValues()
        .add(PROP_NAME, _name)
        .add(PROP_DESCRIPTION, _description)
        .toString();
  }
}
