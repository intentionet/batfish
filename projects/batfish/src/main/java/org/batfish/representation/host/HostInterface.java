package org.batfish.representation.host;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.Vrf;

public class HostInterface implements Serializable {

  private static final String PROP_BANDWIDTH = "bandwidth";

  private static final String PROP_GATEWAY = "gateway";

  private static final String PROP_NAME = "name";

  private static final String PROP_OTHER_PREFIXES = "otherPrefixes";

  private static final String PROP_PREFIX = "prefix";

  private static final String PROP_SHARED = "shared";

  private static final String PROP_VRF = "vrf";

  /** */
  private static final long serialVersionUID = 1L;

  private Double _bandwidth = 1000 * 1000 * 1000.0; // default is 1 Gbps

  private transient String _canonicalName;

  private Ip _gateway;

  private String _name;

  private Set<Prefix> _otherPrefixes;

  private Prefix _prefix;

  private boolean _shared;

  private Vrf _vrf;

  @JsonCreator
  public HostInterface(@JsonProperty(PROP_NAME) String name) {
    _name = name;
    _otherPrefixes = new TreeSet<>();
  }

  @JsonProperty(PROP_BANDWIDTH)
  public Double getBandwidth() {
    return _bandwidth;
  }

  @JsonIgnore
  public String getCanonicalName() {
    return _canonicalName;
  }

  @JsonProperty(PROP_GATEWAY)
  public Ip getGateway() {
    return _gateway;
  }

  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }

  @JsonProperty(PROP_OTHER_PREFIXES)
  public Set<Prefix> getOtherPrefixes() {
    return _otherPrefixes;
  }

  @JsonProperty(PROP_PREFIX)
  public Prefix getPrefix() {
    return _prefix;
  }

  @JsonProperty(PROP_SHARED)
  public boolean getShared() {
    return _shared;
  }

  @JsonProperty(PROP_VRF)
  public Vrf getVrf() {
    return _vrf;
  }

  @JsonProperty(PROP_BANDWIDTH)
  public void setBandwidth(Double bandwidth) {
    _bandwidth = bandwidth;
  }

  @JsonIgnore
  public void setCanonicalName(String canonicalName) {
    _canonicalName = canonicalName;
  }

  @JsonProperty(PROP_GATEWAY)
  public void setGateway(Ip gateway) {
    _gateway = gateway;
  }

  @JsonProperty(PROP_OTHER_PREFIXES)
  public void setOtherPrefixes(Set<Prefix> otherPrefixes) {
    _otherPrefixes = otherPrefixes;
  }

  @JsonProperty(PROP_PREFIX)
  public void setPrefix(Prefix prefix) {
    _prefix = prefix;
  }

  @JsonProperty(PROP_SHARED)
  public void setShared(boolean shared) {
    _shared = shared;
  }

  @JsonProperty(PROP_VRF)
  public void setVrf(Vrf vrf) {
    _vrf = vrf;
  }

  public Interface toInterface(Configuration configuration, Warnings warnings) {
    Interface iface = new Interface(_canonicalName, configuration);
    iface.setBandwidth(_bandwidth);
    iface.setDeclaredNames(ImmutableSortedSet.of(_name));
    iface.setAddress(_prefix);
    iface.setAllAddresses(Iterables.concat(Collections.singleton(_prefix), _otherPrefixes));
    iface.setVrf(configuration.getDefaultVrf());
    if (_shared) {
      SourceNat sourceNat = new SourceNat();
      iface.setSourceNats(ImmutableList.of(sourceNat));
      Ip publicIp = _prefix.getAddress();
      sourceNat.setPoolIpFirst(publicIp);
      sourceNat.setPoolIpLast(publicIp);
    }
    return iface;
  }
}
