package org.batfish.minesweeper.bdd;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This class represents the results of symbolic analysis of a {@link
 * org.batfish.datamodel.routing_policy.communities.SetCommunities} routing statement, as performed
 * by the {@link SetCommunitiesVisitor}.
 *
 * <p>In general, setting communities depends on the current communities in the route, which are
 * represented by the {@link org.batfish.datamodel.routing_policy.communities.InputCommunities}
 * expression. Hence, symbolic analysis has the effect of partitioning the atomic predicates into
 * three sets: those that are definitely set by the statement; those that are definitely removed by
 * the statement; and those that are on the route announcement after the statement if and only if
 * they were beforehand. We maintain the first two sets explicitly, while the third set is implicit
 * (everything not in the other two sets).
 *
 * <p>For example, consider representing the effects of setting (InputCommunities U 20:30), which is
 * how community addition is modeled. In this case, we know that after the statement 20:30 is
 * definitely set, nothing is definitely removed; and everything else is on the route if and only if
 * it was originally. Hence, this class can be viewed as representing a non-standard form of
 * "three-valued" set, where some elements are "maybe" in the set.
 */
@ParametersAreNonnullByDefault
public class CommunityAPDispositions {

  private final int _numAPs;
  // the atomic predicates that are definitely on the route announcement
  @Nonnull private final Set<Integer> _mustExist;
  // the atomic predicates that are definitely not on the route announcement
  @Nonnull private final Set<Integer> _mustNotExist;

  public CommunityAPDispositions(int numAPs, Set<Integer> mustExist, Set<Integer> mustNotExist) {
    assert mustExist.stream().allMatch(i -> i >= 0 && i < numAPs)
        : "community atomic predicates must be in the range [0, numAPs)";
    assert mustNotExist.stream().allMatch(i -> i >= 0 && i < numAPs)
        : "community atomic predicates must be in the range [0, numAPs)";
    _numAPs = numAPs;
    _mustExist = ImmutableSet.copyOf(mustExist);
    _mustNotExist = ImmutableSet.copyOf(mustNotExist);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CommunityAPDispositions)) {
      return false;
    }
    CommunityAPDispositions other = (CommunityAPDispositions) obj;
    return _numAPs == other._numAPs
        && _mustExist.equals(other._mustExist)
        && _mustNotExist.equals(other._mustNotExist);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_numAPs, _mustExist, _mustNotExist);
  }

  // produce the set difference of two CommunityAPDispositions; the result is only
  // representable as a CommunityAPDispositions object if the right-hand object is exact
  // (see the isExact method below), so we require that to be the case.
  // (if needed we can extend this object to be able to represent the more general case.)
  public CommunityAPDispositions diff(CommunityAPDispositions other) {
    assert _numAPs == other._numAPs
        : "diffed CommunityAPDispositions must have the same number of atomic predicates";
    assert other.isExact() : "the right-hand CommunityAPDisposition in a diff must be exact";
    return new CommunityAPDispositions(
        _numAPs,
        setIntersect(_mustExist, other.getMustNotExist()),
        setUnion(_mustNotExist, other.getMustExist()));
  }

  // produce the set union of two CommunityAPDispositions
  public CommunityAPDispositions union(CommunityAPDispositions other) {
    assert _numAPs == other._numAPs
        : "unioned CommunityAPDispositions must have the same number of atomic predicates";
    return new CommunityAPDispositions(
        _numAPs,
        setUnion(_mustExist, other.getMustExist()),
        setIntersect(_mustNotExist, other.getMustNotExist()));
  }

  // the empty CommunityAPDispositions object has all atomic predicates in the
  // mustNotExist set
  public static CommunityAPDispositions empty(BDDRoute bddRoute) {
    int numAPs = bddRoute.getCommunityAtomicPredicates().length;
    return new CommunityAPDispositions(
        numAPs,
        ImmutableSet.of(),
        IntStream.range(0, numAPs).boxed().collect(ImmutableSet.toImmutableSet()));
  }

  // create a CommunityAPDispositions object representing exactly the given set aps;
  // all other atomic predicates are put in the mustNotExist set
  public static CommunityAPDispositions exactly(Set<Integer> aps, BDDRoute bddRoute) {
    int numAPs = bddRoute.getCommunityAtomicPredicates().length;
    return new CommunityAPDispositions(
        numAPs,
        aps,
        IntStream.range(0, numAPs)
            .filter(i -> !aps.contains(i))
            .boxed()
            .collect(ImmutableSet.toImmutableSet()));
  }

  // an exact CommunityAPDispositions object has no atomic predicates that have unknown
  // status
  public boolean isExact() {
    return IntStream.range(0, _numAPs)
        .allMatch(i -> _mustExist.contains(i) || _mustNotExist.contains(i));
  }

  public int getNumAPs() {
    return _numAPs;
  }

  public Set<Integer> getMustExist() {
    return _mustExist;
  }

  public Set<Integer> getMustNotExist() {
    return _mustNotExist;
  }

  private static ImmutableSet<Integer> setUnion(Set<Integer> s1, Set<Integer> s2) {
    return ImmutableSet.<Integer>builder().addAll(s1).addAll(s2).build();
  }

  private static ImmutableSet<Integer> setIntersect(Set<Integer> s1, Set<Integer> s2) {
    return s1.stream().filter(s2::contains).collect(ImmutableSet.toImmutableSet());
  }
}
