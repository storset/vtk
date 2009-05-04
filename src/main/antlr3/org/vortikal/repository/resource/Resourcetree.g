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

import org.vortikal.repository.Namespace;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
}

@lexer::header {
package org.vortikal.repository.resource;
}

resources     : (resourcetypedef)+ ;

resourcetypedef
scope {
        PrimaryResourceTypeDefinitionImpl resource;
        List<PropertyTypeDefinition> props;
      }
@init {
        $resourcetypedef::resource = new PrimaryResourceTypeDefinitionImpl();
        $resourcetypedef::props = new ArrayList<PropertyTypeDefinition>();
      }
              : (
                   RESOURCETYPE NAME parent
                     resourcedef
                )
                {
                  $resourcetypedef::resource.setName($NAME.text);
                  $resourcetypedef::resource.setNamespace(Namespace.DEFAULT_NAMESPACE);
                  $resourcetypedef::resource.setPropertyTypeDefinitions(
                    $resourcetypedef::props.toArray(new PropertyTypeDefinition[$resourcetypedef::props.size()])
                  );
                }
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

propertytypedef
scope { PropertyTypeDefinitionImpl property; }
@init { $propertytypedef::property = new PropertyTypeDefinitionImpl(); }
              : (
                  NAME COLON PROPTYPE
                )
                {
                  $propertytypedef::property.setName($NAME.text);
                  $propertytypedef::property.setNamespace(Namespace.DEFAULT_NAMESPACE);
                  // XXX set proptype
                  $resourcetypedef::props.add($propertytypedef::property);
                }
              ;

editrules     : EDITRULES LCB
                  // XXX write ruledef
                RCB
              ;

viewdefinition : VIEWDEFINITION LCB
                   // XXX write ruledef
                 RCB
               ;

PROPTYPE : ('string' | 'html' | 'boolean' | 'int' | 'timestamp');
NAME     : ('a'..'z' | 'A'..'Z' | '-')+;
WS       : (' ' | '\t' | '\n')+ {$channel=HIDDEN;};
