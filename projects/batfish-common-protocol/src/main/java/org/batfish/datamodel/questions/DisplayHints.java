package org.batfish.datamodel.questions;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.answers.Schema;

public class DisplayHints {

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
        retSet.addAll(_dictionary.values());
      }
      return retSet;
    }

    @JsonProperty(PROP_DICTIONARY)
    public void setDictionary(Map<String, String> dictionary) {
      _dictionary = dictionary;
    }

    @JsonProperty(PROP_SCHEMA)
    public void setSchema(Schema schema) {
      _schema = schema;
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
    public void setSchema(Schema schema) {
      _schema = schema;
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
    _compositions = firstNonNull(compositions, new HashMap<>());
    _extractions = firstNonNull(extractions, new HashMap<>());
    _textDesc = firstNonNull(textDesc, "");

    Set<String> varsInEntities = new HashSet<>();
    for (Entry<String, Composition> entry : _compositions.entrySet()) {
      entry.getValue().validate(entry.getKey());
      varsInEntities.addAll(entry.getValue().getVars());
    }

    for (Entry<String, Extraction> entry : _extractions.entrySet()) {
      entry.getValue().validate(entry.getKey());
    }

    // all extraction vars mentioned in entity configuration should have extraction hints
    Set<String> varsInExtractionHints = _extractions.keySet();
    SetView<String> missingExtractionVars = Sets.difference(varsInEntities, varsInExtractionHints);
    if (!missingExtractionVars.isEmpty()) {
      throw new BatfishException(
          "entities refer to variables that are not in extraction hints: " + missingExtractionVars);
    }

    // the names of entities and extraction vars should have no overlap
    Set<String> commonNames = Sets.intersection(varsInExtractionHints, _compositions.keySet());
    if (!commonNames.isEmpty()) {
      throw new BatfishException(
          "entities and extraction vars should not have common names: " + commonNames);
    }
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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DisplayHints)) {
      return false;
    }
    // ignore extactions and compositions -- we will remove those soon from this class
    return Objects.equals(_textDesc, ((DisplayHints) o)._textDesc);
  }

  @Override
  public int hashCode() {
    // ignore extactions and compositions -- we will remove those soon from this class
    return Objects.hash(_textDesc);
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
  public DisplayHints setTextDesc(String textDesc) {
    _textDesc = textDesc;
    return this;
  }
}
