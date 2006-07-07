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
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.LocaleHelper;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.webdav.ifheader.IfHeaderImpl;

/**
 * Handler for PROPPATCH requests.
 *
 */
public class ProppatchController extends AbstractWebdavController  {

    private ValueFactory valueFactory;
    
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {
         
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        Map model = new HashMap();

        try {
            Resource resource = this.repository.retrieve(token, uri, false);
            this.ifHeader = new IfHeaderImpl(request);
            verifyIfHeader(resource, true);
            
            /* Parse the request body XML: */
            Document requestBody = parseRequestBody(request);

            /* Make sure the request is valid: */
            validateRequestBody(requestBody);

            //Resource resource = repository.retrieve(token, uri, false);

            doPropertyUpdate(resource, requestBody, token);
            
            /* Store the altered resource: */
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("storing modified Resource");
            }
            resource = this.repository.store(token, resource);

            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_OK));
            model.put(WebdavConstants.WEBDAVMODEL_REQUESTED_RESOURCE,
                      resource);
            model.put(WebdavConstants.WEBDAVMODEL_ETAG, resource.getEtag());

        } catch (InvalidRequestException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught InvalidRequestException for URI "
                             + uri, e);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_BAD_REQUEST));

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
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (IllegalOperationException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught IllegalOperationException for URI "
                             + uri, e);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (ReadOnlyException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught ReadOnlyException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (ResourceLockedException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught ResourceLockedException for URI " + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpUtil.SC_LOCKED));

//        } catch (AclException e) {
//            if (logger.isDebugEnabled()) {
//                logger.debug("Caught AclException for URI " + uri, e);
//            }
//            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
//            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
//                      new Integer(HttpServletResponse.SC_FORBIDDEN));
//
        } catch (IOException e) {
            this.logger.info("Caught IOException for URI " + uri, e);
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }

        return new ModelAndView("PROPPATCH", model);
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
    protected void validateRequestBody(Document requestBody)
        throws InvalidRequestException {

        Element root = requestBody.getRootElement();

        if (!"propertyupdate".equals( root.getName())) {

            throw new InvalidRequestException(
                "Invalid request element '" + root.getName()
                + "' (expected 'propertyupdate')");
        }      

        for (Iterator actionIterator = root.getChildren().iterator();
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
    protected void doPropertyUpdate(Resource resource,
                                    Document requestBody, String token) 
        throws ResourceNotFoundException, AuthorizationException,
        AuthenticationException, IllegalOperationException,
        InvalidRequestException {
        
        Element root = requestBody.getRootElement();

        for (Iterator actionIterator = root.getChildren().iterator();
             actionIterator.hasNext();) {

            Element actionElement = (Element) actionIterator.next();

            String action = actionElement.getName();

            if (action.equals("set")) {
                setProperties(resource, 
                              actionElement.getChild(
                                  "prop", WebdavConstants.DAV_NAMESPACE).getChildren(), 
                              token);
                
            } else if (action.equals("remove")) {
                removeProperties(resource,
                                 actionElement.getChild(
                                     "prop", WebdavConstants.DAV_NAMESPACE).getChildren());

            } else {
                throw new InvalidRequestException(
                    "invalid element '" + action + "' (expected "
                    + "'set' or 'remove')");
            }
        }

    }




    /**
     * Sets a list of properties on a resource .
     *
     * @param resource the <code>Resource</code> to modify
     * @param propElements a list of <code>org.jdom.Element</code>
     * objects representing DAV 'prop' elements (see RFC 2518,
     * sec. 12.11)
     */
    protected void setProperties(Resource resource,  
                                 List propElements, 
                                 String token)
        throws ResourceNotFoundException, AuthorizationException,
        AuthenticationException, IllegalOperationException {

        for (Iterator elementIterator = propElements.iterator();
             elementIterator.hasNext();) {
            Element propElement = (Element) elementIterator.next();
            setProperty(resource, propElement, token);
        }
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
    protected void setProperty(Resource resource, Element propertyElement, 
                               String token)
        throws ResourceNotFoundException, AuthorizationException,
        AuthenticationException, IllegalOperationException {

        String propertyName = propertyElement.getName();
        String nameSpace = propertyElement.getNamespace().getURI();

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Setting property with namespace: " + nameSpace);
        }

        
        if (nameSpace.toUpperCase().equals(WebdavConstants.DAV_NAMESPACE.getURI())) {

            if (propertyName.equals("displayname")) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("setting property 'displayname' to '"
                                 + propertyElement.getText() + "'");
                }
                resource.setDisplayName(propertyElement.getText());
                
            } else if (propertyName.equals("getcontentlanguage")) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("setting property 'getcontentlanguage' to '"
                                 + propertyElement.getText() + "'");
                }
                // XXX: Locale needs to be better handled
                Locale locale = LocaleHelper.getLocale(propertyElement.getText());
                resource.setContentLocale(propertyElement.getText());
                
            } else if (propertyName.equals("getcontenttype")) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("setting property 'getcontenttype' to '"
                                 + propertyElement.getText() + "'");
                }
                resource.setContentType(propertyElement.getText());
                
            } else {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Unsupported property: " + propertyName);
                }
                //throw new AuthorizationException();
            }

        } else {

            Namespace ns;
            if (nameSpace.toUpperCase().equals(WebdavConstants.DEFAULT_NAMESPACE.getURI().toUpperCase())) {
                ns = Namespace.DEFAULT_NAMESPACE;
            } else {
                ns = Namespace.getNamespace(nameSpace);
            }
 
            Property property = resource.getProperty(ns, propertyName);

            if (property == null) {
                /* Create a new property: */
                property = resource.createProperty(ns, propertyName);
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
                        property.setValues(elementToValues(propertyElement, def.getType()));
                    } else {
                        property.setValue(elementToValue(propertyElement, def.getType()));
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
    }
    
    

    protected void removeProperties(Resource resource, List propElements) {
        for (Iterator elementIterator = propElements.iterator();
             elementIterator.hasNext();) {
            Element theProperty = (Element) elementIterator.next();

            removeProperty(resource, theProperty);
        }
    }
    

    protected void removeProperty(Resource resource, Element propElement) {

        if (propElement.getNamespace().equals(WebdavConstants.DAV_NAMESPACE)) {
            return; 
        }

        String propertyName = propElement.getName();
        Namespace namespace = Namespace.getNamespace(propElement.getNamespace().getURI());
        resource.removeProperty(namespace, propertyName);
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
    
    protected Value elementToValue(Element element, int type) throws ValueFormatException {
        String stringValue = element.getText();
        
        if (type == PropertyType.TYPE_DATE) {
            // Try to be liberal in accepting date formats:
            try {
                return new Value(WebdavUtil.parsePropertyDateValue(stringValue));
            } catch (ParseException e) {
                try {
                    return this.valueFactory.createValue(stringValue, type);
                } catch (Exception vfe) {
                    throw new ValueFormatException(e);
                }
            }
        } 
        return this.valueFactory.createValue(stringValue, type);
    }
    
    protected Value[] elementToValues(Element element, int type) throws ValueFormatException {
        
        String[] stringValues;
        Element valuesElement;
        if ((valuesElement = element.getChild("values", 
                WebdavConstants.VORTIKAL_PROPERTYVALUES_XML_NAMESPACE))!= null) {
                
            List children = valuesElement.getChildren(
                "value", WebdavConstants.VORTIKAL_PROPERTYVALUES_XML_NAMESPACE);
            
            stringValues = new String[children.size()];
            int u=0;
            for (Iterator i = children.iterator(); i.hasNext(); ) {
                stringValues[u++] = ((Element)i.next()).getText();
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
        if (type == PropertyType.TYPE_DATE) {
            values = new Value[stringValues.length];
            try {
                for (int i=0; i<values.length; i++) {
                    values[i] = new Value(WebdavUtil.parsePropertyDateValue(stringValues[i]));
                }
            } catch (ParseException e) {
                throw new ValueFormatException(e.getMessage());
            }
        } else {
            values = this.valueFactory.createValues(stringValues, type);
        }
        
        return values;
    }




    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }
    
}
