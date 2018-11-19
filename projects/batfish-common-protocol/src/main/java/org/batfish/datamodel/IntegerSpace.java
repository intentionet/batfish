package org.batfish.datamodel;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A finite, closed, not necessarily contiguous space of integers. Designed to be able to represent
 * set of port ranges or other packet fields that are integers. Integer spaces are by design
 * <i>immutable</i>, but can be altered by converting {@link #toBuilder()} and recreated again.
 */
@ParametersAreNonnullByDefault
public final class IntegerSpace implements Serializable {
  /** Empty integer space */
  public static final IntegerSpace EMPTY = builder().build();

  /** A range expressing TCP/UDP ports */
  public static final IntegerSpace PORTS = builder().including(Range.closed(0, 65535)).build();

  private static final String ERROR_MESSAGE_TEMPLATE = "Invalid range specification %s";

  /*
   * Invariant: always ensure ranges are stored in canonical form (enforced in builder methods)
   * and immutable (enforced in constructor)
   */
  @Nonnull private final RangeSet<Integer> _rangeset;

  private IntegerSpace(RangeSet<Integer> rangeset) {
    _rangeset = ImmutableRangeSet.copyOf(rangeset);
  }

  @JsonCreator
  @VisibleForTesting
  @Nonnull
  static IntegerSpace create(@Nullable String s) {
    return IntegerSpace.Builder.create(s).build();
  }

  /** This space as a set of included {@link Range}s */
  public Set<Range<Integer>> getRanges() {
    return _rangeset.asRanges();
  }

  /** Return this space as a set of included {@link SubRange}s */
  public Set<SubRange> getSubRanges() {
    return _rangeset
        .asRanges()
        .stream()
        .map(r -> new SubRange(r.lowerEndpoint(), r.upperEndpoint() - 1))
        .collect(ImmutableSet.toImmutableSet());
  }

  /** Check that this space contains a given {@code value}. */
  public boolean contains(int value) {
    return _rangeset.contains(value);
  }

  /** Check that this space *fully* contains the {@code other} integer space. */
  public boolean contains(IntegerSpace other) {
    return _rangeset.enclosesAll(other._rangeset.asRanges());
  }

  /** Return an ordered set of integers described by this space. */
  public Set<Integer> enumerate() {
    return ImmutableRangeSet.copyOf(_rangeset).asSet(DiscreteDomain.integers());
  }

  /** Returns true if this space is a contiguous space */
  public boolean isContiguous() {
    return _rangeset.asRanges().size() <= 1;
  }

  /** Return true iff this space is empty (contains no values) */
  public boolean isEmpty() {
    return _rangeset.isEmpty();
  }

  /** Return true iff this space is a singleton (contains exactly one value) */
  public boolean isSingleton() {
    return getRanges().size() == 1 && isSingletonRange(getRanges().iterator().next());
  }

  private static boolean isSingletonRange(Range<Integer> range) {
    return range.upperEndpoint() - range.lowerEndpoint() == 1;
  }

  /**
   * Return singleton value if this space is a singleton. Otherwise throws {@link
   * NoSuchElementException}
   */
  public int singletonValue() throws NoSuchElementException {
    if (!isSingleton()) {
      throw new NoSuchElementException();
    }
    return _rangeset.asRanges().iterator().next().lowerEndpoint();
  }

  /** Returns a stream of the included integers. */
  public IntStream stream() {
    return enumerate().stream().mapToInt(Integer::intValue);
  }

  /** Intersect two integer spaces together. */
  public IntegerSpace intersection(IntegerSpace other) {
    return new IntegerSpace(
        other
            ._rangeset
            .asRanges()
            .stream()
            .map(_rangeset::subRangeSet) // intersect individual ranges with _rangeset
            .map(RangeSet::asRanges) // flatten each intersection result to set of ranges
            .flatMap(Set::stream) // stream for collection
            .collect(ImmutableRangeSet.toImmutableRangeSet()));
  }

  /** Union two integer spaces together */
  public IntegerSpace union(IntegerSpace other) {
    Builder builder = this.toBuilder();
    other._rangeset.asRanges().forEach(builder::including);
    return builder.build();
  }

  /** Take the complement of this space, bounded by some other {@link IntegerSpace} */
  public IntegerSpace not(IntegerSpace within) {
    return new IntegerSpace(_rangeset.complement()).intersection(within);
  }

  /**
   * Take the complement of this space, bounded by existing lower and upper limits of the space.
   * This can be used as a way to represent a set of excluded ranges as a positive space.
   */
  public IntegerSpace not() {
    if (_rangeset.isEmpty()) {
      return EMPTY;
    }
    return new IntegerSpace(_rangeset.complement().subRangeSet(_rangeset.span()));
  }

  /** Compute the difference between two integer spaces */
  public IntegerSpace difference(IntegerSpace other) {
    return this.intersection(other.not(this));
  }

  /** Compute the symmetric difference between two integer spaces */
  public IntegerSpace symmetricDifference(IntegerSpace other) {
    return this.union(other).difference(intersection(other));
  }

  /** Return a builder initialized with existing integer space */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /** Create a new integer space from a {@link SubRange} */
  public static IntegerSpace of(SubRange range) {
    return builder().including(range).build();
  }

  /** Create a new integer space containing the union of the given {@link SubRange ranges}. */
  public static IntegerSpace unionOf(SubRange... ranges) {
    return unionOf(Arrays.asList(ranges));
  }

  /** Create a new integer space containing the union of the given {@link SubRange ranges}. */
  public static IntegerSpace unionOf(Iterable<SubRange> ranges) {
    Builder b = builder();
    for (SubRange range : ranges) {
      b.including(range);
    }
    return b.build();
  }

  /** Create a new integer space from a {@link Range} */
  public static IntegerSpace of(Range<Integer> range) {
    return builder().including(range).build();
  }

  /** Create a new singleton integer space from an integer value */
  public static IntegerSpace of(int value) {
    return builder().including(Range.singleton(value)).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  /** A builder for {@link IntegerSpace} */
  public static final class Builder {
    private Set<Range<Integer>> _including;
    private Set<Range<Integer>> _excluding;

    private Builder() {
      _including = new HashSet<>();
      _excluding = new HashSet<>();
    }

    private Builder(IntegerSpace space) {
      this();
      _including.addAll(space._rangeset.asRanges());
    }

    /** Include a {@link SubRange} */
    public Builder including(SubRange range) {
      if (!range.isEmpty()) {
        _including.add(
            Range.closed(range.getStart(), range.getEnd()).canonical(DiscreteDomain.integers()));
      }
      return this;
    }

    /** Include an integer. */
    public Builder including(@Nonnull Integer range) {
      return including(Range.singleton(range));
    }

    /** Include a range. The {@link Range} must be a finite range. */
    public Builder including(Range<Integer> range) {
      checkArgument(
          range.hasLowerBound() && range.hasUpperBound(), "Infinite ranges are not supported");
      if (!range.isEmpty()) {
        _including.add(range.canonical(DiscreteDomain.integers()));
      }
      return this;
    }

    /** Include an {@link IntegerSpace} */
    public Builder including(IntegerSpace space) {
      space._rangeset.asRanges().forEach(this::including);
      return this;
    }

    /** Exclude an integer. */
    public Builder excluding(@Nonnull Integer range) {
      return excluding(Range.singleton(range));
    }

    /** Exclude a {@link SubRange} */
    public Builder excluding(SubRange range) {
      if (!range.isEmpty()) {
        _excluding.add(
            Range.closed(range.getStart(), range.getEnd()).canonical(DiscreteDomain.integers()));
      }
      return this;
    }

    /** Exclude a range. The {@link Range} must be finite range. */
    public Builder excluding(Range<Integer> range) {
      checkArgument(
          range.hasLowerBound() && range.hasUpperBound(), "Infinite ranges are not supported");
      if (!range.isEmpty()) {
        _excluding.add(range.canonical(DiscreteDomain.integers()));
      }
      return this;
    }

    /** Exclude an {@link IntegerSpace} */
    public Builder excluding(IntegerSpace space) {
      space._rangeset.asRanges().forEach(this::excluding);
      return this;
    }

    /**
     * Returns true if this builder has exclusions only, no positive space.
     *
     * <p>Serves as utility function to determine if special handling for such negative-only cases
     * is required (otherwise empty spaces will be built)
     */
    public boolean hasExclusionsOnly() {
      return _including.isEmpty() && !_excluding.isEmpty();
    }

    /** Returns a new {@link IntegerSpace} */
    public IntegerSpace build() {
      RangeSet<Integer> rangeSet = TreeRangeSet.create(_including);
      rangeSet.removeAll(_excluding);
      return new IntegerSpace(rangeSet);
    }

    @JsonCreator
    @Nonnull
    @VisibleForTesting
    static Builder create(@Nullable String s) {
      if (Strings.isNullOrEmpty(s)) {
        return builder();
      }
      String[] atoms = s.trim().split(",", -1);
      Builder builder = builder();
      Arrays.stream(atoms).forEach(atom -> processStringAtom(atom.trim(), builder));
      return builder;
    }

    private static Range<Integer> parse(String s) {
      try {
        int i = Integer.parseUnsignedInt(s);
        return (Range.closed(i, i));
      } catch (NumberFormatException e) {
        String[] endpoints = s.split("-");
        checkArgument((endpoints.length == 2), ERROR_MESSAGE_TEMPLATE, s);
        int low = Integer.parseUnsignedInt(endpoints[0].trim());
        int high = Integer.parseUnsignedInt(endpoints[1].trim());
        checkArgument(low <= high, ERROR_MESSAGE_TEMPLATE, s);
        return Range.closed(low, high);
      }
    }

    private static void processStringAtom(String s, Builder builder) {
      if (s.startsWith("!")) {
        builder.excluding(parse(s.replaceAll("!", "")));
      } else {
        builder.including(parse(s));
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IntegerSpace)) {
      return false;
    }
    IntegerSpace that = (IntegerSpace) o;
    return Objects.equals(_rangeset, that._rangeset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_rangeset);
  }

  private static String toRangeString(Range<Integer> r) {
    int lower = r.lowerEndpoint();
    int upper = r.upperEndpoint() - 1;
    if (lower == upper) {
      return Integer.toString(lower);
    }
    return lower + "-" + upper;
  }

  @JsonValue
  @Override
  public String toString() {
    return getRanges().stream().map(IntegerSpace::toRangeString).collect(Collectors.joining(","));
  }

  private static final long serialVersionUID = 1L;
}
