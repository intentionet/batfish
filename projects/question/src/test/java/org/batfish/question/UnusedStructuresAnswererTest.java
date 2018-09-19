package org.batfish.question;

import static org.batfish.question.UnusedStructuresQuestionPlugin.UnusedStructuresAnswerer.COL_SOURCE_LINES;
import static org.batfish.question.UnusedStructuresQuestionPlugin.UnusedStructuresAnswerer.COL_STRUCTURE_NAME;
import static org.batfish.question.UnusedStructuresQuestionPlugin.UnusedStructuresAnswerer.COL_STRUCTURE_TYPE;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.collections.FileLines;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Rows;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.question.UnusedStructuresQuestionPlugin.UnusedStructuresAnswerer;
import org.batfish.question.UnusedStructuresQuestionPlugin.UnusedStructuresQuestion;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.SpecifierContext;
import org.junit.Test;

/** Tests of {@link UnusedStructuresAnswerer}. */
public class UnusedStructuresAnswererTest {

  // Entry is: filename -> struct type -> struct name -> defined structure info
  private static final SortedMap<String, SortedMap<String, SortedMap<String, DefinedStructureInfo>>>
      BASIC_DEFINED_STRUCTS_MAP =
          ImmutableSortedMap.of(
              "f",
              ImmutableSortedMap.of(
                  "t",
                  ImmutableSortedMap.of(
                      "n",
                      new DefinedStructureInfo(ImmutableSortedSet.of(1), 0),
                      "n2",
                      new DefinedStructureInfo(ImmutableSortedSet.of(2), 1))));
  private static final Row BASIC_ROW =
      Row.of(
          COL_STRUCTURE_TYPE,
          "t",
          COL_STRUCTURE_NAME,
          "n",
          COL_SOURCE_LINES,
          new FileLines("f", ImmutableSortedSet.of(1)));

  @Test
  public void testProcessOneEntry() {
    List<Row> expected = ImmutableList.of(BASIC_ROW);

    List<Row> rows =
        UnusedStructuresAnswerer.processEntryToRows(
            BASIC_DEFINED_STRUCTS_MAP.entrySet().iterator().next());
    assertThat(rows, equalTo(expected));
  }

  @Test
  public void testProcessMultipleEntries() {
    Map<String, SortedMap<String, SortedMap<String, DefinedStructureInfo>>> refsMap =
        ImmutableMap.of(
            "f",
            ImmutableSortedMap.of(
                "t",
                ImmutableSortedMap.of(
                    "n",
                    new DefinedStructureInfo(ImmutableSortedSet.of(1), 0),
                    "n2",
                    new DefinedStructureInfo(ImmutableSortedSet.of(2, 3), 0))));

    List<Row> expected =
        ImmutableList.of(
            BASIC_ROW,
            Row.of(
                COL_STRUCTURE_TYPE,
                "t",
                COL_STRUCTURE_NAME,
                "n2",
                COL_SOURCE_LINES,
                new FileLines("f", ImmutableSortedSet.of(2, 3))));

    List<Row> rows =
        UnusedStructuresAnswerer.processEntryToRows(refsMap.entrySet().iterator().next());
    assertThat(rows, equalTo(expected));
  }

  @Test
  public void testAnswererFlow() {
    UnusedStructuresAnswerer answerer =
        new UnusedStructuresAnswerer(new UnusedStructuresQuestion(), new TestBatfish());
    TableAnswerElement answer = answerer.answer();
    assertThat(answer.getRows(), equalTo(new Rows().add(BASIC_ROW)));
  }

  private static class TestBatfish extends IBatfishTestAdapter {
    @Override
    public ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElementOrReparse() {
      ConvertConfigurationAnswerElement ccae = new ConvertConfigurationAnswerElement();
      ccae.setDefinedStructures(BASIC_DEFINED_STRUCTS_MAP);
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
