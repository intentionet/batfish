package org.batfish.question.bgpsessionstatus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.table.Row;
import org.batfish.question.bgpsessionstatus.BgpSessionInfo.SessionStatus;
import org.batfish.question.bgpsessionstatus.BgpSessionInfo.SessionType;
import org.junit.Test;

public class BgpSessionStatusAnswerElementTest {

  @Test
  public void toAndFromRow() throws JsonProcessingException {
    BgpSessionInfo session =
        new BgpSessionInfo(
            SessionStatus.UNKNOWN_REMOTE,
            -1,
            "nodeName",
            new Ip("2.2.2.2"),
            null,
            "remoteNode",
            new Prefix(new Ip("1.1.1.1"), 32),
            SessionType.EBGP_SINGLEHOP,
            "vrfName");

    Row row = BgpSessionStatusAnswerer.toRow(session);
    BgpSessionInfo session2 = BgpSessionStatusAnswerer.fromRow(row);

    // session2 and session should be identical
    assertThat(session, equalTo(session2));
  }
}
