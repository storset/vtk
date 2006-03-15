package org.vortikal.repositoryimpl;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;

public class Authorization {

    private PrincipalManager principalManager;
    private Principal principal;
    private Principal owner;
    private ACLImpl acl;
    
    Authorization(Principal principal, ResourceImpl resource, 
            PrincipalManager principalManager) {
        this.principal = principal;
        this.principalManager = principalManager;
        this.owner = resource.getOwner();
        this.acl = resource.getACL();
    }
    
    void authorize(int protectionLevel) {
        // XXX: Implement me...
    }
}
