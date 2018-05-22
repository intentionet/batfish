package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.Network;
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
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpSession;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NeighborType;
import org.batfish.datamodel.OspfNeighbor;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.RipNeighbor;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.RoleEdge;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.VerboseEdge;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.collections.IpEdge;
import org.batfish.datamodel.collections.VerboseBgpEdge;
import org.batfish.datamodel.collections.VerboseOspfEdge;
import org.batfish.datamodel.collections.VerboseRipEdge;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.role.NodeRoleDimension;

@AutoService(Plugin.class)
public class NeighborsQuestionPlugin extends QuestionPlugin {

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

    private static final String PROP_LAN_NEIGHBORS = "lanNeighbors";

    private static final String PROP_OSPF_NEIGHBORS = "ospfNeighbors";

    private static final String PROP_RIP_NEIGHBORS = "ripNeighbors";

    private static final String PROP_ROLE_EBGP_NEIGHBORS = "roleEbgpNeighbors";

    private static final String PROP_ROLE_IBGP_NEIGHBORS = "roleIbgpNeighbors";

    private static final String PROP_ROLE_LAN_NEIGHBORS = "roleLanNeighbors";

    private static final String PROP_ROLE_OSPF_NEIGHBORS = "roleOspfNeighbors";

    private static final String PROP_ROLE_RIP_NEIGHBORS = "roleRipNeighbors";

    private static final String PROP_VERBOSE_EBGP_NEIGHBORS = "verboseEbgpNeighbors";

    private static final String PROP_VERBOSE_IBGP_NEIGHBORS = "verboseIbgpNeighbors";

    private static final String PROP_VERBOSE_LAN_NEIGHBORS = "verboseLanNeighbors";

    private static final String PROP_VERBOSE_OSPF_NEIGHBORS = "verboseOspfNeighbors";

    private static final String PROP_VERBOSE_RIP_NEIGHBORS = "verboseRipNeighbors";

    private SortedSet<IpEdge> _ebgpNeighbors;

    private SortedSet<IpEdge> _ibgpNeighbors;

    private SortedSet<Edge> _lanNeighbors;

    private SortedSet<IpEdge> _ospfNeighbors;

    private SortedSet<IpEdge> _ripNeighbors;

    private SortedSet<RoleEdge> _roleEbgpNeighbors;

    private SortedSet<RoleEdge> _roleIbgpNeighbors;

    private SortedSet<RoleEdge> _roleLanNeighbors;

    private SortedSet<RoleEdge> _roleOspfNeighbors;

    private SortedSet<RoleEdge> _roleRipNeighbors;

    private SortedSet<VerboseBgpEdge> _verboseEbgpNeighbors;

    private SortedSet<VerboseBgpEdge> _verboseIbgpNeighbors;

    private SortedSet<VerboseEdge> _verboseLanNeighbors;

    private SortedSet<VerboseOspfEdge> _verboseOspfNeighbors;

    private SortedSet<VerboseRipEdge> _verboseRipNeighbors;

    public void addLanEdge(Edge edge) {
      _lanNeighbors.add(edge);
    }

    @JsonProperty(PROP_EBGP_NEIGHBORS)
    public SortedSet<IpEdge> getEbgpNeighbors() {
      return _ebgpNeighbors;
    }

    @JsonProperty(PROP_IBGP_NEIGHBORS)
    public SortedSet<IpEdge> getIbgpNeighbors() {
      return _ibgpNeighbors;
    }

