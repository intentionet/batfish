package org.batfish.question.jsonpath;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.batfish.common.BatfishException;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.question.jsonpath.JsonPathResult.JsonPathResultEntry;

public class JsonPathAssertion {

  private static final String PROP_TYPE = "type";

  private static final String PROP_EXPECT = "expect";

  private JsonPathAssertionType _assertionType;

  private JsonNode _expect;

  @JsonCreator
  public JsonPathAssertion(
      @JsonProperty(PROP_TYPE) JsonPathAssertionType assertionType,
      @JsonProperty(PROP_EXPECT) JsonNode expect) {
    switch (assertionType) {
      case countequals:
      case countlessthan:
      case countmorethan:
        if (!expect.isInt()) {
          throw new IllegalArgumentException(
              String.format(
                  "Value '%s' of assertion type '%s' is not an integer", expect, assertionType));
        }
        break;
      case equals:
        if (!expect.isArray()) {
          throw new IllegalArgumentException(
              String.format(
                  "Value '%s' of assertion type '%s' is not a list", expect, assertionType));
        }
        break;
      default:
        throw new IllegalArgumentException("Unhandled assertion type: " + assertionType);
    }
    _assertionType = assertionType;
    _expect = expect;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof JsonPathAssertion)) {
      return false;
    }
    JsonPathAssertion other = (JsonPathAssertion) o;
    return Objects.equal(_assertionType, other.getType())
        && Objects.equal(_expect, other.getExpect());
  }

  public boolean evaluate(Set<JsonPathResultEntry> resultEntries) {
    switch (getType()) {
      case countequals:
        return resultEntries.size() == _expect.asInt();
      case countlessthan:
        return resultEntries.size() < _expect.asInt();
      case countmorethan:
        return resultEntries.size() > _expect.asInt();
      case equals:
        Set<JsonPathResultEntry> expectedEntries = new HashSet<>();
        for (final JsonNode nodeEntry : _expect) {
          try {
            JsonPathResultEntry expectedEntry =
                BatfishObjectMapper.mapper()
                    .readValue(nodeEntry.toString(), JsonPathResultEntry.class);
            expectedEntries.add(expectedEntry);
          } catch (IOException e) {
            throw new BatfishException(
                "Could not convert '" + nodeEntry + "' to JsonPathResultEntry", e);
          }
        }
        SetView<JsonPathResultEntry> difference1 = Sets.difference(expectedEntries, resultEntries);
        SetView<JsonPathResultEntry> difference2 = Sets.difference(resultEntries, expectedEntries);
        return difference1.isEmpty() && difference2.isEmpty();
      default:
        throw new BatfishException("Unhandled assertion type: " + getType());
    }
  }

  @JsonProperty(PROP_TYPE)
  public JsonPathAssertionType getType() {
    return _assertionType;
  }

  @JsonProperty(PROP_EXPECT)
  public JsonNode getExpect() {
    return _expect;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(_assertionType, _expect);
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
