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
package org.vortikal.web.controller.repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Ace;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class CreateCourseController extends SimpleFormController {
	private static Log logger = LogFactory.getLog(CreateCourseController.class);
    private Repository repository = null;
    private String courseTemplateUri = "";
    private String coursesToBeCreatedUri = "";
    private String coursePermissionsUri = "";
    
	public void setRepository(Repository repository) {
        this.repository = repository;
    }

	public void setCourseTemplateUri(String courseTemplateUri) {
		this.courseTemplateUri = courseTemplateUri;
	}
	
	public void setCoursesToBeCreatedUri(String coursesToBeCreatedUri) {
		this.coursesToBeCreatedUri = coursesToBeCreatedUri;
	}
	
	public void setCoursePermissionsUri(String coursePermissionsUri) {
		this.coursePermissionsUri = coursePermissionsUri;
	}
    
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        Resource resource = repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        String url = service.constructLink(resource, securityContext.getPrincipal());
         
        CreateCourseCommand command =
            new CreateCourseCommand(url);
        return command;
    }


    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        CreateCourseCommand createCourseCommand =
            (CreateCourseCommand) command;
        if (createCourseCommand.getCancelAction() != null) {
            createCourseCommand.setDone(true);
            return;
        }
        String uri = requestContext.getResourceURI();
        Principal principal = securityContext.getPrincipal();
        String token = securityContext.getToken();

        try {     	
        
        String type = Lock.LOCKTYPE_EXCLUSIVE_WRITE;
        repository.lock(token, coursesToBeCreatedUri, type, principal.getQualifiedName(), "0", 5, null);
            
        } catch (Exception e) {
        		if (logger.isDebugEnabled()) {
				logger.debug("The courses to be created file is locked. Aborting new course folder creation.");    		
	        	}
        		return;
        }
        
        // Constructing the URI to the new course collection
        String newURI = uri;
        if (!"/".equals(uri)) newURI += "/";
        newURI += createCourseCommand.getName();

        // Creating the new course-folder based on the configured template
        repository.copy(token, courseTemplateUri, newURI, "infinity", false, false);

        // Trying to find and store configured permission for course-folder
        Ace[] newAcl = resolveACL(token, coursePermissionsUri + 
        		uri.substring(uri.lastIndexOf("/emner") + "/emner".length()));
        
        if (newAcl != null) {
        		if (logger.isDebugEnabled()) {
				logger.debug("Found Acl on a template, trying to store it on the created folder");    		
	        	}
        		repository.storeACL(token, newURI, newAcl);
        }
        
        // Appending the url of the new course-folder to a file for processing by 
        // script (that updates mapping.xml)
        
        InputStream stream = repository.getInputStream(token, coursesToBeCreatedUri, false);

        byte[] bytes = StreamUtil.readInputStream(stream);
        
        String newContent = new String(bytes) + newURI + "\n";
        
        repository.storeContent(token, coursesToBeCreatedUri, 
                new ByteArrayInputStream(newContent.getBytes()));
        
        repository.unlock(token, coursesToBeCreatedUri, null);
        
        createCourseCommand.setDone(true);
    }
    
    
    protected Ace[] resolveACL(String token, String permissionsUri) {
    		Ace[] a = null;
    		
    		if (logger.isDebugEnabled()) {
				logger.debug("Trying to get ACL from the institute-level: " + permissionsUri);    		
	        	}

    		try {
   			a = repository.getACL(token, permissionsUri);
   		} catch (Exception e) { 
   			logger.info("Problems retrieving folder on institute-level" + e.getClass().getName());
   		}
   		
   		if (logger.isDebugEnabled()) {
			logger.debug("Trying to get ACL from the faculty-level: " + permissionsUri);    		
        	}

    		if (a == null) {
    			System.out.println("### permissionsUri (fakultet): " + 
        				permissionsUri.substring(0, permissionsUri.lastIndexOf("/")));
    			try {
    				a =  repository.getACL(token, permissionsUri.substring(0, permissionsUri.lastIndexOf("/")));
    			} catch (Exception e) {
    				logger.info("Problems retrieving folder on institute-level" + e.getClass().getName());
    			}
    		}
    		
    		return a;
    }
    
}