    @JsonProperty(PROP_LAN_NEIGHBORS)
    public SortedSet<Edge> getLanNeighbors() {
      return _lanNeighbors;
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

    @JsonProperty(PROP_ROLE_IBGP_NEIGHBORS)
    public SortedSet<RoleEdge> getRoleIbgpNeighbors() {
      return _roleIbgpNeighbors;
    }

    @JsonProperty(PROP_ROLE_LAN_NEIGHBORS)
    public SortedSet<RoleEdge> getRoleLanNeighbors() {
      return _roleLanNeighbors;
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

    @JsonProperty(PROP_VERBOSE_IBGP_NEIGHBORS)
    public SortedSet<VerboseBgpEdge> getVerboseIbgpNeighbors() {
      return _verboseIbgpNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_LAN_NEIGHBORS)
    public SortedSet<VerboseEdge> getVerboseLanNeighbors() {
      return _verboseLanNeighbors;
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

    public void initIbgpNeighbors() {
      _ibgpNeighbors = new TreeSet<>();
    }

    public void initLanNeighbors() {
      _lanNeighbors = new TreeSet<>();
    }

    public void initOspfNeighbors() {
      _ospfNeighbors = new TreeSet<>();
    }

    public void initRipNeighbors() {
      _ripNeighbors = new TreeSet<>();
    }

    public void initVerboseLanNeighbors() {
      _verboseLanNeighbors = new TreeSet<>();
    }

    @Override
    public String prettyPrint() {
      StringBuilder sb = new StringBuilder("Results for neighbors\n");

      if (_lanNeighbors != null) {
        sb.append("  LAN neighbors\n");
        for (Edge edge : _lanNeighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_verboseLanNeighbors != null) {
        sb.append("  LAN neighbors\n");
        for (VerboseEdge edge : _verboseLanNeighbors) {
          sb.append("    " + edge + "\n");
        }
      }

      if (_roleLanNeighbors != null) {
        sb.append("  LAN neighbors\n");
        for (RoleEdge edge : _roleLanNeighbors) {
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

    @JsonProperty(PROP_IBGP_NEIGHBORS)
    public void setIbgpNeighbors(SortedSet<IpEdge> ibgpNeighbors) {
      _ibgpNeighbors = ibgpNeighbors;
    }

    @JsonProperty(PROP_LAN_NEIGHBORS)
    public void setLanNeighbors(SortedSet<Edge> lanNeighbors) {
      _lanNeighbors = lanNeighbors;
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

    @JsonProperty(PROP_ROLE_IBGP_NEIGHBORS)
    public void setRoleIbgpNeighbors(SortedSet<RoleEdge> roleIbgpNeighbors) {
      _roleIbgpNeighbors = roleIbgpNeighbors;
    }

    @JsonProperty(PROP_ROLE_LAN_NEIGHBORS)
    public void setRoleLanNeighbors(SortedSet<RoleEdge> roleLanNeighbors) {
      _roleLanNeighbors = roleLanNeighbors;
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
    public void setVerboseEbgpNeighbors(SortedSet<VerboseBgpEdge> verboseEbgpNeighbors) {
      _verboseEbgpNeighbors = verboseEbgpNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_IBGP_NEIGHBORS)
    public void setVerboseIbgpNeighbors(SortedSet<VerboseBgpEdge> verboseIbgpNeighbors) {
      _verboseIbgpNeighbors = verboseIbgpNeighbors;
    }

    @JsonProperty(PROP_VERBOSE_LAN_NEIGHBORS)
    public void setVerboseLanNeighbors(SortedSet<VerboseEdge> verboseLanNeighbors) {
      _verboseLanNeighbors = verboseLanNeighbors;
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

    private Network<BgpNeighbor, BgpSession> _bgpTopology;

    private SortedMap<String, SortedSet<String>> _nodeRolesMap;

    private boolean _remoteBgpNeighborsInitialized;

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
      Set<String> includeNodes1 = question.getNode1Regex().getMatchingNodes(_batfish);
      Set<String> includeNodes2 = question.getNode2Regex().getMatchingNodes(_batfish);

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

      if (question.getNeighborTypes().contains(NeighborType.OSPF)) {
        SortedSet<VerboseOspfEdge> vedges = new TreeSet<>();
        initTopology();
        initRemoteOspfNeighbors(_batfish, configurations, _topology);
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
        SortedSet<VerboseBgpEdge> vedges = new TreeSet<>();
        for (BgpSession session : _bgpTopology.edges()) {
          BgpNeighbor bgpNeighbor = session.getSrc();
          BgpNeighbor remoteBgpNeighbor = session.getDst();
          boolean ebgp = session.isEbgp();
          if (ebgp) {
            VerboseBgpEdge edge =
                constructVerboseBgpEdge(
                    includeNodes1, includeNodes2, bgpNeighbor, remoteBgpNeighbor);
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
        SortedSet<VerboseBgpEdge> vedges = new TreeSet<>();
        initRemoteBgpNeighbors(configurations);
        for (BgpSession session : _bgpTopology.edges()) {
          BgpNeighbor bgpNeighbor = session.getSrc();
          BgpNeighbor remoteBgpNeighbor = session.getDst();
          if (remoteBgpNeighbor != null) {
            boolean ibgp = !session.isEbgp();
            if (ibgp) {
              VerboseBgpEdge edge =
                  constructVerboseBgpEdge(
                      includeNodes1, includeNodes2, bgpNeighbor, remoteBgpNeighbor);
              if (edge != null) {
                vedges.add(edge);
              }
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
          || question.getNeighborTypes().contains(NeighborType.LAN)) {
        initTopology();
        SortedSet<Edge> matchingEdges = new TreeSet<>();
        for (Edge edge : _topology.getEdges()) {
          if (includeNodes1.contains(edge.getNode1()) && includeNodes2.contains(edge.getNode2())) {
            matchingEdges.add(edge);
          }
        }
        switch (question.getStyle()) {
          case SUMMARY:
            answerElement.setLanNeighbors(matchingEdges);
            break;
          case VERBOSE:
            SortedSet<VerboseEdge> vMatchingEdges = new TreeSet<>();
            for (Edge edge : matchingEdges) {
              Configuration n1 = configurations.get(edge.getNode1());
              Interface i1 = n1.getInterfaces().get(edge.getInt1());
              Configuration n2 = configurations.get(edge.getNode2());
              Interface i2 = n2.getInterfaces().get(edge.getInt2());
              vMatchingEdges.add(new VerboseEdge(i1, i2, edge));
            }
            answerElement.setVerboseLanNeighbors(vMatchingEdges);
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
            answerElement.setRoleLanNeighbors(rMatchingEdges);
            break;
          default:
            throw new BatfishException(
                "Unsupported " + EdgeStyle.class.getCanonicalName() + ": " + question.getStyle());
        }
      }

      return answerElement;
    }

    /**
     * Create a verbose bgp edge, if hostnames match specified pattern
     *
     * @param includeNodes1 Allowed src hostnames
     * @param includeNodes2 Allowed dst hostnames
     * @param bgpNeighbor node1 bgp neighbor
     * @param remoteBgpNeighbor node2 bgp neighbor
     * @return a new {@link VerboseBgpEdge} describing the BGP peering or {@code null} if hostname
     *     filters are not satisfied.
     */
    @Nullable
    private static VerboseBgpEdge constructVerboseBgpEdge(
        Set<String> includeNodes1,
        Set<String> includeNodes2,
        BgpNeighbor bgpNeighbor,
        BgpNeighbor remoteBgpNeighbor) {
      String hostname = bgpNeighbor.getOwner().getHostname();
      String remoteHostname = remoteBgpNeighbor.getOwner().getHostname();
      if (includeNodes1.contains(hostname) && includeNodes2.contains(remoteHostname)) {
        Ip localIp = bgpNeighbor.getLocalIp();
        Ip remoteIp = remoteBgpNeighbor.getLocalIp();
        IpEdge edge = new IpEdge(hostname, localIp, remoteHostname, remoteIp);
        return new VerboseBgpEdge(bgpNeighbor, remoteBgpNeighbor, edge);
      }
      return null;
    }

    private void initRemoteBgpNeighbors(Map<String, Configuration> configurations) {
      if (!_remoteBgpNeighborsInitialized) {
        Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
        _bgpTopology =
            CommonUtil.initBgpTopology(configurations, ipOwners, false, false, null, null);
        _remoteBgpNeighborsInitialized = true;
      }
    }

    private void initRemoteOspfNeighbors(
        IBatfish batfish, Map<String, Configuration> configurations, Topology topology) {
      if (!_remoteOspfNeighborsInitialized) {
        Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
        CommonUtil.initRemoteOspfNeighbors(configurations, ipOwners, topology);
        _remoteOspfNeighborsInitialized = true;
      }
    }

    private void initRemoteRipNeighbors(
        IBatfish batfish, Map<String, Configuration> configurations, Topology topology) {
      if (!_remoteRipNeighborsInitialized) {
        Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
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

  // <question_page_comment>

  /**
   * Lists neighbor relationships in the testrig.
   *
   * <p>Details coming
   *
   * @type Neighbors multifile
   * @param neighborType The type(s) of neighbor relationships to focus on among (eBGP, iBGP, IP).
   *     Default is IP.
   * @param node1Regex Regular expression to match the nodes names for one end of pair. Default is
   *     '.*' (all nodes).
   * @param node2Regex Regular expression to match the nodes names for the other end of the pair.
   *     Default is '.*' (all nodes).
   * @param style String indicating the style of information requested about each edge: "summary" is
   *     the default and returns only the names of nodes/interfaces in the edge; "verbose" provides
   *     full configuration information about those nodes/interfaces; "role" abstracts edges to the
   *     role level.
   * @param roleSpecifier NodeRoleSpecifier that assigns roles to nodes. This is an optional
   *     variable that is only used if the style is set to "role". If no roleSpecifier is provided
   *     then by default the roles originally assigned to nodes when the configurations were loaded
   *     are used.
   * @example bf_answer("Neighbors", neighborType=["ebgp", "ibgp"] node1Regex="as1.*",
   *     node2Regex="as2.*") Shows all eBGP and iBGP neighbor relationships between nodes that start
   *     with as1 and those that start with as2.
   */
  public static class NeighborsQuestion extends Question {

    private static final String PROP_NEIGHBOR_TYPES = "neighborTypes";

    private static final String PROP_NODE1_REGEX = "node1Regex";

    private static final String PROP_NODE2_REGEX = "node2Regex";

    private static final String PROP_ROLE_DIMENSION = "roleDimension";

    private static final String PROP_STYLE = "style";

    @Nonnull private final SortedSet<NeighborType> _neighborTypes;

    @Nonnull private final NodesSpecifier _node1Regex;

    @Nonnull private final NodesSpecifier _node2Regex;

    @Nullable private final String _roleDimension;

    @Nonnull private final EdgeStyle _style;

    @JsonCreator
    public NeighborsQuestion(
        @JsonProperty(PROP_NODE1_REGEX) NodesSpecifier nodes1,
        @JsonProperty(PROP_NODE2_REGEX) NodesSpecifier nodes2,
        @JsonProperty(PROP_NEIGHBOR_TYPES) SortedSet<NeighborType> neighborTypes,
        @JsonProperty(PROP_STYLE) EdgeStyle style,
        @JsonProperty(PROP_ROLE_DIMENSION) String roleDimension) {
      _node1Regex = nodes1 == null ? NodesSpecifier.ALL : nodes1;
      _node2Regex = nodes2 == null ? NodesSpecifier.ALL : nodes2;
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

    @JsonProperty(PROP_NODE1_REGEX)
    public NodesSpecifier getNode1Regex() {
      return _node1Regex;
    }

    @JsonProperty(PROP_NODE2_REGEX)
    public NodesSpecifier getNode2Regex() {
      return _node2Regex;
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
                PROP_NODE1_REGEX,
                _node1Regex,
                PROP_NODE2_REGEX,
                _node2Regex,
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
