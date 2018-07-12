package org.batfish.z3;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.emptySet;
import static org.batfish.common.util.CommonUtil.computeIpInterfaceOwners;
import static org.batfish.common.util.CommonUtil.computeIpVrfOwners;
import static org.batfish.common.util.CommonUtil.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.math.LongMath;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.ForwardingAnalysis;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.IntExpr;
import org.batfish.z3.expr.IpSpaceMatchExpr;
import org.batfish.z3.expr.LitIntExpr;
import org.batfish.z3.expr.RangeMatchExpr;
import org.batfish.z3.expr.TransformedVarIntExpr;
import org.batfish.z3.expr.visitors.IpSpaceBooleanExprTransformer;
import org.batfish.z3.state.AclPermit;
import org.batfish.z3.state.StateParameter.Type;

public final class SynthesizerInputImpl implements SynthesizerInput {

  static final String SRC_INTERFACE_FIELD_NAME = "SRC_INTERFACE";
  private final Map<IngressLocation, BooleanExpr> _srcIpConstraints;

  public static class Builder {
    private Map<String, Configuration> _configurations;

    private Map<String, Set<String>> _disabledInterfaces = ImmutableMap.of();

    private Set<String> _disabledNodes = ImmutableSet.of();

    private Map<String, Set<String>> _disabledVrfs = ImmutableMap.of();

    private Map<String, Set<String>> _enabledAclNames;

    private ForwardingAnalysis _forwardingAnalysis;

    @Nullable private HeaderSpace _headerSpace;

    private Map<IngressLocation, IpSpace> _srcIpConstraints = ImmutableMap.of();

    private Set<String> _nonTransitNodes = ImmutableSortedSet.of();

    private boolean _simplify = false;

    private boolean _specialize = true;

    private Topology _topology;

    private Set<String> _transitNodes = ImmutableSortedSet.of();

    private Set<Type> _vectorizedParameters = ImmutableSet.of();

    private Builder() {}

    public SynthesizerInputImpl build() {
      return new SynthesizerInputImpl(this);
    }

    public Builder setForwardingAnalysis(ForwardingAnalysis forwardingAnalysis) {
      _forwardingAnalysis = forwardingAnalysis;
      return this;
    }

    public Builder setConfigurations(Map<String, Configuration> configurations) {
      _configurations = configurations;
      return this;
    }

