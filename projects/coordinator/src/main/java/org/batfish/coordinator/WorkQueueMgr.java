package org.batfish.coordinator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts.TaskStatus;
import org.batfish.common.CoordConsts.WorkStatusCode;
import org.batfish.common.Task;
import org.batfish.common.WorkItem;
import org.batfish.common.util.WorkItemBuilder;
import org.batfish.coordinator.WorkDetails.WorkType;
import org.batfish.coordinator.queues.AzureQueue;
import org.batfish.coordinator.queues.MemoryQueue;
import org.batfish.coordinator.queues.WorkQueue;
import org.batfish.datamodel.EnvironmentMetadata;
import org.batfish.datamodel.EnvironmentMetadata.ProcessingStatus;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

// the design of this WorkQueueMgr is such that all synchronization sits here
// individual queues do not need to be synchronized

public class WorkQueueMgr {

  public enum QueueType {
    COMPLETED,
    INCOMPLETE
  }

  private Set<UUID> _blockingWork;

  private BatfishLogger _logger;

  private WorkQueue _queueCompletedWork;

  private WorkQueue _queueIncompleteWork;

  public WorkQueueMgr(BatfishLogger logger) {
    this(Main.getSettings().getQueueType(), logger);
  }

  public WorkQueueMgr(WorkQueue.Type wqType, BatfishLogger logger) {
    _blockingWork = new HashSet<>();
    _logger = logger;
    switch (wqType) {
      case azure:
        String storageConnectionString =
            String.format(
                "DefaultEndpointsProtocol=%s;AccountName=%s;AccountKey=%s",
                Main.getSettings().getStorageProtocol(),
                Main.getSettings().getStorageAccountName(),
                Main.getSettings().getStorageAccountKey());
        _queueCompletedWork =
            new AzureQueue(Main.getSettings().getQueueCompletedWork(), storageConnectionString);
        _queueIncompleteWork =
            new AzureQueue(Main.getSettings().getQueueIncompleteWork(), storageConnectionString);
        break;
      case memory:
        _queueCompletedWork = new MemoryQueue();
        _queueIncompleteWork = new MemoryQueue();
        break;
      default:
        throw new BatfishException("Unsupported queue type: " + wqType);
    }
  }

  private void cleanUpEnvMetaDataIfNeeded(String container, String testrig, String environment)
      throws IOException {
    EnvironmentMetadata envMetadata =
        TestrigMetadataMgr.getEnvironmentMetadata(container, testrig, environment);
    if (envMetadata.getProcessingStatus() == ProcessingStatus.PARSING
        && getIncompleteWork(container, testrig, environment, WorkType.PARSING) == null) {
      TestrigMetadataMgr.updateEnvironmentStatus(
          container, testrig, environment, ProcessingStatus.PARSING_FAIL);
    } else if (envMetadata.getProcessingStatus() == ProcessingStatus.DATAPLANING
        && getIncompleteWork(container, testrig, environment, WorkType.DATAPLANING) == null) {
      TestrigMetadataMgr.updateEnvironmentStatus(
          container, testrig, environment, ProcessingStatus.DATAPLANING_FAIL);
    }
  }

  private QueuedWork generateAndQueueDataplaneWork(
      String container, String testrig, String environment) throws Exception {
    WorkItem newWItem =
        WorkItemBuilder.getWorkItemGenerateDataPlane(container, testrig, environment);
    WorkDetails details =
        new WorkDetails(testrig, environment, null, null, false, WorkType.DATAPLANING);
    QueuedWork newWork = new QueuedWork(newWItem, details);
    boolean queued = queueUnassignedWork(newWork);
    if (!queued) {
      throw new BatfishException("Failed to auto-queue dataplane work");
    }
    return newWork;
  }

