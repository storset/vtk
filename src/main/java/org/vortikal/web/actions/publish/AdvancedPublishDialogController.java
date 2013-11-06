/* Copyright (c) 2013 University of Oslo, Norway
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class AdvancedPublishDialogController extends SimpleFormController {

    private PropertyTypeDefinition publishDatePropDef;
    private PropertyTypeDefinition unpublishDatePropDef;

    protected Object formBackingObject(HttpServletRequest request) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Resource resource = repository.retrieve(token, uri, false);

        Service service = requestContext.getService();
        String submitURL = service.constructLink(resource, requestContext.getPrincipal());

        EditPublishingCommand command = new EditPublishingCommand(submitURL);
        command.setResource(resource);

        return command;
    }

    @SuppressWarnings("rawtypes")
    protected Map referenceData(HttpServletRequest request) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("formName", this.getCommandName());
        return model;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors) throws Exception {

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("command", command);

        EditPublishingCommand publishCommand = (EditPublishingCommand) command;
        if (publishCommand.getUpdateAction() != null) {

            Date publishDate = null;
            if (publishCommand.getPublishDateValue() != null) {
                publishDate = publishCommand.getPublishDateValue().getDateValue();
                setPropertyDateValue(publishDatePropDef, publishDate);
            } else {
                removePropertyValue(publishDatePropDef);
            }

            Date unpublishDate = null;
            if (publishCommand.getUnpublishDateValue() != null) {
                unpublishDate = publishCommand.getUnpublishDateValue().getDateValue();
                if (publishDate != null && unpublishDate.after(publishDate)) {
                    setPropertyDateValue(unpublishDatePropDef, unpublishDate);
                }
            } else {
                removePropertyValue(unpublishDatePropDef);
            }

        }
        return new ModelAndView(getSuccessView(), model);
    }

    public void removePropertyValue(PropertyTypeDefinition propDef) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = SecurityContext.getSecurityContext().getToken();
        Path uri = requestContext.getResourceURI();
        Resource resource = repository.retrieve(token, uri, true);
        if (resource.getProperty(propDef) != null) {
            resource.removeProperty(propDef);
            repository.store(token, resource);
        }
    }

    public void setPropertyDateValue(PropertyTypeDefinition datePropDef, Date date) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = SecurityContext.getSecurityContext().getToken();
        Path uri = requestContext.getResourceURI();
        Resource resource = repository.retrieve(token, uri, true);
        Property dateProp = resource.getProperty(datePropDef);
        if (dateProp == null) {
            dateProp = datePropDef.createProperty();
            resource.addProperty(dateProp);
        }
        dateProp.setDateValue(date);
        repository.store(token, resource);
    }

    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

    public void setUnpublishDatePropDef(PropertyTypeDefinition unpublishDatePropDef) {
        this.unpublishDatePropDef = unpublishDatePropDef;
    }

}
