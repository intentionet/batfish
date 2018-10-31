package org.batfish.main;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static java.util.stream.Collectors.toMap;
import static org.batfish.bddreachability.BDDMultipathInconsistency.computeMultipathInconsistencies;
import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.acl.SourcesReferencedByIpAccessLists.SOURCE_ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.SourcesReferencedByIpAccessLists.referencedSources;
import static org.batfish.main.ReachabilityParametersResolver.resolveReachabilityParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.bddreachability.BDDReachabilityAnalysis;
import org.batfish.bddreachability.BDDReachabilityAnalysisFactory;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishException.BatfishStackTrace;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.CleanBatfishException;
import org.batfish.common.CoordConsts;
import org.batfish.common.CoordConstsV2;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Pair;
import org.batfish.common.Version;
import org.batfish.common.Warning;
import org.batfish.common.Warnings;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.HeaderSpaceToBDD;
import org.batfish.common.plugin.BgpTablePlugin;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.plugin.DataPlanePlugin.ComputeDataPlaneResult;
import org.batfish.common.plugin.ExternalBgpAdvertisementPlugin;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.PluginClientType;
import org.batfish.common.plugin.PluginConsumer;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.Layer3Topology;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.config.TestrigSettings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.DeviceType;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.GenericConfigObject;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.RipNeighbor;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclExplainer;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.AnswerMetadataUtil;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.DataPlaneAnswerElement;
import org.batfish.datamodel.answers.FlattenVendorConfigurationAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.InitStepAnswerElement;
import org.batfish.datamodel.answers.MajorIssueConfig;
import org.batfish.datamodel.answers.NodAnswerElement;
import org.batfish.datamodel.answers.ParseAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentBgpTablesAnswerElement;
import org.batfish.datamodel.answers.ParseEnvironmentRoutingTablesAnswerElement;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.answers.RunAnalysisAnswerElement;
import org.batfish.datamodel.answers.ValidateSnapshotAnswerElement;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.flow.TraceWrapperAsAnswerElement;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.datamodel.questions.InvalidReachabilityParametersException;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.smt.HeaderLocationQuestion;
import org.batfish.datamodel.questions.smt.HeaderQuestion;
import org.batfish.datamodel.questions.smt.RoleQuestion;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BgpTableFormat;
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
import org.batfish.job.ParseVendorConfigurationJob;
import org.batfish.question.ReachabilityParameters;
import org.batfish.question.ResolvedReachabilityParameters;
import org.batfish.question.SearchFiltersParameters;
import org.batfish.question.SrcNattedConstraint;
import org.batfish.question.differentialreachability.DifferentialReachabilityParameters;
import org.batfish.question.differentialreachability.DifferentialReachabilityResult;
import org.batfish.question.multipath.MultipathConsistencyParameters;
import org.batfish.question.searchfilters.DifferentialSearchFiltersResult;
import org.batfish.question.searchfilters.SearchFiltersResult;
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
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationVisitor;
import org.batfish.specifier.SpecifierContext;
import org.batfish.specifier.SpecifierContextImpl;
import org.batfish.specifier.UnionLocationSpecifier;
import org.batfish.storage.FileBasedStorage;
import org.batfish.storage.StorageProvider;
import org.batfish.symbolic.abstraction.BatfishCompressor;
import org.batfish.symbolic.abstraction.Roles;
import org.batfish.symbolic.bdd.BDDAcl;
import org.batfish.symbolic.smt.PropertyChecker;
import org.batfish.vendor.VendorConfiguration;
import org.batfish.z3.BlacklistDstIpQuerySynthesizer;
import org.batfish.z3.CompositeNodJob;
import org.batfish.z3.IngressLocation;
import org.batfish.z3.LocationToIngressLocation;
import org.batfish.z3.MultipathInconsistencyQuerySynthesizer;
import org.batfish.z3.NodJob;
import org.batfish.z3.QuerySynthesizer;
import org.batfish.z3.ReachEdgeQuerySynthesizer;
import org.batfish.z3.ReachabilityQuerySynthesizer;
import org.batfish.z3.StandardReachabilityQuerySynthesizer;
import org.batfish.z3.Synthesizer;
import org.batfish.z3.SynthesizerInputImpl;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.OrExpr;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/** This class encapsulates the main control logic for Batfish. */
public class Batfish extends PluginConsumer implements IBatfish {