  private QueuedWork getBlockerForDataplaningWork(QueuedWork work) throws IOException {

    WorkItem wItem = work.getWorkItem();
    WorkDetails wDetails = work.getDetails();

    QueuedWork currentParsingWork =
        getIncompleteWork(
            wItem.getContainerName(), wDetails.baseTestrig, wDetails.baseEnv, WorkType.PARSING);

    EnvironmentMetadata envMetadata =
        TestrigMetadataMgr.getEnvironmentMetadata(
            wItem.getContainerName(), wDetails.baseTestrig, wDetails.baseEnv);

    switch (envMetadata.getProcessingStatus()) {
      case UNINITIALIZED:
      case PARSING_FAIL:
      case PARSING:
        if (currentParsingWork == null) {
          throw new BatfishException(
              String.format(
                  "Cannot queue dataplane work for %s / %s: "
                      + "Status is %s but no incomplete parsing work exists",
                  wDetails.baseTestrig, wDetails.baseEnv, envMetadata.getProcessingStatus()));
        }
        return currentParsingWork;
      case PARSED:
        return currentParsingWork;
      case DATAPLANING:
        // we get here only when currentDataplaningWork is null; by virtue of the calling context
        throw new BatfishException(
            String.format(
                "Cannot queue dataplane work for %s / %s: "
                    + "Status is %s but no incomplete dataplaning work exists",
                wDetails.baseTestrig, wDetails.baseEnv, envMetadata.getProcessingStatus()));
      case DATAPLANED:
      case DATAPLANING_FAIL:
        return null;
      default:
        throw new BatfishException(
            "Unknown testrig processingStatus: " + envMetadata.getProcessingStatus());
    }
  }

  /**
   * This function has a side effect It will inject a dataplane generation work in the queue if none
   * exists
   */
  private QueuedWork getBlockerForDataplaneDependentWork(
      QueuedWork work, String testrig, String environment) throws Exception {

    WorkItem wItem = work.getWorkItem();

    EnvironmentMetadata envMetadata =
        TestrigMetadataMgr.getEnvironmentMetadata(wItem.getContainerName(), testrig, environment);

    QueuedWork parsingWork =
        getIncompleteWork(wItem.getContainerName(), testrig, environment, WorkType.PARSING);

    QueuedWork dataplaningWork =
        getIncompleteWork(wItem.getContainerName(), testrig, environment, WorkType.DATAPLANING);

    switch (envMetadata.getProcessingStatus()) {
      case UNINITIALIZED:
      case PARSING_FAIL:
      case PARSING:
        if (parsingWork == null) {
          throw new BatfishException(
              String.format(
                  "Cannot queue dataplane dependent work for %s / %s: "
                      + "Status is %s but no incomplete parsing work exists",
                  testrig, environment, envMetadata.getProcessingStatus()));
        }
        return parsingWork;
      case PARSED:
        if (parsingWork != null) {
          return parsingWork;
        }
        if (dataplaningWork != null) {
          return dataplaningWork;
        }
        return generateAndQueueDataplaneWork(wItem.getContainerName(), testrig, environment);
      case DATAPLANING_FAIL:
      case DATAPLANING:
        if (dataplaningWork == null) {
          throw new BatfishException(
              String.format(
                  "Cannot queue dataplane dependent work for %s / %s: "
                      + "Status is %s but no incomplete dataplaning work exists",
                  testrig, environment, envMetadata.getProcessingStatus()));
        }
        return dataplaningWork;
      case DATAPLANED:
        if (parsingWork != null) {
          return parsingWork;
        }
        if (dataplaningWork != null) {
          return dataplaningWork;
        }
        return null;
      default:
        throw new BatfishException(
            "Unknown environment processingStatus: " + envMetadata.getProcessingStatus());
    }
  }

  private QueuedWork getBlockerForDataplaneIndependentWork(
      QueuedWork work, String testrig, String environment) throws IOException {

    WorkItem wItem = work.getWorkItem();

    EnvironmentMetadata envMetadata =
        TestrigMetadataMgr.getEnvironmentMetadata(wItem.getContainerName(), testrig, environment);

    QueuedWork parsingWork =
        getIncompleteWork(wItem.getContainerName(), testrig, environment, WorkType.PARSING);

    switch (envMetadata.getProcessingStatus()) {
      case UNINITIALIZED:
      case PARSING:
      case PARSING_FAIL:
        if (parsingWork == null) {
          throw new BatfishException(
              String.format(
                  "Cannot queue dataplane independent work for %s / %s: "
                      + "Status is %s but no incomplete parsing work exists",
                  testrig, environment, envMetadata.getProcessingStatus()));
        }
        return parsingWork;
      case PARSED:
      case DATAPLANING:
      case DATAPLANED:
      case DATAPLANING_FAIL:
        return parsingWork;
      default:
        throw new BatfishException(
            "Unknown testrig processingStatus: " + envMetadata.getProcessingStatus());
    }
  }

