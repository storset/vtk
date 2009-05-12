parser grammar ResourcetreeParser;

options {
  tokenVocab = ResourcetreeLexer;
  output = AST;
}

tokens {
  /* Imaginary token used for intermediate handeling 
     of proper child-parent relationship */
  PARENT;
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
	:	NAME COLON PROPTYPE (REQUIRED)? -> ^(NAME PROPTYPE (REQUIRED)?);

editrules
	:	EDITRULES LCB
                  (editruledef (COMMA editruledef)*)*
                RCB
                -> ^(EDITRULES (editruledef)*)
        ;

editruledef
	:	NAME position -> ^(NAME position)
	|	GROUP NAME grouping (position)? -> ^(GROUP ^(NAME grouping) ^(position)?)
	;

position
	:	pos NAME COLON NAME -> ^(pos ^(NAME NAME));

pos	:	(BEFORE | AFTER);

grouping:	LP NAME (COMMA NAME)+ RP -> ^(NAME) ^(NAME)+;

viewdefinition
	:	VIEWDEFINITION LCB
                   // viewdef
                 RCB
                 -> ^(VIEWDEFINITION)
        ;
