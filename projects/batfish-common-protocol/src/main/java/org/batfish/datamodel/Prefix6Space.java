package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

public class Prefix6Space implements Serializable {

  private static class BitTrie implements Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    private BitTrieNode _root;

    public BitTrie() {
      _root = new BitTrieNode();
    }

    public void addPrefix6Range(Prefix6Range prefix6Range) {
      Prefix6 prefix6 = prefix6Range.getPrefix6();
      int prefixLength = prefix6.getPrefixLength();
      BitSet bits = prefix6.getAddress().getAddressBits();
      _root.addPrefix6Range(prefix6Range, bits, prefixLength, 0);
    }

    public void addTrieNodeSpace(BitTrieNode node) {
      if (node._left != null) {
        addTrieNodeSpace(node._left);
      }
      if (node._right != null) {
        addTrieNodeSpace(node._right);
      }
      for (Prefix6Range prefix6Range : node._prefix6Ranges) {
        addPrefix6Range(prefix6Range);
      }
    }

    public boolean containsPrefix6Range(Prefix6Range prefix6Range) {
      Prefix6 prefix6 = prefix6Range.getPrefix6();
      int prefixLength = prefix6.getPrefixLength();
      BitSet bits = prefix6.getAddress().getAddressBits();
      return _root.containsPrefix6Range(prefix6Range, bits, prefixLength, 0);
    }

    public Set<Prefix6Range> getPrefix6Ranges() {
      Set<Prefix6Range> prefix6Ranges = new HashSet<>();
      _root.collectPrefix6Ranges(prefix6Ranges);
      return prefix6Ranges;
    }
  }

  private static class BitTrieNode implements Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    private BitTrieNode _left;

    private Set<Prefix6Range> _prefix6Ranges;

    private BitTrieNode _right;

    public BitTrieNode() {
      _prefix6Ranges = new HashSet<>();
    }

    public void addPrefix6Range(
        Prefix6Range prefix6Range, BitSet bits, int prefixLength, int depth) {
      for (Prefix6Range nodeRange : _prefix6Ranges) {
        if (nodeRange.includesPrefix6Range(prefix6Range)) {
          return;
        }
      }
      if (prefixLength == depth) {
        _prefix6Ranges.add(prefix6Range);
        prune(prefix6Range);
      } else {
        boolean currentBit = bits.get(depth);
        if (currentBit) {
          if (_right == null) {
            _right = new BitTrieNode();
          }
          _right.addPrefix6Range(prefix6Range, bits, prefixLength, depth + 1);
        } else {
          if (_left == null) {
            _left = new BitTrieNode();
          }
          _left.addPrefix6Range(prefix6Range, bits, prefixLength, depth + 1);
        }
      }
    }

    public void collectPrefix6Ranges(Set<Prefix6Range> prefix6Ranges) {
      prefix6Ranges.addAll(_prefix6Ranges);
      if (_left != null) {
        _left.collectPrefix6Ranges(prefix6Ranges);
      }
      if (_right != null) {
        _right.collectPrefix6Ranges(prefix6Ranges);
      }
    }

    public boolean containsPrefix6Range(
        Prefix6Range prefix6Range, BitSet bits, int prefixLength, int depth) {
      for (Prefix6Range nodeRange : _prefix6Ranges) {
        if (nodeRange.includesPrefix6Range(prefix6Range)) {
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
            return _right.containsPrefix6Range(prefix6Range, bits, prefixLength, depth + 1);
          }
        } else {
          if (_left == null) {
            return false;
          } else {
            return _left.containsPrefix6Range(prefix6Range, bits, prefixLength, depth + 1);
          }
        }
      }
    }

    private boolean isEmpty() {
      return _left == null && _right == null && _prefix6Ranges.isEmpty();
    }

    private void prune(Prefix6Range prefix6Range) {
      if (_left != null) {
        _left.prune(prefix6Range);
        if (_left.isEmpty()) {
          _left = null;
        }
      }
      if (_right != null) {
        _right.prune(prefix6Range);
        if (_right.isEmpty()) {
          _right = null;
        }
      }
      Set<Prefix6Range> oldPrefix6Ranges = new HashSet<>();
      oldPrefix6Ranges.addAll(_prefix6Ranges);
      for (Prefix6Range oldPrefix6Range : oldPrefix6Ranges) {
        if (!prefix6Range.equals(oldPrefix6Range)
            && prefix6Range.includesPrefix6Range(oldPrefix6Range)) {
          _prefix6Ranges.remove(oldPrefix6Range);
        }
      }
    }
  }

  /** */
  private static final long serialVersionUID = 1L;

  private BitTrie _trie;

  public Prefix6Space() {
    _trie = new BitTrie();
  }

  @JsonCreator
  public Prefix6Space(Iterable<Prefix6Range> prefix6Ranges) {
    _trie = new BitTrie();
    for (Prefix6Range prefix6Range : prefix6Ranges) {
      _trie.addPrefix6Range(prefix6Range);
    }
  }

  public Prefix6Space(Prefix6Range... prefix6Ranges) {
    this(Arrays.asList(prefix6Ranges));
  }

  public void addPrefix6(Prefix6 prefix6) {
    addPrefix6Range(Prefix6Range.fromPrefix6(prefix6));
  }

  public void addPrefix6Range(Prefix6Range prefix6Range) {
    _trie.addPrefix6Range(prefix6Range);
  }

  public void addSpace(Prefix6Space prefix6Space) {
    _trie.addTrieNodeSpace(prefix6Space._trie._root);
  }

  public boolean containsPrefix6(Prefix6 prefix6) {
    return containsPrefix6Range(Prefix6Range.fromPrefix6(prefix6));
  }

  public boolean containsPrefix6Range(Prefix6Range prefix6Range) {
    return _trie.containsPrefix6Range(prefix6Range);
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
    return getPrefix6Ranges().equals(((Prefix6Space) obj).getPrefix6Ranges());
  }

  @JsonValue
  public Set<Prefix6Range> getPrefix6Ranges() {
    return _trie.getPrefix6Ranges();
  }

  @Override
  public int hashCode() {
    return getPrefix6Ranges().hashCode();
  }

  public Prefix6Space intersection(Prefix6Space intersectSpace) {
    Prefix6Space newSpace = new Prefix6Space();
    Set<Prefix6Range> intersectRanges = intersectSpace.getPrefix6Ranges();
    for (Prefix6Range intersectRange : intersectRanges) {
      if (containsPrefix6Range(intersectRange)) {
        newSpace.addPrefix6Range(intersectRange);
      }
    }
    return newSpace;
  }

  @JsonIgnore
  public boolean isEmpty() {
    return _trie._root.isEmpty();
  }

  public boolean overlaps(Prefix6Space intersectSpace) {
    Prefix6Space intersection = intersection(intersectSpace);
    return !intersection.isEmpty();
  }

  @Override
  public String toString() {
    return getPrefix6Ranges().toString();
  }
}
