package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AuthenticationKeyChain;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IkeGateway;
import org.batfish.datamodel.IkePolicy;
import org.batfish.datamodel.IkeProposal;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpsecPolicy;
import org.batfish.datamodel.IpsecProposal;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.collections.NamedStructureEquivalenceSets;
import org.batfish.datamodel.questions.INodeRegexQuestion;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.routing_policy.RoutingPolicy;

@AutoService(Plugin.class)
public class CompareSameNameQuestionPlugin extends QuestionPlugin {

  public static class CompareSameNameAnswerElement implements AnswerElement {

    private static final String PROP_EQUIVALENCE_SETS_MAP = "equivalenceSetsMap";

    private static final String PROP_NODES = "nodes";

    /** Equivalence sets are keyed by classname */
    private SortedMap<String, NamedStructureEquivalenceSets<?>> _equivalenceSets;

    private List<String> _nodes;

    public CompareSameNameAnswerElement() {
      _equivalenceSets = new TreeMap<>();
    }

    public void add(String className, NamedStructureEquivalenceSets<?> sets) {
      _equivalenceSets.put(className, sets);
    }

    private String equivalenceSetToString(
        String indent, String name, NamedStructureEquivalenceSets<?> nseSets) {
      StringBuilder sb = new StringBuilder(indent + name + "\n");
      sb.append(nseSets.prettyPrint(indent + indent));
      return sb.toString();
    }

    @JsonProperty(PROP_EQUIVALENCE_SETS_MAP)
    public SortedMap<String, NamedStructureEquivalenceSets<?>> getEquivalenceSets() {
      return _equivalenceSets;
    }

    @JsonProperty(PROP_NODES)
    public List<String> getNodes() {
      return _nodes;
    }

    @Override
    public String prettyPrint() {
      StringBuilder sb = new StringBuilder("Results for comparing same name structure\n");
      for (String name : _equivalenceSets.keySet()) {
        if (_equivalenceSets.get(name).size() > 0) {
          sb.append(equivalenceSetToString("  ", name, _equivalenceSets.get(name)));
        }
      }
      return sb.toString();
    }

    @JsonProperty(PROP_EQUIVALENCE_SETS_MAP)
    public void setEquivalenceSets(
        SortedMap<String, NamedStructureEquivalenceSets<?>> equivalenceSets) {
      _equivalenceSets = equivalenceSets;
    }

    @JsonProperty(PROP_NODES)
    public void setNodes(List<String> nodes) {
      _nodes = nodes;
    }
  }

  public static class CompareSameNameAnswerer extends Answerer {

    private CompareSameNameAnswerElement _answerElement;

    private Map<String, Configuration> _configurations;

    private boolean _missing;

    private Set<String> _namedStructTypes;

    private Set<String> _excludedNamedStructTypes;

    private List<String> _nodes;

    private boolean _singletons;

    public CompareSameNameAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    private <T> void add(
        Class<T> structureClass, Function<Configuration, Map<String, T>> structureMapRetriever) {
      String structType = structureClass.getSimpleName().toLowerCase();
      if ((_namedStructTypes.isEmpty() && !(_excludedNamedStructTypes.contains(structType)))
          || _namedStructTypes.contains(structType)) {
        _answerElement.add(
            structureClass.getSimpleName(),
            processStructures(structureClass, _nodes, _configurations, structureMapRetriever));
      }
    }

    @Override
    public CompareSameNameAnswerElement answer() {

      CompareSameNameQuestion question = (CompareSameNameQuestion) _question;
      _configurations = _batfish.loadConfigurations();
      // collect relevant nodes in a list.
      _nodes = CommonUtil.getMatchingStrings(question.getNodeRegex(), _configurations.keySet());
      _namedStructTypes =
          question
              .getNamedStructTypes()
              .stream()
              .map(String::toLowerCase)
              .collect(Collectors.toSet());
      _excludedNamedStructTypes =
          question
              .getExcludedNamedStructTypes()
              .stream()
              .map(String::toLowerCase)
              .collect(Collectors.toSet());
      _singletons = question.getSingletons();
      _missing = question.getMissing();

      _answerElement = new CompareSameNameAnswerElement();
      _answerElement.setNodes(_nodes);

      add(AsPathAccessList.class, Configuration::getAsPathAccessLists);
      add(AuthenticationKeyChain.class, Configuration::getAuthenticationKeyChains);
      add(CommunityList.class, Configuration::getCommunityLists);
      add(IkeGateway.class, Configuration::getIkeGateways);
      add(IkePolicy.class, Configuration::getIkePolicies);
      add(IkeProposal.class, Configuration::getIkeProposals);
      add(Interface.class, Configuration::getInterfaces);
      add(Ip6AccessList.class, Configuration::getIp6AccessLists);
      add(IpAccessList.class, Configuration::getIpAccessLists);
      add(IpsecPolicy.class, Configuration::getIpsecPolicies);
      add(IpsecProposal.class, Configuration::getIpsecProposals);
      add(IpsecVpn.class, Configuration::getIpsecVpns);
      add(Route6FilterList.class, Configuration::getRoute6FilterLists);
      add(RouteFilterList.class, Configuration::getRouteFilterLists);
      add(RoutingPolicy.class, Configuration::getRoutingPolicies);
      add(Vrf.class, Configuration::getVrfs);
      add(Zone.class, Configuration::getZones);

      return _answerElement;
    }

