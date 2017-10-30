parser grammar Cisco_interface;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

if_autostate
:
   NO? AUTOSTATE NEWLINE
;

if_default_gw
:
   DEFAULT_GW IP_ADDRESS NEWLINE
;

if_description
:
   description_line
;

if_flow_sampler
:
   NO? FLOW_SAMPLER variable EGRESS? NEWLINE
;

if_hsrp
:
   HSRP group = DEC NEWLINE
   (
      if_hsrp_ip_address
      | if_hsrp_null
      | if_hsrp_preempt
      | if_hsrp_priority
      | if_hsrp_track
   )*
;

if_hsrp_ip_address
:
   IP ip = IP_ADDRESS NEWLINE
;

if_hsrp_null
:
   NO?
   (
      AUTHENTICATION
      | MAC_ADDRESS
      | NAME
      | TIMERS
   ) ~NEWLINE* NEWLINE
;

if_hsrp_preempt
:
   NO? PREEMPT ~NEWLINE* NEWLINE
;

if_hsrp_priority
:
   NO? PRIORITY value = DEC ~NEWLINE* NEWLINE
;

if_hsrp_track
:
   NO? TRACK ~NEWLINE* NEWLINE
;

if_ip_access_group
:
   (
      (
         (
            IP
            | IPV4
         ) PORT? ACCESS_GROUP
      )
      |
      (
         ACCESS_LIST NAME
      )
   ) name = variable
   (
      EGRESS
      | IN
      | INGRESS
      | OUT
   )
   (
      HARDWARE_COUNT
      | OPTIMIZED
   )* NEWLINE
;

if_ip_address
:
   (
      IP
      | IPV4
   ) ADDRESS VIRTUAL?
   (
      (
         ip = IP_ADDRESS subnet = IP_ADDRESS
      )
      | prefix = IP_PREFIX
   )
   (
      STANDBY standby_address = IP_ADDRESS
   )? NEWLINE
;

if_ip_address_dhcp
:
   IP ADDRESS DHCP NEWLINE
;

if_ip_address_secondary
:
   (
      IP
      | IPV4
   ) ADDRESS
   (
      (
         ip = IP_ADDRESS subnet = IP_ADDRESS
      )
      | prefix = IP_PREFIX
   ) SECONDARY DHCP_GIADDR? NEWLINE
;

if_ip_dhcp
:
   NO? IP DHCP
   (
      ifdhcp_null
      | ifdhcp_relay
   )
;

if_ip_helper_address
:
   IP HELPER_ADDRESS address = IP_ADDRESS NEWLINE
;

if_ip_inband_access_group
:
   IP INBAND ACCESS_GROUP name = variable_permissive NEWLINE
;

if_ip_igmp
:
   NO? IP IGMP
   (
      NEWLINE
      | ifigmp_access_group
      | ifigmp_null
      | ifigmp_static_group
   )
;

if_ip_nat_destination
:
   IP NAT DESTINATION STATIC IP_ADDRESS ACCESS_LIST acl = variable IP_ADDRESS
   NEWLINE
;

if_ip_nat_source
:
   IP NAT SOURCE DYNAMIC ACCESS_LIST acl = variable
   (
      OVERLOAD
      |
      (
         POOL pool = variable
      )
   )* NEWLINE
;

if_ip_ospf_area
:
   IP OSPF procnum = DEC AREA area = DEC NEWLINE
;

if_ip_ospf_cost
:
   IP? OSPF COST cost = DEC NEWLINE
;

if_ip_ospf_dead_interval
:
   IP OSPF DEAD_INTERVAL seconds = DEC NEWLINE
;

if_ip_ospf_dead_interval_minimal
:
   IP OSPF DEAD_INTERVAL MINIMAL HELLO_MULTIPLIER mult = DEC NEWLINE
;

if_ip_ospf_hello_interval
:
   IP OSPF HELLO_INTERVAL seconds = DEC NEWLINE
;

if_ip_ospf_passive_interface
:
   NO? IP OSPF PASSIVE_INTERFACE NEWLINE
;

if_ip_pim_neighbor_filter
:
   IP PIM NEIGHBOR_FILTER acl = variable NEWLINE
;

if_ip_policy
:
   IP POLICY ROUTE_MAP name = ~NEWLINE NEWLINE
;

if_ip_proxy_arp
:
   NO? IP PROXY_ARP NEWLINE
;

if_ip_router_isis
:
   IP ROUTER ISIS NEWLINE
;

if_ip_router_ospf_area
:
   IP ROUTER OSPF procnum = DEC AREA area = IP_ADDRESS NEWLINE
