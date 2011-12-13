/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.web.actions.versioning;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.Revision;
import org.vortikal.repository.store.Revisions;
import org.vortikal.security.Principal;
import org.vortikal.security.Principal.Type;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class DisplayRevisionsController implements Controller {

    private Service viewService = null;
    private Service deleteService = null;
    private Service restoreService = null;
    private String viewName = null;
    private PrincipalFactory principalFactory;
    
    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();
        
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Principal principal = requestContext.getPrincipal();
        Path uri = requestContext.getResourceURI();
        List<Revision> revisions = requestContext.getRepository().getRevisions(token, uri);
        
        Object workingCopy = null;
        Revision latest = null;
        List<Object> allRevisions = new ArrayList<Object>();
        List<Object> regularRevisions = new ArrayList<Object>();

        for (Revision revision: revisions) {
            Map<String, Object> rev = new HashMap<String, Object>();
            rev.put("id", revision.getID());
            rev.put("name", revision.getName());
            rev.put("timestamp", revision.getTimestamp());
            rev.put("principal", this.principalFactory.getPrincipal(revision.getUid(), Type.USER));
            rev.put("acl", revision.getAcl());
            rev.put("checksum", revision.getChecksum());
            rev.put("changeAmount", revision.getChangeAmount());
            allRevisions.add(rev);
            
            if (revision.getType() == Revision.Type.WORKING_COPY) {
                workingCopy = rev;
            } else {
                if (latest == null) {
                    latest = revision;
                }
                regularRevisions.add(rev);
            }

            Resource resource = null;
            try {
                resource = repository.retrieve(token, uri, false, revision);
            } catch (Throwable t) {
                continue;
            }
            URL displayURL = null;
            try {
                displayURL = this.viewService.constructURL(resource, principal);
            } catch (Throwable t) { }
            URL deleteURL = null;
            try {
                deleteURL = this.deleteService.constructURL(resource, principal);
            } catch (Throwable t) { }
            URL restoreURL = null;
            try {
                restoreURL = this.restoreService.constructURL(resource, principal);
            } catch (Throwable t) { }
            
            if (displayURL != null) {
                rev.put("displayURL", new URL(displayURL)
                   .setParameter("revision", revision.getName()));
            }
            if (deleteURL != null) {
                rev.put("deleteURL", new URL(deleteURL)
                   .setParameter("revision", revision.getName()));
            }
            if (restoreURL != null) {
                rev.put("restoreURL", new URL(restoreURL)
                   .setParameter("revision", revision.getName()));
            }
        }
        Resource current = repository.retrieve(token, uri, false);
        URL displayURL = null;
        try {
            displayURL = this.viewService.constructURL(current, principal);
        } catch (Throwable t) { }
        if (latest != null) {
            try {
                InputStream s1 = repository.getInputStream(token, uri, true, latest);
                InputStream s2 = repository.getInputStream(token, uri, true);
                Integer changeAmount = Revisions.changeAmount(s1, s2);
                model.put("resourceChangeAmount", changeAmount);
            } catch (Throwable t) { }
        }

        model.put("resource", current);
        model.put("displayURL", displayURL);
        
        model.put("workingCopy", workingCopy);
        model.put("allRevisions", allRevisions);
        model.put("regularRevisions", regularRevisions);

        return new ModelAndView(this.viewName, model);
        
    }

    @Required
    public void setViewName(String viewName) {
        if (viewName == null || "".equals(viewName.trim())) {
            throw new IllegalArgumentException("viewName must be specified");
        }
        this.viewName = viewName;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    @Required
    public void setDeleteService(Service deleteService) {
        this.deleteService = deleteService;
    }

    @Required
    public void setRestoreService(Service restoreService) {
        this.restoreService = restoreService;
    }
    
    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

}
