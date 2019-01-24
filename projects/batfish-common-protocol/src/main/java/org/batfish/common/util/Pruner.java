package org.batfish.common.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Prune a set of objects to "maximize" coverage over some ranked set of properties of interest.
 *
 * @param <T> The type of objects being pruned.
 */
public class Pruner<T> {
  private static class Property<T, P> {
    private final Function<T, P> _extractor;
    private final ListMultimap<P, T> _objectsByPropertyValue;

    Property(Function<T, P> extractor, List<T> objects) {
      _extractor = extractor;

      _objectsByPropertyValue = ArrayListMultimap.create();
      objects.forEach(obj -> _objectsByPropertyValue.put(_extractor.apply(obj), obj));
    }

    void picked(T obj) {
      _objectsByPropertyValue.removeAll(_extractor.apply(obj));
    }

    @Nullable
    T pick(Set<T> selectedObjects) {
      if (_objectsByPropertyValue.isEmpty()) {
        return null;
      }
      List<P> valuesToRemove = new ArrayList<>();
      T selected = null;
      Iterator<Entry<P, Collection<T>>> iter =
          _objectsByPropertyValue.asMap().entrySet().iterator();
      while (selected == null && iter.hasNext()) {
        Entry<P, Collection<T>> entry = iter.next();
        P propertyValue = entry.getKey();
        for (T obj : entry.getValue()) {
          if (!selectedObjects.contains(obj)) {
            selected = obj;
            break;
          }
        }
        /* Either we selected an object with this property, or all have already
         * been selected. Either way, remove it.
         */
        valuesToRemove.add(propertyValue);
      }
      valuesToRemove.forEach(_objectsByPropertyValue::removeAll);
      return selected;
    }
  }

  // Properties in order of decreasing importance.
  private final List<Function<T, ?>> _propertyExtractors;

  private Pruner(List<Function<T, ?>> propertyExtractors) {
    checkArgument(!propertyExtractors.isEmpty(), "Must define at least one property");
    _propertyExtractors = ImmutableList.copyOf(propertyExtractors);
  }

  public List<T> prune(List<T> objects, int maxSize) {
    if (objects.size() <= maxSize) {
      return objects;
    }

    LinkedHashSet<T> selectedObjects = new LinkedHashSet<>();
    List<Property<T, ?>> properties =
        _propertyExtractors.stream()
            .map(propertyExtractor -> new Property<>(propertyExtractor, objects))
            .collect(ImmutableList.toImmutableList());

    boolean done = false;
    while (selectedObjects.size() < maxSize && !done) {
      done = true;
      for (Property<T, ?> property : properties) {
        T picked = property.pick(selectedObjects);
        if (picked != null) {
          selectedObjects.add(picked);
          properties.forEach(prop -> prop.picked(picked));
          done = false;
          break;
        }
      }
    }

    if (selectedObjects.size() < maxSize) {
      // add remaining objects in input order until maxSize is reached
      selectedObjects.addAll(
          objects.stream()
              .filter(obj -> !selectedObjects.contains(obj))
              .limit(maxSize - selectedObjects.size())
              .collect(Collectors.toList()));
    }
    return ImmutableList.copyOf(selectedObjects);
  }

  /** @param <T> The type of objects being pruned. */
  public static class Builder<T> {
    private final ImmutableList.Builder<Function<T, ?>> _propertyExtractors =
        ImmutableList.builder();

    /**
     * Add a new property for the {@link Pruner}. The property will have higher priority in picking
     * objects than all properties added later.
     *
     * @param extractor A function to extract the property value from an object. This method is
     *     required to be deterministic wrt {@link Object#equals(Object)}.
     * @param <P> The property type.
     */
    public <P> Builder<T> addProperty(Function<T, P> extractor) {
      _propertyExtractors.add(extractor::apply);
      return this;
    }

    public Pruner<T> build() {
      return new Pruner<T>(_propertyExtractors.build());
    }
  }

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }
}
