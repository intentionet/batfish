package org.batfish.datamodel.dataplane.rib;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Represents the configuration for defining a Rib Group -- a collection of named RIBs, with
 * attached import policies.
 */
@ParametersAreNonnullByDefault
public final class RibGroup implements Serializable {
  private static final long serialVersionUID = 1;

  private static final String PROP_NAME = "name";
  private static final String PROP_EXPORT_RIB = "exportRib";
  private static final String PROP_IMPORT_POLICY = "importPolicies";
  private static final String PROP_RIB_IDS = "ribIds";

  @Nonnull private final String _name;
  @Nonnull private final List<RibId> _importRibs;
  @Nonnull private final String _importPolicy;
  @Nullable private final RibId _exportRib;

  /** Create a new RibGroup */
  public RibGroup(
      String name, List<RibId> importRibs, String importPolicy, @Nullable RibId exportRib) {
    _name = name;
    _exportRib = exportRib;
    _importPolicy = importPolicy;
    _importRibs = importRibs;
  }

  @JsonCreator
  private static RibGroup create(
      @Nullable @JsonProperty(PROP_NAME) String name,
      @Nullable @JsonProperty(PROP_RIB_IDS) List<RibId> ribIds,
      @Nullable @JsonProperty(PROP_IMPORT_POLICY) String importPolicy,
      @Nullable @JsonProperty(PROP_EXPORT_RIB) RibId exportRib) {
    checkArgument(name != null, "RibGroup: missing %s", PROP_NAME);
    checkArgument(ribIds != null, "RibGroup: missing %s", PROP_RIB_IDS);
    checkArgument(importPolicy != null, "RibGroup: missing %s", PROP_IMPORT_POLICY);
    return new RibGroup(name, ribIds, importPolicy, exportRib);
  }

  @Nonnull
  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }

  @Nullable
  @JsonProperty(PROP_EXPORT_RIB)
  public RibId getExportRib() {
    return _exportRib;
  }

  /**
   * Return the import policy to apply when importing routes from protocol RIBs into {@link
   * #_importRibs}
   */
  @Nonnull
  @JsonProperty(PROP_IMPORT_POLICY)
  public String getImportPolicy() {
    return _importPolicy;
  }

  @Nonnull
  @JsonProperty(PROP_RIB_IDS)
  public List<RibId> getImportRibs() {
    return _importRibs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RibGroup)) {
      return false;
    }
    RibGroup ribGroup = (RibGroup) o;
    return Objects.equals(_name, ribGroup._name)
        && Objects.equals(_exportRib, ribGroup._exportRib)
        && Objects.equals(_importPolicy, ribGroup._importPolicy)
        && Objects.equals(_importRibs, ribGroup._importRibs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, _exportRib, _importPolicy, _importRibs);
  }
}
