package org.vortikal.web.service;

import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;

public class IsAuthenticatedAssertion implements PrincipalAssertion {


    private boolean requiresAuthentication = false;
    private boolean invert = false;
    
    public boolean matches(Principal principal) {
        
        
        if (requiresAuthentication && principal == null)
            throw new AuthenticationException();
        
        if (principal == null) 
            return invert;

        return !invert;
    }

    public void processURL(URL url, Resource resource, Principal principal) {
        // Nothing to do
    }

    public boolean conflicts(Assertion assertion) {
        if (assertion instanceof IsAuthenticatedAssertion) {
            IsAuthenticatedAssertion a = (IsAuthenticatedAssertion) assertion;
            return (isInvert() != a.isInvert());
        }
        return false;
    }

    public boolean isInvert() {
        return invert;
    }
    
    public void setInvert(boolean invert) {
        this.invert = invert;
    }
    

    public void setRequiresAuthentication(boolean requiresAuthentication) {
        this.requiresAuthentication = requiresAuthentication;
    }
    

}
