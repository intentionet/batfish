package org.batfish.job;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.BatfishException;
import org.batfish.common.ParseTreeSentences;
import org.batfish.common.Warnings;
import org.batfish.config.Settings;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.ParseTreePrettyPrinter;
import org.batfish.grammar.VendorConfigurationFormatDetector;
import org.batfish.grammar.cisco.CiscoCombinedParser;
import org.batfish.grammar.cisco.CiscoControlPlaneExtractor;
import org.batfish.grammar.flatjuniper.FlatJuniperCombinedParser;
import org.batfish.grammar.flatjuniper.FlatJuniperControlPlaneExtractor;
import org.batfish.grammar.flatvyos.FlatVyosCombinedParser;
import org.batfish.grammar.flatvyos.FlatVyosControlPlaneExtractor;
import org.batfish.grammar.iptables.IptablesCombinedParser;
import org.batfish.grammar.iptables.IptablesControlPlaneExtractor;
import org.batfish.grammar.mrv.MrvCombinedParser;
import org.batfish.grammar.mrv.MrvControlPlaneExtractor;
import org.batfish.main.Batfish;
import org.batfish.main.ParserBatfishException;
import org.batfish.representation.host.HostConfiguration;
import org.batfish.vendor.VendorConfiguration;

public class ParseVendorConfigurationJob extends BatfishJob<ParseVendorConfigurationResult> {

  private static Pattern BANNER_PATTERN =
      Pattern.compile("(?m)banner[ \t][ \t]*[^ \r\n\t][^ \r\n\t]*[ \t][ \t]*([^ \r\n\t])[ \r\n]");

  private static String preprocessBanner(String fileText, ConfigurationFormat format) {
    if (format == ConfigurationFormat.CADANT) {
      return fileText;
    }
    Matcher matcher = BANNER_PATTERN.matcher(fileText);
    if (matcher.find()) {
      int delimiterIndex = matcher.start(1);
      char delimiter = fileText.charAt(delimiterIndex);
      String delimiterText =
          (delimiter == '^' ? "\\^" : ("[" + Character.toString(delimiter) + "]"));
      Pattern finalDelimiterPattern = Pattern.compile("(?m)[" + delimiterText + "][\r\n]");
      Matcher finalDelimiterMatcher = finalDelimiterPattern.matcher(fileText);
      if (finalDelimiterMatcher.find(delimiterIndex + 1)) {
        int finalDelimiterIndex = finalDelimiterMatcher.start();
        String beforeDelimiter = fileText.substring(0, delimiterIndex);
        String betweenDelimiters = fileText.substring(delimiterIndex + 1, finalDelimiterIndex);
        String afterDelimiter = fileText.substring(finalDelimiterIndex + 1, fileText.length());
        String newFileText = beforeDelimiter + "^C" + betweenDelimiters + "^C" + afterDelimiter;
        return newFileText;
      } else {
        throw new BatfishException("Invalid banner");
      }
    } else {
      return fileText;
    }
  }

  private Path _file;

  private String _fileText;

  private ConfigurationFormat _format;

  private ParseTreeSentences _ptSentences;

  private Warnings _warnings;

  public ParseVendorConfigurationJob(
      Settings settings,
      String fileText,
      Path file,
      Warnings warnings,
      ConfigurationFormat configurationFormat) {
    super(settings);
    _fileText = fileText;
    _file = file;
    _ptSentences = new ParseTreeSentences();
    _warnings = warnings;
    _format = configurationFormat;
  }

