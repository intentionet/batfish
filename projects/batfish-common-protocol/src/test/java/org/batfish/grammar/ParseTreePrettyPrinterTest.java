package org.batfish.grammar;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ParseTreePrettyPrinterTest {

  @Test
  public void testParseTreePrettyPrintWithCharacterLimit() throws IOException {
    List<String> strings = new ArrayList<>();
    strings.add("1234");

    String string = ParseTreePrettyPrinter.printWithCharacterLimit(strings, 0);
    assertThat(string, equalTo("1234"));

    string = ParseTreePrettyPrinter.printWithCharacterLimit(strings, 3);
    assertThat(string, equalTo("1234"));

    string = ParseTreePrettyPrinter.printWithCharacterLimit(strings, 4);
    assertThat(string, equalTo("1234"));

    strings.add("5678");
    string = ParseTreePrettyPrinter.printWithCharacterLimit(strings, 0);
    assertThat(string, equalTo("1234\n5678"));

    string = ParseTreePrettyPrinter.printWithCharacterLimit(strings, 1);
    assertThat(string, equalTo("1234\nand 1 more line(s)"));

    string = ParseTreePrettyPrinter.printWithCharacterLimit(strings, 5);
    assertThat(string, equalTo("1234\nand 1 more line(s)"));

    string = ParseTreePrettyPrinter.printWithCharacterLimit(strings, 6);
    assertThat(string, equalTo("1234\n5678"));
  }
}
