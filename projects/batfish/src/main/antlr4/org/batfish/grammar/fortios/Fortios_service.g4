parser grammar Fortios_service;

options {
  tokenVocab = FortiosLexer;
}

cf_service: SERVICE (cfs_custom | cfs_group | IGNORED_CONFIG_BLOCK);

// Service custom
cfs_custom: CUSTOM newline cfsc*;

cfsc: cfsc_edit | cfsc_rename;

cfsc_rename: RENAME current_name = service_name TO new_name = service_name newline;

cfsc_edit: EDIT service_name newline cfsce* NEXT newline;

cfsce
:
    cfsc_set
    | (UNSET | SELECT | UNSELECT | APPEND | CLEAR) unimplemented
;

cfsc_set: SET cfsc_set_singletons;

cfsc_set_singletons:
    cfsc_set_category
    | cfsc_set_comment
    | cfsc_set_icmpcode
    | cfsc_set_icmptype
    | cfsc_set_protocol
    | cfsc_set_protocol_number
    | cfsc_set_sctp_portrange
    | cfsc_set_tcp_portrange
    | cfsc_set_udp_portrange
    | cfsc_set_visibility
;

cfsc_set_category: CATEGORY category = str newline;

cfsc_set_comment: COMMENT comment = str newline;

cfsc_set_icmpcode: ICMPCODE code = uint8 newline;

cfsc_set_icmptype: ICMPTYPE type = uint8 newline;

cfsc_set_protocol: PROTOCOL protocol = service_protocol newline;

cfsc_set_protocol_number: PROTOCOL_NUMBER number = ip_protocol_number newline;

cfsc_set_sctp_portrange: SCTP_PORTRANGE value = service_port_ranges newline;

cfsc_set_tcp_portrange: TCP_PORTRANGE value = service_port_ranges newline;

cfsc_set_udp_portrange: UDP_PORTRANGE value = service_port_ranges newline;

cfsc_set_visibility: VISIBILITY enable_or_disable newline;

// Service group
cfs_group: GROUP newline cfsg*;

cfsg: cfsg_edit | cfsg_rename;

cfsg_rename: RENAME current_name = service_name TO new_name = service_name newline;

cfsg_edit: EDIT service_name newline cfsge* NEXT newline;

cfsge: cfsg_set | cfsg_append | cfsg_select;

cfsg_set: SET (cfsg_set_singletons | cfsg_set_member);

cfsg_select: SELECT cfsg_set_member;

cfsg_set_singletons: cfsg_set_comment | cfsg_set_null;

cfsg_set_comment: COMMENT comment = str newline;

cfsg_set_null: COLOR null_rest_of_line;

cfsg_set_member: MEMBER service_names newline;

cfsg_append: APPEND cfsga_member;

cfsga_member: MEMBER service_names newline;

service_protocol
:
    ICMP
    | ICMP6
    | IP_UPPER
    | TCP_UDP_SCTP
;

service_port_ranges: service_port_range+;

// TODO: get from a portange lexing mode
service_port_range: dst_ports = port_range (COLON src_ports = port_range)?;

// 0-254
ip_protocol_number: uint8;
