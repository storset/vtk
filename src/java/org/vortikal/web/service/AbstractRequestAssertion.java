package org.vortikal.web.service;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;

public abstract class AbstractRequestAssertion extends AssertionSupport implements RequestAssertion {

    public boolean matches(HttpServletRequest request, Resource resource,
            Principal principal) {
        return matches(request);
    }

}