  @SuppressWarnings("fallthrough")
  @Override
  public ParseVendorConfigurationResult call() throws Exception {
    long startTime = System.currentTimeMillis();
    long elapsedTime;
    String currentPath = _file.toAbsolutePath().toString();
    VendorConfiguration vc = null;
    BatfishCombinedParser<?, ?> combinedParser = null;
    ParserRuleContext tree = null;
    ControlPlaneExtractor extractor = null;
    ConfigurationFormat format = _format;
    _logger.info("Processing: '" + currentPath + "'\n");

    for (String s : _settings.ignoreFilesWithStrings()) {
      if (_fileText.contains(s)) {
        format = ConfigurationFormat.IGNORED;
        break;
      }
    }

    String relativePathStr =
        _settings.getActiveTestrigSettings().getBasePath().relativize(_file).toString();

    if (format == ConfigurationFormat.UNKNOWN) {
      format = VendorConfigurationFormatDetector.identifyConfigurationFormat(_fileText);
    }
    switch (format) {
      case EMPTY:
        _warnings.redFlag("Empty file: '" + currentPath + "'\n");
        elapsedTime = System.currentTimeMillis() - startTime;
        return new ParseVendorConfigurationResult(
            elapsedTime, _logger.getHistory(), _file, _warnings, ParseStatus.EMPTY);

      case IGNORED:
        _warnings.pedantic("Ignored file: '" + currentPath + "'\n");
        elapsedTime = System.currentTimeMillis() - startTime;
        return new ParseVendorConfigurationResult(
            elapsedTime, _logger.getHistory(), _file, _warnings, ParseStatus.IGNORED);

      case ARISTA:
      case CADANT:
      case CISCO_IOS:
      case CISCO_IOS_XR:
      case CISCO_NX:
      case FORCE10:
      case FOUNDRY:
        String newFileText = _fileText;
        String fileText;
        _logger.info("\tPreprocessing...");
        do {
          fileText = newFileText;
          try {
            newFileText = preprocessBanner(fileText, format);
          } catch (BatfishException e) {
            elapsedTime = System.currentTimeMillis() - startTime;
            return new ParseVendorConfigurationResult(
                elapsedTime,
                _logger.getHistory(),
                _file,
                new BatfishException("Error preprocessing banner", e));
          }
        } while (newFileText != fileText);
        _logger.info("OK\n");
        CiscoCombinedParser ciscoParser = new CiscoCombinedParser(newFileText, _settings, format);
        combinedParser = ciscoParser;
        extractor =
            new CiscoControlPlaneExtractor(
                newFileText, ciscoParser, format, _warnings, _settings.getUnrecognizedAsRedFlag());
        break;

      case HOST:
        vc = HostConfiguration.fromJson(_fileText, _warnings);
        elapsedTime = System.currentTimeMillis() - startTime;
        return new ParseVendorConfigurationResult(
            elapsedTime, _logger.getHistory(), _file, vc, _warnings, _ptSentences);

      case VYOS:
        if (_settings.flattenOnTheFly()) {
          String msg =
              "Flattening: '"
                  + currentPath
                  + "' on-the-fly; line-numbers reported for this file will be spurious\n";
          _warnings.pedantic(msg);
          // _logger
          // .warn("Flattening: \""
          // + currentPath
          // +
          // "\" on-the-fly; line-numbers reported for this file will be
          // spurious\n");
          _fileText =
              Batfish.flatten(
                  _fileText,
                  _logger,
                  _settings,
                  ConfigurationFormat.VYOS,
                  VendorConfigurationFormatDetector.BATFISH_FLATTENED_VYOS_HEADER);
        } else {
          elapsedTime = System.currentTimeMillis() - startTime;
          return new ParseVendorConfigurationResult(
              elapsedTime,
              _logger.getHistory(),
              _file,
              new BatfishException(
                  "Vyos configurations must be flattened prior to this stage: '"
                      + relativePathStr
                      + "'"));
        }
        // fall through
      case FLAT_VYOS:
        FlatVyosCombinedParser flatVyosParser = new FlatVyosCombinedParser(_fileText, _settings);
        combinedParser = flatVyosParser;
        extractor = new FlatVyosControlPlaneExtractor(_fileText, flatVyosParser, _warnings);
        break;

      case JUNIPER:
        if (_settings.flattenOnTheFly()) {
          String msg =
              "Flattening: '"
                  + currentPath
                  + "' on-the-fly; line-numbers reported for this file will be spurious\n";
          _warnings.pedantic(msg);
          // _logger
          // .warn("Flattening: \""
          // + currentPath
          // +
          // "\" on-the-fly; line-numbers reported for this file will be
          // spurious\n");
          try {
            _fileText =
                Batfish.flatten(
                    _fileText,
                    _logger,
                    _settings,
                    ConfigurationFormat.JUNIPER,
                    VendorConfigurationFormatDetector.BATFISH_FLATTENED_JUNIPER_HEADER);
          } catch (BatfishException e) {
            String error = "Error flattening configuration file: '" + currentPath + "'";
            elapsedTime = System.currentTimeMillis() - startTime;
            return new ParseVendorConfigurationResult(
                elapsedTime, _logger.getHistory(), _file, new BatfishException(error, e));
          }
        } else {
          elapsedTime = System.currentTimeMillis() - startTime;
          return new ParseVendorConfigurationResult(
              elapsedTime,
              _logger.getHistory(),
              _file,
              new BatfishException(
                  "Juniper configurations must be flattened prior to this stage: '"
                      + relativePathStr
                      + "'"));
        }
        // fall through
      case FLAT_JUNIPER:
        FlatJuniperCombinedParser flatJuniperParser =
            new FlatJuniperCombinedParser(_fileText, _settings);
        combinedParser = flatJuniperParser;
        extractor = new FlatJuniperControlPlaneExtractor(_fileText, flatJuniperParser, _warnings);
        break;

      case IPTABLES:
        IptablesCombinedParser iptablesParser = new IptablesCombinedParser(_fileText, _settings);
        combinedParser = iptablesParser;
        extractor =
            new IptablesControlPlaneExtractor(
                _fileText, iptablesParser, _warnings, relativePathStr);
        break;

      case MRV:
        MrvCombinedParser mrvParser = new MrvCombinedParser(_fileText, _settings);
        combinedParser = mrvParser;
        extractor = new MrvControlPlaneExtractor(_fileText, mrvParser, _warnings);
        break;

      case ALCATEL_AOS:
      case AWS_VPC:
      case BLADENETWORK:
      case F5:
      case JUNIPER_SWITCH:
      case MRV_COMMANDS:
      case MSS:
      case VXWORKS:
        String unsupportedError =
            "Unsupported configuration format: '" + format + "' for file: '" + currentPath + "'\n";
        if (!_settings.ignoreUnsupported()) {
          elapsedTime = System.currentTimeMillis() - startTime;
          return new ParseVendorConfigurationResult(
              elapsedTime, _logger.getHistory(), _file, new BatfishException(unsupportedError));
        } else {
          _warnings.unimplemented(unsupportedError);
          elapsedTime = System.currentTimeMillis() - startTime;
          return new ParseVendorConfigurationResult(
              elapsedTime, _logger.getHistory(), _file, _warnings, ParseStatus.UNSUPPORTED);
        }

      case UNKNOWN:
      default:
        String unknownError = "Unknown configuration format for file: '" + currentPath + "'\n";
        if (!_settings.ignoreUnknown()) {
          elapsedTime = System.currentTimeMillis() - startTime;
          return new ParseVendorConfigurationResult(
              elapsedTime, _logger.getHistory(), _file, new BatfishException(unknownError));
        } else {
          _warnings.unimplemented(unknownError);
          elapsedTime = System.currentTimeMillis() - startTime;
          return new ParseVendorConfigurationResult(
              elapsedTime, _logger.getHistory(), _file, _warnings, ParseStatus.UNKNOWN);
        }
    }

    try {
      _logger.info("\tParsing...");
      tree = Batfish.parse(combinedParser, _logger, _settings);
      if (_settings.printParseTree()) {
        _ptSentences = ParseTreePrettyPrinter.getParseTreeSentences(tree, combinedParser);
      }
      _logger.info("\tPost-processing...");
      extractor.processParseTree(tree);
      if (!combinedParser.getErrors().isEmpty()) {
        elapsedTime = System.currentTimeMillis() - startTime;
        return new ParseVendorConfigurationResult(
            elapsedTime,
            _logger.getHistory(),
            _file,
            new BatfishException(
                String.format(
                    "Configuration file: '%s' contains unrecognized lines:\n%s",
                    _file.getFileName().toString(),
                    String.join("\n", combinedParser.getErrors()))));
      }
      _logger.info("OK\n");
    } catch (ParserBatfishException e) {
      String error = "Error parsing configuration file: '" + currentPath + "'";
      elapsedTime = System.currentTimeMillis() - startTime;
      return new ParseVendorConfigurationResult(
          elapsedTime, _logger.getHistory(), _file, new BatfishException(error, e));
    } catch (Exception e) {
      String error =
          "Error post-processing parse tree of configuration file: '" + currentPath + "'";
      elapsedTime = System.currentTimeMillis() - startTime;
      return new ParseVendorConfigurationResult(
          elapsedTime, _logger.getHistory(), _file, new BatfishException(error, e));
    } finally {
      Batfish.logWarnings(_logger, _warnings);
    }
    vc = extractor.getVendorConfiguration();
    vc.setVendor(format);
    vc.setFilename(_file.getFileName().toString());
    // at this point we should have a VendorConfiguration vc
    String hostname = vc.getHostname();
    if (hostname == null) {
      String error = "No hostname set in file: '" + relativePathStr.replace("\\", "/") + "'\n";
      try {
        _warnings.redFlag(error);
      } catch (BatfishException e) {
        elapsedTime = System.currentTimeMillis() - startTime;
        return new ParseVendorConfigurationResult(elapsedTime, _logger.getHistory(), _file, e);
      }
      String filename = _file.getFileName().toString();
      String guessedHostname = filename.replaceAll("\\.(cfg|conf)$", "");
      _logger.redflag(
          "\tNo hostname set! Guessing hostname from filename: '"
              + filename
              + "' ==> '"
              + guessedHostname
              + "'\n");
      vc.setHostname(guessedHostname);
    }
    elapsedTime = System.currentTimeMillis() - startTime;
    return new ParseVendorConfigurationResult(
        elapsedTime, _logger.getHistory(), _file, vc, _warnings, _ptSentences);
  }
}
