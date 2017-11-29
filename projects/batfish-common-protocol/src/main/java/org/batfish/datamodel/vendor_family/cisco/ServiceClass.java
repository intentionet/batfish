package org.batfish.datamodel.vendor_family.cisco;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.util.ComparableStructure;
import org.batfish.common.util.DefinedStructure;

public class ServiceClass extends ComparableStructure<String> implements DefinedStructure {

  private static final String PROP_FRIENDLY_NAME = "friendlyName";

  /** */
  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private String _friendlyName;

  @JsonCreator
  private ServiceClass(@JsonProperty(PROP_NAME) String number) {
    super(number);
    _definitionLine = -1;
  }

  public ServiceClass(String number, int definitionLine) {
    super(number);
    _definitionLine = definitionLine;
  }

  @JsonIgnore
  @Override
  public int getDefinitionLine() {
    return _definitionLine;
  }

  @JsonProperty(PROP_FRIENDLY_NAME)
  public String getFriendlyName() {
    return _friendlyName;
  }

  @JsonProperty(PROP_FRIENDLY_NAME)
  public void setFriendlyName(String friendlyName) {
    _friendlyName = friendlyName;
  }
}
