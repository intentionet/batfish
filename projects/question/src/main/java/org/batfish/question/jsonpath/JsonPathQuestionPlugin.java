package org.batfish.question.jsonpath;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.auto.service.AutoService;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Configuration.ConfigurationBuilder;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.BfConsts;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.QuestionPlugin;
import org.batfish.question.jsonpath.JsonPathResult.JsonPathResultEntry;

@AutoService(Plugin.class)
public class JsonPathQuestionPlugin extends QuestionPlugin {

  public static class JsonPathAnswerElement implements AnswerElement {

    private static final String PROP_DEBUG = "debug";

    private static final String PROP_RESULTS = "results";

    private Map<String, Object> _debug;

    private SortedMap<Integer, JsonPathResult> _results;

    private AnswerSummary _summary;

    public JsonPathAnswerElement() {
      // don't initialize _debug, so we won't serialize when its null (common case)
      _results = new TreeMap<>();
      _summary = new AnswerSummary();
    }

    public void addDebugInfo(String key, Object value) {
      if (_debug == null) {
        _debug = new HashMap<>();
      }
      if (_debug.containsKey(key)) {
        throw new BatfishException("Duplicate debug key");
      }
      _debug.put(key, value);
    }

    @JsonProperty(PROP_DEBUG)
    public Map<String, Object> getDebug() {
      return _debug;
    }

    @JsonProperty(PROP_RESULTS)
    public SortedMap<Integer, JsonPathResult> getResults() {
      return _results;
    }

    @Override
    public AnswerSummary getSummary() {
      return _summary;
    }

    @Override
    public String prettyPrint() {
      return prettyPrint(_results);
    }

    static String prettyPrint(SortedMap<Integer, JsonPathResult> results) {
      StringBuilder sb = new StringBuilder("Results for JsonPath\n");
      for (Integer index : results.keySet()) {
        JsonPathResult result = results.get(index);
        sb.append(String.format("  [%d]: %d results\n", index, result.getNumResults()));
        if (result.getAssertionResult() != null) {
          sb.append(String.format("    Assertion : %s\n", result.getAssertionResult()));
        }
        for (JsonPathResultEntry resultEntry : result.getResult().values()) {
          JsonNode suffix = resultEntry.getSuffix();
          String pathString = resultEntry.getMapKey();
          if (suffix != null) {
            sb.append(String.format("    %s : %s\n", pathString, suffix));
          } else {
            sb.append(String.format("    %s\n", pathString));
          }
        }
      }
      return sb.toString();
    }

    @JsonProperty(PROP_DEBUG)
    private void setDebug(Map<String, Object> debug) {
      _debug = debug;
    }

    @JsonProperty(PROP_RESULTS)
    public void setResults(SortedMap<Integer, JsonPathResult> results) {
      _results = results;
    }

    public void updateSummary() {
      _summary.reset();
      for (JsonPathResult result : _results.values()) {
        if (result.getAssertionResult() != null) {
          if (result.getAssertionResult()) {
            _summary.setNumPassed(_summary.getNumPassed() + 1);
          } else {
            _summary.setNumFailed(_summary.getNumFailed() + 1);
          }
        }
        _summary.setNumResults(_summary.getNumResults() + result.getNumResults());
      }
    }
  }

  public static class JsonPathAnswerer extends Answerer {

