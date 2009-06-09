lexer grammar ResourcetreeLexer;

tokens {
  /* Imaginary token used for intermediate handling of AST */
  PARENT;
  PROPERTY;
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
POSITION:	'position';
VIEWDEFINITION
	:	'view-definition';

LCB	:	'{';
RCB	:	'}';
LP	:	'(';
RP	:	')';
LB	:	'[';
RB	:	']';
COLON	:	':';
COMMA	:	',';

PROPTYPE:	(STRING | HTML | BOOLEAN | INT | DATETIME | IMAGEREF);
EDITHINT:	(TEXTFIELD (LB NUMBER RB)? | TEXTAREA | RADIO (LB VALUELIST RB)? | DROPDOWN (LB VALUELIST RB)?);
REQUIRED:	'required';
NOEXTRACT
	:	'noextract';
OVERRIDES
	:	'overrides';
GROUP	:	'group';
BEFORE	:	'before';
AFTER	:	'after';
LETTER	:	('a'..'z' | 'A'..'Z');
NUMBER	:	('0'..'9')+;
// XXX write rule
VALUELIST
	:	;
NAME	:	(LETTER | '-')+;
VIEWDEF	:	'$$' (options {greedy=false;} : .)* '$$'
		{
		  String s = getText();
		  s = s.replaceAll("\\s+", " ");
		  s = s.replace("$$", "");
		  setText(s.trim());
		};
WS	:	(' ' | '\t' | '\n')+ {$channel=HIDDEN;};

// Propertytypes
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
fragment IMAGEREF
	:	'image_ref';

// Edithints
fragment TEXTFIELD
	:	'textfield';
fragment TEXTAREA
	:	'textarea';
fragment RADIO
	:	'radio';
fragment DROPDOWN
	:	'dropdown';
