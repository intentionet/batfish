package org.batfish.symbolic.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.batfish.datamodel.Interface;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.GraphEdge;

public class PatternUtils {

  public static List<String> findMatchingDestinationNodes(Graph graph, PathRegexes p) {
    return findMatchingNodes(graph, p.getDstRegex(), p.getNotDstRegex());
  }

  public static List<String> findMatchingSourceNodes(Graph graph, PathRegexes p) {
    return findMatchingNodes(graph, p.getSrcRegex(), p.getNotSrcRegex());
  }

  public static List<String> findMatchingNodes(Graph graph, Pattern p1, Pattern p2) {
    List<String> acc = new ArrayList<>();
    for (String router : graph.getRouters()) {
      Matcher m1 = p1.matcher(router);
      Matcher m2 = p2.matcher(router);
      if (m1.matches() && !m2.matches()) {
        acc.add(router);
      }
    }
    return acc;
  }

  public static List<GraphEdge> findMatchingEdges(Graph graph, PathRegexes p) {
    return findMatchingEdges(
        graph, p.getDstRegex(), p.getNotDstRegex(), p.getIfaceRegex(), p.getNotIfaceRegex());
  }

  public static List<GraphEdge> findMatchingEdges(
      Graph graph, Pattern p1, Pattern p2, Pattern p3, Pattern p4) {
    List<GraphEdge> acc = new ArrayList<>();
    for (Entry<String, List<GraphEdge>> entry : graph.getEdgeMap().entrySet()) {
      String router = entry.getKey();
      List<GraphEdge> edges = entry.getValue();
      Matcher m1 = p1.matcher(router);
      Matcher m2 = p2.matcher(router);
      if (m1.matches() && !m2.matches()) {
        for (GraphEdge edge : edges) {
          if (!edge.isAbstract()) {
            Interface i = edge.getStart();
            String ifaceName = i.getName();
            Matcher m3 = p3.matcher(ifaceName);
            Matcher m4 = p4.matcher(ifaceName);
            if (m3.matches() && !m4.matches()) {
              acc.add(edge);
            }
          }
        }
      }
    }
    return acc;
  }

  public static Set<GraphEdge> findMatchingEdges(Graph graph, Pattern p1, Pattern p2) {
    Set<GraphEdge> acc = new HashSet<>();
    for (List<GraphEdge> edges : graph.getEdgeMap().values()) {
      for (GraphEdge ge : edges) {
        if (!ge.isAbstract() && ge.getPeer() != null) {
          Matcher m1 = p1.matcher(ge.getRouter());
          Matcher m2 = p2.matcher(ge.getPeer());
          if (m1.matches() && m2.matches()) {
            acc.add(ge);
          }
        }
      }
    }
    return acc;
  }
}
