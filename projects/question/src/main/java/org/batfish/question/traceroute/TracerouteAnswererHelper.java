package org.batfish.question.traceroute;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.datamodel.SetFlowStartLocation.setStartLocation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.PacketHeaderConstraintsUtil;
import org.batfish.datamodel.visitors.IpSpaceRepresentative;
import org.batfish.specifier.AllInterfacesLocationSpecifier;
import org.batfish.specifier.InferFromLocationIpSpaceSpecifier;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.IpSpaceAssignment.Entry;
import org.batfish.specifier.IpSpaceSpecifier;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationSpecifier;
import org.batfish.specifier.SpecifierContext;
import org.batfish.specifier.SpecifierFactories;

/**
 * Helper for {@link TracerouteAnswerer} and {@link BidirectionalTracerouteAnswerer}. Processes
 * question parameters and constructs {@link Flow Flows} for the backend engine.
 */
public final class TracerouteAnswererHelper {
  private final IpSpaceRepresentative _ipSpaceRepresentative;
  private final PacketHeaderConstraints _packetHeaderConstraints;
  private final String _sourceLocationStr;
  private final IpSpaceAssignment _sourceIpAssignment;
  private final SpecifierContext _specifierContext;

  public TracerouteAnswererHelper(
      PacketHeaderConstraints packetHeaderConstraints,
      String sourceLocationStr,
      SpecifierContext specifierContext) {
    _ipSpaceRepresentative = new IpSpaceRepresentative();
    _packetHeaderConstraints = packetHeaderConstraints;
    _sourceLocationStr = sourceLocationStr;
    _specifierContext = specifierContext;

    _sourceIpAssignment =
        initSourceIpAssignment(
            _sourceLocationStr, _packetHeaderConstraints.getSrcIps(), _specifierContext);
  }

  private static final int TRACEROUTE_PORT = 33434;

  @VisibleForTesting
  static IpSpaceAssignment initSourceIpAssignment(
      String sourceLocation, String sourceIps, SpecifierContext specifierContext) {
    /* construct specifiers */
    LocationSpecifier sourceLocationSpecifier =
        SpecifierFactories.getLocationSpecifierOrDefault(
            sourceLocation, AllInterfacesLocationSpecifier.INSTANCE);

    IpSpaceSpecifier sourceIpSpaceSpecifier =
        SpecifierFactories.getIpSpaceSpecifierOrDefault(
            sourceIps, InferFromLocationIpSpaceSpecifier.INSTANCE);

    /* resolve specifiers */
    Set<Location> sourceLocations = sourceLocationSpecifier.resolve(specifierContext);
    return sourceIpSpaceSpecifier.resolve(sourceLocations, specifierContext);
  }

  private void setSrcIp(
      PacketHeaderConstraints constraints, Location srcLocation, Flow.Builder builder) {
    String headerSrcIp = constraints.getSrcIps();
    if (headerSrcIp != null) {
      // interpret given Src IP "flexibly"
      IpSpaceSpecifier srcIpSpecifier =
          SpecifierFactories.getIpSpaceSpecifierOrDefault(
              headerSrcIp, InferFromLocationIpSpaceSpecifier.INSTANCE);
      // Resolve to set of locations/IPs
      IpSpaceAssignment srcIps = srcIpSpecifier.resolve(ImmutableSet.of(), _specifierContext);
      // Filter out empty IP assignments
      List<Entry> nonEmptyIpSpaces =
          srcIps.getEntries().stream()
              .filter(e -> !e.getIpSpace().equals(EmptyIpSpace.INSTANCE))
              .collect(ImmutableList.toImmutableList());
      checkArgument(
          !nonEmptyIpSpaces.isEmpty(), "At least one source IP is required, could not resolve any");
      checkArgument(
          nonEmptyIpSpaces.size() == 1,
          "Specified source IP %s resolves to more than one location/IP: %s",
          headerSrcIp,
          nonEmptyIpSpaces);
      IpSpace space = srcIps.getEntries().iterator().next().getIpSpace();
      Optional<Ip> srcIp = _ipSpaceRepresentative.getRepresentative(space);
      // Extra check to ensure that we actually got an IP
      checkArgument(srcIp.isPresent(), "At least one source IP is required, could not resolve any");
      builder.setSrcIp(srcIp.get());
    } else {
      // Use from source location to determine header Src IP
      Optional<Entry> entry =
          _sourceIpAssignment.getEntries().stream()
              .filter(e -> e.getLocations().contains(srcLocation))
              .findFirst();

      checkArgument(
          entry.isPresent(),
          "Cannot resolve a source IP address from location %s",
          _sourceLocationStr);
      Optional<Ip> srcIp = _ipSpaceRepresentative.getRepresentative(entry.get().getIpSpace());
      checkArgument(
          srcIp.isPresent(),
          "At least one source IP is required, location %s produced none",
          srcLocation);
      builder.setSrcIp(srcIp.get());
    }
  }

