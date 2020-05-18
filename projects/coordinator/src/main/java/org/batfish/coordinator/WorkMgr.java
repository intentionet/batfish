package org.batfish.coordinator;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Comparators.lexicographical;
import static com.google.common.io.MoreFiles.createParentDirectories;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.MustBeClosed;
import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.util.GlobalTracer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.batfish.common.AnswerRowsOptions;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts;
import org.batfish.common.BfConsts.TaskStatus;
import org.batfish.common.ColumnSortOption;
import org.batfish.common.CompletionMetadata;
import org.batfish.common.Container;
import org.batfish.common.CoordConsts.WorkStatusCode;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Task;
import org.batfish.common.WorkItem;
import org.batfish.common.plugin.AbstractCoordinator;
import org.batfish.common.runtime.SnapshotRuntimeData;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CollectionUtil;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.UnzipUtility;
import org.batfish.common.util.WorkItemBuilder;
import org.batfish.coordinator.AnalysisMetadataMgr.AnalysisType;
import org.batfish.coordinator.WorkDetails.WorkType;
import org.batfish.coordinator.WorkQueueMgr.QueueType;
import org.batfish.coordinator.config.Settings;
import org.batfish.coordinator.id.IdManager;
import org.batfish.coordinator.resources.ForkSnapshotBean;
import org.batfish.datamodel.AnalysisMetadata;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.SnapshotMetadata;
import org.batfish.datamodel.SnapshotMetadataEntry;
import org.batfish.datamodel.acl.AclTrace;
import org.batfish.datamodel.acl.TraceEvent;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.AnswerMetadata;
import org.batfish.datamodel.answers.AnswerMetadataUtil;
import org.batfish.datamodel.answers.AnswerStatus;
import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.datamodel.answers.AutoCompleteUtils;
import org.batfish.datamodel.answers.AutocompleteSuggestion;
import org.batfish.datamodel.answers.InputValidationNotes;
import org.batfish.datamodel.answers.InputValidationUtils;
import org.batfish.datamodel.answers.Issue;
import org.batfish.datamodel.answers.MajorIssueConfig;
import org.batfish.datamodel.answers.Metrics;
import org.batfish.datamodel.answers.MinorIssueConfig;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.answers.Schema.Type;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.Hop;
import org.batfish.datamodel.flow.Step;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.pojo.Topology;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.questions.Variable;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.ExcludedRows;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.datamodel.table.TableView;
import org.batfish.datamodel.table.TableViewRow;
import org.batfish.identifiers.AnalysisId;
import org.batfish.identifiers.AnswerId;
import org.batfish.identifiers.IssueSettingsId;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.NodeRolesId;
import org.batfish.identifiers.QuestionId;
import org.batfish.identifiers.QuestionSettingsId;
import org.batfish.identifiers.SnapshotId;
import org.batfish.referencelibrary.ReferenceLibrary;
import org.batfish.role.NodeRolesData;
import org.batfish.storage.StorageProvider;
import org.batfish.storage.StoredObjectMetadata;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.uri.UriComponent;

public class WorkMgr extends AbstractCoordinator {

  private static final Set<String> IGNORED_PATHS =
      ImmutableSet.<String>builder()
          .add(".DS_STORE")
          .add("__MACOSX")
          .add(".git")
          .add(".svn")
          .build();

  private static final Comparator<AclTrace> COMPARATOR_ACL_TRACE =
      Comparator.comparing(
          AclTrace::getEvents,
          Comparators.lexicographical(Comparator.comparing(TraceEvent::getDescription)));

  private static final Comparator<Node> COMPARATOR_NODE = Comparator.comparing(Node::getName);

  private static final Comparator<Trace> COMPARATOR_TRACE =
      Comparator.comparing(Trace::getDisposition)
          .thenComparing(
              Trace::getHops,
              Comparators.lexicographical(
                  Comparator.comparing(Hop::getNode, Comparator.comparing(Node::getName))
                      .thenComparing(
                          Hop::getSteps,
                          Comparators.lexicographical(
                              Comparator.<Step<?>, String>comparing(
                                      step -> step.getDetail().toString())
                                  .thenComparing(Step::getAction)))));

  private static Path getCanonicalPath(Path path) {
    try {
      return Paths.get(path.toFile().getCanonicalPath());
    } catch (IOException e) {
      throw new BatfishException("Could not get canonical path from: '" + path + "'", e);
    }
  }

