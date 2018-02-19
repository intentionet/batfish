package org.batfish.datamodel.answers;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.batfish.common.Version;

public class ValidateEnvironmentAnswerElement implements AnswerElement, Serializable {

  private static final String PROP_UNDEFINED_INTERFACE_BLACKLIST_INTERFACES =
      "undefinedInterfaceBlacklistInterfaces";

  private static final String PROP_UNDEFINED_INTERFACE_BLACKLIST_NODES =
      "undefinedInterfaceBlacklistNodes";

  private static final String PROP_UNDEFINED_NODE_BLACKLIST_NODES = "undefinedNodeBlacklistNodes";

  private static final String PROP_UNDEFINED_NODE_ROLE_SPECIFIER_NODES =
      "undefinedNodeRoleSpecifierNodes";

  private static final String PROP_VALID = "valid";

  private static final String PROP_VERSION = "version";

  /** */
  private static final long serialVersionUID = 1L;

  private SortedMap<String, SortedSet<String>> _undefinedInterfaceBlacklistInterfaces;

  private SortedSet<String> _undefinedInterfaceBlacklistNodes;

  private SortedSet<String> _undefinedNodeBlacklistNodes;

  private SortedSet<String> _undefinedNodeRoleSpecifierNodes;

  private boolean _valid;

  private String _version;

  public ValidateEnvironmentAnswerElement() {
    _undefinedInterfaceBlacklistInterfaces = new TreeMap<>();
    _undefinedInterfaceBlacklistNodes = new TreeSet<>();
    _undefinedNodeBlacklistNodes = new TreeSet<>();
    _undefinedNodeRoleSpecifierNodes = new TreeSet<>();
    _valid = true;
    _version = Version.getVersion();
  }

  @JsonProperty(PROP_UNDEFINED_INTERFACE_BLACKLIST_INTERFACES)
  public SortedMap<String, SortedSet<String>> getUndefinedInterfaceBlacklistInterfaces() {
    return _undefinedInterfaceBlacklistInterfaces;
  }

  @JsonProperty(PROP_UNDEFINED_INTERFACE_BLACKLIST_NODES)
  public SortedSet<String> getUndefinedInterfaceBlacklistNodes() {
    return _undefinedInterfaceBlacklistNodes;
  }

  @JsonProperty(PROP_UNDEFINED_NODE_BLACKLIST_NODES)
  public SortedSet<String> getUndefinedNodeBlacklistNodes() {
    return _undefinedNodeBlacklistNodes;
  }

  @JsonProperty(PROP_UNDEFINED_NODE_ROLE_SPECIFIER_NODES)
  public SortedSet<String> getUndefinedNodeRoleSpecifierNodes() {
    return _undefinedNodeRoleSpecifierNodes;
  }

  @JsonProperty(PROP_VALID)
  public boolean getValid() {
    return _valid;
  }

  @JsonProperty(PROP_VERSION)
  public String getVersion() {
    return _version;
  }

  @Override
  public String prettyPrint() {
    StringBuilder retString =
        new StringBuilder("Results of environment processing and validation:\n");
    if (!_valid) {
      retString.append("ENVIRONMENT IS INVALID!\n");
    }
    if (!_undefinedNodeRoleSpecifierNodes.isEmpty()) {
      retString.append(PROP_UNDEFINED_NODE_ROLE_SPECIFIER_NODES + ":\n");
      for (String hostname : _undefinedNodeRoleSpecifierNodes) {
        retString.append("  " + hostname + "\n");
      }
    }
    if (!_undefinedNodeBlacklistNodes.isEmpty()) {
      retString.append(PROP_UNDEFINED_NODE_BLACKLIST_NODES + ":\n");
      for (String hostname : _undefinedNodeBlacklistNodes) {
        retString.append("  " + hostname + "\n");
      }
    }
    if (!_undefinedInterfaceBlacklistNodes.isEmpty()) {
      retString.append(PROP_UNDEFINED_INTERFACE_BLACKLIST_NODES + ":\n");
      for (String hostname : _undefinedInterfaceBlacklistNodes) {
        retString.append("  " + hostname + "\n");
      }
    }
    if (!_undefinedInterfaceBlacklistInterfaces.isEmpty()) {
      retString.append(PROP_UNDEFINED_INTERFACE_BLACKLIST_INTERFACES + ":\n");
      for (String hostname : _undefinedInterfaceBlacklistInterfaces.keySet()) {
        retString.append("  " + hostname + ":\n");
        SortedSet<String> ifaceNames = _undefinedInterfaceBlacklistInterfaces.get(hostname);
        for (String ifaceName : ifaceNames) {
          retString.append("    " + ifaceName + "\n");
        }
      }
    }
    return retString.toString();
  }

  @JsonProperty(PROP_UNDEFINED_INTERFACE_BLACKLIST_INTERFACES)
  public void setUndefinedInterfaceBlacklistInterfaces(
      SortedMap<String, SortedSet<String>> undefinedInterfaceBlacklistInterfaces) {
    _undefinedInterfaceBlacklistInterfaces = undefinedInterfaceBlacklistInterfaces;
  }

  @JsonProperty(PROP_UNDEFINED_INTERFACE_BLACKLIST_NODES)
  public void setUndefinedInterfaceBlacklistNodes(
      SortedSet<String> undefinedInterfaceBlacklistNodes) {
    _undefinedInterfaceBlacklistNodes = undefinedInterfaceBlacklistNodes;
  }

  @JsonProperty(PROP_UNDEFINED_NODE_BLACKLIST_NODES)
  public void setUndefinedNodeBlacklistNodes(SortedSet<String> undefinedNodeBlacklistNodes) {
    _undefinedNodeBlacklistNodes = undefinedNodeBlacklistNodes;
  }

  @JsonProperty(PROP_UNDEFINED_NODE_ROLE_SPECIFIER_NODES)
  public void setUndefinedNodeRoleSpecifierNodes(
      SortedSet<String> undefinedNodeRoleSpecifierNodes) {
    _undefinedNodeRoleSpecifierNodes = undefinedNodeRoleSpecifierNodes;
  }

  @JsonProperty(PROP_VALID)
  public void setValid(boolean valid) {
    _valid = valid;
  }

  @JsonProperty(PROP_VERSION)
  public void setVersion(String version) {
    _version = version;
  }
}
