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
package org.vortikal.web.referencedata.provider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.DOMOutputter;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.InvalidModelException;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.w3c.dom.NodeList;


/**
 * XSL reference data provider
 * 
 * This class provides backend-data to the model suitable for use in
 * XSLT transformations.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li>repository - the content repository
 *   <li>service - the {@link Service} for constructing path URLs
 *   <li>principalManager - a {@link PrincipalManager} instance
 *   <li><code>requireDocumentInModel</code> - whether to throw an
 *   exception when (normally required) <code>jdomDocument</code>
 *   model data is absent in the model. Default is <code>true</code>.
 *   <li>modelName - name to use for the provided (sub)model
 * </ul>
 *
 * <p>Required model data:
 * <ul>
 *   <li>resource - a <code>org.vortikal.repository.Resource</code> object</li>
 *   <li>jdomDocument - a <code>org.jdom.Document</code> representation of the resource</li>
 * </ul>
 * 
 * <p>Model data provided:
 * <ul>
 *   <li>a submodel having the bean property <code>modelName</code> of
 *   this class as its key. This submodel (map) in turn contains the following data:
 *   <ul>
 *     <li><code>uri</code>, <code>creationTime</code>,
 *     <code>lastModified</code>, <code>name</code>,
 *     <code>owner</code>, <code>modifiedBy</code>,
 *     <code>contentLength</code>, <code>displayName</code>,
 *     <code>currentUserPrivilegeSet</code>, <code>properties</code>:
 *     these are all {@link Resource} properties.
 *     <li><code>currentUser</code>: the currently logged in user
 *     <li><code>pathElements</code>: the breadcrumb path (see below)
 *   </ul>
 * </ul>
 *
 * <p>The breadcrumb path: this is a {@link NodeList} of elements
 *  containing attributes <code>title</code> and <code>URL</code>.
 *  It is a straight forward XML mapping of the <code>breadCrumbProvider</code> 
 *  result.
 */
public class XSLReferenceDataProvider
  implements InitializingBean, ReferenceDataProvider {
    protected Log logger = LogFactory.getLog(this.getClass());
    
    private String modelName = null;
    private PrincipalManager principalManager;
    private Repository repository; 
    private Service service; 
    private boolean requireDocumentInModel = true;
    
    private BreadCrumbProvider breadCrumbProvider; 
        
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }


    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    
    public void setService(Service service) {
        this.service = service;
    }
    
    public void setRequireDocumentInModel(boolean requireDocumentInModel) {
        this.requireDocumentInModel = requireDocumentInModel;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.modelName == null) {
            throw new BeanInitializationException(
                "Bean property 'modelName' must be set");
        }
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                "Bean property 'principalStore' must be set");
        }
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
        if (this.service == null) {
            throw new BeanInitializationException(
                "Bean property 'service' must be set");
        }
        if (this.breadCrumbProvider == null) {
            throw new BeanInitializationException(
                "Bean property 'breadCrumbProvider' must be set");
        }
    }


    public void referenceData(Map model, HttpServletRequest request) {
        
        Resource resource = (Resource) model.get("resource");
        if (resource == null) {
            throw new InvalidModelException(
                "Expected object of name 'resource', class "
                + Resource.class.getName() + " in model");
        }

        Document doc = (Document) model.get("jdomDocument");

        if (doc == null && this.requireDocumentInModel) {
            throw new InvalidModelException(
                "Expected object of name 'jdomDocument', class "
                + Document.class.getName() + " in model");
        }

        if (doc == null) {
            return;
        }

        Map subModel = (Map) model.get(this.modelName);
        if (subModel == null) {
            subModel = new HashMap();
            model.put(this.modelName, subModel);
        }

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Principal principal = securityContext.getPrincipal();

        String uri = resource.getURI();

        /* setting variables */
        subModel.put("uri", uri);       
        subModel.put("creationTime", resource.getCreationTime());
        subModel.put("lastModified", convertDate(resource.getLastModified()));
        subModel.put("name", resource.getName());
        subModel.put("owner", resource.getOwner().getQualifiedName());
        subModel.put("modifiedBy", resource.getModifiedBy().getQualifiedName());
        subModel.put("contentLength", new Long(resource.getContentLength()));
        // XXX: remove?
        subModel.put("displayName", resource.getName());
        subModel.put("properties", resource.getProperties());
        
        /* this property is only set if the page requires authentication */
        String currentUser = (principal != null) ? principal.getName() : null;
        subModel.put("currentUser", currentUser);
         
        NodeList path = buildPaths(request);
        subModel.put("pathElements", path);

    }


    
    
    private NodeList buildPaths(HttpServletRequest request) {

        Document doc = new Document(new Element("pathElements"));
        Element pathElements = doc.getRootElement();

        Map model = new HashMap();
        this.breadCrumbProvider.referenceData(model, request);
        
        BreadcrumbElement[] crumbs = 
            (BreadcrumbElement[])model.get("breadcrumb");

        for (int i = crumbs.length - 1; i >= 0; i--) {
            BreadcrumbElement element = crumbs[i];
          Element pathElement = new Element("pathElement");
          String title = element.getTitle();
          if (title == null) title = "";
          pathElement.setAttribute("title", title);
          String url = element.getURL();
          if (url == null) url = "";
          pathElement.setAttribute("URL", url);
          pathElements.addContent(0, pathElement);
            if (logger.isDebugEnabled()) {
                logger.debug("Built path element: title = "
                        + element.getTitle() + ", URL = " + element.getURL());
            }

        }
        // Convert the JDOM element to a org.w3c.dom Element:

        DOMOutputter oupt = new DOMOutputter();
        NodeList nodeList = null;
        org.w3c.dom.Document domDoc = null;
        
        try {
            domDoc = oupt.output(doc);
            nodeList =  domDoc.getDocumentElement().getChildNodes();
        } catch (JDOMException e) {
            logger.warn("Failed to build path document", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("pathElements: " + pathElements);
            logger.debug("nodeList: " + nodeList.getLength());
        }

        return nodeList;
    }


    private String convertDate(Date d) {

        DateFormat stringDate = new SimpleDateFormat("dd.MM.yyyy");
        return stringDate.format(d);
    }


    public void setBreadCrumbProvider(BreadCrumbProvider breadCrumbProvider) {
        this.breadCrumbProvider = breadCrumbProvider;
    }
    
}
