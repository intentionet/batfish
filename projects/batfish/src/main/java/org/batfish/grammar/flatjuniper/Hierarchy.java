package org.batfish.grammar.flatjuniper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.batfish.common.BatfishException;
import org.batfish.config.Settings;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Deactivate_lineContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Flat_juniper_configurationContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Set_lineContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Set_line_tailContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.StatementContext;
import org.batfish.grammar.flatjuniper.Hierarchy.HierarchyTree.HierarchyPath;
import org.batfish.main.PartialGroupMatchException;
import org.batfish.main.UndefinedGroupBatfishException;

public class Hierarchy {

  public static class HierarchyTree {

    private enum AddPathResult {
      BLACKLISTED,
      MODIFIED,
      UNMODIFIED
    }

    private abstract static class HierarchyChildNode extends HierarchyNode {

      private Set_lineContext _line;
      protected String _sourceGroup;
      public List<String> _sourceWildcards;
      protected String _text;

      private HierarchyChildNode(String text) {
        _text = text;
      }

      public abstract HierarchyChildNode copy();

      public abstract boolean isMatchedBy(HierarchyLiteralNode node);

      public abstract boolean isMatchedBy(HierarchyWildcardNode node);

      public abstract boolean matches(HierarchyChildNode node);
    }

    private static final class HierarchyLiteralNode extends HierarchyChildNode {

      private HierarchyLiteralNode(String text) {
        super(text);
      }

      @Override
      public HierarchyChildNode copy() {
        return new HierarchyLiteralNode(_text);
      }

      @Override
      public boolean isMatchedBy(HierarchyLiteralNode node) {
        return _text.equals(node._text);
      }

      @Override
      public boolean isMatchedBy(HierarchyWildcardNode node) {
        String regex = node._wildcard.replaceAll("\\*", ".*");
        return _text.matches(regex);
      }

      @Override
      public boolean matches(HierarchyChildNode node) {
        return node.isMatchedBy(this);
      }

      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Literal(" + _text);
        if (_sourceGroup != null) {
          sb.append(", srcGroup:\"" + _sourceGroup + "\"");
        }
        if (_sourceWildcards != null) {
          sb.append(", srcWildcard:\"" + _sourceWildcards + "\"");
        }
        sb.append(")");
        return sb.toString();
      }
    }

    private abstract static class HierarchyNode {

      protected Set<String> _blacklistedGroups;
      private Map<String, HierarchyChildNode> _children;

      public HierarchyNode() {
        _children = new LinkedHashMap<>();
        _blacklistedGroups = new HashSet<>();
      }

      public void addBlacklistedGroup(String groupName) {
        _blacklistedGroups.add(groupName);
      }

      public void addChildNode(HierarchyChildNode node) {
        _children.put(node._text, node);
      }

      public HierarchyChildNode getChildNode(String text) {
        return _children.get(text);
      }

      public Map<String, HierarchyChildNode> getChildren() {
        return _children;
      }

      @Nullable
      public HierarchyChildNode getFirstMatchingChildNode(HierarchyChildNode node) {
        for (HierarchyChildNode child : _children.values()) {
          if (child.matches(node)) {
            return child;
          }
        }
        return null;
      }

      public boolean isWildcard() {
        return false;
      }
    }

    public static final class HierarchyPath {

      private boolean _containsWildcard;
      private List<HierarchyChildNode> _nodes;
      private StatementContext _statement;

      public HierarchyPath() {
        _nodes = new ArrayList<>();
      }

      public void addNode(String text) {
        HierarchyChildNode newNode = new HierarchyLiteralNode(text);
        _nodes.add(newNode);
      }

      public void addWildcardNode(String text) {
        _containsWildcard = true;
        HierarchyChildNode newNode = new HierarchyWildcardNode(text);
        _nodes.add(newNode);
      }

      public boolean containsWildcard() {
        return _containsWildcard;
      }

      public String pathString() {
        StringBuilder sb = new StringBuilder();
        for (HierarchyChildNode node : _nodes) {
          sb.append(node._text + " ");
        }
        String out = sb.toString().trim();
        return out;
      }

      public void setStatement(StatementContext statement) {
        _statement = statement;
      }

      @Override
      public String toString() {
        return "Path(Statement:" + _statement + "," + _nodes + ")";
      }
    }

