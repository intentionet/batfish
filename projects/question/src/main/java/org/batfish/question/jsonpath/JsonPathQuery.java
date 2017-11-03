package org.batfish.question.jsonpath;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Set;
import org.batfish.common.BatfishException;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.question.jsonpath.JsonPathResult.JsonPathResultEntry;

public class JsonPathQuery {

  private static final String PROP_ASSERTION = "assertion";

  private static final String PROP_DESCRIPTION = "description";

  private static final String PROP_DISPLAY_HINTS = "displayHints";

  private static final String PROP_EXCEPTIONS = "exceptions";

  private static final String PROP_PATH = "path";

  private static final String PROP_SUFFIX = "suffix";

  private JsonPathAssertion _assertion;

  private String _description;

  private DisplayHints _displayHints;

  private Set<JsonPathResultEntry> _exceptions;

  private String _path;

  private boolean _suffix;

  @JsonProperty(PROP_ASSERTION)
  public JsonPathAssertion getAssertion() {
    return _assertion;
  }

  @JsonProperty(PROP_DESCRIPTION)
  public String getDescription() {
    return _description;
  }

  @JsonProperty(PROP_DISPLAY_HINTS)
  public DisplayHints getDisplayHints() {
    return _displayHints;
  }

  @JsonProperty(PROP_EXCEPTIONS)
  public Set<JsonPathResultEntry> getExceptions() {
    return _exceptions;
  }

  @JsonProperty(PROP_PATH)
  public String getPath() {
    return _path;
  }

  @JsonProperty(PROP_SUFFIX)
  public boolean getSuffix() {
    return _suffix;
  }

  public boolean hasValidAssertion() {
    return _assertion != null && _assertion.getType() != JsonPathAssertionType.none;
  }

  @JsonProperty(PROP_ASSERTION)
  public void setAssertion(JsonPathAssertion assertion) {
    _assertion = assertion;
  }

  @JsonProperty(PROP_DESCRIPTION)
  public void setDescription(String description) {
    _description = description;
  }

  @JsonProperty(PROP_DISPLAY_HINTS)
  public void setDisplayHints(DisplayHints displayHints) {
    _displayHints = displayHints;
  }

  @JsonProperty(PROP_EXCEPTIONS)
  public void setExceptions(Set<JsonPathResultEntry> exceptions) {
    _exceptions = exceptions;
  }

  @JsonProperty(PROP_PATH)
  public void setPath(String path) {
    _path = path;
  }

  @JsonProperty(PROP_SUFFIX)
  public void setSuffix(boolean suffix) {
    _suffix = suffix;
  }

  @Override
  public String toString() {
    BatfishObjectMapper mapper = new BatfishObjectMapper(false);
    try {
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new BatfishException("Could not map to JSON string", e);
    }
  }

  public boolean isException(JsonPathResultEntry resultEntry) {
    if (_exceptions == null) {
      return false;
    }
    return _exceptions.contains(resultEntry);
  }
}
