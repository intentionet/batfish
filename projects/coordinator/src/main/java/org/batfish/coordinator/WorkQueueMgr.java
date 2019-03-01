package org.batfish.coordinator;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BfConsts.TaskStatus;
import org.batfish.common.CoordConsts.WorkStatusCode;
import org.batfish.common.Task;
import org.batfish.common.WorkItem;
import org.batfish.common.util.WorkItemBuilder;
import org.batfish.coordinator.WorkDetails.WorkType;
import org.batfish.coordinator.queues.MemoryQueue;
import org.batfish.coordinator.queues.WorkQueue;
import org.batfish.datamodel.InitializationMetadata;
import org.batfish.datamodel.InitializationMetadata.ProcessingStatus;
import org.batfish.identifiers.NetworkId;
import org.batfish.identifiers.SnapshotId;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

// the design of this WorkQueueMgr is such that all synchronization sits here
// individual queues do not need to be synchronized

public class WorkQueueMgr {

  public enum QueueType {
    COMPLETED,
    INCOMPLETE
  }

  @GuardedBy("this")
  private Set<UUID> _blockingWork;

  private BatfishLogger _logger;

  @GuardedBy("this")
  private WorkQueue _queueCompletedWork;

  @GuardedBy("this")
  private WorkQueue _queueIncompleteWork;

  WorkQueueMgr(BatfishLogger logger) {
    this(Main.getSettings().getQueueType(), logger);
  }

  WorkQueueMgr(WorkQueue.Type wqType, BatfishLogger logger) {
    _blockingWork = new HashSet<>();
    _logger = logger;
    switch (wqType) {
      case memory:
        _queueCompletedWork = new MemoryQueue();
        _queueIncompleteWork = new MemoryQueue();
        break;
      default:
        throw new BatfishException("Unsupported queue type: " + wqType);
    }
  }

  private void cleanUpInitMetaDataIfNeeded(NetworkId networkId, SnapshotId snapshotId)
      throws IOException {
    InitializationMetadata metadata =
        SnapshotMetadataMgr.getInitializationMetadata(networkId, snapshotId);
    if (metadata.getProcessingStatus() == ProcessingStatus.PARSING
        && getIncompleteWork(networkId, snapshotId, WorkType.PARSING) == null) {
      SnapshotMetadataMgr.updateInitializationStatus(
          networkId, snapshotId, ProcessingStatus.PARSING_FAIL, null);
    } else if (metadata.getProcessingStatus() == ProcessingStatus.DATAPLANING
        && getIncompleteWork(networkId, snapshotId, WorkType.DATAPLANING) == null) {
      SnapshotMetadataMgr.updateInitializationStatus(
          networkId, snapshotId, ProcessingStatus.DATAPLANING_FAIL, null);
    }
  }

  private QueuedWork generateAndQueueDataplaneWork(
      String network, NetworkId networkId, String snapshot, SnapshotId snapshotId)
      throws Exception {
    WorkItem newWItem = WorkItemBuilder.getWorkItemGenerateDataPlane(network, snapshot);
    WorkDetails details =
        WorkDetails.builder()
            .setWorkType(WorkType.DATAPLANING)
            .setSnapshotId(snapshotId)
            .setNetworkId(networkId)
            .build();
    QueuedWork newWork = new QueuedWork(newWItem, details);
    boolean queued = queueUnassignedWork(newWork);
    if (!queued) {
      throw new BatfishException("Failed to auto-queue dataplane work");
    }
    return newWork;
  }

