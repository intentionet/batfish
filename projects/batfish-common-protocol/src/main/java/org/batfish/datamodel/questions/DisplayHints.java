package org.batfish.datamodel.questions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.batfish.common.BatfishException;

public class DisplayHints {

  public static class Schema {

    private static final Map<String, String> schemaAliases;

    static {
      Map<String, String> aMap = new HashMap<>();
      aMap.put("Environment", "class:org.batfish.datamodel.pojo.Environment");
      aMap.put("FileLine", "class:org.batfish.datamodel.collections.FileLinePair");
      aMap.put("Flow", "class:org.batfish.datamodel.Flow");
      aMap.put("FlowTrace", "class:org.batfish.datamodel.FlowTrace");
      aMap.put("Integer", "class:java.lang.Integer");
      aMap.put("Interface", "class:org.batfish.datamodel.collections.NodeInterfacePair");
      aMap.put("Ip", "class:org.batfish.datamodel.Ip");
      aMap.put("Node", "class:org.batfish.datamodel.Configuration");
      aMap.put("String", "class:java.lang.String");
      schemaAliases = Collections.unmodifiableMap(aMap);
    }

    private Class<?> _baseType;

    private boolean _isListType;

    private String _schemaStr;

    public Schema(String schema) {
      _schemaStr = schema;

      String baseTypeName = schema;
      _isListType = false;

      Matcher matcher = Pattern.compile("List<(.+)>").matcher(schema);
      if (matcher.find()) {
        baseTypeName = matcher.group(1);
        _isListType = true;
      }

      if (!schemaAliases.containsKey(baseTypeName)) {
        throw new BatfishException("Unknown schema type: " + baseTypeName);
      }

      baseTypeName = schemaAliases.get(baseTypeName);

      if (!baseTypeName.startsWith("class:")) {
        throw new BatfishException("Only class-based schemas are supported. Got " + baseTypeName);
      }

      baseTypeName = baseTypeName.replaceFirst("class:", "");

      try {
        _baseType = Class.forName(baseTypeName);
      } catch (ClassNotFoundException e) {
        throw new BatfishException("Could not get a class from " + baseTypeName);
      }
    }

    public Class<?> getBaseType() {
      return _baseType;
    }

    public boolean isList() {
      return _isListType;
    }

    public String toString() {
      return _schemaStr;
    }

    public boolean isIntOrIntList() {
      return _baseType.getCanonicalName().equals("java.lang.Integer");
    }
  }

  public static class Composition {

    private static final String PROP_SCHEMA = "schema";

    private static final String PROP_DICTIONARY = "dictionary";

    private Map<String, String> _dictionary;

    private Schema _schema;

    @JsonProperty(PROP_DICTIONARY)
    public Map<String, String> getDictionary() {
      return _dictionary;
    }

    @JsonProperty(PROP_SCHEMA)
    public String getSchema() {
      return _schema.toString();
    }

    @JsonIgnore
    public Schema getSchemaAsObject() {
      return _schema;
    }

    @JsonIgnore
    public Set<String> getVars() {
      Set<String> retSet = new HashSet<>();
      if (_dictionary != null) {
        for (String var : _dictionary.values()) {
          retSet.add(var);
        }
      }
      return retSet;
    }

    @JsonProperty(PROP_DICTIONARY)
    public void setDictionary(Map<String, String> dictionary) {
      _dictionary = dictionary;
    }

    @JsonProperty(PROP_SCHEMA)
    public void setSchema(String schema) {
      _schema = new Schema(schema);
    }

    public void validate(String varName) {
      if (_dictionary == null || _dictionary.isEmpty()) {
        throw new BatfishException("dictionary not specified for composition for " + varName);
      }
    }
  }

  public static class Extraction {
    private static final String PROP_METHOD = "method";

    private static final String PROP_SCHEMA = "schema";

    private Map<String, JsonNode> _method;

    private Schema _schema;

    @JsonProperty(PROP_METHOD)
    public Map<String, JsonNode> getMethod() {
      return _method;
    }

