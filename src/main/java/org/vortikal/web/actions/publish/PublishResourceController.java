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

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.Message;
import org.vortikal.web.RequestContext;

public class PublishResourceController implements Controller {

    protected Repository repository;
    private String viewName;
    private PropertyTypeDefinition publishDatePropDef;

    private static final String ACTION_PARAM = "action";
    private static final String PUBLISH_PARAM = "publish";
    private static final String UNPUBLISH_PARAM = "unpublish";


    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();

        String token = SecurityContext.getSecurityContext().getToken();
        Path resourceURI = RequestContext.getRequestContext().getResourceURI();
        Resource resource = repository.retrieve(token, resourceURI, true);

        String msgCode = "publish.permission.";

        String action = request.getParameter(ACTION_PARAM);
        if (PUBLISH_PARAM.equals(action)) {
            Property publishDateProp = resource.getProperty(this.publishDatePropDef);
            if (publishDateProp == null) {
                publishDateProp = resource.createProperty(this.publishDatePropDef);
            }
            publishDateProp.setDateValue(Calendar.getInstance().getTime());
            msgCode += "publish";
        } else if (UNPUBLISH_PARAM.equals(action)) {
            resource.removeProperty(this.publishDatePropDef);
            msgCode += "unpublish";
        }

        this.repository.store(token, resource);

        RequestContext.getRequestContext().addInfoMessage(new Message(msgCode));

        return new ModelAndView(this.viewName, model);
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setViewName(String viewName) {
        this.viewName = viewName;
    }


    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

}
