package org.batfish.common;

//
// IMPORTANT
// if you change the value of these constants,
// ensure that the clients (javascript, python, ... ) are up to date
//

public class BfConsts {

  public enum TaskStatus {
    InProgress,
    TerminatedAbnormally,
    TerminatedNormally,
    Unknown,
    UnreachableOrBadResponse,
    Unscheduled;
  }

  public static final String ABSPATH_CONFIG_FILE_NAME_ALLINONE =
      "/org/batfish/allinone/config/allinone.properties";
  public static final String ABSPATH_CONFIG_FILE_NAME_BATFISH =
      "/org/batfish/  config/batfish.properties";
  public static final String ABSPATH_CONFIG_FILE_NAME_CLIENT =
      "/org/batfish/client/config/client.properties";
  public static final String ABSPATH_CONFIG_FILE_NAME_COORDINATOR =
      "/org/batfish/coordinator/config/coordinator.properties";
  public static final String ABSPATH_DEFAULT_SALT = "org/batfish/common/util/salt";

  public static final String ARG_ANALYSIS_NAME = "analysisname";
  public static final String ARG_ANSWER_JSON_PATH = "answerjsonpath";
  public static final String ARG_BDP_DETAIL = "bdpdetail";
  public static final String ARG_BDP_MAX_OSCILLATION_RECOVERY_ATTEMPTS =
      "bdpmaxoscillationrecoveryattempts";
  public static final String ARG_BDP_MAX_RECORDED_ITERATIONS = "bdpmaxrecordediterations";
  public static final String ARG_BDP_PRINT_ALL_ITERATIONS = "bdpprintalliterations";
  public static final String ARG_BDP_PRINT_OSCILLATING_ITERATIONS = "bdpprintoscillatingiterations";
  public static final String ARG_BDP_RECORD_ALL_ITERATIONS = "bdprecordalliterations";
  public static final String ARG_BLOCK_NAMES = "blocknames";
  public static final String ARG_CONTAINER_DIR = "containerdir";
  public static final String ARG_DELTA_ENVIRONMENT_NAME = "deltaenv";
  public static final String ARG_DELTA_TESTRIG = "deltatestrig";
  public static final String ARG_DIFF_ACTIVE = "diffactive";
  public static final String ARG_DIFFERENTIAL = "differential";
  public static final String ARG_DISABLE_UNRECOGNIZED = "disableunrecognized";
  public static final String ARG_ENVIRONMENT_NAME = "env";
  public static final String ARG_HALT_ON_CONVERT_ERROR = "haltonconverterror";
  public static final String ARG_HALT_ON_PARSE_ERROR = "haltonparseerror";
  public static final String ARG_IGNORE_FILES_WITH_STRINGS = "ignorefileswithstrings";
  public static final String ARG_LOG_FILE = "logfile";
  public static final String ARG_LOG_LEVEL = "loglevel";
  public static final String ARG_OUTPUT_ENV = "outputenv";
  public static final String ARG_PEDANTIC_AS_ERROR = "pedanticerror";
  public static final String ARG_PEDANTIC_SUPPRESS = "pedanticsuppress";
  public static final String ARG_PRETTY_PRINT_ANSWER = "ppa";
  public static final String ARG_QUESTION_NAME = "questionname";
  public static final String ARG_RED_FLAG_AS_ERROR = "redflagerror";
  public static final String ARG_RED_FLAG_SUPPRESS = "redflagsuppress";
  public static final String ARG_SSL_DISABLE = "ssldisable";
  public static final String ARG_SSL_KEYSTORE_FILE = "sslkeystorefile";
  public static final String ARG_SSL_KEYSTORE_PASSWORD = "sslkeystorepassword";
  public static final String ARG_SSL_TRUST_ALL_CERTS = "ssltrustallcerts";
  public static final String ARG_SSL_TRUSTSTORE_FILE = "ssltruststorefile";
  public static final String ARG_SSL_TRUSTSTORE_PASSWORD = "ssltruststorepassword";
  public static final String ARG_SYNTHESIZE_JSON_TOPOLOGY = "synthesizejsontopology";
  public static final String ARG_SYNTHESIZE_TOPOLOGY = "synthesizetopology";
  public static final String ARG_TASK_PLUGIN = "taskplugin";
  public static final String ARG_TESTRIG = "testrig";
  public static final String ARG_UNIMPLEMENTED_AS_ERROR = "unimplementederror";
  public static final String ARG_UNIMPLEMENTED_SUPPRESS = "unimplementedsuppress";
  public static final String ARG_UNRECOGNIZED_AS_RED_FLAG = "urf";
  public static final String ARG_VERBOSE_PARSE = "verboseparse";

