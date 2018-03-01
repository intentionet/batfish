package org.batfish.datamodel;

import java.util.Objects;
import javax.annotation.Nonnull;

public class ArpIpChoice {

  public static final ArpIpChoice USE_DST_IP = new ArpIpChoice(Ip.AUTO);

  public static ArpIpChoice of(@Nonnull Ip ip) {
    return ip == Ip.AUTO ? USE_DST_IP : new ArpIpChoice(ip);
  }

  private final Ip _ip;

  private ArpIpChoice(@Nonnull Ip ip) {
    _ip = ip;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && obj instanceof ArpIpChoice
        && Objects.equals(_ip, ((ArpIpChoice) obj)._ip);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_ip);
  }
}
