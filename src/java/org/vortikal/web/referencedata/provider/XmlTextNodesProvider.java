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
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.Xml;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;


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
     * @see org.vortikal.web.referencedata.provider.Provider#referenceData(java.util.Map,
     *      javax.servlet.http.HttpServletRequest)
     */
    public void referenceData(Map model, HttpServletRequest req) throws Exception {

        String currentUri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
            
        logger.debug("Will try to extract the xpath expressions '"
                + this.expressions + "' from resource '" + this.relativeURI
                + "' relative to current resource '" + currentUri
                + "'");

        Map data = new HashMap();
        model.put(this.modelKey, data);
        
        try {
            
            String docUri = URIUtil.getAbsolutePath(this.relativeURI, currentUri + "/");

            // fetch the xml-doc to work on
            Resource docResource = this.repository.retrieve(token, docUri, true);
            if (! "text/xml".equals(docResource.getContentType())) {
                logger.warn("Resource (" + docUri + ") is not xml");
                return;
            }

            // build DOM
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(this.repository.getInputStream(token, docUri, true));

            for (Iterator iter = xpathExpressions.keySet().iterator(); iter.hasNext();) {
                Object key = iter.next();
                XPath xpathExpression = (XPath) xpathExpressions.get(key);
                ArrayList nodes = Xml.getNodesByXPath(doc, xpathExpression);
            
                if (nodes != null && nodes.size() > 0)
                    data.put(key, nodes);
            }
             
            if (this.editService != null) {
                String url = this.editService.constructLink(docResource, principal);
                if (url != null)
                    data.put("editUrl", url);
            }
        } catch (Exception e) {
            // FIXME: better handling.. 
            logger.warn("Caught exception trying to extract xml element(s)", e);
        }
    }

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
}
