package org.batfish.job;

import java.nio.file.Path;
import java.util.SortedMap;
import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.BatfishException;
import org.batfish.common.ParseTreeSentences;
import org.batfish.common.Warnings;
import org.batfish.common.plugin.BgpTablePlugin;
import org.batfish.config.Settings;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BgpTableExtractor;
import org.batfish.grammar.BgpTableFormat;
import org.batfish.grammar.BgpTableFormatDetector;
import org.batfish.grammar.ParseTreePrettyPrinter;
import org.batfish.main.Batfish;
import org.batfish.main.ParserBatfishException;

public class ParseEnvironmentBgpTableJob extends BatfishJob<ParseEnvironmentBgpTableResult> {

  private SortedMap<BgpTableFormat, BgpTablePlugin> _bgpTablePlugins;

  private Path _file;

  private String _fileText;

  private String _hostname;

  private ParseTreeSentences _ptSentences;

  private Warnings _warnings;

  public ParseEnvironmentBgpTableJob(
      Settings settings,
      String fileText,
      String hostname,
      Path file,
      Warnings warnings,
      SortedMap<BgpTableFormat, BgpTablePlugin> bgpTablePlugins) {
    super(settings);
    _bgpTablePlugins = bgpTablePlugins;
    _fileText = fileText;
    _file = file;
    _hostname = hostname;
    _ptSentences = new ParseTreeSentences();
    _warnings = warnings;
  }

  @Override
  public ParseEnvironmentBgpTableResult call() throws Exception {
    long startTime = System.currentTimeMillis();
    long elapsedTime;
    String currentPath = _file.toAbsolutePath().toString();
    ParserRuleContext tree = null;
    _logger.info("Processing: '" + currentPath + "'\n");
    // String relativePathStr =
    // _settings.getActiveTestrigSettings().getEnvironmentSettings().getEnvPath()
    // .relativize(_file).toString();
    BgpTablePlugin plugin = null;
    BgpTableFormat format = BgpTableFormatDetector.identifyBgpTableFormat(_fileText);
    switch (format) {
      case EMPTY:
        _warnings.redFlag("Empty file: '" + currentPath + "'\n");
        elapsedTime = System.currentTimeMillis() - startTime;
        return new ParseEnvironmentBgpTableResult(
            elapsedTime, _logger.getHistory(), _file, _warnings, ParseStatus.EMPTY);

      case UNKNOWN:
        String unknownError = "Unknown bgp-table format for file: '" + currentPath + "'\n";
        if (!_settings.ignoreUnknown()) {
          elapsedTime = System.currentTimeMillis() - startTime;
          return new ParseEnvironmentBgpTableResult(
              elapsedTime, _logger.getHistory(), _file, new BatfishException(unknownError));
        } else {
          _warnings.unimplemented(unknownError);
          elapsedTime = System.currentTimeMillis() - startTime;
          return new ParseEnvironmentBgpTableResult(
              elapsedTime, _logger.getHistory(), _file, _warnings, ParseStatus.UNKNOWN);
        }

        // $CASES-OMITTED$
      default:
        break;
    }
    plugin = _bgpTablePlugins.get(format);
    if (plugin == null) {
      String unsupportedError =
          "Unsupported bgp-table format: '"
              + format.bgpTableFormatName()
              + "' for file: '"
              + currentPath
              + "'\n";
      if (!_settings.ignoreUnsupported()) {
        elapsedTime = System.currentTimeMillis() - startTime;
        return new ParseEnvironmentBgpTableResult(
            elapsedTime, _logger.getHistory(), _file, new BatfishException(unsupportedError));
      } else {
        _warnings.unimplemented(unsupportedError);
        elapsedTime = System.currentTimeMillis() - startTime;
        return new ParseEnvironmentBgpTableResult(
            elapsedTime, _logger.getHistory(), _file, _warnings, ParseStatus.UNSUPPORTED);
      }
    }
    BgpTableExtractor extractor;
    try {
      _logger.info("\tParsing...");
      BatfishCombinedParser<?, ?> combinedParser = plugin.parser(_fileText, _settings);
      extractor = plugin.extractor(_hostname, _fileText, combinedParser, _warnings);
      tree = Batfish.parse(combinedParser, _logger, _settings);
      if (_settings.getPrintParseTree()) {
        _ptSentences = ParseTreePrettyPrinter.getParseTreeSentences(tree, combinedParser);
      }
      _logger.info("\tPost-processing...");
      extractor.processParseTree(tree);
      _logger.info("OK\n");
    } catch (ParserBatfishException e) {
      String error = "Error parsing configuration file: '" + currentPath + "'";
      elapsedTime = System.currentTimeMillis() - startTime;
      return new ParseEnvironmentBgpTableResult(
          elapsedTime, _logger.getHistory(), _file, new BatfishException(error, e));
    } catch (Exception e) {
      String error =
          "Error post-processing parse tree of configuration file: '" + currentPath + "'";
      elapsedTime = System.currentTimeMillis() - startTime;
      return new ParseEnvironmentBgpTableResult(
          elapsedTime, _logger.getHistory(), _file, new BatfishException(error, e));
    } finally {
      Batfish.logWarnings(_logger, _warnings);
    }
    BgpAdvertisementsByVrf bgpAdvertisementsByVrf = extractor.getBgpAdvertisementsByVrf();
    elapsedTime = System.currentTimeMillis() - startTime;
    return new ParseEnvironmentBgpTableResult(
        elapsedTime,
        _logger.getHistory(),
        _file,
        _hostname,
        bgpAdvertisementsByVrf,
        _warnings,
        _ptSentences);
  }
}
