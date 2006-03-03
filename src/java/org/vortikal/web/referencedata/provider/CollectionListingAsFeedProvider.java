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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.ResourceSorter;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;


public class CollectionListingAsFeedProvider implements ReferenceDataProvider {

    //public static final String DEFAULT_SORT_BY_PARAMETER = "name";
    public static final String DEFAULT_SORT_BY_PARAMETER = "last-modified";

    private static final Log logger = LogFactory.getLog(CollectionListingAsFeedProvider.class);
    
    private static final Set supportedResourceColumns = 
        new HashSet(Arrays.asList(new String[] {
                                      "name", 
                                      "content-length", 
                                      "last-modified",
                                      "locked", 
                                      "content-type", 
                                      "owner" }));
    
    private Repository repository;
    private Service browsingService;
    //private boolean retrieveForProcessing = false;
    private Set contentTypeFilter;
    private Pattern contentTypeRegexpFilter;
    

    public void setBrowsingService(Service browsingService) {
        this.browsingService = browsingService;
    }
    
    private String[] childInfoItems = 
        new String[] {DEFAULT_SORT_BY_PARAMETER, "content-length", "last-modified"};
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setChildInfoItems(String[] childInfoItems)  {
        this.childInfoItems = childInfoItems;
    }

    public void setContentTypeFilter(Set contentTypeFilter) {
        this.contentTypeFilter = contentTypeFilter;
    }
    
    public void setContentTypeRegexpFilter(String contentTypeRegexpFilter) {
        if (contentTypeRegexpFilter != null) {
            this.contentTypeRegexpFilter = Pattern.compile(contentTypeRegexpFilter);
        }
    }
    
    public void afterPropertiesSet() {
        if (repository == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'repository' must be set");
        }

        if (browsingService == null) {
            throw new BeanInitializationException(
                    "JavaBean Property 'browsingService' must be set");    
        }

        if (this.contentTypeRegexpFilter != null && this.contentTypeFilter != null) {
            throw new BeanInitializationException(
                "JavaBean properties 'contentTypeRegexpFilter' and "
                + "'contentTypeFilter' cannot both be specified");
        }
    }

    
    public void referenceData(Map model, HttpServletRequest request)
        throws Exception {

        Map feedModel = new HashMap();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Resource[] children = null;

        Resource resource = repository.retrieve(token, uri, true);
        if (!resource.isCollection()) {
            // Can't do anything unless resource is a collection
            return;
        }

        children = repository.listChildren(token, uri, true);
        children = filterChildren(children);
        feedModel.put("resources", children);
   
        String feedTitleText = resource.getName();
        // Set format and feed header info (title, link, description)
        if (feedTitleText.length() > 40) {
            logger.warn("Title of the feed cannot exceed 40 characters");
            feedTitleText = feedTitleText.substring(0, 35) + "...";
        }
        feedModel.put("title", feedTitleText);
        
        String description;
        if (!"".equals(feedTitleText)) {
            description = feedTitleText;
        } else {
            description = "Feed for resource without title";
        }
        
        feedModel.put("description", description);
        
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        String resourceUrl = browsingService.constructLink(resource, principal);
        feedModel.put("url", resourceUrl);
        
        model.put("feedModel", feedModel);
        

    }

    private Resource[] filterChildren(Resource[] children) {

        if (this.contentTypeFilter == null && this.contentTypeRegexpFilter == null) {
            return children;
        }
        
        List filteredChildren = new ArrayList();
        for (int i = 0; i < children.length; i++) {
                
            if (this.contentTypeFilter != null) {
                if (this.contentTypeFilter.contains(children[i].getContentType())) {
                    filteredChildren.add(children[i]);
                }
            } else {
                Matcher m = this.contentTypeRegexpFilter.matcher(
                    children[i].getContentType());
                if (m.matches()) {
                    filteredChildren.add(children[i]);
                }
            }
        }
        return (Resource[]) filteredChildren.toArray(
            new Resource[filteredChildren.size()]);
    }
    

    private void sortChildren(Resource[] children, String sortBy, boolean invert) {
        int order = ResourceSorter.ORDER_BY_NAME;

        if ("content-length".equals(sortBy)) {
            order = ResourceSorter.ORDER_BY_FILESIZE;
        }
        if ("last-modified".equals(sortBy)) {
            order = ResourceSorter.ORDER_BY_DATE;
        }
        if ("locked".equals(sortBy)) {
            order = ResourceSorter.ORDER_BY_LOCKS;
        }
        if ("content-type".equals(sortBy)) {
            order = ResourceSorter.ORDER_BY_CONTENT_TYPE;
        }
        if ("owner".equals(sortBy)) {
            order = ResourceSorter.ORDER_BY_OWNER;
        }
        
        ResourceSorter.sort(children, order, invert);
    }
    
}
