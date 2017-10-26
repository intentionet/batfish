package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpNeighbor.BgpNeighborSummary;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class BgpSessionCheckQuestionPlugin extends QuestionPlugin {

  public static class BgpSessionCheckAnswerElement implements AnswerElement {

    private static final String PROP_ALL_BGP_NEIGHBORS = "allBgpNeighbors";

    private static final String PROP_BROKEN = "broken";

    private static final String PROP_EBGP_BROKEN = "ebgpBroken";

    private static final String PROP_EBGP_HALF_OPEN = "ebgpHalfOpen";

    private static final String PROP_EBGP_LOCAL_IP_ON_LOOPBACK = "ebgpLocalIpOnLoopback";

    private static final String PROP_EBGP_LOCAL_IP_UNKNOWN = "ebgpLocalIpUnknown";

    private static final String PROP_EBGP_MISSING_LOCAL_IP = "ebgpMissingLocalIp";

    private static final String PROP_EBGP_NON_UNIQUE_ENDPOINT = "ebgpNonUniqueEndpoint";

    private static final String PROP_EBGP_REMOTE_IP_ON_LOOPBACK = "ebgpRemoteIpOnLoopback";

    private static final String PROP_EBGP_REMOTE_IP_UNKNOWN = "ebgpRemoteIpUnknown";

    private static final String PROP_HALF_OPEN = "halfOpen";

    private static final String PROP_IBGP_BROKEN = "ibgpBroken";

    private static final String PROP_IBGP_HALF_OPEN = "ibgpHalfOpen";

    private static final String PROP_IBGP_REMOTE_IP_ON_NON_LOOPBACK = "ibgpLocalIpOnNonLoopback";

    private static final String PROP_IBGP_LOCAL_IP_UNKNOWN = "ibgpLocalIpUnknown";

    private static final String PROP_IBGP_MISSING_LOCAL_IP = "ibgpMissingLocalIp";

    private static final String PROP_IBGP_NON_UNIQUE_ENDPOINT = "ibgpNonUniqueEndpoint";

    private static final String PROP_IBGP_LOCAL_IP_ON_NON_LOOPBACK = "ibgpRemoteIpOnNonLoopback";

    private static final String PROP_IBGP_REMOTE_IP_UNKNOWN = "ibgpRemoteIpUnknown";

    private static final String PROP_IGNORED_FOREIGN_ENDPOINTS = "ignoredForeignEndpoints";

    private static final String PROP_LOCAL_IP_UNKNOWN = "localIpUnkown";

    private static final String PROP_MISSING_LOCAL_IP = "missingLocalIp";

    private static final String PROP_NON_UNIQUE_ENDPOINT = "nonUniqueEndpoint";

    private static final String PROP_REMOTE_IP_UNKNOWN = "remoteIpUnknown";

    private SortedMap<String, SortedMap<String, SortedMap<Prefix, BgpNeighborSummary>>>
        _allBgpNeighbors;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _broken;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ebgpBroken;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ebgpHalfOpen;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ebgpLocalIpOnLoopback;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ebgpLocalIpUnknown;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ebgpMissingLocalIp;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ebgpNonUniqueEndpoint;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ebgpRemoteIpOnLoopback;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ebgpRemoteIpUnknown;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _halfOpen;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ibgpBroken;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ibgpHalfOpen;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ibgpLocalIpOnNonLoopback;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ibgpLocalIpUnknown;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ibgpMissingLocalIp;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ibgpNonUniqueEndpoint;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ibgpRemoteIpOnNonLoopback;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ibgpRemoteIpUnknown;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _ignoredForeignEndpoints;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _localIpUnknown;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _missingLocalIp;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _nonUniqueEndpoint;

    private SortedMap<String, SortedMap<String, SortedSet<Prefix>>> _remoteIpUnknown;

    public BgpSessionCheckAnswerElement() {
      _allBgpNeighbors = new TreeMap<>();
      _broken = new TreeMap<>();
      _ebgpBroken = new TreeMap<>();
      _ibgpBroken = new TreeMap<>();
      _ebgpHalfOpen = new TreeMap<>();
      _ebgpLocalIpOnLoopback = new TreeMap<>();
      _ebgpLocalIpUnknown = new TreeMap<>();
      _ebgpMissingLocalIp = new TreeMap<>();
      _ebgpNonUniqueEndpoint = new TreeMap<>();
      _ebgpRemoteIpOnLoopback = new TreeMap<>();
      _ebgpRemoteIpUnknown = new TreeMap<>();
      _halfOpen = new TreeMap<>();
      _ibgpHalfOpen = new TreeMap<>();
      _ibgpLocalIpOnNonLoopback = new TreeMap<>();
      _ibgpLocalIpUnknown = new TreeMap<>();
      _ibgpMissingLocalIp = new TreeMap<>();
      _ibgpNonUniqueEndpoint = new TreeMap<>();
      _ibgpRemoteIpOnNonLoopback = new TreeMap<>();
      _ibgpRemoteIpUnknown = new TreeMap<>();
      _ignoredForeignEndpoints = new TreeMap<>();
      _localIpUnknown = new TreeMap<>();
      _missingLocalIp = new TreeMap<>();
      _nonUniqueEndpoint = new TreeMap<>();
      _remoteIpUnknown = new TreeMap<>();
    }

    public void add(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> neighborsByHostname,
        String hostname,
        String vrf,
        BgpNeighborSummary bgpNeighbor) {
      SortedMap<String, SortedSet<Prefix>> neighborsByVrf =
          neighborsByHostname.computeIfAbsent(hostname, k -> new TreeMap<>());
      SortedSet<Prefix> neighbors = neighborsByVrf.computeIfAbsent(vrf, k -> new TreeSet<>());
      Prefix prefix = bgpNeighbor.getPrefix();
      neighbors.add(prefix);
    }

    public void addToAll(
        SortedMap<String, SortedMap<String, SortedMap<Prefix, BgpNeighborSummary>>>
            neighborsByHostname,
        String hostname,
        String vrf,
        BgpNeighborSummary bgpNeighbor) {
      SortedMap<String, SortedMap<Prefix, BgpNeighborSummary>> neighborsByVrf =
          neighborsByHostname.computeIfAbsent(hostname, k -> new TreeMap<>());
      SortedMap<Prefix, BgpNeighborSummary> neighborsByPrefix =
          neighborsByVrf.computeIfAbsent(vrf, k -> new TreeMap<>());
      Prefix prefix = bgpNeighbor.getPrefix();
      neighborsByPrefix.put(prefix, bgpNeighbor);
    }

    @JsonProperty(PROP_ALL_BGP_NEIGHBORS)
    public SortedMap<String, SortedMap<String, SortedMap<Prefix, BgpNeighborSummary>>>
        getAllBgpNeighbors() {
      return _allBgpNeighbors;
    }

    @JsonProperty(PROP_BROKEN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getBroken() {
      return _broken;
    }

    @JsonProperty(PROP_EBGP_BROKEN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getEbgpBroken() {
      return _ebgpBroken;
    }

    @JsonProperty(PROP_EBGP_HALF_OPEN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getEbgpHalfOpen() {
      return _ebgpHalfOpen;
    }

    @JsonProperty(PROP_EBGP_LOCAL_IP_ON_LOOPBACK)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getEbgpLocalIpOnLoopback() {
      return _ebgpLocalIpOnLoopback;
    }

    @JsonProperty(PROP_EBGP_LOCAL_IP_UNKNOWN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getEbgpLocalIpUnknown() {
      return _ebgpLocalIpUnknown;
    }

    @JsonProperty(PROP_EBGP_MISSING_LOCAL_IP)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getEbgpMissingLocalIp() {
      return _ebgpMissingLocalIp;
    }

    @JsonProperty(PROP_EBGP_NON_UNIQUE_ENDPOINT)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getEbgpNonUniqueEndpoint() {
      return _ebgpNonUniqueEndpoint;
    }

    @JsonProperty(PROP_EBGP_REMOTE_IP_ON_LOOPBACK)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getEbgpRemoteIpOnLoopback() {
      return _ebgpRemoteIpOnLoopback;
    }

    @JsonProperty(PROP_EBGP_REMOTE_IP_UNKNOWN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getEbgpRemoteIpUnknown() {
      return _ebgpRemoteIpUnknown;
    }

    @JsonProperty(PROP_HALF_OPEN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getHalfOpen() {
      return _halfOpen;
    }

    @JsonProperty(PROP_IBGP_BROKEN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getIbgpBroken() {
      return _ibgpBroken;
    }

    @JsonProperty(PROP_IBGP_HALF_OPEN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getIbgpHalfOpen() {
      return _ibgpHalfOpen;
    }

    @JsonProperty(PROP_IBGP_LOCAL_IP_ON_NON_LOOPBACK)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getIbgpLocalIpOnNonLoopback() {
      return _ibgpLocalIpOnNonLoopback;
    }

    @JsonProperty(PROP_IBGP_LOCAL_IP_UNKNOWN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getIbgpLocalIpUnknown() {
      return _ibgpLocalIpUnknown;
    }

    @JsonProperty(PROP_IBGP_MISSING_LOCAL_IP)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getIbgpMissingLocalIp() {
      return _ibgpMissingLocalIp;
    }

    @JsonProperty(PROP_IBGP_NON_UNIQUE_ENDPOINT)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getIbgpNonUniqueEndpoint() {
      return _ibgpNonUniqueEndpoint;
    }

    @JsonProperty(PROP_IBGP_REMOTE_IP_ON_NON_LOOPBACK)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getIbgpRemoteIpOnNonLoopback() {
      return _ibgpRemoteIpOnNonLoopback;
    }

    @JsonProperty(PROP_IBGP_REMOTE_IP_UNKNOWN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getIbgpRemoteIpUnknown() {
      return _ibgpRemoteIpUnknown;
    }

    @JsonProperty(PROP_IGNORED_FOREIGN_ENDPOINTS)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getIgnoredForeignEndpoints() {
      return _ignoredForeignEndpoints;
    }

    @JsonProperty(PROP_LOCAL_IP_UNKNOWN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getLocalIpUnknown() {
      return _localIpUnknown;
    }

    @JsonProperty(PROP_MISSING_LOCAL_IP)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getMissingLocalIp() {
      return _missingLocalIp;
    }

    @JsonProperty(PROP_NON_UNIQUE_ENDPOINT)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getNonUniqueEndpoint() {
      return _nonUniqueEndpoint;
    }

    @JsonProperty(PROP_REMOTE_IP_UNKNOWN)
    public SortedMap<String, SortedMap<String, SortedSet<Prefix>>> getRemoteIpUnknown() {
      return _remoteIpUnknown;
    }

    @Override
    public String prettyPrint() {
      StringBuilder sb = new StringBuilder();
      sb.append(prettyPrintCategory(_ebgpLocalIpOnLoopback, PROP_EBGP_LOCAL_IP_ON_LOOPBACK));
      sb.append(prettyPrintCategory(_ebgpRemoteIpOnLoopback, PROP_EBGP_REMOTE_IP_ON_LOOPBACK));
      sb.append(prettyPrintCategory(_halfOpen, PROP_HALF_OPEN));
      sb.append(
          prettyPrintCategory(_ibgpLocalIpOnNonLoopback, PROP_IBGP_REMOTE_IP_ON_NON_LOOPBACK));
      sb.append(
          prettyPrintCategory(_ibgpRemoteIpOnNonLoopback, PROP_IBGP_LOCAL_IP_ON_NON_LOOPBACK));
      sb.append(prettyPrintCategory(_localIpUnknown, PROP_LOCAL_IP_UNKNOWN));
      sb.append(prettyPrintCategory(_missingLocalIp, PROP_MISSING_LOCAL_IP));
      sb.append(prettyPrintCategory(_nonUniqueEndpoint, PROP_NON_UNIQUE_ENDPOINT));
      sb.append(prettyPrintCategory(_remoteIpUnknown, PROP_REMOTE_IP_UNKNOWN));
      return sb.toString();
    }

    private CharSequence prettyPrintCategory(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> category, String name) {
      StringBuilder sb = new StringBuilder();
      if (!category.isEmpty()) {
        sb.append(name + ":\n");
        for (Entry<String, SortedMap<String, SortedSet<Prefix>>> e1 : category.entrySet()) {
          String hostname = e1.getKey();
          sb.append("  " + hostname + ":\n");
          SortedMap<String, SortedSet<Prefix>> neighborsByHostname = e1.getValue();
          for (Entry<String, SortedSet<Prefix>> e2 : neighborsByHostname.entrySet()) {
            String vrf = e2.getKey();
            sb.append("    " + vrf + ":\n");
            SortedSet<Prefix> neighbors = e2.getValue();
            for (Prefix prefix : neighbors) {
              BgpNeighborSummary summary = _allBgpNeighbors.get(hostname).get(vrf).get(prefix);
              int localAs = summary.getLocalAs();
              Ip localIp = summary.getLocalIp();
              int remoteAs = summary.getRemoteAs();
              Ip remoteIp = summary.getRemoteIp();
              String group = summary.getGroup();
              if (group == null) {
                group = "";
              }
              String description = summary.getDescription();
              if (description == null) {
                description = "";
              }
              sb.append(
                  String.format(
                      "      remoteIp: %-15s   remoteAs: %-5d   localIp: %-15s   localAs: %-5d   "
                          + "group: %s   desc: %s\n",
                      remoteIp, remoteAs, localIp, localAs, group, description));
            }
          }
        }
      }
      return sb;
    }

    @JsonProperty(PROP_ALL_BGP_NEIGHBORS)
    public void setAllBgpNeighbors(
        SortedMap<String, SortedMap<String, SortedMap<Prefix, BgpNeighborSummary>>>
            allBgpNeighbors) {
      _allBgpNeighbors = allBgpNeighbors;
    }

    @JsonProperty(PROP_BROKEN)
    public void setBroken(SortedMap<String, SortedMap<String, SortedSet<Prefix>>> broken) {
      _broken = broken;
    }

    @JsonProperty(PROP_EBGP_BROKEN)
    public void setEbgpBroken(SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ebgpBroken) {
      _ebgpBroken = ebgpBroken;
    }

    @JsonProperty(PROP_EBGP_HALF_OPEN)
    public void setEbgpHalfOpen(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ebgpHalfOpen) {
      _ebgpHalfOpen = ebgpHalfOpen;
    }

    @JsonProperty(PROP_EBGP_LOCAL_IP_ON_LOOPBACK)
    public void setEbgpLocalIpOnLoopback(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ebgpLocalIpOnLoopback) {
      _ebgpLocalIpOnLoopback = ebgpLocalIpOnLoopback;
    }

    @JsonProperty(PROP_EBGP_LOCAL_IP_UNKNOWN)
    public void setEbgpLocalIpUnknown(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ebgpLocalIpUnknown) {
      _ebgpLocalIpUnknown = ebgpLocalIpUnknown;
    }

    @JsonProperty(PROP_EBGP_MISSING_LOCAL_IP)
    public void setEbgpMissingLocalIp(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ebgpMissingLocalIp) {
      _ebgpMissingLocalIp = ebgpMissingLocalIp;
    }

    @JsonProperty(PROP_EBGP_NON_UNIQUE_ENDPOINT)
    public void setEbgpNonUniqueEndpoint(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ebgpNonUniqueEndpoint) {
      _ebgpNonUniqueEndpoint = ebgpNonUniqueEndpoint;
    }

    @JsonProperty(PROP_EBGP_REMOTE_IP_ON_LOOPBACK)
    public void setEbgpRemoteIpOnLoopback(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ebgpRemoteIpOnLoopback) {
      _ebgpRemoteIpOnLoopback = ebgpRemoteIpOnLoopback;
    }

    @JsonProperty(PROP_EBGP_REMOTE_IP_UNKNOWN)
    public void setEbgpRemoteIpUnknown(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ebgpRemoteIpUnknown) {
      _ebgpRemoteIpUnknown = ebgpRemoteIpUnknown;
    }

    @JsonProperty(PROP_HALF_OPEN)
    public void setHalfOpen(SortedMap<String, SortedMap<String, SortedSet<Prefix>>> halfOpen) {
      _halfOpen = halfOpen;
    }

    @JsonProperty(PROP_IBGP_BROKEN)
    public void setIbgpBroken(SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ibgpBroken) {
      _ibgpBroken = ibgpBroken;
    }

    @JsonProperty(PROP_IBGP_HALF_OPEN)
    public void setIbgpHalfOpen(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ibgpHalfOpen) {
      _ibgpHalfOpen = ibgpHalfOpen;
    }

    @JsonProperty(PROP_IBGP_LOCAL_IP_ON_NON_LOOPBACK)
    public void setIbgpLocalIpOnNonLoopback(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ibgpLocalIpOnNonLoopback) {
      _ibgpLocalIpOnNonLoopback = ibgpLocalIpOnNonLoopback;
    }

    @JsonProperty(PROP_IBGP_LOCAL_IP_UNKNOWN)
    public void setIbgpLocalIpUnknown(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ibgpLocalIpUnknown) {
      _ibgpLocalIpUnknown = ibgpLocalIpUnknown;
    }

    @JsonProperty(PROP_IBGP_MISSING_LOCAL_IP)
    public void setIbgpMissingLocalIp(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ibgpMissingLocalIp) {
      _ibgpMissingLocalIp = ibgpMissingLocalIp;
    }

    @JsonProperty(PROP_IBGP_NON_UNIQUE_ENDPOINT)
    public void setIbgpNonUniqueEndpoint(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ibgpNonUniqueEndpoint) {
      _ibgpNonUniqueEndpoint = ibgpNonUniqueEndpoint;
    }

    @JsonProperty(PROP_IBGP_REMOTE_IP_ON_NON_LOOPBACK)
    public void setIbgpRemoteIpOnNonLoopback(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ibgpRemoteIpOnNonLoopback) {
      _ibgpRemoteIpOnNonLoopback = ibgpRemoteIpOnNonLoopback;
    }

    @JsonProperty(PROP_IBGP_REMOTE_IP_UNKNOWN)
    public void setIbgpRemoteIpUnknown(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ibgpRemoteIpUnknown) {
      _ibgpRemoteIpUnknown = ibgpRemoteIpUnknown;
    }

    @JsonProperty(PROP_IGNORED_FOREIGN_ENDPOINTS)
    public void setIgnoredForeignEndpoints(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> ignoredForeignEndpoints) {
      _ignoredForeignEndpoints = ignoredForeignEndpoints;
    }

    @JsonProperty(PROP_LOCAL_IP_UNKNOWN)
    public void setLocalIpUnknown(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> localIpUnknown) {
      _localIpUnknown = localIpUnknown;
    }

    @JsonProperty(PROP_MISSING_LOCAL_IP)
    public void setMissingLocalIp(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> missingLocalIp) {
      _missingLocalIp = missingLocalIp;
    }

    @JsonProperty(PROP_NON_UNIQUE_ENDPOINT)
    public void setNonUniqueEndpoint(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> nonUniqueEndpoint) {
      _nonUniqueEndpoint = nonUniqueEndpoint;
    }

    @JsonProperty(PROP_REMOTE_IP_UNKNOWN)
    public void setRemoteIpUnknown(
        SortedMap<String, SortedMap<String, SortedSet<Prefix>>> remoteIpUnknown) {
      _remoteIpUnknown = remoteIpUnknown;
    }
  }

  public static class BgpSessionCheckAnswerer extends Answerer {

    public BgpSessionCheckAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {

      BgpSessionCheckQuestion question = (BgpSessionCheckQuestion) _question;

      Pattern node1Regex;
      Pattern node2Regex;
      try {
        node1Regex = Pattern.compile(question.getNode1Regex());
        node2Regex = Pattern.compile(question.getNode2Regex());
      } catch (PatternSyntaxException e) {
        throw new BatfishException(
            String.format(
                "One of the supplied regexes (%s  OR  %s) is not a valid java regex.",
                question.getNode1Regex(), question.getNode2Regex()),
            e);
      }
      Map<String, Configuration> configurations = _batfish.loadConfigurations();

      BgpSessionCheckAnswerElement answerElement = new BgpSessionCheckAnswerElement();
      Set<Ip> allInterfaceIps = new HashSet<>();
      Set<Ip> loopbackIps = new HashSet<>();
      Map<Ip, Set<String>> ipOwners = new HashMap<>();
      for (Configuration c : configurations.values()) {
        for (Interface i : c.getInterfaces().values()) {
          if (i.getActive() && i.getPrefix() != null) {
            for (Prefix prefix : i.getAllPrefixes()) {
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
      _batfish.initRemoteBgpNeighbors(configurations, ipOwners);
      for (Configuration co : configurations.values()) {
        String hostname = co.getHostname();
        if (!node1Regex.matcher(co.getHostname()).matches()) {
          continue;
        }
        for (Vrf vrf : co.getVrfs().values()) {
          String vrfName = vrf.getName();
          BgpProcess proc = vrf.getBgpProcess();

          if (proc != null) {
            for (BgpNeighbor bgpNeighbor : proc.getNeighbors().values()) {
              BgpNeighborSummary bgpNeighborSummary = new BgpNeighborSummary(bgpNeighbor);
              answerElement.addToAll(
                  answerElement.getAllBgpNeighbors(), hostname, vrfName, bgpNeighborSummary);
              boolean foreign =
                  bgpNeighbor.getGroup() != null
                      && question.getForeignBgpGroups().contains(bgpNeighbor.getGroup());
              boolean ebgp = !bgpNeighbor.getRemoteAs().equals(bgpNeighbor.getLocalAs());
              boolean ebgpMultihop = bgpNeighbor.getEbgpMultihop();
              Ip localIp = bgpNeighbor.getLocalIp();
              Ip remoteIp = bgpNeighbor.getAddress();
              if (bgpNeighbor.getPrefix().getPrefixLength() != 32) {
                continue;
              }
              if (ebgp) {
                if (!ebgpMultihop && loopbackIps.contains(localIp)) {
                  answerElement.add(
                      answerElement.getEbgpLocalIpOnLoopback(),
                      hostname,
                      vrfName,
                      bgpNeighborSummary);
                }
                if (localIp == null) {
                  answerElement.add(
                      answerElement.getBroken(), hostname, vrfName, bgpNeighborSummary);
                  answerElement.add(
                      answerElement.getMissingLocalIp(), hostname, vrfName, bgpNeighborSummary);
                  answerElement.add(
                      answerElement.getEbgpBroken(), hostname, vrfName, bgpNeighborSummary);
                  answerElement.add(
                      answerElement.getEbgpMissingLocalIp(), hostname, vrfName, bgpNeighborSummary);
                }
              } else {
                // ibgp
                if (!loopbackIps.contains(localIp)) {
                  answerElement.add(
                      answerElement.getIbgpLocalIpOnNonLoopback(),
                      hostname,
                      vrfName,
                      bgpNeighborSummary);
                }
                if (localIp == null) {
                  answerElement.add(
                      answerElement.getBroken(), hostname, vrfName, bgpNeighborSummary);
                  answerElement.add(
                      answerElement.getMissingLocalIp(), hostname, vrfName, bgpNeighborSummary);
                  answerElement.add(
                      answerElement.getIbgpBroken(), hostname, vrfName, bgpNeighborSummary);
                  answerElement.add(
                      answerElement.getIbgpMissingLocalIp(), hostname, vrfName, bgpNeighborSummary);
                }
              }
              if (foreign) {
                answerElement.add(
                    answerElement.getIgnoredForeignEndpoints(),
                    hostname,
                    vrfName,
                    bgpNeighborSummary);
              } else {
                // not foreign
                if (ebgp) {
                  if (localIp != null && !allInterfaceIps.contains(localIp)) {
                    answerElement.add(
                        answerElement.getBroken(), hostname, vrfName, bgpNeighborSummary);
                    answerElement.add(
                        answerElement.getLocalIpUnknown(), hostname, vrfName, bgpNeighborSummary);
                    answerElement.add(
                        answerElement.getEbgpBroken(), hostname, vrfName, bgpNeighborSummary);
                    answerElement.add(
                        answerElement.getEbgpLocalIpUnknown(),
                        hostname,
                        vrfName,
                        bgpNeighborSummary);
                  }
                  if (!allInterfaceIps.contains(remoteIp)) {
                    answerElement.add(
                        answerElement.getBroken(), hostname, vrfName, bgpNeighborSummary);
                    answerElement.add(
                        answerElement.getRemoteIpUnknown(), hostname, vrfName, bgpNeighborSummary);
                    answerElement.add(
                        answerElement.getEbgpBroken(), hostname, vrfName, bgpNeighborSummary);
                    answerElement.add(
                        answerElement.getEbgpRemoteIpUnknown(),
                        hostname,
                        vrfName,
                        bgpNeighborSummary);
                  } else {
                    if (!ebgpMultihop
                        && loopbackIps.contains(remoteIp)
                        && node2RegexMatchesIp(remoteIp, ipOwners, node2Regex)) {
                      answerElement.add(
                          answerElement.getEbgpRemoteIpOnLoopback(),
                          hostname,
                          vrfName,
                          bgpNeighborSummary);
                    }
                  }
                  // check half open
                  if (localIp != null
                      && allInterfaceIps.contains(remoteIp)
                      && node2RegexMatchesIp(remoteIp, ipOwners, node2Regex)) {
                    if (bgpNeighbor.getRemoteBgpNeighbor() == null) {
                      answerElement.add(
                          answerElement.getBroken(), hostname, vrfName, bgpNeighborSummary);
                      answerElement.add(
                          answerElement.getHalfOpen(), hostname, vrfName, bgpNeighborSummary);
                      answerElement.add(
                          answerElement.getEbgpBroken(), hostname, vrfName, bgpNeighborSummary);
                      answerElement.add(
                          answerElement.getEbgpHalfOpen(), hostname, vrfName, bgpNeighborSummary);
                    } else if (bgpNeighbor.getCandidateRemoteBgpNeighbors().size() != 1) {
                      answerElement.add(
                          answerElement.getNonUniqueEndpoint(),
                          hostname,
                          vrfName,
                          bgpNeighborSummary);
                      answerElement.add(
                          answerElement.getEbgpNonUniqueEndpoint(),
                          hostname,
                          vrfName,
                          bgpNeighborSummary);
                    }
                  }
                } else {
                  // ibgp
                  if (localIp != null && !allInterfaceIps.contains(localIp)) {
                    answerElement.add(
                        answerElement.getBroken(), hostname, vrfName, bgpNeighborSummary);
                    answerElement.add(
                        answerElement.getIbgpBroken(), hostname, vrfName, bgpNeighborSummary);
                    answerElement.add(
                        answerElement.getIbgpLocalIpUnknown(),
                        hostname,
                        vrfName,
                        bgpNeighborSummary);
                  }
                  if (!allInterfaceIps.contains(remoteIp)) {
                    answerElement.add(
                        answerElement.getBroken(), hostname, vrfName, bgpNeighborSummary);
                    answerElement.add(
                        answerElement.getIbgpBroken(), hostname, vrfName, bgpNeighborSummary);
                    answerElement.add(
                        answerElement.getIbgpRemoteIpUnknown(),
                        hostname,
                        vrfName,
                        bgpNeighborSummary);
                  } else {
                    if (!loopbackIps.contains(remoteIp)
                        && node2RegexMatchesIp(remoteIp, ipOwners, node2Regex)) {
                      answerElement.add(
                          answerElement.getIbgpRemoteIpOnNonLoopback(),
                          hostname,
                          vrfName,
                          bgpNeighborSummary);
                    }
                  }
                  if (localIp != null
                      && allInterfaceIps.contains(remoteIp)
                      && node2RegexMatchesIp(remoteIp, ipOwners, node2Regex)) {
                    if (bgpNeighbor.getRemoteBgpNeighbor() == null) {
                      answerElement.add(
                          answerElement.getBroken(), hostname, vrfName, bgpNeighborSummary);
                      answerElement.add(
                          answerElement.getHalfOpen(), hostname, vrfName, bgpNeighborSummary);
                      answerElement.add(
                          answerElement.getIbgpBroken(), hostname, vrfName, bgpNeighborSummary);
                      answerElement.add(
                          answerElement.getIbgpHalfOpen(), hostname, vrfName, bgpNeighborSummary);
                    } else if (bgpNeighbor.getCandidateRemoteBgpNeighbors().size() != 1) {
                      answerElement.add(
                          answerElement.getNonUniqueEndpoint(),
                          hostname,
                          vrfName,
                          bgpNeighborSummary);
                      answerElement.add(
                          answerElement.getIbgpNonUniqueEndpoint(),
                          hostname,
                          vrfName,
                          bgpNeighborSummary);
                    }
                  }
                }
              }
            }
          }
        }
      }
      return answerElement;
    }

    private boolean node2RegexMatchesIp(Ip ip, Map<Ip, Set<String>> ipOwners, Pattern node2Regex) {
      Set<String> owners = ipOwners.get(ip);
      if (owners == null) {
        throw new BatfishException("Expected at least one owner of ip: " + ip);
      }
      for (String owner : owners) {
        if (node2Regex.matcher(owner).matches()) {
          return true;
        }
      }
      return false;
    }
  }

  // <question_page_comment>

  /**
   * Checks if BGP sessions are correctly configured.
   *
   * <p>Details coming
   *
   * @type BgpSessionCheck multifile
   * @param foreignBgpGroups Details coming.
   * @param node1Regex Regular expression to match the nodes names for one end of the sessions.
   *     Default is '.*' (all nodes).
   * @param node2Regex Regular expression to match the nodes names for the other end of the
   *     sessions. Default is '.*' (all nodes).
   * @example bf_answer("BgpSessionCheck", node1Regex="as1.*", node2Regex="as2.*") Checks all BGP
   *     sessions between nodes that start with as1 and those that start with as2.
   */
  public static class BgpSessionCheckQuestion extends Question {

    private static final String PROP_FOREIGN_BGP_GROUPS = "foreignBgpGroups";

    private static final String PROP_NODE1_REGEX = "node1Regex";

    private static final String PROP_NODE2_REGEX = "node2Regex";

    private SortedSet<String> _foreignBgpGroups;

    private String _node1Regex;

    private String _node2Regex;

    public BgpSessionCheckQuestion() {
      _foreignBgpGroups = new TreeSet<>();
      _node1Regex = ".*";
      _node2Regex = ".*";
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @JsonProperty(PROP_FOREIGN_BGP_GROUPS)
    public SortedSet<String> getForeignBgpGroups() {
      return _foreignBgpGroups;
    }

    @Override
    public String getName() {
      return "bgpsessioncheck";
    }

    @JsonProperty(PROP_NODE1_REGEX)
    public String getNode1Regex() {
      return _node1Regex;
    }

    @JsonProperty(PROP_NODE2_REGEX)
    public String getNode2Regex() {
      return _node2Regex;
    }

    @Override
    public boolean getTraffic() {
      return false;
    }

    @JsonProperty(PROP_FOREIGN_BGP_GROUPS)
    public void setForeignBgpGroups(SortedSet<String> foreignBgpGroups) {
      _foreignBgpGroups = foreignBgpGroups;
    }

    @JsonProperty(PROP_NODE1_REGEX)
    public void setNode1Regex(String regex) {
      _node1Regex = regex;
    }

    @JsonProperty(PROP_NODE2_REGEX)
    public void setNode2Regex(String regex) {
      _node2Regex = regex;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new BgpSessionCheckAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new BgpSessionCheckQuestion();
  }
}