  private synchronized QueuedWork getIncompleteWork(
      String container, String testrig, String environment, WorkType wType) {
    for (QueuedWork work : _queueIncompleteWork) {
      WorkDetails wDetails = work.getDetails();
      if (container.equals(work.getWorkItem().getContainerName())
          && ((testrig.equals(wDetails.baseTestrig) && environment.equals(wDetails.baseEnv))
              || (wDetails.isDifferential
                  && testrig.equals(wDetails.deltaTestrig)
                  && environment.equals(wDetails.deltaEnv)))
          && (wType == null || wDetails.workType == wType)) {
        return work;
      }
    }
    return null;
  }

  public synchronized long getLength(QueueType qType) {
    switch (qType) {
      case COMPLETED:
        return _queueCompletedWork.getLength();
      case INCOMPLETE:
        return _queueIncompleteWork.getLength();
      default:
        return -1;
    }
  }

  public synchronized JSONObject getStatusJson() throws JSONException {

    JSONObject jObject = new JSONObject();

    jObject.put("incomplete-works", _queueIncompleteWork.getLength());
    for (QueuedWork work : _queueIncompleteWork) {
      jObject.put(work.getId().toString(), work.toString());
    }

    jObject.put("completed-works", _queueCompletedWork.getLength());
    for (QueuedWork work : _queueCompletedWork) {
      jObject.put(work.getId().toString(), work.toString());
    }

    return jObject;
  }

  public synchronized QueuedWork getMatchingWork(WorkItem workItem, QueueType qType) {
    switch (qType) {
      case COMPLETED:
        return getMatchingWork(workItem, _queueCompletedWork);
      case INCOMPLETE:
        return getMatchingWork(workItem, _queueIncompleteWork);
      default:
        throw new BatfishException("Unknown QueueType " + qType);
    }
  }

  private synchronized QueuedWork getMatchingWork(WorkItem workItem, WorkQueue queue) {
    for (QueuedWork work : queue) {
      if (work.getWorkItem().matches(workItem)) {
        return work;
      }
    }
    return null;
  }

  public synchronized QueuedWork getWork(UUID workId) {
    QueuedWork work = getWork(workId, QueueType.INCOMPLETE);
    if (work == null) {
      work = getWork(workId, QueueType.COMPLETED);
    }
    return work;
  }

  @Nullable
  private synchronized QueuedWork getWork(UUID workId, QueueType qType) {
    switch (qType) {
      case COMPLETED:
        return _queueCompletedWork.getWork(workId);
      case INCOMPLETE:
        return _queueIncompleteWork.getWork(workId);
      default:
        return null;
    }
  }

  @Nullable
  public synchronized QueuedWork getWorkForAssignment() {

    for (QueuedWork work : _queueIncompleteWork) {
      if (work.getStatus() == WorkStatusCode.UNASSIGNED) {
        work.setStatus(WorkStatusCode.TRYINGTOASSIGN);
        return work;
      }
    }

    return null;
  }

  @Nonnull
  public List<QueuedWork> getWorkForChecking() {
    List<QueuedWork> workToCheck = new ArrayList<>();
    for (QueuedWork work : _queueIncompleteWork) {
      if (work.getStatus() == WorkStatusCode.ASSIGNED) {
        work.setStatus(WorkStatusCode.CHECKINGSTATUS);
        workToCheck.add(work);
      }
    }
    return workToCheck;
  }

  public synchronized List<QueuedWork> listIncompleteWork(String containerName) {
    List<QueuedWork> retList = new LinkedList<>();
    for (QueuedWork work : _queueIncompleteWork) {
      if (work.getWorkItem().getContainerName().equals(containerName)) {
        retList.add(work);
      }
    }
    return retList;
  }

  public synchronized QueuedWork getDataplaningWork(String containerName, String testrigName) {
    QueuedWork work =
        getTestrigWork(containerName, testrigName, _queueIncompleteWork, WorkType.DATAPLANING);
    if (work != null) {
      return work;
    }
    return getTestrigWork(containerName, testrigName, _queueCompletedWork, WorkType.DATAPLANING);
  }

  public synchronized QueuedWork getParsingWork(String containerName, String testrigName) {
    QueuedWork work =
        getTestrigWork(containerName, testrigName, _queueIncompleteWork, WorkType.PARSING);
    if (work != null) {
      return work;
    }
    return getTestrigWork(containerName, testrigName, _queueCompletedWork, WorkType.PARSING);
  }

