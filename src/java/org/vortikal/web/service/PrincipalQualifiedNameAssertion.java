package org.vortikal.web.service;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;

public class PrincipalQualifiedNameAssertion extends AbstractRepositoryAssertion {

    private String username;
    private boolean equals = true;
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }

    
    public boolean matches(Resource resource, Principal principal) {
        if (principal != null) {
           
            boolean match = username.equals(principal.getQualifiedName());
            
            return (isEquals()) ? match : !match;
        }
        
        return false;
    }
    
    /** 
     * @see org.vortikal.web.service.Assertion#conflicts(org.vortikal.web.service.Assertion)
     */
    public boolean conflicts(Assertion assertion) {
        if (assertion instanceof PrincipalQualifiedNameAssertion) {
            PrincipalQualifiedNameAssertion other = (PrincipalQualifiedNameAssertion) assertion;
            
            if (isEquals() && other.isEquals()) {
                if (!getUsername().equals(other.getUsername()))
                    return true;
            } else if (isEquals() || other.isEquals()) {
                if (getUsername().equals(other.getUsername()))
                    return true;
            }
        }
        return false;
    }
    
    public boolean isEquals() {
        return equals;
    }
    

    public void setEquals(boolean equals) {
        this.equals = equals;
    }

}
