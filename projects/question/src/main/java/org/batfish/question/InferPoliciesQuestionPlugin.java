package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NamedStructureOutlierSet;
import org.batfish.datamodel.collections.OutlierSet;
import org.batfish.datamodel.collections.RoleBasedOutlierSet;
import org.batfish.datamodel.questions.AbstractRoleConsistencyQuestion;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.DisplayHints.Composition;
import org.batfish.datamodel.questions.DisplayHints.Extraction;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.Question.InstanceData;
import org.batfish.question.NamedStructureRoleConsistencyQuestionPlugin.NamedStructureRoleConsistencyQuestion;
import org.batfish.question.OutliersQuestionPlugin.OutliersAnswerElement;
import org.batfish.question.OutliersQuestionPlugin.OutliersQuestion;
import org.batfish.question.PerRoleQuestionPlugin.PerRoleAnswerElement;
import org.batfish.question.PerRoleQuestionPlugin.PerRoleQuestion;
import org.batfish.question.RoleConsistencyQuestionPlugin.RoleConsistencyQuestion;
import org.batfish.question.jsonpath.JsonPathExtractionHint;
import org.batfish.question.jsonpath.JsonPathExtractionHint.UseType;
import org.batfish.question.jsonpath.JsonPathQuery;
import org.batfish.question.jsonpath.JsonPathQuestionPlugin.JsonPathQuestion;
import org.batfish.role.OutliersHypothesis;

@AutoService(Plugin.class)
public class InferPoliciesQuestionPlugin extends QuestionPlugin {

  /** Helper to generate a name for a role consistency question. */
  private static String getInstanceNameForQuestion(AbstractRoleConsistencyQuestion roleQ) {
    switch (roleQ.getHypothesis()) {
      case SAME_DEFINITION:
      case SAME_NAME:
        {
          NamedStructureRoleConsistencyQuestion q = (NamedStructureRoleConsistencyQuestion) roleQ;
          return q.getHypothesis().hypothesisName() + " " + q.getStructType();
        }
      case SAME_SERVERS:
        {
          RoleConsistencyQuestion q = (RoleConsistencyQuestion) roleQ;
          return q.getHypothesis().hypothesisName() + " " + q.getPropertyName();
        }
      default:
        return "Unknown question for hypothesis: " + roleQ.getHypothesis().hypothesisName();
    }
  }

