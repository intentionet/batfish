package org.batfish.datamodel.pojo;

import java.util.Map;

public abstract class BfObject {

  private String _id;

  private Map<String, String> _properties;

  public BfObject(String id) {
    _id = id;
  }

  public String getId() {
    return _id;
  }

  public Map<String, String> getProperties() {
    return _properties;
  }

  public void setId(String id) {
    _id = id;
  }

  public void getProperties(Map<String, String> properties) {
    _properties = properties;
  }
}
