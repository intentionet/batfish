package org.batfish.question.initialization;

import static org.batfish.question.initialization.ParseWarningAnswerer.COL_COMMENT;
import static org.batfish.question.initialization.ParseWarningAnswerer.COL_FILENAME;
import static org.batfish.question.initialization.ParseWarningAnswerer.COL_LINE;
import static org.batfish.question.initialization.ParseWarningAnswerer.COL_PARSER_CONTEXT;
import static org.batfish.question.initialization.ParseWarningAnswerer.COL_TEXT;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSortedMap;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Rows;
import org.batfish.datamodel.table.TableAnswerElement;
import org.junit.Test;

/** Tests of {@link ParseWarningAnswerer}. */
public class ParseWarningAnswererTest {

  @Test
  public void testGetRowWithoutComment() {
    ParseWarning input = new ParseWarning(3, "text", "[configuration]", null);
    Row expected =
        Row.of(
            COL_FILENAME,
            "nohost",
            COL_LINE,
            input.getLine(),
            COL_TEXT,
            input.getText(),
            COL_PARSER_CONTEXT,
            input.getParserContext(),
            COL_COMMENT,
            "(not provided)");

    Row row = ParseWarningAnswerer.getRow("nohost", input);
    assertThat(row, equalTo(expected));
  }

  @Test
  public void testGetRowWithComment() {
    ParseWarning input = new ParseWarning(3, "text", "[configuration]", "comment");
    Row expected =
        Row.of(
            COL_FILENAME,
            "nohost",
            COL_LINE,
            input.getLine(),
            COL_TEXT,
            input.getText(),
            COL_PARSER_CONTEXT,
            input.getParserContext(),
            COL_COMMENT,
            input.getComment());

    Row row = ParseWarningAnswerer.getRow("nohost", input);
    assertThat(row, equalTo(expected));
  }

  @Test
  public void testAnswererFlow() {
    ParseWarningAnswerer answerer =
        new ParseWarningAnswerer(new ParseWarningQuestion(), new TestBatfish());
    TableAnswerElement answer = answerer.answer();
    assertThat(
        answer.getRows(),
        equalTo(
            new Rows()
                .add(
                    Row.of(
                        COL_FILENAME,
                        "f",
                        COL_LINE,
                        3,
                        COL_TEXT,
                        "text",
                        COL_PARSER_CONTEXT,
                        "ctx",
                        COL_COMMENT,
                        "comment"))));
  }

  private static class TestBatfish extends IBatfishTestAdapter {
    @Override
    public ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement() {
      ParseVendorConfigurationAnswerElement pvcae = new ParseVendorConfigurationAnswerElement();
      Warnings warnings = new Warnings();
      warnings.getParseWarnings().add(new ParseWarning(3, "text", "ctx", "comment"));
      pvcae.setWarnings(ImmutableSortedMap.of("nowarnings", new Warnings(), "f", warnings));
      return pvcae;
    }
  }
}
