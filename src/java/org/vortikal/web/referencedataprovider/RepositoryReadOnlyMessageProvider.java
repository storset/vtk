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
package org.vortikal.web.referencedataprovider;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;

/**
 * A reference data provider that puts a message in the model if the
 * content repository operates in read-only mode.
 *
 * <p>Configurable properties:
 * <ul>
 *  <li><code>repository</code> - the content {@link Repository} repository
 *  <li><code>modelName</code> - the name to use for the submodel
 *  <li><code>messageKey</code> - the key to use when looking up the
 *  localized message. If no localized version of the string exists,
 *  the key itself is used as the message.
 * </ul>
 * 
 * <p>Model data provided:
 * <ul>
 *   <li>an entry of the name configured in the <code>modelName</code>
 *   bean property containing a localized {@link String} containing
 *   the message. If the repository is not in read-only mode, this
 *   message is <code>null</code>.
 * </ul>
 * 
 */
public class RepositoryReadOnlyMessageProvider
  implements Provider, InitializingBean {

    private Repository repository = null;
    private String modelName = null;
    private String messageKey = null;


    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }
    

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
        if (this.modelName == null) {
            throw new BeanInitializationException(
                "Bean property 'modelName' must be set");
        }
        if (this.messageKey == null) {
            throw new BeanInitializationException(
                "Bean property 'messageKey' must be set");
        }
    }
    


    public void referenceData(Map model, HttpServletRequest request)
            throws Exception {
        String message = null;

        if (this.repository.getConfiguration().isReadOnly()) {
            org.springframework.web.servlet.support.RequestContext springContext =
                new org.springframework.web.servlet.support.RequestContext(request);
            message = springContext.getMessage(this.messageKey, this.messageKey);
        }
        model.put(this.modelName, message);
    }
}
