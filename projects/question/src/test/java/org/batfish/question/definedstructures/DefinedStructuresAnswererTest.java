package org.batfish.question.definedstructures;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.datamodel.matchers.TableAnswerElementMatchers.hasRows;
import static org.batfish.question.definedstructures.DefinedStructuresAnswerer.COL_SOURCE_LINES;
import static org.batfish.question.definedstructures.DefinedStructuresAnswerer.COL_STRUCTURE_NAME;
import static org.batfish.question.definedstructures.DefinedStructuresAnswerer.COL_STRUCT_TYPE;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import java.util.SortedMap;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.FileLines;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.SpecifierContext;
import org.junit.Test;

public class DefinedStructuresAnswererTest {
  // Entry is: filename -> struct type -> struct name -> defined structure info
  private static final SortedMap<String, SortedMap<String, SortedMap<String, DefinedStructureInfo>>>
      BASIC_DEFINED_STRUCTS_MAP =
          ImmutableSortedMap.of(
              "file1",
              ImmutableSortedMap.of(
                  "type1",
                  ImmutableSortedMap.of(
                      "name1",
                      new DefinedStructureInfo(ImmutableSortedSet.of(1), 1),
                      "name2",
                      new DefinedStructureInfo(ImmutableSortedSet.of(2), 1))),
              "file2",
              ImmutableSortedMap.of(
                  "type1",
                  ImmutableSortedMap.of(
                      "name2",
                      new DefinedStructureInfo(ImmutableSortedSet.of(1), 1),
                      "name3",
                      new DefinedStructureInfo(ImmutableSortedSet.of(2), 1))));

  // Hostname -> Files that make up that host.
  private static final Multimap<String, String> FILE_MAP =
      ImmutableMultimap.<String, String>builder()
          .putAll("a", "file1", "file2")
          .putAll("b", "file1")
          .putAll("c", "file2")
          .build();

  /** Tests that when a file filter is specified, correct results are returned. */
  @Test
  public void testFilterFiles() {
    DefinedStructuresQuestion fileFilter = new DefinedStructuresQuestion("file1", null, ".*", ".*");
    TestBatfish tb = new TestBatfish();
    TableAnswerElement answer =
        new DefinedStructuresAnswerer(fileFilter, tb).answer(tb.getSnapshot());
    assertThat(
        answer,
        hasRows(
            containsInAnyOrder(
                allOf(
                    hasColumn(COL_STRUCTURE_NAME, "name1", Schema.STRING),
                    hasColumn(
                        COL_SOURCE_LINES,
                        new FileLines("file1", ImmutableSortedSet.of(1)),
                        Schema.FILE_LINES),
                    hasColumn(COL_STRUCT_TYPE, "type1", Schema.STRING)),
                allOf(
                    hasColumn(COL_STRUCTURE_NAME, "name2", Schema.STRING),
                    hasColumn(
                        COL_SOURCE_LINES,
                        new FileLines("file1", ImmutableSortedSet.of(2)),
                        Schema.FILE_LINES),
                    hasColumn(COL_STRUCT_TYPE, "type1", Schema.STRING)))));
  }

  /** Tests that when a file filter is specified along with nodes, correct results are returned. */
  @Test
  public void testFilterFilesAndNodes() {
    DefinedStructuresQuestion fileAndNode = new DefinedStructuresQuestion("file1", "a", ".*", ".*");
    TestBatfish tb = new TestBatfish();
    assertThat(
        new DefinedStructuresAnswerer(fileAndNode, tb).answer(tb.getSnapshot()),
        hasRows(
            containsInAnyOrder(
                allOf(
                    hasColumn(COL_STRUCTURE_NAME, "name1", Schema.STRING),
                    hasColumn(
                        COL_SOURCE_LINES,
                        new FileLines("file1", ImmutableSortedSet.of(1)),
                        Schema.FILE_LINES),
                    hasColumn(COL_STRUCT_TYPE, "type1", Schema.STRING)),
                allOf(
                    hasColumn(COL_STRUCTURE_NAME, "name2", Schema.STRING),
                    hasColumn(
                        COL_SOURCE_LINES,
                        new FileLines("file1", ImmutableSortedSet.of(2)),
                        Schema.FILE_LINES),
                    hasColumn(COL_STRUCT_TYPE, "type1", Schema.STRING)))));

    DefinedStructuresQuestion fileAndNodeMismatch =
        new DefinedStructuresQuestion("file1", "c", ".*", ".*");
    assertThat(
        new DefinedStructuresAnswerer(fileAndNodeMismatch, tb).answer(tb.getSnapshot()),
        hasRows(emptyIterable()));
  }

  private static class TestBatfish extends IBatfishTestAdapter {
    @Override
    public ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElementOrReparse(
        NetworkSnapshot snapshot) {
      ConvertConfigurationAnswerElement ccae = new ConvertConfigurationAnswerElement();
      ccae.setDefinedStructures(BASIC_DEFINED_STRUCTS_MAP);
      return ccae;
    }

    @Override
    public ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement(
        NetworkSnapshot snapshot) {
      ParseVendorConfigurationAnswerElement pvcae = new ParseVendorConfigurationAnswerElement();
      pvcae.setFileMap(FILE_MAP);
      return pvcae;
    }

    @Override
    public SpecifierContext specifierContext(NetworkSnapshot snapshot) {
      assertThat(snapshot, equalTo(getSnapshot()));
      Configuration c1 = new Configuration("a", ConfigurationFormat.CISCO_IOS);
      Configuration c2 = new Configuration("b", ConfigurationFormat.CISCO_IOS);
      Configuration c3 = new Configuration("c", ConfigurationFormat.CISCO_IOS);
      Configuration c4 = new Configuration("d", ConfigurationFormat.CISCO_IOS);
      return MockSpecifierContext.builder()
          .setConfigs(
              ImmutableSortedMap.of(
                  c1.getHostname(),
                  c1,
                  c2.getHostname(),
                  c2,
                  c3.getHostname(),
                  c3,
                  c4.getHostname(),
                  c4))
          .build();
    }
  }
}
