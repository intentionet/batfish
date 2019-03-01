package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;

/** Describes a collection of {@link Prefix}es and {@link PrefixRange}s */
public class PrefixSpace implements Serializable {

  private static class BitTrie implements Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    private BitTrieNode _root;

    public BitTrie() {
      _root = new BitTrieNode();
    }

    public void addPrefixRange(PrefixRange prefixRange) {
      Prefix prefix = prefixRange.getPrefix();
      BitSet bits = getAddressBits(prefix.getStartIp());

      // The minimum length of the range may be shorter than the actual prefix length.
      // If so, we need to specially handle all shorter prefixes with a custom address and bitset.
      int minLength = prefixRange.getLengthRange().getStart();
      int maxLength = Math.min(prefixRange.getLengthRange().getEnd(), prefix.getPrefixLength() - 1);
      for (int currentLength = minLength; currentLength <= maxLength; currentLength++) {
        Prefix currentPrefix = Prefix.create(prefix.getStartIp(), currentLength);
        PrefixRange currentPrefixRange = PrefixRange.fromPrefix(currentPrefix);
        BitSet currentBits = getAddressBits(currentPrefix.getStartIp());
        _root.addPrefixRange(currentPrefixRange, currentBits, currentLength, 0);
      }

      // Otherwise, add the prefix range as-is.
      _root.addPrefixRange(prefixRange, bits, prefix.getPrefixLength(), 0);
    }

    public void addTrieNodeSpace(BitTrieNode node) {
      if (node._left != null) {
        addTrieNodeSpace(node._left);
      }
      if (node._right != null) {
        addTrieNodeSpace(node._right);
      }
      for (PrefixRange prefixRange : node._prefixRanges) {
        addPrefixRange(prefixRange);
      }
    }

    public boolean containsPrefixRange(PrefixRange prefixRange) {
      Prefix prefix = prefixRange.getPrefix();
      int prefixLength = prefix.getPrefixLength();
      BitSet bits = getAddressBits(prefix.getStartIp());
      return _root.containsPrefixRange(prefixRange, bits, prefixLength, 0);
    }

