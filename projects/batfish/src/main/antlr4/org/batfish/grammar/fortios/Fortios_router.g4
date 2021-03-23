parser grammar Fortios_router;

options {
  tokenVocab = FortiosLexer;
}

c_router: ROUTER (
    cr_bgp
    | cr_static
);
