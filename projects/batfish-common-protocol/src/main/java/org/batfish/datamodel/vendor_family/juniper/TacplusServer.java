package org.batfish.datamodel.vendor_family.juniper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.Ip;

public class TacplusServer extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private String _secret;

  private Ip _sourceAddress;

  @JsonCreator
  public TacplusServer(@JsonProperty(PROP_NAME) String name) {
    super(name);
  }

  public String getSecret() {
    return _secret;
  }

  public Ip getSourceAddress() {
    return _sourceAddress;
  }

  public void setSecret(String secret) {
    _secret = secret;
  }

  public void setSourceAddress(Ip sourceAddress) {
    _sourceAddress = sourceAddress;
  }
}
