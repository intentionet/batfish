package org.batfish.client.config;

import java.nio.file.Path;
import org.batfish.common.BaseSettings;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.CoordConsts;
import org.batfish.common.util.CommonUtil;

public class Settings extends BaseSettings {

  public static final String ARG_BATFISH_LOG_LEVEL = "batfishloglevel";
  public static final String ARG_COMMAND_FILE = "cmdfile";
  public static final String ARG_CONTAINER_ID = "containerid";
  public static final String ARG_COORDINATOR_HOST = "coordinatorhost";
  public static final String ARG_DATAMODEL_DIR = "datamodeldir";
  public static final String ARG_LOG_FILE = "logfile";
  public static final String ARG_LOG_LEVEL = "loglevel";
  public static final String ARG_QUESTIONS_DIR = "questionsdir";
  public static final String ARG_RUN_MODE = "runmode";
  public static final String ARG_TESTRIG_DIR = "testrigdir";
  public static final String ARG_TESTRIG_ID = "testrigid";
  public static final String ARG_TRACING_ENABLE = "tracingenable";
  private static final String ARG_API_KEY = "apikey";
  private static final String ARG_HELP = "help";
  private static final String ARG_NO_SANITY_CHECK = "nosanitycheck";
  private static final String ARG_PERIOD_CHECK_WORK = "periodcheckworkms";
  private static final String ARG_PRETTY_PRINT_ANSWERS = "prettyanswers";
  private static final String ARG_SERVICE_POOL_PORT = "coordinatorpoolport";
  private static final String ARG_SERVICE_WORK_PORT = "coordinatorworkport";
  private static final String ARG_TRACING_AGENT_HOST = "tracingagenthost";
  private static final String ARG_TRACING_AGENT_PORT = "tracingagentport";
  private static final String EXECUTABLE_NAME = "batfish_client";
  private String _apiKey;
  private String _batchCommandFile;
  private String _batfishLogLevel;
  private String _containerId;
  private String _coordinatorHost;
  private int _coordinatorPoolPort;
  private int _coordinatorWorkPort;
  private String _datamodelDir;
  private String _logFile;
  private String _logLevel;
  private long _periodCheckWorkMs;
  private boolean _prettyPrintAnswers;
  private String _questionsDir;
  private RunMode _runMode;
  private boolean _sanityCheck;
  private boolean _sslDisable;
  private Path _sslKeystoreFile;
  private String _sslKeystorePassword;
  private boolean _sslTrustAllCerts;
  private Path _sslTruststoreFile;
  private String _sslTruststorePassword;
  private String _testrigDir;
  private String _testrigId;
  private String _tracingAgentHost;
  private Integer _tracingAgentPort;
  private boolean _tracingEnable;

  public Settings(String[] args) throws Exception {
    super(
        CommonUtil.getConfig(
            BfConsts.PROP_CLIENT_PROPERTIES_PATH,
            BfConsts.ABSPATH_CONFIG_FILE_NAME_CLIENT,
            ConfigurationLocator.class));

    initConfigDefaults();

    initOptions();
    parseCommandLine(args);
  }

  public String getApiKey() {
    return _apiKey;
  }

  public String getBatchCommandFile() {
    return _batchCommandFile;
  }

  public String getBatfishLogLevel() {
    return _batfishLogLevel;
  }

  public void setBatfishLogLevel(String logLevel) {
    _batfishLogLevel = logLevel;
  }

  public String getContainerId() {
    return _containerId;
  }

  public String getCoordinatorHost() {
    return _coordinatorHost;
  }

  public int getCoordinatorPoolPort() {
    return _coordinatorPoolPort;
  }

  public int getCoordinatorWorkPort() {
    return _coordinatorWorkPort;
  }

  public String getDatamodelDir() {
    return _datamodelDir;
  }

  public String getLogFile() {
    return _logFile;
  }

  public String getLogLevel() {
    return _logLevel;
  }

  public void setLogLevel(String logLevel) {
    _logLevel = logLevel;
  }

  public long getPeriodCheckWorkMs() {
    return _periodCheckWorkMs;
  }

  public boolean getPrettyPrintAnswers() {
    return _prettyPrintAnswers;
  }

  public void setPrettyPrintAnswers(boolean prettyPrint) {
    _prettyPrintAnswers = prettyPrint;
  }

  public String getQuestionsDir() {
    return _questionsDir;
  }

  public RunMode getRunMode() {
    return _runMode;
  }

  public boolean getSanityCheck() {
    return _sanityCheck;
  }

  public boolean getSslDisable() {
    return _sslDisable;
  }

