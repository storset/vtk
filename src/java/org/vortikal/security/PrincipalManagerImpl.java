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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.vortikal.util.cache.SimpleCacheImpl;


/**
 * A simple principal manager implementation.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>defaultDomain</code> - a {@link String} specifying the
 *   domain to append to unqualified principal names. If this
 *   property is not specified, the behavior will be as if no domains
 *   exist at all.
 *   <li><code>domainURLMap</code> - a map of (<code>domain,
 *   URL-pattern</code>) entries. The domain corresponds to a
 *   principal {@link Principal#getDomain domain}, and the URL pattern
 *   is a string in which the sequence <code>%u</code> is substituted
 *   with the principal's (short) {@link Principal#getName name},
 *   thereby forming a unique URL for each principal. This URL can be
 *   acessed using the {@link Principal#getURL} method.
 * </ul>
 */
public class PrincipalManagerImpl implements PrincipalManager, InitializingBean,
                                             ApplicationContextAware {
    
    private Log logger = LogFactory.getLog(this.getClass());

    private String delimiter = "@";
    private String defaultDomain;
    private Map domainURLMap;
    private PrincipalStore principalStore;
    
    private ApplicationContext applicationContext;
    


    public void setDefaultDomain(String defaultDomain) {
        if (defaultDomain != null) {

            if ("".equals(defaultDomain.trim())) {
                defaultDomain = null;

            } else if (defaultDomain.indexOf(this.delimiter) != -1) {
                throw new InvalidPrincipalException(
                    "Invalid domain: " + defaultDomain + ": "
                    + "must not contain delimiter: '" + this.delimiter + "'");
            }
            this.defaultDomain = defaultDomain;
        }
    }
    

    public void setDomainURLMap(Map domainURLMap) {
        this.domainURLMap = domainURLMap;
    }
    

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
                applicationContext, PrincipalStore.class, true, false);
    
            List allStores = new ArrayList(matchingBeans.values());
            List stores = new ArrayList();
            for (Iterator iter = allStores.iterator(); iter.hasNext();) {
                PrincipalStore store = (PrincipalStore) iter.next();
                if (store instanceof Ordered) {
                    stores.add(store);
                }
            }

            Collections.sort(stores, new OrderComparator());

            if (stores.size() > 0) {
                this.principalStore = new ChainedPrincipalStore(stores, new SimpleCacheImpl(60));
            }
        }
        
        if (this.principalStore == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'principalStore' must be specified");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Using principal store " + this.principalStore);
        }
    }
    
    public Principal getUserPrincipal(String id) {
        return getPrincipal(id, Principal.TYPE_USER);
    }
    
    public Principal getGroupPrincipal(String id) {
        return getPrincipal(id, Principal.TYPE_GROUP);
    }
    
    private Principal getPrincipal(String id, int type) {
        if (id == null) {
            throw new InvalidPrincipalException("Tried to get null principal");
        }

        id = id.trim();
        
        if (id.equals(""))
            throw new InvalidPrincipalException("Tried to get \"\" (empty string) principal");
        
        if (id.startsWith(this.delimiter)) {
            throw new InvalidPrincipalException(
                "Invalid principal id: " + id + ": "
                + "must not start with delimiter: '" + this.delimiter + "'");
        }
        if (id.endsWith(this.delimiter)) {
            throw new InvalidPrincipalException(
                "Invalid principal id: " + id + ": "
                + "must not end with delimiter: '" + this.delimiter + "'");
        }

        if (id.indexOf(this.delimiter) != id.lastIndexOf(this.delimiter)) {
            throw new InvalidPrincipalException(
                "Invalid principal id: " + id + ": "
                + "must not contain more that one delimiter: '"
                + this.delimiter + "'");
        }


        /* Initialize name, domain and qualifiedName to default values
         * matching a setup "without" domains: */
        String name = id;
        String domain = null;
        String qualifiedName = id;
        
        if (id.indexOf(this.delimiter) > 0) {

            /* id is a fully qualified principal with a domain part: */
            domain = id.substring(id.indexOf(this.delimiter) + 1);

            if (this.defaultDomain != null && this.defaultDomain.equals(domain)) {
                /* In cases where domain equals default domain, strip
                 * the domain part off the name: */
                name = id.substring(0, id.indexOf(this.delimiter));
            } 
                        
        } else if (this.defaultDomain != null) {

            /* id is not a fully qualified principal, but since we
             * have a default domain, we append it: */
            domain = this.defaultDomain;
            qualifiedName = name + this.delimiter + domain;
        }

        String url = null;
        if (domain != null && this.domainURLMap != null) {
            String pattern = (String) this.domainURLMap.get(domain);
            if (pattern != null) {
                url = pattern.replaceAll("%u", name);
            }
        }

        PrincipalImpl p  = new PrincipalImpl(name, qualifiedName, domain, url);
        p.setType(type);
        return p;
    }

    public boolean validatePrincipal(Principal principal)
        throws AuthenticationProcessingException {
        return this.principalStore.validatePrincipal(principal);
    }


    public boolean validateGroup(Principal group) throws AuthenticationProcessingException {
        return this.principalStore.validateGroup(group);
    }


//    public String[] resolveGroup(Principal group) throws AuthenticationProcessingException {
//        return this.principalStore.resolveGroup(group);
//    }


    public boolean isMember(Principal principal, Principal group) {
        return this.principalStore.isMember(principal, group);
    }
}
