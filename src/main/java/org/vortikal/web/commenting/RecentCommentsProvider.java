/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.commenting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Comment;
import org.vortikal.repository.Path;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class RecentCommentsProvider implements ReferenceDataProvider {

    private Repository repository;
    private boolean deepCommentsListing;
    private int maxComments = 100;
    private Service viewService;
    private Service resourceCommentsFeedService;
    private Service recentCommentsService;
    private boolean includeCommentsFromUnpublished;
    private PropertyTypeDefinition publishedDatePropDef;

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setDeepCommentsListing(boolean deepCommentsListing) {
        this.deepCommentsListing = deepCommentsListing;
    }

    public void setMaxComments(int maxComments) {
        if (maxComments <= 0) {
            throw new IllegalArgumentException("Number must be a positive integer");
        }
        this.maxComments = maxComments;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    public void setRecentCommentsService(Service recentCommentsService) {
        this.recentCommentsService = recentCommentsService;
    }

    public void setResourceCommentsFeedService(Service resourceCommentsFeedService) {
        this.resourceCommentsFeedService = resourceCommentsFeedService;
    }

    public void setIncludeCommentsFromUnpublished(boolean includeCommentsFromUnpublished) {
        this.includeCommentsFromUnpublished = includeCommentsFromUnpublished;
    }

    public void setPublishedDatePropDef(PropertyTypeDefinition publishedDatePropDef) {
        this.publishedDatePropDef = publishedDatePropDef;
    }

    @SuppressWarnings(value = { "unchecked" })
    public void referenceData(Map model, HttpServletRequest servletRequest) throws Exception {
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();

        Resource resource = repository.retrieve(token, uri, true);
        // If deepCommentListing is specified, always find the nearest
        // collection:
        if (!resource.isCollection() && this.deepCommentsListing) {
            uri = uri.getParent();
            resource = this.repository.retrieve(token, uri, true);
        }

        List<Comment> comments = repository.getComments(token, resource, this.deepCommentsListing, this.maxComments);

        Map<String, Resource> resourceMap = new HashMap<String, Resource>();
        Map<String, URL> commentURLMap = new HashMap<String, URL>();
        List<Comment> filteredComments = new ArrayList<Comment>();
        for (Comment comment : comments) {
            try {
                Resource r = this.repository.retrieve(token, comment.getURI(), true);
                // Don't include comments from resources that have no published
                // date
                Property publishedDate = null;
                if (publishedDatePropDef != null) {
                    publishedDate = r.getProperty(publishedDatePropDef);
                }
                if (!this.includeCommentsFromUnpublished && publishedDate == null) {
                    continue;
                }
                filteredComments.add(comment);
                resourceMap.put(r.getURI().toString(), r);
                URL commentURL = this.viewService.constructURL(r, principal);
                commentURLMap.put(comment.getID(), commentURL);
            } catch (Throwable t) {
            }
        }

        boolean commentsEnabled = resource.getAcl().getActions().contains(Privilege.ADD_COMMENT);

        URL baseCommentURL = null;
        try {
            baseCommentURL = this.viewService.constructURL(resource, principal);
        } catch (Exception e) {
        }

        URL feedURL = null;
        if (this.resourceCommentsFeedService != null) {
            try {
                feedURL = this.resourceCommentsFeedService.constructURL(resource, principal);
            } catch (Exception e) {
            }
        }

        URL recentCommentsURL = null;
        if (this.recentCommentsService != null) {
            try {
                recentCommentsURL = this.recentCommentsService.constructURL(resource, principal);
            } catch (Exception e) {
            }
        }

        model.put("resource", resource);
        model.put("principal", principal);
        model.put("comments", filteredComments);
        model.put("resourceMap", resourceMap);
        model.put("commentURLMap", commentURLMap);
        model.put("commentsEnabled", Boolean.valueOf(commentsEnabled));
        model.put("baseCommentURL", baseCommentURL);
        model.put("feedURL", feedURL);
        model.put("recentCommentsURL", recentCommentsURL);
    }

}
