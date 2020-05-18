package org.batfish.main;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.Warnings.forLogger;
import static org.batfish.main.CliUtils.readAllFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.Warnings;
import org.batfish.config.Settings;
import org.batfish.datamodel.answers.FlattenVendorConfigurationAnswerElement;
import org.batfish.job.BatfishJobExecutor;
import org.batfish.job.FlattenVendorConfigurationJob;

public final class Flatten {

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
    settings.setLogger(new BatfishLogger(BatfishLogger.LEVELSTR_WARN, false, System.out));

    flatten(inputPath, outputPath, settings);
  }

  private static void flatten(Path inputPath, Path outputPath, Settings settings)
      throws IOException {
    BatfishLogger logger = settings.getLogger();
    logger.info("\n*** READING FILES TO FLATTEN ***\n");
    Map<Path, String> configurationData =
        readAllFiles(inputPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR), logger);

    Map<Path, String> outputConfigurationData = new TreeMap<>();
    Path outputConfigDir = outputPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR);
    Files.createDirectories(outputConfigDir);
    logger.info("\n*** FLATTENING TEST RIG ***\n");
    logger.resetTimer();
    List<FlattenVendorConfigurationJob> jobs = new ArrayList<>();
    for (Entry<Path, String> configFile : configurationData.entrySet()) {
      Path inputFile = configFile.getKey();
      String fileText = configFile.getValue();
      Warnings warnings = forLogger(logger);
      String name = inputFile.getFileName().toString();
      Path outputFile = outputConfigDir.resolve(name);
      FlattenVendorConfigurationJob job =
          new FlattenVendorConfigurationJob(settings, fileText, inputFile, outputFile, warnings);
      jobs.add(job);
    }
    BatfishJobExecutor.runJobsInExecutor(
        settings,
        logger,
        jobs,
        outputConfigurationData,
        new FlattenVendorConfigurationAnswerElement(),
        settings.getFlatten() || settings.getHaltOnParseError(),
        "Flatten configurations");
    logger.printElapsedTime();
    for (Entry<Path, String> e : outputConfigurationData.entrySet()) {
      Path outputFile = e.getKey();
      String flatConfigText = e.getValue();
      logger.debugf("Writing config to \"%s\"...", outputFile);
      Files.write(outputFile, flatConfigText.getBytes(UTF_8));
      logger.debug("OK\n");
    }
  }
}
