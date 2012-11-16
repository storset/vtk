/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.web.actions.publish;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.actions.ActionsHelper;
import org.vortikal.web.service.Service;

public class PublishResourceController extends SimpleFormController implements InitializingBean {

    private String viewName;
    private PropertyTypeDefinition publishDatePropDef;
    private static final String ACTION_PARAM = "action";
    private static final String PUBLISH_PARAM = "publish-confirmed";
    private static final String PUBLISH_PARAM_GLOBAL = "global-publish-confirmed";
    private static final String UNPUBLISH_PARAM = "unpublish-confirmed";
    private static final String UNPUBLISH_PARAM_GLOBAL = "global-unpublish-confirmed";

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.viewName == null)
            throw new BeanInitializationException("Property 'viewName' must be set");
    }

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        Service service = requestContext.getService();

        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Principal principal = requestContext.getPrincipal();

        Resource resource = repository.retrieve(token, uri, false);
        String url = service.constructLink(resource, principal);

        return new PublishResourceCommand(url);
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path resourceURI = RequestContext.getRequestContext().getResourceURI();

        PublishResourceCommand publishResourceCommand = (PublishResourceCommand) command;

        String action = request.getParameter(ACTION_PARAM);
        
        // Map of files that for some reason failed on publish. Separated by a
        // key (String) that specifies type of failure and identifies list of
        // paths to resources that failed.
        Map<String, List<Path>> failures = new HashMap<String, List<Path>>();
        
        if (publishResourceCommand.getPublishResourceAction() != null) {
            if (PUBLISH_PARAM.equals(action) || PUBLISH_PARAM_GLOBAL.equals(action)) {
                ActionsHelper.publishResource(publishDatePropDef, Calendar.getInstance().getTime(), repository, token, resourceURI, failures);
            } else if (UNPUBLISH_PARAM.equals(action) || UNPUBLISH_PARAM_GLOBAL.equals(action)) {
                ActionsHelper.unpublishResource(publishDatePropDef, repository, token, resourceURI, failures);
            }
        }
        ActionsHelper.addFailureMessages(failures, requestContext);
        
        return new ModelAndView(this.viewName, model);
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    @Required
    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

}
