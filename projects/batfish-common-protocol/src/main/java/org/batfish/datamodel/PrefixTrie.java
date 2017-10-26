package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;

public class PrefixTrie implements Serializable {

  private class ByteTrie implements Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    private ByteTrieNode _root;

    public ByteTrie() {
      _root = new ByteTrieNode();
    }

    public void addPrefix(Prefix prefix) {
      int prefixLength = prefix.getPrefixLength();
      BitSet bits = prefix.getAddress().getAddressBits();
      _root.addPrefix(prefix, bits, prefixLength, 0);
    }

    public boolean containsPathFromPrefix(Prefix prefix) {
      int prefixLength = prefix.getPrefixLength();
      BitSet bits = prefix.getAddress().getAddressBits();
      return _root.containsPathFromPrefix(bits, prefixLength, 0);
    }

    public Prefix getLongestPrefixMatch(Ip address) {
      BitSet addressBits = address.getAddressBits();
      return _root.getLongestPrefixMatch(address, addressBits, 0);
    }
  }

  private class ByteTrieNode implements Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    private ByteTrieNode _left;

    private Prefix _prefix;

    private ByteTrieNode _right;

    public void addPrefix(Prefix prefix, BitSet bits, int prefixLength, int depth) {
      if (prefixLength == depth) {
        _prefix = prefix;
        return;
      } else {
        boolean currentBit = bits.get(depth);
        if (currentBit) {
          if (_right == null) {
            _right = new ByteTrieNode();
          }
          _right.addPrefix(prefix, bits, prefixLength, depth + 1);
        } else {
          if (_left == null) {
            _left = new ByteTrieNode();
          }
          _left.addPrefix(prefix, bits, prefixLength, depth + 1);
        }
      }
    }

    public boolean containsPathFromPrefix(BitSet bits, int prefixLength, int depth) {
      if (prefixLength == depth) {
        if (depth == 0 && _prefix == null) {
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
    private Prefix getLongestPrefixMatch(Ip address) {
      if (_prefix != null && _prefix.contains(address)) {
        return _prefix;
      } else {
        return null;
      }
    }

    public Prefix getLongestPrefixMatch(Ip address, BitSet bits, int index) {
      Prefix longestPrefixMatch = getLongestPrefixMatch(address);
      if (index == Prefix.MAX_PREFIX_LENGTH) {
        return longestPrefixMatch;
      }
      boolean currentBit = bits.get(index);
      Prefix longerMatch = null;
      if (currentBit) {
        if (_right != null) {
          longerMatch = _right.getLongestPrefixMatch(address, bits, index + 1);
        }
      } else {
        if (_left != null) {
          longerMatch = _left.getLongestPrefixMatch(address, bits, index + 1);
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

  private SortedSet<Prefix> _prefixes;

  private ByteTrie _trie;

  public PrefixTrie() {
    _trie = new ByteTrie();
    _prefixes = new TreeSet<>();
  }

  @JsonCreator
  public PrefixTrie(SortedSet<Prefix> prefixes) {
    _trie = new ByteTrie();
    _prefixes = prefixes;
    for (Prefix prefix : prefixes) {
      _trie.addPrefix(prefix);
    }
  }

  public boolean add(Prefix prefix) {
    if (prefix == null) {
      throw new BatfishException("Cannot add null prefix to trie");
    }
    boolean changed = _prefixes.add(prefix);
    if (changed) {
      _trie.addPrefix(prefix);
    }
    return changed;
  }

  public boolean addAll(Collection<Prefix> prefixes) {
    boolean changed = false;
    for (Prefix prefix : prefixes) {
      changed = changed || add(prefix);
    }
    return changed;
  }

  public boolean containsIp(Ip address) {
    return _trie.getLongestPrefixMatch(address) != null;
  }

  public boolean containsPathFromPrefix(Prefix prefix) {
    return _trie.containsPathFromPrefix(prefix);
  }

  public Prefix getLongestPrefixMatch(Ip address) {
    return _trie.getLongestPrefixMatch(address);
  }

  @JsonValue
  public SortedSet<Prefix> getPrefixes() {
    return Collections.unmodifiableSortedSet(_prefixes);
  }
}