    public Builder setEnabledAcls(Map<String, Set<String>> enabledAcls) {
      _enabledAclNames = enabledAcls;
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

    public Builder setHeaderSpace(HeaderSpace headerSpace) {
      _headerSpace = headerSpace;
      return this;
    }

    public Builder setSimplify(boolean simplify) {
      _simplify = simplify;
      return this;
    }

    public Builder setSpecialize(boolean specialize) {
      _specialize = specialize;
      return this;
    }

    public Builder setTopology(Topology topology) {
      _topology = topology;
      return this;
    }

    public Builder setTransitNodes(Set<String> transitNodes) {
      _transitNodes = ImmutableSortedSet.copyOf(transitNodes);
      return this;
    }

    public Builder setVectorizedParameters(Set<Type> vectorizedParameters) {
      _vectorizedParameters = vectorizedParameters;
      return this;
    }

    public Builder setNonTransitNodes(Set<String> nonTransitNodes) {
      _nonTransitNodes = ImmutableSet.copyOf(nonTransitNodes);
      return this;
    }

    public Builder setSrcIpConstraints(Map<IngressLocation, IpSpace> srcIpConstraints) {
      _srcIpConstraints = ImmutableMap.copyOf(srcIpConstraints);
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final @Nonnull Map<String, Map<String, List<LineAction>>> _aclActions;

  private final @Nonnull Map<String, Map<String, List<BooleanExpr>>> _aclConditions;

  private final @Nullable Map<
          String, Map<String, Map<String, Map<String, Map<String, BooleanExpr>>>>>
      _arpTrueEdge;

  private final @Nonnull Map<String, Configuration> _configurations;

  private final @Nonnull Map<String, Set<String>> _disabledInterfaces;

  private final @Nonnull Set<String> _disabledNodes;

  private final @Nonnull Map<String, Set<String>> _disabledVrfs;

  private final @Nullable Set<Edge> _edges;

  private final @Nonnull Map<String, Map<String, IpAccessList>> _enabledAcls;

  private final @Nullable Set<Edge> _enabledEdges;

  private final @Nonnull Map<String, Set<String>> _enabledInterfaces;

  private final @Nonnull Map<String, Map<String, Set<String>>> _enabledInterfacesByNodeVrf;

  private final @Nonnull Set<String> _enabledNodes;

  private final @Nonnull Map<String, Set<String>> _enabledVrfs;

  private final @Nonnull Map<String, Map<String, String>> _incomingAcls;

  private final @Nonnull Map<String, IpAccessListSpecializer> _ipAccessListSpecializers;

  private final @Nullable Map<String, Set<Ip>> _ipsByHostname;

  private final @Nullable Map<String, Map<String, Set<Ip>>> _ipsByNodeVrf;

  private final @Nonnull Map<String, IpSpaceSpecializer> _ipSpaceSpecializers;

  private final boolean _dataPlane;

  private final @Nonnull Map<String, Map<String, IpSpace>> _namedIpSpaces;

  private final @Nullable Map<String, Map<String, Map<String, BooleanExpr>>> _neighborUnreachable;

  private final @Nonnull Map<String, List<String>> _nodeInterfaces;

  private final @Nonnull Set<String> _nodesWithSrcInterfaceConstraints;

  private final @Nonnull Set<String> _nonTransitNodes;

  private final @Nullable Map<String, Map<String, BooleanExpr>> _nullRoutedIps;

  private final @Nonnull Map<String, Map<String, String>> _outgoingAcls;

  private final @Nullable Map<String, Map<String, BooleanExpr>> _routableIps;

  private final boolean _simplify;

  private final @Nonnull IpSpace _specializationIpSpace;

  private final @Nonnull Field _sourceInterfaceField;

  private final @Nonnull Map<String, Map<String, IntExpr>> _sourceInterfaceFieldValues;

  private final @Nullable Map<String, Map<String, List<Entry<AclPermit, BooleanExpr>>>> _sourceNats;

  private final @Nullable Map<String, Set<String>> _topologyInterfaces;

  private final @Nonnull Set<String> _transitNodes;

  private final @Nonnull Set<Type> _vectorizedParameters;

  private SynthesizerInputImpl(Builder builder) {
    if (builder._configurations == null) {
      throw new BatfishException("Must supply configurations");
    }
    _configurations = ImmutableMap.copyOf(builder._configurations);
    _namedIpSpaces =
        toImmutableMap(_configurations, Entry::getKey, entry -> entry.getValue().getIpSpaces());
    HeaderSpace headerSpace =
        builder._headerSpace != null ? builder._headerSpace : new HeaderSpace();
    _specializationIpSpace =
        builder._specialize
            ? firstNonNull(
                AclIpSpace.difference(headerSpace.getDstIps(), headerSpace.getNotDstIps()),
                UniverseIpSpace.INSTANCE)
            : UniverseIpSpace.INSTANCE;
    _ipSpaceSpecializers =
        toImmutableMap(
            _namedIpSpaces,
            Entry::getKey,
            namedIpSpacesEntry ->
                new IpSpaceIpSpaceSpecializer(
                    _specializationIpSpace, namedIpSpacesEntry.getValue()));
    _ipAccessListSpecializers =
        builder._specialize
            ? toImmutableMap(
                _namedIpSpaces,
                Entry::getKey,
                namedIpSpacesEntry ->
                    new IpSpaceIpAccessListSpecializer(headerSpace, namedIpSpacesEntry.getValue()))
            : ImmutableMap.of();
    _disabledInterfaces = ImmutableMap.copyOf(builder._disabledInterfaces);
    _disabledNodes = ImmutableSet.copyOf(builder._disabledNodes);
    _disabledVrfs = ImmutableMap.copyOf(builder._disabledVrfs);
    _enabledAcls = computeEnabledAcls(builder._enabledAclNames);
    _enabledNodes = computeEnabledNodes();
    _enabledVrfs = computeEnabledVrfs();
    _enabledInterfacesByNodeVrf = computeEnabledInterfacesByNodeVrf();
    _enabledInterfaces = computeEnabledInterfaces();
    _incomingAcls = computeIncomingAcls();
    _srcIpConstraints = computeSrcIpConstraints(builder._srcIpConstraints);
    _outgoingAcls = computeOutgoingAcls();
    _simplify = builder._simplify;
    _vectorizedParameters = builder._vectorizedParameters;

    ForwardingAnalysis forwardingAnalysis = builder._forwardingAnalysis;
    _dataPlane = forwardingAnalysis != null;
    if (_dataPlane) {
      _arpTrueEdge = computeArpTrueEdge(forwardingAnalysis.getArpTrueEdge());
      _neighborUnreachable =
          computeNeighborUnreachable(forwardingAnalysis.getNeighborUnreachable());
      _nullRoutedIps = computeNullRoutedIps(forwardingAnalysis.getNullRoutedIps());
      _routableIps = computeRoutableIps(forwardingAnalysis.getRoutableIps());
      _ipsByHostname = computeIpsByHostname();
      _ipsByNodeVrf = computeIpsByNodeVrf();
      _edges = builder._topology.getEdges();
      _enabledEdges = computeEnabledEdges();
      _topologyInterfaces = computeTopologyInterfaces();
      _sourceNats = computeSourceNats();
    } else {
      _arpTrueEdge = null;
      _neighborUnreachable = null;
      _nullRoutedIps = null;
      _routableIps = null;
      _ipsByHostname = null;
      _ipsByNodeVrf = null;
      _edges = null;
      _enabledEdges = null;
      _topologyInterfaces = null;
      _sourceNats = null;
    }
    _aclActions = computeAclActions();
    _nodeInterfaces = computeNodeInterfaces();
    _nodesWithSrcInterfaceConstraints = computeNodesWithSrcInterfaceConstraints();
    _sourceInterfaceField = computeSourceInterfaceField();
    _sourceInterfaceFieldValues = computeSourceInterfaceFieldValues();
    _nonTransitNodes = ImmutableSortedSet.copyOf(builder._nonTransitNodes);
    _transitNodes = ImmutableSortedSet.copyOf(builder._transitNodes);
    _aclConditions = computeAclConditions();
  }

  private static Map<IngressLocation, BooleanExpr> computeSrcIpConstraints(
      Map<IngressLocation, IpSpace> ipSpaceAssignment) {
    // Named IP spaces are not allowed here. TODO do we need them?
    IpSpaceBooleanExprTransformer toBooleanExpr =
        new IpSpaceBooleanExprTransformer(ImmutableMap.of(), Field.ORIG_SRC_IP);
    return toImmutableMap(
        ipSpaceAssignment, Entry::getKey, entry -> entry.getValue().accept(toBooleanExpr));
  }

  private Set<String> computeNodesWithSrcInterfaceConstraints() {
    return _configurations
        .entrySet()
        .stream()
        .filter(
            entry -> {
              DependsOnSourceInterface dependsOnSourceInterface =
                  new DependsOnSourceInterface(entry.getValue().getIpAccessLists());
              return entry
                  .getValue()
                  .getIpAccessLists()
                  .values()
                  .stream()
                  .anyMatch(dependsOnSourceInterface::dependsOnSourceInterface);
            })
        .map(Entry::getKey)
        .collect(ImmutableSet.toImmutableSet());
  }

  private Field computeSourceInterfaceField() {
    /* The number of values the field needs to be able to have, equal to the maximum number of
     * interfaces on a single node, plus 1 (for the "no source interface" case that can arise
     * if we originate at a Vrf for example).
     */
    int numValues =
        _nodesWithSrcInterfaceConstraints
                .stream()
                .mapToInt(node -> _nodeInterfaces.get(node).size())
                .max()
                .orElse(0)
            + 1;
    int fieldBits = Math.max(LongMath.log2(numValues, RoundingMode.CEILING), 1);
    return new Field(SRC_INTERFACE_FIELD_NAME, fieldBits);
  }

  private Map<String, List<String>> computeNodeInterfaces() {
    return _enabledNodes
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Function.identity(),
                node ->
                    ImmutableList.sortedCopyOf(
                        _configurations.get(node).getInterfaces().keySet())));
  }

