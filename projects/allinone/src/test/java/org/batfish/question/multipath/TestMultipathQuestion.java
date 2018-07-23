package org.batfish.question.multipath;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.datamodel.matchers.TableAnswerElementMatchers.hasRows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import org.batfish.bddreachability.TestNetwork;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestMultipathQuestion {
  @Rule public TemporaryFolder temp = new TemporaryFolder();

  private TestNetwork _testNetwork;

  private Batfish _batfish;

  @Before
  public void setup() throws IOException {
    _testNetwork = new TestNetwork();
    _batfish = BatfishTestUtils.getBatfish(_testNetwork._configs, temp);
    _batfish.computeDataPlane(false);
  }

  @Test
  public void testMultipath() {
    MultipathConsistencyQuestion question = new MultipathConsistencyQuestion();
    MultipathConsistencyAnswerer answerer = new MultipathConsistencyAnswerer(question, _batfish);
    TableAnswerElement ae = (TableAnswerElement) answerer.answer();
    assertThat(
        ae,
        hasRows(
            contains(
                allOf(
                    hasColumn(
                        "node",
                        equalTo(new Node(_testNetwork._srcNode.getHostname())),
                        Schema.NODE),
                    hasColumn(
                        "dstIp", equalTo(TestNetwork.DST_PREFIX_2.getStartIp()), Schema.IP)))));
  }
}
