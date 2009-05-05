parser grammar ResourcetreeParser;

options {
  tokenVocab = ResourcetreeLexer;
  output = AST;
}

@header {
package org.vortikal.repository.resource;
}

resources     : (resourcetypedef)+;

resourcetypedef : RESOURCETYPE NAME (parent)? LCB
                    resourcedef
                  RCB
                  -> ^(NAME resourcedef)
                ;

parent        : COLON NAME;

resourcedef   : (resourceprops)?
                (editrules)?
                (viewdefinition)?
              ;

resourceprops : PROPERTIES LCB
                  (propertytypedef (COMMA propertytypedef)*)*
                RCB
                -> ^(PROPERTIES (propertytypedef)*)
              ;

propertytypedef : NAME COLON PROPTYPE -> ^(NAME PROPTYPE);

editrules     : EDITRULES LCB
                  // ruledef
                RCB
                -> ^(EDITRULES)
              ;

viewdefinition : VIEWDEFINITION LCB
                   // ruledef
                 RCB
                 -> ^(VIEWDEFINITION)
               ;
