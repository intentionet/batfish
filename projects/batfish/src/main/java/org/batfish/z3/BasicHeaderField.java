package org.batfish.z3;

public enum BasicHeaderField implements HeaderField {
  DSCP(6),
  DST_IP(32),
  DST_PORT(16),
  ECN(2),
  FRAGMENT_OFFSET(13),
  ICMP_CODE(8),
  ICMP_TYPE(8),
  IP_PROTOCOL(8),
  ORIG_SRC_IP(32),
  PACKET_LENGTH(16),
  SRC_IP(32),
  SRC_PORT(16),
  STATE(2),
  TCP_FLAGS_ACK(1),
  TCP_FLAGS_CWR(1),
  TCP_FLAGS_ECE(1),
  TCP_FLAGS_FIN(1),
  TCP_FLAGS_PSH(1),
  TCP_FLAGS_RST(1),
  TCP_FLAGS_SYN(1),
  TCP_FLAGS_URG(1);

  private final int _size;

  private BasicHeaderField(int size) {
    _size = size;
  }

  @Override
  public String getName() {
    return name();
  }

  @Override
  public int getSize() {
    return _size;
  }
}
