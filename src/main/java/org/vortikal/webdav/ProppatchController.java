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
package org.vortikal.webdav;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.InheritablePropertiesStoreContext;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.TypeInfo;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.text.html.HtmlUtil;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.InvalidRequestException;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;
import org.vortikal.webdav.ifheader.IfHeaderImpl;

/**
 * Handler for PROPPATCH requests.
 *
 */
public class ProppatchController extends AbstractWebdavController  {

    private Service webdavService;
    
    @SuppressWarnings("deprecation")
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
         
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Principal principal = requestContext.getPrincipal();
        Path uri = requestContext.getResourceURI();
        Map<String, Object> model = new HashMap<String, Object>();

        try {
            Resource resource = repository.retrieve(token, uri, false);
            TypeInfo typeInfo = repository.getTypeInfo(token, uri);
            this.ifHeader = new IfHeaderImpl(request);
            verifyIfHeader(resource, true);
            
            /* Parse the request body XML: */
            Document requestBody = parseRequestBody(request);

            /* Make sure the request is valid: */
            validateRequestBody(requestBody);

            Document doc = doPropertyUpdate(resource, typeInfo, requestBody, principal);
            Format format = Format.getPrettyFormat();
            format.setEncoding("utf-8");

            /* Store the altered resource: */
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("storing modified Resource");
            }
            
            // Allow to store contentLocale with inheritable-store-context:
            InheritablePropertiesStoreContext sc = new InheritablePropertiesStoreContext();
            for (Property prop: resource) {
                PropertyTypeDefinition def = prop.getDefinition();
                // XXX Check of prop.isInherited() is not bullet proof yet, probably need to reset
                //     inherited-flag when a new value is set on a PropertyImpl.
                //     In this case we should be OK, since a new PropertyImpl is created.
                if (def.isInheritable() && PropertyType.CONTENTLOCALE_PROP_NAME.equals(def.getName()) && !prop.isInherited()) {
                    sc.addAffectedProperty(def);
                }
            }

            if (sc.getAffectedProperties().isEmpty()) {
                resource = repository.store(token, resource);
            } else {
                resource = repository.store(token, resource, sc);
            }

