parser grammar Fortios_bgp;

options {
  tokenVocab = FortiosLexer;
}

cr_bgp: BGP newline crb*;

crb
:
    SET crb_set_singletons
    | crb_config
;

crb_set_singletons
:
    crb_set_as
    | crb_set_router_id
;

crb_set_as: AS bgp_as newline;

crb_set_router_id: ROUTER_ID router_id = ip_address newline;

crb_config
:
    CONFIG (
        crbc_neighbor
        | crbc_redistribute
    ) END NEWLINE
;

crbc_neighbor: NEIGHBOR newline crbcn_edit*;

crbcn_edit: EDIT bgp_neighbor_id newline crbcne* NEXT newline;

crbcne
:
    SET crbcne_set_singletons
;

crbcne_set_singletons
:
    crbcne_set_remote_as
;

crbcne_set_remote_as: REMOTE_AS bgp_remote_as newline;

crbc_redistribute: REDISTRIBUTE protocol = str newline crbcr*;

crbcr
:
    SET crbcr_set_singletons
;

crbcr_set_singletons
:
    crbcr_set_status
;

crbcr_set_status: STATUS enable_or_disable newline;

// 0-4294967295
bgp_as: str;

// 1-4294967295
bgp_remote_as: str;

// An IP (but not using ip_address rule because EDIT pushes str mode)
bgp_neighbor_id: str;
