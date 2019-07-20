package org.batfish.main;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toMap;
import static org.batfish.bddreachability.BDDMultipathInconsistency.computeMultipathInconsistencies;
import static org.batfish.common.util.CommonUtil.detectCharset;
import static org.batfish.common.util.CompletionMetadataUtils.getFilterNames;
import static org.batfish.common.util.CompletionMetadataUtils.getInterfaces;
import static org.batfish.common.util.CompletionMetadataUtils.getIps;
import static org.batfish.common.util.CompletionMetadataUtils.getMlagIds;
import static org.batfish.common.util.CompletionMetadataUtils.getNodes;
import static org.batfish.common.util.CompletionMetadataUtils.getPrefixes;
import static org.batfish.common.util.CompletionMetadataUtils.getRoutingPolicyNames;
import static org.batfish.common.util.CompletionMetadataUtils.getStructureNames;
import static org.batfish.common.util.CompletionMetadataUtils.getVrfs;
import static org.batfish.common.util.CompletionMetadataUtils.getZones;
import static org.batfish.datamodel.acl.AclLineMatchExprs.not;
import static org.batfish.main.ReachabilityParametersResolver.resolveReachabilityParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import io.opentracing.ActiveSpan;
import io.opentracing.References;
import io.opentracing.SpanContext;
import io.opentracing.util.GlobalTracer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.bddreachability.BDDLoopDetectionAnalysis;
import org.batfish.bddreachability.BDDReachabilityAnalysis;
import org.batfish.bddreachability.BDDReachabilityAnalysisFactory;
import org.batfish.bddreachability.BidirectionalReachabilityAnalysis;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishException.BatfishStackTrace;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.CleanBatfishException;
import org.batfish.common.CompletionMetadata;
import org.batfish.common.CoordConsts;
import org.batfish.common.CoordConstsV2;
import org.batfish.common.ErrorDetails;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Version;
import org.batfish.common.Warning;
import org.batfish.common.Warnings;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.plugin.BgpTablePlugin;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.plugin.DataPlanePlugin.ComputeDataPlaneResult;
import org.batfish.common.plugin.ExternalBgpAdvertisementPlugin;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.PluginClientType;
import org.batfish.common.plugin.PluginConsumer;
import org.batfish.common.plugin.TracerouteEngine;
import org.batfish.common.topology.TopologyContainer;
import org.batfish.common.topology.TopologyProvider;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.IspModelingUtils;
import org.batfish.config.Settings;
import org.batfish.config.TestrigSettings;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.DeviceType;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.GenericConfigObject;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.AnswerMetadataUtil;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.ConvertStatus;
import org.batfish.datamodel.answers.DataPlaneAnswerElement;
import org.batfish.datamodel.answers.FlattenVendorConfigurationAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.InitStepAnswerElement;
import org.batfish.datamodel.answers.MajorIssueConfig;
import org.batfish.datamodel.answers.ParseAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentBgpTablesAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentRoutingTablesAnswerElement;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.answers.RunAnalysisAnswerElement;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.datamodel.eigrp.EigrpTopologyUtils;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.flow.TraceWrapperAsAnswerElement;
import org.batfish.datamodel.isp_configuration.IspConfiguration;
import org.batfish.datamodel.ospf.OspfTopologyUtils;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.datamodel.questions.InvalidReachabilityParametersException;
import org.batfish.datamodel.questions.Question;
import org.batfish.dataplane.TracerouteEngineImpl;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishParseException;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.BgpTableFormat;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.ParseTreePrettyPrinter;
import org.batfish.grammar.flattener.Flattener;
import org.batfish.grammar.juniper.JuniperCombinedParser;
import org.batfish.grammar.juniper.JuniperFlattener;
import org.batfish.grammar.vyos.VyosCombinedParser;
import org.batfish.grammar.vyos.VyosFlattener;
import org.batfish.identifiers.AnalysisId;
import org.batfish.identifiers.AnswerId;
import org.batfish.identifiers.FileBasedIdResolver;
import org.batfish.identifiers.IdResolver;
import org.batfish.identifiers.IssueSettingsId;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.NodeRolesId;
import org.batfish.identifiers.QuestionId;
import org.batfish.identifiers.QuestionSettingsId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.job.BatfishJobExecutor;
import org.batfish.job.ConvertConfigurationJob;
import org.batfish.job.FlattenVendorConfigurationJob;
import org.batfish.job.ParseEnvironmentBgpTableJob;
import org.batfish.job.ParseEnvironmentRoutingTableJob;
import org.batfish.job.ParseResult;
import org.batfish.job.ParseVendorConfigurationJob;
import org.batfish.job.ParseVendorConfigurationResult;
import org.batfish.question.ReachabilityParameters;
import org.batfish.question.ResolvedReachabilityParameters;
import org.batfish.question.SrcNattedConstraint;
import org.batfish.question.bidirectionalreachability.BidirectionalReachabilityResult;
import org.batfish.question.differentialreachability.DifferentialReachabilityParameters;
import org.batfish.question.differentialreachability.DifferentialReachabilityResult;
import org.batfish.question.multipath.MultipathConsistencyParameters;
import org.batfish.referencelibrary.ReferenceLibrary;
import org.batfish.representation.aws.AwsConfiguration;
import org.batfish.representation.host.HostConfiguration;
import org.batfish.representation.iptables.IptablesVendorConfiguration;
import org.batfish.role.InferRoles;
import org.batfish.role.NodeRoleDimension;
import org.batfish.role.NodeRolesData;
import org.batfish.specifier.AllInterfaceLinksLocationSpecifier;
import org.batfish.specifier.AllInterfacesLocationSpecifier;
import org.batfish.specifier.InferFromLocationIpSpaceSpecifier;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.Location;
import org.batfish.specifier.SpecifierContext;
import org.batfish.specifier.SpecifierContextImpl;
import org.batfish.specifier.UnionLocationSpecifier;
import org.batfish.storage.FileBasedStorage;
import org.batfish.storage.StorageProvider;
import org.batfish.symbolic.IngressLocation;
import org.batfish.topology.TopologyProviderImpl;
import org.batfish.vendor.VendorConfiguration;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/** This class encapsulates the main control logic for Batfish. */
public class Batfish extends PluginConsumer implements IBatfish {

  public static final String DIFFERENTIAL_FLOW_TAG = "DIFFERENTIAL";

  private static final Pattern MANAGEMENT_INTERFACES =
      Pattern.compile(
          "(\\Amgmt)|(\\Amanagement)|(\\Afxp0)|(\\Aem0)|(\\Ame0)|(\\Avme)", CASE_INSENSITIVE);

  private static final Pattern MANAGEMENT_VRFS =
      Pattern.compile("(\\Amgmt)|(\\Amanagement)", CASE_INSENSITIVE);

  /** The name of the [optional] topology file within a test-rig */
  public static void applyBaseDir(TestrigSettings settings, Path containerDir, SnapshotId testrig) {
    Path testrigDir =
        containerDir.resolve(Paths.get(BfConsts.RELPATH_SNAPSHOTS_DIR, testrig.getId()));
    settings.setName(testrig);
    settings.setBasePath(testrigDir);
  }

  static void checkTopology(Map<String, Configuration> configurations, Topology topology) {
    for (Edge edge : topology.getEdges()) {
      if (!configurations.containsKey(edge.getNode1())) {
        throw new BatfishException(
            String.format("Topology contains a non-existent node '%s'", edge.getNode1()));
      }
      if (!configurations.containsKey(edge.getNode2())) {
        throw new BatfishException(
            String.format("Topology contains a non-existent node '%s'", edge.getNode2()));
      }
      // nodes are valid, now checking corresponding interfaces
      Configuration config1 = configurations.get(edge.getNode1());
      Configuration config2 = configurations.get(edge.getNode2());
      if (!config1.getAllInterfaces().containsKey(edge.getInt1())) {
        throw new BatfishException(
            String.format(
                "Topology contains a non-existent interface '%s' on node '%s'",
                edge.getInt1(), edge.getNode1()));
      }
      if (!config2.getAllInterfaces().containsKey(edge.getInt2())) {
        throw new BatfishException(
            String.format(
                "Topology contains a non-existent interface '%s' on node '%s'",
                edge.getInt2(), edge.getNode2()));
      }
    }
  }

  public static Flattener flatten(
      String input,
      BatfishLogger logger,
      Settings settings,
      Warnings warnings,
      ConfigurationFormat format,
      String header) {
    switch (format) {
      case JUNIPER:
        // Just use the Juniper flattener for PaloAlto for now since the process is identical
      case PALO_ALTO_NESTED:
        {
          JuniperCombinedParser parser = new JuniperCombinedParser(input, settings);
          ParserRuleContext tree = parse(parser, logger, settings);
          JuniperFlattener flattener = new JuniperFlattener(header);
          ParseTreeWalker walker = new BatfishParseTreeWalker(parser);
          try {
            walker.walk(flattener, tree);
          } catch (BatfishParseException e) {
            warnings.setErrorDetails(e.getErrorDetails());
            throw new BatfishException(
                String.format("Error flattening %s config", format.getVendorString()), e);
          }
          return flattener;
        }

      case VYOS:
        {
          VyosCombinedParser parser = new VyosCombinedParser(input, settings);
          ParserRuleContext tree = parse(parser, logger, settings);
          VyosFlattener flattener = new VyosFlattener(header);
          ParseTreeWalker walker = new BatfishParseTreeWalker(parser);
          try {
            walker.walk(flattener, tree);
          } catch (BatfishParseException e) {
            warnings.setErrorDetails(e.getErrorDetails());
            throw new BatfishException(
                String.format("Error flattening %s config", format.getVendorString()), e);
          }
          return flattener;
        }

        // $CASES-OMITTED$
      default:
        throw new BatfishException("Invalid format for flattening");
    }
  }

  public static void initTestrigSettings(Settings settings) {
    SnapshotId testrig = settings.getTestrig();
    Path containerDir = settings.getStorageBase().resolve(settings.getContainer().getId());
    if (testrig != null) {
      applyBaseDir(settings.getBaseTestrigSettings(), containerDir, testrig);
      SnapshotId deltaTestrig = settings.getDeltaTestrig();
      TestrigSettings deltaTestrigSettings = settings.getDeltaTestrigSettings();
      if (deltaTestrig != null) {
        applyBaseDir(deltaTestrigSettings, containerDir, deltaTestrig);
      }
      if (settings.getDiffActive()) {
        settings.setActiveTestrigSettings(settings.getDeltaTestrigSettings());
      } else {
        settings.setActiveTestrigSettings(settings.getBaseTestrigSettings());
      }
    } else {
      throw new CleanBatfishException("Must supply argument to -" + BfConsts.ARG_TESTRIG);
    }
  }

