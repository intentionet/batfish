package org.batfish.representation.cisco;

import java.util.HashSet;
import java.util.Set;
import org.batfish.common.util.ComparableStructure;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfConfiguration;

public final class Vrf extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private CiscoNxBgpVrfConfiguration _bgpNxConfig;

  private BgpProcess _bgpProcess;

  private String _description;

  private IsisProcess _isisProcess;

  private OspfProcess _ospfProcess;

  private RipProcess _ripProcess;

  private final Set<StaticRoute> _staticRoutes;

  public Vrf(String name) {
    super(name);
    _staticRoutes = new HashSet<>();
  }

  public CiscoNxBgpVrfConfiguration getBgpNxConfig() {
    return _bgpNxConfig;
  }

  public BgpProcess getBgpProcess() {
    return _bgpProcess;
  }

  public String getDescription() {
    return _description;
  }

  public IsisProcess getIsisProcess() {
    return _isisProcess;
  }

  public OspfProcess getOspfProcess() {
    return _ospfProcess;
  }

  public RipProcess getRipProcess() {
    return _ripProcess;
  }

  public Set<StaticRoute> getStaticRoutes() {
    return _staticRoutes;
  }

  public void setBgpNxConfig(CiscoNxBgpVrfConfiguration bgpNxConfig) {
    _bgpNxConfig = bgpNxConfig;
  }

  public void setBgpProcess(BgpProcess bgpProcess) {
    _bgpProcess = bgpProcess;
  }

  public void setDescription(String description) {
    _description = description;
  }

  public void setIsisProcess(IsisProcess isisProcess) {
    _isisProcess = isisProcess;
  }

  public void setOspfProcess(OspfProcess proc) {
    _ospfProcess = proc;
  }

  public void setRipProcess(RipProcess ripProcess) {
    _ripProcess = ripProcess;
  }
}
