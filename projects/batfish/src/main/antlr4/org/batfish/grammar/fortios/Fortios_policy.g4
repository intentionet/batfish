parser grammar Fortios_policy;

options {
  tokenVocab = FortiosLexer;
}

cf_policy: POLICY newline cfp*;

cfp: cfp_clone | cfp_delete | cfp_edit | cfp_move;

cfp_clone: CLONE name = policy_number TO to = policy_number newline;

cfp_delete: DELETE name = policy_number newline;

cfp_move: MOVE name = policy_number after_or_before pivot = policy_number newline;

cfp_edit: EDIT policy_number newline cfpe* NEXT newline;

cfpe: cfp_set | cfp_append | cfp_select;

cfp_set: SET (cfp_set_singletons | cfp_set_lists);

cfp_select: SELECT cfp_set_lists;

cfp_set_singletons
:
    cfp_set_action
    | cfp_set_comments
    | cfp_set_name
    | cfp_set_status
    | cfp_set_uuid
;

cfp_set_action: ACTION action = policy_action newline;

cfp_set_comments: COMMENTS comments = str newline;

cfp_set_name: NAME name = policy_name newline;

cfp_set_status: STATUS status = policy_status newline;

cfp_set_uuid: UUID id = str newline;

cfp_set_lists
:
    cfp_set_dstaddr
    | cfp_set_dstintf
    | cfp_set_service
    | cfp_set_srcaddr
    | cfp_set_srcintf
;

cfp_set_dstaddr: DSTADDR addresses = address_names newline;

cfp_set_srcaddr: SRCADDR addresses = address_names newline;

cfp_set_service: SERVICE services = service_names newline;

cfp_set_dstintf: DSTINTF interfaces = interface_or_zone_names newline;

cfp_set_srcintf: SRCINTF interfaces = interface_or_zone_names newline;

cfp_append
:
    APPEND (
        cfp_append_dstaddr
        | cfp_append_dstintf
        | cfp_append_service
        | cfp_append_srcaddr
        | cfp_append_srcintf
    )
;

cfp_append_dstaddr: DSTADDR addresses = address_names newline;

cfp_append_srcaddr: SRCADDR addresses = address_names newline;

cfp_append_service: SERVICE services = service_names newline;

cfp_append_dstintf: DSTINTF interfaces = interface_or_zone_names newline;

cfp_append_srcintf: SRCINTF interfaces = interface_or_zone_names newline;

// 1-35
policy_name: str;

policy_status: enable_or_disable;

// 0-4294967294
policy_number: str;

policy_action: ACCEPT | DENY | IPSEC;

address_names: address_name+;

interface_or_zone_names: interface_or_zone_name+;
