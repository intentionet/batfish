package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;
import org.batfish.common.BatfishException;

public final class Flow implements Comparable<Flow>, Serializable {
  public static class Builder {

    private Integer _dscp;

    private Ip _dstIp;

    private Integer _dstPort;

    private Integer _ecn;

    private Integer _fragmentOffset;

    private Integer _icmpCode;

    private Integer _icmpType;

    private String _ingressInterface;

    private String _ingressNode;

    private String _ingressVrf;

    private IpProtocol _ipProtocol;

    private Integer _packetLength;

    private Ip _srcIp;

    private Integer _srcPort;

    private State _state;

    private String _tag;

    private Integer _tcpFlagsAck;

    private Integer _tcpFlagsCwr;

    private Integer _tcpFlagsEce;

    private Integer _tcpFlagsFin;

    private Integer _tcpFlagsPsh;

    private Integer _tcpFlagsRst;

    private Integer _tcpFlagsSyn;

    private Integer _tcpFlagsUrg;

    public Builder() {
      _dscp = 0;
      _dstIp = Ip.ZERO;
      _dstPort = 0;
      _ecn = 0;
      _fragmentOffset = 0;
      _ipProtocol = IpProtocol.IP;
      _srcIp = Ip.ZERO;
      _srcPort = 0;
      _icmpType = IcmpType.UNSET;
      _icmpCode = IcmpCode.UNSET;
      _ingressVrf = Configuration.DEFAULT_VRF_NAME;
      _packetLength = 0;
      _state = State.NEW;
      _tcpFlagsCwr = 0;
      _tcpFlagsEce = 0;
      _tcpFlagsUrg = 0;
      _tcpFlagsAck = 0;
      _tcpFlagsPsh = 0;
      _tcpFlagsRst = 0;
      _tcpFlagsSyn = 0;
      _tcpFlagsFin = 0;
    }

    public Builder(Flow flow) {
      _ingressNode = flow._ingressNode;
      _ingressInterface = flow._ingressInterface;
      _ingressVrf = flow._ingressVrf;
      _srcIp = flow._srcIp;
      _dstIp = flow._dstIp;
      _srcPort = flow._srcPort;
      _dstPort = flow._dstPort;
      _ipProtocol = flow._ipProtocol;
      _dscp = flow._dscp;
      _ecn = flow._ecn;
      _fragmentOffset = flow._fragmentOffset;
      _icmpType = flow._icmpType;
      _icmpCode = flow._icmpCode;
      _packetLength = flow._packetLength;
      _state = flow._state;
      _tcpFlagsCwr = flow._tcpFlagsCwr;
      _tcpFlagsEce = flow._tcpFlagsEce;
      _tcpFlagsUrg = flow._tcpFlagsUrg;
      _tcpFlagsAck = flow._tcpFlagsAck;
      _tcpFlagsPsh = flow._tcpFlagsPsh;
      _tcpFlagsRst = flow._tcpFlagsRst;
      _tcpFlagsSyn = flow._tcpFlagsSyn;
      _tcpFlagsFin = flow._tcpFlagsFin;
      _tag = flow._tag;
    }

    public Flow build() {
      if (_ingressNode == null) {
        throw new BatfishException("Cannot build flow without at least specifying ingress node");
      }
      if (_tag == null) {
        throw new BatfishException("Cannot build flow without specifying tag");
      }
      return new Flow(
          _ingressNode,
          _ingressInterface,
          _ingressVrf,
          _srcIp,
          _dstIp,
          _srcPort,
          _dstPort,
          _ipProtocol,
          _dscp,
          _ecn,
          _fragmentOffset,
          _icmpType,
          _icmpCode,
          _packetLength,
          _state,
          _tcpFlagsCwr,
          _tcpFlagsEce,
          _tcpFlagsUrg,
          _tcpFlagsAck,
          _tcpFlagsPsh,
          _tcpFlagsRst,
          _tcpFlagsSyn,
          _tcpFlagsFin,
          _tag);
    }

    public Integer getDscp() {
      return _dscp;
    }

    public Ip getDstIp() {
      return _dstIp;
    }

    public Integer getDstPort() {
      return _dstPort;
    }

    public Integer getEcn() {
      return _ecn;
    }

