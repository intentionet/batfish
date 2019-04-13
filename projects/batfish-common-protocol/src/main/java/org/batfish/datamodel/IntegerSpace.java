package org.batfish.datamodel;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** A {@link NumberSpace} of {@link Integer}s */
@ParametersAreNonnullByDefault
public final class IntegerSpace extends NumberSpace<Integer, IntegerSpace, IntegerSpace.Builder> {

  protected IntegerSpace(RangeSet<Integer> rangeset) {
    super(rangeset);
  }

  /** Empty integer space */
  public static final IntegerSpace EMPTY = builder().build();

  /** A range expressing TCP/UDP ports */
  public static final IntegerSpace PORTS = builder().including(Range.closed(0, 65535)).build();

  @Override
  protected @Nonnull DiscreteDomain<Integer> discreteDomain() {
    return DiscreteDomain.integers();
  }

  @Override
  protected @Nonnull IntegerSpace getThis() {
    return this;
  }

  @Override
  protected @Nonnull Builder newBuilder() {
    return builder();
  }

  @JsonCreator
  @VisibleForTesting
  @Nonnull
  static IntegerSpace create(@Nullable String s) {
    return IntegerSpace.Builder.create(s).build();
  }

  @Override
  protected @Nonnull IntegerSpace empty() {
    return EMPTY;
  }

  public static @Nonnull IntegerSpace parse(String s) {
    return create(s);
  }

  /** Return this space as a set of included {@link SubRange}s */
  public Set<SubRange> getSubRanges() {
    return _rangeset.asRanges().stream()
        .map(r -> new SubRange(r.lowerEndpoint(), r.upperEndpoint() - 1))
        .collect(ImmutableSet.toImmutableSet());
  }

  /** Return an ordered set of integers described by this space. */
  public Set<Integer> enumerate() {
    return ImmutableRangeSet.copyOf(_rangeset).asSet(DiscreteDomain.integers());
  }

  /** Returns a stream of the included integers. */
  public IntStream intStream() {
    return stream().mapToInt(Integer::intValue);
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

  /** Create a new integer space from a {@link RangeSet} */
  public static IntegerSpace of(RangeSet<Integer> rangeSet) {
    return builder().includingAll(rangeSet).build();
  }

  /** Create a new singleton integer space from an integer value */
  public static IntegerSpace of(int value) {
    return builder().including(Range.singleton(value)).build();
  }

  public static @Nonnull Builder builder() {
    return new Builder();
  }

  /** A builder for {@link IntegerSpace} */
  public static final class Builder extends NumberSpace.Builder<Integer, IntegerSpace, Builder> {
    /** Include given {@code longs}. */
    public final Builder includingAllSubranges(Iterable<SubRange> numbers) {
      numbers.forEach(this::including);
      return getThis();
    }

    private Builder() {
      super();
    }

    @Override
    protected @Nonnull Builder getThis() {
      return this;
    }

    @Override
    protected @Nonnull IntegerSpace build(RangeSet<Integer> rangeSet) {
      return new IntegerSpace(rangeSet);
    }

    @Override
    protected Range<Integer> parse(String s) {
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

    @Override
    protected DiscreteDomain<Integer> discreteDomain() {
      return DiscreteDomain.integers();
    }

    private Builder(IntegerSpace space) {
      super(space);
    }

    /** Include a {@link SubRange} */
    public Builder excluding(SubRange range) {
      if (!range.isEmpty()) {
        excluding(
            Range.closed(range.getStart(), range.getEnd()).canonical(DiscreteDomain.integers()));
      }
      return this;
    }

    /** Include a {@link SubRange} */
    public Builder including(SubRange range) {
      if (!range.isEmpty()) {
        including(
            Range.closed(range.getStart(), range.getEnd()).canonical(DiscreteDomain.integers()));
      }
      return this;
    }

    @JsonCreator
    @Nonnull
    @VisibleForTesting
    static Builder create(@Nullable String s) {
      Builder builder = new Builder();
      NumberSpace.Builder.create(builder, s);
      return builder;
    }
  }

  private static final long serialVersionUID = 1L;
}
