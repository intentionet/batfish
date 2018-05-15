package org.batfish.dataplane.ibdp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.routing_policy.Environment.Direction;

/** Keeps data about which prefixes where advertised to neighboring routers */
public class PrefixTracer implements Serializable {

  private static final long serialVersionUID = 1L;

  public class Neighbor implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String _hostname;
    private final Ip _ip;
    @Nullable private final String _routingPolicy;
    private final String _vrfName;

    /**
     * A non-protocol-specific L3 data plane neighbor, identified by hostname, VRF name & IP
     * address. Optional routing policy can be specified as meta data to indicate a policy that
     * allowed or filtered the prefix.
     */
    Neighbor(String hostname, Ip ip, String vrfName, @Nullable String routingPolicy) {
      this._hostname = hostname;
      this._ip = ip;
      this._routingPolicy = routingPolicy;
      this._vrfName = vrfName;
    }

    /** Return the remote neighbor's hostname */
    public String getHostname() {
      return _hostname;
    }

    /** Return the remote neighbor's IP */
    public Ip getIp() {
      return _ip;
    }

    /** Return the routing policy (of the current router) */
    @Nullable
    public String getRoutingPolicy() {
      return _routingPolicy;
    }

    /** Return remote neighbor's VRF name */
    public String getVrfname() {
      return _vrfName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Neighbor neighbor = (Neighbor) o;
      return Objects.equals(_hostname, neighbor._hostname)
          && Objects.equals(_ip, neighbor._ip)
          && Objects.equals(_vrfName, neighbor._vrfName)
          && Objects.equals(_routingPolicy, neighbor._routingPolicy);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_hostname, _vrfName, _ip, _routingPolicy);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("hostname", _hostname)
          .add("ip", _ip)
          .add("routingPolicy", _routingPolicy)
          .add("vrfName", _vrfName)
          .toString();
    }
  }

  private Map<Prefix, Set<Neighbor>> _filteredOnImport;

  private Map<Prefix, Set<Neighbor>> _filteredOnExport;

  private Set<Prefix> _originated;

  private Map<Prefix, Set<Neighbor>> _installed;

  private Map<Prefix, Set<Neighbor>> _sent;

  PrefixTracer() {
    _originated = new HashSet<>();
    _installed = new HashMap<>();
    _sent = new HashMap<>();
    _filteredOnImport = new HashMap<>();
    _filteredOnExport = new HashMap<>();
  }

  /** Note that we considered given prefix for origination */
  void originated(Prefix prefix) {
    _originated.add(prefix);
  }

  /** Note that we exported given prefix to a given {@link Neighbor} */
  void sentTo(
      Prefix prefix,
      String neighborHostname,
      Ip neighborIp,
      String neighborVrf,
      @Nullable String exportPolicy) {
    Set<Neighbor> set = _sent.computeIfAbsent(prefix, p -> new HashSet<>());
    set.add(new Neighbor(neighborHostname, neighborIp, neighborVrf, exportPolicy));
  }

  /** Note that we installed a prefix received from a given {@link Neighbor} */
  void installed(
      Prefix prefix,
      String neighborHostname,
      Ip neighborIp,
      String neighborVrf,
      @Nullable String importPolicy) {
    Set<Neighbor> set = _installed.computeIfAbsent(prefix, p -> new HashSet<>());
    set.add(new Neighbor(neighborHostname, neighborIp, neighborVrf, importPolicy));
  }

  /**
   * Note that we did filtered a prefix when sending/receiving routes from given {@link Neighbor}.
   *
   * @param direction {@link Direction} indicating whether we were sending ({@link Direction#OUT})
   *     or receiving ({@link Direction#IN}) the prefix
   */
  void filtered(
      Prefix prefix,
      String neighborHostname,
      Ip neighborIp,
      String neighborVrf,
      @Nullable String policyName,
      Direction direction) {
    if (direction == Direction.IN) {
      Set<Neighbor> set = _filteredOnImport.computeIfAbsent(prefix, p -> new HashSet<>());
      set.add(new Neighbor(neighborHostname, neighborIp, neighborVrf, policyName));
    } else if (direction == Direction.OUT) {
      Set<Neighbor> set = _filteredOnExport.computeIfAbsent(prefix, p -> new HashSet<>());
      set.add(new Neighbor(neighborHostname, neighborIp, neighborVrf, policyName));
    } else {
      throw new UnsupportedOperationException("Unknown filtering direction");
    }
  }

  /** Return the set of prefixes we attempted to originate */
  public SortedSet<Prefix> getOriginated() {
    return ImmutableSortedSet.copyOf(_originated);
  }

  /** Return the set of prefixes we successfully sent/exported. */
  public Map<Prefix, Set<Neighbor>> getSent() {
    return _sent;
  }

  /** Return the set of prefixes we received and installed. */
  public Map<Prefix, Set<Neighbor>> getInstalled() {
    return _installed;
  }

  /**
   * Return the set of prefixes we filtered (in a given direction)
   *
   * @param direction {@link Direction} indicating whether the prefixes were filtered on export
   *     ({@link Direction#OUT}) or import ({@link Direction#IN}).
   */
  public Map<Prefix, Set<Neighbor>> getFiltered(Direction direction) {
    if (direction == Direction.IN) {
      return _filteredOnImport;
    } else if (direction == Direction.OUT) {
      return _filteredOnExport;
    } else {
      throw new UnsupportedOperationException("Unknown filtering direction");
    }
  }
}