    private static final class HierarchyRootNode extends HierarchyNode {}

    private static final class HierarchyWildcardNode extends HierarchyChildNode {

      private String _wildcard;

      private HierarchyWildcardNode(String text) {
        super(text);
        if (text.charAt(0) != '<' || text.charAt(text.length() - 1) != '>') {
          throw new BatfishException("Improperly-formatted wildcard: " + text);
        }
        _wildcard = text.substring(1, text.length() - 1);
      }

      @Override
      public HierarchyChildNode copy() {
        return new HierarchyWildcardNode(_text);
      }

      @Override
      public boolean isMatchedBy(HierarchyLiteralNode node) {
        return false;
      }

      @Override
      public boolean isMatchedBy(HierarchyWildcardNode node) {
        // TODO: check whether this is the only way to match two wildcards
        return _text.equals(node._text);
      }

      @Override
      public boolean isWildcard() {
        return true;
      }

      @Override
      public boolean matches(HierarchyChildNode node) {
        return node.isMatchedBy(this);
      }

      @Override
      public String toString() {
        return "Wildcard(" + _text + ")";
      }
    }

    private static Settings parserSettings() {
      Settings settings = new Settings();
      settings.setThrowOnLexerError(true);
      settings.setThrowOnParserError(true);
      return settings;
    }

    private String _groupName;

    private HierarchyRootNode _root;

    private HierarchyTree(@Nullable String groupName) {
      _groupName = groupName;
      _root = new HierarchyRootNode();
    }

    private void addGroupPaths(
        @Nullable Set_lineContext groupLine,
        Collection<HierarchyChildNode> currentGroupChildren,
        HierarchyTree masterTree,
        HierarchyPath path,
        List<ParseTree> lines,
        Flat_juniper_configurationContext configurationContext) {
      if (groupLine != null) {
        Set_lineContext setLine = new Set_lineContext(configurationContext, -1);
        if (masterTree.addPath(path, setLine, _groupName) == AddPathResult.BLACKLISTED) {
          return;
        }
        StringBuilder sb = new StringBuilder();
        for (HierarchyChildNode pathNode : path._nodes) {
          sb.append(pathNode._text + " ");
        }
        String newStatementText = sb.toString();
        // get rid of last " ", which matters for tokens where whitespace is
        // not ignored
        newStatementText =
            "set " + newStatementText.substring(0, newStatementText.length() - 1) + "\n";
        TerminalNode set = new TerminalNodeImpl(new CommonToken(FlatJuniperLexer.SET, "set"));
        Set_line_tailContext setLineTail = new Set_line_tailContext(setLine, -1);
        TerminalNode newline =
            new TerminalNodeImpl(new CommonToken(FlatJuniperLexer.NEWLINE, "\n"));
        setLine.children = new ArrayList<ParseTree>();
        setLine.children.add(set);
        setLine.children.add(setLineTail);
        setLine.children.add(newline);
        Settings settings = parserSettings();
        FlatJuniperCombinedParser parser =
            new FlatJuniperCombinedParser(newStatementText, settings);
        Flat_juniper_configurationContext newConfiguration =
            parser.getParser().flat_juniper_configuration();
        // StatementContext newStatement = parser.getParser().statement();
        StatementContext newStatement = newConfiguration.set_line(0).set_line_tail().statement();
        newStatement.parent = setLineTail;

        setLineTail.children = new ArrayList<ParseTree>();
        setLineTail.children.add(newStatement);
        lines.add(setLine);
      }
      for (HierarchyChildNode childNode : currentGroupChildren) {
        HierarchyChildNode newPathNode = childNode.copy();
        path._nodes.add(newPathNode);
        addGroupPaths(
            childNode._line,
            childNode.getChildren().values(),
            masterTree,
            path,
            lines,
            configurationContext);
        path._nodes.remove(path._nodes.size() - 1);
      }
    }

