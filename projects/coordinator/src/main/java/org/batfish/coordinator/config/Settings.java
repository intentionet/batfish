package org.batfish.coordinator.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.batfish.common.BaseSettings;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.CoordConsts;
import org.batfish.common.util.CommonUtil;
import org.batfish.coordinator.authorizer.Authorizer;
import org.batfish.coordinator.queues.WorkQueue;
import org.batfish.datamodel.Ip;

public class Settings extends BaseSettings {

  private static final String ARG_ALLOW_DEFAULT_KEY_LISTINGS = "allowdefaultkeylistings";
  private static final String ARG_AUTHORIZER_TYPE = "authorizertype";
  private static final String ARG_CONTAINERS_LOCATION = "containerslocation";
  private static final String ARG_DB_AUTHORIZER_CACHE_EXPIRY_MS = "dbcacheexpiry";
  private static final String ARG_DB_AUTHORIZER_CONN_STRING = "dbconnection";
  private static final String ARG_DRIVER_CLASS = "driverclass";
  /** (the arguments below are not wired to command line) */
  private static final String ARG_FILE_AUTHORIZER_PERMS_FILE = "fileauthpermsfile";

  private static final String ARG_FILE_AUTHORIZER_ROOT_DIR = "fileauthrootdir";
  private static final String ARG_FILE_AUTHORIZER_USERS_FILE = "fileauthusersfile";
  private static final String ARG_HELP = "help";
  private static final String ARG_LOG_FILE = "logfile";
  private static final String ARG_LOG_LEVEL = "loglevel";
  private static final String ARG_PERIOD_ASSIGN_WORK_MS = "periodassignworkms";
  private static final String ARG_PERIOD_CHECK_WORK_MS = "periodcheckworkms";
  private static final String ARG_PERIOD_WORKER_STATUS_REFRESH_MS = "periodworkerrefreshms";
  private static final String ARG_POOL_BIND_HOST = "poolbindhost";
  private static final String ARG_QUESTION_TEMPLATE_DIRS = "templatedirs";
  private static final String ARG_QUEUE_COMPLETED_WORK = "qcompletedwork";
  private static final String ARG_QUEUE_INCOMPLETE_WORK = "qincompletework";
  private static final String ARG_QUEUE_TYPE = "qtype";

  private static final String ARG_SERVICE_POOL_PORT = "poolport";
  private static final String ARG_SERVICE_WORK_PORT = "workport";
  private static final String ARG_SERVICE_WORK_V2_PORT = "workv2port";
  private static final String ARG_SSL_POOL_DISABLE = "sslpooldisable";

  private static final String ARG_SSL_POOL_KEYSTORE_FILE = "sslpoolkeystorefile";
  private static final String ARG_SSL_POOL_KEYSTORE_PASSWORD = "sslpoolkeystorepassword";
  private static final String ARG_SSL_POOL_TRUST_ALL_CERTS = "sslpooltrustallcerts";
  private static final String ARG_SSL_POOL_TRUSTSTORE_FILE = "sslpooltruststorefile";
  private static final String ARG_SSL_POOL_TRUSTSTORE_PASSWORD = "sslpooltruststorepassword";
  private static final String ARG_SSL_WORK_DISABLE = "sslworkdisable";
  private static final String ARG_SSL_WORK_KEYSTORE_FILE = "sslworkkeystorefile";

  private static final String ARG_SSL_WORK_KEYSTORE_PASSWORD = "sslworkkeystorepassword";
  private static final String ARG_SSL_WORK_TRUST_ALL_CERTS = "sslworktrustallcerts";
  private static final String ARG_SSL_WORK_TRUSTSTORE_FILE = "sslworktruststorefile";
  private static final String ARG_SSL_WORK_TRUSTSTORE_PASSWORD = "sslworktruststorepassword";
  /** Need when using Azure queues for storing work items */
  private static final String ARG_STORAGE_ACCOUNT_KEY = "storageaccountkey";

