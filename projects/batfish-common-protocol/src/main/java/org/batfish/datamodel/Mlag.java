package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Abstracted configuration for Multi-chassis link aggregation, as configured on a single device */
@ParametersAreNonnullByDefault
public class Mlag implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final String PROP_ID = "id";
  private static final String PROP_PEER_ADDRESS = "peerAddress";
  private static final String PROP_PEER_INTERFACE = "peerInterface";
  private static final String PROP_LOCAL_INTERFACE = "localInterface";
  private static final String PROP_ACCESS_INTERFACES = "accessInterfaces";

  private final String _id;
  @Nullable private final Ip _peerAddress;
  @Nullable private final String _peerInterface;
  @Nullable private final String _localInterface;
  private final Set<String> _accessInterfaces;

  public Mlag(
      String id,
      @Nullable Ip peerAddress,
      @Nullable String peerInterface,
      @Nullable String localInterface,
      Set<String> accessInterfaces) {
    _id = id;
    _peerAddress = peerAddress;
    _peerInterface = peerInterface;
    _localInterface = localInterface;
    _accessInterfaces = ImmutableSet.copyOf(accessInterfaces);
  }

  @JsonCreator
  private static Mlag create(
      @Nullable @JsonProperty(PROP_ID) String id,
      @Nullable @JsonProperty(PROP_PEER_ADDRESS) Ip peerAddress,
      @Nullable @JsonProperty(PROP_PEER_INTERFACE) String peerInterface,
      @Nullable @JsonProperty(PROP_LOCAL_INTERFACE) String localInterface,
      @Nullable @JsonProperty(PROP_ACCESS_INTERFACES) Set<String> accessInterfaces) {
    checkArgument(id != null, "Missing Mlag %s", PROP_ID);
    return new Mlag(
        id,
        peerAddress,
        peerInterface,
        localInterface,
        firstNonNull(accessInterfaces, ImmutableSet.of()));
  }

  @JsonProperty(PROP_ID)
  public String getId() {
    return _id;
  }

  @Nullable
  @JsonProperty(PROP_PEER_ADDRESS)
  public Ip getPeerAddress() {
    return _peerAddress;
  }

  @Nullable
  @JsonProperty(PROP_PEER_INTERFACE)
  public String getPeerInterface() {
    return _peerInterface;
  }

  @Nullable
  @JsonProperty(PROP_LOCAL_INTERFACE)
  public String getLocalInterface() {
    return _localInterface;
  }

  @JsonProperty(PROP_ACCESS_INTERFACES)
  public Set<String> getAccessInterfaces() {
    return _accessInterfaces;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Mlag)) {
      return false;
    }
    Mlag mlag = (Mlag) o;
    return _id.equals(mlag._id)
        && Objects.equals(_peerAddress, mlag._peerAddress)
        && Objects.equals(_peerInterface, mlag._peerInterface)
        && Objects.equals(_localInterface, mlag._localInterface)
        && _accessInterfaces.equals(mlag._accessInterfaces);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_id, _peerAddress, _peerInterface, _localInterface, _accessInterfaces);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    @Nullable private String _id;
    @Nullable private Ip _peerAddress;
    @Nullable private String _peerInterface;
    @Nullable private String _localInterface;
    @Nullable private Set<String> _accessInterfaces;

    private Builder() {}

    public Builder setId(@Nonnull String id) {
      this._id = id;
      return this;
    }

    public Builder setPeerAddress(Ip peerAddress) {
      this._peerAddress = peerAddress;
      return this;
    }

    public Builder setPeerInterface(String peerInterface) {
      this._peerInterface = peerInterface;
      return this;
    }

    public Builder setLocalInterface(String localInterface) {
      this._localInterface = localInterface;
      return this;
    }

    public Builder setAccessInterfaces(Set<String> accessInterfaces) {
      this._accessInterfaces = accessInterfaces;
      return this;
    }

    public Mlag build() throws IllegalArgumentException {
      checkArgument(_id != null, "Missing Mlag %s", PROP_ID);
      return new Mlag(
          _id,
          _peerAddress,
          _peerInterface,
          _localInterface,
          firstNonNull(_accessInterfaces, ImmutableSet.of()));
    }
  }
}
