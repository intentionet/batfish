package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.collections.NamedStructureEquivalenceSet;
import org.batfish.datamodel.collections.NamedStructureEquivalenceSets;
import org.batfish.datamodel.collections.NamedStructureOutlierSet;
import org.batfish.datamodel.collections.OutlierSet;
import org.batfish.datamodel.questions.INodeRegexQuestion;
import org.batfish.datamodel.questions.Question;
import org.batfish.role.OutliersHypothesis;

@AutoService(Plugin.class)
public class OutliersQuestionPlugin extends QuestionPlugin {

  public static class OutliersAnswerElement implements AnswerElement {

    private static final String PROP_NAMED_STRUCTURE_OUTLIERS = "namedStructureOutliers";

    private static final String PROP_SERVER_OUTLIERS = "serverOutliers";

    private SortedSet<NamedStructureOutlierSet<?>> _namedStructureOutliers;

    private SortedSet<OutlierSet<NavigableSet<String>>> _serverOutliers;

    public OutliersAnswerElement() {
      _namedStructureOutliers = new TreeSet<>();
      _serverOutliers = new TreeSet<>();
    }

    @JsonProperty(PROP_NAMED_STRUCTURE_OUTLIERS)
    public SortedSet<NamedStructureOutlierSet<?>> getNamedStructureOutliers() {
      return _namedStructureOutliers;
    }

    @JsonProperty(PROP_SERVER_OUTLIERS)
    public SortedSet<OutlierSet<NavigableSet<String>>> getServerOutliers() {
      return _serverOutliers;
    }

    @Override
    public String prettyPrint() {
      if (_namedStructureOutliers.size() == 0 && _serverOutliers.size() == 0) {
        return "";
      }

      StringBuilder sb = new StringBuilder("Results for outliers\n");

      for (OutlierSet<?> outlier : _serverOutliers) {
        sb.append("  Hypothesis: every node has the following set of ");
        sb.append(outlier.getName() + ": " + outlier.getDefinition() + "\n");
        sb.append("  Outliers: ");
        sb.append(outlier.getOutliers() + "\n");
        sb.append("  Conformers: ");
        sb.append(outlier.getConformers() + "\n\n");
      }

      for (NamedStructureOutlierSet<?> outlier : _namedStructureOutliers) {
        switch (outlier.getHypothesis()) {
          case SAME_DEFINITION:
            sb.append(
                "  Hypothesis: every "
                    + outlier.getStructType()
                    + " named "
                    + outlier.getName()
                    + " has the same definition\n");
            break;
          case SAME_NAME:
            sb.append("  Hypothesis:");
            if (outlier.getNamedStructure() != null) {
              sb.append(" every ");
            } else {
              sb.append(" no ");
            }
            sb.append(
                "node should define a "
                    + outlier.getStructType()
                    + " named "
                    + outlier.getName()
                    + "\n");
            break;
          default:
            throw new BatfishException("Unexpected hypothesis" + outlier.getHypothesis());
        }
        sb.append("  Outliers: ");
        sb.append(outlier.getOutliers() + "\n");
        sb.append("  Conformers: ");
        sb.append(outlier.getConformers() + "\n\n");
      }
      return sb.toString();
    }

    @JsonProperty(PROP_NAMED_STRUCTURE_OUTLIERS)
    public void setNamedStructureOutliers(
        SortedSet<NamedStructureOutlierSet<?>> namedStructureOutliers) {
      _namedStructureOutliers = namedStructureOutliers;
    }

    @JsonProperty(PROP_SERVER_OUTLIERS)
    public void setServerOutliers(SortedSet<OutlierSet<NavigableSet<String>>> serverOutliers) {
      _serverOutliers = serverOutliers;
    }
  }

  public static class OutliersAnswerer extends Answerer {

    private OutliersAnswerElement _answerElement;

    private Map<String, Configuration> _configurations;

    // the node names that match the question's node regex
    private List<String> _nodes;

    // only report outliers that represent this percentage or less of
    // the total number of nodes
    private static double OUTLIERS_THRESHOLD = 1.0 / 3.0;