  private Map<String, Map<String, IntExpr>> computeSourceInterfaceFieldValues() {
    Field sourceInterfaceField = _sourceInterfaceField;
    return toImmutableMap(
        _nodeInterfaces,
        Entry::getKey,
        entry -> {
          ImmutableMap.Builder<String, IntExpr> values = ImmutableMap.builder();
          CommonUtil.forEachWithIndex(
              entry.getValue(),
              (index, iface) ->
                  values.put(iface, new LitIntExpr(index + 1, sourceInterfaceField.getSize())));
          return values.build();
        });
  }

  private Map<String, Map<String, List<LineAction>>> computeAclActions() {
    return toImmutableMap(
        _enabledAcls,
        Entry::getKey,
        enabledAclsByHostnameEntry ->
            toImmutableMap(
                enabledAclsByHostnameEntry.getValue(),
                Entry::getKey,
                enabledAclsByAclNameEntry ->
                    enabledAclsByAclNameEntry
                        .getValue()
                        .getLines()
                        .stream()
                        .map(IpAccessListLine::getAction)
                        .collect(ImmutableList.toImmutableList())));
  }

  private Map<String, Map<String, List<BooleanExpr>>> computeAclConditions() {
    return toImmutableMap(
        _enabledAcls,
        Entry::getKey, /* Node name */
        e -> {
          String node = e.getKey();
          Map<String, IpAccessList> nodeAcls =
              _configurations.get(node).getIpAccessLists(); // e.getValue();
          AclLineMatchExprToBooleanExpr aclLineMatchExprToBooleanExpr =
              new AclLineMatchExprToBooleanExpr(
                  nodeAcls,
                  _namedIpSpaces.get(node),
                  _sourceInterfaceField,
                  _sourceInterfaceFieldValues.get(node));
          return toImmutableMap(
              e.getValue(),
              Entry::getKey, /* Acl name */
              e2 ->
                  e2.getValue()
                      .getLines()
                      .stream()
                      .map(IpAccessListLine::getMatchCondition)
                      .map(aclLineMatchExprToBooleanExpr::toBooleanExpr)
                      .collect(ImmutableList.toImmutableList()));
        });
  }

