parser grammar PaloAltoParser;

/* This is only needed if parser grammar is spread across files */
import
PaloAlto_common, PaloAlto_deviceconfig, PaloAlto_network, PaloAlto_service, PaloAlto_service_group, PaloAlto_shared, PaloAlto_vsys, PaloAlto_zone;

options {
    superClass = 'org.batfish.grammar.BatfishParser';
    tokenVocab = PaloAltoLexer;
}

palo_alto_configuration
:
    NEWLINE?
    (
        set_line_config_devices
        | set_line_config_general
    )+ NEWLINE? EOF
;

/*
 * The distinction between config device and general config statements is needed in order to handle
 * syntax differences in filesystem-style config dumps
 */
set_line_config_devices
:
    SET (CONFIG DEVICES name = variable)? statement_config_devices NEWLINE
;

set_line_config_general
:
    SET CONFIG? statement_config_general NEWLINE
;

/*
 * These are settings that show up on the device under /config/devices/<DEV>/...
 */
statement_config_devices
:
    s_deviceconfig
    | s_service
    | s_service_group
    | s_network
    | s_vsys
    | s_zone
;

/*
 * These are settings that show up on the device under /config/... (NOT under the devices/ dir)
 */
statement_config_general
:
    s_shared
;
