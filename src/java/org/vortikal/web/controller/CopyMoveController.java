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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


/**
 * A controller that copies (or moves) resources from one folder to another 
 *
 * <p>Description:
 *  
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the content repository
 *   <li><code>viewService</code> - the service for which to construct a viewURL
 * </ul>
 *
 * <p>Model data published:
 * <ul>
 * <li><code>browseURL</code>: a string with the url to the linked resource
 * <li><code>editfield</code>: id of the form-element to put the url in
 * </ul>
 */

public class CopyMoveController implements Controller {

    private String viewName = "DEFAULT_VIEW_NAME";    
    private Repository repository = null;

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	public final void setRepository(final Repository newRepository) {
        this.repository = newRepository;
    }
    
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
    
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        RequestContext requestContext = RequestContext.getRequestContext();

        String uri = requestContext.getResourceURI();
        Resource resource = repository.retrieve(token, uri, false);

		Map model = new HashMap();

		List filesToBeCopied = new ArrayList();
		
		Enumeration e = request.getParameterNames();

		while (e.hasMoreElements()) {
        		String name = (String) e.nextElement();

        		// Need to replace this with a more proper check...
        		if (name.startsWith("/")) {
        			filesToBeCopied.add(name);
        		}
        	}	
		
		model.put("filesToBeCopied", filesToBeCopied);
		
		return new ModelAndView("copyMove", model);
    	}

}

