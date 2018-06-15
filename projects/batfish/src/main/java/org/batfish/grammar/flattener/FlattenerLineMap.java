package org.batfish.grammar.flattener;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FlattenerLineMap {
  /**
   * Map of new line number to word map, where word map is map of a word's starting-position in
   * new/flattened line to original line number
   */
  private NavigableMap<Integer, NavigableMap<Integer, Integer>> _lineMap;

  public FlattenerLineMap() {
    _lineMap = new TreeMap<>();
  }

  /**
   * Return original line number corresponding to the character at the specified position on the
   * specified new/flattened line. If there is no entry covering the specified position, the last
   * original line number associated with this flattened line is returned.
   */
  public @Nullable Integer getOriginalLine(
      @Nonnull Integer newLineNumber, @Nonnull Integer newStartingPosition) {
    NavigableMap<Integer, Integer> wordMap = _lineMap.get(newLineNumber);
    if (wordMap == null) {
      // Result from looking up an unmapped line, should never get here
      return null;
    } else {
      Entry<Integer, Integer> originalLineEntry = wordMap.floorEntry(newStartingPosition);
      return (originalLineEntry == null)
          ? wordMap.lastEntry().getValue()
          : originalLineEntry.getValue();
    }
  }

  /**
   * Set the original line corresponding to the word starting at the specified position on the
   * specified new/flattened line
   */
  public void setOriginalLine(
      @Nonnull Integer newLineNumber,
      @Nonnull Integer newStartingPosition,
      @Nonnull Integer originalLineNumber) {
    SortedMap<Integer, Integer> wordMap =
        _lineMap.computeIfAbsent(newLineNumber, l -> new TreeMap<>());
    wordMap.put(newStartingPosition, originalLineNumber);
  }
}
