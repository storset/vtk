/* Copyright (c) 2004, 2008 University of Oslo, Norway
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
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.util.io.BoundedInputStream;
import org.vortikal.util.io.SizeLimitException;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.InvalidRequestException;
import org.vortikal.web.RequestContext;

/**
 * Handler for PROPFIND requests.
 * 
 * Analyzes the propfind body and puts the list of requested resources
 * in the model.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>maxRequestSize</code> - the maximum number of bytes the
 *   request size may consist of before it is considered invalid. The
 *   default value is <code>40000</code>.
 * </ul>
 *
 * <p>View names returned:
 * <ul>
 *   <li>PROPFIND - in successful cases
 *   <li>HTTP_STATUS_VIEW - in error cases
 * </ul>
 * 
 * <p>Model data provided (successful cases):
 * <ul>
 *   <li>resources - a list of the resources being requested
 *   <li>requestedProperties - the list of the requested resource
 *       properties (these are <code>org.jdom.Element</code> objects,
 *       representing the <code>dav:prop</code> elements from the
 *       request body).
 *   <li>appendValuesToRequestedProperties - a boolean indicating
 *       whether the values of the requested properties (not only the
 *       names and their existence) should be appended
 * </ul>
 *
 * <p>Model data provided (error cases):
 * <ul>
 *   <li>httpStatusCode - the HTTP status code indicating the type of
 *       error
 *   <li>errorObject - the cause of the error
 * </ul>
 * 
 */
public class PropfindController extends AbstractWebdavController {


    private long maxRequestSize = 40000;

