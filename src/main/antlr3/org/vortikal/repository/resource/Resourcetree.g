grammar Resourcetree;

tokens {
  RESOURCETYPE = 'resourcetype';
  PROPERTIES = 'properties';
  EDITRULES = 'edit-rules';
  VIEWDEFINITION = 'view-definition';
  LCB    = '{' ;
  RCB    = '}' ;
  LP     = '(' ;
  RP     = ')' ;
  COLON  = ':' ;
  COMMA  = ',' ;
}

@header {
package org.vortikal.repository.resource;
}

@lexer::header {
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
                  propertytypedef (COMMA propertytypedef)*
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

PROPTYPE : ('string' | 'html' | 'boolean' | 'int' | 'timestamp');
NAME     : ('a'..'z' | 'A'..'Z' | '-')+;
WS       : (' ' | '\t' | '\n')+ {$channel=HIDDEN;};
