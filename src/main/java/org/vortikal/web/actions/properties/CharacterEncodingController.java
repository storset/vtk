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
package org.vortikal.web.actions.properties;


import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.TypeInfo;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


/**
 * Controller for setting the <code>characterEncoding</code> property
 * on resources.
 */
public class CharacterEncodingController extends SimpleFormController {

    private static Log logger = LogFactory.getLog(CharacterEncodingController.class);
    
    private Repository repository = null;
    
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        Resource resource = this.repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        String url = service.constructLink(resource, securityContext.getPrincipal());
         
        CharacterEncodingCommand command =
            new CharacterEncodingCommand(resource.getCharacterEncoding(), url);
        return command;
    }


    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        CharacterEncodingCommand encodingCommand =
            (CharacterEncodingCommand) command;

        if (encodingCommand.getCancelAction() != null) {
            encodingCommand.setDone(true);
            return;
        }
        
        Resource resource = this.repository.retrieve(token, uri, false);
        TypeInfo typeInfo = this.repository.getTypeInfo(token, uri);
        
        if (!ContentTypeHelper.isTextContentType(resource.getContentType())) {
            encodingCommand.setDone(true);
            return;
        }

        if (encodingCommand.getCharacterEncoding() == null ||
            "".equals(encodingCommand.getCharacterEncoding().trim())) {
            resource.removeProperty(Namespace.DEFAULT_NAMESPACE, 
                    PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME);
        } else {
            Property prop = typeInfo.createProperty(Namespace.DEFAULT_NAMESPACE, 
                    PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME);
            // XXX: Needs check for understandable encoding
            prop.setStringValue(encodingCommand.getCharacterEncoding().trim());
            resource.addProperty(prop);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Setting new character encoding '" +
                         resource.getCharacterEncoding() + 
                         "' for resource " + uri);
        }
        this.repository.store(token, resource);

        encodingCommand.setDone(true);
    }
    

}

