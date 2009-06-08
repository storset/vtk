parser grammar ResourcetreeParser;

options {
  tokenVocab = ResourcetreeLexer;
  output = AST;
}

@header {
package org.vortikal.repository.resource;
}

resources
	:	(resourcetypedef)+;


parent	:	COLON NAME -> ^(PARENT NAME);

resourcetypedef
	:	RESOURCETYPE NAME (parent)? LCB
		  resourcedef
		RCB
		-> ^(RESOURCETYPE ^(NAME (parent)? (resourcedef)?))
	;

resourcedef
	:	(resourceprops)?
		(editrules)?
		(viewdefinition)?
	;

resourceprops
	:	PROPERTIES LCB
		  (propertytypedef (COMMA propertytypedef)*)*
		RCB
		-> ^(PROPERTIES (propertytypedef)*)
	;

propertytypedef
	:	NAME COLON PROPTYPE (REQUIRED)? (NOEXTRACT)? (overrides)?
		-> ^(NAME PROPTYPE (REQUIRED)? (NOEXTRACT)? (overrides)?)
	;

overrides
	:	LP OVERRIDES COLON NAME RP
		-> ^(OVERRIDES NAME)
	;


editruledef
	:	NAME (position)? (edithint)?
		-> ^(NAME ^(position)? ^(edithint)?)
	|	GROUP NAME grouping position
		-> ^(GROUP ^(NAME grouping) ^(position))
	;

editrules
	:	EDITRULES LCB
		  (editruledef (COMMA editruledef)*)*
		RCB
		-> ^(EDITRULES (editruledef)*)
	;


position
	:	LP pos NAME RP -> ^(pos NAME);

pos	:	(BEFORE | AFTER);

edithint:	LP EDITHINT RP -> ^(EDITHINT);

// XXX write rult
displayhint
	:	;

grouping:	LP NAME (COMMA NAME)+ RP -> ^(NAME) ^(NAME)+;

viewdefinition
	:	VIEWDEFINITION LCB
		  (VIEWDEF)?
		RCB
		-> ^(VIEWDEFINITION (VIEWDEF)?)
	;
