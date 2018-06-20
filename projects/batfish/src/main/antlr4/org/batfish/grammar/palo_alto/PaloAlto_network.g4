parser grammar PaloAlto_network;

import PaloAlto_common;

options {
    tokenVocab = PaloAltoLexer;
}

s_network
:
    NETWORK
    (
        sn_interface
        | sn_virtual_router
    )
;

sn_interface
:
    INTERFACE
    (
        sni_ethernet
    )
;

sn_virtual_router
:
    VIRTUAL_ROUTER name = variable
    (
        snvr_routing_table
    )
;

sni_ethernet
:
    ETHERNET name = variable
    (
        snie_comment
        | snie_layer3
        | snie_link_status
    )
;

snie_comment
:
    COMMENT text = variable
;

snie_layer3
:
    LAYER3
    (
        sniel3_ip
        | sniel3_mtu
    )
;

snie_link_status
:
    LINK_STATUS
    (
        AUTO
        | DOWN
        | UP
    )
;

sniel3_ip
:
    IP address =
    (
        IP_PREFIX
        | IP_ADDRESS
    )
;

sniel3_mtu
:
    MTU mtu = DEC
;

snvr_routing_table
:
    ROUTING_TABLE IP STATIC_ROUTE name = variable
    (
        snvrrt_admin_dist
        | snvrrt_destination
        | snvrrt_interface
        | snvrrt_metric
        | snvrrt_nexthop
    )
;

snvrrt_admin_dist
:
    ADMIN_DIST distance = DEC
;

snvrrt_destination
:
    DESTINATION destination = IP_PREFIX
;

snvrrt_interface
:
    INTERFACE iface = variable
;

snvrrt_metric
:
    METRIC metric = DEC
;

snvrrt_nexthop
:
    NEXTHOP IP_ADDRESS_LITERAL address = IP_ADDRESS
;
