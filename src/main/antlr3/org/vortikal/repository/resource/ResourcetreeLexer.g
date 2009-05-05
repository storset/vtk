lexer grammar ResourcetreeLexer;

@header {
package org.vortikal.repository.resource;
}

RESOURCETYPE   : 'resourcetype';
PROPERTIES     : 'properties';
EDITRULES      : 'edit-rules';
VIEWDEFINITION : 'view-definition';

LCB    : '{' ;
RCB    : '}' ;
LP     : '(' ;
RP     : ')' ;
COLON  : ':' ;
COMMA  : ',' ;

PROPTYPE : (STRING | HTML | BOOLEAN | INT | TIMESTAMP);
NAME     : ('a'..'z' | 'A'..'Z' | '-')+;
WS       : (' ' | '\t' | '\n')+ {$channel=HIDDEN;};

fragment STRING    : 'string';
fragment HTML      : 'html';
fragment BOOLEAN   : 'boolean';
fragment INT       : 'int';
fragment TIMESTAMP : 'timestamp';