package org.batfish.allinone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.question.CompareSameNameQuestionPlugin;
import org.batfish.question.CompareSameNameQuestionPlugin.CompareSameNameAnswerElement;
import org.batfish.question.CompareSameNameQuestionPlugin.CompareSameNameAnswerer;
import org.batfish.question.CompareSameNameQuestionPlugin.CompareSameNameQuestion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CompareSameNameTest {

  private Configuration.Builder _cb;

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private NetworkFactory _nf;

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
  }

  @Test
  public void testAssumeAllUnique() throws IOException {
    Configuration c1 = _cb.build();
    Configuration c2 = _cb.build();
    String aclName = "acl";
    IpAccessList.Builder aclb = _nf.aclBuilder().setName(aclName);
    aclb.setOwner(c1).build();
    aclb.setOwner(c2).build();

    Batfish batfish =
        BatfishTestUtils.getBatfish(
            ImmutableSortedMap.of(c1.getHostname(), c1, c2.getHostname(), c2), _folder);

    CompareSameNameQuestion csnQuestion =
        new CompareSameNameQuestion(
            null,
            null,
            null,
            ImmutableSortedSet.of(IpAccessList.class.getSimpleName()),
            null,
            true);
    CompareSameNameAnswerer csnAnswerer = new CompareSameNameAnswerer(csnQuestion, batfish);
    CompareSameNameAnswerElement answerNoAssumption = csnAnswerer.answer();
    batfish
        .getSettings()
        .setDebugFlags(
            ImmutableList.of(CompareSameNameQuestionPlugin.DEBUG_FLAG_ASSUME_ALL_UNIQUE));
    CompareSameNameAnswerElement answerAssumeAllUnique = csnAnswerer.answer();

    assertThat(
        answerNoAssumption
            .getEquivalenceSets()
            .get(IpAccessList.class.getSimpleName())
            .getSameNamedStructures()
            .get(aclName),
        hasSize(1));
    assertThat(
        answerAssumeAllUnique
            .getEquivalenceSets()
            .get(IpAccessList.class.getSimpleName())
            .getSameNamedStructures()
            .get(aclName),
        hasSize(2));
  }
}
