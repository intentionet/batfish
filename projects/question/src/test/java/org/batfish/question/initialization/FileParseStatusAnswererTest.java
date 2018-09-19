package org.batfish.question.initialization;

import static org.batfish.question.initialization.FileParseStatusAnswerer.COL_FILENAME;
import static org.batfish.question.initialization.FileParseStatusAnswerer.COL_NODES;
import static org.batfish.question.initialization.FileParseStatusAnswerer.COL_PARSE_STATUS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Rows;
import org.batfish.datamodel.table.TableAnswerElement;
import org.junit.Test;

/** Tests of {@link FileParseStatusAnswerer}. */
public class FileParseStatusAnswererTest {

  @Test
  public void testGetRowWithoutHost() {
    Row expected =
        Row.of(COL_FILENAME, "nohost", COL_PARSE_STATUS, "FAILED", COL_NODES, ImmutableList.of());

    Row row = FileParseStatusAnswerer.getRow("nohost", ParseStatus.FAILED, null);
    assertThat(row, equalTo(expected));
  }

  @Test
  public void testGetRowWithHost() {
    Row expected =
        Row.of(
            COL_FILENAME,
            "host",
            COL_PARSE_STATUS,
            "EMPTY",
            COL_NODES,
            ImmutableList.of(new Node("h1")));

    Row row = FileParseStatusAnswerer.getRow("host", ParseStatus.EMPTY, ImmutableList.of("h1"));
    assertThat(row, equalTo(expected));
  }

  @Test
  public void testAnswererFlow() {
    FileParseStatusAnswerer answerer =
        new FileParseStatusAnswerer(new FileParseStatusQuestion(), new TestBatfish());
    TableAnswerElement answer = answerer.answer();
    assertThat(
        answer.getRows(),
        equalTo(
            new Rows()
                .add(
                    Row.of(
                        COL_FILENAME,
                        "f",
                        COL_PARSE_STATUS,
                        ParseStatus.PASSED,
                        COL_NODES,
                        ImmutableList.of(new Node("h"))))));
  }

  private static class TestBatfish extends IBatfishTestAdapter {
    @Override
    public ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement() {
      ParseVendorConfigurationAnswerElement pvcae = new ParseVendorConfigurationAnswerElement();
      pvcae.setFileMap(ImmutableSortedMap.of("h", "f"));
      pvcae.setParseStatus(ImmutableSortedMap.of("f", ParseStatus.PASSED));
      return pvcae;
    }
  }
}
