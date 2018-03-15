package org.batfish.question.jsonpath;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Configuration.ConfigurationBuilder;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import org.batfish.common.BatfishException;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.DisplayHints.Composition;
import org.batfish.datamodel.questions.DisplayHints.Extraction;
import org.batfish.question.jsonpath.JsonPathExtractionHint.UseType;
import org.batfish.question.jsonpath.JsonPathQuestionPlugin.JsonPathAnswerer;

public class JsonPathResult {

  public static class JsonPathResultEntry {

    static final String PROP_CONCRETE_PATH = "concretePath";

    static final String PROP_SUFFIX = "suffix";

    private final List<String> _concretePath;

    private final JsonNode _suffix;

    @JsonCreator
    public JsonPathResultEntry(
        @JsonProperty(PROP_CONCRETE_PATH) List<String> concretePath,
        @JsonProperty(PROP_SUFFIX) JsonNode suffix) {
      _concretePath = concretePath;
      if (suffix != null && suffix.isNull()) {
        _suffix = null;
      } else {
        _suffix = suffix;
      }
    }

    public JsonPathResultEntry(JsonNode prefix, JsonNode suffix) {
      this(getPrefixParts(prefix), suffix);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof JsonPathResultEntry)) {
        return false;
      }
      return Objects.equals(_concretePath, ((JsonPathResultEntry) o)._concretePath)
          && Objects.equals(_suffix, ((JsonPathResultEntry) o)._suffix);
    }

    @JsonIgnore
    public String getMapKey() {
      return String.join("->", _concretePath);
    }

    @JsonProperty(PROP_CONCRETE_PATH)
    public List<String> getConcretePath() {
      return _concretePath;
    }

    public String getPrefixPart(int index) {
      if (_concretePath.size() <= index) {
        throw new BatfishException(
            "No valid part at index " + index + "for concrete path " + _concretePath);
      }
      // remove the single quotes around the string
      return _concretePath.get(index).replaceAll("^\'|\'$", "");
    }

    private static List<String> getPrefixParts(JsonNode prefix) {
      String text = prefix.textValue();
      if (text.equals("$")) {
        return Arrays.asList("$");
      }
      if (text.length() < 2) {
        throw new BatfishException("Unexpected prefix " + text);
      }
      String endsCut = text.substring(2, text.length() - 1);
      return Arrays.asList(endsCut.split("\\]\\["));
    }

    @JsonProperty(PROP_SUFFIX)
    public JsonNode getSuffix() {
      return _suffix;
    }

    @Override
    public int hashCode() {
      return Objects.hash(_concretePath, _suffix);
    }
  }

  private static final String PROP_ASSERTION_RESULT = "assertionResult";

  private static final String PROP_EXTRACTED_VALUES = "extractedValues";

  private static final String PROP_NUM_RESULTS = "numResults";

  private static final String PROP_RESULT = "result";

  private Boolean _assertionResult;

  private Map<String, Map<String, JsonNode>> _displayValues;

  private Integer _numResults;

  private SortedMap<String, JsonPathResultEntry> _result;

  public JsonPathResult() {
    _result = new TreeMap<>();
  }

  public Map<String, Map<String, JsonNode>> computeDisplayValues(DisplayHints displayHints) {
    _displayValues = new HashMap<>(); // reset anything we may have done in the past
    for (Entry<String, Extraction> entry : displayHints.getExtractions().entrySet()) {
      JsonPathExtractionHint jpeHint = null;
      try {
        jpeHint = JsonPathExtractionHint.fromExtractionHint(entry.getValue());
      } catch (IOException e) {
        throw new BatfishException(
            "Could not extract JsonPathExtractionHint from ExtractionHint", e);
      }
      switch (jpeHint.getUse()) {
        case PREFIX:
          extractValuesFromPrefix(entry.getKey(), entry.getValue(), jpeHint);
          break;
        case FUNCOFSUFFIX:
        case PREFIXOFSUFFIX:
        case SUFFIXOFSUFFIX:
          extractValuesFromSuffix(entry.getKey(), entry.getValue(), jpeHint);
          break;
        default:
          throw new BatfishException("Unknown use type " + jpeHint.getUse());
      }
    }
    if (displayHints.getCompositions() != null) {
      doCompositions(displayHints.getCompositions(), displayHints.getExtractions());
    }
    return _displayValues;
  }

  private void doCompositions(
      Map<String, Composition> compositions, Map<String, Extraction> extractions) {
    for (String resultKey : _result.keySet()) {
      for (Entry<String, Composition> cEntry : compositions.entrySet()) {
        String compositionName = cEntry.getKey();
        Composition composition = cEntry.getValue();
        if (composition.getSchemaAsObject().isList()) {
          doCompositionList(resultKey, compositionName, composition, extractions);
        } else {
          doCompositionSingleton(resultKey, compositionName, composition);
        }
      }
    }
  }

  private void doCompositionList(
      String resultKey,
      String compositionName,
      Composition composition,
      Map<String, Extraction> extractions) {
    // check if we have any list type extraction variables and listLengths agree
    int listLen = 0;
    for (Entry<String, String> pEntry : composition.getDictionary().entrySet()) {
      String propertyName = pEntry.getKey();
      String varName = pEntry.getValue();
      if (!extractions.containsKey(varName)) {
        throw new BatfishException(
            String.format(
                "varName '%s' for '%s' of '%s' is not in extractions",
                varName, composition.getDictionary().get(varName), compositionName));
      }
      if (extractions.get(varName).getSchemaAsObject().isList()) {
        if (!_displayValues.get(resultKey).containsKey(varName)) {
          throw new BatfishException(
              String.format(
                  "varName '%s' for '%s' of '%s' is not in display values",
                  varName, propertyName, compositionName));
        }
        ArrayNode varNode = (ArrayNode) _displayValues.get(resultKey).get(varName);
        if (listLen != 0 && listLen != varNode.size()) {
          throw new BatfishException(
              "Found lists of different lengths in values: " + listLen + " " + varNode.size());
        }
        listLen = varNode.size();
      }
    }
    if (listLen == 0) {
      throw new BatfishException("None of the extraction values is a list for " + compositionName);
    }
    ObjectMapper mapper = BatfishObjectMapper.mapper();
    ArrayNode arrayNode = mapper.createArrayNode();
    for (int index = 0; index < listLen; index++) {
      ObjectNode object = mapper.createObjectNode();
      for (Entry<String, String> pEntry : composition.getDictionary().entrySet()) {
        String propertyName = pEntry.getKey();
        String varName = pEntry.getValue();
        JsonNode varNode = _displayValues.get(resultKey).get(varName);
        if (extractions.get(varName).getSchemaAsObject().isList()) {
          object.set(propertyName, ((ArrayNode) varNode).get(index));
        } else {
          object.set(propertyName, varNode);
        }
      }
      confirmValueType(object, composition.getSchemaAsObject().getBaseType());
      arrayNode.add(object);
    }
    _displayValues.get(resultKey).put(compositionName, arrayNode);
  }

  private void doCompositionSingleton(
      String resultKey, String compositionName, Composition composition) {
    ObjectMapper mapper = BatfishObjectMapper.mapper();
    ObjectNode object = mapper.createObjectNode();
    for (Entry<String, String> pEntry : composition.getDictionary().entrySet()) {
      String propertyName = pEntry.getKey();
      String varName = pEntry.getValue();
      if (!_displayValues.get(resultKey).containsKey(varName)) {
        throw new BatfishException(
            String.format(
                "varName '%s' for property '%s' of composition '%s' is not in display values",
                varName, propertyName, compositionName));
      }
      object.set(propertyName, _displayValues.get(resultKey).get(varName));
    }
    confirmValueType(object, composition.getSchemaAsObject().getBaseType());
    _displayValues.get(resultKey).put(compositionName, object);
  }

  private void extractValuesFromPrefix(
      String displayVar, Extraction extraction, JsonPathExtractionHint jpeHint) {
    if (extraction.getSchemaAsObject().isList()) {
      throw new BatfishException("Prefix-based hints are incompatible with list types");
    }
    for (Entry<String, JsonPathResultEntry> entry : _result.entrySet()) {
      if (!_displayValues.containsKey(entry.getKey())) {
        _displayValues.put(entry.getKey(), new HashMap<>());
      }
      String prefixPart = entry.getValue().getPrefixPart(jpeHint.getIndex());
      _displayValues.get(entry.getKey()).put(displayVar, new TextNode(prefixPart));
    }
  }

  private void extractValuesFromSuffix(
      String displayVar, Extraction extraction, JsonPathExtractionHint jpeHint) {
    for (Entry<String, JsonPathResultEntry> entry : _result.entrySet()) {
      if (!_displayValues.containsKey(entry.getKey())) {
        _displayValues.put(entry.getKey(), new HashMap<>());
      }
      if (entry.getValue().getSuffix() == null) {
        throw new BatfishException(
            "Cannot compute suffix-based display values with null suffix. "
                + "(Was suffix set to True in the original JsonPath Query?)");
      }
      Configuration.setDefaults(BatfishJsonPathDefaults.INSTANCE);
      Configuration c = (new ConfigurationBuilder()).build();
      Object jsonObject = JsonPath.parse(entry.getValue().getSuffix(), c).json();

      JsonPathQuery query = new JsonPathQuery(jpeHint.getFilter(), true);

      List<JsonNode> extractedList = new LinkedList<>();
      switch (jpeHint.getUse()) {
        case FUNCOFSUFFIX:
          {
            if (!extraction.getSchemaAsObject().isIntOrIntList()) {
              throw new BatfishException(
                  "schema must be INT(LIST) with funcofsuffix-based extraction hint");
            }
            Object result = JsonPathAnswerer.computePathFunction(jsonObject, query);
            if (result != null) {
              if (result instanceof Integer) {
                extractedList.add(new IntNode((Integer) result));
              } else if (result instanceof ArrayNode) {
                for (JsonNode node : (ArrayNode) result) {
                  if (!(node instanceof IntNode)) {
                    throw new BatfishException(
                        "Got non-integer result from path function after filter "
                            + query.getPath());
                  }
                  extractedList.add(node);
                }
              } else {
                throw new BatfishException("Unknown result type from computePathFunction");
              }
            }
          }
          break;
        case PREFIXOFSUFFIX:
        case SUFFIXOFSUFFIX:
          {
            JsonPathResult filterResult = JsonPathAnswerer.computeResult(jsonObject, query);
            Map<String, JsonPathResultEntry> filterResultEntries = filterResult.getResult();
            for (Entry<String, JsonPathResultEntry> resultEntry : filterResultEntries.entrySet()) {
              JsonNode value =
                  (jpeHint.getUse() == UseType.PREFIXOFSUFFIX)
                      ? new TextNode(resultEntry.getValue().getPrefixPart(jpeHint.getIndex()))
                      : resultEntry.getValue().getSuffix();
              confirmValueType(value, extraction.getSchemaAsObject().getBaseType());
              extractedList.add(value);
            }
          }
          break;
        default:
          throw new BatfishException("Unknown UseType " + jpeHint.getUse());
      }
      if (extractedList.size() == 0) {
        throw new BatfishException(
            "Got no results after filtering suffix values of the answer"
                + "\nFilter: "
                + jpeHint.getFilter()
                + "\nJson: "
                + jsonObject);
      }

      if (extraction.getSchemaAsObject().isList()) {
        ArrayNode arrayNode = BatfishObjectMapper.mapper().valueToTree(extractedList);
        _displayValues.get(entry.getKey()).put(displayVar, arrayNode);
      } else {
        if (extractedList.size() > 1) {
          throw new BatfishException(
              "Got multiple results after filtering suffix values "
                  + " of the answer, but the display type is non-list");
        }
        _displayValues.get(entry.getKey()).put(displayVar, extractedList.get(0));
      }
    }
  }

  private static void confirmValueType(JsonNode value, Class<?> baseClass) {
    try {
      BatfishObjectMapper.mapper().readValue(value.toString(), baseClass);
    } catch (IOException e) {
      throw new BatfishException(
          "Could not map extracted value to expected type " + baseClass + "\nValue: " + value, e);
    }
  }

  @JsonProperty(PROP_ASSERTION_RESULT)
  public Boolean getAssertionResult() {
    return _assertionResult;
  }

  @JsonProperty(PROP_EXTRACTED_VALUES)
  public Map<String, Map<String, JsonNode>> getExtractedValues() {
    return _displayValues;
  }

  @JsonProperty(PROP_NUM_RESULTS)
  public Integer getNumResults() {
    return _numResults;
  }

  //  @JsonProperty(PROP_PATH)
  //  public JsonPathQuery getPath() {
  //    return _path;
  //  }

  @JsonProperty(PROP_RESULT)
  public SortedMap<String, JsonPathResultEntry> getResult() {
    return _result;
  }

  @JsonProperty(PROP_ASSERTION_RESULT)
  public void setAssertionResult(Boolean assertionResult) {
    _assertionResult = assertionResult;
  }

  @JsonProperty(PROP_EXTRACTED_VALUES)
  public void setExtractedValues(Map<String, Map<String, JsonNode>> extractedValues) {
    _displayValues = extractedValues;
  }

  @JsonProperty(PROP_NUM_RESULTS)
  public void setNumResults(Integer numResults) {
    _numResults = numResults;
  }

  //  @JsonProperty(PROP_PATH)
  //  public void setPath(JsonPathQuery path) {
  //    _path = path;
  //  }

  @JsonProperty(PROP_RESULT)
  public void setResult(SortedMap<String, JsonPathResultEntry> result) {
    _result = result;
  }
}
