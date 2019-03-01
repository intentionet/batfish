package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Network;
import com.google.common.graph.ValueGraph;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NeighborType;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.RipNeighbor;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.RoleEdge;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.VerboseEdge;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.bgp.BgpTopologyUtils;
import org.batfish.datamodel.collections.IpEdge;
import org.batfish.datamodel.collections.VerboseBgpEdge;
import org.batfish.datamodel.collections.VerboseEigrpEdge;
import org.batfish.datamodel.collections.VerboseOspfEdge;
import org.batfish.datamodel.collections.VerboseRipEdge;
import org.batfish.datamodel.eigrp.EigrpEdge;
import org.batfish.datamodel.eigrp.EigrpInterface;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.ospf.OspfNeighbor;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.OspfTopologyUtils;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.role.NodeRoleDimension;

@AutoService(Plugin.class)
public class NeighborsQuestionPlugin extends QuestionPlugin {

  private static final Comparator<VerboseBgpEdge> VERBOSE_BGP_EDGE_COMPARATOR =
      Comparator.nullsFirst(
          Comparator.comparing(VerboseBgpEdge::getEdgeSummary)
              .thenComparing(VerboseBgpEdge::getSession1Id)
              .thenComparing(VerboseBgpEdge::getSession2Id));

  public enum EdgeStyle {
    ROLE("role"),
    SUMMARY("summary"),
    VERBOSE("verbose");

    private static final Map<String, EdgeStyle> _map = buildMap();

    private static Map<String, EdgeStyle> buildMap() {
      ImmutableMap.Builder<String, EdgeStyle> map = ImmutableMap.builder();
      for (EdgeStyle value : EdgeStyle.values()) {
        String name = value._name;
        map.put(name, value);
      }
      return map.build();
    }

    @JsonCreator
    public static EdgeStyle fromName(String name) {
      EdgeStyle instance = _map.get(name.toLowerCase());
      if (instance == null) {
        throw new BatfishException(
            "No " + EdgeStyle.class.getSimpleName() + " with name: '" + name + "'");
      }
      return instance;
    }

    private final String _name;

    EdgeStyle(String name) {
      _name = name;
    }

    @JsonValue
    public String edgeStyleName() {
      return _name;
    }
  }

  public static class NeighborsAnswerElement extends AnswerElement {

    private static final String PROP_EBGP_NEIGHBORS = "ebgpNeighbors";

    private static final String PROP_IBGP_NEIGHBORS = "ibgpNeighbors";

    private static final String PROP_LAYER1_NEIGHBORS = "layer1Neighbors";

    private static final String PROP_LAYER2_NEIGHBORS = "layer2Neighbors";

    private static final String PROP_LAYER3_NEIGHBORS = "layer3Neighbors";

    private static final String PROP_EIGRP_NEIGHBORS = "eigrpNeighbors";

    private static final String PROP_OSPF_NEIGHBORS = "ospfNeighbors";

    private static final String PROP_RIP_NEIGHBORS = "ripNeighbors";

    private static final String PROP_ROLE_EBGP_NEIGHBORS = "roleEbgpNeighbors";

    private static final String PROP_ROLE_EIGRP_NEIGHBORS = "roleEigrpNeighbors";

    private static final String PROP_ROLE_IBGP_NEIGHBORS = "roleIbgpNeighbors";

    private static final String PROP_ROLE_LAYER3_NEIGHBORS = "roleLayer3Neighbors";

    private static final String PROP_ROLE_OSPF_NEIGHBORS = "roleOspfNeighbors";

    private static final String PROP_ROLE_RIP_NEIGHBORS = "roleRipNeighbors";

    private static final String PROP_VERBOSE_EBGP_NEIGHBORS = "verboseEbgpNeighbors";

    private static final String PROP_VERBOSE_EIGRP_NEIGHBORS = "verboseEigrpNeighbors";

    private static final String PROP_VERBOSE_IBGP_NEIGHBORS = "verboseIbgpNeighbors";

    private static final String PROP_VERBOSE_LAYER3_NEIGHBORS = "verboseLayer3Neighbors";

    private static final String PROP_VERBOSE_OSPF_NEIGHBORS = "verboseOspfNeighbors";

    private static final String PROP_VERBOSE_RIP_NEIGHBORS = "verboseRipNeighbors";

    private SortedSet<IpEdge> _ebgpNeighbors;

    private SortedSet<IpEdge> _ibgpNeighbors;

    private Layer1Topology _layer1Neighbors;

    private Layer2Topology _layer2Neighbors;

    private SortedSet<Edge> _layer3Neighbors;

    private SortedSet<IpEdge> _eigrpNeighbors;

    private SortedSet<IpEdge> _ospfNeighbors;

    private SortedSet<IpEdge> _ripNeighbors;

    private SortedSet<RoleEdge> _roleEbgpNeighbors;

    private SortedSet<RoleEdge> _roleEigrpNeighbors;

    private SortedSet<RoleEdge> _roleIbgpNeighbors;

    private SortedSet<RoleEdge> _roleLayer3Neighbors;

