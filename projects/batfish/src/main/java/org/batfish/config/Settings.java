package org.batfish.config;

import java.nio.file.Path;
import java.util.List;
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
import org.batfish.grammar.GrammarSettings;

public final class Settings extends BaseSettings implements BdpSettings, GrammarSettings {

  public static final class EnvironmentSettings {

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

    private Path _convertAnswerPath;

    private EnvironmentSettings _environmentSettings;

    private Path _inferredNodeRolesPath;

    private String _name;

    private Path _nodeRolesPath;

    private Path _parseAnswerPath;

    private Path _protocolDependencyGraphPath;

    private Path _protocolDependencyGraphZipPath;

    private Path _serializeIndependentPath;

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

    public Path getConvertAnswerPath() {
      return _convertAnswerPath;
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

    public Path getProtocolDependencyGraphPath() {
      return _protocolDependencyGraphPath;
    }

    public Path getProtocolDependencyGraphZipPath() {
      return _protocolDependencyGraphZipPath;
    }

    public Path getSerializeIndependentPath() {
      return _serializeIndependentPath;
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

    public void setConvertAnswerPath(Path convertAnswerPath) {
      _convertAnswerPath = convertAnswerPath;
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

    public void setProtocolDependencyGraphPath(Path protocolDependencyGraphPath) {
      _protocolDependencyGraphPath = protocolDependencyGraphPath;
    }

    public void setProtocolDependencyGraphZipPath(Path protocolDependencyGraphZipPath) {
      _protocolDependencyGraphZipPath = protocolDependencyGraphZipPath;
    }

    public void setSerializeIndependentPath(Path path) {
      _serializeIndependentPath = path;
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

  private static final String ARG_ANONYMIZE = "anonymize";

  public static final String ARG_COORDINATOR_HOST = "coordinatorhost";

  private static final String ARG_COORDINATOR_POOL_PORT = "coordinatorpoolport";

  public static final String ARG_COORDINATOR_REGISTER = "register";

  private static final String ARG_COORDINATOR_WORK_PORT = "coordinatorworkport";

  private static final String ARG_DISABLE_Z3_SIMPLIFICATION = "nosimplify";

  private static final String ARG_EXIT_ON_FIRST_ERROR = "ee";

  private static final String ARG_FLATTEN = "flatten";

  private static final String ARG_FLATTEN_DESTINATION = "flattendst";

  private static final String ARG_FLATTEN_ON_THE_FLY = "flattenonthefly";

  private static final String ARG_GEN_OSPF_TOPLOGY_PATH = "genospf";

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

  private static final String ARG_MAX_RUNTIME_MS = "maxruntime";

  private static final String ARG_NO_SHUFFLE = "noshuffle";

  private static final String ARG_PRINT_PARSE_TREES = "ppt";

  private static final String ARG_PRINT_SYMMETRIC_EDGES = "printsymmetricedges";

  private static final String ARG_SEQUENTIAL = "sequential";

  private static final String ARG_SERIALIZE_TO_TEXT = "stext";

  private static final String ARG_SERVICE_BIND_HOST = "servicebindhost";

  public static final String ARG_SERVICE_HOST = "servicehost";

  public static final String ARG_SERVICE_MODE = "servicemode";

  private static final String ARG_SERVICE_PORT = "serviceport";

  private static final String ARG_TRACING_AGENT_HOST = "tracingagenthost";

  private static final String ARG_TRACING_AGENT_PORT = "tracingagentport";

  public static final String ARG_TRACING_ENABLE = "tracingenable";

  private static final String ARG_THROW_ON_LEXER_ERROR = "throwlexer";

  private static final String ARG_THROW_ON_PARSER_ERROR = "throwparser";

  private static final String ARG_TIMESTAMP = "timestamp";

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

  private TestrigSettings _activeTestrigSettings;

  private String _analysisName;

  private boolean _analyze;

  private boolean _anonymize;

  private boolean _answer;

  private Path _answerJsonPath;

  private TestrigSettings _baseTestrigSettings;

  private boolean _bdpDetail;

  private int _bdpMaxOscillationRecoveryAttempts;

  private int _bdpMaxRecordedIterations;

  private boolean _bdpPrintAllIterations;

  private boolean _bdpPrintOscillatingIterations;

  private boolean _bdpRecordAllIterations;

  private List<String> _blockNames;

  private boolean _canExecute;

  private boolean _compileDiffEnvironment;

  private Path _containerDir;

  private String _coordinatorHost;

  private int _coordinatorPoolPort;

  private boolean _coordinatorRegister;

  private int _coordinatorWorkPort;

  private boolean _dataPlane;

  private String _deltaEnvironmentName;

  private String _deltaTestrig;

  private TestrigSettings _deltaTestrigSettings;

  private boolean _diffActive;

  private boolean _differential;

  private boolean _diffQuestion;

  private boolean _disableUnrecognized;

  private String _environmentName;

  private boolean _exitOnFirstError;

  private boolean _flatten;

  private Path _flattenDestination;

  private boolean _flattenOnTheFly;

  private boolean _generateStubs;

  private String _generateStubsInputRole;

  private String _generateStubsInterfaceDescriptionRegex;

  private Integer _generateStubsRemoteAs;

  private Path _genOspfTopologyPath;

  private boolean _haltOnConvertError;

  private boolean _haltOnParseError;

  private List<String> _helpPredicates;

  private boolean _histogram;

  private List<String> _ignoreFilesWithStrings;

  private boolean _ignoreUnknown;

  private boolean _ignoreUnsupported;

  private boolean _initInfo;

  private int _jobs;

  private String _logFile;

  private BatfishLogger _logger;

  private String _logLevel;

  private boolean _logTee;

  private int _maxParserContextLines;

  private int _maxParserContextTokens;

  private int _maxRuntimeMs;

  private String _outputEnvironmentName;

  private boolean _pedanticAsError;

  private boolean _pedanticRecord;

  private List<String> _predicates;

  private boolean _prettyPrintAnswer;

  private boolean _printParseTree;

  private boolean _printSymmetricEdges;

  private String _questionName;

  private Path _questionPath;

  private boolean _redFlagAsError;

  private boolean _redFlagRecord;

  private boolean _report;

  private boolean _runInServiceMode;

  private boolean _sequential;

  private boolean _serializeIndependent;

  private boolean _serializeToText;

  private boolean _serializeVendor;

  private String _serviceBindHost;

  private String _serviceHost;

  private int _servicePort;

  private boolean _shuffleJobs;

  private boolean _simplify;

  private boolean _sslDisable;

  private Path _sslKeystoreFile;

  private String _sslKeystorePassword;

  private boolean _sslTrustAllCerts;

  private Path _sslTruststoreFile;

  private String _sslTruststorePassword;

  private boolean _synthesizeJsonTopology;

  private String _taskId;

  private String _taskPlugin;

  private String _testrig;

  private boolean _throwOnLexerError;

  private boolean _throwOnParserError;

  private boolean _timestamp;

  private String _tracingAgentHost;

  private Integer _tracingAgentPort;

  private boolean _tracingEnable;

  private boolean _unimplementedAsError;

  private boolean _unimplementedRecord;

  private boolean _unrecognizedAsRedFlag;

  private boolean _validateEnvironment;

  private boolean _verboseParse;

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

  public boolean canExecute() {
    return _canExecute;
  }

  public boolean flattenOnTheFly() {
    return _flattenOnTheFly;
  }

  public TestrigSettings getActiveTestrigSettings() {
    return _activeTestrigSettings;
  }

  public String getAnalysisName() {
    return _analysisName;
  }

  public boolean getAnalyze() {
    return _analyze;
  }

  public boolean getAnonymize() {
    return _anonymize;
  }

  public boolean getAnswer() {
    return _answer;
  }

  public Path getAnswerJsonPath() {
    return _answerJsonPath;
  }

  public TestrigSettings getBaseTestrigSettings() {
    return _baseTestrigSettings;
  }

  public boolean getBdpDetail() {
    return _bdpDetail;
  }

  public int getBdpMaxOscillationRecoveryAttempts() {
    return _bdpMaxOscillationRecoveryAttempts;
  }

  public int getBdpMaxRecordedIterations() {
    return _bdpMaxRecordedIterations;
  }

  public boolean getBdpPrintAllIterations() {
    return _bdpPrintAllIterations;
  }

  public boolean getBdpPrintOscillatingIterations() {
    return _bdpPrintOscillatingIterations;
  }

  public boolean getBdpRecordAllIterations() {
    return _bdpRecordAllIterations;
  }

  public List<String> getBlockNames() {
    return _blockNames;
  }

  public boolean getCompileEnvironment() {
    return _compileDiffEnvironment;
  }

  public Path getContainerDir() {
    return _containerDir;
  }

  public String getCoordinatorHost() {
    return _coordinatorHost;
  }

  public int getCoordinatorPoolPort() {
    return _coordinatorPoolPort;
  }

  public boolean getCoordinatorRegister() {
    return _coordinatorRegister;
  }

  public int getCoordinatorWorkPort() {
    return _coordinatorWorkPort;
  }

  public boolean getDataPlane() {
    return _dataPlane;
  }

  public String getDeltaEnvironmentName() {
    return _deltaEnvironmentName;
  }

  public String getDeltaTestrig() {
    return _deltaTestrig;
  }

  public TestrigSettings getDeltaTestrigSettings() {
    return _deltaTestrigSettings;
  }

  public boolean getDiffActive() {
    return _diffActive;
  }

  public boolean getDifferential() {
    return _differential;
  }

  public boolean getDiffQuestion() {
    return _diffQuestion;
  }

  @Override
  public boolean getDisableUnrecognized() {
    return _disableUnrecognized;
  }

  public String getEnvironmentName() {
    return _environmentName;
  }

  public boolean getExitOnFirstError() {
    return _exitOnFirstError;
  }

  public boolean getFlatten() {
    return _flatten;
  }

  public Path getFlattenDestination() {
    return _flattenDestination;
  }

  public Path getGenerateOspfTopologyPath() {
    return _genOspfTopologyPath;
  }

  public boolean getGenerateStubs() {
    return _generateStubs;
  }

  public String getGenerateStubsInputRole() {
    return _generateStubsInputRole;
  }

  public String getGenerateStubsInterfaceDescriptionRegex() {
    return _generateStubsInterfaceDescriptionRegex;
  }

  public int getGenerateStubsRemoteAs() {
    return _generateStubsRemoteAs;
  }

  public boolean getHaltOnConvertError() {
    return _haltOnConvertError;
  }

  public boolean getHaltOnParseError() {
    return _haltOnParseError;
  }

  public List<String> getHelpPredicates() {
    return _helpPredicates;
  }

  public boolean getHistogram() {
    return _histogram;
  }

  public boolean getInitInfo() {
    return _initInfo;
  }

  public int getJobs() {
    return _jobs;
  }

  public String getLogFile() {
    return _logFile;
  }

  public BatfishLogger getLogger() {
    return _logger;
  }

  public String getLogLevel() {
    return _logLevel;
  }

  public boolean getLogTee() {
    return _logTee;
  }

  @Override
  public int getMaxParserContextLines() {
    return _maxParserContextLines;
  }

  @Override
  public int getMaxParserContextTokens() {
    return _maxParserContextTokens;
  }

  public int getMaxRuntimeMs() {
    return _maxRuntimeMs;
  }

  public String getOutputEnvironmentName() {
    return _outputEnvironmentName;
  }

  public boolean getPedanticAsError() {
    return _pedanticAsError;
  }

  public boolean getPedanticRecord() {
    return _pedanticRecord;
  }

  public List<String> getPredicates() {
    return _predicates;
  }

  @Override
  public boolean getPrintParseTree() {
    return _printParseTree;
  }

  public boolean getPrintSymmetricEdgePairs() {
    return _printSymmetricEdges;
  }

  public String getQuestionName() {
    return _questionName;
  }

  public Path getQuestionPath() {
    return _questionPath;
  }

  public boolean getRedFlagAsError() {
    return _redFlagAsError;
  }

  public boolean getRedFlagRecord() {
    return _redFlagRecord;
  }

  public boolean getReport() {
    return _report;
  }

  public boolean getSequential() {
    return _sequential;
  }

  public boolean getSerializeIndependent() {
    return _serializeIndependent;
  }

  public boolean getSerializeToText() {
    return _serializeToText;
  }

  public boolean getSerializeVendor() {
    return _serializeVendor;
  }

  public String getServiceBindHost() {
    return _serviceBindHost;
  }

  public String getServiceHost() {
    return _serviceHost;
  }

  public int getServicePort() {
    return _servicePort;
  }

  public boolean getShuffleJobs() {
    return _shuffleJobs;
  }

  public boolean getSimplify() {
    return _simplify;
  }

  public boolean getSslDisable() {
    return _sslDisable;
  }

  public Path getSslKeystoreFile() {
    return _sslKeystoreFile;
  }

  public String getSslKeystorePassword() {
    return _sslKeystorePassword;
  }

  public boolean getSslTrustAllCerts() {
    return _sslTrustAllCerts;
  }

  public Path getSslTruststoreFile() {
    return _sslTruststoreFile;
  }

  public String getSslTruststorePassword() {
    return _sslTruststorePassword;
  }

  public boolean getSynthesizeJsonTopology() {
    return _synthesizeJsonTopology;
  }

  public String getTaskId() {
    return _taskId;
  }

  public String getTaskPlugin() {
    return _taskPlugin;
  }

  public String getTestrig() {
    return _testrig;
  }

  @Override
  public boolean getThrowOnLexerError() {
    return _throwOnLexerError;
  }

  @Override
  public boolean getThrowOnParserError() {
    return _throwOnParserError;
  }

  public boolean getTimestamp() {
    return _timestamp;
  }

  public Integer getTracingAgentPort() {
    return _tracingAgentPort;
  }

  public String getTracingAgentHost() {
    return _tracingAgentHost;
  }

  public boolean getTracingEnable() {
    return _tracingEnable;
  }

  public boolean getUnimplementedAsError() {
    return _unimplementedAsError;
  }

  public boolean getUnimplementedRecord() {
    return _unimplementedRecord;
  }

  public boolean getUnrecognizedAsRedFlag() {
    return _unrecognizedAsRedFlag;
  }

  public boolean getValidateEnvironment() {
    return _validateEnvironment;
  }

  public boolean getVerboseParse() {
    return _verboseParse;
  }

  public List<String> ignoreFilesWithStrings() {
    return _ignoreFilesWithStrings;
  }

  public boolean ignoreUnknown() {
    return _ignoreUnknown;
  }

  public boolean ignoreUnsupported() {
    return _ignoreUnsupported;
  }

  private void initConfigDefaults() {
    setDefaultProperty(BfConsts.ARG_ANALYSIS_NAME, null);
    setDefaultProperty(ARG_ANONYMIZE, false);
    setDefaultProperty(BfConsts.ARG_ANSWER_JSON_PATH, null);
    setDefaultProperty(BfConsts.ARG_BDP_DETAIL, false);
    setDefaultProperty(BfConsts.ARG_BDP_MAX_OSCILLATION_RECOVERY_ATTEMPTS, 0);
    setDefaultProperty(BfConsts.ARG_BDP_MAX_RECORDED_ITERATIONS, 5);
    setDefaultProperty(BfConsts.ARG_BDP_PRINT_ALL_ITERATIONS, false);
    setDefaultProperty(BfConsts.ARG_BDP_PRINT_OSCILLATING_ITERATIONS, false);
    setDefaultProperty(BfConsts.ARG_BDP_RECORD_ALL_ITERATIONS, false);
    setDefaultProperty(BfConsts.ARG_BLOCK_NAMES, new String[] {});
    setDefaultProperty(BfConsts.ARG_CONTAINER_DIR, null);
    setDefaultProperty(ARG_COORDINATOR_REGISTER, false);
    setDefaultProperty(ARG_COORDINATOR_HOST, "localhost");
    setDefaultProperty(ARG_COORDINATOR_POOL_PORT, CoordConsts.SVC_CFG_POOL_PORT);
    setDefaultProperty(ARG_COORDINATOR_WORK_PORT, CoordConsts.SVC_CFG_WORK_PORT);
    setDefaultProperty(BfConsts.ARG_DIFF_ACTIVE, false);
    setDefaultProperty(BfConsts.ARG_DELTA_ENVIRONMENT_NAME, null);
    setDefaultProperty(BfConsts.ARG_DIFFERENTIAL, false);
    setDefaultProperty(BfConsts.ARG_DISABLE_UNRECOGNIZED, false);
    setDefaultProperty(ARG_DISABLE_Z3_SIMPLIFICATION, false);
    setDefaultProperty(BfConsts.ARG_ENVIRONMENT_NAME, null);
    setDefaultProperty(ARG_EXIT_ON_FIRST_ERROR, false);
    setDefaultProperty(ARG_FLATTEN, false);
    setDefaultProperty(ARG_FLATTEN_DESTINATION, null);
    setDefaultProperty(ARG_FLATTEN_ON_THE_FLY, true);
    setDefaultProperty(ARG_GEN_OSPF_TOPLOGY_PATH, null);
    setDefaultProperty(ARG_GENERATE_STUBS, false);
    setDefaultProperty(ARG_GENERATE_STUBS_INPUT_ROLE, null);
    setDefaultProperty(ARG_GENERATE_STUBS_INTERFACE_DESCRIPTION_REGEX, null);
    setDefaultProperty(ARG_GENERATE_STUBS_REMOTE_AS, null);
    setDefaultProperty(BfConsts.ARG_HALT_ON_CONVERT_ERROR, false);
    setDefaultProperty(BfConsts.ARG_HALT_ON_PARSE_ERROR, false);
    setDefaultProperty(ARG_HELP, false);
    setDefaultProperty(ARG_HISTOGRAM, false);
    setDefaultProperty(ARG_IGNORE_UNSUPPORTED, true);
    setDefaultProperty(ARG_IGNORE_UNKNOWN, true);
    setDefaultProperty(ARG_JOBS, Integer.MAX_VALUE);
    setDefaultProperty(BfConsts.ARG_LOG_FILE, null);
    setDefaultProperty(ARG_LOG_TEE, false);
    setDefaultProperty(BfConsts.ARG_LOG_LEVEL, "debug");
    setDefaultProperty(ARG_MAX_PARSER_CONTEXT_LINES, 10);
    setDefaultProperty(ARG_MAX_PARSER_CONTEXT_TOKENS, 10);
    setDefaultProperty(ARG_MAX_RUNTIME_MS, 0);
    setDefaultProperty(ARG_NO_SHUFFLE, false);
    setDefaultProperty(BfConsts.ARG_OUTPUT_ENV, null);
    setDefaultProperty(BfConsts.ARG_PEDANTIC_AS_ERROR, false);
    setDefaultProperty(BfConsts.ARG_PEDANTIC_SUPPRESS, false);
    setDefaultProperty(BfConsts.ARG_PRETTY_PRINT_ANSWER, false);
    setDefaultProperty(ARG_PRINT_PARSE_TREES, false);
    setDefaultProperty(ARG_PRINT_SYMMETRIC_EDGES, false);
    setDefaultProperty(BfConsts.ARG_QUESTION_NAME, null);
    setDefaultProperty(BfConsts.ARG_RED_FLAG_AS_ERROR, false);
    setDefaultProperty(BfConsts.ARG_RED_FLAG_SUPPRESS, false);
    setDefaultProperty(ARG_SEQUENTIAL, false);
    setDefaultProperty(ARG_SERIALIZE_TO_TEXT, false);
    setDefaultProperty(ARG_SERVICE_BIND_HOST, Ip.ZERO.toString());
    setDefaultProperty(ARG_SERVICE_HOST, "localhost");
    setDefaultProperty(ARG_SERVICE_MODE, false);
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
  }

  private void initOptions() {

    addOption(BfConsts.ARG_ANALYSIS_NAME, "name of analysis", ARGNAME_NAME);

    addBooleanOption(ARG_ANONYMIZE, "created anonymized versions of configs in test rig");

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

    addListOption(
        BfConsts.ARG_BLOCK_NAMES, "list of blocks of logic rules to add or remove", "blocknames");

    addOption(BfConsts.ARG_CONTAINER_DIR, "path to container directory", ARGNAME_PATH);

    addOption(
        ARG_COORDINATOR_HOST,
        "hostname of coordinator for registration with -" + ARG_SERVICE_MODE,
        ARGNAME_HOSTNAME);

    addOption(ARG_COORDINATOR_POOL_PORT, "coordinator pool manager listening port", ARGNAME_PORT);

    addBooleanOption(ARG_COORDINATOR_REGISTER, "register service with coordinator on startup");

    addOption(ARG_COORDINATOR_WORK_PORT, "coordinator work manager listening port", "port_number");

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

    addOption(
        ARG_GEN_OSPF_TOPLOGY_PATH, "generate ospf configs from specified topology", ARGNAME_PATH);

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

    addOption(ARG_MAX_RUNTIME_MS, "maximum time (in ms) to allow a task to run", ARGNAME_NUMBER);

    addBooleanOption(ARG_NO_SHUFFLE, "do not shuffle parallel jobs");

    addOption(BfConsts.ARG_OUTPUT_ENV, "name of output environment", ARGNAME_NAME);

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

    addBooleanOption(ARG_SEQUENTIAL, "force sequential operation");

    addBooleanOption(ARG_SERIALIZE_TO_TEXT, "serialize to text");

    addOption(
        ARG_SERVICE_BIND_HOST,
        "local hostname used bind service (default is 0.0.0.0 which listens on all interfaces)",
        ARGNAME_HOSTNAME);

    addOption(ARG_SERVICE_HOST, "local hostname to report to coordinator", ARGNAME_HOSTNAME);

    addBooleanOption(ARG_SERVICE_MODE, "run in service mode");

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
  }

  private void parseCommandLine(String[] args) {
    initCommandLine(args);
    _canExecute = true;
    _runInServiceMode = false;

    // SPECIAL OPTIONS
    _logFile = getStringOptionValue(BfConsts.ARG_LOG_FILE);
    _logLevel = getStringOptionValue(BfConsts.ARG_LOG_LEVEL);
    if (getBooleanOptionValue(ARG_HELP)) {
      _canExecute = false;
      printHelp(EXECUTABLE_NAME);
      return;
    }

    // REGULAR OPTIONS
    _anonymize = getBooleanOptionValue(ARG_ANONYMIZE);
    _analysisName = getStringOptionValue(BfConsts.ARG_ANALYSIS_NAME);
    _analyze = getBooleanOptionValue(BfConsts.COMMAND_ANALYZE);
    _answer = getBooleanOptionValue(BfConsts.COMMAND_ANSWER);
    _answerJsonPath = getPathOptionValue(BfConsts.ARG_ANSWER_JSON_PATH);
    _bdpRecordAllIterations = getBooleanOptionValue(BfConsts.ARG_BDP_RECORD_ALL_ITERATIONS);
    _bdpDetail = getBooleanOptionValue(BfConsts.ARG_BDP_DETAIL);
    _bdpMaxOscillationRecoveryAttempts =
        getIntOptionValue(BfConsts.ARG_BDP_MAX_OSCILLATION_RECOVERY_ATTEMPTS);
    _bdpMaxRecordedIterations = getIntOptionValue(BfConsts.ARG_BDP_MAX_RECORDED_ITERATIONS);
    _bdpPrintAllIterations = getBooleanOptionValue(BfConsts.ARG_BDP_PRINT_ALL_ITERATIONS);
    _bdpPrintOscillatingIterations =
        getBooleanOptionValue(BfConsts.ARG_BDP_PRINT_OSCILLATING_ITERATIONS);
    _blockNames = getStringListOptionValue(BfConsts.ARG_BLOCK_NAMES);
    _compileDiffEnvironment = getBooleanOptionValue(BfConsts.COMMAND_COMPILE_DIFF_ENVIRONMENT);
    _containerDir = getPathOptionValue(BfConsts.ARG_CONTAINER_DIR);
    _coordinatorHost = getStringOptionValue(ARG_COORDINATOR_HOST);
    _coordinatorPoolPort = getIntOptionValue(ARG_COORDINATOR_POOL_PORT);
    _coordinatorRegister = getBooleanOptionValue(ARG_COORDINATOR_REGISTER);
    _coordinatorWorkPort = getIntOptionValue(ARG_COORDINATOR_WORK_PORT);
    _dataPlane = getBooleanOptionValue(BfConsts.COMMAND_DUMP_DP);
    _deltaEnvironmentName = getStringOptionValue(BfConsts.ARG_DELTA_ENVIRONMENT_NAME);
    _deltaTestrig = getStringOptionValue(BfConsts.ARG_DELTA_TESTRIG);
    _diffActive = getBooleanOptionValue(BfConsts.ARG_DIFF_ACTIVE);
    _differential = getBooleanOptionValue(BfConsts.ARG_DIFFERENTIAL);
    _disableUnrecognized = getBooleanOptionValue(BfConsts.ARG_DISABLE_UNRECOGNIZED);
    _environmentName = getStringOptionValue(BfConsts.ARG_ENVIRONMENT_NAME);
    _exitOnFirstError = getBooleanOptionValue(ARG_EXIT_ON_FIRST_ERROR);
    _flatten = getBooleanOptionValue(ARG_FLATTEN);
    _flattenDestination = getPathOptionValue(ARG_FLATTEN_DESTINATION);
    _flattenOnTheFly = getBooleanOptionValue(ARG_FLATTEN_ON_THE_FLY);
    _generateStubs = getBooleanOptionValue(ARG_GENERATE_STUBS);
    _generateStubsInputRole = getStringOptionValue(ARG_GENERATE_STUBS_INPUT_ROLE);
    _generateStubsInterfaceDescriptionRegex =
        getStringOptionValue(ARG_GENERATE_STUBS_INTERFACE_DESCRIPTION_REGEX);
    _generateStubsRemoteAs = getIntegerOptionValue(ARG_GENERATE_STUBS_REMOTE_AS);
    _genOspfTopologyPath = getPathOptionValue(ARG_GEN_OSPF_TOPLOGY_PATH);
    _haltOnConvertError = getBooleanOptionValue(BfConsts.ARG_HALT_ON_CONVERT_ERROR);
    _haltOnParseError = getBooleanOptionValue(BfConsts.ARG_HALT_ON_PARSE_ERROR);
    _histogram = getBooleanOptionValue(ARG_HISTOGRAM);
    _ignoreFilesWithStrings = getStringListOptionValue(BfConsts.ARG_IGNORE_FILES_WITH_STRINGS);
    _ignoreUnknown = getBooleanOptionValue(ARG_IGNORE_UNKNOWN);
    _ignoreUnsupported = getBooleanOptionValue(ARG_IGNORE_UNSUPPORTED);
    _initInfo = getBooleanOptionValue(BfConsts.COMMAND_INIT_INFO);
    _jobs = getIntOptionValue(ARG_JOBS);
    _logTee = getBooleanOptionValue(ARG_LOG_TEE);
    _maxParserContextLines = getIntOptionValue(ARG_MAX_PARSER_CONTEXT_LINES);
    _maxParserContextTokens = getIntOptionValue(ARG_MAX_PARSER_CONTEXT_TOKENS);
    _maxRuntimeMs = getIntOptionValue(ARG_MAX_RUNTIME_MS);
    _outputEnvironmentName = getStringOptionValue(BfConsts.ARG_OUTPUT_ENV);
    _pedanticAsError = getBooleanOptionValue(BfConsts.ARG_PEDANTIC_AS_ERROR);
    _pedanticRecord = !getBooleanOptionValue(BfConsts.ARG_PEDANTIC_SUPPRESS);
    _prettyPrintAnswer = getBooleanOptionValue(BfConsts.ARG_PRETTY_PRINT_ANSWER);
    _printParseTree = getBooleanOptionValue(ARG_PRINT_PARSE_TREES);
    _printSymmetricEdges = getBooleanOptionValue(ARG_PRINT_SYMMETRIC_EDGES);
    _questionName = getStringOptionValue(BfConsts.ARG_QUESTION_NAME);
    _redFlagAsError = getBooleanOptionValue(BfConsts.ARG_RED_FLAG_AS_ERROR);
    _redFlagRecord = !getBooleanOptionValue(BfConsts.ARG_RED_FLAG_SUPPRESS);
    _report = getBooleanOptionValue(BfConsts.COMMAND_REPORT);
    _runInServiceMode = getBooleanOptionValue(ARG_SERVICE_MODE);
    _sequential = getBooleanOptionValue(ARG_SEQUENTIAL);
    _serializeIndependent = getBooleanOptionValue(BfConsts.COMMAND_PARSE_VENDOR_INDEPENDENT);
    _serializeToText = getBooleanOptionValue(ARG_SERIALIZE_TO_TEXT);
    _serializeVendor = getBooleanOptionValue(BfConsts.COMMAND_PARSE_VENDOR_SPECIFIC);
    _serviceBindHost = getStringOptionValue(ARG_SERVICE_BIND_HOST);
    _serviceHost = getStringOptionValue(ARG_SERVICE_HOST);
    _servicePort = getIntOptionValue(ARG_SERVICE_PORT);
    _shuffleJobs = !getBooleanOptionValue(ARG_NO_SHUFFLE);
    _simplify = !getBooleanOptionValue(ARG_DISABLE_Z3_SIMPLIFICATION);
    _sslDisable = getBooleanOptionValue(BfConsts.ARG_SSL_DISABLE);
    _sslKeystoreFile = getPathOptionValue(BfConsts.ARG_SSL_KEYSTORE_FILE);
    _sslKeystorePassword = getStringOptionValue(BfConsts.ARG_SSL_KEYSTORE_PASSWORD);
    _sslTrustAllCerts = getBooleanOptionValue(BfConsts.ARG_SSL_TRUST_ALL_CERTS);
    _sslTruststoreFile = getPathOptionValue(BfConsts.ARG_SSL_TRUSTSTORE_FILE);
    _sslTruststorePassword = getStringOptionValue(BfConsts.ARG_SSL_TRUSTSTORE_PASSWORD);
    _synthesizeJsonTopology = getBooleanOptionValue(BfConsts.ARG_SYNTHESIZE_JSON_TOPOLOGY);
    _taskPlugin = getStringOptionValue(BfConsts.ARG_TASK_PLUGIN);
    _testrig = getStringOptionValue(BfConsts.ARG_TESTRIG);
    _throwOnLexerError = getBooleanOptionValue(ARG_THROW_ON_LEXER_ERROR);
    _throwOnParserError = getBooleanOptionValue(ARG_THROW_ON_PARSER_ERROR);
    _timestamp = getBooleanOptionValue(ARG_TIMESTAMP);
    _tracingAgentHost = getStringOptionValue(ARG_TRACING_AGENT_HOST);
    _tracingAgentPort = getIntegerOptionValue(ARG_TRACING_AGENT_PORT);
    _tracingEnable = getBooleanOptionValue(ARG_TRACING_ENABLE);
    _unimplementedAsError = getBooleanOptionValue(BfConsts.ARG_UNIMPLEMENTED_AS_ERROR);
    _unimplementedRecord = !getBooleanOptionValue(BfConsts.ARG_UNIMPLEMENTED_SUPPRESS);
    _unrecognizedAsRedFlag = getBooleanOptionValue(BfConsts.ARG_UNRECOGNIZED_AS_RED_FLAG);
    _validateEnvironment = getBooleanOptionValue(BfConsts.COMMAND_VALIDATE_ENVIRONMENT);
    _verboseParse = getBooleanOptionValue(BfConsts.ARG_VERBOSE_PARSE);
  }

  public boolean prettyPrintAnswer() {
    return _prettyPrintAnswer;
  }

  public boolean runInServiceMode() {
    return _runInServiceMode;
  }

  public void setActiveTestrigSettings(TestrigSettings activeTestrigSettings) {
    _activeTestrigSettings = activeTestrigSettings;
  }

  public void setBdpDetail(boolean bdpDetail) {
    _bdpDetail = bdpDetail;
  }

  public void setBdpMaxOscillationRecoveryAttempts(int bdpMaxOscillationRecoveryAttempts) {
    _bdpMaxOscillationRecoveryAttempts = bdpMaxOscillationRecoveryAttempts;
  }

  public void setBdpMaxRecordedIterations(int bdpMaxRecordedIterations) {
    _bdpMaxRecordedIterations = bdpMaxRecordedIterations;
  }

  public void setBdpPrintAllIterations(boolean bdpPrintAllIterations) {
    _bdpPrintAllIterations = bdpPrintAllIterations;
  }

  public void setBdpPrintOscillatingIterations(boolean bdpPrintOscillatingIterations) {
    _bdpPrintOscillatingIterations = bdpPrintOscillatingIterations;
  }

  public void setBdpRecordAllIterations(boolean bdpRecordAllIterations) {
    _bdpRecordAllIterations = bdpRecordAllIterations;
  }

  public void setContainerDir(Path containerDir) {
    _containerDir = containerDir;
  }

  public void setDeltaEnvironmentName(String diffEnvironmentName) {
    _deltaEnvironmentName = diffEnvironmentName;
  }

  public void setDeltaTestrig(String deltaTestrig) {
    _deltaTestrig = deltaTestrig;
  }

  public void setDiffActive(boolean diffActive) {
    _diffActive = diffActive;
  }

  public void setDiffQuestion(boolean diffQuestion) {
    _diffQuestion = diffQuestion;
  }

  @Override
  public void setDisableUnrecognized(boolean b) {
    _disableUnrecognized = b;
  }

  public void setEnvironmentName(String envName) {
    _environmentName = envName;
  }

  public void setHaltOnConvertError(boolean haltOnConvertError) {
    _haltOnConvertError = haltOnConvertError;
  }

  public void setHaltOnParseError(boolean haltOnParseError) {
    _haltOnParseError = haltOnParseError;
  }

  public void setInitInfo(boolean initInfo) {
    _initInfo = initInfo;
  }

  public void setLogger(BatfishLogger logger) {
    _logger = logger;
  }

  public void setMaxParserContextLines(int maxParserContextLines) {
    _maxParserContextLines = maxParserContextLines;
  }

  public void setMaxParserContextTokens(int maxParserContextTokens) {
    _maxParserContextTokens = maxParserContextTokens;
  }

  public void setMaxRuntimeMs(int runtimeMs) {
    _maxRuntimeMs = runtimeMs;
  }

  @Override
  public void setPrintParseTree(boolean printParseTree) {
    _printParseTree = printParseTree;
  }

  public void setQuestionPath(@Nullable Path questionPath) {
    _questionPath = questionPath;
  }

  public void setReport(boolean report) {
    _report = report;
  }

  public void setSequential(boolean sequential) {
    _sequential = true;
  }

  public void setSslDisable(boolean sslDisable) {
    _sslDisable = sslDisable;
  }

  public void setSslKeystoreFile(Path sslKeystoreFile) {
    _sslKeystoreFile = sslKeystoreFile;
  }

  public void setSslKeystorePassword(String sslKeystorePassword) {
    _sslKeystorePassword = sslKeystorePassword;
  }

  public void setSslTrustAllCerts(boolean sslTrustAllCerts) {
    _sslTrustAllCerts = sslTrustAllCerts;
  }

  public void setSslTruststoreFile(Path sslTruststoreFile) {
    _sslTruststoreFile = sslTruststoreFile;
  }

  public void setSslTruststorePassword(String sslTruststorePassword) {
    _sslTruststorePassword = sslTruststorePassword;
  }

  public void setTaskId(String taskId) {
    _taskId = taskId;
  }

  public void setTestrig(String testrig) {
    _testrig = testrig;
  }

  @Override
  public void setThrowOnLexerError(boolean throwOnLexerError) {
    _throwOnLexerError = throwOnLexerError;
  }

  @Override
  public void setThrowOnParserError(boolean throwOnParserError) {
    _throwOnParserError = throwOnParserError;
  }

  public void setUnrecognizedAsRedFlag(boolean unrecognizedAsRedFlag) {
    _unrecognizedAsRedFlag = unrecognizedAsRedFlag;
  }

  public void setValidateEnvironment(boolean validateEnvironment) {
    _validateEnvironment = validateEnvironment;
  }

  public void setVerboseParse(boolean verboseParse) {
    _verboseParse = verboseParse;
  }
}