    public Integer getIcmpCode() {
      return _icmpCode;
    }

    public Integer getIcmpType() {
      return _icmpType;
    }

    public String getIngressInterface() {
      return _ingressInterface;
    }

    public String getIngressNode() {
      return _ingressNode;
    }

    public String getIngressVrf() {
      return _ingressVrf;
    }

    public IpProtocol getIpProtocol() {
      return _ipProtocol;
    }

    public Integer getPacketLength() {
      return _packetLength;
    }

    public Ip getSrcIp() {
      return _srcIp;
    }

    public Integer getSrcPort() {
      return _srcPort;
    }

    public State getState() {
      return _state;
    }

    public String getTag() {
      return _tag;
    }

    public Integer getTcpFlagsAck() {
      return _tcpFlagsAck;
    }

    public Integer getTcpFlagsCwr() {
      return _tcpFlagsCwr;
    }

    public Integer getTcpFlagsEce() {
      return _tcpFlagsEce;
    }

    public Integer getTcpFlagsFin() {
      return _tcpFlagsFin;
    }

    public Integer getTcpFlagsPsh() {
      return _tcpFlagsPsh;
    }

    public Integer getTcpFlagsRst() {
      return _tcpFlagsRst;
    }

    public Integer getTcpFlagsSyn() {
      return _tcpFlagsSyn;
    }

    public Integer getTcpFlagsUrg() {
      return _tcpFlagsUrg;
    }

    public Builder setDscp(Integer dscp) {
      _dscp = dscp;
      return this;
    }

    public Builder setDstIp(Ip dstIp) {
      _dstIp = dstIp;
      return this;
    }

    public Builder setDstPort(int dstPort) {
      _dstPort = dstPort;
      return this;
    }

    public Builder setDstPort(Integer dstPort) {
      _dstPort = dstPort;
      return this;
    }

    public Builder setEcn(Integer ecn) {
      _ecn = ecn;
      return this;
    }

    public Builder setFragmentOffset(int fragmentOffset) {
      _fragmentOffset = fragmentOffset;
      return this;
    }

    public Builder setIcmpCode(Integer icmpCode) {
      _icmpCode = icmpCode;
      return this;
    }

    public Builder setIcmpType(Integer icmpType) {
      _icmpType = icmpType;
      return this;
    }

    public Builder setIngressInterface(String ingressInterface) {
      _ingressInterface = ingressInterface;
      return this;
    }

    public Builder setIngressNode(String ingressNode) {
      _ingressNode = ingressNode;
      return this;
    }

    public Builder setIngressVrf(String ingressVrf) {
      _ingressVrf = ingressVrf;
      return this;
    }

    public Builder setIpProtocol(IpProtocol ipProtocol) {
      _ipProtocol = ipProtocol;
      return this;
    }

    public Builder setPacketLength(Integer packetLength) {
      _packetLength = packetLength;
      return this;
    }

    public Builder setSrcIp(Ip srcIp) {
      _srcIp = srcIp;
      return this;
    }

    public Builder setSrcPort(int srcPort) {
      _srcPort = srcPort;
      return this;
    }

    public Builder setSrcPort(Integer srcPort) {
      _srcPort = srcPort;
      return this;
    }

    public Builder setState(State state) {
      _state = state;
      return this;
    }

    public Builder setTag(String tag) {
      _tag = tag;
      return this;
    }

    public Builder setTcpFlagsAck(Integer tcpFlagsAck) {
      _tcpFlagsAck = tcpFlagsAck;
      return this;
    }

    public Builder setTcpFlagsCwr(Integer tcpFlagsCwr) {
      _tcpFlagsCwr = tcpFlagsCwr;
      return this;
    }

    public Builder setTcpFlagsEce(Integer tcpFlagsEce) {
      _tcpFlagsEce = tcpFlagsEce;
      return this;
    }

    public Builder setTcpFlagsFin(Integer tcpFlagsFin) {
      _tcpFlagsFin = tcpFlagsFin;
      return this;
    }

    public Builder setTcpFlagsPsh(Integer tcpFlagsPsh) {
      _tcpFlagsPsh = tcpFlagsPsh;
      return this;
    }

    public Builder setTcpFlagsRst(Integer tcpFlagsRst) {
      _tcpFlagsRst = tcpFlagsRst;
      return this;
    }

