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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vortikal.repository.AclException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Property;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.RequestContext;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.web.servlet.ModelAndView;

/**
 * Handler for PROPPATCH requests.
 *
 */
public class ProppatchController extends AbstractWebdavController {




    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {
         
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        Map model = new HashMap();

        try {

            /* Parse the request body XML: */
            Document requestBody = parseRequestBody(request);

            /* Make sure the request is valid: */
            validateRequestBody(requestBody);

            Resource resource = repository.retrieve(token, uri, false);

            doPropertyUpdate(resource, requestBody, token);
            
            /* Store the altered resource: */
            if (logger.isDebugEnabled()) {
                logger.debug("storing modified Resource");
            }
            repository.store(token, resource);

            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_OK));
            model.put(WebdavConstants.WEBDAVMODEL_REQUESTED_RESOURCE,
                      resource);
            return new ModelAndView("PROPPATCH", model);
            
        } catch (InvalidRequestException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught InvalidRequestException for URI "
                             + uri, e);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_BAD_REQUEST));

        } catch (ResourceNotFoundException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught ResourceNotFoundException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_NOT_FOUND));

        } catch (IllegalOperationException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught IllegalOperationException for URI "
                             + uri, e);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (ReadOnlyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught ReadOnlyException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (ResourceLockedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught ResourceLockedException for URI " + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpUtil.SC_LOCKED));

        } catch (AclException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caught AclException for URI " + uri, e);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpServletResponse.SC_FORBIDDEN));

        } catch (IOException e) {
            logger.info("Caught IOException for URI " + uri, e);
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
            logger.info("Invalid XML in request body", e);
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
        InvalidRequestException, AclException, java.io.IOException {
        
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
        AuthenticationException, IllegalOperationException,
        java.io.IOException, AclException {

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
     * @param property an <code>org.jdom.Element</code> object
     * representing a DAV property element. This may be a standard DAV
     * property, or a custom one, although at present only standard
     * DAV properties are supported.
     */
    protected void setProperty(Resource resource, Element property, 
                               String token)
        throws ResourceNotFoundException, AuthorizationException,
        AuthenticationException, IllegalOperationException,
        java.io.IOException, AclException {

        String propertyName = property.getName();
        String nameSpace = property.getNamespace().getURI();

        if (logger.isDebugEnabled()) {
            logger.debug("Setting property with namespace: " + nameSpace);
        }

        
        if (nameSpace.toUpperCase().equals(WebdavConstants.DAV_NAMESPACE.getURI())) {

            if (propertyName.equals("displayname")) {
                if (logger.isDebugEnabled()) {
                    logger.debug("setting property 'displayname' to '"
                                 + property.getText() + "'");
                }
                resource.setDisplayName(property.getText());
                
            } else if (propertyName.equals("getcontentlanguage")) {
                if (logger.isDebugEnabled()) {
                    logger.debug("setting property 'getcontentlanguage' to '"
                                 + property.getText() + "'");
                }
                resource.setContentLanguage(property.getText());
                
            } else if (propertyName.equals("getcontenttype")) {
                if (logger.isDebugEnabled()) {
                    logger.debug("setting property 'getcontenttype' to '"
                                 + property.getText() + "'");
                }
                resource.setContentType(property.getText());
                
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unsupported property: " + propertyName);
                }
                //throw new AuthorizationException();
            }

        } else {

            ArrayList properties = new ArrayList(
                Arrays.asList(resource.getProperties()));

            Property theProperty = null;

            for (Iterator i = properties.iterator(); i.hasNext();) {
                Property currProperty = (Property) i.next();

                if (currProperty.getName().equals(propertyName) &&
                    currProperty.getNamespace().equals(nameSpace)) {
                    theProperty = currProperty;
                }
            }
            
            if (theProperty == null) {

                /* Create a new property: */
                theProperty = new Property();
                theProperty.setNamespace(nameSpace);
                theProperty.setName(propertyName);
                properties.add(theProperty);
            }
            
            if (logger.isDebugEnabled()) {
                logger.debug("Setting property " + theProperty + 
                             " on resource " + resource.getURI());
            }
            theProperty.setValue(elementToString(property));
            
            resource.setProperties((Property[])
                                   properties.toArray(new Property[]{}));
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
            return; //throw new AuthorizationException();
        }

        String propertyName = propElement.getName();
        String namespace = propElement.getNamespace().getURI();

        ArrayList properties = new ArrayList(
            Arrays.asList(resource.getProperties()));

        Property theProperty = null;

        for (Iterator i = properties.iterator(); i.hasNext();) {
            Property currProperty = (Property) i.next();

            if (logger.isDebugEnabled()) {
                logger.debug("Checking property to remove: " + currProperty.getName());
            }
            if (currProperty.getName().equals(propertyName) &&
                currProperty.getNamespace().equals(namespace)) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Found property to remove: " + propElement.getName());
                }
                
                theProperty = currProperty;
            }
        }
            
        if (theProperty == null) {
            return;
        }
            
        if (logger.isDebugEnabled()) {
            logger.debug("properties before: " + properties.size());
        }
        properties.remove(theProperty);
        if (logger.isDebugEnabled()) {
            logger.debug("properties after: " + properties.size());
        }

        resource.setProperties((Property[])
                               properties.toArray(new Property[]{}));
        
    }
    


    /**
     * Builds a string representation of a property element.
     *
     * @param element an <code>Element</code> of type "dav:prop".
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
            logger.warn("Error reading property value", e);
            return null;
        }
    }
    
}
