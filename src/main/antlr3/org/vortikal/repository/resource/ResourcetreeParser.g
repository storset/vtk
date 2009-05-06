parser grammar ResourcetreeParser;

options {
  tokenVocab = ResourcetreeLexer;
  output = AST;
}

tokens {
  PARENT;
  PROPERTY;
}

@header {
package org.vortikal.repository.resource;
}

resources     : (resourcetypedef)+;

resourcetypedef : RESOURCETYPE NAME (parent)? LCB
                    resourcedef
                  RCB
                  -> ^(NAME (parent)? resourcedef)
                ;

parent        : COLON NAME -> ^(PARENT NAME);

resourcedef   : (resourceprops)?
                (editrules)?
                (viewdefinition)?
              ;

resourceprops : PROPERTIES LCB
                  (propertytypedef (COMMA propertytypedef)*)*
                RCB
                -> ^(PROPERTIES (propertytypedef)*)
              ;

propertytypedef : NAME COLON PROPTYPE -> ^(PROPERTY ^(COLON NAME PROPTYPE));

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
