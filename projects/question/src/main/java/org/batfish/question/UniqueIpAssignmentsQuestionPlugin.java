package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllInterfacesInterfaceSpecifier;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.InterfaceSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierContext;
import org.batfish.specifier.SpecifierFactories;

@AutoService(Plugin.class)
public class UniqueIpAssignmentsQuestionPlugin extends QuestionPlugin {

  public static class UniqueIpAssignmentsAnswerElement extends AnswerElement {
    private static final String PROP_DUPLICATE_IPS = "duplicateIps";

    private SortedMap<Ip, SortedSet<NodeInterfacePair>> _duplicateIps;

    public UniqueIpAssignmentsAnswerElement() {
      _summary = new AnswerSummary();
      _duplicateIps = new TreeMap<>();
    }

    @JsonProperty(PROP_DUPLICATE_IPS)
    public SortedMap<Ip, SortedSet<NodeInterfacePair>> getDuplicateIps() {
      return _duplicateIps;
    }

    private Object ipsToString() {
      StringBuilder sb = new StringBuilder("  Duplicate IPs\n");
      for (Ip ip : _duplicateIps.keySet()) {
        sb.append(String.format("    %s\n", ip));
        for (NodeInterfacePair nip : _duplicateIps.get(ip)) {
          sb.append(String.format("      %s\n", nip));
        }
      }
      return sb.toString();
    }

    @JsonProperty(PROP_DUPLICATE_IPS)
    public void setDuplicateIps(SortedMap<Ip, SortedSet<NodeInterfacePair>> duplicateIps) {
      _summary.setNumResults(duplicateIps.size());
      _duplicateIps = duplicateIps;
    }
  }

  public static class UniqueIpAssignmentsAnswerer extends Answerer {
    public UniqueIpAssignmentsAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {
      UniqueIpAssignmentsAnswerElement answerElement = new UniqueIpAssignmentsAnswerElement();
      answerElement.setDuplicateIps(getDuplicateIps());
      return answerElement;
    }

    private SortedMap<Ip, SortedSet<NodeInterfacePair>> getDuplicateIps() {
      UniqueIpAssignmentsQuestion question = (UniqueIpAssignmentsQuestion) _question;
      SpecifierContext ctxt = _batfish.specifierContext();
      Map<String, Configuration> configs = ctxt.getConfigs();
      Set<String> nodes = question.getNodeSpecifier().resolve(ctxt);
      // we do nodes and interfaces separately because of interface equality is currently broken
      // (does not take owner node into account)
      return nodes.stream()
          .flatMap(n -> question.getInterfaceSpecifier().resolve(ImmutableSet.of(n), ctxt).stream())
          .map(
              ifaceId ->
                  configs.get(ifaceId.getHostname()).getAllInterfaces().get(ifaceId.getInterface()))
          // narrow to interfaces of interest
          .filter(iface -> (!question.getEnabledIpsOnly() || iface.getActive()))
          // convert to stream of Entry<Ip, NodeInterfacePair>
          .flatMap(
              iface ->
                  iface.getAllConcreteAddresses().stream()
                      .map(
                          ifaceAdrr ->
                              Maps.immutableEntry(ifaceAdrr.getIp(), NodeInterfacePair.of(iface))))
          // group by Ip
          .collect(Multimaps.toMultimap(Entry::getKey, Entry::getValue, TreeMultimap::create))
          // convert to stream of Entry<Ip, Set<NodeInterfacePair>>
          .asMap()
          .entrySet()
          .stream()
          // narrow to entries with multiple NodeInterfacePairs
          .filter(entry -> entry.getValue().size() > 1)
          .collect(
              ImmutableSortedMap.toImmutableSortedMap(
                  Comparator.naturalOrder(),
                  Entry::getKey,
                  entry -> ImmutableSortedSet.copyOf(entry.getValue())));
    }
  }

  /**
   * Lists IP addresses that are assigned to multiple interfaces.
   *
   * <p>Except in cases of anycast, an IP address should be assigned to only one interface. This
   * question produces the list of IP addresses for which this condition does not hold.
   */
  @ParametersAreNonnullByDefault
  public static class UniqueIpAssignmentsQuestion extends Question {
    private static final String PROP_ENABLED_IPS_ONLY = "enabledIpsOnly";
    private static final String PROP_INTERFACES = "interfaces";
    private static final String PROP_NODES = "nodes";

    private final boolean _enabledIpsOnly;

    @Nullable private final String _interfaces;

    @Nullable private final String _nodes;

    @JsonCreator
    private static UniqueIpAssignmentsQuestion create(
        @Nullable @JsonProperty(PROP_ENABLED_IPS_ONLY) Boolean enabledIpsOnly,
        @Nullable @JsonProperty(PROP_INTERFACES) String interfaces,
        @Nullable @JsonProperty(PROP_NODES) String nodes) {
      return new UniqueIpAssignmentsQuestion(
          enabledIpsOnly != null && enabledIpsOnly, interfaces, nodes);
    }

    public UniqueIpAssignmentsQuestion(
        boolean enabledIpsOnly, @Nullable String interfaces, @Nullable String nodes) {
      _enabledIpsOnly = enabledIpsOnly;
      _interfaces = interfaces;
      _nodes = nodes;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "uniqueipassignments";
    }

    @JsonProperty(PROP_ENABLED_IPS_ONLY)
    public boolean getEnabledIpsOnly() {
      return _enabledIpsOnly;
    }

    @Nullable
    @JsonProperty(PROP_INTERFACES)
    public String getInterfaces() {
      return _interfaces;
    }

    @Nonnull
    @JsonIgnore
    InterfaceSpecifier getInterfaceSpecifier() {
      return SpecifierFactories.getInterfaceSpecifierOrDefault(
          _interfaces, AllInterfacesInterfaceSpecifier.INSTANCE);
    }

    @Nullable
    @JsonProperty(PROP_NODES)
    public String getNodes() {
      return _nodes;
    }

    @Nonnull
    @JsonIgnore
    NodeSpecifier getNodeSpecifier() {
      return SpecifierFactories.getNodeSpecifierOrDefault(_nodes, AllNodesNodeSpecifier.INSTANCE);
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new UniqueIpAssignmentsAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new UniqueIpAssignmentsQuestion(true, null, null);
  }
}