  /**
   * Reads the files in the given directory (recursively) and returns a map from each file's {@link
   * Path} to its contents.
   *
   * <p>Temporary files (files start with {@code .} are omitted from the returned list.
   *
   * <p>This method follows all symbolic links.
   */
  static SortedMap<Path, String> readAllFiles(Path directory, BatfishLogger logger) {
    try (Stream<Path> paths = Files.walk(directory, FileVisitOption.FOLLOW_LINKS)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> !path.getFileName().toString().startsWith("."))
          .map(
              path -> {
                logger.debugf("Reading: \"%s\"\n", path);
                String fileText = CommonUtil.readFile(path.toAbsolutePath());
                if (!fileText.isEmpty()) {
                  // Adding a trailing newline helps EOF in some parsers.
                  fileText += '\n';
                }
                return new SimpleEntry<>(path, fileText);
              })
          .collect(
              ImmutableSortedMap.toImmutableSortedMap(
                  Ordering.natural(), SimpleEntry::getKey, SimpleEntry::getValue));
    } catch (IOException e) {
      throw new BatfishException("Failed to walk path: " + directory, e);
    }
  }

  public static void logWarnings(BatfishLogger logger, Warnings warnings) {
    for (Warning warning : warnings.getRedFlagWarnings()) {
      logger.redflag(logWarningsHelper(warning));
    }
    for (Warning warning : warnings.getUnimplementedWarnings()) {
      logger.unimplemented(logWarningsHelper(warning));
    }
    for (Warning warning : warnings.getPedanticWarnings()) {
      logger.pedantic(logWarningsHelper(warning));
    }
  }

  private static String logWarningsHelper(Warning warning) {
    return "   " + warning.getTag() + ": " + warning.getText() + "\n";
  }

  /**
   * Returns the parse tree for the given parser, logging to the given logger and using the given
   * settings to control the parse tree printing, if applicable.
   */
  public static ParserRuleContext parse(
      BatfishCombinedParser<?, ?> parser, BatfishLogger logger, GrammarSettings settings) {
    ParserRuleContext tree;
    try {
      tree = parser.parse();
    } catch (BatfishException e) {
      throw new ParserBatfishException("Parser error", e);
    }
    List<String> errors = parser.getErrors();
    int numErrors = errors.size();
    if (numErrors > 0) {
      throw new ParserBatfishException("Parser error(s)", errors);
    } else if (!settings.getPrintParseTree()) {
      logger.info("OK\n");
    } else {
      logger.info("OK, PRINTING PARSE TREE:\n");
      logger.info(
          ParseTreePrettyPrinter.print(tree, parser, settings.getPrintParseTreeLineNums())
              + "\n\n");
    }
    return tree;
  }

  private final Map<String, AnswererCreator> _answererCreators;

  private TestrigSettings _baseTestrigSettings;

  private SortedMap<BgpTableFormat, BgpTablePlugin> _bgpTablePlugins;

  private final Cache<NetworkSnapshot, SortedMap<String, Configuration>> _cachedConfigurations;

  private final Cache<NetworkSnapshot, DataPlane> _cachedDataPlanes;

  private final Map<NetworkSnapshot, SortedMap<String, BgpAdvertisementsByVrf>>
      _cachedEnvironmentBgpTables;

  private final Map<NetworkSnapshot, SortedMap<String, RoutesByVrf>>
      _cachedEnvironmentRoutingTables;

  private TestrigSettings _deltaTestrigSettings;

  private Set<ExternalBgpAdvertisementPlugin> _externalBgpAdvertisementPlugins;

  private IdResolver _idResolver;

  private BatfishLogger _logger;

  private Settings _settings;

  private final StorageProvider _storage;

  // this variable is used communicate with parent thread on how the job
  // finished (null if job finished successfully)
  private String _terminatingExceptionMessage;

  private TestrigSettings _testrigSettings;

  private final List<TestrigSettings> _testrigSettingsStack;

  private Map<String, DataPlanePlugin> _dataPlanePlugins;

  private final TopologyProvider _topologyProvider;

  public Batfish(
      Settings settings,
      Cache<NetworkSnapshot, SortedMap<String, Configuration>> cachedConfigurations,
      Cache<NetworkSnapshot, DataPlane> cachedDataPlanes,
      Map<NetworkSnapshot, SortedMap<String, BgpAdvertisementsByVrf>> cachedEnvironmentBgpTables,
      Map<NetworkSnapshot, SortedMap<String, RoutesByVrf>> cachedEnvironmentRoutingTables,
      @Nullable StorageProvider alternateStorageProvider,
      @Nullable IdResolver alternateIdResolver) {
    _settings = settings;
    _bgpTablePlugins = new TreeMap<>();
    _cachedConfigurations = cachedConfigurations;
    _cachedDataPlanes = cachedDataPlanes;
    _cachedEnvironmentBgpTables = cachedEnvironmentBgpTables;
    _cachedEnvironmentRoutingTables = cachedEnvironmentRoutingTables;
    _externalBgpAdvertisementPlugins = new TreeSet<>();
    _testrigSettings = settings.getActiveTestrigSettings();
    _baseTestrigSettings = settings.getBaseTestrigSettings();
    _logger = _settings.getLogger();
    _deltaTestrigSettings = settings.getDeltaTestrigSettings();
    _terminatingExceptionMessage = null;
    _answererCreators = new HashMap<>();
    _testrigSettingsStack = new ArrayList<>();
    _dataPlanePlugins = new HashMap<>();
    _storage =
        alternateStorageProvider != null
            ? alternateStorageProvider
            : new FileBasedStorage(_settings.getStorageBase(), _logger, this::newBatch);
    _idResolver =
        alternateIdResolver != null
            ? alternateIdResolver
            : new FileBasedIdResolver(_settings.getStorageBase());
    _topologyProvider = new TopologyProviderImpl(this, _storage);
    loadPlugins();
  }

  /**
   * A shallow wrapper for {@link Files#createDirectories} that throws a {@link BatfishException}
   * instead of {@link IOException}.
   *
   * @throws BatfishException if there is an error creating directories.
   */
  private static void createDirectories(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new BatfishException(
          "Could not create directories leading up to and including '" + path + "'", e);
    }
  }

  private Answer analyze() {
    try {
      Answer answer = new Answer();
      AnswerSummary summary = new AnswerSummary();
      AnalysisId analysisName = _settings.getAnalysisName();
      NetworkId containerName = _settings.getContainer();
      RunAnalysisAnswerElement ae = new RunAnalysisAnswerElement();
      _idResolver
          .listQuestions(containerName, analysisName)
          .forEach(
              questionName -> {
                QuestionId questionId =
                    _idResolver.getQuestionId(questionName, containerName, analysisName);
                _settings.setQuestionName(questionId);
                Answer currentAnswer;
                try (ActiveSpan analysisQuestionSpan =
                    GlobalTracer.get()
                        .buildSpan("Getting answer to analysis question")
                        .startActive()) {
                  assert analysisQuestionSpan != null; // make span not show up as unused
                  analysisQuestionSpan.setTag("analysis-name", analysisName.getId());
                  currentAnswer = answer();
                }
                // Ensuring that question was parsed successfully
                if (currentAnswer.getQuestion() != null) {
                  try {
                    // TODO: This can be represented much cleanly and easily with a Json
                    _logger.infof(
                        "Ran question:%s from analysis:%s in container:%s; work-id:%s, status:%s, "
                            + "computed dataplane:%s, parameters:%s\n",
                        questionName,
                        analysisName,
                        containerName,
                        getTaskId(),
                        currentAnswer.getSummary().getNumFailed() > 0 ? "failed" : "passed",
                        currentAnswer.getQuestion().getDataPlane(),
                        BatfishObjectMapper.writeString(
                            currentAnswer.getQuestion().getInstance().getVariables()));
                  } catch (JsonProcessingException e) {
                    throw new BatfishException(
                        String.format(
                            "Error logging question %s in analysis %s", questionName, analysisName),
                        e);
                  }
                }
                try {
                  outputAnswer(currentAnswer);
                  outputAnswerMetadata(currentAnswer);
                  ae.getAnswers().put(questionName, currentAnswer);
                } catch (Exception e) {
                  Answer errorAnswer = new Answer();
                  errorAnswer.addAnswerElement(
                      new BatfishStackTrace(new BatfishException("Failed to output answer", e)));
                  ae.getAnswers().put(questionName, errorAnswer);
                }
                ae.getAnswers().put(questionName, currentAnswer);
                summary.combine(currentAnswer.getSummary());
              });

      answer.addAnswerElement(ae);
      answer.setSummary(summary);
      return answer;
    } finally {
      // ensure question name is null so logger does not try to write analysis answer into a
      // question's answer folder
      _settings.setQuestionName(null);
    }
  }

  public Answer answer() {
    Question question = null;

    // return right away if we cannot parse the question successfully
    try (ActiveSpan parseQuestionSpan =
        GlobalTracer.get().buildSpan("Parse question").startActive()) {
      assert parseQuestionSpan != null; // avoid not used warning
      String rawQuestionStr;
      try {
        rawQuestionStr =
            _storage.loadQuestion(
                _settings.getContainer(), _settings.getQuestionName(), _settings.getAnalysisName());
      } catch (Exception e) {
        Answer answer = new Answer();
        BatfishException exception = new BatfishException("Could not read question", e);
        answer.setStatus(AnswerStatus.FAILURE);
        answer.addAnswerElement(exception.getBatfishStackTrace());
        return answer;
      }
      try {
        question = Question.parseQuestion(rawQuestionStr);
      } catch (Exception e) {
        Answer answer = new Answer();
        BatfishException exception = new BatfishException("Could not parse question", e);
        answer.setStatus(AnswerStatus.FAILURE);
        answer.addAnswerElement(exception.getBatfishStackTrace());
        return answer;
      }
    }

    if (GlobalTracer.get().activeSpan() != null) {
      ActiveSpan activeSpan = GlobalTracer.get().activeSpan();
      activeSpan
          .setTag("container-name", getContainerName().getId())
          .setTag("testrig_name", getTestrigName().getId());
      if (question.getInstance() != null) {
        activeSpan.setTag("question-name", question.getInstance().getInstanceName());
      }
    }

    if (_settings.getDifferential()) {
      question.setDifferential(true);
    }
    boolean dp = question.getDataPlane();
    boolean diff = question.getDifferential();
    boolean diffActive = _settings.getDiffActive() && !diff;
    _settings.setDiffActive(diffActive);
    _settings.setDiffQuestion(diff);

    // Ensures configurations are parsed and ready
    loadConfigurations();

    try (ActiveSpan initQuestionEnvSpan =
        GlobalTracer.get().buildSpan("Init question environment").startActive()) {
      assert initQuestionEnvSpan != null; // avoid not used warning
      prepareToAnswerQuestions(diff, diffActive, dp);
    }

    AnswerElement answerElement = null;
    BatfishException exception = null;
    try (ActiveSpan getAnswerSpan = GlobalTracer.get().buildSpan("Get answer").startActive()) {
      assert getAnswerSpan != null; // avoid not used warning
      if (question.getDifferential()) {
        answerElement = Answerer.create(question, this).answerDiff();
      } else {
        answerElement = Answerer.create(question, this).answer();
      }
    } catch (Exception e) {
      exception = new BatfishException("Failed to answer question", e);
    }

    Answer answer = new Answer();
    answer.setQuestion(question);

    if (exception == null) {
      // success
      answer.setStatus(AnswerStatus.SUCCESS);
      answer.addAnswerElement(answerElement);
    } else {
      // failure
      answer.setStatus(AnswerStatus.FAILURE);
      answer.addAnswerElement(exception.getBatfishStackTrace());
    }
    return answer;
  }

  private static void computeAggregatedInterfaceBandwidths(Map<String, Interface> interfaces) {
    // Set bandwidths for aggregate interfaces
    interfaces.values().stream()
        .filter(iface -> iface.getInterfaceType() == InterfaceType.AGGREGATED)
        .forEach(
            iface -> {
              /* If interface has dependencies, bandwidth should be sum of their bandwidths. */
              if (!iface.getDependencies().isEmpty()) {
                iface.setBandwidth(
                    iface.getDependencies().stream()
                        .map(dependency -> interfaces.get(dependency.getInterfaceName()))
                        .filter(Objects::nonNull)
                        .filter(Interface::getActive)
                        .map(Interface::getBandwidth)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .sum());
              } else {
                /* Bandwidth should be sum of bandwidth of channel-group members. */
                iface.setBandwidth(
                    iface.getChannelGroupMembers().stream()
                        .mapToDouble(ifaceName -> interfaces.get(ifaceName).getBandwidth())
                        .sum());
              }
            });
    // Now that aggregate interfaces have bandwidths, set bandwidths for aggregate child interfaces
    interfaces.values().stream()
        .filter(iface -> iface.getInterfaceType() == InterfaceType.AGGREGATE_CHILD)
        .forEach(
            iface -> {
              /*
              Bandwidth for aggregate child interfaces (e.g. units) should be inherited from parent.
              */
              double bandwidth =
                  iface.getDependencies().stream()
                      .filter(d -> d.getType() == DependencyType.BIND)
                      .findFirst()
                      .map(Dependency::getInterfaceName)
                      .map(interfaces::get)
                      .map(Interface::getBandwidth)
                      .orElse(0.0);
              iface.setBandwidth(bandwidth);
            });
  }

  public static Warnings buildWarnings(Settings settings) {
    return new Warnings(
        settings.getPedanticRecord() && settings.getLogger().isActive(BatfishLogger.LEVEL_PEDANTIC),
        settings.getRedFlagRecord() && settings.getLogger().isActive(BatfishLogger.LEVEL_REDFLAG),
        settings.getUnimplementedRecord()
            && settings.getLogger().isActive(BatfishLogger.LEVEL_UNIMPLEMENTED));
  }

  public static void checkDataPlane(TestrigSettings testrigSettings) {
    if (!Files.exists(testrigSettings.getDataPlanePath())) {
      throw new CleanBatfishException(
          "Missing data plane for testrig: \"" + testrigSettings.getName() + "\"\n");
    }
  }

  @Override
  public void checkSnapshotOutputReady() {
    checkSnapshotOutputReady(_testrigSettings);
  }

  public void checkSnapshotOutputReady(TestrigSettings testrigSettings) {
    checkState(
        outputExists(testrigSettings),
        "Output directory does not exist for snapshot %s",
        testrigSettings.getName());
  }

  @Override
  public DataPlaneAnswerElement computeDataPlane() {
    checkSnapshotOutputReady();
    ComputeDataPlaneResult result = getDataPlanePlugin().computeDataPlane();
    saveDataPlane(result);
    return result._answerElement;
  }

  /* Write the dataplane to disk and cache, and write the answer element to disk.
   */
  private void saveDataPlane(ComputeDataPlaneResult result) {
    _cachedDataPlanes.put(getNetworkSnapshot(), result._dataPlane);

    _logger.resetTimer();
    newBatch("Writing data plane to disk", 0);
    try (ActiveSpan writeDataplane =
        GlobalTracer.get().buildSpan("Writing data plane").startActive()) {
      assert writeDataplane != null; // avoid unused warning
      serializeObject(result._dataPlane, _testrigSettings.getDataPlanePath());
      serializeObject(result._answerElement, _testrigSettings.getDataPlaneAnswerPath());
      TopologyContainer topologies = result._topologies;
      NetworkSnapshot networkSnapshot = getNetworkSnapshot();
      _storage.storeBgpTopology(topologies.getBgpTopology(), networkSnapshot);
      _storage.storeEigrpTopology(topologies.getEigrpTopology(), networkSnapshot);
      _storage.storeLayer2Topology(topologies.getLayer2Topology(), networkSnapshot);
      _storage.storeLayer3Topology(topologies.getLayer3Topology(), networkSnapshot);
      _storage.storeOspfTopology(topologies.getOspfTopology(), networkSnapshot);
      _storage.storeVxlanTopology(topologies.getVxlanTopology(), networkSnapshot);
    } catch (IOException e) {
      throw new BatfishException("Failed to save data plane", e);
    }
    _logger.printElapsedTime();
  }

  private void computeEnvironmentBgpTables() {
    Path outputPath = _testrigSettings.getSerializeEnvironmentBgpTablesPath();
    Path inputPath = _testrigSettings.getEnvironmentBgpTablesPath();
    serializeEnvironmentBgpTables(inputPath, outputPath);
  }

  private void computeEnvironmentRoutingTables() {
    Path outputPath = _testrigSettings.getSerializeEnvironmentRoutingTablesPath();
    Path inputPath = _testrigSettings.getEnvironmentRoutingTablesPath();
    serializeEnvironmentRoutingTables(inputPath, outputPath);
  }

  private Map<String, Configuration> convertConfigurations(
      Map<String, GenericConfigObject> vendorConfigurations,
      ConvertConfigurationAnswerElement answerElement) {
    _logger.info("\n*** CONVERTING VENDOR CONFIGURATIONS TO INDEPENDENT FORMAT ***\n");
    _logger.resetTimer();
    Map<String, Configuration> configurations = new TreeMap<>();
    List<ConvertConfigurationJob> jobs = new ArrayList<>();
    for (Entry<String, GenericConfigObject> config : vendorConfigurations.entrySet()) {
      GenericConfigObject vc = config.getValue();
      ConvertConfigurationJob job = new ConvertConfigurationJob(_settings, vc, config.getKey());
      jobs.add(job);
    }
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        configurations,
        answerElement,
        _settings.getHaltOnConvertError(),
        "Convert configurations to vendor-independent format");
    _logger.printElapsedTime();
    return configurations;
  }

  private boolean dataPlaneDependenciesExist(TestrigSettings testrigSettings) {
    Path dpPath = testrigSettings.getDataPlaneAnswerPath();
    return Files.exists(dpPath);
  }

  @Override
  public boolean debugFlagEnabled(String flag) {
    return _settings.debugFlagEnabled(flag);
  }

  private SortedMap<String, BgpAdvertisementsByVrf> deserializeEnvironmentBgpTables(
      Path serializeEnvironmentBgpTablesPath) {
    _logger.info("\n*** DESERIALIZING ENVIRONMENT BGP TABLES ***\n");
    _logger.resetTimer();
    Map<Path, String> namesByPath = new TreeMap<>();
    try (DirectoryStream<Path> serializedBgpTables =
        Files.newDirectoryStream(serializeEnvironmentBgpTablesPath)) {
      for (Path serializedBgpTable : serializedBgpTables) {
        String name = serializedBgpTable.getFileName().toString();
        namesByPath.put(serializedBgpTable, name);
      }
    } catch (IOException e) {
      throw new BatfishException("Error reading serialized BGP tables directory", e);
    }
    SortedMap<String, BgpAdvertisementsByVrf> bgpTables =
        deserializeObjects(namesByPath, BgpAdvertisementsByVrf.class);
    _logger.printElapsedTime();
    return bgpTables;
  }

  private SortedMap<String, RoutesByVrf> deserializeEnvironmentRoutingTables(
      Path serializeEnvironmentRoutingTablesPath) {
    _logger.info("\n*** DESERIALIZING ENVIRONMENT ROUTING TABLES ***\n");
    _logger.resetTimer();
    Map<Path, String> namesByPath = new TreeMap<>();
    try (DirectoryStream<Path> serializedRoutingTables =
        Files.newDirectoryStream(serializeEnvironmentRoutingTablesPath)) {
      for (Path serializedRoutingTable : serializedRoutingTables) {
        String name = serializedRoutingTable.getFileName().toString();
        namesByPath.put(serializedRoutingTable, name);
      }
    } catch (IOException e) {
      throw new BatfishException("Error reading serialized routing tables directory", e);
    }
    SortedMap<String, RoutesByVrf> routingTables =
        deserializeObjects(namesByPath, RoutesByVrf.class);
    _logger.printElapsedTime();
    return routingTables;
  }

  /**
   * Deserialize a bunch of objects
   *
   * @param namesByPath Mapping of object paths to their names
   * @param outputClass the class type for {@link S}
   * @param <S> desired type of objects
   * @return a map of objects keyed by their name (from {@code namesByPath})
   */
  public <S extends Serializable> SortedMap<String, S> deserializeObjects(
      Map<Path, String> namesByPath, Class<S> outputClass) {
    String outputClassName = outputClass.getName();
    BatfishLogger logger = getLogger();
    AtomicInteger readCompleted =
        newBatch(
            "Reading, unpacking, and deserializing files containing '"
                + outputClassName
                + "' instances",
            namesByPath.size());
    return namesByPath
        .entrySet()
        .parallelStream()
        .map(
            e -> {
              logger.debugf(
                  "Reading and unzipping: %s '%s' from %s%n",
                  outputClassName, e.getValue(), e.getKey());
              S object = deserializeObject(e.getKey(), outputClass);
              logger.debug(" ...OK\n");
              readCompleted.incrementAndGet();
              return new SimpleImmutableEntry<>(e.getValue(), object);
            })
        .collect(
            ImmutableSortedMap.toImmutableSortedMap(
                String::compareTo, Entry::getKey, Entry::getValue));
  }

  public Map<String, GenericConfigObject> deserializeVendorConfigurations(
      Path serializedVendorConfigPath) {
    _logger.info("\n*** DESERIALIZING VENDOR CONFIGURATION STRUCTURES ***\n");
    _logger.resetTimer();
    Map<Path, String> namesByPath = new TreeMap<>();
    try (DirectoryStream<Path> serializedConfigs =
        Files.newDirectoryStream(serializedVendorConfigPath)) {
      for (Path serializedConfig : serializedConfigs) {
        String name = serializedConfig.getFileName().toString();
        namesByPath.put(serializedConfig, name);
      }
    } catch (IOException e) {
      throw new BatfishException("Error reading vendor configs directory", e);
    }
    Map<String, GenericConfigObject> vendorConfigurations =
        deserializeObjects(namesByPath, GenericConfigObject.class);
    _logger.printElapsedTime();
    return vendorConfigurations;
  }

  private void disableUnusableVlanInterfaces(Map<String, Configuration> configurations) {
    for (Configuration c : configurations.values()) {
      String hostname = c.getHostname();

      Map<Integer, Interface> vlanInterfaces = new HashMap<>();
      Map<Integer, Integer> vlanMemberCounts = new HashMap<>();
      Set<Interface> nonVlanInterfaces = new HashSet<>();
      Integer vlanNumber = null;
      // Populate vlanInterface and nonVlanInterfaces, and initialize
      // vlanMemberCounts:
      for (Interface iface : c.getAllInterfaces().values()) {
        if ((iface.getInterfaceType() == InterfaceType.VLAN)
            && ((vlanNumber = CommonUtil.getInterfaceVlanNumber(iface.getName())) != null)) {
          vlanInterfaces.put(vlanNumber, iface);
          vlanMemberCounts.put(vlanNumber, 0);
        } else {
          nonVlanInterfaces.add(iface);
        }
      }
      // Update vlanMemberCounts:
      for (Interface iface : nonVlanInterfaces) {
        IntegerSpace.Builder vlans = IntegerSpace.builder();
        if (iface.getSwitchportMode() == SwitchportMode.TRUNK) { // vlan trunked interface
          IntegerSpace allowed = iface.getAllowedVlans();
          if (!allowed.isEmpty()) {
            // Explicit list of allowed VLANs
            vlans.including(allowed);
          } else {
            // No explicit list, so all VLANs are allowed.
            vlanInterfaces.keySet().forEach(vlans::including);
          }
          // Add the native VLAN as well.
          vlanNumber = iface.getNativeVlan();
          if (vlanNumber != null) {
            vlans.including(vlanNumber);
          }
        } else if (iface.getSwitchportMode() == SwitchportMode.ACCESS) { // access mode ACCESS
          vlanNumber = iface.getAccessVlan();
          if (vlanNumber != null) {
            vlans.including(vlanNumber);
          }
          // Any other Switch Port mode is unsupported
        } else if (iface.getSwitchportMode() != SwitchportMode.NONE) {
          _logger.warnf(
              "WARNING: Unsupported switch port mode %s, assuming no VLANs allowed: \"%s:%s\"\n",
              iface.getSwitchportMode(), hostname, iface.getName());
        }

        vlans.build().stream()
            .forEach(
                vlanId -> vlanMemberCounts.compute(vlanId, (k, v) -> (v == null) ? 1 : (v + 1)));
      }
      // Disable all "normal" vlan interfaces with zero member counts:
      SubRange normalVlanRange = c.getNormalVlanRange();
      for (Map.Entry<Integer, Integer> entry : vlanMemberCounts.entrySet()) {
        if (entry.getValue() == 0) {
          vlanNumber = entry.getKey();
          if ((vlanNumber >= normalVlanRange.getStart())
              && (vlanNumber <= normalVlanRange.getEnd())) {
            Interface iface = vlanInterfaces.get(vlanNumber);
            if ((iface != null) && iface.getAutoState()) {
              _logger.warnf(
                  "WARNING: Disabling unusable vlan interface because no switch port is assigned "
                      + "to it: \"%s:%d\"\n",
                  hostname, vlanNumber);
              iface.blacklist();
            }
          }
        }
      }
    }
  }

  private boolean environmentBgpTablesExist(TestrigSettings testrigSettings) {
    Path answerPath = testrigSettings.getParseEnvironmentBgpTablesAnswerPath();
    return Files.exists(answerPath);
  }

  private boolean outputExists(TestrigSettings testrigSettings) {
    return testrigSettings.getOutputPath().toFile().exists();
  }

  private boolean environmentRoutingTablesExist(TestrigSettings testrigSettings) {
    Path answerPath = testrigSettings.getParseEnvironmentRoutingTablesAnswerPath();
    return Files.exists(answerPath);
  }

  public void flatten(Path inputPath, Path outputPath) {
    _logger.info("\n*** READING FILES TO FLATTEN ***\n");
    Map<Path, String> configurationData =
        readAllFiles(inputPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR), _logger);

    Map<Path, String> outputConfigurationData = new TreeMap<>();
    Path outputConfigDir = outputPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR);
    createDirectories(outputConfigDir);
    _logger.info("\n*** FLATTENING TEST RIG ***\n");
    _logger.resetTimer();
    List<FlattenVendorConfigurationJob> jobs = new ArrayList<>();
    for (Entry<Path, String> configFile : configurationData.entrySet()) {
      Path inputFile = configFile.getKey();
      String fileText = configFile.getValue();
      Warnings warnings = buildWarnings(_settings);
      String name = inputFile.getFileName().toString();
      Path outputFile = outputConfigDir.resolve(name);
      FlattenVendorConfigurationJob job =
          new FlattenVendorConfigurationJob(_settings, fileText, inputFile, outputFile, warnings);
      jobs.add(job);
    }
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        outputConfigurationData,
        new FlattenVendorConfigurationAnswerElement(),
        _settings.getFlatten() || _settings.getHaltOnParseError(),
        "Flatten configurations");
    _logger.printElapsedTime();
    for (Entry<Path, String> e : outputConfigurationData.entrySet()) {
      Path outputFile = e.getKey();
      String flatConfigText = e.getValue();
      String outputFileAsString = outputFile.toString();
      _logger.debugf("Writing config to \"%s\"...", outputFileAsString);
      CommonUtil.writeFile(outputFile, flatConfigText);
      _logger.debug("OK\n");
    }
  }

  public Map<String, Configuration> getConfigurations(
      Path serializedVendorConfigPath, ConvertConfigurationAnswerElement answerElement) {
    Map<String, GenericConfigObject> vendorConfigurations =
        deserializeVendorConfigurations(serializedVendorConfigPath);
    Map<String, Configuration> configurations =
        convertConfigurations(vendorConfigurations, answerElement);

    identifyDeviceTypes(configurations.values());
    return configurations;
  }

  @Override
  public NetworkId getContainerName() {
    return _settings.getContainer();
  }

  @Override
  public DataPlanePlugin getDataPlanePlugin() {
    DataPlanePlugin plugin = _dataPlanePlugins.get(_settings.getDataPlaneEngineName());
    if (plugin == null) {
      throw new BatfishException(
          String.format(
              "Dataplane engine %s is unavailable or unsupported",
              _settings.getDataPlaneEngineName()));
    }
    return plugin;
  }

  @Override
  public String getDifferentialFlowTag() {
    return DIFFERENTIAL_FLOW_TAG;
  }

  @Override
  public Environment getEnvironment() {
    // TODO: delete entirely
    return new Environment(
        _settings.getSnapshotName(),
        ImmutableSortedSet.of(),
        ImmutableSortedSet.of(),
        ImmutableSortedSet.of(),
        null,
        null,
        null);
  }

  private SortedMap<String, BgpAdvertisementsByVrf> getEnvironmentBgpTables(
      Path inputPath, ParseEnvironmentBgpTablesAnswerElement answerElement) {
    if (Files.exists(inputPath.getParent()) && !Files.exists(inputPath)) {
      return new TreeMap<>();
    }
    _logger.info("\n*** READING Environment BGP Tables ***\n");
    SortedMap<Path, String> inputData = readAllFiles(inputPath, _logger);
    SortedMap<String, BgpAdvertisementsByVrf> bgpTables =
        parseEnvironmentBgpTables(inputData, answerElement);
    return bgpTables;
  }

  private SortedMap<String, RoutesByVrf> getEnvironmentRoutingTables(
      Path inputPath, ParseEnvironmentRoutingTablesAnswerElement answerElement) {
    if (Files.exists(inputPath.getParent()) && !Files.exists(inputPath)) {
      return new TreeMap<>();
    }
    _logger.info("\n*** READING Environment Routing Tables ***\n");
    SortedMap<Path, String> inputData = readAllFiles(inputPath, _logger);
    SortedMap<String, RoutesByVrf> routingTables =
        parseEnvironmentRoutingTables(inputData, answerElement);
    return routingTables;
  }

  @Override
  public String getFlowTag() {
    return getFlowTag(_testrigSettings);
  }

  public String getFlowTag(TestrigSettings testrigSettings) {
    if (testrigSettings == _deltaTestrigSettings) {
      return Flow.DELTA_FLOW_TAG;
    } else if (testrigSettings == _baseTestrigSettings) {
      return Flow.BASE_FLOW_TAG;
    } else {
      throw new BatfishException("Could not determine flow tag");
    }
  }

  @Nonnull
  private Map<String, Configuration> getIspConfigurations(
      Map<String, Configuration> configurations, Map<String, Warnings> warningsByHost) {
    NetworkSnapshot networkSnapshot = getNetworkSnapshot();
    IspConfiguration ispConfiguration =
        _storage.loadIspConfiguration(networkSnapshot.getNetwork(), networkSnapshot.getSnapshot());
    if (ispConfiguration == null) {
      return ImmutableMap.of();
    }
    Warnings warnings =
        warningsByHost.computeIfAbsent(
            IspModelingUtils.INTERNET_HOST_NAME, k -> buildWarnings(_settings));
    return IspModelingUtils.getInternetAndIspNodes(
        configurations, ispConfiguration, _logger, warnings);
  }

  @Override
  public BatfishLogger getLogger() {
    return _logger;
  }

  /**
   * Gets the {@link NodeRolesData} for the testrig
   *
   * @return The {@link NodeRolesData} object.
   */
  @Override
  public NodeRolesData getNodeRolesData() {
    try {
      NetworkId networkId = _settings.getContainer();
      if (!_idResolver.hasNetworkNodeRolesId(networkId)) {
        return null;
      }
      NodeRolesId nodeRolesId = _idResolver.getNetworkNodeRolesId(networkId);
      return BatfishObjectMapper.mapper()
          .readValue(_storage.loadNodeRoles(nodeRolesId), NodeRolesData.class);
    } catch (IOException e) {
      _logger.errorf("Could not read roles data: %s", e);
      return null;
    }
  }

  /**
   * Gets the {@link NodeRoleDimension} object given dimension name. If {@code dimension} is null,
   * returns the default dimension.
   *
   * @param dimension The dimension name
   * @return An {@link Optional} that has the requested NodeRoleDimension or empty otherwise.
   */
  @Override
  public Optional<NodeRoleDimension> getNodeRoleDimension(@Nullable String dimension) {
    NodeRolesData nodeRolesData = getNodeRolesData();
    return nodeRolesData.getNodeRoleDimension(dimension);
  }

  /**
   * Returns the {@link MajorIssueConfig} for the given major issue type.
   *
   * <p>If the corresponding file is not found or it cannot be deserealized, return an empty object.
   */
  @Override
  public MajorIssueConfig getMajorIssueConfig(String majorIssueType) {
    IssueSettingsId id = _idResolver.getIssueSettingsId(majorIssueType, _settings.getContainer());
    if (id == null) {
      return new MajorIssueConfig(majorIssueType, ImmutableMap.of());
    }
    MajorIssueConfig loaded = _storage.loadMajorIssueConfig(_settings.getContainer(), id);
    return loaded != null ? loaded : new MajorIssueConfig(majorIssueType, ImmutableMap.of());
  }

  @Override
  public Map<String, String> getQuestionTemplates(boolean verbose) {
    if (_settings.getCoordinatorHost() == null) {
      throw new BatfishException("Cannot get question templates: coordinator host is not set");
    }
    String protocol = _settings.getSslDisable() ? "http" : "https";
    String url =
        String.format(
            "%s://%s:%s%s/%s",
            protocol,
            _settings.getCoordinatorHost(),
            _settings.getCoordinatorPoolPort(),
            CoordConsts.SVC_CFG_POOL_MGR,
            CoordConsts.SVC_RSC_POOL_GET_QUESTION_TEMPLATES);
    Map<String, String> params = new HashMap<>();
    params.put(CoordConsts.SVC_KEY_VERSION, Version.getVersion());
    params.put(CoordConstsV2.QP_VERBOSE, String.valueOf(verbose));

    JSONObject response = (JSONObject) Driver.talkToCoordinator(url, params, _logger);
    if (response == null) {
      throw new BatfishException("Could not get question templates: Got null response");
    }
    if (!response.has(CoordConsts.SVC_KEY_QUESTION_LIST)) {
      throw new BatfishException("Could not get question templates: Response lacks question list");
    }

    try {
      Map<String, String> templates =
          BatfishObjectMapper.mapper()
              .readValue(
                  response.get(CoordConsts.SVC_KEY_QUESTION_LIST).toString(),
                  new TypeReference<Map<String, String>>() {});
      return templates;
    } catch (JSONException | IOException e) {
      throw new BatfishException("Could not cast response to Map: ", e);
    }
  }

  /** Gets the {@link ReferenceLibrary} for the network */
  @Override
  public ReferenceLibrary getReferenceLibraryData() {
    Path libraryPath =
        _settings
            .getStorageBase()
            .resolve(_settings.getContainer().getId())
            .resolve(BfConsts.RELPATH_REFERENCE_LIBRARY_PATH);
    try {
      return ReferenceLibrary.read(libraryPath);
    } catch (IOException e) {
      _logger.errorf("Could not read reference library data from %s: %s", libraryPath, e);
      return null;
    }
  }

  public Settings getSettings() {
    return _settings;
  }

  @Override
  public ImmutableConfiguration getSettingsConfiguration() {
    return _settings.getImmutableConfiguration();
  }

  @Override
  public NetworkSnapshot getNetworkSnapshot() {
    return new NetworkSnapshot(_settings.getContainer(), _testrigSettings.getName());
  }

  @Override
  public String getTaskId() {
    return _settings.getTaskId();
  }

  public String getTerminatingExceptionMessage() {
    return _terminatingExceptionMessage;
  }

  @Override
  public SnapshotId getTestrigName() {
    return _testrigSettings.getName();
  }

  @Nonnull
  @Override
  public TopologyProvider getTopologyProvider() {
    return _topologyProvider;
  }

  @Override
  public PluginClientType getType() {
    return PluginClientType.BATFISH;
  }

  @Override
  public InitInfoAnswerElement initInfo(boolean summary, boolean verboseError) {
    ParseVendorConfigurationAnswerElement parseAnswer = loadParseVendorConfigurationAnswerElement();
    InitInfoAnswerElement answerElement = mergeParseAnswer(summary, verboseError, parseAnswer);
    mergeConvertAnswer(summary, verboseError, answerElement);
    _logger.info(answerElement.toString());
    return answerElement;
  }

  @Override
  public InitInfoAnswerElement initInfoBgpAdvertisements(boolean summary, boolean verboseError) {
    ParseEnvironmentBgpTablesAnswerElement parseAnswer =
        loadParseEnvironmentBgpTablesAnswerElement();
    InitInfoAnswerElement answerElement = mergeParseAnswer(summary, verboseError, parseAnswer);
    _logger.info(answerElement.toString());
    return answerElement;
  }

  @Override
  public InitInfoAnswerElement initInfoRoutes(boolean summary, boolean verboseError) {
    ParseEnvironmentRoutingTablesAnswerElement parseAnswer =
        loadParseEnvironmentRoutingTablesAnswerElement();
    InitInfoAnswerElement answerElement = mergeParseAnswer(summary, verboseError, parseAnswer);
    _logger.info(answerElement.toString());
    return answerElement;
  }

  private void prepareToAnswerQuestions(boolean dp) {
    if (!outputExists(_testrigSettings)) {
      createDirectories(_testrigSettings.getOutputPath());
    }
    if (!environmentBgpTablesExist(_testrigSettings)) {
      computeEnvironmentBgpTables();
    }
    if (!environmentRoutingTablesExist(_testrigSettings)) {
      computeEnvironmentRoutingTables();
    }
    if (dp) {
      if (!dataPlaneDependenciesExist(_testrigSettings)) {
        computeDataPlane();
      }
    }
  }

  private void prepareToAnswerQuestions(boolean diff, boolean diffActive, boolean dp) {
    if (diff || !diffActive) {
      pushBaseSnapshot();
      prepareToAnswerQuestions(dp);
      popSnapshot();
    }
    if (diff || diffActive) {
      pushDeltaSnapshot();
      prepareToAnswerQuestions(dp);
      popSnapshot();
    }
  }

  @Override
  public SortedMap<String, Configuration> loadConfigurations() {
    return loadConfigurations(getNetworkSnapshot());
  }

  @Override
  public SortedMap<String, Configuration> loadConfigurations(NetworkSnapshot snapshot) {
    try (ActiveSpan span = GlobalTracer.get().buildSpan("Load configurations").startActive()) {
      assert span != null; // avoid unused warning
      _logger.debugf("Loading configurations for %s\n", snapshot);
      // Do we already have configurations in the cache?
      SortedMap<String, Configuration> configurations =
          _cachedConfigurations.getIfPresent(snapshot);
      if (configurations != null) {
        return configurations;
      }
      _logger.debugf("Loading configurations for %s, cache miss", snapshot);

      // Next, see if we have an up-to-date configurations on disk.
      configurations = _storage.loadConfigurations(snapshot.getNetwork(), snapshot.getSnapshot());
      if (configurations != null) {
        _logger.debugf("Loaded configurations for %s off disk", snapshot);
        postProcessSnapshot(configurations);
      } else {
        // Otherwise, we have to parse the configurations. Fall back to old, hacky code.
        configurations = parseConfigurationsAndApplyEnvironment();
      }

      _cachedConfigurations.put(snapshot, configurations);
      return configurations;
    }
  }

  @Nonnull
  private SortedMap<String, Configuration> parseConfigurationsAndApplyEnvironment() {
    _logger.infof("Repairing configurations for testrig %s", _testrigSettings.getName());
    repairConfigurations();
    SortedMap<String, Configuration> configurations =
        _storage.loadConfigurations(_settings.getContainer(), _testrigSettings.getName());
    verify(
        configurations != null,
        "Configurations should not be null when loaded immediately after repair.");
    postProcessSnapshot(configurations);
    return configurations;
  }

  @Override
  public ConvertConfigurationAnswerElement loadConvertConfigurationAnswerElementOrReparse() {
    ConvertConfigurationAnswerElement ccae =
        _storage.loadConvertConfigurationAnswerElement(
            _settings.getContainer(), _testrigSettings.getName());
    if (ccae != null
        && Version.isCompatibleVersion(
            "Service", "Old processed configurations", ccae.getVersion())) {
      return ccae;
    }

    repairConfigurations();
    ccae =
        _storage.loadConvertConfigurationAnswerElement(
            _settings.getContainer(), _testrigSettings.getName());
    if (ccae != null
        && Version.isCompatibleVersion(
            "Service", "Old processed configurations", ccae.getVersion())) {
      return ccae;
    } else {
      throw new BatfishException(
          "Version error repairing configurations for convert configuration answer element");
    }
  }

  @Override
  public DataPlane loadDataPlane() {
    try (ActiveSpan span = GlobalTracer.get().buildSpan("Load data plane").startActive()) {
      assert span != null; // avoid unused warning
      NetworkSnapshot snapshot = getNetworkSnapshot();
      DataPlane dp = _cachedDataPlanes.getIfPresent(snapshot);
      if (dp == null) {
        newBatch("Loading data plane from disk", 0);
        checkDataPlaneAnswerElement();
        dp = deserializeObject(_testrigSettings.getDataPlanePath(), DataPlane.class);
        _cachedDataPlanes.put(snapshot, dp);
      }
      return dp;
    }
  }

  private void checkDataPlaneAnswerElement() {
    if (!Version.isCompatibleVersion(
        "Service",
        "Old data plane",
        deserializeObject(_testrigSettings.getDataPlaneAnswerPath(), DataPlaneAnswerElement.class)
            .getVersion())) {
      throw new BatfishException(
          "Version error repairing data plane for data plane answer element");
    }
  }

  @Override
  public SortedMap<String, BgpAdvertisementsByVrf> loadEnvironmentBgpTables() {
    NetworkSnapshot snapshot = getNetworkSnapshot();
    SortedMap<String, BgpAdvertisementsByVrf> environmentBgpTables =
        _cachedEnvironmentBgpTables.get(snapshot);
    if (environmentBgpTables == null) {
      loadParseEnvironmentBgpTablesAnswerElement();
      environmentBgpTables =
          deserializeEnvironmentBgpTables(_testrigSettings.getSerializeEnvironmentBgpTablesPath());
      _cachedEnvironmentBgpTables.put(snapshot, environmentBgpTables);
    }
    return environmentBgpTables;
  }

  @Override
  public SortedMap<String, RoutesByVrf> loadEnvironmentRoutingTables() {
    NetworkSnapshot snapshot = getNetworkSnapshot();
    SortedMap<String, RoutesByVrf> environmentRoutingTables =
        _cachedEnvironmentRoutingTables.get(snapshot);
    if (environmentRoutingTables == null) {
      ParseEnvironmentRoutingTablesAnswerElement pertae =
          loadParseEnvironmentRoutingTablesAnswerElement();
      if (!Version.isCompatibleVersion(
          "Service", "Old processed environment routing tables", pertae.getVersion())) {
        repairEnvironmentRoutingTables();
      }
      environmentRoutingTables =
          deserializeEnvironmentRoutingTables(
              _testrigSettings.getSerializeEnvironmentRoutingTablesPath());
      _cachedEnvironmentRoutingTables.put(snapshot, environmentRoutingTables);
    }
    return environmentRoutingTables;
  }

  @Override
  public ParseEnvironmentBgpTablesAnswerElement loadParseEnvironmentBgpTablesAnswerElement() {
    return loadParseEnvironmentBgpTablesAnswerElement(true);
  }

  private ParseEnvironmentBgpTablesAnswerElement loadParseEnvironmentBgpTablesAnswerElement(
      boolean firstAttempt) {
    Path answerPath = _testrigSettings.getParseEnvironmentBgpTablesAnswerPath();
    if (!Files.exists(answerPath)) {
      repairEnvironmentBgpTables();
    }
    try {
      ParseEnvironmentBgpTablesAnswerElement ae =
          deserializeObject(answerPath, ParseEnvironmentBgpTablesAnswerElement.class);
      if (Version.isCompatibleVersion(
          "Service", "Old processed environment BGP tables", ae.getVersion())) {
        return ae;
      }
    } catch (Exception e) {
      /* Do nothing, this is expected on serialization or other errors. */
      _logger.warn(
          "Unable to load prior parse data from "
              + _testrigSettings.getParseEnvironmentBgpTablesAnswerPath()
              + "\n");
    }

    if (firstAttempt) {
      repairEnvironmentBgpTables();
      return loadParseEnvironmentBgpTablesAnswerElement(false);
    } else {
      throw new BatfishException(
          "Version error repairing environment BGP tables for parse environment BGP tables "
              + "answer element");
    }
  }

  @Override
  public ParseEnvironmentRoutingTablesAnswerElement
      loadParseEnvironmentRoutingTablesAnswerElement() {
    return loadParseEnvironmentRoutingTablesAnswerElement(true);
  }

  private ParseEnvironmentRoutingTablesAnswerElement loadParseEnvironmentRoutingTablesAnswerElement(
      boolean firstAttempt) {
    Path answerPath = _testrigSettings.getParseEnvironmentRoutingTablesAnswerPath();
    if (!Files.exists(answerPath)) {
      repairEnvironmentRoutingTables();
    }
    ParseEnvironmentRoutingTablesAnswerElement pertae =
        deserializeObject(answerPath, ParseEnvironmentRoutingTablesAnswerElement.class);
    if (!Version.isCompatibleVersion(
        "Service", "Old processed environment routing tables", pertae.getVersion())) {
      if (firstAttempt) {
        repairEnvironmentRoutingTables();
        return loadParseEnvironmentRoutingTablesAnswerElement(false);
      } else {
        throw new BatfishException(
            "Version error repairing environment routing tables for parse environment routing "
                + "tables answer element");
      }
    } else {
      return pertae;
    }
  }

  @Override
  public ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement() {
    return loadParseVendorConfigurationAnswerElement(true);
  }

  private ParseVendorConfigurationAnswerElement loadParseVendorConfigurationAnswerElement(
      boolean firstAttempt) {
    if (Files.exists(_testrigSettings.getParseAnswerPath())) {
      try {
        ParseVendorConfigurationAnswerElement pvcae =
            deserializeObject(
                _testrigSettings.getParseAnswerPath(), ParseVendorConfigurationAnswerElement.class);
        if (Version.isCompatibleVersion(
            "Service", "Old processed configurations", pvcae.getVersion())) {
          return pvcae;
        }
      } catch (Exception e) {
        /* Do nothing, this is expected on serialization or other errors. */
        _logger.warn(
            "Unable to load prior parse data from " + _testrigSettings.getParseAnswerPath() + "\n");
      }
    }
    if (firstAttempt) {
      repairVendorConfigurations();
      return loadParseVendorConfigurationAnswerElement(false);
    } else {
      throw new BatfishException(
          "Version error repairing vendor configurations for parse configuration answer element");
    }
  }

  private void mergeConvertAnswer(
      boolean summary, boolean verboseError, InitInfoAnswerElement answerElement) {
    ConvertConfigurationAnswerElement convertAnswer =
        loadConvertConfigurationAnswerElementOrReparse();
    mergeInitStepAnswer(answerElement, convertAnswer, summary, verboseError);
    convertAnswer.getConvertStatus().entrySet().stream()
        .filter(s -> s.getValue() == ConvertStatus.FAILED)
        .forEach(s -> answerElement.getParseStatus().put(s.getKey(), ParseStatus.FAILED));
  }

  private void mergeInitStepAnswer(
      InitInfoAnswerElement initInfoAnswerElement,
      InitStepAnswerElement initStepAnswerElement,
      boolean summary,
      boolean verboseError) {
    if (!summary) {
      if (verboseError) {
        SortedMap<String, List<BatfishStackTrace>> errors = initInfoAnswerElement.getErrors();
        initStepAnswerElement
            .getErrors()
            .forEach(
                (hostname, initStepErrors) ->
                    errors.computeIfAbsent(hostname, k -> new ArrayList<>()).add(initStepErrors));
      }
      SortedMap<String, Warnings> warnings = initInfoAnswerElement.getWarnings();
      initStepAnswerElement
          .getWarnings()
          .forEach(
              (hostname, initStepWarnings) -> {
                Warnings combined =
                    warnings.computeIfAbsent(hostname, h -> buildWarnings(_settings));
                combined.getParseWarnings().addAll(initStepWarnings.getParseWarnings());
                combined.getPedanticWarnings().addAll(initStepWarnings.getPedanticWarnings());
                combined.getRedFlagWarnings().addAll(initStepWarnings.getRedFlagWarnings());
                combined
                    .getUnimplementedWarnings()
                    .addAll(initStepWarnings.getUnimplementedWarnings());
              });
    }
  }

  private InitInfoAnswerElement mergeParseAnswer(
      boolean summary, boolean verboseError, ParseAnswerElement parseAnswer) {
    InitInfoAnswerElement answerElement = new InitInfoAnswerElement();
    mergeInitStepAnswer(answerElement, parseAnswer, summary, verboseError);
    answerElement.setParseStatus(parseAnswer.getParseStatus());
    answerElement.setParseTrees(parseAnswer.getParseTrees());
    return answerElement;
  }

  @Override
  public AtomicInteger newBatch(String description, int jobs) {
    return Driver.newBatch(_settings, description, jobs);
  }

  private void outputAnswer(Answer answer) {
    outputAnswer(answer, /* log */ false);
  }

  void outputAnswerWithLog(Answer answer) {
    outputAnswer(answer, /* log */ true);
  }

  private void outputAnswer(Answer answer, boolean writeLog) {
    try {
      String answerString = BatfishObjectMapper.writeString(answer);
      _logger.debug(answerString);
      @Nullable String logString = writeLog ? answerString : null;
      writeJsonAnswerWithLog(logString, answerString);
    } catch (Exception e) {
      BatfishException be = new BatfishException("Error in sending answer", e);
      try {
        Answer failureAnswer = Answer.failureAnswer(e.toString(), answer.getQuestion());
        failureAnswer.addAnswerElement(be.getBatfishStackTrace());
        String answerString = BatfishObjectMapper.writeString(failureAnswer);
        _logger.error(answerString);
        @Nullable String logString = writeLog ? answerString : null;
        writeJsonAnswerWithLog(logString, answerString);
      } catch (Exception e1) {
        _logger.errorf(
            "Could not serialize failure answer. %s", Throwables.getStackTraceAsString(e1));
      }
      throw be;
    }
  }

  void outputAnswerMetadata(Answer answer) {
    QuestionId questionId = _settings.getQuestionName();
    if (questionId == null) {
      return;
    }
    SnapshotId deltaSnapshot = _settings.getDiffQuestion() ? _deltaTestrigSettings.getName() : null;
    NetworkId networkId = _settings.getContainer();
    AnalysisId analysisId = _settings.getAnalysisName();
    QuestionSettingsId questionSettingsId;
    try {
      String questionClassId = _storage.loadQuestionClassId(networkId, questionId, analysisId);
      if (_idResolver.hasQuestionSettingsId(questionClassId, networkId)) {
        questionSettingsId = _idResolver.getQuestionSettingsId(questionClassId, networkId);
      } else {
        questionSettingsId = QuestionSettingsId.DEFAULT_QUESTION_SETTINGS_ID;
      }
    } catch (IOException e) {
      throw new BatfishException("Failed to retrieve question settings ID", e);
    }
    NodeRolesId networkNodeRolesId =
        _idResolver.hasNetworkNodeRolesId(networkId)
            ? _idResolver.getNetworkNodeRolesId(networkId)
            : NodeRolesId.DEFAULT_NETWORK_NODE_ROLES_ID;
    AnswerId baseAnswerId =
        _idResolver.getBaseAnswerId(
            networkId,
            _baseTestrigSettings.getName(),
            questionId,
            questionSettingsId,
            networkNodeRolesId,
            deltaSnapshot,
            analysisId);

    _storage.storeAnswerMetadata(
        AnswerMetadataUtil.computeAnswerMetadata(answer, _logger), baseAnswerId);
  }

  @VisibleForTesting
  public static AwsConfiguration parseAwsConfigurations(
      Map<Path, String> configurationData, ParseVendorConfigurationAnswerElement pvcae) {
    AwsConfiguration config = new AwsConfiguration();
    for (Entry<Path, String> configFile : configurationData.entrySet()) {
      Path path = configFile.getKey();
      int pathLength = configFile.getKey().getNameCount();
      String fileText = configFile.getValue();
      String regionName = path.getName(pathLength - 2).toString(); // parent dir name
      String fileName = path.subpath(pathLength - 3, pathLength).toString();
      pvcae.getFileMap().put(BfConsts.RELPATH_AWS_CONFIGS_FILE, fileName);

      JSONObject jsonObj = null;
      try {
        jsonObj = new JSONObject(fileText);
      } catch (JSONException e) {
        pvcae.addRedFlagWarning(
            BfConsts.RELPATH_AWS_CONFIGS_FILE,
            new Warning(String.format("AWS file %s is not valid JSON", fileName), "AWS"));
      }

      if (jsonObj != null) {
        try {
          config.addConfigElement(regionName, jsonObj, fileName, pvcae);
        } catch (JSONException e) {
          throw new BatfishException("Problems parsing JSON in " + fileName, e);
        }
      }
    }
    return config;
  }

  private SortedMap<String, BgpAdvertisementsByVrf> parseEnvironmentBgpTables(
      SortedMap<Path, String> inputData, ParseEnvironmentBgpTablesAnswerElement answerElement) {
    _logger.info("\n*** PARSING ENVIRONMENT BGP TABLES ***\n");
    _logger.resetTimer();
    SortedMap<String, BgpAdvertisementsByVrf> bgpTables = new TreeMap<>();
    List<ParseEnvironmentBgpTableJob> jobs = new ArrayList<>();
    SortedMap<String, Configuration> configurations = loadConfigurations();
    for (Entry<Path, String> bgpFile : inputData.entrySet()) {
      Path currentFile = bgpFile.getKey();
      String fileText = bgpFile.getValue();

      String hostname = currentFile.getFileName().toString();
      String optionalSuffix = ".bgp";
      if (hostname.endsWith(optionalSuffix)) {
        hostname = hostname.substring(0, hostname.length() - optionalSuffix.length());
      }
      if (!configurations.containsKey(hostname)) {
        continue;
      }
      Warnings warnings = buildWarnings(_settings);
      ParseEnvironmentBgpTableJob job =
          new ParseEnvironmentBgpTableJob(
              _settings, fileText, hostname, currentFile, warnings, _bgpTablePlugins);
      jobs.add(job);
    }
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        bgpTables,
        answerElement,
        _settings.getHaltOnParseError(),
        "Parse environment BGP tables");
    _logger.printElapsedTime();
    return bgpTables;
  }

  private SortedMap<String, RoutesByVrf> parseEnvironmentRoutingTables(
      SortedMap<Path, String> inputData, ParseEnvironmentRoutingTablesAnswerElement answerElement) {
    _logger.info("\n*** PARSING ENVIRONMENT ROUTING TABLES ***\n");
    _logger.resetTimer();
    SortedMap<String, RoutesByVrf> routingTables = new TreeMap<>();
    List<ParseEnvironmentRoutingTableJob> jobs = new ArrayList<>();
    SortedMap<String, Configuration> configurations = loadConfigurations();
    for (Entry<Path, String> routingFile : inputData.entrySet()) {
      Path currentFile = routingFile.getKey();
      String fileText = routingFile.getValue();

      String hostname = currentFile.getFileName().toString();
      if (!configurations.containsKey(hostname)) {
        continue;
      }

      Warnings warnings = buildWarnings(_settings);
      ParseEnvironmentRoutingTableJob job =
          new ParseEnvironmentRoutingTableJob(_settings, fileText, currentFile, warnings, this);
      jobs.add(job);
    }
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        routingTables,
        answerElement,
        _settings.getHaltOnParseError(),
        "Parse environment routing tables");
    _logger.printElapsedTime();
    return routingTables;
  }

  /**
   * Returns a list of {@link ParseVendorConfigurationJob} to parse each file.
   *
   * <p>{@code expectedFormat} specifies the type of files expected in the {@code keyedFileText}
   * map, or is set to {@link ConfigurationFormat#UNKNOWN} to trigger format detection.
   */
  private List<ParseVendorConfigurationJob> makeParseVendorConfigurationsJobs(
      Map<String, String> keyedFileText, ConfigurationFormat expectedFormat) {
    List<ParseVendorConfigurationJob> jobs = new ArrayList<>(keyedFileText.size());
    for (Entry<String, String> vendorFile : keyedFileText.entrySet()) {
      @Nullable
      SpanContext parseVendorConfigurationSpanContext =
          GlobalTracer.get().activeSpan() == null
              ? null
              : GlobalTracer.get().activeSpan().context();

      ParseVendorConfigurationJob job =
          new ParseVendorConfigurationJob(
              _settings,
              vendorFile.getValue(),
              vendorFile.getKey(),
              buildWarnings(_settings),
              expectedFormat,
              HashMultimap.create(),
              parseVendorConfigurationSpanContext);
      jobs.add(job);
    }
    return jobs;
  }

  /**
   * Parses the given configuration files and returns a map keyed by hostname representing the
   * {@link VendorConfiguration vendor-specific configurations}.
   *
   * <p>{@code expectedFormat} specifies the type of files expected in the {@code keyedFileText}
   * map, or is set to {@link ConfigurationFormat#UNKNOWN} to trigger format detection.
   */
  private SortedMap<String, VendorConfiguration> parseVendorConfigurations(
      Map<String, String> keyedConfigurationText,
      ParseVendorConfigurationAnswerElement answerElement,
      ConfigurationFormat expectedFormat) {
    _logger.info("\n*** PARSING VENDOR CONFIGURATION FILES ***\n");
    _logger.resetTimer();
    SortedMap<String, VendorConfiguration> vendorConfigurations = new TreeMap<>();
    List<ParseVendorConfigurationJob> jobs =
        makeParseVendorConfigurationsJobs(keyedConfigurationText, expectedFormat);
    BatfishJobExecutor.runJobsInExecutor(
        _settings,
        _logger,
        jobs,
        vendorConfigurations,
        answerElement,
        _settings.getHaltOnParseError(),
        "Parse configurations");
    _logger.printElapsedTime();
    return vendorConfigurations;
  }

  @Override
  public void popSnapshot() {
    int lastIndex = _testrigSettingsStack.size() - 1;
    _testrigSettings = _testrigSettingsStack.get(lastIndex);
    _testrigSettingsStack.remove(lastIndex);
  }

  private void populateChannelGroupMembers(
      Map<String, Interface> interfaces, String ifaceName, Interface iface) {
    String portChannelName = iface.getChannelGroup();
    if (portChannelName == null) {
      return;
    }
    Interface portChannel = interfaces.get(portChannelName);
    if (portChannel == null) {
      return;
    }
    portChannel.setChannelGroupMembers(
        ImmutableSortedSet.<String>naturalOrder()
            .addAll(portChannel.getChannelGroupMembers())
            .add(ifaceName)
            .build());
  }

  private void postProcessAggregatedInterfaces(Map<String, Configuration> configurations) {
    configurations
        .values()
        .forEach(
            c ->
                c.getVrfs()
                    .values()
                    .forEach(v -> postProcessAggregatedInterfacesHelper(v.getInterfaces())));
  }

  private void postProcessAggregatedInterfacesHelper(Map<String, Interface> interfaces) {
    /* Populate aggregated interfaces with members referring to them. */
    interfaces.forEach(
        (ifaceName, iface) -> populateChannelGroupMembers(interfaces, ifaceName, iface));

    /* Compute bandwidth for aggregated interfaces. */
    computeAggregatedInterfaceBandwidths(interfaces);
  }

  private void identifyDeviceTypes(Collection<Configuration> configurations) {
    for (Configuration c : configurations) {
      if (c.getDeviceType() != null) {
        continue;
      }
      // Set device type to host iff the configuration format is HOST
      if (c.getConfigurationFormat() == ConfigurationFormat.HOST) {
        c.setDeviceType(DeviceType.HOST);
      } else if (c.getVrfs().values().stream()
          .anyMatch(
              vrf ->
                  vrf.getBgpProcess() != null
                      || !vrf.getEigrpProcesses().isEmpty()
                      || !vrf.getOspfProcesses().isEmpty()
                      || vrf.getRipProcess() != null)) {
        // If any vrf on device has BGP, EIGRP, OSPF, or RIP, set device type to router
        c.setDeviceType(DeviceType.ROUTER);
      } else {
        // If device was not a host or router, call it a switch
        c.setDeviceType(DeviceType.SWITCH);
      }
    }
  }

  @VisibleForTesting
  static void postProcessInterfaceDependencies(Map<String, Configuration> configurations) {
    configurations
        .values()
        .forEach(
            config -> {
              Map<String, Interface> allInterfaces = config.getAllInterfaces();
              Graph<String, Dependency> graph = new SimpleDirectedGraph<>(Dependency.class);
              allInterfaces.keySet().forEach(graph::addVertex);
              allInterfaces
                  .values()
                  .forEach(
                      iface ->
                          iface
                              .getDependencies()
                              .forEach(
                                  dependency ->
                                      graph.addEdge(
                                          // Reverse edge direction to aid topological sort
                                          dependency.getInterfaceName(),
                                          iface.getName(),
                                          dependency)));

              // Traverse interfaces in topological order and deactivate if necessary
              for (TopologicalOrderIterator<String, Dependency> iterator =
                      new TopologicalOrderIterator<>(graph);
                  iterator.hasNext(); ) {
                String ifaceName = iterator.next();
                deactivateInterfaceIfNeeded(allInterfaces.get(ifaceName));
              }
            });
  }

  /** Deactivate an interface if it is blacklisted or its dependencies are not active */
  private static void deactivateInterfaceIfNeeded(Interface iface) {
    Configuration config = iface.getOwner();
    Set<Dependency> dependencies = iface.getDependencies();
    if (dependencies.stream()
        // Look at bind dependencies
        .filter(d -> d.getType() == DependencyType.BIND)
        .map(d -> config.getAllInterfaces().get(d.getInterfaceName()))
        // Find any missing or inactive interfaces
        .anyMatch(parent -> parent == null || !parent.getActive())) {
      iface.setActive(false);
    }

    // Look at aggregate dependencies only now
    Set<Dependency> aggregateDependencies =
        dependencies.stream()
            .filter(d -> d.getType() == DependencyType.AGGREGATE)
            .collect(ImmutableSet.toImmutableSet());
    if (iface.getInterfaceType() == InterfaceType.AGGREGATED
        && aggregateDependencies.stream()
            // Extract existing and active interfaces
            .map(d -> config.getAllInterfaces().get(d.getInterfaceName()))
            .filter(Objects::nonNull)
            .noneMatch(Interface::getActive)) {
      iface.setActive(false);
    }
  }

  private void postProcessOspfCosts(Map<String, Configuration> configurations) {
    configurations
        .values()
        .forEach(
            c ->
                c.getVrfs()
                    .values()
                    .forEach(
                        vrf -> {
                          // Compute OSPF interface costs where they are missing
                          vrf.getOspfProcesses().values().forEach(p -> p.initInterfaceCosts(c));
                        }));
  }

  @Override
  public Set<BgpAdvertisement> loadExternalBgpAnnouncements(
      Map<String, Configuration> configurations) {
    Set<BgpAdvertisement> advertSet = new LinkedHashSet<>();
    for (ExternalBgpAdvertisementPlugin plugin : _externalBgpAdvertisementPlugins) {
      Set<BgpAdvertisement> currentAdvertisements = plugin.loadExternalBgpAdvertisements();
      advertSet.addAll(currentAdvertisements);
    }
    return advertSet;
  }

  /**
   * Builds the {@link Trace}s for a {@link Set} of {@link Flow}s
   *
   * @param flows {@link Set} of {@link Flow} for which {@link Trace}s are to be found
   * @param ignoreFilters if true, will ignore ACLs
   * @return {@link SortedMap} of {@link Flow}s to {@link List} of {@link Trace}s
   */
  @Override
  public SortedMap<Flow, List<Trace>> buildFlows(Set<Flow> flows, boolean ignoreFilters) {
    return getTracerouteEngine().computeTraces(flows, ignoreFilters);
  }

  @Override
  public TracerouteEngine getTracerouteEngine() {
    return new TracerouteEngineImpl(
        loadDataPlane(), _topologyProvider.getLayer3Topology(getNetworkSnapshot()));
  }

  /** Function that processes an interface blacklist across all configurations */
  private static void processInterfaceBlacklist(
      Set<NodeInterfacePair> interfaceBlacklist, NetworkConfigurations configurations) {
    interfaceBlacklist.stream()
        .map(iface -> configurations.getInterface(iface.getHostname(), iface.getInterface()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(Interface::blacklist);
  }

  @VisibleForTesting
  static Set<NodeInterfacePair> nodeToInterfaceBlacklist(
      SortedSet<String> blacklistNodes, NetworkConfigurations configurations) {
    return blacklistNodes.stream()
        // Get all valid/present node configs
        .map(configurations::get)
        .filter(Optional::isPresent)
        .map(Optional::get)
        // All interfaces in each config
        .flatMap(c -> c.getAllInterfaces().values().stream())
        .map(NodeInterfacePair::new)
        .collect(ImmutableSet.toImmutableSet());
  }

  @VisibleForTesting
  static void processManagementInterfaces(Map<String, Configuration> configurations) {
    configurations
        .values()
        .forEach(
            configuration -> {
              for (Interface iface : configuration.getAllInterfaces().values()) {
                if (MANAGEMENT_INTERFACES.matcher(iface.getName()).find()
                    || MANAGEMENT_VRFS.matcher(iface.getVrfName()).find()) {
                  iface.blacklist();
                }
              }
            });
  }

  @Override
  public void pushBaseSnapshot() {
    _testrigSettingsStack.add(_testrigSettings);
    _testrigSettings = _baseTestrigSettings;
  }

  @Override
  public void pushDeltaSnapshot() {
    _testrigSettingsStack.add(_testrigSettings);
    _testrigSettings = _deltaTestrigSettings;
  }

  @Nullable
  @Override
  public String readExternalBgpAnnouncementsFile() {
    Path externalBgpAnnouncementsPath = _testrigSettings.getExternalBgpAnnouncementsPath();
    if (Files.exists(externalBgpAnnouncementsPath)) {
      String externalBgpAnnouncementsFileContents =
          CommonUtil.readFile(externalBgpAnnouncementsPath);
      return externalBgpAnnouncementsFileContents;
    } else {
      return null;
    }
  }

  /**
   * Read Iptable Files for each host in the keyset of {@code hostConfigurations}, and store the
   * contents in {@code iptablesDate}. Each task fails if the Iptable file specified by host is not
   * under {@code testRigPath} or does not exist.
   *
   * @throws BatfishException if there is a failed task and either {@link
   *     Settings#getExitOnFirstError()} or {@link Settings#getHaltOnParseError()} is set.
   */
  void readIptableFiles(
      Path testRigPath,
      SortedMap<String, VendorConfiguration> hostConfigurations,
      SortedMap<Path, String> iptablesData,
      ParseVendorConfigurationAnswerElement answerElement) {
    List<BatfishException> failureCauses = new ArrayList<>();
    for (VendorConfiguration vc : hostConfigurations.values()) {
      HostConfiguration hostConfig = (HostConfiguration) vc;
      String iptablesFile = hostConfig.getIptablesFile();
      if (iptablesFile == null) {
        continue;
      }

      Path path = Paths.get(testRigPath.toString(), iptablesFile);

      // ensure that the iptables file is not taking us outside of the
      // testrig
      try {
        if (!path.toFile().getCanonicalPath().contains(testRigPath.toFile().getCanonicalPath())
            || !path.toFile().exists()) {
          String failureMessage =
              String.format(
                  "Iptables file %s for host %s is not contained within the testrig",
                  hostConfig.getIptablesFile(), hostConfig.getHostname());
          BatfishException bfc;
          if (answerElement.getErrors().containsKey(hostConfig.getHostname())) {
            bfc =
                new BatfishException(
                    failureMessage,
                    answerElement.getErrors().get(hostConfig.getHostname()).getException());
            answerElement.getErrors().put(hostConfig.getHostname(), bfc.getBatfishStackTrace());
            answerElement
                .getErrorDetails()
                .put(
                    hostConfig.getHostname(),
                    new ErrorDetails(Throwables.getStackTraceAsString(bfc)));
          } else {
            bfc = new BatfishException(failureMessage);
            if (_settings.getExitOnFirstError()) {
              throw bfc;
            } else {
              failureCauses.add(bfc);
              answerElement.getErrors().put(hostConfig.getHostname(), bfc.getBatfishStackTrace());
              answerElement.getParseStatus().put(hostConfig.getIptablesFile(), ParseStatus.FAILED);
              answerElement
                  .getErrorDetails()
                  .put(
                      hostConfig.getHostname(),
                      new ErrorDetails(Throwables.getStackTraceAsString(bfc)));
            }
          }
        } else {
          String fileText = CommonUtil.readFile(path);
          iptablesData.put(path, fileText);
        }
      } catch (IOException e) {
        throw new BatfishException("Could not get canonical path", e);
      }
    }

    if (_settings.getHaltOnParseError() && !failureCauses.isEmpty()) {
      BatfishException e =
          new BatfishException(
              "Fatal exception due to at least one Iptables file is"
                  + " not contained within the testrig");
      failureCauses.forEach(e::addSuppressed);
      throw e;
    }
  }

  @Override
  public void registerAnswerer(
      String questionName,
      String questionClassName,
      BiFunction<Question, IBatfish, Answerer> answererCreator) {
    AnswererCreator oldAnswererCreator =
        _answererCreators.putIfAbsent(
            questionName, new AnswererCreator(questionClassName, answererCreator));
    if (oldAnswererCreator != null) {
      // Error: questionName collision.
      String oldQuestionClassName = _answererCreators.get(questionClassName).getQuestionClassName();
      throw new IllegalArgumentException(
          String.format(
              "questionName %s already exists.\n"
                  + "  old questionClassName: %s\n"
                  + "  new questionClassName: %s",
              questionName, oldQuestionClassName, questionClassName));
    }
  }

  @Override
  public void registerBgpTablePlugin(BgpTableFormat format, BgpTablePlugin bgpTablePlugin) {
    _bgpTablePlugins.put(format, bgpTablePlugin);
  }

  @Override
  public void registerExternalBgpAdvertisementPlugin(
      ExternalBgpAdvertisementPlugin externalBgpAdvertisementPlugin) {
    _externalBgpAdvertisementPlugins.add(externalBgpAdvertisementPlugin);
  }

  private void repairConfigurations() {
    ParseVendorConfigurationAnswerElement pvcae = loadParseVendorConfigurationAnswerElement();
    if (!Version.isCompatibleVersion("Service", "Old parsed configurations", pvcae.getVersion())) {
      repairVendorConfigurations();
    }
    Path inputPath = _testrigSettings.getSerializeVendorPath();
    serializeIndependentConfigs(inputPath);
  }

  /**
   * Post-process the configuration in the current snapshot. Post-processing includes:
   *
   * <ul>
   *   <li>Applying node and interface blacklists.
   *   <li>Process interface dependencies and deactivate interfaces that cannot be up
   * </ul>
   */
  private void updateBlacklistedAndInactiveConfigs(Map<String, Configuration> configurations) {
    NetworkConfigurations nc = NetworkConfigurations.of(configurations);
    NetworkSnapshot networkSnapshot = getNetworkSnapshot();
    NetworkId networkId = networkSnapshot.getNetwork();
    SnapshotId snapshotId = networkSnapshot.getSnapshot();

    processInterfaceBlacklist(
        nodeToInterfaceBlacklist(
            firstNonNull(
                _storage.loadNodeBlacklist(networkId, snapshotId), ImmutableSortedSet.of()),
            nc),
        nc);
    processInterfaceBlacklist(
        firstNonNull(_storage.loadInterfaceBlacklist(networkId, snapshotId), ImmutableSet.of()),
        nc);
    if (_settings.ignoreManagementInterfaces()) {
      processManagementInterfaces(configurations);
    }
    postProcessInterfaceDependencies(configurations);

    // We do not process the edge blacklist here. Instead, we rely on these edges being explicitly
    // deleted from the Topology (aka list of edges) that is used along with configurations in
    // answering questions.

    // TODO: take this out once dependencies are *the* definitive way to disable interfaces
    disableUnusableVlanInterfaces(configurations);
  }

  /**
   * Ensures that the current configurations for the current snapshot are correct by performing some
   * post-processing on the vendor-independent datamodel. Among other things, this includes:
   *
   * <ul>
   *   <li>Invalidating cached configs if the in-memory copy has been changed by question
   *       processing.
   *   <li>Re-loading configurations from disk, including re-parsing if the configs were parsed on a
   *       previous version of Batfish.
   *   <li>Ensuring that blacklists are honored.
   * </ul>
   */
  private void postProcessSnapshot(Map<String, Configuration> configurations) {
    updateBlacklistedAndInactiveConfigs(configurations);
    postProcessAggregatedInterfaces(configurations);
    NetworkConfigurations nc = NetworkConfigurations.of(configurations);
    OspfTopologyUtils.initNeighborConfigs(nc);
    postProcessOspfCosts(configurations);
    EigrpTopologyUtils.initNeighborConfigs(nc);
    computeAndStoreCompletionMetadata(configurations);
  }

  private void computeAndStoreCompletionMetadata(Map<String, Configuration> configurations) {
    try {
      _storage.storeCompletionMetadata(
          computeCompletionMetadata(configurations),
          _settings.getContainer(),
          _testrigSettings.getName());
    } catch (IOException e) {
      _logger.errorf("Error storing CompletionMetadata: %s", e);
    }
  }

  private CompletionMetadata computeCompletionMetadata(Map<String, Configuration> configurations) {
    return new CompletionMetadata(
        getFilterNames(configurations),
        getInterfaces(configurations),
        getIps(configurations),
        getMlagIds(configurations),
        getNodes(configurations),
        getPrefixes(configurations),
        getRoutingPolicyNames(configurations),
        getStructureNames(configurations),
        getVrfs(configurations),
        getZones(configurations));
  }

  @Override
  public String getSnapshotInputObject(String key) throws FileNotFoundException, IOException {
    InputStream inputObject =
        _storage.loadSnapshotInputObject(_settings.getContainer(), _testrigSettings.getName(), key);
    byte[] bytes = IOUtils.toByteArray(inputObject);
    return new String(bytes, detectCharset(bytes));
  }

  private void repairEnvironmentBgpTables() {
    Path answerPath = _testrigSettings.getParseEnvironmentBgpTablesAnswerPath();
    CommonUtil.deleteIfExists(answerPath);
    Path bgpTablesOutputPath = _testrigSettings.getSerializeEnvironmentBgpTablesPath();
    CommonUtil.deleteDirectory(bgpTablesOutputPath);
    computeEnvironmentBgpTables();
  }

  private void repairEnvironmentRoutingTables() {
    Path answerPath = _testrigSettings.getParseEnvironmentRoutingTablesAnswerPath();
    CommonUtil.deleteIfExists(answerPath);
    Path rtOutputPath = _testrigSettings.getSerializeEnvironmentRoutingTablesPath();
    CommonUtil.deleteDirectory(rtOutputPath);
    computeEnvironmentRoutingTables();
  }

  private void repairVendorConfigurations() {
    Path outputPath = _testrigSettings.getSerializeVendorPath();
    CommonUtil.deleteDirectory(outputPath);
    Path testRigPath = _testrigSettings.getInputPath();
    serializeVendorConfigs(testRigPath, outputPath);
  }

  public Answer run() {
    newBatch("Begin job", 0);
    boolean action = false;
    Answer answer = new Answer();

    if (_settings.getFlatten()) {
      Path flattenSource = _testrigSettings.getInputPath();
      Path flattenDestination = _settings.getFlattenDestination();
      flatten(flattenSource, flattenDestination);
      return answer;
    }

    if (_settings.getSerializeVendor()) {
      Path testRigPath = _testrigSettings.getInputPath();
      Path outputPath = _testrigSettings.getSerializeVendorPath();
      answer.append(serializeVendorConfigs(testRigPath, outputPath));
      action = true;
    }

    if (_settings.getSerializeIndependent()) {
      Path inputPath = _testrigSettings.getSerializeVendorPath();
      answer.append(serializeIndependentConfigs(inputPath));
      // TODO: compute topology on initialization in cleaner way
      initializeTopology(getNetworkSnapshot());
      updateSnapshotNodeRoles();
      action = true;
    }

    if (_settings.getInitInfo()) {
      InitInfoAnswerElement initInfoAnswerElement = initInfo(true, false);
      // In this context we can remove parse trees because they will be returned in preceding answer
      // element. Note that parse trees are not removed when asking initInfo as its own question.
      initInfoAnswerElement.setParseTrees(Collections.emptySortedMap());
      answer.addAnswerElement(initInfoAnswerElement);
      action = true;
    }

    if (_settings.getAnswer()) {
      try (ActiveSpan questionSpan =
          GlobalTracer.get().buildSpan("Getting answer to question").startActive()) {
        assert questionSpan != null; // avoid unused warning
        answer.append(answer());
        action = true;
      }
    }

    if (_settings.getAnalyze()) {
      answer.append(analyze());
      action = true;
    }

    if (_settings.getDataPlane()) {
      answer.addAnswerElement(computeDataPlane());
      action = true;
    }

    if (!action) {
      throw new CleanBatfishException("No task performed! Run with -help flag to see usage\n");
    }
    return answer;
  }

  /** Initialize topologies, commit {raw, raw pojo, pruned} layer-3 topologies to storage. */
  @VisibleForTesting
  void initializeTopology(NetworkSnapshot networkSnapshot) {
    Map<String, Configuration> configurations = loadConfigurations();
    Topology rawLayer3Topology = _topologyProvider.getRawLayer3Topology(networkSnapshot);
    checkTopology(configurations, rawLayer3Topology);
    org.batfish.datamodel.pojo.Topology pojoTopology =
        org.batfish.datamodel.pojo.Topology.create(
            _settings.getSnapshotName(), configurations, rawLayer3Topology);
    try {
      _storage.storePojoTopology(
          pojoTopology, networkSnapshot.getNetwork(), networkSnapshot.getSnapshot());
    } catch (IOException e) {
      throw new BatfishException("Could not serialize layer-3 POJO topology", e);
    }
    Topology layer3Topology = _topologyProvider.getInitialLayer3Topology(getNetworkSnapshot());
    try {
      _storage.storeInitialTopology(
          layer3Topology, networkSnapshot.getNetwork(), networkSnapshot.getSnapshot());
    } catch (IOException e) {
      throw new BatfishException("Could not serialize layer-3 topology", e);
    }
  }

  private void serializeAwsConfigs(
      Path testRigPath, Path outputPath, ParseVendorConfigurationAnswerElement pvcae) {
    _logger.info("\n*** READING AWS CONFIGS ***\n");
    Map<Path, String> configurationData =
        readAllFiles(testRigPath.resolve(BfConsts.RELPATH_AWS_CONFIGS_DIR), _logger);
    AwsConfiguration config;
    try (ActiveSpan parseAwsConfigsSpan =
        GlobalTracer.get().buildSpan("Parse AWS configs").startActive()) {
      assert parseAwsConfigsSpan != null; // avoid unused warning
      config = parseAwsConfigurations(configurationData, pvcae);
    }

    _logger.info("\n*** SERIALIZING AWS CONFIGURATION STRUCTURES ***\n");
    _logger.resetTimer();
    outputPath.toFile().mkdirs();
    Path currentOutputPath = outputPath.resolve(BfConsts.RELPATH_AWS_CONFIGS_FILE);
    _logger.debugf("Serializing AWS to \"%s\"...", currentOutputPath);
    serializeObject(config, currentOutputPath);
    _logger.debug("OK\n");
    _logger.printElapsedTime();
  }

  private Answer serializeEnvironmentBgpTables(Path inputPath, Path outputPath) {
    Answer answer = new Answer();
    ParseEnvironmentBgpTablesAnswerElement answerElement =
        new ParseEnvironmentBgpTablesAnswerElement();
    answerElement.setVersion(Version.getVersion());
    answer.addAnswerElement(answerElement);
    SortedMap<String, BgpAdvertisementsByVrf> bgpTables =
        getEnvironmentBgpTables(inputPath, answerElement);
    serializeEnvironmentBgpTables(bgpTables, outputPath);
    serializeObject(answerElement, _testrigSettings.getParseEnvironmentBgpTablesAnswerPath());
    return answer;
  }

  private void serializeEnvironmentBgpTables(
      SortedMap<String, BgpAdvertisementsByVrf> bgpTables, Path outputPath) {
    if (bgpTables == null) {
      throw new BatfishException("Exiting due to parsing error(s)");
    }
    _logger.info("\n*** SERIALIZING ENVIRONMENT BGP TABLES ***\n");
    _logger.resetTimer();
    outputPath.toFile().mkdirs();
    SortedMap<Path, BgpAdvertisementsByVrf> output = new TreeMap<>();
    bgpTables.forEach(
        (name, rt) -> {
          Path currentOutputPath = outputPath.resolve(name);
          output.put(currentOutputPath, rt);
        });
    serializeObjects(output);
    _logger.printElapsedTime();
  }

  private Answer serializeEnvironmentRoutingTables(Path inputPath, Path outputPath) {
    Answer answer = new Answer();
    ParseEnvironmentRoutingTablesAnswerElement answerElement =
        new ParseEnvironmentRoutingTablesAnswerElement();
    answerElement.setVersion(Version.getVersion());
    answer.addAnswerElement(answerElement);
    SortedMap<String, RoutesByVrf> routingTables =
        getEnvironmentRoutingTables(inputPath, answerElement);
    serializeEnvironmentRoutingTables(routingTables, outputPath);
    serializeObject(answerElement, _testrigSettings.getParseEnvironmentRoutingTablesAnswerPath());
    return answer;
  }

  private void serializeEnvironmentRoutingTables(
      SortedMap<String, RoutesByVrf> routingTables, Path outputPath) {
    if (routingTables == null) {
      throw new BatfishException("Exiting due to parsing error(s)");
    }
    _logger.info("\n*** SERIALIZING ENVIRONMENT ROUTING TABLES ***\n");
    _logger.resetTimer();
    outputPath.toFile().mkdirs();
    SortedMap<Path, RoutesByVrf> output = new TreeMap<>();
    routingTables.forEach(
        (name, rt) -> {
          Path currentOutputPath = outputPath.resolve(name);
          output.put(currentOutputPath, rt);
        });
    serializeObjects(output);
    _logger.printElapsedTime();
  }

  private SortedMap<String, VendorConfiguration> serializeHostConfigs(
      Path testRigPath, Path outputPath, ParseVendorConfigurationAnswerElement answerElement) {
    _logger.info("\n*** READING HOST CONFIGS ***\n");
    Map<String, String> keyedHostText =
        readAllFiles(testRigPath.resolve(BfConsts.RELPATH_HOST_CONFIGS_DIR), _logger).entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    e -> testRigPath.relativize(e.getKey()).toString(), Entry::getValue));
    // read the host files
    SortedMap<String, VendorConfiguration> allHostConfigurations;
    try (ActiveSpan parseHostConfigsSpan =
        GlobalTracer.get().buildSpan("Parse host configs").startActive()) {
      assert parseHostConfigsSpan != null; // avoid unused warning
      allHostConfigurations =
          parseVendorConfigurations(keyedHostText, answerElement, ConfigurationFormat.HOST);
    }
    if (allHostConfigurations == null) {
      throw new BatfishException("Exiting due to parser errors");
    }
    _logger.infof(
        "Testrig:%s in container:%s has total number of host configs:%d",
        getTestrigName(), getContainerName(), allHostConfigurations.size());

    // split into hostConfigurations and overlayConfigurations
    SortedMap<String, VendorConfiguration> overlayConfigurations =
        allHostConfigurations.entrySet().stream()
            .filter(e -> ((HostConfiguration) e.getValue()).getOverlay())
            .collect(toMap(Entry::getKey, Entry::getValue, (v1, v2) -> v1, TreeMap::new));
    SortedMap<String, VendorConfiguration> nonOverlayHostConfigurations =
        allHostConfigurations.entrySet().stream()
            .filter(e -> !((HostConfiguration) e.getValue()).getOverlay())
            .collect(toMap(Entry::getKey, Entry::getValue, (v1, v2) -> v1, TreeMap::new));

    // read and associate iptables files for specified hosts
    SortedMap<Path, String> iptablesData = new TreeMap<>();
    readIptableFiles(testRigPath, allHostConfigurations, iptablesData, answerElement);
    Map<String, String> keyedIptablesText =
        iptablesData.entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    e -> testRigPath.relativize(e.getKey()).toString(), Entry::getValue));

    SortedMap<String, VendorConfiguration> iptablesConfigurations =
        parseVendorConfigurations(keyedIptablesText, answerElement, ConfigurationFormat.IPTABLES);
    for (VendorConfiguration vc : allHostConfigurations.values()) {
      HostConfiguration hostConfig = (HostConfiguration) vc;
      if (hostConfig.getIptablesFile() != null) {
        Path path = Paths.get(testRigPath.toString(), hostConfig.getIptablesFile());
        String relativePathStr = testRigPath.relativize(path).toString();
        if (iptablesConfigurations.containsKey(relativePathStr)) {
          hostConfig.setIptablesVendorConfig(
              (IptablesVendorConfiguration) iptablesConfigurations.get(relativePathStr));
        }
      }
    }

    // now, serialize
    _logger.info("\n*** SERIALIZING VENDOR CONFIGURATION STRUCTURES ***\n");
    _logger.resetTimer();
    createDirectories(outputPath);

    Map<Path, VendorConfiguration> output = new TreeMap<>();
    nonOverlayHostConfigurations.forEach(
        (name, vc) -> {
          Path currentOutputPath = outputPath.resolve(name);
          output.put(currentOutputPath, vc);
        });
    serializeObjects(output);
    // serialize warnings
    serializeObject(answerElement, _testrigSettings.getParseAnswerPath());
    _logger.printElapsedTime();
    return overlayConfigurations;
  }

  private Answer serializeIndependentConfigs(Path vendorConfigPath) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("Serialize vendor-independent configs").startActive()) {
      assert span != null; // avoid unused warning
      Answer answer = new Answer();
      ConvertConfigurationAnswerElement answerElement = new ConvertConfigurationAnswerElement();
      answerElement.setVersion(Version.getVersion());
      if (_settings.getVerboseParse()) {
        answer.addAnswerElement(answerElement);
      }
      Map<String, Configuration> configurations;
      try (ActiveSpan convertSpan =
          GlobalTracer.get()
              .buildSpan("Convert vendor-specific configs to vendor-independent configs")
              .startActive()) {
        assert convertSpan != null; // avoid unused warning
        configurations = getConfigurations(vendorConfigPath, answerElement);
      }

      configurations.putAll(getIspConfigurations(configurations, answerElement.getWarnings()));

      try (ActiveSpan storeSpan =
          GlobalTracer.get().buildSpan("Store vendor-independent configs").startActive()) {
        assert storeSpan != null; // avoid unused warning
        _storage.storeConfigurations(
            configurations, answerElement, _settings.getContainer(), _testrigSettings.getName());
      }

      try (ActiveSpan ppSpan =
          GlobalTracer.get().buildSpan("Post-process vendor-independent configs").startActive()) {
        assert ppSpan != null; // avoid unused warning
        postProcessSnapshot(configurations);
      }
      return answer;
    }
  }

  private void updateSnapshotNodeRoles() {
    // Compute new auto role data and updates existing auto data with it
    NetworkId networkId = _settings.getContainer();
    SnapshotId snapshotId = _settings.getTestrig();
    NodeRolesId snapshotNodeRolesId = _idResolver.getSnapshotNodeRolesId(networkId, snapshotId);
    Set<String> nodeNames = loadConfigurations().keySet();
    Topology rawLayer3Topology = _topologyProvider.getRawLayer3Topology(getNetworkSnapshot());
    SortedSet<NodeRoleDimension> autoRoles =
        new InferRoles(nodeNames, rawLayer3Topology).inferRoles();
    NodeRolesData.Builder snapshotNodeRoles = NodeRolesData.builder();
    try {
      if (!autoRoles.isEmpty()) {
        snapshotNodeRoles.setDefaultDimension(autoRoles.first().getName());
        snapshotNodeRoles.setRoleDimensions(autoRoles);
      }
      _storage.storeNodeRoles(snapshotNodeRoles.build(), snapshotNodeRolesId);
    } catch (IOException e) {
      _logger.warnf("Could not update node roles: %s", e);
    }
  }

  private ParseVendorConfigurationResult getOrParse(
      ParseVendorConfigurationJob job, @Nullable SpanContext span, GrammarSettings settings) {
    String filename = job.getFilename();
    String filetext = job.getFileText();
    try (ActiveSpan parseNetworkConfigsSpan =
        GlobalTracer.get()
            .buildSpan("Parse " + job.getFilename())
            .addReference(References.FOLLOWS_FROM, span)
            .startActive()) {
      assert parseNetworkConfigsSpan != null; // avoid unused warning

      // Short-circuit all cache-related code.
      if (!_settings.getParseReuse()) {
        long startTime = System.currentTimeMillis();
        ParseResult result = job.parse();
        long elapsed = System.currentTimeMillis() - startTime;
        return job.fromResult(result, elapsed);
      }

      String id =
          Hashing.murmur3_128()
              .newHasher()
              .putString("Cached Parse Result", UTF_8)
              .putString(filename, UTF_8)
              .putString(filetext, UTF_8)
              .putBoolean(settings.getDisableUnrecognized())
              .putInt(settings.getMaxParserContextLines())
              .putInt(settings.getMaxParserContextTokens())
              .putInt(settings.getMaxParseTreePrintLength())
              .putBoolean(settings.getPrintParseTreeLineNums())
              .putBoolean(settings.getPrintParseTree())
              .putBoolean(settings.getThrowOnLexerError())
              .putBoolean(settings.getThrowOnParserError())
              .putBoolean(settings.getUseNewCiscoNxosParser())
              .hash()
              .toString();
      long startTime = System.currentTimeMillis();
      boolean cached = false;
      ParseResult result;
      try (InputStream in = _storage.loadNetworkBlob(getContainerName(), id)) {
        result = SerializationUtils.deserialize(in);
        // sanity-check filenames. In the extremely unlikely event of a collision, we'll lose reuse
        // for this input.
        cached = result.getFilename().equals(filename);
      } catch (FileNotFoundException e) {
        result = job.parse();
      } catch (Exception e) {
        _logger.warnf(
            "Error deserializing cached parse result for %s: %s",
            filename, Throwables.getStackTraceAsString(e));
        result = job.parse();
      }
      if (!cached) {
        try {
          byte[] serialized = SerializationUtils.serialize(result);
          _storage.storeNetworkBlob(new ByteArrayInputStream(serialized), getContainerName(), id);
        } catch (Exception e) {
          _logger.warnf(
              "Error caching parse result for %s: %s",
              filename, Throwables.getStackTraceAsString(e));
        }
      }
      long elapsed = System.currentTimeMillis() - startTime;
      return job.fromResult(result, elapsed);
    }
  }

  /**
   * Parses configuration files for networking devices from the uploaded user data and produces
   * {@link VendorConfiguration vendor-specific configurations} serialized to the given output path.
   *
   * <p>This function should be named better, but it's called by the {@link
   * #serializeVendorConfigs(Path, Path)}, so leaving as-is for now.
   */
  private void serializeNetworkConfigs(
      Path userUploadPath,
      Path outputPath,
      ParseVendorConfigurationAnswerElement answerElement,
      SortedMap<String, VendorConfiguration> overlayHostConfigurations) {
    if (!overlayHostConfigurations.isEmpty()) {
      // Not able to cache with overlays.
      oldSerializeNetworkConfigs(
          userUploadPath, outputPath, answerElement, overlayHostConfigurations);
      return;
    }

    _logger.info("\n*** READING DEVICE CONFIGURATION FILES ***\n");

    List<ParseVendorConfigurationResult> parseResults;
    try (ActiveSpan parseNetworkConfigsSpan =
        GlobalTracer.get().buildSpan("Parse network configs").startActive()) {
      assert parseNetworkConfigsSpan != null; // avoid unused warning

      // user filename (configs/foo) -> text of configs/foo
      Map<String, String> keyedConfigText =
          readAllFiles(userUploadPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR), _logger)
              .entrySet().stream()
              .collect(
                  ImmutableMap.toImmutableMap(
                      e -> userUploadPath.relativize(e.getKey()).toString(), Entry::getValue));
      List<ParseVendorConfigurationJob> jobs =
          makeParseVendorConfigurationsJobs(keyedConfigText, ConfigurationFormat.UNKNOWN);
      AtomicInteger batch = newBatch("Parse network configs", keyedConfigText.size());
      parseResults =
          jobs.parallelStream()
              .map(
                  j -> {
                    ParseVendorConfigurationResult result =
                        this.getOrParse(j, parseNetworkConfigsSpan.context(), _settings);
                    batch.incrementAndGet();
                    return result;
                  })
              .collect(ImmutableList.toImmutableList());
    }

    if (_settings.getHaltOnParseError()
        && parseResults.stream().anyMatch(r -> r.getFailureCause() != null)) {
      BatfishException e = new BatfishException("Exiting due to parser errors");
      parseResults.stream()
          .map(ParseVendorConfigurationResult::getFailureCause)
          .filter(Objects::nonNull)
          .forEach(e::addSuppressed);
      throw e;
    }

    _logger.infof(
        "Snapshot %s in network %s has total number of network configs:%d",
        getTestrigName(), getContainerName(), parseResults.size());

    /* Assemble answer. */
    SortedMap<String, VendorConfiguration> vendorConfigurations = new TreeMap<>();
    parseResults.forEach(pvcr -> pvcr.applyTo(vendorConfigurations, _logger, answerElement));

    try (ActiveSpan serializeNetworkConfigsSpan =
        GlobalTracer.get().buildSpan("Serialize network configs").startActive()) {
      assert serializeNetworkConfigsSpan != null; // avoid unused warning
      _logger.info("\n*** SERIALIZING VENDOR CONFIGURATION STRUCTURES ***\n");
      _logger.resetTimer();
      createDirectories(outputPath);
      Map<Path, VendorConfiguration> output = new TreeMap<>();
      vendorConfigurations.forEach(
          (name, vc) -> {
            if (name.contains(File.separator)) {
              // iptables will get a hostname like configs/iptables-save if they
              // are not set up correctly using host files
              _logger.errorf("Cannot serialize configuration with hostname %s\n", name);
              answerElement.addRedFlagWarning(
                  name,
                  new Warning(
                      "Cannot serialize network config. Bad hostname " + name.replace("\\", "/"),
                      "MISCELLANEOUS"));
            } else {
              Path currentOutputPath = outputPath.resolve(name);
              output.put(currentOutputPath, vc);
            }
          });

      serializeObjects(output);
      _logger.printElapsedTime();
    }
  }

  private void oldSerializeNetworkConfigs(
      Path userUploadPath,
      Path outputPath,
      ParseVendorConfigurationAnswerElement answerElement,
      SortedMap<String, VendorConfiguration> overlayHostConfigurations) {
    _logger.info("\n*** READING DEVICE CONFIGURATION FILES ***\n");

    Map<String, VendorConfiguration> vendorConfigurations;
    try (ActiveSpan parseNetworkConfigsSpan =
        GlobalTracer.get().buildSpan("Parse network configs").startActive()) {
      assert parseNetworkConfigsSpan != null; // avoid unused warning
      Map<String, String> keyedConfigText =
          readAllFiles(userUploadPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR), _logger)
              .entrySet().stream()
              .collect(
                  ImmutableMap.toImmutableMap(
                      e -> userUploadPath.relativize(e.getKey()).toString(), Entry::getValue));
      vendorConfigurations =
          parseVendorConfigurations(keyedConfigText, answerElement, ConfigurationFormat.UNKNOWN);
    }
    if (vendorConfigurations == null) {
      throw new BatfishException("Exiting due to parser errors");
    }
    _logger.infof(
        "Snapshot %s in network %s has total number of network configs:%d",
        getTestrigName(), getContainerName(), vendorConfigurations.size());

    try (ActiveSpan serializeNetworkConfigsSpan =
        GlobalTracer.get().buildSpan("Serialize network configs").startActive()) {
      assert serializeNetworkConfigsSpan != null; // avoid unused warning
      _logger.info("\n*** SERIALIZING VENDOR CONFIGURATION STRUCTURES ***\n");
      _logger.resetTimer();
      createDirectories(outputPath);
      Map<Path, VendorConfiguration> output = new TreeMap<>();
      vendorConfigurations.forEach(
          (name, vc) -> {
            if (name.contains(File.separator)) {
              // iptables will get a hostname like configs/iptables-save if they
              // are not set up correctly using host files
              _logger.errorf("Cannot serialize configuration with hostname %s\n", name);
              answerElement.addRedFlagWarning(
                  name,
                  new Warning(
                      "Cannot serialize network config. Bad hostname " + name.replace("\\", "/"),
                      "MISCELLANEOUS"));
            } else {
              // apply overlay if it exists
              VendorConfiguration overlayConfig = overlayHostConfigurations.get(name);
              if (overlayConfig != null) {
                vc.setOverlayConfiguration(overlayConfig);
                overlayHostConfigurations.remove(name);
              }

              Path currentOutputPath = outputPath.resolve(name);
              output.put(currentOutputPath, vc);
            }
          });

      // warn about unused overlays
      overlayHostConfigurations.forEach(
          (name, overlay) ->
              answerElement.getParseStatus().put(overlay.getFilename(), ParseStatus.ORPHANED));

      serializeObjects(output);
      _logger.printElapsedTime();
    }
  }

  public <S extends Serializable> void serializeObjects(Map<Path, S> objectsByPath) {
    if (objectsByPath.isEmpty()) {
      return;
    }

    int size = objectsByPath.size();
    String className = objectsByPath.values().iterator().next().getClass().getName();
    AtomicInteger serializeCompleted =
        newBatch(String.format("Serializing '%s' instances to disk", className), size);
    objectsByPath
        .entrySet()
        .parallelStream()
        .forEach(
            entry -> {
              Path outputPath = entry.getKey();
              S object = entry.getValue();
              serializeObject(object, outputPath);
              serializeCompleted.incrementAndGet();
            });
  }

  /**
   * Parses configuration files from the uploaded user data and produces {@link VendorConfiguration
   * vendor-specific configurations} serialized to the given output path.
   *
   * <p>This function should be named better, but it's called by the {@code -sv} argument to Batfish
   * so leaving as-is for now.
   */
  private Answer serializeVendorConfigs(Path userUploadPath, Path outputPath) {
    Answer answer = new Answer();
    boolean configsFound = false;

    ParseVendorConfigurationAnswerElement answerElement =
        new ParseVendorConfigurationAnswerElement();
    answerElement.setVersion(Version.getVersion());
    if (_settings.getVerboseParse()) {
      answer.addAnswerElement(answerElement);
    }

    // look for host configs and overlay configs in the `hosts/` subfolder.
    SortedMap<String, VendorConfiguration> overlayHostConfigurations = new TreeMap<>();
    if (Files.exists(userUploadPath.resolve(BfConsts.RELPATH_HOST_CONFIGS_DIR))) {
      overlayHostConfigurations = serializeHostConfigs(userUploadPath, outputPath, answerElement);
      configsFound = true;
    }

    // look for network configs in the `configs/` subfolder.
    if (Files.exists(userUploadPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR))) {
      serializeNetworkConfigs(userUploadPath, outputPath, answerElement, overlayHostConfigurations);
      configsFound = true;
    }

    // look for AWS VPC configs in the `aws_configs/` subfolder.
    if (Files.exists(userUploadPath.resolve(BfConsts.RELPATH_AWS_CONFIGS_DIR))) {
      serializeAwsConfigs(userUploadPath, outputPath, answerElement);
      configsFound = true;
    }

    if (!configsFound) {
      throw new BatfishException(
          "No valid configurations found in snapshot path " + userUploadPath);
    }

    // serialize warnings
    serializeObject(answerElement, _testrigSettings.getParseAnswerPath());

    return answer;
  }

  @Override
  public void registerDataPlanePlugin(DataPlanePlugin plugin, String name) {
    _dataPlanePlugins.put(name, plugin);
  }

  public void setTerminatingExceptionMessage(String terminatingExceptionMessage) {
    _terminatingExceptionMessage = terminatingExceptionMessage;
  }

  @Override
  public SpecifierContext specifierContext() {
    return new SpecifierContextImpl(this, getNetworkSnapshot());
  }

  @Override
  public SpecifierContext specifierContext(NetworkSnapshot networkSnapshot) {
    return new SpecifierContextImpl(this, networkSnapshot);
  }

  @Override
  public BidirectionalReachabilityResult bidirectionalReachability(
      BDDPacket bddPacket, ReachabilityParameters parameters) {
    ResolvedReachabilityParameters params;
    try {
      params = resolveReachabilityParameters(this, parameters, getNetworkSnapshot());
    } catch (InvalidReachabilityParametersException e) {
      throw new BatfishException("Error resolving reachability parameters", e);
    }

    return new BidirectionalReachabilityAnalysis(
            bddPacket,
            loadConfigurations(),
            loadDataPlane().getForwardingAnalysis(),
            params.getSourceIpAssignment(),
            params.getHeaderSpace(),
            params.getForbiddenTransitNodes(),
            params.getRequiredTransitNodes(),
            params.getFinalNodes(),
            params.getActions())
        .getResult();
  }

  @Override
  public AnswerElement standard(ReachabilityParameters reachabilityParameters) {
    return bddSingleReachability(reachabilityParameters);
  }

  public AnswerElement bddSingleReachability(ReachabilityParameters parameters) {
    try (ActiveSpan span = GlobalTracer.get().buildSpan("bddSingleReachability").startActive()) {
      assert span != null; // avoid not used warning
      ResolvedReachabilityParameters params;
      try {
        params = resolveReachabilityParameters(this, parameters, getNetworkSnapshot());
      } catch (InvalidReachabilityParametersException e) {
        return e.getInvalidParametersAnswer();
      }

      checkArgument(
          params.getSrcNatted() == SrcNattedConstraint.UNCONSTRAINED,
          "Requiring or forbidding Source NAT is currently unsupported");

      BDDPacket pkt = new BDDPacket();
      boolean ignoreFilters = params.getIgnoreFilters();
      BDDReachabilityAnalysisFactory bddReachabilityAnalysisFactory =
          getBddReachabilityAnalysisFactory(pkt, ignoreFilters);

      Map<IngressLocation, BDD> reachableBDDs =
          bddReachabilityAnalysisFactory.getAllBDDs(
              params.getSourceIpAssignment(),
              params.getHeaderSpace(),
              params.getForbiddenTransitNodes(),
              params.getRequiredTransitNodes(),
              params.getFinalNodes(),
              params.getActions());

      String flowTag = getFlowTag();
      Set<Flow> flows =
          reachableBDDs.entrySet().stream()
              .flatMap(
                  entry -> {
                    IngressLocation loc = entry.getKey();
                    BDD headerSpace = entry.getValue();
                    Optional<Flow.Builder> optionalFlow = pkt.getFlow(headerSpace);
                    if (!optionalFlow.isPresent()) {
                      return Stream.of();
                    }
                    Flow.Builder flow = optionalFlow.get();
                    flow.setIngressNode(loc.getNode());
                    flow.setTag(flowTag);
                    switch (loc.getType()) {
                      case INTERFACE_LINK:
                        flow.setIngressInterface(loc.getInterface());
                        break;
                      case VRF:
                        flow.setIngressVrf(loc.getVrf());
                        break;
                      default:
                        throw new BatfishException(
                            "Unexpected IngressLocation Type: " + loc.getType().name());
                    }
                    return Stream.of(flow.build());
                  })
              .collect(ImmutableSet.toImmutableSet());

      return new TraceWrapperAsAnswerElement(buildFlows(flows, ignoreFilters));
    }
  }

  @Override
  public Set<Flow> bddLoopDetection() {
    try (ActiveSpan span = GlobalTracer.get().buildSpan("bddLoopDetection").startActive()) {
      assert span != null; // avoid unused warning
      BDDPacket pkt = new BDDPacket();
      // TODO add ignoreFilters parameter
      boolean ignoreFilters = false;
      BDDReachabilityAnalysisFactory bddReachabilityAnalysisFactory =
          getBddReachabilityAnalysisFactory(pkt, ignoreFilters);
      BDDLoopDetectionAnalysis analysis =
          bddReachabilityAnalysisFactory.bddLoopDetectionAnalysis(
              getAllSourcesInferFromLocationIpSpaceAssignment());
      Map<IngressLocation, BDD> loopBDDs = analysis.detectLoops();

      String flowTag = getFlowTag();
      try (ActiveSpan span1 =
          GlobalTracer.get().buildSpan("bddLoopDetection.computeResultFlows").startActive()) {
        assert span1 != null; // avoid unused warning
        return loopBDDs.entrySet().stream()
            .map(
                entry ->
                    pkt.getFlow(entry.getValue())
                        .map(
                            fb -> {
                              IngressLocation loc = entry.getKey();
                              fb.setTag(flowTag);
                              fb.setIngressNode(loc.getNode());
                              switch (loc.getType()) {
                                case INTERFACE_LINK:
                                  fb.setIngressInterface(loc.getInterface());
                                  break;
                                case VRF:
                                  fb.setIngressVrf(loc.getVrf());
                                  break;
                                default:
                                  throw new BatfishException(
                                      "Unknown Location Type: " + loc.getType());
                              }
                              return fb.build();
                            }))
            .flatMap(optional -> optional.map(Stream::of).orElse(Stream.empty()))
            .collect(ImmutableSet.toImmutableSet());
      }
    }
  }

  @Override
  public Set<Flow> bddMultipathConsistency(MultipathConsistencyParameters parameters) {
    try (ActiveSpan span = GlobalTracer.get().buildSpan("bddMultipathConsistency").startActive()) {
      assert span != null; // avoid unused warning
      BDDPacket pkt = new BDDPacket();
      // TODO add ignoreFilters parameter
      boolean ignoreFilters = false;
      BDDReachabilityAnalysisFactory bddReachabilityAnalysisFactory =
          getBddReachabilityAnalysisFactory(pkt, ignoreFilters);
      IpSpaceAssignment srcIpSpaceAssignment = parameters.getSrcIpSpaceAssignment();
      Set<String> finalNodes = parameters.getFinalNodes();
      Set<FlowDisposition> failureDispositions =
          ImmutableSet.of(
              FlowDisposition.DENIED_IN,
              FlowDisposition.DENIED_OUT,
              FlowDisposition.LOOP,
              FlowDisposition.INSUFFICIENT_INFO,
              FlowDisposition.NEIGHBOR_UNREACHABLE,
              FlowDisposition.NO_ROUTE,
              FlowDisposition.NULL_ROUTED);
      Set<FlowDisposition> successDispositions =
          ImmutableSet.of(
              FlowDisposition.ACCEPTED,
              FlowDisposition.DELIVERED_TO_SUBNET,
              FlowDisposition.EXITS_NETWORK);
      Set<String> forbiddenTransitNodes = parameters.getForbiddenTransitNodes();
      Set<String> requiredTransitNodes = parameters.getRequiredTransitNodes();
      Map<IngressLocation, BDD> successBdds =
          bddReachabilityAnalysisFactory.getAllBDDs(
              srcIpSpaceAssignment,
              parameters.getHeaderSpace(),
              forbiddenTransitNodes,
              requiredTransitNodes,
              finalNodes,
              successDispositions);
      Map<IngressLocation, BDD> failureBdds =
          bddReachabilityAnalysisFactory.getAllBDDs(
              srcIpSpaceAssignment,
              parameters.getHeaderSpace(),
              forbiddenTransitNodes,
              requiredTransitNodes,
              finalNodes,
              failureDispositions);

      return ImmutableSet.copyOf(
          computeMultipathInconsistencies(pkt, getFlowTag(), successBdds, failureBdds));
    }
  }

  @Nonnull
  public IpSpaceAssignment getAllSourcesInferFromLocationIpSpaceAssignment() {
    SpecifierContextImpl specifierContext = new SpecifierContextImpl(this, getNetworkSnapshot());
    Set<Location> locations =
        new UnionLocationSpecifier(
                AllInterfacesLocationSpecifier.INSTANCE,
                AllInterfaceLinksLocationSpecifier.INSTANCE)
            .resolve(specifierContext);
    return InferFromLocationIpSpaceSpecifier.INSTANCE.resolve(locations, specifierContext);
  }

  @Nonnull
  private BDDReachabilityAnalysisFactory getBddReachabilityAnalysisFactory(
      BDDPacket pkt, boolean ignoreFilters) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("getBddReachabilityAnalysisFactory").startActive()) {
      assert span != null; // avoid unused warning
      return new BDDReachabilityAnalysisFactory(
          pkt, loadConfigurations(), loadDataPlane().getForwardingAnalysis(), ignoreFilters, false);
    }
  }

  @Nonnull
  public BDDReachabilityAnalysis getBddReachabilityAnalysis(
      BDDPacket pkt,
      IpSpaceAssignment srcIpSpaceAssignment,
      AclLineMatchExpr initialHeaderSpace,
      Set<String> forbiddenTransitNodes,
      Set<String> requiredTransitNodes,
      Set<String> finalNodes,
      Set<FlowDisposition> actions,
      boolean ignoreFilters) {
    BDDReachabilityAnalysisFactory factory = getBddReachabilityAnalysisFactory(pkt, ignoreFilters);
    return factory.bddReachabilityAnalysis(
        srcIpSpaceAssignment,
        initialHeaderSpace,
        forbiddenTransitNodes,
        requiredTransitNodes,
        finalNodes,
        actions);
  }

  /**
   * Return a set of flows (at most 1 per source {@link Location}) for which reachability has been
   * reduced by the change from base to delta snapshot.
   */
  @Override
  public DifferentialReachabilityResult bddDifferentialReachability(
      DifferentialReachabilityParameters parameters) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("bddDifferentialReachability").startActive()) {
      assert span != null; // avoid unused warning
      checkArgument(
          !parameters.getFlowDispositions().isEmpty(), "Must specify at least one FlowDisposition");
      BDDPacket pkt = new BDDPacket();

      AclLineMatchExpr headerSpace =
          parameters.getInvertSearch()
              ? not(parameters.getHeaderSpace())
              : parameters.getHeaderSpace();

      /*
       * TODO should we have separate parameters for base and delta?
       * E.g. suppose we add a host subnet in the delta network. This would be a source of
       * differential reachability, but we currently won't find it because it won't be in the
       * IpSpaceAssignment.
       */
      pushBaseSnapshot();
      Map<IngressLocation, BDD> baseAcceptBDDs =
          getBddReachabilityAnalysisFactory(pkt, parameters.getIgnoreFilters())
              .getAllBDDs(
                  parameters.getIpSpaceAssignment(),
                  headerSpace,
                  parameters.getForbiddenTransitNodes(),
                  parameters.getRequiredTransitNodes(),
                  parameters.getFinalNodes(),
                  parameters.getFlowDispositions());
      popSnapshot();

      pushDeltaSnapshot();
      Map<IngressLocation, BDD> deltaAcceptBDDs =
          getBddReachabilityAnalysisFactory(pkt, parameters.getIgnoreFilters())
              .getAllBDDs(
                  parameters.getIpSpaceAssignment(),
                  headerSpace,
                  parameters.getForbiddenTransitNodes(),
                  parameters.getRequiredTransitNodes(),
                  parameters.getFinalNodes(),
                  parameters.getFlowDispositions());
      popSnapshot();

      Set<IngressLocation> commonSources =
          Sets.intersection(baseAcceptBDDs.keySet(), deltaAcceptBDDs.keySet());
      String flowTag = getDifferentialFlowTag();

      Set<Flow> decreasedFlows =
          getDifferentialFlows(pkt, commonSources, baseAcceptBDDs, deltaAcceptBDDs, flowTag);
      Set<Flow> increasedFlows =
          getDifferentialFlows(pkt, commonSources, deltaAcceptBDDs, baseAcceptBDDs, flowTag);
      return new DifferentialReachabilityResult(increasedFlows, decreasedFlows);
    }
  }

  private static Set<Flow> getDifferentialFlows(
      BDDPacket pkt,
      Set<IngressLocation> commonSources,
      Map<IngressLocation, BDD> includeBDDs,
      Map<IngressLocation, BDD> excludeBDDs,
      String flowTag) {
    return commonSources.stream()
        .flatMap(
            source -> {
              BDD difference = includeBDDs.get(source).diff(excludeBDDs.get(source));

              if (difference.isZero()) {
                return Stream.of();
              }

              Flow.Builder flow =
                  pkt.getFlow(difference)
                      .orElseThrow(() -> new BatfishException("Error getting flow from BDD"));

              // set flow parameters
              flow.setTag(flowTag);
              flow.setIngressNode(source.getNode());
              switch (source.getType()) {
                case VRF:
                  flow.setIngressVrf(source.getVrf());
                  break;
                case INTERFACE_LINK:
                  flow.setIngressInterface(source.getInterface());
                  break;
                default:
                  throw new BatfishException("Unexpected IngressLocationType: " + source.getType());
              }
              return Stream.of(flow.build());
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  private void writeJsonAnswer(String structuredAnswerString) {
    SnapshotId deltaSnapshot = _settings.getDiffQuestion() ? _deltaTestrigSettings.getName() : null;
    NetworkId networkId = _settings.getContainer();
    QuestionId questionId = _settings.getQuestionName();
    AnalysisId analysisId = _settings.getAnalysisName();
    QuestionSettingsId questionSettingsId;
    try {
      String questionClassId = _storage.loadQuestionClassId(networkId, questionId, analysisId);
      if (_idResolver.hasQuestionSettingsId(questionClassId, networkId)) {
        questionSettingsId = _idResolver.getQuestionSettingsId(questionClassId, networkId);
      } else {
        questionSettingsId = QuestionSettingsId.DEFAULT_QUESTION_SETTINGS_ID;
      }
    } catch (IOException e) {
      throw new BatfishException("Failed to retrieve question settings ID", e);
    }
    NodeRolesId networkNodeRolesId =
        _idResolver.hasNetworkNodeRolesId(networkId)
            ? _idResolver.getNetworkNodeRolesId(networkId)
            : NodeRolesId.DEFAULT_NETWORK_NODE_ROLES_ID;
    AnswerId baseAnswerId =
        _idResolver.getBaseAnswerId(
            networkId,
            _baseTestrigSettings.getName(),
            questionId,
            questionSettingsId,
            networkNodeRolesId,
            deltaSnapshot,
            analysisId);
    _storage.storeAnswer(structuredAnswerString, baseAnswerId);
  }

  private void writeJsonAnswerWithLog(@Nullable String logString, String structuredAnswerString) {
    // Write log of WorkItem task to the configured path for logs
    if (logString != null && _settings.getTaskId() != null) {
      Path jsonPath =
          _settings
              .getStorageBase()
              .resolve(_settings.getContainer().getId())
              .resolve(BfConsts.RELPATH_SNAPSHOTS_DIR)
              .resolve(_settings.getTestrig().getId())
              .resolve(BfConsts.RELPATH_OUTPUT)
              .resolve(_settings.getTaskId() + BfConsts.SUFFIX_ANSWER_JSON_FILE);
      CommonUtil.writeFile(jsonPath, logString);
    }
    // Write answer.json and answer-pretty.json if WorkItem was answering a question
    if (_settings.getQuestionName() != null) {
      writeJsonAnswer(structuredAnswerString);
    }
  }

  @Override
  public @Nullable String loadQuestionSettings(@Nonnull Question question) {
    String questionClassId = question.getName();
    NetworkId networkId = _settings.getContainer();
    if (!_idResolver.hasQuestionSettingsId(questionClassId, networkId)) {
      return null;
    }
    try {
      QuestionSettingsId questionSettingsId =
          _idResolver.getQuestionSettingsId(questionClassId, networkId);
      return _storage.loadQuestionSettings(_settings.getContainer(), questionSettingsId);
    } catch (IOException e) {
      throw new BatfishException(
          String.format("Failed to read question settings for question: '%s'", questionClassId), e);
    }
  }

  @Override
  public @Nullable Answerer createAnswerer(@Nonnull Question question) {
    AnswererCreator creator = _answererCreators.get(question.getName());
    return creator != null ? creator.create(question, this) : null;
  }
}
