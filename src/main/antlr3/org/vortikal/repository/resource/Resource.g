grammar Resource;

@header {
package org.vortikal.repository.resource;
}

@lexer::header {
package org.vortikal.repository.resource;
}

resourceDef : 'resourcetype' NAME ;
NAME : ('a'..'z' | 'A'..'Z' | '-')+ ;