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
package org.vortikal.web.commenting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Comment;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;


public class CommentsProvider implements ReferenceDataProvider {
    
    private Repository repository;
    private Service postCommentService;
    private Service deleteCommentService;
    private Service deleteAllCommentsService;
    private Service loginService;
    private Service resourceCommentsFeedService;
    private String formSessionAttributeName;
    

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required public void setPostCommentService(Service postCommentService) {
        this.postCommentService = postCommentService;
    }
    

    @Required public void setDeleteCommentService(Service deleteCommentService) {
        this.deleteCommentService = deleteCommentService;
    }
    
    @Required public void setDeleteAllCommentsService(Service deleteAllCommentsService) {
        this.deleteAllCommentsService = deleteAllCommentsService;
    }
    
    public void setResourceCommentsFeedService(Service resourceCommentsFeedService) {
        this.resourceCommentsFeedService = resourceCommentsFeedService;
    }
    
    public void setLoginService(Service loginService) {
        this.loginService = loginService;
    }
    
    public void setFormSessionAttributeName(String formSessionAttributeName) {
        this.formSessionAttributeName = formSessionAttributeName;
    }
    
    @SuppressWarnings(value={"unchecked"}) 
    public void referenceData(Map model, HttpServletRequest servletRequest) throws Exception {
        
        if (this.formSessionAttributeName != null) {

            if (servletRequest.getSession(false) != null) {
                Map map = (Map) servletRequest.getSession(false).getAttribute(
                    this.formSessionAttributeName);
                if (map != null) {
                    model.put("form", map.get("form"));
                    model.put("errors", map.get("errors"));
                }
            }
        }

        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();
        Service currentService = RequestContext.getRequestContext().getService();

        model.put("principal", principal);

        Resource resource = repository.retrieve(token, uri, true);
        List<Comment> comments = repository.getComments(token, resource);
        model.put("comments", comments);

        boolean commentsEnabled =
            resource.getAcl().getActions().contains(RepositoryAction.ADD_COMMENT);
        model.put("commentsEnabled", commentsEnabled);
        model.put("repositoryReadOnly", this.repository.isReadOnly());

        Map<String, URL> deleteCommentURLs = new HashMap<String, URL>();

        URL baseDeleteURL = null;
        try {
            baseDeleteURL = this.deleteCommentService.constructURL(resource, principal);
        } catch (Exception e) { }

        for (Comment c: comments) {
            if (baseDeleteURL != null) {
                URL clone = new URL(baseDeleteURL);
                clone.addParameter("comment-id", String.valueOf(c.getID()));
                deleteCommentURLs.put(String.valueOf(c.getID()), clone);
            }
        }
        model.put("deleteCommentURLs", deleteCommentURLs);

        try {
            URL deleteAllCommentsURL = this.deleteAllCommentsService.constructURL(resource, principal);
            model.put("deleteAllCommentsURL", deleteAllCommentsURL);
        } catch (Exception e) { }
        

        URL baseCommentURL = null;
        try {
            baseCommentURL = currentService.constructURL(resource, principal);
        } catch (Exception e) { }
        model.put("baseCommentURL", baseCommentURL);

        try {
            URL postCommentURL = this.postCommentService.constructURL(resource, principal);
            model.put("postCommentURL", postCommentURL);
        } catch (Exception e) { }

        if (this.loginService != null && principal == null) {
            try {
                URL loginURL = this.loginService.constructURL(resource, principal);
                model.put("loginURL", loginURL);
            } catch (Exception e) { }
        }

        if (this.resourceCommentsFeedService != null) {
            
            // Only provide feed subscription link if resource is READ for ALL.
            if (this.repository.isAuthorized(resource, RepositoryAction.READ, PrincipalFactory.ALL)) {
                try {
                    URL feedURL = this.resourceCommentsFeedService.constructURL(resource, principal);
                    model.put("feedURL", feedURL);
                } catch (Exception e) { }
            }
        }
    }

    protected Map<String, String> getParameterDescriptionsInternal() {
        return java.util.Collections.<String, String>emptyMap();
    }

}