;

if_ip_verify
:
   IP VERIFY UNICAST
   (
      (
         NOTIFICATION THRESHOLD DEC
      )
      |
      (
         REVERSE_PATH ALLOW_SELF_PING? acl = DEC?
      )
      |
      (
         SOURCE REACHABLE_VIA
         (
            ANY
            | RX
         )
         (
            ALLOW_DEFAULT
            | ALLOW_SELF_PING
            | L2_SRC
         )* acl = DEC?
      )
   ) NEWLINE
;

if_ip_virtual_router
:
   IP VIRTUAL_ROUTER ADDRESS address = IP_ADDRESS NEWLINE
;

if_isis_circuit_type
:
   ISIS CIRCUIT_TYPE
   (
      LEVEL_1
      | LEVEL_2_ONLY
      | LEVEL_2
   ) NEWLINE
;

if_isis_enable
:
   ISIS ENABLE num = DEC NEWLINE
;

if_isis_hello_interval
:
   ISIS HELLO_INTERVAL DEC
   (
      LEVEL_1
      | LEVEL_2
   )? NEWLINE
;

if_isis_metric
:
   ISIS IPV6? METRIC metric = DEC
   (
      LEVEL_1
      | LEVEL_2
   )? NEWLINE
;

if_isis_network
:
   ISIS NETWORK POINT_TO_POINT NEWLINE
;

if_isis_passive
:
   ISIS PASSIVE NEWLINE
;

if_isis_tag
:
   ISIS TAG tag = DEC NEWLINE
;

if_load_interval
:
   LOAD_INTERVAL li = DEC NEWLINE
;

if_mtu
:
   MTU mtu_size = DEC NEWLINE
;

if_no_ip_address
:
   NO IP ADDRESS NEWLINE
;