    public Set<PrefixRange> getPrefixRanges() {
      Set<PrefixRange> prefixRanges = new HashSet<>();
      _root.collectPrefixRanges(prefixRanges);
      return prefixRanges;
    }
  }

  private static class BitTrieNode implements Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    private BitTrieNode _left;

    private Set<PrefixRange> _prefixRanges;

    private BitTrieNode _right;

    public BitTrieNode() {
      _prefixRanges = ImmutableSet.of();
    }

    public void addPrefixRange(PrefixRange prefixRange, BitSet bits, int prefixLength, int depth) {
      if (_prefixRanges.stream().anyMatch(nr -> nr.includesPrefixRange(prefixRange))) {
        return;
      }

      if (prefixLength == depth) {
        prune(prefixRange);
        _prefixRanges =
            ImmutableSet.<PrefixRange>builderWithExpectedSize(_prefixRanges.size() + 1)
                .addAll(_prefixRanges)
                .add(prefixRange)
                .build();
      } else {
        boolean currentBit = bits.get(depth);
        if (currentBit) {
          if (_right == null) {
            _right = new BitTrieNode();
          }
          _right.addPrefixRange(prefixRange, bits, prefixLength, depth + 1);
        } else {
          if (_left == null) {
            _left = new BitTrieNode();
          }
          _left.addPrefixRange(prefixRange, bits, prefixLength, depth + 1);
        }
      }
    }

    public void collectPrefixRanges(Set<PrefixRange> prefixRanges) {
      prefixRanges.addAll(_prefixRanges);
      if (_left != null) {
        _left.collectPrefixRanges(prefixRanges);
      }
      if (_right != null) {
        _right.collectPrefixRanges(prefixRanges);
      }
    }

    public boolean containsPrefixRange(
        PrefixRange prefixRange, BitSet bits, int prefixLength, int depth) {
      for (PrefixRange nodeRange : _prefixRanges) {
        if (nodeRange.includesPrefixRange(prefixRange)) {
          return true;
        }
      }
      if (prefixLength == depth) {
        return false;
      } else {
        boolean currentBit = bits.get(depth);
        if (currentBit) {
          if (_right == null) {
            return false;
          } else {
            return _right.containsPrefixRange(prefixRange, bits, prefixLength, depth + 1);
          }
        } else {
          if (_left == null) {
            return false;
          } else {
            return _left.containsPrefixRange(prefixRange, bits, prefixLength, depth + 1);
          }
        }
      }
    }

    private boolean isEmpty() {
      return _left == null && _right == null && _prefixRanges.isEmpty();
    }

    private void prune(PrefixRange prefixRange) {
      if (_left != null) {
        _left.prune(prefixRange);
        if (_left.isEmpty()) {
          _left = null;
        }
      }
      if (_right != null) {
        _right.prune(prefixRange);
        if (_right.isEmpty()) {
          _right = null;
        }
      }
      if (_prefixRanges.stream().anyMatch(prefixRange::includesPrefixRange)) {
        _prefixRanges =
            _prefixRanges.stream()
                .filter(pr -> !prefixRange.includesPrefixRange(pr))
                .collect(ImmutableSet.toImmutableSet());
      }
    }
  }

  /** */
  private static final long serialVersionUID = 1L;

  /**
   * Converts an IPv4 address into a {@link BitSet} useful for prefix matching. The highest bit of
   * the address is the lowest bit of the bitset: the address 128.0.0.0 when converted to a {@link
   * BitSet} has only the lowest bit set.
   */
  // visible for testing
  static BitSet getAddressBits(Ip address) {
    return BitSet.valueOf(new long[] {Integer.reverse((int) address.asLong()) & 0xffffffffL});
  }

  private transient ConcurrentMap<Prefix, Boolean> _cache;

  private BitTrie _trie;

  public PrefixSpace() {
    _trie = new BitTrie();
    _cache = new ConcurrentHashMap<>();
  }

  @JsonCreator
  public PrefixSpace(Iterable<PrefixRange> prefixRanges) {
    this();
    for (PrefixRange prefixRange : prefixRanges) {
      _trie.addPrefixRange(prefixRange);
    }
  }

  public PrefixSpace(PrefixRange... prefixRanges) {
    this(Arrays.asList(prefixRanges));
  }

  /**
   * Adds the given {@link Prefix} to this {@link PrefixSpace}.
   *
   * @param prefix Prefix to add
   */
  public void addPrefix(Prefix prefix) {
    addPrefixRange(PrefixRange.fromPrefix(prefix));
  }

  /**
   * Adds the given prefix range to this {@link PrefixSpace}.
   *
   * @param prefixRange Range of prefixes to add
   */
  public void addPrefixRange(PrefixRange prefixRange) {
    _trie.addPrefixRange(prefixRange);
  }

  /**
   * Adds all prefixes in the given {@link PrefixSpace} to this {@link PrefixSpace}.
   *
   * @param prefixSpace {@link PrefixSpace} whose prefixes to add
   */
  public void addSpace(PrefixSpace prefixSpace) {
    _trie.addTrieNodeSpace(prefixSpace._trie._root);
  }

  public boolean containsPrefix(Prefix prefix) {
    @Nullable Boolean result = _cache.get(prefix);
    if (result != null) {
      return result;
    } else {
      boolean contained = containsPrefixRange(PrefixRange.fromPrefix(prefix));
      _cache.put(prefix, contained);
      return contained;
    }
  }

  /**
   * Returns whether this {@link PrefixSpace} contains the given {@link PrefixRange}.
   *
   * @param prefixRange Range of prefixes to check for
   * @return {@code true} if this {@link PrefixSpace} contains the given range of prefixes
   */
  public boolean containsPrefixRange(PrefixRange prefixRange) {
    return _trie.containsPrefixRange(prefixRange);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return getPrefixRanges().equals(((PrefixSpace) obj).getPrefixRanges());
  }

  /**
   * Returns all {@link PrefixRange}s contained in this {@link PrefixSpace} as a {@code Set}.
   *
   * @return A {@code Set} of all {@link PrefixRange}s in this {@link PrefixSpace}
   */
  public Set<PrefixRange> getPrefixRanges() {
    return _trie.getPrefixRanges();
  }

  @JsonValue
  private SortedSet<PrefixRange> jsonValue() {
    // Sorted for deterministic output
    return getPrefixRanges().stream()
        .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural()));
  }

  @Override
  public int hashCode() {
    return getPrefixRanges().hashCode();
  }

  /**
   * Returns a new {@link PrefixSpace} containing all {@link Prefix}es shared between this {@link
   * PrefixSpace} and the one given.
   *
   * @param intersectSpace {@link PrefixSpace} with which to find intersecting {@link Prefix}es
   * @return A new {@link PrefixSpace} containing all shared {@link Prefix}es
   */
  public PrefixSpace intersection(PrefixSpace intersectSpace) {
    PrefixSpace newSpace = new PrefixSpace();
    Set<PrefixRange> intersectRanges = intersectSpace.getPrefixRanges();
    for (PrefixRange intersectRange : intersectRanges) {
      if (containsPrefixRange(intersectRange)) {
        newSpace.addPrefixRange(intersectRange);
      }
    }
    return newSpace;
  }

  @JsonIgnore
  public boolean isEmpty() {
    return _trie._root.isEmpty();
  }

  /**
   * Returns whether the given {@link PrefixSpace} has any {@link Prefix}es in common with this one.
   *
   * @param intersectSpace {@link PrefixSpace} in which to check for overlap
   * @return {@code true} if the given {@link PrefixSpace} has any {@link Prefix}es in common with
   *     this one
   */
  public boolean overlaps(PrefixSpace intersectSpace) {
    PrefixSpace intersection = intersection(intersectSpace);
    return !intersection.isEmpty();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    _cache = new ConcurrentHashMap<>();
  }

  /**
   * Returns a {@code String} listing the {@link PrefixRange}s contained in this {@link
   * PrefixSpace}.
   *
   * @return {@code String} representation of this {@link PrefixSpace}
   */
  @Override
  public String toString() {
    return getPrefixRanges().toString();
  }
}
