package org.batfish.main;

import static com.google.common.io.MoreFiles.createParentDirectories;
import static org.batfish.main.PreprocessJuniper.main;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BfConsts;
import org.batfish.common.util.CommonUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Test of {@link PreprocessJuniper}. */
@ParametersAreNonnullByDefault
public final class PreprocessJuniperTest {

  private static final String JUNIPER_TESTCONFIGS_PREFIX = "org/batfish/main/testconfigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  /**
   * Equivalent to calling {@link PreprocessJuniperTest#assertValidPair(String,String,String)} with
   * {@code prefix} set to {@code JUNIPER_TESTCONFIGS_PREFIX}.
   */
  private void assertValidPair(String before, String after) throws IOException {
    assertValidPair(JUNIPER_TESTCONFIGS_PREFIX, before, after);
  }

  /**
   * Assert that the result of preprocessing the text of the resource {@code before} under {@code
   * prefix} is equal to the text of the resource {@code after} under {@code prefix}.
   */
  private void assertValidPair(String prefix, String before, String after) throws IOException {
    Path root = _folder.getRoot().toPath();
    Path inputDir = root.resolve("input");
    Path outputDir = root.resolve("output");
    Path inputFile = inputDir.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR).resolve("conf");
    Path outputFile = outputDir.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR).resolve("conf");
    createParentDirectories(inputFile);
    Files.write(
        inputFile,
        CommonUtil.readResource(String.format("%s%s", prefix, before))
            .getBytes(StandardCharsets.UTF_8));
    main(new String[] {inputDir.toString(), outputDir.toString()});

    assertThat(
        CommonUtil.readFile(outputFile).trim(),
        equalTo(CommonUtil.readResource(String.format("%s%s", prefix, after)).trim()));
  }

  @Test
  public void testFlat() throws IOException {
    assertValidPair("preprocess-flat-before", "preprocess-flat-after");
  }

  @Test
  public void testHierarchical() throws IOException {
    assertValidPair("preprocess-hierarchical-before", "preprocess-hierarchical-after");
  }

  @Test
  public void testMainBadArgs() throws IOException {
    _thrown.expect(IllegalArgumentException.class);
    main(new String[] {});
  }

  @Test
  public void testNonJuniper() throws IOException {
    assertValidPair(JUNIPER_TESTCONFIGS_PREFIX, "non-juniper", "non-juniper");
  }
}
