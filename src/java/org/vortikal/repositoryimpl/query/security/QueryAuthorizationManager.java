package org.vortikal.repositoryimpl.query.security;

import org.vortikal.repository.AuthorizationException;

/**
 * Authorize results in queries.
 * 
 * @author oyviste
 *
 */
public interface QueryAuthorizationManager {

    public void authorizeQueryResult(String token, String uri)
        throws AuthorizationException;
    
    public void authorizeQueryResult(String token, int resourceId, int aclInheritedFrom)
        throws AuthorizationException;

}
