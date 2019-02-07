package org.batfish.datamodel;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A generic implementation of a Trie, specialized to keys being prefixes.
 *
 * <p>This trie is a more restrictive version of a ddNF (disjoint difference Normal Form), where the
 * wildcard symbols can appear only after (to-the-right-of) non wildcard symbols in the bit vector.
 * E.g., 101010**, but not 1*001***
 *
 * <p>Internally, this data structure employs path compression which optimizes look-ups, since
 * branching does not have to be done on each bit of the prefix.
 */
@ParametersAreNonnullByDefault
public abstract class PrefixTrieNode<NodeT extends PrefixTrieNode<NodeT, DataT>, DataT>
    implements Serializable {

  private static final long serialVersionUID = 1L;

  @Nonnull protected final Prefix _prefix;
  @Nonnull protected final Set<DataT> _objects;

  @Nullable protected NodeT _left;
  @Nullable protected NodeT _right;

  protected PrefixTrieNode(Prefix prefix) {
    _objects = new HashSet<>();
    _prefix = prefix;
  }

  /** Return the key {@link Prefix} for this node */
  @Nonnull
  public final Prefix getPrefix() {
    return _prefix;
  }

  /** Create a new node */
  @Nonnull
  public abstract NodeT makeNode(Prefix prefix);

  @Nonnull
  public abstract NodeT getThis();

  /** Find or create a node for a given prefix (must be an exact match) */
  @Nonnull
  protected NodeT findOrCreateNode(Prefix prefix) {
    return findOrCreateNode(prefix, prefix.getStartIp().asLong(), 0);
  }

  protected void collect(ImmutableCollection.Builder<DataT> collectionBuilder) {
    if (_left != null) {
      _left.collect(collectionBuilder);
    }
    if (_right != null) {
      _right.collect(collectionBuilder);
    }
    collectionBuilder.addAll(_objects);
  }

  @Nonnull
  NodeT findOrCreateNode(Prefix prefix, long bits, int firstUnmatchedBitIndex) {
    /*
     * We have reached the node where an object should be inserted, because:
     * 1) the prefix length of this node matches the desired prefix length exactly, and
     * 2) going deeper can only gets us longer matches
     */
    if (prefix.getPrefixLength() == _prefix.getPrefixLength()) {
      return getThis();
    }

    /*
     * The prefix match is not exact, do some extra insertion logic.
     * Current bit determines which side of the tree to go down (1 = right, 0 = left)
     */
    boolean currentBit = Ip.getBitAtPosition(bits, firstUnmatchedBitIndex);
    return findHelper(getThis(), prefix, bits, firstUnmatchedBitIndex, currentBit);
  }

  @Nullable
  protected final NodeT findNode(Prefix prefix) {
    return findNode(prefix.getStartIp().asLong(), prefix.getPrefixLength(), 0);
  }

  @Nullable
  NodeT findNode(long bits, int prefixLength, int firstUnmatchedBitIndex) {
    // If prefix lengths match, this is the node where such object would be stored.
    if (prefixLength == _prefix.getPrefixLength()) {
      return getThis();
    }

    boolean currentBit = Ip.getBitAtPosition(bits, firstUnmatchedBitIndex);
    /*
     * If prefixes don't match exactly, look at the current bit. That determines whether we look
     * left or right. As long as the child is not null, recurse.
     *
     * Note that:
     * 1) objects are stored in the nodes where lengths of the node prefix and the desired prefix
     *    match exactly; and
     * 2) prefix matches only get more specific (longer) the deeper we go in the tree
     *
     * Therefore, we can fast-forward the firstUnmatchedBitIndex to the prefix length of the
     * child node
     */
    if (currentBit) {
      return (_right != null)
          ? _right.findNode(bits, prefixLength, _right._prefix.getPrefixLength())
          : null;
    } else {
      return (_left != null)
          ? _left.findNode(bits, prefixLength, _left._prefix.getPrefixLength())
          : null;
    }
  }

  /**
   * Returns a set of routes with the longest prefix match for a given IP address
   *
   * @param address IP address
   * @param bits IP address represented as a set of bits
   * @param maxPrefixLength only return routes with prefix length less than or equal to given value
   * @return a set of routes
   */
  @Nullable
  protected NodeT findLongestPrefixMatchNode(Ip address, long bits, int maxPrefixLength) {
    // If the current subtree only contains routes that are too long, no matches.
    int index = _prefix.getPrefixLength();
    if (index > maxPrefixLength) {
      return null;
    }

    // If the current subtree does not contain the destination IP, no matches.
    if (!_prefix.containsIp(address)) {
      return null;
    }

    // If the network of the current node is exactly the desired maximum length, stop here.
    if (index == maxPrefixLength) {
      return getThis();
    }

    // Examine the bit at the given index
    boolean currentBit = Ip.getBitAtPosition(bits, index);
    // If the current bit is 1, go right recursively
    NodeT child = currentBit ? _right : _left;
    if (child == null) {
      return getThis();
    }

    // Represents any potentially longer route matches (than ones stored at this node)
    NodeT candidateNode = child.findLongestPrefixMatchNode(address, bits, maxPrefixLength);

    // If we found no better matches, return the ones from this node
    if (candidateNode == null) {
      return getThis();
    } else { // otherwise return longer matches
      return candidateNode;
    }
  }

  /** Retrieve an immutable copy of objects currently available for the given prefix. */
  @Nonnull
  protected Set<DataT> get(Prefix p) {
    NodeT node = findNode(p.getStartIp().asLong(), p.getPrefixLength(), 0);
    return node == null ? ImmutableSet.of() : ImmutableSet.copyOf(node._objects);
  }

  /**
   * Takes care of adding new nodes to the tree and maintaining correct pointers.
   *
   * @param parent node that we are trying to merge an object into
   * @param prefix the desired prefix the object should be mapped to
   * @param bits the {@code long} representation of the desired prefix
   * @param firstUnmatchedBitIndex the index of the first bit in the desired prefix that we haven't
   *     checked yet
   * @param rightBranch whether we should recurse down the right side of the tree
   * @return the node into which an object can be added
   */
  @Nonnull
  private NodeT findHelper(
      NodeT parent, Prefix prefix, long bits, int firstUnmatchedBitIndex, boolean rightBranch) {
    NodeT node;

    // Get our node from one of the tree sides
    if (rightBranch) {
      node = parent._right;
    } else {
      node = parent._left;
    }

    // Node doesn't exist, so create one. By construction, it will be the best match
    // for the given objectToMerge
    if (node == null) {
      node = makeNode(prefix);
      // don't forget to assign new node object to parent node
      assignChild(parent, node, rightBranch);
      return node;
    }

    // Node exists, get some helper data out of the current node we are examining
    Prefix nodePrefix = node._prefix;
    int nodePrefixLength = nodePrefix.getPrefixLength();
    Ip nodeAddress = nodePrefix.getStartIp();
    long nodeAddressBits = nodeAddress.asLong();
    int nextUnmatchedBit;
    // Set up two "pointers" as we scan through the bits and the node's prefixes
    boolean currentAddressBit = false;
    boolean currentNodeAddressBit;

    /*
     * We know we matched up to firstUnmatchedBitIndex. Continue going forward in the bits
     * to find a longer match.
     * At the end of this loop nextUnmatchedBit will be the first place where the objectToMerge prefix
     * and this node's prefix diverge.
     * Note that nextUnmatchedBit can be outside of the node's or the objectToMerge's prefix.
     */
    for (nextUnmatchedBit = firstUnmatchedBitIndex + 1;
        nextUnmatchedBit < nodePrefixLength && nextUnmatchedBit < prefix.getPrefixLength();
        nextUnmatchedBit++) {
      currentAddressBit = Ip.getBitAtPosition(bits, nextUnmatchedBit);
      currentNodeAddressBit = Ip.getBitAtPosition(nodeAddressBits, nextUnmatchedBit);
      if (currentNodeAddressBit != currentAddressBit) {
        break;
      }
    }

    /*
     * If the next unmatched bit is the same as node prefix length, we "ran off" the node prefix.
     * Recursively find the appropriate node that's a child of this node.
     */
    if (nextUnmatchedBit == nodePrefixLength) {
      return node.findOrCreateNode(prefix, bits, nextUnmatchedBit);
    }

    /*
     * If we reached the desired prefix length (but have not exhausted the nodes) we need to create a new node
     * above the current node that matches the prefix and re-attach the current node to
     * the newly created node.
     */
    if (nextUnmatchedBit == prefix.getPrefixLength()) {
      currentNodeAddressBit = Ip.getBitAtPosition(nodeAddressBits, nextUnmatchedBit);
      NodeT oldNode = node;
      node = makeNode(prefix);
      // Keep track of pointers
      assignChild(parent, node, rightBranch);
      assignChild(node, oldNode, currentNodeAddressBit);
      return node;
    }

    /*
     * If we are here, there is a bit difference between the node's prefix and the desired prefix,
     * before we reach the end of either prefix. This requires the following:
     * - Compute the max prefix match (up to nextUnmatchedBit)
     * - Create a new node with this new prefix above the current node
     * - Create a new node with the objectToMerge's full prefix and assign it the parent.
     * - Existing node becomes a sibling of the node with full objectToMerge prefix
     */
    NodeT oldNode = node;

    // newNetwork has the max prefix match up to nextUnmatchedBit
    Prefix newNetwork = Prefix.create(prefix.getStartIp(), nextUnmatchedBit);
    node = makeNode(newNetwork); // this is the node we are inserting in the middle
    NodeT child = makeNode(prefix);

    assignChild(parent, node, rightBranch);
    // child and old node become siblings, children of the newly inserted node
    assignChild(node, child, currentAddressBit);
    assignChild(node, oldNode, !currentAddressBit);
    return child;
  }

  private void assignChild(NodeT parent, NodeT child, boolean branchRight) {
    if (branchRight) {
      parent._right = child;
    } else {
      parent._left = child;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrefixTrieNode<?, ?> that = (PrefixTrieNode<?, ?>) o;
    return getPrefix().equals(that.getPrefix())
        && Objects.equals(_left, that._left)
        && Objects.equals(_right, that._right)
        && _objects.equals(that._objects);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_prefix, _left, _right, _objects);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("prefix", _prefix).toString();
  }
}
