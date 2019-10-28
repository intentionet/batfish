package org.batfish.datamodel.phc_to_flow;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.specifier.Location;

/** Extract Ip procotol from {@link PacketHeaderConstraints}; if failed use TCP as the default. */
@ParametersAreNonnullByDefault
public enum IpProtocolExtractorDefaultTcp implements FieldExtractor<IpProtocol> {
  /** single instance */
  INSTANCE;

  @Override
  public IpProtocol getValue(PacketHeaderConstraints phc, Location srcLoction) {
    Set<IpProtocol> ipProtocols =
        Optional.ofNullable(phc.resolveIpProtocols()).orElse(ImmutableSet.of(IpProtocol.TCP));

    checkArgument(ipProtocols.size() == 1, "Cannot construct flow with multiple IP protocols");

    return ipProtocols.iterator().next();
  }
}
