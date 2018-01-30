package org.batfish.common;

// IMPORTANT:
// If you change the values of any of these constants,
// make sure that the javascript, python clients is updated too

import org.batfish.common.BfConsts.TaskStatus;

public class CoordConsts {

  public enum WorkStatusCode {
    ASSIGNED,
    ASSIGNMENTERROR,
    BLOCKED,
    CHECKINGSTATUS,
    TERMINATEDABNORMALLY,
    TERMINATEDBYUSER,
    TERMINATEDNORMALLY,
    TRYINGTOASSIGN,
    UNASSIGNED;

    public static WorkStatusCode fromTerminatedTaskStatus(TaskStatus status) {
      switch (status) {
        case TerminatedAbnormally:
          return TERMINATEDABNORMALLY;
        case TerminatedByUser:
          return TERMINATEDBYUSER;
        case TerminatedNormally:
          return TERMINATEDNORMALLY;
        default:
          throw new IllegalArgumentException(
              "Cannot convert from " + status + " to WorkStatusCode");
      }
    }

    public boolean isTerminated() {
      return (this == ASSIGNMENTERROR // because we don't attempt assignment of this work
          || this == WorkStatusCode.TERMINATEDABNORMALLY
          || this == WorkStatusCode.TERMINATEDBYUSER
          || this == WorkStatusCode.TERMINATEDNORMALLY);
    }
  }

  public static final String DEFAULT_API_KEY = "00000000000000000000000000000000";

  /** Constants for where and how services are hosted */
  public static final String SVC_CFG_POOL_MGR = "/batfishpoolmgr";

  public static final Integer SVC_CFG_POOL_PORT = 9998;
  public static final boolean SVC_CFG_POOL_SSL_DISABLE = true;
  public static final String SVC_CFG_WORK_MGR = "/batfishworkmgr";
  public static final Integer SVC_CFG_WORK_PORT = 9997;
  public static final String SVC_CFG_WORK_MGR2 = "/v2";
  public static final Integer SVC_CFG_WORK_V2_PORT = 9996;
  public static final boolean SVC_CFG_WORK_SSL_DISABLE = true;

  public static final String SVC_FILENAME_HDR = "FileName";

  /** Various constants used as keys on multi-part form data */
  public static final String SVC_KEY_ADD_WORKER = "addworker";

  public static final String SVC_KEY_ANALYSIS_LIST = "analysislist";
  public static final String SVC_KEY_ANALYSIS_NAME = "analysisname";
  public static final String SVC_KEY_ANSWER = "answer";
  public static final String SVC_KEY_ANSWERS = "answers";
  public static final String SVC_KEY_API_KEY = "apikey";
  public static final String SVC_KEY_AUTO_ANALYZE_TESTRIG = "autoanalyzetestrig";
  public static final String SVC_KEY_BASE_ENV_NAME = "baseenvname";
  public static final String SVC_KEY_CONFIGURATION_NAME = "configurationname";
  public static final String SVC_KEY_CONTAINERS = "containers";
  public static final String SVC_KEY_CONTAINER_LIST = "containerlist";
  public static final String SVC_KEY_CONTAINER_NAME = "container";
  public static final String SVC_KEY_CONTAINER_PREFIX = "containerprefix";
  public static final String SVC_KEY_DEL_ANALYSIS_QUESTIONS = "delanalysisquestions";
  public static final String SVC_KEY_DEL_WORKER = "delworker";
  public static final String SVC_KEY_DELTA_ENV_NAME = "deltaenvname";
  public static final String SVC_KEY_DELTA_TESTRIG_NAME = "deltatestrigname";
  public static final String SVC_KEY_ENV_NAME = "envname";
  public static final String SVC_KEY_ENVIRONMENT_LIST = "environmentlist";
  public static final String SVC_KEY_FAILURE = "failure";
  public static final String SVC_KEY_FILE = "file";
  public static final String SVC_KEY_FILE2 = "file2";
  public static final String SVC_KEY_FORCE = "force";
  public static final String SVC_KEY_NEW_ANALYSIS = "newanalysis";
  public static final String SVC_KEY_OBJECT_NAME = "objectname";
  public static final String SVC_KEY_PLUGIN_ID = "pluginid";
  public static final String SVC_KEY_QUESTION_LIST = "questionlist";
  public static final String SVC_KEY_QUESTION_NAME = "questionname";
  public static final String SVC_KEY_RESULT = "result";
  public static final String SVC_KEY_SETTINGS = "settings";
  public static final String SVC_KEY_SUCCESS = "success";
  public static final String SVC_KEY_TASKSTATUS = "taskstatus";
  public static final String SVC_KEY_TESTRIG_INFO = "testriginfo";
  public static final String SVC_KEY_TESTRIG_LIST = "testriglist";
  public static final String SVC_KEY_TESTRIG_METADATA = "testrigmetadata";
  public static final String SVC_KEY_TESTRIG_NAME = "testrigname";
  public static final String SVC_KEY_VERSION = "version";
  public static final String SVC_KEY_WORK_LIST = "worklist";
  public static final String SVC_KEY_WORKID = "workid";
  public static final String SVC_KEY_WORKITEM = "workitem";
  public static final String SVC_KEY_WORKSPACE_NAME = "workspace";
  public static final String SVC_KEY_WORKSTATUS = "workstatus";
  public static final String SVC_KEY_ZIPFILE = "zipfile";

