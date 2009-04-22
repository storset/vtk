grammar Resource;

@header {
package org.vortikal.repository.resource;
}

@lexer::header {
package org.vortikal.repository.resource;
}

resourceDef : 'resourcetype' NAME ;
NAME : ('a'..'z' | 'A'..'Z' | '-')+ ;

WS : (' '|'\r'|'\t'|'\u000C'|'\n') {$channel=HIDDEN;} ;

COMMENT : '/*' ( options {greedy=false;} : . )* '*/' {$channel=HIDDEN;} ;