package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.batfish.common.util.CommonUtil;

public class AsPath implements Serializable, Comparable<AsPath> {

  private static final long serialVersionUID = 1L;

  /**
   * Returns true iff the provided AS number is reserved for private use by RFC 6696:
   * https://tools.ietf.org/html/rfc6996#section-5
   */
  public static boolean isPrivateAs(int as) {
    return (as >= 64512 && as <= 65535);
  }

  public static AsPath ofSingletonAsSets(Integer... asNums) {
    return ofSingletonAsSets(Arrays.asList(asNums));
  }

  public static AsPath ofSingletonAsSets(List<Integer> asNums) {
    return new AsPath(asNums.stream().map(ImmutableSortedSet::of).collect(Collectors.toList()));
  }

  public static List<SortedSet<Integer>> removePrivateAs(List<SortedSet<Integer>> asPath) {
    return asPath
        .stream()
        .map(
            asSet ->
                asSet
                    .stream()
                    .filter(as -> !AsPath.isPrivateAs(as))
                    .collect(ImmutableSortedSet.toImmutableSortedSet(Integer::compare)))
        .filter(asSet -> !asSet.isEmpty())
        .collect(ImmutableList.toImmutableList());
  }

  private final List<SortedSet<Integer>> _asSets;

  @JsonCreator
  public AsPath(List<SortedSet<Integer>> asSets) {
    _asSets = copyAsSets(asSets);
  }

  @Override
  public int compareTo(@Nonnull AsPath rhs) {
    Iterator<SortedSet<Integer>> l = _asSets.iterator();
    Iterator<SortedSet<Integer>> r = rhs._asSets.iterator();
    while (l.hasNext()) {
      if (!r.hasNext()) {
        return 1;
      }
      SortedSet<Integer> lVal = l.next();
      SortedSet<Integer> rVal = r.next();
      int ret = CommonUtil.compareCollection(lVal, rVal);
      if (ret != 0) {
        return ret;
      }
    }
    if (r.hasNext()) {
      return -1;
    }
    return 0;
  }

  public boolean containsAs(int as) {
    for (Set<Integer> asSet : _asSets) {
      if (asSet.contains(as)) {
        return true;
      }
    }
    return false;
  }

  private List<SortedSet<Integer>> copyAsSets(List<SortedSet<Integer>> asSets) {
    List<SortedSet<Integer>> newAsSets = new ArrayList<>(asSets.size());
    for (SortedSet<Integer> asSet : asSets) {
      SortedSet<Integer> newAsSet = ImmutableSortedSet.copyOf(asSet);
      newAsSets.add(newAsSet);
    }
    return newAsSets;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AsPath other = (AsPath) obj;
    if (_asSets == null) {
      if (other._asSets != null) {
        return false;
      }
    } else if (!_asSets.equals(other._asSets)) {
      return false;
    }
    return true;
  }

  public String getAsPathString() {
    StringBuilder sb = new StringBuilder();
    for (Set<Integer> asSet : _asSets) {
      if (asSet.size() == 1) {
        int elem = asSet.iterator().next();
        sb.append(elem);
      } else {
        sb.append("{");
        Iterator<Integer> i = asSet.iterator();
        sb.append(i.next());
        while (i.hasNext()) {
          sb.append(",");
          sb.append(i.next());
        }
        sb.append("}");
      }
      sb.append(" ");
    }
    return sb.toString().trim();
  }

  @JsonValue
  public List<SortedSet<Integer>> getAsSets() {
    return copyAsSets(_asSets);
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
