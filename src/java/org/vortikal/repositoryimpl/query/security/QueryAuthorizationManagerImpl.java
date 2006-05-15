/**
 * 
 */
package org.vortikal.repositoryimpl.query.security;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repositoryimpl.dao.IndexDataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.PrincipalManager;

/**
 * TODO: finish
 * @author oyviste
 */
public class QueryAuthorizationManagerImpl 
    implements QueryAuthorizationManager, InitializingBean {

    private PrincipalManager principalManager;
    private IndexDataAccessor indexDataAccessor;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (principalManager == null) {
            throw new BeanInitializationException("Property 'principalManager' not set.");
        } else if (indexDataAccessor == null) {
            throw new BeanInitializationException("Property 'indexDataAccessor' not set.");
        }
    }

    public void authorizeQueryResult(String token, String uri) 
        throws AuthorizationException, AuthenticationException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.vortikal.repositoryimpl.query.security.QueryAuthorizationManager#authorizeQueryResult(java.lang.String, int, int)
     */
    public void authorizeQueryResult(String token, int resourceId, int aclInheritedFrom)
            throws AuthorizationException, AuthenticationException {

        // TODO Auto-generated method stub
    }

}
