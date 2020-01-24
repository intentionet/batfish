package org.batfish.representation.juniper;

import javax.annotation.Nullable;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.TraceElement;
import org.batfish.representation.juniper.FwTerm.Field;

/** Class for firewall filter from destination-address */
public final class FwFromDestinationAddress extends FwFrom {

  @Nullable private final IpWildcard _ipWildcard;

  public FwFromDestinationAddress(IpWildcard ipWildcard) {
    _ipWildcard = ipWildcard;
  }

  @Override
  public void applyTo(
      HeaderSpace.Builder headerSpaceBuilder,
      JuniperConfiguration jc,
      Warnings w,
      Configuration c) {
    headerSpaceBuilder.setDstIps(
        AclIpSpace.union(headerSpaceBuilder.getDstIps(), _ipWildcard.toIpSpace()));
  }

  public IpWildcard getIpWildcard() {
    return _ipWildcard;
  }

  @Override
  Field getField() {
    return Field.DESTINATION;
  }

  @Override
  TraceElement getTraceElement() {
    return TraceElement.of(String.format("Matched destination-address %s", _ipWildcard.toString()));
  }

  @Override
  HeaderSpace toHeaderspace(JuniperConfiguration jc, Configuration c, Warnings w) {
    return HeaderSpace.builder().setDstIps(_ipWildcard.toIpSpace()).build();
  }
}
