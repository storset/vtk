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
package org.vortikal.web.controller;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;


/**
 * Controller that provides a preconfigured resource and its input
 * stream in the model, regardless of which resource is requested by
 * the client.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the repository is required</li>
 *   <li><code>resourceURI</code> - the URI of the resource to display
 *   <li><code>viewName</code> - name of the returned view. The
 *       default value is <code>displayResource</code></li>
 * </ul>
 * </p>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>resource</code> - the resource object</li>
 *   <li><code>resourceStream</code> - the input stream of the
 *       resource. (Note: be sure to couple this controller with a
 *       view that closes this stream)</li>
 * </ul>
 */
public class DisplayStaticResourceController implements Controller {

    public static final String DEFAULT_VIEW_NAME = "displayResource";
    
    private static Log logger = LogFactory.getLog(DisplayStaticResourceController.class);

    private Repository repository;
    private String resourceURI;
    private String viewName = DEFAULT_VIEW_NAME;


    public void setResourceURI(String resourceURI) {
        this.resourceURI = resourceURI;
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();


        String token = securityContext.getToken();
        Map model = new HashMap();
        Resource resource = repository.retrieve(token, this.resourceURI, true);

        if (resource.isCollection()) {
            throw new RuntimeException(
                "Cannot display a collection resource");
        }

        InputStream stream = repository.getInputStream(token, this.resourceURI, true);

        model.put("resource", resource);
        model.put("resourceStream", stream);

        return new ModelAndView(this.viewName, model);
    }

}
