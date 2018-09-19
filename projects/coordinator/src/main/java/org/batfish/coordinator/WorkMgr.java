package org.batfish.coordinator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toCollection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import io.opentracing.ActiveSpan;
import io.opentracing.References;
import io.opentracing.SpanContext;
import io.opentracing.util.GlobalTracer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.batfish.common.AnswerRowsOptions;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.BfConsts.TaskStatus;
import org.batfish.common.ColumnSortOption;
import org.batfish.common.Container;
import org.batfish.common.CoordConsts.WorkStatusCode;
import org.batfish.common.Pair;
import org.batfish.common.Task;
import org.batfish.common.Warnings;
import org.batfish.common.WorkItem;
import org.batfish.common.plugin.AbstractCoordinator;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.UnzipUtility;
import org.batfish.common.util.WorkItemBuilder;
import org.batfish.common.util.ZipUtility;
import org.batfish.coordinator.AnalysisMetadataMgr.AnalysisType;
import org.batfish.coordinator.WorkDetails.WorkType;
import org.batfish.coordinator.WorkQueueMgr.QueueType;
import org.batfish.coordinator.config.Settings;
import org.batfish.datamodel.AnalysisMetadata;
import org.batfish.datamodel.TestrigMetadata;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.AnswerMetadataUtil;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.datamodel.answers.AutocompleteSuggestion;
import org.batfish.datamodel.answers.AutocompleteSuggestion.CompletionType;
import org.batfish.datamodel.answers.Issue;
import org.batfish.datamodel.answers.MajorIssueConfig;
import org.batfish.datamodel.answers.MinorIssueConfig;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.pojo.Topology;
import org.batfish.datamodel.questions.BgpPropertySpecifier;
import org.batfish.datamodel.questions.InterfacePropertySpecifier;
import org.batfish.datamodel.questions.NamedStructureSpecifier;
import org.batfish.datamodel.questions.NodePropertySpecifier;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.OspfPropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.ExcludedRows;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.referencelibrary.ReferenceLibrary;
import org.batfish.role.NodeRolesData;
import org.batfish.storage.StorageProvider;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.uri.UriComponent;

public class WorkMgr extends AbstractCoordinator {

  static final class AssignWorkTask implements Runnable {
    @Override
    public void run() {
      Main.getWorkMgr().checkTasks();
      Main.getWorkMgr().assignWork();
    }
  }

  private static final Set<String> CONTAINER_FILENAMES = initContainerFilenames();

  private static final Set<String> ENV_FILENAMES = initEnvFilenames();

  private static final int MAX_SHOWN_TESTRIG_INFO_SUBDIR_ENTRIES = 10;

  private static Set<String> initContainerFilenames() {
    return ImmutableSet.of(
        BfConsts.RELPATH_REFERENCE_LIBRARY_PATH, BfConsts.RELPATH_NODE_ROLES_PATH);
  }

  private static Set<String> initEnvFilenames() {
    Set<String> envFilenames = new HashSet<>();
    envFilenames.add(BfConsts.RELPATH_NODE_BLACKLIST_FILE);
    envFilenames.add(BfConsts.RELPATH_INTERFACE_BLACKLIST_FILE);
    envFilenames.add(BfConsts.RELPATH_EDGE_BLACKLIST_FILE);
    envFilenames.add(BfConsts.RELPATH_ENVIRONMENT_BGP_TABLES);
    envFilenames.add(BfConsts.RELPATH_ENVIRONMENT_ROUTING_TABLES);
    envFilenames.add(BfConsts.RELPATH_EXTERNAL_BGP_ANNOUNCEMENTS);
    return envFilenames;
  }

  private final BatfishLogger _logger;

  private final Settings _settings;

  private WorkQueueMgr _workQueueMgr;

  private final StorageProvider _storage;

  public WorkMgr(Settings settings, BatfishLogger logger, @Nonnull StorageProvider storage) {
    super(false);
    _settings = settings;
    _storage = storage;
    _logger = logger;
    _workQueueMgr = new WorkQueueMgr(logger);
    loadPlugins();
  }

  @VisibleForTesting
  public @Nonnull StorageProvider getStorage() {
    return _storage;
  }

  private void assignWork() {

    try {
      QueuedWork work = _workQueueMgr.getWorkForAssignment();

      // get out if no work was found
      if (work == null) {
        // _logger.info("WM:AssignWork: No unassigned work\n");
        return;
      }

      String idleWorker = Main.getPoolMgr().getWorkerForAssignment();

      // get out if no idle worker was found, but release the work first
      if (idleWorker == null) {
        _workQueueMgr.markAssignmentFailure(work);

        _logger.info("WM:AssignWork: No idle worker\n");
        return;
      }

      assignWork(work, idleWorker);
    } catch (Exception e) {
      _logger.errorf("Got exception in assignWork: %s\n", Throwables.getStackTraceAsString(e));
    }
  }

  private void assignWork(QueuedWork work, String worker) {

    _logger.infof("WM:AssignWork: Trying to assign %s to %s\n", work, worker);

    boolean assignmentError = false;
    boolean assigned = false;

    Client client = null;
    SpanContext queueWorkSpan = work.getWorkItem().getSourceSpan();
    try (ActiveSpan assignWorkSpan =
        GlobalTracer.get()
            .buildSpan("Assign Work")
            .addReference(References.FOLLOWS_FROM, queueWorkSpan)
            .startActive()) {
      assert assignWorkSpan != null; // avoid unused warning
      // get the task and add other standard stuff
      JSONObject task = new JSONObject(work.getWorkItem().getRequestParams());
      task.put(BfConsts.ARG_CONTAINER, work.getWorkItem().getContainerName());
      task.put(
          BfConsts.ARG_STORAGE_BASE,
          Main.getSettings().getContainersLocation().toAbsolutePath().toString());
      task.put(BfConsts.ARG_TESTRIG, work.getWorkItem().getTestrigName());

      client =
          CommonUtil.createHttpClientBuilder(
                  _settings.getSslPoolDisable(),
                  _settings.getSslPoolTrustAllCerts(),
                  _settings.getSslPoolKeystoreFile(),
                  _settings.getSslPoolKeystorePassword(),
                  _settings.getSslPoolTruststoreFile(),
                  _settings.getSslPoolTruststorePassword(),
                  true)
              .build();

      String protocol = _settings.getSslPoolDisable() ? "http" : "https";
      WebTarget webTarget =
          client
              .target(
                  String.format(
                      "%s://%s%s/%s",
                      protocol, worker, BfConsts.SVC_BASE_RSC, BfConsts.SVC_RUN_TASK_RSC))
              .queryParam(
                  BfConsts.SVC_TASKID_KEY,
                  UriComponent.encode(
                      work.getId().toString(), UriComponent.Type.QUERY_PARAM_SPACE_ENCODED))
              .queryParam(
                  BfConsts.SVC_TASK_KEY,
                  UriComponent.encode(
                      task.toString(), UriComponent.Type.QUERY_PARAM_SPACE_ENCODED));

      Response response = webTarget.request(MediaType.APPLICATION_JSON).get();

      if (response.getStatus() != Response.Status.OK.getStatusCode()) {
        _logger.errorf("WM:AssignWork: Got non-OK response %s\n", response.getStatus());
      } else {
        String sobj = response.readEntity(String.class);
        JSONArray array = new JSONArray(sobj);
        _logger.info(
            String.format(
                "WM:AssignWork: response: %s [%s] [%s]\n",
                array.toString(), array.get(0), array.get(1)));

        if (!array.get(0).equals(BfConsts.SVC_SUCCESS_KEY)) {
          _logger.error(
              String.format("ERROR in assigning task: %s %s\n", array.get(0), array.get(1)));

          assignmentError = true;
        } else {
          assigned = true;
        }
      }
    } catch (ProcessingException e) {
      String stackTrace = Throwables.getStackTraceAsString(e);
      _logger.error(String.format("Unable to connect to worker at %s: %s\n", worker, stackTrace));
    } catch (Exception e) {
      String stackTrace = Throwables.getStackTraceAsString(e);
      _logger.error(String.format("Exception assigning work: %s\n", stackTrace));
    } finally {
      if (client != null) {
        client.close();
      }
    }

    if (work.getStatus() == WorkStatusCode.TERMINATEDBYUSER) {
      if (assigned) {
        killWork(work, worker);
      }
      return;
    }

    // mark the assignment results for both work and worker
    if (assignmentError) {
      try {
        _workQueueMgr.markAssignmentError(work);
      } catch (Exception e) {
        String stackTrace = Throwables.getStackTraceAsString(e);
        _logger.errorf("Unable to markAssignmentError for work %s: %s\n", work, stackTrace);
      }
    } else if (assigned) {
      try {
        _workQueueMgr.markAssignmentSuccess(work, worker);
      } catch (Exception e) {
        String stackTrace = Throwables.getStackTraceAsString(e);
        _logger.errorf("Unable to markAssignmentSuccess for work %s: %s\n", work, stackTrace);
      }

    } else {
      _workQueueMgr.markAssignmentFailure(work);
    }

    Main.getPoolMgr().markAssignmentResult(worker, assigned);
  }

  private void checkTasks() {
    try {
      List<QueuedWork> workToCheck = _workQueueMgr.getWorkForChecking();
      for (QueuedWork work : workToCheck) {
        String assignedWorker = work.getAssignedWorker();
        if (assignedWorker == null) {
          _logger.errorf("WM:CheckWork no assigned worker for %s\n", work);
          _workQueueMgr.makeWorkUnassigned(work);
          continue;
        }
        checkTask(work, assignedWorker);
      }
    } catch (Exception e) {
      _logger.errorf("Got exception in checkTasks: %s\n", Throwables.getStackTraceAsString(e));
    }
  }