  /** Helper to generate display hints for a role question. */
  private static DisplayHints getDisplayHintsForQuestion(AbstractRoleConsistencyQuestion roleQ) {
    ImmutableMap.Builder<String, Extraction> extractions = ImmutableMap.builder();

    /////// All role consistency questions have outliers.
    JsonPathExtractionHint outliers = new JsonPathExtractionHint();
    outliers.setUse(UseType.SUFFIXOFSUFFIX);
    outliers.setFilter("$.outliers[*]");
    Extraction outliersEx = new Extraction();
    try {
      outliersEx.setMethod(
          BatfishObjectMapper.clone(outliers, new TypeReference<Map<String, JsonNode>>() {}));
    } catch (IOException e) {
      throw new BatfishException("Error creating extraction", e);
    }
    outliersEx.setSchema(Schema.list(Schema.STRING));
    extractions.put("outlierNames", outliersEx);

    /////// All role consistency questions have a role in the answer.
    JsonPathExtractionHint role = new JsonPathExtractionHint();
    role.setUse(UseType.SUFFIXOFSUFFIX);
    role.setFilter("$.role");
    Extraction roleEx = new Extraction();
    try {
      roleEx.setMethod(
          BatfishObjectMapper.clone(role, new TypeReference<Map<String, JsonNode>>() {}));
    } catch (IOException e) {
      throw new BatfishException("Error creating extraction", e);
    }
    roleEx.setSchema(Schema.STRING);
    extractions.put("role", roleEx);

    String hypothesisSpecificDesc;

    switch (roleQ.getHypothesis()) {
      case SAME_NAME:
        {
          JsonPathExtractionHint name = new JsonPathExtractionHint();
          name.setUse(UseType.SUFFIXOFSUFFIX);
          name.setFilter("$.name");
          Extraction nameEx = new Extraction();
          try {
            nameEx.setMethod(
                BatfishObjectMapper.clone(name, new TypeReference<Map<String, JsonNode>>() {}));
          } catch (IOException e) {
            throw new BatfishException("Error creating extraction", e);
          }
          nameEx.setSchema(Schema.STRING);
          extractions.put("name", nameEx);

          JsonPathExtractionHint struct = new JsonPathExtractionHint();
          struct.setUse(UseType.SUFFIXOFSUFFIX);
          struct.setFilter("$.structType");
          Extraction structEx = new Extraction();
          try {
            structEx.setMethod(
                BatfishObjectMapper.clone(struct, new TypeReference<Map<String, JsonNode>>() {}));
          } catch (IOException e) {
            throw new BatfishException("Error creating extraction", e);
          }
          structEx.setSchema(Schema.STRING);
          extractions.put("structType", structEx);

          hypothesisSpecificDesc = "define a ${structType} named ${name}";
          break;
        }

      case SAME_SERVERS:
        {
          JsonPathExtractionHint name = new JsonPathExtractionHint();
          name.setUse(UseType.SUFFIXOFSUFFIX);
          name.setFilter("$.name");
          Extraction nameEx = new Extraction();
          try {
            nameEx.setMethod(
                BatfishObjectMapper.clone(name, new TypeReference<Map<String, JsonNode>>() {}));
          } catch (IOException e) {
            throw new BatfishException("Error creating extraction", e);
          }
          nameEx.setSchema(Schema.STRING);
          extractions.put("name", nameEx);

          JsonPathExtractionHint definition = new JsonPathExtractionHint();
          definition.setUse(UseType.SUFFIXOFSUFFIX);
          definition.setFilter("$.definition[*]");
          Extraction definitionEx = new Extraction();
          try {
            definitionEx.setMethod(
                BatfishObjectMapper.clone(
                    definition, new TypeReference<Map<String, JsonNode>>() {}));
          } catch (IOException e) {
            throw new BatfishException("Error creating extraction", e);
          }
          definitionEx.setSchema(Schema.list(Schema.IP));
          extractions.put("definition", definitionEx);
          hypothesisSpecificDesc = "have ${name} equal to ${definition}";
          break;
        }

      case SAME_DEFINITION:
        {
          JsonPathExtractionHint name = new JsonPathExtractionHint();
          name.setUse(UseType.SUFFIXOFSUFFIX);
          name.setFilter("$.name");
          Extraction nameEx = new Extraction();
          try {
            nameEx.setMethod(
                BatfishObjectMapper.clone(name, new TypeReference<Map<String, JsonNode>>() {}));
          } catch (IOException e) {
            throw new BatfishException("Error creating extraction", e);
          }
          nameEx.setSchema(Schema.STRING);
          extractions.put("name", nameEx);

          JsonPathExtractionHint struct = new JsonPathExtractionHint();
          struct.setUse(UseType.SUFFIXOFSUFFIX);
          struct.setFilter("$.structType");
          Extraction structEx = new Extraction();
          try {
            structEx.setMethod(
                BatfishObjectMapper.clone(struct, new TypeReference<Map<String, JsonNode>>() {}));
          } catch (IOException e) {
            throw new BatfishException("Error creating extraction", e);
          }
          structEx.setSchema(Schema.STRING);
          extractions.put("structType", structEx);

          JsonPathExtractionHint definition = new JsonPathExtractionHint();
          definition.setUse(UseType.SUFFIXOFSUFFIX);
          definition.setFilter("$.structDefinition");
          Extraction definitionEx = new Extraction();
          try {
            definitionEx.setMethod(
                BatfishObjectMapper.clone(
                    definition, new TypeReference<Map<String, JsonNode>>() {}));
          } catch (IOException e) {
            throw new BatfishException("Error creating extraction", e);
          }
          definitionEx.setSchema(Schema.OBJECT);
          extractions.put("structDefinition", definitionEx);

          hypothesisSpecificDesc =
              "define a ${structType} named ${name} with definition ${structDefinition}";
          break;
        }

      default:
        hypothesisSpecificDesc = "[WARN: unhandled hypothesis type]";
    }

    Composition nodeComp = new Composition();
    nodeComp.setSchema(Schema.list(Schema.NODE));
    nodeComp.setDictionary(ImmutableMap.of("name", "outlierNames"));

    return new DisplayHints(
        ImmutableMap.of("outliers", nodeComp),
        extractions.build(),
        String.format(
            "Hypothesis %s for role ${role}: all nodes should %s, but ${outliers} do not.",
            roleQ.getHypothesis(), hypothesisSpecificDesc));
  }

