package org.batfish.main;

import static com.google.common.io.MoreFiles.createParentDirectories;
import static org.batfish.common.util.CommonUtil.readFile;
import static org.batfish.common.util.CommonUtil.readResource;
import static org.batfish.main.Flatten.main;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.batfish.common.BfConsts;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public final class FlattenTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testMainBadArgs() throws IOException {
    _thrown.expect(IllegalArgumentException.class);
    main(new String[] {});
  }

  private static final String TESTCONFIGS_PATH = "org/batfish/main/testconfigs";

  public void assertInputOutputPair(String inputFilename, String outputFilename)
      throws IOException {
    Path root = _folder.getRoot().toPath();
    Path inputDir = root.resolve("input");
    Path outputDir = root.resolve("output");
    Path inputFile = inputDir.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR).resolve("conf");
    Path outputFile = outputDir.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR).resolve("conf");
    createParentDirectories(inputFile);
    Files.write(
        inputFile,
        readResource(String.format("%s/%s", TESTCONFIGS_PATH, inputFilename))
            .getBytes(StandardCharsets.UTF_8));
    main(new String[] {inputDir.toString(), outputDir.toString()});

    assertThat(
        readFile(outputFile),
        equalTo(readResource(String.format("%s/%s", TESTCONFIGS_PATH, outputFilename))));
  }

  @Test
  public void testMainValid() throws IOException {
    assertInputOutputPair("hierarchical", "flat");
  }

  @Test
  public void testReplaceOnly() throws IOException {
    assertInputOutputPair("replace_only", "replace_only_flattened");
  }

  @Test
  public void testReplaceFilter() throws IOException {
    assertInputOutputPair("replace_filter", "replace_filter_flattened");
  }
}
