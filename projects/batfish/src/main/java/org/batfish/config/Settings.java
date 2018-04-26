package org.batfish.config;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.batfish.common.BaseSettings;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.CoordConsts;
import org.batfish.common.PedanticBatfishException;
import org.batfish.common.RedFlagBatfishException;
import org.batfish.common.UnimplementedBatfishException;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Ip;
import org.batfish.dataplane.bdp.BdpSettings;
import org.batfish.grammar.GrammarSettings;
import org.batfish.main.Driver.RunMode;

public final class Settings extends BaseSettings implements BdpSettings, GrammarSettings {

  public static final class EnvironmentSettings {

    private Path _compressedDataPlaneAnswerPath;

    private Path _compressedDataPlanePath;

    private Path _dataPlaneAnswerPath;

    private Path _dataPlanePath;

    private Path _deltaCompiledConfigurationsDir;

    private Path _deltaConfigurationsDir;

    private Path _deltaVendorConfigurationsDir;

    private Path _edgeBlacklistPath;

    private Path _environmentBasePath;

    private Path _environmentBgpTablesPath;

    private Path _environmentRoutingTablesPath;

    private Path _envPath;

    private Path _externalBgpAnnouncementsPath;

    private Path _interfaceBlacklistPath;

    private String _name;

    private Path _nodeBlacklistPath;

    private Path _parseEnvironmentBgpTablesAnswerPath;

    private Path _parseEnvironmentRoutingTablesAnswerPath;

    private Path _precomputedRoutesPath;

    private Path _serializedTopologyPath;

    private Path _serializeEnvironmentBgpTablesPath;

    private Path _serializeEnvironmentRoutingTablesPath;

    private Path _validateEnvironmentAnswerPath;

    public Path getCompressedDataPlaneAnswerPath() {
      return _compressedDataPlaneAnswerPath;
    }

    public Path getCompressedDataPlanePath() {
      return _compressedDataPlanePath;
    }

    public Path getDataPlaneAnswerPath() {
      return _dataPlaneAnswerPath;
    }

    public Path getDataPlanePath() {
      return _dataPlanePath;
    }

    public Path getDeltaCompiledConfigurationsDir() {
      return _deltaCompiledConfigurationsDir;
    }

    public Path getDeltaConfigurationsDir() {
      return _deltaConfigurationsDir;
    }

    public Path getDeltaVendorConfigurationsDir() {
      return _deltaVendorConfigurationsDir;
    }

    public Path getEdgeBlacklistPath() {
      return _edgeBlacklistPath;
    }

    public Path getEnvironmentBasePath() {
      return _environmentBasePath;
    }

    public Path getEnvironmentBgpTablesPath() {
      return _environmentBgpTablesPath;
    }

    public Path getEnvironmentRoutingTablesPath() {
      return _environmentRoutingTablesPath;
    }

    public Path getEnvPath() {
      return _envPath;
    }

    public Path getExternalBgpAnnouncementsPath() {
      return _externalBgpAnnouncementsPath;
    }

    public Path getInterfaceBlacklistPath() {
      return _interfaceBlacklistPath;
    }

    public String getName() {
      return _name;
    }

    public Path getNodeBlacklistPath() {
      return _nodeBlacklistPath;
    }

    public Path getParseEnvironmentBgpTablesAnswerPath() {
      return _parseEnvironmentBgpTablesAnswerPath;
    }

    public Path getParseEnvironmentRoutingTablesAnswerPath() {
      return _parseEnvironmentRoutingTablesAnswerPath;
    }

    public Path getPrecomputedRoutesPath() {
      return _precomputedRoutesPath;
    }

    public Path getSerializedTopologyPath() {
      return _serializedTopologyPath;
    }

    public Path getSerializeEnvironmentBgpTablesPath() {
      return _serializeEnvironmentBgpTablesPath;
    }

    public Path getSerializeEnvironmentRoutingTablesPath() {
      return _serializeEnvironmentRoutingTablesPath;
    }

    public Path getValidateEnvironmentAnswerPath() {
      return _validateEnvironmentAnswerPath;
    }

    public void setCompressedDataPlaneAnswerPath(Path compressedDataPlaneAnswerPath) {
      _compressedDataPlaneAnswerPath = compressedDataPlaneAnswerPath;
    }

    public void setCompressedDataPlanePath(Path compressedDataPlanePath) {
      _compressedDataPlanePath = compressedDataPlanePath;
    }

    public void setDataPlaneAnswerPath(Path dataPlaneAnswerPath) {
      _dataPlaneAnswerPath = dataPlaneAnswerPath;
    }

    public void setDataPlanePath(Path path) {
      _dataPlanePath = path;
    }

    public void setDeltaCompiledConfigurationsDir(Path deltaCompiledConfigurationsDir) {
      _deltaCompiledConfigurationsDir = deltaCompiledConfigurationsDir;
    }

    public void setDeltaConfigurationsDir(Path deltaConfigurationsDir) {
      _deltaConfigurationsDir = deltaConfigurationsDir;
    }

    public void setDeltaVendorConfigurationsDir(Path deltaVendorConfigurationsDir) {
      _deltaVendorConfigurationsDir = deltaVendorConfigurationsDir;
    }

    public void setEdgeBlacklistPath(Path edgeBlacklistPath) {
      _edgeBlacklistPath = edgeBlacklistPath;
    }

    public void setEnvironmentBasePath(Path environmentBasePath) {
      _environmentBasePath = environmentBasePath;
    }

    public void setEnvironmentBgpTablesPath(Path environmentBgpTablesPath) {
      _environmentBgpTablesPath = environmentBgpTablesPath;
    }

    public void setEnvironmentRoutingTablesPath(Path environmentRoutingTablesPath) {
      _environmentRoutingTablesPath = environmentRoutingTablesPath;
    }

    public void setEnvPath(Path envPath) {
      _envPath = envPath;
    }

    public void setExternalBgpAnnouncementsPath(Path externalBgpAnnouncementsPath) {
      _externalBgpAnnouncementsPath = externalBgpAnnouncementsPath;
    }

    public void setInterfaceBlacklistPath(Path interfaceBlacklistPath) {
      _interfaceBlacklistPath = interfaceBlacklistPath;
    }

    public void setName(String name) {
      _name = name;
    }

    public void setNodeBlacklistPath(Path nodeBlacklistPath) {
      _nodeBlacklistPath = nodeBlacklistPath;
    }

    public void setParseEnvironmentBgpTablesAnswerPath(Path parseEnvironmentBgpTablesAnswerPath) {
      _parseEnvironmentBgpTablesAnswerPath = parseEnvironmentBgpTablesAnswerPath;
    }

    public void setParseEnvironmentRoutingTablesAnswerPath(
        Path parseEnvironmentRoutingTablesAnswerPath) {
      _parseEnvironmentRoutingTablesAnswerPath = parseEnvironmentRoutingTablesAnswerPath;
    }

    public void setPrecomputedRoutesPath(Path writeRoutesPath) {
      _precomputedRoutesPath = writeRoutesPath;
    }

    public void setSerializedTopologyPath(Path serializedTopologyPath) {
      _serializedTopologyPath = serializedTopologyPath;
    }

    public void setSerializeEnvironmentBgpTablesPath(Path serializeEnvironmentBgpTablesPath) {
      _serializeEnvironmentBgpTablesPath = serializeEnvironmentBgpTablesPath;
    }

    public void setSerializeEnvironmentRoutingTablesPath(
        Path serializeEnvironmentRoutingTablesPath) {
      _serializeEnvironmentRoutingTablesPath = serializeEnvironmentRoutingTablesPath;
    }

    public void setValidateEnvironmentAnswerPath(Path validateEnvironmentAnswerPath) {
      _validateEnvironmentAnswerPath = validateEnvironmentAnswerPath;
    }
  }

  public static final class TestrigSettings {

    private Path _basePath;

    private EnvironmentSettings _environmentSettings;

    private Path _inferredNodeRolesPath;

    private String _name;

    private Path _nodeRolesPath;

    private Path _parseAnswerPath;

    private Path _pojoTopologyPath;

    private Path _protocolDependencyGraphPath;

    private Path _protocolDependencyGraphZipPath;

    private Path _serializeVendorPath;

    private Path _testRigPath;

    private Path _topologyPath;

