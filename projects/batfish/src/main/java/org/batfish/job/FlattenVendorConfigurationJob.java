package org.batfish.job;

import java.nio.file.Path;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.config.Settings;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.grammar.VendorConfigurationFormatDetector;
import org.batfish.main.Batfish;
import org.batfish.main.ParserBatfishException;

public class FlattenVendorConfigurationJob extends BatfishJob<FlattenVendorConfigurationResult> {

  private String _fileText;

  private Path _inputFile;

  private Path _outputFile;

  private Warnings _warnings;

  public FlattenVendorConfigurationJob(
      Settings settings, String fileText, Path inputFile, Path outputFile, Warnings warnings) {
    super(settings);
    _fileText = fileText;
    _inputFile = inputFile;
    _outputFile = outputFile;
    _warnings = warnings;
  }

  @Override
  public FlattenVendorConfigurationResult call() throws Exception {
    long startTime = System.currentTimeMillis();
    long elapsedTime;
    String inputFileAsString = _inputFile.toAbsolutePath().toString();
    ConfigurationFormat format =
        VendorConfigurationFormatDetector.identifyConfigurationFormat(_fileText);

    if (format == ConfigurationFormat.JUNIPER || format == ConfigurationFormat.VYOS) {
      String header = null;
      if (format == ConfigurationFormat.JUNIPER) {
        header = VendorConfigurationFormatDetector.BATFISH_FLATTENED_JUNIPER_HEADER;
      }
      if (format == ConfigurationFormat.VYOS) {
        header = VendorConfigurationFormatDetector.BATFISH_FLATTENED_VYOS_HEADER;
      }
      _logger.debug("Flattening config: \"" + _inputFile + "\"...");
      String flatConfigText;
      try {
        flatConfigText = Batfish.flatten(_fileText, _logger, _settings, format, header);
      } catch (ParserBatfishException e) {
        String error = "Error parsing configuration file: \"" + inputFileAsString + "\"";
        elapsedTime = System.currentTimeMillis() - startTime;
        return new FlattenVendorConfigurationResult(
            elapsedTime, _logger.getHistory(), _outputFile, new BatfishException(error, e));
      } catch (Exception e) {
        String error =
            "Error post-processing parse tree of configuration file: \"" + inputFileAsString + "\"";
        elapsedTime = System.currentTimeMillis() - startTime;
        return new FlattenVendorConfigurationResult(
            elapsedTime, _logger.getHistory(), _outputFile, new BatfishException(error, e));
      } finally {
        Batfish.logWarnings(_logger, _warnings);
      }
      elapsedTime = System.currentTimeMillis() - startTime;
      return new FlattenVendorConfigurationResult(
          elapsedTime, _logger.getHistory(), _outputFile, flatConfigText);
    } else if (!_settings.ignoreUnsupported() && format == ConfigurationFormat.UNKNOWN) {
      elapsedTime = System.currentTimeMillis() - startTime;
      return new FlattenVendorConfigurationResult(
          elapsedTime,
          _logger.getHistory(),
          _outputFile,
          new BatfishException("Unknown configuration format for: \"" + _inputFile + "\""));
    } else {
      _logger.debug("Skipping: \"" + _inputFile + "\"\n");
      String flatConfigText = _fileText;
      elapsedTime = System.currentTimeMillis() - startTime;
      return new FlattenVendorConfigurationResult(
          elapsedTime, _logger.getHistory(), _outputFile, flatConfigText);
    }
  }
}