    public AddPathResult addPath(
        HierarchyPath path, @Nullable Set_lineContext ctx, @Nullable String group) {
      AddPathResult result = AddPathResult.UNMODIFIED;
      HierarchyNode currentNode = _root;
      HierarchyChildNode matchNode = null;
      for (HierarchyChildNode currentPathNode : path._nodes) {
        matchNode = currentNode.getChildNode(currentPathNode._text);
        if (matchNode == null) {
          result = AddPathResult.MODIFIED;
          matchNode = currentPathNode.copy();
          currentNode.addChildNode(matchNode);
        }
        if (matchNode._blacklistedGroups.contains(group)) {
          return AddPathResult.BLACKLISTED;
        }
        currentNode = matchNode;
      }
      matchNode._line = ctx;
      matchNode._sourceGroup = group;
      return result;
    }

    public List<ParseTree> applyWildcardPath(
        HierarchyPath path, Flat_juniper_configurationContext configurationContext) {
      HierarchyChildNode wildcardNode = findExactPathMatchNode(path);
      String sourceGroup = wildcardNode._sourceGroup;
      int remainingWildcards = 0;
      for (HierarchyChildNode node : path._nodes) {
        if (node.isWildcard()) {
          remainingWildcards++;
        }
      }
      List<String> appliedWildcards = new ArrayList<>();
      HierarchyPath newPath = new HierarchyPath();
      List<ParseTree> lines = new ArrayList<>();
      applyWildcardPath(
          path,
          configurationContext,
          sourceGroup,
          _root,
          0,
          remainingWildcards,
          appliedWildcards,
          newPath,
          lines);
      return lines;
    }

    private void applyWildcardPath(
        HierarchyPath path,
        Flat_juniper_configurationContext configurationContext,
        String sourceGroup,
        HierarchyNode destinationTreeRoot,
        int startingIndex,
        int remainingWildcards,
        List<String> appliedWildcards,
        HierarchyPath newPath,
        List<ParseTree> lines) {
      if (destinationTreeRoot._blacklistedGroups.contains(sourceGroup)) {
        return;
      }
      HierarchyChildNode currentPathNode = path._nodes.get(startingIndex);
      if (!currentPathNode.isWildcard()) {
        String currentPathNodeText = currentPathNode._text;
        HierarchyChildNode newDestinationTreeRoot =
            destinationTreeRoot.getChildNode(currentPathNodeText);
        if (newDestinationTreeRoot == null) {
          // If literal node does not exist, but there are still more
          // wildcards to match, we abort.
          // Else, we create node and continue recursing
          if (remainingWildcards > 0) {
            return;
          }
          newDestinationTreeRoot = currentPathNode.copy();
          destinationTreeRoot._children.put(newDestinationTreeRoot._text, newDestinationTreeRoot);
        }
        newPath._nodes.add(newDestinationTreeRoot);
        if (startingIndex == path._nodes.size() - 1) {
          newDestinationTreeRoot._sourceWildcards = new ArrayList<>();
          newDestinationTreeRoot._sourceWildcards.addAll(appliedWildcards);
          newDestinationTreeRoot._line = generateSetLine(newPath, configurationContext);
          lines.add(newDestinationTreeRoot._line);
        } else {
          applyWildcardPath(
              path,
              configurationContext,
              sourceGroup,
              newDestinationTreeRoot,
              startingIndex + 1,
              remainingWildcards,
              appliedWildcards,
              newPath,
              lines);
        }
        newPath._nodes.remove(newPath._nodes.size() - 1);
      } else {
        appliedWildcards.add(currentPathNode._text);
        if (startingIndex < path._nodes.size() - 1) {
          for (HierarchyChildNode destinationTreeNode : destinationTreeRoot._children.values()) {
            // if there are no matching children, then we recurse no
            // further
            if (!destinationTreeNode.isWildcard() && currentPathNode.matches(destinationTreeNode)) {
              newPath._nodes.add(destinationTreeNode);
              applyWildcardPath(
                  path,
                  configurationContext,
                  sourceGroup,
                  destinationTreeNode,
                  startingIndex + 1,
                  remainingWildcards - 1,
                  appliedWildcards,
                  newPath,
                  lines);
              newPath._nodes.remove(newPath._nodes.size() - 1);
            }
          }
        }
        appliedWildcards.remove(appliedWildcards.size() - 1);
      }
    }

    public boolean containsPathPrefixOf(HierarchyPath path) {
      Map<String, HierarchyChildNode> currentChildren = _root.getChildren();
      List<HierarchyChildNode> pathNodes = path._nodes;
      for (HierarchyChildNode currentPathNode : pathNodes) {
        HierarchyChildNode treeMatchNode = currentChildren.get(currentPathNode._text);
        if (treeMatchNode != null) {
          if (treeMatchNode.getChildren().size() == 0) {
            return true;
          } else {
            currentChildren = treeMatchNode.getChildren();
          }
        } else {
          break;
        }
      }
      return false;
    }