    public TestrigSettings() {
      _environmentSettings = new EnvironmentSettings();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (!(obj instanceof TestrigSettings)) {
        return false;
      }
      TestrigSettings other = (TestrigSettings) obj;
      return _name.equals(other._name)
          && _environmentSettings._name.equals(other._environmentSettings._name);
    }

    public Path getBasePath() {
      return _basePath;
    }

    public EnvironmentSettings getEnvironmentSettings() {
      return _environmentSettings;
    }

    public Path getInferredNodeRolesPath() {
      return _inferredNodeRolesPath;
    }

    public String getName() {
      return _name;
    }

    public Path getNodeRolesPath() {
      return _nodeRolesPath;
    }

    public Path getParseAnswerPath() {
      return _parseAnswerPath;
    }

    public Path getPojoTopologyPath() {
      return _pojoTopologyPath;
    }

    public Path getProtocolDependencyGraphPath() {
      return _protocolDependencyGraphPath;
    }

    public Path getProtocolDependencyGraphZipPath() {
      return _protocolDependencyGraphZipPath;
    }

    public Path getSerializeVendorPath() {
      return _serializeVendorPath;
    }

    public Path getTestRigPath() {
      return _testRigPath;
    }

    public Path getTopologyPath() {
      return _topologyPath;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((_name == null) ? 0 : _name.hashCode());
      result =
          prime * result
              + ((_environmentSettings._name == null) ? 0 : _environmentSettings._name.hashCode());
      return result;
    }

    public void setBasePath(Path basePath) {
      _basePath = basePath;
    }

    public void setEnvironmentSettings(EnvironmentSettings environmentSettings) {
      _environmentSettings = environmentSettings;
    }

    public void setInferredNodeRolesPath(Path inferredNodeRolesPath) {
      _inferredNodeRolesPath = inferredNodeRolesPath;
    }

    public void setName(String name) {
      _name = name;
    }

    public void setNodeRolesPath(Path nodeRolesPath) {
      _nodeRolesPath = nodeRolesPath;
    }

    public void setParseAnswerPath(Path parseAnswerPath) {
      _parseAnswerPath = parseAnswerPath;
    }

    public void setPojoTopologyPath(Path path) {
      _pojoTopologyPath = path;
    }

    public void setProtocolDependencyGraphPath(Path protocolDependencyGraphPath) {
      _protocolDependencyGraphPath = protocolDependencyGraphPath;
    }

    public void setProtocolDependencyGraphZipPath(Path protocolDependencyGraphZipPath) {
      _protocolDependencyGraphZipPath = protocolDependencyGraphZipPath;
    }

    public void setSerializeVendorPath(Path path) {
      _serializeVendorPath = path;
    }

    public void setTestRigPath(Path path) {
      _testRigPath = path;
    }

    public void setTopologyPath(Path path) {
      _topologyPath = path;
    }
  }

  public static final String ARG_CHECK_BGP_REACHABILITY = "checkbgpsessionreachability";

  public static final String ARG_COORDINATOR_HOST = "coordinatorhost";

  private static final String ARG_COORDINATOR_POOL_PORT = "coordinatorpoolport";

  public static final String ARG_COORDINATOR_REGISTER = "register";

  private static final String ARG_DATAPLANE_ENGINE_NAME = "dataplaneengine";

  private static final String ARG_DEBUG_FLAGS = "debugflags";

  private static final String ARG_DISABLE_Z3_SIMPLIFICATION = "nosimplify";

  private static final String ARG_EXIT_ON_FIRST_ERROR = "ee";

  private static final String ARG_FLATTEN = "flatten";

  private static final String ARG_FLATTEN_DESTINATION = "flattendst";

  private static final String ARG_FLATTEN_ON_THE_FLY = "flattenonthefly";

  private static final String ARG_GENERATE_STUBS = "gs";

  private static final String ARG_GENERATE_STUBS_INPUT_ROLE = "gsinputrole";

  private static final String ARG_GENERATE_STUBS_INTERFACE_DESCRIPTION_REGEX = "gsidregex";

  private static final String ARG_GENERATE_STUBS_REMOTE_AS = "gsremoteas";

  private static final String ARG_HELP = "help";

  private static final String ARG_HISTOGRAM = "histogram";

  private static final String ARG_IGNORE_UNKNOWN = "ignoreunknown";

  private static final String ARG_IGNORE_UNSUPPORTED = "ignoreunsupported";

  private static final String ARG_JOBS = "jobs";

  private static final String ARG_LOG_TEE = "logtee";

  private static final String ARG_MAX_PARSER_CONTEXT_LINES = "maxparsercontextlines";

  private static final String ARG_MAX_PARSER_CONTEXT_TOKENS = "maxparsercontexttokens";

  private static final String ARG_MAX_PARSE_TREE_PRINT_LENGTH = "maxparsetreeprintlength";

  private static final String ARG_MAX_RUNTIME_MS = "maxruntime";

  private static final String ARG_NO_SHUFFLE = "noshuffle";

  public static final String ARG_PARENT_PID = "parentpid";

  private static final String ARG_PRINT_PARSE_TREES = "ppt";

  private static final String ARG_PRINT_SYMMETRIC_EDGES = "printsymmetricedges";

  public static final String ARG_RUN_MODE = "runmode";

  private static final String ARG_SEQUENTIAL = "sequential";

  private static final String ARG_SERIALIZE_TO_TEXT = "stext";

  private static final String ARG_SERVICE_BIND_HOST = "servicebindhost";

  public static final String ARG_SERVICE_HOST = "servicehost";

  public static final String ARG_SERVICE_NAME = "servicename";

  private static final String ARG_SERVICE_PORT = "serviceport";

  private static final String ARG_TRACING_AGENT_HOST = "tracingagenthost";

  private static final String ARG_TRACING_AGENT_PORT = "tracingagentport";

  public static final String ARG_TRACING_ENABLE = "tracingenable";

  private static final String ARG_THROW_ON_LEXER_ERROR = "throwlexer";

  private static final String ARG_THROW_ON_PARSER_ERROR = "throwparser";

  private static final String ARG_TIMESTAMP = "timestamp";

  private static final String ARG_Z3_TIMEOUT = "z3timeout";

  private static final String ARGNAME_AS = "as";

  private static final String ARGNAME_HOSTNAME = "hostname";

  private static final String ARGNAME_JAVA_REGEX = "java-regex";

  private static final String ARGNAME_LOG_LEVEL = "level-name";

  private static final String ARGNAME_NAME = "name";

  private static final String ARGNAME_NUMBER = "number";

  private static final String ARGNAME_PATH = "path";

  private static final String ARGNAME_PORT = "port";

  private static final String ARGNAME_ROLE = "role";

  private static final String ARGNAME_STRINGS = "string..";

  private static final String EXECUTABLE_NAME = "batfish";

  private static final String CAN_EXECUTE = "canexecute";

  private static final String DIFFERENTIAL_QUESTION = "diffquestion";

  public static final String TASK_ID = "taskid";

  private static final String QUESTION_PATH = "questionpath";

  private TestrigSettings _activeTestrigSettings;

  private TestrigSettings _baseTestrigSettings;

  private final TestrigSettings _deltaTestrigSettings;

  private BatfishLogger _logger;

  public Settings() {
    this(new String[] {});
  }

  public Settings(String[] args) {
    super(
        CommonUtil.getConfig(
            BfConsts.PROP_BATFISH_PROPERTIES_PATH,
            BfConsts.ABSPATH_CONFIG_FILE_NAME_BATFISH,
            ConfigurationLocator.class));
    _baseTestrigSettings = new TestrigSettings();
    _deltaTestrigSettings = new TestrigSettings();
    initConfigDefaults();
    initOptions();
    parseCommandLine(args);
  }

  /**
   * Create a copy of some existing settings.
   *
   * @param other the {@link Settings to copy}
   */
  public Settings(Settings other) {
    super(other._config);
    _baseTestrigSettings = new TestrigSettings();
    _deltaTestrigSettings = new TestrigSettings();
    _activeTestrigSettings = new TestrigSettings();
    _logger = other._logger;
    initOptions();
  }

  /**
   * Remove certain setting values
   *
   * @param keys a list of keys to clear
   */
  public void clearValues(String... keys) {
    for (String s : keys) {
      _config.clearProperty(s);
    }
  }

  public boolean canExecute() {
    return _config.getBoolean(CAN_EXECUTE);
  }

