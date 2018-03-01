package org.batfish.z3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.collections.FibRow;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.FibRowMatchExpr;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.OrExpr;
import org.batfish.z3.expr.RangeMatchExpr;
import org.batfish.z3.state.AclPermit;
import org.batfish.z3.state.StateParameter.Type;

public final class SynthesizerInputImpl implements SynthesizerInput {

  public static class Builder {
    private Map<String, Configuration> _configurations;

    private DataPlane _dataPlane;

    private Map<String, Set<String>> _disabledAcls;

    private Map<String, Set<String>> _disabledInterfaces;

    private Set<String> _disabledNodes;

    private Map<String, Set<String>> _disabledVrfs;

    private boolean _simplify;

    private Set<Type> _vectorizedParameters;

    private Builder() {
      _disabledAcls = ImmutableMap.of();
      _disabledInterfaces = ImmutableMap.of();
      _disabledNodes = ImmutableSet.of();
      _disabledVrfs = ImmutableMap.of();
      _simplify = false;
      _vectorizedParameters = ImmutableSet.of();
    }

    public SynthesizerInputImpl build() {
      return new SynthesizerInputImpl(
          _configurations,
          _dataPlane,
          _disabledAcls,
          _disabledInterfaces,
          _disabledNodes,
          _disabledVrfs,
          _simplify,
          _vectorizedParameters);
    }

    public Builder setConfigurations(Map<String, Configuration> configurations) {
      _configurations = configurations;
      return this;
    }

    public Builder setDataPlane(DataPlane dataPlane) {
      _dataPlane = dataPlane;
      return this;
    }

    public Builder setDisabledAcls(Map<String, Set<String>> disabledAcls) {
      _disabledAcls = disabledAcls;
      return this;
    }

    public Builder setDisabledInterfaces(Map<String, Set<String>> disabledInterfaces) {
      _disabledInterfaces = disabledInterfaces;
      return this;
    }

    public Builder setDisabledNodes(Set<String> disabledNodes) {
      _disabledNodes = disabledNodes;
      return this;
    }

    public Builder setDisabledVrfs(Map<String, Set<String>> disabledVrfs) {
      _disabledVrfs = disabledVrfs;
      return this;
    }

    public Builder setSimplify(boolean simplify) {
      _simplify = simplify;
      return this;
    }

