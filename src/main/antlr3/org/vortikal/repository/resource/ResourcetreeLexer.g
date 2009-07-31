lexer grammar ResourcetreeLexer;

tokens {
  /* Imaginary token used for intermediate handling of AST */
  PARENT;
}

@header {
package org.vortikal.repository.resource;
}

INCLUDE	:	'include';
RESOURCETYPE
	:	'resourcetype';
PROPERTIES
	:	'properties';
EDITRULES
	:	'edit-rules';
VIEWCOMPONENTS
	:	'view-components';
VIEWDEFINITION
	:	'view-definition';
LOCALIZATIONPROPERTIES
	:	'localization-properties';
SCRIPTS	:	'scripts';

LCB	:	'{';
RCB	:	'}';
LP	:	'(';
RP	:	')';
LB	:	'[';
RB	:	']';
COLON	:	':';
COMMA	:	',';
DQ	:	'"';

PROPTYPE:	(STRING | HTML | SIMPLEHTML | BOOLEAN | INT | DATETIME | IMAGEREF | MEDIAREF);
EDITHINT:	(SIZE LB NUMBER RB | TEXTFIELD | TEXTAREA | RADIO | DROPDOWN);
SHOWHIDE:	'show-hide';
AUTOCOMPLETE
	:	'autocomplete';
SCRIPTTRIGGER 
	:	 (ONCLICK | ONFOCUS | ONSELECT);
REQUIRED:	'required';
NOEXTRACT
	:	'noextract';
OVERRIDES
	:	'overrides';
GROUP	:	'group';
ORIANTATION
	:	HORIZONTAL;
BEFORE	:	'before';
AFTER	:	'after';
LETTER	:	('a'..'z' | 'A'..'Z');
NUMBER	:	('0'..'9')+;
NAME	:	(LETTER | '-' | '_')+;
FILENAME:	(NAME | '.' | '/')+;
ESC_SEQ	:	'\\' ('\"'|'\''|'\\') ;
QTEXT	:	'"'  ( ESC_SEQ | ~('\\'|'"') )* '"'
        {
            String s = getText();
            if (s.length() > 2) {
                s = s.substring(1, s.length() - 1);
                setText(s);
            }
        };
DEF	:	'##' .* '##'
        {
		  String s = getText();
		  s = s.replace("##", "");
		  setText(s.trim());
        };

WS	:	(' ' | '\t' | '\n')+ {$channel=HIDDEN;};

// Propertytypes
fragment STRING
	:	'string';
fragment HTML
	:	'html';
fragment SIMPLEHTML
	:	'simple_html';
fragment BOOLEAN
	:	'boolean';
fragment INT
	:	'int';
fragment DATETIME
	:	'datetime';
fragment IMAGEREF
	:	'image_ref';
fragment MEDIAREF
	:	'media_ref';

// Edithints
fragment SIZE
	:	'size';
fragment TEXTFIELD
	:	'textfield';
fragment TEXTAREA
	:	'textarea';
fragment RADIO
	:	'radio';
fragment DROPDOWN
	:	'dropdown';
fragment HORIZONTAL 
	:	'horizontal';

// Scripting
fragment ONCLICK
	:	'onclick';
fragment ONFOCUS
	:	'onfocus';
fragment ONSELECT
	:	'onselect';