    private HierarchyChildNode findExactPathMatchNode(HierarchyPath path) {
      HierarchyNode currentGroupNode = _root;
      HierarchyChildNode matchNode = null;
      for (HierarchyChildNode currentPathNode : path._nodes) {
        matchNode = currentGroupNode.getChildNode(currentPathNode._text);
        currentGroupNode = matchNode;
      }
      return matchNode;
    }

    private Set_lineContext generateSetLine(
        HierarchyPath path, Flat_juniper_configurationContext configurationContext) {
      Set_lineContext setLine = new Set_lineContext(configurationContext, -1);
      StringBuilder sb = new StringBuilder();
      for (HierarchyChildNode pathNode : path._nodes) {
        sb.append(pathNode._text + " ");
      }
      String newStatementText = sb.toString();
      newStatementText =
          "set " + newStatementText.substring(0, newStatementText.length() - 1) + "\n";
      TerminalNode set = new TerminalNodeImpl(new CommonToken(FlatJuniperLexer.SET, "set"));
      Set_line_tailContext setLineTail = new Set_line_tailContext(setLine, -1);
      TerminalNode newline = new TerminalNodeImpl(new CommonToken(FlatJuniperLexer.NEWLINE, "\n"));
      setLine.children = new ArrayList<ParseTree>();
      setLine.children.add(set);
      setLine.children.add(setLineTail);
      setLine.children.add(newline);
      Settings settings = parserSettings();
      FlatJuniperCombinedParser parser = new FlatJuniperCombinedParser(newStatementText, settings);
      Flat_juniper_configurationContext newConfiguration =
          parser.getParser().flat_juniper_configuration();
      StatementContext newStatement = newConfiguration.set_line(0).set_line_tail().statement();
      newStatement.parent = setLineTail;

      setLineTail.children = new ArrayList<ParseTree>();
      setLineTail.children.add(newStatement);

      return setLine;
    }

    public List<ParseTree> getApplyGroupsLines(
        HierarchyPath path,
        Flat_juniper_configurationContext configurationContext,
        HierarchyTree masterTree) {
      List<ParseTree> lines = new ArrayList<>();
      HierarchyNode currentGroupNode = _root;
      HierarchyChildNode matchNode = null;
      HierarchyPath partialMatch = new HierarchyPath();
      if (path._nodes.size() == 0) {
        addGroupPaths(
            null, _root.getChildren().values(), masterTree, path, lines, configurationContext);
      } else {
        for (HierarchyChildNode currentPathNode : path._nodes) {
          matchNode = currentGroupNode.getFirstMatchingChildNode(currentPathNode);
          if (matchNode == null) {
            String message = "No matching path";
            if (partialMatch._nodes.size() > 0) {
              message +=
                  ": Partial path match within applied group: \""
                      + partialMatch.pathString()
                      + "\"";
            }
            throw new PartialGroupMatchException(message);
          }
          partialMatch._nodes.add(matchNode);
          currentGroupNode = matchNode;
        }

        // at this point, matchNode is the node in the group tree whose
        // children must be added to the main tree with substitutions
        // applied
        // according to the supplied path
        addGroupPaths(
            matchNode._line,
            matchNode.getChildren().values(),
            masterTree,
            path,
            lines,
            configurationContext);
      }
      return lines;
    }

