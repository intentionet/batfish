package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.collections.NamedStructureEquivalenceSet;
import org.batfish.datamodel.collections.NamedStructureEquivalenceSets;
import org.batfish.datamodel.collections.NamedStructureOutlierSet;
import org.batfish.datamodel.collections.OutlierSet;
import org.batfish.datamodel.questions.INodeRegexQuestion;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.role.OutliersHypothesis;

@AutoService(Plugin.class)
public class OutliersQuestionPlugin extends QuestionPlugin {

  public static class OutliersAnswerElement extends AnswerElement {

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
        sb.append("  Hypothesis: every node should have the following set of ");
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
                    + " should have the same definition\n");
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
    private Set<String> _nodes;

    // only report outliers that represent this percentage or less of
    // the total number of nodes
    private static double OUTLIERS_THRESHOLD = 0.34;

    // if this flag is true, report all outliers, even those that exceed the threshold above,
    // and including situations when there are zero outliers
    private boolean _verbose;

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
            eClasses.stream()
                .max(Comparator.comparingInt(es -> es.getNodes().size()))
                .orElseThrow(
                    () ->
                        new BatfishException(
                            "Named structure " + name + " has no equivalence classes"));
        SortedSet<String> conformers = max.getNodes();

        SortedSet<String> outliers =
            eClasses.stream()
                .filter(eClass -> eClass != max)
                .flatMap(eClass -> eClass.getNodes().stream())
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
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
          equivSets.entrySet().stream()
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
      if (_verbose || isWithinThreshold(conformers, outliers)) {
        rankedOutliers.add(new OutlierSet<>(name, definition, conformers, outliers));
      }
    }

    @Override
    public OutliersAnswerElement answer() {

      OutliersQuestion question = (OutliersQuestion) _question;
      _answerElement = new OutliersAnswerElement();

      _configurations = _batfish.loadConfigurations();
      _nodes = question.getNodeRegex().getMatchingNodes(_batfish);
      _verbose = question.getVerbose();

      switch (question.getHypothesis()) {
        case SAME_DEFINITION:
        case SAME_NAME:
          SortedSet<NamedStructureOutlierSet<?>> outliers = namedStructureOutliers(question);
          _answerElement.setNamedStructureOutliers(outliers);
          break;
        case SAME_SERVERS:
          _answerElement.setServerOutliers(serverOutliers(question));
          break;
        default:
          throw new BatfishException(
              "Unexpected outlier detection hypothesis " + question.getHypothesis());
      }

      return _answerElement;
    }

    // check that there is at least one outlier, but also that the fraction of outliers
    // does not exceed our threshold for reporting
    private static boolean isWithinThreshold(
        SortedSet<String> conformers, SortedSet<String> outliers) {
      double cSize = conformers.size();
      double oSize = outliers.size();
      return oSize > 0 && (oSize / (cSize + oSize)) <= OUTLIERS_THRESHOLD;
    }

    private SortedSet<NamedStructureOutlierSet<?>> namedStructureOutliers(
        OutliersQuestion question) {

      // first get the results of compareSameName
      CompareSameNameQuestionPlugin.CompareSameNameQuestion inner =
          new CompareSameNameQuestionPlugin.CompareSameNameQuestion(
              null,
              new TreeSet<>(),
              null,
              question.getNamedStructTypes(),
              question.getNodeRegex(),
              true);
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

      SortedSet<NamedStructureOutlierSet<?>> outliers =
          rankNamedStructureOutliers(hypothesis, equivalenceSets);

      if (!_verbose) {
        // remove outlier sets where the hypothesis is that a particular named structure
        // should *not* exist (this  happens when more nodes lack such a structure than contain
        // such a structure).  such hypotheses do not seem to be useful in general.
        outliers.removeIf(oset -> oset.getNamedStructure() == null);

        // remove outlier sets that don't meet our threshold
        outliers.removeIf(oset -> !isWithinThreshold(oset.getConformers(), oset.getOutliers()));
      }
      return outliers;
    }

    private SortedSet<OutlierSet<NavigableSet<String>>> serverOutliers(OutliersQuestion question) {
      SortedSet<String> serverSets = new TreeSet<>(question.getServerSets());

      SortedMap<String, Function<Configuration, NavigableSet<String>>> serverSetAccessors =
          new TreeMap<>();
      serverSetAccessors.put("DnsServers", Configuration::getDnsServers);
      serverSetAccessors.put("LoggingServers", Configuration::getLoggingServers);
      serverSetAccessors.put("NtpServers", Configuration::getNtpServers);
      serverSetAccessors.put("SnmpTrapServers", Configuration::getSnmpTrapServers);
      serverSetAccessors.put("TacacsServers", Configuration::getTacacsServers);

      if (serverSets.isEmpty()) {
        serverSets.addAll(serverSetAccessors.keySet());
      }

      SortedSet<OutlierSet<NavigableSet<String>>> rankedOutliers = new TreeSet<>();
      for (String serverSet : serverSets) {
        Function<Configuration, NavigableSet<String>> accessorF = serverSetAccessors.get(serverSet);
        if (accessorF != null) {
          addPropertyOutliers(serverSet, accessorF, rankedOutliers);
        }
      }

      if (!_verbose) {
        // remove outlier sets where the hypothesis is that a particular server set
        // should be empty. such hypotheses do not seem to be useful in general.
        rankedOutliers.removeIf(oset -> oset.getDefinition().isEmpty());
      }

      return rankedOutliers;
    }

    /**
     * Use the results of CompareSameName to partition nodes into those containing a structure of a
     * given name and those lacking such a structure. This information will later be used to test
     * the sameName hypothesis.
     */
    private <T> void toNameOnlyEquivalenceSets(
        NamedStructureEquivalenceSets<T> eSets, Set<String> nodes) {
      ImmutableSortedMap.Builder<String, SortedSet<NamedStructureEquivalenceSet<T>>> newESetsMap =
          new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());
      for (Map.Entry<String, SortedSet<NamedStructureEquivalenceSet<T>>> entry :
          eSets.getSameNamedStructures().entrySet()) {
        SortedSet<NamedStructureEquivalenceSet<T>> eSetSets = entry.getValue();
        SortedSet<String> presentNodes =
            eSetSets.stream()
                .flatMap(eSet -> eSet.getNodes().stream())
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
        T struct = eSetSets.first().getNamedStructure();

        SortedSet<String> absentNodes =
            ImmutableSortedSet.copyOf(Sets.difference(ImmutableSet.copyOf(nodes), presentNodes));
        NamedStructureEquivalenceSet<T> presentSet =
            new NamedStructureEquivalenceSet<>(presentNodes.first(), struct);
        presentSet.setNodes(presentNodes);
        String name = entry.getKey();
        ImmutableSortedSet.Builder<NamedStructureEquivalenceSet<T>> eqSetsBuilder =
            new ImmutableSortedSet.Builder<>(Comparator.naturalOrder());
        eqSetsBuilder.add(presentSet);
        if (!absentNodes.isEmpty()) {
          NamedStructureEquivalenceSet<T> absentSet =
              new NamedStructureEquivalenceSet<>(absentNodes.first(), null);
          absentSet.setNodes(absentNodes);
          eqSetsBuilder.add(absentSet);
        }
        newESetsMap.put(name, eqSetsBuilder.build());
      }
      eSets.setSameNamedStructures(newESetsMap.build());
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

  /**
   * Detects and ranks outliers based on a comparison of nodes' configurations.
   *
   * <p>If many nodes have a structure of a given name and a few do not, this may indicate an error.
   * If many nodes have a structure named N whose definition is identical, and a few nodes have a
   * structure named N that is defined differently, this may indicate an error. This question
   * leverages this and similar intuition to find outliers.
   */
  public static final class OutliersQuestion extends Question implements INodeRegexQuestion {

    private static final String PROP_HYPOTHESIS = "hypothesis";

    private static final String PROP_NAMED_STRUCT_TYPES = "namedStructTypes";

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private static final String PROP_SERVER_SETS = "serverSets";

    private static final String PROP_VERBOSE = "verbose";

    private OutliersHypothesis _hypothesis;

    private SortedSet<String> _namedStructTypes;

    private NodesSpecifier _nodeRegex;

    private SortedSet<String> _serverSets;

    private boolean _verbose;

    public OutliersQuestion() {
      _namedStructTypes = new TreeSet<>();
      _serverSets = new TreeSet<>();
      _nodeRegex = NodesSpecifier.ALL;
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
    public NodesSpecifier getNodeRegex() {
      return _nodeRegex;
    }

    @JsonProperty(PROP_SERVER_SETS)
    public SortedSet<String> getServerSets() {
      return _serverSets;
    }

    @JsonProperty(PROP_VERBOSE)
    public boolean getVerbose() {
      return _verbose;
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
    public void setNodeRegex(NodesSpecifier regex) {
      _nodeRegex = regex;
    }

    @JsonProperty(PROP_SERVER_SETS)
    public void setServerSets(SortedSet<String> serverSets) {
      _serverSets = serverSets;
    }

    @JsonProperty(PROP_VERBOSE)
    public void setVerbose(boolean verbose) {
      _verbose = verbose;
    }
  }

  @Override
  protected OutliersAnswerer createAnswerer(Question question, IBatfish batfish) {
    return new OutliersAnswerer(question, batfish);
  }

  @Override
  protected OutliersQuestion createQuestion() {
    return new OutliersQuestion();
  }
}
