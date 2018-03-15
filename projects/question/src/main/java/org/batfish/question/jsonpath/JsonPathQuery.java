package org.batfish.question.jsonpath;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashSet;
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

  private Set<JsonPathException> _exceptions;

  private String _path;

  private boolean _suffix;

  public JsonPathQuery(String path, boolean suffix) {
    this(path, suffix, null, null, null, null);
  }

  @JsonCreator
  public JsonPathQuery(
      @JsonProperty(PROP_PATH) String path,
      @JsonProperty(PROP_SUFFIX) boolean suffix,
      @JsonProperty(PROP_DESCRIPTION) String description,
      @JsonProperty(PROP_DISPLAY_HINTS) DisplayHints displayHints,
      @JsonProperty(PROP_EXCEPTIONS) Set<JsonPathException> exceptions,
      @JsonProperty(PROP_ASSERTION) JsonPathAssertion assertion) {
    _path = path;
    _suffix = suffix;
    _description = description;
    _displayHints = displayHints;
    _exceptions = (exceptions == null) ? new HashSet<>() : exceptions;
    _assertion = assertion;
  }

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
  public Set<JsonPathException> getExceptions() {
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

  public boolean isException(JsonPathResultEntry resultEntry) {
    return _exceptions.contains(resultEntry);
  }

  public void setAssertion(JsonPathAssertion assertion) {
    _assertion = assertion;
  }

  public void setExceptions(Set<JsonPathException> exceptions) {
    _exceptions = (exceptions == null) ? new HashSet<>() : exceptions;
  }

  @Override
  public String toString() {
    try {
      return BatfishObjectMapper.writePrettyString(this);
    } catch (JsonProcessingException e) {
      throw new BatfishException("Cannot serialize to Json", e);
    }
  }
}