  private static final String ARG_STORAGE_ACCOUNT_NAME = "storageaccountname";
  private static final String ARG_STORAGE_PROTOCOL = "storageprotocol";

  private static final String ARG_TRACING_AGENT_HOST = "tracingagenthost";
  private static final String ARG_TRACING_AGENT_PORT = "tracingagentport";
  public static final String ARG_TRACING_ENABLE = "tracingenable";

  private static final String ARG_WORK_BIND_HOST = "workbindhost";

  private static final String ARGNAME_PATHS = "path..";

  private static final String EXECUTABLE_NAME = "coordinator";

  private Authorizer.Type _authorizerType;
  private Path _containersLocation;
  private String _dbAuthorizerConnString;
  private long _dbCacheExpiryMs;
  private boolean _defaultKeyListings;
  private String _driverClass;
  private Path _fileAuthorizerPermsFile;
  private Path _fileAuthorizerRootDir;
  private Path _fileAuthorizerUsersFile;
  private String _logFile;
  private String _logLevel;
  private long _periodAssignWorkMs;
  private long _periodCheckWorkMs;
  private long _periodWorkerStatusRefreshMs;
  private String _poolBindHost;
  private List<Path> _questionTemplateDirs;
  private String _queueCompletedWork;
  private WorkQueue.Type _queueType;
  private String _queuIncompleteWork;
  private int _servicePoolPort;
  private int _serviceWorkPort;
  private int _serviceWorkV2Port;
  private boolean _sslPoolDisable;
  private Path _sslPoolKeystoreFile;
  private String _sslPoolKeystorePassword;
  private boolean _sslPoolTrustAllCerts;
  private Path _sslPoolTruststoreFile;
  private String _sslPoolTruststorePassword;
  private boolean _sslWorkDisable;
  private Path _sslWorkKeystoreFile;
  private String _sslWorkKeystorePassword;
  private boolean _sslWorkTrustAllCerts;
  private Path _sslWorkTruststoreFile;
  private String _sslWorkTruststorePassword;
  private String _storageAccountKey;
  private String _storageAccountName;
  private String _storageProtocol;
  private String _tracingAgentHost;
  private Integer _tracingAgentPort;
  private boolean _tracingEnable;
  private String _workBindHost;

  public Settings(String[] args) throws Exception {
    super(
        CommonUtil.getConfig(
            BfConsts.PROP_COORDINATOR_PROPERTIES_PATH,
            BfConsts.ABSPATH_CONFIG_FILE_NAME_COORDINATOR,
            Settings.class));

    initConfigDefaults();

    initOptions();
    parseCommandLine(args);
  }

  public Authorizer.Type getAuthorizationType() {
    return _authorizerType;
  }

  public Path getContainersLocation() {
    return _containersLocation;
  }

  public long getDbAuthorizerCacheExpiryMs() {
    return _dbCacheExpiryMs;
  }

  public String getDbAuthorizerConnString() {
    return _dbAuthorizerConnString;
  }

  public boolean getDefaultKeyListings() {
    return _defaultKeyListings;
  }

  public String getDriverClass() {
    return _driverClass;
  }

  public Path getFileAuthorizerPermsFile() {
    return _fileAuthorizerPermsFile;
  }

  public Path getFileAuthorizerRootDir() {
    return _fileAuthorizerRootDir;
  }

  public Path getFileAuthorizerUsersFile() {
    return _fileAuthorizerUsersFile;
  }

  public String getLogFile() {
    return _logFile;
  }

  public String getLogLevel() {
    return _logLevel;
  }

  public long getPeriodAssignWorkMs() {
    return _periodAssignWorkMs;
  }

  public long getPeriodCheckWorkMs() {
    return _periodCheckWorkMs;
  }

  public long getPeriodWorkerStatusRefreshMs() {
    return _periodWorkerStatusRefreshMs;
  }

  public String getPoolBindHost() {
    return _poolBindHost;
  }

  public String getQueueCompletedWork() {
    return _queueCompletedWork;
  }

  public String getQueueIncompleteWork() {
    return _queuIncompleteWork;
  }