  public List<AutocompleteSuggestion> autoComplete(
      String container,
      String testrig,
      CompletionType completionType,
      String query,
      int maxSuggestions)
      throws IOException {
    switch (completionType) {
      case BGP_PROPERTY:
        {
          List<AutocompleteSuggestion> suggestions = BgpPropertySpecifier.autoComplete(query);
          return suggestions.subList(0, Integer.min(suggestions.size(), maxSuggestions));
        }
      case INTERFACE_PROPERTY:
        {
          List<AutocompleteSuggestion> suggestions = InterfacePropertySpecifier.autoComplete(query);
          return suggestions.subList(0, Integer.min(suggestions.size(), maxSuggestions));
        }
      case NAMED_STRUCTURE:
        {
          List<AutocompleteSuggestion> suggestions = NamedStructureSpecifier.autoComplete(query);
          return suggestions.subList(0, Integer.min(suggestions.size(), maxSuggestions));
        }
      case NODE:
        {
          checkArgument(
              !isNullOrEmpty(testrig),
              "Snapshot name should be supplied for 'NODE' autoCompletion");
          List<AutocompleteSuggestion> suggestions =
              NodesSpecifier.autoComplete(
                  query, getNodes(container, testrig), getNodeRolesData(container));
          return suggestions.subList(0, Integer.min(suggestions.size(), maxSuggestions));
        }
      case NODE_PROPERTY:
        {
          List<AutocompleteSuggestion> suggestions = NodePropertySpecifier.autoComplete(query);
          return suggestions.subList(0, Integer.min(suggestions.size(), maxSuggestions));
        }
      case OSPF_PROPERTY:
        {
          List<AutocompleteSuggestion> suggestions = OspfPropertySpecifier.autoComplete(query);
          return suggestions.subList(0, Integer.min(suggestions.size(), maxSuggestions));
        }
      default:
        throw new UnsupportedOperationException("Unsupported completion type: " + completionType);
    }
  }

  private void checkTask(QueuedWork work, String worker) {
    _logger.infof("WM:CheckWork: Trying to check %s on %s\n", work, worker);

    Task task = new Task(TaskStatus.UnreachableOrBadResponse);

    Client client = null;
    SpanContext queueWorkSpan = work.getWorkItem().getSourceSpan();
    try (ActiveSpan checkTaskSpan =
        GlobalTracer.get()
            .buildSpan("Checking Task Status")
            .addReference(References.FOLLOWS_FROM, queueWorkSpan)
            .startActive()) {
      assert checkTaskSpan != null; // avoid unused warning
      client =
          CommonUtil.createHttpClientBuilder(
                  _settings.getSslPoolDisable(),
                  _settings.getSslPoolTrustAllCerts(),
                  _settings.getSslPoolKeystoreFile(),
                  _settings.getSslPoolKeystorePassword(),
                  _settings.getSslPoolTruststoreFile(),
                  _settings.getSslPoolTruststorePassword(),
                  true)
              .build();

      String protocol = _settings.getSslPoolDisable() ? "http" : "https";
      WebTarget webTarget =
          client
              .target(
                  String.format(
                      "%s://%s%s/%s",
                      protocol, worker, BfConsts.SVC_BASE_RSC, BfConsts.SVC_GET_TASKSTATUS_RSC))
              .queryParam(
                  BfConsts.SVC_TASKID_KEY,
                  UriComponent.encode(
                      work.getId().toString(), UriComponent.Type.QUERY_PARAM_SPACE_ENCODED));
      Response response = webTarget.request(MediaType.APPLICATION_JSON).get();

      if (response.getStatus() != Response.Status.OK.getStatusCode()) {
        _logger.errorf("WM:CheckTask: Got non-OK response %s\n", response.getStatus());
      } else {
        String sobj = response.readEntity(String.class);
        JSONArray array = new JSONArray(sobj);
        _logger.info(String.format("response: %s [%s] [%s]\n", array, array.get(0), array.get(1)));

        if (!array.get(0).equals(BfConsts.SVC_SUCCESS_KEY)) {
          _logger.error(
              String.format(
                  "got error while refreshing status: %s %s\n", array.get(0), array.get(1)));
        } else {
          String taskStr = array.get(1).toString();
          task = BatfishObjectMapper.mapper().readValue(taskStr, Task.class);
          if (task.getStatus() == null) {
            _logger.error("did not see status key in json response\n");
          }
        }
      }
    } catch (ProcessingException e) {
      String stackTrace = Throwables.getStackTraceAsString(e);
      _logger.error(String.format("unable to connect to %s: %s\n", worker, stackTrace));
    } catch (Exception e) {
      String stackTrace = Throwables.getStackTraceAsString(e);
      _logger.error(String.format("exception: %s\n", stackTrace));
    } finally {
      if (client != null) {
        client.close();
      }
    }

    if (work.getStatus() == WorkStatusCode.TERMINATEDBYUSER) {
      return;
    }

    try {
      _workQueueMgr.processTaskCheckResult(work, task);
    } catch (Exception e) {
      _logger.errorf("exception: %s\n", Throwables.getStackTraceAsString(e));
    }

    // if the task ended, send a hint to the pool manager to look up worker status
    if (task.getStatus().isTerminated()) {
      Main.getPoolMgr().refreshWorkerStatus(worker);
    }
  }

  @VisibleForTesting
  Path getQuestionPath(WorkItem workItem) {
    String qName = WorkItemBuilder.getQuestionName(workItem);
    if (qName == null) {
      throw new BatfishException("Question name not provided for ANSWER work");
    }
    String aName = WorkItemBuilder.getAnalysisName(workItem);
    if (aName == null) {
      // answering ad-hoc question
      return getpathContainerQuestion(workItem.getContainerName(), qName);
    } else {
      // answering question in policy
      return getpathAnalysisQuestion(workItem.getContainerName(), aName, qName);
    }
  }

  private WorkDetails computeWorkDetails(WorkItem workItem) {

    WorkType workType = WorkType.UNKNOWN;

    if (WorkItemBuilder.isParsingWorkItem(workItem)) {
      workType = WorkType.PARSING;
    }

    if (WorkItemBuilder.isDataplaningWorkItem(workItem)) {
      if (workType != WorkType.UNKNOWN) {
        throw new BatfishException("Cannot do composite work. Separate PARSING and DATAPLANING.");
      }
      workType = WorkType.DATAPLANING;
    }

    if (WorkItemBuilder.isAnsweringWorkItem(workItem)) {
      if (workType != WorkType.UNKNOWN) {
        throw new BatfishException("Cannot do composite work. Separate ANSWER from other work.");
      }
      Path qFile = getQuestionPath(workItem);
      Question question = Question.parseQuestion(qFile);
      workType =
          question.getIndependent()
              ? WorkType.INDEPENDENT_ANSWERING
              : question.getDataPlane()
                  ? WorkType.DATAPLANE_DEPENDENT_ANSWERING
                  : WorkType.PARSING_DEPENDENT_ANSWERING;
    }

    if (WorkItemBuilder.isAnalyzingWorkItem(workItem)) {
      if (workType != WorkType.UNKNOWN) {
        throw new BatfishException("Cannot do composite work. Separate ANALYZE from other work.");
      }
      String aName = WorkItemBuilder.getAnalysisName(workItem);
      if (aName == null) {
        throw new BatfishException("Analysis name not provided for ANALYZE work");
      }
      Set<String> qNames = listAnalysisQuestions(workItem.getContainerName(), aName);
      // compute the strongest dependency among the embedded questions
      workType = WorkType.INDEPENDENT_ANSWERING;
      for (String qName : qNames) {
        Path qFile = getpathAnalysisQuestion(workItem.getContainerName(), aName, qName);
        Question question = Question.parseQuestion(qFile);
        if (question.getDataPlane()) {
          workType = WorkType.DATAPLANE_DEPENDENT_ANSWERING;
          break;
        }
        if (!question.getIndependent()) {
          workType = WorkType.PARSING_DEPENDENT_ANSWERING;
        }
      }
    }

    Pair<Pair<String, String>, Pair<String, String>> settings =
        WorkItemBuilder.getBaseAndDeltaSettings(workItem);
    WorkDetails details =
        new WorkDetails(
            WorkItemBuilder.getBaseTestrig(settings),
            WorkItemBuilder.getBaseEnvironment(settings),
            WorkItemBuilder.getDeltaTestrig(settings),
            WorkItemBuilder.getDeltaEnvironment(settings),
            WorkItemBuilder.isDifferential(workItem),
            workType);

    return details;
  }

  /**
   * Create, update, or truncate an analysis with provided questions or and/or question names
   *
   * @param containerName The container in which the analysis resides
   * @param newAnalysis Whether or not to create a new analysis. Incompatible with {@code
   *     delQuestionsStr}.
   * @param aName The name of the analysis
   * @param questionsToAdd The questions to be added to or initially populate the analysis.
   * @param questionsToDelete A list of question names to be deleted from the analysis. Incompatible
   *     with {@code newAnalysis}.
   * @param suggested An optional Boolean indicating whether analysis is suggested (default: false).
   */
  public void configureAnalysis(
      String containerName,
      boolean newAnalysis,
      String aName,
      Map<String, String> questionsToAdd,
      List<String> questionsToDelete,
      @Nullable Boolean suggested) {
    Path containerDir = getdirNetwork(containerName);
    Path aDir = containerDir.resolve(BfConsts.RELPATH_ANALYSES_DIR).resolve(aName);

    this.configureAnalysisValidityCheck(
        containerName, newAnalysis, aName, questionsToAdd, questionsToDelete, aDir);

    if (newAnalysis) {
      aDir.toFile().mkdirs();
    }

    // Create metadata if it's a new analysis, or update it if suggested is not null
    if (newAnalysis || suggested != null) {
      AnalysisMetadata metadata;
      if (newAnalysis) {
        metadata = new AnalysisMetadata(Instant.now(), (suggested != null) && suggested);
      } else if (!Files.exists(getpathAnalysisMetadata(containerName, aName))) {
        // Configuring an old analysis with no metadata file; create one. Know suggested != null
        metadata = new AnalysisMetadata(Instant.MIN, suggested);
      } else {
        try {
          metadata = AnalysisMetadataMgr.readMetadata(containerName, aName);
          metadata.setSuggested(suggested);
        } catch (IOException e) {
          throw new BatfishException(
              "Unable to read metadata file for analysis '" + aName + "'", e);
        }
      }
      // Write metadata to file
      try {
        AnalysisMetadataMgr.writeMetadata(metadata, containerName, aName);
      } catch (JsonProcessingException e) {
        throw new BatfishException("Could not write analysisMetadata", e);
      }
    }

    /* Delete questionsToDelete and add questionsToAdd */
    Path questionsDir = aDir.resolve(BfConsts.RELPATH_QUESTIONS_DIR);
    for (String qName : questionsToDelete) {
      CommonUtil.deleteDirectory(questionsDir.resolve(qName));
    }
    for (Entry<String, String> entry : questionsToAdd.entrySet()) {
      questionsDir.resolve(entry.getKey()).toFile().mkdirs();
      Path qFile = questionsDir.resolve(Paths.get(entry.getKey(), BfConsts.RELPATH_QUESTION_FILE));
      CommonUtil.writeFile(qFile, entry.getValue());
    }
  }

