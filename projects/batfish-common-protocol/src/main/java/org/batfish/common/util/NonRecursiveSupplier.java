package org.batfish.common.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A {@link Supplier} that throws an exception if two simultaneous calls are attempted from within
 * the same thread (i.e. the supplier is recursive). The goal is to detect the recursion, and under
 * the assumption that it's an infinite recursion, throw an exception.
 */
public class NonRecursiveSupplier<T> implements Supplier<T>, com.google.common.base.Supplier<T> {
  public static final class NonRecursiveSupplierException extends RuntimeException {
    public static final long serialVersionUID = 1;
  }

  private final Supplier<T> _inner;

  /*
   * True if we are currently inside a call to _inner.get(). If so, and we try to call again,
   * then we have detected recursion.
   */
  private AtomicBoolean _inInner;

  public NonRecursiveSupplier(Supplier<T> inner) {
    _inner = inner;
    _inInner = new AtomicBoolean(false);
  }

  @Override
  public T get() {
    acquire();
    try {
      return _inner.get();
    } finally {
      release();
    }
  }

  private void acquire() {
    if (!_inInner.compareAndSet(false, true)) {
      throw new NonRecursiveSupplierException();
    }
  }

  private void release() {
    if (!_inInner.compareAndSet(true, false)) {
      throw new IllegalStateException("_inInner should be true when exiting _inner");
    }
  }
}
