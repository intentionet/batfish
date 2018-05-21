package org.batfish.question.aclreachability2;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.answers.AclLinesAnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NamedStructureEquivalenceSets;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.question.CompareSameNameQuestionPlugin.CompareSameNameAnswerElement;
import org.batfish.question.CompareSameNameQuestionPlugin.CompareSameNameAnswerer;
import org.batfish.question.CompareSameNameQuestionPlugin.CompareSameNameQuestion;

public class AclReachability2Answerer extends Answerer {

  public AclReachability2Answerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AclLinesAnswerElement answer() {
    AclReachability2Question question = (AclReachability2Question) _question;
    // get comparesamename results for acls
    CompareSameNameQuestion csnQuestion = new CompareSameNameQuestion();
    csnQuestion.setCompareGenerated(true);
    csnQuestion.setNodeRegex(question.getNodeRegex());
    csnQuestion.setNamedStructTypes(
        new TreeSet<>(Collections.singleton(IpAccessList.class.getSimpleName())));
    csnQuestion.setSingletons(true);
    CompareSameNameAnswerer csnAnswerer = new CompareSameNameAnswerer(csnQuestion, _batfish);
    CompareSameNameAnswerElement csnAnswer = csnAnswerer.answer();
    NamedStructureEquivalenceSets<?> aclEqSets =
        csnAnswer.getEquivalenceSets().get(IpAccessList.class.getSimpleName());

    AclLinesAnswerElement answer = new AclLinesAnswerElement(createMetadata(question));
    _batfish.answerAclReachability(question.getAclNameRegex(), aclEqSets, answer);
    answer.postProcessAnswer(question, answer.getInitialRows().getData());
    return answer;
  }

  /**
   * Creates a {@link TableMetadata} object from the question.
   *
   * @param question The question
   * @return The resulting {@link TableMetadata} object
   */
  static TableMetadata createMetadata(AclReachability2Question question) {
    List<ColumnMetadata> columnMetadata =
        new ImmutableList.Builder<ColumnMetadata>()
            .add(
                new ColumnMetadata(
                    AclLinesAnswerElement.COL_NODES,
                    Schema.list(Schema.NODE),
                    "Nodes",
                    true,
                    false))
            .add(
                new ColumnMetadata(
                    AclLinesAnswerElement.COL_ACL, Schema.STRING, "ACL name", true, false))
            .add(
                new ColumnMetadata(
                    AclLinesAnswerElement.COL_LINES,
                    Schema.list(Schema.STRING),
                    "ACL lines",
                    false,
                    false))
            .add(
                new ColumnMetadata(
                    AclLinesAnswerElement.COL_BLOCKED_LINE_NUM,
                    Schema.INTEGER,
                    "Blocked line number",
                    true,
                    false))
            .add(
                new ColumnMetadata(
                    AclLinesAnswerElement.COL_BLOCKING_LINE_NUMS,
                    Schema.list(Schema.INTEGER),
                    "Blocking line numbers",
                    false,
                    true))
            .add(
                new ColumnMetadata(
                    AclLinesAnswerElement.COL_DIFF_ACTION,
                    Schema.BOOLEAN,
                    "Different action",
                    false,
                    true))
            .add(
                new ColumnMetadata(
                    AclLinesAnswerElement.COL_MESSAGE, Schema.STRING, "Message", false, false))
            .build();

    DisplayHints dhints = question.getDisplayHints();
    if (dhints == null) {
      dhints = new DisplayHints();
      dhints.setTextDesc(String.format("${%s}", AclLinesAnswerElement.COL_MESSAGE));
    }
    return new TableMetadata(columnMetadata, dhints);
  }
}
