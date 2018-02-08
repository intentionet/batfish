parser grammar Cisco_logging;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

logging_address
:
   hostname =
   (
      IP_ADDRESS
      | IPV6_ADDRESS
   )
   (
      DISCRIMINATOR descr = variable
      | FACILITY facility = variable
      |
      (
         PORT
         (
            DEFAULT
            | DEC
         )
      )
      | SEVERITY severity = variable
      | TYPE ltype = variable
      | VRF vrf = variable
   )* NEWLINE
;

logging_archive
:
   ARCHIVE ~NEWLINE* NEWLINE
   (
      logging_archive_null
   )*
;

logging_archive_null
:
   NO?
   (
      ARCHIVE_LENGTH
      | DEVICE
      | FREQUENCY
   ) ~NEWLINE* NEWLINE
;

logging_buffered
:
   BUFFERED
   (
      (
         DISCRIMINATOR descr = variable
      )? size = DEC? logging_severity?
   ) NEWLINE
;

logging_common
:
   logging_address
   | logging_archive
   | logging_buffered
   | logging_console
   | logging_enable
   | logging_format
   | logging_host
   | logging_null
   | logging_on
   | logging_server
   | logging_source_interface
   | logging_suppress
   | logging_trap
;

logging_console
:
   CONSOLE
   (
      DISABLE
      | logging_severity
   )?
   (
      EXCEPT ERRORS
   )? NEWLINE
;

logging_enable
:
   ENABLE NEWLINE
;

logging_format
:
   FORMAT
   (
      (
         HOSTNAME FQDN
      )
      |
      (
         TIMESTAMP
         (
            HIGH_RESOLUTION
            |
            (
               TRADITIONAL ~NEWLINE*
            )
         )
      )
   ) NEWLINE
;

logging_host
:
   HOST iname = variable? hostname = variable
   (
      VRF vrf = variable
   )?
   (
      DISCRIMINATOR descr = variable
   )?
   (
      TRANSPORT
      (
         TCP
         | UDP
      )
   )?
   (
      PORT
      (
         DEFAULT
         | DEC
      )
   )?
   (
      FACILITY name = variable
   )? NEWLINE
;

logging_null
:
   (
      ALARM
      | ASDM
      | ASDM_BUFFER_SIZE
      | BUFFER_SIZE
      | CMTS
      | DEBUG_TRACE
      | DISCRIMINATOR
      | ESM
      | EVENT
      | EVENTS
      | FACILITY
      | HISTORY
      | IP
      | LEVEL
      | LINECARD
      | LOCAL_VOLATILE
      | LOGFILE
      | MESSAGE_COUNTER
      | MONITOR
      | PERMIT_HOSTDOWN
      | PROPRIETARY
      | ORIGIN_ID
      | OVERRIDE
      | QUEUE_LIMIT
      | RATE_LIMIT
      | SEQUENCE_NUMS
      | SERVER_ARP
      | SNMP_AUTHFAIL
      | SYNCHRONOUS
      | SYSLOG
      | TIMESTAMP
   ) ~NEWLINE* NEWLINE
;

logging_on
:
   ON NEWLINE
;

logging_severity
:
   DEC
   | ALERTS
   | CRITICAL
   | DEBUGGING
   | EMERGENCIES
   | ERRORS
   | INFORMATIONAL
   | NOTIFICATIONS
   | WARNINGS
;

logging_server
:
   SERVER hostname =
   (
      IP_ADDRESS
      | IPV6_ADDRESS
   ) ~NEWLINE* NEWLINE
;

logging_source_interface
:
   SOURCE_INTERFACE interface_name
   (
      VRF vrf = variable
   )? NEWLINE
;

logging_suppress
:
   SUPPRESS ~NEWLINE* NEWLINE
   (
      logging_suppress_null
   )*
;

logging_suppress_null
:
   NO?
   (
      ALARM
      | ALL_ALARMS
      | ALL_OF_ROUTER
   ) ~NEWLINE* NEWLINE
;

logging_trap
:
   TRAP logging_severity? NEWLINE
;

logging_vrf
:
   VRF vrf = variable logging_common
;

s_logging
:
   NO? LOGGING
   (
      logging_common
      | logging_vrf
   )
;