  private void setDstIp(PacketHeaderConstraints constraints, Flow.Builder builder) {
    String headerDstIp = constraints.getDstIps();
    checkArgument(
        constraints.getDstIps() != null, "Cannot perform traceroute without a destination");
    IpSpaceSpecifier dstIpSpecifier =
        SpecifierFactories.getIpSpaceSpecifierOrDefault(
            headerDstIp, InferFromLocationIpSpaceSpecifier.INSTANCE);
    IpSpaceAssignment dstIps = dstIpSpecifier.resolve(ImmutableSet.of(), _specifierContext);
    checkArgument(
        dstIps.getEntries().size() == 1,
        "Specified destination: %s, resolves to more than one IP",
        headerDstIp);
    IpSpace space = dstIps.getEntries().iterator().next().getIpSpace();
    Optional<Ip> dstIp = _ipSpaceRepresentative.getRepresentative(space);
    checkArgument(dstIp.isPresent(), "At least one destination IP is required");
    builder.setDstIp(dstIp.get());
  }

  /**
   * Generate a flow builder given some set of packet header constraints.
   *
   * @param constraints {@link PacketHeaderConstraints}
   * @throws IllegalArgumentException if the {@code constraints} cannot be resolved to a single
   *     value.
   */
  private Flow.Builder headerConstraintsToFlow(
      PacketHeaderConstraints constraints, Location srcLocation) throws IllegalArgumentException {
    Flow.Builder builder = PacketHeaderConstraintsUtil.toFlow(constraints);

    // Extract and source IP from header constraints,
    setSrcIp(constraints, srcLocation, builder);
    setDstIp(constraints, builder);

    // Set defaults for protocol, and ports and packet lengths:
    if (builder.getIpProtocol() == null || builder.getIpProtocol() == IpProtocol.IP) {
      builder.setIpProtocol(IpProtocol.UDP);
    }
    if (builder.getDstPort() == null) {
      builder.setDstPort(TRACEROUTE_PORT);
    }
    if (builder.getSrcPort() == null) {
      builder.setSrcPort(NamedPort.EPHEMERAL_LOWEST.number());
    }
    return builder;
  }

  /** Generate a set of flows to do traceroute */
  @VisibleForTesting
  Set<Flow> getFlows(String tag) {
    Set<Location> srcLocations =
        SpecifierFactories.getLocationSpecifierOrDefault(
                _sourceLocationStr, AllInterfacesLocationSpecifier.INSTANCE)
            .resolve(_specifierContext);

    ImmutableSet.Builder<Flow> setBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<String> allProblems = ImmutableSet.builder();

    // Perform cross-product of all locations to flows
    for (Location srcLocation : srcLocations) {
      try {
        Flow.Builder flowBuilder = headerConstraintsToFlow(_packetHeaderConstraints, srcLocation);
        setStartLocation(_specifierContext.getConfigs(), flowBuilder, srcLocation);
        flowBuilder.setTag(tag);
        setBuilder.add(flowBuilder.build());
      } catch (IllegalArgumentException e) {
        // Try to ignore silently if possible
        allProblems.add(e.getMessage());
      }
    }

    Set<Flow> flows = setBuilder.build();
    checkArgument(
        !flows.isEmpty(),
        "Could not construct a flow for traceroute. Found issues: %s",
        String.join(",", allProblems.build()));
    return flows;
  }
}
