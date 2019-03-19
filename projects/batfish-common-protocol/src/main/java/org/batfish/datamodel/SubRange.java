package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;

/** A closed interval of integers. */
public final class SubRange implements Serializable, Comparable<SubRange> {

  private static final long serialVersionUID = 1L;

  private final int _end;

  private final int _start;

  /**
   * Create a new subrange. {@code start} and {@code end} are included in the range. If {@code
   * start} is larger than {@code end}, the subrange will be empty.
   */
  public SubRange(int start, int end) throws IllegalArgumentException {
    _start = start;
    _end = end;
  }

  @JsonCreator
  public SubRange(@Nullable Object o) {
    if (o instanceof String) {
      String s = (String) o;
      String[] parts = s.split("-");
      if (parts.length != 2) {
        throw new BatfishException("Invalid subrange: \"" + s + "\"");
      }
      try {
        _start = Integer.parseInt(parts[0]);
      } catch (NumberFormatException e) {
        throw new BatfishException("Invalid subrange start: \"" + parts[0] + "\"", e);
      }
      try {
        _end = Integer.parseInt(parts[1]);
      } catch (NumberFormatException e) {
        throw new BatfishException("Invalid subrange end: \"" + parts[1] + "\"", e);
      }
    } else if (o instanceof Integer) {
      int i = (Integer) o;
      _start = i;
      _end = i;
    } else if (o == null) {
      throw new BatfishException("Cannot create SubRange from null");
    } else {
      throw new BatfishException(
          "Cannot create SubRange from input object of type: " + o.getClass().getCanonicalName());
    }
  }

  public IntStream asStream() {
    return IntStream.range(_start, _end + 1);
  }

  @Override
  public int compareTo(@Nonnull SubRange rhs) {
    return Comparator.comparing(SubRange::getStart)
        .thenComparing(SubRange::getEnd)
        .compare(this, rhs);
  }

  /**
   * Returns true if the {@code other} subrange is <b>fully</b> contained within this subrange. An
   * empty {@code other} is trivially considered to be contained within any subrange.
   */
  public boolean contains(@Nonnull SubRange other) {
    return other.isEmpty() || (_start <= other.getStart() && _end >= other.getEnd());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof SubRange)) {
      return false;
    }
    SubRange other = (SubRange) o;
    return _start == other._start && _end == other._end;
  }

  /** Return the end of the interval. */
  public int getEnd() {
    return _end;
  }

  /** Return the start of the interval. */
  public int getStart() {
    return _start;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_end, _start);
  }

  /** Check whether a given integer belongs to this range. */
  public boolean includes(@Nullable Integer integer) {
    return integer != null && includes(integer.intValue());
  }

  /** Check whether a given integer belongs to this range */
  public boolean includes(int integer) {
    return _start <= integer && integer <= _end;
  }

  /** Compute the intersection of this and another range. */
  public Optional<SubRange> intersection(SubRange other) {
    int start = Integer.max(_start, other._start);
    int end = Integer.min(_end, other._end);
    return start <= end ? Optional.of(new SubRange(start, end)) : Optional.empty();
  }

  /** Returns true if this subrange is empty */
  public boolean isEmpty() {
    return _end < _start;
  }

  /** Check if this range is a singleton value. */
  public boolean isSingleValue() {
    return _start == _end;
  }

  @JsonValue
  public String serializedForm() {
    return String.format("%d-%d", _start, _end);
  }

  @Override
  public String toString() {
    return "[" + _start + "," + _end + "]";
  }
}
