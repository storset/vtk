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
package org.vortikal.web.actions.create;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class CreateDocumentController extends SimpleFormController
  implements InitializingBean {

    private Repository repository = null;
    private DocumentTemplates documentTemplates;
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    } 

    public void afterPropertiesSet() throws Exception {
        if (this.documentTemplates == null) {
            throw new BeanInitializationException("Required bean property 'documentTemplates' not set.");
        }

    }
    
    protected Object formBackingObject(HttpServletRequest request)
        throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        Map<Path, String> topTemplates = this.documentTemplates.getTopTemplates();
        Map<String, Map<Path, String>> categories = this.documentTemplates.getCategoryTemplates();
        
        Resource resource = this.repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        String url = service.constructLink(resource, securityContext.getPrincipal());
         
        CreateDocumentCommand command =
            new CreateDocumentCommand(url);

        Map<Path, String> m = null;

        // Setting default value for CreateDocument dialog

        if (topTemplates != null) {
            m = topTemplates;
        } else if (categories != null) {
            Iterator<String> i = categories.keySet().iterator();
            
            if (i.hasNext()) m = categories.get(i.next());
        }

        if (m != null) {
            Iterator<Path> i = m.keySet().iterator(); 

            if (i.hasNext()) {
                command.setSourceURI(i.next().toString());
            }
        }
        
        return command;
    }


    @SuppressWarnings("unchecked")
    protected Map referenceData(HttpServletRequest request) throws Exception {
        Map<Path, String> topTemplates = this.documentTemplates.getTopTemplates();
        Map<String, Map<Path, String>> categories = this.documentTemplates.getCategoryTemplates();
        
        Map<String, Object> model = new HashMap<String, Object>();
        
        if (topTemplates != null) model.put("topTemplates", topTemplates);
        if (categories != null) model.put("categoryTemplates", categories);
        
        return model;
    }
    

    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        CreateDocumentCommand createDocumentCommand =
            (CreateDocumentCommand) command;
        if (createDocumentCommand.getCancelAction() != null) {
            createDocumentCommand.setDone(true);
            return;
        }
        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        Path sourceURI = Path.fromString(createDocumentCommand.getSourceURI());
        Path destinationURI = uri.extend(createDocumentCommand.getName());

        this.repository.copy(token, sourceURI, destinationURI, Depth.ZERO, false, false);
        createDocumentCommand.setDone(true);
    }
    

    /**
     * @param documentTemplates The documentTemplates to set.
     */
    public void setDocumentTemplates(DocumentTemplates documentTemplates) {
        this.documentTemplates = documentTemplates;
    }

}

