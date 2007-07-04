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
package org.vortikal.security.store;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalStore;


public class ChainedPrincipalStore implements InitializingBean, PrincipalStore {

    private Log logger = LogFactory.getLog(this.getClass());

    private List<PrincipalStore> managers = null;

    public ChainedPrincipalStore() {
    }

    public ChainedPrincipalStore(List<PrincipalStore> managers) {
        this.managers = managers;
    }

    public void setManagers(List<PrincipalStore> managers) {
        this.managers = managers;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.managers == null) {
            throw new BeanInitializationException(
                "Bean property 'managers' cannot be null");
        }
    }


    public boolean validatePrincipal(Principal principal)
        throws AuthenticationProcessingException {

        for (PrincipalStore manager: this.managers) {
            if (manager.validatePrincipal(principal)) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Validated principal '" + principal.getQualifiedName()
                                 + "' using manager " + manager);
                }
                return true;
            }
        }
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Principal '" + principal.getQualifiedName() + "' doesn't exist");
        }
        return false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(": managers = [").append(this.managers).append("]");
        return sb.toString();
    }
    
    public int getOrder() {
        // XXX: DUMMY - not used, but should be refactored
        return 0;
    }

}
