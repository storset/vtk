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
package org.vortikal.edit.xml;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerConfigurationException;

import org.vortikal.repository.Lock;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.security.AuthenticationException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;
import org.vortikal.xml.StylesheetCompilationException;
import org.vortikal.xml.TransformerManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;



/**
 * Abstract superclass for the XML edit controllers.
 *
 * @version $Id$
 */
public abstract class AbstractXmlEditController implements Controller {

    private Service editElementService;
    private Service editElementDoneService;
    
    private Service newElementAtService;

    private Service editService;
    private Service browseService;

    private Service moveElementService;
    private Service moveElementDoneService;
    private Service deleteElementService;
    private Service newElementService;

    private Service newSubElementAtService;
    private Service deleteSubElementAtService;
    private Service finishEditingService;
    
    protected String viewName = "edit";


    /**
     * @param deleteSubElementAtService The deleteSubElementAtService to set.
     */
    public void setDeleteSubElementAtService(Service deleteSubElementAtService) {
        this.deleteSubElementAtService = deleteSubElementAtService;
    }
    /**
     * @param newElementService The newElementService to set.
     */
    public void setNewElementService(Service newElementService) {
        this.newElementService = newElementService;
    }

    public void setNewSubElementAtService(Service newSubElementAtService) {
        this.newSubElementAtService = newSubElementAtService;
    }

    public void setEditElementService(Service editElementService) {
        this.editElementService = editElementService;
    }

    public void setEditService(Service editService) {
        this.editService = editService;
    }

    public void setBrowseService(Service browseService) {
        this.browseService = browseService;
    }
    
    protected Log logger = LogFactory.getLog(this.getClass());
    
    public final Namespace XSI_NAMESPACE = 
        Namespace.getNamespace("xsi",
                               "http://www.w3.org/2001/XMLSchema-instance");

    public final String PROPERTY_NAMESPACE =
        "http://www.uio.no/vortex/custom-properties";

    public final String EDIT_PROPERTY = "web-edit";

    protected Repository repository;
    private TransformerManager transformerManager;
    
    /**
     * @param transformerManager The transformerManager to set.
     */
    public void setTransformerManager(TransformerManager transformerManager) {
        this.transformerManager = transformerManager;
    }
    
