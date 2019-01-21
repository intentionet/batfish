package org.batfish.representation.juniper;

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Rib group configuration */
public class RibGroup implements Serializable {

  private static final long serialVersionUID = 1;

  @Nonnull private String _name;
  @Nullable private String _exportRib;
  @Nonnull private List<String> _importPolicies;
  @Nonnull private List<String> _importRibs;

  public RibGroup(@Nonnull String name) {
    _name = name;
    _importPolicies = ImmutableList.of();
    _importRibs = ImmutableList.of();
  }

  @Nonnull
  public String getName() {
    return _name;
  }

  public void setName(@Nonnull String name) {
    _name = name;
  }

  @Nullable
  public String getExportRib() {
    return _exportRib;
  }

  public void setExportRib(@Nonnull String rib) {
    _exportRib = rib;
  }

  @Nonnull
  public List<String> getImportPolicies() {
    return _importPolicies;
  }

  public void addImportPolicy(@Nonnull String policy) {
    _importPolicies = ImmutableList.<String>builder().addAll(_importPolicies).add(policy).build();
  }

  @Nonnull
  public List<String> getImportRibs() {
    return _importRibs;
  }

  public void addImportRib(@Nonnull String importRib) {
    _importRibs = ImmutableList.<String>builder().addAll(_importRibs).add(importRib).build();
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
        && Objects.equals(_importPolicies, ribGroup._importPolicies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, _exportRib, _importPolicies);
  }
}
