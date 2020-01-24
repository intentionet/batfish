package org.batfish.representation.juniper;

import com.google.common.collect.ImmutableList;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.TraceElement;
import org.batfish.representation.juniper.FwTerm.Field;

/** Class for firewall filter from destination-prefix-list except */
public final class FwFromDestinationPrefixListExcept extends FwFrom {

  private final String _name;

  public FwFromDestinationPrefixListExcept(String name) {
    _name = name;
  }

  @Override
  public void applyTo(
      HeaderSpace.Builder headerSpaceBuilder,
      JuniperConfiguration jc,
      Warnings w,
      Configuration c) {
    PrefixList pl = jc.getMasterLogicalSystem().getPrefixLists().get(_name);
    if (pl != null) {
      if (pl.getIpv6()) {
        return;
      }
      RouteFilterList destinationPrefixList = c.getRouteFilterLists().get(_name);

      // if referenced prefix list is empty, it should not match anything
      if (destinationPrefixList.getLines().isEmpty()) {
        headerSpaceBuilder.addNotDstIp(EmptyIpSpace.INSTANCE);
        return;
      }

      headerSpaceBuilder.addNotDstIp(
          AclIpSpace.union(
              destinationPrefixList.getMatchingIps().stream()
                  .map(IpWildcard::toIpSpace)
                  .collect(ImmutableList.toImmutableList())));
    } else {
      w.redFlag("Reference to undefined destination prefix-list: \"" + _name + "\"");
    }
  }

  @Override
  Field getField() {
    return Field.DESTINATION;
  }

  @Override
  TraceElement getTraceElement() {
    return TraceElement.of(String.format("Matched destination-prefix-list %s except", _name));
  }

  @Override
  HeaderSpace toHeaderspace(JuniperConfiguration jc, Configuration c, Warnings w) {
    PrefixList pl = jc.getMasterLogicalSystem().getPrefixLists().get(_name);

    if (pl == null) {
      w.redFlag("Reference to undefined destination-prefix-list: \"" + _name + "\"");
      // match nothing
      return HeaderSpace.builder().setNotDstIps(EmptyIpSpace.INSTANCE).build();
    }

    if (pl.getIpv6()) {
      // do not handle Ipv6 for now, assume matching nothing
      return HeaderSpace.builder().setNotDstIps(EmptyIpSpace.INSTANCE).build();
    }

    RouteFilterList destinationPrefixList = c.getRouteFilterLists().get(_name);

    // if referenced prefix list is empty, it should not match anything
    if (destinationPrefixList.getLines().isEmpty()) {
      return HeaderSpace.builder().setNotDstIps(EmptyIpSpace.INSTANCE).build();
    }

    return HeaderSpace.builder()
        .setNotDstIps(
            AclIpSpace.union(
                destinationPrefixList.getMatchingIps().stream()
                    .map(IpWildcard::toIpSpace)
                    .collect(ImmutableList.toImmutableList())))
        .build();
  }
}