  private Map<String, Map<String, Map<String, Map<String, Map<String, BooleanExpr>>>>>
      computeArpTrueEdge(Map<Edge, IpSpace> arpTrueEdge) {
    Map<String, Map<String, Map<String, Map<String, Map<String, BooleanExpr>>>>> output =
        new HashMap<>();
    arpTrueEdge.forEach(
        (edge, ipSpace) -> {
          String hostname = edge.getNode1();
          ipSpace = _ipSpaceSpecializers.get(hostname).specialize(ipSpace);
          if (ipSpace instanceof EmptyIpSpace) {
            return;
          }
          String outInterface = edge.getInt1();
          String vrf = _configurations.get(hostname).getInterfaces().get(outInterface).getVrfName();
          String recvNode = edge.getNode2();
          String recvInterface = edge.getInt2();
          output
              .computeIfAbsent(hostname, n -> new HashMap<>())
              .computeIfAbsent(vrf, n -> new HashMap<>())
              .computeIfAbsent(outInterface, n -> new HashMap<>())
              .computeIfAbsent(recvNode, n -> new HashMap<>())
              .put(
                  recvInterface,
                  ipSpace.accept(
                      new IpSpaceBooleanExprTransformer(
                          _namedIpSpaces.get(hostname), Field.DST_IP)));
        });

    // freeze
    return toImmutableMap(
        output,
        Entry::getKey, /* node */
        outputByHostnameEntry ->
            toImmutableMap(
                outputByHostnameEntry.getValue(),
                Entry::getKey, /* vrf */
                outputByVrfEntry ->
                    toImmutableMap(
                        outputByVrfEntry.getValue(),
                        Entry::getKey /* outInterface */,
                        outputByOutInterfaceEntry ->
                            toImmutableMap(
                                outputByOutInterfaceEntry.getValue(),
                                Entry::getKey /* recvNode */,
                                outputByRecvNodeEntry ->
                                    toImmutableMap(
                                        outputByRecvNodeEntry.getValue(),
                                        Entry::getKey /* recvInterface */,
                                        Entry::getValue)))));
  }