  public static final String COMMAND_ANALYZE = "analyze";
  public static final String COMMAND_ANSWER = "answer";
  public static final String COMMAND_COMPILE_DIFF_ENVIRONMENT = "diffcompile";
  public static final String COMMAND_DUMP_DP = "dp";
  public static final String COMMAND_INIT_INFO = "initinfo";
  public static final String COMMAND_PARSE_VENDOR_INDEPENDENT = "si";
  public static final String COMMAND_PARSE_VENDOR_SPECIFIC = "sv";
  public static final String COMMAND_QUERY = "query";
  public static final String COMMAND_REPORT = "report";
  public static final String COMMAND_VALIDATE_ENVIRONMENT = "venv";

  /*
   * JSON key names
   */
  public static final String PROP_ALLINONE_PROPERTIES_PATH = "batfishAllinonePropertiesPath";
  public static final String PROP_ALLOWED_VALUES = "allowedValues";
  public static final String PROP_ANSWER_ELEMENTS = "answerElements";
  public static final String PROP_BATFISH_PROPERTIES_PATH = "batfishBatfishPropertiesPath";
  public static final String PROP_BGP_ANNOUNCEMENTS = "Announcements";
  public static final String PROP_CLIENT_PROPERTIES_PATH = "batfishClientPropertiesPath";
  public static final String PROP_COORDINATOR_PROPERTIES_PATH = "batfishCoordinatorPropertiesPath";
  public static final String PROP_DESCRIPTION = "description";
  public static final String PROP_DIFFERENTIAL = "differential";
  public static final String PROP_INNER_QUESTION = "innerQuestion";
  public static final String PROP_INSTANCE = "instance";
  public static final String PROP_INSTANCE_NAME = "instanceName";
  public static final String PROP_LONG_DESCRIPTION = "longDescription";
  public static final String PROP_MIN_ELEMENTS = "minElements";
  public static final String PROP_MIN_LENGTH = "minLength";
  public static final String PROP_OPTIONAL = "optional";
  public static final String PROP_QUESTION = "question";
  public static final String PROP_STATUS = "status";
  public static final String PROP_SUMMARY = "summary";
  public static final String PROP_TAGS = "tags";
  public static final String PROP_TYPE = "type";
  public static final String PROP_VALUE = "value";
  public static final String PROP_VARIABLES = "variables";

