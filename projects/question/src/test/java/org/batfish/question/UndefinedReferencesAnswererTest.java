package org.batfish.question;

import static org.batfish.question.UndefinedReferencesQuestionPlugin.UndefinedReferencesAnswerer.COL_CONTEXT;
import static org.batfish.question.UndefinedReferencesQuestionPlugin.UndefinedReferencesAnswerer.COL_FILENAME;
import static org.batfish.question.UndefinedReferencesQuestionPlugin.UndefinedReferencesAnswerer.COL_LINES;
import static org.batfish.question.UndefinedReferencesQuestionPlugin.UndefinedReferencesAnswerer.COL_REF_NAME;
import static org.batfish.question.UndefinedReferencesQuestionPlugin.UndefinedReferencesAnswerer.COL_STRUCT_TYPE;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.collections.FileLines;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Rows;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.question.UndefinedReferencesQuestionPlugin.UndefinedReferencesAnswerer;
import org.batfish.question.UndefinedReferencesQuestionPlugin.UndefinedReferencesQuestion;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.SpecifierContext;
import org.junit.Test;

public class UndefinedReferencesAnswererTest {

  private static final SortedMap<
          String, SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>>>
      BASIC_UNDEFINED_REFS_MAP =
          ImmutableSortedMap.of(
              "f",
              ImmutableSortedMap.of(
                  "t",
                  ImmutableSortedMap.of(
                      "n", ImmutableSortedMap.of("c", ImmutableSortedSet.of(1)))));
  private static final Row BASIC_ROW =
      Row.of(
          COL_FILENAME,
          "f",
          COL_STRUCT_TYPE,
          "t",
          COL_REF_NAME,
          "n",
          COL_CONTEXT,
          "c",
          COL_LINES,
          new FileLines("f", ImmutableSortedSet.of(1)));

  @Test
  public void testProcessOneEntry() {
    List<Row> expected = ImmutableList.of(BASIC_ROW);

    List<Row> rows =
        UndefinedReferencesAnswerer.processEntryToRows(
            BASIC_UNDEFINED_REFS_MAP.entrySet().iterator().next());
    assertThat(rows, equalTo(expected));
  }

  @Test
  public void testProcessMultipleEntries() {
    Map<String, SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>>>
        refsMap =
            ImmutableMap.of(
                "f",
                ImmutableSortedMap.of(
                    "t",
                    ImmutableSortedMap.of(
                        "n", ImmutableSortedMap.of("c", ImmutableSortedSet.of(1))),
                    "t2",
                    ImmutableSortedMap.of(
                        "n2", ImmutableSortedMap.of("c2", ImmutableSortedSet.of(2, 3)))));

    List<Row> expected =
        ImmutableList.of(
            BASIC_ROW,
            Row.of(
                COL_FILENAME,
                "f",
                COL_STRUCT_TYPE,
                "t2",
                COL_REF_NAME,
                "n2",
                COL_CONTEXT,
                "c2",
                COL_LINES,
                new FileLines("f", ImmutableSortedSet.of(2, 3))));

    List<Row> rows =
        UndefinedReferencesAnswerer.processEntryToRows(refsMap.entrySet().iterator().next());
    assertThat(rows, equalTo(expected));
  }

  @Test
  public void testAnswererFlow() {
    UndefinedReferencesAnswerer answerer =
        new UndefinedReferencesAnswerer(new UndefinedReferencesQuestion(), new TestBatfish());
    TableAnswerElement answer = answerer.answer();
    assertThat(answer.getRows(), equalTo(new Rows().add(BASIC_ROW)));
  }

  private static class TestBatfish extends IBatfishTestAdapter {
    @Override
    public ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElementOrReparse() {
      ConvertConfigurationAnswerElement ccae = new ConvertConfigurationAnswerElement();
      ccae.setUndefinedReferences(BASIC_UNDEFINED_REFS_MAP);
      return ccae;
    }

    @Override
    public ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement() {
      ParseVendorConfigurationAnswerElement pvcae = new ParseVendorConfigurationAnswerElement();
      pvcae.setFileMap(ImmutableSortedMap.of("h", "f", "h2", "f2"));
      return pvcae;
    }

    @Override
    public SpecifierContext specifierContext() {
      Configuration c1 = new Configuration("h", ConfigurationFormat.CISCO_IOS);
      Configuration c2 = new Configuration("h2", ConfigurationFormat.CISCO_IOS);
      return MockSpecifierContext.builder()
          .setConfigs(ImmutableSortedMap.of("h", c1, "h2", c2))
          .build();
    }
  }
}