if_null_block
:
   NO?
   (
      ACTIVE
      | AFFINITY
      | ANTENNA
      | ARP
      | ASYNC
      | ATM
      | AUTHENTICATION
      | AUTO
      | AUTOROUTE
      | BANDWIDTH
      | BEACON
      | BFD
      | BGP_POLICY
      | BRIDGE_GROUP
      | BUNDLE
      | CABLE
      | CABLELENGTH
      | CARRIER_DELAY
      | CDP
      | CHANNEL
      | CHANNEL_GROUP
      | CHANNEL_PROTOCOL
      | CLASS
      | CLNS
      | CLOCK
      | COUNTER
      | CRC
      | CRYPTO
      | DAMPENING
      | DCB
      | DCBX
      | DCB_POLICY
      | DELAY
      | DESTINATION
      | DIALER
      | DIALER_GROUP
      | DFS
      | DOWNSTREAM
      | DSL
      |
      (
         DSU BANDWIDTH
      )
      | DUPLEX
      | ENABLE
      | ENCAPSULATION
      | ENCRYPTION
      | ETHERNET
      | EXIT
      | FAIR_QUEUE
      | FAST_REROUTE
      | FLOW
      | FLOW_CONTROL
      | FLOWCONTROL
      | FORWARDER
      | FRAME_RELAY
      | FRAMING
      | FULL_DUPLEX
      | GIG_DEFAULT
      | GLBP
      | GROUP_RANGE
      | H323_GATEWAY
      | HALF_DUPLEX
      | HARDWARE
      | HISTORY
      | HOLD_QUEUE
      |
      (
         HSRP
         (
            BFD
            | DELAY
            | USE_BIA
            | VERSION
         )
      )
      | IGNORE
      | INGRESS
      |
      (
         IP
         (
            ACCOUNTING
            | ADDRESS
            (
               NEGOTIATED
            )
            | ARP
            | BGP
            | BROADCAST_ADDRESS
            | CGMP
            | CONTROL_APPS_USE_MGMT_PORT
            | DVMRP
            |
            (
               DIRECTED_BROADCAST
            )
            | FLOW
            | IP_ADDRESS
            | IRDP
            | LOAD_SHARING
            | MROUTE_CACHE
            | MTU
            | MULTICAST
            | MULTICAST_BOUNDARY
            |
            (
               NAT
               (
                  INSIDE
                  | OUTSIDE
               )
            )
            | NHRP
            |
            (
               OSPF
               (
                  AUTHENTICATION
                  | AUTHENTICATION_KEY
                  | BFD
                  | DEMAND_CIRCUIT
                  | MESSAGE_DIGEST_KEY
                  | MTU_IGNORE
                  | NETWORK
                  | PRIORITY
                  | RETRANSMIT_INTERVAL
                  | TRANSMIT_DELAY
               )
            )
            |
            (
               PIM
               (
                  BORDER
                  | BORDER_ROUTER
                  | BSR_BORDER
                  | DR_PRIORITY
                  | HELLO_INTERVAL
                  | PASSIVE
                  | QUERY_INTERVAL
                  | SNOOPING
                  | SPARSE_DENSE_MODE
                  | SPARSE_MODE
                  | SPARSE_MODE_SSM
               )
            )
            | PIM_SPARSE
            | PORT_UNREACHABLE
            | REDIRECT
            | REDIRECTS
            | RIP
            | ROUTE_CACHE
            | RSVP
            | SDR
            | TCP
            | UNNUMBERED
            | UNREACHABLES
            | VERIFY
            | VIRTUAL_REASSEMBLY
            | VIRTUAL_ROUTER
            | VRF
            | WCCP
         )
      )
      |
      (
         IPV4
         (
            ICMP
            | MTU
            | POINT_TO_POINT
            | UNNUMBERED
            | UNREACHABLES
            | VERIFY
         )
      )
      | IPV6
      | ISDN
      |
      (
         ISIS
         (
            AUTHENTICATION
            | CSNP_INTERVAL
            | DS_HELLO_INTERVAL
            | HELLO
            | HELLO_INTERVAL
            | HELLO_MULTIPLIER
            | LSP_INTERVAL
            | POINT_TO_POINT
            | PROTOCOL
            | SMALL_HELLO
            | WIDE_METRIC
         )
      )
      | KEEPALIVE
      | L2_FILTER
      | L2PROTOCOL_TUNNEL
      | L2TRANSPORT
      | LANE
      | LAPB
      | LACP
      | LINK
      | LINK_FAULT_SIGNALING
      | LLDP
      | LOAD_BALANCING
      | LOAD_INTERVAL
      | LOGGING
      | LOOPBACK
      | LRE
      | MAC
      | MAC_ADDRESS
      | MACRO
      | MANAGEMENT
      | MANAGEMENT_ONLY
      | MAP_GROUP
      | MDIX
      | MEDIA_TYPE
      | MEDIUM
      | MEMBER
      | MINIMUM_LINKS
      | MLAG
      | MLS
      | MOBILITY
      | MOP
      | MPLS
      | NAME
      | NAMEIF
      | NEGOTIATE
      | NEGOTIATION
      | NMSP
      |
      (
         NO
         (
            DESCRIPTION
         )
      )
      |
      (
         NTP
         (
            BROADCAST
            | DISABLE
            | MULTICAST
         )
      )
      | NV
      | OPENFLOW
      | OPTICAL_MONITOR
      | OSPFV3
      | PACKET
      | PATH_OPTION
      | PEAKDETECT
      | PEER
      | PFC PRIORITY
      | PHYSICAL_LAYER
      | PLATFORM
      | PORT_CHANNEL
      | PORT_CHANNEL_PROTOCOL
      | PORT_NAME
      | PORT_TYPE
      | PORTMODE
      | POS
      | POWER
      | POWER_LEVEL
      | PPP
      | PREEMPT
      | PRIORITY
      | PRIORITY_FLOW_CONTROL
      | PRIORITY_QUEUE
      | PVC
      | QOS
      | QUEUE_MONITOR
      | QUEUE_SET
      | RANDOM_DETECT
      | RATE_LIMIT
      | RATE_MODE
      | RCV_QUEUE
      | REDIRECTS
      | REMOTE
      | ROUTE_CACHE
      | ROUTE_ONLY
      | SCRAMBLE
      | SECURITY_LEVEL
      | SERIAL
      | SERVICE
      | SERVICE_MODULE
      | SERVICE_POLICY
      | SFLOW
      | SHAPE
      | SIGNALLED_BANDWIDTH
      | SIGNALLED_NAME
      | SONET
      | SOURCE
      | SPEED
      | SPEED_DUPLEX
      | SNMP
      | SRR_QUEUE
      | SSID
      | STACK_MIB
      | STANDBY
      | STATION_ROLE
      | STBC
      | STORM_CONTROL
      |
      (
         SWITCHPORT
         (
            BACKUP
            | BLOCK
            | DOT1Q
            | EMPTY
            |
            (
               MODE PRIVATE_VLAN
            )
            | MONITOR
            | NONEGOTIATE
            | PORT_SECURITY
            | PRIORITY
            | TAP
            | TOOL
            |
            (
               TRUNK
               (
                  GROUP
                  | PRUNING
               )
            )
            | VOICE
            | VLAN
         )
      )
      | TAG_SWITCHING
      | TAGGED
      | TAP
      | TCAM
      | TRANSCEIVER
      | TRANSPORT_MODE
      | TRUST
      | TUNABLE_OPTIC
      | TUNNEL
      | TX_QUEUE
      | UC_TX_QUEUE
      | UDLD
      | UNTAGGED
      | VLT_PEER_LAG
      | VMTRACER
      | VPC
      | VTP
      | VXLAN
      | WEIGHTING
      | WRR_QUEUE
      | X25
      | XCONNECT
   ) ~NEWLINE* NEWLINE if_null_inner*
