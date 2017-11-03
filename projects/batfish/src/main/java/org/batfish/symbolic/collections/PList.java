package org.batfish.symbolic.collections;

import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A simple persistent stack of non-null values.
 *
 * <p>This implementation is thread-safe (assuming Java's AbstractSequentialList is thread-safe),
 * although its iterators may not be.
 *
 * @author harold
 */
public final class PList<E> extends AbstractSequentialList<E> {
  //// STATIC FACTORY METHODS ////
  private static final PList<Object> EMPTY = new PList<>();

  /** @return an empty stack */
  @SuppressWarnings("unchecked")
  public static <E> PList<E> empty() {
    return (PList<E>) EMPTY;
  }

  /** @return empty().plus(e) */
  public static <E> PList<E> singleton(final E e) {
    return PList.<E>empty().plus(e);
  }

  /** @return a stack consisting of the elements of list in the order of list.iterator() */
  @SuppressWarnings("unchecked")
  public static <E> PList<E> from(final Collection<? extends E> list) {
    if (list instanceof PList) {
      return (PList<E>) list; // (actually we only know it's ConsPStack<? extends E>)
    }
    // but that's good enough for an immutable
    // (i.e. we can't mess someone else up by adding the wrong type to it)
    return PList.from(list.iterator());
  }

  private static <E> PList<E> from(final Iterator<? extends E> i) {
    if (!i.hasNext()) {
      return empty();
    }
    E e = i.next();
    return PList.<E>from(i).plus(e);
  }

  //// PRIVATE CONSTRUCTORS ////
  private final E _first;
  private final PList<E> _rest;
  private final int _size;
  // not externally instantiable (or subclassable):

  private PList() { // EMPTY constructor
    if (EMPTY != null) {
      throw new RuntimeException("empty constructor should only be used once");
    }
    _size = 0;
    _first = null;
    _rest = null;
  }

  private PList(@Nullable final E first, final PList<E> rest) {
    this._first = first;
    this._rest = rest;

    _size = 1 + rest._size;
  }

  //// REQUIRED METHODS FROM AbstractSequentialList ////
  @Override
  public int size() {
    return _size;
  }

  @Override
  public ListIterator<E> listIterator(final int index) {
    if (index < 0 || index > _size) {
      throw new IndexOutOfBoundsException();
    }

    return new ListIterator<E>() {
      int _i = index;
      PList<E> _next = subList(index);

      @Override
      public boolean hasNext() {
        return _next._size > 0;
      }

      @Override
      public boolean hasPrevious() {
        return _i > 0;
      }

      @Override
      public int nextIndex() {
        return index;
      }

      @Override
      public int previousIndex() {
        return index - 1;
      }

      @Nullable
      @Override
      public E next() {
        E e = _next._first;
        _next = _next._rest;
        return e;
      }

      @Nullable
      @Override
      public E previous() {
        System.err.println("ConsPStack.listIterator().previous() is inefficient, don't use it!");
        _next = subList(index - 1); // go from beginning...
        return _next._first;
      }

      @Override
      public void add(final E o) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void set(final E o) {
        throw new UnsupportedOperationException();
      }
    };
  }

  //// OVERRIDDEN METHODS FROM AbstractSequentialList ////
  @Nonnull
  @Override
  public PList<E> subList(final int start, final int end) {
    if (start < 0 || end > _size || start > end) {
      throw new IndexOutOfBoundsException();
    }
    if (end == _size) { // want a substack
      return subList(start); // this is faster
    }
    if (start == end) { // want nothing
      return empty();
    }
    if (start == 0) { // want the current element
      assert _rest != null;
      return new PList<>(_first, _rest.subList(0, end - 1));
    }
    // otherwise, don't want the current element:
    assert _rest != null;
    return _rest.subList(start - 1, end - 1);
  }

  //// IMPLEMENTED METHODS OF PStack ////
  public PList<E> plus(final E e) {
    return new PList<>(e, this);
  }

  public PList<E> plusAll(final Collection<? extends E> list) {
    PList<E> result = this;
    for (E e : list) {
      result = result.plus(e);
    }
    return result;
  }

  public PList<E> plus(final int i, final E e) {
    if (i < 0 || i > _size) {
      throw new IndexOutOfBoundsException();
    }
    if (i == 0) { // insert at beginning
      return plus(e);
    }
    assert _rest != null;
    return new PList<>(_first, _rest.plus(i - 1, e));
  }

  public PList<E> plusAll(final int i, final Collection<? extends E> list) {
    // TODO inefficient if list.isEmpty()
    if (i < 0 || i > _size) {
      throw new IndexOutOfBoundsException();
    }
    if (i == 0) {
      return plusAll(list);
    }
    assert _rest != null;
    return new PList<>(_first, _rest.plusAll(i - 1, list));
  }

  @Nullable
  public PList<E> minus(final Object e) {
    if (_size == 0) {
      return this;
    }
    assert _first != null;
    if (_first.equals(e)) { // found it
      return _rest; // don't recurse (only remove one)
    }
    // otherwise keep looking:
    assert _rest != null;
    PList<E> newRest = _rest.minus(e);
    if (newRest == _rest) {
      return this;
    }
    assert newRest != null;
    return new PList<>(_first, newRest);
  }

  public PList<E> minus(final int i) {
    if (i < 0 || i >= _size) {
      throw new IndexOutOfBoundsException("Index: " + i + "; _size: " + _size);
    } else if (i == 0) {
      assert _rest != null;
      return _rest;
    } else {
      assert _rest != null;
      return new PList<>(_first, _rest.minus(i - 1));
    }
  }

  public PList<E> minusAll(final Collection<?> list) {
    if (_size == 0) {
      return this;
    }
    if (list.contains(_first)) { // get rid of current element
      assert _rest != null;
      return _rest.minusAll(list); // recursively delete all
    }
    // either way keep looking:
    assert _rest != null;
    PList<E> newRest = _rest.minusAll(list);
    if (newRest == _rest) {
      return this;
    }
    return new PList<>(_first, newRest);
  }

  public PList<E> with(final int i, final E e) {
    if (i < 0 || i >= _size) {
      throw new IndexOutOfBoundsException();
    }
    if (i == 0) {
      assert _first != null;
      if (_first.equals(e)) {
        return this;
      }
      assert _rest != null;
      return new PList<>(e, _rest);
    }
    assert _rest != null;
    PList<E> newRest = _rest.with(i - 1, e);
    if (newRest == _rest) {
      return this;
    }
    return new PList<>(_first, newRest);
  }

  private int find(Predicate<E> f, final int i) {
    if (_size == 0) {
      return -1;
    }
    if (f.test(_first)) { // found it
      return i; // don't recurse (only remove one)
    }
    // otherwise keep looking:
    assert _rest != null;
    return _rest.find(f, i + 1);
  }

  public int find(Predicate<E> f) {
    return find(f, 0);
  }

  public PList<E> subList(final int start) {
    if (start < 0 || start > _size) {
      throw new IndexOutOfBoundsException();
    }
    if (start == 0) {
      return this;
    }

    assert _rest != null;
    return _rest.subList(start - 1);
  }
}
