package org.batfish.common.bdd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.batfish.common.bdd.BDDUtils.isAssignment;
import static org.batfish.common.util.CommonUtil.forEachWithIndex;
import static org.batfish.common.util.CommonUtil.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.LongMath;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import net.sf.javabdd.BDD;

/** Given a finite set of values, assigns each an integer id that can be tracked via BDD. */
public final class BDDFiniteDomain<V> {
  private final Map<V, BDD> _valueBdds;
  private final BDD _isValidValue;
  private final BDD _varBits;

  /** Allocate a variable sufficient for the given set of values. */
  BDDFiniteDomain(BDDPacket pkt, String varName, Set<V> values) {
    this(pkt.allocateBDDInteger(varName, computeBitsRequired(values.size()), false), values);
  }

  /** Use the given variable. */
  BDDFiniteDomain(BDDInteger var, Set<V> values) {
    int size = values.size();
    BDD one = var.getFactory().one();
    if (size == 0) {
      _valueBdds = ImmutableMap.of();
      _isValidValue = one;
      _varBits = null;
    } else if (size == 1) {
      V value = values.iterator().next();
      _valueBdds = ImmutableMap.of(value, one);
      _isValidValue = one;
      _varBits = null;
    } else {
      int bitsRequired = computeBitsRequired(size);
      checkArgument(bitsRequired <= var.getBitvec().length);
      _valueBdds = computeValueBdds(var, values);
      _isValidValue = var.leq(size - 1);
      _varBits = Arrays.stream(var.getBitvec()).reduce(one, BDD::and);
    }
  }

  private static int computeBitsRequired(int size) {
    if (size < 2) {
      return 0;
    }
    return LongMath.log2(size, RoundingMode.CEILING);
  }

  private static <V> Map<V, BDD> computeValueBdds(BDDInteger var, Set<V> values) {
    ImmutableMap.Builder<V, BDD> builder = ImmutableMap.builder();
    forEachWithIndex(values, (idx, src) -> builder.put(src, var.value(idx)));
    return builder.build();
  }

  /**
   * Create multiple domains (never in use at the same time) backed by the same variable. For
   * example, we can use this to track domains that are per-node.
   */
  public static <K, V> Map<K, BDDFiniteDomain<V>> domainsWithSharedVariable(
      BDDPacket pkt, String varName, Map<K, Set<V>> values) {
    int maxSize = values.values().stream().mapToInt(Set::size).max().getAsInt();
    int bitsRequired = computeBitsRequired(maxSize);
    BDDInteger var = pkt.allocateBDDInteger(varName, bitsRequired, false);
    return toImmutableMap(
        values, Entry::getKey, entry -> new BDDFiniteDomain<>(var, entry.getValue()));
  }

  public BDD existsValue(BDD bdd) {
    return _varBits == null ? bdd : bdd.exist(_varBits);
  }

  public BDD getConstraintForValue(V value) {
    return checkNotNull(_valueBdds.get(value), "value not in domain");
  }

  Map<V, BDD> getValueBdds() {
    return _valueBdds;
  }

  public Optional<V> getValueFromAssignment(BDD bdd) {
    checkArgument(isAssignment(bdd));
    checkArgument(bdd.imp(_isValidValue).isOne());

    return _valueBdds.entrySet().stream()
        .filter(entry -> bdd.imp(entry.getValue()).isOne())
        .map(Entry::getKey)
        .findFirst();
  }

  public boolean isEmpty() {
    return _valueBdds.isEmpty();
  }

  public BDD getIsValidConstraint() {
    return _isValidValue;
  }
}
