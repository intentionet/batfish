package org.batfish.coordinator;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;
import org.batfish.common.BatfishException;
import org.batfish.common.CoordConsts.WorkStatusCode;
import org.batfish.common.WorkItem;
import org.batfish.coordinator.WorkQueueMgr.QueueType;
import org.batfish.coordinator.queues.WorkQueue.Type;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests for {@link WorkQueueMgr}. */
public class WorkQueueMgrTest {

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void qetMatchingWorkAbsent() throws Exception {
    WorkQueueMgr workQueueMgr = new WorkQueueMgr(Type.memory);
    WorkItem wItem1 = new WorkItem("container", "testrig");
    wItem1.addRequestParam("key1", "value1");
    QueuedWork work1 = new QueuedWork(wItem1);
    QueuedWork work2 = new QueuedWork(new WorkItem("container", "testrig"));
    workQueueMgr.queueUnassignedWork(work1);
    workQueueMgr.queueUnassignedWork(work2);

    // build a work item that matches none of the queued works
    WorkItem wItem3 = new WorkItem("container", "testrig");
    wItem3.addRequestParam("key3", "value3");

    QueuedWork matchingWork = workQueueMgr.getMatchingWork(wItem3, QueueType.INCOMPLETE);

    assertThat(matchingWork, equalTo(null));
  }

  @Test
  public void qetMatchingWorkPresent() throws Exception {
    WorkQueueMgr workQueueMgr = new WorkQueueMgr(Type.memory);
    WorkItem wItem1 = new WorkItem("container", "testrig");
    wItem1.addRequestParam("key1", "value1");
    QueuedWork work1 = new QueuedWork(wItem1);
    QueuedWork work2 = new QueuedWork(new WorkItem("container", "testrig"));
    workQueueMgr.queueUnassignedWork(work1);
    workQueueMgr.queueUnassignedWork(work2);

    // build a work item that should match wItem1
    WorkItem wItem3 = new WorkItem("container", "testrig");
    wItem3.addRequestParam("key1", "value1");

    QueuedWork matchingWork = workQueueMgr.getMatchingWork(wItem3, QueueType.INCOMPLETE);

    assertThat(matchingWork, equalTo(work1));
  }

  @Test
  public void testGetWorkForChecking() throws Exception {
    WorkQueueMgr workQueueMgr = new WorkQueueMgr(Type.memory);

    List<QueuedWork> workToCheck = workQueueMgr.getWorkForChecking();
    assertThat(workToCheck.size(), equalTo(0));

    QueuedWork work1 = new QueuedWork(new WorkItem("container", "testrig"));
    QueuedWork work2 = new QueuedWork(new WorkItem("container", "testrig"));
    workQueueMgr.queueUnassignedWork(work1);
    workQueueMgr.queueUnassignedWork(work2);
    workToCheck = workQueueMgr.getWorkForChecking();
    assertThat(workToCheck.size(), equalTo(0));

    work2.setStatus(WorkStatusCode.ASSIGNED);
    workToCheck = workQueueMgr.getWorkForChecking();
    assertThat(workToCheck.size(), equalTo(1));

    // After getWorkForChecking(), work2 should have status CHECKINGSTATUS and
    // therefore no longer show up in the workListToCheck
    assertThat(work2.getStatus(), equalTo(WorkStatusCode.CHECKINGSTATUS));
    workToCheck = workQueueMgr.getWorkForChecking();
    assertThat(workToCheck.size(), equalTo(0));

    work1.setStatus(WorkStatusCode.ASSIGNED);
    work2.setStatus(WorkStatusCode.ASSIGNED);
    workToCheck = workQueueMgr.getWorkForChecking();
    assertThat(workToCheck.size(), equalTo(2));
  }

  @Test
  public void queueUnassignedWork() throws Exception {
    WorkQueueMgr workQueueMgr = new WorkQueueMgr(Type.memory);
    QueuedWork work1 = new QueuedWork(new WorkItem("container", "testrig"));
    workQueueMgr.queueUnassignedWork(work1);
    assertThat(workQueueMgr.getLength(QueueType.INCOMPLETE), equalTo(1L));
    QueuedWork work2 = new QueuedWork(new WorkItem("container", "testrig"));
    workQueueMgr.queueUnassignedWork(work2);
    assertThat(workQueueMgr.getLength(QueueType.INCOMPLETE), equalTo(2L));
  }

  @Test
  public void queueUnassignedWorkDuplicate() throws Exception {
    WorkQueueMgr workQueueMgr = new WorkQueueMgr(Type.memory);
    QueuedWork work1 = new QueuedWork(new WorkItem("container", "testrig"));
    workQueueMgr.queueUnassignedWork(work1);

    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("Duplicate work item");

    workQueueMgr.queueUnassignedWork(work1);
  }
}