  private synchronized QueuedWork getTestrigWork(
      String containerName, String testrigName, WorkQueue queue, WorkType type) {
    for (QueuedWork work : queue) {
      if (work.getWorkItem().getContainerName().equals(containerName)
          && work.getDetails().baseTestrig.equals(testrigName)
          && work.getDetails().workType == type) {
        return work;
      }
    }
    return null;
  }

  public synchronized void makeWorkUnassigned(QueuedWork work) {
    work.setStatus(WorkStatusCode.UNASSIGNED);
  }

  // when assignment attempt ends in error, we do not try to reassign
  public synchronized void markAssignmentError(QueuedWork work) throws Exception {
    _queueIncompleteWork.delete(work);
    _queueCompletedWork.enque(work);
    work.setStatus(WorkStatusCode.ASSIGNMENTERROR);
  }

  public synchronized void markAssignmentFailure(QueuedWork work) {
    work.setStatus(WorkStatusCode.UNASSIGNED);
  }

  public synchronized void markAssignmentSuccess(QueuedWork work, String assignedWorker)
      throws IOException {
    work.setAssignment(assignedWorker);

    // update testrig metadata
    WorkItem wItem = work.getWorkItem();
    WorkDetails wDetails = work.getDetails();
    if (wDetails.workType == WorkType.PARSING) {
      TestrigMetadataMgr.updateEnvironmentStatus(
          wItem.getContainerName(),
          wDetails.baseTestrig,
          wDetails.baseEnv,
          ProcessingStatus.PARSING);
    } else if (wDetails.workType == WorkType.DATAPLANING) {
      TestrigMetadataMgr.updateEnvironmentStatus(
          wItem.getContainerName(),
          wDetails.baseTestrig,
          wDetails.baseEnv,
          ProcessingStatus.DATAPLANING);
    }
  }