  public boolean debugFlagEnabled(String flag) {
    List<String> debugFlags = getStringListOptionValue(ARG_DEBUG_FLAGS);
    return debugFlags != null && debugFlags.contains(flag);
  }

  public boolean flattenOnTheFly() {
    return _config.getBoolean(ARG_FLATTEN_ON_THE_FLY);
  }

  public TestrigSettings getActiveTestrigSettings() {
    return _activeTestrigSettings;
  }

  public String getAnalysisName() {
    return _config.getString(BfConsts.ARG_ANALYSIS_NAME);
  }

  public boolean getAnalyze() {
    return _config.getBoolean(BfConsts.COMMAND_ANALYZE);
  }

  public boolean getAnswer() {
    return _config.getBoolean(BfConsts.COMMAND_ANSWER);
  }

  @Nullable
  public Path getAnswerJsonPath() {
    return nullablePath(_config.getString(BfConsts.ARG_ANSWER_JSON_PATH));
  }

  public int getAvailableThreads() {
    return Math.min(Runtime.getRuntime().availableProcessors(), getJobs());
  }

  public TestrigSettings getBaseTestrigSettings() {
    return _baseTestrigSettings;
  }

  @Override
  public boolean getBdpDetail() {
    return _config.getBoolean(BfConsts.ARG_BDP_DETAIL);
  }

  @Override
  public int getBdpMaxOscillationRecoveryAttempts() {
    return _config.getInt(BfConsts.ARG_BDP_MAX_OSCILLATION_RECOVERY_ATTEMPTS);
  }

  @Override
  public int getBdpMaxRecordedIterations() {
    return _config.getInt(BfConsts.ARG_BDP_MAX_RECORDED_ITERATIONS);
  }

  @Override
  public boolean getBdpPrintAllIterations() {
    return _config.getBoolean(BfConsts.ARG_BDP_PRINT_ALL_ITERATIONS);
  }

  @Override
  public boolean getBdpPrintOscillatingIterations() {
    return _config.getBoolean(BfConsts.ARG_BDP_PRINT_OSCILLATING_ITERATIONS);
  }

  @Override
  public boolean getBdpRecordAllIterations() {
    return _config.getBoolean(BfConsts.ARG_BDP_RECORD_ALL_ITERATIONS);
  }

  public boolean getCompileEnvironment() {
    return _config.getBoolean(BfConsts.COMMAND_COMPILE_DIFF_ENVIRONMENT);
  }

  public Path getContainerDir() {
    return Paths.get(_config.getString(BfConsts.ARG_CONTAINER_DIR));
  }

  public String getCoordinatorHost() {
    return _config.getString(ARG_COORDINATOR_HOST);
  }

  public int getCoordinatorPoolPort() {
    return _config.getInt(ARG_COORDINATOR_POOL_PORT);
  }

  public boolean getCoordinatorRegister() {
    return _config.getBoolean(ARG_COORDINATOR_REGISTER);
  }

  public boolean getDataPlane() {
    return _config.getBoolean(BfConsts.COMMAND_DUMP_DP);
  }

  public List<String> getDebugFlags() {
    return getStringListOptionValue(ARG_DEBUG_FLAGS);
  }

  public String getDeltaEnvironmentName() {
    return _config.getString(BfConsts.ARG_DELTA_ENVIRONMENT_NAME);
  }

  public String getDeltaTestrig() {
    return _config.getString(BfConsts.ARG_DELTA_TESTRIG);
  }

  public TestrigSettings getDeltaTestrigSettings() {
    return _deltaTestrigSettings;
  }

  public boolean getDiffActive() {
    return _config.getBoolean(BfConsts.ARG_DIFF_ACTIVE);
  }

  public boolean getDifferential() {
    return _config.getBoolean(BfConsts.ARG_DIFFERENTIAL);
  }

  public boolean getDiffQuestion() {
    return _config.getBoolean(DIFFERENTIAL_QUESTION);
  }

  @Override
  public boolean getDisableUnrecognized() {
    return _config.getBoolean(BfConsts.ARG_DISABLE_UNRECOGNIZED);
  }

  public boolean getEnableCiscoNxParser() {
    return _config.getBoolean(BfConsts.ARG_ENABLE_CISCO_NX_PARSER);
  }

  public String getEnvironmentName() {
    return _config.getString(BfConsts.ARG_ENVIRONMENT_NAME);
  }

  public boolean getExitOnFirstError() {
    return _config.getBoolean(ARG_EXIT_ON_FIRST_ERROR);
  }

  public boolean getFlatten() {
    return _config.getBoolean(ARG_FLATTEN);
  }

  public Path getFlattenDestination() {
    return Paths.get(_config.getString(ARG_FLATTEN_DESTINATION));
  }

  public boolean getGenerateStubs() {
    return _config.getBoolean(ARG_GENERATE_STUBS);
  }

  public String getGenerateStubsInputRole() {
    return _config.getString(ARG_GENERATE_STUBS_INPUT_ROLE);
  }

  public String getGenerateStubsInterfaceDescriptionRegex() {
    return _config.getString(ARG_GENERATE_STUBS_INTERFACE_DESCRIPTION_REGEX);
  }

  public int getGenerateStubsRemoteAs() {
    return _config.getInt(ARG_GENERATE_STUBS_REMOTE_AS);
  }

  public boolean getHaltOnConvertError() {
    return _config.getBoolean(BfConsts.ARG_HALT_ON_CONVERT_ERROR);
  }

  public boolean getHaltOnParseError() {
    return _config.getBoolean(BfConsts.ARG_HALT_ON_PARSE_ERROR);
  }

  public boolean getHistogram() {
    return _config.getBoolean(ARG_HISTOGRAM);
  }

  public boolean getInitInfo() {
    return _config.getBoolean(BfConsts.COMMAND_INIT_INFO);
  }

  public int getJobs() {
    return _config.getInt(ARG_JOBS);
  }

  public String getLogFile() {
    return _config.getString(BfConsts.ARG_LOG_FILE);
  }

  public BatfishLogger getLogger() {
    return _logger;
  }

  public String getLogLevel() {
    return _config.getString(BfConsts.ARG_LOG_LEVEL);
  }

  public boolean getLogTee() {
    return _config.getBoolean(ARG_LOG_TEE);
  }

  public int getParentPid() {
    return _config.getInt(ARG_PARENT_PID);
  }

  @Override
  public int getMaxParserContextLines() {
    return _config.getInt(ARG_MAX_PARSER_CONTEXT_LINES);
  }

  @Override
  public int getMaxParserContextTokens() {
    return _config.getInt(ARG_MAX_PARSER_CONTEXT_TOKENS);
  }

  @Override
  public int getMaxParseTreePrintLength() {
    return _config.getInt(ARG_MAX_PARSE_TREE_PRINT_LENGTH);
  }

  public int getMaxRuntimeMs() {
    return _config.getInt(ARG_MAX_RUNTIME_MS);
  }

  public String getOutputEnvironmentName() {
    return _config.getString(BfConsts.ARG_OUTPUT_ENV);
  }

  public boolean getPedanticAsError() {
    return _config.getBoolean(BfConsts.ARG_PEDANTIC_AS_ERROR);
  }

  public boolean getPedanticRecord() {
    return !_config.getBoolean(BfConsts.ARG_PEDANTIC_SUPPRESS);
  }

  public boolean getPrettyPrintAnswer() {
    return _config.getBoolean(BfConsts.ARG_PRETTY_PRINT_ANSWER);
  }

  @Override
  public boolean getPrintParseTree() {
    return _config.getBoolean(ARG_PRINT_PARSE_TREES);
  }

  public boolean getPrintSymmetricEdgePairs() {
    return _config.getBoolean(ARG_PRINT_SYMMETRIC_EDGES);
  }

  public String getQuestionName() {
    return _config.getString(BfConsts.ARG_QUESTION_NAME);
  }

  @Nullable
  public Path getQuestionPath() {
    return nullablePath(_config.getString(QUESTION_PATH));
  }

  public boolean getRedFlagAsError() {
    return _config.getBoolean(BfConsts.ARG_RED_FLAG_AS_ERROR);
  }

  public boolean getRedFlagRecord() {
    return !_config.getBoolean(BfConsts.ARG_RED_FLAG_SUPPRESS);
  }

  public boolean getReport() {
    return _config.getBoolean(BfConsts.COMMAND_REPORT);
  }

