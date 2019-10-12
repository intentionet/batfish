package org.batfish.representation.iptables;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;

public class IptablesMatch implements Serializable {

  public enum MatchType {
    DESTINATION,
    DESTINATION_PORT,
    IN_INTERFACE,
    OUT_INTERFACE,
    PROTOCOL,
    SOURCE,
    SOURCE_PORT,
  }

  private boolean _inverted;

  private Object _matchData;

  private MatchType _matchType;

  public IptablesMatch(boolean inverted, MatchType matchType, Object matchData) {
    _inverted = inverted;
    _matchType = matchType;
    _matchData = matchData;
  }

  public boolean getInverted() {
    return _inverted;
  }

  public Object getMatchData() {
    return _matchData;
  }

  public MatchType getMatchType() {
    return _matchType;
  }

  public IpProtocol toIpProtocol() {
    if (_inverted) {
      // _warnings.redFlag("Inversion of protocol matching is not supported.
      // Current analysis will match everything.");
      // return IpWildcard.ANY;
      throw new BatfishException("Unknown matchdata type");
    }

    return (IpProtocol) _matchData;
  }

  public IpWildcard toIpWildcard() {

    if (_inverted) {
      // _warnings.redFlag("Inversion of src/dst matching is not supported.
      // Current analysis will match everything.");
      // return IpWildcard.ANY;
      throw new BatfishException("Unknown matchdata type");
    }

    if (_matchData instanceof Ip) {
      Prefix pfx = ((Ip) _matchData).toPrefix();
      return IpWildcard.create(pfx);
    } else if (_matchData instanceof Prefix) {
      return IpWildcard.create((Prefix) _matchData);
    } else {
      throw new BatfishException("Unknown matchdata type: " + _matchData);
    }
  }

  public List<SubRange> toPortRanges() {

    List<SubRange> subRanges = new LinkedList<>();

    int port = (int) _matchData;

    if (_inverted) {
      if (port != 0) {
        subRanges.add(new SubRange(0, port - 1));
      }
      if (port != 65535) {
        subRanges.add(new SubRange(port + 1, 65535));
      }
    } else {
      subRanges.add(SubRange.singleton(port));
    }

    return subRanges;
  }

  public String toInterfaceName() {
    if (_inverted) {
      // _warnings.redFlag("Inversion of interface matching is not supported.");
      throw new BatfishException("Unknown matchdata type");
    }
    return (String) _matchData;
  }
}