    public OutliersAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    private <T> void addNamedStructureOutliers(
        OutliersHypothesis hypothesis,
        NamedStructureEquivalenceSets<T> equivSet,
        SortedSet<NamedStructureOutlierSet<?>> rankedOutliers) {
      String structType = equivSet.getStructureClassName();
      for (Map.Entry<String, SortedSet<NamedStructureEquivalenceSet<T>>> entry :
          equivSet.getSameNamedStructures().entrySet()) {
        String name = entry.getKey();
        SortedSet<NamedStructureEquivalenceSet<T>> eClasses = entry.getValue();
        NamedStructureEquivalenceSet<T> max =
            eClasses
                .stream()
                .max(Comparator.comparingInt(es -> es.getNodes().size()))
                .orElseThrow(
                    () ->
                        new BatfishException(
                            "Named structure " + name + " has no equivalence classes"));
        SortedSet<String> conformers = max.getNodes();
        eClasses.remove(max);
        SortedSet<String> outliers = new TreeSet<>();
        for (NamedStructureEquivalenceSet<T> eClass : eClasses) {
          outliers.addAll(eClass.getNodes());
        }
        rankedOutliers.add(
            new NamedStructureOutlierSet<>(
                hypothesis, structType, name, max.getNamedStructure(), conformers, outliers));
      }
    }

    private <T> void addPropertyOutliers(
        String name, Function<Configuration, T> accessor, SortedSet<OutlierSet<T>> rankedOutliers) {

      // partition the nodes into equivalence classes based on their values for the
      // property of interest
      Map<T, SortedSet<String>> equivSets = new HashMap<>();
      for (String node : _nodes) {
        T definition = accessor.apply(_configurations.get(node));
        SortedSet<String> matchingNodes = equivSets.getOrDefault(definition, new TreeSet<>());
        matchingNodes.add(node);
        equivSets.put(definition, matchingNodes);
      }

      // the equivalence class of the largest size is treated as the one whose value is
      // hypothesized to be the correct one
      Map.Entry<T, SortedSet<String>> max =
          equivSets
              .entrySet()
              .stream()
              .max(Comparator.comparingInt(e -> e.getValue().size()))
              .orElseThrow(
                  () -> new BatfishException("Set " + name + " has no equivalence classes"));
      SortedSet<String> conformers = max.getValue();
      T definition = max.getKey();
      equivSets.remove(definition);
      SortedSet<String> outliers = new TreeSet<>();
      for (SortedSet<String> nodes : equivSets.values()) {
        outliers.addAll(nodes);
      }
      if (outliers.size() > 0 && isWithinThreshold(conformers, outliers)) {
        rankedOutliers.add(new OutlierSet<T>(name, definition, conformers, outliers));
      }
    }

    @Override
    public OutliersAnswerElement answer() {

      OutliersQuestion question = (OutliersQuestion) _question;
      _answerElement = new OutliersAnswerElement();

      _configurations = _batfish.loadConfigurations();
      _nodes = CommonUtil.getMatchingStrings(question.getNodeRegex(), _configurations.keySet());

      switch (question.getHypothesis()) {
        case SAME_DEFINITION:
        case SAME_NAME:
          SortedSet<NamedStructureOutlierSet<?>> outliers = namedStructureOutliers(question);
          _answerElement.setNamedStructureOutliers(outliers);
          break;
        case SAME_SERVERS:
          _answerElement.setServerOutliers(serverOutliers());
          break;
        default:
          throw new BatfishException(
              "Unexpected outlier detection hypothesis " + question.getHypothesis());
      }

      return _answerElement;
    }

    private static boolean isWithinThreshold(
        SortedSet<String> conformers, SortedSet<String> outliers) {
      double cSize = conformers.size();
      double oSize = outliers.size();
      return (oSize / (cSize + oSize)) <= OUTLIERS_THRESHOLD;
    }