    public Builder setTcpFlagsSyn(Integer tcpFlagsSyn) {
      _tcpFlagsSyn = tcpFlagsSyn;
      return this;
    }

    public Builder setTcpFlagsUrg(Integer tcpFlagsUrg) {
      _tcpFlagsUrg = tcpFlagsUrg;
      return this;
    }
  }

  private static final String PROP_DSCP = "dscp";

  private static final String PROP_DST_IP = "dstIp";

  private static final String PROP_DST_PORT = "dstPort";

  private static final String PROP_ECN = "ecn";

  private static final String PROP_FRAGMENT_OFFSET = "fragmentOffset";

  private static final String PROP_ICMP_CODE = "icmpCode";

  private static final String PROP_ICMP_TYPE = "icmpVar";

  private static final String PROP_INGRESS_INTERFACE = "ingressInterface";

  private static final String PROP_INGRESS_NODE = "ingressNode";

  private static final String PROP_INGRESS_VRF = "ingressVrf";

  private static final String PROP_IP_PROTOCOL = "ipProtocol";

  private static final String PROP_PACKET_LENGTH = "packetLength";

  private static final String PROP_SRC_IP = "srcIp";

  private static final String PROP_SRC_PORT = "srcPort";

  private static final String PROP_STATE = "state";

  private static final String PROP_TAG = "tag";

  private static final String PROP_TCP_FLAGS_ACK = "tcpFlagsAck";

  private static final String PROP_TCP_FLAGS_CWR = "tcpFlagsCwr";

  private static final String PROP_TCP_FLAGS_ECE = "tcpFlagsEce";

  private static final String PROP_TCP_FLAGS_FIN = "tcpFlagsFin";

  private static final String PROP_TCP_FLAGS_PSH = "tcpFlagsPsh";

  private static final String PROP_TCP_FLAGS_RST = "tcpFlagsRst";

  private static final String PROP_TCP_FLAGS_SYN = "tcpFlagsSyn";

  private static final String PROP_TCP_FLAGS_URG = "tcpFlagsUrg";

  /** */
  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  private final int _dscp;

  private final Ip _dstIp;

  private final int _dstPort;

  private final int _ecn;

  private final int _fragmentOffset;

  private final int _icmpCode;

  private final int _icmpType;

  private String _ingressInterface;

  private final String _ingressNode;

  private final String _ingressVrf;

  private final IpProtocol _ipProtocol;

  private final int _packetLength;

  private final Ip _srcIp;

  private final int _srcPort;

  private final State _state;

  private final String _tag;

  private final int _tcpFlagsAck;

  private final int _tcpFlagsCwr;

  private final int _tcpFlagsEce;

  private final int _tcpFlagsFin;

  private final int _tcpFlagsPsh;

  private final int _tcpFlagsRst;

  private final int _tcpFlagsSyn;

  private final int _tcpFlagsUrg;