  private Map<String, Map<String, IpAccessList>> computeEnabledAcls(
      @Nullable Map<String, Set<String>> enabledAcls) {
    return _configurations
        .entrySet()
        .stream()
        .filter(e -> !_disabledNodes.contains(e.getKey()))
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                e -> {
                  String h = e.getKey();
                  IpAccessListSpecializer ipAccessListSpecializer =
                      _ipAccessListSpecializers.get(h);
                  return e.getValue()
                      .getIpAccessLists()
                      .entrySet()
                      .stream()
                      .filter(
                          e2 ->
                              enabledAcls == null
                                  || enabledAcls.getOrDefault(h, emptySet()).contains(e2.getKey()))
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Entry::getKey,
                              entry ->
                                  ipAccessListSpecializer == null
                                      ? entry.getValue()
                                      : ipAccessListSpecializer.specialize(entry.getValue())));
                }));
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

  private Map<String, Set<String>> computeEnabledInterfaces() {
    return toImmutableMap(
        _enabledInterfacesByNodeVrf,
        Entry::getKey,
        enabledInterfacesByNodeVrfEntry ->
            enabledInterfacesByNodeVrfEntry
                .getValue()
                .entrySet()
                .stream()
                .flatMap(
                    enabledInterfacesByVrfEntry -> enabledInterfacesByVrfEntry.getValue().stream())
                .collect(ImmutableSet.toImmutableSet()));
  }

  private Map<String, Map<String, Set<String>>> computeEnabledInterfacesByNodeVrf() {
    return toImmutableMap(
        _enabledVrfs,
        Entry::getKey,
        enabledVrfsEntry -> {
          String hostname = enabledVrfsEntry.getKey();
          Set<String> disabledInterfaces = _disabledInterfaces.get(hostname);
          Configuration c = _configurations.get(hostname);
          return toImmutableMap(
              enabledVrfsEntry.getValue(),
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
                                  || !disabledInterfaces.contains(interfaceEntry.getKey()))
                      .filter(interfaceEntry -> interfaceEntry.getValue().getActive())
                      .filter(interfaceEntry -> !interfaceEntry.getValue().getBlacklisted())
                      .map(Entry::getKey)
                      .collect(ImmutableSet.toImmutableSet()));
        });
  }

  private Set<String> computeEnabledNodes() {
    return ImmutableSet.copyOf(Sets.difference(_configurations.keySet(), _disabledNodes));
  }

  private Map<String, Set<String>> computeEnabledVrfs() {
    return toImmutableMap(
        _enabledNodes,
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
        });
  }

  private Map<String, Map<String, String>> computeIncomingAcls() {
    return toImmutableMap(
        _enabledInterfaces,
        Entry::getKey,
        enabledInterfacesEntry -> {
          Configuration c = _configurations.get(enabledInterfacesEntry.getKey());
          return enabledInterfacesEntry
              .getValue()
              .stream()
              .filter(ifaceName -> c.getInterfaces().get(ifaceName).getIncomingFilterName() != null)
              .collect(
                  ImmutableMap.toImmutableMap(
                      Function.identity(),
                      ifaceName -> c.getInterfaces().get(ifaceName).getIncomingFilterName()));
        });
  }

  private Map<String, Set<Ip>> computeIpsByHostname() {
    Map<String, Set<Interface>> enabledInterfaces =
        toImmutableMap(
            _enabledInterfaces,
            Entry::getKey,
            entry -> {
              String hostname = entry.getKey();
              Set<String> enabledInterfaceNames = entry.getValue();
              Map<String, Interface> nodeInterfaces = _configurations.get(hostname).getInterfaces();
              return enabledInterfaceNames
                  .stream()
                  .map(nodeInterfaces::get)
                  .collect(ImmutableSet.toImmutableSet());
            });

    Map<Ip, Map<String, Set<String>>> ipInterfaceOwners =
        computeIpInterfaceOwners(enabledInterfaces, true);

    Map<String, Set<Ip>> map = new HashMap<>();
    /*
     * ipOwners may not contain all nodes (i.e. a node may not own any IPs),
     * so first initialize to make sure there is an entry for each node.
     */
    _enabledInterfaces.keySet().forEach(node -> map.put(node, new HashSet<>()));
    ipInterfaceOwners.forEach(
        (ip, owners) -> {
          for (String owner : owners.keySet()) {
            if (_specializationIpSpace.containsIp(ip, _namedIpSpaces.get(owner))) {
              map.get(owner).add(ip);
            }
          }
        });
    // freeze
    return toImmutableMap(map, Entry::getKey, e -> ImmutableSet.copyOf(e.getValue()));
  }

  private Map<String, Map<String, Set<Ip>>> computeIpsByNodeVrf() {
    Map<String, Set<Interface>> enabledInterfaces =
        toImmutableMap(
            _enabledInterfaces,
            Entry::getKey,
            enabledInterfacesEntry ->
                ImmutableSet.copyOf(
                    _configurations.get(enabledInterfacesEntry.getKey()).getInterfaces().values()));

    Map<Ip, Map<String, Set<String>>> ipOwners = computeIpVrfOwners(true, enabledInterfaces);
    Map<String, Map<String, Set<Ip>>> ipsByNodeByVrf = new HashMap<>();
    /*
     * ipOwners may not contain all nodes (i.e. a node may not own any IPs),
     * so first initialize to make sure there is an entry for each node and vrf
     */
    _enabledInterfacesByNodeVrf.forEach(
        (node, ifacesByVrf) -> {
          Map<String, Set<Ip>> ipsByVrf =
              ipsByNodeByVrf.computeIfAbsent(node, k -> new HashMap<>());
          ifacesByVrf.keySet().forEach(vrf -> ipsByVrf.put(vrf, new HashSet<>()));
        });

    ipOwners.forEach(
        (ip, nodeVrfOwners) ->
            nodeVrfOwners.forEach(
                (node, vrfs) -> {
                  if (_specializationIpSpace.containsIp(ip, _namedIpSpaces.get(node))) {
                    vrfs.forEach(vrf -> ipsByNodeByVrf.get(node).get(vrf).add(ip));
                  }
                }));
    // freeze
    return toImmutableMap(
        ipsByNodeByVrf,
        Entry::getKey,
        e1 ->
            toImmutableMap(e1.getValue(), Entry::getKey, e2 -> ImmutableSet.copyOf(e2.getValue())));
  }

  private Map<String, Map<String, Map<String, BooleanExpr>>> computeNeighborUnreachable(
      Map<String, Map<String, Map<String, IpSpace>>> neighborUnreachable) {
    return toImmutableMap(
        neighborUnreachable,
        Entry::getKey /* hostname */,
        neighborUnreachableByHostnameEntry -> {
          String hostname = neighborUnreachableByHostnameEntry.getKey();
          IpSpaceSpecializer specializer = _ipSpaceSpecializers.get(hostname);
          return toImmutableMap(
              neighborUnreachableByHostnameEntry.getValue(),
              Entry::getKey /* vrf */,
              neighborUnreachableByVrfEntry ->
                  toImmutableMap(
                      neighborUnreachableByVrfEntry.getValue(),
                      Entry::getKey /* interface */,
                      neighborUnreachableByOutInterfaceEntry ->
                          new IpSpaceMatchExpr(
                                  specializer.specialize(
                                      neighborUnreachableByOutInterfaceEntry.getValue()),
                                  _namedIpSpaces.get(hostname),
                                  Field.DST_IP)
                              .getExpr()));
        });
  }

  private Map<String, Map<String, BooleanExpr>> computeNullRoutedIps(
      Map<String, Map<String, IpSpace>> nullRoutedIps) {
    return toImmutableMap(
        nullRoutedIps,
        Entry::getKey /* hostname */,
        nullRoutedIpsByHostnameEntry -> {
          String hostname = nullRoutedIpsByHostnameEntry.getKey();
          IpSpaceSpecializer specializer = _ipSpaceSpecializers.get(hostname);
          return toImmutableMap(
              nullRoutedIpsByHostnameEntry.getValue(),
              Entry::getKey /* vrf */,
              nullRoutedIpsByVrfEntry ->
                  new IpSpaceMatchExpr(
                      specializer.specialize(nullRoutedIpsByVrfEntry.getValue()),
                      _namedIpSpaces.get(hostname),
                      Field.DST_IP));
        });
  }

  private Map<String, Map<String, String>> computeOutgoingAcls() {
    return toImmutableMap(
        _enabledInterfaces,
        Entry::getKey,
        enabledInterfacesEntry -> {
          Configuration c = _configurations.get(enabledInterfacesEntry.getKey());
          return enabledInterfacesEntry
              .getValue()
              .stream()
              .filter(ifaceName -> c.getInterfaces().get(ifaceName).getOutgoingFilterName() != null)
              .collect(
                  ImmutableMap.toImmutableMap(
                      Function.identity(),
                      ifaceName -> c.getInterfaces().get(ifaceName).getOutgoingFilterName()));
        });
  }

  private Map<String, Map<String, BooleanExpr>> computeRoutableIps(
      Map<String, Map<String, IpSpace>> routableIps) {
    return toImmutableMap(
        routableIps,
        Entry::getKey /* hostname */,
        routableIpsByHostnameEntry -> {
          String hostname = routableIpsByHostnameEntry.getKey();
          IpSpaceSpecializer specializer = _ipSpaceSpecializers.get(hostname);
          return toImmutableMap(
              routableIpsByHostnameEntry.getValue(),
              Entry::getKey /* vrf */,
              routableIpsByVrfEntry ->
                  new IpSpaceMatchExpr(
                      specializer.specialize(routableIpsByVrfEntry.getValue()),
                      _namedIpSpaces.get(hostname),
                      Field.DST_IP));
        });
  }

  private Map<String, Map<String, List<Entry<AclPermit, BooleanExpr>>>> computeSourceNats() {
    return toImmutableMap(
        _topologyInterfaces,
        Entry::getKey,
        topologyInterfacesEntryByHostname -> {
          String hostname = topologyInterfacesEntryByHostname.getKey();
          Set<String> ifaces = topologyInterfacesEntryByHostname.getValue();
          Configuration c = _configurations.get(hostname);
          return toImmutableMap(
              ifaces,
              Function.identity(),
              ifaceName ->
                  c.getInterfaces()
                      .get(ifaceName)
                      .getSourceNats()
                      .stream()
                      .map(
                          sourceNat -> {
                            IpAccessList acl = sourceNat.getAcl();
                            AclPermit preconditionPreTransformationState =
                                acl == null ? null : new AclPermit(hostname, acl.getName());
                            BooleanExpr transformationConstraint =
                                new RangeMatchExpr(
                                    new TransformedVarIntExpr(Field.SRC_IP),
                                    Field.SRC_IP.getSize(),
                                    ImmutableSet.of(
                                        Range.closed(
                                            sourceNat.getPoolIpFirst().asLong(),
                                            sourceNat.getPoolIpLast().asLong())));
                            return Maps.immutableEntry(
                                preconditionPreTransformationState, transformationConstraint);
                          })
                      .collect(ImmutableList.toImmutableList()));
        });
  }

  private Map<String, Set<String>> computeTopologyInterfaces() {
    Map<String, Set<String>> topologyEdges = new HashMap<>();
    _enabledEdges.forEach(
        enabledEdge ->
            topologyEdges
                .computeIfAbsent(enabledEdge.getNode1(), n -> new HashSet<>())
                .add(enabledEdge.getInt1()));
    // freeze
    return toImmutableMap(topologyEdges, Entry::getKey, e -> ImmutableSet.copyOf(e.getValue()));
  }

  @Override
  public Map<String, Map<String, List<LineAction>>> getAclActions() {
    return _aclActions;
  }

  /**
   * Mapping: hostname -> aclName -> lineNumber -> lineConditions <br>
   * lineConditions is a boolean expression representing the constraints on a header necessary for
   * that line to be matched.
   */
  @Override
  public Map<String, Map<String, List<BooleanExpr>>> getAclConditions() {
    return _aclConditions;
  }

  @Override
  public Map<String, Map<String, Map<String, Map<String, Map<String, BooleanExpr>>>>>
      getArpTrueEdge() {
    return _arpTrueEdge;
  }

  @Override
  public Set<Edge> getEnabledEdges() {
    return _enabledEdges;
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
  public Map<String, Map<String, String>> getIncomingAcls() {
    return _incomingAcls;
  }

  @Override
  public Map<IngressLocation, BooleanExpr> getSrcIpConstraints() {
    return _srcIpConstraints;
  }

  @Override
  public Map<String, Set<Ip>> getIpsByHostname() {
    return _ipsByHostname;
  }

  @Override
  public Map<String, Map<String, Set<Ip>>> getIpsByNodeVrf() {
    return _ipsByNodeVrf;
  }

  @Override
  public Map<String, Map<String, IpSpace>> getNamedIpSpaces() {
    return _namedIpSpaces;
  }

  @Override
  public Map<String, Map<String, Map<String, BooleanExpr>>> getNeighborUnreachable() {
    return _neighborUnreachable;
  }

  @Override
  public int getNodeInterfaceId(String node, String iface) {
    return _nodeInterfaces.get(node).indexOf(iface) + 1;
  }

  @Override
  public Map<String, Map<String, BooleanExpr>> getNullRoutedIps() {
    return _nullRoutedIps;
  }

  @Override
  public Map<String, Map<String, String>> getOutgoingAcls() {
    return _outgoingAcls;
  }

  @Override
  public Map<String, Map<String, BooleanExpr>> getRoutableIps() {
    return _routableIps;
  }

  @Override
  public boolean getSimplify() {
    return _simplify;
  }

  @Override
  public Map<String, Map<String, List<Entry<AclPermit, BooleanExpr>>>> getSourceNats() {
    return _sourceNats;
  }

  @Override
  public Set<String> getTransitNodes() {
    return _transitNodes;
  }

  @Override
  public Map<String, Set<String>> getTraversableInterfaces() {
    return _topologyInterfaces;
  }

  @Override
  public Set<Type> getVectorizedParameters() {
    return _vectorizedParameters;
  }

  @Override
  public Map<String, List<String>> getNodeInterfaces() {
    return _nodeInterfaces;
  }

  @Override
  public Field getSourceInterfaceField() {
    return _sourceInterfaceField;
  }

  @Override
  public Map<String, Map<String, IntExpr>> getSourceInterfaceFieldValues() {
    return _sourceInterfaceFieldValues;
  }

  @Override
  public Set<String> getNodesWithSrcInterfaceConstraints() {
    return _nodesWithSrcInterfaceConstraints;
  }

  @Override
  public Set<String> getNonTransitNodes() {
    return _nonTransitNodes;
  }

  @Override
  public boolean isDataPlane() {
    return _dataPlane;
  }
}
