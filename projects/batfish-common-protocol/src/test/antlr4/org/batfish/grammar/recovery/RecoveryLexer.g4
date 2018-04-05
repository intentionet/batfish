lexer grammar RecoveryLexer;

options {
   superClass = 'org.batfish.grammar.BatfishLexer';
}

@members {
private int lastTokenType = -1;

@Override
public void emit(Token token) {
    super.emit(token);
    if (token.getChannel() != HIDDEN) {
       lastTokenType = token.getType();
    }
}
}

/* Simple tokens */
BLOCK
:
   'block'
;

INNER
:
   'inner'
;

SIMPLE
:
   'simple'
;

/* Complex tokens */
BLANK_LINE
:
   (
      F_Whitespace
   )* F_Newline
   {lastTokenType == NEWLINE}?

   F_Newline* -> channel ( HIDDEN )
;

COMMENT_LINE
:
   (
      F_Whitespace
   )* '#'
   {lastTokenType == NEWLINE || lastTokenType == EOF}?

   F_NonNewline* F_Newline+ -> channel ( HIDDEN )
;

COMMENT_TAIL
:
   '#' F_NonNewline* -> channel ( HIDDEN )
;

NEWLINE
:
   F_Newline+
;

WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

fragment
F_Newline
:
   [\r\n]
;

fragment
F_NonNewline
:
   ~[\n\r]
;

fragment
F_Whitespace
:
   [ \t]
;

/* Test ability to recover out of bad lexer modes. */
ENTER_BAD_MODE
:
   'enter-bad-mode' -> pushMode ( M_BadMode)
;

mode M_BadMode;

M_BadMode_nonexistent
:
    'nonexistent'
;