    public JsonPathAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    /**
     * This procedure proceeds as follows:
     *
     * <ul>
     *   <li>Compute the inner answer
     *   <li>Then, for each path query, do computeResult which returns JsonPathResult
     *   <li>The result is a list of JsonPathResultEntry minus exception entries
     *   <li>It also contains the result of evaluating the assertion on the result
     * </ul>
     */
    @Override
    public JsonPathAnswerElement answer() {

      Configuration.setDefaults(BatfishJsonPathDefaults.INSTANCE);
      ConfigurationBuilder b = new ConfigurationBuilder();
      final Configuration c = b.build();

      JsonPathQuestion question = (JsonPathQuestion) _question;
      List<JsonPathQuery> paths = question.getPaths();

      Question innerQuestion = question._innerQuestion;
      String innerQuestionName = innerQuestion.getName();
      Answerer innerAnswerer =
          _batfish.getAnswererCreators().get(innerQuestionName).apply(innerQuestion, _batfish);
      AnswerElement innerAnswer =
          (innerQuestion.getDifferential()) ? innerAnswerer.answerDiff() : innerAnswerer.answer();

      String innerAnswerStr = null;
      try {
        innerAnswerStr = BatfishObjectMapper.writeString(innerAnswer);
      } catch (IOException e) {
        throw new BatfishException("Could not get JSON string from inner answer", e);
      }
      Object jsonObject = JsonPath.parse(innerAnswerStr, c).json();
      Map<Integer, JsonPathResult> allResults = new ConcurrentHashMap<>();
      List<Integer> indices = new ArrayList<>();
      for (int i = 0; i < paths.size(); i++) {
        indices.add(i);
      }
      AtomicInteger completed = _batfish.newBatch("JsonPath queries", indices.size());
      indices
          .parallelStream()
          .forEach(
              i -> {
                JsonPathQuery query = paths.get(i);
                JsonPathResult jsonPathResult = computeResult(jsonObject, query);

                if (query.getDisplayHints() != null) {
                  jsonPathResult.computeDisplayValues(query.getDisplayHints());
                }

                allResults.put(i, jsonPathResult);
                completed.incrementAndGet();
              });
      JsonPathAnswerElement answerElement = new JsonPathAnswerElement();
      answerElement.getResults().putAll(allResults);
      answerElement.updateSummary();

      if (question.getDebug()) {
        answerElement.addDebugInfo("innerAnswer", innerAnswer);
      }

      return answerElement;
    }

    @Override
    public AnswerElement answerDiff() {
      // if the inner question is differential, use answer() (so we are not taking diff of diff)
      JsonPathQuestion question = (JsonPathQuestion) _question;
      Question innerQuestion = question._innerQuestion;
      if (innerQuestion.getDifferential()) {
        return answer();
      }

      _batfish.pushBaseEnvironment();
      _batfish.checkEnvironmentExists();
      _batfish.popEnvironment();
      _batfish.pushDeltaEnvironment();
      _batfish.checkEnvironmentExists();
      _batfish.popEnvironment();
      _batfish.pushBaseEnvironment();
      JsonPathAnswerer beforeAnswerer = (JsonPathAnswerer) create(_question, _batfish);
      JsonPathAnswerElement before = beforeAnswerer.answer();
      _batfish.popEnvironment();
      _batfish.pushDeltaEnvironment();
      JsonPathAnswerer afterAnswerer = (JsonPathAnswerer) create(_question, _batfish);
      JsonPathAnswerElement after = afterAnswerer.answer();
      _batfish.popEnvironment();
      return new JsonPathDiffAnswerElement(before, after);
    }

    public static Object computePathFunction(Object jsonObject, JsonPathQuery query) {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      Configuration configuration = cb.build();

      JsonPath jsonPath;
      try {
        jsonPath = JsonPath.compile(query.getPath());
      } catch (InvalidPathException e) {
        throw new BatfishException("Invalid JsonPath: " + query.getPath(), e);
      }

      try {
        return jsonPath.read(jsonObject, configuration);
      } catch (PathNotFoundException e) {
        return null;
      } catch (Exception e) {
        throw new BatfishException("Error reading JSON path: " + jsonPath, e);
      }
    }

