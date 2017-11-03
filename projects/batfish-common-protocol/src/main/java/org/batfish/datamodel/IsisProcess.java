package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

@JsonSchemaDescription("An IS-IS routing process")
public class IsisProcess implements Serializable {

  public static final int DEFAULT_ISIS_INTERFACE_COST = 10;

  /** */
  private static final long serialVersionUID = 1L;

  private Set<GeneratedRoute> _generatedRoutes;

  private IsisLevel _level;

  private IsoAddress _netAddress;

  public IsisProcess() {
    _generatedRoutes = new LinkedHashSet<>();
  }

  @JsonPropertyDescription(
      "Generated IPV4 routes for the purpose of export into IS-IS. These routes are not imported "
          + "into the main RIB.")
  public Set<GeneratedRoute> getGeneratedRoutes() {
    return _generatedRoutes;
  }

  @JsonPropertyDescription("The IS-IS level(s) for this process")
  public IsisLevel getLevel() {
    return _level;
  }

  @JsonPropertyDescription("The net address is an ISO address representing the IS-IS router ID.")
  public IsoAddress getNetAddress() {
    return _netAddress;
  }

  public void setGeneratedRoutes(Set<GeneratedRoute> generatedRoutes) {
    _generatedRoutes = generatedRoutes;
  }

  public void setLevel(IsisLevel level) {
    _level = level;
  }

  public void setNetAddress(IsoAddress netAddress) {
    _netAddress = netAddress;
  }
}
