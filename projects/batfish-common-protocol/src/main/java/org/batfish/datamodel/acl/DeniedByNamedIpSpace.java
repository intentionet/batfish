package org.batfish.datamodel.acl;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.TraceElement;

public final class DeniedByNamedIpSpace extends IpSpaceTraceEvent {
  private static final String PROP_DESCRIPTION = "description";
  private static final String PROP_NAME = "name";

  private static String computeDescription(
      @Nonnull Ip ip,
      @Nonnull String ipDescription,
      @Nonnull String ipSpaceDescription,
      @Nullable TraceElement traceElement,
      @Nonnull String name) {
    if (traceElement != null) {
      return traceElement.toString();
    }
    String type = IpSpace.class.getSimpleName();
    String displayName = name;
    String description = String.format(": %s", ipSpaceDescription);
    return String.format(
        "%s %s denied by '%s' named '%s'%s", ipDescription, ip, type, displayName, description);
  }

  @JsonCreator
  private static DeniedByNamedIpSpace create(
      @JsonProperty(PROP_DESCRIPTION) String description,
      @JsonProperty(PROP_IP) Ip ip,
      @JsonProperty(PROP_IP_DESCRIPTION) String ipDescription,
      @JsonProperty(PROP_NAME) String name) {
    return new DeniedByNamedIpSpace(
        requireNonNull(description),
        requireNonNull(ip),
        requireNonNull(ipDescription),
        requireNonNull(name));
  }

  private final String _name;

  public DeniedByNamedIpSpace(
      @Nonnull Ip ip,
      @Nonnull String ipDescription,
      @Nonnull String ipSpaceDescription,
      @Nullable TraceElement traceElement,
      @Nonnull String name) {
    this(
        computeDescription(ip, ipDescription, ipSpaceDescription, traceElement, name),
        ip,
        ipDescription,
        name);
  }

  private DeniedByNamedIpSpace(
      @Nonnull String description,
      @Nonnull Ip ip,
      @Nonnull String ipDescription,
      @Nonnull String name) {
    super(description, ip, ipDescription);
    _name = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DeniedByNamedIpSpace)) {
      return false;
    }
    DeniedByNamedIpSpace rhs = (DeniedByNamedIpSpace) obj;
    return getDescription().equals(rhs.getDescription())
        && getIp().equals(rhs.getIp())
        && getIpDescription().equals(rhs.getIpDescription())
        && _name.equals(rhs._name);
  }

  @JsonProperty(PROP_NAME)
  public @Nonnull String getName() {
    return _name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDescription(), getIp(), getIpDescription(), _name);
  }
}
