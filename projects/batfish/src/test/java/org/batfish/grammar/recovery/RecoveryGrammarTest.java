package org.batfish.grammar.recovery;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.recovery.RecoveryParser.RecoveryContext;
import org.junit.Test;

public class RecoveryGrammarTest {

  @Test
  public void testParsingRecovery() throws IOException {
    String recoveryText = CommonUtil.readResource("org/batfish/grammar/recovery/recovery_text");
    GrammarSettings settings = new Settings();
    RecoveryCombinedParser cp = new RecoveryCombinedParser(recoveryText, settings);
    RecoveryContext ctx = cp.parse();
    RecoveryExtractor extractor = new RecoveryExtractor();
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(extractor, ctx);
    assertThat(extractor.getNumBlockStatements(), equalTo(2));
    assertThat(extractor.getNumErrorNodes(), equalTo(15));
    assertThat(extractor.getNumInnerStatements(), equalTo(1));
    assertThat(extractor.getNumSimpleStatements(), equalTo(5));
    assertThat(
        extractor.getNumStatements(),
        equalTo(extractor.getNumBlockStatements() + extractor.getNumSimpleStatements()));
    assertThat(extractor.getNumTailWords(), equalTo(5));
  }
}
