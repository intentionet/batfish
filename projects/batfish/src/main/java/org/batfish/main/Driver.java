package org.batfish.main;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.uber.jaeger.Configuration.ReporterConfiguration;
import com.uber.jaeger.Configuration.SamplerConfiguration;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.ActiveSpan;
import io.opentracing.References;
import io.opentracing.SpanContext;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.util.GlobalTracer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.BfConsts.TaskStatus;
import org.batfish.common.CleanBatfishException;
import org.batfish.common.CompositeBatfishException;
import org.batfish.common.CoordConsts;
import org.batfish.common.QuestionException;
import org.batfish.common.Task;
import org.batfish.common.Task.Batch;
import org.batfish.common.Version;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.ConfigurationLocator;
import org.batfish.config.Settings;
import org.batfish.config.Settings.EnvironmentSettings;
import org.batfish.config.Settings.TestrigSettings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.collections.BgpAdvertisementsByVrf;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.codehaus.jettison.json.JSONArray;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.server.ResourceConfig;

@SuppressWarnings("restriction")
public class Driver {

  public enum RunMode {
    WATCHDOG,
    WORKER,
    WORKSERVICE,
  }

  static final class CheckParentProcessTask implements Runnable {
    final int _ppid;

    public CheckParentProcessTask(int ppid) {
      _ppid = ppid;
    }

    @Override
    public void run() {
      Driver.checkParentProcess(_ppid);
    }
  }

  private static void checkParentProcess(int ppid) {
    try {
      if (!isProcessRunning(ppid)) {
        _mainLogger.infof("Committing suicide; ppid %d is dead.\n", ppid);
        System.exit(0);
      }
    } catch (Exception e) {
      _mainLogger.errorf("Exception while checking parent process with pid: %s", ppid);
    }
  }

  private static boolean _idle = true;

  private static Date _lastPollFromCoordinator = new Date();

  private static String[] _mainArgs = null;

  private static BatfishLogger _mainLogger = null;

  private static Settings _mainSettings = null;

  private static ConcurrentMap<String, Task> _taskLog;

  private static final Cache<TestrigSettings, DataPlane> CACHED_DATA_PLANES = buildDataPlaneCache();

  private static final Map<EnvironmentSettings, SortedMap<String, BgpAdvertisementsByVrf>>
      CACHED_ENVIRONMENT_BGP_TABLES = buildEnvironmentBgpTablesCache();

  private static final Map<EnvironmentSettings, SortedMap<String, RoutesByVrf>>
      CACHED_ENVIRONMENT_ROUTING_TABLES = buildEnvironmentRoutingTablesCache();

  private static final Cache<TestrigSettings, SortedMap<String, Configuration>> CACHED_TESTRIGS =
      buildTestrigCache();

  private static final int COORDINATOR_CHECK_INTERVAL_MS = 1 * 60 * 1000; // 1 min

  private static final int COORDINATOR_POLL_TIMEOUT_MS = 30 * 1000; // 30 secs

  private static final int COORDINATOR_REGISTRATION_RETRY_INTERVAL_MS = 1 * 1000; // 1 sec

  private static final int PARENT_CHECK_INTERVAL_MS = 1 * 1000; // 1 sec

  static Logger httpServerLogger =
      Logger.getLogger(org.glassfish.grizzly.http.server.HttpServer.class.getName());

  private static final int MAX_CACHED_DATA_PLANES = 2;

  private static final int MAX_CACHED_ENVIRONMENT_BGP_TABLES = 4;

  private static final int MAX_CACHED_ENVIRONMENT_ROUTING_TABLES = 4;

  private static final int MAX_CACHED_TESTRIGS = 5;

  static Logger networkListenerLogger =
      Logger.getLogger("org.glassfish.grizzly.http.server.NetworkListener");

  private static Cache<TestrigSettings, DataPlane> buildDataPlaneCache() {
    return CacheBuilder.newBuilder().maximumSize(MAX_CACHED_DATA_PLANES).weakValues().build();
  }

