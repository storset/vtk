tree grammar ResourcetreeWalker;

options {
  tokenVocab = ResourcetreeParser;
  ASTLabelType = CommonTree;
}

@header {
package org.vortikal.repository.resource;
}

@members {
private CommonTree getParent(CommonTree child) {
  return null;
}
}

resources
        // Damn it...
	:	 {};
