lexer grammar ResourcetreeLexer;

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