;

if_null_inner
:
   NO?
   (
      ADDRESS
      | BACKUP
      | BRIDGE_DOMAIN
      | DIALER
      | ENCAPSULATION
      | L2PROTOCOL
      | MODE
      | PRIORITY
      | PROPAGATE
      | PROTOCOL
      | RECEIVE
      | REMOTE_PORTS
      | REWRITE
      | SATELLITE_FABRIC_LINK
      | SERVICE_POLICY
      | TRANSMIT
      | VIRTUAL_ADDRESS
   ) ~NEWLINE* NEWLINE
;

if_null_single
:
   NO?
   (
      BCMC_OPTIMIZATION
      | JUMBO
      | LINKDEBOUNCE
      | PHY
      | SUPPRESS_ARP
      | TRIMODE
      | TRUSTED
   ) ~NEWLINE* NEWLINE
;

if_port_security
:
   PORT SECURITY NEWLINE
   (
      if_port_security_null
   )*
;

if_spanning_tree
:
   NO? SPANNING_TREE
   (
      if_st_null
      | if_st_portfast
      | NEWLINE
   )
;

if_st_null
:
   (
      BPDUFILTER
      | BPDUGUARD
      | COST
      | GUARD
      | LINK_TYPE
      | MST
      | PORT
      | PORT_PRIORITY
      | PRIORITY
      | PROTECT
      | RSTP
      | VLAN
   ) ~NEWLINE* NEWLINE
;

if_st_portfast
:
   PORTFAST
   (
      disable = DISABLE
      | edge = EDGE
      | network = NETWORK
      | trunk = TRUNK
   )* NEWLINE
;

if_port_security_null
:
   NO?
   (
      AGE
      | ENABLE
      | MAXIMUM
      | SECURE_MAC_ADDRESS
      | VIOLATION
   ) ~NEWLINE* NEWLINE
;

if_shutdown
:
   NO?
   (
      DISABLE
      | SHUTDOWN
   ) FORCE? LAN? NEWLINE
;

if_switchport
:
   NO? SWITCHPORT NEWLINE
;

if_switchport_access
:
   SWITCHPORT ACCESS VLAN
   (
      vlan = DEC
      | DYNAMIC
   ) NEWLINE
;

if_switchport_mode
:
   SWITCHPORT MODE
   (
      ACCESS
      | DOT1Q_TUNNEL
      |
      (
         DYNAMIC
         (
            AUTO
            | DESIRABLE
         )
      )
      | FEX_FABRIC
      | TAP
      | TOOL
      | TRUNK
   ) NEWLINE
;

if_switchport_private_vlan_association
:
   SWITCHPORT PRIVATE_VLAN ASSOCIATION TRUNK primary_vlan_id = DEC
   secondary_vlan_id = DEC NEWLINE
;

if_switchport_private_vlan_host_association
:
   SWITCHPORT PRIVATE_VLAN HOST_ASSOCIATION primary_vlan_id = DEC
   secondary_vlan_id = DEC NEWLINE
;

if_switchport_private_vlan_mapping
:
   SWITCHPORT PRIVATE_VLAN MAPPING TRUNK? primary_vlan_id = DEC
   secondary_vlan_list = range NEWLINE
;

if_switchport_trunk_allowed
:
   SWITCHPORT TRUNK ALLOWED VLAN ADD? r = range NEWLINE
;

if_switchport_trunk_encapsulation
:
   SWITCHPORT TRUNK ENCAPSULATION e = switchport_trunk_encapsulation NEWLINE
;

if_switchport_trunk_native
:
   SWITCHPORT TRUNK NATIVE VLAN vlan = DEC NEWLINE
;

if_vrf
:
   VRF name = variable NEWLINE
;

if_vrf_forwarding
:
   VRF FORWARDING name = variable NEWLINE
;

if_vrf_member
:
   VRF MEMBER name = variable NEWLINE
;

