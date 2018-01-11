package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.common.base.MoreObjects;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OspfNeighbor;
import org.batfish.datamodel.OspfNeighbor.OspfNeighborSummary;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixTrie;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.collections.IpPair;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class OspfSessionCheckQuestionPlugin extends QuestionPlugin {

  public static class OspfSessionCheckAnswerElement implements AnswerElement {

    private static final String PROP_ALL_OSPF_NEIGHBORS = "allOspfNeighbors";

    private static final String PROP_BROKEN = "broken";

    private static final String PROP_HALF_OPEN = "halfOpen";

    private static final String PROP_IGNORED_FOREIGN_ENDPOINTS = "ignoredForeignEndpoints";

    private static final String PROP_MISMATCH_LINK_COST = "mismatchLinkCost";

    private SortedMap<String, SortedMap<String, SortedMap<IpPair, OspfNeighborSummary>>>
        _allOspfNeighbors;

    private SortedMap<String, SortedMap<String, SortedSet<IpPair>>> _broken;

    private SortedMap<String, SortedMap<String, SortedSet<IpPair>>> _halfOpen;

    private SortedMap<String, SortedMap<String, SortedSet<IpPair>>> _ignoredForeignEndpoints;

    private SortedMap<String, SortedMap<String, SortedSet<IpPair>>> _mismatchLinkCost;

    public OspfSessionCheckAnswerElement() {
      _allOspfNeighbors = new TreeMap<>();
      _broken = new TreeMap<>();
      _halfOpen = new TreeMap<>();
      _ignoredForeignEndpoints = new TreeMap<>();
      _mismatchLinkCost = new TreeMap<>();
    }

    public void add(
        SortedMap<String, SortedMap<String, SortedSet<IpPair>>> neighborsByHostname,
        String hostname,
        String vrf,
        OspfNeighborSummary ospfNeighbor) {
      SortedMap<String, SortedSet<IpPair>> neighborsByVrf =
          neighborsByHostname.computeIfAbsent(hostname, k -> new TreeMap<>());
      SortedSet<IpPair> neighbors = neighborsByVrf.computeIfAbsent(vrf, k -> new TreeSet<>());
      IpPair ipPair = new IpPair(ospfNeighbor.getLocalIp(), ospfNeighbor.getRemoteIp());
      neighbors.add(ipPair);
    }

    public void addToAll(
        SortedMap<String, SortedMap<String, SortedMap<IpPair, OspfNeighborSummary>>>
            neighborsByHostname,
        String hostname,
        String vrf,
        OspfNeighborSummary ospfNeighbor) {
      SortedMap<String, SortedMap<IpPair, OspfNeighborSummary>> neighborsByVrf =
          neighborsByHostname.computeIfAbsent(hostname, k -> new TreeMap<>());
      SortedMap<IpPair, OspfNeighborSummary> neighborsByIp =
          neighborsByVrf.computeIfAbsent(vrf, k -> new TreeMap<>());
      IpPair ipPair = new IpPair(ospfNeighbor.getLocalIp(), ospfNeighbor.getRemoteIp());
      neighborsByIp.put(ipPair, ospfNeighbor);
    }

    @JsonProperty(PROP_ALL_OSPF_NEIGHBORS)
    public SortedMap<String, SortedMap<String, SortedMap<IpPair, OspfNeighborSummary>>>
        getAllOspfNeighbors() {
      return _allOspfNeighbors;
    }

    @JsonProperty(PROP_BROKEN)
    private SortedMap<String, SortedMap<String, SortedSet<IpPair>>> getBroken() {
      return _broken;
    }

    @JsonProperty(PROP_HALF_OPEN)
    private SortedMap<String, SortedMap<String, SortedSet<IpPair>>> getHalfOpen() {
      return _halfOpen;
    }

    @JsonProperty(PROP_IGNORED_FOREIGN_ENDPOINTS)
    private SortedMap<String, SortedMap<String, SortedSet<IpPair>>> getIgnoredForeignEndpoints() {
      return _ignoredForeignEndpoints;
    }

    @JsonProperty(PROP_MISMATCH_LINK_COST)
    private SortedMap<String, SortedMap<String, SortedSet<IpPair>>> getMismatchLinkCost() {
      return _mismatchLinkCost;
    }

    @Override
    public String prettyPrint() {
      StringBuilder sb = new StringBuilder();
      sb.append(prettyPrintCategory(_halfOpen, PROP_HALF_OPEN));
      sb.append(prettyPrintCategory(_mismatchLinkCost, PROP_MISMATCH_LINK_COST));
      return sb.toString();
    }

    private CharSequence prettyPrintCategory(
        SortedMap<String, SortedMap<String, SortedSet<IpPair>>> category, String name) {
      StringBuilder sb = new StringBuilder();
      if (!category.isEmpty()) {
        sb.append(name + ":\n");
        for (Entry<String, SortedMap<String, SortedSet<IpPair>>> e1 : category.entrySet()) {
          String hostname = e1.getKey();
          sb.append("  " + hostname + ":\n");
          SortedMap<String, SortedSet<IpPair>> neighborsByHostname = e1.getValue();
          for (Entry<String, SortedSet<IpPair>> e2 : neighborsByHostname.entrySet()) {
            String vrf = e2.getKey();
            sb.append("    " + vrf + ":\n");
            SortedSet<IpPair> neighbors = e2.getValue();
            for (IpPair ipPair : neighbors) {
              OspfNeighborSummary summary = _allOspfNeighbors.get(hostname).get(vrf).get(ipPair);
              Ip remoteIp = summary.getRemoteIp();
              Ip localIp = summary.getLocalIp();
              sb.append(
                  String.format("      remoteIp: %-15s   localIp: %-15s\n", remoteIp, localIp));
            }
          }
        }
      }
      return sb;
    }

    @JsonProperty(PROP_ALL_OSPF_NEIGHBORS)
    public void setAllOspfNeighbors(
        SortedMap<String, SortedMap<String, SortedMap<IpPair, OspfNeighborSummary>>>
            allOspfNeighbors) {
      _allOspfNeighbors = allOspfNeighbors;
    }

    @JsonProperty(PROP_BROKEN)
    public void setBroken(SortedMap<String, SortedMap<String, SortedSet<IpPair>>> broken) {
      _broken = broken;
    }

    @JsonProperty(PROP_HALF_OPEN)
    public void setHalfOpen(SortedMap<String, SortedMap<String, SortedSet<IpPair>>> halfOpen) {
      _halfOpen = halfOpen;
    }

    @JsonProperty(PROP_IGNORED_FOREIGN_ENDPOINTS)
    public void setIgnoredForeignEndpoints(
        SortedMap<String, SortedMap<String, SortedSet<IpPair>>> ignoredForeignEndpoints) {
      _ignoredForeignEndpoints = ignoredForeignEndpoints;
    }

    @JsonProperty(PROP_MISMATCH_LINK_COST)
    public void setMismatchLinkCost(
        SortedMap<String, SortedMap<String, SortedSet<IpPair>>> mismatchLinkCost) {
      _mismatchLinkCost = mismatchLinkCost;
    }
  }

  public static class OspfSessionCheckAnswerer extends Answerer {

    public OspfSessionCheckAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {

      OspfSessionCheckQuestion question = (OspfSessionCheckQuestion) _question;

      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      Set<String> includeNodes1 = question.getNode1Regex().getMatchingNodes(configurations);
      Set<String> includeNodes2 = question.getNode2Regex().getMatchingNodes(configurations);

      SortedSet<Prefix> foreignPrefixes =
          MoreObjects.firstNonNull(question._foreignOspfNetworks, Collections.emptySortedSet());
      PrefixTrie foreignPrefixTrie = new PrefixTrie(foreignPrefixes);

      OspfSessionCheckAnswerElement answerElement = new OspfSessionCheckAnswerElement();
      Set<Ip> allInterfaceIps = new HashSet<>();
      Set<Ip> loopbackIps = new HashSet<>();
      Map<Ip, Set<String>> ipOwners = new HashMap<>();
      for (Configuration c : configurations.values()) {
        for (Interface i : c.getInterfaces().values()) {
          if (i.getActive() && i.getAddress() != null) {
            for (Prefix prefix : i.getAllAddresses()) {
              Ip address = prefix.getAddress();
              if (i.isLoopback(c.getConfigurationFormat())) {
                loopbackIps.add(address);
              }
              allInterfaceIps.add(address);
              Set<String> currentIpOwners = ipOwners.computeIfAbsent(address, k -> new HashSet<>());
              currentIpOwners.add(c.getHostname());
            }
          }
        }
      }
      Topology topology = _batfish.getEnvironmentTopology();
      _batfish.initRemoteOspfNeighbors(configurations, ipOwners, topology);
      for (Configuration co : configurations.values()) {
        String hostname = co.getHostname();
        if (!includeNodes1.contains(co.getHostname())) {
          continue;
        }
        for (Vrf vrf : co.getVrfs().values()) {
          String vrfName = vrf.getName();
          OspfProcess proc = vrf.getOspfProcess();
          if (proc != null) {
            for (OspfNeighbor ospfNeighbor : proc.getOspfNeighbors().values()) {
              OspfNeighborSummary ospfNeighborSummary = new OspfNeighborSummary(ospfNeighbor);
              answerElement.addToAll(
                  answerElement.getAllOspfNeighbors(), hostname, vrfName, ospfNeighborSummary);
              boolean foreign = foreignPrefixTrie.containsIp(ospfNeighbor.getRemoteIp());
              if (foreign) {
                answerElement.add(
                    answerElement.getIgnoredForeignEndpoints(),
                    hostname,
                    vrfName,
                    ospfNeighborSummary);
              } else if (ospfNeighbor.getRemoteOspfNeighbor() == null) {
                if (!ospfNeighbor.getIface().getOspfPassive()) {
                  // half-open
                  answerElement.add(
                      answerElement.getBroken(), hostname, vrfName, ospfNeighborSummary);
                  answerElement.add(
                      answerElement.getHalfOpen(), hostname, vrfName, ospfNeighborSummary);
                }
              } else {
                OspfNeighbor remoteOspfNeighbor = ospfNeighbor.getRemoteOspfNeighbor();

                String remoteHostname = remoteOspfNeighbor.getOwner().getHostname();
                if (!includeNodes2.contains(remoteHostname)) {
                  continue;
                }

                // check link cost mismatch
                int linkCost = ospfNeighbor.getIface().getOspfCost();
                int remoteLinkCost = remoteOspfNeighbor.getIface().getOspfCost();
                if (linkCost != remoteLinkCost) {
                  answerElement.add(
                      answerElement.getMismatchLinkCost(), hostname, vrfName, ospfNeighborSummary);
                }
              }
            }
          }
        }
      }

      return answerElement;
    }
  }

  // <question_page_comment>

  /**
   * Checks if OSPF sessions are correctly configured.
   *
   * <p>Details coming
   *
   * @type OspfSessionCheck multifile
   * @param foreignOspfNetworks Details coming.
   * @param node1Regex Regular expression to match the nodes names for one end of the sessions.
   *     Default is '.*' (all nodes).
   * @param node2Regex Regular expression to match the nodes names for the other end of the
   *     sessions. Default is '.*' (all nodes).
   * @example bf_answer("OspfSessionCheck", node1Regex="as1.*", node2Regex="as2.*") Checks all OSPF
   *     sessions between nodes that start with as1 and those that start with as2.
   */
  public static class OspfSessionCheckQuestion extends Question {

    private static final String PROP_FOREIGN_OSPF_NETWORKS = "foreignOspfNetworks";

    private static final String PROP_NODE1_REGEX = "node1Regex";

    private static final String PROP_NODE2_REGEX = "node2Regex";

    private SortedSet<Prefix> _foreignOspfNetworks;

    private NodesSpecifier _node1Regex;

    private NodesSpecifier _node2Regex;

    public OspfSessionCheckQuestion() {
      _foreignOspfNetworks = new TreeSet<>();
      _node1Regex = NodesSpecifier.ALL;
      _node2Regex = NodesSpecifier.ALL;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @JsonProperty(PROP_FOREIGN_OSPF_NETWORKS)
    public SortedSet<Prefix> getForeignOspfNetworks() {
      return _foreignOspfNetworks;
    }

    @Override
    public String getName() {
      return "ospfsessioncheck";
    }

    @JsonProperty(PROP_NODE1_REGEX)
    public NodesSpecifier getNode1Regex() {
      return _node1Regex;
    }

    @JsonProperty(PROP_NODE2_REGEX)
    public NodesSpecifier getNode2Regex() {
      return _node2Regex;
    }

    @JsonProperty(PROP_FOREIGN_OSPF_NETWORKS)
    public void setForeignOspfNetworks(SortedSet<Prefix> foreignOspfNetworks) {
      _foreignOspfNetworks = foreignOspfNetworks;
    }

    @JsonProperty(PROP_NODE1_REGEX)
    public void setNode1Regex(NodesSpecifier regex) {
      _node1Regex = regex;
    }

    @JsonProperty(PROP_NODE2_REGEX)
    public void setNode2Regex(NodesSpecifier regex) {
      _node2Regex = regex;
    }
  }

  @Override
  protected OspfSessionCheckAnswerer createAnswerer(Question question, IBatfish batfish) {
    return new OspfSessionCheckAnswerer(question, batfish);
  }

  @Override
  protected OspfSessionCheckQuestion createQuestion() {
    return new OspfSessionCheckQuestion();
  }
}
