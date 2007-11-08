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
package org.vortikal.edit.editor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;

public class ResourceEditController extends SimpleFormController {

    private Repository repository;
    private List<Service> tooltipServices;
    private List<PropertyTypeDefinition> propDefs;
    
    public ResourceEditController() {
        super();
        setValidator(new ResourceCommandValidator());
    }

    
    
    @Override
    protected ModelAndView onSubmit(Object command) throws Exception {
        if (!((ResourceCommand) command).getErrors().isEmpty()) {
            Map model = new HashMap();
            model.put(getCommandName(), command);
            return new ModelAndView(getFormView(),model);
        }
        return super.onSubmit(command);
    }



    @Override
    protected ServletRequestDataBinder createBinder(HttpServletRequest request, Object command)
    throws Exception {
         ServletRequestDataBinder binder = new ResourceCommandDataBinder(command, getCommandName());
         prepareBinder(binder);
         initBinder(request, binder);
         return binder;
    }
        
    @Override
    protected Object formBackingObject(HttpServletRequest request)
            throws Exception {

        String token = SecurityContext.getSecurityContext().getToken();
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        String uri = RequestContext.getRequestContext().getResourceURI();
        
        Resource resource = this.repository.retrieve(token, uri, false);
        InputStream is = this.repository.getInputStream(token, uri, false);
        
        byte[] bytes = StreamUtil.readInputStream(is);
        
        String content = new String(bytes, resource.getCharacterEncoding());

        ResourceCommand command = new ResourceCommand();

        command.setContent(content);

        command.setTooltips(resolveTooltips(resource, principal));

        command.setPropsMap(getPropsMap(resource));
        return command;
    }

    @Override
    protected void doSubmitAction(Object command) throws Exception {
        String token = SecurityContext.getSecurityContext().getToken();
        String uri = RequestContext.getRequestContext().getResourceURI();
        Resource resource = this.repository.retrieve(token, uri, false);

        ResourceCommand c = (ResourceCommand) command;

        byte[] bytes = c.getContent().getBytes(resource.getCharacterEncoding());
        resource = this.repository.storeContent(token, uri, new ByteArrayInputStream(bytes));

        
        Map<PropertyTypeDefinition, String> propsMap = getPropsMap(resource);
        
        if (propsMap == null) { 
            return;
        }

        boolean propChange = false;

        for (PropertyTypeDefinition propDef : c.getPropsMap().keySet()) {
            String originalValue = propsMap.get(propDef);
            String newValue = c.getPropsMap().get(propDef);
            if (originalValue.equals(newValue)) {
                continue;
            }
            propChange = true;
            
            Property prop = resource.getProperty(propDef);
            if (prop == null) {
                prop = resource.createProperty(propDef.getNamespace(), propDef.getName());
            } 
            
            setPropValue(newValue, prop);
        }

        if (propChange) {
            this.repository.store(token, resource);
        }
    }


    private void setPropValue(String valueString, Property prop) {
        PropertyTypeDefinition propDef = prop.getDefinition();

        if (propDef.isMultiple()) {
            String[] strings = valueString.split(",");
            Value[] values = new Value[strings.length];

            int i = 0;
            for (String string : strings) {
                values[i++] = propDef.getValueFormatter().stringToValue(string, null, null);
            }
            prop.setValues(values);
        } else {
            Value value = propDef.getValueFormatter().stringToValue(valueString, null, null);
            prop.setValue(value);
        }
    }


    private Map<PropertyTypeDefinition, String> getPropsMap(Resource resource) {
        if (this.propDefs == null) { return null;}
        Map<PropertyTypeDefinition, String> prodDefsMap = 
            new HashMap<PropertyTypeDefinition, String>();

        for (PropertyTypeDefinition propDef: this.propDefs) {
            String formattedValue = "unset";

            Property property = resource.getProperty(propDef);
            if (property != null) {
                formattedValue = property.getFormattedValue(null, null);
            }
            prodDefsMap.put(propDef, formattedValue);
        }
        return prodDefsMap;
    }

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setTooltipServices(List<Service> tooltipServices) {
        this.tooltipServices = tooltipServices;
    }

    private List<Map<String, String>> resolveTooltips(Resource resource, Principal principal) {
        if (this.tooltipServices == null) {
            return null;
        }
        List<Map<String, String>> tooltips = new ArrayList<Map<String, String>>();
        for (Service service: this.tooltipServices) {
            String url = null;
            try {
                url = service.constructLink(resource, principal);
                Map<String, String> tooltip = new HashMap<String, String>();
                tooltip.put("url", url);
                tooltip.put("messageKey", "plaintextEdit.tooltip." + service.getName());
                tooltips.add(tooltip);
            } catch (ServiceUnlinkableException e) {
                // Ignore
            }
        }
        return tooltips;
    }


    public void setPropDefs(List<PropertyTypeDefinition> propDefs) {
        this.propDefs = propDefs;
    }


}
