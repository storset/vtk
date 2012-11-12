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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.web.Message;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class PublishResourceController extends SimpleFormController implements InitializingBean {

    private String viewName;
    private PropertyTypeDefinition publishDatePropDef;
    private static final String ACTION_PARAM = "action";
    private static final String PUBLISH_PARAM = "publish-confirmed";
    private static final String PUBLISH_PARAM_GLOBAL = "global-publish-confirmed";
    private static final String UNPUBLISH_PARAM = "unpublish-confirmed";
    private static final String UNPUBLISH_PARAM_GLOBAL = "global-unpublish-confirmed";

    public void afterPropertiesSet() throws Exception {
        if (this.viewName == null)
            throw new BeanInitializationException("Property 'viewName' must be set");
    }

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

    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path resourceURI = RequestContext.getRequestContext().getResourceURI();
        Resource resource = repository.retrieve(token, resourceURI, true);

        PublishResourceCommand publishResourceCommand = (PublishResourceCommand) command;

        String action = request.getParameter(ACTION_PARAM);

        if (publishResourceCommand.getPublishResourceAction() != null) {
            String msgCode = "publish.permission.";

            if (PUBLISH_PARAM.equals(action) || PUBLISH_PARAM_GLOBAL.equals(action)) {
                Property publishDateProp = resource.getProperty(this.publishDatePropDef);
                if (publishDateProp == null) {
                    publishDateProp = this.publishDatePropDef.createProperty();
                    resource.addProperty(publishDateProp);
                }
                publishDateProp.setDateValue(Calendar.getInstance().getTime());
                msgCode += "publish";
            } else if (UNPUBLISH_PARAM.equals(action) || UNPUBLISH_PARAM_GLOBAL.equals(action)) {
                resource.removeProperty(this.publishDatePropDef);
                msgCode += "unpublish";
            }
            repository.store(token, resource);
            RequestContext.getRequestContext().addInfoMessage(new Message(msgCode));
        }
        return new ModelAndView(this.viewName, model);
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

}
