parser grammar PaloAlto_deviceconfig;

import PaloAlto_common;

options {
    tokenVocab = PaloAltoLexer;
}

null_rest_of_line
:
    ~NEWLINE* NEWLINE
;

s_deviceconfig
:
   DEVICECONFIG
   (
      sd_system
   )
;

sd_system
:
   SYSTEM
   (
      sds_dns_setting
      | sds_hostname
      | sds_ntp_servers
   )
;

sds_dns_setting
:
   DNS_SETTING SERVERS
   (
      sdsd_servers
   )
;

sdsd_servers
:
   SERVERS
   (
      PRIMARY primary_name = IP_ADDRESS
      | SECONDARY secondary_name = IP_ADDRESS
   )+
   // Can I do this instead of +? {1,2}
;

sds_hostname
:
   HOSTNAME name = VARIABLE
;

sds_ntp_servers
:
   NTP_SERVERS
   (
      PRIMARY_NTP_SERVER
      | SECONDARY_NTP_SERVER
   )
   (
      sdsn_ntp_server_address
      | sdsn_authentication_type
   )
;

sdsn_authentication_type
:
   AUTHENTICATION_TYPE null_rest_of_line
;

sdsn_ntp_server_address
:
   NTP_SERVER_ADDRESS IP_ADDRESS
;

