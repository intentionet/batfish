parser grammar PaloAlto_rulebase;

import PaloAlto_common;

options {
    tokenVocab = PaloAltoLexer;
}

s_rulebase
:
    RULEBASE
    (
        sr_security
    )
;

sr_security
:
    SECURITY RULES name = variable
    (
        srs_action
        | srs_application
        | srs_category
        | srs_description
        | srs_destination
        | srs_disabled
        | srs_from
        | srs_hip_profiles
        | srs_negate_destination
        | srs_negate_source
        | srs_service
        | srs_source
        | srs_source_user
        | srs_to
    )
;

srs_action
:
    ACTION
    (
        ALLOW
        | DENY
        | DROP
        | RESET_BOTH
        | RESET_CLIENT
        | RESET_SERVER
    )
;

srs_application
:
    APPLICATION variable_list
;

srs_category
:
    CATEGORY
    null_rest_of_line
;

srs_description
:
    DESCRIPTION description = variable
;

srs_destination
:
    DESTINATION src_or_dst_list
;

srs_disabled
:
    DISABLED
    (
        NO
        | YES
    )
;

srs_from
:
    FROM variable_list
;

srs_hip_profiles
:
    HIP_PROFILES ANY // only support any
;

srs_negate_destination
:
    NEGATE_DESTINATION (YES | NO)
;

srs_negate_source
:
    NEGATE_SOURCE (YES | NO)
;

srs_service
:
    SERVICE variable_list
;

srs_source
:
    SOURCE src_or_dst_list
;

srs_source_user
:
    SOURCE_USER ANY // only support user any so far
;

srs_to
:
    TO variable_list
;
