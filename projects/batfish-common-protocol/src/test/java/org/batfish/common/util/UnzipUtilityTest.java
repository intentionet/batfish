package org.batfish.common.util;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.batfish.common.BatfishException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link UnzipUtility}. */
@RunWith(JUnit4.class)
public class UnzipUtilityTest {
  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testUnzipEmptyFile() throws IOException {
    File empty = _folder.newFile("empty");
    try (FileOutputStream fos = new FileOutputStream(empty);
        ZipOutputStream ignored = new ZipOutputStream(fos)) {
      assert ignored != null; // avoid unused warning.
    }

    File dest = _folder.newFolder("dest");
    UnzipUtility.unzip(empty.toPath(), dest.toPath());
    assertThat("output folder exists", dest.exists(), is(true));
    assertThat("output folder is a directory", dest.isDirectory(), is(true));
    assertThat("output folder is empty", dest.list(), equalTo(new String[0]));
  }

  @Test
  public void testUnzipDirectoriesMissing() throws IOException {
    byte[] contents = "contents of file.txt".getBytes();
    File directoriesMissing = _folder.newFile("directories_missing");
    try (FileOutputStream fos = new FileOutputStream(directoriesMissing);
        ZipOutputStream out = new ZipOutputStream(fos)) {
      out.putNextEntry(new ZipEntry("missing_dir/child_dir/file.txt"));
      out.write(contents);
    }

    File dest = _folder.newFolder("dest");
    UnzipUtility.unzip(directoriesMissing.toPath(), dest.toPath());

    File file =
        dest.toPath().resolve("missing_dir").resolve("child_dir").resolve("file.txt").toFile();
    assertThat("output file exists", file.exists(), is(true));
    assertThat("output file is a file", file.isDirectory(), is(false));
    assertThat("output file contents", Files.readAllBytes(file.toPath()), equalTo(contents));
  }

  @Test
  public void testUnzipPathViolation() throws IOException {
    File pathViolation = _folder.newFile("pathViolation");
    try (FileOutputStream fos = new FileOutputStream(pathViolation);
        ZipOutputStream out = new ZipOutputStream(fos)) {
      out.putNextEntry(new ZipEntry("../../"));
      out.putNextEntry(new ZipEntry("../../file.txt"));
      out.write("contents".getBytes());
    }

    File dest = _folder.newFolder("dest");
    _thrown.expect(BatfishException.class);
    _thrown.expectCause(instanceOf(IOException.class));
    UnzipUtility.unzip(pathViolation.toPath(), dest.toPath());
  }
}
