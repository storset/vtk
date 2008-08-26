/* Copyright (c) 2007, University of Oslo, Norway
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.ResourcePropertyComparator;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;
import org.vortikal.web.service.URL;

public class SimpleCollectionListingProvider implements ReferenceDataProvider {

    private String modelName = "collectionListing";

    private Repository repository;

    private Service displayService;

    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition lastModifiedPropDef;
    

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required public void setDisplayService(Service displayService) {
        this.displayService = displayService;
    }

    @Required public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }

    @Required public void setLastModifiedPropDef(PropertyTypeDefinition lastModifiedPropDef) {
        this.lastModifiedPropDef = lastModifiedPropDef;
    }
    

    @SuppressWarnings("unchecked")
    public void referenceData(Map model, HttpServletRequest request) throws Exception {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();

        Resource[] children = this.repository.listChildren(token, uri, true);
        List<Resource> collections = new ArrayList<Resource>();
        List<Resource> files = new ArrayList<Resource>();
        Map<Path, URL> urls = new HashMap<Path, URL>();
        
        for (Resource child: children) {
            try {
                URL url = this.displayService.constructURL(child, principal);
                urls.put(child.getURI(), url);
                if (child.isCollection()) {
                    collections.add(child);
                } else {
                    files.add(child);
                }
            } catch (ServiceUnlinkableException e) { }
        }

        Locale locale = new org.springframework.web.servlet.support.RequestContext(request).getLocale();
        Collections.sort(collections, new ResourcePropertyComparator(this.titlePropDef, false, locale));

        Map<String, URL> sortURLs = new HashMap<String, URL>();
        URL sortByTitleURL = URL.create(request);
        sortByTitleURL.removeParameter("sort-by");
        sortByTitleURL.addParameter("sort-by", "title");
        
        URL sortByLastModifiedURL = URL.create(request);
        sortByLastModifiedURL.removeParameter("sort-by");
        sortByLastModifiedURL.addParameter("sort-by", "last-modified");

        sortURLs.put("title", sortByTitleURL);
        sortURLs.put("last-modified", sortByLastModifiedURL);


        String sortBy = request.getParameter("sort-by");
        if ("last-modified".equals(sortBy)) {
            Collections.sort(files, new ResourcePropertyComparator(this.lastModifiedPropDef, true));
        } else {
            sortBy = "title";
            Collections.sort(files, new ResourcePropertyComparator(this.titlePropDef, false, locale));
        }

        Map<String, Object> subModel = new HashMap<String, Object>();
        subModel.put("collections", collections);
        subModel.put("files", files);
        subModel.put("urls", urls);
        subModel.put("sortURLs", sortURLs);
        subModel.put("sortProperty", sortBy);

        model.put(this.modelName, subModel);
    }
    
}