  private QueuedWork getBlockerForDataplaningWork(QueuedWork work) throws IOException {
    WorkDetails wDetails = work.getDetails();

    QueuedWork currentParsingWork =
        getIncompleteWork(wDetails.getNetworkId(), wDetails.getSnapshotId(), WorkType.PARSING);

    InitializationMetadata metadata =
        SnapshotMetadataMgr.getInitializationMetadata(
            wDetails.getNetworkId(), wDetails.getSnapshotId());

    switch (metadata.getProcessingStatus()) {
      case UNINITIALIZED:
      case PARSING_FAIL:
      case PARSING:
        if (currentParsingWork == null) {
          throw new BatfishException(
              String.format(
                  "Cannot queue dataplane work for %s: Status is %s but no incomplete parsing work exists",
                  wDetails.getSnapshotId(), metadata.getProcessingStatus()));
        }
        return currentParsingWork;
      case PARSED:
        return currentParsingWork;
      case DATAPLANING:
        // we get here only when currentDataplaningWork is null; by virtue of the calling context
        throw new BatfishException(
            String.format(
                "Cannot queue dataplane work for %s: Status is %s but no incomplete dataplaning work exists",
                wDetails.getSnapshotId(), metadata.getProcessingStatus()));
      case DATAPLANED:
      case DATAPLANING_FAIL:
        return null;
      default:
        throw new BatfishException(
            "Unknown snapshot processingStatus: " + metadata.getProcessingStatus());
    }
  }

  /**
   * This function has a side effect It will inject a dataplane generation work in the queue if none
   * exists
   */
  private QueuedWork getBlockerForDataplaneDependentWork(
      QueuedWork work, String snapshot, SnapshotId snapshotId) throws Exception {

    WorkItem wItem = work.getWorkItem();
    NetworkId networkId = work.getDetails().getNetworkId();

    InitializationMetadata metadata =
        SnapshotMetadataMgr.getInitializationMetadata(networkId, snapshotId);

    QueuedWork parsingWork = getIncompleteWork(networkId, snapshotId, WorkType.PARSING);

    QueuedWork dataplaningWork = getIncompleteWork(networkId, snapshotId, WorkType.DATAPLANING);

    switch (metadata.getProcessingStatus()) {
      case UNINITIALIZED:
      case PARSING_FAIL:
      case PARSING:
        if (parsingWork == null) {
          throw new BatfishException(
              String.format(
                  "Cannot queue dataplane dependent work for %s: "
                      + "Status is %s but no incomplete parsing work exists",
                  snapshotId, metadata.getProcessingStatus()));
        }
        return parsingWork;
      case PARSED:
        if (parsingWork != null) {
          return parsingWork;
        }
        if (dataplaningWork != null) {
          return dataplaningWork;
        }
        return generateAndQueueDataplaneWork(
            wItem.getNetwork(), work.getDetails().getNetworkId(), snapshot, snapshotId);
      case DATAPLANING_FAIL:
      case DATAPLANING:
        if (dataplaningWork == null) {
          throw new BatfishException(
              String.format(
                  "Cannot queue dataplane dependent work for %s: "
                      + "Status is %s but no incomplete dataplaning work exists",
                  snapshotId, metadata.getProcessingStatus()));
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
            "Unknown snapshot processingStatus: " + metadata.getProcessingStatus());
    }
  }

  private QueuedWork getBlockerForParsingDependentWork(
      QueuedWork work, String snapshot, SnapshotId snapshotId) throws IOException {

    NetworkId networkId = work.getDetails().getNetworkId();

    InitializationMetadata metadata =
        SnapshotMetadataMgr.getInitializationMetadata(networkId, snapshotId);

    QueuedWork parsingWork = getIncompleteWork(networkId, snapshotId, WorkType.PARSING);

    switch (metadata.getProcessingStatus()) {
      case UNINITIALIZED:
      case PARSING:
      case PARSING_FAIL:
        if (parsingWork == null) {
          throw new BatfishException(
              String.format(
                  "Cannot queue parsing dependent work for %s: "
                      + "Status is %s but no incomplete parsing work exists",
                  snapshot, metadata.getProcessingStatus()));
        }
        return parsingWork;
      case PARSED:
      case DATAPLANING:
      case DATAPLANED:
      case DATAPLANING_FAIL:
        return parsingWork;
      default:
        throw new BatfishException(
            "Unknown snapshot processingStatus: " + metadata.getProcessingStatus());
    }
  }