  public synchronized void processTaskCheckResult(QueuedWork work, Task task) throws Exception {

    // {Unscheduled, InProgress, TerminatedNormally, TerminatedAbnormally, TerminatedByUser
    // Unknown, UnreachableOrBadResponse}

    switch (task.getStatus()) {
      case Unscheduled:
      case InProgress:
        work.setStatus(WorkStatusCode.ASSIGNED);
        work.recordTaskCheckResult(task);
        break;
      case TerminatedNormally:
      case TerminatedAbnormally:
      case TerminatedByUser:
        {
          // move the work to completed queue
          _queueIncompleteWork.delete(work);
          _queueCompletedWork.enque(work);
          work.setStatus(WorkStatusCode.fromTerminatedTaskStatus(task.getStatus()));
          work.recordTaskCheckResult(task);

          // update testrig metadata
          WorkItem wItem = work.getWorkItem();
          WorkDetails wDetails = work.getDetails();
          if (wDetails.workType == WorkType.PARSING) {
            ProcessingStatus status =
                (task.getStatus() == TaskStatus.TerminatedNormally)
                    ? ProcessingStatus.PARSED
                    : ProcessingStatus.PARSING_FAIL;
            TestrigMetadataMgr.updateEnvironmentStatus(
                wItem.getContainerName(), wDetails.baseTestrig, wDetails.baseEnv, status);
          } else if (wDetails.workType == WorkType.DATAPLANING) {
            ProcessingStatus status =
                (task.getStatus() == TaskStatus.TerminatedNormally)
                    ? ProcessingStatus.DATAPLANED
                    : ProcessingStatus.DATAPLANING_FAIL;
            TestrigMetadataMgr.updateEnvironmentStatus(
                wItem.getContainerName(), wDetails.baseTestrig, wDetails.baseEnv, status);
          }

          // check if we unblocked anything
          if (_blockingWork.contains(wItem.getId())) {
            _blockingWork.remove(wItem.getId());
            List<QueuedWork> requeueWorks = new LinkedList<>();
            for (QueuedWork incompleteWork : _queueIncompleteWork) {
              if (incompleteWork.getStatus() == WorkStatusCode.BLOCKED
                  && wDetails.isOverlappingInput(incompleteWork.getDetails())) {
                requeueWorks.add(incompleteWork);
              }
            }
            for (QueuedWork requeueWork : requeueWorks) {
              _queueIncompleteWork.delete(requeueWork);
              requeueWork.setStatus(WorkStatusCode.UNASSIGNED);
            }
            for (QueuedWork requeueWork : requeueWorks) {
              try {
                boolean queued = queueUnassignedWork(requeueWork);
                if (!queued) {
                  throw new BatfishException(
                      "Failed to requeue previously blocked work " + requeueWork.getId());
                }
              } catch (Exception e) {
                String stackTrace = ExceptionUtils.getFullStackTrace(e);
                _logger.errorf("exception: %s\n", stackTrace);
                // put this work back on incomplete queue and process as if it terminatedabnormally
                // people may be checking its status and this work may be blocking others
                _queueIncompleteWork.enque(requeueWork);
                Task fakeTask =
                    new Task(TaskStatus.TerminatedAbnormally, "Couldn't requeue after unblocking");
                processTaskCheckResult(requeueWork, fakeTask);
              }
            }
          }
        }
        break;
      case Unknown:
        // we mark this unassigned, so we try to schedule it again
        work.setStatus(WorkStatusCode.UNASSIGNED);
        work.clearAssignment();
        break;
      case UnreachableOrBadResponse:
        {
          if (work.getLastTaskCheckResult().getStatus() == TaskStatus.UnreachableOrBadResponse) {
            // if we saw the same thing last time around, free the task to be scheduled elsewhere
            work.setStatus(WorkStatusCode.UNASSIGNED);
            work.clearAssignment();
            work.recordTaskCheckResult(task);

            // update testrig metadata
            WorkItem wItem = work.getWorkItem();
            WorkDetails wDetails = work.getDetails();
            if (wDetails.workType == WorkType.PARSING
                || wDetails.workType == WorkType.DATAPLANING) {
              EnvironmentMetadata envMetadata =
                  TestrigMetadataMgr.getEnvironmentMetadata(
                      wItem.getContainerName(), wDetails.baseTestrig, wDetails.baseEnv);
              if (wDetails.workType == WorkType.PARSING) {
                if (envMetadata.getProcessingStatus() != ProcessingStatus.PARSING) {
                  _logger.errorf(
                      "Unexpected status %s when parsing failed for %s / %s",
                      envMetadata.getProcessingStatus(), wDetails.baseTestrig, wDetails.baseEnv);
                } else {
                  TestrigMetadataMgr.updateEnvironmentStatus(
                      wItem.getContainerName(),
                      wDetails.baseTestrig,
                      wDetails.baseEnv,
                      ProcessingStatus.UNINITIALIZED);
                }
              } else { // wDetails.workType == WorkType.DATAPLANING
                if (envMetadata.getProcessingStatus() != ProcessingStatus.DATAPLANING) {
                  _logger.errorf(
                      "Unexpected status %s when dataplaning failed for %s / %s",
                      envMetadata.getProcessingStatus(), wDetails.baseTestrig, wDetails.baseEnv);
                } else {
                  TestrigMetadataMgr.updateEnvironmentStatus(
                      wItem.getContainerName(),
                      wDetails.baseTestrig,
                      wDetails.baseEnv,
                      ProcessingStatus.PARSED);
                }
              }
            }
          } else {
            work.setStatus(WorkStatusCode.ASSIGNED);
            work.recordTaskCheckResult(task);
          }
        }
        break;
      default:
        throw new BatfishException(
            "Unhandled " + TaskStatus.class.getCanonicalName() + ": " + task.getStatus());
    }
  }

  private boolean queueAnsweringWork(QueuedWork work, boolean dataplaneDependent) throws Exception {
    WorkDetails wDetails = work.getDetails();

    QueuedWork baseBlocker =
        dataplaneDependent
            ? getBlockerForDataplaneDependentWork(work, wDetails.baseTestrig, wDetails.baseEnv)
            : getBlockerForDataplaneIndependentWork(work, wDetails.baseTestrig, wDetails.baseEnv);

    if (baseBlocker != null) {
      return queueBlockedWork(work, baseBlocker);
    } else if (wDetails.isDifferential) {
      QueuedWork deltaBlocker =
          dataplaneDependent
              ? getBlockerForDataplaneDependentWork(work, wDetails.deltaTestrig, wDetails.deltaEnv)
              : getBlockerForDataplaneIndependentWork(
                  work, wDetails.deltaTestrig, wDetails.deltaEnv);
      if (deltaBlocker != null) {
        return queueBlockedWork(work, deltaBlocker);
      }
    }
    return _queueIncompleteWork.enque(work);
  }

