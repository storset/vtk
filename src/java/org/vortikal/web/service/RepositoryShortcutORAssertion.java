/*
 * $Id$
 */
package org.vortikal.web.service;

import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;


/**
 * @author Eirik Meland (eirik.meland@usit.uio.no)
 */
public class RepositoryShortcutORAssertion extends AbstractRepositoryAssertion implements InitializingBean {

    private List _assertions = null;

    /** 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet()
            throws Exception {
        if (_assertions == null) {
            throw new BeanInitializationException("No assertions provided");
        }
    }

    /** 
     * @see org.vortikal.web.service.Assertion#conflicts(org.vortikal.web.service.Assertion)
     */
    public boolean conflicts(Assertion assertion) {
        // TODO Auto-generated method stub
        return false;
    }
    
    /** 
     * @see org.vortikal.web.service.AbstractRepositoryAssertion#matches(org.vortikal.repository.Resource, org.vortikal.security.Principal)
     */
    public boolean matches(Resource resource, Principal principal) {
        for (Iterator assertionIter = _assertions.iterator(); assertionIter.hasNext();) {
            RepositoryAssertion assertion = (RepositoryAssertion) assertionIter.next();
            if (assertion.matches(resource, principal)) {
                return true;
            }
        }
        
        return false;
    }
    
    public void setAssertions(List assertions) {
        _assertions = assertions;
    }
}
