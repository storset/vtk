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
package org.vortikal.web.actions.copymove;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class RenameController extends SimpleFormController {

    private String confirmView;
    private static Log logger = LogFactory.getLog(RenameController.class);
    private Repository repository = null;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();

        Resource resource = this.repository
                .retrieve(securityContext.getToken(), requestContext.getResourceURI(), false);
        String url = service.constructLink(resource, securityContext.getPrincipal());

        RenameCommand command = new RenameCommand(resource.getName(), url);
        return command;
    }

    protected ModelAndView onSubmit(Object command, BindException errors) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();

        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        RenameCommand rename = (RenameCommand) command;

        if (rename.getCancel() != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not setting new name for resource " + uri);
            }
            rename.setDone(true);
            return new ModelAndView(getSuccessView());
        }

        Resource resource = this.repository.retrieve(token, uri, false);
        String name = resource.getName();

        Map<String, Object> model = null;

        boolean overwrite = false;

        try {
            Path newUri = uri.getParent().extend(rename.getName());

            if (repository.exists(token, newUri)) {
                model = errors.getModel();
                Resource resource2 = repository.retrieve(token, newUri, false);
                if (rename.getOverwrite() == null) {
                    if (resource.isCollection() || resource2.isCollection()) {
                        errors.rejectValue("name", "manage.rename.resource.exists");
                    } else {
                        errors.rejectValue("name", "manage.rename.resource.overwrite",
                                "A resource of this name already exists, do you want to overwrite it?");
                        model.put("confirm", true);
                    }
                    return new ModelAndView(getFormView(), model);
                } else {
                    if (!(resource.isCollection() || resource2.isCollection())) {
                        overwrite = true;
                    }
                }
            }

            if (!name.equals(rename.getName())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting new name '" + rename.getName() + "' for resource " + uri);
                }

                this.repository.move(token, uri, newUri, overwrite);
                Resource newResource = this.repository.retrieve(token, newUri, false);
                model = new HashMap<String, Object>();
                model.put("resource", newResource);
            }
            rename.setDone(true);
            return new ModelAndView(getSuccessView(), model);

        } catch (IllegalOperationException e) {
            errors.rejectValue("name", "manage.rename.invalid.name", "The name is not valid for this resource");
            return new ModelAndView(getFormView(), errors.getModel());
        }
    }

    public void setConfirmView(String confirmView) {
        this.confirmView = confirmView;
    }

    public String getConfirmView() {
        return confirmView;
    }
}
