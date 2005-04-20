
package org.vortikal.web.service;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;



public interface RepositoryAssertion {

    public boolean matches(Resource resource, Principal principal);

}