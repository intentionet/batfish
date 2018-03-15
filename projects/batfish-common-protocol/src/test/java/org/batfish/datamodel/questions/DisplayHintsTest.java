package org.batfish.datamodel.questions;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Map;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.questions.DisplayHints.Extraction;
import org.junit.Test;

public class DisplayHintsTest {

  @Test
  public void displayHintsParsingTest() throws IOException {
    String text = CommonUtil.readResource("org/batfish/datamodel/questions/displayHintsTest.json");
    DisplayHints displayHints = BatfishObjectMapper.mapper().readValue(text, DisplayHints.class);

    // here, we only test for ExtractionHint level concepts
    // tests that sit with jsonpath question validate if prefix/suffix filters are parsed correctly

    assertThat(
        displayHints.getTextDesc().equals("${mynode} has nothing on ${myinterface}"),
        equalTo(true));

    Map<String, Extraction> extractions = displayHints.getExtractions();
    assertThat(extractions.size(), equalTo(3));

    Extraction hint0 = extractions.get("node1");
    assertThat(hint0.getSchemaAsObject().isList(), equalTo(false));
    assertThat(
        hint0.getSchemaAsObject().getBaseType().getCanonicalName(), equalTo("java.lang.String"));
    assertThat(hint0.getMethod().containsKey("use"), equalTo(true));

    Extraction hint1 = extractions.get("interfaces1");
    assertThat(hint1.getSchemaAsObject().isList(), equalTo(true));

    Extraction hint2 = extractions.get("nodes1");
    assertThat(hint2.getSchemaAsObject().isIntOrIntList(), equalTo(true));
  }
}
