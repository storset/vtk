parser grammar ResourcetreeParser;

options {
  tokenVocab = ResourcetreeLexer;
  output = AST;
}

@header {
package org.vortikal.repository.resource;
}

@members {
    private java.util.List<String> messages = new java.util.ArrayList<String>();

    public void emitErrorMessage(String msg) {
        this.messages.add(msg);
    }

    public java.util.List<String> getErrorMessages() {
        return this.messages;
    }
}

resources
	:	(resourcetypedef)+;

resourcetypedef
	:	RESOURCETYPE NAME (parent)? LCB
		  resourcedef
		RCB
		-> ^(RESOURCETYPE ^(NAME (parent)? (resourcedef)?))
	|	include
	;

include	:	INCLUDE FILENAME -> ^(INCLUDE FILENAME);

parent	:	COLON NAME -> ^(PARENT NAME);

resourcedef
	:	(resourceprops)?
		(editrules)?
		(scripts)?
		(services)?
		(viewcomponents)?
		(viewdefinition)?
		(localization)?
	;

localization
	:	LOCALIZATION LCB
		  (localizationentry (COMMA localizationentry)*)*
		RCB
		-> ^(LOCALIZATION (localizationentry)*)
	;

localizationentry
	:	NAME COLON LP (namevaluepair (COMMA namevaluepair)*) RP
		-> ^(NAME (namevaluepair)*)
	;
	
resourceprops
	:	PROPERTIES LCB
		  (propertytypedef (COMMA propertytypedef)*)*
		RCB
		-> ^(PROPERTIES (propertytypedef)*)
	;

propertytypedef
	:	NAME COLON PROPTYPE (MULTIPLE)? (REQUIRED)? (NOEXTRACT)? (overrides)?
		-> ^(NAME PROPTYPE (MULTIPLE)? (REQUIRED)? (NOEXTRACT)? (overrides)?)
	;

overrides
	:	LP OVERRIDES COLON NAME RP
		-> ^(OVERRIDES NAME)
	;


editruledef
	:	NAME (position)? (edithint)?
		-> ^(NAME ^(position)? ^(edithint)?)
	|	GROUP NAME namelist position (ORIANTATION)?
		-> ^(GROUP ^(NAME namelist) ^(position) ^(ORIANTATION)?)
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

viewcomponents
	:	VIEWCOMPONENTS LCB
		  (viewcomponent)*
		RCB
		-> ^(VIEWCOMPONENTS (viewcomponent)*)
	;

viewcomponent
	:	NAME LCB
		  DEF
		RCB
		-> ^(NAME (DEF)?)
	;

viewdefinition
	:	VIEW LCB
		  (DEF)?
		RCB
		-> ^(VIEW (DEF)?)
	;

scripts	:	SCRIPTS LCB
		  (scriptdef (COMMA scriptdef)*)*
		RCB
		-> ^(SCRIPTS (scriptdef)*)
	;

scriptdef:	NAME SHOWHIDE SCRIPTTRIGGER namelist
		-> ^(NAME ^(SHOWHIDE ^(SCRIPTTRIGGER namelist)))
	|	NAME AUTOCOMPLETE LP (namevaluepair (COMMA namevaluepair)*) RP
		-> ^(NAME ^(AUTOCOMPLETE (namevaluepair)*))
	;

services:	SERVICES LCB
		  (servicedef (COMMA servicedef)*)*
		RCB
		-> ^(SERVICES ^(servicedef)*)
	;

servicedef
	:	NAME EXTERNAL (requires)? (affects)?
		-> ^(NAME ^(EXTERNAL (requires)? (affects)?))
	;

requires:	REQUIRES LP NAME RP -> ^(REQUIRES NAME);

affects	:	AFFECTS namelist -> ^(AFFECTS namelist);

namevaluepair
	:	NAME COLON QTEXT
		-> ^(NAME QTEXT)
	;

namelist:	LP NAME (COMMA NAME)* RP -> ^(NAME) ^(NAME)*;
