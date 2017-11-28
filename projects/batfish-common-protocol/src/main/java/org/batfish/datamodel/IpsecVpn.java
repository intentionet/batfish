package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.common.util.ComparableStructure;

public final class IpsecVpn extends ComparableStructure<String> {

  private static final String PROP_BIND_INTERFACE = "bindInterface";

  private static final String PROP_IKE_GATEWAY = "ikeGateway";

  private static final String PROP_IPSEC_POLICY = "ipsecPolicy";

  /** */
  private static final long serialVersionUID = 1L;

  private Interface _bindInterface;

  private transient String _bindInterfaceName;

  private transient Set<IpsecVpn> _candidateRemoteIpsecVpns;

  private IkeGateway _ikeGateway;

  private transient String _ikeGatewayName;

  private IpsecPolicy _ipsecPolicy;

  private transient String _ipsecPolicyName;

  private Configuration _owner;

  private transient IpsecVpn _remoteIpsecVpn;

  @JsonCreator
  public IpsecVpn(@JsonProperty(PROP_NAME) String name) {
    super(name);
  }

  public IpsecVpn(String name, Configuration owner) {
    super(name);
    _owner = owner;
  }

  public Boolean compatibleIkeProposals(IpsecVpn remoteIpsecVpn) {
    IkeProposal activeIkeProposal = getActiveIkeProposal(remoteIpsecVpn);
    return activeIkeProposal != null;
  }

  public Boolean compatibleIpsecProposals(IpsecVpn remoteIpsecVpn) {
    // handle dynamic pfs key group
    if (_ipsecPolicy.getPfsKeyGroupDynamicIke()) {
      IkeProposal activeProposal = getActiveIkeProposal(remoteIpsecVpn);
      if (activeProposal == null) {
        return false;
      }
      if (!remoteIpsecVpn.getIpsecPolicy().getPfsKeyGroupDynamicIke()
          && !activeProposal
              .getDiffieHellmanGroup()
              .equals(remoteIpsecVpn.getIpsecPolicy().getPfsKeyGroup())) {
        /*
         * Remote vpn uses static pfs key group, but active proposal's dh group does not match
         * remote's pfsKeyGroup.
         */
        return false;
      }
      // else remote vpn also uses dynamic pfs key group. They must agree as
      // long as a compatible proposal is selected, which has already
      // happened.
    } else if (_ipsecPolicy.getPfsKeyGroup() != remoteIpsecVpn.getIpsecPolicy().getPfsKeyGroup()) {
      return false;
    }
    for (IpsecProposal lhs : _ipsecPolicy.getProposals().values()) {
      for (IpsecProposal rhs : remoteIpsecVpn.getIpsecPolicy().getProposals().values()) {
        if (lhs.compatibleWith(rhs)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean compatiblePreSharedKey(IpsecVpn remoteIpsecVpn) {
    if (_ikeGateway == null || remoteIpsecVpn._ikeGateway == null) {
      return false;
    }
    IkePolicy ikePolicy = _ikeGateway.getIkePolicy();
    IkePolicy remoteIkePolicy = remoteIpsecVpn._ikeGateway.getIkePolicy();
    if (ikePolicy == null || remoteIkePolicy == null) {
      return false;
    }
    String psk = ikePolicy.getPreSharedKeyHash();
    String remotePsk = remoteIkePolicy.getPreSharedKeyHash();
    return psk != null && remotePsk != null && psk.equals(remotePsk);
  }

  @Nullable
  @JsonIgnore
  private IkeProposal getActiveIkeProposal(IpsecVpn remoteIpsecVpn) {
    for (IkeProposal lhs : _ikeGateway.getIkePolicy().getProposals().values()) {
      for (IkeProposal rhs :
          remoteIpsecVpn.getIkeGateway().getIkePolicy().getProposals().values()) {
        if (lhs.compatibleWith(rhs)) {
          return lhs;
        }
      }
    }
    return null;
  }

  @JsonIgnore
  public Interface getBindInterface() {
    return _bindInterface;
  }

  @JsonProperty(PROP_BIND_INTERFACE)
  @JsonPropertyDescription("Tunnel interface on which the VPN will be bound")
  public String getBindInterfaceName() {
    if (_bindInterface != null) {
      return _bindInterface.getName();
    } else {
      return _bindInterfaceName;
    }
  }

  @JsonIdentityReference(alwaysAsId = true)
  public Set<IpsecVpn> getCandidateRemoteIpsecVpns() {
    return _candidateRemoteIpsecVpns;
  }

  @JsonIgnore
  public IkeGateway getIkeGateway() {
    return _ikeGateway;
  }

  @JsonProperty(PROP_IKE_GATEWAY)
  @JsonPropertyDescription("Remote VPN gateway configuration")
  public String getIkeGatewayName() {
    if (_ikeGateway != null) {
      return _ikeGateway.getName();
    } else {
      return _ikeGatewayName;
    }
  }

  @JsonIgnore
  public IpsecPolicy getIpsecPolicy() {
    return _ipsecPolicy;
  }

  @JsonProperty(PROP_IPSEC_POLICY)
  @JsonPropertyDescription("IPSEC policy for this VPN")
  public String getIpsecPolicyName() {
    if (_ipsecPolicy != null) {
      return _ipsecPolicy.getName();
    } else {
      return _ipsecPolicyName;
    }
  }

  @JsonIgnore
  public Configuration getOwner() {
    return _owner;
  }

  @JsonIdentityReference(alwaysAsId = true)
  public IpsecVpn getRemoteIpsecVpn() {
    return _remoteIpsecVpn;
  }

  public void initCandidateRemoteVpns() {
    _candidateRemoteIpsecVpns = new TreeSet<>();
  }

  public void resolveReferences(Configuration owner) {
    _owner = owner;
    if (_bindInterfaceName != null) {
      _bindInterface = owner.getInterfaces().get(_bindInterfaceName);
    }
    if (_ikeGatewayName != null) {
      _ikeGateway = owner.getIkeGateways().get(_ikeGatewayName);
    }
    if (_ipsecPolicyName != null) {
      _ipsecPolicy = owner.getIpsecPolicies().get(_ipsecPolicyName);
    }
  }

  @JsonIgnore
  public void setBindInterface(Interface bindInterface) {
    _bindInterface = bindInterface;
  }

  @JsonProperty(PROP_BIND_INTERFACE)
  public void setBindInterfaceName(String bindInterfaceName) {
    _bindInterfaceName = bindInterfaceName;
  }

  @JsonIgnore
  public void setIkeGateway(IkeGateway ikeGateway) {
    _ikeGateway = ikeGateway;
  }

  @JsonProperty(PROP_IKE_GATEWAY)
  public void setIkeGatewayName(String ikeGatewayName) {
    _ikeGatewayName = ikeGatewayName;
  }

  @JsonIgnore
  public void setIpsecPolicy(IpsecPolicy ipsecPolicy) {
    _ipsecPolicy = ipsecPolicy;
  }

  @JsonProperty(PROP_IPSEC_POLICY)
  public void setIpsecPolicyName(String ipsecPolicyName) {
    _ipsecPolicyName = ipsecPolicyName;
  }

  public void setOwner(Configuration owner) {
    _owner = owner;
  }

  public void setRemoteIpsecVpn(IpsecVpn remoteIpsecVpn) {
    _remoteIpsecVpn = remoteIpsecVpn;
  }
}