  private void configureAnalysisValidityCheck(
      String containerName,
      boolean newAnalysis,
      String analysisName,
      Map<String, String> questionsToAdd,
      List<String> questionsToDelete,
      Path aDir) {
    // Reasons to throw error for a new analysis:
    // 1. Analysis with same name already exists
    // 2. questionsToDelete is not empty
    if (newAnalysis) {
      if (Files.exists(aDir)) {
        throw new BatfishException(
            String.format(
                "Analysis '%s' already exists for container '%s'", analysisName, containerName));
      } else if (!questionsToDelete.isEmpty()) {
        throw new BatfishException("Cannot delete questions from a new analysis");
      }
    } else {
      // Reasons to throw error for an existing analysis:
      // 1. Analysis directory does not exist
      // 2. questionsToDelete includes a question that doesn't exist in the analysis
      // 3. questionsToAdd includes a question that already exists and won't be deleted
      if (!Files.exists(aDir)) {
        throw new BatfishException(
            String.format(
                "Analysis '%s' does not exist for container '%s'", analysisName, containerName));
      }
      Path questionsDir = aDir.resolve(BfConsts.RELPATH_QUESTIONS_DIR);
      for (String qName : questionsToDelete) {
        Path qDir = questionsDir.resolve(qName);
        if (!Files.exists(qDir)) {
          throw new BatfishException(
              String.format("Question '%s' does not exist for analysis '%s'", qName, analysisName));
        }
      }
      for (Entry<String, String> entry : questionsToAdd.entrySet()) {
        if (!questionsToDelete.contains(entry.getKey())
            && Files.exists(questionsDir.resolve(entry.getKey()))) {
          throw new BatfishException(
              String.format(
                  "Question '%s' already exists for analysis '%s'", entry.getKey(), analysisName));
        }
      }
    }
  }

  public void delAnalysis(String containerName, String aName) {
    Path aDir = getdirContainerAnalysis(containerName, aName);
    CommonUtil.deleteDirectory(aDir);
  }

  public boolean delContainer(String containerName) {
    Path containerDir = getdirContainer(containerName, false);
    if (Files.exists(containerDir)) {
      CommonUtil.deleteDirectory(containerDir);
      return true;
    }
    return false;
  }

  public void delEnvironment(String containerName, String testrigName, String envName) {
    Path envDir = getdirEnvironment(containerName, testrigName, envName);
    CommonUtil.deleteDirectory(envDir);
  }

  public void delTestrig(String containerName, String testrigName) {
    Path testrigDir = getdirTestrig(containerName, testrigName);
    CommonUtil.deleteDirectory(testrigDir);
  }

  public void delQuestion(String containerName, String qName) {
    Path qDir = getdirContainerQuestion(containerName, qName);
    CommonUtil.deleteDirectory(qDir);
  }

