package org.batfish.datamodel;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class IpAddressAcl {

  public static class Builder {

    private List<IpAddressAclLine> _lines;

    private Builder() {
      _lines = ImmutableList.of();
    }

    public IpAddressAcl build() {
      return new IpAddressAcl(_lines);
    }

    public Builder setLines(List<IpAddressAclLine> lines) {
      _lines = lines;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final List<IpAddressAclLine> _lines;

  private IpAddressAcl(List<IpAddressAclLine> lines) {
    _lines = lines;
  }

  private LineAction action(Ip ip) {
    return _lines
        .stream()
        .filter(line -> line.getIpSpace().contains(ip))
        .map(IpAddressAclLine::getAction)
        .findFirst()
        .orElse(LineAction.REJECT);
  }

  public boolean permits(Ip ip) {
    return action(ip) == LineAction.ACCEPT;
  }
}
