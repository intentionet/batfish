package org.batfish.datamodel.collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.common.BatfishException;
import org.batfish.common.util.BatfishObjectMapper;
import org.skyscreamer.jsonassert.JSONAssert;

public class NamedStructureEquivalenceSets<T> {

  public static class Builder<T> {

    private static boolean checkJsonStringEquals(String lhs, String rhs) {
      try {
        JSONAssert.assertEquals(lhs, rhs, false);
        return true;
      } catch (Exception e) {
        throw new BatfishException("JSON equality check failed", e);
      } catch (AssertionError err) {
        return false;
      }
    }

    private Map<String, Map<Integer, Set<NamedStructureEquivalenceSet<T>>>>
        _sameNamedStructuresByNameAndHash;

    private final String _structureClassName;

    private Builder(String structureClassName) {
      _sameNamedStructuresByNameAndHash = new HashMap<>();
      _structureClassName = structureClassName;
    }

    public void addEntry(String structureName, String hostname, T structure) {
      Map<Integer, Set<NamedStructureEquivalenceSet<T>>> sameNamedStructuresByHash =
          _sameNamedStructuresByNameAndHash.computeIfAbsent(structureName, s -> new HashMap<>());
      String structureJson = writeObject(structure);
      int hash = structureJson.hashCode();
      Set<NamedStructureEquivalenceSet<T>> eqSetsWithSameHash =
          sameNamedStructuresByHash.computeIfAbsent(hash, h -> new HashSet<>());
      if (eqSetsWithSameHash.isEmpty()) {
        eqSetsWithSameHash.add(new NamedStructureEquivalenceSet<>(hostname, structure));
      } else {
        Optional<NamedStructureEquivalenceSet<T>> potentialMatchingSet =
            eqSetsWithSameHash
                .stream()
                .filter(
                    s -> checkJsonStringEquals(structureJson, writeObject(s.getNamedStructure())))
                .findAny();
        if (potentialMatchingSet.isPresent()) {
          NamedStructureEquivalenceSet<T> matchingSet = potentialMatchingSet.get();
          matchingSet.setNodes(
              new ImmutableSortedSet.Builder<String>(Comparator.naturalOrder())
                  .addAll(matchingSet.getNodes())
                  .add(hostname)
                  .build());
        } else {
          eqSetsWithSameHash.add(new NamedStructureEquivalenceSet<>(hostname, structure));
        }
      }
    }

    public NamedStructureEquivalenceSets<T> build() {
      ImmutableSortedMap.Builder<String, SortedSet<NamedStructureEquivalenceSet<T>>> builder =
          new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());
      for (Entry<String, Map<Integer, Set<NamedStructureEquivalenceSet<T>>>> e :
          _sameNamedStructuresByNameAndHash.entrySet()) {
        String structureName = e.getKey();
        Map<Integer, Set<NamedStructureEquivalenceSet<T>>> structuresByHash = e.getValue();
        SortedSet<NamedStructureEquivalenceSet<T>> newSet =
            structuresByHash
                .values()
                .stream()
                .flatMap(ss -> ss.stream())
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
        builder.put(structureName, newSet);
      }
      NamedStructureEquivalenceSets<T> eqSets =
          new NamedStructureEquivalenceSets<>(_structureClassName);
      eqSets.setSameNamedStructures(builder.build());
      return eqSets;
    }

    private String writeObject(T t) {
      try {
        String structureJson = BatfishObjectMapper.writePrettyString(t);
        return structureJson;
      } catch (JsonProcessingException e) {
        throw new BatfishException("Could not write named structure as JSON", e);
      }
    }
  }

  private static final String PROP_SAME_NAMED_STRUCTURES = "sameNamedStructures";

  private static final String PROP_STRUCTURE_CLASS_NAME = "structureClassName";

  public static <T> Builder<T> builder(String structureClassName) {
    return new Builder<>(structureClassName);
  }

  private SortedMap<String, SortedSet<NamedStructureEquivalenceSet<T>>> _sameNamedStructures;

  private final String _structureClassName;

  @JsonCreator
  public NamedStructureEquivalenceSets(
      @JsonProperty(PROP_STRUCTURE_CLASS_NAME) String structureClassName) {
    _structureClassName = structureClassName;
    _sameNamedStructures = Collections.emptySortedMap();
  }

  /** Remove structures with only one equivalence class, since they indicate nothing of note */
  public void clean() {
    _sameNamedStructures =
        _sameNamedStructures
            .entrySet()
            .stream()
            .filter(e -> e.getValue().size() != 1)
            .collect(
                ImmutableSortedMap.toImmutableSortedMap(
                    Comparator.naturalOrder(), Entry::getKey, Entry::getValue));
  }

  @JsonProperty(PROP_SAME_NAMED_STRUCTURES)
  public SortedMap<String, SortedSet<NamedStructureEquivalenceSet<T>>> getSameNamedStructures() {
    return _sameNamedStructures;
  }

  public String getStructureClassName() {
    return _structureClassName;
  }

  public String prettyPrint(String indent) {
    StringBuilder sb = new StringBuilder();
    for (String name : _sameNamedStructures.keySet()) {
      sb.append(indent + name + "\n");
      for (NamedStructureEquivalenceSet<T> set : _sameNamedStructures.get(name)) {
        sb.append(set.prettyPrint(indent + indent));
      }
    }
    return sb.toString();
  }

  @JsonProperty(PROP_SAME_NAMED_STRUCTURES)
  public void setSameNamedStructures(
      SortedMap<String, SortedSet<NamedStructureEquivalenceSet<T>>> sameNamedStructures) {
    _sameNamedStructures = sameNamedStructures;
  }

  public int size() {
    return _sameNamedStructures.size();
  }

  @Override
  public String toString() {
    return "<" + _structureClassName + ", " + _sameNamedStructures + ">";
  }
}