    public List<ParseTree> getApplyPathLines(
        HierarchyPath basePath,
        HierarchyPath applyPathPath,
        Flat_juniper_configurationContext configurationContext) {
      List<ParseTree> lines = new ArrayList<>();
      List<String> candidatePrefixes = getApplyPathPrefixes(applyPathPath);
      for (String candidatePrefix : candidatePrefixes) {
        String finalPrefixStr;
        boolean ipv6 = candidatePrefix.contains(":");
        boolean isPrefix = candidatePrefix.contains("/");
        try {
          if (isPrefix) {
            if (ipv6) {
              Prefix6 prefix6 = new Prefix6(candidatePrefix);
              finalPrefixStr = prefix6.toString();
            } else {
              Prefix prefix = Prefix.fromString(candidatePrefix);
              finalPrefixStr = prefix.toString();
            }
          } else {
            String candidateAddress;
            if (ipv6) {
              candidateAddress = new Ip6(candidatePrefix).toString();
            } else {
              candidateAddress = new Ip(candidatePrefix).toString();
            }
            finalPrefixStr = candidateAddress + (ipv6 ? "/64" : "/32");
          }
        } catch (BatfishException e) {
          throw new BatfishException(
              "Invalid ip(v6) address or prefix: \"" + candidatePrefix + "\"", e);
        }
        basePath.addNode(finalPrefixStr);
        Set_lineContext setLine = generateSetLine(basePath, configurationContext);
        lines.add(setLine);
        basePath._nodes.remove(basePath._nodes.size() - 1);
      }
      return lines;
    }

    private List<String> getApplyPathPrefixes(HierarchyPath path) {
      List<String> prefixes = new ArrayList<>();
      getApplyPathPrefixes(path, _root, 0, prefixes);
      return prefixes;
    }

    private void getApplyPathPrefixes(
        HierarchyPath path, HierarchyNode currentNode, int currentDepth, List<String> prefixes) {
      if (currentDepth == path._nodes.size() - 1) {
        for (HierarchyChildNode currentChild : currentNode.getChildren().values()) {
          if (!currentChild.isWildcard()) {
            prefixes.add(currentChild._text);
          }
        }
      } else {
        HierarchyChildNode currentPathNode = path._nodes.get(currentDepth);
        for (HierarchyChildNode currentChild : currentNode.getChildren().values()) {
          if (currentPathNode.matches(currentChild)) {
            getApplyPathPrefixes(path, currentChild, currentDepth + 1, prefixes);
          }
        }
      }
    }

    public String getGroupName() {
      return _groupName;
    }

    public void pruneAfterPath(HierarchyPath path) {
      HierarchyChildNode pathEnd = findExactPathMatchNode(path);
      pathEnd.getChildren().clear();
    }

    public void setApplyGroupsExcept(HierarchyPath path, String groupName) {
      HierarchyChildNode node = findExactPathMatchNode(path);
      node.addBlacklistedGroup(groupName);
    }
  }

  private HierarchyTree _deactivateTree;

  private HierarchyTree _masterTree;

  private Map<String, HierarchyTree> _trees;

  public Hierarchy() {
    _trees = new HashMap<>();
    _masterTree = new HierarchyTree(null);
    _deactivateTree = new HierarchyTree(null);
  }

  public void addDeactivatePath(HierarchyPath path, Deactivate_lineContext ctx) {
    if (isDeactivated(path)) {
      return;
    }
    _deactivateTree.addPath(path, null, null);
    _deactivateTree.pruneAfterPath(path);
  }

  public void addMasterPath(HierarchyPath path, @Nullable Set_lineContext ctx) {
    _masterTree.addPath(path, ctx, null);
  }

  public List<ParseTree> getApplyGroupsLines(
      String groupName,
      HierarchyPath path,
      Flat_juniper_configurationContext configurationContext) {
    HierarchyTree tree = _trees.get(groupName);
    if (tree == null) {
      throw new UndefinedGroupBatfishException("No such group: \"" + groupName + "\"");
    }
    return tree.getApplyGroupsLines(path, configurationContext, _masterTree);
  }

  public List<ParseTree> getApplyPathLines(
      HierarchyPath basePath,
      HierarchyPath applyPathPath,
      Flat_juniper_configurationContext configurationContext) {
    return _masterTree.getApplyPathLines(basePath, applyPathPath, configurationContext);
  }

  public HierarchyTree getMasterTree() {
    return _masterTree;
  }

  public HierarchyTree getTree(String groupName) {
    return _trees.get(groupName);
  }

  public boolean isDeactivated(HierarchyPath path) {
    return _deactivateTree.containsPathPrefixOf(path);
  }

  public HierarchyTree newTree(String groupName) {
    HierarchyTree newTree = new HierarchyTree(groupName);
    _trees.put(groupName, newTree);
    return newTree;
  }

  public void setApplyGroupsExcept(HierarchyPath path, String groupName) {
    _masterTree.setApplyGroupsExcept(path, groupName);
  }
}
