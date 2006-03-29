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
package org.vortikal.security.web;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.util.cache.SimpleCache;
import org.vortikal.util.codec.Base64;
import org.vortikal.util.codec.MD5;


/**
 * Abstract base class for performing HTTP/Basic
 * authentication. Subclasses normally only need to override the
 * {@link #authenticateInternal(Principal, String)} method.
 * 
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>principalManager</code> - a {@link PrincipalManager} (required)
 *   <li><code>challenge</code> - an {@link
 *   HttpBasicAuthenticationChallenge authentication challenge}
 *   (required)
 *   <li><code>recognizedDomains</code> - a {@link Set} specifying the
 *     recognized principal {@link Principal#getDomain domains}. If
 *     this property is not specified, all domains are matched.
 *   <li><code>cache</code> - simple {@link SimpleCache cache} to
 *   allow for clients that don't send cookies (optional)
 *   <li><code>excludedPrincipals</code> - a {@link Set} of principal
 *     names to actively exclude when matching authentication
 *     requests. If the principal in an authentication request is
 *     present in this set, the authentication request is treated as if
 *     it were not recognized, even though it normally would.
 *   <li><code>order</code> - the bean order returned in {@link
 *   Ordered#getOrder}
 *  </ul>
 */
public abstract class AbstractHttpBasicAuthenticationHandler 
  implements AuthenticationHandler, Ordered, InitializingBean {

    protected Log logger = LogFactory.getLog(this.getClass());

    /* Simple cache to allow for clients that don't send cookies */
    private SimpleCache cache = null;
    private Set recognizedDomains = null;
    private Set excludedPrincipals = new HashSet();
    private int order = Integer.MAX_VALUE;

    protected HttpBasicAuthenticationChallenge challenge;
    protected PrincipalManager principalManager;
  
    public void setCache(SimpleCache cache) {
        this.cache = cache;
    }
    
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setChallenge(HttpBasicAuthenticationChallenge challenge) {
        this.challenge = challenge;
    }
    
    public void setRecognizedDomains(Set recognizedDomains) {
        this.recognizedDomains = recognizedDomains;
    }

    public void setExcludedPrincipals(Set excludedPrincipals) {
        this.excludedPrincipals = excludedPrincipals;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public int getOrder() {
        return this.order;
    }
    

    public void afterPropertiesSet() {
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                "Property 'principalManager' must be set");
        }
        
        if (this.challenge == null) {
            throw new BeanInitializationException(
                "Property 'challenge' must be set");
        }
    }

    public boolean isRecognizedAuthenticationRequest(HttpServletRequest req)
    throws AuthenticationProcessingException {

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) 
            return false;

        String username = null;
        Principal principal = null;
        
        try {
            username = getUserName(req);
        } catch (IllegalArgumentException e) {
            throw new InvalidAuthenticationRequestException(e);
        }


        try {
            principal = this.principalManager.getUserPrincipal(username);
        } catch (InvalidPrincipalException e) {
            return false;
        }
        
        if (this.excludedPrincipals != null
            && this.excludedPrincipals.contains(principal.getQualifiedName())) {
            return false;
        }

        if (this.recognizedDomains == null
            || this.recognizedDomains.contains(principal.getDomain()))
            return true;
    
        return false;
    }


    public Principal authenticate(HttpServletRequest request)
        throws AuthenticationProcessingException, AuthenticationException {

        String username = null;
        String password = null;
        try {

            username = getUserName(request);
            password = getPassword(request);

        } catch (IllegalArgumentException e) {
            throw new InvalidAuthenticationRequestException(e);
        }

        Principal principal = null;
        
        try {
            principal = principalManager.getUserPrincipal(username);
        } catch (InvalidPrincipalException e) {
            throw new AuthenticationException("Invalid principal '" + username + "'", e);
        }

        if (cache != null) {
            Principal cachedPrincipal = (Principal) 
                this.cache.get(MD5.md5sum(principal.getQualifiedName() + password));
        
            if (cachedPrincipal != null) {
                if (logger.isDebugEnabled())
                    logger.debug("Found authenticated principal '" + username + "' in cache.");
                return cachedPrincipal;
            }
        }
        
        authenticateInternal(principal, password);
        
        if (cache != null)
            /* add to cache */
            cache.put(MD5.md5sum(principal.getQualifiedName() + password), principal);

        return principal;
    }
    

    public boolean postAuthentication(HttpServletRequest req, HttpServletResponse resp)
        throws AuthenticationProcessingException {
        return false;
    }
    

    public boolean isLogoutSupported() {
        return false;
    }


    public boolean logout(Principal principal, HttpServletRequest req,
                          HttpServletResponse resp)
        throws AuthenticationProcessingException {
        // FIXME: redirect user to page explaining how to exit the browser? 
        // Can't do nothing
        return false;
    }
    


    public abstract void authenticateInternal(Principal principal, String password)
        throws AuthenticationProcessingException, AuthenticationException;
    

    public AuthenticationChallenge getAuthenticationChallenge() {
        return challenge;
    }
    
    protected String getUserName(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new IllegalArgumentException(
                "No valid 'Authorization' header in request");
        } 

        String encodedString = authHeader.substring(
            "Basic ".length(), authHeader.length());
        String decodedString = Base64.decode(encodedString);
        int pos = decodedString.indexOf(":");

        if (pos == -1) {
            throw new IllegalArgumentException("Malformed 'Authorization' header");
        }

        String username = decodedString.substring(
            0, pos);
        return username;
    }


    protected String getPassword(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new IllegalArgumentException(
                "No valid 'Authorization' header in request");
        } 

        String encodedString = authHeader.substring(
            "Basic ".length(), authHeader.length());
        String decodedString = Base64.decode(encodedString);
        String password = decodedString.substring(
            decodedString.indexOf(":") + 1, decodedString.length());

        return password;
    }
}