  public RunMode getRunMode() {
    return RunMode.valueOf(_config.getString(ARG_RUN_MODE).toUpperCase());
  }

  public boolean getSequential() {
    return _config.getBoolean(ARG_SEQUENTIAL);
  }

  public boolean getSerializeIndependent() {
    return _config.getBoolean(BfConsts.COMMAND_PARSE_VENDOR_INDEPENDENT);
  }

  public boolean getSerializeToText() {
    return _config.getBoolean(ARG_SERIALIZE_TO_TEXT);
  }

  public boolean getSerializeVendor() {
    return _config.getBoolean(BfConsts.COMMAND_PARSE_VENDOR_SPECIFIC);
  }

  public String getServiceBindHost() {
    return _config.getString(ARG_SERVICE_BIND_HOST);
  }

  public String getServiceHost() {
    return _config.getString(ARG_SERVICE_HOST);
  }

  public String getServiceName() {
    return _config.getString(ARG_SERVICE_NAME);
  }

  public int getServicePort() {
    return _config.getInt(ARG_SERVICE_PORT);
  }

  public boolean getShuffleJobs() {
    return !_config.getBoolean(ARG_NO_SHUFFLE);
  }

  public boolean getSimplify() {
    return !_config.getBoolean(ARG_DISABLE_Z3_SIMPLIFICATION);
  }

  public boolean getSslDisable() {
    return _config.getBoolean(BfConsts.ARG_SSL_DISABLE);
  }

  @Nullable
  public Path getSslKeystoreFile() {
    return nullablePath(_config.getString(BfConsts.ARG_SSL_KEYSTORE_FILE));
  }

  public String getSslKeystorePassword() {
    return _config.getString(BfConsts.ARG_SSL_KEYSTORE_PASSWORD);
  }

  public boolean getSslTrustAllCerts() {
    return _config.getBoolean(BfConsts.ARG_SSL_TRUST_ALL_CERTS);
  }

  public Path getSslTruststoreFile() {
    return _config.get(Path.class, BfConsts.ARG_SSL_TRUSTSTORE_FILE);
  }

  public String getSslTruststorePassword() {
    return _config.getString(BfConsts.ARG_SSL_TRUSTSTORE_PASSWORD);
  }

  public boolean getSynthesizeJsonTopology() {
    return _config.getBoolean(BfConsts.ARG_SYNTHESIZE_JSON_TOPOLOGY);
  }

  public String getTaskId() {
    return _config.getString(TASK_ID);
  }

  public String getTaskPlugin() {
    return _config.getString(BfConsts.ARG_TASK_PLUGIN);
  }

  public String getTestrig() {
    return _config.getString(BfConsts.ARG_TESTRIG);
  }

  @Override
  public boolean getThrowOnLexerError() {
    return _config.getBoolean(ARG_THROW_ON_LEXER_ERROR);
  }

  @Override
  public boolean getThrowOnParserError() {
    return _config.getBoolean(ARG_THROW_ON_PARSER_ERROR);
  }

  public boolean getTimestamp() {
    return _config.getBoolean(ARG_TIMESTAMP);
  }

  public String getTracingAgentHost() {
    return _config.getString(ARG_TRACING_AGENT_HOST);
  }

  public Integer getTracingAgentPort() {
    return _config.getInt(ARG_TRACING_AGENT_PORT);
  }

  public boolean getTracingEnable() {
    return _config.getBoolean(ARG_TRACING_ENABLE);
  }

  public boolean getUnimplementedAsError() {
    return _config.getBoolean(BfConsts.ARG_UNIMPLEMENTED_AS_ERROR);
  }

  public boolean getUnimplementedRecord() {
    return !_config.getBoolean(BfConsts.ARG_UNIMPLEMENTED_SUPPRESS);
  }

  public boolean getUnrecognizedAsRedFlag() {
    return _config.getBoolean(BfConsts.ARG_UNRECOGNIZED_AS_RED_FLAG);
  }

  public boolean getValidateEnvironment() {
    return _config.getBoolean(BfConsts.COMMAND_VALIDATE_ENVIRONMENT);
  }

  public boolean getVerboseParse() {
    return _config.getBoolean(BfConsts.ARG_VERBOSE_PARSE);
  }

  public List<String> ignoreFilesWithStrings() {
    List<String> l = _config.getList(String.class, BfConsts.ARG_IGNORE_FILES_WITH_STRINGS);
    return l == null ? ImmutableList.of() : l;
  }

  public boolean ignoreUnknown() {
    return _config.getBoolean(ARG_IGNORE_UNKNOWN);
  }

  public boolean ignoreUnsupported() {
    return _config.getBoolean(ARG_IGNORE_UNSUPPORTED);
  }

  public int getZ3timeout() {
    return _config.getInt(ARG_Z3_TIMEOUT);
  }

  public String getDataPlaneEngineName() {
    return _config.getString(ARG_DATAPLANE_ENGINE_NAME);
  }