    public static JsonPathResult computeResult(Object jsonObject, JsonPathQuery query) {
      ConfigurationBuilder prefixCb = new ConfigurationBuilder();
      prefixCb.options(Option.ALWAYS_RETURN_LIST);
      prefixCb.options(Option.AS_PATH_LIST);
      Configuration prefixC = prefixCb.build();

      ConfigurationBuilder suffixCb = new ConfigurationBuilder();
      suffixCb.options(Option.ALWAYS_RETURN_LIST);
      Configuration suffixC = suffixCb.build();

      ArrayNode prefixes = null;
      ArrayNode suffixes = null;
      JsonPath jsonPath;
      try {
        jsonPath = JsonPath.compile(query.getPath());
      } catch (InvalidPathException e) {
        throw new BatfishException("Invalid JsonPath: " + query.getPath(), e);
      }

      JsonPathResult jsonPathResult = new JsonPathResult();

      try {
        prefixes = jsonPath.read(jsonObject, prefixC);
        suffixes = jsonPath.read(jsonObject, suffixC);
      } catch (PathNotFoundException e) {
        suffixes = JsonNodeFactory.instance.arrayNode();
        prefixes = JsonNodeFactory.instance.arrayNode();
      } catch (Exception e) {
        throw new BatfishException("Error reading JSON path: " + query.getPath(), e);
      }

      Set<JsonPathResultEntry> resultEntries = new HashSet<>();
      Iterator<JsonNode> p = prefixes.iterator();
      Iterator<JsonNode> s = suffixes.iterator();
      while (p.hasNext()) {
        JsonNode prefix = p.next();
        JsonNode suffix = query.getSuffix() ? s.next() : null;
        String prefixStr = prefix.textValue();
        if (prefixStr == null) {
          throw new BatfishException("Did not expect null value");
        }
        JsonPathResultEntry resultEntry = new JsonPathResultEntry(prefix, suffix);

        if (!query.isException(resultEntry)) {
          resultEntries.add(resultEntry);
          jsonPathResult.getResult().put(resultEntry.getMapKey(), resultEntry);
        }
      }

      if (query.getAssertion() != null) {
        boolean assertion = query.getAssertion().evaluate(resultEntries);
        jsonPathResult.setAssertionResult(assertion);
      }

      jsonPathResult.setNumResults(resultEntries.size());
      return jsonPathResult;
    }
  }

  public static class JsonPathDiffAnswerElement implements AnswerElement {

    static String prettyPrint(SortedMap<Integer, JsonPathDiffResult> results) {
      StringBuilder sb = new StringBuilder();
      results.forEach(
          (index, diff) -> {
            SortedMap<String, JsonPathResultEntry> added = diff.getAdded();
            SortedMap<String, JsonPathResultEntry> removed = diff.getRemoved();
            sb.append(
                String.format(
                    "  [%d]: %d added and %d removed \n", index, added.size(), removed.size()));
            SortedSet<String> allKeys =
                CommonUtil.union(added.keySet(), removed.keySet(), TreeSet::new);
            for (String key : allKeys) {
              if (removed.containsKey(key)) {
                JsonNode removedNode = removed.get(key).getSuffix();
                if (removedNode != null) {
                  sb.append(String.format("-   %s : %s\n", key, removedNode));
                } else {
                  sb.append(String.format("-   %s\n", key));
                }
              }
              if (added.containsKey(key)) {
                JsonNode addedNode = added.get(key).getSuffix();
                if (addedNode != null) {
                  sb.append(String.format("+   %s : %s\n", key, addedNode));
                } else {
                  sb.append(String.format("+   %s\n", key));
                }
              }
            }
          });
      String result = sb.toString();
      return result;
    }

    private SortedMap<Integer, JsonPathDiffResult> _results;

    @JsonCreator
    public JsonPathDiffAnswerElement() {}

    public JsonPathDiffAnswerElement(JsonPathAnswerElement before, JsonPathAnswerElement after) {
      _results = new TreeMap<>();
      for (Integer index : before._results.keySet()) {
        JsonPathResult nprBefore = before._results.get(index);
        JsonPathResult nprAfter = after._results.get(index);
        JsonPathDiffResult diff = new JsonPathDiffResult(nprBefore, nprAfter);
        _results.put(index, diff);
      }
    }

    public SortedMap<Integer, JsonPathDiffResult> getResults() {
      return _results;
    }

    @Override
    public String prettyPrint() {
      return prettyPrint(_results);
    }

    public void setResults(SortedMap<Integer, JsonPathDiffResult> results) {
      _results = results;
    }
  }

  // <question_page_comment>

