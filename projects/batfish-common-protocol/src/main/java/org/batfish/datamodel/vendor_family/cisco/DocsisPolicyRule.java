package org.batfish.datamodel.vendor_family.cisco;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.util.ComparableStructure;

public class DocsisPolicyRule extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  @JsonCreator
  private DocsisPolicyRule(@JsonProperty(PROP_NAME) String number) {
    super(number);
    _definitionLine = -1;
  }

  public DocsisPolicyRule(String number, int definitionLine) {
    super(number);
    _definitionLine = definitionLine;
  }

  @JsonIgnore
  public int getDefinitionLine() {
    return _definitionLine;
  }
}
