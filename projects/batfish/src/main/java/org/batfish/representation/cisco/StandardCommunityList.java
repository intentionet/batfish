package org.batfish.representation.cisco;

import java.util.ArrayList;
import java.util.List;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.ComparableStructure;
import org.batfish.common.util.DefinedStructure;

public final class StandardCommunityList extends ComparableStructure<String>
    implements DefinedStructure {

  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private final List<StandardCommunityListLine> _lines;

  public StandardCommunityList(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
    _lines = new ArrayList<>();
  }

  @Override
  public int getDefinitionLine() {
    return _definitionLine;
  }

  public List<StandardCommunityListLine> getLines() {
    return _lines;
  }

  public ExpandedCommunityList toExpandedCommunityList() {
    ExpandedCommunityList newList = new ExpandedCommunityList(_key, _definitionLine);
    for (StandardCommunityListLine line : _lines) {
      List<Long> standardCommunities = line.getCommunities();
      String regex;
      if (standardCommunities.size() == 1) {
        regex = "^" + CommonUtil.longToCommunity(standardCommunities.get(0)) + "$";
      } else {
        regex = "(";
        for (Long l : standardCommunities) {
          regex += "^" + CommonUtil.longToCommunity(l) + "$|";
        }
        regex = regex.substring(0, regex.length() - 1) + ")";
      }
      ExpandedCommunityListLine newLine = new ExpandedCommunityListLine(line.getAction(), regex);
      newList.addLine(newLine);
    }
    return newList;
  }
}