  private void initConfigDefaults() {
    setDefaultProperty(BfConsts.ARG_ANALYSIS_NAME, null);
    setDefaultProperty(BfConsts.ARG_ANSWER_JSON_PATH, null);
    setDefaultProperty(BfConsts.ARG_BDP_DETAIL, false);
    setDefaultProperty(BfConsts.ARG_BDP_MAX_OSCILLATION_RECOVERY_ATTEMPTS, 0);
    setDefaultProperty(BfConsts.ARG_BDP_MAX_RECORDED_ITERATIONS, 5);
    setDefaultProperty(BfConsts.ARG_BDP_PRINT_ALL_ITERATIONS, false);
    setDefaultProperty(BfConsts.ARG_BDP_PRINT_OSCILLATING_ITERATIONS, false);
    setDefaultProperty(BfConsts.ARG_BDP_RECORD_ALL_ITERATIONS, false);
    setDefaultProperty(CAN_EXECUTE, true);
    setDefaultProperty(BfConsts.ARG_CONTAINER_DIR, null);
    setDefaultProperty(ARG_COORDINATOR_REGISTER, false);
    setDefaultProperty(ARG_COORDINATOR_HOST, "localhost");
    setDefaultProperty(ARG_COORDINATOR_POOL_PORT, CoordConsts.SVC_CFG_POOL_PORT);
    setDefaultProperty(BfConsts.ARG_DIFF_ACTIVE, false);
    setDefaultProperty(DIFFERENTIAL_QUESTION, false);
    setDefaultProperty(ARG_DEBUG_FLAGS, ImmutableList.of());
    setDefaultProperty(BfConsts.ARG_DELTA_ENVIRONMENT_NAME, null);
    setDefaultProperty(BfConsts.ARG_DIFFERENTIAL, false);
    setDefaultProperty(BfConsts.ARG_DISABLE_UNRECOGNIZED, false);
    setDefaultProperty(
        BfConsts.ARG_ENABLE_CISCO_NX_PARSER,
        false); // TODO: enable CiscoNxParser by default and remove this flag.
    setDefaultProperty(ARG_DISABLE_Z3_SIMPLIFICATION, false);
    setDefaultProperty(BfConsts.ARG_ENVIRONMENT_NAME, BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME);
    setDefaultProperty(ARG_EXIT_ON_FIRST_ERROR, false);
    setDefaultProperty(ARG_FLATTEN, false);
    setDefaultProperty(ARG_FLATTEN_DESTINATION, null);
    setDefaultProperty(ARG_FLATTEN_ON_THE_FLY, true);
    setDefaultProperty(ARG_GENERATE_STUBS, false);
    setDefaultProperty(ARG_GENERATE_STUBS_INPUT_ROLE, null);
    setDefaultProperty(ARG_GENERATE_STUBS_INTERFACE_DESCRIPTION_REGEX, null);
    setDefaultProperty(ARG_GENERATE_STUBS_REMOTE_AS, null);
    setDefaultProperty(BfConsts.ARG_HALT_ON_CONVERT_ERROR, false);
    setDefaultProperty(BfConsts.ARG_HALT_ON_PARSE_ERROR, false);
    setDefaultProperty(ARG_HELP, false);
    setDefaultProperty(ARG_HISTOGRAM, false);
    setDefaultProperty(BfConsts.ARG_IGNORE_FILES_WITH_STRINGS, ImmutableList.of());
    setDefaultProperty(ARG_IGNORE_UNSUPPORTED, true);
    setDefaultProperty(ARG_IGNORE_UNKNOWN, true);
    setDefaultProperty(ARG_JOBS, Integer.MAX_VALUE);
    setDefaultProperty(BfConsts.ARG_LOG_FILE, null);
    setDefaultProperty(ARG_LOG_TEE, false);
    setDefaultProperty(BfConsts.ARG_LOG_LEVEL, "debug");
    setDefaultProperty(ARG_MAX_PARSER_CONTEXT_LINES, 10);
    setDefaultProperty(ARG_MAX_PARSER_CONTEXT_TOKENS, 10);
    setDefaultProperty(ARG_MAX_PARSE_TREE_PRINT_LENGTH, 0);
    setDefaultProperty(ARG_MAX_RUNTIME_MS, 0);
    setDefaultProperty(ARG_CHECK_BGP_REACHABILITY, true);
    setDefaultProperty(ARG_NO_SHUFFLE, false);
    setDefaultProperty(BfConsts.ARG_OUTPUT_ENV, null);
    setDefaultProperty(BfConsts.ARG_PEDANTIC_AS_ERROR, false);
    setDefaultProperty(BfConsts.ARG_PEDANTIC_SUPPRESS, false);
    setDefaultProperty(BfConsts.ARG_PRETTY_PRINT_ANSWER, false);
    setDefaultProperty(ARG_PARENT_PID, -1);
    setDefaultProperty(ARG_PRINT_PARSE_TREES, false);
    setDefaultProperty(ARG_PRINT_SYMMETRIC_EDGES, false);
    setDefaultProperty(BfConsts.ARG_QUESTION_NAME, null);
    setDefaultProperty(BfConsts.ARG_RED_FLAG_AS_ERROR, false);
    setDefaultProperty(BfConsts.ARG_RED_FLAG_SUPPRESS, false);
    setDefaultProperty(ARG_RUN_MODE, RunMode.WORKER.toString());
    setDefaultProperty(ARG_SEQUENTIAL, false);
    setDefaultProperty(ARG_SERIALIZE_TO_TEXT, false);
    setDefaultProperty(ARG_SERVICE_BIND_HOST, Ip.ZERO.toString());
    setDefaultProperty(ARG_SERVICE_HOST, "localhost");
    setDefaultProperty(ARG_SERVICE_NAME, "worker-service");
    setDefaultProperty(ARG_SERVICE_PORT, BfConsts.SVC_PORT);
    setDefaultProperty(BfConsts.ARG_SSL_DISABLE, CoordConsts.SVC_CFG_POOL_SSL_DISABLE);
    setDefaultProperty(BfConsts.ARG_SSL_KEYSTORE_FILE, null);
    setDefaultProperty(BfConsts.ARG_SSL_KEYSTORE_PASSWORD, null);
    setDefaultProperty(BfConsts.ARG_SSL_TRUST_ALL_CERTS, false);
    setDefaultProperty(BfConsts.ARG_SSL_TRUSTSTORE_FILE, null);
    setDefaultProperty(BfConsts.ARG_SSL_TRUSTSTORE_PASSWORD, null);
    setDefaultProperty(BfConsts.ARG_SYNTHESIZE_JSON_TOPOLOGY, false);
    setDefaultProperty(BfConsts.ARG_TASK_PLUGIN, null);
    setDefaultProperty(ARG_THROW_ON_LEXER_ERROR, true);
    setDefaultProperty(ARG_THROW_ON_PARSER_ERROR, true);
    setDefaultProperty(ARG_TIMESTAMP, false);
    setDefaultProperty(ARG_TRACING_AGENT_HOST, "localhost");
    setDefaultProperty(ARG_TRACING_AGENT_PORT, 5775);
    setDefaultProperty(ARG_TRACING_ENABLE, false);
    setDefaultProperty(BfConsts.ARG_UNRECOGNIZED_AS_RED_FLAG, true);
    setDefaultProperty(BfConsts.ARG_UNIMPLEMENTED_AS_ERROR, false);
    setDefaultProperty(BfConsts.ARG_UNIMPLEMENTED_SUPPRESS, true);
    setDefaultProperty(BfConsts.ARG_VERBOSE_PARSE, false);
    setDefaultProperty(BfConsts.COMMAND_ANALYZE, false);
    setDefaultProperty(BfConsts.COMMAND_ANSWER, false);
    setDefaultProperty(BfConsts.COMMAND_COMPILE_DIFF_ENVIRONMENT, false);
    setDefaultProperty(BfConsts.COMMAND_DUMP_DP, false);
    setDefaultProperty(BfConsts.COMMAND_INIT_INFO, false);
    setDefaultProperty(BfConsts.COMMAND_PARSE_VENDOR_INDEPENDENT, false);
    setDefaultProperty(BfConsts.COMMAND_PARSE_VENDOR_SPECIFIC, false);
    setDefaultProperty(BfConsts.COMMAND_REPORT, false);
    setDefaultProperty(BfConsts.COMMAND_VALIDATE_ENVIRONMENT, false);
    setDefaultProperty(ARG_Z3_TIMEOUT, 0);
    setDefaultProperty(ARG_DATAPLANE_ENGINE_NAME, "bdp");
  }