            XMLOutputter xmlOutputter = new XMLOutputter(format);
            String xml = xmlOutputter.outputString(doc);
            byte[] buffer = null;
            try {
                buffer = xml.getBytes("utf-8");
            } catch (UnsupportedEncodingException ex) {
                logger.warn("Warning: UTF-8 encoding not supported", ex);
                throw new RuntimeException("UTF-8 encoding not supported");
            }
            response.setHeader("Content-Type", "text/xml;charset=utf-8");
            response.setContentLength(buffer.length);
            response.setStatus(HttpUtil.SC_MULTI_STATUS,
                               WebdavUtil.getStatusMessage(
                                   HttpUtil.SC_MULTI_STATUS));
            OutputStream out = null;
            try {
                out = response.getOutputStream();
                out.write(buffer, 0, buffer.length);
                out.flush();
                out.close();

            } finally {
                if (out != null) {
                    out.flush();
                    out.close();
                }
            }
            return null;
            
        } catch (InvalidRequestException e) {
            this.logger.info("Invalid request on URI '" + uri + "'", e);
            writeDavErrorResponse(response, new Integer(HttpServletResponse.SC_BAD_REQUEST), e);
            return null;
            
        } catch (ResourceNotFoundException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught ResourceNotFoundException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_NOT_FOUND));

        } catch (ConstraintViolationException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught ConstraintViolationException for URI "
                             + uri, e);
            }
            writeDavErrorResponse(response, new Integer(HttpServletResponse.SC_FORBIDDEN), e);
            return null;
            
        } catch (IllegalOperationException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught IllegalOperationException for URI "
                             + uri, e);
            }
            writeDavErrorResponse(response, new Integer(HttpServletResponse.SC_FORBIDDEN), e);
            return null;

        } catch (ReadOnlyException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught ReadOnlyException for URI "
                             + uri);
            }
            writeDavErrorResponse(response, new Integer(HttpServletResponse.SC_FORBIDDEN), e);
            return null;

        } catch (ResourceLockedException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught ResourceLockedException for URI " + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpUtil.SC_LOCKED));
        }

        return new ModelAndView("PROPPATCH", model);
    }
    
    @SuppressWarnings("deprecation")
    private void writeDavErrorResponse(HttpServletResponse response, Integer status, Exception e) throws Exception { 
        Element error = new Element("error", WebdavConstants.DAV_NAMESPACE);
        Element errormsg = new Element("errormsg", WebdavConstants.DEFAULT_NAMESPACE);
        errormsg.setText(HtmlUtil.escapeHtmlString(e.getMessage()));
        error.addContent(errormsg);

        Document doc = new Document(error);
        Format format = Format.getPrettyFormat();
        format.setEncoding("utf-8");

        XMLOutputter xmlOutputter = new XMLOutputter(format);
        String xml = xmlOutputter.outputString(doc);
        byte[] buffer = null;
        try {
            buffer = xml.getBytes("utf-8");
        } catch (UnsupportedEncodingException ex) {
            logger.warn("Warning: UTF-8 encoding not supported", ex);
            throw new RuntimeException("UTF-8 encoding not supported");
        }
        response.setHeader("Content-Type", "text/xml;charset=utf-8");
        response.setContentLength(buffer.length);
        response.setStatus(status, WebdavUtil.getStatusMessage(status));
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            out.write(buffer, 0, buffer.length);
            out.flush();
            out.close();

        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }       
    }
    
    /**
     * Builds a JDOM tree from the XML request body.
     *
     * @param request the <code>HttpServletRequest</code> 
     * @return a <code>org.jdom.Document</code> tree
     * @exception InvalidRequestException if request does not contain
     * a valid XML request body
     * @exception IOException if an I/O error occurs
     */
    protected Document parseRequestBody(HttpServletRequest request)
        throws InvalidRequestException, IOException {
        SAXBuilder builder = new SAXBuilder();

        try {

            Document requestBody = builder.build(
                request.getInputStream());
            return requestBody;

        } catch (JDOMException e) {
            this.logger.info("Invalid XML in request body", e);
            throw new InvalidRequestException("Invalid XML in request body");
        }
    }

    /**
     * Verifies that a JDOM tree constitutes a valid PROPPATCH request
     * body.
     *
     * @param requestBody a <code>org.jdom.Document</code> tree
     * representing the WebDAV request body
     * @exception InvalidRequestException if the request body is not
     * valid
     */ 
    @SuppressWarnings("unchecked") 
    protected void validateRequestBody(Document requestBody)
        throws InvalidRequestException {

        Element root = requestBody.getRootElement();

        if (!"propertyupdate".equals( root.getName())) {

            throw new InvalidRequestException(
                "Invalid request element '" + root.getName()
                + "' (expected 'propertyupdate')");
        }      

        for (Iterator<Element> actionIterator = root.getChildren().iterator();
             actionIterator.hasNext();) {

            Element actionElement = (Element) actionIterator.next();
            String action = actionElement.getName();

            if (!("set".equals(action) || "remove".equals(action))) {
                throw new InvalidRequestException(
                    "invalid element '" + action + "' (expected "
                    + "'set' or 'remove')");
            }

            /* FIXME: check name and namespace of the children of the
             * 'set' or 'remove' element */
        }
    }
    
    /**
     * Performs setting or removing of properties on a resource 
     *
     * @param resource a <code>Resource</code> value
     * @param requestBody a <code>Document</code> value
     * @exception InvalidRequestException if an error occurs
     */
    @SuppressWarnings("rawtypes")
    protected Document doPropertyUpdate(Resource resource, TypeInfo typeInfo, Document requestBody, 
            Principal principal)
        throws ResourceNotFoundException, AuthorizationException,
        AuthenticationException, IllegalOperationException,
        InvalidRequestException {
        
        Element root = requestBody.getRootElement();

        Element multistatus = new Element("multistatus", WebdavConstants.DAV_NAMESPACE);
        Element response = new Element("response", WebdavConstants.DAV_NAMESPACE);
        Element href = new Element("href", WebdavConstants.DAV_NAMESPACE);
        URL url = this.webdavService.constructURL(resource, principal);
        href.setText(url.toString());

        
        Element propstat = new Element("propstat", WebdavConstants.DAV_NAMESPACE);
        multistatus.addContent(response);
        response.addContent(href);
        response.addContent(propstat);
        
        for (Iterator actionIterator = root.getChildren().iterator();
             actionIterator.hasNext();) {

            Element actionElement = (Element) actionIterator.next();

            String action = actionElement.getName();

            if (action.equals("set")) {
                setProperties(propstat, resource, typeInfo, 
                              actionElement.getChild(
                                  "prop", WebdavConstants.DAV_NAMESPACE).getChildren());
                
            } else if (action.equals("remove")) {
                removeProperties(propstat, resource,
                                 actionElement.getChild(
                                     "prop", WebdavConstants.DAV_NAMESPACE).getChildren());

            } else {
                throw new InvalidRequestException(
                    "invalid element '" + action + "' (expected "
                    + "'set' or 'remove')");
            }
        }
        return new Document(multistatus);
    }

    /**
     * Sets a list of properties on a resource .
     *
     * @param resource the <code>Resource</code> to modify
     * @param propElements a list of <code>org.jdom.Element</code>
     * objects representing DAV 'prop' elements (see RFC 2518,
     * sec. 12.11)
     */
    @SuppressWarnings("rawtypes")
    protected void setProperties(Element propstat,
                                 Resource resource,
                                 TypeInfo typeInfo,
                                 List propElements)
        throws ResourceNotFoundException, AuthorizationException,
        AuthenticationException, IllegalOperationException {

        Element resultPropElement = new Element("prop", WebdavConstants.DAV_NAMESPACE);
        for (Iterator elementIterator = propElements.iterator();
             elementIterator.hasNext();) {
            Element propElement = (Element) elementIterator.next();
            setProperty(resultPropElement, resource, typeInfo, propElement);
        }
        propstat.addContent(resultPropElement);

        Element statusElement = new Element("status", WebdavConstants.DAV_NAMESPACE);
        statusElement.setText("HTTP/" + WebdavConstants.HTTP_VERSION_USED + " 200 OK");
        propstat.addContent(statusElement);
    }
    
    /**
     * Sets a single property on a resource .
     *
     * @param resource the <code>Resource</code> to modify.
     * @param propertyElement an <code>org.jdom.Element</code> object
     * representing a DAV property element. This may be a standard DAV
     * property, or a custom one, although at present only standard
     * DAV properties are supported.
     */
    protected void setProperty(Element resultElement, Resource resource, TypeInfo typeInfo, Element propertyElement)
        throws ResourceNotFoundException, AuthorizationException,
        AuthenticationException, IllegalOperationException {

        String propertyName = propertyElement.getName();
        String nameSpace = propertyElement.getNamespace().getURI();

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Setting property with namespace: " + nameSpace);
        }

        if (nameSpace.toUpperCase().equals(WebdavConstants.DAV_NAMESPACE.getURI())) {

            if (propertyName.equals("displayname")) {
                throw new AuthorizationException("Setting property 'displayname' not permitted");
                
            } else if (propertyName.equals("getcontentlanguage")) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("setting property 'getcontentlanguage' to '"
                                 + propertyElement.getText() + "'");
                }
                Property prop = typeInfo.createProperty(Namespace.DEFAULT_NAMESPACE, 
                        PropertyType.CONTENTLOCALE_PROP_NAME);
                prop.setStringValue(propertyElement.getText());
                resource.addProperty(prop);
                
            } else if (propertyName.equals("getcontenttype")) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("setting property 'getcontenttype' to '"
                                 + propertyElement.getText() + "'");
                
                }
                Property prop = typeInfo.createProperty(Namespace.DEFAULT_NAMESPACE, 
                        PropertyType.CONTENTTYPE_PROP_NAME);
                prop.setStringValue(propertyElement.getText());
                resource.addProperty(prop);
                
            } else {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Unsupported property: " + propertyName);
                }
                //throw new AuthorizationException();
            }

        } else {

            Namespace ns;
            if (nameSpace.toUpperCase().equals(
                    WebdavConstants.DEFAULT_NAMESPACE.getURI().toUpperCase())) {
                ns = Namespace.DEFAULT_NAMESPACE;
            } else {
                ns = Namespace.getNamespace(nameSpace);
            }
 
            Property property = resource.getProperty(ns, propertyName);

            if (property == null) {
                /* Create a new property: */
                property = typeInfo.createProperty(ns, propertyName);
                resource.addProperty(property);
            }
            
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Setting property " + property + 
                             " on resource " + resource.getURI());
            }
            
            PropertyTypeDefinition def = property.getDefinition();
            
            if (def != null) {
                // Set value of controlled property
                try {
                    if (def.isMultiple()) {
                        property.setValues(elementToValues(propertyElement, def));
                    } else {
                        property.setValue(elementToValue(propertyElement, def));
                    }
                } catch (ValueFormatException e) {
                    this.logger.warn("Could not convert given value(s) for property " 
                            + property + " to the correct type: " + e.getMessage());
                    throw new IllegalOperationException("Could not convert given value(s) for property " 
                            + property + " to the correct type: " + e.getMessage());
                }
            } else {
                // Set string value of un-controlled property
                property.setStringValue(elementToString(propertyElement));
            }
        }
        Element resultPropertyElement = new Element(propertyName, propertyElement.getNamespace());
        resultElement.addContent(resultPropertyElement);
    }
    
    @SuppressWarnings("rawtypes")
    protected void removeProperties(Element propstat, Resource resource, 
            List propElements) {
        Element resultPropElement = new Element("prop", WebdavConstants.DAV_NAMESPACE);
        for (Iterator elementIterator = propElements.iterator();
             elementIterator.hasNext();) {
            Element theProperty = (Element) elementIterator.next();
            removeProperty(resultPropElement, resource, theProperty);
        }
        propstat.addContent(resultPropElement);

        Element statusElement = new Element("status", WebdavConstants.DAV_NAMESPACE);
        statusElement.setText("HTTP/" + WebdavConstants.HTTP_VERSION_USED + " 200 OK");
        propstat.addContent(statusElement);
    }
    

    protected void removeProperty(Element resultElement, Resource resource, Element propElement) {
        if (propElement.getNamespace().equals(WebdavConstants.DAV_NAMESPACE)) {
            return; 
        }
        String elementNamespaceURI = propElement.getNamespace().getURI();
        String propertyName = propElement.getName();
        Namespace propertyNamespace;
        if (elementNamespaceURI.toUpperCase().equals(
                WebdavConstants.DEFAULT_NAMESPACE.getURI().toUpperCase())) {
            propertyNamespace = Namespace.DEFAULT_NAMESPACE;
        } else {
            propertyNamespace = Namespace.getNamespace(elementNamespaceURI);
        }
        resource.removeProperty(propertyNamespace, propertyName);
        Element resultPropertyElement = new Element(propertyName, propElement.getNamespace());
        resultElement.addContent(resultPropertyElement);
    }
    
    /**
     * Builds a string representation of a property element.
     *
     * @param element a child element (property) of the "dav:prop" element.
     * @return a String representation of the property. If the element
     * has no child elements, the string returned is the value of the
     * element's text, otherwise the XML structure is preserved.
     */
    protected String elementToString(Element element) {
        try {
            if (element.getChildren().size() == 0) {
                /* Assume a "name = value" style property */
                return element.getText();
            }
            
            Format format = Format.getRawFormat();
            format.setOmitDeclaration(true);
            XMLOutputter xmlOutputter = new XMLOutputter(format);
            
            return xmlOutputter.outputString(element.getChildren());
        } catch (Exception e) {
            this.logger.warn("Error reading property value", e);
            return null;
        }
    }
    
    protected Value elementToValue(Element element, PropertyTypeDefinition def) throws ValueFormatException {
        String stringValue = element.getText();
        
        if (def.getType() == PropertyType.Type.TIMESTAMP || def.getType() == PropertyType.Type.DATE) {
            // Try to be liberal in accepting date formats:
            try {
                return new Value(WebdavUtil.parsePropertyDateValue(stringValue), def.getType() == PropertyType.Type.DATE);
            } catch (ParseException e) {
                try {
                    return def.getValueFormatter().stringToValue(stringValue, null, null);
                } catch (Exception vfe) {
                    throw new ValueFormatException(e);
                }
            }
        } 
        return def.getValueFormatter().stringToValue(stringValue, null, null);
    }
    
    @SuppressWarnings("unchecked") 
    protected Value[] elementToValues(Element element, PropertyTypeDefinition def) throws ValueFormatException {
        
        String[] stringValues;
        Element valuesElement;
        if ((valuesElement = element.getChild("values", 
                WebdavConstants.VORTIKAL_PROPERTYVALUES_XML_NAMESPACE))!= null) {
                
            List<Element> children = valuesElement.getChildren(
                "value", WebdavConstants.VORTIKAL_PROPERTYVALUES_XML_NAMESPACE);
            
            stringValues = new String[children.size()];
            int u=0;
            for (Element e: children) {
                stringValues[u++] = e.getText();
            }
        } else if (element.getChildren().size() == 0) {
            // Assume values separated by comma (CSV)
            stringValues = element.getText().split(",");
        } else {
            throw new ValueFormatException("Invalid multi-value syntax.");
        }

        if (stringValues.length == 0) {
            throw new ValueFormatException("Empty value lists are not supported.");
        }
    
        Value[] values;
        if (def.getType() == PropertyType.Type.TIMESTAMP || def.getType() == PropertyType.Type.DATE) {
            values = new Value[stringValues.length];
            try {
                for (int i=0; i<values.length; i++) {
                    values[i] = new Value(WebdavUtil.parsePropertyDateValue(stringValues[i]), def.getType() == PropertyType.Type.DATE);
                }
            } catch (ParseException e) {
                throw new ValueFormatException(e.getMessage());
            }
        } else {
            values = new Value[stringValues.length];
            for (int i=0; i<values.length; i++) {
            	values[i] = def.getValueFormatter().stringToValue(stringValues[i], null, null);	
            }
        }
        
        return values;
    }

    @Required
    public void setWebdavService(Service webdavService) {
        this.webdavService = webdavService;
    }
}
