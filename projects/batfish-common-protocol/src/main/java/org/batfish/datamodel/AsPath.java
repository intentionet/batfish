package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.commons.lang3.StringUtils;

@ParametersAreNonnullByDefault
public class AsPath implements Serializable, Comparable<AsPath> {

  private static final long serialVersionUID = 1L;

  /**
   * Returns true iff the provided AS number is reserved for private use by RFC 6696:
   * https://tools.ietf.org/html/rfc6996#section-5
   */
  public static boolean isPrivateAs(long as) {
    return (as >= 64512L && as <= 65534L) || (as >= 4200000000L && as <= 4294967294L);
  }

  public static AsPath ofSingletonAsSets(Long... asNums) {
    return ofSingletonAsSets(Arrays.asList(asNums));
  }

  public static AsPath ofSingletonAsSets(List<Long> asNums) {
    return createAsPath(asNums.stream().map(AsSet::of).collect(Collectors.toList()));
  }

  public static List<AsSet> removePrivateAs(List<AsSet> asPath) {
    return asPath
        .stream()
        .map(
            asSet ->
                asSet
                    .getAsSet()
                    .stream()
                    .filter(as -> !AsPath.isPrivateAs(as))
                    .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())))
        .filter(asnList -> !asnList.isEmpty())
        .map(AsSet::of)
        .collect(ImmutableList.toImmutableList());
  }

  private final List<AsSet> _asSets;

  private static final Cache<List<AsSet>, AsPath> VALUES_CACHE =
      CacheBuilder.newBuilder().softValues().maximumSize(1 << 16).build();

  private AsPath(ImmutableList<AsSet> asSets) {
    _asSets = asSets;
  }

  @JsonCreator
  private static AsPath jsonCreator(@Nullable ImmutableList<AsSet> value) {
    return createAsPath(firstNonNull(value, ImmutableList.of()));
  }

  public static AsPath createAsPath(AsSet asSet) {
    return AsPath.createAsPath(ImmutableList.of(asSet));
  }

  public static AsPath createAsPath(List<AsSet> asSets) {
    ImmutableList<AsSet> immutableValue = ImmutableList.copyOf(asSets);
    try {
      return VALUES_CACHE.get(immutableValue, () -> new AsPath(immutableValue));
    } catch (ExecutionException e) {
      // This shouldn't happen, but handle anyway.
      return new AsPath(immutableValue);
    }
  }

  @Override
  public int compareTo(AsPath rhs) {
    return Comparators.lexicographical(Ordering.<AsSet>natural()).compare(_asSets, rhs._asSets);
  }

  public boolean containsAs(Long as) {
    return _asSets.stream().anyMatch(a -> a.getAsSet().contains(as));
  }

  private static List<SortedSet<Long>> copyAsSets(List<SortedSet<Long>> asSets) {
    List<SortedSet<Long>> newAsSets = new ArrayList<>(asSets.size());
    for (SortedSet<Long> asSet : asSets) {
      SortedSet<Long> newAsSet = ImmutableSortedSet.copyOf(asSet);
      newAsSets.add(newAsSet);
    }
    return newAsSets;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof AsPath)) {
      return false;
    }
    AsPath other = (AsPath) obj;
    return _asSets.equals(other._asSets);
  }

  public String getAsPathString() {
    return StringUtils.join(_asSets, " ");
  }

  @JsonValue
  public List<AsSet> getAsSets() {
    return _asSets;
  }

  @Override
  public int hashCode() {
    return _asSets.hashCode();
  }

  public int size() {
    return _asSets.size();
  }

  @Override
  public String toString() {
    return _asSets.toString();
  }
}