  public static final String DIFFERENTIAL_FLOW_TAG = "DIFFERENTIAL";

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
          ParseTreeWalker walker = new ParseTreeWalker();
          walker.walk(flattener, tree);
          return flattener;
        }

      case VYOS:
        {
          VyosCombinedParser parser = new VyosCombinedParser(input, settings);
          ParserRuleContext tree = parse(parser, logger, settings);
          VyosFlattener flattener = new VyosFlattener(header);
          ParseTreeWalker walker = new ParseTreeWalker();
          walker.walk(flattener, tree);
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
   * Returns a sorted list of {@link Path paths} contains all files under the directory indicated by
   * {@code configsPath}. Directories under {@code configsPath} are recursively expanded but not
   * included in the returned list.
   *
   * <p>Temporary files(files start with {@code .} are omitted from the returned list.
   *
   * <p>This method follows all symbolic links.
   */
  static List<Path> listAllFiles(Path configsPath) {
    List<Path> configFilePaths;
    try (Stream<Path> allFiles = Files.walk(configsPath, FileVisitOption.FOLLOW_LINKS)) {
      configFilePaths =
          allFiles
              .filter(
                  path ->
                      !path.getFileName().toString().startsWith(".") && Files.isRegularFile(path))
              .sorted()
              .collect(Collectors.toList());
    } catch (IOException e) {
      throw new BatfishException("Failed to walk path: " + configsPath, e);
    }
    return configFilePaths;
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

  public static ParserRuleContext parse(
      BatfishCombinedParser<?, ?> parser, BatfishLogger logger, Settings settings) {
    ParserRuleContext tree;
    try {
      tree = parser.parse();
    } catch (BatfishException e) {
      throw new ParserBatfishException("Parser error", e);
    }
    List<String> errors = parser.getErrors();
    int numErrors = errors.size();
    if (numErrors > 0) {
      logger.error(numErrors + " ERROR(S)\n");
      for (int i = 0; i < numErrors; i++) {
        String prefix = "ERROR " + (i + 1) + ": ";
        String msg = errors.get(i);
        String prefixedMsg = CommonUtil.applyPrefix(prefix, msg);
        logger.error(prefixedMsg + "\n");
      }
      throw new ParserBatfishException("Parser error(s)");
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

  private final Map<String, BiFunction<Question, IBatfish, Answerer>> _answererCreators;

  private TestrigSettings _baseTestrigSettings;

  private SortedMap<BgpTableFormat, BgpTablePlugin> _bgpTablePlugins;

  private final Cache<NetworkSnapshot, SortedMap<String, Configuration>>
      _cachedCompressedConfigurations;

  private final Cache<NetworkSnapshot, SortedMap<String, Configuration>> _cachedConfigurations;

  private final Cache<NetworkSnapshot, DataPlane> _cachedCompressedDataPlanes;

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

  public Batfish(
      Settings settings,
      Cache<NetworkSnapshot, SortedMap<String, Configuration>> cachedCompressedConfigurations,
      Cache<NetworkSnapshot, SortedMap<String, Configuration>> cachedConfigurations,
      Cache<NetworkSnapshot, DataPlane> cachedCompressedDataPlanes,
      Cache<NetworkSnapshot, DataPlane> cachedDataPlanes,
      Map<NetworkSnapshot, SortedMap<String, BgpAdvertisementsByVrf>> cachedEnvironmentBgpTables,
      Map<NetworkSnapshot, SortedMap<String, RoutesByVrf>> cachedEnvironmentRoutingTables,
      @Nullable StorageProvider alternateStorageProvider,
      @Nullable IdResolver alternateIdResolver) {
    super(settings.getSerializeToText());
    _settings = settings;
    _bgpTablePlugins = new TreeMap<>();
    _cachedCompressedConfigurations = cachedCompressedConfigurations;
    _cachedConfigurations = cachedConfigurations;
    _cachedCompressedDataPlanes = cachedCompressedDataPlanes;
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

    try (ActiveSpan loadConfigurationSpan =
        GlobalTracer.get().buildSpan("Load configurations").startActive()) {
      assert loadConfigurationSpan != null; // avoid not used warning
      // Ensures configurations are parsed and ready
      loadConfigurations();
    }

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

  private void computeAggregatedInterfaceBandwidth(
      Interface iface, Map<String, Interface> interfaces) {
    if (iface.getInterfaceType() != InterfaceType.AGGREGATED) {
      return;
    }
    /* Bandwidth should be sum of bandwidth of channel-group members. */
    iface.setBandwidth(
        iface
            .getChannelGroupMembers()
            .stream()
            .mapToDouble(ifaceName -> interfaces.get(ifaceName).getBandwidth())
            .sum());
  }

  public static Warnings buildWarnings(Settings settings) {
    return new Warnings(
        settings.getPedanticRecord() && settings.getLogger().isActive(BatfishLogger.LEVEL_PEDANTIC),
        settings.getRedFlagRecord() && settings.getLogger().isActive(BatfishLogger.LEVEL_REDFLAG),
        settings.getUnimplementedRecord()
            && settings.getLogger().isActive(BatfishLogger.LEVEL_UNIMPLEMENTED));
  }

  @Override
  public void checkDataPlane() {
    checkDataPlane(_testrigSettings);
  }

  public static void checkDataPlane(TestrigSettings testrigSettings) {
    if (!Files.exists(testrigSettings.getDataPlanePath())) {
      throw new CleanBatfishException(
          "Missing data plane for testrig: \"" + testrigSettings.getName() + "\"\n");
    }
  }

  public void checkDifferentialDataPlaneQuestionDependencies() {
    checkDataPlane(_baseTestrigSettings);
    checkDataPlane(_deltaTestrigSettings);
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

  public Set<Flow> computeCompositeNodOutput(
      List<CompositeNodJob> jobs, NodAnswerElement answerElement) {
    _logger.info("\n*** EXECUTING COMPOSITE NOD JOBS ***\n");
    _logger.resetTimer();
    Set<Flow> flows = new TreeSet<>();
    BatfishJobExecutor.runJobsInExecutor(
        _settings, _logger, jobs, flows, answerElement, true, "Composite NOD");
    _logger.printElapsedTime();
    return flows;
  }

  private CompressDataPlaneResult computeCompressedDataPlane() {
    CompressDataPlaneResult result = computeCompressedDataPlane(new HeaderSpace());
    _cachedCompressedConfigurations.put(
        getNetworkSnapshot(), new TreeMap<>(result._compressedConfigs));
    saveDataPlane(result._compressedDataPlane, result._answerElement, true);
    return result;
  }

  public class CompressDataPlaneResult {
    public final Map<String, Configuration> _compressedConfigs;
    public final DataPlane _compressedDataPlane;
    public final DataPlaneAnswerElement _answerElement;

    public CompressDataPlaneResult(
        Map<String, Configuration> compressedConfigs,
        DataPlane compressedDataPlane,
        DataPlaneAnswerElement answerElement) {
      _compressedConfigs = compressedConfigs;
      _compressedDataPlane = compressedDataPlane;
      _answerElement = answerElement;
    }
  }

  CompressDataPlaneResult computeCompressedDataPlane(HeaderSpace headerSpace) {
    // Since compression mutates the configurations, we must clone them before that happens.
    // A simple way to do this is to create a deep clone of each entry using Java serialization.
    _logger.info("Computing compressed dataplane\n");
    Map<String, Configuration> clonedConfigs =
        loadConfigurations()
            .entrySet()
            .parallelStream()
            .collect(toMap(Entry::getKey, entry -> SerializationUtils.clone(entry.getValue())));

    Map<String, Configuration> configs =
        new BatfishCompressor(new BDDPacket(), this, clonedConfigs).compress(headerSpace);
    Topology topo = CommonUtil.synthesizeTopology(configs);
    DataPlanePlugin dataPlanePlugin = getDataPlanePlugin();
    ComputeDataPlaneResult result = dataPlanePlugin.computeDataPlane(false, configs, topo);

    _storage.storeCompressedConfigurations(
        configs, _settings.getContainer(), _testrigSettings.getName());
    return new CompressDataPlaneResult(configs, result._dataPlane, result._answerElement);
  }

  @Override
  public DataPlaneAnswerElement computeDataPlane(boolean differentialContext) {
    checkSnapshotOutputReady();
    ComputeDataPlaneResult result = getDataPlanePlugin().computeDataPlane(differentialContext);
    saveDataPlane(result._dataPlane, result._answerElement, false);
    return result._answerElement;
  }

  /* Write the dataplane to disk and cache, and write the answer element to disk.
   */
  private void saveDataPlane(
      DataPlane dataPlane, DataPlaneAnswerElement answerElement, boolean compressed) {
    Path dataPlanePath =
        compressed
            ? _testrigSettings.getCompressedDataPlanePath()
            : _testrigSettings.getDataPlanePath();

    Path answerElementPath =
        compressed
            ? _testrigSettings.getCompressedDataPlaneAnswerPath()
            : _testrigSettings.getDataPlaneAnswerPath();

    Cache<NetworkSnapshot, DataPlane> cache =
        compressed ? _cachedCompressedDataPlanes : _cachedDataPlanes;

    cache.put(getNetworkSnapshot(), dataPlane);

    _logger.resetTimer();
    newBatch("Writing data plane to disk", 0);
    try (ActiveSpan writeDataplane =
        GlobalTracer.get().buildSpan("Writing data plane").startActive()) {
      assert writeDataplane != null; // avoid unused warning
      serializeObject(dataPlane, dataPlanePath);
      serializeObject(answerElement, answerElementPath);
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

  Topology computeEnvironmentTopology(Map<String, Configuration> configurations) {
    _logger.resetTimer();
    Topology topology = computeTestrigTopology(configurations);
    topology.prune(getEdgeBlacklist(), getNodeBlacklist(), getInterfaceBlacklist());
    _logger.printElapsedTime();
    return topology;
  }

  public Set<Flow> computeNodOutput(List<NodJob> jobs) {
    _logger.info("\n*** EXECUTING NOD JOBS ***\n");
    _logger.resetTimer();
    Set<Flow> flows = new TreeSet<>();
    BatfishJobExecutor.runJobsInExecutor(
        _settings, _logger, jobs, flows, new NodAnswerElement(), true, "NOD");
    _logger.printElapsedTime();
    return flows;
  }

  @VisibleForTesting
  Topology computeTestrigTopology(Map<String, Configuration> configurations) {
    Topology legacyTopology =
        _storage.loadLegacyTopology(_settings.getContainer(), _testrigSettings.getName());
    if (legacyTopology != null) {
      return legacyTopology;
    }
    Layer1Topology rawLayer1Topology =
        _storage.loadLayer1Topology(_settings.getContainer(), _testrigSettings.getName());
    if (rawLayer1Topology != null) {
      _logger.infof(
          "Testrig:%s in container:%s has layer-1 topology file",
          getTestrigName(), getContainerName());
      newBatch("Processing layer-1 topology", 0);
      Layer1Topology layer1Topology =
          TopologyUtil.computeLayer1Topology(rawLayer1Topology, configurations);
      newBatch("Computing layer-2 topology", 0);
      Layer2Topology layer2Topology =
          TopologyUtil.computeLayer2Topology(layer1Topology, configurations);
      newBatch("Computing layer-3 topology", 0);
      Layer3Topology layer3Topology =
          TopologyUtil.computeLayer3Topology(layer2Topology, configurations);
      return TopologyUtil.toTopology(layer3Topology);
    }
    // guess adjacencies based on interface subnetworks
    _logger.info("*** (GUESSING TOPOLOGY IN ABSENCE OF EXPLICIT FILE) ***\n");
    return CommonUtil.synthesizeTopology(configurations);
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

  private boolean compressedDataPlaneDependenciesExist(TestrigSettings testrigSettings) {
    Path path = testrigSettings.getCompressedDataPlaneAnswerPath();
    return Files.exists(path);
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
        List<SubRange> vlans = new ArrayList<>();
        if (iface.getSwitchportMode() == SwitchportMode.TRUNK) { // vlan trunked interface
          Collection<SubRange> allowed = iface.getAllowedVlans();
          if (!allowed.isEmpty()) {
            // Explicit list of allowed VLANs
            vlans.addAll(allowed);
          } else {
            // No explicit list, so all VLANs are allowed.
            vlanInterfaces.keySet().forEach(v -> vlans.add(new SubRange(v, v)));
          }
          // Add the native VLAN as well.
          vlanNumber = iface.getNativeVlan();
          vlans.add(new SubRange(vlanNumber, vlanNumber));
        } else if (iface.getSwitchportMode() == SwitchportMode.ACCESS) { // access mode ACCESS
          vlanNumber = iface.getAccessVlan();
          vlans.add(new SubRange(vlanNumber, vlanNumber));
          // Any other Switch Port mode is unsupported
        } else if (iface.getSwitchportMode() != SwitchportMode.NONE) {
          _logger.warnf(
              "WARNING: Unsupported switch port mode %s, assuming no VLANs allowed: \"%s:%s\"\n",
              iface.getSwitchportMode(), hostname, iface.getName());
        }

        for (SubRange sr : vlans) {
          for (int vlanId = sr.getStart(); vlanId <= sr.getEnd(); ++vlanId) {
            vlanMemberCounts.compute(vlanId, (k, v) -> (v == null) ? 1 : (v + 1));
          }
        }
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
              iface.setActive(false);
              iface.setBlacklisted(true);
            }
          }
        }
      }
    }
  }

  private void disableUnusableVpnInterfaces(Map<String, Configuration> configurations) {
    CommonUtil.initRemoteIpsecVpns(configurations);
    for (Configuration c : configurations.values()) {
      for (IpsecVpn vpn : c.getIpsecVpns().values()) {
        Interface bindInterface = vpn.getBindInterface();
        if (bindInterface == null) {
          // Nothing to disable.
          continue;
        }

        if (bindInterface.getInterfaceType() == InterfaceType.PHYSICAL) {
          // Skip tunnels bound to physical interfaces (aka, Cisco interface crypto-map).
          continue;
        }

        IpsecVpn remoteVpn = vpn.getRemoteIpsecVpn();
        if (remoteVpn == null
            || !vpn.compatibleIkeProposals(remoteVpn)
            || !vpn.compatibleIpsecProposals(remoteVpn)
            || !vpn.compatiblePreSharedKey(remoteVpn)) {
          String hostname = c.getHostname();
          bindInterface.setActive(false);
          bindInterface.setBlacklisted(true);
          String bindInterfaceName = bindInterface.getName();
          _logger.warnf(
              "WARNING: Disabling unusable vpn interface because we cannot determine remote "
                  + "endpoint: \"%s:%s\"\n",
              hostname, bindInterfaceName);
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

  private void flatten(Path inputPath, Path outputPath) {
    Map<Path, String> configurationData =
        readConfigurationFiles(inputPath, BfConsts.RELPATH_CONFIGURATIONS_DIR);
    Map<Path, String> outputConfigurationData = new TreeMap<>();
    Path outputConfigDir = outputPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR);
    CommonUtil.createDirectories(outputConfigDir);
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

  @Deprecated
  @Override
  public Map<String, BiFunction<Question, IBatfish, Answerer>> getAnswererCreators() {
    return _answererCreators;
  }

  public TestrigSettings getBaseTestrigSettings() {
    return _baseTestrigSettings;
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
    SortedSet<Edge> edgeBlackList = getEdgeBlacklist();
    SortedSet<NodeInterfacePair> interfaceBlackList = getInterfaceBlacklist();
    SortedSet<String> nodeBlackList = getNodeBlacklist();
    // TODO: add bgp tables and external announcements as well
    return new Environment(
        _settings.getSnapshotName(),
        edgeBlackList,
        interfaceBlackList,
        nodeBlackList,
        null,
        null,
        null);
  }

  private SortedMap<String, BgpAdvertisementsByVrf> getEnvironmentBgpTables(
      Path inputPath, ParseEnvironmentBgpTablesAnswerElement answerElement) {
    if (Files.exists(inputPath.getParent()) && !Files.exists(inputPath)) {
      return new TreeMap<>();
    }
    SortedMap<Path, String> inputData = readFiles(inputPath, "Environment BGP Tables");
    SortedMap<String, BgpAdvertisementsByVrf> bgpTables =
        parseEnvironmentBgpTables(inputData, answerElement);
    return bgpTables;
  }

  private SortedMap<String, RoutesByVrf> getEnvironmentRoutingTables(
      Path inputPath, ParseEnvironmentRoutingTablesAnswerElement answerElement) {
    if (Files.exists(inputPath.getParent()) && !Files.exists(inputPath)) {
      return new TreeMap<>();
    }
    SortedMap<Path, String> inputData = readFiles(inputPath, "Environment Routing Tables");
    SortedMap<String, RoutesByVrf> routingTables =
        parseEnvironmentRoutingTables(inputData, answerElement);
    return routingTables;
  }

  @Override
  public Topology getEnvironmentTopology() {
    try {
      return BatfishObjectMapper.mapper()
          .readValue(
              CommonUtil.readFile(_testrigSettings.getSerializeTopologyPath()), Topology.class);
    } catch (IOException e) {
      throw new BatfishException("Could not getEnvironmentTopology: ", e);
    }
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

  @Override
  public FlowHistory getHistory() {
    FlowHistory flowHistory = new FlowHistory();
    if (_settings.getDiffQuestion()) {
      String flowTag = getDifferentialFlowTag();
      String baseEnvTag = getFlowTag(_baseTestrigSettings);
      String deltaEnvTag = getFlowTag(_deltaTestrigSettings);
      pushBaseSnapshot();
      Environment baseEnv = getEnvironment();
      populateFlowHistory(flowHistory, baseEnvTag, baseEnv, flowTag);
      popSnapshot();
      pushDeltaSnapshot();
      Environment deltaEnv = getEnvironment();
      populateFlowHistory(flowHistory, deltaEnvTag, deltaEnv, flowTag);
      popSnapshot();
    } else {
      String flowTag = getFlowTag();
      String envTag = flowTag;
      Environment env = getEnvironment();
      populateFlowHistory(flowHistory, envTag, env, flowTag);
    }
    _logger.debug(flowHistory.toString());
    return flowHistory;
  }

  @Nonnull
  private SortedSet<Edge> getEdgeBlacklist() {
    SortedSet<Edge> blacklistEdges =
        _storage.loadEdgeBlacklist(_settings.getContainer(), _settings.getTestrig());
    if (blacklistEdges == null) {
      return Collections.emptySortedSet();
    }
    return blacklistEdges;
  }

  @Nonnull
  private SortedSet<NodeInterfacePair> getInterfaceBlacklist() {
    SortedSet<NodeInterfacePair> blacklistInterfaces =
        _storage.loadInterfaceBlacklist(_settings.getContainer(), _settings.getTestrig());
    if (blacklistInterfaces == null) {
      return Collections.emptySortedSet();
    }
    return blacklistInterfaces;
  }

  @Nonnull
  private SortedSet<String> getNodeBlacklist() {
    SortedSet<String> blacklistNodes =
        _storage.loadNodeBlacklist(_settings.getContainer(), _settings.getTestrig());
    if (blacklistNodes == null) {
      return Collections.emptySortedSet();
    }
    return blacklistNodes;
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
    try {
      NodeRolesData nodeRolesData = getNodeRolesData();
      return nodeRolesData.getNodeRoleDimension(dimension);
    } catch (IOException e) {
      _logger.errorf("Could not read roles data: %s", e);
      return Optional.empty();
    }
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

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getRoutes(
      boolean useCompression) {
    return getDataPlanePlugin().getRoutes(loadDataPlane(useCompression));
  }

  public Settings getSettings() {
    return _settings;
  }

  @Override
  public ImmutableConfiguration getSettingsConfiguration() {
    return _settings.getImmutableConfiguration();
  }

  NetworkSnapshot getNetworkSnapshot() {
    return new NetworkSnapshot(_settings.getContainer(), _testrigSettings.getName());
  }

  private Set<Edge> getSymmetricEdgePairs(SortedSet<Edge> edges) {
    Set<Edge> consumedEdges = new LinkedHashSet<>();
    for (Edge edge : edges) {
      if (consumedEdges.contains(edge)) {
        continue;
      }
      consumedEdges.add(edge);
      consumedEdges.add(edge.reverse());
    }
    return consumedEdges;
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

  @Override
  public PluginClientType getType() {
    return PluginClientType.BATFISH;
  }

  @Override
  public void initBgpOriginationSpaceExplicit(Map<String, Configuration> configurations) {
    // ProtocolDependencyAnalysis protocolDependencyAnalysis = new
    // ProtocolDependencyAnalysis(
    // configurations);
    // DependencyDatabase database = protocolDependencyAnalysis
    // .getDependencyDatabase();
    //
    // for (Entry<String, Configuration> e : configurations.entrySet()) {
    // PrefixSpace ebgpExportSpace = new PrefixSpace();
    // String name = e.getKey();
    // Configuration node = e.getValue();
    // BgpProcess proc = node.getBgpProcess();
    // if (proc != null) {
    // Set<PotentialExport> bgpExports = database.getPotentialExports(name,
    // RoutingProtocol.BGP);
    // for (PotentialExport export : bgpExports) {
    // DependentRoute exportSourceRoute = export.getDependency();
    // if (!exportSourceRoute.dependsOn(RoutingProtocol.BGP)
    // && !exportSourceRoute.dependsOn(RoutingProtocol.IBGP)) {
    // Prefix prefix = export.getIp();
    // ebgpExportSpace.addPrefix(prefix);
    // }
    // }
    // proc.setOriginationSpace(ebgpExportSpace);
    // }
    // }
  }

  @Override
  public InitInfoAnswerElement initInfo(boolean summary, boolean verboseError) {
    ParseVendorConfigurationAnswerElement parseAnswer = loadParseVendorConfigurationAnswerElement();
    InitInfoAnswerElement answerElement = mergeParseAnswer(summary, verboseError, parseAnswer);
    mergeConvertAnswer(summary, verboseError, answerElement);
    _logger.info(answerElement.prettyPrint());
    return answerElement;
  }

  @Override
  public InitInfoAnswerElement initInfoBgpAdvertisements(boolean summary, boolean verboseError) {
    ParseEnvironmentBgpTablesAnswerElement parseAnswer =
        loadParseEnvironmentBgpTablesAnswerElement();
    InitInfoAnswerElement answerElement = mergeParseAnswer(summary, verboseError, parseAnswer);
    _logger.info(answerElement.prettyPrint());
    return answerElement;
  }

  @Override
  public InitInfoAnswerElement initInfoRoutes(boolean summary, boolean verboseError) {
    ParseEnvironmentRoutingTablesAnswerElement parseAnswer =
        loadParseEnvironmentRoutingTablesAnswerElement();
    InitInfoAnswerElement answerElement = mergeParseAnswer(summary, verboseError, parseAnswer);
    _logger.info(answerElement.prettyPrint());
    return answerElement;
  }

  private void prepareToAnswerQuestions(boolean dp, boolean differentialContext) {
    if (!outputExists(_testrigSettings)) {
      CommonUtil.createDirectories(_testrigSettings.getOutputPath());
    }
    if (!environmentBgpTablesExist(_testrigSettings)) {
      computeEnvironmentBgpTables();
    }
    if (!environmentRoutingTablesExist(_testrigSettings)) {
      computeEnvironmentRoutingTables();
    }
    if (dp) {
      if (!dataPlaneDependenciesExist(_testrigSettings)) {
        computeDataPlane(differentialContext);
      }

      if (!compressedDataPlaneDependenciesExist(_testrigSettings)) {
        // computeCompressedDataPlane();
      }
    }
  }

  private void prepareToAnswerQuestions(boolean diff, boolean diffActive, boolean dp) {
    if (diff || !diffActive) {
      pushBaseSnapshot();
      prepareToAnswerQuestions(dp, false);
      popSnapshot();
    }
    if (diff || diffActive) {
      pushDeltaSnapshot();
      prepareToAnswerQuestions(dp, true);
      popSnapshot();
    }
  }

  @Override
  public void initRemoteRipNeighbors(
      Map<String, Configuration> configurations, Map<Ip, Set<String>> ipOwners, Topology topology) {
    for (Entry<String, Configuration> e : configurations.entrySet()) {
      String hostname = e.getKey();
      Configuration c = e.getValue();
      for (Entry<String, Vrf> e2 : c.getVrfs().entrySet()) {
        Vrf vrf = e2.getValue();
        RipProcess proc = vrf.getRipProcess();
        if (proc != null) {
          proc.setRipNeighbors(new TreeMap<>());
          String vrfName = e2.getKey();
          for (String ifaceName : proc.getInterfaces()) {
            Interface iface = vrf.getInterfaces().get("ifaceName");
            SortedSet<Edge> ifaceEdges =
                topology.getInterfaceEdges().get(new NodeInterfacePair(hostname, ifaceName));
            boolean hasNeighbor = false;
            Ip localIp = iface.getAddress().getIp();
            if (ifaceEdges != null) {
              for (Edge edge : ifaceEdges) {
                if (edge.getNode1().equals(hostname)) {
                  String remoteHostname = edge.getNode2();
                  String remoteIfaceName = edge.getInt2();
                  Configuration remoteNode = configurations.get(remoteHostname);
                  Interface remoteIface = remoteNode.getAllInterfaces().get(remoteIfaceName);
                  Vrf remoteVrf = remoteIface.getVrf();
                  String remoteVrfName = remoteVrf.getName();
                  RipProcess remoteProc = remoteVrf.getRipProcess();
                  if (remoteProc != null) {
                    if (remoteProc.getRipNeighbors() == null) {
                      remoteProc.setRipNeighbors(new TreeMap<>());
                    }
                    if (remoteProc.getInterfaces().contains(remoteIfaceName)) {
                      Ip remoteIp = remoteIface.getAddress().getIp();
                      Pair<Ip, Ip> localKey = new Pair<>(localIp, remoteIp);
                      RipNeighbor neighbor = proc.getRipNeighbors().get(localKey);
                      if (neighbor == null) {
                        hasNeighbor = true;

                        // initialize local neighbor
                        neighbor = new RipNeighbor(localKey);
                        neighbor.setVrf(vrfName);
                        neighbor.setOwner(c);
                        neighbor.setInterface(iface);
                        proc.getRipNeighbors().put(localKey, neighbor);

                        // initialize remote neighbor
                        Pair<Ip, Ip> remoteKey = new Pair<>(remoteIp, localIp);
                        RipNeighbor remoteNeighbor = new RipNeighbor(remoteKey);
                        remoteNeighbor.setVrf(remoteVrfName);
                        remoteNeighbor.setOwner(remoteNode);
                        remoteNeighbor.setInterface(remoteIface);
                        remoteProc.getRipNeighbors().put(remoteKey, remoteNeighbor);

                        // link neighbors
                        neighbor.setRemoteRipNeighbor(remoteNeighbor);
                        remoteNeighbor.setRemoteRipNeighbor(neighbor);
                      }
                    }
                  }
                }
              }
            }
            if (!hasNeighbor) {
              Pair<Ip, Ip> key = new Pair<>(localIp, Ip.ZERO);
              RipNeighbor neighbor = new RipNeighbor(key);
              neighbor.setVrf(vrfName);
              neighbor.setOwner(c);
              neighbor.setInterface(iface);
              proc.getRipNeighbors().put(key, neighbor);
            }
          }
        }
      }
    }
  }

  @Override
  public SortedMap<String, Configuration> loadConfigurations() {
    NetworkSnapshot snapshot = getNetworkSnapshot();
    _logger.debugf("Loading configurations for %s\n", snapshot);
    return loadConfigurations(snapshot);
  }

  SortedMap<String, Configuration> loadCompressedConfigurations(NetworkSnapshot snapshot) {
    // Do we already have configurations in the cache?
    SortedMap<String, Configuration> configurations =
        _cachedCompressedConfigurations.getIfPresent(snapshot);
    if (configurations != null) {
      return configurations;
    }
    _logger.debugf("Loading configurations for %s, cache miss", snapshot);

    // Next, see if we have an up-to-date configurations on disk.
    configurations =
        _storage.loadCompressedConfigurations(_settings.getContainer(), snapshot.getSnapshot());
    if (configurations != null) {
      return configurations;
    } else {
      computeCompressedDataPlane();
      configurations = _cachedCompressedConfigurations.getIfPresent(snapshot);
      if (configurations == null) {
        throw new BatfishException("Could not compute compressed configs");
      }
      return configurations;
    }
  }

  /**
   * Returns the configurations for given snapshot, including any environmental features such as
   * failed links.
   */
  SortedMap<String, Configuration> loadConfigurations(NetworkSnapshot snapshot) {
    // Do we already have configurations in the cache?
    SortedMap<String, Configuration> configurations = _cachedConfigurations.getIfPresent(snapshot);
    if (configurations != null) {
      return configurations;
    }
    _logger.debugf("Loading configurations for %s, cache miss", snapshot);

    // Next, see if we have an up-to-date configurations on disk.
    configurations = _storage.loadConfigurations(snapshot.getNetwork(), snapshot.getSnapshot());
    if (configurations != null) {
      _logger.debugf("Loaded configurations for %s off disk", snapshot);
      applyEnvironment(configurations);
    } else {
      // Otherwise, we have to parse the configurations. Fall back to old, hacky code.
      configurations = parseConfigurationsAndApplyEnvironment();
    }

    _cachedConfigurations.put(snapshot, configurations);
    return configurations;
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
    applyEnvironment(configurations);
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
    return loadDataPlane(false);
  }

  DataPlane loadDataPlane(boolean compressed) {
    Cache<NetworkSnapshot, DataPlane> cache =
        compressed ? _cachedCompressedDataPlanes : _cachedDataPlanes;

    Path path =
        compressed
            ? _testrigSettings.getCompressedDataPlanePath()
            : _testrigSettings.getDataPlanePath();

    NetworkSnapshot snapshot = getNetworkSnapshot();
    DataPlane dp = cache.getIfPresent(snapshot);
    if (dp == null) {
      /*
       * Data plane should exist after loading answer element, as it triggers
       * repair if necessary. However, it might not be cached if it was not
       * repaired, so we still might need to load it from disk.
       */
      loadDataPlaneAnswerElement(compressed);
      dp = cache.getIfPresent(snapshot);
      if (dp == null) {
        newBatch("Loading data plane from disk", 0);
        dp = deserializeObject(path, DataPlane.class);
        cache.put(snapshot, dp);
      }
    }
    return dp;
  }

  private DataPlaneAnswerElement loadDataPlaneAnswerElement(boolean compressed) {
    return loadDataPlaneAnswerElement(compressed, true);
  }

  private DataPlaneAnswerElement loadDataPlaneAnswerElement(
      boolean compressed, boolean firstAttempt) {
    Path answerPath =
        compressed
            ? _testrigSettings.getCompressedDataPlaneAnswerPath()
            : _testrigSettings.getDataPlaneAnswerPath();

    DataPlaneAnswerElement bae = deserializeObject(answerPath, DataPlaneAnswerElement.class);
    if (!Version.isCompatibleVersion("Service", "Old data plane", bae.getVersion())) {
      if (firstAttempt) {
        repairDataPlane(compressed);
        return loadDataPlaneAnswerElement(compressed, false);
      } else {
        throw new BatfishException(
            "Version error repairing data plane for data plane answer element");
      }
    } else {
      return bae;
    }
  }

  @Override
  public SortedMap<String, BgpAdvertisementsByVrf> loadEnvironmentBgpTables() {
    NetworkSnapshot snapshot = getNetworkSnapshot();
    SortedMap<String, BgpAdvertisementsByVrf> environmentBgpTables =
        _cachedEnvironmentBgpTables.get(snapshot);
    if (environmentBgpTables == null) {
      ParseEnvironmentBgpTablesAnswerElement ae = loadParseEnvironmentBgpTablesAnswerElement();
      if (!Version.isCompatibleVersion(
          "Service", "Old processed environment BGP tables", ae.getVersion())) {
        repairEnvironmentBgpTables();
      }
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
    ParseEnvironmentBgpTablesAnswerElement ae =
        deserializeObject(answerPath, ParseEnvironmentBgpTablesAnswerElement.class);
    if (!Version.isCompatibleVersion(
        "Service", "Old processed environment BGP tables", ae.getVersion())) {
      if (firstAttempt) {
        repairEnvironmentRoutingTables();
        return loadParseEnvironmentBgpTablesAnswerElement(false);
      } else {
        throw new BatfishException(
            "Version error repairing environment BGP tables for parse environment BGP tables "
                + "answer element");
      }
    } else {
      return ae;
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
      ParseVendorConfigurationAnswerElement pvcae =
          deserializeObject(
              _testrigSettings.getParseAnswerPath(), ParseVendorConfigurationAnswerElement.class);
      if (Version.isCompatibleVersion(
          "Service", "Old processed configurations", pvcae.getVersion())) {
        return pvcae;
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

  private ValidateSnapshotAnswerElement loadValidateSnapshotAnswerElement() {
    return loadValidateSnapshotAnswerElement(true);
  }

  private ValidateSnapshotAnswerElement loadValidateSnapshotAnswerElement(boolean firstAttempt) {
    Path answerPath = _testrigSettings.getValidateSnapshotAnswerPath();
    if (Files.exists(answerPath)) {
      ValidateSnapshotAnswerElement veae =
          deserializeObject(answerPath, ValidateSnapshotAnswerElement.class);
      if (Version.isCompatibleVersion("Service", "Old processed environment", veae.getVersion())) {
        return veae;
      }
    }
    if (firstAttempt) {
      parseConfigurationsAndApplyEnvironment();
      return loadValidateSnapshotAnswerElement(false);
    } else {
      throw new BatfishException(
          "Version error repairing environment for validate environment answer element");
    }
  }

  private void mergeConvertAnswer(
      boolean summary, boolean verboseError, InitInfoAnswerElement answerElement) {
    ConvertConfigurationAnswerElement convertAnswer =
        loadConvertConfigurationAnswerElementOrReparse();
    mergeInitStepAnswer(answerElement, convertAnswer, summary, verboseError);
    for (String failed : convertAnswer.getFailed()) {
      answerElement.getParseStatus().put(failed, ParseStatus.FAILED);
    }
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
  public AnswerElement multipath(ReachabilityParameters reachabilityParameters) {
    return singleReachability(
        reachabilityParameters, MultipathInconsistencyQuerySynthesizer.builder());
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
      String answerString = BatfishObjectMapper.writePrettyString(answer) + '\n';
      _logger.debug(answerString);
      @Nullable String logString = writeLog ? answerString : null;
      writeJsonAnswerWithLog(logString, answerString);
    } catch (Exception e) {
      BatfishException be = new BatfishException("Error in sending answer", e);
      try {
        Answer failureAnswer = Answer.failureAnswer(e.toString(), answer.getQuestion());
        failureAnswer.addAnswerElement(be.getBatfishStackTrace());
        String answerString = BatfishObjectMapper.writePrettyString(failureAnswer) + '\n';
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

  private ParserRuleContext parse(BatfishCombinedParser<?, ?> parser) {
    return parse(parser, _logger, _settings);
  }

  public ParserRuleContext parse(BatfishCombinedParser<?, ?> parser, String filename) {
    _logger.infof("Parsing: \"%s\"...", filename);
    return parse(parser);
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

  private SortedMap<String, VendorConfiguration> parseVendorConfigurations(
      Map<Path, String> configurationData,
      ParseVendorConfigurationAnswerElement answerElement,
      ConfigurationFormat configurationFormat) {
    _logger.info("\n*** PARSING VENDOR CONFIGURATION FILES ***\n");
    _logger.resetTimer();
    SortedMap<String, VendorConfiguration> vendorConfigurations = new TreeMap<>();
    List<ParseVendorConfigurationJob> jobs = new ArrayList<>();
    for (Entry<Path, String> vendorFile : configurationData.entrySet()) {
      Path currentFile = vendorFile.getKey();
      String fileText = vendorFile.getValue();

      Warnings warnings = buildWarnings(_settings);
      String filename =
          _settings.getActiveTestrigSettings().getInputPath().relativize(currentFile).toString();
      ParseVendorConfigurationJob job =
          new ParseVendorConfigurationJob(
              _settings, fileText, filename, warnings, configurationFormat);
      jobs.add(job);
    }
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
  public AnswerElement pathDiff(ReachabilityParameters reachabilityParameters) {
    Settings settings = getSettings();
    checkDifferentialDataPlaneQuestionDependencies();
    String tag = getDifferentialFlowTag();

    ResolvedReachabilityParameters baseParameters;

    // load base configurations and generate base data plane
    pushBaseSnapshot();
    Topology baseTopology = getEnvironmentTopology();
    try {
      baseParameters =
          resolveReachabilityParameters(this, reachabilityParameters, getNetworkSnapshot());
    } catch (InvalidReachabilityParametersException e) {
      return e.getInvalidParametersAnswer();
    }

    Map<String, Configuration> baseConfigurations = baseParameters.getConfigurations();
    Synthesizer baseDataPlaneSynthesizer = synthesizeDataPlane(baseParameters);
    popSnapshot();

    // load delta configurations and generate delta data plane
    ResolvedReachabilityParameters deltaParameters;
    pushDeltaSnapshot();
    try {
      deltaParameters =
          resolveReachabilityParameters(this, reachabilityParameters, getNetworkSnapshot());
    } catch (InvalidReachabilityParametersException e) {
      return e.getInvalidParametersAnswer();
    }

    Map<String, Configuration> diffConfigurations = deltaParameters.getConfigurations();
    Synthesizer diffDataPlaneSynthesizer = synthesizeDataPlane(deltaParameters);
    Topology diffTopology = getEnvironmentTopology();
    popSnapshot();

    pushDeltaSnapshot();
    SortedSet<String> blacklistNodes = getNodeBlacklist();
    Set<NodeInterfacePair> blacklistInterfaces = getInterfaceBlacklist();
    SortedSet<Edge> blacklistEdges = getEdgeBlacklist();
    popSnapshot();

    BlacklistDstIpQuerySynthesizer blacklistQuery =
        new BlacklistDstIpQuerySynthesizer(
            null, blacklistNodes, blacklistInterfaces, blacklistEdges, baseConfigurations);

    // compute composite program and flows
    List<Synthesizer> commonEdgeSynthesizers =
        ImmutableList.of(
            baseDataPlaneSynthesizer, diffDataPlaneSynthesizer, baseDataPlaneSynthesizer);

    List<CompositeNodJob> jobs = new ArrayList<>();

    Map<IngressLocation, BooleanExpr> srcIpConstraints =
        baseDataPlaneSynthesizer.getInput().getSrcIpConstraints();

    Set<Location> sourceLocations =
        baseParameters
            .getSourceIpAssignment()
            .getEntries()
            .stream()
            .flatMap(entry -> entry.getLocations().stream())
            .collect(ImmutableSet.toImmutableSet());

    // generate local edge reachability and black hole queries
    SortedSet<Edge> diffEdges = diffTopology.getEdges();
    for (Edge edge : diffEdges) {
      String ingressNode = edge.getNode1();
      String outInterface = edge.getInt1();

      // skip if the source interface is not specified by the user
      if (!sourceLocations.contains(new InterfaceLocation(ingressNode, outInterface))) {
        continue;
      }

      String vrf =
          diffConfigurations
              .get(ingressNode)
              .getAllInterfaces()
              .get(outInterface)
              .getVrf()
              .getName();
      IngressLocation ingressLocation = IngressLocation.vrf(ingressNode, vrf);
      BooleanExpr srcIpConstraint = srcIpConstraints.get(ingressLocation);

      ReachEdgeQuerySynthesizer reachQuery =
          new ReachEdgeQuerySynthesizer(
              ingressNode, vrf, edge, true, reachabilityParameters.getHeaderSpace());
      ReachEdgeQuerySynthesizer noReachQuery =
          new ReachEdgeQuerySynthesizer(ingressNode, vrf, edge, true, TRUE);
      noReachQuery.setNegate(true);
      List<QuerySynthesizer> queries = ImmutableList.of(reachQuery, noReachQuery, blacklistQuery);
      CompositeNodJob job =
          new CompositeNodJob(
              settings,
              commonEdgeSynthesizers,
              queries,
              ImmutableMap.of(ingressLocation, srcIpConstraint),
              reachabilityParameters.getSpecialize(),
              tag);
      jobs.add(job);
    }

    // we also need queries for nodes next to edges that are now missing,
    // in the case that those nodes still exist
    List<Synthesizer> missingEdgeSynthesizers =
        ImmutableList.of(baseDataPlaneSynthesizer, baseDataPlaneSynthesizer);
    SortedSet<Edge> baseEdges = baseTopology.getEdges();
    SortedSet<Edge> missingEdges = ImmutableSortedSet.copyOf(Sets.difference(baseEdges, diffEdges));
    for (Edge missingEdge : missingEdges) {
      String ingressNode = missingEdge.getNode1();
      String outInterface = missingEdge.getInt1();

      // skip if the source interface is not specified by the user
      if (!sourceLocations.contains(new InterfaceLocation(ingressNode, outInterface))) {
        continue;
      }

      // skip if the source node or interface has been removed
      if (!diffConfigurations.containsKey(ingressNode)
          || !diffConfigurations.get(ingressNode).getAllInterfaces().containsKey(outInterface)) {
        continue;
      }

      String vrf =
          diffConfigurations
              .get(ingressNode)
              .getAllInterfaces()
              .get(outInterface)
              .getVrf()
              .getName();
      IngressLocation ingressLocation = IngressLocation.vrf(ingressNode, vrf);
      BooleanExpr srcIpConstraint = srcIpConstraints.get(ingressLocation);

      ReachEdgeQuerySynthesizer reachQuery =
          new ReachEdgeQuerySynthesizer(
              ingressNode, vrf, missingEdge, true, reachabilityParameters.getHeaderSpace());
      List<QuerySynthesizer> queries = ImmutableList.of(reachQuery, blacklistQuery);
      CompositeNodJob job =
          new CompositeNodJob(
              settings,
              missingEdgeSynthesizers,
              queries,
              ImmutableMap.of(ingressLocation, srcIpConstraint),
              reachabilityParameters.getSpecialize(),
              tag);
      jobs.add(job);
    }

    // TODO: maybe do something with nod answer element
    Set<Flow> flows = computeCompositeNodOutput(jobs, new NodAnswerElement());
    pushBaseSnapshot();
    DataPlane baseDataPlane = loadDataPlane();
    getDataPlanePlugin().processFlows(flows, baseDataPlane, false);
    popSnapshot();
    pushDeltaSnapshot();
    DataPlane deltaDataPlane = loadDataPlane();
    getDataPlanePlugin().processFlows(flows, deltaDataPlane, false);
    popSnapshot();

    AnswerElement answerElement = getHistory();
    return answerElement;
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

  private void populateFlowHistory(
      FlowHistory flowHistory, String envTag, Environment environment, String flowTag) {
    DataPlanePlugin dataPlanePlugin = getDataPlanePlugin();
    List<Flow> flows = dataPlanePlugin.getHistoryFlows(loadDataPlane());
    List<FlowTrace> flowTraces = dataPlanePlugin.getHistoryFlowTraces(loadDataPlane());
    int numEntries = flows.size();
    for (int i = 0; i < numEntries; i++) {
      Flow flow = flows.get(i);
      if (flow.getTag().equals(flowTag)) {
        FlowTrace flowTrace = flowTraces.get(i);
        flowHistory.addFlowTrace(flow, envTag, environment, flowTrace);
      }
    }
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

    /* Disable aggregated interfaces with no members. */
    interfaces
        .values()
        .stream()
        .filter(
            i ->
                i.getInterfaceType() == InterfaceType.AGGREGATED
                    && i.getChannelGroupMembers().isEmpty())
        .forEach(i -> i.setActive(false));

    /* Compute bandwidth for aggregated interfaces. */
    interfaces.values().forEach(iface -> computeAggregatedInterfaceBandwidth(iface, interfaces));
  }

  private void identifyDeviceTypes(Collection<Configuration> configurations) {
    for (Configuration c : configurations) {
      if (c.getDeviceType() != null) {
        continue;
      }
      // Set device type to host iff the configuration format is HOST
      if (c.getConfigurationFormat() == ConfigurationFormat.HOST) {
        c.setDeviceType(DeviceType.HOST);
      } else if (c.getVrfs()
          .values()
          .stream()
          .anyMatch(
              vrf ->
                  vrf.getBgpProcess() != null
                      || !vrf.getEigrpProcesses().isEmpty()
                      || vrf.getOspfProcess() != null
                      || vrf.getRipProcess() != null)) {
        // If any vrf on device has BGP, EIGRP, OSPF, or RIP, set device type to router
        c.setDeviceType(DeviceType.ROUTER);
      } else {
        // If device was not a host or router, call it a switch
        c.setDeviceType(DeviceType.SWITCH);
      }
    }
  }

  private void postProcessForEnvironment(Map<String, Configuration> configurations) {
    postProcessAggregatedInterfaces(configurations);
    postProcessOspfCosts(configurations);
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
                          OspfProcess proc = vrf.getOspfProcess();
                          if (proc != null) {
                            proc.initInterfaceCosts(c);
                          }
                        }));
  }

  private void printSymmetricEdgePairs() {
    Map<String, Configuration> configs = loadConfigurations();
    SortedSet<Edge> edges = CommonUtil.synthesizeTopology(configs).getEdges();
    Set<Edge> symmetricEdgePairs = getSymmetricEdgePairs(edges);
    List<Edge> edgeList = new ArrayList<>(symmetricEdgePairs);
    for (int i = 0; i < edgeList.size() / 2; i++) {
      Edge edge1 = edgeList.get(2 * i);
      Edge edge2 = edgeList.get(2 * i + 1);
      _logger.output(
          edge1.getNode1()
              + ":"
              + edge1.getInt1()
              + ","
              + edge1.getNode2()
              + ":"
              + edge1.getInt2()
              + " "
              + edge2.getNode1()
              + ":"
              + edge2.getInt1()
              + ","
              + edge2.getNode2()
              + ":"
              + edge2.getInt2()
              + "\n");
    }
    _logger.printElapsedTime();
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
   * Reads the external bgp announcement specified in the environment, and populates the
   * vendor-independent configurations with data about those announcements
   *
   * @param configurations The vendor-independent configurations to be modified
   */
  public Set<BgpAdvertisement> processExternalBgpAnnouncements(
      Map<String, Configuration> configurations, SortedSet<Long> allCommunities) {
    Set<BgpAdvertisement> advertSet = new LinkedHashSet<>();
    Path externalBgpAnnouncementsPath = _testrigSettings.getExternalBgpAnnouncementsPath();
    if (Files.exists(externalBgpAnnouncementsPath)) {
      String externalBgpAnnouncementsFileContents =
          CommonUtil.readFile(externalBgpAnnouncementsPath);
      // Populate advertSet with BgpAdvertisements that
      // gets passed to populatePrecomputedBgpAdvertisements.
      // See populatePrecomputedBgpAdvertisements for the things that get
      // extracted from these advertisements.

      try {
        JSONObject jsonObj = new JSONObject(externalBgpAnnouncementsFileContents);

        JSONArray announcements = jsonObj.getJSONArray(BfConsts.PROP_BGP_ANNOUNCEMENTS);

        for (int index = 0; index < announcements.length(); index++) {
          JSONObject announcement = new JSONObject();
          announcement.put("@id", index);
          JSONObject announcementSrc = announcements.getJSONObject(index);
          for (Iterator<?> i = announcementSrc.keys(); i.hasNext(); ) {
            String key = (String) i.next();
            if (!key.equals("@id")) {
              announcement.put(key, announcementSrc.get(key));
            }
          }
          BgpAdvertisement bgpAdvertisement =
              BatfishObjectMapper.mapper()
                  .readValue(announcement.toString(), BgpAdvertisement.class);
          allCommunities.addAll(bgpAdvertisement.getCommunities());
          advertSet.add(bgpAdvertisement);
        }

      } catch (JSONException | IOException e) {
        throw new BatfishException("Problems parsing JSON in " + externalBgpAnnouncementsPath, e);
      }
    }
    return advertSet;
  }

  @Override
  public void processFlows(Set<Flow> flows, boolean ignoreAcls) {
    DataPlane dp = loadDataPlane();
    getDataPlanePlugin().processFlows(flows, dp, ignoreAcls);
  }

  /**
   * Builds the {@link Trace}s for a {@link Set} of {@link Flow}s
   *
   * @param flows {@link Set} of {@link Flow} for which {@link Trace}s are to be found
   * @param ignoreAcls if true, will ignore ACLs
   * @return {@link SortedMap} of {@link Flow}s to {@link List} of {@link Trace}s
   */
  @Override
  public SortedMap<Flow, List<Trace>> buildFlows(Set<Flow> flows, boolean ignoreAcls) {
    DataPlane dp = loadDataPlane();
    return getDataPlanePlugin().buildFlows(flows, dp, ignoreAcls);
  }

  /**
   * Helper function to disable a blacklisted interface and update the given {@link
   * ValidateSnapshotAnswerElement} if the interface does not actually exist.
   */
  private static void blacklistInterface(
      Map<String, Configuration> configurations,
      ValidateSnapshotAnswerElement veae,
      NodeInterfacePair iface) {
    String hostname = iface.getHostname();
    String ifaceName = iface.getInterface();
    @Nullable Configuration node = configurations.get(hostname);
    if (node == null) {
      veae.setValid(false);
      veae.getUndefinedInterfaceBlacklistNodes().add(hostname);
      return;
    }

    @Nullable Interface nodeIface = node.getAllInterfaces().get(ifaceName);
    if (nodeIface == null) {
      veae.setValid(false);
      veae.getUndefinedInterfaceBlacklistInterfaces()
          .computeIfAbsent(hostname, k -> new TreeSet<>())
          .add(ifaceName);
      return;
    }

    nodeIface.setActive(false);
    nodeIface.setBlacklisted(true);
  }

  private void processInterfaceBlacklist(
      Map<String, Configuration> configurations, ValidateSnapshotAnswerElement veae) {
    Set<NodeInterfacePair> blacklistInterfaces = getInterfaceBlacklist();
    for (NodeInterfacePair p : blacklistInterfaces) {
      blacklistInterface(configurations, veae, p);
    }
  }

  private void processNodeBlacklist(
      Map<String, Configuration> configurations, ValidateSnapshotAnswerElement veae) {
    SortedSet<String> blacklistNodes = getNodeBlacklist();
    for (String hostname : blacklistNodes) {
      Configuration node = configurations.get(hostname);
      if (node != null) {
        for (Interface iface : node.getAllInterfaces().values()) {
          iface.setActive(false);
          iface.setBlacklisted(true);
        }
      } else {
        veae.setValid(false);
        veae.getUndefinedNodeBlacklistNodes().add(hostname);
      }
    }
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

  private SortedMap<Path, String> readConfigurationFiles(Path testRigPath, String configsType) {
    _logger.infof("\n*** READING %s FILES ***\n", configsType);
    _logger.resetTimer();
    SortedMap<Path, String> configurationData = new TreeMap<>();
    Path configsPath = testRigPath.resolve(configsType);
    List<Path> configFilePaths = listAllFiles(configsPath);
    AtomicInteger completed =
        newBatch("Reading network configuration files", configFilePaths.size());
    for (Path file : configFilePaths) {
      _logger.debugf("Reading: \"%s\"\n", file);
      String fileTextRaw = CommonUtil.readFile(file.toAbsolutePath());
      String fileText = fileTextRaw + ((fileTextRaw.length() != 0) ? "\n" : "");
      configurationData.put(file, fileText);
      completed.incrementAndGet();
    }
    _logger.printElapsedTime();
    return configurationData;
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

  private SortedMap<Path, String> readFiles(Path directory, String description) {
    _logger.infof("\n*** READING FILES: %s ***\n", description);
    _logger.resetTimer();
    SortedMap<Path, String> fileData = new TreeMap<>();
    List<Path> filePaths;
    try (Stream<Path> paths = CommonUtil.list(directory)) {
      filePaths =
          paths
              .filter(path -> !path.getFileName().toString().startsWith("."))
              .sorted()
              .collect(Collectors.toList());
    }
    AtomicInteger completed = newBatch("Reading files: " + description, filePaths.size());
    for (Path file : filePaths) {
      _logger.debugf("Reading: \"%s\"\n", file);
      String fileTextRaw = CommonUtil.readFile(file.toAbsolutePath());
      String fileText = fileTextRaw + ((fileTextRaw.length() != 0) ? "\n" : "");
      fileData.put(file, fileText);
      completed.incrementAndGet();
    }
    _logger.printElapsedTime();
    return fileData;
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
          } else {
            bfc = new BatfishException(failureMessage);
            if (_settings.getExitOnFirstError()) {
              throw bfc;
            } else {
              failureCauses.add(bfc);
              answerElement.getErrors().put(hostConfig.getHostname(), bfc.getBatfishStackTrace());
              answerElement.getParseStatus().put(hostConfig.getIptablesFile(), ParseStatus.FAILED);
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
  public AnswerElement reducedReachability(ReachabilityParameters params) {
    checkDifferentialDataPlaneQuestionDependencies();
    pushBaseSnapshot();
    ResolvedReachabilityParameters baseParams;
    try {
      baseParams = resolveReachabilityParameters(this, params, getNetworkSnapshot());
    } catch (InvalidReachabilityParametersException e) {
      return e.getInvalidParametersAnswer();
    }
    popSnapshot();

    pushDeltaSnapshot();
    ResolvedReachabilityParameters deltaParams;
    try {
      deltaParams = resolveReachabilityParameters(this, params, getNetworkSnapshot());
    } catch (InvalidReachabilityParametersException e) {
      return e.getInvalidParametersAnswer();
    }
    popSnapshot();

    return reducedReachability(baseParams, deltaParams);
  }

  public AnswerElement reducedReachability(
      ResolvedReachabilityParameters baseParams, ResolvedReachabilityParameters deltaParams) {
    Settings settings = getSettings();
    String tag = getDifferentialFlowTag();

    /* Invaraint: baseParams should agree with deltaParams on all params
     * other than those that are computed by resolution (i.e. those determined
     * by specifiers).
     */
    assert baseParams.getActions().equals(deltaParams.getActions());
    assert baseParams.getHeaderSpace() == deltaParams.getHeaderSpace()
        || baseParams.getHeaderSpace().equals(deltaParams.getHeaderSpace());
    assert baseParams.getSpecialize() == deltaParams.getSpecialize();
    assert baseParams.getSrcNatted().equals(deltaParams.getSrcNatted());

    // push environment so we use the right forwarding analysis.
    pushBaseSnapshot();
    Synthesizer baseDataPlaneSynthesizer = synthesizeDataPlane(baseParams);
    popSnapshot();

    pushDeltaSnapshot();
    Synthesizer diffDataPlaneSynthesizer = synthesizeDataPlane(deltaParams);
    popSnapshot();

    /*
    // TODO refine dstIp to exclude blacklisted destinations
    pushDeltaSnapshot();
    SortedSet<String> blacklistNodes = getNodeBlacklist();
    Set<NodeInterfacePair> blacklistInterfaces = getInterfaceBlacklist();
    SortedSet<Edge> blacklistEdges = getEdgeBlacklist();
    popSnapshot();
    */

    // compute composite program and flows
    List<Synthesizer> synthesizers =
        ImmutableList.of(baseDataPlaneSynthesizer, diffDataPlaneSynthesizer);

    /*
     * Merge the srcIpConstraints
     */
    Map<IngressLocation, BooleanExpr> srcIpConstraints =
        new HashMap<>(baseDataPlaneSynthesizer.getInput().getSrcIpConstraints());
    diffDataPlaneSynthesizer
        .getInput()
        .getSrcIpConstraints()
        .forEach(
            (loc, expr) ->
                srcIpConstraints.merge(
                    loc, expr, (expr1, expr2) -> new OrExpr(ImmutableList.of(expr1, expr2))));

    List<CompositeNodJob> jobs =
        srcIpConstraints
            .entrySet()
            .stream()
            .map(
                entry -> {
                  Map<IngressLocation, BooleanExpr> srcIpConstraint =
                      ImmutableMap.of(entry.getKey(), entry.getValue());
                  // build the query for the base testrig
                  StandardReachabilityQuerySynthesizer baseQuery =
                      StandardReachabilityQuerySynthesizer.builder()
                          .setActions(baseParams.getActions())
                          .setHeaderSpace(baseParams.getHeaderSpace())
                          .setFinalNodes(ImmutableSet.of())
                          .setForbiddenTransitNodes(ImmutableSet.of())
                          .setRequiredTransitNodes(ImmutableSet.of())
                          .setSrcIpConstraints(srcIpConstraint)
                          .setSrcNatted(baseParams.getSrcNatted())
                          .build();
                  // build the query for the delta testrig
                  StandardReachabilityQuerySynthesizer deltaQuery =
                      StandardReachabilityQuerySynthesizer.builder()
                          .setActions(deltaParams.getActions())
                          .setHeaderSpace(deltaParams.getHeaderSpace())
                          .setFinalNodes(ImmutableSet.of())
                          .setForbiddenTransitNodes(ImmutableSet.of())
                          .setRequiredTransitNodes(ImmutableSet.of())
                          .setSrcIpConstraints(srcIpConstraint)
                          .setSrcNatted(deltaParams.getSrcNatted())
                          .build();
                  /*
                   * "Reduced" means flows that match the constraints on the base testrig,
                   * bot not on the delta testrig.
                   */
                  deltaQuery.setNegate(true);
                  List<QuerySynthesizer> queries =
                      ImmutableList.of(baseQuery, deltaQuery /*, blacklistQuery*/);
                  return new CompositeNodJob(
                      settings,
                      synthesizers,
                      queries,
                      srcIpConstraint,
                      baseParams.getSpecialize(),
                      tag);
                })
            .collect(Collectors.toList());

    // TODO: maybe do something with nod answer element
    Set<Flow> flows = computeCompositeNodOutput(jobs, new NodAnswerElement());
    pushBaseSnapshot();
    getDataPlanePlugin().processFlows(flows, loadDataPlane(), false);
    popSnapshot();
    pushDeltaSnapshot();
    getDataPlanePlugin().processFlows(flows, loadDataPlane(), false);
    popSnapshot();

    AnswerElement answerElement = getHistory();
    return answerElement;
  }

  @Override
  public void registerAnswerer(
      String questionName,
      String questionClassName,
      BiFunction<Question, IBatfish, Answerer> answererCreator) {
    _answererCreators.put(questionName, answererCreator);
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

  private void repairDataPlane(boolean compressed) {
    Path dataPlanePath =
        compressed
            ? _testrigSettings.getCompressedDataPlanePath()
            : _testrigSettings.getDataPlanePath();

    Path dataPlaneAnswerPath =
        compressed
            ? _testrigSettings.getCompressedDataPlaneAnswerPath()
            : _testrigSettings.getDataPlaneAnswerPath();

    CommonUtil.deleteIfExists(dataPlanePath);
    CommonUtil.deleteIfExists(dataPlaneAnswerPath);

    if (compressed) {
      computeCompressedDataPlane();
    } else {
      computeDataPlane(false);
    }
  }

  /**
   * Applies the current environment to the specified configurations and updates the given {@link
   * ValidateSnapshotAnswerElement}. Applying the environment includes:
   *
   * <ul>
   *   <li>Applying node and interface blacklists.
   *   <li>Applying node and interface blacklists.
   * </ul>
   */
  private void updateBlacklistedAndInactiveConfigs(
      Map<String, Configuration> configurations, ValidateSnapshotAnswerElement veae) {
    processNodeBlacklist(configurations, veae);
    processInterfaceBlacklist(configurations, veae);
    // We do not process the edge blacklist here. Instead, we rely on these edges being explicitly
    // deleted from the Topology (aka list of edges) that is used along with configurations in
    // answering questions.
    disableUnusableVlanInterfaces(configurations);
    disableUnusableVpnInterfaces(configurations);
  }

  /**
   * Ensures that the current configurations for the current testrig+environment are up to date.
   * Among other things, this includes:
   *
   * <ul>
   *   <li>Invalidating cached configs if the in-memory copy has been changed by question
   *       processing.
   *   <li>Re-loading configurations from disk, including re-parsing if the configs were parsed on a
   *       previous version of Batfish.
   *   <li>Re-applying the environment to the configs, to ensure that blacklists are honored.
   * </ul>
   */
  private void applyEnvironment(Map<String, Configuration> configurationsWithoutEnvironment) {
    ValidateSnapshotAnswerElement veae = new ValidateSnapshotAnswerElement();
    updateBlacklistedAndInactiveConfigs(configurationsWithoutEnvironment, veae);
    postProcessForEnvironment(configurationsWithoutEnvironment);

    serializeObject(veae, _testrigSettings.getValidateSnapshotAnswerPath());
  }

  private void repairEnvironmentBgpTables() {
    Path answerPath = _testrigSettings.getParseEnvironmentBgpTablesAnswerPath();
    Path bgpTablesOutputPath = _testrigSettings.getSerializeEnvironmentBgpTablesPath();
    CommonUtil.deleteIfExists(answerPath);
    CommonUtil.deleteDirectory(bgpTablesOutputPath);
    computeEnvironmentBgpTables();
  }

  private void repairEnvironmentRoutingTables() {
    Path answerPath = _testrigSettings.getParseEnvironmentRoutingTablesAnswerPath();
    Path rtOutputPath = _testrigSettings.getSerializeEnvironmentRoutingTablesPath();
    CommonUtil.deleteIfExists(answerPath);
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
    loadPlugins();
    boolean action = false;
    Answer answer = new Answer();

    if (_settings.getPrintSymmetricEdgePairs()) {
      printSymmetricEdgePairs();
      return answer;
    }

    if (_settings.getSynthesizeJsonTopology()) {
      writeJsonTopology();
      return answer;
    }

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
      answer.addAnswerElement(computeDataPlane(_settings.getDiffActive()));
      action = true;
    }

    if (_settings.getValidateSnapshot()) {
      answer.append(validateSnapshot());
      action = true;
    }

    if (!action) {
      throw new CleanBatfishException("No task performed! Run with -help flag to see usage\n");
    }
    return answer;
  }

  public static void serializeAsJson(Path outputPath, Object object, String objectName) {
    try {
      BatfishObjectMapper.prettyWriter().writeValue(outputPath.toFile(), object);
    } catch (IOException e) {
      throw new BatfishException("Could not serialize " + objectName + " ", e);
    }
  }

  private void serializeAwsConfigs(
      Path testRigPath, Path outputPath, ParseVendorConfigurationAnswerElement pvcae) {
    Map<Path, String> configurationData =
        readConfigurationFiles(testRigPath, BfConsts.RELPATH_AWS_CONFIGS_DIR);
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
    SortedMap<Path, String> configurationData =
        readConfigurationFiles(testRigPath, BfConsts.RELPATH_HOST_CONFIGS_DIR);
    // read the host files
    SortedMap<String, VendorConfiguration> allHostConfigurations;
    try (ActiveSpan parseHostConfigsSpan =
        GlobalTracer.get().buildSpan("Parse host configs").startActive()) {
      assert parseHostConfigsSpan != null; // avoid unused warning
      allHostConfigurations =
          parseVendorConfigurations(configurationData, answerElement, ConfigurationFormat.HOST);
    }
    if (allHostConfigurations == null) {
      throw new BatfishException("Exiting due to parser errors");
    }
    _logger.infof(
        "Testrig:%s in container:%s has total number of host configs:%d",
        getTestrigName(), getContainerName(), allHostConfigurations.size());

    // split into hostConfigurations and overlayConfigurations
    SortedMap<String, VendorConfiguration> overlayConfigurations =
        allHostConfigurations
            .entrySet()
            .stream()
            .filter(e -> ((HostConfiguration) e.getValue()).getOverlay())
            .collect(toMap(Entry::getKey, Entry::getValue, (v1, v2) -> v1, TreeMap::new));
    SortedMap<String, VendorConfiguration> nonOverlayHostConfigurations =
        allHostConfigurations
            .entrySet()
            .stream()
            .filter(e -> !((HostConfiguration) e.getValue()).getOverlay())
            .collect(toMap(Entry::getKey, Entry::getValue, (v1, v2) -> v1, TreeMap::new));

    // read and associate iptables files for specified hosts
    SortedMap<Path, String> iptablesData = new TreeMap<>();
    readIptableFiles(testRigPath, allHostConfigurations, iptablesData, answerElement);

    SortedMap<String, VendorConfiguration> iptablesConfigurations =
        parseVendorConfigurations(iptablesData, answerElement, ConfigurationFormat.IPTABLES);
    for (VendorConfiguration vc : allHostConfigurations.values()) {
      HostConfiguration hostConfig = (HostConfiguration) vc;
      if (hostConfig.getIptablesFile() != null) {
        Path path = Paths.get(testRigPath.toString(), hostConfig.getIptablesFile());
        String relativePathStr = _testrigSettings.getInputPath().relativize(path).toString();
        if (iptablesConfigurations.containsKey(relativePathStr)) {
          hostConfig.setIptablesVendorConfig(
              (IptablesVendorConfiguration) iptablesConfigurations.get(relativePathStr));
        }
      }
    }

    // now, serialize
    _logger.info("\n*** SERIALIZING VENDOR CONFIGURATION STRUCTURES ***\n");
    _logger.resetTimer();
    CommonUtil.createDirectories(outputPath);

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
    Answer answer = new Answer();
    ConvertConfigurationAnswerElement answerElement = new ConvertConfigurationAnswerElement();
    answerElement.setVersion(Version.getVersion());
    if (_settings.getVerboseParse()) {
      answer.addAnswerElement(answerElement);
    }
    Map<String, Configuration> configurations = getConfigurations(vendorConfigPath, answerElement);
    Topology testrigTopology = computeTestrigTopology(configurations);
    serializeAsJson(_testrigSettings.getTopologyPath(), testrigTopology, "testrig topology");
    checkTopology(configurations, testrigTopology);
    org.batfish.datamodel.pojo.Topology pojoTopology =
        org.batfish.datamodel.pojo.Topology.create(
            _settings.getSnapshotName(), configurations, testrigTopology);
    serializeAsJson(_testrigSettings.getPojoTopologyPath(), pojoTopology, "testrig pojo topology");
    _storage.storeConfigurations(
        configurations, answerElement, _settings.getContainer(), _testrigSettings.getName());

    applyEnvironment(configurations);
    Topology envTopology = computeEnvironmentTopology(configurations);
    serializeAsJson(
        _testrigSettings.getSerializeTopologyPath(), envTopology, "environment topology");

    updateSnapshotNodeRoles();
    return answer;
  }

  private void updateSnapshotNodeRoles() {
    // Compute new auto role data and updates existing auto data with it
    NetworkId networkId = _settings.getContainer();
    SnapshotId snapshotId = _settings.getTestrig();
    NodeRolesId snapshotNodeRolesId = _idResolver.getSnapshotNodeRolesId(networkId, snapshotId);
    Set<String> nodeNames = loadConfigurations().keySet();
    Topology envTopology = getEnvironmentTopology();
    SortedSet<NodeRoleDimension> autoRoles = new InferRoles(nodeNames, envTopology).inferRoles();
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

  private void serializeNetworkConfigs(
      Path testRigPath,
      Path outputPath,
      ParseVendorConfigurationAnswerElement answerElement,
      SortedMap<String, VendorConfiguration> overlayHostConfigurations) {
    Map<Path, String> configurationData =
        readConfigurationFiles(testRigPath, BfConsts.RELPATH_CONFIGURATIONS_DIR);
    Map<String, VendorConfiguration> vendorConfigurations;
    try (ActiveSpan parseNetworkConfigsSpan =
        GlobalTracer.get().buildSpan("Parse network configs").startActive()) {
      assert parseNetworkConfigsSpan != null; // avoid unused warning
      vendorConfigurations =
          parseVendorConfigurations(configurationData, answerElement, ConfigurationFormat.UNKNOWN);
    }
    if (vendorConfigurations == null) {
      throw new BatfishException("Exiting due to parser errors");
    }
    _logger.infof(
        "Testrig:%s in container:%s has total number of network configs:%d",
        getTestrigName(), getContainerName(), vendorConfigurations.size());
    _logger.info("\n*** SERIALIZING VENDOR CONFIGURATION STRUCTURES ***\n");
    _logger.resetTimer();
    CommonUtil.createDirectories(outputPath);
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

  Answer serializeVendorConfigs(Path testRigPath, Path outputPath) {
    Answer answer = new Answer();
    boolean configsFound = false;

    // look for network configs
    Path networkConfigsPath = testRigPath.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR);
    ParseVendorConfigurationAnswerElement answerElement =
        new ParseVendorConfigurationAnswerElement();
    answerElement.setVersion(Version.getVersion());
    if (_settings.getVerboseParse()) {
      answer.addAnswerElement(answerElement);
    }

    // look for host configs and overlay configs
    SortedMap<String, VendorConfiguration> overlayHostConfigurations = new TreeMap<>();
    Path hostConfigsPath = testRigPath.resolve(BfConsts.RELPATH_HOST_CONFIGS_DIR);
    if (Files.exists(hostConfigsPath)) {
      overlayHostConfigurations = serializeHostConfigs(testRigPath, outputPath, answerElement);
      configsFound = true;
    }

    if (Files.exists(networkConfigsPath)) {
      serializeNetworkConfigs(testRigPath, outputPath, answerElement, overlayHostConfigurations);
      configsFound = true;
    }

    // look for AWS VPC configs
    Path awsVpcConfigsPath = testRigPath.resolve(BfConsts.RELPATH_AWS_CONFIGS_DIR);
    if (Files.exists(awsVpcConfigsPath)) {
      serializeAwsConfigs(testRigPath, outputPath, answerElement);
      configsFound = true;
    }

    if (!configsFound) {
      throw new BatfishException("No valid configurations found in snapshot path " + testRigPath);
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

  private AnswerElement singleReachability(
      ReachabilityParameters reachabilityParameters,
      ReachabilityQuerySynthesizer.Builder<?, ?> builder) {
    Settings settings = getSettings();
    String tag = getFlowTag(_testrigSettings);

    ResolvedReachabilityParameters parameters;
    try {
      parameters =
          resolveReachabilityParameters(this, reachabilityParameters, getNetworkSnapshot());
    } catch (InvalidReachabilityParametersException e) {
      return e.getInvalidParametersAnswer();
    }

    Set<FlowDisposition> actions = reachabilityParameters.getActions();

    Map<String, Configuration> configurations = parameters.getConfigurations();
    DataPlane dataPlane = parameters.getDataPlane();
    Set<String> forbiddenTransitNodes = parameters.getForbiddenTransitNodes();
    AclLineMatchExpr headerSpace = parameters.getHeaderSpace();
    Set<String> requiredTransitNodes = parameters.getRequiredTransitNodes();
    Synthesizer dataPlaneSynthesizer =
        synthesizeDataPlane(
            configurations,
            dataPlane,
            headerSpace,
            forbiddenTransitNodes,
            requiredTransitNodes,
            parameters.getSourceIpAssignment(),
            reachabilityParameters.getSpecialize());

    Map<IngressLocation, BooleanExpr> srcIpConstraints =
        dataPlaneSynthesizer.getInput().getSrcIpConstraints();

    // chunk ingress locations
    int chunkSize =
        Math.max(
            1,
            Math.min(
                parameters.getMaxChunkSize(),
                srcIpConstraints.size() / _settings.getAvailableThreads()));

    // partition ingress locations into chunks.
    List<List<Entry<IngressLocation, BooleanExpr>>> partitionedIngressLocations =
        Lists.partition(ImmutableList.copyOf(srcIpConstraints.entrySet()), chunkSize);

    List<Map<IngressLocation, BooleanExpr>> chunkedSrcIpConstraints =
        partitionedIngressLocations.stream().map(ImmutableMap::copyOf).collect(Collectors.toList());

    // build query jobs
    List<NodJob> jobs =
        chunkedSrcIpConstraints
            .stream()
            .map(
                chunkSrcIpConstraints -> {
                  ReachabilityQuerySynthesizer query =
                      builder
                          .setActions(actions)
                          .setFinalNodes(parameters.getFinalNodes())
                          .setForbiddenTransitNodes(forbiddenTransitNodes)
                          .setHeaderSpace(headerSpace)
                          .setSrcIpConstraints(chunkSrcIpConstraints)
                          .setRequiredTransitNodes(requiredTransitNodes)
                          .setSrcNatted(parameters.getSrcNatted())
                          .build();

                  return new NodJob(
                      settings,
                      dataPlaneSynthesizer,
                      query,
                      chunkSrcIpConstraints,
                      tag,
                      parameters.getSpecialize());
                })
            .collect(Collectors.toList());

    // run jobs and get resulting flows
    Set<Flow> flows = computeNodOutput(jobs);

    DataPlane dp = loadDataPlane();
    getDataPlanePlugin().processFlows(flows, dp, false);

    AnswerElement answerElement = getHistory();
    return answerElement;
  }

  /** Performs a difference reachFilters analysis (both increased and decreased reachability). */
  @Override
  public DifferentialSearchFiltersResult differentialReachFilter(
      Configuration baseConfig,
      IpAccessList baseAcl,
      Configuration deltaConfig,
      IpAccessList deltaAcl,
      SearchFiltersParameters searchFiltersParameters) {
    BDDPacket bddPacket = new BDDPacket();

    HeaderSpace headerSpace = searchFiltersParameters.resolveHeaderspace(specifierContext());
    BDD headerSpaceBDD =
        new HeaderSpaceToBDD(bddPacket, baseConfig.getIpSpaces()).toBDD(headerSpace);

    // resolve specified source interfaces that exist in both configs.
    Set<String> commonSources =
        Sets.intersection(
            resolveBaseSources(searchFiltersParameters, baseConfig.getHostname()),
            resolveDeltaSources(searchFiltersParameters, deltaConfig.getHostname()));

    Set<String> inactiveInterfaces =
        Sets.union(
            Sets.difference(baseConfig.getAllInterfaces().keySet(), baseConfig.activeInterfaces()),
            Sets.difference(
                deltaConfig.getAllInterfaces().keySet(), deltaConfig.activeInterfaces()));

    // effectively active sources are those of interest that are active in both configs.
    Set<String> activeSources = Sets.difference(commonSources, inactiveInterfaces);

    Set<String> referencedSources =
        Sets.union(
            referencedSources(baseConfig.getIpAccessLists(), baseAcl),
            referencedSources(deltaConfig.getIpAccessLists(), deltaAcl));

    BDDSourceManager mgr = BDDSourceManager.forSources(bddPacket, activeSources, referencedSources);
    BDD baseAclBDD =
        BDDAcl.create(
                bddPacket, baseAcl, baseConfig.getIpAccessLists(), baseConfig.getIpSpaces(), mgr)
            .getBdd()
            .and(headerSpaceBDD)
            .and(mgr.isValidValue());
    BDD deltaAclBDD =
        BDDAcl.create(
                bddPacket, deltaAcl, deltaConfig.getIpAccessLists(), deltaConfig.getIpSpaces(), mgr)
            .getBdd()
            .and(headerSpaceBDD)
            .and(mgr.isValidValue());

    String hostname = baseConfig.getHostname();

    BDD increasedBDD = baseAclBDD.not().and(deltaAclBDD);
    Optional<Flow> increasedFlow = getFlow(bddPacket, mgr, hostname, increasedBDD);

    BDD decreasedBDD = baseAclBDD.and(deltaAclBDD.not());
    Optional<Flow> decreasedFlow = getFlow(bddPacket, mgr, hostname, decreasedBDD);

    boolean explain = searchFiltersParameters.getGenerateExplanations();

    /*
     * Only generate an explanation if the differential headerspace is non-empty (i.e. we found a
     * flow).
     */
    Optional<SearchFiltersResult> increasedResult =
        increasedFlow.map(
            flow ->
                new SearchFiltersResult(
                    flow,
                    !explain
                        ? null
                        : AclExplainer.explainDifferential(
                            bddPacket,
                            mgr,
                            new MatchHeaderSpace(headerSpace),
                            baseAcl,
                            baseConfig.getIpAccessLists(),
                            baseConfig.getIpSpaces(),
                            deltaAcl,
                            deltaConfig.getIpAccessLists(),
                            deltaConfig.getIpSpaces())));

    Optional<SearchFiltersResult> decreasedResult =
        decreasedFlow.map(
            flow ->
                new SearchFiltersResult(
                    flow,
                    !explain
                        ? null
                        : AclExplainer.explainDifferential(
                            bddPacket,
                            mgr,
                            new MatchHeaderSpace(headerSpace),
                            deltaAcl,
                            deltaConfig.getIpAccessLists(),
                            deltaConfig.getIpSpaces(),
                            baseAcl,
                            baseConfig.getIpAccessLists(),
                            baseConfig.getIpSpaces())));

    return new DifferentialSearchFiltersResult(
        increasedResult.orElse(null), decreasedResult.orElse(null));
  }

  private Set<String> resolveDeltaSources(SearchFiltersParameters parameters, String node) {
    pushDeltaSnapshot();
    Set<String> sources = resolveSources(parameters, node);
    popSnapshot();
    return sources;
  }

  private Set<String> resolveBaseSources(SearchFiltersParameters parameters, String node) {
    pushBaseSnapshot();
    Set<String> sources = resolveSources(parameters, node);
    popSnapshot();
    return sources;
  }

  private Set<String> resolveSources(SearchFiltersParameters parameters, String node) {
    LocationVisitor<String> locationToSource =
        new LocationVisitor<String>() {
          @Override
          public String visitInterfaceLinkLocation(InterfaceLinkLocation interfaceLinkLocation) {
            return interfaceLinkLocation.getInterfaceName();
          }

          @Override
          public String visitInterfaceLocation(InterfaceLocation interfaceLocation) {
            return SOURCE_ORIGINATING_FROM_DEVICE;
          }
        };

    return parameters
        .getStartLocationSpecifier()
        .resolve(specifierContext())
        .stream()
        .filter(LocationVisitor.onNode(node)::visit)
        .map(locationToSource::visit)
        .collect(ImmutableSet.toImmutableSet());
  }

  private Optional<Flow> getFlow(
      BDDPacket pkt, BDDSourceManager bddSourceManager, String hostname, BDD bdd) {
    if (bdd.isZero()) {
      return Optional.empty();
    }
    BDD assignment = bdd.fullSatOne();
    return Optional.of(
        pkt.getFlowFromAssignment(assignment)
            .setTag(getFlowTag())
            .setIngressNode(hostname)
            .setIngressInterface(bddSourceManager.getSourceFromAssignment(assignment).orElse(null))
            .build());
  }

  @Override
  public Optional<SearchFiltersResult> reachFilter(
      Configuration node, IpAccessList acl, SearchFiltersParameters parameters) {
    BDDPacket bddPacket = new BDDPacket();

    Set<String> inactiveInterfaces =
        Sets.difference(node.getAllInterfaces().keySet(), node.activeInterfaces());
    Set<String> activeSources =
        Sets.difference(resolveSources(parameters, node.getHostname()), inactiveInterfaces);

    Set<String> referencedSources = referencedSources(node.getIpAccessLists(), acl);

    BDDSourceManager mgr = BDDSourceManager.forSources(bddPacket, activeSources, referencedSources);

    HeaderSpace headerSpace = parameters.resolveHeaderspace(specifierContext());
    BDD headerSpaceBDD = new HeaderSpaceToBDD(bddPacket, node.getIpSpaces()).toBDD(headerSpace);
    BDD bdd =
        BDDAcl.create(bddPacket, acl, node.getIpAccessLists(), node.getIpSpaces(), mgr)
            .getBdd()
            .and(headerSpaceBDD)
            .and(mgr.isValidValue());

    return getFlow(bddPacket, mgr, node.getHostname(), bdd)
        .map(
            flow ->
                new SearchFiltersResult(
                    flow,
                    parameters.getGenerateExplanations()
                        ? AclExplainer.explain(
                            bddPacket,
                            mgr,
                            new MatchHeaderSpace(headerSpace),
                            acl,
                            node.getIpAccessLists(),
                            node.getIpSpaces())
                        : null));
  }

  @Override
  public AnswerElement smtBlackhole(HeaderQuestion q) {
    PropertyChecker p = new PropertyChecker(new BDDPacket(), this, _settings);
    return p.checkBlackHole(q);
  }

  @Override
  public AnswerElement smtBoundedLength(HeaderLocationQuestion q, Integer bound) {
    if (bound == null) {
      throw new BatfishException("Missing parameter length bound: (e.g., bound=3)");
    }
    PropertyChecker p = new PropertyChecker(new BDDPacket(), this, _settings);
    return p.checkBoundedLength(q, bound);
  }

  @Override
  public AnswerElement smtDeterminism(HeaderQuestion q) {
    PropertyChecker p = new PropertyChecker(new BDDPacket(), this, _settings);
    return p.checkDeterminism(q);
  }

  @Override
  public AnswerElement smtEqualLength(HeaderLocationQuestion q) {
    PropertyChecker p = new PropertyChecker(new BDDPacket(), this, _settings);
    return p.checkEqualLength(q);
  }

  @Override
  public AnswerElement smtForwarding(HeaderQuestion q) {
    PropertyChecker p = new PropertyChecker(new BDDPacket(), this, _settings);
    return p.checkForwarding(q);
  }

  @Override
  public AnswerElement smtLoadBalance(HeaderLocationQuestion q, int threshold) {
    PropertyChecker p = new PropertyChecker(new BDDPacket(), this, _settings);
    return p.checkLoadBalancing(q, threshold);
  }

  @Override
  public AnswerElement smtLocalConsistency(Pattern routerRegex, boolean strict, boolean fullModel) {
    PropertyChecker p = new PropertyChecker(new BDDPacket(), this, _settings);
    return p.checkLocalEquivalence(routerRegex, strict, fullModel);
  }

  @Override
  public AnswerElement smtMultipathConsistency(HeaderLocationQuestion q) {
    PropertyChecker p = new PropertyChecker(new BDDPacket(), this, _settings);
    return p.checkMultipathConsistency(q);
  }

  @Override
  public AnswerElement smtReachability(HeaderLocationQuestion q) {
    PropertyChecker p = new PropertyChecker(new BDDPacket(), this, _settings);
    return p.checkReachability(q);
  }

  @Override
  public AnswerElement smtRoles(RoleQuestion q) {
    Roles roles = Roles.create(this, q.getDstIps(), new NodesSpecifier(q.getNodeRegex()));
    return roles.asAnswer(q.getType());
  }

  @Override
  public AnswerElement smtRoutingLoop(HeaderQuestion q) {
    PropertyChecker p = new PropertyChecker(new BDDPacket(), this, _settings);
    return p.checkRoutingLoop(q);
  }

  @Override
  public SpecifierContext specifierContext() {
    return new SpecifierContextImpl(this, loadConfigurations());
  }

  @Override
  public AnswerElement standard(ReachabilityParameters reachabilityParameters) {
    if (debugFlagEnabled("useNodReachability")) {
      return singleReachability(
          reachabilityParameters, StandardReachabilityQuerySynthesizer.builder());
    }
    return bddSingleReachability(reachabilityParameters);
  }

  public AnswerElement bddSingleReachability(ReachabilityParameters parameters) {
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
    boolean ignoreAcls = params.getIgnoreAcls();
    BDDReachabilityAnalysisFactory bddReachabilityAnalysisFactory =
        getBddReachabilityAnalysisFactory(pkt, ignoreAcls);
    Map<IngressLocation, BDD> reachableBDDs =
        bddReachabilityAnalysisFactory
            .bddReachabilityAnalysis(
                params.getSourceIpAssignment(),
                params.getHeaderSpace(),
                params.getForbiddenTransitNodes(),
                params.getRequiredTransitNodes(),
                params.getFinalNodes(),
                params.getActions())
            .getIngressLocationReachableBDDs();

    String flowTag = getFlowTag();
    Set<Flow> flows =
        reachableBDDs
            .entrySet()
            .stream()
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

    DataPlane dp = loadDataPlane();
    if (_settings.debugFlagEnabled("oldtraceroute")) {
      getDataPlanePlugin().processFlows(flows, dp, ignoreAcls);
      return getHistory();
    } else {
      return new TraceWrapperAsAnswerElement(buildFlows(flows, ignoreAcls));
    }
  }

  @Override
  public Set<Flow> bddLoopDetection() {
    BDDPacket pkt = new BDDPacket();
    // TODO add ignoreAcls parameter
    boolean ignoreAcls = false;
    BDDReachabilityAnalysisFactory bddReachabilityAnalysisFactory =
        getBddReachabilityAnalysisFactory(pkt, ignoreAcls);
    BDDReachabilityAnalysis analysis =
        bddReachabilityAnalysisFactory.bddReachabilityAnalysis(
            getAllSourcesInferFromLocationIpSpaceAssignment());
    Map<IngressLocation, BDD> loopBDDs = analysis.detectLoops();

    String flowTag = getFlowTag();
    return loopBDDs
        .entrySet()
        .stream()
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
                              throw new BatfishException("Unknown Location Type: " + loc.getType());
                          }
                          return fb.build();
                        }))
        .flatMap(optional -> optional.map(Stream::of).orElse(Stream.empty()))
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public Set<Flow> bddMultipathConsistency(MultipathConsistencyParameters parameters) {
    BDDPacket pkt = new BDDPacket();
    // TODO add ignoreAcls parameter
    boolean ignoreAcls = false;
    BDDReachabilityAnalysisFactory bddReachabilityAnalysisFactory =
        getBddReachabilityAnalysisFactory(pkt, ignoreAcls);
    IpSpaceAssignment srcIpSpaceAssignment = parameters.getSrcIpSpaceAssignment();
    Set<String> finalNodes = parameters.getFinalNodes();
    Set<FlowDisposition> dropDispositions =
        ImmutableSet.of(
            FlowDisposition.DENIED_IN,
            FlowDisposition.DENIED_OUT,
            FlowDisposition.NO_ROUTE,
            FlowDisposition.NULL_ROUTED);
    Set<String> forbiddenTransitNodes = parameters.getForbiddenTransitNodes();
    Set<String> requiredTransitNodes = parameters.getRequiredTransitNodes();
    Map<IngressLocation, BDD> acceptedBDDs =
        bddReachabilityAnalysisFactory
            .bddReachabilityAnalysis(
                srcIpSpaceAssignment,
                parameters.getHeaderSpace(),
                forbiddenTransitNodes,
                requiredTransitNodes,
                finalNodes,
                ImmutableSet.of(FlowDisposition.ACCEPTED))
            .getIngressLocationReachableBDDs();
    Map<IngressLocation, BDD> droppedBDDs =
        bddReachabilityAnalysisFactory
            .bddReachabilityAnalysis(
                srcIpSpaceAssignment,
                parameters.getHeaderSpace(),
                forbiddenTransitNodes,
                requiredTransitNodes,
                finalNodes,
                dropDispositions)
            .getIngressLocationReachableBDDs();
    Map<IngressLocation, BDD> neighborUnreachableBDDs =
        bddReachabilityAnalysisFactory
            .bddReachabilityAnalysis(
                srcIpSpaceAssignment,
                parameters.getHeaderSpace(),
                forbiddenTransitNodes,
                requiredTransitNodes,
                finalNodes,
                ImmutableSet.of(
                    FlowDisposition.NEIGHBOR_UNREACHABLE,
                    FlowDisposition.DELIVERED_TO_SUBNET,
                    FlowDisposition.EXITS_NETWORK,
                    FlowDisposition.INSUFFICIENT_INFO))
            .getIngressLocationReachableBDDs();

    String flowTag = getFlowTag();
    return Streams.concat(
            computeMultipathInconsistencies(pkt, flowTag, acceptedBDDs, droppedBDDs).stream(),
            computeMultipathInconsistencies(pkt, flowTag, acceptedBDDs, neighborUnreachableBDDs)
                .stream(),
            computeMultipathInconsistencies(pkt, flowTag, droppedBDDs, neighborUnreachableBDDs)
                .stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  @Nonnull
  public IpSpaceAssignment getAllSourcesInferFromLocationIpSpaceAssignment() {
    SpecifierContextImpl specifierContext = new SpecifierContextImpl(this, loadConfigurations());
    Set<Location> locations =
        new UnionLocationSpecifier(
                AllInterfacesLocationSpecifier.INSTANCE,
                AllInterfaceLinksLocationSpecifier.INSTANCE)
            .resolve(specifierContext);
    return InferFromLocationIpSpaceSpecifier.INSTANCE.resolve(locations, specifierContext);
  }

  @Nonnull
  private BDDReachabilityAnalysisFactory getBddReachabilityAnalysisFactory(
      BDDPacket pkt, boolean ignoreAcls) {
    return new BDDReachabilityAnalysisFactory(
        pkt, loadConfigurations(), loadDataPlane().getForwardingAnalysis(), ignoreAcls);
  }

  /**
   * Return a set of flows (at most 1 per source {@link Location}) for which reachability has been
   * reduced by the change from base to delta snapshot.
   */
  @Override
  public DifferentialReachabilityResult bddReducedReachability(
      DifferentialReachabilityParameters parameters) {
    checkArgument(
        !parameters.getFlowDispositions().isEmpty(), "Must specify at least one FlowDisposition");
    BDDPacket pkt = new BDDPacket();

    /*
     * TODO should we have separate parameters for base and delta?
     * E.g. suppose we add a host subnet in the delta network. This would be a source of
     * differential reachability, but we currently won't find it because it won't be in the
     * IpSpaceAssignment.
     */
    pushBaseSnapshot();
    Map<IngressLocation, BDD> baseAcceptBDDs =
        getBddReachabilityAnalysisFactory(pkt, parameters.getIgnoreAcls())
            .bddReachabilityAnalysis(
                parameters.getIpSpaceAssignment(),
                parameters.getHeaderSpace(),
                parameters.getForbiddenTransitNodes(),
                parameters.getRequiredTransitNodes(),
                parameters.getFinalNodes(),
                parameters.getFlowDispositions())
            .getIngressLocationReachableBDDs();
    popSnapshot();

    pushDeltaSnapshot();
    Map<IngressLocation, BDD> deltaAcceptBDDs =
        getBddReachabilityAnalysisFactory(pkt, parameters.getIgnoreAcls())
            .bddReachabilityAnalysis(
                parameters.getIpSpaceAssignment(),
                parameters.getHeaderSpace(),
                parameters.getForbiddenTransitNodes(),
                parameters.getRequiredTransitNodes(),
                parameters.getFinalNodes(),
                parameters.getFlowDispositions())
            .getIngressLocationReachableBDDs();
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

  private static Set<Flow> getDifferentialFlows(
      BDDPacket pkt,
      Set<IngressLocation> commonSources,
      Map<IngressLocation, BDD> includeBDDs,
      Map<IngressLocation, BDD> excludeBDDs,
      String flowTag) {
    return commonSources
        .stream()
        .flatMap(
            source -> {
              BDD difference = includeBDDs.get(source).and(excludeBDDs.get(source).not());

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

  @Nonnull
  private Synthesizer synthesizeDataPlane(ResolvedReachabilityParameters parameters) {
    Map<String, Configuration> configs = parameters.getConfigurations();
    DataPlane dataPlane = parameters.getDataPlane();
    return synthesizeDataPlane(
        configs,
        dataPlane,
        parameters.getHeaderSpace(),
        parameters.getForbiddenTransitNodes(),
        parameters.getRequiredTransitNodes(),
        parameters.getSourceIpAssignment(),
        parameters.getSpecialize());
  }

  @Nonnull
  public Synthesizer synthesizeDataPlane(
      Map<String, Configuration> configurations,
      DataPlane dataPlane,
      AclLineMatchExpr headerSpace,
      Set<String> nonTransitNodes,
      Set<String> transitNodes,
      IpSpaceAssignment ipSpaceAssignment,
      boolean specialize) {
    _logger.info("\n*** GENERATING Z3 LOGIC ***\n");
    _logger.resetTimer();

    _logger.info("Synthesizing Z3 logic...");

    Synthesizer s =
        new Synthesizer(
            computeSynthesizerInput(
                configurations,
                dataPlane,
                headerSpace,
                ipSpaceAssignment,
                transitNodes,
                nonTransitNodes,
                _settings.getSimplify(),
                specialize));

    List<String> warnings = s.getWarnings();
    int numWarnings = warnings.size();
    if (numWarnings == 0) {
      _logger.info("OK\n");
    } else {
      for (String warning : warnings) {
        _logger.warn(warning);
      }
    }
    _logger.printElapsedTime();
    return s;
  }

  public static SynthesizerInputImpl computeSynthesizerInput(
      Map<String, Configuration> configurations,
      DataPlane dataPlane,
      AclLineMatchExpr headerSpace,
      IpSpaceAssignment ipSpaceAssignment,
      Set<String> transitNodes,
      Set<String> nonTransitNodes,
      boolean simplify,
      boolean specialize) {
    Topology topology = new Topology(dataPlane.getTopologyEdges());

    // convert Locations to IngressLocations
    Map<IngressLocation, IpSpace> ipSpacePerLocation = new HashMap<>();
    LocationToIngressLocation toIngressLocation = new LocationToIngressLocation(configurations);
    ipSpaceAssignment
        .getEntries()
        .forEach(
            entry ->
                entry
                    .getLocations()
                    .forEach(
                        location ->
                            ipSpacePerLocation.merge(
                                location.accept(toIngressLocation),
                                entry.getIpSpace(),
                                ((ipSpace1, ipSpace2) -> AclIpSpace.union(ipSpace1, ipSpace2)))));

    return SynthesizerInputImpl.builder()
        .setConfigurations(configurations)
        .setForwardingAnalysis(dataPlane.getForwardingAnalysis())
        .setHeaderSpace(headerSpace)
        .setSrcIpConstraints(ipSpacePerLocation)
        .setNonTransitNodes(nonTransitNodes)
        .setSimplify(simplify)
        .setSpecialize(specialize)
        .setTopology(topology)
        .setTransitNodes(transitNodes)
        .build();
  }

  private Answer validateSnapshot() {
    Answer answer = new Answer();
    ValidateSnapshotAnswerElement ae = loadValidateSnapshotAnswerElement();
    answer.addAnswerElement(ae);
    Topology envTopology = computeEnvironmentTopology(loadConfigurations());
    serializeAsJson(
        _testrigSettings.getSerializeTopologyPath(), envTopology, "environment topology");
    return answer;
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

  private void writeJsonTopology() {
    try {
      Map<String, Configuration> configs = loadConfigurations();
      SortedSet<Edge> textEdges = CommonUtil.synthesizeTopology(configs).getEdges();
      JSONArray jEdges = new JSONArray();
      for (Edge textEdge : textEdges) {
        Configuration node1 = configs.get(textEdge.getNode1());
        Configuration node2 = configs.get(textEdge.getNode2());
        Interface interface1 = node1.getAllInterfaces().get(textEdge.getInt1());
        Interface interface2 = node2.getAllInterfaces().get(textEdge.getInt2());
        JSONObject jEdge = new JSONObject();
        jEdge.put("interface1", interface1.toJSONObject());
        jEdge.put("interface2", interface2.toJSONObject());
        jEdges.put(jEdge);
      }
      JSONObject master = new JSONObject();
      JSONObject topology = new JSONObject();
      topology.put("edges", jEdges);
      master.put("topology", topology);
      String text = master.toString(3);
      _logger.output(text);
    } catch (JSONException e) {
      throw new BatfishException("Failed to synthesize JSON topology", e);
    }
  }

  @Override
  public @Nullable Layer1Topology getLayer1Topology() {
    return _storage.loadLayer1Topology(_settings.getContainer(), _testrigSettings.getName());
  }

  @Override
  public @Nullable Layer2Topology getLayer2Topology() {
    Layer1Topology layer1Topology = getLayer1Topology();
    if (layer1Topology == null) {
      return null;
    }
    return TopologyUtil.computeLayer2Topology(layer1Topology, loadConfigurations());
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
    BiFunction<Question, IBatfish, Answerer> creator = _answererCreators.get(question.getName());
    return creator != null ? creator.apply(question, this) : null;
  }
}