  private void initOptions() {

    addOption(BfConsts.ARG_ANALYSIS_NAME, "name of analysis", ARGNAME_NAME);

    addOption(
        BfConsts.ARG_ANSWER_JSON_PATH, "save query json output to specified file", ARGNAME_PATH);

    addBooleanOption(
        BfConsts.ARG_BDP_DETAIL,
        "Set to true to print/record detailed protocol-specific information about routes in each"
            + "iteration rather than only protocol-independent information.");

    addOption(
        BfConsts.ARG_BDP_MAX_OSCILLATION_RECOVERY_ATTEMPTS,
        "Max number of recovery attempts when oscillation occurs during data plane computations",
        ARGNAME_NUMBER);

    addOption(
        BfConsts.ARG_BDP_MAX_RECORDED_ITERATIONS,
        "Max number of iterations to record when debugging BDP. To avoid extra fixed-point"
            + "computation when oscillations occur, set this at least as high as the length of the"
            + "cycle.",
        ARGNAME_NUMBER);

    addBooleanOption(
        BfConsts.ARG_BDP_PRINT_ALL_ITERATIONS,
        "Set to true to print all iterations when oscillation occurs. Make sure to either set max"
            + "recorded iterations to minimum necessary value, or simply record all iterations");

    addBooleanOption(
        BfConsts.ARG_BDP_PRINT_OSCILLATING_ITERATIONS,
        "Set to true to print only oscillating iterations when oscillation occurs. Make sure to"
            + "set max recorded iterations to minimum necessary value.");

    addBooleanOption(
        BfConsts.ARG_BDP_RECORD_ALL_ITERATIONS,
        "Set to true to record all iterations, including during oscillation. Ignores max recorded "
            + "iterations value.");

    addBooleanOption(
        ARG_CHECK_BGP_REACHABILITY,
        "whether to check BGP session reachability during data plane computation");

    addOption(BfConsts.ARG_CONTAINER_DIR, "path to container directory", ARGNAME_PATH);

    addOption(
        ARG_COORDINATOR_HOST,
        "hostname of coordinator for registration when running as service",
        ARGNAME_HOSTNAME);

    addOption(ARG_COORDINATOR_POOL_PORT, "coordinator pool manager listening port", ARGNAME_PORT);

    addBooleanOption(ARG_COORDINATOR_REGISTER, "register service with coordinator on startup");

    addListOption(ARG_DEBUG_FLAGS, "a list of flags to enable debugging code", "debug flags");

    addOption(BfConsts.ARG_DELTA_ENVIRONMENT_NAME, "name of delta environment to use", "name");

    addOption(BfConsts.ARG_DELTA_TESTRIG, "name of delta testrig", ARGNAME_NAME);

    addBooleanOption(
        BfConsts.ARG_DIFF_ACTIVE,
        "make differential environment the active one for questions about a single environment");

    addBooleanOption(
        BfConsts.ARG_DIFFERENTIAL,
        "force treatment of question as differential (to be used when not answering question)");

    addBooleanOption(
        BfConsts.ARG_DISABLE_UNRECOGNIZED, "disable parser recognition of unrecognized stanzas");

    addBooleanOption(ARG_DISABLE_Z3_SIMPLIFICATION, "disable z3 simplification");

    addOption(BfConsts.ARG_ENVIRONMENT_NAME, "name of environment to use", "name");

    addBooleanOption(
        ARG_EXIT_ON_FIRST_ERROR,
        "exit on first parse error (otherwise will exit on last parse error)");

    addBooleanOption(ARG_FLATTEN, "flatten hierarchical juniper configuration files");

    addOption(
        ARG_FLATTEN_DESTINATION,
        "output path to test rig in which flat juniper (and all other) configurations will be "
            + "placed",
        ARGNAME_PATH);

    addBooleanOption(
        ARG_FLATTEN_ON_THE_FLY,
        "flatten hierarchical juniper configuration files on-the-fly (line number references will "
            + "be spurious)");

    addBooleanOption(
        BfConsts.COMMAND_INIT_INFO, "include parse/convert initialization info in answer");

    addBooleanOption(ARG_GENERATE_STUBS, "generate stubs");

    addOption(
        ARG_GENERATE_STUBS_INPUT_ROLE, "input role for which to generate stubs", ARGNAME_ROLE);

    addOption(
        ARG_GENERATE_STUBS_INTERFACE_DESCRIPTION_REGEX,
        "java regex to extract hostname of generated stub from description of adjacent interface",
        ARGNAME_JAVA_REGEX);

    addOption(
        ARG_GENERATE_STUBS_REMOTE_AS,
        "autonomous system number of stubs to be generated",
        ARGNAME_AS);

    addBooleanOption(
        BfConsts.ARG_HALT_ON_CONVERT_ERROR,
        "Halt on conversion error instead of proceeding with successfully converted configs");

    addBooleanOption(
        BfConsts.ARG_HALT_ON_PARSE_ERROR,
        "Halt on parse error instead of proceeding with successfully parsed configs");

    addBooleanOption(ARG_HELP, "print this message");

    addOption(
        BfConsts.ARG_IGNORE_FILES_WITH_STRINGS,
        "ignore configuration files containing these strings",
        ARGNAME_STRINGS);

    addBooleanOption(
        ARG_IGNORE_UNKNOWN, "ignore configuration files with unknown format instead of crashing");

    addBooleanOption(
        ARG_IGNORE_UNSUPPORTED,
        "ignore configuration files with unsupported format instead of crashing");

    addOption(ARG_JOBS, "number of threads used by parallel jobs executor", ARGNAME_NUMBER);

    addOption(BfConsts.ARG_LOG_LEVEL, "log level", ARGNAME_LOG_LEVEL);

    addBooleanOption(ARG_HISTOGRAM, "build histogram of unimplemented features");

    addOption(BfConsts.ARG_LOG_FILE, "path to main log file", ARGNAME_PATH);

    addBooleanOption(ARG_LOG_TEE, "print output to both logfile and standard out");

    addOption(
        ARG_MAX_PARSER_CONTEXT_LINES,
        "max number of surrounding lines to print on parser error",
        ARGNAME_NUMBER);

    addOption(
        ARG_MAX_PARSER_CONTEXT_TOKENS,
        "max number of context tokens to print on parser error",
        ARGNAME_NUMBER);

    addOption(
        ARG_MAX_PARSE_TREE_PRINT_LENGTH,
        "max number of characters to print for parsetree pretty print "
            + "(<= 0 is treated as no limit)",
        ARGNAME_NUMBER);

    addOption(ARG_MAX_RUNTIME_MS, "maximum time (in ms) to allow a task to run", ARGNAME_NUMBER);

    addBooleanOption(ARG_NO_SHUFFLE, "do not shuffle parallel jobs");

    addOption(BfConsts.ARG_OUTPUT_ENV, "name of output environment", ARGNAME_NAME);

    addOption(ARG_PARENT_PID, "name of parent PID", ARGNAME_NUMBER);

    addBooleanOption(
        BfConsts.ARG_PEDANTIC_AS_ERROR,
        "throws "
            + PedanticBatfishException.class.getSimpleName()
            + " for likely harmless warnings (e.g. deviation from good configuration style), "
            + "instead of emitting warning and continuing");

    addBooleanOption(BfConsts.ARG_PEDANTIC_SUPPRESS, "suppresses pedantic warnings");

    addBooleanOption(BfConsts.ARG_PRETTY_PRINT_ANSWER, "pretty print answer");

    addBooleanOption(ARG_PRINT_PARSE_TREES, "print parse trees");

    addBooleanOption(
        ARG_PRINT_SYMMETRIC_EDGES, "print topology with symmetric edges adjacent in listing");

    addOption(BfConsts.ARG_QUESTION_NAME, "name of question", ARGNAME_NAME);

    addBooleanOption(
        BfConsts.ARG_RED_FLAG_AS_ERROR,
        "throws "
            + RedFlagBatfishException.class.getSimpleName()
            + " on some recoverable errors (e.g. bad config lines), instead of emitting warning "
            + "and attempting to recover");

    addBooleanOption(BfConsts.ARG_RED_FLAG_SUPPRESS, "suppresses red-flag warnings");

    addOption(
        ARG_RUN_MODE,
        "mode to run in",
        Arrays.stream(RunMode.values()).map(v -> v.toString()).collect(Collectors.joining("|")));

    addBooleanOption(ARG_SEQUENTIAL, "force sequential operation");

    addBooleanOption(ARG_SERIALIZE_TO_TEXT, "serialize to text");

    addOption(
        ARG_SERVICE_BIND_HOST,
        "local hostname used bind service (default is 0.0.0.0 which listens on all interfaces)",
        ARGNAME_HOSTNAME);

    addOption(ARG_SERVICE_HOST, "local hostname to report to coordinator", ARGNAME_HOSTNAME);

    addOption(ARG_SERVICE_NAME, "service name", "service_name");

    addOption(ARG_SERVICE_PORT, "port for batfish service", ARGNAME_PORT);

    addBooleanOption(
        BfConsts.ARG_SSL_DISABLE, "whether to disable SSL during communication with coordinator");

    addBooleanOption(
        BfConsts.ARG_SSL_TRUST_ALL_CERTS,
        "whether to trust all SSL certificates during communication with coordinator");

    addBooleanOption(
        BfConsts.ARG_SYNTHESIZE_JSON_TOPOLOGY,
        "synthesize json topology from interface ip subnet information");

    addBooleanOption(
        BfConsts.ARG_SYNTHESIZE_TOPOLOGY,
        "synthesize topology from interface ip subnet information");

    addOption(BfConsts.ARG_TASK_PLUGIN, "fully-qualified name of task plugin class", ARGNAME_NAME);

    addOption(BfConsts.ARG_TESTRIG, "name of testrig", ARGNAME_NAME);

    addBooleanOption(ARG_THROW_ON_LEXER_ERROR, "throw exception immediately on lexer error");

    addBooleanOption(ARG_THROW_ON_PARSER_ERROR, "throw exception immediately on parser error");

    addBooleanOption(ARG_TIMESTAMP, "print timestamps in log messages");

    addOption(ARG_TRACING_AGENT_HOST, "jaeger agent host", "jaeger_agent_host");

    addOption(ARG_TRACING_AGENT_PORT, "jaeger agent port", "jaeger_agent_port");

    addBooleanOption(ARG_TRACING_ENABLE, "enable tracing");

    addBooleanOption(
        BfConsts.ARG_UNIMPLEMENTED_AS_ERROR,
        "throws "
            + UnimplementedBatfishException.class.getSimpleName()
            + " when encountering unimplemented configuration directives, instead of emitting "
            + "warning and ignoring");

    addBooleanOption(
        BfConsts.ARG_UNIMPLEMENTED_SUPPRESS,
        "suppresses warnings about unimplemented configuration directives");

    addBooleanOption(
        BfConsts.ARG_UNRECOGNIZED_AS_RED_FLAG,
        "treat unrecognized configuration directives as red flags instead of force-crashing");

    addBooleanOption(
        BfConsts.ARG_VERBOSE_PARSE,
        "(developer option) include parse/convert data in init-testrig answer");

    addBooleanOption(BfConsts.COMMAND_ANALYZE, "run provided analysis");

    addBooleanOption(BfConsts.COMMAND_ANSWER, "answer provided question");

    addBooleanOption(
        BfConsts.COMMAND_COMPILE_DIFF_ENVIRONMENT,
        "compile configurations for differential environment");

    addBooleanOption(BfConsts.COMMAND_DUMP_DP, "compute and serialize data plane");

    addBooleanOption(
        BfConsts.COMMAND_PARSE_VENDOR_INDEPENDENT, "serialize vendor-independent configs");

    addBooleanOption(BfConsts.COMMAND_PARSE_VENDOR_SPECIFIC, "serialize vendor configs");

    addBooleanOption(BfConsts.COMMAND_REPORT, "generate report based on answered questions");

    addBooleanOption(
        BfConsts.COMMAND_VALIDATE_ENVIRONMENT, "validate an environment that has been initialized");

    addOption(ARG_Z3_TIMEOUT, "set a timeout (in milliseconds) for Z3 queries", "z3timeout");

    addOption(
        ARG_DATAPLANE_ENGINE_NAME,
        "name of the dataplane generation engine to use.",
        "dataplane engine name");
  }