  /**
   * Runs JsonPath <a href=https://github.com/jayway/JsonPath></a> queries on the JSON data model
   * that is the output of the 'Nodes' question.
   *
   * <p>This query can be used to perform server-side queries for the presence or absence of
   * specified patterns in the data model induced by the configurations supplied in the test-rig.
   *
   * @type JsonPath onefile
   * @param paths A JSON list of path queries, each of which is a JSON object containing the
   *     remaining documented fields (path, suffix, summary). For each specified path query, the
   *     question returns a list of paths in the data model matching the criteria of the query.
   * @hparam path (Property of each element of 'paths') The JsonPath query to execute.
   * @hparam suffix (Property of each element of 'paths') Defaults to false. If true, then each path
   *     in the returned list will map to the remaining content of the datamodel at the end of that
   *     path. This can be useful for debugging, but can also be very verbose. If false, then each
   *     path will map to a null value.
   * @hparam summary (Property of each element of 'paths') Defaults to false. If true, then instead
   *     of outputting each matching path, only the count of matching paths will be output.
   * @example bf_answer("NodesPath",
   *     paths=[{"path":"$.nodes[*].interfaces[*][?(@.mtu!=1500)].mtu"}]) Return all interfaces with
   *     MTUs not equal to 1500
   */
  public static class JsonPathQuestion extends Question {

    private static final String PROP_DEBUG = "debug";

    private static final String PROP_PATHS = "paths";

    private Boolean _debug;

    private Question _innerQuestion;

    private List<JsonPathQuery> _paths;

    public JsonPathQuestion() {
      _paths = Collections.emptyList();
    }

    @Override
    public Question configureTemplate(@Nullable String exceptions, @Nullable String assertion) {
      try {
        JsonPathQuestion question = BatfishObjectMapper.clone(this, JsonPathQuestion.class);

        if (exceptions != null) {
          Set<JsonPathException> jpExceptions =
              BatfishObjectMapper.mapper()
                  .readValue(exceptions, new TypeReference<Set<JsonPathException>>() {});
          for (JsonPathQuery query : question.getPaths()) {
            query.setExceptions(jpExceptions);
          }
        }
        if (assertion != null) {
          JsonPathAssertion jpAssertion =
              // indicates a desire to remove the assertion
              (assertion.equals("") || assertion.equals("{}"))
                  ? null
                  : BatfishObjectMapper.mapper().readValue(assertion, JsonPathAssertion.class);

          for (JsonPathQuery query : question.getPaths()) {
            query.setAssertion(jpAssertion);
          }
        }
        return question;
      } catch (IOException e) {
        throw new BatfishException("Could not clone the question", e);
      }
    }

    @Override
    public boolean getDataPlane() {
      return _innerQuestion.getDataPlane();
    }

    @JsonProperty(PROP_DEBUG)
    public boolean getDebug() {
      if (_debug == null) {
        return false;
      }
      return _debug;
    }

    @JsonProperty(BfConsts.PROP_INNER_QUESTION)
    public Question getInnerQuestion() {
      return _innerQuestion;
    }

    @Override
    public String getName() {
      return "jsonpath";
    }

    @JsonProperty(PROP_PATHS)
    public List<JsonPathQuery> getPaths() {
      return _paths;
    }

    @Override
    public String prettyPrint() {
      String retString =
          String.format(
              "%s %s%s=\"%s\" %s=\"%s\"",
              getName(),
              prettyPrintBase(),
              PROP_PATHS,
              _paths,
              BfConsts.PROP_INNER_QUESTION,
              _innerQuestion.prettyPrint());
      return retString;
    }

    @JsonProperty(PROP_DEBUG)
    public void setDebug(Boolean debug) {
      _debug = debug;
    }

    @JsonProperty(BfConsts.PROP_INNER_QUESTION)
    public void setInnerQuestion(Question innerQuestion) {
      _innerQuestion = innerQuestion;
    }

    @JsonProperty(PROP_PATHS)
    public void setPaths(List<JsonPathQuery> paths) {
      _paths = paths;
    }
  }

  @Override
  protected JsonPathAnswerer createAnswerer(Question question, IBatfish batfish) {
    return new JsonPathAnswerer(question, batfish);
  }

  @Override
  protected JsonPathQuestion createQuestion() {
    return new JsonPathQuestion();
  }
}
