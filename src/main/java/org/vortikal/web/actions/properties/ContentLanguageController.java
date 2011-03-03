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

import java.util.Locale;

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
import org.vortikal.util.repository.LocaleHelper;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class ContentLanguageController extends SimpleFormController {
    private static Log logger = LogFactory.getLog(ContentLanguageController.class);
    private String[] possibleLanguages;
    
    public void setPossibleLanguages(String[] possibleLanguages) {
        this.possibleLanguages = possibleLanguages;
    }
	
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        Service service = requestContext.getService();
        Resource resource = repository.retrieve(requestContext.getSecurityToken(),
                                                requestContext.getResourceURI(), false);
        String url = service.constructLink(resource, requestContext.getPrincipal());
        String language = resource.getContentLanguage();
        ContentLanguageCommand command =
            new ContentLanguageCommand(language, this.possibleLanguages, url);
        return command;
    }


    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();

        ContentLanguageCommand contentLanguageCommand =
            (ContentLanguageCommand) command;

        if (contentLanguageCommand.getCancelAction() != null) {
            contentLanguageCommand.setDone(true);
            return;
        }
        
        Resource resource = repository.retrieve(token, uri, false);
        TypeInfo typeInfo = repository.getTypeInfo(token, uri);
        Locale locale = LocaleHelper.getLocale(contentLanguageCommand.getContentLanguage());
        
        if (locale == null) {
            resource.removeProperty(Namespace.DEFAULT_NAMESPACE, 
                    PropertyType.CONTENTLOCALE_PROP_NAME);
        } else {
            Property prop = typeInfo.createProperty(Namespace.DEFAULT_NAMESPACE, 
                    PropertyType.CONTENTLOCALE_PROP_NAME);
            prop.setStringValue(locale.toString());
            resource.addProperty(prop);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Setting new content language '" +
                         resource.getContentLanguage() + 
                         "' for resource " + uri);
        }
        repository.store(token, resource);
        contentLanguageCommand.setDone(true);
    }
}