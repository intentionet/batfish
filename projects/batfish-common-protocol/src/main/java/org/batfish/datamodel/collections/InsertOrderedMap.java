package org.batfish.datamodel.collections;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A (mostly) insert-ordered {@link Map} implementation. Similar to {@link LinkedHashMap}, but in
 * addition provides APIs for inserting at the front of the map or re-ordering keys: {@link
 * #putFirst(K, V)}, {@link #moveBefore(K, K)}, {@link #moveAfter(K, K)}
 *
 * <p>Note: this implementation rejects {@code null} keys and values by throwing an {@link
 * IllegalArgumentException}. This implementation is also <b>not</b> thread-safe.
 */
@ParametersAreNonnullByDefault
public class InsertOrderedMap<K, V> implements Map<K, V>, Serializable {

  /** Create a new empty map */
  public InsertOrderedMap() {
    _map = new HashMap<>(1);
    _keyList = new LinkedList<>();
  }

  public InsertOrderedMap(Map<K, V> other) {
    this();
    putAll(other);
  }

  @Override
  public int size() {
    return _map.size();
  }

  @Override
  public boolean isEmpty() {
    return _map.isEmpty();
  }

  /** See {@link Map#containsKey}, with the exception that this rejects {@code null} keys */
  @Override
  public boolean containsKey(Object key) {
    checkArgument(key != null);
    return _map.containsKey(key);
  }

  /**
   * See {@link Map#containsKey}, with the exception that this rejects {@code null} values.
   *
   * @throws IllegalArgumentException if the key is {@code null}
   */
  @Override
  public boolean containsValue(Object value) {
    checkArgument(value != null);
    return _map.containsValue(value);
  }

  /**
   * See {@link Map#get}, with the exception that this rejects {@code null} keys
   *
   * @throws IllegalArgumentException if the key is {@code null}
   */
  @Override
  @Nullable
  public V get(Object key) {
    checkArgument(key != null);
    return _map.get(key);
  }

  /**
   * Insert a new value into the map, at the end. See {@link Map#get}, with the exception that this
   * rejects {@code null} keys
   *
   * @throws IllegalArgumentException if the key is {@code null}
   */
  @Override
  @Nullable
  public V put(K key, V value) {
    checkArgument(key != null && value != null, "Keys and values must be nonnull");
    V ret = putIntoMapClearFromKeyList(key, value);
    _keyList.add(key);
    return ret;
  }

  /** Insert a value at the front of the map. See {@link #put(K, V)} for return value semantics. */
  @Nullable
  public V putFirst(K key, V value) {
    V ret = putIntoMapClearFromKeyList(key, value);
    _keyList.add(0, key);
    return ret;
  }

  /** See {@link Map#remove}, with the exception that this rejects {@code null} keys */
  @Override
  public V remove(@Nullable Object key) {
    checkArgument(key != null);
    _keyList.remove(key);
    return _map.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    _map.clear();
    _keyList.clear();
  }

  /** Returns and immutable copy of the key set. Map iteration order is reflected in the set. */
  @Override
  @Nonnull
  public Set<K> keySet() {
    return ImmutableSet.copyOf(_keyList);
  }

  /**
   * Returns and immutable copy of the map values. Map iteration order is reflected in the
   * collection.
   */
  @Override
  public Collection<V> values() {
    ImmutableList.Builder<V> valuesBuilder = ImmutableList.builder();
    for (K k : _keyList) {
      valuesBuilder.add(_map.get(k));
    }
    return valuesBuilder.build();
  }

  /**
   * Returns an immutable copy of the entry set. Map iteration order is reflected in the collection.
   */
  @Override
  public Set<Entry<K, V>> entrySet() {
    ImmutableSet.Builder<Entry<K, V>> valuesBuilder = ImmutableSet.builder();
    for (K k : _keyList) {
      valuesBuilder.add(new SimpleEntry<>(k, _map.get(k)));
    }
    return valuesBuilder.build();
  }

  void moveBefore(K key, K pivot) {
    checkArgument(_keyList.contains(pivot) && _keyList.contains(key));
    _keyList.remove(key);
    _keyList.add(_keyList.indexOf(pivot), key);
  }

  void moveAfter(K key, K pivot) {
    checkArgument(_keyList.contains(pivot) && _keyList.contains(key));
    _keyList.remove(key);
    _keyList.add(_keyList.indexOf(pivot) + 1, key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_keyList, _map);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof InsertOrderedMap)) {
      return false;
    }
    InsertOrderedMap<?, ?> that = (InsertOrderedMap<?, ?>) obj;
    if (!_keyList.equals(that._keyList)) {
      return false;
    }
    for (K k : keySet()) {
      if (!get(k).equals(that.get(k))) {
        return false;
      }
    }
    return true;
  }

  /////////////////////////
  // Private implementation

  private final Map<K, V> _map;
  private final List<K> _keyList;

  /**
   * Put a new value in the map. If the value already existed for a given key, remove it from the
   * key list.
   */
  @Nullable
  private V putIntoMapClearFromKeyList(K key, @Nonnull V value) {
    V ret = _map.put(key, value);
    if (ret != null) {
      // A value has been replaced
      _keyList.remove(key);
    }
    return ret;
  }
}
