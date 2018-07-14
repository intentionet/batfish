package org.batfish.representation.palo_alto;

import com.google.common.collect.ImmutableList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.acl.MatchHeaderSpace;

public final class Service implements ServiceGroupMember {
  private static final long serialVersionUID = 1L;

  private String _description;

  private final String _name;

  private final SortedSet<Integer> _ports;

  private IpProtocol _protocol;

  private final SortedSet<Integer> _sourcePorts;

  public Service(String name) {
    _name = name;
    _ports = new TreeSet<>();
    _sourcePorts = new TreeSet<>();
  }

  public String getDescription() {
    return _description;
  }

  @Override
  public String getName() {
    return _name;
  }

  public SortedSet<Integer> getPorts() {
    return _ports;
  }

  public IpProtocol getProtocol() {
    return _protocol;
  }

  public SortedSet<Integer> getSourcePorts() {
    return _sourcePorts;
  }

  public void setDescription(String description) {
    _description = description;
  }

  public void setProtocol(IpProtocol protocol) {
    _protocol = protocol;
  }

  @Override
  public IpAccessList toIpAccessList(LineAction action, PaloAltoConfiguration pc, Vsys vsys) {
    HeaderSpace.Builder headerSpaceBuilder = HeaderSpace.builder();
    headerSpaceBuilder.setSrcPorts(
        _sourcePorts.stream().map(SubRange::new).collect(Collectors.toSet()));
    headerSpaceBuilder.setDstPorts(_ports.stream().map(SubRange::new).collect(Collectors.toSet()));
    headerSpaceBuilder.setIpProtocols(ImmutableList.of(_protocol));
    return new IpAccessList(
        _name,
        ImmutableList.of(
            IpAccessListLine.builder()
                .setAction(action)
                .setMatchCondition(new MatchHeaderSpace(headerSpaceBuilder.build()))
                .build()));
  }
}
