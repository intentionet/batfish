package org.batfish.main;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.SortedMap;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.batfish.common.BatfishLogger;

/** Utility functions for internal CLI tools */
final class CliUtils {

  /**
   * Reads the files in the given directory (recursively) and returns a map from each file's {@link
   * Path} to its contents.
   *
   * <p>Temporary files (files start with {@code .} are omitted from the returned list.
   *
   * <p>This method follows all symbolic links.
   *
   * @throws IOException if there is an error
   */
  static SortedMap<Path, String> readAllFiles(Path directory, BatfishLogger logger)
      throws IOException {
    try (Stream<Path> paths = Files.walk(directory, FileVisitOption.FOLLOW_LINKS)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> !path.getFileName().toString().startsWith("."))
          .map(
              path -> {
                logger.debugf("Reading: \"%s\"\n", path);
                String fileText = null;
                try {
                  fileText =
                      FileUtils.readFileToString(
                          path.toAbsolutePath().toFile(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
                if (!fileText.isEmpty()) {
                  // Adding a trailing newline helps EOF in some parsers.
                  fileText += '\n';
                }
                return new SimpleEntry<>(path, fileText);
              })
          .collect(
              ImmutableSortedMap.toImmutableSortedMap(
                  Ordering.natural(), SimpleEntry::getKey, SimpleEntry::getValue));
    }
  }

  private CliUtils() {}
}
