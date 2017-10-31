package org.batfish.job;

import java.nio.file.Path;
import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.BatfishException;
import org.batfish.common.ParseTreeSentences;
import org.batfish.common.Warnings;
import org.batfish.common.plugin.IBatfish;
import org.batfish.config.Settings;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.ParseTreePrettyPrinter;
import org.batfish.grammar.RoutingTableExtractor;
import org.batfish.grammar.RoutingTableFormat;
import org.batfish.grammar.RoutingTableFormatDetector;
import org.batfish.grammar.routing_table.eos.EosRoutingTableCombinedParser;
import org.batfish.grammar.routing_table.eos.EosRoutingTableExtractor;
import org.batfish.grammar.routing_table.ios.IosRoutingTableCombinedParser;
import org.batfish.grammar.routing_table.ios.IosRoutingTableExtractor;
import org.batfish.grammar.routing_table.nxos.NxosRoutingTableCombinedParser;
import org.batfish.grammar.routing_table.nxos.NxosRoutingTableExtractor;
import org.batfish.main.Batfish;
import org.batfish.main.ParserBatfishException;

public class ParseEnvironmentRoutingTableJob
    extends BatfishJob<ParseEnvironmentRoutingTableResult> {

  private IBatfish _batfish;

  private Path _file;

  private String _fileText;

  private String _hostname;

  private ParseTreeSentences _ptSentences;

  private Warnings _warnings;

  public ParseEnvironmentRoutingTableJob(
      Settings settings, String fileText, Path file, Warnings warnings, IBatfish batfish) {
    super(settings);
    _batfish = batfish;
    _fileText = fileText;
    _file = file;
    _hostname = file.getFileName().toString();
    _ptSentences = new ParseTreeSentences();
    _warnings = warnings;
  }

  @Override
  public ParseEnvironmentRoutingTableResult call() throws Exception {
    long startTime = System.currentTimeMillis();
    long elapsedTime;
    String currentPath = _file.toAbsolutePath().toString();
    BatfishCombinedParser<?, ?> combinedParser = null;
    ParserRuleContext tree = null;
    RoutingTableExtractor extractor = null;
    _logger.info("Processing: '" + currentPath + "'\n");
    // String relativePathStr =
    // _settings.getActiveTestrigSettings().getEnvironmentSettings().getEnvPath()
    // .relativize(_file).toString();
    RoutingTableFormat format = RoutingTableFormatDetector.identifyRoutingTableFormat(_fileText);
    switch (format) {
      case EMPTY:
        _warnings.redFlag("Empty file: '" + currentPath + "'\n");
        elapsedTime = System.currentTimeMillis() - startTime;
        return new ParseEnvironmentRoutingTableResult(
            elapsedTime, _logger.getHistory(), _file, _warnings, ParseStatus.EMPTY);

      case EOS:
        EosRoutingTableCombinedParser eosRoutingTableParser =
            new EosRoutingTableCombinedParser(_fileText, _settings);
        combinedParser = eosRoutingTableParser;
        extractor =
            new EosRoutingTableExtractor(
                _hostname, _fileText, eosRoutingTableParser, _warnings, _batfish);
        break;

      case IOS:
        IosRoutingTableCombinedParser iosRoutingTableParser =
            new IosRoutingTableCombinedParser(_fileText, _settings);
        combinedParser = iosRoutingTableParser;
        extractor =
            new IosRoutingTableExtractor(
                _hostname, _fileText, iosRoutingTableParser, _warnings, _batfish);
        break;

      case NXOS:
        NxosRoutingTableCombinedParser nxosRoutingTableParser =
            new NxosRoutingTableCombinedParser(_fileText, _settings);
        combinedParser = nxosRoutingTableParser;
        extractor =
            new NxosRoutingTableExtractor(
                _hostname, _fileText, nxosRoutingTableParser, _warnings, _batfish);
        break;

        /** PLACEHOLDER CODES FOR IDENTIFIED BUT UNSUPPORTED ROUTING TABLE FORMATS */
        /*
         * String unsupportedError = "Unsupported routing-table format: '" +
         * format.toString() + "' for file: '" + currentPath + "'\n"; if
         * (!_settings.ignoreUnsupported()) { elapsedTime =
         * System.currentTimeMillis() - startTime; return new
         * ParseEnvironmentRoutingTableResult(elapsedTime, _logger.getHistory(),
         * _file, new BatfishException(unsupportedError)); } else {
         * _warnings.unimplemented(unsupportedError); elapsedTime =
         * System.currentTimeMillis() - startTime; return new
         * ParseEnvironmentRoutingTableResult(elapsedTime, _logger.getHistory(),
         * _file, _warnings, ParseStatus.UNSUPPORTED); }
         */

      case UNKNOWN:
      default:
        String unknownError = "Unknown routing-table format for file: '" + currentPath + "'\n";
        if (!_settings.ignoreUnknown()) {
          elapsedTime = System.currentTimeMillis() - startTime;
          return new ParseEnvironmentRoutingTableResult(
              elapsedTime, _logger.getHistory(), _file, new BatfishException(unknownError));
        } else {
          _warnings.unimplemented(unknownError);
          elapsedTime = System.currentTimeMillis() - startTime;
          return new ParseEnvironmentRoutingTableResult(
              elapsedTime, _logger.getHistory(), _file, _warnings, ParseStatus.UNKNOWN);
        }
    }

    try {
      _logger.info("\tParsing...");
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
      return new ParseEnvironmentRoutingTableResult(
          elapsedTime, _logger.getHistory(), _file, new BatfishException(error, e));
    } catch (Exception e) {
      String error =
          "Error post-processing parse tree of configuration file: '" + currentPath + "'";
      elapsedTime = System.currentTimeMillis() - startTime;
      return new ParseEnvironmentRoutingTableResult(
          elapsedTime, _logger.getHistory(), _file, new BatfishException(error, e));
    } finally {
      Batfish.logWarnings(_logger, _warnings);
    }
    RoutesByVrf routesByVrf = extractor.getRoutesByVrf();
    elapsedTime = System.currentTimeMillis() - startTime;
    return new ParseEnvironmentRoutingTableResult(
        elapsedTime, _logger.getHistory(), _file, _hostname, routesByVrf, _warnings, _ptSentences);
  }
}