if_vrrp
:
   VRRP groupnum = DEC
   (
      ifvrrp_authentication
      | ifvrrp_ip
      | ifvrrp_ip_secondary
      | ifvrrp_preempt
      | ifvrrp_priority
   )
;

ifdhcp_null
:
   (
      SMART_RELAY
      | SNOOPING
   ) ~NEWLINE* NEWLINE
;

ifdhcp_relay
:
   RELAY
   (
      ifdhcpr_address
      | ifdhcpr_client
      | ifdhcpr_null
   )
;

ifdhcpr_address
:
   ADDRESS address = IP_ADDRESS NEWLINE
;

ifdhcpr_client
:
   CLIENT NEWLINE
;

ifdhcpr_null
:
   (
      INFORMATION
      | SUBNET_BROADCAST
   ) ~NEWLINE* NEWLINE
;

ifigmp_access_group
:
   ACCESS_GROUP name = variable NEWLINE
;

ifigmp_null
:
   (
      GROUP_TIMEOUT
      | HOST_PROXY
      | LAST_MEMBER_QUERY_COUNT
      | LAST_MEMBER_QUERY_INTERVAL
      | LAST_MEMBER_QUERY_RESPONSE_TIME
      | MULTICAST_STATIC_ONLY
      | QUERY_INTERVAL
      | QUERY_MAX_RESPONSE_TIME
      | QUERY_TIMEOUT
      | ROBUSTNESS_VARIABLE
      | ROUTER_ALERT
      | SNOOPING
      | STARTUP_QUERY_COUNT
      | STARTUP_QUERY_INTERVAL
      | VERSION
   ) ~NEWLINE* NEWLINE
;

ifigmp_static_group
:
   STATIC_GROUP
   (
      ifigmpsg_acl
      | ifigmpsg_null
   )
;

ifigmpsg_acl
:
   ACL name = variable NEWLINE
;

ifigmpsg_null
:
   (
      IP_ADDRESS
      | RANGE
   ) ~NEWLINE* NEWLINE
;

ifvrrp_authentication
:
   AUTHENTICATION TEXT text = variable_permissive NEWLINE
;

ifvrrp_ip
:
   IP ip = IP_ADDRESS NEWLINE
;

ifvrrp_ip_secondary
:
   IP ip = IP_ADDRESS SECONDARY NEWLINE
;

ifvrrp_preempt
:
   PREEMPT DELAY
   (
      MINIMUM
      | RELOAD
   ) DEC NEWLINE
;

ifvrrp_priority
:
   PRIORITY priority = DEC NEWLINE
;

s_interface
:
   INTERFACE PRECONFIGURE? iname = interface_name
   (
      L2TRANSPORT
      | MULTIPOINT
      | POINT_TO_POINT
   )?
   (
      NEWLINE
      |
      {_cadant}?

      NEWLINE?
   )
   (
      if_autostate
      | if_default_gw
      | if_description
      | if_flow_sampler
      | if_hsrp
      | if_ip_proxy_arp
      | if_ip_verify
      | if_ip_access_group
      | if_ip_address
      | if_ip_address_dhcp
      | if_ip_address_secondary
      | if_ip_dhcp
      | if_ip_helper_address
      | if_ip_inband_access_group
      | if_ip_igmp
      | if_ip_nat_destination
      | if_ip_nat_source
      | if_ip_ospf_area
      | if_ip_ospf_cost
      | if_ip_ospf_dead_interval
      | if_ip_ospf_dead_interval_minimal
      | if_ip_ospf_hello_interval
      | if_ip_ospf_passive_interface
      | if_ip_pim_neighbor_filter
      | if_ip_policy
      | if_ip_router_isis
      | if_ip_router_ospf_area
      | if_ip_virtual_router
      | if_isis_circuit_type
      | if_isis_enable
      | if_isis_hello_interval
      | if_isis_metric
      | if_isis_network
      | if_isis_passive
      | if_isis_tag
      | if_load_interval
      | if_mtu
      | if_no_ip_address
      | if_port_security
      | if_shutdown
      | if_spanning_tree
      | if_switchport
      | if_switchport_access
      | if_switchport_mode
      | if_switchport_private_vlan_association
      | if_switchport_private_vlan_host_association
      | if_switchport_private_vlan_mapping
      | if_switchport_trunk_allowed
      | if_switchport_trunk_encapsulation
      | if_switchport_trunk_native
      | if_vrf
      | if_vrf_forwarding
      | if_vrf_member
      | if_vrrp
      // do not rearrange items below

      | if_null_single
      | if_null_block
   )*
;