  public String getAnswer(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws JsonProcessingException, FileNotFoundException {
    // Side effect: ensures metadata and answer are up to date
    getAnswerMetadata(network, snapshot, question, referenceSnapshot, analysis);

    String answer = "unknown";
    try {
      answer = _storage.loadAnswer(network, snapshot, question, referenceSnapshot, analysis);
      if (_storage
              .getQuestionLastModifiedTime(network, question, analysis)
              .compareTo(
                  _storage.getAnswerLastModifiedTime(
                      network, snapshot, question, referenceSnapshot, analysis))
          > 0) {
        Answer ans = Answer.failureAnswer("Not fresh", null);
        ans.setStatus(AnswerStatus.STALE);
        answer = BatfishObjectMapper.writePrettyString(ans);
      }
    } catch (FileNotFoundException e) {
      Answer ans = Answer.failureAnswer("Not answered", null);
      ans.setStatus(AnswerStatus.NOTFOUND);
      answer = BatfishObjectMapper.writePrettyString(ans);
    } catch (IOException e) {
      String message =
          String.format("Failed to read answer file:\n%s", Throwables.getStackTraceAsString(e));
      _logger.error(message);
      Answer ans = Answer.failureAnswer(message, null);
      ans.setStatus(AnswerStatus.FAILURE);
      answer = BatfishObjectMapper.writePrettyString(ans);
    }
    return answer;
  }

  public @Nonnull Map<String, String> getAnalysisAnswers(
      String network,
      String snapshot,
      String baseEnv,
      String referenceSnapshot,
      String deltaEnv,
      String analysis,
      Set<String> analysisQuestions)
      throws JsonProcessingException, FileNotFoundException {
    Set<String> questions =
        analysisQuestions.isEmpty() ? listAnalysisQuestions(network, analysis) : analysisQuestions;
    ImmutableSortedMap.Builder<String, String> result = ImmutableSortedMap.naturalOrder();
    for (String questionName : questions) {
      result.put(
          questionName, getAnswer(network, snapshot, questionName, referenceSnapshot, analysis));
    }
    return result.build();
  }

  public @Nonnull Map<String, AnswerMetadata> getAnalysisAnswersMetadata(
      String network,
      String snapshot,
      String referenceSnapshot,
      String analysis,
      Set<String> analysisQuestions)
      throws JsonProcessingException, FileNotFoundException {
    Set<String> questions =
        analysisQuestions.isEmpty() ? listAnalysisQuestions(network, analysis) : analysisQuestions;
    ImmutableSortedMap.Builder<String, AnswerMetadata> result = ImmutableSortedMap.naturalOrder();
    for (String question : questions) {
      result.put(
          question, getAnswerMetadata(network, snapshot, question, referenceSnapshot, analysis));
    }
    return result.build();
  }

  public String getAnalysisQuestion(
      String containerName, String analysisName, String questionName) {
    Path qFile = getpathAnalysisQuestion(containerName, analysisName, questionName);
    if (!Files.exists(qFile)) {
      throw new BatfishException("Question file not found for " + questionName);
    }
    return CommonUtil.readFile(qFile);
  }

  public @Nonnull AnswerMetadata getAnswerMetadata(
      @Nonnull String network,
      @Nonnull String snapshot,
      @Nonnull String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws JsonProcessingException, FileNotFoundException {
    if (!_storage.checkQuestionExists(network, question, analysis)) {
      throw new FileNotFoundException(
          String.format(
              "Question file not found for question named '%s' in network:%s; snapshot:%s; referenceSnapshot:%s; analysis:%s",
              question, network, snapshot, referenceSnapshot, analysis));
    }
    try {
      AnswerMetadata answerMetadata =
          _storage.loadAnswerMetadata(network, snapshot, question, referenceSnapshot, analysis);
      FileTime answerMetadataLastModifiedTime =
          _storage.getAnswerMetadataLastModifiedTime(
              network, snapshot, question, referenceSnapshot, analysis);
      if (_storage
              .getQuestionLastModifiedTime(network, question, analysis)
              .compareTo(answerMetadataLastModifiedTime)
          > 0) {
        return AnswerMetadata.forStatus(AnswerStatus.STALE);
      }
      return applyPendingIssuesSettingsChanges(
          answerMetadata, network, snapshot, question, referenceSnapshot, analysis);
    } catch (FileNotFoundException e) {
      return AnswerMetadata.forStatus(AnswerStatus.NOTFOUND);
    } catch (IOException e) {
      _logger.errorf(
          "Failed to read answer metadata file:\n%s", Throwables.getStackTraceAsString(e));
      return AnswerMetadata.forStatus(AnswerStatus.FAILURE);
    }
  }

  @VisibleForTesting
  @Nonnull
  AnswerMetadata applyPendingIssuesSettingsChanges(
      @Nonnull AnswerMetadata currentAnswerMetadata,
      @Nonnull String network,
      @Nonnull String snapshot,
      @Nonnull String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
    if (currentAnswerMetadata.getMetrics() == null) {
      // issues configuration not applicable to this answer, so nothing to do.
      return currentAnswerMetadata;
    }
    Map<String, MajorIssueConfig> answerMajorIssueConfigs =
        currentAnswerMetadata.getMetrics().getMajorIssueConfigs();
    Map<String, MajorIssueConfig> latestMajorIssueConfigs =
        _storage.loadMajorIssueConfigs(network, answerMajorIssueConfigs.keySet());
    if (answerIssueConfigsMatchConfiguredIssues(currentAnswerMetadata, latestMajorIssueConfigs)) {
      return currentAnswerMetadata;
    }
    return applyIssuesConfiguration(
        latestMajorIssueConfigs, network, snapshot, question, referenceSnapshot, analysis);
  }

  private boolean answerIssueConfigsMatchConfiguredIssues(
      AnswerMetadata answerMetadata, Map<String, MajorIssueConfig> configuredMajorIssues) {
    return answerMetadata
        .getMetrics()
        .getMajorIssueConfigs()
        .values()
        .stream()
        .allMatch(
            answerMajor ->
                answerIssueConfigMatchesConfiguredIssues(answerMajor, configuredMajorIssues));
  }

  @VisibleForTesting
  boolean answerIssueConfigMatchesConfiguredIssues(
      MajorIssueConfig answerIssueConfig, Map<String, MajorIssueConfig> configuredMajorIssues) {
    MajorIssueConfig configuredMajorIssue =
        configuredMajorIssues.get(answerIssueConfig.getMajorIssue());
    Map<String, MinorIssueConfig> answerMinorIssues = answerIssueConfig.getMinorIssueConfigsMap();
    Map<String, MinorIssueConfig> configuredMinorIssues =
        configuredMajorIssue.getMinorIssueConfigsMap();
    return Sets.intersection(answerMinorIssues.keySet(), configuredMinorIssues.keySet())
        .stream()
        .allMatch(minor -> answerMinorIssues.get(minor).equals(configuredMinorIssues.get(minor)));
  }

  @VisibleForTesting
  @Nonnull
  AnswerMetadata applyIssuesConfiguration(
      @Nonnull Map<String, MajorIssueConfig> majorIssueConfigs,
      @Nonnull String network,
      @Nonnull String snapshot,
      @Nonnull String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
    Answer oldAnswer =
        BatfishObjectMapper.mapper()
            .readValue(
                _storage.loadAnswer(network, snapshot, question, referenceSnapshot, analysis),
                new TypeReference<Answer>() {});
    TableAnswerElement oldTable = (TableAnswerElement) oldAnswer.getAnswerElements().get(0);
    TableMetadata tableMetadata = oldTable.getMetadata();
    Set<String> issueColumns =
        tableMetadata
            .getColumnMetadata()
            .stream()
            .filter(cm -> cm.getSchema().equals(Schema.ISSUE))
            .map(ColumnMetadata::getName)
            .collect(ImmutableSet.toImmutableSet());
    TableAnswerElement newTable = new TableAnswerElement(tableMetadata);
    applyIssuesConfigurationToRows(oldTable.getRowsList(), issueColumns, majorIssueConfigs)
        .forEach(newTable::addRow);
    applyIssuesConfigurationToAllExcludedRows(
            oldTable.getExcludedRows(), issueColumns, majorIssueConfigs)
        .forEach(
            excludedRows -> {
              String exclusionName = excludedRows.getExclusionName();
              excludedRows
                  .getRowsList()
                  .forEach(excludedRow -> newTable.addExcludedRow(excludedRow, exclusionName));
            });
    Answer newAnswer = new Answer();

    // Copy relevant portions from old answer and table
    newAnswer.setQuestion(oldAnswer.getQuestion());
    newAnswer.setStatus(AnswerStatus.SUCCESS);
    newAnswer.setSummary(oldAnswer.getSummary());
    newTable.setSummary(oldTable.getSummary());

    // Use new rows
    newAnswer.setAnswerElements(ImmutableList.of(newTable));

    // Compute and store new answer and answer metdata
    AnswerMetadata newAnswerMetadata = AnswerMetadataUtil.computeAnswerMetadata(newAnswer, _logger);
    String answerStr = BatfishObjectMapper.writePrettyString(newAnswer);
    _storage.storeAnswer(answerStr, network, snapshot, question, referenceSnapshot, analysis);
    _storage.storeAnswerMetadata(
        newAnswerMetadata, network, snapshot, question, referenceSnapshot, analysis);

    return newAnswerMetadata;
  }

  private Stream<ExcludedRows> applyIssuesConfigurationToAllExcludedRows(
      List<ExcludedRows> allExcludedRows,
      Set<String> issueColumns,
      Map<String, MajorIssueConfig> issueConfigs) {
    return allExcludedRows
        .stream()
        .map(
            excludedRows ->
                applyIssuesConfigurationToExcludedRows(excludedRows, issueColumns, issueConfigs));
  }

  private ExcludedRows applyIssuesConfigurationToExcludedRows(
      ExcludedRows oldExcludedRows,
      Set<String> issueColumns,
      Map<String, MajorIssueConfig> issueConfigs) {
    ExcludedRows newExcludedRows = new ExcludedRows(oldExcludedRows.getExclusionName());
    applyIssuesConfigurationToRows(oldExcludedRows.getRowsList(), issueColumns, issueConfigs)
        .forEach(newExcludedRows::addRow);
    return newExcludedRows;
  }

  private Stream<Row> applyIssuesConfigurationToRows(
      List<Row> rowsList, Set<String> issueColumns, Map<String, MajorIssueConfig> issueConfigs) {
    return rowsList
        .stream()
        .map(row -> applyRowIssuesConfiguration(row, issueColumns, issueConfigs));
  }

  @VisibleForTesting
  @Nonnull
  Row applyRowIssuesConfiguration(
      Row oldRow, Set<String> issueColumns, Map<String, MajorIssueConfig> issueConfigs) {
    Row.RowBuilder builder = Row.builder();
    oldRow
        .getColumnNames()
        .forEach(
            column -> {
              if (!issueColumns.contains(column)) {
                builder.put(column, oldRow.get(column));
                return;
              }
              Issue oldIssue = oldRow.getIssue(column);
              MajorIssueConfig config = issueConfigs.get(oldIssue.getType().getMajor());
              Issue newIssue;
              if (config == null) {
                newIssue = oldIssue;
              } else {
                String minorIssue = oldIssue.getType().getMinor();
                Optional<MinorIssueConfig> optionalMinorConfig =
                    config.getMinorIssueConfig(minorIssue);
                if (optionalMinorConfig.isPresent()) {
                  MinorIssueConfig minorConfig = optionalMinorConfig.get();
                  newIssue =
                      new Issue(
                          oldIssue.getExplanation(),
                          minorConfig.getSeverity(),
                          oldIssue.getType(),
                          minorConfig.getUrl());
                } else {
                  newIssue = oldIssue;
                }
              }
              builder.put(column, newIssue);
            });
    return builder.build();
  }

  /**
   * Returns a string representation of the content of configuration file {@code configName}.
   *
   * @throws BatfishException if the configuration file {@code configName} does not exist, or there
   *     are more than one file with name {@code configName}, or failed to read content from the
   *     file.
   */
  public String getConfiguration(String containerName, String testrigName, String configName) {
    Path testrigPath = getdirTestrig(containerName, testrigName);
    try (Stream<Path> paths = Files.walk(testrigPath.resolve(BfConsts.RELPATH_TEST_RIG_DIR))) {
      List<Path> configPaths =
          paths
              .filter(x -> x.getFileName().toString().equals(configName))
              .collect(Collectors.toList());
      if (configPaths.isEmpty()) {
        throw new BatfishException(
            String.format(
                "Configuration file %s does not exist in snapshot %s for network %s",
                configName, testrigName, containerName));
      } else if (configPaths.size() > 1) {
        throw new BatfishException(
            String.format(
                "More than one configuration file with name %s in snapshot %s for network %s",
                configName, testrigName, containerName));
      }
      String configContent = "";
      try {
        configContent = new String(Files.readAllBytes(configPaths.get(0)));
      } catch (IOException e) {
        throw new BatfishException(
            String.format(
                "Failed to read configuration file %s in snapshot %s for network %s",
                configName, testrigName, containerName),
            e);
      }
      return configContent;
    } catch (IOException e) {
      throw new BatfishException(
          String.format(
              "Failed to list directory %s", testrigPath.resolve(BfConsts.RELPATH_TEST_RIG_DIR)));
    }
  }

  /** Return a {@link Container container} contains all testrigs directories inside it. */
  public Container getContainer(String containerName) {
    return getContainer(getdirNetwork(containerName));
  }

  /** Return a {@link Container container} contains all testrigs directories inside it */
  public Container getContainer(Path containerDir) {
    SortedSet<String> testrigs =
        CommonUtil.getSubdirectories(containerDir.resolve(BfConsts.RELPATH_TESTRIGS_DIR))
            .stream()
            .map(dir -> dir.getFileName().toString())
            .collect(toCollection(TreeSet::new));

    return Container.of(containerDir.toFile().getName(), testrigs);
  }

  private Path getdirAnalysisQuestion(String containerName, String analysisName, String qName) {
    Path analysisDir = getdirContainerAnalysis(containerName, analysisName);
    Path qDir = analysisDir.resolve(Paths.get(BfConsts.RELPATH_QUESTIONS_DIR, qName));
    if (!Files.exists(qDir)) {
      throw new BatfishException(
          "Question '" + qName + "' does not exist in analysis '" + analysisName + "'");
    }
    return qDir;
  }

  @Override
  public Path getdirNetwork(String networkName) {
    return getdirContainer(networkName, true);
  }

  @Override
  public BatfishLogger getLogger() {
    return _logger;
  }

  @Override
  public Set<String> getNetworkNames() {
    Path containersDir = Main.getSettings().getContainersLocation();
    if (!Files.exists(containersDir)) {
      containersDir.toFile().mkdirs();
    }
    SortedSet<String> containers =
        CommonUtil.getSubdirectories(containersDir)
            .stream()
            .map(dir -> dir.getFileName().toString())
            .collect(toCollection(TreeSet::new));
    return containers;
  }

  private static Path getdirContainer(String containerName, boolean errIfNotEixst) {
    Path containerDir =
        Main.getSettings().getContainersLocation().resolve(containerName).toAbsolutePath();
    if (errIfNotEixst && !Files.exists(containerDir)) {
      throw new BatfishException("Container '" + containerName + "' does not exist");
    }
    return containerDir;
  }

  private Path getdirContainerAnalysis(String containerName, String analysisName) {
    Path containerDir = getdirNetwork(containerName);
    Path aDir = containerDir.resolve(Paths.get(BfConsts.RELPATH_ANALYSES_DIR, analysisName));
    if (!Files.exists(aDir)) {
      throw new BatfishException(
          "Analysis '" + analysisName + "' does not exists for container '" + containerName + "'");
    }
    return aDir;
  }

  private Path getdirContainerQuestion(String containerName, String qName) {
    Path containerDir = getdirNetwork(containerName);
    Path qDir = containerDir.resolve(Paths.get(BfConsts.RELPATH_QUESTIONS_DIR, qName));
    if (!Files.exists(qDir)) {
      throw new BatfishException("Question '" + qName + "' does not exist");
    }
    return qDir;
  }

  private Path getdirEnvironment(String containerName, String testrigName, String envName) {
    Path testrigDir = getdirTestrig(containerName, testrigName);
    Path envDir = testrigDir.resolve(Paths.get(BfConsts.RELPATH_ENVIRONMENTS_DIR, envName));
    if (!Files.exists(envDir)) {
      throw new BatfishException("Environment '" + envName + "' does not exist");
    }
    return envDir;
  }

  public Path getdirTestrig(String containerName, String testrigName) {
    Path snapshotDir = getdirSnapshots(containerName).resolve(Paths.get(testrigName));
    if (!Files.exists(snapshotDir)) {
      throw new BatfishException("Snapshot '" + testrigName + "' does not exist");
    }
    return snapshotDir;
  }

  @Override
  public Path getdirSnapshots(String networkName) {
    return getdirNetwork(networkName).resolve(Paths.get(BfConsts.RELPATH_TESTRIGS_DIR));
  }

  /** Fetches the {@code MajorIssueConfig} for the given network and major issue type. */
  public MajorIssueConfig getMajorIssueConfig(String networkName, String majorIssueType) {
    return _storage.loadMajorIssueConfig(networkName, majorIssueType);
  }

  /**
   * Returns the latest testrig in the container.
   *
   * @return An {@link Optional} object with the latest testrig or empty if no testrigs exist
   */
  public Optional<String> getLatestTestrig(String container) {
    Function<String, Instant> toTestrigTimestamp =
        t -> TestrigMetadataMgr.getTestrigCreationTimeOrMin(container, t);
    return listTestrigs(container)
        .stream()
        .max(
            Comparator.comparing(
                toTestrigTimestamp, Comparator.nullsFirst(Comparator.naturalOrder())));
  }

  /**
   * Gets the {@link NodeRolesData} for the {@code container}.
   *
   * @param container The container for which we should fetch the node roles
   * @return The node roles
   * @throws IOException The contents of node roles file cannot be converted to {@link
   *     NodeRolesData}
   */
  public NodeRolesData getNodeRolesData(String container) throws IOException {
    return NodeRolesData.read(getNodeRolesPath(container));
  }

  /** Gets the path of the node roles file */
  public Path getNodeRolesPath(String container) {
    return getdirNetwork(container).resolve(BfConsts.RELPATH_NODE_ROLES_PATH);
  }

  /**
   * Gets the set of nodes in this container and testrig. Extracts the set based on the topology
   * file that is generated as part of the testrig initialization.
   *
   * @param container The container
   * @param testrig The testrig
   * @return The set of nodes
   * @throws IOException If the contents of the topology file cannot be mapped to the topology
   *     object
   */
  public Set<String> getNodes(String container, String testrig) throws IOException {
    Path pojoTopologyPath =
        getdirTestrig(container, testrig).resolve(BfConsts.RELPATH_TESTRIG_POJO_TOPOLOGY_PATH);
    Topology topology =
        BatfishObjectMapper.mapper().readValue(pojoTopologyPath.toFile(), Topology.class);
    return topology.getNodes().stream().map(Node::getName).collect(Collectors.toSet());
  }

  public JSONObject getParsingResults(String containerName, String testrigName)
      throws JsonProcessingException, JSONException {

    ParseVendorConfigurationAnswerElement pvcae =
        deserializeObject(
            getdirTestrig(containerName, testrigName).resolve(BfConsts.RELPATH_PARSE_ANSWER_PATH),
            ParseVendorConfigurationAnswerElement.class);
    JSONObject warnings = new JSONObject();
    SortedMap<String, Warnings> warningsMap = pvcae.getWarnings();
    ObjectWriter writer = BatfishObjectMapper.prettyWriter();
    for (String s : warningsMap.keySet()) {
      warnings.put(s, writer.writeValueAsString(warningsMap.get(s)));
    }
    return warnings;
  }

  public Path getpathAnalysisQuestion(
      String containerName, String analysisName, String questionName) {
    Path questionDir = getdirAnalysisQuestion(containerName, analysisName, questionName);
    return questionDir.resolve(BfConsts.RELPATH_QUESTION_FILE);
  }

  public Path getpathContainerQuestion(String containerName, String questionName) {
    Path questionDir = getdirContainerQuestion(containerName, questionName);
    return questionDir.resolve(BfConsts.RELPATH_QUESTION_FILE);
  }

  // this function should build on others but some overrides are getting in the way
  // TODO: cleanup later
  public static Path getpathAnalysisMetadata(String container, String analysis) {
    return Main.getSettings()
        .getContainersLocation()
        .resolve(container)
        .resolve(BfConsts.RELPATH_ANALYSES_DIR)
        .resolve(analysis)
        .resolve(BfConsts.RELPATH_METADATA_FILE);
  }

  // this function should build on others but some overrides are getting in the way
  // TODO: cleanup later
  public static Path getpathTestrigMetadata(String container, String testrig) {
    return Main.getSettings()
        .getContainersLocation()
        .resolve(container)
        .resolve(BfConsts.RELPATH_TESTRIGS_DIR)
        .resolve(testrig)
        .resolve(BfConsts.RELPATH_METADATA_FILE);
  }

  /**
   * Gets the {@link ReferenceLibrary} for the {@code container}.
   *
   * @throws IOException The contents of reference library file cannot be converted to {@link
   *     ReferenceLibrary}
   */
  public ReferenceLibrary getReferenceLibrary(String container) throws IOException {
    return ReferenceLibrary.read(getReferenceLibraryPath(container));
  }

  /** Gets the path of the reference library file */
  public Path getReferenceLibraryPath(String container) {
    return getdirNetwork(container).resolve(BfConsts.RELPATH_REFERENCE_LIBRARY_PATH);
  }

  public JSONObject getStatusJson() throws JSONException {
    return _workQueueMgr.getStatusJson();
  }

  public String getTestrigInfo(String containerName, String testrigName) {
    Path testrigDir = getdirTestrig(containerName, testrigName);
    Path submittedTestrigDir = testrigDir.resolve(BfConsts.RELPATH_TEST_RIG_DIR);
    if (!Files.exists(submittedTestrigDir)) {
      return "Missing folder '"
          + BfConsts.RELPATH_TEST_RIG_DIR
          + "' for snapshot '"
          + testrigName
          + "'\n";
    }
    StringBuilder retStringBuilder = new StringBuilder();
    SortedSet<Path> entries = CommonUtil.getEntries(submittedTestrigDir);
    for (Path entry : entries) {
      retStringBuilder.append(entry.getFileName());
      if (Files.isDirectory(entry)) {
        String[] subdirEntryNames =
            CommonUtil.getEntries(entry)
                .stream()
                .map(subdirEntry -> subdirEntry.getFileName().toString())
                .collect(Collectors.toList())
                .toArray(new String[] {});
        retStringBuilder.append("/\n");
        // now append a maximum of MAX_SHOWN_TESTRIG_INFO_SUBDIR_ENTRIES
        for (int index = 0;
            index < subdirEntryNames.length && index < MAX_SHOWN_TESTRIG_INFO_SUBDIR_ENTRIES;
            index++) {
          retStringBuilder.append("  " + subdirEntryNames[index] + "\n");
        }
        if (subdirEntryNames.length > 10) {
          retStringBuilder.append(
              "  ...... "
                  + (subdirEntryNames.length - MAX_SHOWN_TESTRIG_INFO_SUBDIR_ENTRIES)
                  + " more entries\n");
        }
      } else {
        retStringBuilder.append("\n");
      }
    }
    return retStringBuilder.toString();
  }

  public TestrigMetadata getTestrigMetadata(String containerName, String testrigName)
      throws IOException {
    return TestrigMetadataMgr.readMetadata(getpathTestrigMetadata(containerName, testrigName));
  }

  @Nullable
  public Path getTestrigObject(String containerName, String testrigName, String objectName) {
    Path testrigDir = getdirTestrig(containerName, testrigName);
    Path file = testrigDir.resolve(objectName);
    /*
     * Check if we got an object name outside of the testrig folder, perhaps because of ".." in the
     * name; disallow it
     */
    if (!CommonUtil.getCanonicalPath(file).startsWith(CommonUtil.getCanonicalPath(testrigDir))) {
      throw new BatfishException("Illegal object name: '" + objectName + "'");
    }
    if (Files.isRegularFile(file)) {
      return file;
    } else if (Files.isDirectory(file)) {
      Path zipfile = Paths.get(file + ".zip");
      if (Files.exists(zipfile)) {
        CommonUtil.deleteIfExists(zipfile);
      }
      ZipUtility.zipFiles(file, zipfile);

      // TODO: delete the zipfile

      return zipfile;
    }

    return null;
  }

  public String getQuestion(String containerName, String questionName) {
    Path containerDir = getdirContainer(containerName, true);
    Path questionDir =
        containerDir.resolve(Paths.get(BfConsts.RELPATH_QUESTIONS_DIR, questionName));
    Path qFile = questionDir.resolve(BfConsts.RELPATH_QUESTION_FILE);
    if (!Files.exists(qFile)) {
      throw new BatfishException("Question file not found for " + questionName);
    }
    return CommonUtil.readFile(qFile);
  }

  public QueuedWork getMatchingWork(WorkItem workItem, QueueType qType) {
    return _workQueueMgr.getMatchingWork(workItem, qType);
  }

  public QueuedWork getWork(UUID workItemId) {
    return _workQueueMgr.getWork(workItemId);
  }

  public String initContainer(@Nullable String containerName, @Nullable String containerPrefix) {
    String newContainerName =
        isNullOrEmpty(containerName) ? containerPrefix + "_" + UUID.randomUUID() : containerName;
    Path containerDir = Main.getSettings().getContainersLocation().resolve(newContainerName);
    if (Files.exists(containerDir)) {
      throw new BatfishException("Container '" + newContainerName + "' already exists!");
    }
    if (!containerDir.toFile().mkdirs()) {
      throw new BatfishException("failed to create directory '" + containerDir + "'");
    }
    Path testrigsDir = containerDir.resolve(BfConsts.RELPATH_TESTRIGS_DIR);
    if (!testrigsDir.toFile().mkdir()) {
      throw new BatfishException("failed to create directory '" + testrigsDir + "'");
    }
    Path analysesDir = containerDir.resolve(BfConsts.RELPATH_ANALYSES_DIR);
    if (!analysesDir.toFile().mkdir()) {
      throw new BatfishException("failed to create directory '" + analysesDir + "'");
    }
    Path questionsDir = containerDir.resolve(BfConsts.RELPATH_QUESTIONS_DIR);
    if (!questionsDir.toFile().mkdir()) {
      throw new BatfishException("failed to create directory '" + questionsDir + "'");
    }
    return newContainerName;
  }

  @Override
  public void initSnapshot(
      String networkName, String snapshotName, Path srcDir, boolean autoAnalyze) {
    /*
     * Sanity check what we got:
     *    There should be just one top-level folder.
     */
    SortedSet<Path> srcDirEntries = CommonUtil.getEntries(srcDir);
    if (srcDirEntries.size() != 1 || !Files.isDirectory(srcDirEntries.iterator().next())) {
      throw new BatfishException(
          "Unexpected packaging of snapshot. There should be just one top-level folder");
    }

    Path srcSubdir = srcDirEntries.iterator().next();
    SortedSet<Path> subFileList = CommonUtil.getEntries(srcSubdir);

    Path containerDir = getdirNetwork(networkName);
    Path testrigDir = containerDir.resolve(Paths.get(BfConsts.RELPATH_TESTRIGS_DIR, snapshotName));

    if (!testrigDir.toFile().mkdirs()) {
      throw new BatfishException("Failed to create directory: '" + testrigDir + "'");
    }

    // Now that the directory exists, we must also create the metadata.
    try {
      TestrigMetadataMgr.writeMetadata(
          new TestrigMetadata(Instant.now(), BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME),
          testrigDir.resolve(BfConsts.RELPATH_METADATA_FILE));
    } catch (Exception e) {
      BatfishException metadataError = new BatfishException("Could not write testrigMetadata", e);
      try {
        CommonUtil.deleteDirectory(testrigDir);
      } catch (Exception inner) {
        metadataError.addSuppressed(inner);
      }
      throw metadataError;
    }

    Path srcTestrigDir = testrigDir.resolve(BfConsts.RELPATH_TEST_RIG_DIR);

    // create empty default environment
    Path defaultEnvironmentLeafDir =
        testrigDir.resolve(
            Paths.get(
                BfConsts.RELPATH_ENVIRONMENTS_DIR,
                BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME,
                BfConsts.RELPATH_ENV_DIR));
    defaultEnvironmentLeafDir.toFile().mkdirs();

    // things look ok, now make the move
    boolean routingTables = false;
    boolean bgpTables = false;
    boolean roleData = false;
    boolean referenceLibraryData = false;
    for (Path subFile : subFileList) {
      String name = subFile.getFileName().toString();
      if (isEnvFile(subFile)) {
        // copy environment level files to the environment directory
        if (name.equals(BfConsts.RELPATH_ENVIRONMENT_ROUTING_TABLES)) {
          routingTables = true;
        }
        if (name.equals(BfConsts.RELPATH_ENVIRONMENT_BGP_TABLES)) {
          bgpTables = true;
        }
        CommonUtil.copy(subFile, defaultEnvironmentLeafDir.resolve(subFile.getFileName()));
      } else if (isContainerFile(subFile)) {
        // derive and write the new container level file from the input
        if (name.equals(BfConsts.RELPATH_NODE_ROLES_PATH)) {
          roleData = true;
          try {
            NodeRolesData testrigData = NodeRolesData.read(subFile);
            Path nodeRolesPath = containerDir.resolve(BfConsts.RELPATH_NODE_ROLES_PATH);
            NodeRolesData.mergeNodeRoleDimensions(
                nodeRolesPath,
                testrigData.getNodeRoleDimensions(),
                testrigData.getDefaultDimension(),
                false);
          } catch (IOException e) {
            // lets not stop the upload because that file is busted.
            // TODO: figure out a way to surface this error to the user
            _logger.errorf("Could not process node role data: %s", e);
          }
        }
        if (name.equals(BfConsts.RELPATH_REFERENCE_LIBRARY_PATH)) {
          referenceLibraryData = true;
          try {
            ReferenceLibrary testrigData = ReferenceLibrary.read(subFile);
            Path path = containerDir.resolve(BfConsts.RELPATH_REFERENCE_LIBRARY_PATH);
            ReferenceLibrary.mergeReferenceBooks(path, testrigData.getReferenceBooks());
          } catch (IOException e) {
            // lets not stop the upload because that file is busted.
            // TODO: figure out a way to surface this error to the user
            _logger.errorf("Could not process reference library data: %s", e);
          }
        }
      } else {
        // rest is plain copy
        CommonUtil.copy(subFile, srcTestrigDir.resolve(subFile.getFileName()));
      }
    }
    _logger.infof(
        "Environment data for snapshot:%s; bgpTables:%s, routingTables:%s, nodeRoles:%s referenceBooks:%s\n",
        snapshotName, bgpTables, routingTables, roleData, referenceLibraryData);

    if (autoAnalyze) {
      for (WorkItem workItem : getAutoWorkQueue(networkName, snapshotName)) {
        boolean queued = queueWork(workItem);
        if (!queued) {
          _logger.errorf("Unable to queue work while auto processing: %s", workItem);
        }
      }
    }
  }

  List<WorkItem> getAutoWorkQueue(String containerName, String testrigName) {
    List<WorkItem> autoWorkQueue = new LinkedList<>();

    WorkItem parseWork = WorkItemBuilder.getWorkItemParse(containerName, testrigName);
    autoWorkQueue.add(parseWork);

    Set<String> analysisNames = listAnalyses(containerName, AnalysisType.ALL);
    for (String analysis : analysisNames) {
      WorkItem analyzeWork =
          WorkItemBuilder.getWorkItemRunAnalysis(
              analysis,
              containerName,
              testrigName,
              BfConsts.RELPATH_DEFAULT_ENVIRONMENT_NAME,
              null,
              null,
              false,
              false);
      autoWorkQueue.add(analyzeWork);
    }
    return autoWorkQueue;
  }

  private boolean isContainerFile(Path path) {
    String name = path.getFileName().toString();
    return CONTAINER_FILENAMES.contains(name);
  }

  private boolean isEnvFile(Path path) {
    String name = path.getFileName().toString();
    return ENV_FILENAMES.contains(name);
  }

  public boolean killWork(QueuedWork work) {
    String worker = work.getAssignedWorker();

    if (worker != null) {
      return killWork(work, worker);
    }

    // (worker = null) => this work was not assigned in the first place
    boolean killed = false;
    Task fakeTask = new Task(TaskStatus.TerminatedByUser, "Killed unassigned work");
    try {
      _workQueueMgr.processTaskCheckResult(work, fakeTask);
      killed = true;
    } catch (Exception e) {
      _logger.errorf("exception: %s\n", Throwables.getStackTraceAsString(e));
    }
    return killed;
  }

  private boolean killWork(QueuedWork work, String worker) {
    Client client = null;
    boolean killed = false;

    SpanContext queueWorkSpan = work.getWorkItem().getSourceSpan();
    try (ActiveSpan killTaskSpan =
        GlobalTracer.get()
            .buildSpan("Checking Task Status")
            .addReference(References.FOLLOWS_FROM, queueWorkSpan)
            .startActive()) {
      assert killTaskSpan != null; // avoid unused warning
      client =
          CommonUtil.createHttpClientBuilder(
                  _settings.getSslPoolDisable(),
                  _settings.getSslPoolTrustAllCerts(),
                  _settings.getSslPoolKeystoreFile(),
                  _settings.getSslPoolKeystorePassword(),
                  _settings.getSslPoolTruststoreFile(),
                  _settings.getSslPoolTruststorePassword(),
                  true)
              .build();

      String protocol = _settings.getSslPoolDisable() ? "http" : "https";
      WebTarget webTarget =
          client
              .target(
                  String.format(
                      "%s://%s%s/%s",
                      protocol, worker, BfConsts.SVC_BASE_RSC, BfConsts.SVC_KILL_TASK_RSC))
              .queryParam(
                  BfConsts.SVC_TASKID_KEY,
                  UriComponent.encode(
                      work.getId().toString(), UriComponent.Type.QUERY_PARAM_SPACE_ENCODED));
      Response response = webTarget.request(MediaType.APPLICATION_JSON).get();

      if (response.getStatus() != Response.Status.OK.getStatusCode()) {
        _logger.errorf("WM:KillTask: Got non-OK response %s\n", response.getStatus());
      } else {
        try {
          String sobj = response.readEntity(String.class);
          JSONArray array = new JSONArray(sobj);
          _logger.infof("response: %s [%s] [%s]\n", array, array.get(0), array.get(1));
          if (!array.get(0).equals(BfConsts.SVC_SUCCESS_KEY)) {
            _logger.errorf("Got error while killing task: %s %s\n", array.get(0), array.get(1));
          } else {
            Task task = BatfishObjectMapper.mapper().readValue(array.getString(1), Task.class);
            _workQueueMgr.processTaskCheckResult(work, task);
            killed = true;
          }
        } catch (IllegalStateException e) {
          // can happen if the worker dies before we could finish reading; let's assume success
          _logger.infof("worker appears dead before response completion\n");
          Task fakeTask =
              new Task(TaskStatus.TerminatedByUser, "worker appears dead before responding");
          _workQueueMgr.processTaskCheckResult(work, fakeTask);
          killed = true;
        }
      }
    } catch (ProcessingException e) {
      _logger.errorf("unable to connect to %s: %s\n", worker, Throwables.getStackTraceAsString(e));
    } catch (Exception e) {
      _logger.errorf("exception: %s\n", Throwables.getStackTraceAsString(e));
    } finally {
      if (client != null) {
        client.close();
      }
    }
    return killed;
  }

  /**
   * Returns the Analysis names which exist in the container and match the {@link AnalysisType}
   *
   * @param containerName Container name
   * @param analysisType {@link AnalysisType} requested
   * @return {@link Set} of container names
   */
  public SortedSet<String> listAnalyses(String containerName, AnalysisType analysisType) {
    Path containerDir = getdirNetwork(containerName);
    Path analysesDir = containerDir.resolve(BfConsts.RELPATH_ANALYSES_DIR);
    if (!Files.exists(analysesDir)) {
      return ImmutableSortedSet.of();
    }
    SortedSet<String> analyses =
        CommonUtil.getSubdirectories(analysesDir)
            .stream()
            .map(subdir -> subdir.getFileName().toString())
            .filter(aName -> selectAnalysis(aName, analysisType, containerName))
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
    return analyses;
  }

  private boolean selectAnalysis(String aName, AnalysisType analysisType, String containerName) {
    if (analysisType == AnalysisType.ALL) {
      return true;
    }
    boolean suggested = AnalysisMetadataMgr.getAnalysisSuggestedOrFalse(containerName, aName);
    return (analysisType == AnalysisType.SUGGESTED && suggested
        || analysisType == AnalysisType.USER && !suggested);
  }

  public SortedSet<String> listAnalysisQuestions(String containerName, String analysisName) {
    Path analysisDir = getdirContainerAnalysis(containerName, analysisName);
    Path questionsDir = analysisDir.resolve(BfConsts.RELPATH_QUESTIONS_DIR);
    if (!Files.exists(questionsDir)) {
      /* TODO: Something better than returning empty set? */
      return new TreeSet<>();
    }
    SortedSet<Path> subdirectories = CommonUtil.getSubdirectories(questionsDir);
    SortedSet<String> subdirectoryNames =
        subdirectories
            .stream()
            .map(path -> path.getFileName().toString())
            .collect(toCollection(TreeSet::new));
    return subdirectoryNames;
  }

  public SortedSet<String> listContainers(String apiKey) {
    Path containersDir = Main.getSettings().getContainersLocation();
    if (!Files.exists(containersDir)) {
      containersDir.toFile().mkdirs();
    }
    SortedSet<String> authorizedContainers =
        CommonUtil.getSubdirectories(containersDir)
            .stream()
            .map(dir -> dir.getFileName().toString())
            .filter(
                container -> Main.getAuthorizer().isAccessibleContainer(apiKey, container, false))
            .collect(toCollection(TreeSet::new));
    return authorizedContainers;
  }

  public List<Container> getContainers(@Nullable String apiKey) {
    return listContainers(apiKey).stream().map(this::getContainer).collect(Collectors.toList());
  }

  public SortedSet<String> listEnvironments(String containerName, String testrigName) {
    Path testrigDir = getdirTestrig(containerName, testrigName);
    Path environmentsDir = testrigDir.resolve(BfConsts.RELPATH_ENVIRONMENTS_DIR);
    if (!Files.exists(environmentsDir)) {
      return new TreeSet<>();
    }
    SortedSet<String> environments =
        CommonUtil.getSubdirectories(environmentsDir)
            .stream()
            .map(dir -> dir.getFileName().toString())
            .collect(toCollection(TreeSet::new));
    return environments;
  }

  public List<QueuedWork> listIncompleteWork(
      String containerName, @Nullable String testrigName, @Nullable WorkType workType) {
    return _workQueueMgr.listIncompleteWork(containerName, testrigName, workType);
  }

  public SortedSet<String> listQuestions(String containerName, boolean verbose) {
    Path containerDir = getdirNetwork(containerName);
    Path questionsDir = containerDir.resolve(BfConsts.RELPATH_QUESTIONS_DIR);
    if (!Files.exists(questionsDir)) {
      return new TreeSet<>();
    }
    SortedSet<String> questions =
        CommonUtil.getSubdirectories(questionsDir)
            .stream()
            .map(dir -> dir.getFileName().toString())
            // Question dirs starting with __ are internal questions
            // and should not show up in listQuestions
            .filter(dir -> verbose || !dir.startsWith("__"))
            .collect(toCollection(TreeSet::new));
    return questions;
  }

  public List<String> listTestrigs(String containerName) {
    Path containerDir = getdirNetwork(containerName);
    Path testrigsDir = containerDir.resolve(BfConsts.RELPATH_TESTRIGS_DIR);
    if (!Files.exists(testrigsDir)) {
      return new ArrayList<>();
    }
    List<String> testrigs =
        CommonUtil.getSubdirectories(testrigsDir)
            .stream()
            .map(dir -> dir.getFileName().toString())
            .sorted(
                (t1, t2) -> { // reverse sorting by creation-time, name
                  String key1 =
                      TestrigMetadataMgr.getTestrigCreationTimeOrMin(containerName, t1) + t1;
                  String key2 =
                      TestrigMetadataMgr.getTestrigCreationTimeOrMin(containerName, t2) + t2;
                  return key2.compareTo(key1);
                })
            .collect(Collectors.toList());
    return testrigs;
  }

  /** Writes the {@code MajorIssueConfig} for the given network and major issue type. */
  public void putMajorIssueConfig(
      String networkName, String majorIssueType, MajorIssueConfig config) throws IOException {
    _storage.storeMajorIssueConfig(networkName, majorIssueType, config);
  }

  public void putObject(
      String containerName, String testrigName, String objectName, InputStream fileStream) {
    Path testrigDir = getdirTestrig(containerName, testrigName);
    Path file = testrigDir.resolve(objectName);
    // check if we got an object name outside of the testrig folder,
    // perhaps because of ".." in the name; disallow it
    if (!CommonUtil.getCanonicalPath(file).startsWith(CommonUtil.getCanonicalPath(testrigDir))) {
      throw new BatfishException("Illegal object name: '" + objectName + "'");
    }
    Path parentFolder = file.getParent();
    if (!Files.exists(parentFolder)) {
      if (!parentFolder.toFile().mkdirs()) {
        throw new BatfishException("Failed to create directory: '" + parentFolder + "'");
      }
    } else {
      if (!Files.isDirectory(parentFolder)) {
        throw new BatfishException(parentFolder + " already exists but is not a folder");
      }
    }
    CommonUtil.writeStreamToFile(fileStream, file);
  }

  public boolean queueWork(WorkItem workItem) {
    Path testrigDir = getdirTestrig(workItem.getContainerName(), workItem.getTestrigName());
    if (workItem.getTestrigName().isEmpty() || !Files.exists(testrigDir)) {
      throw new BatfishException("Non-existent snapshot: '" + testrigDir.getFileName() + "'");
    }
    boolean success;
    try {
      workItem.setSourceSpan(GlobalTracer.get().activeSpan());
      WorkDetails workDetails = computeWorkDetails(workItem);
      if (TestrigMetadataMgr.getEnvironmentMetadata(
              workItem.getContainerName(), workDetails.baseTestrig, workDetails.baseEnv)
          == null) {
        throw new BatfishException(
            String.format(
                "Snapshot/environment metadata not found for %s/%s",
                workDetails.baseTestrig, workDetails.baseEnv));
      }
      if (workDetails.isDifferential
          && TestrigMetadataMgr.getEnvironmentMetadata(
                  workItem.getContainerName(), workDetails.deltaTestrig, workDetails.deltaEnv)
              == null) {
        throw new BatfishException(
            String.format(
                "Snapshot/environment metadata not found for %s/%s",
                workDetails.deltaTestrig, workDetails.deltaEnv));
      }
      success = _workQueueMgr.queueUnassignedWork(new QueuedWork(workItem, workDetails));
    } catch (Exception e) {
      throw new BatfishException(String.format("Failed to queue work: %s", e.getMessage()), e);
    }
    // as an optimization trigger AssignWork to see if we can schedule this (or another) work
    if (success) {
      Thread thread = new Thread(this::assignWork);
      thread.start();
    }
    return success;
  }

  public void startWorkManager() {
    // for some bizarre reason, this ordering of scheduling checktask before
    // assignwork, is important
    // in the other order, assignwork never fires
    // TODO: track this down
    // _checkWorkTask = new CheckTaskTask();
    // _checkService = Executors.newScheduledThreadPool(1);
    // _checkFuture = _checkService.scheduleAtFixedRate(_checkWorkTask, 0,
    // Main.getSettings().getPeriodCheckWorkMs(),
    // TimeUnit.MILLISECONDS);

    Executors.newScheduledThreadPool(1)
        .scheduleAtFixedRate(
            new AssignWorkTask(),
            0,
            Main.getSettings().getPeriodAssignWorkMs(),
            TimeUnit.MILLISECONDS);
  }

  public int syncTestrigsSyncNow(String containerName, String pluginId, boolean force) {
    if (!_testrigSyncers.containsKey(pluginId)) {
      throw new BatfishException(
          "PluginId " + pluginId + " not found." + " (Are SyncTestrigs plugins loaded?)");
    }
    return _testrigSyncers.get(pluginId).syncNow(containerName, force);
  }

  public boolean syncTestrigsUpdateSettings(
      String containerName, String pluginId, Map<String, String> settings) {
    if (!_testrigSyncers.containsKey(pluginId)) {
      throw new BatfishException(
          "PluginId " + pluginId + " not found." + " (Are SyncTestrigs plugins loaded?)");
    }
    return _testrigSyncers.get(pluginId).updateSettings(containerName, settings);
  }

  /**
   * Upload a new environment to an existing testrig.
   *
   * @param containerName The container in which the testrig resides
   * @param testrigName The testrig in which the (optional base environment and) new environment
   *     reside
   * @param baseEnvName The name of an optional base environment. The new environment is initialized
   *     with files from this base if it is provided.
   * @param newEnvName The name of the new environment to be created
   * @param fileStream A stream providing the zip file containing the file structure of the new
   *     environment.
   */
  public void uploadEnvironment(
      String containerName,
      String testrigName,
      String baseEnvName,
      String newEnvName,
      InputStream fileStream) {
    Path testrigDir = getdirTestrig(containerName, testrigName);
    Path environmentsDir = testrigDir.resolve(BfConsts.RELPATH_ENVIRONMENTS_DIR);
    Path newEnvDir = environmentsDir.resolve(newEnvName);
    Path dstDir = newEnvDir.resolve(BfConsts.RELPATH_ENV_DIR);
    if (Files.exists(newEnvDir)) {
      throw new BatfishException(
          "Environment: '" + newEnvName + "' already exists for snapshot: '" + testrigName + "'");
    }
    if (!dstDir.toFile().mkdirs()) {
      throw new BatfishException("Failed to create directory: '" + dstDir + "'");
    }
    Path zipFile = CommonUtil.createTempFile("coord_up_env_", ".zip");
    CommonUtil.writeStreamToFile(fileStream, zipFile);

    /* First copy base environment if it is set */
    if (baseEnvName.length() > 0) {
      Path baseEnvPath = environmentsDir.resolve(Paths.get(baseEnvName, BfConsts.RELPATH_ENV_DIR));
      if (!Files.exists(baseEnvPath)) {
        CommonUtil.delete(zipFile);
        throw new BatfishException(
            "Base environment for copy does not exist: '" + baseEnvName + "'");
      }
      SortedSet<Path> baseFileList = CommonUtil.getEntries(baseEnvPath);
      dstDir.toFile().mkdirs();
      for (Path baseFile : baseFileList) {
        Path target;
        if (isEnvFile(baseFile)) {
          target = dstDir.resolve(baseFile.getFileName());
          CommonUtil.copy(baseFile, target);
        }
      }
    }

    // now unzip
    Path unzipDir = CommonUtil.createTempDirectory("coord_up_env_unzip_dir_");
    UnzipUtility.unzip(zipFile, unzipDir);

    /*-
     *  Sanity check what we got:
     *    There should be just one top-level folder
     */
    SortedSet<Path> unzipDirEntries = CommonUtil.getEntries(unzipDir);
    if (unzipDirEntries.size() != 1 || !Files.isDirectory(unzipDirEntries.iterator().next())) {
      CommonUtil.deleteDirectory(newEnvDir);
      CommonUtil.deleteDirectory(unzipDir);
      throw new BatfishException(
          "Unexpected packaging of environment. There should be just one top-level folder");
    }
    Path unzipSubdir = unzipDirEntries.iterator().next();
    SortedSet<Path> subFileList = CommonUtil.getEntries(unzipSubdir);

    // things look ok, now make the move
    for (Path subdirFile : subFileList) {
      Path target = dstDir.resolve(subdirFile.getFileName());
      CommonUtil.moveByCopy(subdirFile, target);
    }

    try {
      TestrigMetadataMgr.initializeEnvironment(containerName, testrigName, newEnvName);
    } catch (IOException e) {
      throw new BatfishException("Could not initialize environmentMetadata", e);
    }

    // delete the empty directory and the zip file
    CommonUtil.deleteDirectory(unzipDir);
    CommonUtil.deleteIfExists(zipFile);
  }

  public void uploadQuestion(String network, String question, String questionJson) {
    uploadQuestion(network, question, questionJson, true);
  }

  @VisibleForTesting
  void uploadQuestion(String network, String question, String questionJson, boolean validate) {
    if (validate) {
      // Validate the question before saving it to disk.
      try {
        Question.parseQuestion(questionJson);
      } catch (Exception e) {
        throw new BatfishException(
            String.format("Invalid question %s/%s: %s", network, question, e.getMessage()), e);
      }
    }
    if (_storage.checkQuestionExists(network, question, null)) {
      throw new BatfishException(
          "Question: '" + question + "' already exists in container '" + network + "'");
    }
    _storage.storeQuestion(questionJson, network, question, null);
  }

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss.SSS")
          .withLocale(Locale.getDefault())
          .withZone(ZoneOffset.UTC);

  static String generateFileDateString(String base, Instant instant) {
    return base + "_" + FORMATTER.format(instant);
  }

  private static String generateFileDateString(String base) {
    return generateFileDateString(base, Instant.now());
  }

  /**
   * Upload a new snapshot to the specified network.
   *
   * @param networkName Name of the network to upload the snapshot to.
   * @param snapshotName Name of the new snapshot.
   * @param fileStream {@link InputStream} of the snapshot zip.
   * @param autoAnalyze Boolean determining if the snapshot analysis should be triggered on upload.
   */
  public void uploadSnapshot(
      String networkName, String snapshotName, InputStream fileStream, boolean autoAnalyze) {
    Path networkDir = getdirNetwork(networkName);

    // Fail early if the snapshot already exists
    if (Files.exists(networkDir.resolve(Paths.get(BfConsts.RELPATH_TESTRIGS_DIR, snapshotName)))) {
      throw new BatfishException("Snapshot with name: '" + snapshotName + "' already exists");
    }

    // Save uploaded zip for troubleshooting
    Path originalDir =
        networkDir
            .resolve(BfConsts.RELPATH_ORIGINAL_DIR)
            .resolve(generateFileDateString(snapshotName));
    if (!originalDir.toFile().mkdirs()) {
      throw new BatfishException("Failed to create directory: '" + originalDir + "'");
    }
    Path snapshotZipFile = originalDir.resolve(BfConsts.RELPATH_SNAPSHOT_ZIP_FILE);
    CommonUtil.writeStreamToFile(fileStream, snapshotZipFile);
    Path unzipDir = CommonUtil.createTempDirectory("tr");
    UnzipUtility.unzip(snapshotZipFile, unzipDir);

    try {
      initSnapshot(networkName, snapshotName, unzipDir, autoAnalyze);
    } catch (Exception e) {
      throw new BatfishException("Error initializing snapshot", e);
    } finally {
      CommonUtil.deleteDirectory(unzipDir);
    }
  }

  public boolean checkContainerExists(String containerName) {
    return _storage.checkNetworkExists(containerName);
  }

  /**
   * Filter and sort {@code rawAnswers} according to options specified in {@code
   * analysisAnswersOptions}
   */
  public Map<String, Answer> processAnalysisAnswers(
      Map<String, String> rawAnswers, Map<String, AnswerRowsOptions> answersRowsOptions) {
    return CommonUtil.toImmutableMap(
        rawAnswers,
        Entry::getKey,
        rawAnswersEntry ->
            processAnswerRows(
                rawAnswersEntry.getValue(), answersRowsOptions.get(rawAnswersEntry.getKey())));
  }

  @VisibleForTesting
  @Nonnull
  Answer processAnswerRows(String rawAnswerStr, AnswerRowsOptions options) {
    if (rawAnswerStr == null) {
      Answer answer = Answer.failureAnswer("Not found", null);
      answer.setStatus(AnswerStatus.NOTFOUND);
      return answer;
    }
    try {
      Answer rawAnswer =
          BatfishObjectMapper.mapper().readValue(rawAnswerStr, new TypeReference<Answer>() {});
      TableAnswerElement rawTable = (TableAnswerElement) rawAnswer.getAnswerElements().get(0);
      Answer answer = new Answer();
      answer.setStatus(rawAnswer.getStatus());
      answer.addAnswerElement(processAnswerTable(rawTable, options));
      return answer;
    } catch (Exception e) {
      _logger.errorf("Failed to convert answer string to Answer: %s", e.getMessage());
      return Answer.failureAnswer(e.getMessage(), null);
    }
  }

  @VisibleForTesting
  @Nonnull
  TableAnswerElement processAnswerTable(TableAnswerElement rawTable, AnswerRowsOptions options) {
    Map<String, ColumnMetadata> rawColumnMap = rawTable.getMetadata().toColumnMap();
    List<Row> filteredRows =
        rawTable
            .getRowsList()
            .stream()
            .filter(row -> options.getFilters().stream().allMatch(filter -> filter.matches(row)))
            .collect(ImmutableList.toImmutableList());

    Stream<Row> sortedStream =
        options.getSortOrder().isEmpty()
            ? filteredRows.stream()
            : filteredRows.stream().sorted(buildComparator(rawColumnMap, options.getSortOrder()));
    Stream<Row> projectedStream;
    TableAnswerElement table;
    if (options.getColumns().isEmpty()) {
      projectedStream = sortedStream;
      table = new TableAnswerElement(rawTable.getMetadata());
    } else {
      projectedStream =
          sortedStream.map(rawRow -> Row.builder().putAll(rawRow, options.getColumns()).build());
      Map<String, ColumnMetadata> columnMap = new LinkedHashMap<>(rawColumnMap);
      columnMap.keySet().retainAll(options.getColumns());
      List<ColumnMetadata> columnMetadata =
          columnMap.values().stream().collect(ImmutableList.toImmutableList());
      table =
          new TableAnswerElement(
              new TableMetadata(columnMetadata, rawTable.getMetadata().getTextDesc()));
    }
    Stream<Row> uniquifiedStream = projectedStream;
    if (options.getUniqueRows()) {
      uniquifiedStream = uniquifiedStream.distinct();
    }
    Stream<Row> truncatedStream =
        uniquifiedStream.skip(options.getRowOffset()).limit(options.getMaxRows());
    truncatedStream.forEach(table::addRow);
    table.setSummary(rawTable.getSummary() != null ? rawTable.getSummary() : new AnswerSummary());
    table.getSummary().setNumResults(filteredRows.size());
    return table;
  }

  @VisibleForTesting
  @Nonnull
  Comparator<Row> buildComparator(
      Map<String, ColumnMetadata> rawColumnMap, List<ColumnSortOption> sortOrder) {
    ColumnSortOption firstColumnSortOption = sortOrder.get(0);
    ColumnMetadata firstMetadata = rawColumnMap.get(firstColumnSortOption.getColumn());
    Comparator<Row> comparator = columnComparator(firstMetadata);
    if (firstColumnSortOption.getReversed()) {
      comparator = comparator.reversed();
    }
    for (int i = 1; i < sortOrder.size(); i++) {
      ColumnSortOption columnSortOption = sortOrder.get(i);
      Comparator<Row> nextComparator =
          columnComparator(rawColumnMap.get(columnSortOption.getColumn()));
      if (columnSortOption.getReversed()) {
        nextComparator = nextComparator.reversed();
      }
      comparator = comparator.thenComparing(nextComparator);
    }
    return comparator;
  }

  @VisibleForTesting
  Comparator<Row> columnComparator(ColumnMetadata columnMetadata) {
    Schema schema = columnMetadata.getSchema();
    String column = columnMetadata.getName();
    if (schema.equals(Schema.INTEGER)) {
      return Comparator.comparing(r -> r.getInteger(column));
    } else if (schema.equals(Schema.ISSUE)) {
      return Comparator.comparing(r -> r.getIssue(column).getSeverity());
    } else if (schema.equals(Schema.STRING)) {
      return Comparator.comparing(r -> r.getString(column));
    } else {
      String message = String.format("Unsupported Schema for sorting: %s", schema);
      _logger.error(message);
      throw new UnsupportedOperationException(message);
    }
  }

  /**
   * Return the JSON-serialized settings for the specified question class for the specified network;
   * or {@code null} if either no custom settings exist for the question or no value is present at
   * the path produced from the sepcified components.
   *
   * @param network The name of the network
   * @param questionClass The fully-qualified class name of the question
   * @param components The components to traverse from the root of the question settings to reach
   *     the desired section or value
   * @throws IOException if there is an error reading the settings
   */
  public @Nullable String getQuestionSettings(
      String network, String questionClass, List<String> components) throws IOException {
    String questionSettings;
    questionSettings = _storage.loadQuestionSettings(network, questionClass);
    if (questionSettings == null) {
      return null;
    }
    if (components.isEmpty()) {
      return questionSettings;
    }
    JsonNode root = BatfishObjectMapper.mapper().readTree(questionSettings);
    JsonNode current = root;
    for (String component : components) {
      if (!current.has(component)) {
        return null;
      }
      current = current.get(component);
    }
    return BatfishObjectMapper.writeString(current);
  }

  /**
   * Write the JSON settings for the specified question class for the specified network at the end
   * of the path computed from the specified components. Any absent components will be created.
   *
   * @param network The name of the network
   * @param questionClass The fully-qualified class name of the question
   * @param components The components to traverse from the root of the question settings to reach
   *     the desired section or value
   * @param value The settings value to write at the end of the path
   * @throws IOException if there is an error writing the settings
   */
  public synchronized void writeQuestionSettings(
      String network, String questionClass, List<String> components, JsonNode value)
      throws IOException {
    String questionSettings;
    questionSettings = _storage.loadQuestionSettings(network, questionClass);
    JsonNodeFactory factory = BatfishObjectMapper.mapper().getNodeFactory();
    JsonNode root;
    if (!components.isEmpty()) {
      root =
          questionSettings != null
              ? (ObjectNode) BatfishObjectMapper.mapper().readTree(questionSettings)
              : new ObjectNode(factory);
      ObjectNode current = (ObjectNode) root;
      for (String component : components.subList(0, components.size() - 1)) {
        if (!current.has(component)) {
          current.set(component, new ObjectNode(factory));
        }
        current = (ObjectNode) current.get(component);
      }
      current.set(components.get(components.size() - 1), value);
    } else {
      root = value;
    }
    _storage.storeQuestionSettings(
        BatfishObjectMapper.writePrettyString(root), network, questionClass);
  }
}
