package org.batfish.symbolic.utils;

import java.util.Objects;

public interface TriConsumer<A, B, C> {
  void accept(final A a, final B b, final C c);

  default TriConsumer<A, B, C> andThen(final TriConsumer<? super A, ? super B, ? super C> after) {
    Objects.requireNonNull(after);
    return (A a, B b, C c) -> {
      accept(a, b, c);
      after.accept(a, b, c);
    };
  }
}