    /**
     * @param repository The repository to set.
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    /**
     * @see org.springframework.web.servlet.mvc.Controller#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {
        
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();

        // FIXME: possible multiple repositories at once!
        String sessionID = AbstractXmlEditController.class.getName() + ":" + uri; 
        
        Map sessionMap = (Map) request.getSession().getAttribute(sessionID);

        /* Check that sessionmap isn't stale (the lock has been released) */
        // FIXME: a user can access the same (locked) resource from different clients
        if (sessionMap != null) {
            String token = SecurityContext.getSecurityContext().getToken();
            Principal principal = SecurityContext.getSecurityContext().getPrincipal();
            Resource resource = 
                repository.retrieve(token, uri, false);
            Lock[] locks = resource.getActiveLocks();
            if (locks == null
                || (locks.length > 0 && !locks[0].getPrincipal().equals(principal))) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Stored xml edit session data is out of date.");
                }
                request.getSession().removeAttribute(sessionID);
                sessionMap = null;
            }
        }
        
        /* "Validate" and create web-editable resource session */
        if (sessionMap == null) {
            initEditSession(request);
            sessionMap = (Map) request.getSession().getAttribute(sessionID);
        } 
        
        EditDocument document = (EditDocument) 
            sessionMap.get(EditDocument.class.getName());
        SchemaDocumentDefinition documentDefinition = (SchemaDocumentDefinition)
            sessionMap.get(SchemaDocumentDefinition.class.getName());
        
        ModelAndView mov = handleRequestInternal(request, response, document, documentDefinition);
        
        if (mov == null) {
            mov = handleModeError(document, request);
        }
        
        Map model = mov.getModel();
        model.put("resource", document.getResource());
        model.put("jdomDocument", document);

        referenceData(model, document);

        return mov;
    }
    
    private void referenceData(Map model, EditDocument document) 
        throws IOException {
        Resource resource = document.getResource();
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();

        String token = SecurityContext.getSecurityContext().getToken();

//        setXsltParameter(model, "RESOURCESURL", null);
//        setXsltParameter(model, "BROWSEURL", null);

        setXsltParameter(model, "pageTitle", "Du redigerer: " + resource.getName());
        setXsltParameter(model, "DAY", date("dd"));
        setXsltParameter(model, "MONTH", date("MM"));
        setXsltParameter(model, "YEAR", date("yyyy"));
        setXsltParameter(model, "TIMESTAMP", date("yyMMddHHmmss"));
        setXsltParameter(model, "CMSURL", resource.getURI());
        setXsltParameter(model,"applicationMode", "edit");
        if (principal != null)
            setXsltParameter(model, "USERNAME", principal.getName());

        // The Browse service is optional, must javadoc this
        if (browseService != null) {
            try {
                Resource parentResource = repository.retrieve(token, resource
                        .getParent(), false);
                setXsltParameter(model, "BROWSEURL", browseService
                        .constructLink(parentResource, principal));
            } catch (AuthorizationException e) {
                // No browse available for this resource
            } catch (AuthenticationException e) {
                // No browse available for this resource
            } catch (ServiceUnlinkableException e) {
                // No browse available for this resource
            }
        }

        setXsltParameter(model, "editServiceURL", 
                editService.constructLink(resource, principal));
        setXsltParameter(model, "editElementServiceURL", editElementService
                .constructLink(resource, principal));
        setXsltParameter(model, "editElementDoneServiceURL", editElementDoneService
                .constructLink(resource, principal));
        setXsltParameter(model, "moveElementServiceURL", moveElementService
                .constructLink(resource, principal));
        setXsltParameter(model, "moveElementDoneServiceURL", moveElementDoneService
                .constructLink(resource, principal));
        setXsltParameter(model, "deleteElementServiceURL", deleteElementService
                .constructLink(resource, principal));
        setXsltParameter(model, "newElementAtServiceURL", newElementAtService
                .constructLink(resource, principal));
        setXsltParameter(model, "newElementServiceURL", newElementService
                .constructLink(resource, principal));
        setXsltParameter(model, "newSubElementAtServiceURL",
                newSubElementAtService.constructLink(resource, principal));
        setXsltParameter(model, "deleteSubElementAtServiceURL",
                deleteSubElementAtService.constructLink(resource, principal));
        setXsltParameter(model, "finishEditingServiceURL",
                finishEditingService.constructLink(resource, principal));

    }

    private String date(String format) {
        Date today = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(today);
    }

    private ModelAndView handleModeError(EditDocument document, HttpServletRequest request) {
        Map model = new HashMap();
        
        setXsltParameter(model, "ERRORMESSAGE", "UNNSUPPORTED_ACTION_IN_MODE");
        return new ModelAndView("edit", model);
    }
    

    /**
     * Internal request handler method. Gets called after editing
     * session initialization (i.e. making sure there an editing
     * session actually exists, that the document is locked,
     * etc.). Subclasses must implement this method.
     * 
     * @param request the servlet request
     * @param response the servlet response
     * @return a model and view
     */
    protected abstract ModelAndView handleRequestInternal(
            HttpServletRequest request, 
            HttpServletResponse response,
            EditDocument document,
            SchemaDocumentDefinition documentDefinition);


    private void initEditSession(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        
        // FIXME: possible multiple repositories at once!
        String sessionID = AbstractXmlEditController.class.getName() + ":" + uri; 
        
        Resource resource = repository.retrieve(token, uri, false);
        
        Map sessionMap = new HashMap();

        EditDocument document = null;
        SchemaDocumentDefinition documentDefinition = null;
        
        
        /* The resource has to be an XML document */
        if (!"text/xml".equals(resource.getContentType())) {
            throw new RuntimeException("Resource is not an xml document");
        }

        /* The property web-edit should be 'true' or 'yes' */
        String webEdit = 
            resource.getProperty(PROPERTY_NAMESPACE, EDIT_PROPERTY).getValue();

        if (webEdit == null || !(webEdit.equals("true") || webEdit.equals("yes"))) {
            throw new RuntimeException("Xml resource is not set to web editable");
        }
        
        /* Try to build document */
        try {
            document = EditDocument.createEditDocument(repository);
        } catch (JDOMException e) {
            // FIXME: error handling?
            throw new RuntimeException("Document build failure", e);
        } catch (IOException e) {
            throw new RuntimeException("Document build failure", e);
        } 
        
        
        String docType = document.getRootElement().getName();

        URL schemaURL = null;

        try {
            schemaURL = getSchemaReference(document);
            
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid schema URI", e);
        }
        

        /* try to instantiate schema-parser */
        try {

            if (schemaURL == null || schemaURL.toString().trim().equals("")) {
                throw new RuntimeException("XML document is uneditable, schema reference is missing");
            }

            documentDefinition = 
                new SchemaDocumentDefinition(docType, schemaURL);
        } catch (JDOMException e) {
            throw new RuntimeException("Schema build failure for schema '" + schemaURL + "'", e);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid schema uri '" + schemaURL + "'", e);
        } catch (IOException e) {
            throw new RuntimeException("Schema build failure for schema '" + schemaURL + "'", e);
        }

        /* Locate the edit XSL for this document type */
        String relativePath = documentDefinition.getXSLPath();
        if (relativePath == null || relativePath.trim().equals("")) {
            throw new RuntimeException("Edit XSL path not defined in schema '" + schemaURL + "'");
        }
        
        
        try {
            transformerManager.getTransformer(resource, document);
        } catch (IOException e) {
            // FIXME: error handling
            throw new RuntimeException("Unable to compile edit stylesheets for document '" + uri + "'", e);
        } catch (TransformerConfigurationException e) {
            // FIXME: error handling
            throw new RuntimeException("Unable to compile edit stylesheets for document '" + uri + "'", e);
        } catch (StylesheetCompilationException e) {
            // FIXME: error handling
            throw new RuntimeException("Unable to compile edit stylesheets for document '" + uri + "'", e);
        }

        sessionMap.put(EditDocument.class.getName(), document);
        sessionMap.put(SchemaDocumentDefinition.class.getName(), documentDefinition);
        request.getSession().setAttribute(sessionID, sessionMap);
    } 

    


    /**
     * Finds the schema reference in the document root element (if it
     * exists).
     *
     * @param document a <code>Document</code> value
     * @return the URL of the schema document, or <code>null</code> if
     * there is no such reference
     * @exception MalformedURLException if the schema URL is not a valid URL
     */
    public URL getSchemaReference(Document document) throws MalformedURLException {
        String xsdURL = document.getRootElement().getAttributeValue(
            "noNamespaceSchemaLocation", XSI_NAMESPACE);
        
        if (xsdURL != null) {
        
            return new URL(xsdURL);
        }

        return null;
    }

    protected Map getRequestParameterMap(HttpServletRequest request) {
        Map parameterMap = new HashMap();
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            
            parameterMap.put(key, request.getParameter(key));
        }
        return parameterMap;
    
    }

    protected Object getXsltParameter(Map model, String key) {
        Map parameters = (Map)model.get("xsltParameters");
        if (parameters != null) {
            return parameters.get(key);
        }
        return null;
    }
    
    protected final void setXsltParameter(Map model, String key, Object value) {
        Map parameters = (Map)model.get("xsltParameters");
        if (parameters == null) {
            parameters = new HashMap();
            model.put("xsltParameters", parameters);
        }
        parameters.put(key, value);
    }
    /**
     * @param deleteElementService The deleteElementService to set.
     */
    public void setDeleteElementService(Service deleteElementService) {
        this.deleteElementService = deleteElementService;
    }
    /**
     * @param moveElementService The moveElementService to set.
     */
    public void setMoveElementService(Service moveElementService) {
        this.moveElementService = moveElementService;
    }
    /**
     * @param newElementAtService The newElementAtService to set.
     */
    public void setNewElementAtService(Service newElementAtService) {
        this.newElementAtService = newElementAtService;
    }
    /**
     * @param editElementDoneService The editElementDoneService to set.
     */
    public void setEditElementDoneService(Service editElementDoneService) {
        this.editElementDoneService = editElementDoneService;
    }

    /**
     * @param viewName viewName as String
     */
    public void setViewName(final String viewName){
	this.viewName = viewName;
    }

    /**
     * @param moveElementDoneService The moveElementDoneService to set.
     */
    public void setMoveElementDoneService(Service moveElementDoneService) {
        this.moveElementDoneService = moveElementDoneService;
    }
    /**
     * @param finishEditingService The finishEditingService to set.
     */
    public void setFinishEditingService(Service finishEditingService) {
        this.finishEditingService = finishEditingService;
    }
}
