package org.batfish.common.util;

import com.google.common.io.ByteStreams;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.batfish.common.BatfishException;

/**
 * This utility extracts files and directories of a standard zip file to a destination directory.
 *
 * @author www.codejava.net with minor local changes tagged with :ratul:
 */
public final class UnzipUtility {
  /**
   * Extracts a zip entry (file entry)
   *
   * @param zipIn The zip input stream providing the file data
   * @param filePath The path to write the output file
   */
  private static void extractFile(ZipInputStream zipIn, Path filePath) {
    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
      ByteStreams.copy(zipIn, fos);
    } catch (IOException e) {
      throw new BatfishException("Error unzipping to output file: '" + filePath + "'", e);
    }
  }

  /**
   * Extracts a zip file specified by the zipFilePath to a directory specified by {@code
   * destDirectory} (will be created if does not exists)
   *
   * @param zipFile The path to the input zip file
   * @param destDirectory The output directory in which to extract the zip
   */
  public static void unzip(Path zipFile, Path destDirectory) {
    if (!Files.exists(destDirectory) && !destDirectory.toFile().mkdirs()) {
      throw new BatfishException("Could not create zip output directory " + destDirectory);
    }

    try {
      // :ratul:
      // this lets us check if the zip file is proper
      // for bad zip files this will throw an exception
      ZipFile zipTest = new ZipFile(zipFile.toFile());
      zipTest.close();

      try (FileInputStream fis = new FileInputStream(zipFile.toFile());
          ZipInputStream zipIn = new ZipInputStream(fis)) {

        for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
          Path outputPath = destDirectory.resolve(entry.getName());
          if (entry.isDirectory()) {
            // Make the directory, including parent dirs.
            if (!outputPath.toFile().mkdirs()) {
              throw new IOException("Unable to make directory " + outputPath);
            }
          } else {
            // Extract the file.
            extractFile(zipIn, outputPath);
          }
          zipIn.closeEntry();
        }
      }
    } catch (IOException e) {
      throw new BatfishException(
          "Could not unzip: '" + zipFile + "' into: '" + destDirectory + "'", e);
    }
  }

  // Prevent instantiation of utility class.
  private UnzipUtility() {}
}
