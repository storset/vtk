/* Copyright (c) 2004, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.vortikal.security.store.ChainedGroupStore;
import org.vortikal.security.store.ChainedPrincipalStore;
import org.vortikal.util.cache.SimpleCacheImpl;


/**
 * A simple principal manager implementation.
 *
 * XXX: reevaluate lookup strategy for stores!
 */
public class PrincipalManagerImpl implements PrincipalManager, InitializingBean,
                                             ApplicationContextAware {
    
    private Log logger = LogFactory.getLog(this.getClass());

    private PrincipalStore principalStore;
    private GroupStore groupStore;
    
    private ApplicationContext applicationContext;
    



    public void setPrincipalStore(PrincipalStore principalStore) {
        this.principalStore = principalStore;
    }


    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void afterPropertiesSet() throws Exception {

        if (this.principalStore == null) {
            
            // Try to look up principal stores from the context

            Map matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
                this.applicationContext, PrincipalStore.class, true, false);
    
            List stores = new ArrayList(matchingBeans.values());
            Collections.sort(stores, new OrderComparator());

            if (stores.size() > 0) {
                this.principalStore = new ChainedPrincipalStore(stores);
            }
        }
        
        if (this.principalStore == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'principalStore' must be specified");
        }

        if (this.logger.isInfoEnabled()) {
            this.logger.info("Using principal store " + this.principalStore);
        }

        if (this.groupStore == null) {
            
            // Try to look up principal stores from the context

            Map matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
                this.applicationContext, GroupStore.class, true, false);
    
            List stores = new ArrayList(matchingBeans.values());
            Collections.sort(stores, new OrderComparator());

            if (stores.size() > 0) {
                this.groupStore = new ChainedGroupStore(stores, new SimpleCacheImpl(60));
            }
        }
        
        if (this.groupStore == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'groupStore' must be specified");
        }

        if (this.logger.isInfoEnabled()) {
            this.logger.info("Using group store " + this.groupStore);
        }

    }
    
    public boolean validatePrincipal(Principal principal)
        throws AuthenticationProcessingException {
        return this.principalStore.validatePrincipal(principal);
    }


    public boolean validateGroup(Principal group) throws AuthenticationProcessingException {
        return this.groupStore.validateGroup(group);
    }


    public boolean isMember(Principal principal, Principal group) {
        return this.groupStore.isMember(principal, group);
    }


    public Set getMemberGroups(Principal principal) {
        return this.groupStore.getMemberGroups(principal);
    }
}
