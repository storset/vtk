/* Copyright (c) 2005, University of Oslo, Norway
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.Xml;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;


/**
 * 
 * @deprecated This class is only used by noticeboard. It is replaced by {@link no.uio.tavle.provider.ConfigurationTextNodesProvider} 
 *
 */


public class XmlTextNodesProvider
  implements ReferenceDataProvider, InitializingBean {
    private static Log logger = LogFactory.getLog(XmlTextNodesProvider.class);
    private Map expressions;
    private String modelKey;
    private Repository repository;
    private String relativeURI;
    private Service editService;

    private Map xpathExpressions = new HashMap();

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet()
      throws Exception {
        if (this.repository == null) {
            throw new BeanInitializationException("Repository not set");
        } else if (this.expressions == null) {
            throw new BeanInitializationException("No XPath expressions set");
        } else if (this.relativeURI == null) {
            throw new BeanInitializationException("RelativeURI not set");
        } else if (this.modelKey == null) {
            throw new BeanInitializationException("modelKey not set");
        } 

        for (Iterator iter = this.expressions.keySet().iterator(); iter.hasNext();) {
            Object key = iter.next();
            Object value = this.expressions.get(key);
            
            try {
                xpathExpressions.put(key, XPath.newInstance((String)value));
            } catch (Exception e) {
                throw new BeanInitializationException("Caught exception evaluating " +
                        "xpath expression with key '" + key + "'", e);
            }
        }
    }
    
    /**
     * As this provider is used by both view (no authentication needed) and admin beans
     * (authentication required), AuthenticationExceptions are explicitly handled (i.e. ignored)
     * @see org.vortikal.web.referencedata.ReferenceDataProvider#referenceData(java.util.Map,
     *      javax.servlet.http.HttpServletRequest)
     */
    public void referenceData(Map model, HttpServletRequest req) throws Exception {
        Map data = new HashMap();
        
        String currentUri = RequestContext.getRequestContext().getResourceURI();
        // 'token' and 'principal' will both be NULL if user is not authenticated
        String token = SecurityContext.getSecurityContext().getToken(); 
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        
        if (token == null && principal == null) {
            if ( logger.isDebugEnabled() )
                logger.debug("'token' and 'principal' are NULL which means current user is not authenticated");
        }
        
        if ( logger.isDebugEnabled() )
            logger.debug("Will try to extract the xpath expressions '"
                + this.expressions + "' from resource '" + this.relativeURI
                + "' relative to current resource '" + currentUri
                + "'");        
        
        // get complete path to xml-doc
        String docUri = URIUtil.getAbsolutePath(this.relativeURI, currentUri + "/");       
        // fetch the xml-doc to work on
        Resource docResource = this.repository.retrieve(token, docUri, true);
        
        if (! "text/xml".equals(docResource.getContentType())) {
            logger.warn("Resource (" + docUri + ") is not xml");
            return;
        }
        
        // build DOM and extract data (from XML docs) according to bean configuration
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(this.repository.getInputStream(token, docUri, true));
            
            for (Iterator iter = xpathExpressions.keySet().iterator(); iter.hasNext();) {
                Object key = iter.next();
                XPath xpathExpression = (XPath) xpathExpressions.get(key);
                ArrayList nodes = Xml.getNodesByXPath(doc, xpathExpression);
            
                if (nodes != null && nodes.size() > 0)
                    data.put(key, nodes);
            }
        } catch (AuthenticationException ae) {
            if( logger.isDebugEnabled() )
                logger.debug("AuthenticationException when fetching '" + docUri + "' from repository, " +
                             "as current user is not authenticated, was properly handled"); 
        } catch (Exception e) {
            logger.error( "Unhandled exception when trying to extract element(s) from the DOM", e);
        }
        
        
        // Attempt to generate link to edit view for noticeboard config file
        // (the link is only used by some admin views, hence users must be 
        // authenticated to access these webpages)
        try {
            if (this.editService != null) {
                /**
                 * FIXME: Er feil her (se log hos Eirik)
                 */
                String url = this.editService.constructLink(docResource, principal);
                if (url != null)
                    data.put("editUrl", url);
            }
        } catch (AuthenticationException ae) {
            if( logger.isDebugEnabled() )
                logger.debug("AuthenticationException when generating 'editUrl' link, as current user " +
                             "is not authenticated, was properly handled");
        } catch (ServiceUnlinkableException sue) {
            if( logger.isDebugEnabled() )
                logger.debug("ServiceUnlinkableException, as current user is not authorized to " +
                             "access resource '" + docResource.getURI() + "', was properly handled" );
        } catch (Exception e) {
            logger.error("Unhandled exception when trying to generating 'editUrl' link", e);
        }
        // populate model
        model.put(this.modelKey, data);
    }

    /**
     * Public bean property setter methods
     */
    public void setEditService(Service editService) {
        this.editService = editService;
    }

    public void setExpressions(Map expressions) {
        this.expressions = expressions;
    }

    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setRelativeURI(String relURI) {
        this.relativeURI = relURI;
    }

    protected Repository getRepository() {
        return repository;
    }
    
}
