package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;

public class Prefix6Trie implements Serializable {

  private class ByteTrie implements Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    private ByteTrieNode _root;

    public ByteTrie() {
      _root = new ByteTrieNode();
    }

    public void addPrefix(Prefix6 prefix6) {
      int prefixLength = prefix6.getPrefixLength();
      BitSet bits = prefix6.getAddress().getAddressBits();
      _root.addPrefix6(prefix6, bits, prefixLength, 0);
    }

    public boolean containsPathFromPrefix(Prefix6 prefix6) {
      int prefixLength = prefix6.getPrefixLength();
      BitSet bits = prefix6.getAddress().getAddressBits();
      return _root.containsPathFromPrefix(bits, prefixLength, 0);
    }

    public Prefix6 getLongestPrefixMatch(Ip6 address6) {
      BitSet address6Bits = address6.getAddressBits();
      return _root.getLongestPrefixMatch(address6, address6Bits, 0);
    }
  }

  private class ByteTrieNode implements Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    private ByteTrieNode _left;

    private Prefix6 _prefix6;

    private ByteTrieNode _right;

    public void addPrefix6(Prefix6 prefix6, BitSet bits, int prefixLength, int depth) {
      if (prefixLength == depth) {
        _prefix6 = prefix6;
        return;
      } else {
        boolean currentBit = bits.get(depth);
        if (currentBit) {
          if (_right == null) {
            _right = new ByteTrieNode();
          }
          _right.addPrefix6(prefix6, bits, prefixLength, depth + 1);
        } else {
          if (_left == null) {
            _left = new ByteTrieNode();
          }
          _left.addPrefix6(prefix6, bits, prefixLength, depth + 1);
        }
      }
    }

    public boolean containsPathFromPrefix(BitSet bits, int prefixLength, int depth) {
      if (prefixLength == depth) {
        if (depth == 0 && _prefix6 == null) {
          return false;
        } else {
          return true;
        }
      } else {
        boolean currentBit = bits.get(depth);
        if (currentBit) {
          if (_right == null) {
            return false;
          } else {
            return _right.containsPathFromPrefix(bits, prefixLength, depth + 1);
          }
        } else {
          if (_left == null) {
            return false;
          } else {
            return _left.containsPathFromPrefix(bits, prefixLength, depth + 1);
          }
        }
      }
    }

    @Nullable
    private Prefix6 getLongestPrefixMatch(Ip6 address6) {
      if (_prefix6.contains(address6)) {
        return _prefix6;
      } else {
        return null;
      }
    }

    public Prefix6 getLongestPrefixMatch(Ip6 address6, BitSet bits, int index) {
      Prefix6 longestPrefixMatch = getLongestPrefixMatch(address6);
      if (index == Prefix6.MAX_PREFIX_LENGTH) {
        return longestPrefixMatch;
      }
      boolean currentBit = bits.get(index);
      Prefix6 longerMatch = null;
      if (currentBit) {
        if (_right != null) {
          longerMatch = _right.getLongestPrefixMatch(address6, bits, index + 1);
        }
      } else {
        if (_left != null) {
          longerMatch = _left.getLongestPrefixMatch(address6, bits, index + 1);
        }
      }
      if (longerMatch == null) {
        return longestPrefixMatch;
      } else {
        return longerMatch;
      }
    }
  }

  /** */
  private static final long serialVersionUID = 1L;

  private SortedSet<Prefix6> _prefixes;

  private ByteTrie _trie;

  public Prefix6Trie() {
    _trie = new ByteTrie();
    _prefixes = new TreeSet<>();
  }

  @JsonCreator
  public Prefix6Trie(SortedSet<Prefix6> prefixes) {
    _trie = new ByteTrie();
    _prefixes = prefixes;
    for (Prefix6 prefix6 : prefixes) {
      _trie.addPrefix(prefix6);
    }
  }

  public boolean add(Prefix6 prefix) {
    boolean changed = _prefixes.add(prefix);
    if (changed) {
      _trie.addPrefix(prefix);
    }
    return changed;
  }

  public boolean containsPathFromPrefix(Prefix6 prefix) {
    return _trie.containsPathFromPrefix(prefix);
  }

  public Prefix6 getLongestPrefixMatch(Ip6 address) {
    return _trie.getLongestPrefixMatch(address);
  }

  @JsonValue
  public SortedSet<Prefix6> getPrefixes() {
    return Collections.unmodifiableSortedSet(_prefixes);
  }
}
