/* Copyright (c) 2006, 2008, University of Oslo, Norway
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;

/**
 * Directory listing ass feed model builder. Creates a model map 'feedModel' with a list of
 * children and associated data for the requested collection.
 * <p>
 * Configurable properties:
 * </p>
 * <ul>
 * <li><code>browsingService</code> - The service used for constructing the link to the resource
 * in the list "resources"</li>
 * </ul>
 */

public class CollectionListingAsFeedProvider implements ReferenceDataProvider {

    private static final int RSS_TITLE_MAX_LENGTH = 40;

    public static final String DEFAULT_SORT_BY_PARAMETER = "last-modified";

    private static final Log logger = LogFactory.getLog(CollectionListingAsFeedProvider.class);
        
    private Repository repository;

    private Service browsingService;

    private Set<String> contentTypeFilter;
    private Pattern contentTypeRegexpFilter;
    

    @Required
    public void setBrowsingService(Service browsingService) {
        this.browsingService = browsingService;
    }
    
    private String truncationString = "...";
    

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setContentTypeFilter(Set<String> contentTypeFilter) {
        this.contentTypeFilter = contentTypeFilter;
    }
    
    public void setContentTypeRegexpFilter(String contentTypeRegexpFilter) {
        if (contentTypeRegexpFilter != null) {
            this.contentTypeRegexpFilter = Pattern.compile(contentTypeRegexpFilter);
        }
    }
    
    public void afterPropertiesSet() {
        if (this.contentTypeRegexpFilter != null && this.contentTypeFilter != null) {
            throw new BeanInitializationException(
                "JavaBean properties 'contentTypeRegexpFilter' and "
                + "'contentTypeFilter' cannot both be specified");
        }
    }

    
    @SuppressWarnings("unchecked")
    public void referenceData(Map model, HttpServletRequest request)
        throws Exception {

        Map<String, Object> feedModel = new HashMap<String, Object>();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Resource[] children = null;

        Resource resource = this.repository.retrieve(token, uri, true);
        if (!resource.isCollection()) {
            // Can't do anything unless resource is a collection
            return;
        }

        children = this.repository.listChildren(token, uri, true);
        children = filterChildren(children);
        feedModel.put("resources", children);
   
        String feedTitleText = resource.getTitle();
        // Set format and feed header info (title, link, description)
        if (feedTitleText.length() > RSS_TITLE_MAX_LENGTH) {
            logger.debug("Title of the feed cannot exceed " + RSS_TITLE_MAX_LENGTH + " characters." +
            		"Title is \'" + feedTitleText + "\'");

            feedTitleText = feedTitleText.substring(0, RSS_TITLE_MAX_LENGTH
                    - truncationString.length())
                    + truncationString;
        }
        feedModel.put("title", feedTitleText);
        
        String description = feedTitleText;

        if ("".equals(description)) {
            description = "Feed for resource without title";
        }
        
        feedModel.put("description", description);
        
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        String resourceUrl = this.browsingService.constructLink(resource, principal);
        feedModel.put("url", resourceUrl);
        
        model.put("feedModel", feedModel);

    }

    private Resource[] filterChildren(Resource[] children) {

        if (this.contentTypeFilter == null && this.contentTypeRegexpFilter == null) {
            return children;
        }
        
        List<Resource> filteredChildren = new ArrayList<Resource>();

        for (Resource resource: children) {
            if (this.contentTypeFilter != null) {
                if (this.contentTypeFilter.contains(resource.getContentType())) {
                    filteredChildren.add(resource);
                }
            } else {
                Matcher m = this.contentTypeRegexpFilter.matcher(
                    resource.getContentType());
                if (m.matches()) {
                    filteredChildren.add(resource);
                }
            }
        }
        return filteredChildren.toArray(new Resource[filteredChildren.size()]);
    }
    
}
