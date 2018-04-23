parser grammar FlatJuniper_bgp;

import FlatJuniper_common;

options {
   tokenVocab = FlatJuniperLexer;
}

b_advertise_external
:
   ADVERTISE_EXTERNAL
;

b_advertise_inactive
:
   ADVERTISE_INACTIVE
;

b_advertise_peer_as
:
   ADVERTISE_PEER_AS
;

b_authentication_algorithm
:
   AUTHENTICATION_ALGORITHM
   (
      AES_128_CMAC_96
      | HMAC_SHA_1_96
      | MD5
   )
;

b_authentication_key
:
   AUTHENTICATION_KEY key = string
;

b_authentication_key_chain
:
   AUTHENTICATION_KEY_CHAIN name = string
;

b_allow
:
   ALLOW
   (
       IP_PREFIX
       | IPV6_PREFIX
       | ALL
   )
;

b_as_override
:
   AS_OVERRIDE
;

b_cluster
:
   CLUSTER id = IP_ADDRESS
;

b_common
:
   apply
   | b_advertise_external
   | b_advertise_inactive
   | b_advertise_peer_as
   | b_as_override
   | b_authentication_algorithm
   | b_authentication_key
   | b_authentication_key_chain
   | b_cluster
   | b_damping
   | b_description
   | b_disable_4byte_as
   | b_enforce_first_as
   | b_export
   | b_family
   | b_import
   | b_local_address
   | b_local_as
   | b_multihop
   | b_multipath
   | b_no_client_reflect
   | b_null
   | b_passive
   | b_path_selection
   | b_peer_as
   | b_remove_private
   | b_tcp_mss
   | b_type
;

b_damping
:
   DAMPING
;

b_description
:
   description
;

b_disable_4byte_as
:
   DISABLE_4BYTE_AS
;

b_enable
:
   ENABLE
;

b_enforce_first_as
:
   ENFORCE_FIRST_AS
;

b_export
:
   EXPORT expr = policy_expression
;

b_family
:
   FAMILY
   (
      bf_inet
      | bf_inet6
      | bf_null
   ) bf_accepted_prefix_limit?
;

b_group
:
   GROUP
   (
      name = variable
      | WILDCARD
   )
   (
      b_common
      | b_neighbor
      | b_allow
   )
;

b_import
:
   IMPORT expr = policy_expression
;

b_local_address
:
   LOCAL_ADDRESS
   (
      IP_ADDRESS
      | IPV6_ADDRESS
   )?
;

b_local_as
:
   LOCAL_AS bl_number? bl_common*
;

b_multihop
:
   MULTIHOP
   (
      apply
      | bm_no_nexthop_change
      | bm_ttl
   )
;

b_multipath
:
   MULTIPATH MULTIPLE_AS?
;

b_neighbor
:
   NEIGHBOR
   (
      IP_ADDRESS
      | IPV6_ADDRESS
      | WILDCARD
   ) b_common
;

b_no_client_reflect
:
   NO_CLIENT_REFLECT
;

b_null
:
   (
      AUTHENTICATION_KEY
      | BFD_LIVENESS_DETECTION
      | GRACEFUL_RESTART
      | HOLD_TIME
      | KEEP
      | LOG_UPDOWN
      | MTU_DISCOVERY
      | OUT_DELAY
      | PRECISION_TIMERS
      | TRACEOPTIONS
   ) null_filler
;

b_passive
:
   PASSIVE
;

b_path_selection
:
   PATH_SELECTION
   (
      bps_always_compare_med
      | bps_external_router_id
   )
;

b_peer_as
:
   PEER_AS
   (
      apply
      | bpa_as
   )
;

b_remove_private
:
   REMOVE_PRIVATE
;

b_tcp_mss
:
   TCP_MSS DEC
;

b_type
:
   TYPE
   (
      EXTERNAL
      | INTERNAL
   )
;

bf_accepted_prefix_limit
:
   ACCEPTED_PREFIX_LIMIT
   (
      (
         MAXIMUM max = DEC
      )
      |
      (
         TEARDOWN limit_threshold = DEC?
         (
            IDLE_TIMEOUT
            (
               idle_timeout = DEC
               | FOREVER
            )
         )?
      )
   )*
;

bf_inet
:
   INET
   (
      bfi_any
      | bfi_flow
      | bfi_labeled_unicast
      | bfi_multicast
      | bfi_unicast
   )
;

bf_inet6
:
   INET6
   (
      bfi6_any
      | bfi6_null
      | bfi6_unicast
   )
;

bf_null
:
   (
      INET_MDT
      | INET_MVPN
      | INET_VPN
      | INET6_VPN
      | L2VPN
   ) null_filler
;

bfi_any
:
   ANY null_filler
;

bfi_flow
:
   FLOW null_filler
;

bfi_labeled_unicast
:
   LABELED_UNICAST null_filler
;

bfi_multicast
:
   MULTICAST null_filler
;

bfi_unicast
:
   UNICAST
   (
      apply
      | bfiu_add_path
      | bfiu_prefix_limit
      | bfiu_rib_group
   )
;

bfi6_any
:
   ANY null_filler
;

bfi6_null
:
   (
      LABELED_UNICAST
      | MULTICAST
   ) null_filler
;

bfi6_unicast
:
   UNICAST
   (
      apply
      | bfi6u_prefix_limit
   )
;

bfi6u_prefix_limit
:
   PREFIX_LIMIT null_filler
;

bfiu_add_path
:
   ADD_PATH
   (
      bfiua_receive
      | bfiua_send
   )
;

bfiu_prefix_limit
:
   PREFIX_LIMIT null_filler
;

bfiu_rib_group
:
   RIB_GROUP name = variable
;

bfiua_receive
:
   RECEIVE
;

bfiua_send
:
   SEND
   (
      bfiuas_path_count
      | bfiuas_prefix_policy
   )
;

bfiuas_path_count
:
   PATH_COUNT count = DEC
;

bfiuas_prefix_policy
:
   PREFIX_POLICY policy = variable
;

bl_alias
:
   ALIAS
;

bl_common
:
   bl_alias
   | bl_loops
   | bl_private
;

bl_loops
:
   LOOPS DEC
;

bl_number
:
   as = DEC
;

bl_private
:
   PRIVATE
;

bm_no_nexthop_change
:
   NO_NEXTHOP_CHANGE
;

bm_ttl
:
   TTL DEC
;

bpa_as
:
   as = DEC
;

bps_always_compare_med
:
   ALWAYS_COMPARE_MED
;

bps_external_router_id
:
   EXTERNAL_ROUTER_ID
;

p_bgp
:
   BGP
   (
      b_common
      | b_enable
      | b_group
      | b_neighbor
   )
;