  private static SortedSet<Path> getEntries(Path directory) {
    SortedSet<Path> entries = new TreeSet<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      for (Path entry : stream) {
        entries.add(entry);
      }
    } catch (IOException | DirectoryIteratorException e) {
      throw new BatfishException("Error listing directory '" + directory + "'", e);
    }
    return entries;
  }

  private static Path createTempDirectory(String prefix, FileAttribute<?>... attrs) {
    try {
      Path tempDir = Files.createTempDirectory(prefix, attrs);
      tempDir.toFile().deleteOnExit();
      return tempDir;
    } catch (IOException e) {
      throw new BatfishException("Failed to create temporary directory", e);
    }
  }

  private static void deleteDirectory(Path path) {
    try {
      FileUtils.deleteDirectory(path.toFile());
    } catch (IOException | NullPointerException e) {
      throw new BatfishException("Could not delete directory: " + path, e);
    }
  }

  static final class AssignWorkTask implements Runnable {
    @Override
    public void run() {
      Main.getWorkMgr().checkTasks();
      Main.getWorkMgr().assignWork();
    }
  }

  private static final Set<String> WELL_KNOWN_NETWORK_FILENAMES =
      ImmutableSet.of(BfConsts.RELPATH_REFERENCE_LIBRARY_PATH, BfConsts.RELPATH_NODE_ROLES_PATH);

  private static final String SNAPSHOT_PACKAGING_INSTRUCTIONS_URL =
      "https://github.com/batfish/batfish/wiki/Packaging-snapshots-for-analysis";

  private final IdManager _idManager;
  private final BatfishLogger _logger;
  private final Settings _settings;
  private final SnapshotMetadataMgr _snapshotMetadataManager;
  private WorkQueueMgr _workQueueMgr;
  private final StorageProvider _storage;

  public WorkMgr(
      Settings settings,
      BatfishLogger logger,
      @Nonnull IdManager idManager,
      @Nonnull StorageProvider storage) {
    _settings = settings;
    _idManager = idManager;
    _storage = storage;
    _snapshotMetadataManager = new SnapshotMetadataMgr(_storage);
    _logger = logger;
    _workQueueMgr = new WorkQueueMgr(logger, _snapshotMetadataManager);
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
    Span span =
        GlobalTracer.get()
            .buildSpan("Assign work")
            .addReference(References.FOLLOWS_FROM, queueWorkSpan)
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      // get the task and add other standard stuff
      JSONObject task = new JSONObject(work.resolveRequestParams());
      task.put(
          BfConsts.ARG_STORAGE_BASE,
          Main.getSettings().getContainersLocation().toAbsolutePath().toString());

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

      JSONArray array;
      try (Response response = webTarget.request(MediaType.APPLICATION_JSON).get()) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
          _logger.errorf("WM:AssignWork: Got non-OK response %s\n", response.getStatus());
          return;
        }
        String sobj = response.readEntity(String.class);
        array = new JSONArray(sobj);
      }
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
        Span postAssignmentSpan =
            GlobalTracer.get().buildSpan("Post-assignment task operations").start();
        try (Scope postAssignmentScope =
            GlobalTracer.get().scopeManager().activate(postAssignmentSpan)) {
          assert postAssignmentScope != null;
          work.setPostAssignmentContext(postAssignmentSpan.context());
        } finally {
          postAssignmentSpan.finish();
        }
      }
    } catch (ProcessingException e) {
      String stackTrace = Throwables.getStackTraceAsString(e);
      _logger.error(String.format("Unable to connect to worker at %s: %s\n", worker, stackTrace));
    } catch (Exception e) {
      String stackTrace = Throwables.getStackTraceAsString(e);
      _logger.error(String.format("Exception assigning work: %s\n", stackTrace));
    } finally {
      span.finish();
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

  private CompletionMetadata getCompletionMetadata(String network, String snapshot)
      throws IOException {
    checkArgument(!isNullOrEmpty(network), "Network name should be supplied");
    checkArgument(!isNullOrEmpty(snapshot), "Snapshot name should be supplied");

    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);

    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    return _storage.loadCompletionMetadata(networkId, snapshotId);
  }

  @Nullable
  public List<AutocompleteSuggestion> autoComplete(
      String network,
      String snapshot,
      Variable.Type completionType,
      String query,
      int maxSuggestions)
      throws IOException {

    return AutoCompleteUtils.autoComplete(
        network,
        snapshot,
        completionType,
        query,
        maxSuggestions,
        getCompletionMetadata(network, snapshot),
        getNetworkNodeRoles(network),
        getReferenceLibrary(network));
  }

  private void checkTask(QueuedWork work, String worker) {
    _logger.infof("WM:CheckWork: Trying to check %s on %s\n", work, worker);

    Task task = new Task(TaskStatus.UnreachableOrBadResponse);

    Client client = null;
    Span span =
        GlobalTracer.get()
            .buildSpan("Checking Task Status")
            .addReference(References.FOLLOWS_FROM, work.getPostAssignmentContext())
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
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

      JSONArray array;
      try (Response response = webTarget.request(MediaType.APPLICATION_JSON).get()) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
          _logger.errorf("WM:CheckTask: Got non-OK response %s\n", response.getStatus());
          return;
        }
        String sobj = response.readEntity(String.class);
        array = new JSONArray(sobj);
      }
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
    } catch (ProcessingException e) {
      String stackTrace = Throwables.getStackTraceAsString(e);
      _logger.error(String.format("unable to connect to %s: %s\n", worker, stackTrace));
    } catch (Exception e) {
      String stackTrace = Throwables.getStackTraceAsString(e);
      _logger.error(String.format("exception: %s\n", stackTrace));
    } finally {
      span.finish();
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

  WorkDetails computeWorkDetails(WorkItem workItem) throws IOException {
    String referenceSnapshotName = WorkItemBuilder.getReferenceSnapshotName(workItem);
    String questionName = WorkItemBuilder.getQuestionName(workItem);
    String analysisName = WorkItemBuilder.getAnalysisName(workItem);

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
      Question question =
          Question.parseQuestion(getQuestion(workItem.getNetwork(), questionName, analysisName));
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
      if (analysisName == null) {
        throw new BatfishException("Analysis name not provided for ANALYZE work");
      }
      Set<String> qNames = listAnalysisQuestions(workItem.getNetwork(), analysisName);
      // compute the strongest dependency among the embedded questions
      workType = WorkType.INDEPENDENT_ANSWERING;
      for (String qName : qNames) {
        Question question =
            Question.parseQuestion(getQuestion(workItem.getNetwork(), qName, analysisName));
        if (question.getDataPlane()) {
          workType = WorkType.DATAPLANE_DEPENDENT_ANSWERING;
          break;
        }
        if (!question.getIndependent()) {
          workType = WorkType.PARSING_DEPENDENT_ANSWERING;
        }
      }
    }

    NetworkId networkId = _idManager.getNetworkId(workItem.getNetwork());
    WorkDetails.Builder builder =
        WorkDetails.builder()
            .setNetworkId(networkId)
            .setSnapshotId(_idManager.getSnapshotId(workItem.getSnapshot(), networkId))
            .setWorkType(workType)
            .setIsDifferential(WorkItemBuilder.isDifferential(workItem));
    if (referenceSnapshotName != null) {
      builder.setReferenceSnapshotId(_idManager.getSnapshotId(referenceSnapshotName, networkId));
    }
    AnalysisId analysisId = null;
    if (analysisName != null) {
      analysisId = _idManager.getAnalysisId(analysisName, networkId);
      builder.setAnalysisId(analysisId);
    }
    if (questionName != null) {
      builder.setQuestionId(_idManager.getQuestionId(questionName, networkId, analysisId));
    }
    return builder.build();
  }

  /**
   * Create, update, or truncate an analysis with provided questions or and/or question names.
   *
   * @param network The network in which the analysis resides
   * @param newAnalysis Whether or not to create a new analysis. Incompatible with {@code
   *     delQuestionsStr}.
   * @param analysis The name of the analysis
   * @param questionsToAdd The questions to be added to or initially populate the analysis.
   * @param questionsToDelete A list of question names to be deleted from the analysis. Incompatible
   *     with {@code newAnalysis}.
   * @param suggested An optional Boolean indicating whether analysis is suggested (default: false).
   * @throws IllegalArgumentException if network does not exist; or if {@code newAnalysis} is {@code
   *     false} and analysis does not exist; or if {@code newAnalysis} is {@code true} and analysis
   *     already exists; or if a question to delete does not exist; or if a question to add already
   *     exists.
   * @throws IOException if there is some other error configuration the analysis
   */
  public void configureAnalysis(
      String network,
      boolean newAnalysis,
      String analysis,
      Map<String, String> questionsToAdd,
      List<String> questionsToDelete,
      @Nullable Boolean suggested)
      throws IOException {
    NetworkId networkId = _idManager.getNetworkId(network);
    configureAnalysisValidityCheck(
        network, newAnalysis, analysis, questionsToAdd, questionsToDelete);
    AnalysisId analysisId =
        newAnalysis
            ? _idManager.generateAnalysisId()
            : _idManager.getAnalysisId(analysis, networkId);

    // Create metadata if it's a new analysis, or update it if suggested is not null
    if (newAnalysis || suggested != null) {
      AnalysisMetadata metadata;
      if (newAnalysis) {
        metadata = new AnalysisMetadata(Instant.now(), (suggested != null) && suggested);
      } else if (!_storage.hasAnalysisMetadata(
          networkId, _idManager.getAnalysisId(analysis, networkId))) {
        // Configuring an old analysis with no metadata file; create one. Know suggested != null
        metadata = new AnalysisMetadata(Instant.MIN, suggested);
      } else {
        try {
          metadata = AnalysisMetadataMgr.readMetadata(networkId, analysisId);
          metadata.setSuggested(suggested);
        } catch (IOException e) {
          throw new BatfishException(
              "Unable to read metadata file for analysis '" + analysis + "'", e);
        }
      }
      // Write metadata to file
      try {
        AnalysisMetadataMgr.writeMetadata(metadata, networkId, analysisId);
      } catch (IOException e) {
        throw new BatfishException("Could not write analysisMetadata", e);
      }
    }

    /* Delete questionsToDelete and add questionsToAdd */
    for (String qName : questionsToDelete) {
      _idManager.deleteQuestion(qName, networkId, analysisId);
    }
    for (Entry<String, String> entry : questionsToAdd.entrySet()) {
      String qName = entry.getKey();
      String qText = entry.getValue();
      QuestionId questionId = _idManager.generateQuestionId();
      _storage.storeQuestion(qText, networkId, questionId, analysisId);
      _idManager.assignQuestion(qName, networkId, questionId, analysisId);
    }
    if (newAnalysis) {
      _idManager.assignAnalysis(analysis, networkId, analysisId);
    }
  }

  /**
   * Checks validity of configureAnalysis call.
   *
   * @throws IllegalArgumentException if network does not exist; or if {@code newAnalysis} is {@code
   *     false} and analysis does not exist; or if {@code newAnalysis} is {@code true} and analysis
   *     already exists; or if a question to delete does not exist; or if a question to add already
   *     exists.
   */
  private void configureAnalysisValidityCheck(
      String networkName,
      boolean newAnalysis,
      String analysisName,
      Map<String, String> questionsToAdd,
      List<String> questionsToDelete) {
    NetworkId networkId = _idManager.getNetworkId(networkName);
    // Reasons to throw error for a new analysis:
    // 1. Analysis with same name already exists
    // 2. questionsToDelete is not empty
    if (newAnalysis) {
      if (_idManager.hasAnalysisId(analysisName, networkId)) {
        throw new IllegalArgumentException(
            String.format(
                "Analysis '%s' already exists for network '%s'", analysisName, networkName));
      } else if (!questionsToDelete.isEmpty()) {
        throw new IllegalArgumentException("Cannot delete questions from a new analysis");
      }
    } else {
      // Reasons to throw error for an existing analysis:
      // 1. Analysis directory does not exist
      // 2. questionsToDelete includes a question that doesn't exist in the analysis
      // 3. questionsToAdd includes a question that already exists and won't be deleted
      if (!_idManager.hasAnalysisId(analysisName, networkId)) {
        throw new IllegalArgumentException(
            String.format(
                "Analysis '%s' does not exist for network '%s'", analysisName, networkName));
      }
      AnalysisId analysisId = _idManager.getAnalysisId(analysisName, networkId);
      for (String qName : questionsToDelete) {
        if (!_idManager.hasQuestionId(qName, networkId, analysisId)) {
          throw new IllegalArgumentException(
              String.format("Question '%s' does not exist for analysis '%s'", qName, analysisName));
        }
      }
      for (Entry<String, String> entry : questionsToAdd.entrySet()) {
        String qName = entry.getKey();
        if (!questionsToDelete.contains(qName)
            && _idManager.hasQuestionId(qName, networkId, analysisId)) {
          throw new IllegalArgumentException(
              String.format(
                  "Question '%s' already exists for analysis '%s'", entry.getKey(), analysisName));
        }
      }
    }
  }

  /**
   * Delete the specified analysis under the specified network. Returns {@code true} if deletion is
   * successful. Returns {@code false} if either network or analysis does not exist.
   */
  public boolean delAnalysis(@Nonnull String network, @Nonnull String analysis) {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasAnalysisId(analysis, networkId)) {
      return false;
    }
    _idManager.deleteAnalysis(analysis, networkId);
    return true;
  }

  /**
   * Delete the specified network. Returns {@code true} if deletion is successful. Returns {@code
   * false} if network does not exist.
   */
  public boolean delNetwork(@Nonnull String network) {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    _idManager.deleteNetwork(network);
    return true;
  }

  /**
   * Delete the specified snapshot under the specified network. Returns {@code true} if deletion is
   * successful. Returns {@code false} if either network or snapshot does not exist.
   */
  public boolean delSnapshot(@Nonnull String network, @Nonnull String snapshot) {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return false;
    }
    _idManager.deleteSnapshot(snapshot, networkId);
    return true;
  }

  /**
   * Delete the specified question under the specified network and analysis. If {@code analysis} is
   * {@code null}, deletes an ad-hoc question. Returns {@code true} if deletion is successful.
   * Returns {@code false} if network, (non-null) analysis, or question does not exist.
   */
  public boolean delQuestion(
      @Nonnull String network, @Nonnull String question, @Nullable String analysis) {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    AnalysisId analysisId;
    if (analysis != null) {
      if (!_idManager.hasAnalysisId(analysis, networkId)) {
        return false;
      }
      analysisId = _idManager.getAnalysisId(analysis, networkId);
    } else {
      analysisId = null;
    }
    if (!_idManager.hasQuestionId(question, networkId, analysisId)) {
      return false;
    }
    _idManager.deleteQuestion(question, networkId, analysisId);
    return true;
  }

  /**
   * Get the answer for the specified question.
   *
   * @throws IllegalArgumentException if the network, question, analysis, or snapshots cannot be
   *     found
   * @throws IOException if there are any other errors
   */
  public @Nullable Answer getAnswer(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws IOException {
    String ansString = loadAnswer(network, snapshot, question, referenceSnapshot, analysis);
    return ansString == null
        ? null
        : BatfishObjectMapper.mapper().readValue(ansString, Answer.class);
  }

  /** Get the answer string for the specified question. */
  public @Nonnull String getAnswerString(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws JsonProcessingException {
    try {
      String answer = loadAnswer(network, snapshot, question, referenceSnapshot, analysis);
      if (answer == null) {
        Answer ans = Answer.failureAnswer("Not answered", null);
        ans.setStatus(AnswerStatus.NOTFOUND);
        return BatfishObjectMapper.writeString(ans);
      }
      return answer;
    } catch (IOException e) {
      String message =
          String.format(
              "Could not get answer: network=%s, snapshot=%s, question=%s, referenceSnapshot=%s, analysis=%s: %s",
              network,
              snapshot,
              question,
              referenceSnapshot,
              analysis,
              Throwables.getStackTraceAsString(e));
      Answer ans = Answer.failureAnswer(message, null);
      ans.setStatus(AnswerStatus.FAILURE);
      return BatfishObjectMapper.writeString(ans);
    }
  }

  /**
   * Get the answer string for the specified question. Returns {@code null} if the question is not
   * answered.
   */
  private @Nullable String loadAnswer(
      String network,
      String snapshot,
      String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis)
      throws IOException {
    NetworkId networkId = _idManager.getNetworkId(network);
    AnalysisId analysisId = analysis != null ? _idManager.getAnalysisId(analysis, networkId) : null;
    QuestionId questionId = _idManager.getQuestionId(question, networkId, analysisId);
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    SnapshotId referenceSnapshotId =
        referenceSnapshot != null ? _idManager.getSnapshotId(referenceSnapshot, networkId) : null;
    QuestionSettingsId questionSettingsId =
        getOrDefaultQuestionSettingsId(networkId, questionId, analysisId);
    NodeRolesId networkNodeRolesId = getOrDefaultNodeRolesId(networkId);
    AnswerId baseAnswerId =
        _idManager.getBaseAnswerId(
            networkId,
            snapshotId,
            questionId,
            questionSettingsId,
            networkNodeRolesId,
            referenceSnapshotId,
            analysisId);
    // No metadata means the question has not been answered
    if (!_storage.hasAnswerMetadata(baseAnswerId)) {
      return null;
    }
    AnswerMetadata baseAnswerMetadata = _storage.loadAnswerMetadata(baseAnswerId);
    AnswerId finalAnswerId =
        computeFinalAnswerAndId(
            baseAnswerMetadata,
            networkId,
            snapshotId,
            questionId,
            baseAnswerId,
            referenceSnapshotId,
            analysisId);
    return _storage.loadAnswer(finalAnswerId);
  }

  /**
   * Get all completed work for the specified network and snapshot.
   *
   * @param networkName name of the network to get completed work for.
   * @param snapshotName name of the snapshot to get completed work for.
   * @return {@link List} of completed {@link QueuedWork}.
   */
  public List<QueuedWork> getCompletedWork(String networkName, String snapshotName) {
    NetworkId networkId = _idManager.getNetworkId(networkName);
    return _workQueueMgr.getCompletedWork(
        networkId, _idManager.getSnapshotId(snapshotName, networkId));
  }

  private @Nonnull QuestionSettingsId getOrDefaultQuestionSettingsId(
      NetworkId networkId, QuestionId questionId, AnalysisId analysisId)
      throws FileNotFoundException, IOException {
    String questionClassId = _storage.loadQuestionClassId(networkId, questionId, analysisId);
    return getOrDefaultQuestionSettingsId(questionClassId, networkId);
  }

  private @Nonnull QuestionSettingsId getOrDefaultQuestionSettingsId(
      String questionClassId, NetworkId networkId) {
    return _idManager.hasQuestionSettingsId(questionClassId, networkId)
        ? _idManager.getQuestionSettingsId(questionClassId, networkId)
        : QuestionSettingsId.DEFAULT_QUESTION_SETTINGS_ID;
  }

  private @Nonnull AnswerId computeFinalAnswerAndId(
      @Nonnull AnswerMetadata baseAnswerMetadata,
      @Nonnull NetworkId networkId,
      @Nonnull SnapshotId snapshotId,
      @Nonnull QuestionId questionId,
      @Nonnull AnswerId baseAnswerId,
      @Nullable SnapshotId referenceSnapshotId,
      @Nullable AnalysisId analysisId)
      throws IOException {
    Set<IssueSettingsId> issueSettingsIds =
        getOrCreateIssueSettingsIds(networkId, baseAnswerMetadata);
    AnswerId finalAnswerId = _idManager.getFinalAnswerId(baseAnswerId, issueSettingsIds);
    if (!_storage.hasAnswerMetadata(finalAnswerId)) {
      Metrics metrics = baseAnswerMetadata.getMetrics();
      if (metrics == null) {
        _storage.storeAnswer(_storage.loadAnswer(baseAnswerId), finalAnswerId);
        _storage.storeAnswerMetadata(baseAnswerMetadata, finalAnswerId);
      } else {
        rebuildFinalAnswerAndMetadata(
            metrics.getMajorIssueConfigs(),
            networkId,
            snapshotId,
            questionId,
            baseAnswerId,
            finalAnswerId,
            issueSettingsIds,
            referenceSnapshotId,
            analysisId);
      }
    }
    return finalAnswerId;
  }

  private void rebuildFinalAnswerAndMetadata(
      @Nonnull Map<String, MajorIssueConfig> baseAnswerMajorIssueConfigs,
      @Nonnull NetworkId networkId,
      @Nonnull SnapshotId snapshotId,
      @Nonnull QuestionId questionId,
      @Nonnull AnswerId baseAnswerId,
      @Nonnull AnswerId finalAnswerId,
      @Nonnull Set<IssueSettingsId> issueSettingsIds,
      @Nullable SnapshotId referenceSnapshotId,
      @Nullable AnalysisId analysisId)
      throws IOException {
    Map<String, MajorIssueConfig> combinedMajorIssueConfigs =
        new HashMap<>(baseAnswerMajorIssueConfigs);
    for (IssueSettingsId issueSettingsId : issueSettingsIds) {
      MajorIssueConfig networkMajorIssueConfig =
          _storage.loadMajorIssueConfig(networkId, issueSettingsId);
      String majorIssueType = networkMajorIssueConfig.getMajorIssue();
      MajorIssueConfig combinedMajorIssueConfig =
          overlayMajorIssueConfig(
              baseAnswerMajorIssueConfigs.get(majorIssueType), networkMajorIssueConfig);
      combinedMajorIssueConfigs.put(majorIssueType, combinedMajorIssueConfig);
    }
    applyIssuesConfiguration(
        combinedMajorIssueConfigs,
        networkId,
        snapshotId,
        questionId,
        baseAnswerId,
        finalAnswerId,
        referenceSnapshotId,
        analysisId);
  }

  private MajorIssueConfig overlayMajorIssueConfig(
      MajorIssueConfig baseMajorIssueConfig, MajorIssueConfig networkMajorIssueConfig) {
    Map<String, MinorIssueConfig> networkMinorIssues =
        networkMajorIssueConfig.getMinorIssueConfigsMap();
    ImmutableList.Builder<MinorIssueConfig> combinedMinorIssues = ImmutableList.builder();
    // note there is no need to address minor issues not mentioned in base answer
    baseMajorIssueConfig
        .getMinorIssueConfigsMap()
        .forEach(
            (minorIssueType, baseMinorIssueConfig) -> {
              MinorIssueConfig networkMinorIssueConfig = networkMinorIssues.get(minorIssueType);
              if (networkMinorIssueConfig == null) {
                combinedMinorIssues.add(baseMinorIssueConfig);
                return;
              }
              Integer networkSeverity = networkMinorIssueConfig.getSeverity();
              Integer severity =
                  networkSeverity != null ? networkSeverity : baseMinorIssueConfig.getSeverity();
              String networkUrl = networkMinorIssueConfig.getUrl();
              String url =
                  networkUrl != null && !networkUrl.isEmpty()
                      ? networkUrl
                      : baseMinorIssueConfig.getUrl();
              combinedMinorIssues.add(new MinorIssueConfig(minorIssueType, severity, url));
            });
    return new MajorIssueConfig(baseMajorIssueConfig.getMajorIssue(), combinedMinorIssues.build());
  }

  private Set<IssueSettingsId> getOrCreateIssueSettingsIds(
      NetworkId networkId, AnswerMetadata baseAnswerMetadata) throws IOException {
    Metrics metrics = baseAnswerMetadata.getMetrics();
    if (metrics == null) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<IssueSettingsId> ids = ImmutableSet.builder();
    for (String majorIssueType : metrics.getMajorIssueConfigs().keySet()) {
      ids.add(getOrCreateIssueSettingsId(networkId, majorIssueType));
    }
    return ids.build();
  }

  public @Nonnull Map<String, String> getAnalysisAnswers(
      String network,
      String snapshot,
      String referenceSnapshot,
      String analysis,
      Set<String> analysisQuestions)
      throws JsonProcessingException {
    Set<String> questions =
        analysisQuestions.isEmpty() ? listAnalysisQuestions(network, analysis) : analysisQuestions;
    ImmutableSortedMap.Builder<String, String> result = ImmutableSortedMap.naturalOrder();
    for (String questionName : questions) {
      try {
        result.put(
            questionName,
            getAnswerString(network, snapshot, questionName, referenceSnapshot, analysis));
      } catch (Exception e) {
        _logger.errorf(
            "Got exception in getAnalysisAnswers: %s\n", Throwables.getStackTraceAsString(e));
        result.put(
            questionName,
            BatfishObjectMapper.mapper()
                .writeValueAsString(Answer.failureAnswer(e.getMessage(), null)));
      }
    }
    return result.build();
  }

  public @Nonnull Map<String, AnswerMetadata> getAnalysisAnswersMetadata(
      String network,
      String snapshot,
      String referenceSnapshot,
      String analysis,
      Set<String> analysisQuestions) {
    Set<String> questions =
        analysisQuestions.isEmpty() ? listAnalysisQuestions(network, analysis) : analysisQuestions;
    ImmutableSortedMap.Builder<String, AnswerMetadata> result = ImmutableSortedMap.naturalOrder();
    for (String question : questions) {
      try {
        result.put(
            question, getAnswerMetadata(network, snapshot, question, referenceSnapshot, analysis));
      } catch (Exception e) {
        _logger.errorf(
            "Got exception in getAnalysisAnswersMetadata: %s\n",
            Throwables.getStackTraceAsString(e));
        result.put(question, AnswerMetadata.forStatus(AnswerStatus.FAILURE));
      }
    }
    return result.build();
  }

  public @Nonnull AnswerMetadata getAnswerMetadata(
      @Nonnull String network,
      @Nonnull String snapshot,
      @Nonnull String question,
      @Nullable String referenceSnapshot,
      @Nullable String analysis) {
    try {
      NetworkId networkId = _idManager.getNetworkId(network);
      AnalysisId analysisId =
          analysis != null ? _idManager.getAnalysisId(analysis, networkId) : null;
      QuestionId questionId = _idManager.getQuestionId(question, networkId, analysisId);
      SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
      SnapshotId referenceSnapshotId =
          referenceSnapshot != null ? _idManager.getSnapshotId(referenceSnapshot, networkId) : null;
      QuestionSettingsId questionSettingsId =
          getOrDefaultQuestionSettingsId(networkId, questionId, analysisId);
      NodeRolesId networkNodeRolesId = getOrDefaultNodeRolesId(networkId);
      AnswerId baseAnswerId =
          _idManager.getBaseAnswerId(
              networkId,
              snapshotId,
              questionId,
              questionSettingsId,
              networkNodeRolesId,
              referenceSnapshotId,
              analysisId);
      if (!_storage.hasAnswerMetadata(baseAnswerId)) {
        return AnswerMetadata.forStatus(AnswerStatus.NOTFOUND);
      }
      AnswerMetadata baseAnswerMetadata = _storage.loadAnswerMetadata(baseAnswerId);
      AnswerId finalAnswerId =
          computeFinalAnswerAndId(
              baseAnswerMetadata,
              networkId,
              snapshotId,
              questionId,
              baseAnswerId,
              referenceSnapshotId,
              analysisId);
      return _storage.loadAnswerMetadata(finalAnswerId);
    } catch (IOException e) {
      _logger.errorf(
          "Could not get answer metadata: network=%s, snapshot=%s, question=%s, referenceSnapshot=%s, analysis=%s: %s",
          network,
          snapshot,
          question,
          referenceSnapshot,
          analysis,
          Throwables.getStackTraceAsString(e));
      return AnswerMetadata.forStatus(AnswerStatus.FAILURE);
    }
  }

  private @Nonnull NodeRolesId getOrDefaultNodeRolesId(NetworkId networkId) {
    return _idManager.hasNetworkNodeRolesId(networkId)
        ? _idManager.getNetworkNodeRolesId(networkId)
        : NodeRolesId.DEFAULT_NETWORK_NODE_ROLES_ID;
  }

  @VisibleForTesting
  boolean answerIssueConfigMatchesConfiguredIssues(
      MajorIssueConfig answerIssueConfig, Map<String, MajorIssueConfig> configuredMajorIssues) {
    MajorIssueConfig configuredMajorIssue =
        configuredMajorIssues.get(answerIssueConfig.getMajorIssue());
    Map<String, MinorIssueConfig> answerMinorIssues = answerIssueConfig.getMinorIssueConfigsMap();
    Map<String, MinorIssueConfig> configuredMinorIssues =
        configuredMajorIssue.getMinorIssueConfigsMap();
    return Sets.intersection(answerMinorIssues.keySet(), configuredMinorIssues.keySet()).stream()
        .allMatch(minor -> answerMinorIssues.get(minor).equals(configuredMinorIssues.get(minor)));
  }

  @VisibleForTesting
  void applyIssuesConfiguration(
      @Nonnull Map<String, MajorIssueConfig> majorIssueConfigs,
      @Nonnull NetworkId networkId,
      @Nonnull SnapshotId snapshotId,
      @Nonnull QuestionId questionId,
      @Nonnull AnswerId baseAnswerId,
      @Nonnull AnswerId finalAnswerId,
      @Nullable SnapshotId referenceSnapshotId,
      @Nullable AnalysisId analysisId)
      throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
    Answer oldAnswer =
        BatfishObjectMapper.mapper()
            .readValue(_storage.loadAnswer(baseAnswerId), new TypeReference<Answer>() {});
    TableAnswerElement oldTable = (TableAnswerElement) oldAnswer.getAnswerElements().get(0);
    TableMetadata tableMetadata = oldTable.getMetadata();
    Set<String> issueColumns =
        tableMetadata.getColumnMetadata().stream()
            .filter(cm -> cm.getSchema().equals(Schema.ISSUE))
            .map(ColumnMetadata::getName)
            .collect(ImmutableSet.toImmutableSet());
    // apply issue configuration to all rows and excluded rows, then collect them
    ImmutableList.Builder<Row> allRows = ImmutableList.builder();
    applyIssuesConfigurationToRows(oldTable.getRowsList(), issueColumns, majorIssueConfigs)
        .forEach(allRows::add);
    applyIssuesConfigurationToAllExcludedRows(
            oldTable.getExcludedRows(), issueColumns, majorIssueConfigs)
        .map(ExcludedRows::getRowsList)
        .flatMap(Collection::stream)
        .forEach(allRows::add);

    // grab the question for its exclusions
    String questionStr = _storage.loadQuestion(networkId, questionId, analysisId);
    Question questionObj = Question.parseQuestion(questionStr);

    // postprocess using question exclusions, collected rows
    TableAnswerElement newTable = new TableAnswerElement(tableMetadata);
    newTable.postProcessAnswer(questionObj, allRows.build());
    Answer newAnswer = new Answer();

    // Apply new info to answer
    newAnswer.setStatus(AnswerStatus.SUCCESS);
    newAnswer.setQuestion(questionObj);
    newAnswer.setSummary(newTable.getSummary());
    newAnswer.setAnswerElements(ImmutableList.of(newTable));

    // Compute and store new answer and answer metadata
    AnswerMetadata newAnswerMetadata = AnswerMetadataUtil.computeAnswerMetadata(newAnswer, _logger);
    String answerStr = BatfishObjectMapper.writeString(newAnswer);
    _storage.storeAnswer(answerStr, finalAnswerId);
    _storage.storeAnswerMetadata(newAnswerMetadata, finalAnswerId);
  }

  private Stream<ExcludedRows> applyIssuesConfigurationToAllExcludedRows(
      List<ExcludedRows> allExcludedRows,
      Set<String> issueColumns,
      Map<String, MajorIssueConfig> issueConfigs) {
    return allExcludedRows.stream()
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
    return rowsList.stream()
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

  /** Return a {@link Container container} contains all snapshots directories inside it. */
  public Container getContainer(String networkName) {
    if (!_idManager.hasNetworkId(networkName)) {
      throw new IllegalArgumentException(String.format("Network '%s' does not exist", networkName));
    }
    NetworkId networkId = _idManager.getNetworkId(networkName);
    SortedSet<String> testrigs = ImmutableSortedSet.copyOf(_idManager.listSnapshots(networkId));
    return Container.of(networkName, testrigs);
  }

  @Override
  public BatfishLogger getLogger() {
    return _logger;
  }

  @Override
  public Set<String> getNetworkNames() {
    return _idManager.listNetworks();
  }

  private IssueSettingsId getOrCreateIssueSettingsId(NetworkId networkId, String majorIssueType)
      throws IOException {
    if (!_idManager.hasIssueSettingsId(majorIssueType, networkId)) {
      IssueSettingsId issueSettingsId = _idManager.generateIssueSettingsId();
      _storage.storeMajorIssueConfig(
          networkId, issueSettingsId, new MajorIssueConfig(majorIssueType, ImmutableList.of()));
      _idManager.assignIssueSettingsId(majorIssueType, networkId, issueSettingsId);
      return issueSettingsId;
    } else {
      return _idManager.getIssueSettingsId(majorIssueType, networkId);
    }
  }

  /**
   * Returns the latest snapshot in the network.
   *
   * @return An {@link Optional} object with the latest snapshot or empty if no snapshots exist
   */
  public Optional<String> getLatestSnapshot(String network) {
    NetworkId networkId = _idManager.getNetworkId(network);
    Function<String, Instant> toSnapshotTimestamp =
        t ->
            _snapshotMetadataManager.getSnapshotCreationTimeOrMin(
                networkId, _idManager.getSnapshotId(t, networkId));
    return listSnapshots(network).stream()
        .max(
            Comparator.comparing(
                toSnapshotTimestamp, Comparator.nullsFirst(Comparator.naturalOrder())));
  }

  /**
   * Gets the set of nodes in this snapshot. Extracts the set based on the topology file that is
   * generated as part of the testrig initialization. Returns {@code null} if the network or the
   * snapshot does not exist.
   *
   * @throws IOException If the contents of the topology file cannot be mapped to the topology
   *     object
   */
  public @Nullable Set<String> getNodes(String network, String snapshot) throws IOException {
    Topology topology = getPojoTopology(network, snapshot);
    if (topology == null) {
      return null;
    }
    return topology.getNodes().stream().map(Node::getName).collect(Collectors.toSet());
  }

  /**
   * Returns the pojo topology for the given network and snapshot, or {@code null} if either does
   * not exist.
   */
  public @Nullable Topology getPojoTopology(@Nonnull String network, @Nonnull String snapshot)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    return BatfishObjectMapper.mapper()
        .readValue(_storage.loadPojoTopology(networkId, snapshotId), Topology.class);
  }

  /**
   * Gets the {@link ReferenceLibrary} for the {@code network}. Returns an empty {@link
   * ReferenceLibrary} if one does not exist for that network. Returns {@code null} if the network
   * does not exist.
   *
   * @throws IOException if there is an error loading the {@link ReferenceLibrary}
   */
  public ReferenceLibrary getReferenceLibrary(String network) throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    return _storage.loadReferenceLibrary(networkId).orElse(new ReferenceLibrary(null));
  }

  public JSONObject getStatusJson() throws JSONException {
    return _workQueueMgr.getStatusJson();
  }

  /** Checks if the specified snapshot exists. */
  public boolean checkSnapshotExists(String network, String snapshot) {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    return _idManager.hasSnapshotId(snapshot, networkId);
  }

  /** Checks if the specified question exists. */
  public boolean checkQuestionExists(String network, String question, @Nullable String analysis) {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    AnalysisId analysisId;
    if (analysis != null) {
      if (!_idManager.hasAnalysisId(analysis, networkId)) {
        return false;
      }
      analysisId = _idManager.getAnalysisId(analysis, networkId);
    } else {
      analysisId = null;
    }
    return _idManager.hasQuestionId(question, networkId, analysisId);
  }

  /**
   * Get content of given question under network and analysis. Gets ad-hoc question content if
   * analysis is {@code null}. Returns {@code null} if network, analysis (when non-null), or
   * question does not exist.
   *
   * @throws IOException if there is an error reading the question
   */
  public @Nullable String getQuestion(String network, String question, @Nullable String analysis)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    AnalysisId analysisId;
    if (analysis != null) {
      if (!_idManager.hasAnalysisId(analysis, networkId)) {
        return null;
      }
      analysisId = _idManager.getAnalysisId(analysis, networkId);
    } else {
      analysisId = null;
    }
    if (!_idManager.hasQuestionId(question, networkId, analysisId)) {
      return null;
    }
    QuestionId questionId = _idManager.getQuestionId(question, networkId, analysisId);
    return _storage.loadQuestion(networkId, questionId, analysisId);
  }

  public QueuedWork getMatchingWork(WorkItem workItem, QueueType qType) {
    return _workQueueMgr.getMatchingWork(workItem, qType);
  }

  public QueuedWork getWork(UUID workItemId) {
    return _workQueueMgr.getWork(workItemId);
  }

  /**
   * Load and return the log file for a given work item ID in a given snapshot.
   *
   * @throws IOException if the log could not be read successfully.
   * @return Content of the log file as a string; {@code null} if the network, snapshot or log file
   *     is not available
   */
  @Nullable
  public String getWorkLog(String networkName, String snapshotName, String workId)
      throws IOException {
    if (!_idManager.hasNetworkId(networkName)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(networkName);
    if (!_idManager.hasSnapshotId(snapshotName, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshotName, networkId);
    try {
      return _storage.loadWorkLog(networkId, snapshotId, workId);
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  /**
   * Load and return the answer JSON file for a given work item ID in a given snapshot.
   *
   * @throws IOException if the JSON could not be read successfully.
   * @return Content of the JSON file as a string; {@code null} if the network, snapshot or log file
   *     is not available
   */
  @Nullable
  public String getWorkJson(String networkName, String snapshotName, String workId)
      throws IOException {
    if (!_idManager.hasNetworkId(networkName)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(networkName);
    if (!_idManager.hasSnapshotId(snapshotName, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshotName, networkId);
    try {
      return _storage.loadWorkJson(networkId, snapshotId, workId);
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  public String initNetwork(@Nullable String network, @Nullable String networkPrefix) {
    String newNetworkName =
        isNullOrEmpty(network) ? networkPrefix + "_" + UUID.randomUUID() : network;
    if (checkNetworkExists(newNetworkName)) {
      throw new BatfishException(String.format("Network '%s' already exists!", newNetworkName));
    }
    NetworkId networkId = _idManager.generateNetworkId();
    _storage.initNetwork(networkId);
    _idManager.assignNetwork(newNetworkName, networkId);
    return newNetworkName;
  }

  @Override
  public void initSnapshot(
      String networkName, String snapshotName, Path srcDir, boolean autoAnalyze) {
    initSnapshot(networkName, snapshotName, srcDir, autoAnalyze, null);
  }

  public void initSnapshot(
      String networkName,
      String snapshotName,
      Path srcDir,
      boolean autoAnalyze,
      @Nullable SnapshotId parentSnapshotId) {
    Path subDir = getSnapshotSubdir(srcDir);
    validateSnapshotDir(subDir);

    // If interface blacklist was provided, delete it and copy contents into runtime data
    List<NodeInterfacePair> ifaceBlacklist =
        deserializeAndDeleteInterfaceBlacklist(
            subDir.resolve(BfConsts.RELPATH_INTERFACE_BLACKLIST_FILE));
    updateRuntimeData(
        subDir.resolve(BfConsts.RELPATH_RUNTIME_DATA_FILE), ifaceBlacklist, ImmutableList.of());

    SortedSet<Path> subFileList = getEntries(subDir);

    NetworkId networkId = _idManager.getNetworkId(networkName);
    SnapshotId snapshotId = _idManager.generateSnapshotId();

    // Now that the directory exists, we must also create the metadata.
    try {
      _snapshotMetadataManager.writeMetadata(
          new SnapshotMetadata(Instant.now(), parentSnapshotId), networkId, snapshotId);
    } catch (Exception e) {
      throw new BatfishException("Could not write testrigMetadata", e);
    }

    // things look ok, now make the move
    boolean bgpTables = false;
    boolean roleData = false;
    boolean referenceLibraryData = false;
    for (Path subFile : subFileList) {
      String name = subFile.getFileName().toString();
      if (name.equals(BfConsts.RELPATH_ENVIRONMENT_BGP_TABLES)) {
        bgpTables = true;
      } else if (isWellKnownNetworkFile(subFile)) {
        if (name.equals(BfConsts.RELPATH_REFERENCE_LIBRARY_PATH)) {
          referenceLibraryData = true;
          try {
            ReferenceLibrary testrigData =
                BatfishObjectMapper.mapper().readValue(subFile.toFile(), ReferenceLibrary.class);
            ReferenceLibrary mergedLibrary =
                getReferenceLibrary(networkName)
                    .mergeReferenceBooks(testrigData.getReferenceBooks());
            _storage.storeReferenceLibrary(mergedLibrary, networkId);
          } catch (IOException e) {
            // lets not stop the upload because that file is busted.
            // TODO: figure out a way to surface this error to the user
            _logger.errorf("Could not process reference library data: %s", e);
          }
        }
      }
      // Copy everything over
      try {
        if (Files.isDirectory(subFile)) {
          Files.walk(subFile)
              .filter(Files::isRegularFile)
              .forEach(
                  deepFile -> {
                    try (InputStream srcFileStream = Files.newInputStream(deepFile)) {
                      _storage.storeSnapshotInputObject(
                          srcFileStream,
                          subDir.relativize(deepFile).toString(),
                          new NetworkSnapshot(networkId, snapshotId));
                    } catch (IOException e) {
                      throw new UncheckedIOException(
                          String.format("Failed to copy: '%s'", subFile), e);
                    }
                  });
        } else {
          try (InputStream srcFileStream = Files.newInputStream(subFile)) {
            _storage.storeSnapshotInputObject(
                srcFileStream,
                subFile.getFileName().toString(),
                new NetworkSnapshot(networkId, snapshotId));
          }
        }
      } catch (IOException e) {
        throw new UncheckedIOException(String.format("Failed to copy: '%s'", subFile), e);
      }
    }
    _logger.infof(
        "Environment data for snapshot:%s; bgpTables:%s, nodeRoles:%s referenceBooks:%s\n",
        snapshotName, bgpTables, roleData, referenceLibraryData);
    _idManager.assignSnapshot(snapshotName, networkId, snapshotId);
    if (autoAnalyze) {
      for (WorkItem workItem : getAutoWorkQueue(networkName, snapshotName)) {
        boolean queued = queueWork(workItem);
        if (!queued) {
          _logger.errorf("Unable to queue work while auto processing: %s", workItem);
        }
      }
    }
  }

  /**
   * Helper function to assert that the specified dir contains configs
   *
   * @throws BatfishException when specified dir does not contain network configs dir, AWS configs
   *     dir, or a hosts dir
   */
  private static void validateSnapshotDir(Path subDir) {
    // Confirm there is a configs, hosts, or AWS configs dir
    Path hostConfigsPath = subDir.resolve(BfConsts.RELPATH_HOST_CONFIGS_DIR);
    Path networkConfigsPath = subDir.resolve(BfConsts.RELPATH_CONFIGURATIONS_DIR);
    Path awsConfigsPath = subDir.resolve(BfConsts.RELPATH_AWS_CONFIGS_DIR);
    if (!Files.exists(hostConfigsPath)
        && !Files.exists(networkConfigsPath)
        && !Files.exists(awsConfigsPath)) {
      Path srcDir = subDir.getParent();
      throw new BatfishException(
          String.format(
              "Unexpected packaging of snapshot. No networks configs dir '%s', AWS configs dir '%s', or hosts dir '%s' found. See %s for more details on how to package your snapshot for analysis.",
              srcDir.relativize(networkConfigsPath),
              srcDir.relativize(awsConfigsPath),
              srcDir.relativize(hostConfigsPath),
              SNAPSHOT_PACKAGING_INSTRUCTIONS_URL));
    }
  }

  /**
   * Helper function to assert there is only one subdir in the specified snapshot dir and return
   * that subdir
   */
  @VisibleForTesting
  static Path getSnapshotSubdir(Path srcDir) {
    SortedSet<Path> srcDirEntries =
        getEntries(srcDir).stream()
            .filter(path -> !IGNORED_PATHS.contains(path.getFileName().toString()))
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
    /*
     * Sanity check what we got:
     *    There should be just one top-level folder.
     */
    if (srcDirEntries.size() != 1 || !Files.isDirectory(srcDirEntries.iterator().next())) {
      throw new BatfishException(
          String.format(
              "Unexpected packaging of snapshot. There should be just one top-level folder.  See %s for more details on how to package your snapshot for analysis.",
              SNAPSHOT_PACKAGING_INSTRUCTIONS_URL));
    }
    return srcDirEntries.iterator().next();
  }

  private static final int STREAMED_FILE_BUFFER_SIZE = 1024;

  private void writeStreamToFile(InputStream inputStream, Path outputFile) throws IOException {
    createParentDirectories(outputFile);
    try (OutputStream fileOutputStream = Files.newOutputStream(outputFile)) {
      int read = 0;
      final byte[] bytes = new byte[STREAMED_FILE_BUFFER_SIZE];
      while ((read = inputStream.read(bytes)) != -1) {
        fileOutputStream.write(bytes, 0, read);
      }
    }
  }

  /**
   * Copy a snapshot and make modifications to the copy.
   *
   * @param networkName Name of the network containing the original snapshot
   * @param forkSnapshotBean {@link ForkSnapshotBean} containing parameters used to create the fork
   * @throws IllegalArgumentException If the new snapshot name conflicts with an existing snapshot;
   *     or if item to restore had not been deactivated; or if network does not exist
   * @throws FileNotFoundException if base snapshot does not exist
   * @throws IOException If there is an error reading or writing snapshot files.
   */
  public void forkSnapshot(String networkName, ForkSnapshotBean forkSnapshotBean)
      throws IllegalArgumentException, IOException {

    String baseSnapshotName = forkSnapshotBean.baseSnapshot;
    String snapshotName = forkSnapshotBean.newSnapshot;

    // Fail early if the new snapshot already exists or the base snapshot does not
    checkArgument(_idManager.hasNetworkId(networkName), "Network '%s' does not exist", networkName);
    NetworkId networkId = _idManager.getNetworkId(networkName);
    checkArgument(
        !_idManager.hasSnapshotId(snapshotName, networkId),
        "Snapshot with name: '%s' already exists",
        snapshotName);
    if (!_idManager.hasSnapshotId(baseSnapshotName, networkId)) {
      throw new FileNotFoundException(
          String.format("Base snapshot with name: '%s' does not exist", baseSnapshotName));
    }

    // Save user input for troubleshooting
    String forkSnapshotKey = generateFileDateString(snapshotName);
    _storage.storeForkSnapshotRequest(
        BatfishObjectMapper.writeString(forkSnapshotBean), forkSnapshotKey, networkId);

    SnapshotId baseSnapshotId = _idManager.getSnapshotId(baseSnapshotName, networkId);

    // Copy baseSnapshot so initSnapshot will see a properly formatted upload
    Path newSnapshotInputsDir =
        createTempDirectory("files_to_add").resolve(Paths.get(BfConsts.RELPATH_INPUT));
    if (!newSnapshotInputsDir.toFile().mkdirs()) {
      throw new BatfishException("Failed to create directory: '" + newSnapshotInputsDir + "'");
    }

    try (Stream<String> baseInputObjectKeys =
        _storage.listSnapshotInputObjectKeys(new NetworkSnapshot(networkId, baseSnapshotId))) {
      baseInputObjectKeys.forEach(
          key -> {
            try (InputStream baseObjectStream =
                _storage.loadSnapshotInputObject(networkId, baseSnapshotId, key)) {
              writeStreamToFile(baseObjectStream, newSnapshotInputsDir.resolve(key));
            } catch (IOException e) {
              throw new UncheckedIOException(
                  String.format("Unable to copy base snapshot input object with key: %s", key), e);
            }
          });
    }
    // Write user-specified files to the forked snapshot input dir, overwriting existing ones
    if (forkSnapshotBean.zipFile != null) {
      Path unzipDir = createTempDirectory("upload");
      UnzipUtility.unzip(new ByteArrayInputStream(forkSnapshotBean.zipFile), unzipDir);

      // Preserve proper snapshot dir formatting (single top-level dir), so copy new files directly
      // into existing top-level dir
      FileUtils.copyDirectory(getSnapshotSubdir(unzipDir).toFile(), newSnapshotInputsDir.toFile());
    }

    // Update line-up/line-down interface statuses
    Set<NodeInterfacePair> deactivate = new HashSet<>();
    if (forkSnapshotBean.deactivateInterfaces != null) {
      deactivate.addAll(forkSnapshotBean.deactivateInterfaces);
    }
    // Deactivate any interfaces in interface blacklist and delete blacklist if present
    deactivate.addAll(
        deserializeAndDeleteInterfaceBlacklist(
            newSnapshotInputsDir.resolve(BfConsts.RELPATH_INTERFACE_BLACKLIST_FILE)));
    List<NodeInterfacePair> restore =
        firstNonNull(forkSnapshotBean.restoreInterfaces, ImmutableList.of());
    updateRuntimeData(
        newSnapshotInputsDir.resolve(BfConsts.RELPATH_RUNTIME_DATA_FILE), deactivate, restore);

    // Add user-specified failures to new blacklists
    addToSerializedList(
        newSnapshotInputsDir.resolve(BfConsts.RELPATH_EDGE_BLACKLIST_FILE),
        forkSnapshotBean.deactivateLinks,
        new TypeReference<List<Edge>>() {});
    addToSerializedList(
        newSnapshotInputsDir.resolve(BfConsts.RELPATH_NODE_BLACKLIST_FILE),
        forkSnapshotBean.deactivateNodes,
        new TypeReference<List<String>>() {});

    // Remove user-specified items from blacklists
    removeFromSerializedList(
        newSnapshotInputsDir.resolve(BfConsts.RELPATH_EDGE_BLACKLIST_FILE),
        forkSnapshotBean.restoreLinks,
        new TypeReference<List<Edge>>() {});
    removeFromSerializedList(
        newSnapshotInputsDir.resolve(BfConsts.RELPATH_NODE_BLACKLIST_FILE),
        forkSnapshotBean.restoreNodes,
        new TypeReference<List<String>>() {});

    // Use initSnapshot to handle creating metadata, etc.
    initSnapshot(
        networkName, snapshotName, newSnapshotInputsDir.getParent(), false, baseSnapshotId);
  }

  /**
   * Creates or updates {@link SnapshotRuntimeData} at the given {@code runtimeDataPath} by setting
   * the given {@code deactivateIfaces} to line down and the given {@code activateIfaces} to line
   * up. Interfaces in both collections will be marked line up.
   */
  @VisibleForTesting
  static void updateRuntimeData(
      Path runtimeDataPath,
      @Nonnull Collection<NodeInterfacePair> deactivateIfaces,
      @Nonnull Collection<NodeInterfacePair> restoreIfaces) {
    if (deactivateIfaces.isEmpty() && restoreIfaces.isEmpty()) {
      return;
    }
    SnapshotRuntimeData runtimeData = SnapshotRuntimeData.EMPTY_SNAPSHOT_RUNTIME_DATA;
    if (runtimeDataPath.toFile().exists()) {
      try {
        runtimeData =
            firstNonNull(
                BatfishObjectMapper.mapper()
                    .readValue(runtimeDataPath.toFile(), SnapshotRuntimeData.class),
                SnapshotRuntimeData.EMPTY_SNAPSHOT_RUNTIME_DATA);
      } catch (IOException e) {
        // fine, existing runtime data is unreadable. Don't care, replace it. TODO warn?
      }
    }
    SnapshotRuntimeData updatedRuntimeData =
        runtimeData
            .toBuilder()
            .setInterfacesLineDown(deactivateIfaces)
            .setInterfacesLineUp(restoreIfaces)
            .build();
    try {
      BatfishObjectMapper.writer().writeValue(runtimeDataPath.toFile(), updatedRuntimeData);
    } catch (IOException e) {
      // TODO Warn here?
    }
  }

  /**
   * If interface blacklist is present at the given path, deserializes it and deletes file. Returns
   * an empty list if file did not exist or could not be deserialized. File is deleted regardless of
   * whether deserialization was successful.
   *
   * <p>TODO Delete method when {@link BfConsts#RELPATH_INTERFACE_BLACKLIST_FILE} is removed.
   */
  @VisibleForTesting
  @Nonnull
  static List<NodeInterfacePair> deserializeAndDeleteInterfaceBlacklist(Path blacklistPath) {
    File blacklistFile = blacklistPath.toFile();
    if (!blacklistFile.exists()) {
      return ImmutableList.of();
    }
    try (FileInputStream in = new FileInputStream(blacklistFile)) {
      return BatfishObjectMapper.mapper()
          .readValue(in, new TypeReference<List<NodeInterfacePair>>() {});
    } catch (IOException e) {
      // Blacklist could not be deserialized as List<NodeInterfacePair>
      return ImmutableList.of();
    } finally {
      FileUtils.deleteQuietly(blacklistFile);
    }
  }

  @VisibleForTesting
  /* Helper method to add the specified collection to the serialized list at the specified path. */
  static <T> void addToSerializedList(
      Path serializedObjectPath, @Nullable Collection<T> addition, TypeReference<List<T>> type)
      throws IOException {
    if (addition == null || addition.isEmpty()) {
      return;
    }

    List<T> baseList;
    if (serializedObjectPath.toFile().exists()) {
      baseList = BatfishObjectMapper.mapper().readValue(serializedObjectPath.toFile(), type);
      baseList.addAll(addition);
    } else {
      baseList = ImmutableList.copyOf(addition);
    }
    BatfishObjectMapper.writer().writeValue(serializedObjectPath.toFile(), baseList);
  }

  @VisibleForTesting
  /*
   * Helper method to remove the specified collection from the serialized list at the specified
   * path.
   */
  static <T> void removeFromSerializedList(
      Path serializedObjectPath, @Nullable Collection<T> subtraction, TypeReference<List<T>> type)
      throws IOException {
    if (subtraction == null || subtraction.isEmpty()) {
      return;
    }

    List<T> baseList;
    if (serializedObjectPath.toFile().exists()) {
      baseList = BatfishObjectMapper.mapper().readValue(serializedObjectPath.toFile(), type);
    } else {
      throw new IllegalArgumentException("Cannot remove element(s) from non-existent blacklist.");
    }

    List<T> missing =
        subtraction.stream()
            .filter(s -> !baseList.contains(s))
            .collect(ImmutableList.toImmutableList());
    checkArgument(
        missing.isEmpty(),
        "Existing blacklist does not contain element(s) specified for removal: '%s'",
        missing);

    baseList.removeAll(subtraction);
    BatfishObjectMapper.writer().writeValue(serializedObjectPath.toFile(), baseList);
  }

  List<WorkItem> getAutoWorkQueue(String networkName, String testrigName) {
    List<WorkItem> autoWorkQueue = new LinkedList<>();

    WorkItem parseWork = WorkItemBuilder.getWorkItemParse(networkName, testrigName);
    autoWorkQueue.add(parseWork);

    Set<String> analysisNames = listAnalyses(networkName, AnalysisType.ALL);
    for (String analysis : analysisNames) {
      WorkItem analyzeWork =
          WorkItemBuilder.getWorkItemRunAnalysis(analysis, networkName, testrigName);
      autoWorkQueue.add(analyzeWork);
    }
    return autoWorkQueue;
  }

  private static boolean isWellKnownNetworkFile(Path path) {
    return WELL_KNOWN_NETWORK_FILENAMES.contains(path.getFileName().toString());
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

    Span span = GlobalTracer.get().buildSpan("Killing task").start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
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

      JSONArray array;
      try (Response response = webTarget.request(MediaType.APPLICATION_JSON).get()) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
          _logger.errorf("WM:KillTask: Got non-OK response %s\n", response.getStatus());
          return killed;
        }
        String sobj = response.readEntity(String.class);
        array = new JSONArray(sobj);
      }
      try {
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
    } catch (ProcessingException e) {
      _logger.errorf("unable to connect to %s: %s\n", worker, Throwables.getStackTraceAsString(e));
    } catch (Exception e) {
      _logger.errorf("exception: %s\n", Throwables.getStackTraceAsString(e));
    } finally {
      span.finish();
      if (client != null) {
        client.close();
      }
    }
    return killed;
  }

  /**
   * Returns the Analysis names which exist in the network and match the {@link AnalysisType}
   *
   * @param network Network name
   * @param analysisType {@link AnalysisType} requested
   * @return {@link Set} of network names, or {@code null} if network does not exist.
   */
  public @Nullable SortedSet<String> listAnalyses(String network, AnalysisType analysisType) {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    SortedSet<String> analyses =
        _idManager.listAnalyses(networkId).stream()
            .filter(
                aName ->
                    selectAnalysis(
                        _idManager.getAnalysisId(aName, networkId), analysisType, networkId))
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
    return analyses;
  }

  private boolean selectAnalysis(
      AnalysisId analysisId, AnalysisType analysisType, NetworkId networkId) {
    if (analysisType == AnalysisType.ALL) {
      return true;
    }
    boolean suggested = AnalysisMetadataMgr.getAnalysisSuggestedOrFalse(networkId, analysisId);
    return (analysisType == AnalysisType.SUGGESTED && suggested
        || analysisType == AnalysisType.USER && !suggested);
  }

  /**
   * List questions for the given network and analysis. Returns list of questions if successful, or
   * {@code null} if network or analysis does not exist.
   */
  public @Nullable SortedSet<String> listAnalysisQuestions(String network, String analysis) {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasAnalysisId(analysis, networkId)) {
      return null;
    }
    AnalysisId analysisId = _idManager.getAnalysisId(analysis, networkId);
    return ImmutableSortedSet.copyOf(_idManager.listQuestions(networkId, analysisId));
  }

  public SortedSet<String> listNetworks(String apiKey) {
    Path networksDir = Main.getSettings().getContainersLocation();
    if (!Files.exists(networksDir)) {
      networksDir.toFile().mkdirs();
    }
    SortedSet<String> authorizedNetworks =
        _idManager.listNetworks().stream()
            .filter(network -> Main.getAuthorizer().isAccessibleNetwork(apiKey, network, false))
            .collect(toCollection(TreeSet::new));
    return authorizedNetworks;
  }

  public List<Container> getContainers(@Nullable String apiKey) {
    return listNetworks(apiKey).stream().map(this::getContainer).collect(Collectors.toList());
  }

  public List<QueuedWork> listIncompleteWork(
      String network, @Nullable String snapshot, @Nullable WorkType workType) {
    NetworkId networkId = _idManager.getNetworkId(network);
    SnapshotId snapshotId = snapshot != null ? _idManager.getSnapshotId(snapshot, networkId) : null;
    return _workQueueMgr.listIncompleteWork(networkId, snapshotId, workType);
  }

  /**
   * List questions for the given network. If {@code verbose} is {@code true}, include hidden
   * questions. Returns list of questions if successful, or {@code null} if network does not exist.
   */
  public @Nullable SortedSet<String> listQuestions(String network, boolean verbose) {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    Set<String> questions = _idManager.listQuestions(networkId, null);
    if (!verbose) {
      questions =
          questions.stream()
              .filter(name -> !name.startsWith("__"))
              .collect(ImmutableSet.toImmutableSet());
    }
    return ImmutableSortedSet.copyOf(questions);
  }

  /** Returns list of snapshots for given network, or {@code null} if network does not exist. */
  public @Nullable List<String> listSnapshots(@Nonnull String network) {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    List<String> testrigs =
        _idManager.listSnapshots(networkId).stream()
            .sorted(
                (t1, t2) -> { // reverse sorting by creation-time, name
                  SnapshotId snapshotId1 = _idManager.getSnapshotId(t1, networkId);
                  SnapshotId snapshotId2 = _idManager.getSnapshotId(t2, networkId);
                  String key1 =
                      _snapshotMetadataManager.getSnapshotCreationTimeOrMin(networkId, snapshotId1)
                          + t1;
                  String key2 =
                      _snapshotMetadataManager.getSnapshotCreationTimeOrMin(networkId, snapshotId2)
                          + t2;
                  return key2.compareTo(key1);
                })
            .collect(Collectors.toList());
    return testrigs;
  }

  /**
   * Returns list of snapshots for given network along with their metadata, or {@code null} if
   * network does not exist.
   *
   * @throws IOException if there is an error reading metadata for any snapshot
   */
  public @Nullable List<SnapshotMetadataEntry> listSnapshotsWithMetadata(@Nonnull String network)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    ImmutableList.Builder<SnapshotMetadataEntry> snapshotMetadataList = ImmutableList.builder();
    for (String snapshot : listSnapshots(network)) {
      snapshotMetadataList.add(
          new SnapshotMetadataEntry(snapshot, getSnapshotMetadata(network, snapshot)));
    }
    return snapshotMetadataList.build();
  }

  /**
   * Write the reference library for the given network.
   *
   * @throws IOException if there is an error
   */
  public void putReferenceLibrary(ReferenceLibrary referenceLibrary, String network)
      throws IOException {
    checkArgument(_idManager.hasNetworkId(network), "Invalid network: %s", network);
    NetworkId networkId = _idManager.getNetworkId(network);
    _storage.storeReferenceLibrary(referenceLibrary, networkId);
  }

  /** Writes the {@code MajorIssueConfig} for the given network and major issue type. */
  public void putMajorIssueConfig(String network, String majorIssueType, MajorIssueConfig config)
      throws IOException {
    NetworkId networkId = _idManager.getNetworkId(network);
    IssueSettingsId issueSettingsId = _idManager.generateIssueSettingsId();
    _storage.storeMajorIssueConfig(networkId, issueSettingsId, config);
    _idManager.assignIssueSettingsId(majorIssueType, networkId, issueSettingsId);
  }

  public boolean queueWork(WorkItem workItem) {
    NetworkId networkId = _idManager.getNetworkId(requireNonNull(workItem.getNetwork()));
    boolean success;
    try {
      workItem.setSourceSpan(GlobalTracer.get().scopeManager().activeSpan());
      WorkDetails workDetails = computeWorkDetails(workItem);
      _snapshotMetadataManager.getInitializationMetadata(networkId, workDetails.getSnapshotId());
      if (workDetails.isDifferential()) {
        assert workDetails.getReferenceSnapshotId() != null;
        _snapshotMetadataManager.getInitializationMetadata(
            networkId, workDetails.getReferenceSnapshotId());
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

    loadPlugins();

    Executors.newScheduledThreadPool(1)
        .scheduleAtFixedRate(
            new AssignWorkTask(),
            0,
            Main.getSettings().getPeriodAssignWorkMs(),
            TimeUnit.MILLISECONDS);
  }

  public int syncSnapshotsSyncNow(String networkName, String pluginId, boolean force) {
    if (!_snapshotsSyncers.containsKey(pluginId)) {
      throw new BatfishException(
          "PluginId " + pluginId + " not found." + " (Are SyncSnapshots plugins loaded?)");
    }
    return _snapshotsSyncers.get(pluginId).syncNow(networkName, force);
  }

  public boolean syncSnapshotsUpdateSettings(
      String networkName, String pluginId, Map<String, String> settings) {
    if (!_snapshotsSyncers.containsKey(pluginId)) {
      throw new BatfishException(
          "PluginId " + pluginId + " not found." + " (Are SyncSnapshots plugins loaded?)");
    }
    return _snapshotsSyncers.get(pluginId).updateSettings(networkName, settings);
  }

  /**
   * Uploads the given ad-hoc question to the given network. Returns {@code true} if successful.
   * Returns {@code false} if network does not exist.
   *
   * @throws IOException if there as an error saving the question
   */
  public boolean uploadQuestion(String network, String question, String questionJson)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    uploadQuestion(network, question, questionJson, true);
    return true;
  }

  @VisibleForTesting
  void uploadQuestion(String network, String question, String questionJson, boolean validate)
      throws IOException {
    if (validate) {
      // Validate the question before saving it to disk.
      try {
        Question.parseQuestion(questionJson);
      } catch (Exception e) {
        throw new BatfishException(
            String.format("Invalid question %s/%s: %s", network, question, e.getMessage()), e);
      }
    }
    NetworkId networkId;
    try {
      networkId = _idManager.getNetworkId(network);
    } catch (IllegalArgumentException e) {
      throw new InternalServerErrorException(
          String.format("Error uploading question: network=%s, question=%s", network, question), e);
    }
    QuestionId questionId = _idManager.generateQuestionId();
    _storage.storeQuestion(questionJson, networkId, questionId, null);
    _idManager.assignQuestion(question, networkId, questionId, null);
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
    NetworkId networkId = _idManager.getNetworkId(networkName);

    // Fail early if the snapshot already exists
    if (_idManager.hasSnapshotId(snapshotName, networkId)) {
      throw new BatfishException("Snapshot with name: '" + snapshotName + "' already exists");
    }

    // Save uploaded zip for troubleshooting
    String uploadZipKey = generateFileDateString(snapshotName);
    try {
      _storage.storeUploadSnapshotZip(fileStream, uploadZipKey, networkId);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    Path unzipDir = createTempDirectory("tr");
    try (InputStream zipStream = _storage.loadUploadSnapshotZip(uploadZipKey, networkId)) {
      UnzipUtility.unzip(zipStream, unzipDir);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to extract uploaded zip", e);
    }

    try {
      initSnapshot(networkName, snapshotName, unzipDir, autoAnalyze);
    } catch (Exception e) {
      throw new BatfishException(
          String.format("Error initializing snapshot: %s", e.getMessage()), e);
    } finally {
      deleteDirectory(unzipDir);
    }
  }

  public boolean checkNetworkExists(String networkName) {
    if (!_idManager.hasNetworkId(networkName)) {
      return false;
    }
    NetworkId id = _idManager.getNetworkId(networkName);
    return _storage.checkNetworkExists(id);
  }

  /**
   * Filter and sort {@code rawAnswers} according to options specified in {@code
   * analysisAnswersOptions}
   */
  public Map<String, Answer> processAnalysisAnswers(
      Map<String, String> rawAnswers, Map<String, AnswerRowsOptions> answersRowsOptions) {
    return CollectionUtil.toImmutableMap(
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
      // If the AnswerStatus is not SUCCESS, the answer cannot have any AnswerElements related to
      // actual answers (but, e.g., it might have a BatfishStackTrace). Return that as-is.
      if (rawAnswer.getStatus() != AnswerStatus.SUCCESS) {
        return rawAnswer;
      }
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
  Answer processAnswerRows2(String rawAnswerStr, AnswerRowsOptions options) {
    if (rawAnswerStr == null) {
      Answer answer = Answer.failureAnswer("Not found", null);
      answer.setStatus(AnswerStatus.NOTFOUND);
      return answer;
    }
    try {
      Answer rawAnswer =
          BatfishObjectMapper.mapper().readValue(rawAnswerStr, new TypeReference<Answer>() {});
      // If the AnswerStatus is not SUCCESS, the answer cannot have any AnswerElements related to
      // actual answers (but, e.g., it might have a BatfishStackTrace). Return that as-is.
      if (rawAnswer.getStatus() != AnswerStatus.SUCCESS) {
        return rawAnswer;
      }
      return filterAnswer(rawAnswer, options);
    } catch (Exception e) {
      _logger.errorf(
          "Failed to convert answer string to Answer: %s\n", Throwables.getStackTraceAsString(e));
      return Answer.failureAnswer(e.getMessage(), null);
    }
  }

  /** Filter the supplied rawAnswer based on the options provided */
  public Answer filterAnswer(Answer rawAnswer, AnswerRowsOptions options) {
    AnswerElement answerElement = rawAnswer.getAnswerElements().get(0);
    if (!(answerElement instanceof TableAnswerElement)) {
      return rawAnswer;
    }
    TableAnswerElement rawTable = (TableAnswerElement) answerElement;
    Answer answer = new Answer();
    answer.setStatus(rawAnswer.getStatus());
    answer.addAnswerElement(processAnswerTable2(rawTable, options));
    return answer;
  }

  @VisibleForTesting
  @Nonnull
  TableAnswerElement processAnswerTable(TableAnswerElement rawTable, AnswerRowsOptions options) {
    Map<String, ColumnMetadata> rawColumnMap = rawTable.getMetadata().toColumnMap();
    List<Row> filteredRows =
        rawTable.getRowsList().stream()
            .filter(row -> options.getFilters().stream().allMatch(filter -> filter.matches(row)))
            .collect(ImmutableList.toImmutableList());

    Stream<Row> rowStream = filteredRows.stream();
    if (!options.getSortOrder().isEmpty()) {
      // sort using specified sort order
      rowStream = rowStream.sorted(buildComparator(rawColumnMap, options.getSortOrder()));
    }
    TableAnswerElement table;
    if (options.getColumns().isEmpty()) {
      table = new TableAnswerElement(rawTable.getMetadata());
    } else {
      // project to desired columns
      rowStream =
          rowStream.map(rawRow -> Row.builder().putAll(rawRow, options.getColumns()).build());
      Map<String, ColumnMetadata> columnMap = new LinkedHashMap<>(rawColumnMap);
      columnMap.keySet().retainAll(options.getColumns());
      List<ColumnMetadata> columnMetadata =
          columnMap.values().stream().collect(ImmutableList.toImmutableList());
      table =
          new TableAnswerElement(
              new TableMetadata(columnMetadata, rawTable.getMetadata().getTextDesc()));
    }
    if (options.getUniqueRows()) {
      // uniquify if desired
      rowStream = rowStream.distinct();
    }
    // offset, truncate, and add to table
    rowStream.skip(options.getRowOffset()).limit(options.getMaxRows()).forEach(table::addRow);
    table.setSummary(rawTable.getSummary() != null ? rawTable.getSummary() : new AnswerSummary());
    table.getSummary().setNumResults(filteredRows.size());
    return table;
  }

  @VisibleForTesting
  @Nonnull
  TableView processAnswerTable2(TableAnswerElement rawTable, AnswerRowsOptions options) {
    Map<Row, Integer> rowIds = Maps.newIdentityHashMap();
    CommonUtil.forEachWithIndex(rawTable.getRowsList(), (i, row) -> rowIds.put(row, i));
    Map<String, ColumnMetadata> rawColumnMap = rawTable.getMetadata().toColumnMap();
    List<Row> filteredRows =
        rawTable.getRowsList().stream()
            .filter(row -> options.getFilters().stream().allMatch(filter -> filter.matches(row)))
            .collect(ImmutableList.toImmutableList());

    Stream<Row> rowStream = filteredRows.stream();
    if (!options.getSortOrder().isEmpty()) {
      // sort using specified sort order
      rowStream = rowStream.sorted(buildComparator(rawColumnMap, options.getSortOrder()));
    }
    TableMetadata tableMetadata;
    if (options.getColumns().isEmpty()) {
      tableMetadata = rawTable.getMetadata();
    } else {
      // project to desired columns
      rowStream =
          rowStream.map(
              rawRow -> {
                Row row = Row.builder().putAll(rawRow, options.getColumns()).build();
                rowIds.put(row, rowIds.get(rawRow));
                return row;
              });
      Map<String, ColumnMetadata> columnMap = new LinkedHashMap<>(rawColumnMap);
      columnMap.keySet().retainAll(options.getColumns());
      List<ColumnMetadata> columnMetadata =
          columnMap.values().stream().collect(ImmutableList.toImmutableList());
      tableMetadata = new TableMetadata(columnMetadata, rawTable.getMetadata().getTextDesc());
    }
    if (options.getUniqueRows()) {
      // uniquify if desired
      rowStream = rowStream.distinct();
    }
    // offset, truncate, and add to table
    TableView tableView =
        new TableView(
            options,
            rowStream
                .skip(options.getRowOffset())
                .limit(options.getMaxRows())
                .map(row -> new TableViewRow(rowIds.get(row), row))
                .collect(ImmutableList.toImmutableList()),
            tableMetadata);
    tableView.setSummary(
        rawTable.getSummary() != null ? rawTable.getSummary() : new AnswerSummary());
    tableView.getSummary().setNumResults(filteredRows.size());
    return tableView;
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

  @SuppressWarnings({"rawtypes", "unchecked"})
  @VisibleForTesting
  @Nonnull
  Comparator<Row> columnComparator(ColumnMetadata columnMetadata) {
    Schema schema = columnMetadata.getSchema();
    Comparator schemaComparator = schemaComparator(schema);
    Comparator comparator =
        comparing((Row r) -> r.get(columnMetadata.getName(), schema), nullsFirst(schemaComparator));
    return comparator;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private @Nonnull Comparator<?> schemaComparator(Schema schema) {
    if (schema.equals(Schema.ACL_TRACE)) {
      return COMPARATOR_ACL_TRACE;
    } else if (schema.equals(Schema.BOOLEAN)) {
      return naturalOrder();
    } else if (schema.equals(Schema.DOUBLE)) {
      return naturalOrder();
    } else if (schema.equals(Schema.FLOW)) {
      return naturalOrder();
    } else if (schema.equals(Schema.INTEGER)) {
      return naturalOrder();
    } else if (schema.equals(Schema.INTERFACE)) {
      return naturalOrder();
    } else if (schema.equals(Schema.IP)) {
      return naturalOrder();
    } else if (schema.equals(Schema.ISSUE)) {
      return comparing(Issue::getSeverity);
    } else if (schema.getType() == Type.LIST) {
      Comparator schemaComparator = schemaComparator(schema.getInnerSchema());
      return lexicographical(nullsFirst(schemaComparator));
    } else if (schema.equals(Schema.LONG)) {
      return naturalOrder();
    } else if (schema.equals(Schema.NODE)) {
      return COMPARATOR_NODE;
    } else if (schema.equals(Schema.PREFIX)) {
      return naturalOrder();
    } else if (schema.getType() == Type.SET) {
      Comparator schemaComparator = schemaComparator(schema.getInnerSchema());
      return lexicographical(nullsFirst(schemaComparator));
    } else if (schema.equals(Schema.STRING)) {
      return naturalOrder();
    } else if (schema.equals(Schema.TRACE)) {
      return COMPARATOR_TRACE;
    } else {
      return comparing(Object::toString);
    }
  }

  /**
   * Return the JSON-serialized settings for the specified question class for the specified network;
   * or {@code null} if either no custom settings exist for the question or no value is present at
   * the path produced from the sepcified components.
   *
   * @param network The name of the network
   * @param questionClassId The ID of the class of questions whose settings are to be returned
   * @param components The components to traverse from the root of the question settings to reach
   *     the desired section or value
   * @throws IOException if there is an error reading the settings
   */
  public @Nullable String getQuestionSettings(
      String network, String questionClassId, List<String> components) throws IOException {
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasQuestionSettingsId(questionClassId, networkId)) {
      return null;
    }
    QuestionSettingsId questionSettingsId =
        _idManager.getQuestionSettingsId(questionClassId, networkId);
    String questionSettings;
    questionSettings = _storage.loadQuestionSettings(networkId, questionSettingsId);
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
   * @param questionClassId The ID of the question class
   * @param components The components to traverse from the root of the question settings to reach
   *     the desired section or value
   * @param value The settings value to write at the end of the path
   * @throws IOException if there is an error writing the settings
   */
  public synchronized void writeQuestionSettings(
      String network, String questionClassId, List<String> components, JsonNode value)
      throws IOException {
    NetworkId networkId = _idManager.getNetworkId(network);
    String questionSettings;
    if (_idManager.hasQuestionSettingsId(questionClassId, networkId)) {
      questionSettings =
          _storage.loadQuestionSettings(
              networkId, _idManager.getQuestionSettingsId(questionClassId, networkId));
    } else {
      questionSettings = "{}";
    }
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
    QuestionSettingsId questionSettingsId = _idManager.generateQuestionSettingsId();
    _storage.storeQuestionSettings(
        BatfishObjectMapper.writeString(root), networkId, questionSettingsId);
    _idManager.assignQuestionSettingsId(questionClassId, networkId, questionSettingsId);
  }

  public MajorIssueConfig getMajorIssueConfig(String network, String majorIssueType)
      throws IOException {
    NetworkId networkId = _idManager.getNetworkId(network);
    return _storage.loadMajorIssueConfig(
        networkId, getOrCreateIssueSettingsId(networkId, majorIssueType));
  }

  @VisibleForTesting
  public IdManager getIdManager() {
    return _idManager;
  }

  @VisibleForTesting
  public SnapshotMetadataMgr getSnapshotMetadataManager() {
    return _snapshotMetadataManager;
  }

  /** Fetch metadata for snapshot. Returns {@code null} if network or snapshot does not exist. */
  public @Nullable SnapshotMetadata getSnapshotMetadata(String network, String snapshot)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    return _snapshotMetadataManager.readMetadata(networkId, snapshotId);
  }

  /**
   * Promote snapshot's inferred node roles to network-wide node roles if latter does not yet exist
   *
   * @throws IOException if there is an error retrieving snapshot's inferred node roles
   */
  public void tryPromoteSnapshotNodeRoles(
      @Nonnull NetworkId networkId, @Nonnull SnapshotId snapshotId) throws IOException {
    if (!_idManager.hasNetworkNodeRolesId(networkId)) {
      putNetworkNodeRoles(
          getSnapshotNodeRoles(networkId, snapshotId),
          networkId,
          _idManager.getSnapshotNodeRolesId(networkId, snapshotId));
    }
  }

  /**
   * Reads the {@link NodeRolesData} object for the provided {@code network}. If none previously set
   * for this {@code network}, returns empty {@link NodeRolesData}. If {@code network} does not
   * exist, returns {@code null}.
   *
   * @throws IOException If there is an error
   */
  public @Nullable NodeRolesData getNetworkNodeRoles(@Nonnull String network) throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasNetworkNodeRolesId(networkId)) {
      return NodeRolesData.builder().build();
    }
    NodeRolesId networkNodeRolesId = _idManager.getNetworkNodeRolesId(networkId);
    try {
      return BatfishObjectMapper.mapper()
          .readValue(_storage.loadNodeRoles(networkNodeRolesId), NodeRolesData.class);
    } catch (IOException e) {
      throw new IOException("Failed to read network node roles", e);
    }
  }

  /**
   * Returns the {@link NodeRolesData} object containing only inferred roles for the provided
   * network and snapshot, or {@code null} if either the network or snapshot does not exist.
   *
   * @throws IOException If there is an error
   */
  public @Nullable NodeRolesData getSnapshotNodeRoles(
      @Nonnull String network, @Nonnull String snapshot) throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    return getSnapshotNodeRoles(networkId, snapshotId);
  }

  /**
   * Returns the {@link NodeRolesData} object containing only inferred roles for the provided
   * networkId and snapshotId.
   *
   * @throws IOException If there is an error
   */
  private @Nonnull NodeRolesData getSnapshotNodeRoles(
      @Nonnull NetworkId networkId, @Nonnull SnapshotId snapshotId) throws IOException {
    NodeRolesId snapshotNodeRolesId = _idManager.getSnapshotNodeRolesId(networkId, snapshotId);
    return BatfishObjectMapper.mapper()
        .readValue(_storage.loadNodeRoles(snapshotNodeRolesId), NodeRolesData.class);
  }

  /**
   * Provides a stream from which the extended object with the given {@code key} for the given
   * {@code network} may be read. Returns {@code null} if the object cannot be found.
   *
   * @throws IOException if there is an error reading the object
   */
  @MustBeClosed
  @Nullable
  public InputStream getNetworkObject(@Nonnull String network, @Nonnull String key)
      throws IOException {
    NetworkId networkId = _idManager.getNetworkId(network);
    try {
      return _storage.loadNetworkObject(networkId, key);
    } catch (FileNotFoundException e) {
      return null;
    } catch (IOException e) {
      throw new IOException(
          String.format("Could not read extended object for network '%s', key '%s'", network, key),
          e);
    }
  }

  /**
   * Writes an extended object from the provided {@code inputStream} with the given {@code key} for
   * the given {@code network}. Returns {@code true} if the object was written, or {@code false} if
   * the network does not exist.
   *
   * @throws IOException if there is an error writing the object
   */
  public boolean putNetworkObject(
      @Nonnull InputStream inputStream, @Nonnull String network, @Nonnull String key)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    try {
      _storage.storeNetworkObject(inputStream, networkId, key);
    } catch (IOException e) {
      throw new IOException(
          String.format("Could not write extended object for network '%s', key '%s'", network, key),
          e);
    }
    return true;
  }

  /**
   * Deletes the extended object with the given {@code key} for the given {@code network}. Returns
   * {@code true} if deletion is successful, or {@code false} if the object or network does not
   * exist.
   *
   * @throws IOException if there is an error deleting the object
   */
  public boolean deleteNetworkObject(@Nonnull String network, @Nonnull String key)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    try {
      _storage.deleteNetworkObject(networkId, key);
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      throw new IOException(
          String.format(
              "Could not delete extended object for network '%s' at key '%s'", network, key),
          e);
    }
    return true;
  }

  /**
   * Provides a stream from which the extended object with the given {@code key} for the given
   * {@code network} and {@code snapshot} may be read. Returns {@code null} if the object cannot be
   * found.
   *
   * @throws IOException if there is an error reading the object
   */
  @MustBeClosed
  @Nullable
  public InputStream getSnapshotObject(
      @Nonnull String network, @Nonnull String snapshot, @Nonnull String key) throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    try {
      return _storage.loadSnapshotObject(networkId, snapshotId, key);
    } catch (FileNotFoundException e) {
      return null;
    } catch (IOException e) {
      throw new IOException(
          String.format(
              "Could not read extended object for network '%s', snapshot '%s', key '%s'",
              network, snapshot, key),
          e);
    }
  }

  /**
   * Writes an extended object from the provided {@code inputStream} with the given {@code key} for
   * the given {@code network} and {@code snapshot}. Returns {@code true} if the object was written,
   * or {@code false} if the network or snapshot does not exist.
   *
   * @throws IOException if there is an error writing the object
   */
  public boolean putSnapshotExtendedObject(
      @Nonnull InputStream inputStream,
      @Nonnull String network,
      @Nonnull String snapshot,
      @Nonnull String key)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return false;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    try {
      _storage.storeSnapshotObject(inputStream, networkId, snapshotId, key);
    } catch (IOException e) {
      throw new IOException(
          String.format(
              "Could not write extended object for network '%s', snapshot '%s', key '%s'",
              network, snapshot, key),
          e);
    }
    return true;
  }

  /**
   * Deletes the extended object with the given {@code key} for the given {@code network} and {@code
   * snapshot}. Returns {@code true} if deletion is successful, or {@code false} if the object,
   * network, or snapshot does not exist.
   *
   * @throws IOException if there is an error deleting the object
   */
  public boolean deleteSnapshotObject(
      @Nonnull String network, @Nonnull String snapshot, @Nonnull String key) throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return false;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    try {
      _storage.deleteSnapshotObject(networkId, snapshotId, key);
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      throw new IOException(
          String.format(
              "Could not delete extended object for network '%s', snapshot '%s', key '%s'",
              network, snapshot, key),
          e);
    }
    return true;
  }

  /**
   * Provides a stream from which the input object with the given {@code key} for the given {@code
   * network} and {@code snapshot} may be read. Returns {@code null} if the object cannot be found.
   *
   * @throws IOException if there is an error reading the object
   */
  @MustBeClosed
  public InputStream getSnapshotInputObject(String network, String snapshot, String key)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    try {
      return _storage.loadSnapshotInputObject(networkId, snapshotId, key);
    } catch (FileNotFoundException e) {
      return null;
    } catch (IOException e) {
      throw new IOException(
          String.format(
              "Could not read input object for network '%s', snapshot '%s', key '%s'",
              network, snapshot, key),
          e);
    }
  }

  @Nullable
  public List<StoredObjectMetadata> getSnapshotInputObjectsMetadata(String network, String snapshot)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    try {
      return _storage.getSnapshotInputObjectsMetadata(networkId, snapshotId);
    } catch (IOException e) {
      throw new IOException(
          String.format(
              "Could not fetch input keys for network '%s', snapshot '%s'", network, snapshot),
          e);
    }
  }

  @Nullable
  public List<StoredObjectMetadata> getSnapshotExtendedObjectsMetadata(
      String network, String snapshot) throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    try {
      return _storage.getSnapshotExtendedObjectsMetadata(networkId, snapshotId);
    } catch (IOException e) {
      throw new IOException(
          String.format(
              "Could not fetch extended objects metadata for network '%s', snapshot '%s'",
              network, snapshot),
          e);
    }
  }

  /**
   * Returns the env topology for the given network and snapshot, or {@code null} if either does not
   * exist.
   */
  public @Nullable org.batfish.datamodel.Topology getTopology(String network, String snapshot)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return null;
    }
    SnapshotId snapshotId = _idManager.getSnapshotId(snapshot, networkId);
    String topologyStr = _storage.loadInitialTopology(networkId, snapshotId);
    return BatfishObjectMapper.mapper()
        .readValue(topologyStr, org.batfish.datamodel.Topology.class);
  }

  /**
   * Writes the {@code nodeRolesData} for the given {@code network}. Returns {@code true} if
   * successful. Returns {@code false} if {@code network} does not exist.
   *
   * @throws IOException if there is an error
   */
  public boolean putNetworkNodeRoles(@Nonnull NodeRolesData nodeRolesData, @Nonnull String network)
      throws IOException {
    if (!_idManager.hasNetworkId(network)) {
      return false;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    NodeRolesId networkNodeRolesId = _idManager.generateNetworkNodeRolesId();
    putNetworkNodeRoles(nodeRolesData, networkId, networkNodeRolesId);
    return true;
  }

  /**
   * Writes the {@code nodeRolesData} for the given {@code networkId} and assigns the given {@code
   * nodeRolesId}.
   *
   * @throws IOException if there is an error
   */
  private void putNetworkNodeRoles(
      @Nonnull NodeRolesData nodeRolesData,
      @Nonnull NetworkId networkId,
      @Nonnull NodeRolesId nodeRolesId)
      throws IOException {
    _storage.storeNodeRoles(nodeRolesData, nodeRolesId);
    _idManager.assignNetworkNodeRolesId(networkId, nodeRolesId);
  }

  /** Provides the results of validating the user supplied input */
  @Nullable
  public InputValidationNotes validateInput(
      String network, String snapshot, Variable.Type varType, String query) throws IOException {

    if (!_idManager.hasNetworkId(network)) {
      return null;
    }
    NetworkId networkId = _idManager.getNetworkId(network);
    if (!_idManager.hasSnapshotId(snapshot, networkId)) {
      return null;
    }

    return InputValidationUtils.validate(
        varType,
        query,
        firstNonNull(
            getCompletionMetadata(network, snapshot), CompletionMetadata.builder().build()),
        firstNonNull(getNetworkNodeRoles(network), NodeRolesData.builder().build()),
        firstNonNull(getReferenceLibrary(network), new ReferenceLibrary(null)));
  }
}
