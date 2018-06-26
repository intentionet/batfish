package org.batfish.coordinator;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.uber.jaeger.Configuration;
import com.uber.jaeger.Configuration.ReporterConfiguration;
import com.uber.jaeger.Configuration.SamplerConfiguration;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.util.GlobalTracer;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.BindPortFutures;
import org.batfish.common.util.CommonUtil;
import org.batfish.coordinator.authorizer.Authorizer;
import org.batfish.coordinator.authorizer.DbAuthorizer;
import org.batfish.coordinator.authorizer.FileAuthorizer;
import org.batfish.coordinator.authorizer.NoneAuthorizer;
import org.batfish.coordinator.config.ConfigurationLocator;
import org.batfish.coordinator.config.Settings;
import org.batfish.datamodel.questions.Question;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class Main {

  // These are all @Nullable because they are static and may not be initialized if Main() has not
  // been called.
  private static @Nullable Authorizer _authorizer;
  private static @Nullable BatfishLogger _logger;
  private static @Nullable PoolMgr _poolManager;
  private static @Nullable Settings _settings;
  private static @Nullable WorkMgr _workManager;

  static Logger httpServerLogger = Logger.getLogger(HttpServer.class.getName());
  static Logger networkListenerLogger =
      Logger.getLogger(org.glassfish.grizzly.http.server.NetworkListener.class.getName());

  public static Authorizer getAuthorizer() {
    checkState(_authorizer != null, "Error: Authorizer has not been configured");
    return _authorizer;
  }

  public static BatfishLogger getLogger() {
    checkState(_logger != null, "Error: Logger has not been configured");
    return _logger;
  }

  public static PoolMgr getPoolMgr() {
    checkState(_poolManager != null, "Error: Pool Manager has not been configured");
    return _poolManager;
  }

  @Nullable
  public static Map<String, String> getQuestionTemplates() {

    List<Path> questionTemplateDir = _settings.getQuestionTemplateDirs();

    if (questionTemplateDir == null || questionTemplateDir.isEmpty()) {
      return null;
    }

    Map<String, String> questionTemplates = new HashMap<>();
    questionTemplateDir.forEach((dir) -> readQuestionTemplates(dir, questionTemplates));

    return questionTemplates;
  }

  private static String readQuestionTemplate(Path file, Map<String, String> templates) {
    String questionText = CommonUtil.readFile(file);
    try {
      JSONObject questionObj = new JSONObject(questionText);
      if (questionObj.has(BfConsts.PROP_INSTANCE) && !questionObj.isNull(BfConsts.PROP_INSTANCE)) {
        JSONObject instanceDataObj = questionObj.getJSONObject(BfConsts.PROP_INSTANCE);
        String instanceDataStr = instanceDataObj.toString();
        Question.InstanceData instanceData =
            BatfishObjectMapper.mapper().readValue(instanceDataStr, Question.InstanceData.class);
        String name = instanceData.getInstanceName();

        if (templates.containsKey(name)) {
          throw new BatfishException("Duplicate template name " + name);
        }

        templates.put(name.toLowerCase(), questionText);
        return name;
      } else {
        throw new BatfishException("Question in file: '" + file + "' has no instance name");
      }
    } catch (JSONException | IOException e) {
      throw new BatfishException("Failed to process question", e);
    }
  }

  private static void readQuestionTemplates(Path questionsPath, Map<String, String> templates) {
    try {
      Files.walkFileTree(
          questionsPath,
          EnumSet.of(FOLLOW_LINKS),
          1,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
              String filename = file.getFileName().toString();
              if (filename.endsWith(".json")) {
                readQuestionTemplate(file, templates);
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      throw new BatfishException("Failed to visit templates dir: " + questionsPath, e);
    }
  }

  public static Settings getSettings() {
    checkState(_settings != null, "Error: Coordinator settings have not been configured");
    return _settings;
  }

  public static WorkMgr getWorkMgr() {
    checkState(_workManager != null, "Error: Work Manager has not been configured");
    return _workManager;
  }

  public static void setLogger(BatfishLogger logger) {
    _logger = logger;
  }

  public static void setWorkMgr(WorkMgr workManager) {
    _workManager = workManager;
  }

  public static void setAuthorizer(Authorizer authorizer) {
    _authorizer = authorizer;
  }

  static void initAuthorizer() throws Exception {
    Settings settings = getSettings();
    Authorizer.Type type = settings.getAuthorizationType();
    switch (type) {
      case none:
        _authorizer = NoneAuthorizer.INSTANCE;
        break;
      case file:
        _authorizer = FileAuthorizer.createFromSettings(settings);
        break;
      case database:
        _authorizer = DbAuthorizer.createFromSettings(settings);
        break;
      default:
        System.err.print(
            "org.batfish.coordinator: Initialization failed. Unsupported authorizer type " + type);
        System.exit(1);
    }
    getLogger().infof("Using authorizer %s\n", _authorizer);
  }

  private static void initPoolManager(BindPortFutures bindPortFutures) {
    ResourceConfig rcPool =
        new ResourceConfig(PoolMgrService.class)
            .register(new JettisonFeature())
            .register(MultiPartFeature.class)
            .register(CrossDomainFilter.class);
    HttpServer server;
    if (_settings.getSslPoolDisable()) {
      URI poolMgrUri =
          UriBuilder.fromUri("http://" + _settings.getPoolBindHost())
              .port(_settings.getServicePoolPort())
              .build();

      _logger.infof("Starting pool manager at %s\n", poolMgrUri);

      server = GrizzlyHttpServerFactory.createHttpServer(poolMgrUri, rcPool);
    } else {
      URI poolMgrUri =
          UriBuilder.fromUri("https://" + _settings.getPoolBindHost())
              .port(_settings.getServicePoolPort())
              .build();

      _logger.infof("Starting pool manager at %s\n", poolMgrUri);

      server =
          CommonUtil.startSslServer(
              rcPool,
              poolMgrUri,
              _settings.getSslPoolKeystoreFile(),
              _settings.getSslPoolKeystorePassword(),
              _settings.getSslPoolTrustAllCerts(),
              _settings.getSslPoolTruststoreFile(),
              _settings.getSslPoolTruststorePassword(),
              ConfigurationLocator.class,
              Main.class);
    }

    _poolManager = new PoolMgr(_settings, _logger);
    _poolManager.startPoolManager();
    int selectedListenPort = server.getListeners().iterator().next().getPort();
    URI actualPoolMgrUri =
        UriBuilder.fromUri("http://" + _settings.getPoolBindHost())
            .port(selectedListenPort)
            .build();
    _logger.infof("Started pool manager at %s\n", actualPoolMgrUri);
    if (!bindPortFutures.getPoolPort().isDone()) {
      bindPortFutures.getPoolPort().complete(selectedListenPort);
    }
  }

  private static void startWorkManagerService(
      Class<?> serviceClass,
      List<Class<?>> features,
      int port,
      CompletableFuture<Integer> portFuture) {
    ResourceConfig rcWork =
        new ResourceConfig(serviceClass)
            .register(ExceptionMapper.class)
            .register(CrossDomainFilter.class);
    if (_settings.getTracingEnable()) {
      _logger.infof("Registering feature %s", ServerTracingDynamicFeature.class.getSimpleName());
      rcWork.register(ServerTracingDynamicFeature.class);
    }
    for (Class<?> feature : features) {
      _logger.infof("Registering feature %s", feature.getSimpleName());
      rcWork.register(feature);
    }

    HttpServer server;
    if (_settings.getSslWorkDisable()) {
      URI workMgrUri =
          UriBuilder.fromUri("http://" + _settings.getWorkBindHost()).port(port).build();

      _logger.infof("Starting work manager %s at %s\n", serviceClass, workMgrUri);

      server = GrizzlyHttpServerFactory.createHttpServer(workMgrUri, rcWork);
    } else {
      URI workMgrUri =
          UriBuilder.fromUri("https://" + _settings.getWorkBindHost()).port(port).build();

      _logger.infof("Starting work manager at %s\n", workMgrUri);
      server =
          CommonUtil.startSslServer(
              rcWork,
              workMgrUri,
              _settings.getSslWorkKeystoreFile(),
              _settings.getSslWorkKeystorePassword(),
              _settings.getSslWorkTrustAllCerts(),
              _settings.getSslWorkTruststoreFile(),
              _settings.getSslWorkTruststorePassword(),
              ConfigurationLocator.class,
              Main.class);
    }
    int selectedListenPort = server.getListeners().iterator().next().getPort();
    URI actualWorkMgrUri =
        UriBuilder.fromUri("http://" + _settings.getWorkBindHost())
            .port(selectedListenPort)
            .build();
    _logger.infof("Started work manager at %s\n", actualWorkMgrUri);
    if (!portFuture.isDone()) {
      portFuture.complete(selectedListenPort);
    }
  }

  private static void initTracer() {
    GlobalTracer.register(
        new Configuration(
                _settings.getServiceName(),
                new SamplerConfiguration(ConstSampler.TYPE, 1),
                new ReporterConfiguration(
                    false,
                    _settings.getTracingAgentHost(),
                    _settings.getTracingAgentPort(),
                    /* flush interval in ms */ 1000,
                    /* max buffered Spans */ 10000))
            .getTracer());
  }

  private static void initWorkManager(BindPortFutures bindPortFutures) {
    _workManager = new WorkMgr(_settings, _logger);
    _workManager.startWorkManager();
    // Initialize and start the work manager service using the legacy API and Jettison.
    startWorkManagerService(
        WorkMgrService.class,
        Lists.newArrayList(JettisonFeature.class, MultiPartFeature.class),
        _settings.getServiceWorkPort(),
        bindPortFutures.getWorkPort());
    // Initialize and start the work manager service using the v2 RESTful API and Jackson.

    startWorkManagerService(
        WorkMgrServiceV2.class,
        Lists.newArrayList(
            ServiceObjectMapper.class,
            JacksonFeature.class,
            ApiKeyAuthenticationFilter.class,
            VersionCompatibilityFilter.class),
        _settings.getServiceWorkV2Port(),
        bindPortFutures.getWorkV2Port());
  }

  public static void main(String[] args) {
    mainInit(args);
    _logger =
        new BatfishLogger(_settings.getLogLevel(), false, _settings.getLogFile(), false, true);
    mainRun(new BindPortFutures());
  }

  public static void main(String[] args, BatfishLogger logger, BindPortFutures portFutures) {
    mainInit(args);

    // Supply ports early if known before binding
    int configuredPoolPort = _settings.getServicePoolPort();
    if (configuredPoolPort > 0) {
      portFutures.getPoolPort().complete(configuredPoolPort);
    }
    int configuredWorkPort = _settings.getServiceWorkPort();
    if (configuredWorkPort > 0) {
      portFutures.getWorkPort().complete(configuredWorkPort);
    }
    int configuredWorkV2Port = _settings.getServiceWorkV2Port();
    if (configuredWorkV2Port > 0) {
      portFutures.getWorkV2Port().complete(configuredWorkV2Port);
    }

    _logger = logger;
    mainRun(portFutures);
  }

  public static void mainInit(String[] args) {
    _settings = null;
    try {
      _settings = new Settings(args);
      networkListenerLogger.setLevel(Level.WARNING);
      httpServerLogger.setLevel(Level.WARNING);
    } catch (Exception e) {
      System.err.print(
          "org.batfish.coordinator: Initialization failed: " + Throwables.getStackTraceAsString(e));
      System.exit(1);
    }
  }

  private static void mainRun(BindPortFutures portFutures) {
    try {
      initAuthorizer();
      initPoolManager(portFutures);
      if (_settings.getTracingEnable() && !GlobalTracer.isRegistered()) {
        initTracer();
      }
      initWorkManager(portFutures);
    } catch (Exception e) {
      System.err.println(
          "org.batfish.coordinator: Initialization of a helper failed: "
              + Throwables.getStackTraceAsString(e));
      System.exit(1);
    }

    // sleep indefinitely, in 10 minute chunks
    try {
      while (true) {
        Thread.sleep(10 * 60 * 1000); // 10 minutes
        _logger.info("Still alive .... waiting for work to show up\n");
      }
    } catch (Exception ex) {
      String stackTrace = Throwables.getStackTraceAsString(ex);
      System.err.println(stackTrace);
    }
  }
}
