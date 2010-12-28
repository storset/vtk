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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Comment;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class CommentsFeedController implements Controller {

    private Repository repository;
    private String viewName;
    private Service viewService;

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();
        Resource resource = repository.retrieve(token, uri, true);

        List<Comment> comments = repository.getComments(token, resource, true, 50);

        Map<String, Resource> resourceMap = new HashMap<String, Resource>();
        Map<String, URL> urlMap = new HashMap<String, URL>();
        resourceMap.put(resource.getURI().toString(), resource);
        urlMap.put(resource.getURI().toString(), this.viewService.constructURL(resource.getURI()));
        for (Comment comment : comments) {
            Resource r = this.repository.retrieve(token, comment.getURI(), true);
            resourceMap.put(r.getURI().toString(), r);
            urlMap.put(r.getURI().toString(), this.viewService.constructURL(r.getURI()));
        }
        URL selfURL = URL.create(request);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("resource", resource);
        model.put("principal", principal);
        model.put("comments", comments);
        model.put("resourceMap", resourceMap);
        model.put("urlMap", urlMap);
        model.put("selfURL", selfURL);

        return new ModelAndView(this.viewName, model);
    }

}