  public void setSslDisable(boolean sslDisable) {
    _sslDisable = sslDisable;
  }

  public Path getSslKeystoreFile() {
    return _sslKeystoreFile;
  }

  public void setSslKeystoreFile(Path sslKeystoreFile) {
    _sslKeystoreFile = sslKeystoreFile;
  }

  public String getSslKeystorePassword() {
    return _sslKeystorePassword;
  }

  public void setSslKeystorePassword(String sslKeystorePassword) {
    _sslKeystorePassword = sslKeystorePassword;
  }

  public boolean getSslTrustAllCerts() {
    return _sslTrustAllCerts;
  }

  public void setSslTrustAllCerts(boolean sslTrustAllCerts) {
    _sslTrustAllCerts = sslTrustAllCerts;
  }

  public Path getSslTruststoreFile() {
    return _sslTruststoreFile;
  }

  public void setSslTruststoreFile(Path sslTruststoreFile) {
    _sslTruststoreFile = sslTruststoreFile;
  }

  public String getSslTruststorePassword() {
    return _sslTruststorePassword;
  }

  public void setSslTruststorePassword(String sslTruststorePassword) {
    _sslTruststorePassword = sslTruststorePassword;
  }

  public String getTestrigDir() {
    return _testrigDir;
  }

  public String getTestrigId() {
    return _testrigId;
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

  private void initConfigDefaults() {
    setDefaultProperty(ARG_API_KEY, CoordConsts.DEFAULT_API_KEY);
    setDefaultProperty(
        ARG_BATFISH_LOG_LEVEL, BatfishLogger.getLogLevelStr(BatfishLogger.LEVEL_WARN));
    setDefaultProperty(ARG_COORDINATOR_HOST, "localhost");
    setDefaultProperty(ARG_DATAMODEL_DIR, "datamodel");
    setDefaultProperty(BfConsts.ARG_SSL_DISABLE, CoordConsts.SVC_CFG_WORK_SSL_DISABLE);
    setDefaultProperty(BfConsts.ARG_SSL_TRUST_ALL_CERTS, false);
    setDefaultProperty(ARG_HELP, false);
    setDefaultProperty(ARG_LOG_FILE, null);
    setDefaultProperty(ARG_LOG_LEVEL, BatfishLogger.getLogLevelStr(BatfishLogger.LEVEL_OUTPUT));
    setDefaultProperty(ARG_NO_SANITY_CHECK, false);
    setDefaultProperty(ARG_PERIOD_CHECK_WORK, 1000);
    setDefaultProperty(ARG_PRETTY_PRINT_ANSWERS, true);
    setDefaultProperty(ARG_RUN_MODE, RunMode.batch.toString());
    setDefaultProperty(ARG_SERVICE_POOL_PORT, CoordConsts.SVC_CFG_POOL_PORT);
    setDefaultProperty(ARG_SERVICE_WORK_PORT, CoordConsts.SVC_CFG_WORK_PORT);
    setDefaultProperty(BfConsts.ARG_SSL_DISABLE, CoordConsts.SVC_CFG_WORK_SSL_DISABLE);
    setDefaultProperty(BfConsts.ARG_SSL_KEYSTORE_FILE, null);
    setDefaultProperty(BfConsts.ARG_SSL_KEYSTORE_PASSWORD, null);
    setDefaultProperty(BfConsts.ARG_SSL_TRUST_ALL_CERTS, false);
    setDefaultProperty(BfConsts.ARG_SSL_TRUSTSTORE_FILE, null);
    setDefaultProperty(BfConsts.ARG_SSL_TRUSTSTORE_PASSWORD, null);
    setDefaultProperty(ARG_TRACING_AGENT_HOST, "localhost");
    setDefaultProperty(ARG_TRACING_AGENT_PORT, 5775);
    setDefaultProperty(ARG_TRACING_ENABLE, false);
  }

  private void initOptions() {
    addOption(ARG_API_KEY, "API key for the coordinator", "apikey");

    addOption(ARG_COMMAND_FILE, "read commands from the specified command file", "cmdfile");

    addOption(ARG_COORDINATOR_HOST, "hostname for the service", "base url for coordinator service");

    addOption(ARG_BATFISH_LOG_LEVEL, "log level for batfish", "batfish_loglevel");

    addOption(ARG_CONTAINER_ID, "container to attach to", "container_id");

    addOption(ARG_DATAMODEL_DIR, "directory where datamodel should be dumped", "datamodel_dir");

    addBooleanOption(ARG_HELP, "print this message");

    addOption(ARG_LOG_FILE, "send output to specified log file", "logfile");

    addOption(ARG_LOG_LEVEL, "log level", "loglevel");

    addBooleanOption(
        ARG_NO_SANITY_CHECK, "do not check if container, testrig etc. are set. (helps debugging.)");

    addOption(
        ARG_PERIOD_CHECK_WORK, "period with which to check work (ms)", "period_check_work_ms");

    addBooleanOption(ARG_PRETTY_PRINT_ANSWERS, "pretty print answers");

    addOption(ARG_QUESTIONS_DIR, "directory to output questions in", "questions_dir");

    addOption(ARG_RUN_MODE, "which mode to run in (batch|interactive|genquestions)", "run_mode");

    addOption(
        ARG_SERVICE_POOL_PORT, "port for pool management service", "port_number_pool_service");

    addOption(
        ARG_SERVICE_WORK_PORT, "port for work management service", "port_number_work_service");

    addBooleanOption(
        BfConsts.ARG_SSL_DISABLE, "whether to disable SSL during communication with coordinator");

    addBooleanOption(
        BfConsts.ARG_SSL_TRUST_ALL_CERTS,
        "whether to trust all SSL certificates during communication with coordinator");

    addOption(ARG_TESTRIG_DIR, "where the testrig sits", "testrig_dir");

    addOption(ARG_TESTRIG_ID, "testrig to attach to", "testrig_id");

    addOption(ARG_TRACING_AGENT_HOST, "jaeger agent host", "jaeger_agent_host");

    addOption(ARG_TRACING_AGENT_PORT, "jaeger agent port", "jaeger_agent_port");

    addBooleanOption(ARG_TRACING_ENABLE, "enable tracing");
  }

  private void parseCommandLine(String[] args) {
    initCommandLine(args);

    if (getBooleanOptionValue(ARG_HELP)) {
      printHelp(EXECUTABLE_NAME);
      System.exit(0);
    }

    _apiKey = getStringOptionValue(ARG_API_KEY);
    _batchCommandFile = getStringOptionValue(ARG_COMMAND_FILE);
    _batfishLogLevel = getStringOptionValue(ARG_BATFISH_LOG_LEVEL);
    _containerId = getStringOptionValue(ARG_CONTAINER_ID);
    _coordinatorHost = getStringOptionValue(ARG_COORDINATOR_HOST);
    _coordinatorPoolPort = getIntegerOptionValue(ARG_SERVICE_POOL_PORT);
    _coordinatorWorkPort = getIntegerOptionValue(ARG_SERVICE_WORK_PORT);
    _datamodelDir = getStringOptionValue(ARG_DATAMODEL_DIR);
    _logFile = getStringOptionValue(ARG_LOG_FILE);
    _logLevel = getStringOptionValue(ARG_LOG_LEVEL);
    _periodCheckWorkMs = getLongOptionValue(ARG_PERIOD_CHECK_WORK);
    _prettyPrintAnswers = getBooleanOptionValue(ARG_PRETTY_PRINT_ANSWERS);
    _questionsDir = getStringOptionValue(ARG_QUESTIONS_DIR);
    _runMode = RunMode.valueOf(getStringOptionValue(ARG_RUN_MODE));
    _sanityCheck = !getBooleanOptionValue(ARG_NO_SANITY_CHECK);
    _sslDisable = getBooleanOptionValue(BfConsts.ARG_SSL_DISABLE);
    _sslKeystoreFile = getPathOptionValue(BfConsts.ARG_SSL_KEYSTORE_FILE);
    _sslKeystorePassword = getStringOptionValue(BfConsts.ARG_SSL_KEYSTORE_PASSWORD);
    _sslTrustAllCerts = getBooleanOptionValue(BfConsts.ARG_SSL_TRUST_ALL_CERTS);
    _sslTruststoreFile = getPathOptionValue(BfConsts.ARG_SSL_TRUSTSTORE_FILE);
    _sslTruststorePassword = getStringOptionValue(BfConsts.ARG_SSL_TRUSTSTORE_PASSWORD);

    _testrigDir = getStringOptionValue(ARG_TESTRIG_DIR);
    _testrigId = getStringOptionValue(ARG_TESTRIG_ID);
    _tracingAgentHost = getStringOptionValue(ARG_TRACING_AGENT_HOST);
    _tracingAgentPort = getIntegerOptionValue(ARG_TRACING_AGENT_PORT);
    _tracingEnable = getBooleanOptionValue(ARG_TRACING_ENABLE);
  }

  public enum RunMode {
    batch,
    gendatamodel,
    genquestions,
    interactive
  }
}
