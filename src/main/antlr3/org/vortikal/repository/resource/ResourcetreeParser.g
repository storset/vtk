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
	|
		include
	;

include	:	INCLUDE FILENAME -> ^(INCLUDE FILENAME);

parent	:	COLON NAME -> ^(PARENT NAME);

resourcedef
	:	(resourceprops)?
		(editrules)?
		(viewcomponents)?
		(viewdefinition)?
		(localization)?
	;

localization
	:	LOCALIZATIONPROPERTIES LCB
		  (localizationentry (COMMA localizationentry)*)*
		RCB
		-> ^(LOCALIZATIONPROPERTIES (localizationentry)*)
	;

localizationentry
	:	NAME COLON LP (localizationdef (COMMA localizationdef)*) RP
		-> ^(NAME (localizationdef)*)
	;
	
localizationdef
	:	NAME COLON QTEXT
		-> ^(NAME (QTEXT)+)
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
	|	GROUP NAME grouping position (ORIANTATION)?
		-> ^(GROUP ^(NAME grouping) ^(position) ^(ORIANTATION)?)
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

// XXX write rule
displayhint
	:
	;

grouping:	LP NAME (COMMA NAME)+ RP -> ^(NAME) ^(NAME)+;

viewcomponents
	:	VIEWCOMPONENTS LCB
		  (viewcomponent)*
		RCB
		-> ^(VIEWCOMPONENTS (viewcomponent)*)
	;

viewcomponent
    :   NAME LCB
		  VIEWCOMPDEF
        RCB
        -> ^(NAME (VIEWCOMPDEF)?)
    ;

viewdefinition
	:	VIEWDEFINITION LCB
		  (VIEWDEF)?
		RCB
		-> ^(VIEWDEFINITION (VIEWDEF)?)
	;
