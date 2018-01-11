package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.util.ComparableStructure;

public class VrrpGroup extends ComparableStructure<Integer> {

  private static final String PROP_PREEMPT = "preempt";

  private static final String PROP_PRIORITY = "priority";

  /** */
  private static final long serialVersionUID = 1L;

  private static final String PROP_VIRTUAL_ADDRESS = "virtualAddress";

  private boolean _preempt;

  private int _priority;

  private NetworkAddress _virtualAddress;

  @JsonCreator
  public VrrpGroup(@JsonProperty(PROP_NAME) Integer name) {
    super(name);
  }

  @JsonProperty(PROP_PREEMPT)
  public boolean getPreempt() {
    return _preempt;
  }

  @JsonProperty(PROP_PRIORITY)
  public int getPriority() {
    return _priority;
  }

  @JsonProperty(PROP_VIRTUAL_ADDRESS)
  public NetworkAddress getVirtualAddress() {
    return _virtualAddress;
  }

  @JsonProperty(PROP_PREEMPT)
  public void setPreempt(boolean preempt) {
    _preempt = preempt;
  }

  @JsonProperty(PROP_PRIORITY)
  public void setPriority(int priority) {
    _priority = priority;
  }

  @JsonProperty(PROP_VIRTUAL_ADDRESS)
  public void setVirtualAddress(NetworkAddress virtualAddress) {
    _virtualAddress = virtualAddress;
  }
}