    private SortedSet<NamedStructureOutlierSet<?>> namedStructureOutliers(
        OutliersQuestion question) {

      // first get the results of compareSameName
      CompareSameNameQuestionPlugin.CompareSameNameQuestion inner =
          new CompareSameNameQuestionPlugin.CompareSameNameQuestion();
      inner.setNodeRegex(question.getNodeRegex());
      inner.setNamedStructTypes(question.getNamedStructTypes());
      inner.setExcludedNamedStructTypes(new TreeSet<>());
      inner.setSingletons(true);
      CompareSameNameQuestionPlugin.CompareSameNameAnswerer innerAnswerer =
          new CompareSameNameQuestionPlugin().createAnswerer(inner, _batfish);
      CompareSameNameQuestionPlugin.CompareSameNameAnswerElement innerAnswer =
          innerAnswerer.answer();

      SortedMap<String, NamedStructureEquivalenceSets<?>> equivalenceSets =
          innerAnswer.getEquivalenceSets();

      OutliersHypothesis hypothesis = question.getHypothesis();
      switch (hypothesis) {
        case SAME_DEFINITION:
          // nothing to do before ranking outliers
          break;
        case SAME_NAME:
          // create at most two equivalence classes for each name:
          // one containing the nodes that have a structure of that name,
          // and one containing the nodes that don't have a structure of that name
          for (NamedStructureEquivalenceSets<?> eSets : equivalenceSets.values()) {
            toNameOnlyEquivalenceSets(eSets, innerAnswer.getNodes());
          }
          break;
        default:
          throw new BatfishException("Default case of switch should be unreachable");
      }

      for (NamedStructureEquivalenceSets<?> eSets : equivalenceSets.values()) {
        eSets.clean();
      }

      SortedSet<NamedStructureOutlierSet<?>> outliers =
          rankNamedStructureOutliers(hypothesis, equivalenceSets);

      // remove outlier sets that don't meet our threshold
      outliers.removeIf(oset -> !isWithinThreshold(oset.getConformers(), oset.getOutliers()));
      return outliers;
    }

    private SortedSet<OutlierSet<NavigableSet<String>>> serverOutliers() {
      SortedSet<OutlierSet<NavigableSet<String>>> rankedOutliers = new TreeSet<>();
      addPropertyOutliers("DnsServers", Configuration::getDnsServers, rankedOutliers);
      addPropertyOutliers("LoggingServers", Configuration::getLoggingServers, rankedOutliers);
      addPropertyOutliers("NtpServers", Configuration::getNtpServers, rankedOutliers);
      addPropertyOutliers("SnmpTrapServers", Configuration::getSnmpTrapServers, rankedOutliers);
      addPropertyOutliers("TacacsServers", Configuration::getTacacsServers, rankedOutliers);

      return rankedOutliers;
    }

    private <T> void toNameOnlyEquivalenceSets(
        NamedStructureEquivalenceSets<T> eSets, List<String> nodes) {
      SortedMap<String, SortedSet<NamedStructureEquivalenceSet<T>>> newESetsMap = new TreeMap<>();
      for (Map.Entry<String, SortedSet<NamedStructureEquivalenceSet<T>>> entry :
          eSets.getSameNamedStructures().entrySet()) {
        SortedSet<String> presentNodes = new TreeSet<>();
        T struct = entry.getValue().first().getNamedStructure();
        for (NamedStructureEquivalenceSet<T> eSet : entry.getValue()) {
          presentNodes.addAll(eSet.getNodes());
        }
        SortedSet<String> absentNodes = new TreeSet<>(nodes);
        absentNodes.removeAll(presentNodes);
        SortedSet<NamedStructureEquivalenceSet<T>> newESets = new TreeSet<>();
        NamedStructureEquivalenceSet<T> presentSet =
            new NamedStructureEquivalenceSet<T>(presentNodes.first(), struct);
        presentSet.setNodes(presentNodes);
        newESets.add(presentSet);
        if (absentNodes.size() > 0) {
          NamedStructureEquivalenceSet<T> absentSet =
              new NamedStructureEquivalenceSet<T>(absentNodes.first());
          absentSet.setNodes(absentNodes);
          newESets.add(absentSet);
        }
        newESetsMap.put(entry.getKey(), newESets);
      }
      eSets.setSameNamedStructures(newESetsMap);
    }

