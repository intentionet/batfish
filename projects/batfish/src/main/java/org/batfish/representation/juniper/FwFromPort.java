package org.batfish.representation.juniper;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.SubRange;

public final class FwFromPort extends FwFrom {

  private final SubRange _portRange;

  public FwFromPort(int port) {
    _portRange = SubRange.singleton(port);
  }

  public FwFromPort(SubRange subrange) {
    _portRange = subrange;
  }

  @Override
  public void applyTo(
      HeaderSpace.Builder headerSpaceBuilder,
      JuniperConfiguration jc,
      Warnings w,
      Configuration c) {
    headerSpaceBuilder.setSrcOrDstPorts(
        Iterables.concat(headerSpaceBuilder.getSrcOrDstPorts(), ImmutableSet.of(_portRange)));
  }

  public SubRange getPortRange() {
    return _portRange;
  }
}
