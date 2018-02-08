parser grammar Cisco_line;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

l_access_class
:
   IPV6? ACCESS_CLASS
   (
      (
         (
            EGRESS
            | INGRESS
         ) name = variable
      )
      |
      (
         name = variable
         (
            IN
            | OUT
         )?
      )
   ) VRF_ALSO? NEWLINE
;

l_accounting
:
   (
      (
         NO ACCOUNTING
         (
            COMMANDS
            | EXEC
         )
      )
      |
      (
         ACCOUNTING
         (
            COMMANDS
            | EXEC
         )
         (
            DEFAULT
            | variable
         )
      )
   ) NEWLINE
;

l_exec_timeout
:
   EXEC_TIMEOUT minutes = DEC seconds = DEC? NEWLINE
;

l_length
:
   (
      LENGTH DEC NEWLINE
   )
   |
   (
      NO LENGTH NEWLINE
   )
;

l_login
:
   LOGIN
   (
      l_login_authentication
      | l_login_local
   )
;

l_login_authentication
:
   AUTHENTICATION
   (
      DEFAULT
      | name = variable
   ) NEWLINE
;

l_login_local
:
   LOCAL NEWLINE
;

l_null
:
   NO?
   (
      ABSOLUTE_TIMEOUT
      | ACTIVATION_CHARACTER
      | AUTHORIZATION
      | AUTOHANGUP
      | AUTOSELECT
      | DATABITS
      | ESCAPE_CHARACTER
      | EXEC
      | FLOWCONTROL
      | FLUSH_AT_ACTIVATION
      | HISTORY
      | IPV6
      | LOCATION
      | LOGGING
      | LOGOUT_WARNING
      | MODEM
      | NOTIFY
      | PASSWORD
      | PRIVILEGE
      | ROTARY
      | RXSPEED
      | SESSION_DISCONNECT_WARNING
      | SESSION_LIMIT
      | SESSION_TIMEOUT
      | STOPBITS
      | TERMINAL_TYPE
      | TIMEOUT
      | TIMESTAMP
      | TXSPEED
      | VACANT_MESSAGE
   ) ~NEWLINE* NEWLINE
;

l_script
:
   SCRIPT DIALER variable NEWLINE
;

l_transport
:
   TRANSPORT
   (
      INPUT
      | OUTPUT
      | PREFERRED
   ) prot += variable+ NEWLINE
;

lc_null
:
   (
      ACCOUNTING
      | AUTHENTICATION
      | AUTHORIZATION
      | ENABLE_AUTHENTICATION
      | IDLE_TIMEOUT
      | LENGTH
      | LOGIN_AUTHENTICATION
      | PASSWORD
      | SESSION_TIMEOUT
      | SPEED
   ) ~NEWLINE* NEWLINE
;

s_line
:
   LINE line_type
   (
      (
         slot1 = DEC FORWARD_SLASH
         (
            port1 = DEC FORWARD_SLASH
         )?
      )? first = DEC
      (
         (
            slot2 = DEC FORWARD_SLASH
            (
               port2 = DEC FORWARD_SLASH
            )?
         )? last = DEC
      )?
   )? NEWLINE
   (
      l_access_class
      | l_accounting
      | l_exec_timeout
      | l_length
      | l_login
      | l_null
      | l_script
      | l_transport
      | description_line
   )*
;

s_line_cadant
:
   LINE line_type_cadant start_line = DEC end_line = DEC?
   (
      lc_null
   )
;