  private boolean queueBlockedWork(QueuedWork work, QueuedWork blocker) throws Exception {
    _blockingWork.add(blocker.getId());
    work.setStatus(WorkStatusCode.BLOCKED);
    return _queueIncompleteWork.enque(work);
  }

  private boolean queueDataplaningWork(QueuedWork work) throws Exception {

    WorkItem wItem = work.getWorkItem();
    WorkDetails wDetails = work.getDetails();

    QueuedWork currentDataplaningWork =
        getIncompleteWork(
            wItem.getContainerName(), wDetails.baseTestrig, wDetails.baseEnv, WorkType.DATAPLANING);
    if (currentDataplaningWork != null) {
      throw new BatfishException("Dataplaning is already in queue/progress");
    }

    // see comment in queueParsingWork for justification
    QueuedWork ddWork =
        getIncompleteWork(
            wItem.getContainerName(),
            wDetails.baseTestrig,
            wDetails.baseEnv,
            WorkType.DATAPLANE_DEPENDENT_ANSWERING);
    if (ddWork != null) {
      throw new BatfishException("Cannot queue dataplaning work while other dependent work exists");
    }

    QueuedWork blocker = getBlockerForDataplaningWork(work);
    if (blocker == null) {
      return _queueIncompleteWork.enque(work);
    } else {
      return queueBlockedWork(work, blocker);
    }
  }

  private synchronized boolean queueParsingWork(QueuedWork work) throws Exception {

    WorkItem wItem = work.getWorkItem();
    WorkDetails wDetails = work.getDetails();

    // if incomplete work for this testrig exists, lets just reject this parsing work.
    // parsing work cannot proceeed in parallel because it may overwrite files used by others.
    // instead of rejecting, we could have queued it as BLOCKED but we risk cycles of BLOCKED work
    // this should not be a common case anyway, so we aren't losing much by rejecting it
    QueuedWork incompleteWork =
        getIncompleteWork(wItem.getContainerName(), wDetails.baseTestrig, wDetails.baseEnv, null);
    if (incompleteWork != null) {
      throw new BatfishException("Cannot queue parsing work while other work is incomplete");
    } else {
      EnvironmentMetadata envMetadata =
          TestrigMetadataMgr.getEnvironmentMetadata(
              wItem.getContainerName(), wDetails.baseTestrig, wDetails.baseEnv);
      if (envMetadata.getProcessingStatus() == ProcessingStatus.PARSING) {
        throw new BatfishException(
            String.format(
                "Cannot queue parsing work for %s / %s: "
                    + "Status is PARSING but no incomplete parsing work exists",
                wDetails.baseTestrig, wDetails.baseEnv));
      } else if (envMetadata.getProcessingStatus() == ProcessingStatus.DATAPLANING) {
        throw new BatfishException(
            String.format(
                "Cannot queue parsing work for %s / %s: "
                    + "Status is DATAPLANING but no incomplete dataplaning work exists",
                wDetails.baseTestrig, wDetails.baseEnv));
      }
    }

    return _queueIncompleteWork.enque(work);
  }

  public synchronized boolean queueUnassignedWork(QueuedWork work) throws Exception {
    QueuedWork previouslyQueuedWork = getWork(work.getId());
    if (previouslyQueuedWork != null) {
      throw new BatfishException("Duplicate work item");
    }
    WorkDetails wDetails = work.getDetails();
    cleanUpEnvMetaDataIfNeeded(
        work.getWorkItem().getContainerName(), wDetails.baseTestrig, wDetails.baseEnv);
    if (work.getDetails().isDifferential) {
      cleanUpEnvMetaDataIfNeeded(
          work.getWorkItem().getContainerName(), wDetails.deltaTestrig, wDetails.deltaEnv);
    }
    switch (work.getDetails().workType) {
      case PARSING:
        return queueParsingWork(work);
      case DATAPLANING:
        return queueDataplaningWork(work);
      case DATAPLANE_DEPENDENT_ANSWERING:
        return queueAnsweringWork(work, true);
      case DATAPLANE_INDEPENDENT_ANSWERING:
        return queueAnsweringWork(work, false);
      case UNKNOWN:
        return _queueIncompleteWork.enque(work);
      default:
        throw new BatfishException("Unknown WorkType " + work.getDetails().workType);
    }
  }
}
