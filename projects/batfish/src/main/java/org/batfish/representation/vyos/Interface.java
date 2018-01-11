package org.batfish.representation.vyos;

import java.util.Set;
import java.util.TreeSet;
import org.batfish.common.BatfishException;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.NetworkAddress;

public class Interface extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  public static double getDefaultBandwidth(InterfaceType type) {
    // TODO: update with correct values
    switch (type) {
      case ETHERNET:
      case LOOPBACK:
      case VTI:
        return 1E12d;

      case BONDING:
      case BRIDGE:
      case DUMMY:
      case INPUT:
      case L2TPV3:
      case OPENVPN:
      case PSEUDO_ETHERNET:
      case TUNNEL:
      case VXLAN:
      case WIRELESS:
      case WIRELESSMODEM:
      default:
        throw new BatfishException("unsupported interface type");
    }
  }

  private final Set<NetworkAddress> _allAddresses;

  private double _bandwidth;

  private String _description;

  private NetworkAddress _address;

  private InterfaceType _type;

  public Interface(String name) {
    super(name);
    _allAddresses = new TreeSet<>();
  }

  public Set<NetworkAddress> getAllAddresses() {
    return _allAddresses;
  }

  public double getBandwidth() {
    return _bandwidth;
  }

  public String getDescription() {
    return _description;
  }

  public NetworkAddress getAddress() {
    return _address;
  }

  public InterfaceType getType() {
    return _type;
  }

  public void setBandwidth(double bandwidth) {
    _bandwidth = bandwidth;
  }

  public void setDescription(String description) {
    _description = description;
  }

  public void setAddress(NetworkAddress address) {
    _address = address;
  }

  public void setType(InterfaceType type) {
    _type = type;
  }
}