  private static Map<EnvironmentSettings, SortedMap<String, BgpAdvertisementsByVrf>>
      buildEnvironmentBgpTablesCache() {
    return Collections.synchronizedMap(
        new LRUMap<EnvironmentSettings, SortedMap<String, BgpAdvertisementsByVrf>>(
            MAX_CACHED_ENVIRONMENT_BGP_TABLES));
  }

  private static Map<EnvironmentSettings, SortedMap<String, RoutesByVrf>>
      buildEnvironmentRoutingTablesCache() {
    return Collections.synchronizedMap(
        new LRUMap<EnvironmentSettings, SortedMap<String, RoutesByVrf>>(
            MAX_CACHED_ENVIRONMENT_ROUTING_TABLES));
  }

  private static Cache<TestrigSettings, SortedMap<String, Configuration>> buildTestrigCache() {
    return CacheBuilder.newBuilder().maximumSize(MAX_CACHED_TESTRIGS).build();
  }

  private static synchronized boolean claimIdle() {
    if (_idle) {
      _idle = false;
      return true;
    }

    return false;
  }

  public static synchronized boolean getIdle() {
    _lastPollFromCoordinator = new Date();
    return _idle;
  }

  public static BatfishLogger getMainLogger() {
    return _mainLogger;
  }

  @Nullable
  private static synchronized Task getTask(Settings settings) {
    String taskId = settings.getTaskId();
    if (taskId == null) {
      return null;
    } else {
      return _taskLog.get(taskId);
    }
  }

  @Nullable
  public static synchronized Task getTaskFromLog(String taskId) {
    return _taskLog.get(taskId);
  }

  private static void initTracer() {
    GlobalTracer.register(
        new com.uber.jaeger.Configuration(
                BfConsts.PROP_WORKER_SERVICE,
                new SamplerConfiguration(ConstSampler.TYPE, 1),
                new ReporterConfiguration(
                    false,
                    _mainSettings.getTracingAgentHost(),
                    _mainSettings.getTracingAgentPort(),
                    /* flush interval in ms */ 1000,
                    /* max buffered Spans */ 10000))
            .getTracer());
  }