    /* a simple first approach to detect and rank outliers:
     * compute the z-score (see Engler's 2001 paper on detecting outliers) for each
     * <structure type, name> pair, based on a hypothesis that the equivalence class
     * with the largest number of elements is correct and the property equivalence classes
     * represent bugs
     */
    private SortedSet<NamedStructureOutlierSet<?>> rankNamedStructureOutliers(
        OutliersHypothesis hypothesis,
        SortedMap<String, NamedStructureEquivalenceSets<?>> equivSets) {
      SortedSet<NamedStructureOutlierSet<?>> rankedOutliers = new TreeSet<>();
      for (NamedStructureEquivalenceSets<?> entry : equivSets.values()) {
        addNamedStructureOutliers(hypothesis, entry, rankedOutliers);
      }
      return rankedOutliers;
    }
  }

  // <question_page_comment>
  /**
   * Detects and ranks outliers based on differences in named structures.
   *
   * <p>If many nodes have a structure of a given name and a few do not, this may indicate an error.
   * If many nodes have a structure named N whose definition is identical, and a few nodes have a
   * structure named N that is defined differently, this may indicate an error. This question
   * leverages this and similar intuition to find outliers.
   *
   * @type InferRoles multifile
   * @param namedStructTypes Set of structure types to analyze drawn from ( AsPathAccessList,
   *     AuthenticationKeyChain, CommunityList, IkeGateway, IkePolicies, IkeProposal, IpAccessList,
   *     IpsecPolicy, IpsecProposal, IpsecVpn, RouteFilterList, RoutingPolicy) Default value is '[]'
   *     (which denotes all structure types). This option is applicable to the "sameName" and
   *     "sameDefinition" hypotheses.
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes).
   * @param hypothesis A string that indicates the hypothesis being used to identify outliers.
   *     "sameDefinition" indicates a hypothesis that same-named structures should have identical
   *     definitions. "sameName" indicates a hypothesis that all nodes should have structures of the
   *     same names. "sameServers" indicates a hypothesis that all nodes should have the same set of
   *     protocol-specific servers (e.g., DNS servers). Default is "sameDefinition".
   */
  public static final class OutliersQuestion extends Question implements INodeRegexQuestion {

    private static final String PROP_HYPOTHESIS = "hypothesis";

    private static final String PROP_NAMED_STRUCT_TYPES = "namedStructTypes";

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private OutliersHypothesis _hypothesis;

    private SortedSet<String> _namedStructTypes;

    private String _nodeRegex;

    public OutliersQuestion() {
      _namedStructTypes = new TreeSet<>();
      _nodeRegex = ".*";
      _hypothesis = OutliersHypothesis.SAME_DEFINITION;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @JsonProperty(PROP_HYPOTHESIS)
    public OutliersHypothesis getHypothesis() {
      return _hypothesis;
    }

    @Override
    public String getName() {
      return "outliers";
    }

    @JsonProperty(PROP_NAMED_STRUCT_TYPES)
    public SortedSet<String> getNamedStructTypes() {
      return _namedStructTypes;
    }

    @Override
    @JsonProperty(PROP_NODE_REGEX)
    public String getNodeRegex() {
      return _nodeRegex;
    }

    @Override
    public boolean getTraffic() {
      return false;
    }

    @JsonProperty(PROP_HYPOTHESIS)
    public void setHypothesis(OutliersHypothesis hypothesis) {
      _hypothesis = hypothesis;
    }

    @JsonProperty(PROP_NAMED_STRUCT_TYPES)
    public void setNamedStructTypes(SortedSet<String> namedStructTypes) {
      _namedStructTypes = namedStructTypes;
    }

    @Override
    @JsonProperty(PROP_NODE_REGEX)
    public void setNodeRegex(String regex) {
      _nodeRegex = regex;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new OutliersAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new OutliersQuestion();
  }
}