    public Builder setVectorizedParameters(Set<Type> vectorizedParameters) {
      _vectorizedParameters = vectorizedParameters;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private static boolean isLoopbackInterface(String ifaceName) {
    String lcIfaceName = ifaceName.toLowerCase();
    return lcIfaceName.startsWith("lo");
  }

  private final Map<String, Map<String, Map<Integer, LineAction>>> _aclActions;

  private final Map<String, Map<String, Map<Integer, BooleanExpr>>> _aclConditions;

  private final Map<String, Configuration> _configurations;

  private final Map<String, Set<String>> _disabledAcls;

  private final Map<String, Set<String>> _disabledInterfaces;

  private final Set<String> _disabledNodes;

  private final Map<String, Set<String>> _disabledVrfs;

  private final Set<Edge> _edges;

  private final Map<String, Map<String, IpAccessList>> _enabledAcls;

  private final Set<Edge> _enabledEdges;

  private final Set<NodeInterfacePair> _enabledFlowSinks;

  private final Map<String, Set<String>> _enabledInterfaces;

  private final Map<String, Map<String, Set<String>>> _enabledInterfacesByNodeVrf;

  private final Set<String> _enabledNodes;

  private final Map<String, Set<String>> _enabledVrfs;

  private final Map<String, Map<String, Map<String, Map<NodeInterfacePair, BooleanExpr>>>>
      _fibConditions;

  private final Map<String, Map<String, SortedSet<FibRow>>> _fibs;

  private final Set<NodeInterfacePair> _flowSinks;

  private final Map<String, Map<String, String>> _incomingAcls;

  private final Map<String, Set<Ip>> _ipsByHostname;

  private final Map<String, Map<String, String>> _outgoingAcls;

  private final boolean _simplify;

  private final Map<String, Map<String, List<Entry<Optional<BasicStateExpr>, BooleanExpr>>>>
      _sourceNats;

  private final Map<String, Set<String>> _topologyInterfaces;

  private final Set<Type> _vectorizedParameters;

  public SynthesizerInputImpl(
      Map<String, Configuration> configurations,
      DataPlane dataPlane,
      Map<String, Set<String>> disabledAcls,
      Map<String, Set<String>> disabledInterfaces,
      Set<String> disabledNodes,
      Map<String, Set<String>> disabledVrfs,
      boolean simplify,
      Set<Type> vectorizedParameters) {
    if (configurations == null) {
      throw new BatfishException("Must supply configurations");
    }
    _configurations = ImmutableMap.copyOf(configurations);
    _disabledAcls = ImmutableMap.copyOf(disabledAcls);
    _disabledInterfaces = ImmutableMap.copyOf(disabledInterfaces);
    _disabledNodes = ImmutableSet.copyOf(disabledNodes);
    _disabledVrfs = ImmutableMap.copyOf(disabledVrfs);
    _enabledNodes = computeEnabledNodes();
    _enabledVrfs = computeEnabledVrfs();
    _enabledInterfacesByNodeVrf = computeEnabledInterfacesByNodeVrf();
    _enabledInterfaces = computeEnabledInterfaces();
    _incomingAcls = computeIncomingAcls();
    _outgoingAcls = computeOutgoingAcls();
    _simplify = simplify;
    _vectorizedParameters = vectorizedParameters;
    if (dataPlane != null) {
      _fibs = ImmutableMap.copyOf(dataPlane.getFibRows());
      _flowSinks = ImmutableSet.copyOf(dataPlane.getFlowSinks());
      _enabledFlowSinks = computeEnabledFlowSinks();
      _ipsByHostname = computeIpsByHostname();
      _fibConditions = computeFibConditions();
      _edges = ImmutableSet.copyOf(dataPlane.getTopologyEdges());
      _enabledEdges = computeEnabledEdges();
      _topologyInterfaces = computeTopologyInterfaces();
      _sourceNats = computeSourceNats();
    } else {
      _fibs = null;
      _flowSinks = null;
      _enabledFlowSinks = null;
      _ipsByHostname = null;
      _fibConditions = null;
      _edges = null;
      _enabledEdges = null;
      _topologyInterfaces = null;
      _sourceNats = null;
    }
    _enabledAcls = computeEnabledAcls();
    _aclActions = computeAclActions();
    _aclConditions = computeAclConditions();
  }

  private Map<String, Map<String, Map<Integer, LineAction>>> computeAclActions() {
    return _enabledAcls
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                e ->
                    e.getValue()
                        .entrySet()
                        .stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey,
                                e2 -> {
                                  IpAccessList acl = e2.getValue();
                                  ImmutableMap.Builder<Integer, LineAction> lineActions =
                                      ImmutableMap.builder();
                                  List<IpAccessListLine> lines = acl.getLines();
                                  for (int i = 0; i < lines.size(); i++) {
                                    lineActions.put(i, lines.get(i).getAction());
                                  }
                                  return lineActions.build();
                                }))));
  }

  private Map<String, Map<String, Map<Integer, BooleanExpr>>> computeAclConditions() {
    return _enabledAcls
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                e ->
                    e.getValue()
                        .entrySet()
                        .stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey,
                                e2 -> {
                                  IpAccessList acl = e2.getValue();
                                  ImmutableMap.Builder<Integer, BooleanExpr> lineConditions =
                                      ImmutableMap.builder();
                                  List<IpAccessListLine> lines = acl.getLines();
                                  for (int i = 0; i < lines.size(); i++) {
                                    lineConditions.put(i, new HeaderSpaceMatchExpr(lines.get(i)));
                                  }
                                  return lineConditions.build();
                                }))));
  }

  private Map<String, Map<String, IpAccessList>> computeEnabledAcls() {
    if (_topologyInterfaces != null) {
      return _topologyInterfaces
          .entrySet()
          .stream()
          .collect(
              ImmutableMap.toImmutableMap(
                  Entry::getKey,
                  topologyInterfacesEntry -> {
                    String hostname = topologyInterfacesEntry.getKey();
                    Configuration c = _configurations.get(hostname);
                    return topologyInterfacesEntry
                        .getValue()
                        .stream()
                        .flatMap(
                            ifaceName -> {
                              Interface i = c.getInterfaces().get(ifaceName);
                              ImmutableList.Builder<Pair<String, IpAccessList>> interfaceAcls =
                                  ImmutableList.builder();
                              IpAccessList aclIn = i.getIncomingFilter();
                              IpAccessList aclOut = i.getOutgoingFilter();
                              if (aclIn != null) {
                                interfaceAcls.add(new Pair<>(aclIn.getName(), aclIn));
                              }
                              if (aclOut != null) {
                                interfaceAcls.add(new Pair<>(aclOut.getName(), aclOut));
                              }
                              i.getSourceNats()
                                  .forEach(
                                      sourceNat -> {
                                        IpAccessList sourceNatAcl = sourceNat.getAcl();
                                        if (sourceNatAcl != null) {
                                          interfaceAcls.add(
                                              new Pair<>(sourceNatAcl.getName(), sourceNatAcl));
                                        }
                                      });

                              return interfaceAcls.build().stream();
                            })
                        .collect(ImmutableSet.toImmutableSet())
                        .stream()
                        .collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));
                  }));
    } else {
      return _configurations
          .entrySet()
          .stream()
          .filter(e -> !_disabledNodes.contains(e.getKey()))
          .collect(
              ImmutableMap.toImmutableMap(
                  Entry::getKey,
                  e -> {
                    String hostname = e.getKey();
                    Set<String> disabledAcls = _disabledAcls.get(hostname);
                    return e.getValue()
                        .getIpAccessLists()
                        .entrySet()
                        .stream()
                        .filter(e2 -> disabledAcls == null || !disabledAcls.contains(e2.getKey()))
                        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
                  }));
    }
  }

  private Set<Edge> computeEnabledEdges() {
    return _edges
        .stream()
        .filter(
            e -> {
              Set<String> enabledInterfaces1 = _enabledInterfaces.get(e.getNode1());
              Set<String> enabledInterfaces2 = _enabledInterfaces.get(e.getNode2());
              return enabledInterfaces1 != null
                  && enabledInterfaces1.contains(e.getInt1())
                  && enabledInterfaces2 != null
                  && enabledInterfaces2.contains(e.getInt2());
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  private Set<NodeInterfacePair> computeEnabledFlowSinks() {
    return _flowSinks
        .stream()
        .filter(
            f -> {
              Set<String> enabledInterfaces = _enabledInterfaces.get(f.getHostname());
              return enabledInterfaces != null && enabledInterfaces.contains(f.getInterface());
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  private Map<String, Set<String>> computeEnabledInterfaces() {
    return _enabledInterfacesByNodeVrf
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                enabledInterfacesByNodeVrfEntry ->
                    enabledInterfacesByNodeVrfEntry
                        .getValue()
                        .entrySet()
                        .stream()
                        .flatMap(
                            enabledInterfacesByVrfEntry ->
                                enabledInterfacesByVrfEntry.getValue().stream())
                        .collect(ImmutableSet.toImmutableSet())));
  }

  private Map<String, Map<String, Set<String>>> computeEnabledInterfacesByNodeVrf() {
    return _enabledVrfs
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                enabledVrfsEntry -> {
                  String hostname = enabledVrfsEntry.getKey();
                  Set<String> disabledInterfaces = _disabledInterfaces.get(hostname);
                  Configuration c = _configurations.get(hostname);
                  return enabledVrfsEntry
                      .getValue()
                      .stream()
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Function.identity(),
                              vrfName ->
                                  c.getVrfs()
                                      .get(vrfName)
                                      .getInterfaces()
                                      .entrySet()
                                      .stream()
                                      .filter(
                                          interfaceEntry ->
                                              disabledInterfaces == null
                                                  || !disabledInterfaces.contains(
                                                      interfaceEntry.getKey()))
                                      .filter(
                                          interfaceEntry -> interfaceEntry.getValue().getActive())
                                      .filter(
                                          interfaceEntry ->
                                              !interfaceEntry.getValue().getBlacklisted())
                                      .map(Entry::getKey)
                                      .collect(ImmutableSet.toImmutableSet())));
                }));
  }

  private Set<String> computeEnabledNodes() {
    return ImmutableSet.copyOf(Sets.difference(_configurations.keySet(), _disabledNodes));
  }

  private Map<String, Set<String>> computeEnabledVrfs() {
    return _enabledNodes
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Function.identity(),
                hostname -> {
                  Set<String> disabledVrfs = _disabledVrfs.get(hostname);
                  return _configurations
                      .get(hostname)
                      .getVrfs()
                      .keySet()
                      .stream()
                      .filter(vrfName -> disabledVrfs == null || !disabledVrfs.contains(vrfName))
                      .collect(ImmutableSet.toImmutableSet());
                }));
  }

  private Map<String, Map<String, Map<String, Map<NodeInterfacePair, BooleanExpr>>>>
      computeFibConditions() {
    return _configurations
        .entrySet()
        .stream()
        .filter(e -> !_disabledNodes.contains(e.getKey()))
        .filter(e -> _fibs.containsKey(e.getKey()))
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                e -> {
                  String hostname = e.getKey();
                  Configuration c = e.getValue();
                  Set<String> disabledVrfs = _disabledVrfs.get(hostname);
                  return c.getVrfs()
                      .keySet()
                      .stream()
                      .filter(vrfName -> disabledVrfs == null || !disabledVrfs.contains(vrfName))
                      .filter(_fibs.get(hostname)::containsKey)
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Function.identity(),
                              vrfName -> computeFibConditionsByInterface(hostname, vrfName)));
                }));
  }

  private Map<String, Map<NodeInterfacePair, BooleanExpr>> computeFibConditionsByInterface(
      String hostname, String vrfName) {
    Map<String, Map<NodeInterfacePair, ImmutableList.Builder<BooleanExpr>>> conditionsByInterface =
        new HashMap<>();
    SortedSet<FibRow> fibSet = _fibs.get(hostname).get(vrfName);
    List<FibRow> fib = ImmutableList.copyOf(fibSet);
    for (int i = 0; i < fib.size(); i++) {
      FibRow currentRow = fib.get(i);
      String ifaceOutName = currentRow.getInterface();
      NodeInterfacePair receiver;
      if (isLoopbackInterface(ifaceOutName)
          || CommonUtil.isNullInterface(ifaceOutName)
          || ifaceOutName.equals(FibRow.DROP_NO_ROUTE)) {
        receiver = NodeInterfacePair.NONE;
      } else {
        receiver = new NodeInterfacePair(currentRow.getNextHop(), currentRow.getNextHopInterface());
      }

      conditionsByInterface
          .computeIfAbsent(ifaceOutName, n -> new HashMap<>())
          .computeIfAbsent(receiver, r -> ImmutableList.builder())
          .add(FibRowMatchExpr.getFibRowConditions(hostname, vrfName, fib, i, currentRow));
    }
    conditionsByInterface.entrySet().stream();
    return conditionsByInterface
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                conditionsByInterfaceEntry ->
                    conditionsByInterfaceEntry
                        .getValue()
                        .entrySet()
                        .stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey,
                                conditionsByReceiverEntry ->
                                    new OrExpr(conditionsByReceiverEntry.getValue().build())))));
  }

  private Map<String, Map<String, String>> computeIncomingAcls() {
    return _enabledInterfaces
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                enabledInterfacesEntry -> {
                  Configuration c = _configurations.get(enabledInterfacesEntry.getKey());
                  return enabledInterfacesEntry
                      .getValue()
                      .stream()
                      .filter(
                          ifaceName ->
                              c.getInterfaces().get(ifaceName).getIncomingFilterName() != null)
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Function.identity(),
                              ifaceName ->
                                  c.getInterfaces().get(ifaceName).getIncomingFilterName()));
                }));
  }

  private Map<String, Set<Ip>> computeIpsByHostname() {
    Map<String, Map<String, Interface>> enabledInterfaces =
        _enabledInterfaces
            .entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey,
                    enabledInterfacesEntry -> {
                      Configuration c = _configurations.get(enabledInterfacesEntry.getKey());
                      return enabledInterfacesEntry
                          .getValue()
                          .stream()
                          .collect(
                              ImmutableMap.toImmutableMap(
                                  Function.identity(),
                                  ifaceName -> c.getInterfaces().get(ifaceName)));
                    }));
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpOwners(true, enabledInterfaces);
    Map<String, Set<Ip>> map = new HashMap<>();
    ipOwners.forEach(
        (ip, owners) -> {
          for (String owner : owners) {
            map.computeIfAbsent(owner, o -> new HashSet<>()).add(ip);
          }
        });
    return map.entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(Entry::getKey, e -> ImmutableSet.copyOf(e.getValue())));
  }

  private Map<String, Map<String, String>> computeOutgoingAcls() {
    return _enabledInterfaces
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                enabledInterfacesEntry -> {
                  Configuration c = _configurations.get(enabledInterfacesEntry.getKey());
                  return enabledInterfacesEntry
                      .getValue()
                      .stream()
                      .filter(
                          ifaceName ->
                              c.getInterfaces().get(ifaceName).getOutgoingFilterName() != null)
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Function.identity(),
                              ifaceName ->
                                  c.getInterfaces().get(ifaceName).getOutgoingFilterName()));
                }));
  }

  private Map<String, Map<String, List<Entry<Optional<BasicStateExpr>, BooleanExpr>>>>
      computeSourceNats() {
    return _topologyInterfaces
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                topologyInterfacesEntryByHostname -> {
                  String hostname = topologyInterfacesEntryByHostname.getKey();
                  Set<String> ifaces = topologyInterfacesEntryByHostname.getValue();
                  Configuration c = _configurations.get(hostname);
                  return ifaces
                      .stream()
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Function.identity(),
                              ifaceName ->
                                  c.getInterfaces()
                                      .get(ifaceName)
                                      .getSourceNats()
                                      .stream()
                                      .map(
                                          sourceNat -> {
                                            IpAccessList acl = sourceNat.getAcl();
                                            Optional<BasicStateExpr>
                                                preconditionPreTransformationState =
                                                    acl != null
                                                        ? Optional.of(
                                                            new AclPermit(hostname, acl.getName()))
                                                        : Optional.empty();
                                            BooleanExpr transformationConstraint =
                                                new RangeMatchExpr(
                                                    TransformationHeaderField.NEW_SRC_IP,
                                                    TransformationHeaderField.NEW_SRC_IP.getSize(),
                                                    ImmutableSet.of(
                                                        Range.closed(
                                                            sourceNat.getPoolIpFirst().asLong(),
                                                            sourceNat.getPoolIpLast().asLong())));
                                            return Maps.immutableEntry(
                                                preconditionPreTransformationState,
                                                transformationConstraint);
                                          })
                                      .collect(ImmutableList.toImmutableList())));
                }));
  }

  private Map<String, Set<String>> computeTopologyInterfaces() {
    Map<String, Set<String>> topologyEdges = new HashMap<>();
    _enabledEdges
        .stream()
        .forEach(
            enabledEdge ->
                topologyEdges
                    .computeIfAbsent(enabledEdge.getNode1(), n -> new HashSet<>())
                    .add(enabledEdge.getInt1()));
    _enabledFlowSinks
        .stream()
        .forEach(
            enabledFlowSink ->
                topologyEdges
                    .computeIfAbsent(enabledFlowSink.getHostname(), n -> new HashSet<>())
                    .add(enabledFlowSink.getInterface()));
    return topologyEdges
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(Entry::getKey, e -> ImmutableSet.copyOf(e.getValue())));
  }

  @Override
  public Map<String, Map<String, Map<Integer, LineAction>>> getAclActions() {
    return _aclActions;
  }

  /**
   * Mapping: hostname -> aclName -> lineNumber -> lineConditions <br>
   * lineConditions is a boolean expression representing the constraints on a header necessary for
   * that line to be matched.
   */
  @Override
  public Map<String, Map<String, Map<Integer, BooleanExpr>>> getAclConditions() {
    return _aclConditions;
  }

  @Override
  public Set<Edge> getEnabledEdges() {
    return _enabledEdges;
  }

  @Override
  public Set<NodeInterfacePair> getEnabledFlowSinks() {
    return _enabledFlowSinks;
  }

  @Override
  public Map<String, Set<String>> getEnabledInterfaces() {
    return _enabledInterfaces;
  }

  @Override
  public Map<String, Map<String, Set<String>>> getEnabledInterfacesByNodeVrf() {
    return _enabledInterfacesByNodeVrf;
  }

  @Override
  public Set<String> getEnabledNodes() {
    return _enabledNodes;
  }

  @Override
  public Map<String, Set<String>> getEnabledVrfs() {
    return _enabledVrfs;
  }

  @Override
  public Map<String, Map<String, Map<String, Map<NodeInterfacePair, BooleanExpr>>>>
      getFibConditions() {
    return _fibConditions;
  }

  @Override
  public Map<String, Map<String, String>> getIncomingAcls() {
    return _incomingAcls;
  }

  @Override
  public Map<String, Set<Ip>> getIpsByHostname() {
    return _ipsByHostname;
  }

  @Override
  public Map<String, Map<String, String>> getOutgoingAcls() {
    return _outgoingAcls;
  }

  @Override
  public boolean getSimplify() {
    return _simplify;
  }

  @Override
  public Map<String, Map<String, List<Entry<Optional<BasicStateExpr>, BooleanExpr>>>>
      getSourceNats() {
    return _sourceNats;
  }

  @Override
  public Map<String, Set<String>> getTopologyInterfaces() {
    return _topologyInterfaces;
  }

  @Override
  public Set<Type> getVectorizedParameters() {
    return _vectorizedParameters;
  }
}
