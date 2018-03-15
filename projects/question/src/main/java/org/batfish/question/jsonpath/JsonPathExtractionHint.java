package org.batfish.question.jsonpath;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jayway.jsonpath.internal.path.PathCompiler;
import java.io.IOException;
import org.batfish.common.BatfishException;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.questions.DisplayHints.Extraction;

public class JsonPathExtractionHint {

  public enum UseType {
    PREFIX,
    FUNCOFSUFFIX,
    PREFIXOFSUFFIX,
    SUFFIXOFSUFFIX,
  }

  private static final String PROP_FILTER = "filter";

  private static final String PROP_INDEX = "index";

  private static final String PROP_USE = "use";

  private String _filter;

  private Integer _index;

  private UseType _use;

  public static JsonPathExtractionHint fromExtractionHint(Extraction extraction)
      throws IOException {
    JsonPathExtractionHint jpExtractionHint =
        BatfishObjectMapper.clone(extraction.getMethod(), JsonPathExtractionHint.class);

    // sanity check what we got
    if (jpExtractionHint.getUse() == null) {
      throw new BatfishException("Unspecified use type in extraction hint");
    }
    switch (jpExtractionHint.getUse()) {
      case PREFIX:
        if (jpExtractionHint.getIndex() == null) {
          throw new BatfishException("Index should be specified in prefix-based extraction hint");
        }
        if (jpExtractionHint.getFilter() != null) {
          throw new BatfishException("Filter should not specified in prefix-based extraction hint");
        }
        break;
      case FUNCOFSUFFIX:
        if (jpExtractionHint.getIndex() != null) {
          throw new BatfishException(
              "Index should not be specified in funcofsuffix-based extraction hint");
        }
        if (jpExtractionHint.getFilter() == null) {
          throw new BatfishException(
              "Filter should be specified in funcofsuffix-based extraction hint");
        }
        if (!PathCompiler.compile(jpExtractionHint.getFilter()).isFunctionPath()) {
          throw new BatfishException(
              "Filter should be a path function in funcofsuffix-based extraction hint");
        }
        break;
      case PREFIXOFSUFFIX:
        if (jpExtractionHint.getIndex() == null) {
          throw new BatfishException(
              "Index should be specified in prefixofsuffix-based extraction hint");
        }
        if (jpExtractionHint.getFilter() == null) {
          throw new BatfishException(
              "Filter should be specified in prefixofsuffix-based extraction hint");
        }
        break;
      case SUFFIXOFSUFFIX:
        if (jpExtractionHint.getIndex() != null) {
          throw new BatfishException(
              "Index should not be specified in suffixofsuffix-based extraction hint");
        }
        if (jpExtractionHint.getFilter() == null) {
          throw new BatfishException(
              "Filter should be specified in suffixofsuffix-based extraction hint");
        }
        break;
      default:
        throw new BatfishException("Unknown use type " + jpExtractionHint.getUse());
    }

    return jpExtractionHint;
  }

  @JsonProperty(PROP_FILTER)
  public String getFilter() {
    return _filter;
  }

  @JsonProperty(PROP_INDEX)
  public Integer getIndex() {
    return _index;
  }

  @JsonProperty(PROP_USE)
  public UseType getUse() {
    return _use;
  }

  @JsonProperty(PROP_FILTER)
  public void setFilter(String filter) {
    _filter = filter;
  }

  @JsonProperty(PROP_INDEX)
  public void setIndex(int index) {
    _index = index;
  }

  @JsonProperty(PROP_USE)
  public void setUse(UseType use) {
    _use = use;
  }
}
