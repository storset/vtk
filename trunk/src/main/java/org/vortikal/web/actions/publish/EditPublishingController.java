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
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class EditPublishingController extends SimpleFormController {

    private PropertyTypeDefinition publishDatePropDef;
    private PropertyTypeDefinition unpublishDatePropDef;

    @Override
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

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors) throws Exception {

        EditPublishingCommand editPublishingCommand = (EditPublishingCommand) command;

        if (editPublishingCommand.getCancelAction() != null) {
            return new ModelAndView(getSuccessView());
        }

        Resource resource = editPublishingCommand.getResource();

        if (editPublishingCommand.getPublishDateUpdateAction() != null) {
            storeResource(resource, editPublishingCommand.getPublishDateValue(), this.publishDatePropDef);
        } else if (editPublishingCommand.getUnpublishDateUpdateAction() != null) {
            storeResource(resource, editPublishingCommand.getUnpublishDateValue(), this.unpublishDatePropDef);
        }

        return new ModelAndView(getSuccessView());
    }

    private void storeResource(Resource resource, Value dateValue, PropertyTypeDefinition propTypeDef) throws Exception {
        if (dateValue != null) {
            Property dateProp = resource.getProperty(propTypeDef);
            if (dateProp == null) {
                dateProp = propTypeDef.createProperty(dateValue.getDateValue());
            } else {
                dateProp.setDateValue(dateValue.getDateValue());
            }
            resource.addProperty(dateProp);
        } else {
            resource.removeProperty(propTypeDef);
        }
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        repository.store(token, resource);
    }

    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

    public void setUnpublishDatePropDef(PropertyTypeDefinition unpublishDatePropDef) {
        this.unpublishDatePropDef = unpublishDatePropDef;
    }

}
