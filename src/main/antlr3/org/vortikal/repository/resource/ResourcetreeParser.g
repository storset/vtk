parser grammar ResourcetreeParser;

options {
  tokenVocab = ResourcetreeLexer;
}

@header {
package org.vortikal.repository.resource;
}

resources     : (resourcetypedef)+ ;

resourcetypedef : RESOURCETYPE NAME parent
                  resourcedef
                ;

parent        : (COLON NAME)?;

resourcedef   : LCB
                 (resourceprops)?
                 (editrules)?
                 (viewdefinition)?
                RCB
              ;

resourceprops : PROPERTIES LCB
                  (propertytypedef (COMMA propertytypedef)*)*
                RCB
              ;

propertytypedef : NAME COLON PROPTYPE;

editrules     : EDITRULES LCB
                  // ruledef
                RCB
              ;

viewdefinition : VIEWDEFINITION LCB
                   // ruledef
                 RCB
               ;