    @JsonProperty(PROP_SCHEMA)
    public String getSchema() {
      return _schema.toString();
    }

    @JsonIgnore
    public Schema getSchemaAsObject() {
      return _schema;
    }

    @JsonProperty(PROP_METHOD)
    public void setMethod(Map<String, JsonNode> method) {
      _method = method;
    }

    @JsonProperty(PROP_SCHEMA)
    public void setSchema(String schema) {
      _schema = new Schema(schema);
    }

    public void validate(String varName) {
      if (_method == null || _method.isEmpty()) {
        throw new BatfishException("method not specified for variable " + varName);
      }
    }
  }

  private static final String PROP_COMPOSITIONS = "compositions";

  private static final String PROP_EXTRACTIONS = "extractions";

  private static final String PROP_TEXT_DESC = "textDesc";

  private Map<String, Composition> _compositions;

  private Map<String, Extraction> _extractions;

  private String _textDesc;

  public DisplayHints() {}

  @JsonCreator
  public DisplayHints(
      @JsonProperty(PROP_COMPOSITIONS) Map<String, Composition> compositions,
      @JsonProperty(PROP_EXTRACTIONS) Map<String, Extraction> extractions,
      @JsonProperty(PROP_TEXT_DESC) String textDesc) {

    if (compositions == null) {
      compositions = new HashMap<>();
    }
    if (extractions == null) {
      extractions = new HashMap<>();
    }
    if (textDesc == null) {
      textDesc = "";
    }

    Set<String> varsInEntities = new HashSet<>();
    for (Entry<String, Composition> entry : compositions.entrySet()) {
      entry.getValue().validate(entry.getKey());
      varsInEntities.addAll(entry.getValue().getVars());
    }

    for (Entry<String, Extraction> entry : extractions.entrySet()) {
      entry.getValue().validate(entry.getKey());
    }

    // all extraction vars mentioned in entity configuration should have extraction hints
    Set<String> varsInExtractionHints = extractions.keySet();
    SetView<String> missingExtractionVars = Sets.difference(varsInEntities, varsInExtractionHints);
    if (!missingExtractionVars.isEmpty()) {
      throw new BatfishException(
          "entities refer to variables that are not in extraction hints: " + missingExtractionVars);
    }

    // the names of entities and extraction vars should have no overlap
    Set<String> commonNames = Sets.intersection(varsInExtractionHints, compositions.keySet());
    if (!commonNames.isEmpty()) {
      throw new BatfishException(
          "entities and extraction vars should not have common names: " + commonNames);
    }

    // names in text description should correspond to those of entities or extraction vars
    Set<String> namesInTextDesc = new HashSet<>();
    Matcher matcher = Pattern.compile("\\$\\{([^\\}]+)\\}").matcher(textDesc);
    while (matcher.find()) {
      namesInTextDesc.add(matcher.group(1));
    }
    SetView<String> missingEntities =
        Sets.difference(namesInTextDesc, Sets.union(compositions.keySet(), extractions.keySet()));
    if (!missingEntities.isEmpty()) {
      throw new BatfishException(
          "textDesc has names that are neither entities nor extractions: " + missingEntities);
    }

    _compositions = compositions;
    _extractions = extractions;
    _textDesc = textDesc;
  }

  @JsonProperty(PROP_COMPOSITIONS)
  public Map<String, Composition> getCompositions() {
    return _compositions;
  }

  @JsonProperty(PROP_EXTRACTIONS)
  public Map<String, Extraction> getExtractions() {
    return _extractions;
  }

  @JsonProperty(PROP_TEXT_DESC)
  public String getTextDesc() {
    return _textDesc;
  }

  @JsonProperty(PROP_COMPOSITIONS)
  public void setCompositions(Map<String, Composition> compositions) {
    _compositions = compositions;
  }

  @JsonProperty(PROP_EXTRACTIONS)
  public void setExtractions(Map<String, Extraction> extractions) {
    _extractions = extractions;
  }

  @JsonProperty(PROP_TEXT_DESC)
  public void setTextDesc(String textDesc) {
    _textDesc = textDesc;
  }
}
