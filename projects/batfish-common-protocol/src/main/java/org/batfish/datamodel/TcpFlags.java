package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.io.Serializable;

public final class TcpFlags implements Serializable {

  public static final int ACK = 0x10;

  public static final int CWR = 0x80;

  public static final int ECE = 0x40;

  public static final int FIN = 0x01;

  public static final int PSH = 0x08;

  public static final int RST = 0x04;

  /** */
  private static final long serialVersionUID = 1L;

  public static final int SYN = 0x02;

  public static final int URG = 0x20;

  private boolean _ack;

  private boolean _cwr;

  private boolean _ece;

  private boolean _fin;

  private boolean _psh;

  private boolean _rst;

  private boolean _syn;

  private boolean _urg;

  private boolean _useAck;

  private boolean _useCwr;

  private boolean _useEce;

  private boolean _useFin;

  private boolean _usePsh;

  private boolean _useRst;

  private boolean _useSyn;

  private boolean _useUrg;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    TcpFlags other = (TcpFlags) obj;
    if (other.toString().equals(this.toString())) {
      return true;
    } else {
      return false;
    }
  }

  @JsonPropertyDescription("Value for ACK bit if used (true->1/false->0)")
  public boolean getAck() {
    return _ack;
  }

  @JsonPropertyDescription("Value for CWR bit if used (true->1/false->0)")
  public boolean getCwr() {
    return _cwr;
  }

  @JsonPropertyDescription("Value for ECE bit if used (true->1/false->0)")
  public boolean getEce() {
    return _ece;
  }

  @JsonPropertyDescription("Value for FIN bit if used (true->1/false->0)")
  public boolean getFin() {
    return _fin;
  }

  @JsonPropertyDescription("Value for PSH bit if used (true->1/false->0)")
  public boolean getPsh() {
    return _psh;
  }

  @JsonPropertyDescription("Value for RST bit if used (true->1/false->0)")
  public boolean getRst() {
    return _rst;
  }

  @JsonPropertyDescription("Value for SYN bit if used (true->1/false->0)")
  public boolean getSyn() {
    return _syn;
  }

  @JsonPropertyDescription("Value for URG bit if used (true->1/false->0)")
  public boolean getUrg() {
    return _urg;
  }

  @JsonPropertyDescription("Whether or not to match against the ACK bit")
  public boolean getUseAck() {
    return _useAck;
  }

  @JsonPropertyDescription("Whether or not to match against the CWR bit")
  public boolean getUseCwr() {
    return _useCwr;
  }

  @JsonPropertyDescription("Whether or not to match against the ECE bit")
  public boolean getUseEce() {
    return _useEce;
  }

  @JsonPropertyDescription("Whether or not to match against the FIN bit")
  public boolean getUseFin() {
    return _useFin;
  }

  @JsonPropertyDescription("Whether or not to match against the PSH bit")
  public boolean getUsePsh() {
    return _usePsh;
  }

  @JsonPropertyDescription("Whether or not to match against the RST bit")
  public boolean getUseRst() {
    return _useRst;
  }

  @JsonPropertyDescription("Whether or not to match against the SYN bit")
  public boolean getUseSyn() {
    return _useSyn;
  }

  @JsonPropertyDescription("Whether or not to match against the URG bit")
  public boolean getUseUrg() {
    return _useUrg;
  }

  @Override
  public int hashCode() {
    // TODO: implement better hashcode
    return 0;
  }

  public boolean match(Flow flow) {
    return !(_useAck && _ack ^ (flow.getTcpFlagsAck() == 1))
        && !(_useCwr && _cwr ^ (flow.getTcpFlagsCwr() == 1))
        && !(_useEce && _ece ^ (flow.getTcpFlagsEce() == 1))
        && !(_useFin && _fin ^ (flow.getTcpFlagsFin() == 1))
        && !(_usePsh && _psh ^ (flow.getTcpFlagsPsh() == 1))
        && !(_useRst && _rst ^ (flow.getTcpFlagsRst() == 1))
        && !(_useSyn && _syn ^ (flow.getTcpFlagsSyn() == 1))
        && !(_useUrg && _urg ^ (flow.getTcpFlagsUrg() == 1));
  }

  public boolean match(Flow6 flow6) {
    return !(_useAck && _ack ^ (flow6.getTcpFlagsAck() == 1))
        && !(_useCwr && _cwr ^ (flow6.getTcpFlagsCwr() == 1))
        && !(_useEce && _ece ^ (flow6.getTcpFlagsEce() == 1))
        && !(_useFin && _fin ^ (flow6.getTcpFlagsFin() == 1))
        && !(_usePsh && _psh ^ (flow6.getTcpFlagsPsh() == 1))
        && !(_useRst && _rst ^ (flow6.getTcpFlagsRst() == 1))
        && !(_useSyn && _syn ^ (flow6.getTcpFlagsSyn() == 1))
        && !(_useUrg && _urg ^ (flow6.getTcpFlagsUrg() == 1));
  }

  public void setAck(boolean ack) {
    _ack = ack;
  }

  public void setCwr(boolean cwr) {
    _cwr = cwr;
  }

  public void setEce(boolean ece) {
    _ece = ece;
  }

  public void setFin(boolean fin) {
    _fin = fin;
  }

  public void setPsh(boolean psh) {
    _psh = psh;
  }

  public void setRst(boolean rst) {
    _rst = rst;
  }

  public void setSyn(boolean syn) {
    _syn = syn;
  }

  public void setUrg(boolean urg) {
    _urg = urg;
  }

  public void setUseAck(boolean useAck) {
    _useAck = useAck;
  }

  public void setUseCwr(boolean useCwr) {
    _useCwr = useCwr;
  }

  public void setUseEce(boolean useEce) {
    _useEce = useEce;
  }

  public void setUseFin(boolean useFin) {
    _useFin = useFin;
  }

  public void setUsePsh(boolean usePsh) {
    _usePsh = usePsh;
  }

  public void setUseRst(boolean useRst) {
    _useRst = useRst;
  }

  public void setUseSyn(boolean useSyn) {
    _useSyn = useSyn;
  }

  public void setUseUrg(boolean useUrg) {
    _useUrg = useUrg;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(toString(_cwr, _useCwr));
    sb.append(toString(_ece, _useEce));
    sb.append(toString(_urg, _useUrg));
    sb.append(toString(_ack, _useAck));
    sb.append(toString(_psh, _usePsh));
    sb.append(toString(_rst, _useRst));
    sb.append(toString(_syn, _useSyn));
    sb.append(toString(_fin, _useFin));
    return sb.toString();
  }

  private String toString(boolean bit, boolean useBit) {
    if (useBit) {
      if (bit) {
        return "1";
      } else {
        return "0";
      }
    } else {
      return "x";
    }
  }
}
