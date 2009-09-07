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
	:	(derivedpropertytypedef | jsonpropertytypedef | plainpropertytypedef)
	;

derivedpropertytypedef
	:	NAME COLON derived (overrides)?
		-> ^(NAME derived (overrides)?)
	;

derived	:	DERIVED LP fieldlist RP EVAL LP evallist RP (defaultprop)?
		-> ^(DERIVED ^(FIELDS fieldlist) ^(EVAL evallist) (defaultprop)?)
	;

fieldlist
    :   NAME (COMMA NAME)*
        ->  NAME+
    ;

evallist:	nameorqtext (PLUS nameorqtext)* -> nameorqtext+;

nameorqtext
	:	NAME -> NAME
	|	QTEXT -> DQ QTEXT DQ
	;
    
defaultprop
    :   DEFAULTPROP NAME
    ;

jsonpropertytypedef
	:	NAME COLON JSON jsonspec (MULTIPLE)? (NOEXTRACT)? (external)?
		-> ^(NAME ^(JSON jsonspec) (MULTIPLE)? (NOEXTRACT)? (external)?)
	;

jsonspec
    : LP jsonpropspeclist RP
        -> jsonpropspeclist
    ;

jsonpropspeclist
    :   jsonpropspec (COMMA jsonpropspec)*
        -> jsonpropspec+
    ;

jsonpropspec
    :  NAME COLON PROPTYPE
        -> ^(NAME PROPTYPE)
    ;

plainpropertytypedef
	:	NAME COLON PROPTYPE (MULTIPLE)? (REQUIRED)? (NOEXTRACT)? (overrides)?
			(external)?
		-> ^(NAME PROPTYPE (MULTIPLE)? (REQUIRED)? (NOEXTRACT)? (overrides)?
			(external)?)
	;


external
	:	EXTERNAL COLON NAME -> ^(EXTERNAL NAME);

overrides
	:	OVERRIDES NAME
		-> ^(OVERRIDES NAME)
	;


editruledef
	:	NAME (position)? (edithint)?
		-> ^(NAME ^(position)? ^(edithint)?)
	|	GROUP NAME namelist (position)? (ORIENTATION)?
		-> ^(GROUP ^(NAME namelist) ^(position)? ^(ORIENTATION)?)
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
	:	NAME (LP paramlist RP)? LCB
		  DEF
		RCB
		-> ^(NAME (DEF)? (paramlist)?)
	;

paramlist
    :   NAME (COMMA NAME)*
        ->  NAME+
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
	|	NAME MULTIPLEINPUTFIELDS
		-> ^(NAME ^(MULTIPLEINPUTFIELDS))
	;

services:	SERVICES LCB
		  (servicedef (COMMA servicedef)*)*
		RCB
		-> ^(SERVICES ^(servicedef)*)
	;

servicedef
	:	NAME NAME (requires)?
		-> ^(NAME ^(NAME (requires)?))
	;

requires:	REQUIRES namelist -> ^(REQUIRES namelist);

namevaluepair
	:	NAME COLON QTEXT
		-> ^(NAME QTEXT)
	;

namelist:	LP NAME (COMMA NAME)* RP -> ^(NAME) ^(NAME)*;
