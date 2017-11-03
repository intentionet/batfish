package org.batfish.datamodel.collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.SortedSet;

public class OutlierSet<T> implements Comparable<OutlierSet<T>> {

  private static final String PROP_NAME = "name";

  private static final String PROP_DEFINITION = "definition";

  private static final String PROP_CONFORMERS = "conformers";

  private static final String PROP_OUTLIERS = "outliers";

  /** The name of the configuration property for which we are performing outlier detection */
  private String _name;

  /** The definition that is hypothesized to be the correct definition of that property. */
  private T _definition;

  /** A lower bound on the probability at which a hypothesis should be considered to be true */
  private static final double THRESHOLD_PROBABILITY = 0.9;

  /** The nodes that satisfy the hypothesis */
  private SortedSet<String> _conformers;

  /** The nodes that violate the hypothesis */
  private SortedSet<String> _outliers;

  @JsonCreator
  public OutlierSet(
      @JsonProperty(PROP_NAME) String name,
      @JsonProperty(PROP_DEFINITION) T definition,
      @JsonProperty(PROP_CONFORMERS) SortedSet<String> conformers,
      @JsonProperty(PROP_OUTLIERS) SortedSet<String> outliers) {
    _name = name;
    _definition = definition;
    _conformers = conformers;
    _outliers = outliers;
  }

  @Override
  // sort in reverse order of zScore, which is a measure of how likely it is that
  // our hypothesis is correct
  public int compareTo(OutlierSet<T> other) {
    int zComp = Double.compare(other.zScore(), this.zScore());
    if (zComp != 0) {
      return zComp;
    }
    return _name.compareTo(other.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof OutlierSet)) {
      return false;
    }
    OutlierSet<?> rhs = (OutlierSet<?>) o;
    return _definition.equals(rhs.getDefinition()) && _name.equals(rhs.getName());
  }

  @JsonProperty(PROP_CONFORMERS)
  public SortedSet<String> getConformers() {
    return _conformers;
  }

  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }

  @JsonProperty(PROP_DEFINITION)
  public T getDefinition() {
    return _definition;
  }

  @JsonProperty(PROP_OUTLIERS)
  public SortedSet<String> getOutliers() {
    return _outliers;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_definition, _name);
  }

  public double zScore() {
    double e = _conformers.size();
    double n = e + _outliers.size();
    double numerator = e / n - THRESHOLD_PROBABILITY;
    double denominator = Math.sqrt(THRESHOLD_PROBABILITY * (1.0 - THRESHOLD_PROBABILITY) / n);
    return numerator / denominator;
  }
}