  private static boolean isProcessRunning(int pid) throws IOException {
    // all of this would be a lot simpler in Java 9, using processHandle

    if (SystemUtils.IS_OS_WINDOWS) {
      throw new UnsupportedOperationException("Process monitoring is not supported on Windows");
    } else {
      ProcessBuilder builder = new ProcessBuilder("ps", "-x", String.valueOf(pid));
      builder.redirectErrorStream(true);
      Process process = builder.start();

      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          String[] columns = line.trim().split("\\s+");
          if (String.valueOf(pid).equals(columns[0])) {
            return true;
          }
        }
      }
    }

    return false;
  }

  @Nullable
  public static synchronized Task killTask(String taskId) {
    Task task = _taskLog.get(taskId);
    if (task == null) {
      throw new BatfishException("Task with provided id not found: " + taskId);
    } else if (task.getStatus().isTerminated()) {
      throw new BatfishException("Task with provided id already terminated " + taskId);
    } else {
      // update task details in case a new query for status check comes in
      task.newBatch("Got kill request");
      task.setStatus(TaskStatus.TerminatedByUser);
      task.setTerminated(new Date());

      // we die after a little bit, to allow for the response making it back to the coordinator
      new java.util.Timer()
          .schedule(
              new java.util.TimerTask() {
                @Override
                public void run() {
                  System.exit(0);
                }
              },
              3000);

      return task;
    }
  }

  private static synchronized void logTask(String taskId, Task task) throws Exception {
    if (_taskLog.containsKey(taskId)) {
      throw new Exception("duplicate UUID for task");
    } else {
      _taskLog.put(taskId, task);
    }
  }

  public static void main(String[] args) {
    mainInit(args);
    _mainLogger =
        new BatfishLogger(
            _mainSettings.getLogLevel(),
            _mainSettings.getTimestamp(),
            _mainSettings.getLogFile(),
            _mainSettings.getLogTee(),
            true);
    mainRun();
  }

  public static void main(String[] args, BatfishLogger logger) {
    mainInit(args);
    _mainLogger = logger;
    mainRun();
  }

  private static void mainInit(String[] args) {
    _taskLog = new ConcurrentHashMap<>();
    _mainArgs = args;
    try {
      _mainSettings = new Settings(args);
      networkListenerLogger.setLevel(Level.WARNING);
      httpServerLogger.setLevel(Level.WARNING);
    } catch (Exception e) {
      System.err.println(
          "batfish: Initialization failed. Reason: " + ExceptionUtils.getFullStackTrace(e));
      System.exit(1);
    }
  }

  private static void mainRun() {
    System.setErr(_mainLogger.getPrintStream());
    System.setOut(_mainLogger.getPrintStream());
    _mainSettings.setLogger(_mainLogger);
    switch (_mainSettings.getRunMode()) {
      case WATCHDOG:
        mainRunWatchdog();
        break;
      case WORKER:
        mainRunWorker();
        break;
      case WORKSERVICE:
        mainRunWorkService();
        break;
      default:
        System.err.println(
            "batfish: Initialization failed. Unknown runmode: " + _mainSettings.getRunMode());
        System.exit(1);
    }
  }

  private static void mainRunWatchdog() {
    while (true) {
      RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
      String classPath = runtimeMxBean.getClassPath();
      List<String> jvmArguments =
          runtimeMxBean
              .getInputArguments()
              .stream()
              .filter(arg -> !arg.startsWith("-agentlib:")) // remove IntelliJ hooks
              .collect(ImmutableList.toImmutableList());
      int myPid = Integer.parseInt(runtimeMxBean.getName().split("@")[0]);

      List<String> command =
          new ImmutableList.Builder<String>()
              .add(
                  Paths.get(System.getProperty("java.home"), "bin", "java")
                      .toAbsolutePath()
                      .toString())
              .addAll(jvmArguments)
              .add("-cp")
              .add(classPath)
              .add(Driver.class.getCanonicalName())
              // if we add runmode here, any runmode argument in mainArgs is ignored
              .add("-" + Settings.ARG_RUN_MODE)
              .add(RunMode.WORKSERVICE.toString())
              .add("-" + Settings.ARG_PARENT_PID)
              .add(Integer.toString(myPid))
              .addAll(Arrays.asList(_mainArgs))
              .build();

      _mainLogger.debugf("Will start workservice with arguments: %s\n", command);

      ProcessBuilder builder = new ProcessBuilder(command);
      builder.redirectErrorStream(true);
      builder.redirectInput(Redirect.INHERIT);

      Process process;
      try {
        process = builder.start();
      } catch (IOException e) {
        _mainLogger.errorf("Exception starting process: %s", ExceptionUtils.getFullStackTrace(e));
        _mainLogger.errorf("Will try again in 1 second\n");
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
          _mainLogger.errorf("Sleep was interrrupted: %s", ExceptionUtils.getFullStackTrace(e1));
        }
        continue;
      }

      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        reader.lines().forEach(line -> _mainLogger.output(line + "\n"));
      } catch (IOException e) {
        _mainLogger.errorf(
            "Interrupted while reading subprocess stream: %s", ExceptionUtils.getFullStackTrace(e));
      }

      try {
        process.waitFor();
      } catch (InterruptedException e) {
        _mainLogger.infof(
            "Subprocess was killed: %s.\nRestarting", ExceptionUtils.getFullStackTrace(e));
      }
    }
  }

  private static void mainRunWorker() {
    if (_mainSettings.canExecute()) {
      _mainSettings.setLogger(_mainLogger);
      Batfish.initTestrigSettings(_mainSettings);
      if (!runBatfish(_mainSettings)) {
        System.exit(1);
      }
    }
  }

  private static void mainRunWorkService() {
    if (_mainSettings.getTracingEnable() && !GlobalTracer.isRegistered()) {
      initTracer();
    }

    String protocol = _mainSettings.getSslDisable() ? "http" : "https";
    String baseUrl = String.format("%s://%s", protocol, _mainSettings.getServiceBindHost());
    URI baseUri = UriBuilder.fromUri(baseUrl).port(_mainSettings.getServicePort()).build();
    _mainLogger.debug(String.format("Starting server at %s\n", baseUri));
    ResourceConfig rc = new ResourceConfig(Service.class).register(new JettisonFeature());
    if (_mainSettings.getTracingEnable()) {
      rc.register(ServerTracingDynamicFeature.class);
    }
    try {
      if (_mainSettings.getSslDisable()) {
        GrizzlyHttpServerFactory.createHttpServer(baseUri, rc);
      } else {
        CommonUtil.startSslServer(
            rc,
            baseUri,
            _mainSettings.getSslKeystoreFile(),
            _mainSettings.getSslKeystorePassword(),
            _mainSettings.getSslTrustAllCerts(),
            _mainSettings.getSslTruststoreFile(),
            _mainSettings.getSslTruststorePassword(),
            ConfigurationLocator.class,
            Driver.class);
      }
      if (_mainSettings.getCoordinatorRegister()) {
        // this function does not return until registration succeeds
        registerWithCoordinatorPersistent();
      }

      if (_mainSettings.getParentPid() > 0) {
        if (SystemUtils.IS_OS_WINDOWS) {
          _mainLogger.errorf(
              "Parent process monitoring is not supported on Windows. We'll live without it.");
        } else {
          Executors.newScheduledThreadPool(1)
              .scheduleAtFixedRate(
                  new CheckParentProcessTask(_mainSettings.getParentPid()),
                  0,
                  PARENT_CHECK_INTERVAL_MS,
                  TimeUnit.MILLISECONDS);
        }
      }

      // sleep indefinitely, check for parent pid and coordinator each time
      while (true) {
        Thread.sleep(COORDINATOR_CHECK_INTERVAL_MS);
        /*
         * every time we wake up, we check if the coordinator has polled us recently
         * if not, re-register the service. the coordinator might have died and come back.
         */
        if (_mainSettings.getCoordinatorRegister()
            && new Date().getTime() - _lastPollFromCoordinator.getTime()
                > COORDINATOR_POLL_TIMEOUT_MS) {
          // this function does not return until registration succeeds
          registerWithCoordinatorPersistent();
        }
      }
    } catch (ProcessingException e) {
      String msg = "FATAL ERROR: " + e.getMessage() + "\n";
      _mainLogger.error(msg);
      System.exit(1);
    } catch (Exception ex) {
      String stackTrace = ExceptionUtils.getFullStackTrace(ex);
      _mainLogger.error(stackTrace);
      System.exit(1);
    }
  }

  private static synchronized void makeIdle() {
    _idle = true;
  }

  public static synchronized AtomicInteger newBatch(
      Settings settings, String description, int jobs) {
    Batch batch = null;
    Task task = getTask(settings);
    if (task != null) {
      batch = task.newBatch(description);
      batch.setSize(jobs);
      return batch.getCompleted();
    } else {
      return new AtomicInteger();
    }
  }

  private static boolean registerWithCoordinator(String poolRegUrl) {
    Map<String, String> params = new HashMap<>();
    params.put(
        CoordConsts.SVC_KEY_ADD_WORKER,
        _mainSettings.getServiceHost() + ":" + _mainSettings.getServicePort());
    params.put(CoordConsts.SVC_KEY_VERSION, Version.getVersion());

    Object response = talkToCoordinator(poolRegUrl, params, _mainLogger);
    return response != null;
  }

  private static void registerWithCoordinatorPersistent() throws InterruptedException {
    boolean registrationSuccess;

    String protocol = _mainSettings.getSslDisable() ? "http" : "https";
    String poolRegUrl =
        String.format(
            "%s://%s:%s%s/%s",
            protocol,
            _mainSettings.getCoordinatorHost(),
            +_mainSettings.getCoordinatorPoolPort(),
            CoordConsts.SVC_CFG_POOL_MGR,
            CoordConsts.SVC_RSC_POOL_UPDATE);

    do {
      registrationSuccess = registerWithCoordinator(poolRegUrl);
      if (!registrationSuccess) {
        Thread.sleep(COORDINATOR_REGISTRATION_RETRY_INTERVAL_MS);
      }
    } while (!registrationSuccess);
  }

  @SuppressWarnings("deprecation")
  private static boolean runBatfish(final Settings settings) {

    final BatfishLogger logger = settings.getLogger();

    try {
      final Batfish batfish =
          new Batfish(
              settings,
              CACHED_TESTRIGS,
              CACHED_DATA_PLANES,
              CACHED_ENVIRONMENT_BGP_TABLES,
              CACHED_ENVIRONMENT_ROUTING_TABLES);

      @Nullable
      SpanContext runBatfishSpanContext =
          GlobalTracer.get().activeSpan() == null
              ? null
              : GlobalTracer.get().activeSpan().context();

      Thread thread =
          new Thread() {
            @Override
            public void run() {
              try (ActiveSpan runBatfishSpan =
                  GlobalTracer.get()
                      .buildSpan("Run Batfish job in a new thread and get the answer")
                      .addReference(References.FOLLOWS_FROM, runBatfishSpanContext)
                      .startActive()) {
                assert runBatfishSpan != null;
                Answer answer = null;
                try {
                  answer = batfish.run();
                  batfish.setTerminatedWithException(false);
                  if (answer.getStatus() == null) {
                    answer.setStatus(AnswerStatus.SUCCESS);
                  }
                } catch (CleanBatfishException e) {
                  batfish.setTerminatedWithException(true);
                  String msg = "FATAL ERROR: " + e.getMessage();
                  logger.error(msg);
                  answer = Answer.failureAnswer(msg, null);
                } catch (QuestionException e) {
                  String stackTrace = ExceptionUtils.getFullStackTrace(e);
                  logger.error(stackTrace);
                  answer = e.getAnswer();
                  answer.setStatus(AnswerStatus.FAILURE);
                  batfish.setTerminatedWithException(true);
                } catch (CompositeBatfishException e) {
                  String stackTrace = ExceptionUtils.getFullStackTrace(e);
                  logger.error(stackTrace);
                  answer = new Answer();
                  answer.setStatus(AnswerStatus.FAILURE);
                  answer.addAnswerElement(e.getAnswerElement());
                  batfish.setTerminatedWithException(true);
                } catch (Throwable e) {
                  String stackTrace = ExceptionUtils.getFullStackTrace(e);
                  logger.error(stackTrace);
                  answer = new Answer();
                  answer.setStatus(AnswerStatus.FAILURE);
                  answer.addAnswerElement(
                      new BatfishException("Batfish job failed", e).getBatfishStackTrace());
                  batfish.setTerminatedWithException(true);
                } finally {
                  try (ActiveSpan outputAnswerSpan =
                      GlobalTracer.get().buildSpan("Outputting answer").startActive()) {
                    assert outputAnswerSpan != null;
                    if (settings.getAnswerJsonPath() != null) {
                      batfish.outputAnswerWithLog(answer);
                    }
                  }
                }
              }
            }
          };

      thread.start();
      thread.join(settings.getMaxRuntimeMs());

      if (thread.isAlive()) {
        // this is deprecated but we should be safe since we don't have
        // locks and such
        // AF: This doesn't do what you think it does, esp. not in Java 8.
        // It needs to be replaced.
        thread.stop();
        logger.error("Batfish worker took too long. Terminated.");
        batfish.setTerminatedWithException(true);
      }

      return !batfish.getTerminatedWithException();
    } catch (Exception e) {
      String stackTrace = ExceptionUtils.getFullStackTrace(e);
      logger.error(stackTrace);
      return false;
    }
  }

  public static List<String> runBatfishThroughService(final String taskId, String[] args) {
    final Settings settings;
    try {
      settings = new Settings(args);
      // assign taskId for status updates, termination requests
      settings.setTaskId(taskId);
    } catch (Exception e) {
      return Arrays.asList(
          "failure", "Initialization failed: " + ExceptionUtils.getFullStackTrace(e));
    }

    try {
      Batfish.initTestrigSettings(settings);
    } catch (Exception e) {
      return Arrays.asList(
          "failure",
          "Failed while applying auto basedir. (All arguments are supplied?): " + e.getMessage());
    }

    if (settings.canExecute()) {
      if (claimIdle()) {

        // lets put a try-catch around all the code around claimIdle
        // so that we never the worker non-idle accidentally

        try {

          final BatfishLogger jobLogger =
              new BatfishLogger(
                  settings.getLogLevel(),
                  settings.getTimestamp(),
                  settings.getLogFile(),
                  settings.getLogTee(),
                  false);
          settings.setLogger(jobLogger);

          settings.setMaxRuntimeMs(_mainSettings.getMaxRuntimeMs());

          final Task task = new Task(args);

          logTask(taskId, task);

          @Nullable
          SpanContext runTaskSpanContext =
              GlobalTracer.get().activeSpan() == null
                  ? null
                  : GlobalTracer.get().activeSpan().context();

          // run batfish on a new thread and set idle to true when done
          Thread thread =
              new Thread() {
                @Override
                public void run() {
                  try (ActiveSpan runBatfishSpan =
                      GlobalTracer.get()
                          .buildSpan("Initialize Batfish in a new thread")
                          .addReference(References.FOLLOWS_FROM, runTaskSpanContext)
                          .startActive()) {
                    assert runBatfishSpan != null; // avoid unused warning
                    task.setStatus(TaskStatus.InProgress);
                    if (runBatfish(settings)) {
                      task.setStatus(TaskStatus.TerminatedNormally);
                    } else {
                      task.setStatus(TaskStatus.TerminatedAbnormally);
                    }
                    task.setTerminated(new Date());
                    jobLogger.close();
                    makeIdle();
                  }
                }
              };

          thread.start();

          return Arrays.asList(BfConsts.SVC_SUCCESS_KEY, "running now");
        } catch (Exception e) {
          _mainLogger.error("Exception while running task: " + e.getMessage());
          makeIdle();
          return Arrays.asList(BfConsts.SVC_FAILURE_KEY, e.getMessage());
        }
      } else {
        return Arrays.asList(BfConsts.SVC_FAILURE_KEY, "Not idle");
      }
    } else {
      return Arrays.asList(BfConsts.SVC_FAILURE_KEY, "Non-executable command");
    }
  }

  public static Object talkToCoordinator(
      String url, Map<String, String> params, BatfishLogger logger) {
    Client client = null;
    try {
      client =
          CommonUtil.createHttpClientBuilder(
                  _mainSettings.getSslDisable(),
                  _mainSettings.getSslTrustAllCerts(),
                  _mainSettings.getSslKeystoreFile(),
                  _mainSettings.getSslKeystorePassword(),
                  _mainSettings.getSslTruststoreFile(),
                  _mainSettings.getSslTruststorePassword())
              .build();
      WebTarget webTarget = client.target(url);
      for (Map.Entry<String, String> entry : params.entrySet()) {
        webTarget = webTarget.queryParam(entry.getKey(), entry.getValue());
      }
      Response response = webTarget.request(MediaType.APPLICATION_JSON).get();

      logger.debug(
          "BF: " + response.getStatus() + " " + response.getStatusInfo() + " " + response + "\n");

      if (response.getStatus() != Response.Status.OK.getStatusCode()) {
        logger.error("Did not get an OK response\n");
        return null;
      }

      String sobj = response.readEntity(String.class);
      JSONArray array = new JSONArray(sobj);
      logger.debugf("BF: response: %s [%s] [%s]\n", array, array.get(0), array.get(1));

      if (!array.get(0).equals(CoordConsts.SVC_KEY_SUCCESS)) {
        logger.errorf(
            "BF: got error while talking to coordinator: %s %s\n", array.get(0), array.get(1));
        return null;
      }

      return array.get(1);
    } catch (ProcessingException e) {
      if (CommonUtil.causedBy(e, SSLHandshakeException.class)
          || CommonUtil.causedByMessage(e, "Unexpected end of file from server")) {
        throw new BatfishException("Unrecoverable connection error", e);
      }
      logger.errorf("BF: unable to connect to coordinator pool mgr at %s\n", url);
      logger.debug(ExceptionUtils.getStackTrace(e) + "\n");
      return null;
    } catch (Exception e) {
      logger.errorf("exception: " + ExceptionUtils.getStackTrace(e));
      return null;
    } finally {
      if (client != null) {
        client.close();
      }
    }
  }
}