  /** Constants for endpoints of various service calls */
  public static final String SVC_RSC_CHECK_API_KEY = "checkapikey";

  public static final String SVC_RSC_CONFIGURE_ANALYSIS = "configureanalysis";
  public static final String SVC_RSC_DEL_ANALYSIS = "delanalysis";
  public static final String SVC_RSC_DEL_CONTAINER = "delcontainer";
  public static final String SVC_RSC_DEL_ENVIRONMENT = "delenvironment";
  public static final String SVC_RSC_DEL_QUESTION = "delquestion";
  public static final String SVC_RSC_DEL_TESTRIG = "deltestrig";
  public static final String SVC_RSC_GET_ANALYSIS_ANSWERS = "getanalysisanswers";
  public static final String SVC_RSC_GET_ANSWER = "getanswer";
  public static final String SVC_RSC_GET_CONFIGURATION = "getconfiguration";
  public static final String SVC_RSC_GET_CONTAINER = "getcontainer";
  public static final String SVC_RSC_GET_DATAPLANING_WORK = "getdataplaningwork";
  public static final String SVC_RSC_GET_OBJECT = "getobject";
  public static final String SVC_RSC_GET_PARSING_WORK = "getparsingwork";
  public static final String SVC_RSC_GET_QUESTION_TEMPLATES = "getquestiontemplates";
  public static final String SVC_RSC_GET_WORKSTATUS = "getworkstatus";
  public static final String SVC_RSC_GETSTATUS = "getstatus";
  public static final String SVC_RSC_INIT_CONTAINER = "initcontainer";
  public static final String SVC_RSC_KILL_WORK = "killwork";
  public static final String SVC_RSC_LIST_ANALYSES = "listanalyses";
  public static final String SVC_RSC_LIST_CONTAINERS = "listcontainers";
  public static final String SVC_RSC_LIST_ENVIRONMENTS = "listenvironments";
  public static final String SVC_RSC_LIST_INCOMPLETE_WORK = "listincompletework";
  public static final String SVC_RSC_LIST_QUESTIONS = "listquestions";
  public static final String SVC_RSC_LIST_TESTRIGS = "listtestrigs";
  public static final String SVC_RSC_POOL_GET_QUESTION_TEMPLATES = "getquestiontemplates";
  public static final String SVC_RSC_POOL_GETSTATUS = "getstatus";
  public static final String SVC_RSC_POOL_UPDATE = "updatepool";
  public static final String SVC_RSC_PUT_OBJECT = "putobject";
  public static final String SVC_RSC_QUEUE_WORK = "queuework";
  public static final String SVC_RSC_SYNC_TESTRIGS_SYNC_NOW = "synctestrigssyncnow";
  public static final String SVC_RSC_SYNC_TESTRIGS_UPDATE_SETTINGS = "synctestrigsupdatesettings";
  public static final String SVC_RSC_UPLOAD_ENV = "uploadenvironment";
  public static final String SVC_RSC_UPLOAD_QUESTION = "uploadquestion";
  public static final String SVC_RSC_UPLOAD_TESTRIG = "uploadtestrig";
}