  private static JsonPathQuestion makeQuestionWithHints(AbstractRoleConsistencyQuestion roleQ) {
    JsonPathQuestion ret = new JsonPathQuestion();

    ret.setInnerQuestion(roleQ);

    JsonPathQuery path = new JsonPathQuery("$.answers[?(@.outliers)]", true);
    path.setDisplayHints(getDisplayHintsForQuestion(roleQ));
    ret.setPaths(Collections.singletonList(path));

    InstanceData instance = new InstanceData();
    instance.setInstanceName(getInstanceNameForQuestion(roleQ));
    ret.setInstance(instance);

    return ret;
  }

  public static class InferPoliciesAnswerElement extends AnswerElement {

    private static final String PROP_ROLE_CONSISTENCY_QUESTIONS = "roleConsistencyQuestions";

    @JsonProperty(PROP_ROLE_CONSISTENCY_QUESTIONS)
    private List<JsonPathQuestion> _roleConsistencyQuestions;

    public InferPoliciesAnswerElement() {
      _roleConsistencyQuestions = new LinkedList<>();
    }

    public void addRoleConsistencyQuestions(
        List<AbstractRoleConsistencyQuestion> roleConsistencyQuestions) {
      roleConsistencyQuestions
          .stream()
          .map(InferPoliciesQuestionPlugin::makeQuestionWithHints)
          .forEach(_roleConsistencyQuestions::add);
    }
  }

  public static class InferPoliciesAnswerer extends Answerer {

    private InferPoliciesAnswerElement _answerElement;

    // the minimum percentage of nodes that must conform to a policy, in order for
    // the policy to be returned
    private static final double CONFORMERS_THRESHOLD = 0.75;

    public InferPoliciesAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public InferPoliciesAnswerElement answer() {

      _answerElement = new InferPoliciesAnswerElement();

      _answerElement.addRoleConsistencyQuestions(serverConsistencyPolicies());
      _answerElement.addRoleConsistencyQuestions(
          namedStructureConsistencyPolicies(OutliersHypothesis.SAME_NAME));
      _answerElement.addRoleConsistencyQuestions(
          namedStructureConsistencyPolicies(OutliersHypothesis.SAME_DEFINITION));

      return _answerElement;
    }

    private List<AbstractRoleConsistencyQuestion> serverConsistencyPolicies() {

      OutliersHypothesis hypothesis = OutliersHypothesis.SAME_SERVERS;

      SortedMap<String, AnswerElement> roleAnswers = perRoleOutlierInfo(hypothesis);

      Multimap<String, OutlierSet<NavigableSet<String>>> outliersPerPropertyName =
          outliersByProperty(
              roleAnswers.values(), OutliersAnswerElement::getServerOutliers, OutlierSet::getName);

      // remove outlier sets where no nodes declare any servers
      List<String> undeclared = new LinkedList<>();
      for (String name : outliersPerPropertyName.keySet()) {
        Collection<OutlierSet<NavigableSet<String>>> outlierSets =
            outliersPerPropertyName.get(name);
        if (outlierSets
            .stream()
            .allMatch(oset -> oset.getDefinition().isEmpty() && oset.getOutliers().isEmpty())) {
          undeclared.add(name);
        }
      }
      for (String name : undeclared) {
        outliersPerPropertyName.removeAll(name);
      }

      return policiesAboveThreshold(outliersPerPropertyName, hypothesis);
    }

    private List<AbstractRoleConsistencyQuestion> namedStructureConsistencyPolicies(
        OutliersHypothesis hypothesis) {

      SortedMap<String, AnswerElement> roleAnswers = perRoleOutlierInfo(hypothesis);

      Multimap<String, NamedStructureOutlierSet<?>> outliersPerStructureType =
          outliersByProperty(
              roleAnswers.values(),
              OutliersAnswerElement::getNamedStructureOutliers,
              NamedStructureOutlierSet::getStructType);

      return policiesAboveThreshold(outliersPerStructureType, hypothesis);
    }