  @JsonCreator
  public Flow(
      @JsonProperty(PROP_INGRESS_NODE) String ingressNode,
      @JsonProperty(PROP_INGRESS_INTERFACE) String ingressInterface,
      @JsonProperty(PROP_INGRESS_VRF) String ingressVrf,
      @JsonProperty(PROP_SRC_IP) Ip srcIp,
      @JsonProperty(PROP_DST_IP) Ip dstIp,
      @JsonProperty(PROP_SRC_PORT) int srcPort,
      @JsonProperty(PROP_DST_PORT) int dstPort,
      @JsonProperty(PROP_IP_PROTOCOL) IpProtocol ipProtocol,
      @JsonProperty(PROP_DSCP) int dscp,
      @JsonProperty(PROP_ECN) int ecn,
      @JsonProperty(PROP_FRAGMENT_OFFSET) int fragmentOffset,
      @JsonProperty(PROP_ICMP_TYPE) int icmpType,
      @JsonProperty(PROP_ICMP_CODE) int icmpCode,
      @JsonProperty(PROP_PACKET_LENGTH) int packetLength,
      @JsonProperty(PROP_STATE) State state,
      @JsonProperty(PROP_TCP_FLAGS_CWR) int tcpFlagsCwr,
      @JsonProperty(PROP_TCP_FLAGS_ECE) int tcpFlagsEce,
      @JsonProperty(PROP_TCP_FLAGS_URG) int tcpFlagsUrg,
      @JsonProperty(PROP_TCP_FLAGS_ACK) int tcpFlagsAck,
      @JsonProperty(PROP_TCP_FLAGS_PSH) int tcpFlagsPsh,
      @JsonProperty(PROP_TCP_FLAGS_RST) int tcpFlagsRst,
      @JsonProperty(PROP_TCP_FLAGS_SYN) int tcpFlagsSyn,
      @JsonProperty(PROP_TCP_FLAGS_FIN) int tcpFlagsFin,
      @JsonProperty(PROP_TAG) String tag) {
    _ingressNode = ingressNode;
    _ingressInterface = ingressInterface;
    _ingressVrf = ingressVrf;
    _srcIp = srcIp;
    _dstIp = dstIp;
    _srcPort = srcPort;
    _dstPort = dstPort;
    _ipProtocol = ipProtocol;
    _dscp = dscp;
    _ecn = ecn;
    _fragmentOffset = fragmentOffset;
    _icmpType = icmpType;
    _icmpCode = icmpCode;
    _packetLength = packetLength;
    _state = state;
    _tcpFlagsCwr = tcpFlagsCwr;
    _tcpFlagsEce = tcpFlagsEce;
    _tcpFlagsUrg = tcpFlagsUrg;
    _tcpFlagsAck = tcpFlagsAck;
    _tcpFlagsPsh = tcpFlagsPsh;
    _tcpFlagsRst = tcpFlagsRst;
    _tcpFlagsSyn = tcpFlagsSyn;
    _tcpFlagsFin = tcpFlagsFin;
    _tag = tag;
  }