  public void parseCommandLine(String[] args) {
    initCommandLine(args);
    _config.setProperty(CAN_EXECUTE, true);

    // SPECIAL OPTIONS
    getStringOptionValue(BfConsts.ARG_LOG_FILE);
    getStringOptionValue(BfConsts.ARG_LOG_LEVEL);
    if (getBooleanOptionValue(ARG_HELP)) {
      _config.setProperty(CAN_EXECUTE, false);
      printHelp(EXECUTABLE_NAME);
      return;
    }

    // REGULAR OPTIONS
    getStringOptionValue(BfConsts.ARG_ANALYSIS_NAME);
    getBooleanOptionValue(BfConsts.COMMAND_ANALYZE);
    getBooleanOptionValue(BfConsts.COMMAND_ANSWER);
    getPathOptionValue(BfConsts.ARG_ANSWER_JSON_PATH);
    getBooleanOptionValue(BfConsts.ARG_BDP_RECORD_ALL_ITERATIONS);
    getBooleanOptionValue(BfConsts.ARG_BDP_DETAIL);
    getIntOptionValue(BfConsts.ARG_BDP_MAX_OSCILLATION_RECOVERY_ATTEMPTS);
    getIntOptionValue(BfConsts.ARG_BDP_MAX_RECORDED_ITERATIONS);
    getBooleanOptionValue(BfConsts.ARG_BDP_PRINT_ALL_ITERATIONS);
    getBooleanOptionValue(BfConsts.ARG_BDP_PRINT_OSCILLATING_ITERATIONS);
    getBooleanOptionValue(ARG_CHECK_BGP_REACHABILITY);
    getBooleanOptionValue(BfConsts.COMMAND_COMPILE_DIFF_ENVIRONMENT);
    getPathOptionValue(BfConsts.ARG_CONTAINER_DIR);
    getStringOptionValue(ARG_COORDINATOR_HOST);
    getIntOptionValue(ARG_COORDINATOR_POOL_PORT);
    getBooleanOptionValue(ARG_COORDINATOR_REGISTER);
    getBooleanOptionValue(BfConsts.COMMAND_DUMP_DP);
    getStringOptionValue(BfConsts.ARG_DELTA_ENVIRONMENT_NAME);
    getStringOptionValue(BfConsts.ARG_DELTA_TESTRIG);
    getBooleanOptionValue(BfConsts.ARG_DIFF_ACTIVE);
    getBooleanOptionValue(BfConsts.ARG_DIFFERENTIAL);
    getBooleanOptionValue(BfConsts.ARG_DISABLE_UNRECOGNIZED);
    getBooleanOptionValue(BfConsts.ARG_ENABLE_CISCO_NX_PARSER);
    getStringOptionValue(BfConsts.ARG_ENVIRONMENT_NAME);
    getBooleanOptionValue(ARG_EXIT_ON_FIRST_ERROR);
    getBooleanOptionValue(ARG_FLATTEN);
    getPathOptionValue(ARG_FLATTEN_DESTINATION);
    getBooleanOptionValue(ARG_FLATTEN_ON_THE_FLY);
    getBooleanOptionValue(ARG_GENERATE_STUBS);
    getStringOptionValue(ARG_GENERATE_STUBS_INPUT_ROLE);
    getStringOptionValue(ARG_GENERATE_STUBS_INTERFACE_DESCRIPTION_REGEX);
    getIntegerOptionValue(ARG_GENERATE_STUBS_REMOTE_AS);
    getBooleanOptionValue(BfConsts.ARG_HALT_ON_CONVERT_ERROR);
    getBooleanOptionValue(BfConsts.ARG_HALT_ON_PARSE_ERROR);
    getBooleanOptionValue(ARG_HISTOGRAM);
    getStringListOptionValue(BfConsts.ARG_IGNORE_FILES_WITH_STRINGS);
    getBooleanOptionValue(ARG_IGNORE_UNKNOWN);
    getBooleanOptionValue(ARG_IGNORE_UNSUPPORTED);
    getBooleanOptionValue(BfConsts.COMMAND_INIT_INFO);
    getIntOptionValue(ARG_JOBS);
    getBooleanOptionValue(ARG_LOG_TEE);
    getIntOptionValue(ARG_MAX_PARSER_CONTEXT_LINES);
    getIntOptionValue(ARG_MAX_PARSER_CONTEXT_TOKENS);
    getIntOptionValue(ARG_MAX_PARSE_TREE_PRINT_LENGTH);
    getIntOptionValue(ARG_MAX_RUNTIME_MS);
    getStringOptionValue(BfConsts.ARG_OUTPUT_ENV);
    getIntOptionValue(ARG_PARENT_PID);
    getBooleanOptionValue(BfConsts.ARG_PEDANTIC_AS_ERROR);
    getBooleanOptionValue(BfConsts.ARG_PEDANTIC_SUPPRESS);
    getBooleanOptionValue(BfConsts.ARG_PRETTY_PRINT_ANSWER);
    getBooleanOptionValue(ARG_PRINT_PARSE_TREES);
    getBooleanOptionValue(ARG_PRINT_SYMMETRIC_EDGES);
    getStringOptionValue(BfConsts.ARG_QUESTION_NAME);
    getBooleanOptionValue(BfConsts.ARG_RED_FLAG_AS_ERROR);
    getBooleanOptionValue(BfConsts.ARG_RED_FLAG_SUPPRESS);
    getBooleanOptionValue(BfConsts.COMMAND_REPORT);
    getStringOptionValue(ARG_RUN_MODE);
    getBooleanOptionValue(ARG_SEQUENTIAL);
    getBooleanOptionValue(BfConsts.COMMAND_PARSE_VENDOR_INDEPENDENT);
    getBooleanOptionValue(ARG_SERIALIZE_TO_TEXT);
    getBooleanOptionValue(BfConsts.COMMAND_PARSE_VENDOR_SPECIFIC);
    getStringOptionValue(ARG_SERVICE_BIND_HOST);
    getStringOptionValue(ARG_SERVICE_HOST);
    getStringOptionValue(ARG_SERVICE_NAME);
    getIntOptionValue(ARG_SERVICE_PORT);
    getBooleanOptionValue(ARG_NO_SHUFFLE);
    getBooleanOptionValue(ARG_DISABLE_Z3_SIMPLIFICATION);
    getBooleanOptionValue(BfConsts.ARG_SSL_DISABLE);
    getPathOptionValue(BfConsts.ARG_SSL_KEYSTORE_FILE);
    getStringOptionValue(BfConsts.ARG_SSL_KEYSTORE_PASSWORD);
    getBooleanOptionValue(BfConsts.ARG_SSL_TRUST_ALL_CERTS);
    getPathOptionValue(BfConsts.ARG_SSL_TRUSTSTORE_FILE);
    getStringOptionValue(BfConsts.ARG_SSL_TRUSTSTORE_PASSWORD);
    getBooleanOptionValue(BfConsts.ARG_SYNTHESIZE_JSON_TOPOLOGY);
    getStringOptionValue(BfConsts.ARG_TASK_PLUGIN);
    getStringOptionValue(BfConsts.ARG_TESTRIG);
    getBooleanOptionValue(ARG_THROW_ON_LEXER_ERROR);
    getBooleanOptionValue(ARG_THROW_ON_PARSER_ERROR);
    getBooleanOptionValue(ARG_TIMESTAMP);
    getStringOptionValue(ARG_TRACING_AGENT_HOST);
    getIntegerOptionValue(ARG_TRACING_AGENT_PORT);
    getBooleanOptionValue(ARG_TRACING_ENABLE);
    getBooleanOptionValue(BfConsts.ARG_UNIMPLEMENTED_AS_ERROR);
    getBooleanOptionValue(BfConsts.ARG_UNIMPLEMENTED_SUPPRESS);
    getBooleanOptionValue(BfConsts.ARG_UNRECOGNIZED_AS_RED_FLAG);
    getBooleanOptionValue(BfConsts.COMMAND_VALIDATE_ENVIRONMENT);
    getBooleanOptionValue(BfConsts.ARG_VERBOSE_PARSE);
    getIntegerOptionValue(ARG_Z3_TIMEOUT);
    getStringOptionValue(ARG_DATAPLANE_ENGINE_NAME);
  }

