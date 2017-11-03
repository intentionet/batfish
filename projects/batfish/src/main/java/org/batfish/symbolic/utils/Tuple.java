package org.batfish.symbolic.utils;

import java.util.Objects;
import javax.annotation.Nullable;

public class Tuple<T1, T2> {

  private T1 _first;

  private T2 _second;

  public Tuple(@Nullable T1 first, @Nullable T2 second) {
    this._first = first;
    this._second = second;
  }

  public T1 getFirst() {
    return _first;
  }

  public T2 getSecond() {
    return _second;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Tuple<?, ?>)) {
      return false;
    }
    Tuple<?, ?> other = (Tuple<?, ?>) o;
    return Objects.equals(_first, other._first) && Objects.equals(_second, other._second);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_first, _second);
  }

  @Override
  public String toString() {
    return "<" + _first + "," + _second + ">";
  }
}
