parser grammar PaloAlto_service_group;

import PaloAlto_common;

options {
    tokenVocab = PaloAltoLexer;
}

s_service_group
:
    SERVICE_GROUP name = variable
    (
        sservgrp_members
    )
;

sservgrp_members
:
    MEMBERS variable_list
;
