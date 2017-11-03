package org.batfish.datamodel.collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.SortedSet;
import org.batfish.role.OutliersHypothesis;

public class NamedStructureOutlierSet<T> implements Comparable<NamedStructureOutlierSet<T>> {

  private static final String PROP_HYPOTHESIS = "hypothesis";

  private static final String PROP_NAME = "name";

  private static final String PROP_STRUCT_TYPE = "structType";

  private static final String PROP_STRUCT_DEFINITION = "structDefinition";

  private static final String PROP_CONFORMERS = "conformers";

  private static final String PROP_OUTLIERS = "outliers";

  // the hypothesis used to identify outliers
  private OutliersHypothesis _hypothesis;

  /**
   * The name of the structure type (e.g., CommunityList, IpAccessList) for which we are performing
   * outlier detection
   */
  private String _structType;

  /** The name of the structure for which we are performing outlier detection */
  private String _name;

  /**
   * If the hypothesis is SAME_DEFINITION, this field contains the structure definition that is
   * hypothesized to be the correct one. If the hypothesis is SAME_NAME, this field is non-null if
   * the hypothesis is that a structure of this name should exist and null if the hypothesis is that
   * a structure of this name should not exist.
   */
  private T _namedStructure;

  /** A lower bound on the probability at which a hypothesis should be considered to be true */
  private static final double THRESHOLD_PROBABILITY = 0.9;

  /** The nodes that satisfy the hypothesis */
  private SortedSet<String> _conformers;

  /** The nodes that violate the hypothesis */
  private SortedSet<String> _outliers;

  @JsonCreator
  public NamedStructureOutlierSet(
      @JsonProperty(PROP_HYPOTHESIS) OutliersHypothesis hypothesis,
      @JsonProperty(PROP_STRUCT_TYPE) String structType,
      @JsonProperty(PROP_NAME) String name,
      @JsonProperty(PROP_STRUCT_DEFINITION) T namedStructure,
      @JsonProperty(PROP_CONFORMERS) SortedSet<String> conformers,
      @JsonProperty(PROP_OUTLIERS) SortedSet<String> outliers) {
    _hypothesis = hypothesis;
    _structType = structType;
    _name = name;
    _namedStructure = namedStructure;
    _conformers = conformers;
    _outliers = outliers;
  }

  @Override
  // sort in reverse order of zScore, which is a measure of how likely it is that
  // our hypothesis is correct
  public int compareTo(NamedStructureOutlierSet<T> other) {
    int zComp = Double.compare(other.zScore(), this.zScore());
    if (zComp != 0) {
      return zComp;
    }
    int structComp = _structType.compareTo(other.getStructType());
    if (structComp != 0) {
      return structComp;
    }
    return _name.compareTo(other.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof NamedStructureOutlierSet)) {
      return false;
    }
    NamedStructureOutlierSet<?> rhs = (NamedStructureOutlierSet<?>) o;
    return _structType.equals(rhs.getStructType()) && _name.equals(rhs.getName());
  }

  @JsonProperty(PROP_HYPOTHESIS)
  public OutliersHypothesis getHypothesis() {
    return _hypothesis;
  }

  @JsonProperty(PROP_CONFORMERS)
  public SortedSet<String> getConformers() {
    return _conformers;
  }

  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }

  // ignore for now to avoid encoding large amounts of information in answer
  @JsonIgnore
  public T getNamedStructure() {
    return _namedStructure;
  }

  @JsonProperty(PROP_OUTLIERS)
  public SortedSet<String> getOutliers() {
    return _outliers;
  }

  @JsonProperty(PROP_STRUCT_TYPE)
  public String getStructType() {
    return _structType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_structType, _name);
  }

  public double zScore() {
    double e = _conformers.size();
    double n = e + _outliers.size();
    double numerator = e / n - THRESHOLD_PROBABILITY;
    double denominator = Math.sqrt(THRESHOLD_PROBABILITY * (1.0 - THRESHOLD_PROBABILITY) / n);
    return numerator / denominator;
  }
}
