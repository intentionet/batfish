package org.batfish.dataplane.ibdp;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMap.Builder;
import java.util.Comparator;
import java.util.SortedMap;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.Configuration;

/** Dataplane-specific encapsulation of {@link Configuration} */
public final class Node extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private final Configuration _c;

  private final SortedMap<String, VirtualRouter> _virtualRouters;

  /**
   * Create a new node based on the configuration. Initializes virtual routers based on {@link
   * Configuration} VRFs.
   *
   * @param configuration the {@link Configuration} backing this node
   */
  public Node(Configuration configuration) {
    super(configuration.getHostname());
    _c = configuration;
    Builder<String, VirtualRouter> b = new Builder<>(Comparator.naturalOrder());
    for (String vrfName : _c.getVrfs().keySet()) {
      VirtualRouter vr = new VirtualRouter(vrfName, _c);
      b.put(vrfName, vr);
    }
    _virtualRouters = b.build();
  }

  /** @return The {@link Configuration} backing this Node */
  public Configuration getConfiguration() {
    return _c;
  }

  /**
   * Return the virtual routers at this node
   *
   * @return Set of {@link VirtualRouter}s, keyed by VRF name
   */
  SortedMap<String, VirtualRouter> getVirtualRouters() {
    return ImmutableSortedMap.copyOf(_virtualRouters);
  }
}
