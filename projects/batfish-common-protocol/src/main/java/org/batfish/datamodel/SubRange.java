package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;
import org.batfish.common.BatfishException;

public final class SubRange implements Serializable, Comparable<SubRange> {

  private static final long serialVersionUID = 1L;

  public static List<SubRange> invertedRange(int value, int min, int max) {
    List<SubRange> returnRange = new LinkedList<>();
    if (value > min) {
      returnRange.add(new SubRange(min, value - 1));
    }
    if (value < max) {
      returnRange.add(new SubRange(value + 1, max));
    }
    return returnRange;
  }

  private final int _end;

  private final int _start;

  public SubRange(int start, int end) {
    _start = start;
    _end = end;
  }

  @JsonCreator
  public SubRange(Object o) {
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
    } else {
      throw new BatfishException(
          "Cannot create SubRange from input object of type: " + o.getClass().getCanonicalName());
    }
  }

  public IntStream asStream() {
    return IntStream.range(_start, _end + 1);
  }

  @Override
  public int compareTo(SubRange rhs) {
    int ret = Integer.compare(_start, rhs._start);
    if (ret == 0) {
      ret = Integer.compare(_end, rhs._end);
    }
    return ret;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof SubRange)) {
      return false;
    }
    SubRange other = (SubRange) o;
    if (_end != other._end) {
      return false;
    }
    if (_start != other._start) {
      return false;
    }
    return true;
  }

  public int getEnd() {
    return _end;
  }

  public int getStart() {
    return _start;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _end;
    result = prime * result + _start;
    return result;
  }

  public boolean includes(int integer) {
    return _start <= integer && integer <= _end;
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
