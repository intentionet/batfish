package org.batfish.main;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.file.Files.createDirectories;
import static org.batfish.common.Warnings.forLogger;
import static org.batfish.main.CliUtils.readAllFiles;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.job.BatfishJobExecutor;
import org.batfish.job.PreprocessJob;

/** Utility to dump output of configuration pre-processing. */
public final class Preprocess {

  public static void main(String[] args) throws IOException {
    checkArgument(args.length == 2, "Expected arguments: <input_dir> <output_dir>");
    Path inputPath = Paths.get(args[0]);
    Path outputPath = Paths.get(args[1]);

    // Bazel: resolve relative to current working directory. No-op if paths are already absolute.
    String wd = System.getenv("BUILD_WORKING_DIRECTORY");
    if (wd != null) {
      inputPath = Paths.get(wd).resolve(inputPath);
      outputPath = Paths.get(wd).resolve(outputPath);
    }

    Settings settings = new Settings(new String[] {"-storagebase", "/"});
    BatfishLogger logger = new BatfishLogger(BatfishLogger.LEVELSTR_WARN, false, System.out);
    settings.setLogger(logger);
    preprocess(inputPath, outputPath, settings);
  }

  /**
   * Pre-process configs in snapshot stored at {@code inputPath}, and dump to {@code outputPath}.
   * Non-preprocessible configs are copied unprocessed.
   */
  private static void preprocess(
      @Nonnull Path inputPath, @Nonnull Path outputPath, Settings settings) throws IOException {
    BatfishLogger logger = settings.getLogger();
    logger.info("\n*** READING INPUT FILES ***\n");
    Map<Path, String> configurationData =
        readAllFiles(inputPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR), logger);

    Map<Path, String> outputConfigurationData = new TreeMap<>();
    Path outputConfigDir = outputPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR);
    createDirectories(outputConfigDir);
    logger.info("\n*** COMPUTING OUTPUT FILES ***\n");
    logger.resetTimer();
    List<PreprocessJob> jobs = new ArrayList<>();
    for (Entry<Path, String> configFile : configurationData.entrySet()) {
      Path inputFile = configFile.getKey();
      String fileText = configFile.getValue();
      Warnings warnings = forLogger(logger);
      String name = inputFile.getFileName().toString();
      Path outputFile = outputConfigDir.resolve(name);
      PreprocessJob job = new PreprocessJob(settings, fileText, inputFile, outputFile, warnings);
      jobs.add(job);
    }
    BatfishJobExecutor.runJobsInExecutor(
        settings,
        logger,
        jobs,
        outputConfigurationData,
        null,
        settings.getFlatten() || settings.getHaltOnParseError(),
        "Preprocesss configurations");
    logger.printElapsedTime();
    for (Entry<Path, String> e : outputConfigurationData.entrySet()) {
      Path outputFile = e.getKey();
      String preprocessedConfigText = e.getValue();
      String outputFileAsString = outputFile.toString();
      logger.debugf("Writing config to \"%s\"...", outputFileAsString);
      CommonUtil.writeFile(outputFile, preprocessedConfigText);
      logger.debug("OK\n");
    }
  }
}