    private SortedSet<RoleEdge> _roleOspfNeighbors;

    private SortedSet<RoleEdge> _roleRipNeighbors;

    private SortedSet<VerboseBgpEdge> _verboseEbgpNeighbors;

    private SortedSet<VerboseEigrpEdge> _verboseEigrpNeighbors;

    private SortedSet<VerboseBgpEdge> _verboseIbgpNeighbors;

    private SortedSet<VerboseEdge> _verboseLayer3Neighbors;

    private SortedSet<VerboseOspfEdge> _verboseOspfNeighbors;

    private SortedSet<VerboseRipEdge> _verboseRipNeighbors;

    public void addLayer3Edge(Edge edge) {
      _layer3Neighbors.add(edge);
    }

    @JsonProperty(PROP_EBGP_NEIGHBORS)
    public SortedSet<IpEdge> getEbgpNeighbors() {
      return _ebgpNeighbors;
    }

    @JsonProperty(PROP_EIGRP_NEIGHBORS)
    public SortedSet<IpEdge> getEigrpNeighbors() {
      return _eigrpNeighbors;
    }

    @JsonProperty(PROP_IBGP_NEIGHBORS)
    public SortedSet<IpEdge> getIbgpNeighbors() {
      return _ibgpNeighbors;
    }

    @JsonProperty(PROP_LAYER1_NEIGHBORS)
    public Layer1Topology getLayer1Neighbors() {
      return _layer1Neighbors;
    }

    @JsonProperty(PROP_LAYER2_NEIGHBORS)
    public Layer2Topology getLayer2Neighbors() {
      return _layer2Neighbors;
    }

    @JsonProperty(PROP_LAYER3_NEIGHBORS)
    public SortedSet<Edge> getLayer3Neighbors() {
      return _layer3Neighbors;
    }

    @JsonProperty(PROP_OSPF_NEIGHBORS)
    public SortedSet<IpEdge> getOspfNeighbors() {
      return _ospfNeighbors;
    }

    @JsonProperty(PROP_RIP_NEIGHBORS)
    public SortedSet<IpEdge> getRipNeighbors() {
      return _ripNeighbors;
    }

    @JsonProperty(PROP_ROLE_EBGP_NEIGHBORS)
    public SortedSet<RoleEdge> getRoleEbgpNeighbors() {
      return _roleEbgpNeighbors;
    }

    @JsonProperty(PROP_ROLE_EIGRP_NEIGHBORS)
    public SortedSet<RoleEdge> getRoleEigrpNeighbors() {
      return _roleEigrpNeighbors;
    }

    @JsonProperty(PROP_ROLE_IBGP_NEIGHBORS)
    public SortedSet<RoleEdge> getRoleIbgpNeighbors() {
      return _roleIbgpNeighbors;
    }

    @JsonProperty(PROP_ROLE_LAYER3_NEIGHBORS)
    public SortedSet<RoleEdge> getRoleLayer3Neighbors() {
      return _roleLayer3Neighbors;
    }

    @JsonProperty(PROP_ROLE_OSPF_NEIGHBORS)
    public SortedSet<RoleEdge> getRoleOspfNeighbors() {
      return _roleOspfNeighbors;
    }

    @JsonProperty(PROP_ROLE_RIP_NEIGHBORS)
    public SortedSet<RoleEdge> getRoleRipNeighbors() {
      return _roleRipNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_EBGP_NEIGHBORS)
    public SortedSet<VerboseBgpEdge> getVerboseEbgpNeighbors() {
      return _verboseEbgpNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_EIGRP_NEIGHBORS)
    public SortedSet<VerboseEigrpEdge> getVerboseEigrpNeighbors() {
      return _verboseEigrpNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_IBGP_NEIGHBORS)
    public SortedSet<VerboseBgpEdge> getVerboseIbgpNeighbors() {
      return _verboseIbgpNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_LAYER3_NEIGHBORS)
    public SortedSet<VerboseEdge> getVerboseLayer3Neighbors() {
      return _verboseLayer3Neighbors;
    }

    @JsonProperty(PROP_VERBOSE_OSPF_NEIGHBORS)
    public SortedSet<VerboseOspfEdge> getVerboseOspfNeighbors() {
      return _verboseOspfNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_RIP_NEIGHBORS)
    public SortedSet<VerboseRipEdge> getVerboseRipNeighbors() {
      return _verboseRipNeighbors;
    }

    public void initEbgpNeighbors() {
      _ebgpNeighbors = new TreeSet<>();
    }

    public void initEigrpNeighbors() {
      _eigrpNeighbors = new TreeSet<>();
    }

    public void initIbgpNeighbors() {
      _ibgpNeighbors = new TreeSet<>();
    }

    public void initLayer3Neighbors() {
      _layer3Neighbors = new TreeSet<>();
    }

    public void initOspfNeighbors() {
      _ospfNeighbors = new TreeSet<>();
    }

    public void initRipNeighbors() {
      _ripNeighbors = new TreeSet<>();
    }