  /**
   * Get all completed work for the specified network and snapshot.
   *
   * @param networkId {@link NetworkId} to get completed work for.
   * @param snapshotId {@link SnapshotId} to get completed work for.
   * @return {@link List} of completed {@link QueuedWork}.
   */
  public synchronized List<QueuedWork> getCompletedWork(
      NetworkId networkId, SnapshotId snapshotId) {
    ImmutableList.Builder<QueuedWork> b = ImmutableList.builder();
    for (QueuedWork work : _queueCompletedWork) {
      if (work.getDetails().getNetworkId().equals(networkId)
          && work.getDetails().getSnapshotId().equals(snapshotId)) {
        b.add(work);
      }
    }
    return b.build();
  }

  private synchronized QueuedWork getIncompleteWork(
      NetworkId networkId, SnapshotId snapshotId, WorkType wType) {
    for (QueuedWork work : _queueIncompleteWork) {
      WorkDetails wDetails = work.getDetails();
      if (networkId.equals(work.getDetails().getNetworkId())
          && ((snapshotId.equals(wDetails.getSnapshotId()))
              || (wDetails.isDifferential()
                  && snapshotId.equals(wDetails.getReferenceSnapshotId())))
          && (wType == null || wDetails.getWorkType() == wType)) {
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
  public synchronized List<QueuedWork> getWorkForChecking() {
    List<QueuedWork> workToCheck = new ArrayList<>();
    for (QueuedWork work : _queueIncompleteWork) {
      if (work.getStatus() == WorkStatusCode.ASSIGNED) {
        work.setStatus(WorkStatusCode.CHECKINGSTATUS);
        workToCheck.add(work);
      }
    }
    return workToCheck;
  }

  public synchronized List<QueuedWork> listIncompleteWork(
      NetworkId networkId, @Nullable SnapshotId snapshotId, @Nullable WorkType workType) {
    List<QueuedWork> retList = new LinkedList<>();
    for (QueuedWork work : _queueIncompleteWork) {
      // Add to queue if it matches container, testrig if provided, and work type if provided
      if (work.getDetails().getNetworkId().equals(networkId)
          && (snapshotId == null || work.getDetails().getSnapshotId().equals(snapshotId))
          && (workType == null || work.getDetails().getWorkType() == workType)) {
        retList.add(work);
      }
    }
    return retList;
  }

  public synchronized void makeWorkUnassigned(QueuedWork work) {
    work.setStatus(WorkStatusCode.UNASSIGNED);
  }

  // when assignment attempt ends in error, we do not try to reassign
  public synchronized void markAssignmentError(QueuedWork work) {
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
    WorkDetails wDetails = work.getDetails();
    if (wDetails.getWorkType() == WorkType.PARSING) {
      SnapshotMetadataMgr.updateInitializationStatus(
          wDetails.getNetworkId(), wDetails.getSnapshotId(), ProcessingStatus.PARSING, null);
    } else if (wDetails.getWorkType() == WorkType.DATAPLANING) {
      SnapshotMetadataMgr.updateInitializationStatus(
          wDetails.getNetworkId(), wDetails.getSnapshotId(), ProcessingStatus.DATAPLANING, null);
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
      case TerminatedAbnormally:
      case TerminatedByUser:
      case TerminatedNormally:
      case RequeueFailure:
        {
          // move the work to completed queue
          _queueIncompleteWork.delete(work);
          _queueCompletedWork.enque(work);
          work.setStatus(WorkStatusCode.fromTerminatedTaskStatus(task.getStatus()));
          work.recordTaskCheckResult(task);

          // update testrig metadata
          WorkItem wItem = work.getWorkItem();
          WorkDetails wDetails = work.getDetails();
          if (wDetails.getWorkType() == WorkType.PARSING) {
            ProcessingStatus status;
            if (task.getStatus() == TaskStatus.TerminatedNormally) {
              status = ProcessingStatus.PARSED;
              Main.getWorkMgr()
                  .tryPromoteSnapshotNodeRoles(wDetails.getNetworkId(), wDetails.getSnapshotId());
            } else {
              status = ProcessingStatus.PARSING_FAIL;
            }
            SnapshotMetadataMgr.updateInitializationStatus(
                wDetails.getNetworkId(), wDetails.getSnapshotId(), status, task.getErrMessage());
          } else if (wDetails.getWorkType() == WorkType.DATAPLANING) {
            // no change in status needed if task.getStatus() is RequeueFailure
            if (task.getStatus() == TaskStatus.TerminatedAbnormally
                || task.getStatus() == TaskStatus.TerminatedByUser) {
              SnapshotMetadataMgr.updateInitializationStatus(
                  wDetails.getNetworkId(),
                  wDetails.getSnapshotId(),
                  ProcessingStatus.DATAPLANING_FAIL,
                  task.getErrMessage());
            } else if (task.getStatus() == TaskStatus.TerminatedNormally) {
              SnapshotMetadataMgr.updateInitializationStatus(
                  wDetails.getNetworkId(),
                  wDetails.getSnapshotId(),
                  ProcessingStatus.DATAPLANED,
                  null);
            }
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
                String stackTrace = Throwables.getStackTraceAsString(e);
                _logger.errorf("exception: %s\n", stackTrace);
                // put this work back on incomplete queue and process as if it terminatedabnormally
                // people may be checking its status and this work may be blocking others
                _queueIncompleteWork.enque(requeueWork);
                Task fakeTask =
                    new Task(
                        TaskStatus.RequeueFailure,
                        String.format("Couldn't requeue after unblocking.\n%s", e.getMessage()));
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

            // update snapshot metadata
            WorkDetails wDetails = work.getDetails();
            if (wDetails.getWorkType() == WorkType.PARSING
                || wDetails.getWorkType() == WorkType.DATAPLANING) {
              InitializationMetadata metadata =
                  SnapshotMetadataMgr.getInitializationMetadata(
                      wDetails.getNetworkId(), wDetails.getSnapshotId());
              if (wDetails.getWorkType() == WorkType.PARSING) {
                if (metadata.getProcessingStatus() != ProcessingStatus.PARSING) {
                  _logger.errorf(
                      "Unexpected status %s when parsing failed for %s",
                      metadata.getProcessingStatus(), wDetails.getSnapshotId());
                } else {
                  SnapshotMetadataMgr.updateInitializationStatus(
                      wDetails.getNetworkId(),
                      wDetails.getSnapshotId(),
                      ProcessingStatus.UNINITIALIZED,
                      task.getErrMessage());
                }
              } else { // wDetails.getWorkType() == WorkType.DATAPLANING
                if (metadata.getProcessingStatus() != ProcessingStatus.DATAPLANING) {
                  _logger.errorf(
                      "Unexpected status %s when dataplaning failed for %s",
                      metadata.getProcessingStatus(), wDetails.getSnapshotId());
                } else {
                  SnapshotMetadataMgr.updateInitializationStatus(
                      wDetails.getNetworkId(),
                      wDetails.getSnapshotId(),
                      ProcessingStatus.PARSED,
                      task.getErrMessage());
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

  private synchronized boolean queueDependentAnsweringWork(
      QueuedWork work, boolean dataplaneDependent) throws Exception {
    WorkDetails wDetails = work.getDetails();

    QueuedWork baseBlocker =
        dataplaneDependent
            ? getBlockerForDataplaneDependentWork(
                work, work.getWorkItem().getSnapshot(), wDetails.getSnapshotId())
            : getBlockerForParsingDependentWork(
                work, work.getWorkItem().getSnapshot(), wDetails.getSnapshotId());

    if (baseBlocker != null) {
      return queueBlockedWork(work, baseBlocker);
    } else if (wDetails.isDifferential()) {
      QueuedWork deltaBlocker =
          dataplaneDependent
              ? getBlockerForDataplaneDependentWork(
                  work,
                  WorkItemBuilder.getReferenceSnapshotName(work.getWorkItem()),
                  wDetails.getReferenceSnapshotId())
              : getBlockerForParsingDependentWork(
                  work,
                  WorkItemBuilder.getReferenceSnapshotName(work.getWorkItem()),
                  wDetails.getReferenceSnapshotId());
      if (deltaBlocker != null) {
        return queueBlockedWork(work, deltaBlocker);
      }
    }
    return _queueIncompleteWork.enque(work);
  }

  private synchronized boolean queueBlockedWork(QueuedWork work, QueuedWork blocker) {
    _blockingWork.add(blocker.getId());
    work.setStatus(WorkStatusCode.BLOCKED);
    return _queueIncompleteWork.enque(work);
  }

  private synchronized boolean queueDataplaningWork(QueuedWork work) throws Exception {
    WorkDetails wDetails = work.getDetails();
    QueuedWork currentDataplaningWork =
        getIncompleteWork(wDetails.getNetworkId(), wDetails.getSnapshotId(), WorkType.DATAPLANING);
    if (currentDataplaningWork != null) {
      throw new BatfishException("Dataplaning is already in queue/progress");
    }

    // see comment in queueParsingWork for justification
    QueuedWork ddWork =
        getIncompleteWork(
            wDetails.getNetworkId(),
            wDetails.getSnapshotId(),
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

    WorkDetails wDetails = work.getDetails();

    // if incomplete work for this testrig exists, lets just reject this parsing work.
    // parsing work cannot proceeed in parallel because it may overwrite files used by others.
    // instead of rejecting, we could have queued it as BLOCKED but we risk cycles of BLOCKED work
    // this should not be a common case anyway, so we aren't losing much by rejecting it
    QueuedWork incompleteWork =
        getIncompleteWork(wDetails.getNetworkId(), wDetails.getSnapshotId(), null);
    if (incompleteWork != null) {
      throw new BatfishException("Cannot queue parsing work while other work is incomplete");
    } else {
      InitializationMetadata metadata =
          SnapshotMetadataMgr.getInitializationMetadata(
              wDetails.getNetworkId(), wDetails.getSnapshotId());
      if (metadata.getProcessingStatus() == ProcessingStatus.PARSING) {
        throw new BatfishException(
            String.format(
                "Cannot queue parsing work for %s: "
                    + "Status is PARSING but no incomplete parsing work exists",
                wDetails.getSnapshotId()));
      } else if (metadata.getProcessingStatus() == ProcessingStatus.DATAPLANING) {
        throw new BatfishException(
            String.format(
                "Cannot queue parsing work for %s: "
                    + "Status is DATAPLANING but no incomplete dataplaning work exists",
                wDetails.getSnapshotId()));
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
    cleanUpInitMetaDataIfNeeded(work.getDetails().getNetworkId(), wDetails.getSnapshotId());
    if (work.getDetails().isDifferential()) {
      cleanUpInitMetaDataIfNeeded(
          work.getDetails().getNetworkId(), wDetails.getReferenceSnapshotId());
    }
    switch (work.getDetails().getWorkType()) {
      case PARSING:
        return queueParsingWork(work);
      case DATAPLANING:
        return queueDataplaningWork(work);
      case INDEPENDENT_ANSWERING:
        // assume that this type of work shouldn't be blocked at all
        return _queueIncompleteWork.enque(work);
      case PARSING_DEPENDENT_ANSWERING:
        return queueDependentAnsweringWork(work, false);
      case DATAPLANE_DEPENDENT_ANSWERING:
        return queueDependentAnsweringWork(work, true);
      case UNKNOWN:
        return _queueIncompleteWork.enque(work);
      default:
        throw new BatfishException("Unknown WorkType " + work.getDetails().getWorkType());
    }
  }
}