  public WorkQueue.Type getQueueType() {
    return _queueType;
  }

  public int getServicePoolPort() {
    return _servicePoolPort;
  }

  public int getServiceWorkPort() {
    return _serviceWorkPort;
  }

  public int getServiceWorkV2Port() {
    return _serviceWorkV2Port;
  }

  public boolean getSslPoolDisable() {
    return _sslPoolDisable;
  }

  public Path getSslPoolKeystoreFile() {
    return _sslPoolKeystoreFile;
  }

  public String getSslPoolKeystorePassword() {
    return _sslPoolKeystorePassword;
  }

  public boolean getSslPoolTrustAllCerts() {
    return _sslPoolTrustAllCerts;
  }

  public Path getSslPoolTruststoreFile() {
    return _sslPoolTruststoreFile;
  }

  public String getSslPoolTruststorePassword() {
    return _sslPoolTruststorePassword;
  }

  public List<Path> getQuestionTemplateDirs() {
    return _questionTemplateDirs;
  }

  public boolean getSslWorkDisable() {
    return _sslWorkDisable;
  }

  public Path getSslWorkKeystoreFile() {
    return _sslWorkKeystoreFile;
  }

  public String getSslWorkKeystorePassword() {
    return _sslWorkKeystorePassword;
  }

  public boolean getSslWorkTrustAllCerts() {
    return _sslWorkTrustAllCerts;
  }

  public Path getSslWorkTruststoreFile() {
    return _sslWorkTruststoreFile;
  }

  public String getSslWorkTruststorePassword() {
    return _sslWorkTruststorePassword;
  }

  public String getStorageAccountKey() {
    return _storageAccountKey;
  }

  public String getStorageAccountName() {
    return _storageAccountName;
  }

