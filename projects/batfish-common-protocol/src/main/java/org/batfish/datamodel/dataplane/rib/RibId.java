package org.batfish.datamodel.dataplane.rib;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.GenericRib;

/** Uniquely identifies a named {@link GenericRib} */
@ParametersAreNonnullByDefault
public final class RibId implements Serializable {
  private static final long serialVersionUID = 1;

  private static final String PROP_HOSTNAME = "hostname";
  private static final String PROP_VRF_NAME = "vrfname";
  private static final String PROP_RIB_NAME = "ribname";
  public static final String DEFAULT_RIB_NAME = "default";

  private final String _hostname;
  private final String _vrfName;
  private final String _ribName;

  /** Create a new RibId */
  public RibId(String hostname, String vrfName, String ribName) {
    _hostname = hostname;
    _vrfName = vrfName;
    _ribName = ribName;
  }

  @JsonCreator
  private static RibId create(
      @Nullable @JsonProperty(PROP_HOSTNAME) String hostname,
      @Nullable @JsonProperty(PROP_VRF_NAME) String vrfName,
      @Nullable @JsonProperty(PROP_RIB_NAME) String ribName) {
    checkArgument(hostname != null, "Missing %s", PROP_HOSTNAME);
    checkArgument(vrfName != null, "Missing %s", PROP_VRF_NAME);
    checkArgument(ribName != null, "Missing %s", PROP_RIB_NAME);
    return new RibId(hostname, vrfName, ribName);
  }

  @JsonProperty(PROP_HOSTNAME)
  public String getHostname() {
    return _hostname;
  }

  @JsonProperty(PROP_VRF_NAME)
  public String getVrfName() {
    return _vrfName;
  }

  @JsonProperty(PROP_RIB_NAME)
  public String getRibName() {
    return _ribName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RibId)) {
      return false;
    }
    RibId ribId = (RibId) o;
    return Objects.equals(_hostname, ribId._hostname)
        && Objects.equals(_vrfName, ribId._vrfName)
        && Objects.equals(_ribName, ribId._ribName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_hostname, _vrfName, _ribName);
  }
}
