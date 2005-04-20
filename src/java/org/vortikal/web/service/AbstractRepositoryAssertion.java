package org.vortikal.web.service;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;

public abstract class AbstractRepositoryAssertion extends AbstractAssertion implements RepositoryAssertion {

    public boolean processURL(URL url, Resource resource, Principal principal,
            boolean match) {
        if (match) return matches(resource, principal);
        return true;
    }

    public boolean matches(HttpServletRequest request, Resource resource,
            Principal principal) {
        return matches(resource, principal);
    }

    public abstract boolean matches(Resource resource, Principal principal);
    
    
}
