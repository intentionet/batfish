package org.batfish.datamodel;

/** Types of device interfaces (see {@link org.batfish.datamodel.Interface}) */
public enum InterfaceType {
  /** Logical interface that aggregates multiple (physical) interfaces */
  AGGREGATED,
  /** Child of a aggregate interface: logical, sub-interface of an AGGREGATED interface */
  AGGREGATE_CHILD,
  /** Tunnel interface configured to use IPsec protocol */
  IPSEC_TUNNEL,
  /** Generic Logical interface, (e.g., units on Juniper devices) */
  LOGICAL,
  /** Logical loopback interface */
  LOOPBACK,
  /** Special null interface */
  NULL,
  /** Physical port */
  PHYSICAL,
  /** Logical redundant ethernet interface (in Juniper parlance) */
  REDUNDANT,
  /** Tunnel interface running anything else than IPsec, for e.g., GRE, IPIP, MPLS, etc. */
  TUNNEL,
  /** Unknown interface type */
  UNKNOWN, // for use as sentinel value
  /** Logical VLAN/irb interface */
  VLAN,
}
