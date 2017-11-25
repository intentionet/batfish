package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.batfish.common.util.ComparableStructure;

public class IkeGateway extends ComparableStructure<String> {

  private static final String PROP_EXTERNAL_INTERFACE = "externalInterface";

  private static final String PROP_IKE_POLICY = "ikePolicy";

  /** */
  private static final long serialVersionUID = 1L;

  private Ip _address;

  private Interface _externalInterface;

  private transient String _externalInterfaceName;

  private IkePolicy _ikePolicy;

  private transient String _ikePolicyName;

  private Ip _localAddress;

  private String _localId;

  private String _remoteId;

  @JsonCreator
  public IkeGateway(@JsonProperty(PROP_NAME) String name) {
    super(name);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof IkeGateway)) {
      return false;
    }
    IkeGateway other = (IkeGateway) o;
    if (!other._address.equals(_address)) {
      return false;
    }
    if (!other._externalInterface.equals(_externalInterface)) {
      return false;
    }
    if (!other._ikePolicy.equals(_ikePolicy)) {
      return false;
    }
    if (!other._localAddress.equals(_localAddress)) {
      return false;
    }
    if (!other._localId.equals(_localId)) {
      return false;
    }
    if (!other._remoteId.equals(_remoteId)) {
      return false;
    }
    return true;
  }

  @JsonPropertyDescription("Remote IP address of IKE gateway")
  public Ip getAddress() {
    return _address;
  }

  @JsonIgnore
  public Interface getExternalInterface() {
    return _externalInterface;
  }

  @JsonProperty(PROP_EXTERNAL_INTERFACE)
  @JsonPropertyDescription(
      "Logical (non-VPN) interface from which to connect to IKE gateway. This interface is used to"
          + " determine source-address for the connection.")
  public String getExternalInterfaceName() {
    if (_externalInterface != null) {
      return _externalInterface.getName();
    } else {
      return _externalInterfaceName;
    }
  }

  @JsonIgnore
  public IkePolicy getIkePolicy() {
    return _ikePolicy;
  }

  @JsonProperty(PROP_IKE_POLICY)
  @JsonPropertyDescription("IKE policy to be used with this IKE gateway.")
  public String getIkePolicyName() {
    if (_ikePolicy != null) {
      return _ikePolicy.getName();
    } else {
      return _ikePolicyName;
    }
  }

  @JsonPropertyDescription(
      "Local IP address from which to connect to IKE gateway. Used instead of external interface.")
  public Ip getLocalAddress() {
    return _localAddress;
  }

  @JsonPropertyDescription("Local IKE ID used in connection to IKE gateway.")
  public String getLocalId() {
    return _localId;
  }

  @JsonPropertyDescription("Remote IKE ID of IKE gateway.")
  public String getRemoteId() {
    return _remoteId;
  }

  public void resolveReferences(Configuration owner) {
    if (_externalInterfaceName != null) {
      _externalInterface = owner.getInterfaces().get(_externalInterfaceName);
    }
    if (_ikePolicyName != null) {
      _ikePolicy = owner.getIkePolicies().get(_ikePolicyName);
    }
  }

  public void setAddress(Ip address) {
    _address = address;
  }

  @JsonIgnore
  public void setExternalInterface(Interface externalInterface) {
    _externalInterface = externalInterface;
  }

  @JsonProperty(PROP_EXTERNAL_INTERFACE)
  public void setExternalInterfaceName(String externalInterfaceName) {
    _externalInterfaceName = externalInterfaceName;
  }

  @JsonIgnore
  public void setIkePolicy(IkePolicy ikePolicy) {
    _ikePolicy = ikePolicy;
  }

  @JsonProperty(PROP_IKE_POLICY)
  public void setIkePolicyName(String ikePolicyName) {
    _ikePolicyName = ikePolicyName;
  }

  public void setLocalAddress(Ip localAddress) {
    _localAddress = localAddress;
  }

  public void setLocalId(String localId) {
    _localId = localId;
  }

  public void setRemoteId(String remoteId) {
    _remoteId = remoteId;
  }
}