    /**
     * Sets the maximum number of bytes allowed in request body. This
     * is to reduce the risk of DoS attacks by clients sending huge
     * request bodies.
     *
     * @param newSize a <code>Long</code> value
     */
    public void setMaxRequestSize(long newSize) {
        this.maxRequestSize = newSize;
    }

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
         
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        Map<String, Object> model = new HashMap<String, Object>();
        try {
            Resource resource = repository.retrieve(token, uri, false);

            /* Parse the request body XML: */
            Document requestBody = parseRequestBody(request);

            validateRequestBody(requestBody);

            String depth = request.getHeader("Depth");

            if (depth == null) {

                /* FIXME: No Depth header from client means treat as
                 * 'infinite'. However, the current (and only) backend
                 * does not support this. (And probably shouldn't?) */
                depth = "1";
            }
            
            model = buildPropfindModel(
                resource, requestBody, depth, token);
         
            return new ModelAndView("PROPFIND", model);

        } catch (InvalidRequestException e) {
            this.logger.warn("Caught InvalidRequestException for URI "
                        + uri, e);
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

        } catch (ResourceLockedException e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Caught ResourceLockedException for URI "
                             + uri);
            }
            model.put(WebdavConstants.WEBDAVMODEL_ERROR, e);
            model.put(WebdavConstants.WEBDAVMODEL_HTTP_STATUS_CODE,
                      new Integer(HttpUtil.SC_LOCKED));
        }
        return new ModelAndView("HTTP_STATUS_VIEW", model);
    }
   
    /**
     * Retrieves the requested resources and puts them in the model.
     * 
     * @param resource the resource 
     * @param requestBody a PROPFIND JDOM tree
     * @param depth defines the recursive behavior of the method
     * @param token the client session
     * @return the MVC model.
     * @exception InvalidRequestException if invalid request body is
     * supplied
     * @exception ResourceNotFoundException if the resource is not found
     * @exception AuthenticationException if the resource in question
     * requires authorization and the session ID is not authenticated
     * @exception AuthorizationException if the client does not have
     * sufficient rights to access the resource
     * @exception IOException if an I/O error occurs
     */
    private Map<String, Object> buildPropfindModel(
        Resource resource, Document requestBody, String depth, String token)
        throws InvalidRequestException, ResourceNotFoundException,
        AuthenticationException, AuthorizationException, Exception {
        Repository repository = RequestContext.getRequestContext().getRepository();
        
        Map<String, Object> model = new HashMap<String, Object>();

        List<Resource> resourceList = new ArrayList<Resource>();
        if (resource.isCollection()) {
            resourceList = getResourceDescendants(
                resource.getURI(), depth, repository, token);
        }
        if (depth.equals("0") || depth.equals("1")) {
            resourceList.add(resource);
        }
        model.put(WebdavConstants.WEBDAVMODEL_REQUESTED_RESOURCES,
                  resourceList);

        Element root = requestBody.getRootElement();
        Element propType = (Element) root.getChildren().get(0);
        String propTypeName = propType != null ? 
                propType.getName().toLowerCase() : null;

        if (! ("allprop".equals(propTypeName)
               || "propname".equals(propTypeName)
               || "prop".equals(propTypeName))) {
            throw new InvalidRequestException(
                "Expected one of `allprop', `propname' or `prop' elements");
        }
        
        boolean wildcardPropRequest = 
            ("allprop".equals(propTypeName) || "propname".equals(propTypeName)); 
        model.put(WebdavConstants.WEBDAVMODEL_WILDCARD_PROP_REQUEST, wildcardPropRequest);
        
        List<Element> requestedProps = getRequestedProperties(requestBody, resource);

        model.put(WebdavConstants.WEBDAVMODEL_REQUESTED_PROPERTIES, requestedProps);

        /* if property name is 'allprop' or 'prop', we expect values
         * of the elements to be filled into the response elements */
        boolean appendPropertyValues = ("allprop".equals(propTypeName)
                                        || "prop".equals(propTypeName));

        model.put(WebdavConstants.WEBDAVMODEL_REQUESTED_PROPERTIES_APPEND_VALUES,
                  new Boolean(appendPropertyValues));
        
        return model;
    }
    

    /**
     * Returns the WebDAV source element for a resource.
     *
     * @param resource the <code>Resource</code> in question
     * @return a WebDAV "source" element
     */
    protected Element buildSourceElement(Resource resource) {
        return new Element("notimplemented");
    }


    /**
     * Finds the properties requested by the client as specified in
     * the PROPFIND body.
     *
     * @param requestBody the WebDAV request body 
     * @return a <code>List</code> of DAV property elements
     * represented as <code>org.jdom.Element</code> objects.
     */
    protected List<Element> getRequestedProperties(Document requestBody, Resource res) {
        List<Element> propList = new ArrayList<Element>();

        /* Check for 'allprop' or 'propname': */
        if (requestBody.getRootElement().getChild(
                "allprop", WebdavConstants.DAV_NAMESPACE) != null ||
            requestBody.getRootElement().getChild(
                "propname", WebdavConstants.DAV_NAMESPACE) != null) {

            /* DAV properties: */
            for (String name: DAV_PROPERTIES) {
                Element e = new Element(name, WebdavConstants.DAV_NAMESPACE);
                propList.add(e);
            }

            List<Element> defaultNsPropList = new ArrayList<Element>();
            List<Element> otherProps = new ArrayList<Element>();

            /* Resource type (treat it as a normal property): */
            defaultNsPropList.add(new Element("resourceType", WebdavConstants.DEFAULT_NAMESPACE.getURI()));
        

            /* Other properties: */
            for (Property prop: res) {
                Namespace namespace = prop.getDefinition().getNamespace();
                String name = prop.getDefinition().getName();

                if (Namespace.DEFAULT_NAMESPACE.equals(namespace)
                    && MAPPED_DAV_PROPERTIES.containsValue(name)) {
                    continue;
                }
                Element e;

                if (Namespace.DEFAULT_NAMESPACE.equals(namespace)) {
                    e = new Element(name, WebdavConstants.DEFAULT_NAMESPACE.getURI());
                    if (isSupportedProperty(name, e.getNamespace())) {
                        defaultNsPropList.add(e);
                    }
                } else {
                    e = new Element(name, namespace.getUri());
                    if (isSupportedProperty(name, e.getNamespace())) {
                        otherProps.add(e);
                    }
                }
            }
            propList.addAll(defaultNsPropList);
            propList.addAll(otherProps);
            
        } else {

            Element propertyElement = requestBody.getRootElement().getChild(
                "prop", WebdavConstants.DAV_NAMESPACE);
      
            for (@SuppressWarnings("rawtypes")
            Iterator propIter = propertyElement.getChildren().iterator();
                 propIter.hasNext();) {

                Element requestedProperty = (Element) propIter.next();
                if (isSupportedProperty(requestedProperty.getName(),
                                        requestedProperty.getNamespace())) {
                    propList.add(requestedProperty);
                }
            }
        }
        return propList;
    }
   

    /**
     * Gets a list of a resource's children/descendants, depending on
     * the value of the <code>depth</code> parameter.
     *
     * @param uri the URI of the resource of which to find children
     * @param depth determines whether to list only the immediate
     * children of the resource or all descendants (legal values are
     * <code>"1"</code> or <code>"infinity"</code>.
     * @param repository the <code>Repository</code> to query
     * @param token the client session
     * @return a <code>List</code> of <code>Resource</code> objects
     */
    protected List<Resource> getResourceDescendants(Path uri, String depth,
                                          Repository repository, String token)
        throws ResourceNotFoundException, AuthorizationException,
        AuthenticationException, Exception {

        ArrayList<Resource> descendants = new ArrayList<Resource>();
        
        if (!(depth.equals("1") || depth.equals("infinity"))) {
            return descendants;
        }
        

        Resource[] resourceArray = new Resource[0];

        if (depth.equals("1")) {

            /* List immediate children: */

            resourceArray = repository.listChildren(token, uri, false);          
            this.logger.debug("Number of children: " + resourceArray.length);

        } else if (depth.equals("infinity")) {

            /* List all descendants: */

            this.logger.warn("NOT IMPLEMENTED: listChildrenRecursively()");
            resourceArray = repository.listChildren(token, uri, false);
        }
        
        for (int i = 0;  i < resourceArray.length; i++) {
            descendants.add(resourceArray[i]);
        }

        return descendants;
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
        builder.setValidation(false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        /* if empty request body, request is implicitly "allprop": */
        if (request.getHeader("Content-Length") != null
            && request.getHeader("Content-Length").equals("0")) {
            Element propFind = new Element("propfind", WebdavConstants.DAV_NAMESPACE);
            propFind.addContent(new Element("allprop", WebdavConstants.DAV_NAMESPACE));
            return new Document(propFind);
        }

        try {
            Document requestBody = builder.build(
                new BoundedInputStream(
                    request.getInputStream(), this.maxRequestSize));
            return requestBody;

        } catch (JDOMException e) {
            throw new InvalidRequestException(e.getMessage(), e);

        } catch (SizeLimitException e) {
            throw new InvalidRequestException(
                "PROPFIND request too large for size limit: " + this.maxRequestSize);
        }
    }
   

    /**
     * Verifies that a JDOM tree constitutes a valid PROPFIND request
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
        if (!root.getName().equals("propfind")) {
            // FIXME: actually validate the request body
            throw new InvalidRequestException(
                "Invalid request element '" + root.getName()
                + "' (expected 'propfind')");
        }      
    }
}
