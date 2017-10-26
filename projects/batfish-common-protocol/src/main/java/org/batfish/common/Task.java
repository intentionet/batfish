package org.batfish.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.batfish.common.BfConsts.TaskStatus;
import org.batfish.common.util.BatfishObjectMapper;

public class Task {

  public static class Batch {

    private static final String PROP_COMPLETED = "completed";

    private static final String PROP_DESCRIPTION = "description";

    private static final String PROP_SIZE = "size";

    private static final String PROP_START_DATE = "startDate";

    private AtomicInteger _completed;

    private String _description;

    private int _size;

    private Date _startDate;

    @JsonCreator
    private Batch() {
      _completed = new AtomicInteger();
    }

    @JsonProperty(PROP_COMPLETED)
    public AtomicInteger getCompleted() {
      return _completed;
    }

    @JsonProperty(PROP_DESCRIPTION)
    public String getDescription() {
      return _description;
    }

    @JsonProperty(PROP_SIZE)
    public int getSize() {
      return _size;
    }

    @JsonProperty(PROP_START_DATE)
    public Date getStartDate() {
      return _startDate;
    }

    @JsonProperty(PROP_COMPLETED)
    private void setCompleted(AtomicInteger completed) {
      _completed = completed;
    }

    @JsonProperty(PROP_DESCRIPTION)
    public void setDescription(String description) {
      _description = description;
    }

    @JsonProperty(PROP_SIZE)
    public void setSize(int size) {
      _size = size;
    }

    @JsonProperty(PROP_START_DATE)
    public void setStartDate(Date startDate) {
      _startDate = startDate;
    }

    @Override
    public String toString() {
      if (_size > 0) {
        return String.format("%s: %s: %s/%d", _startDate, _description, _completed, _size);
      } else {
        return String.format("%s: %s", _startDate, _description);
      }
    }
  }

  private static final String PROP_ARGS = "args";

  private static final String PROP_OBTAINED = "obtained";

  private static final String PROP_STATUS = "status";

  private static final String PROP_TERMINATED = "terminated";

  private String[] _args;

  private List<Batch> _batches;

  private Date _obtained;

  private TaskStatus _status;

  private Date _terminated;

  private volatile boolean _terminationRequested;

  @JsonCreator
  public Task() {}

  public Task(@Nullable String[] args) {
    this._args = args;
    _batches = new ArrayList<>();
    _obtained = new Date();
    _terminated = null;
    _status = TaskStatus.Unscheduled;
  }

  @JsonProperty(PROP_ARGS)
  public String[] getArgs() {
    return _args;
  }

  public List<Batch> getBatches() {
    return _batches;
  }

  @JsonProperty(PROP_OBTAINED)
  public Date getObtained() {
    return _obtained;
  }

  @JsonProperty(PROP_STATUS)
  public TaskStatus getStatus() {
    return _status;
  }

  @JsonProperty(PROP_TERMINATED)
  public Date getTerminated() {
    return _terminated;
  }

  @JsonIgnore
  public boolean getTerminationRequested() {
    return _terminationRequested;
  }

  public Batch newBatch(String description) {
    Batch batch = new Batch();
    batch.setDescription(description);
    Date date = new Date();
    batch.setStartDate(date);
    _batches.add(batch);
    return batch;
  }

  @JsonIgnore
  public void requestTermination() {
    _terminationRequested = true;
  }

  @JsonProperty(PROP_ARGS)
  public void setArgs(String[] args) {
    _args = args;
  }

  public void setBatches(List<Batch> batches) {
    _batches = batches;
  }

  @JsonProperty(PROP_OBTAINED)
  public void setObtained(Date obtained) {
    _obtained = obtained;
  }

  @JsonProperty(PROP_STATUS)
  public void setStatus(TaskStatus status) {
    _status = status;
  }

  public void setTerminated() {
    _terminated = new Date();
  }

  @JsonProperty(PROP_TERMINATED)
  public void setTerminated(Date terminated) {
    _terminated = terminated;
  }

  public synchronized String updateAndWrite() throws JsonProcessingException {
    _obtained = new Date();
    BatfishObjectMapper mapper = new BatfishObjectMapper();
    return mapper.writeValueAsString(this);
  }
}
