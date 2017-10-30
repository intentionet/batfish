parser grammar RecoveryParser;

options {
   superClass = 'org.batfish.grammar.BatfishParser';
   tokenVocab = RecoveryLexer;
}

block_statement
:
   BLOCK tail_word* NEWLINE inner_statement*
;

inner_statement
:
   INNER tail_word* NEWLINE
;

statement
:
   block_statement
   | simple_statement
;

recovery
:
   statement* EOF
;

simple_statement
:
   SIMPLE tail_word* NEWLINE
;

tail_word
:
   BLOCK
   | INNER
   | SIMPLE
;