    private <T> NamedStructureEquivalenceSets<T> processStructures(
        Class<T> structureClass,
        List<String> hostnames,
        Map<String, Configuration> configurations,
        Function<Configuration, Map<String, T>> structureMapRetriever) {
      NamedStructureEquivalenceSets<T> ae =
          new NamedStructureEquivalenceSets<>(structureClass.getSimpleName());
      // collect the set of all names for structures of type T, across all nodes
      Set<String> allNames = new TreeSet<>();
      for (String hostname : hostnames) {
        Configuration node = configurations.get(hostname);
        Map<String, T> structureMap = structureMapRetriever.apply(node);
        allNames.addAll(structureMap.keySet());
      }
      for (String hostname : hostnames) {
        Configuration node = configurations.get(hostname);
        Map<String, T> structureMap = structureMapRetriever.apply(node);
        for (String structName : allNames) {
          if (structName.startsWith("~")) {
            continue;
          }
          T struct = structureMap.get(structName);
          if (struct != null || _missing) {
            ae.add(hostname, structName, struct);
          }
        }
      }
      if (!_singletons) {
        ae.clean();
      }
      return ae;
    }
  }

  // <question_page_comment>

  /**
   * Compares named structures with identical names across multiple nodes.
   *
   * <p>Named structures refer to constructs like route-maps and access-control lists. Often,
   * identical functionality is needed on multiple routers and it is common to have the same name
   * for those structures across routers. We compare the contents of structures with the same name
   * across different routers. When the contents of a same-named structure differ across routers, it
   * usually indicates a configuration error. For instance, if the ACL named
   * ``\verb|block_non_http_ssh|'' has identical content on nine out of ten routers, but is
   * different in the tenth router, the ACL is likely misconfigured on the tenth router.
   *
   * @type CompareSameName multifile
   * @param namedStructTypes Set of structure types to analyze drawn from ( AsPathAccessList,
   *     AuthenticationKeyChain, CommunityList, IkeGateway, IkePolicies, IkeProposal, Interface,
   *     Ip6AccessList, IpAccessList, IpsecPolicy, IpsecProposal, IpsecVpn, Route6FilterList,
   *     RouteFilterList, RoutingPolicy, Vrf, Zone ) Default value is '[]', which denotes all types
   *     except those in excludedNamedStructTypes.
   * @param excludedNamedStructTypes Set of structure types to omit from the analysis. Default is
   *     [Interface, Vrf].
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes).
   * @param singletons Defaults to false. Specifies whether or not to include named structures for
   *     which there is only one equivalence class.
   * @param missing Defaults to false. Specifies whether or not to create an equivalence class for
   *     nodes that are missing a structure of a given name.
   */
  public static final class CompareSameNameQuestion extends Question implements INodeRegexQuestion {

    private static final String PROP_MISSING = "missing";

    private static final String PROP_NAMED_STRUCT_TYPES = "namedStructTypes";

    private static final String PROP_EXCLUDED_NAMED_STRUCT_TYPES = "excludedNamedStructTypes";

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private static final String PROP_SINGLETONS = "singletons";

    private boolean _missing;

    private SortedSet<String> _namedStructTypes;

    private SortedSet<String> _excludedNamedStructTypes;

    private String _nodeRegex;

    private boolean _singletons;

    public CompareSameNameQuestion() {
      _namedStructTypes = new TreeSet<>();
      initExcludedNamedStructTypes();
      _nodeRegex = ".*";
    }

    // These named structure types seem to be less useful and have many entries
    // so slow down the computation considerably.  Therefore they are excluded
    // from the analysis by default.
    private void initExcludedNamedStructTypes() {
      _excludedNamedStructTypes = new TreeSet<>();
      _excludedNamedStructTypes.add(Interface.class.getSimpleName());
      _excludedNamedStructTypes.add(Vrf.class.getSimpleName());
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @JsonProperty(PROP_MISSING)
    public boolean getMissing() {
      return _missing;
    }

    @Override
    public String getName() {
      return "comparesamename";
    }

    @JsonProperty(PROP_NAMED_STRUCT_TYPES)
    public SortedSet<String> getNamedStructTypes() {
      return _namedStructTypes;
    }

    @JsonProperty(PROP_EXCLUDED_NAMED_STRUCT_TYPES)
    public SortedSet<String> getExcludedNamedStructTypes() {
      return _excludedNamedStructTypes;
    }

    @Override
    @JsonProperty(PROP_NODE_REGEX)
    public String getNodeRegex() {
      return _nodeRegex;
    }

    @JsonProperty(PROP_SINGLETONS)
    public boolean getSingletons() {
      return _singletons;
    }

    @Override
    public boolean getTraffic() {
      return false;
    }

    @JsonProperty(PROP_MISSING)
    public void setMissing(boolean missing) {
      _missing = missing;
    }

    @JsonProperty(PROP_NAMED_STRUCT_TYPES)
    public void setNamedStructTypes(SortedSet<String> namedStructTypes) {
      _namedStructTypes = namedStructTypes;
    }

    @JsonProperty(PROP_EXCLUDED_NAMED_STRUCT_TYPES)
    public void setExcludedNamedStructTypes(SortedSet<String> excludedNamedStructTypes) {
      _excludedNamedStructTypes = excludedNamedStructTypes;
    }

    @Override
    @JsonProperty(PROP_NODE_REGEX)
    public void setNodeRegex(String regex) {
      _nodeRegex = regex;
    }

    @JsonProperty(PROP_SINGLETONS)
    public void setSingletons(boolean singletons) {
      _singletons = singletons;
    }
  }

  @Override
  protected CompareSameNameAnswerer createAnswerer(Question question, IBatfish batfish) {
    return new CompareSameNameAnswerer(question, batfish);
  }

  @Override
  protected CompareSameNameQuestion createQuestion() {
    return new CompareSameNameQuestion();
  }
}
