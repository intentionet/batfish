parser grammar PaloAlto_vsys;

import PaloAlto_common, PaloAlto_rulebase, PaloAlto_shared, PaloAlto_zone;

options {
    tokenVocab = PaloAltoLexer;
}

s_vsys
:
    VSYS name = variable
    (
        s_rulebase
        | s_zone
        | ss_common
        | sv_import
    )
;

sv_import
:
    IMPORT
    (
        svi_interface
        | svi_visible_vsys
    )?
;

svi_interface
:
    INTERFACE variable_list
;

svi_visible_vsys
:
    VISIBLE_VSYS variable_list
;
