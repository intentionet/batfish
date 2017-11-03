package org.batfish.symbolic.abstraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;

public class PrefixTrieMap implements Serializable {

  private class ByteTrie implements Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    private ByteTrieNode _root;

    ByteTrie() {
      _root = new ByteTrieNode();
    }

    void addPrefix(Prefix prefix, String device) {
      int prefixLength = prefix.getPrefixLength();
      BitSet bits = prefix.getAddress().getAddressBits();
      Set<String> devices = new HashSet<>();
      devices.add(device);
      _root.addPrefix(prefix.getNetworkPrefix(), devices, bits, prefixLength, 0);
    }
  }

  private class ByteTrieNode implements Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    private ByteTrieNode _left;

    private Prefix _prefix;

    private Set<String> _devices;

    private ByteTrieNode _right;

    private void addPrefix(
        Prefix prefix, Set<String> devices, BitSet bits, int prefixLength, int depth) {
      if (prefixLength == depth) {
        _prefix = prefix;
        if (_devices == null) {
          _devices = devices;
        } else {
          _devices.addAll(devices);
        }
      } else {
        boolean currentBit = bits.get(depth);
        if (_devices != null) {
          devices.addAll(_devices);
        }

        if (currentBit) {
          if (_right == null) {
            _right = new ByteTrieNode();
          }
          _right.addPrefix(prefix, devices, bits, prefixLength, depth + 1);
        } else {
          if (_left == null) {
            _left = new ByteTrieNode();
          }
          _left.addPrefix(prefix, devices, bits, prefixLength, depth + 1);
        }
      }
    }

    private Prefix extendPrefixWith(Prefix p, boolean val) {
      int length = p.getPrefixLength();
      assert (length < 32);
      Ip ip = p.getAddress();
      long l = ip.asLong();
      long lnew = l;
      if (val) {
        long delta = (long) Math.pow(2, 32 - length - 1);
        lnew = l + delta;
      }
      return new Prefix(new Ip(lnew), length + 1);
    }

    private void addEntry(
        Map<Set<String>, List<Prefix>> map, @Nullable Set<String> devices, @Nullable Prefix p) {
      if (devices != null) {
        List<Prefix> prefixes = map.computeIfAbsent(devices, k -> new ArrayList<>());
        prefixes.add(p);
      }
    }

    private boolean hasUniqueDevice(@Nullable Set<String> devices) {
      if (devices == null) {
        return true;
      }
      return _devices != null && !devices.containsAll(_devices)
          || _left != null && _left.hasUniqueDevice(devices)
          || _right != null && _right.hasUniqueDevice(devices);
    }

    private void createDestinationMap(
        Map<Set<String>, List<Prefix>> map,
        @Nullable Prefix prefix,
        @Nullable Set<String> devices) {
      devices = (_devices == null ? devices : _devices);
      prefix = (_prefix == null ? prefix : _prefix);
      if (_left == null && _right == null) {
        addEntry(map, devices, prefix);
      } else {
        // Optimization to avoid creating huge numbers of prefixes:
        // Check if at least one of the branches has a different device in the leaf
        if (hasUniqueDevice(devices)) {
          Prefix left = prefix == null ? null : extendPrefixWith(prefix, false);
          Prefix right = prefix == null ? null : extendPrefixWith(prefix, true);
          if (_left != null) {
            _left.createDestinationMap(map, left, devices);
          } else {
            addEntry(map, devices, left);
          }
          if (_right != null) {
            _right.createDestinationMap(map, right, _devices);
          } else {
            addEntry(map, devices, right);
          }
        } else {
          addEntry(map, devices, prefix);
        }
      }
    }
  }

  /** */
  private static final long serialVersionUID = 1L;

  private ByteTrie _trie;

  public PrefixTrieMap() {
    _trie = new ByteTrie();
  }

  public void add(Prefix prefix, String device) {
    if (prefix == null) {
      throw new BatfishException("Cannot add null prefix to trie");
    }
    _trie.addPrefix(prefix, device);
  }

  public void addAll(Collection<Prefix> prefixes, String device) {
    for (Prefix prefix : prefixes) {
      add(prefix, device);
    }
  }

  public Map<Set<String>, List<Prefix>> createDestinationMap() {
    Map<Set<String>, List<Prefix>> map = new HashMap<>();
    _trie._root.createDestinationMap(map, null, null);
    return map;
  }
}
