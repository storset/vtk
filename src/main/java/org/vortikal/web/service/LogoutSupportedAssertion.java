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
package org.vortikal.web.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.security.token.TokenManager;
import org.vortikal.security.web.AuthenticationHandler;


public class LogoutSupportedAssertion extends AbstractRepositoryAssertion implements InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());
    

    private TokenManager tokenManager = null;


    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }
    
    public void afterPropertiesSet() {
        if (this.tokenManager == null) {
            throw new BeanInitializationException(
                "Bean property 'tokenManager' must be set");
        }
    }
    

    public boolean matches(Resource resource, Principal principal) {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();

        if (token == null) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("No authentication token present, match = false");
            }
            return false;
        }

        AuthenticationHandler handler = this.tokenManager.getAuthenticationHandler(token);

        if (handler == null) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("No authentication handler for token "
                             + token + ", match = false");
            }
            return false;
        }

        if (!handler.isLogoutSupported()) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Authentication handler " + handler
                             + " does not support logout, match = false");
            }
            return false;
        }

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Authentication handler " + handler
                         + " supports logout, match = true");
        }
        
        return true;
    }


    public boolean conflicts(Assertion assertion) {
        return false;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        return sb.toString();
    }

}