    public void initVerboseLayer3Neighbors() {
      _verboseLayer3Neighbors = new TreeSet<>();
    }

    @Override
    public String prettyPrint() {
      StringBuilder sb = new StringBuilder("Results for neighbors\n");

      if (_layer3Neighbors != null) {
        sb.append("  Layer 3 neighbors\n");
        for (Edge edge : _layer3Neighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_verboseLayer3Neighbors != null) {
        sb.append("  Layer 3 neighbors\n");
        for (VerboseEdge edge : _verboseLayer3Neighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_roleLayer3Neighbors != null) {
        sb.append("  Layer 3 neighbors\n");
        for (RoleEdge edge : _roleLayer3Neighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_ebgpNeighbors != null) {
        sb.append("  eBGP Neighbors\n");
        for (IpEdge ipEdge : _ebgpNeighbors) {
          sb.append("    " + ipEdge + "\n");
        }
      }

      if (_verboseEbgpNeighbors != null) {
        sb.append("  eBGP neighbors\n");
        for (VerboseBgpEdge edge : _verboseEbgpNeighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_roleEbgpNeighbors != null) {
        sb.append("  eBGP neighbors\n");
        for (RoleEdge edge : _roleEbgpNeighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_eigrpNeighbors != null) {
        sb.append("  EIGRP Neighbors\n");
        for (IpEdge ipEdge : _eigrpNeighbors) {
          sb.append("    " + ipEdge + "\n");
        }
      }

      if (_verboseEigrpNeighbors != null) {
        sb.append("  EIGRP neighbors\n");
        for (VerboseEigrpEdge edge : _verboseEigrpNeighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_roleEigrpNeighbors != null) {
        sb.append("  EIGRP neighbors\n");
        for (RoleEdge edge : _roleEigrpNeighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_ibgpNeighbors != null) {
        sb.append("  iBGP Neighbors\n");
        for (IpEdge ipEdge : _ibgpNeighbors) {
          sb.append("    " + ipEdge + "\n");
        }
      }

      if (_verboseIbgpNeighbors != null) {
        sb.append("  iBGP neighbors\n");
        for (VerboseBgpEdge edge : _verboseIbgpNeighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_roleIbgpNeighbors != null) {
        sb.append("  iBGP neighbors\n");
        for (RoleEdge edge : _roleIbgpNeighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_ospfNeighbors != null) {
        sb.append("  OSPF Neighbors\n");
        for (IpEdge ipEdge : _ospfNeighbors) {
          sb.append("    " + ipEdge + "\n");
        }
      }

      if (_verboseOspfNeighbors != null) {
        sb.append("  OSPF neighbors\n");
        for (VerboseOspfEdge edge : _verboseOspfNeighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_roleOspfNeighbors != null) {
        sb.append("  OSPF neighbors\n");
        for (RoleEdge edge : _roleOspfNeighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      return sb.toString();
    }

    @JsonProperty(PROP_EBGP_NEIGHBORS)
    public void setEbgpNeighbors(SortedSet<IpEdge> ebgpNeighbors) {
      _ebgpNeighbors = ebgpNeighbors;
    }

    @JsonProperty(PROP_EIGRP_NEIGHBORS)
    public void setEigrpNeighbors(SortedSet<IpEdge> eigrpNeighbors) {
      _eigrpNeighbors = eigrpNeighbors;
    }

    @JsonProperty(PROP_IBGP_NEIGHBORS)
    public void setIbgpNeighbors(SortedSet<IpEdge> ibgpNeighbors) {
      _ibgpNeighbors = ibgpNeighbors;
    }

    @JsonProperty(PROP_LAYER1_NEIGHBORS)
    public void setLayer1Neighbors(Layer1Topology layer1Neighbors) {
      _layer1Neighbors = layer1Neighbors;
    }

    @JsonProperty(PROP_LAYER2_NEIGHBORS)
    public void setLayer2Neighbors(Layer2Topology layer2Neighbors) {
      _layer2Neighbors = layer2Neighbors;
    }

    @JsonProperty(PROP_LAYER3_NEIGHBORS)
    public void setLayer3Neighbors(SortedSet<Edge> layer3Neighbors) {
      _layer3Neighbors = layer3Neighbors;
    }

    @JsonProperty(PROP_OSPF_NEIGHBORS)
    public void setOspfNeighbors(SortedSet<IpEdge> ospfNeighbors) {
      _ospfNeighbors = ospfNeighbors;
    }

    @JsonProperty(PROP_RIP_NEIGHBORS)
    public void setRipNeighbors(SortedSet<IpEdge> ripNeighbors) {
      _ripNeighbors = ripNeighbors;
    }

    @JsonProperty(PROP_ROLE_EBGP_NEIGHBORS)
    public void setRoleEbgpNeighbors(SortedSet<RoleEdge> roleEbgpNeighbors) {
      _roleEbgpNeighbors = roleEbgpNeighbors;
    }

    @JsonProperty(PROP_ROLE_EIGRP_NEIGHBORS)
    public void setRoleEigrpNeighbors(SortedSet<RoleEdge> roleEigrpNeighbors) {
      _roleEigrpNeighbors = roleEigrpNeighbors;
    }

    @JsonProperty(PROP_ROLE_IBGP_NEIGHBORS)
    public void setRoleIbgpNeighbors(SortedSet<RoleEdge> roleIbgpNeighbors) {
      _roleIbgpNeighbors = roleIbgpNeighbors;
    }

    @JsonProperty(PROP_ROLE_LAYER3_NEIGHBORS)
    public void setRoleLayer3Neighbors(SortedSet<RoleEdge> roleLayer3Neighbors) {
      _roleLayer3Neighbors = roleLayer3Neighbors;
    }

    @JsonProperty(PROP_ROLE_OSPF_NEIGHBORS)
    public void setRoleOspfNeighbors(SortedSet<RoleEdge> roleOspfNeighbors) {
      _roleOspfNeighbors = roleOspfNeighbors;
    }

    @JsonProperty(PROP_ROLE_RIP_NEIGHBORS)
    public void setRoleRipNeighbors(SortedSet<RoleEdge> roleRipNeighbors) {
      _roleRipNeighbors = roleRipNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_EBGP_NEIGHBORS)
    public void setVerboseEbgpNeighbors(Set<VerboseBgpEdge> verboseEbgpNeighbors) {
      _verboseEbgpNeighbors = new TreeSet<>(VERBOSE_BGP_EDGE_COMPARATOR);
      _verboseEbgpNeighbors.addAll(verboseEbgpNeighbors);
    }

    @JsonProperty(PROP_VERBOSE_EIGRP_NEIGHBORS)
    public void setVerboseEigrpNeighbors(SortedSet<VerboseEigrpEdge> verboseEigrpNeighbors) {
      _verboseEigrpNeighbors = verboseEigrpNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_IBGP_NEIGHBORS)
    public void setVerboseIbgpNeighbors(Set<VerboseBgpEdge> verboseIbgpNeighbors) {
      _verboseIbgpNeighbors = new TreeSet<>(VERBOSE_BGP_EDGE_COMPARATOR);
      _verboseIbgpNeighbors.addAll(verboseIbgpNeighbors);
    }

    @JsonProperty(PROP_VERBOSE_LAYER3_NEIGHBORS)
    public void setVerboseLayer3Neighbors(SortedSet<VerboseEdge> verboseLayer3Neighbors) {
      _verboseLayer3Neighbors = verboseLayer3Neighbors;
    }

    @JsonProperty(PROP_VERBOSE_OSPF_NEIGHBORS)
    public void setVerboseOspfNeighbors(SortedSet<VerboseOspfEdge> verboseOspfNeighbors) {
      _verboseOspfNeighbors = verboseOspfNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_RIP_NEIGHBORS)
    public void setVerboseRipNeighbors(SortedSet<VerboseRipEdge> verboseRipNeighbors) {
      _verboseRipNeighbors = verboseRipNeighbors;
    }
  }

  public static class NeighborsAnswerer extends Answerer {

    private ValueGraph<BgpPeerConfigId, BgpSessionProperties> _bgpTopology;

    private Network<EigrpInterface, EigrpEdge> _eigrpTopology;

    private SortedMap<String, SortedSet<String>> _nodeRolesMap;

    private boolean _remoteBgpNeighborsInitialized;

    private boolean _remoteEigrpNeighborsInitialized;

    private boolean _remoteOspfNeighborsInitialized;

    Topology _topology;

    private boolean _remoteRipNeighborsInitialized;

    public NeighborsAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public NeighborsAnswerElement answer() {
      NeighborsQuestion question = (NeighborsQuestion) _question;
      NeighborsAnswerElement answerElement = new NeighborsAnswerElement();

      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      Set<String> includeNodes1 = question.getNodes().getMatchingNodes(_batfish);
      Set<String> includeNodes2 = question.getRemoteNodes().getMatchingNodes(_batfish);

      if (question.getStyle() == EdgeStyle.ROLE) {
        NodeRoleDimension roleDimension =
            _batfish
                .getNodeRoleDimension(question.getRoleDimension())
                .orElseThrow(
                    () ->
                        new BatfishException(
                            "No role dimension found for " + question.getRoleDimension()));
        _nodeRolesMap = roleDimension.createNodeRolesMap(configurations.keySet());
      }

      if (question.getNeighborTypes().contains(NeighborType.EIGRP)) {
        NetworkConfigurations nc = NetworkConfigurations.of(configurations);
        SortedSet<VerboseEigrpEdge> vedges = new TreeSet<>();
        initTopology();
        initRemoteEigrpNeighbors(configurations, _topology);
        for (Configuration c : configurations.values()) {
          String hostname = c.getHostname();
          for (Vrf vrf : c.getVrfs().values()) {
            vedges.addAll(
                vrf.getInterfaceNames().stream()
                    .map(ifaceName -> new EigrpInterface(hostname, ifaceName, vrf.getName()))
                    .filter(_eigrpTopology.nodes()::contains)
                    .flatMap(n -> _eigrpTopology.inEdges(n).stream())
                    .map(edge -> new VerboseEigrpEdge(edge, edge.toIpEdge(nc)))
                    .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
          }
        }

        switch (question.getStyle()) {
          case SUMMARY:
            answerElement.initEigrpNeighbors();
            for (VerboseEigrpEdge vedge : vedges) {
              answerElement.getEigrpNeighbors().add(vedge.getEdgeSummary());
            }
            break;
          case VERBOSE:
            answerElement.setVerboseEigrpNeighbors(vedges);
            break;
          case ROLE:
            SortedSet<RoleEdge> redges = new TreeSet<>();
            for (VerboseEigrpEdge vedge : vedges) {
              SortedSet<String> roles1 =
                  _nodeRolesMap.getOrDefault(vedge.getEdgeSummary().getNode1(), new TreeSet<>());
              SortedSet<String> roles2 =
                  _nodeRolesMap.getOrDefault(vedge.getEdgeSummary().getNode2(), new TreeSet<>());
              for (String r1 : roles1) {
                for (String r2 : roles2) {
                  redges.add(new RoleEdge(r1, r2));
                }
              }
            }
            answerElement.setRoleEigrpNeighbors(redges);
            break;
          default:
            throw new BatfishException(
                "Unsupported " + EdgeStyle.class.getCanonicalName() + ": " + question.getStyle());
        }
      }

      if (question.getNeighborTypes().contains(NeighborType.OSPF)) {
        SortedSet<VerboseOspfEdge> vedges = new TreeSet<>();
        initTopology();
        initRemoteOspfNeighbors(configurations, _topology);
        for (Configuration c : configurations.values()) {
          String hostname = c.getHostname();
          for (Vrf vrf : c.getVrfs().values()) {
            OspfProcess proc = vrf.getOspfProcess();
            if (proc != null) {
              for (OspfNeighbor ospfNeighbor : proc.getOspfNeighbors().values()) {
                OspfNeighbor remoteOspfNeighbor = ospfNeighbor.getRemoteOspfNeighbor();
                if (remoteOspfNeighbor != null) {
                  Configuration remoteHost = remoteOspfNeighbor.getOwner();
                  String remoteHostname = remoteHost.getHostname();
                  if (includeNodes1.contains(hostname) && includeNodes2.contains(remoteHostname)) {
                    Ip localIp = ospfNeighbor.getLocalIp();
                    Ip remoteIp = remoteOspfNeighbor.getLocalIp();
                    IpEdge edge = new IpEdge(hostname, localIp, remoteHostname, remoteIp);
                    vedges.add(new VerboseOspfEdge(ospfNeighbor, remoteOspfNeighbor, edge));
                  }
                }
              }
            }
          }
        }

        switch (question.getStyle()) {
          case SUMMARY:
            answerElement.initOspfNeighbors();
            for (VerboseOspfEdge vedge : vedges) {
              answerElement.getOspfNeighbors().add(vedge.getEdgeSummary());
            }
            break;
          case VERBOSE:
            answerElement.setVerboseOspfNeighbors(vedges);
            break;
          case ROLE:
            SortedSet<RoleEdge> redges = new TreeSet<>();
            for (VerboseOspfEdge vedge : vedges) {
              SortedSet<String> roles1 =
                  _nodeRolesMap.getOrDefault(vedge.getEdgeSummary().getNode1(), new TreeSet<>());
              SortedSet<String> roles2 =
                  _nodeRolesMap.getOrDefault(vedge.getEdgeSummary().getNode2(), new TreeSet<>());
              for (String r1 : roles1) {
                for (String r2 : roles2) {
                  redges.add(new RoleEdge(r1, r2));
                }
              }
            }
            answerElement.setRoleOspfNeighbors(redges);
            break;
          default:
            throw new BatfishException(
                "Unsupported " + EdgeStyle.class.getCanonicalName() + ": " + question.getStyle());
        }
      }

      if (question.getNeighborTypes().contains(NeighborType.RIP)) {
        SortedSet<VerboseRipEdge> vedges = new TreeSet<>();
        initTopology();
        initRemoteRipNeighbors(_batfish, configurations, _topology);
        for (Configuration c : configurations.values()) {
          String hostname = c.getHostname();
          for (Vrf vrf : c.getVrfs().values()) {
            RipProcess proc = vrf.getRipProcess();
            if (proc != null) {
              for (RipNeighbor ripNeighbor : proc.getRipNeighbors().values()) {
                RipNeighbor remoteRipNeighbor = ripNeighbor.getRemoteRipNeighbor();
                if (remoteRipNeighbor != null) {
                  Configuration remoteHost = remoteRipNeighbor.getOwner();
                  String remoteHostname = remoteHost.getHostname();
                  if (includeNodes1.contains(hostname) && includeNodes2.contains(remoteHostname)) {
                    Ip localIp = ripNeighbor.getLocalIp();
                    Ip remoteIp = remoteRipNeighbor.getLocalIp();
                    IpEdge edge = new IpEdge(hostname, localIp, remoteHostname, remoteIp);
                    vedges.add(new VerboseRipEdge(ripNeighbor, remoteRipNeighbor, edge));
                  }
                }
              }
            }
          }
        }

        switch (question.getStyle()) {
          case SUMMARY:
            answerElement.initRipNeighbors();
            for (VerboseRipEdge vedge : vedges) {
              answerElement.getRipNeighbors().add(vedge.getEdgeSummary());
            }
            break;
          case VERBOSE:
            answerElement.setVerboseRipNeighbors(vedges);
            break;
          case ROLE:
            SortedSet<RoleEdge> redges = new TreeSet<>();
            for (VerboseRipEdge vedge : vedges) {
              SortedSet<String> roles1 =
                  _nodeRolesMap.getOrDefault(vedge.getEdgeSummary().getNode1(), new TreeSet<>());
              SortedSet<String> roles2 =
                  _nodeRolesMap.getOrDefault(vedge.getEdgeSummary().getNode2(), new TreeSet<>());
              for (String r1 : roles1) {
                for (String r2 : roles2) {
                  redges.add(new RoleEdge(r1, r2));
                }
              }
            }
            answerElement.setRoleOspfNeighbors(redges);
            break;
          default:
            throw new BatfishException(
                "Unsupported " + EdgeStyle.class.getCanonicalName() + ": " + question.getStyle());
        }
      }

      if (question.getNeighborTypes().contains(NeighborType.EBGP)) {
        initRemoteBgpNeighbors(configurations);
        SortedSet<VerboseBgpEdge> vedges = new TreeSet<>(VERBOSE_BGP_EDGE_COMPARATOR);
        for (EndpointPair<BgpPeerConfigId> session : _bgpTopology.edges()) {
          BgpPeerConfigId bgpPeerConfigId = session.source();
          BgpPeerConfigId remoteBgpPeerConfigId = session.target();
          boolean ebgp =
              _bgpTopology
                  .edgeValue(bgpPeerConfigId, remoteBgpPeerConfigId)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              String.format(
                                  "Edge %s -> %s not in BGP topology",
                                  bgpPeerConfigId, remoteBgpPeerConfigId)))
                  .isEbgp();
          if (ebgp) {
            VerboseBgpEdge edge =
                constructVerboseBgpEdge(
                    includeNodes1,
                    includeNodes2,
                    bgpPeerConfigId,
                    remoteBgpPeerConfigId,
                    NetworkConfigurations.of(configurations));
            if (edge != null) {
              vedges.add(edge);
            }
          }
        }

        switch (question.getStyle()) {
          case SUMMARY:
            answerElement.initEbgpNeighbors();
            for (VerboseBgpEdge vedge : vedges) {
              answerElement.getEbgpNeighbors().add(vedge.getEdgeSummary());
            }
            break;
          case VERBOSE:
            answerElement.setVerboseEbgpNeighbors(vedges);
            break;
          case ROLE:
            answerElement.setRoleEbgpNeighbors(verboseToRoleEdges(vedges));
            break;
          default:
            throw new BatfishException(
                "Unsupported " + EdgeStyle.class.getCanonicalName() + ": " + question.getStyle());
        }
      }

      if (question.getNeighborTypes().contains(NeighborType.IBGP)) {
        SortedSet<VerboseBgpEdge> vedges = new TreeSet<>(VERBOSE_BGP_EDGE_COMPARATOR);
        initRemoteBgpNeighbors(configurations);
        for (EndpointPair<BgpPeerConfigId> session : _bgpTopology.edges()) {
          BgpPeerConfigId bgpPeerConfigId = session.source();
          BgpPeerConfigId remoteBgpPeerConfigId = session.target();
          BgpSessionProperties sessionProp =
              _bgpTopology
                  .edgeValue(bgpPeerConfigId, remoteBgpPeerConfigId)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              String.format(
                                  "Edge %s -> %s not in IBGP topology",
                                  bgpPeerConfigId, remoteBgpPeerConfigId)));
          boolean ibgp = sessionProp.getSessionType() == SessionType.IBGP;
          if (ibgp) {
            VerboseBgpEdge edge =
                constructVerboseBgpEdge(
                    includeNodes1,
                    includeNodes2,
                    bgpPeerConfigId,
                    remoteBgpPeerConfigId,
                    NetworkConfigurations.of(configurations));
            if (edge != null) {
              vedges.add(edge);
            }
          }
        }
        switch (question.getStyle()) {
          case SUMMARY:
            answerElement.initIbgpNeighbors();
            for (VerboseBgpEdge vedge : vedges) {
              answerElement.getIbgpNeighbors().add(vedge.getEdgeSummary());
            }
            break;
          case VERBOSE:
            answerElement.setVerboseIbgpNeighbors(vedges);
            break;
          case ROLE:
            answerElement.setRoleIbgpNeighbors(verboseToRoleEdges(vedges));
            break;
          default:
            throw new BatfishException(
                "Unsupported " + EdgeStyle.class.getCanonicalName() + ": " + question.getStyle());
        }
      }

      if (question.getNeighborTypes().isEmpty()
          || question.getNeighborTypes().contains(NeighborType.LAYER3)) {
        initTopology();
        SortedSet<Edge> matchingEdges = new TreeSet<>();
        for (Edge edge : _topology.getEdges()) {
          if (includeNodes1.contains(edge.getNode1()) && includeNodes2.contains(edge.getNode2())) {
            matchingEdges.add(edge);
          }
        }
        switch (question.getStyle()) {
          case SUMMARY:
            answerElement.setLayer3Neighbors(matchingEdges);
            break;
          case VERBOSE:
            SortedSet<VerboseEdge> vMatchingEdges = new TreeSet<>();
            for (Edge edge : matchingEdges) {
              Configuration n1 = configurations.get(edge.getNode1());
              Interface i1 = n1.getAllInterfaces().get(edge.getInt1());
              Configuration n2 = configurations.get(edge.getNode2());
              Interface i2 = n2.getAllInterfaces().get(edge.getInt2());
              vMatchingEdges.add(new VerboseEdge(i1, i2, edge));
            }
            answerElement.setVerboseLayer3Neighbors(vMatchingEdges);
            break;
          case ROLE:
            SortedSet<RoleEdge> rMatchingEdges = new TreeSet<>();
            for (Edge edge : matchingEdges) {
              SortedSet<String> roles1 =
                  _nodeRolesMap.getOrDefault(edge.getNode1(), new TreeSet<>());
              SortedSet<String> roles2 =
                  _nodeRolesMap.getOrDefault(edge.getNode2(), new TreeSet<>());
              for (String r1 : roles1) {
                for (String r2 : roles2) {
                  rMatchingEdges.add(new RoleEdge(r1, r2));
                }
              }
            }
            answerElement.setRoleLayer3Neighbors(rMatchingEdges);
            break;
          default:
            throw new BatfishException(
                "Unsupported " + EdgeStyle.class.getCanonicalName() + ": " + question.getStyle());
        }
      }

      Layer1Topology layer1Topology;
      boolean layer1 = question.getNeighborTypes().contains(NeighborType.LAYER1);
      boolean layer2 = question.getNeighborTypes().contains(NeighborType.LAYER2);
      if (layer1 || layer2) {
        layer1Topology = _batfish.getLayer1Topology();
        if (layer1) {
          answerElement.setLayer1Neighbors(layer1Topology);
        }
        if (layer2) {
          answerElement.setLayer2Neighbors(_batfish.getLayer2Topology());
        }
      }
      return answerElement;
    }

    /**
     * Create a verbose bgp edge, if hostnames match specified pattern
     *
     * @param includeNodes1 Allowed src hostnames
     * @param includeNodes2 Allowed dst hostnames
     * @param bgpPeerConfigId The id of node1 bgp neighbor
     * @param remoteBgpPeerConfigId The id of node2 bgp neighbor
     * @param nc {@link NetworkConfigurations} to get {@link BgpPeerConfig}s
     * @return a new {@link VerboseBgpEdge} describing the BGP peering or {@code null} if hostname
     *     filters are not satisfied.
     */
    @Nullable
    private static VerboseBgpEdge constructVerboseBgpEdge(
        Set<String> includeNodes1,
        Set<String> includeNodes2,
        BgpPeerConfigId bgpPeerConfigId,
        BgpPeerConfigId remoteBgpPeerConfigId,
        NetworkConfigurations nc) {
      String hostname = bgpPeerConfigId.getHostname();
      String remoteHostname = remoteBgpPeerConfigId.getHostname();
      BgpPeerConfig bgpPeerConfig = nc.getBgpPeerConfig(bgpPeerConfigId);
      BgpPeerConfig remoteBgpPeerConfig = nc.getBgpPeerConfig(remoteBgpPeerConfigId);
      if (bgpPeerConfig == null || remoteBgpPeerConfig == null) {
        return null;
      }
      if (includeNodes1.contains(hostname) && includeNodes2.contains(remoteHostname)) {
        Ip localIp = bgpPeerConfig.getLocalIp();
        Ip remoteIp = remoteBgpPeerConfig.getLocalIp();
        IpEdge edge = new IpEdge(hostname, localIp, remoteHostname, remoteIp);
        return new VerboseBgpEdge(
            bgpPeerConfig, remoteBgpPeerConfig, bgpPeerConfigId, remoteBgpPeerConfigId, edge);
      }
      return null;
    }

    private void initRemoteBgpNeighbors(Map<String, Configuration> configurations) {
      if (!_remoteBgpNeighborsInitialized) {
        Map<Ip, Set<String>> ipOwners = TopologyUtil.computeIpNodeOwners(configurations, true);
        _bgpTopology =
            BgpTopologyUtils.initBgpTopology(configurations, ipOwners, false, false, null);
        _remoteBgpNeighborsInitialized = true;
      }
    }

    private void initRemoteEigrpNeighbors(
        Map<String, Configuration> configurations, Topology topology) {
      if (!_remoteEigrpNeighborsInitialized) {
        _eigrpTopology = EigrpTopology.initEigrpTopology(configurations, topology);
        _remoteEigrpNeighborsInitialized = true;
      }
    }

    private void initRemoteOspfNeighbors(
        Map<String, Configuration> configurations, Topology topology) {
      if (!_remoteOspfNeighborsInitialized) {
        OspfTopologyUtils.initRemoteOspfNeighbors(configurations, topology);
        _remoteOspfNeighborsInitialized = true;
      }
    }

    private void initRemoteRipNeighbors(
        IBatfish batfish, Map<String, Configuration> configurations, Topology topology) {
      if (!_remoteRipNeighborsInitialized) {
        Map<Ip, Set<String>> ipOwners = TopologyUtil.computeIpNodeOwners(configurations, true);
        batfish.initRemoteRipNeighbors(configurations, ipOwners, topology);
        _remoteRipNeighborsInitialized = true;
      }
    }

    private void initTopology() {
      if (_topology == null) {
        _topology = _batfish.getEnvironmentTopology();
      }
    }

    private SortedSet<RoleEdge> verboseToRoleEdges(SortedSet<VerboseBgpEdge> vedges) {
      SortedSet<RoleEdge> redges = new TreeSet<>();
      for (VerboseBgpEdge vedge : vedges) {
        SortedSet<String> roles1 =
            _nodeRolesMap.getOrDefault(vedge.getEdgeSummary().getNode1(), new TreeSet<>());
        SortedSet<String> roles2 =
            _nodeRolesMap.getOrDefault(vedge.getEdgeSummary().getNode2(), new TreeSet<>());
        for (String r1 : roles1) {
          for (String r2 : roles2) {
            redges.add(new RoleEdge(r1, r2));
          }
        }
      }
      return redges;
    }
  }

  /**
   * Produces the raw JSON datamodel for neighbor relationships in the snapshot.
   *
   * <p>Generally prefer {@link org.batfish.question.edges.EdgesQuestion}.
   */
  public static class NeighborsQuestion extends Question {

    private static final String PROP_NEIGHBOR_TYPES = "neighborTypes";

    private static final String PROP_NODES = "nodes";

    private static final String PROP_REMOTE_NODES = "remoteNodes";

    private static final String PROP_ROLE_DIMENSION = "roleDimension";

    private static final String PROP_STYLE = "style";

    @Nonnull private final SortedSet<NeighborType> _neighborTypes;

    @Nonnull private final NodesSpecifier _nodes;

    @Nonnull private final NodesSpecifier _remoteNodes;

    @Nullable private final String _roleDimension;

    @Nonnull private final EdgeStyle _style;

    @JsonCreator
    public NeighborsQuestion(
        @JsonProperty(PROP_NODES) NodesSpecifier nodes,
        @JsonProperty(PROP_REMOTE_NODES) NodesSpecifier remoteNodes,
        @JsonProperty(PROP_NEIGHBOR_TYPES) SortedSet<NeighborType> neighborTypes,
        @JsonProperty(PROP_STYLE) EdgeStyle style,
        @JsonProperty(PROP_ROLE_DIMENSION) String roleDimension) {
      _nodes = nodes == null ? NodesSpecifier.ALL : nodes;
      _remoteNodes = remoteNodes == null ? NodesSpecifier.ALL : remoteNodes;
      _neighborTypes = neighborTypes == null ? new TreeSet<>() : neighborTypes;
      _style = style == null ? EdgeStyle.SUMMARY : style;
      _roleDimension = roleDimension;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "neighbors";
    }

    @JsonProperty(PROP_NEIGHBOR_TYPES)
    public SortedSet<NeighborType> getNeighborTypes() {
      return _neighborTypes;
    }

    @JsonProperty(PROP_NODES)
    public NodesSpecifier getNodes() {
      return _nodes;
    }

    @JsonProperty(PROP_REMOTE_NODES)
    public NodesSpecifier getRemoteNodes() {
      return _remoteNodes;
    }

    @JsonProperty(PROP_ROLE_DIMENSION)
    public String getRoleDimension() {
      return _roleDimension;
    }

    @JsonProperty(PROP_STYLE)
    public EdgeStyle getStyle() {
      return _style;
    }

    @Override
    public String prettyPrint() {
      try {
        String retString =
            String.format(
                "neighbors %s%s=%s | %s=%s | %s=%s | %s=%b",
                prettyPrintBase(),
                PROP_NODES,
                _nodes,
                PROP_REMOTE_NODES,
                _remoteNodes,
                PROP_NEIGHBOR_TYPES,
                _neighborTypes.toString(),
                PROP_STYLE,
                _style);
        return retString;
      } catch (Exception e) {
        try {
          return "Pretty printing failed. Printing Json\n" + toJsonString();
        } catch (BatfishException e1) {
          throw new BatfishException("Both pretty and json printing failed\n");
        }
      }
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new NeighborsAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new NeighborsQuestion(null, null, null, null, null);
  }
}
