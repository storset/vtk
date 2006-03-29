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
package org.vortikal.web.controller.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.RepositoryAssertion;
import org.vortikal.web.service.Service;



/**
 * Controller for recursively walking a directory starting from the
 * current directory tree and creating a URL to each ancestor, with
 * optional filtering based on {@link RepositoryAssertion assertions}.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>repository</code> - the {@link Repository content
 *   repository}
 *   <li><code>service</code> - the {@link Service} to use for URL
 *   generation
 *   <lI><code>viewName</code> - the name of the view to return
 *   <lI><code>view</code> - the {@link View}, specified directly
 *   (overrides <code>viewName</code>)
 *   <lI><code>modelName</code> - the name to use for the list of URLs
 *   in the model
 *   <lI><code>assertions</code> - an optional array of {@link
 *   RepositoryAssertion assertions} to filter which resources that
 *   are included in the resulting list.
 * </ul>
 *
 * <p>Model data created:
 * <ul>
 *   <li>A list of URLs, under the name specified by
 *   <code>modelName</code>.
 * </ul>
 */
public class RecursiveResourceListController implements Controller, InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private Repository repository;
    private Service service;
    private String viewName;
    private View view;
    private String modelName;
    private RepositoryAssertion[] assertions;
    
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setService(Service service) {
        this.service = service;
    }


    public void setViewName(String viewName) {
        this.viewName = viewName;
    }


    public void setView(View view) {
        this.view = view;
    }


    public void setModelName(String modelName) {
        this.modelName = modelName;
    }


    public void setAssertions(RepositoryAssertion[] assertions) {
        this.assertions = assertions;
    }


    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' must be specified");
        }
        if (this.service == null) {
            throw new BeanInitializationException(
                "JavaBean property 'service' must be specified");
        }
        if (this.viewName == null && this.view == null) {
            throw new BeanInitializationException(
                "At least one of JavaBean properties 'viewName' or 'view' must be specified");
        }
        if (this.modelName == null) {
            throw new BeanInitializationException(
                "JavaBean property 'modelName' must be specified");
        }
    }
    


    public ModelAndView handleRequest(HttpServletRequest httpServletRequest,
                                      HttpServletResponse httpServletResponse) throws Exception {
        
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();

        Resource resource = this.repository.retrieve(token, uri, true);
        List ancestors = listChildrenRecursively(resource, token, principal);

        Map model = new HashMap();
        model.put(this.modelName, ancestors);
        
        ModelAndView mv = this.view != null ?
            new ModelAndView(this.view, model) :
            new ModelAndView(this.viewName, model);
        return mv;
    }
  


    private List listChildrenRecursively(Resource resource, String token, Principal principal) {
        List ancestors = new ArrayList();

        Stack s = new Stack();
        s.push(resource);
        
        while (s.size() > 0) {
            
            Resource r = (Resource) s.pop();
            boolean match = true;
            if (this.assertions != null) {
                for (int i = 0; i < this.assertions.length; i++) {
                    if (!this.assertions[i].matches(r, principal)) {
                        match = false;
                        break;
                    }
                }
            }

            if (match) {
                try {
                    String url = this.service.constructLink(r, principal);
                    ancestors.add(url);
                } catch (Exception e) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Unable to construct link to resource " + r, e);
                    }
                }
            }

            if (r.isCollection()) {
                try {
                    Resource[] children =
                        this.repository.listChildren(
                            token, r.getURI(), true);

                    for (int i = 0; i < children.length; i++) {
                        s.push(children[i]);
                    }
                } catch (Exception e) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Caught exception while listing ancestors", e);
                    }
                }
            } 
        }
        return ancestors;
    }

}
