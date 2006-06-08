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
package org.vortikal.security.token;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.doomdark.uuid.UUIDGenerator;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.security.Principal;
import org.vortikal.security.web.AuthenticationHandler;
import org.vortikal.util.cache.SimpleCache;
import org.vortikal.util.cache.SimpleCacheImpl;



/**
 * Default implementation of the {@link TokenManager} interface. Keeps
 * tokens in a cache (using a {@link SimpleCache}). Also supports
 * generating trusted tokens for system purposes (e.g. reading all
 * resources, etc.)
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>cache</code> - a {@link SimpleCache} holding the
 *   tokens.
 *   <li><code>defaultPrincipals</code> - a {@link List} of {@link
 *   Principal principals} that exist in the token manager without
 *   having to authenticate (such as root users and other system
 *   users).
 * </ul>
 */
public class TokenManagerImpl implements TokenManager, InitializingBean {

    private static Log logger = LogFactory.getLog(TokenManagerImpl.class);

    private SimpleCache cache = null;
    private Map registeredPrincipals = new HashMap();
    private List defaultPrincipals = null;
    

    public void setCache(SimpleCache cache) {
        this.cache = cache;
    }

    public void setDefaultPrincipals(List defaultPrincipals) {
        this.defaultPrincipals = defaultPrincipals;
    }
    
    public void afterPropertiesSet() throws Exception {

        if (defaultPrincipals != null) {
            for (Iterator iter = defaultPrincipals.iterator(); iter.hasNext();) {
                Principal principal = (Principal) iter.next();
                String token = generateID();
                registeredPrincipals.put(token,principal);
            }
        }
        
        if (cache == null) {
            logger.info("No SimpleCache supplied, instantiating default");
            cache = new SimpleCacheImpl();    
        }
    }


    public Principal getPrincipal(String token) {

        if (registeredPrincipals.containsKey(token))
            return (Principal)registeredPrincipals.get(token);
        
        PrincipalItem item = (PrincipalItem) cache.get(token);
        if (item == null) {
            return null;
        }
        return item.getPrincipal();
    }


    public AuthenticationHandler getAuthenticationHandler(String token) {
        if (registeredPrincipals.containsKey(token))
            return null;
        
        PrincipalItem item = (PrincipalItem) cache.get(token);
        if (item == null) {
            return null;
        }
        return item.getAuthenticationHandler();
    }
    


    public String newToken(Principal principal,
                           AuthenticationHandler authenticationHandler) {
        String token = generateID();
        PrincipalItem item = new PrincipalItem(principal, authenticationHandler);
        cache.put(token, item);
        return token;
    }

    public void removeToken(String token) {

        PrincipalItem item = (PrincipalItem) cache.get(token);
        if (item == null) {
            throw new IllegalArgumentException(
                    "Tried to remove unexisting token: " + token);
        }
        cache.remove(token);
    }

    public String getRegisteredToken(Principal principal) {
        if (registeredPrincipals.containsValue(principal)) {
            for (Iterator iter = registeredPrincipals.keySet().iterator(); iter.hasNext();) {
                String token = (String) iter.next();
                if (registeredPrincipals.get(token).equals(principal))
                        return token;
            }
        }
        return null;
    }


    private String generateID() {
        String uuid = UUIDGenerator.getInstance().generateRandomBasedUUID()
                .toString();
        return uuid;
    }
 

    /**
     * Class holding (Principal, AuthenticationHandler) pairs.
     */
    private class PrincipalItem {
        private Principal principal = null;
        private AuthenticationHandler authenticationHandler = null;

        public PrincipalItem(Principal principal,
                             AuthenticationHandler authenticationHandler) {
            this.principal = principal;
            this.authenticationHandler = authenticationHandler;
        }

        public Principal getPrincipal() {
            return this.principal;
        }

        public AuthenticationHandler getAuthenticationHandler() {
            return this.authenticationHandler;
        }
    }

}
