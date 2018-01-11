package org.batfish.representation.aws;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class IpPermissions implements Serializable {

  private static final long serialVersionUID = 1L;

  @Nullable
  public static IpProtocol toIpProtocol(String ipProtocolAsString) {
    switch (ipProtocolAsString) {
      case "tcp":
        return IpProtocol.TCP;
      case "udp":
        return IpProtocol.UDP;
      case "icmp":
        return IpProtocol.ICMP;
      case "-1":
        return null;
      default:
        try {
          int ipProtocolAsInt = Integer.parseInt(ipProtocolAsString);
          return IpProtocol.fromNumber(ipProtocolAsInt);
        } catch (NumberFormatException e) {
          throw new BatfishException(
              "Could not convert AWS IP protocol: \""
                  + ipProtocolAsString
                  + "\" to batfish Ip Protocol",
              e);
        }
    }
  }

  private int _fromPort = -1;

  private String _ipProtocol;

  private List<Prefix> _ipRanges = new LinkedList<>();

  private int _toPort = -1;

  public IpPermissions(JSONObject jObj, BatfishLogger logger) throws JSONException {
    _ipProtocol = jObj.getString(AwsVpcEntity.JSON_KEY_IP_PROTOCOL);

    _fromPort = Utils.tryGetInt(jObj, AwsVpcEntity.JSON_KEY_FROM_PORT, _fromPort);
    _toPort = Utils.tryGetInt(jObj, AwsVpcEntity.JSON_KEY_TO_PORT, _toPort);

    JSONArray ranges = jObj.getJSONArray(AwsVpcEntity.JSON_KEY_IP_RANGES);

    for (int index = 0; index < ranges.length(); index++) {
      JSONObject childObject = ranges.getJSONObject(index);
      _ipRanges.add(Prefix.fromString(childObject.getString(AwsVpcEntity.JSON_KEY_CIDR_IP)));
    }
  }

  public IpAccessListLine toEgressIpAccessListLine() {
    IpAccessListLine line = toIpAccessListLine();
    line.setDstIps(
        _ipRanges
            .stream()
            .map(IpWildcard::new)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural())));
    return line;
  }

  public IpAccessListLine toIngressIpAccessListLine() {
    IpAccessListLine line = toIpAccessListLine();
    line.setSrcIps(
        _ipRanges
            .stream()
            .map(IpWildcard::new)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural())));
    return line;
  }

  private IpAccessListLine toIpAccessListLine() {
    IpAccessListLine line = new IpAccessListLine();
    line.setAction(LineAction.ACCEPT);
    IpProtocol protocol = toIpProtocol(_ipProtocol);
    if (protocol != null) {
      line.setIpProtocols(Collections.singleton(protocol));
    }
    if (_fromPort != -1) {
      line.setSrcPorts(Collections.singleton(new SubRange(_fromPort, _fromPort)));
    }
    if (_toPort != -1) {
      line.setDstPorts(Collections.singleton(new SubRange(_toPort, _toPort)));
    }
    return line;
  }
}
