/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.commenting;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.mvc.SimpleFormController;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;



public class PostCommentController extends SimpleFormController {

    private Repository repository = null;
    private String formSessionAttributeName;
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public PostCommentController() {
        setSessionForm(true);
    }
    
    public void setFormSessionAttributeName(String formSessionAttributeName) {
        this.formSessionAttributeName = formSessionAttributeName;
    }
    

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        Resource resource = this.repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        URL url = service.constructURL(resource, securityContext.getPrincipal());
        PostCommentCommand command = new PostCommentCommand(url);
        return command;
    }


    protected void onBindAndValidate(HttpServletRequest request, Object command,
                          BindException errors) throws Exception {
        if (this.formSessionAttributeName == null) {
            return;
        }
        if (errors.hasErrors()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("form", command);
            map.put("errors", errors);
            request.getSession(true).setAttribute(this.formSessionAttributeName, map);
        } else {
            if (request.getSession(false) != null) {
                request.getSession().removeAttribute(this.formSessionAttributeName);
            }
        }
    }
    

    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        PostCommentCommand commentCommand = (PostCommentCommand) command;
        if (commentCommand.getCancelAction() != null) {
            commentCommand.setDone(true);
            return;
        }
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        Resource resource = this.repository.retrieve(token, uri, true);
        repository.addComment(token, resource, commentCommand.getTitle(),
                              commentCommand.getText());
    }
}



