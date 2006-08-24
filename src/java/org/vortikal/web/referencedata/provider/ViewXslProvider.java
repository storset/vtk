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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.DOMOutputter;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.w3c.dom.NodeList;

/**
 * TODO: make more robust
 * 
 * Adds commonly used metadata to the model. These model data are
 * useful when transforming XML resources using XSLT.
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the repository is required
 *   <li><code>adminService</code> - The default admin mode service -
 *   required
 *   <li><code>supplyRequestParameters</code> - default
 *     <code>true</code> - supply request parameters as a node list to
 *     xsl processing
 *   <li><code>matchAdminServiceAssertions</code> - default 
 *     <code>false</code> - determines whether all assertions must
 *     match in order for the admin link to be constructed
 * </ul>
 * 
 * <p>Model data provided:
 * <ul>
 *   <li><code>xsltParameters</code> (accessable from xsl with
 *   &lt;xsl:param name="parameterName" /&gt;), containing:
 *      <ul>
 *          <li><code>creationTime</code>
 *          <li><code>lastModified</code>
 *          <li><code>contentLanguage</code>
 *          <li><code>PARENT-COLLECTION</code>
 *          <li><code>CURRENT-URL</code>
 *          <li><code>ADMIN-URL</code> 
 *          <li>If the configuration property
 *            <code>supplyRequestParameters</code> is <code>true</code> (default),
 *            <code>requestParameters</code> is supplied, which is a
 *            node list with elements like:<br> 
 *            <code>&lt;parameter name="requestParameterName"&gt;requestParameterValue&lt;/parameter&gt;</code>
 *      </ul>
 * </ul>
 */
public class ViewXslProvider implements ReferenceDataProvider {

    private Log logger = LogFactory.getLog(this.getClass());

    private Repository repository;
    private Service adminService;
    private boolean supplyRequestParameters = true;
    
    private boolean matchAdminServiceAssertions = false;
    
    /**
     * @param adminService The adminService to set.
     */
    public void setAdminService(Service adminService) {
        this.adminService = adminService;
    }

    /**
     * @param repository The repository to set.
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void referenceData(Map model, HttpServletRequest request)
            throws Exception {
        
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        String uri = requestContext.getResourceURI();

        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
       
        /* Getting resource from model instead of repository to get correct parameters
         for index-files. This shoudn't affect anything else... */
        Resource resource = null;
 
        if (model != null) {
            resource = (Resource) model.get("resource");
        }

        if (resource == null) {
        		resource = this.repository.retrieve(token, uri, true);
        }
        
        try {
            setXsltParameter(model, "creationTime", resource.getCreationTime());
            setXsltParameter(model, "lastModified", resource.getLastModified());
            setXsltParameter(model, "contentLanguage", resource.getContentLanguage());
            setXsltParameter(model, "PARENT-COLLECTION", resource.getParent());
            setXsltParameter(model, "CURRENT-URL", request.getRequestURL());
            setXsltParameter(model, "ADMIN-URL", 
                             this.adminService.constructLink(resource,principal, this.matchAdminServiceAssertions));


            if (this.supplyRequestParameters) {

                /* creating a nodeList with all request parameters */
                Document doc = new Document(new Element("root"));
                Element root =  doc.getRootElement();
                List children = root.getChildren();

                for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
                    String parameterName = (String) e.nextElement();
                
                    Element element = new Element("parameter");
                    element.setAttribute("name", parameterName);
                    element.setText(request.getParameter(parameterName));
                    children.add(element);
                }
                    
                DOMOutputter oupt = new DOMOutputter();
                NodeList nodeList = null;
                org.w3c.dom.Document domDoc = null;
                    
                domDoc = oupt.output(doc);
                nodeList =  domDoc.getDocumentElement().getChildNodes();
                setXsltParameter(model,"requestParameters", nodeList);
            }

        } catch (Throwable t) {
            this.logger.warn("Unable to provide complete XSLT reference data", t);
        }
    }

    
    protected final void setXsltParameter(Map model, String key, Object value) {
        if (key == null || value == null)
            return;
        
        Map parameters = (Map)model.get("xsltParameters");
        if (parameters == null) {
            parameters = new HashMap();
            model.put("xsltParameters", parameters);
        }
        parameters.put(key, value);
    }

    /**
     * @param supplyRequestParameters The supplyRequestParameters to set.
     */
    public void setSupplyRequestParameters(boolean supplyRequestParameters) {
        this.supplyRequestParameters = supplyRequestParameters;
    }

	/**
	 * @param matchAdminServiceAssertions The matchAdminServiceAssertions to set.
	 */
	public void setMatchAdminServiceAssertions(
			boolean matchAdminServiceAssertions) {
		this.matchAdminServiceAssertions = matchAdminServiceAssertions;
	}
}