  public void setActiveTestrigSettings(TestrigSettings activeTestrigSettings) {
    _activeTestrigSettings = activeTestrigSettings;
  }

  @Override
  public void setBdpDetail(boolean bdpDetail) {
    _config.setProperty(BfConsts.ARG_BDP_DETAIL, bdpDetail);
  }

  @Override
  public void setBdpMaxOscillationRecoveryAttempts(int bdpMaxOscillationRecoveryAttempts) {
    _config.setProperty(
        BfConsts.ARG_BDP_MAX_OSCILLATION_RECOVERY_ATTEMPTS, bdpMaxOscillationRecoveryAttempts);
  }

  @Override
  public void setBdpMaxRecordedIterations(int bdpMaxRecordedIterations) {
    _config.setProperty(BfConsts.ARG_BDP_MAX_RECORDED_ITERATIONS, bdpMaxRecordedIterations);
  }

  @Override
  public void setBdpPrintAllIterations(boolean bdpPrintAllIterations) {
    _config.setProperty(BfConsts.ARG_BDP_PRINT_ALL_ITERATIONS, bdpPrintAllIterations);
  }

  @Override
  public void setBdpPrintOscillatingIterations(boolean bdpPrintOscillatingIterations) {
    _config.setProperty(
        BfConsts.ARG_BDP_PRINT_OSCILLATING_ITERATIONS, bdpPrintOscillatingIterations);
  }

  @Override
  public void setBdpRecordAllIterations(boolean bdpRecordAllIterations) {
    _config.setProperty(BfConsts.ARG_BDP_RECORD_ALL_ITERATIONS, bdpRecordAllIterations);
  }

  public void setCanExecute(boolean canExecute) {
    _config.setProperty(CAN_EXECUTE, canExecute);
  }

  public void setContainerDir(Path containerDir) {
    _config.setProperty(BfConsts.ARG_CONTAINER_DIR, containerDir.toString());
  }

  public void setDeltaEnvironmentName(String diffEnvironmentName) {
    _config.setProperty(BfConsts.ARG_DELTA_ENVIRONMENT_NAME, diffEnvironmentName);
  }

  public void setDeltaTestrig(String deltaTestrig) {
    _config.setProperty(BfConsts.ARG_DELTA_TESTRIG, deltaTestrig);
  }

  public void setDiffActive(boolean diffActive) {
    _config.setProperty(BfConsts.ARG_DIFF_ACTIVE, diffActive);
  }

  public void setDiffQuestion(boolean diffQuestion) {
    _config.setProperty(DIFFERENTIAL_QUESTION, diffQuestion);
  }

  @Override
  public void setDisableUnrecognized(boolean b) {
    _config.setProperty(BfConsts.ARG_DISABLE_UNRECOGNIZED, b);
  }

  public void setEnableCiscoNxParser(boolean b) {
    _config.setProperty(BfConsts.ARG_ENABLE_CISCO_NX_PARSER, b);
  }

  public void setEnvironmentName(String envName) {
    _config.setProperty(BfConsts.ARG_ENVIRONMENT_NAME, envName);
  }

  public void setHaltOnConvertError(boolean haltOnConvertError) {
    _config.setProperty(BfConsts.ARG_HALT_ON_CONVERT_ERROR, haltOnConvertError);
  }

  public void setHaltOnParseError(boolean haltOnParseError) {
    _config.setProperty(BfConsts.ARG_HALT_ON_PARSE_ERROR, haltOnParseError);
  }

  public void setInitInfo(boolean initInfo) {
    _config.setProperty(BfConsts.COMMAND_INIT_INFO, initInfo);
  }

  public void setLogger(BatfishLogger logger) {
    _logger = logger;
  }

  public void setMaxParserContextLines(int maxParserContextLines) {
    _config.setProperty(ARG_MAX_PARSER_CONTEXT_LINES, maxParserContextLines);
  }

  public void setMaxParserContextTokens(int maxParserContextTokens) {
    _config.setProperty(ARG_MAX_PARSER_CONTEXT_TOKENS, maxParserContextTokens);
  }

  public void setMaxParseTreePrintLength(int maxParseTreePrintLength) {
    _config.setProperty(ARG_MAX_PARSE_TREE_PRINT_LENGTH, maxParseTreePrintLength);
  }

  public void setMaxRuntimeMs(int runtimeMs) {
    _config.setProperty(ARG_MAX_RUNTIME_MS, runtimeMs);
  }

  @Override
  public void setPrintParseTree(boolean printParseTree) {
    _config.setProperty(ARG_PRINT_PARSE_TREES, printParseTree);
  }

  public void setQuestionPath(@Nullable Path questionPath) {
    if (questionPath != null) {
      _config.setProperty(QUESTION_PATH, questionPath.toString());
    } else {
      _config.clearProperty(QUESTION_PATH);
    }
  }

  public void setReport(boolean report) {
    _config.setProperty(BfConsts.COMMAND_REPORT, report);
  }

  public void setRunMode(RunMode runMode) {
    _config.setProperty(ARG_RUN_MODE, runMode.toString());
  }

  public void setSequential(boolean sequential) {
    _config.setProperty(ARG_SEQUENTIAL, sequential);
  }

  public void setSslDisable(boolean sslDisable) {
    _config.setProperty(BfConsts.ARG_SSL_DISABLE, sslDisable);
  }

  public void setSslKeystoreFile(Path sslKeystoreFile) {
    _config.setProperty(BfConsts.ARG_SSL_KEYSTORE_FILE, sslKeystoreFile.toString());
  }

  public void setSslKeystorePassword(String sslKeystorePassword) {
    _config.setProperty(BfConsts.ARG_SSL_KEYSTORE_PASSWORD, sslKeystorePassword);
  }

  public void setSslTrustAllCerts(boolean sslTrustAllCerts) {
    _config.setProperty(BfConsts.ARG_SSL_TRUST_ALL_CERTS, sslTrustAllCerts);
  }

  public void setSslTruststoreFile(Path sslTruststoreFile) {
    _config.setProperty(BfConsts.ARG_SSL_TRUSTSTORE_FILE, sslTruststoreFile.toString());
  }

  public void setSslTruststorePassword(String sslTruststorePassword) {
    _config.setProperty(BfConsts.ARG_SSL_TRUSTSTORE_PASSWORD, sslTruststorePassword);
  }

  public void setTaskId(String taskId) {
    _config.setProperty(TASK_ID, taskId);
  }

  public void setTestrig(String testrig) {
    _config.setProperty(BfConsts.ARG_TESTRIG, testrig);
  }

  @Override
  public void setThrowOnLexerError(boolean throwOnLexerError) {
    _config.setProperty(ARG_THROW_ON_LEXER_ERROR, throwOnLexerError);
  }

  @Override
  public void setThrowOnParserError(boolean throwOnParserError) {
    _config.setProperty(ARG_THROW_ON_PARSER_ERROR, throwOnParserError);
  }

  public void setUnrecognizedAsRedFlag(boolean unrecognizedAsRedFlag) {
    _config.setProperty(BfConsts.ARG_UNRECOGNIZED_AS_RED_FLAG, unrecognizedAsRedFlag);
  }

  public void setValidateEnvironment(boolean validateEnvironment) {
    _config.setProperty(BfConsts.COMMAND_VALIDATE_ENVIRONMENT, validateEnvironment);
  }

  public void setVerboseParse(boolean verboseParse) {
    _config.setProperty(BfConsts.ARG_VERBOSE_PARSE, verboseParse);
  }

  public void setZ3Timeout(int z3Timeout) {
    _config.setProperty(ARG_Z3_TIMEOUT, z3Timeout);
  }

  public void setDataplaneEngineName(String name) {
    _config.setProperty(ARG_DATAPLANE_ENGINE_NAME, name);
  }
}