    // obtain all outlier sets for the given hypothesis
    private SortedMap<String, AnswerElement> perRoleOutlierInfo(OutliersHypothesis hypothesis) {
      OutliersQuestion innerQ = new OutliersQuestionPlugin().createQuestion();
      innerQ.setHypothesis(hypothesis);
      innerQ.setVerbose(true);

      InferPoliciesQuestion question = (InferPoliciesQuestion) _question;
      PerRoleQuestion outerQ = new PerRoleQuestion(null, innerQ, question.getRoleDimension(), null);

      PerRoleQuestionPlugin outerPlugin = new PerRoleQuestionPlugin();
      PerRoleAnswerElement roleAE = outerPlugin.createAnswerer(outerQ, _batfish).answer();

      SortedMap<String, AnswerElement> answers = roleAE.getAnswers();
      for (Map.Entry<String, AnswerElement> entry : answers.entrySet()) {
        String role = entry.getKey();
        OutliersAnswerElement oae = (OutliersAnswerElement) entry.getValue();
        // update each outlier set to know its associated role
        setRoleAll(oae.getServerOutliers(), role);
        setRoleAll(oae.getNamedStructureOutliers(), role);
      }
      return answers;
    }

    private <T extends RoleBasedOutlierSet> Multimap<String, T> outliersByProperty(
        Collection<AnswerElement> answers,
        Function<OutliersAnswerElement, Collection<T>> outliersFun,
        Function<T, String> propertyNameFun) {

      Multimap<String, T> outliersPerProperty = LinkedListMultimap.create();

      // partition the resulting outliers by structure type (e.g., Ip Access List, Route Map)
      for (AnswerElement ae : answers) {
        OutliersAnswerElement oae = (OutliersAnswerElement) ae;
        for (T os : outliersFun.apply(oae)) {
          outliersPerProperty.put(propertyNameFun.apply(os), os);
        }
      }
      return outliersPerProperty;
    }

    private <T extends RoleBasedOutlierSet>
        List<AbstractRoleConsistencyQuestion> policiesAboveThreshold(
            Multimap<String, T> outliersPerPropertyName, OutliersHypothesis hypothesis) {
      InferPoliciesQuestion question = (InferPoliciesQuestion) _question;
      List<AbstractRoleConsistencyQuestion> policies = new LinkedList<>();
      for (String name : outliersPerPropertyName.keySet()) {
        Collection<T> outlierSets = outliersPerPropertyName.get(name);
        int conformers = outlierSets.stream().mapToInt(oset -> oset.getConformers().size()).sum();
        int outliers = outlierSets.stream().mapToInt(oset -> oset.getOutliers().size()).sum();
        double all = (double) conformers + outliers;
        if (conformers / all >= CONFORMERS_THRESHOLD) {
          switch (hypothesis) {
            case SAME_DEFINITION:
            case SAME_NAME:
              NamedStructureRoleConsistencyQuestion policy =
                  new NamedStructureRoleConsistencyQuestion(
                      name, hypothesis, question.getRoleDimension());
              policies.add(policy);
              break;
            case SAME_SERVERS:
              RoleConsistencyQuestion rcpolicy =
                  new RoleConsistencyQuestion(question.getRoleDimension(), name);
              policies.add(rcpolicy);
              break;
            default:
              throw new BatfishException("Unrecognized hypothesis " + hypothesis);
          }
        }
      }
      return policies;
    }

    private void setRoleAll(Collection<? extends RoleBasedOutlierSet> outlierSets, String role) {
      for (RoleBasedOutlierSet os : outlierSets) {
        os.setRole(role);
      }
    }
  }

  // <question_page_comment>
  /**
   * Infer likely role-based consistency policies for the network. Each such policy in general has
   * the form: all nodes that have the same role, according to a given role specifier S, must be
   * consistent with one another in some particular way.
   *
   * @type InferPolicies multifile
   */
  public static final class InferPoliciesQuestion extends Question {

    private static final String PROP_ROLE_DIMENSION = "roleDimension";

    @Nullable private String _roleDimension;

    @JsonCreator
    public InferPoliciesQuestion(@JsonProperty(PROP_ROLE_DIMENSION) String roleDimension) {
      _roleDimension = roleDimension;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "InferPolicies";
    }

    @JsonProperty(PROP_ROLE_DIMENSION)
    public String getRoleDimension() {
      return _roleDimension;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new InferPoliciesAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new InferPoliciesQuestion(null);
  }
}