  public String getStorageProtocol() {
    return _storageProtocol;
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

  public String getWorkBindHost() {
    return _workBindHost;
  }

  private void initConfigDefaults() {
    setDefaultProperty(ARG_AUTHORIZER_TYPE, Authorizer.Type.none.toString());
    setDefaultProperty(ARG_ALLOW_DEFAULT_KEY_LISTINGS, false);
    setDefaultProperty(ARG_CONTAINERS_LOCATION, "containers");
    setDefaultProperty(
        ARG_DB_AUTHORIZER_CONN_STRING,
        "jdbc:mysql://localhost/batfish?user=batfish&password=batfish");
    setDefaultProperty(ARG_DB_AUTHORIZER_CACHE_EXPIRY_MS, 15 * 60 * 1000); // 15
    // minutes
    setDefaultProperty(ARG_DRIVER_CLASS, null);
    setDefaultProperty(ARG_FILE_AUTHORIZER_PERMS_FILE, "perms.json");
    setDefaultProperty(ARG_FILE_AUTHORIZER_ROOT_DIR, "fileauthorizer");
    setDefaultProperty(ARG_FILE_AUTHORIZER_USERS_FILE, "users.json");
    setDefaultProperty(ARG_HELP, false);
    setDefaultProperty(ARG_LOG_FILE, null);
    setDefaultProperty(ARG_LOG_LEVEL, BatfishLogger.getLogLevelStr(BatfishLogger.LEVEL_OUTPUT));
    setDefaultProperty(ARG_PERIOD_ASSIGN_WORK_MS, 1000);
    setDefaultProperty(ARG_PERIOD_CHECK_WORK_MS, 1000);
    setDefaultProperty(ARG_PERIOD_WORKER_STATUS_REFRESH_MS, 10000);
    setDefaultProperty(ARG_QUESTION_TEMPLATE_DIRS, Collections.<String>emptyList());
    setDefaultProperty(ARG_QUEUE_COMPLETED_WORK, "batfishcompletedwork");
    setDefaultProperty(ARG_QUEUE_INCOMPLETE_WORK, "batfishincompletework");
    setDefaultProperty(ARG_QUEUE_TYPE, WorkQueue.Type.memory.toString());
    setDefaultProperty(ARG_POOL_BIND_HOST, Ip.ZERO.toString());
    setDefaultProperty(ARG_SERVICE_POOL_PORT, CoordConsts.SVC_CFG_POOL_PORT);
    setDefaultProperty(ARG_WORK_BIND_HOST, Ip.ZERO.toString());
    setDefaultProperty(ARG_SERVICE_WORK_PORT, CoordConsts.SVC_CFG_WORK_PORT);
    setDefaultProperty(ARG_SERVICE_WORK_V2_PORT, CoordConsts.SVC_CFG_WORK_V2_PORT);
    setDefaultProperty(ARG_SSL_POOL_DISABLE, CoordConsts.SVC_CFG_POOL_SSL_DISABLE);
    setDefaultProperty(ARG_SSL_POOL_KEYSTORE_FILE, null);
    setDefaultProperty(ARG_SSL_POOL_KEYSTORE_PASSWORD, null);
    setDefaultProperty(ARG_SSL_POOL_TRUST_ALL_CERTS, false);
    setDefaultProperty(ARG_SSL_POOL_TRUSTSTORE_FILE, null);
    setDefaultProperty(ARG_SSL_POOL_TRUSTSTORE_PASSWORD, null);
    setDefaultProperty(ARG_SSL_WORK_DISABLE, CoordConsts.SVC_CFG_WORK_SSL_DISABLE);
    setDefaultProperty(ARG_SSL_WORK_KEYSTORE_FILE, null);
    setDefaultProperty(ARG_SSL_WORK_KEYSTORE_PASSWORD, null);
    setDefaultProperty(ARG_SSL_WORK_TRUST_ALL_CERTS, true);
    setDefaultProperty(ARG_SSL_WORK_TRUSTSTORE_FILE, null);
    setDefaultProperty(ARG_SSL_WORK_TRUSTSTORE_PASSWORD, null);
    setDefaultProperty(ARG_TRACING_AGENT_HOST, "localhost");
    setDefaultProperty(ARG_TRACING_AGENT_PORT, 5775);
    setDefaultProperty(ARG_TRACING_ENABLE, false);
    // setDefaultProperty(ARG_SSL_KEYSTORE_FILE, "selfsigned.jks");
    // setDefaultProperty(ARG_SSL_KEYSTORE_PASSWORD, "batfish");
    setDefaultProperty(
        ARG_STORAGE_ACCOUNT_KEY,
        "zRTT++dVryOWXJyAM7NM0TuQcu0Y23BgCQfkt7xh2f/Mm+r6c8/XtPTY0xxaF6tPSACJiuACsjotDeNIVyXM8Q==");
    setDefaultProperty(ARG_STORAGE_ACCOUNT_NAME, "testdrive");
    setDefaultProperty(ARG_STORAGE_PROTOCOL, "http");
  }

  private void initOptions() {
    addOption(ARG_AUTHORIZER_TYPE, "type of authorizer to use", "authorizer type");

    addBooleanOption(
        ARG_ALLOW_DEFAULT_KEY_LISTINGS, "allow default API key to list containers and testrigs");

    addOption(ARG_CONTAINERS_LOCATION, "where to store containers", "containers_location");

    addOption(
        ARG_DB_AUTHORIZER_CONN_STRING, "connection string for authorizer db", "connection string");

    addOption(
        ARG_DB_AUTHORIZER_CACHE_EXPIRY_MS,
        "when to expire information from authorizer database",
        "expiration time (ms)");

    addBooleanOption(ARG_DRIVER_CLASS, "jdbc driver class to load explicitly");

    addBooleanOption(ARG_HELP, "print this message");

    addOption(ARG_LOG_FILE, "send output to specified log file", "logfile");

    addOption(ARG_LOG_LEVEL, "log level", "loglevel");

    addOption(
        ARG_PERIOD_WORKER_STATUS_REFRESH_MS,
        "period with which to check worker status (ms)",
        "period_worker_status_refresh_ms");

    addOption(
        ARG_PERIOD_ASSIGN_WORK_MS,
        "period with which to assign work (ms)",
        "period_assign_work_ms");

    addOption(
        ARG_PERIOD_CHECK_WORK_MS, "period with which to check work (ms)", "period_check_work_ms");

    addListOption(
        ARG_QUESTION_TEMPLATE_DIRS, "paths to question template directories", ARGNAME_PATHS);

    addOption(ARG_QUEUE_TYPE, "queue type to use {azure, memory}", "qtype");

    addOption(
        ARG_POOL_BIND_HOST,
        "hostname for pool management service",
        "base url for pool management service");

    addOption(
        ARG_SERVICE_POOL_PORT, "port for pool management service", "port_number_pool_service");

    addOption(
        ARG_WORK_BIND_HOST,
        "hostname for work management service",
        "base url for work management service");

    addOption(
        ARG_SERVICE_WORK_PORT, "port for work management service", "port_number_work_service");

    addBooleanOption(ARG_SSL_POOL_DISABLE, "disable SSL on pool manager service");

    addBooleanOption(
        ARG_SSL_POOL_TRUST_ALL_CERTS,
        "trust all SSL certs for outgoing connections from pool manager");

    addBooleanOption(ARG_SSL_WORK_DISABLE, "disable SSL on work manager service");

    addBooleanOption(
        ARG_SSL_WORK_TRUST_ALL_CERTS,
        "trust all SSL certs for outgoing connections from work manager");

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

    _authorizerType = Authorizer.Type.valueOf(getStringOptionValue(ARG_AUTHORIZER_TYPE));
    _dbAuthorizerConnString = getStringOptionValue(ARG_DB_AUTHORIZER_CONN_STRING);
    _dbCacheExpiryMs = getLongOptionValue(ARG_DB_AUTHORIZER_CACHE_EXPIRY_MS);
    _defaultKeyListings = getBooleanOptionValue(ARG_ALLOW_DEFAULT_KEY_LISTINGS);
    _driverClass = getStringOptionValue(ARG_DRIVER_CLASS);
    _fileAuthorizerRootDir = Paths.get(getStringOptionValue(ARG_FILE_AUTHORIZER_ROOT_DIR));
    _fileAuthorizerPermsFile = Paths.get(getStringOptionValue(ARG_FILE_AUTHORIZER_PERMS_FILE));
    _fileAuthorizerUsersFile = Paths.get(getStringOptionValue(ARG_FILE_AUTHORIZER_USERS_FILE));
    _questionTemplateDirs = getPathListOptionValue(ARG_QUESTION_TEMPLATE_DIRS);
    _queuIncompleteWork = getStringOptionValue(ARG_QUEUE_INCOMPLETE_WORK);
    _queueCompletedWork = getStringOptionValue(ARG_QUEUE_COMPLETED_WORK);
    _queueType = WorkQueue.Type.valueOf(getStringOptionValue(ARG_QUEUE_TYPE));
    _poolBindHost = getStringOptionValue(ARG_POOL_BIND_HOST);
    _servicePoolPort = getIntegerOptionValue(ARG_SERVICE_POOL_PORT);
    _workBindHost = getStringOptionValue(ARG_WORK_BIND_HOST);
    _serviceWorkPort = getIntegerOptionValue(ARG_SERVICE_WORK_PORT);
    _serviceWorkV2Port = getIntegerOptionValue(ARG_SERVICE_WORK_V2_PORT);
    _sslPoolDisable = getBooleanOptionValue(ARG_SSL_POOL_DISABLE);
    _sslPoolKeystoreFile = getPathOptionValue(ARG_SSL_POOL_KEYSTORE_FILE);
    _sslPoolKeystorePassword = getStringOptionValue(ARG_SSL_POOL_KEYSTORE_PASSWORD);
    _sslPoolTrustAllCerts = getBooleanOptionValue(ARG_SSL_POOL_TRUST_ALL_CERTS);
    _sslPoolTruststoreFile = getPathOptionValue(ARG_SSL_POOL_TRUSTSTORE_FILE);
    _sslPoolTruststorePassword = getStringOptionValue(ARG_SSL_POOL_TRUSTSTORE_PASSWORD);
    _sslWorkDisable = getBooleanOptionValue(ARG_SSL_WORK_DISABLE);
    _sslWorkKeystoreFile = getPathOptionValue(ARG_SSL_WORK_KEYSTORE_FILE);
    _sslWorkKeystorePassword = getStringOptionValue(ARG_SSL_WORK_KEYSTORE_PASSWORD);
    _sslWorkTrustAllCerts = getBooleanOptionValue(ARG_SSL_WORK_TRUST_ALL_CERTS);
    _sslWorkTruststoreFile = getPathOptionValue(ARG_SSL_WORK_TRUSTSTORE_FILE);
    _sslWorkTruststorePassword = getStringOptionValue(ARG_SSL_WORK_TRUSTSTORE_PASSWORD);
    _storageAccountKey = getStringOptionValue(ARG_STORAGE_ACCOUNT_KEY);
    _storageAccountName = getStringOptionValue(ARG_STORAGE_ACCOUNT_NAME);
    _storageProtocol = getStringOptionValue(ARG_STORAGE_PROTOCOL);
    _tracingAgentHost = getStringOptionValue(ARG_TRACING_AGENT_HOST);
    _tracingAgentPort = getIntegerOptionValue(ARG_TRACING_AGENT_PORT);
    _tracingEnable = getBooleanOptionValue(ARG_TRACING_ENABLE);
    _containersLocation = getPathOptionValue(ARG_CONTAINERS_LOCATION);
    _periodWorkerStatusRefreshMs = getLongOptionValue(ARG_PERIOD_WORKER_STATUS_REFRESH_MS);
    _periodAssignWorkMs = getLongOptionValue(ARG_PERIOD_ASSIGN_WORK_MS);
    _periodCheckWorkMs = getLongOptionValue(ARG_PERIOD_CHECK_WORK_MS);
    _logFile = getStringOptionValue(ARG_LOG_FILE);
    _logLevel = getStringOptionValue(ARG_LOG_LEVEL);
  }

  public void setSslPoolDisable(boolean sslPoolDisable) {
    _sslPoolDisable = sslPoolDisable;
  }

  public void setSslPoolKeystoreFile(Path sslPoolKeystoreFile) {
    _sslPoolKeystoreFile = sslPoolKeystoreFile;
  }

  public void setSslPoolKeystorePassword(String sslPoolKeystorePassword) {
    _sslPoolKeystorePassword = sslPoolKeystorePassword;
  }

  public void setSslPoolTrustAllCerts(boolean sslPoolTrustAllCerts) {
    _sslPoolTrustAllCerts = sslPoolTrustAllCerts;
  }

  public void setSslPoolTruststoreFile(Path sslPoolTruststoreFile) {
    _sslPoolTruststoreFile = sslPoolTruststoreFile;
  }

  public void setSslPoolTruststorePassword(String sslPoolTruststorePassword) {
    _sslPoolTruststorePassword = sslPoolTruststorePassword;
  }

  public void setSslWorkDisable(boolean sslWorkDisable) {
    _sslWorkDisable = sslWorkDisable;
  }

  public void setSslWorkKeystoreFile(Path sslWorkKeystoreFile) {
    _sslWorkKeystoreFile = sslWorkKeystoreFile;
  }

  public void setSslWorkKeystorePassword(String sslWorkKeystorePassword) {
    _sslWorkKeystorePassword = sslWorkKeystorePassword;
  }

  public void setSslWorkTrustAllCerts(boolean sslWorkTrustAllCerts) {
    _sslWorkTrustAllCerts = sslWorkTrustAllCerts;
  }

  public void setSslWorkTruststoreFile(Path sslWorkTruststoreFile) {
    _sslWorkTruststoreFile = sslWorkTruststoreFile;
  }

  public void setSslWorkTruststorePassword(String sslWorkTruststorePassword) {
    _sslWorkTruststorePassword = sslWorkTruststorePassword;
  }
}
