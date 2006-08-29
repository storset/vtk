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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.JDOMException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;
import org.vortikal.xml.StylesheetCompilationException;
import org.vortikal.xml.TransformerManager;



/**
 * Abstract superclass for the XML edit controllers.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>lockTimeoutSeconds</code> - an integer specifying the
 *   number of seconds to lock the resource when editing it. The
 *   default is <code>1800</code> (30 minutes).
 *   <li><code>viewName</code> - the view name to return.
 * </ul>
 */
public abstract class AbstractXmlEditController implements Controller {

    private Namespace schemaNamespace = Namespace.DEFAULT_NAMESPACE;
    private String schemaName = "schema";
    
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
    
    private int lockTimeoutSeconds = 30 * 60;

    protected String viewName = "edit";

    protected Log logger = LogFactory.getLog(this.getClass());
    

    protected Repository repository;
    private TransformerManager transformerManager;


    public void setDeleteSubElementAtService(Service deleteSubElementAtService) {
        this.deleteSubElementAtService = deleteSubElementAtService;
    }

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
    
    public void setTransformerManager(TransformerManager transformerManager) {
        this.transformerManager = transformerManager;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setDeleteElementService(Service deleteElementService) {
        this.deleteElementService = deleteElementService;
    }

    public void setMoveElementService(Service moveElementService) {
        this.moveElementService = moveElementService;
    }

    public void setNewElementAtService(Service newElementAtService) {
        this.newElementAtService = newElementAtService;
    }

    public void setEditElementDoneService(Service editElementDoneService) {
        this.editElementDoneService = editElementDoneService;
    }

    public void setLockTimeoutSeconds(int lockTimeoutSeconds) {
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    public void setViewName(final String viewName){
	this.viewName = viewName;
    }

    public void setMoveElementDoneService(Service moveElementDoneService) {
        this.moveElementDoneService = moveElementDoneService;
    }

    public void setFinishEditingService(Service finishEditingService) {
        this.finishEditingService = finishEditingService;
    }


    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {
        
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();

        String sessionID = AbstractXmlEditController.class.getName() + ":" + uri; 
        
        Map sessionMap = (Map) request.getSession(true).getAttribute(sessionID);

        /* Check that sessionmap isn't stale (the lock has been released) */
        /* a user can access the same (locked) resource from different clients */
        if (sessionMap != null) {
            String token = SecurityContext.getSecurityContext().getToken();
            Principal principal = SecurityContext.getSecurityContext().getPrincipal();
            Resource resource = this.repository.retrieve(token, uri, false);
            Lock lock = resource.getLock();
            if (lock == null || (!lock.getPrincipal().equals(principal))) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Stored xml edit session data is out of date.");
                }
                request.getSession(true).removeAttribute(sessionID);
                sessionMap = null;
            }
        }
        
        /* "Validate" and create web-editable resource session */
        if (sessionMap == null) {
            initEditSession(request);
            sessionMap = (Map) request.getSession(true).getAttribute(sessionID);
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
        if (this.browseService != null) {
            try {
                Resource parentResource = this.repository.retrieve(token, resource
                        .getParent(), false);
                setXsltParameter(model, "BROWSEURL", this.browseService
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
                this.editService.constructLink(resource, principal));
        setXsltParameter(model, "editElementServiceURL", this.editElementService
                .constructLink(resource, principal));
        setXsltParameter(model, "editElementDoneServiceURL", this.editElementDoneService
                .constructLink(resource, principal));
        setXsltParameter(model, "moveElementServiceURL", this.moveElementService
                .constructLink(resource, principal));
        setXsltParameter(model, "moveElementDoneServiceURL", this.moveElementDoneService
                .constructLink(resource, principal));
        setXsltParameter(model, "deleteElementServiceURL", this.deleteElementService
                .constructLink(resource, principal));
        setXsltParameter(model, "newElementAtServiceURL", this.newElementAtService
                .constructLink(resource, principal));
        setXsltParameter(model, "newElementServiceURL", this.newElementService
                .constructLink(resource, principal));
        setXsltParameter(model, "newSubElementAtServiceURL",
                this.newSubElementAtService.constructLink(resource, principal));
        setXsltParameter(model, "deleteSubElementAtServiceURL",
                this.deleteSubElementAtService.constructLink(resource, principal));
        setXsltParameter(model, "finishEditingServiceURL",
                this.finishEditingService.constructLink(resource, principal));

    }

    private String date(String format) {
        Date today = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(today);
    }

    private ModelAndView handleModeError(EditDocument document, HttpServletRequest request) {
        Map model = new HashMap();
        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();

        StringBuffer sb = new StringBuffer();
        

        sb.append("Mismatch in edit state (don't use 'back' button). ");
        sb.append("Request context: [").append(requestContext).append("], ");
        sb.append("security context: [").append(securityContext).append("], ");
        sb.append("request parameters: ").append(request.getParameterMap()).append(", ");
        sb.append("user agent: [").append(request.getHeader("User-Agent")).append("], ");
        sb.append("remote host: [").append(request.getRemoteHost()).append("]");
        sb.append("Current document state:\n").append(document.toStringDetail());

        this.logger.warn(sb.toString());
        
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
            SchemaDocumentDefinition documentDefinition) throws IOException, XMLEditException;


    private void initEditSession(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        
        // FIXME: possible multiple repositories at once!
        String sessionID = AbstractXmlEditController.class.getName() + ":" + uri; 
        
        Resource resource = this.repository.retrieve(token, uri, false);
        
        Map sessionMap = new HashMap();

        EditDocument document = null;
        SchemaDocumentDefinition documentDefinition = null;
        
        
        /* The resource has to be an XML document */
        if (! ContentTypeHelper.isXMLContentType(resource.getContentType())) {
            throw new XMLEditException("Resource is not an xml document");
        }

        /* get required schemaURL */
        Property schemaProp = resource.getProperty(schemaNamespace, schemaName); 
        if (schemaProp == null)
            throw new XMLEditException(
                    "XML document is uneditable, schema reference is missing");

        String schemaURL = schemaProp.getStringValue();
        if (schemaURL == null) 
            throw new XMLEditException("Invalid schema URI '" + schemaURL + "'");
        
        /* Try to build document */
        try {
            document = EditDocument.createEditDocument(this.repository, this.lockTimeoutSeconds);
        } catch (JDOMException e) {
            // FIXME: error handling?
            throw new XMLEditException("Document build failure", e);
        } catch (IOException e) {
            throw new XMLEditException("Document build failure", e);
        } 
        
        
        String docType = document.getRootElement().getName();
        

        /* try to instantiate schema-parser */
        try {
            documentDefinition = 
                new SchemaDocumentDefinition(docType, new URL(schemaURL));
        } catch (JDOMException e) {
            throw new XMLEditException("Schema build failure for schema '" + schemaURL + "'", e);
        } catch (MalformedURLException e) {
            throw new XMLEditException("Invalid schema uri '" + schemaURL + "'", e);
        } catch (IOException e) {
            throw new XMLEditException("Schema build failure for schema '" + schemaURL + "'", e);
        }

        /* Locate the edit XSL for this document type */
        String relativePath = documentDefinition.getXSLPath();
        if (relativePath == null || relativePath.trim().equals("")) {
            throw new XMLEditException("Edit XSL path not defined in schema '" + schemaURL + "'");
        }
        
        
        try {
            this.transformerManager.getTransformer(resource, document);
        } catch (IOException e) {
            // FIXME: error handling
            throw new XMLEditException("Unable to compile edit stylesheets for document '" + uri + "'", e);
        } catch (TransformerConfigurationException e) {
            // FIXME: error handling
            throw new XMLEditException("Unable to compile edit stylesheets for document '" + uri + "'", e);
        } catch (StylesheetCompilationException e) {
            // FIXME: error handling
            throw new XMLEditException("Unable to compile edit stylesheets for document '" + uri + "'", e);
        }

        sessionMap.put(EditDocument.class.getName(), document);
        sessionMap.put(SchemaDocumentDefinition.class.getName(), documentDefinition);
        request.getSession(true).setAttribute(sessionID, sessionMap);
    } 

    


    protected Object getXsltParameter(Map model, String key) {
        Map parameters = (Map)model.get("xsltParameters");
        if (parameters != null) {
            return parameters.get(key);
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

    protected final void setXsltParameter(Map model, String key, Object value) {
        Map parameters = (Map)model.get("xsltParameters");
        if (parameters == null) {
            parameters = new HashMap();
            model.put("xsltParameters", parameters);
        }
        parameters.put(key, value);
    }
}