  public static final String RELPATH_ANALYSES_DIR = "analyses";
  public static final String RELPATH_ANALYSIS_FILE = "analysis";
  public static final String RELPATH_ANSWER_HTML = "answer.html";
  public static final String RELPATH_ANSWER_JSON = "answer.json";
  public static final String RELPATH_ANSWER_PRETTY_JSON = "answer-pretty.json";
  public static final String RELPATH_AWS_VPC_CONFIGS_DIR = "aws_configs";
  public static final String RELPATH_AWS_VPC_CONFIGS_FILE = "aws_configs";
  public static final String RELPATH_CONFIG_FILE_NAME_ALLINONE = "allinone.properties";
  public static final String RELPATH_CONFIG_FILE_NAME_BATFISH = "batfish.properties";
  public static final String RELPATH_CONFIG_FILE_NAME_CLIENT = "client.properties";
  public static final String RELPATH_CONFIG_FILE_NAME_COORDINATOR = "coordinator.properties";
  public static final String RELPATH_CONFIGURATIONS_DIR = "configs";
  public static final String RELPATH_CONVERT_ANSWER_PATH = "convert_answer";
  public static final String RELPATH_DATA_PLANE = "dp";
  public static final String RELPATH_DATA_PLANE_ANSWER_PATH = "dp_answer";
  public static final String RELPATH_DEFAULT_ENVIRONMENT_NAME = "env_default";
  public static final String RELPATH_DELTA = "delta";
  public static final String RELPATH_DIFF = "diff";
  public static final String RELPATH_EDGE_BLACKLIST_FILE = "edge_blacklist";
  public static final String RELPATH_ENV_DIR = "env";
  public static final String RELPATH_ENV_NODE_SET = "env-node-set";
  public static final String RELPATH_ENV_TOPOLOGY_FILE = "env_topology";
  public static final String RELPATH_ENVIRONMENT_BGP_TABLES = "bgp";
  public static final String RELPATH_ENVIRONMENT_BGP_TABLES_ANSWER = "bgp_answer";
  public static final String RELPATH_ENVIRONMENT_ROUTING_TABLES = "rt";
  public static final String RELPATH_ENVIRONMENT_ROUTING_TABLES_ANSWER = "rt_answer";
  public static final String RELPATH_ENVIRONMENTS_DIR = "environments";
  public static final String RELPATH_EXTERNAL_BGP_ANNOUNCEMENTS = "external_bgp_announcements.json";
  public static final String RELPATH_FAILURE_QUERY_PREFIX = "failure-query";
  public static final String RELPATH_FLOWS_DUMP_DIR = "flowdump";
  public static final String RELPATH_HOST_CONFIGS_DIR = "hosts";
  public static final String RELPATH_INFERRED_NODE_ROLES_PATH = "node_roles_inferred.json";
  public static final String RELPATH_INTERFACE_BLACKLIST_FILE = "interface_blacklist";
  public static final String RELPATH_MULTIPATH_QUERY_PREFIX = "multipath-query";
  public static final String RELPATH_NODE_BLACKLIST_FILE = "node_blacklist";
  public static final String RELPATH_NODE_ROLES_PATH = "node_roles.json";
  public static final String RELPATH_PARSE_ANSWER_PATH = "parse_answer";
  public static final String RELPATH_PRECOMPUTED_ROUTES = "precomputedroutes";
  public static final String RELPATH_QUERIES_DIR = "queries";
  public static final String RELPATH_QUESTION_FILE = "question.json";
  public static final String RELPATH_QUESTIONS_DIR = "questions";
  public static final String RELPATH_SERIALIZED_ENVIRONMENT_BGP_TABLES = "bgp_processed";
  public static final String RELPATH_SERIALIZED_ENVIRONMENT_ROUTING_TABLES = "rt_processed";
  public static final String RELPATH_SYNC_TESTRIGS_DIR = "testrig_sync";
  public static final String RELPATH_TEST_RIG_DIR = "testrig";
  public static final String RELPATH_TESTRIG_TOPOLOGY_PATH = "testrig_topology";
  public static final String RELPATH_TESTRIGS_DIR = "testrigs";
  public static final String RELPATH_VALIDATE_ENVIRONMENT_ANSWER = "venv_answer";
  public static final String RELPATH_VENDOR_INDEPENDENT_CONFIG_DIR = "indep";
  public static final String RELPATH_VENDOR_SPECIFIC_CONFIG_DIR = "vendor";
  public static final String RELPATH_Z3_DATA_PLANE_FILE = "dataplane.smt2";

  public static final String SUFFIX_ANSWER_JSON_FILE = ".json";
  public static final String SUFFIX_LOG_FILE = ".log";

  public static final String SVC_BASE_RSC = "/batfishservice";
  public static final String SVC_FAILURE_KEY = "failure";
  public static final String SVC_GET_STATUS_RSC = "getstatus";
  public static final String SVC_GET_TASKSTATUS_RSC = "gettaskstatus";
  public static final Integer SVC_PORT = 9999;
  public static final String SVC_RUN_TASK_RSC = "run";
  public static final String SVC_SUCCESS_KEY = "success";
  public static final String SVC_TASK_KEY = "task";
  public static final String SVC_TASKID_KEY = "taskid";

  public static final String PROP_ALLINONE_SERVICE = "batfish-allinone";
  public static final String PROP_CLIENT_SERVICE = "batfish-client";
  public static final String PROP_COORDINATOR_SERVICE = "batfish-coordinator";
  public static final String PROP_WORKER_SERVICE = "batfish-worker";
}
