lexer grammar ResourcetreeLexer;

tokens {
  /* Imaginary token used for intermediate handling of AST */
  PARENT;
}

@header {
package org.vortikal.repository.resource;
}

RESOURCETYPE
	:	'resourcetype';
PROPERTIES
	:	'properties';
EDITRULES
	:	'edit-rules';
VIEWDEFINITION
	:	'view-definition';

LCB	:	'{' ;
RCB	:	'}' ;
LP	:	'(' ;
RP	:	')' ;
COLON	:	':' ;
COMMA	:	',' ;

PROPTYPE:	(STRING | HTML | BOOLEAN | INT | DATETIME);
REQUIRED:	'required';
NOEXTRACT
	:	'noextract';
OVERRIDES
	:	'overrides';
GROUP	:	'group';
BEFORE	:	'before';
AFTER	:	'after';
LETTERS	:	('a'..'z' | 'A'..'Z');
NAME	:	(LETTERS | '-')+;
VIEWDEF	:	CDATA?;
WS	:	(' ' | '\t' | '\n')+ {$channel=HIDDEN;};

fragment STRING
	:	'string';
fragment HTML
	:	'html';
fragment BOOLEAN
	:	'boolean';
fragment INT
	:	'int';
fragment DATETIME
	:	'datetime';
fragment CDATA
	:	'<![CDATA[' (options {greedy=false;} : .)* ']]>'
	;
