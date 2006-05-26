/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.web.controller.properties;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.mvc.SimpleFormController;

import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;



public class PropertyEditController extends SimpleFormController
  implements ReferenceDataProvider, ReferenceDataProviding, InitializingBean {

    private Repository repository;
    private PrincipalManager principalManager;
    private PropertyTypeDefinition[] propertyTypeDefinitions;
    private ValueFactory valueFactory;
    private String dateFormat;
    
    private String modelKey;
  
    private Service service;
    
    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setPropertyTypeDefinitions(PropertyTypeDefinition[] propertyTypeDefinitions) {
        this.propertyTypeDefinitions = propertyTypeDefinitions;
    }
    
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }
    
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public ReferenceDataProvider[] getReferenceDataProviders() {
        return new ReferenceDataProvider[] {this};
    }
    

    public void afterPropertiesSet() {
        if (this.modelKey == null) {
            throw new BeanInitializationException(
            "JavaBean property 'modelKey' not set");
        }
        
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' not set");
        }
        if (this.propertyTypeDefinitions == null) {
            throw new BeanInitializationException(
                "JavaBean property 'propertyTypeDefinitions' not set");
        }
        if (this.dateFormat == null) {
            throw new BeanInitializationException(
                "JavaBean property 'dateFormat' not set");
        }
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'principalManager' not set");
        }
        setValidator(new PropertyEditValidator(this.valueFactory,
                                               this.principalManager));
    }
    


    protected Object formBackingObject(HttpServletRequest request)
        throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        Resource resource = this.repository.retrieve(securityContext.getToken(),
                                                     requestContext.getResourceURI(), false);
        String value = null;
        String[] formAllowedValues = null;
        String editURL = null;
        PropertyTypeDefinition definition = null;


        for (int i = 0; i < this.propertyTypeDefinitions.length; i++) {

            if (isFocusedProperty(this.propertyTypeDefinitions[i], request)) {

                definition = this.propertyTypeDefinitions[i];

                Property property = resource.getProperty(
                    definition.getNamespace(), definition.getName());

                if (property != null) {
                    if (definition.isMultiple()) {
                        StringBuffer val = new StringBuffer();
                        Value[] values = property.getValues();
                        for (int j = 0; j < values.length; j++) {
                            val.append(values[j].toString());
                            if (j < values.length - 1) val.append(", ");
                        }
                        value = val.toString();
                    } else {
                        value = getValueForPropertyAsString(property);
                    }
                }

                Value[] definitionAllowedValues = definition.getAllowedValues();

                if (definitionAllowedValues != null) {
                    int startIdx = 0;
                    if (!definition.isMandatory()) {
                        // Allow "" value (remove property)
                        formAllowedValues = new String[definitionAllowedValues.length + 1];
                        formAllowedValues[0] = "";
                        startIdx = 1;
                    } else {
                        formAllowedValues = new String[definitionAllowedValues.length];
                    }

                    for (int j = startIdx; j < definitionAllowedValues.length; j++) {
                        formAllowedValues[j] = definitionAllowedValues[j].toString();
                    }
                }
                Map urlParameters = new HashMap();
                String namespaceURI = definition.getNamespace().getUri();
                if (namespaceURI != null) {
                    urlParameters.put("namespace", namespaceURI);
                }
                urlParameters.put("name", definition.getName());
                editURL = service.constructLink(resource, securityContext.getPrincipal(),
                                                urlParameters);
            }
        }
        
        PropertyEditCommand propertyEditCommand = new PropertyEditCommand(
            editURL, definition, value, formAllowedValues);
        return propertyEditCommand;
    }

    private String getValueForPropertyAsString(Property property)
        throws IllegalOperationException {
        String value;
        int type = property.getDefinition().getType();
        switch (type) {

        case PropertyType.TYPE_DATE:
            SimpleDateFormat format = new SimpleDateFormat(dateFormat);
            Date date = property.getDateValue();
            value = format.format(date);
            break;

        default:
            value = property.getValue().toString();
            
        }
        return value;
    }

    protected boolean isFormSubmission(HttpServletRequest request) {
        boolean isFormSubmission = super.isFormSubmission(request);
        if ("toggle".equals(request.getParameter("action"))) {
            return true;
        }
        return isFormSubmission;
    }
    
 
    protected void doSubmitAction(Object command) throws Exception {    
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        String token = securityContext.getToken();

        PropertyEditCommand propertyCommand =
            (PropertyEditCommand) command;

        if (propertyCommand.getCancelAction() != null) {
            propertyCommand.clear();
            propertyCommand.setDone(true);
            return;
        }
        String uri = requestContext.getResourceURI();
        Resource resource = this.repository.retrieve(token, uri, false);
        for (int i = 0; i < this.propertyTypeDefinitions.length; i++) {
            
            PropertyTypeDefinition def = this.propertyTypeDefinitions[i];
            if (isFocusedProperty(def, propertyCommand.getNamespace(),
                                  propertyCommand.getName())) {
                Property property = resource.getProperty(def.getNamespace(), def.getName());

                String stringValue = propertyCommand.getValue();
                if ("".equals(stringValue)) {
                    if (property == null) {
                        propertyCommand.setDone(true);
                        propertyCommand.clear();
                        return;
                    }
                    resource.removeProperty(def.getNamespace(), def.getName());
                } else {
                    if (property == null) {
                        property = resource.createProperty(def.getNamespace(), def.getName());
                    }

                    if (def.isMultiple()) {
                        String[] splitValues = stringValue.split(",");
                        Value[] values = this.valueFactory.createValues(
                            splitValues, def.getType());
                        property.setValues(values);
                    } else {
                        if (def.getType() == PropertyType.TYPE_BOOLEAN) {
                            boolean oldValue = property.getBooleanValue();
                            property.setBooleanValue(!oldValue);
                        } else {
                        Value value = this.valueFactory.createValue(
                            stringValue, def.getType());
                        property.setValue(value);
                        }
                    }
                }
                this.repository.store(token, resource);
                break;
            }
        }

        propertyCommand.clear();
        propertyCommand.setDone(true);
    }
    

    public void referenceData(Map model, HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = this.service;
        if (service == null) service = requestContext.getService();
        
        Resource resource = this.repository.retrieve(securityContext.getToken(),
                                                     requestContext.getResourceURI(), false);

        List propsList = new ArrayList();
        for (int i = 0; i < this.propertyTypeDefinitions.length; i++) {

            PropertyTypeDefinition def = this.propertyTypeDefinitions[i];

            Property property = resource.getProperty(def.getNamespace(), def.getName());

            String editURL = null;

            if (resource.isAuthorized(def.getProtectionLevel(),
                                      securityContext.getPrincipal())) {
                
                Map urlParameters = new HashMap();
                String namespaceURI = def.getNamespace().getUri();
                if (namespaceURI != null) {
                    urlParameters.put("namespace", namespaceURI);
                }
                urlParameters.put("name", def.getName());
                if (def.getType() == PropertyType.TYPE_BOOLEAN) {
                    urlParameters.put("action", "toggle");
                } else {
                }
                try {
                    editURL = service.constructLink(resource, securityContext.getPrincipal(),
                            urlParameters);
                } catch (ServiceUnlinkableException e) {
                    // Assertion doesn't match, OK in this case
                }
            }

            PropertyItem item = new PropertyItem(property, def, editURL);
            propsList.add(item);
        }

        model.put(modelKey, propsList);
    }
    

    private boolean isFocusedProperty(PropertyTypeDefinition propDef,
                                      HttpServletRequest request) {
        String inputNamespace = request.getParameter("namespace");
        String inputName = request.getParameter("name");
        return isFocusedProperty(propDef, inputNamespace, inputName);
    }
    
    private boolean isFocusedProperty(PropertyTypeDefinition propDef,
                                      String inputNamespace, String inputName) {

        if (inputNamespace != null) inputNamespace = inputNamespace.trim();
        if (inputName != null) inputName = inputName.trim();
        
        if (inputName == null || "".equals(inputName)) {
            return false;
        }

        if (!inputName.equals(propDef.getName())) {
            return false;
        }
        
        // We now know it is the same name, check namespace:

        if (propDef.getNamespace().getUri() == null
            && (inputNamespace == null || "".equals(inputNamespace))) {
            return true;
        }

        return propDef.getNamespace().getUri().equals(inputNamespace);
    }

    public void setService(Service service) {
        this.service = service;
    }

}
