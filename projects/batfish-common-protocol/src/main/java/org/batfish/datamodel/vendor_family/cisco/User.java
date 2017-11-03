package org.batfish.datamodel.vendor_family.cisco;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.util.ComparableStructure;

public class User extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private String _password;

  private String _role;

  public User(@JsonProperty(PROP_NAME) String name) {
    super(name);
  }

  public String getPassword() {
    return _password;
  }

  public String getRole() {
    return _role;
  }

  public void setPassword(String password) {
    _password = password;
  }

  public void setRole(String role) {
    _role = role;
  }
}
