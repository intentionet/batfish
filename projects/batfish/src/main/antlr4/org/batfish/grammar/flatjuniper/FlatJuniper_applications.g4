parser grammar FlatJuniper_applications;

import FlatJuniper_common, FlatJuniper_protocols;

options {
   tokenVocab = FlatJuniperLexer;
}

a_application
:
   APPLICATION name = variable
   (
      aa_common
      | aa_term
   )
;

aa_common
:
   aat_alg
   | aat_application_protocol
   | aat_destination_port
   | aat_icmp_code
   | aat_icmp_type
   | aat_icmp6_code
   | aat_icmp6_type
   | aat_inactivity_timeout
   | aat_protocol
   | aat_rpc_program_number
   | aat_source_port
   | aat_uuid
;

aa_term
:
   TERM name = variable aa_common+
;

aat_alg
:
   ALG application_protocol
;

aat_application_protocol
:
   APPLICATION_PROTOCOL application_protocol
;

aat_destination_port
:
   DESTINATION_PORT subrange
;

aat_icmp_code
:
   ICMP_CODE icmp_code
;

aat_icmp_type
:
   ICMP_TYPE icmp_type
;

aat_icmp6_code
:
   ICMP6_CODE code = DEC
;

aat_icmp6_type
:
   ICMP6_TYPE type = DEC
;

aat_inactivity_timeout
:
   INACTIVITY_TIMEOUT
   (
      DEC
      | NEVER
   )
;

aat_protocol
:
   PROTOCOL ip_protocol
;

aat_rpc_program_number
:
   RPC_PROGRAM_NUMBER DEC
;

aat_source_port
:
   SOURCE_PORT subrange
;

aat_uuid
:
   UUID null_filler
;

application_protocol
:
   DNS
   | FTP
   | HTTP
   | HTTPS
   | IGNORE
   | IKE_ESP_NAT
   | IMAP
   | MGCP_CA
   | MGCP_UA
   | MS_RPC
   | Q931
   | RAS
   | REALAUDIO
   | RTSP
   | SCCP
   | SIP
   | SMTP
   | SQLNET_V2
   | SSH
   | SUN_RPC
   | TALK
   | TELNET
   | TFTP
;

s_applications
:
   APPLICATIONS a_application
;
