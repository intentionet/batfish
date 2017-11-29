package org.batfish.datamodel.vendor_family.cisco;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.batfish.common.util.ComparableStructure;
import org.batfish.common.util.DefinedStructure;

public class DocsisPolicy extends ComparableStructure<String> implements DefinedStructure {

  /** */
  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private List<String> _rules;

  @JsonCreator
  private DocsisPolicy(@JsonProperty(PROP_NAME) String number) {
    super(number);
    _rules = new ArrayList<>();
    _definitionLine = -1;
  }

  public DocsisPolicy(String number, int definitionLine) {
    super(number);
    _rules = new ArrayList<>();
    _definitionLine = definitionLine;
  }

  @JsonIgnore
  @Override
  public int getDefinitionLine() {
    return _definitionLine;
  }

  public List<String> getRules() {
    return _rules;
  }

  public void setRules(List<String> rules) {
    _rules = rules;
  }
}
