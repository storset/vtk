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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vortikal.security.AuthenticationException;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.util.cache.SimpleCache;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;



public abstract class AbstractHttpBasicAuthenticationHandler
  implements AuthenticationHandler, InitializingBean {

    protected Log logger = LogFactory.getLog(this.getClass());
    private SimpleCache cache;
    
    private PrincipalManager principalManager = null;
    private HttpBasicAuthenticationChallenge challenge = null;
    
    public void setChallenge(HttpBasicAuthenticationChallenge challenge)  {
        this.challenge = challenge;
    }
    

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }


    public void afterPropertiesSet() {
        if (this.challenge == null) {
            throw new BeanInitializationException(
                "Bean property 'challenge' not set. Must be set to an instance of " +
                HttpBasicAuthenticationChallenge.class.getName());
        }
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                "Bean property 'principalManager' not set. Must be set to an instance of " +
                PrincipalManager.class.getName());
        }
    }
    

    public AuthenticationChallenge getAuthenticationChallenge() {
        return this.challenge;
    }


    public boolean isRecognizedAuthenticationRequest(HttpServletRequest req)
        throws AuthenticationProcessingException {
        String authHeader = req.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Basic ")) {
            // FIXME: more checking
            return true;
        }
        return false;
    }
    

    
    public Principal authenticate(HttpServletRequest request)
        throws AuthenticationProcessingException, AuthenticationException {
        String username = getUserName(request);
        String password = getPassword(request);

        if (username == null || username.trim().equals("")) {
            throw new AuthenticationException();
        } 
        username = username.trim();
        
        if (cache != null) {
            /* Simple cache to allow for clients that don't send cookies */
            String md5sum = md5sum(username + password);
        
            Principal principal = (Principal) cache.get(md5sum);
        
            if (principal != null) {
                if (logger.isDebugEnabled())
                    logger.debug("Found authenticated principal '" + username + "' in cache.");
                return principal;
            }
        }
        
        authenticateInternal(username, password);
        
        //Principal principal = new PrincipalImpl(username);
        Principal principal = principalManager.getPrincipal(username);

        if (cache != null)
            /* No exception means the user authenticated; add to cache */
            cache.put(md5sum(username + password), principal);

        return principal;
    }
    

    public boolean postAuthentication(HttpServletRequest req, HttpServletResponse resp)
        throws AuthenticationProcessingException {
        return false;
    }
    

    public void logout(Principal principal) throws AuthenticationProcessingException {
        
    }
    


    public abstract void authenticateInternal(String username, String password)
        throws AuthenticationProcessingException, AuthenticationException;
    
    

    protected String getUserName(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new IllegalArgumentException(
                "No valid 'Authorization' header in request");
        } 

        String encodedString = authHeader.substring(
            "Basic ".length(), authHeader.length());
        String decodedString = base64decode(encodedString);
        String username = decodedString.substring(
            0, decodedString.indexOf(":"));
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
        String decodedString = base64decode(encodedString);
        String password = decodedString.substring(
            decodedString.indexOf(":") + 1, decodedString.length());

        return password;
    }
    

    protected String base64encode(String str) {
        Base64 encoder = new Base64();
        return new String(encoder.encode(str.getBytes()));
    }
   
   
    protected String base64decode(String str) {
        Base64 decoder = new Base64();
        return new String(decoder.decode(str.getBytes()));
    }


    protected String md5sum(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(str.getBytes());
            StringBuffer result = new StringBuffer(2 * digest.length);
            for (int i = 0; i < digest.length; ++i) {
                int k = digest[i] & 0xFF;
                if (k < 0x10) result.append('0');
                result.append(Integer.toHexString(k));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "MD5 digest not available in JVM");
        }
    }

    /**
     * @param cache The cache to set.
     */
    public void setCache(SimpleCache cache) {
        this.cache = cache;
    }
}