  @Override
  public int compareTo(Flow rhs) {
    int ret;
    ret = _ingressNode.compareTo(rhs._ingressNode);
    if (ret != 0) {
      return ret;
    }
    ret = _ingressVrf.compareTo(rhs._ingressVrf);
    if (ret != 0) {
      return ret;
    }
    ret = _srcIp.compareTo(rhs._srcIp);
    if (ret != 0) {
      return ret;
    }
    ret = _dstIp.compareTo(rhs._dstIp);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_ipProtocol.number(), rhs._ipProtocol.number());
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_srcPort, rhs._srcPort);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_dstPort, rhs._dstPort);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_dscp, rhs._dscp);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_ecn, rhs._ecn);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_fragmentOffset, rhs._fragmentOffset);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_icmpType, rhs._icmpType);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_icmpCode, rhs._icmpCode);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_packetLength, rhs._packetLength);
    if (ret != 0) {
      return ret;
    }
    ret = _state.compareTo(rhs._state);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_tcpFlagsCwr, rhs._tcpFlagsCwr);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_tcpFlagsEce, rhs._tcpFlagsEce);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_tcpFlagsUrg, rhs._tcpFlagsUrg);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_tcpFlagsAck, rhs._tcpFlagsAck);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_tcpFlagsPsh, rhs._tcpFlagsPsh);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_tcpFlagsRst, rhs._tcpFlagsRst);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_tcpFlagsSyn, rhs._tcpFlagsSyn);
    if (ret != 0) {
      return ret;
    }
    return Integer.compare(_tcpFlagsFin, rhs._tcpFlagsFin);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Flow)) {
      return false;
    }
    Flow other = (Flow) o;
    return _dscp == other._dscp
        && Objects.equals(_dstIp, other._dstIp)
        && Objects.equals(_dstPort, other._dstPort)
        && _ecn == other._ecn
        && _fragmentOffset == other._fragmentOffset
        && Objects.equals(_icmpCode, other._icmpCode)
        && Objects.equals(_icmpType, other._icmpType)
        && Objects.equals(_ingressInterface, other._ingressInterface)
        && Objects.equals(_ingressNode, other._ingressNode)
        && Objects.equals(_ingressVrf, other._ingressVrf)
        && Objects.equals(_ipProtocol, other._ipProtocol)
        && _packetLength == other._packetLength
        && Objects.equals(_srcIp, other._srcIp)
        && Objects.equals(_srcPort, other._srcPort)
        && Objects.equals(_state, other._state)
        && Objects.equals(_tag, other._tag)
        && _tcpFlagsAck == other._tcpFlagsAck
        && _tcpFlagsCwr == other._tcpFlagsCwr
        && _tcpFlagsEce == other._tcpFlagsEce
        && _tcpFlagsFin == other._tcpFlagsFin
        && _tcpFlagsPsh == other._tcpFlagsPsh
        && _tcpFlagsRst == other._tcpFlagsRst
        && _tcpFlagsSyn == other._tcpFlagsSyn
        && _tcpFlagsUrg == other._tcpFlagsUrg;
  }

  @JsonProperty(PROP_DSCP)
  public int getDscp() {
    return _dscp;
  }

  @JsonProperty(PROP_DST_IP)
  public Ip getDstIp() {
    return _dstIp;
  }

  @JsonProperty(PROP_DST_PORT)
  public Integer getDstPort() {
    return _dstPort;
  }

  @JsonProperty(PROP_ECN)
  public int getEcn() {
    return _ecn;
  }

  @JsonProperty(PROP_FRAGMENT_OFFSET)
  public int getFragmentOffset() {
    return _fragmentOffset;
  }

  @JsonProperty(PROP_ICMP_CODE)
  public Integer getIcmpCode() {
    return _icmpCode;
  }

  @JsonProperty(PROP_ICMP_TYPE)
  public Integer getIcmpType() {
    return _icmpType;
  }

  @JsonProperty(PROP_INGRESS_INTERFACE)
  public String getIngressInterface() {
    return _ingressInterface;
  }

  @JsonProperty(PROP_INGRESS_NODE)
  public String getIngressNode() {
    return _ingressNode;
  }

  @JsonProperty(PROP_INGRESS_VRF)
  public String getIngressVrf() {
    return _ingressVrf;
  }

  @JsonProperty(PROP_IP_PROTOCOL)
  public IpProtocol getIpProtocol() {
    return _ipProtocol;
  }

  @JsonProperty(PROP_PACKET_LENGTH)
  public int getPacketLength() {
    return _packetLength;
  }

  @JsonProperty(PROP_SRC_IP)
  public Ip getSrcIp() {
    return _srcIp;
  }

  @JsonProperty(PROP_SRC_PORT)
  public Integer getSrcPort() {
    return _srcPort;
  }

  @JsonProperty(PROP_STATE)
  public State getState() {
    return _state;
  }

  @JsonProperty(PROP_TAG)
  public String getTag() {
    return _tag;
  }

  @JsonProperty(PROP_TCP_FLAGS_ACK)
  public int getTcpFlagsAck() {
    return _tcpFlagsAck;
  }

  @JsonProperty(PROP_TCP_FLAGS_CWR)
  public int getTcpFlagsCwr() {
    return _tcpFlagsCwr;
  }

  @JsonProperty(PROP_TCP_FLAGS_ECE)
  public int getTcpFlagsEce() {
    return _tcpFlagsEce;
  }

  @JsonProperty(PROP_TCP_FLAGS_FIN)
  public int getTcpFlagsFin() {
    return _tcpFlagsFin;
  }

  @JsonProperty(PROP_TCP_FLAGS_PSH)
  public int getTcpFlagsPsh() {
    return _tcpFlagsPsh;
  }

  @JsonProperty(PROP_TCP_FLAGS_RST)
  public int getTcpFlagsRst() {
    return _tcpFlagsRst;
  }

  @JsonProperty(PROP_TCP_FLAGS_SYN)
  public int getTcpFlagsSyn() {
    return _tcpFlagsSyn;
  }

  @JsonProperty(PROP_TCP_FLAGS_URG)
  public int getTcpFlagsUrg() {
    return _tcpFlagsUrg;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _dscp,
        _dstIp,
        _dstPort,
        _ecn,
        _fragmentOffset,
        _icmpCode,
        _icmpType,
        _ingressInterface,
        _ingressNode,
        _ingressVrf,
        _ipProtocol,
        _packetLength,
        _srcIp,
        _srcPort,
        _state,
        _tag,
        _tcpFlagsAck,
        _tcpFlagsCwr,
        _tcpFlagsEce,
        _tcpFlagsFin,
        _tcpFlagsPsh,
        _tcpFlagsRst,
        _tcpFlagsSyn,
        _tcpFlagsUrg);
  }

  public String prettyPrint(String prefixString) {
    boolean icmp = _ipProtocol == IpProtocol.ICMP;
    boolean tcp = _ipProtocol == IpProtocol.TCP;
    boolean udp = _ipProtocol == IpProtocol.UDP;
    String srcPortStr = "";
    String dstPortStr = "";
    String icmpTypeStr = "";
    String icmpCodeStr = "";
    String tcpFlagsStr = "";
    if (tcp || udp) {
      srcPortStr = " sport:" + NamedPort.nameFromNumber(_srcPort);
      dstPortStr = " dport:" + NamedPort.nameFromNumber(_dstPort);
    }
    if (tcp) {
      tcpFlagsStr =
          String.format(
              " tcpFlags:%d%d%d%d%d%d%d%d",
              _tcpFlagsCwr,
              _tcpFlagsEce,
              _tcpFlagsUrg,
              _tcpFlagsAck,
              _tcpFlagsPsh,
              _tcpFlagsRst,
              _tcpFlagsSyn,
              _tcpFlagsFin);
    }
    if (icmp) {
      icmpCodeStr = " icmpCode:" + Integer.toString(_icmpCode);
      icmpTypeStr = " icmpType:" + Integer.toString(_icmpType);
    }
    String dscpStr = (_dscp != 0) ? " dscp:" + _dscp : "";
    String ecnStr = (_ecn != 0) ? " ecn:" + _ecn : "";

    return prefixString
        + "Flow: ingress:"
        + _ingressNode
        + (_ingressVrf != null ? " vrf:" + _ingressVrf : "")
        + (_ingressInterface != null ? " iface:" + _ingressInterface : "")
        + " "
        + _srcIp
        + "->"
        + _dstIp
        + " "
        + _ipProtocol
        + srcPortStr
        + dstPortStr
        + dscpStr
        + ecnStr
        + icmpTypeStr
        + icmpCodeStr
        + " packetLength:"
        + _packetLength
        + " state:"
        + _state
        + tcpFlagsStr;
  }

  @JsonProperty(PROP_INGRESS_INTERFACE)
  public void setIngressInterface(String ingressInterface) {
    _ingressInterface = ingressInterface;
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public String toString() {
    boolean icmp = _ipProtocol == IpProtocol.ICMP;
    boolean tcp = _ipProtocol == IpProtocol.TCP;
    boolean udp = _ipProtocol == IpProtocol.UDP;
    String srcPortStr = "";
    String dstPortStr = "";
    String icmpTypeStr = "";
    String icmpCodeStr = "";
    String tcpFlagsStr = "";
    if (tcp || udp) {
      srcPortStr = " srcPort:" + NamedPort.nameFromNumber(_srcPort);
      dstPortStr = " dstPort:" + NamedPort.nameFromNumber(_dstPort);
    }
    if (tcp) {
      tcpFlagsStr =
          String.format(
              " tcpFlags:%d%d%d%d%d%d%d%d",
              _tcpFlagsCwr,
              _tcpFlagsEce,
              _tcpFlagsUrg,
              _tcpFlagsAck,
              _tcpFlagsPsh,
              _tcpFlagsRst,
              _tcpFlagsSyn,
              _tcpFlagsFin);
    }
    if (icmp) {
      icmpCodeStr = " icmpCode:" + Integer.toString(_icmpCode);
      icmpTypeStr = " icmpType:" + Integer.toString(_icmpType);
    }
    return "Flow<ingressNode:"
        + _ingressNode
        + (_ingressVrf != null ? " vrf:" + _ingressVrf : "")
        + (_ingressInterface != null ? " iface:" + _ingressInterface : "")
        + " srcIp:"
        + _srcIp
        + " dstIp:"
        + _dstIp
        + " ipProtocol:"
        + _ipProtocol
        + srcPortStr
        + dstPortStr
        + " dscp: "
        + _dscp
        + " ecn:"
        + _ecn
        + " fragmentOffset:"
        + _fragmentOffset
        + icmpTypeStr
        + icmpCodeStr
        + " packetLength:"
        + _packetLength
        + " state:"
        + _state
        + tcpFlagsStr
        + " tag:"
        + _tag
        + ">";
  }
}
