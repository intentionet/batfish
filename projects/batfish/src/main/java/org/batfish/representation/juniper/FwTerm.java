package org.batfish.representation.juniper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class FwTerm implements Serializable {

  /** */
  private static final long serialVersionUID = 1L;

  private final List<FwFromApplicationOrApplicationSet> _fromApplicationsOrApplicationSets;

  private final List<FwFromJunosApplication> _fromJunosApplications;

  private final List<FwFromJunosApplicationSet> _fromJunosApplicationSets;

  private final List<FwFromHostProtocol> _fromHostProtocols;

  private final List<FwFromHostService> _fromHostServices;

  private final List<FwFrom> _froms;

  private boolean _ipv6;

  private final String _name;

  private final List<FwThen> _thens;

  public FwTerm(String name) {
    _froms = new ArrayList<>();
    _fromApplicationsOrApplicationSets = new ArrayList<>();
    _fromJunosApplications = new ArrayList<>();
    _fromJunosApplicationSets = new ArrayList<>();
    _fromHostProtocols = new ArrayList<>();
    _fromHostServices = new ArrayList<>();
    _name = name;
    _thens = new ArrayList<>();
  }

  public List<FwFromApplicationOrApplicationSet> getFromApplicationsOrApplicationSets() {
    return _fromApplicationsOrApplicationSets;
  }

  public List<FwFromJunosApplication> getFromJunosApplications() {
    return _fromJunosApplications;
  }

  public List<FwFromJunosApplicationSet> getFromJunosApplicationSets() {
    return _fromJunosApplicationSets;
  }

  public List<FwFromHostProtocol> getFromHostProtocols() {
    return _fromHostProtocols;
  }

  public List<FwFromHostService> getFromHostServices() {
    return _fromHostServices;
  }

  public List<FwFrom> getFroms() {
    return _froms;
  }

  public boolean getIpv6() {
    return _ipv6;
  }

  public String getName() {
    return _name;
  }

  public List<FwThen> getThens() {
    return _thens;
  }

  public void setIpv6(boolean ipv6) {
    _ipv6 = ipv6;
  }
}
