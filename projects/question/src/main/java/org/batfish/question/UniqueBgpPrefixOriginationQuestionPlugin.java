package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class UniqueBgpPrefixOriginationQuestionPlugin extends QuestionPlugin {

  public static class UniqueBgpPrefixOriginationAnswerElement implements AnswerElement {

    private SortedMap<String, SortedMap<String, PrefixSpace>> _intersections;

    private SortedMap<String, PrefixSpace> _prefixSpaces;

    public UniqueBgpPrefixOriginationAnswerElement() {
      _intersections = new TreeMap<>();
      _prefixSpaces = new TreeMap<>();
    }

    public void addIntersection(String node1, String node2, PrefixSpace intersection) {
      SortedMap<String, PrefixSpace> intersections =
          _intersections.computeIfAbsent(node1, k -> new TreeMap<>());
      intersections.put(node2, intersection);
    }

    public SortedMap<String, SortedMap<String, PrefixSpace>> getIntersections() {
      return _intersections;
    }

    public SortedMap<String, PrefixSpace> getPrefixSpaces() {
      return _prefixSpaces;
    }

    public void setIntersections(SortedMap<String, SortedMap<String, PrefixSpace>> intersections) {
      _intersections = intersections;
    }

    public void setPrefixSpaces(SortedMap<String, PrefixSpace> prefixSpaces) {
      _prefixSpaces = prefixSpaces;
    }
  }

  public static class UniqueBgpPrefixOriginationAnswerer extends Answerer {

    public UniqueBgpPrefixOriginationAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {

      UniqueBgpPrefixOriginationQuestion question = (UniqueBgpPrefixOriginationQuestion) _question;
      Pattern nodeRegex;
      try {
        nodeRegex = Pattern.compile(question.getNodeRegex());
      } catch (PatternSyntaxException e) {
        throw new BatfishException(
            "Supplied regex for nodes is not a valid java regex: \""
                + question.getNodeRegex()
                + "\"",
            e);
      }

      UniqueBgpPrefixOriginationAnswerElement answerElement =
          new UniqueBgpPrefixOriginationAnswerElement();
      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      _batfish.initBgpOriginationSpaceExplicit(configurations);
      for (Entry<String, Configuration> e : configurations.entrySet()) {
        String node1 = e.getKey();
        if (!nodeRegex.matcher(node1).matches()) {
          continue;
        }
        Configuration c1 = e.getValue();
        PrefixSpace space1 = new PrefixSpace();
        boolean empty1 = true;
        for (Vrf v1 : c1.getVrfs().values()) {
          BgpProcess proc1 = v1.getBgpProcess();
          if (proc1 != null) {
            empty1 = false;
            space1.addSpace(proc1.getOriginationSpace());
          }
        }
        if (!empty1) {
          answerElement.getPrefixSpaces().put(node1, space1);
          for (Entry<String, Configuration> e2 : configurations.entrySet()) {
            String node2 = e2.getKey();
            if (!nodeRegex.matcher(node2).matches() || node1.equals(node2)) {
              continue;
            }
            Configuration c2 = e2.getValue();
            PrefixSpace space2 = new PrefixSpace();
            boolean empty2 = true;
            for (Vrf v2 : c2.getVrfs().values()) {
              BgpProcess proc2 = v2.getBgpProcess();
              if (proc2 != null) {
                empty2 = false;
                space2.addSpace(proc2.getOriginationSpace());
              }
            }
            if (!empty2 && space1.overlaps(space2)) {
              PrefixSpace intersection = space1.intersection(space2);
              answerElement.addIntersection(node1, node2, intersection);
            }
          }
        }
      }
      return answerElement;
    }
  }

  // <question_page_comment>

  /**
   * Lists cases where the same prefix is originated by multiple BGP speakers.
   *
   * <p>Except in cases of anycast or a multihoming arrangement, a prefix be originated by only one
   * BGP speakers. This question produces the list of prefixes for which this condition does not
   * hold.
   *
   * @type UniqueBgpPrefixOrigination multifile
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes).
   * @example bf_answer("UniqueBgpPrefixOrigination", nodeRegex='as2.*') Answers the question only
   *     for nodes whose names start with 'as2'.
   */
  public static class UniqueBgpPrefixOriginationQuestion extends Question {

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private String _nodeRegex;

    public UniqueBgpPrefixOriginationQuestion() {
      _nodeRegex = ".*";
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "uniquebgpprefixorigination";
    }

    @JsonProperty(PROP_NODE_REGEX)
    public String getNodeRegex() {
      return _nodeRegex;
    }

    @Override
    public boolean getTraffic() {
      return false;
    }

    @JsonProperty(PROP_NODE_REGEX)
    public void setNodeRegex(String nodeRegex) {
      _nodeRegex = nodeRegex;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new UniqueBgpPrefixOriginationAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new UniqueBgpPrefixOriginationQuestion();
  }
}
