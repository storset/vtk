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
package org.vortikal.web.controller.graphics;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public class DisplayThumbnailController implements Controller, LastModified {
	
	private static final Logger log = Logger.getLogger(DisplayThumbnailController.class);
	
	private Repository repository;

    public long getLastModified(HttpServletRequest request) {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        try {
            Resource resource = this.repository.retrieve(
                securityContext.getToken(), 
                requestContext.getResourceURI(), true);
            return resource.getLastModified().getTime();
        } catch (Throwable t) {
            return -1;
        }
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();

        Resource image = this.repository.retrieve(token, uri, true);
        Property thumbnail = image.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.THUMBNAIL_PROP_NAME);
        
        if (thumbnail == null || StringUtils.isBlank(thumbnail.getBinaryMimeType())) {
            if (log.isDebugEnabled()) {
                String detailedMessage = thumbnail == null ? "no thumbnail found (null)" : "no mimetype set";
                log.debug("Cannot display thumbnail for image: " + uri + ", " + detailedMessage);
            }
        	response.sendRedirect(uri.toString());
        	return null;
        }
        
        InputStream in = null;
        try {
            
            ContentStream binaryStream = thumbnail.getBinaryStream();
            in = new BufferedInputStream(binaryStream.getStream());
            
            String mimetype = thumbnail.getBinaryMimeType();
            response.setContentType(mimetype);
            
            int length = (int) binaryStream.getLength();
            response.setContentLength(length);
            
            byte[] buf = new byte[length];
            int n = in.read(buf);
            
            OutputStream out = response.getOutputStream();
            out.write(buf, 0, n);
            out.flush();
            out.close();
            
        } finally {
            if (in != null) {
                in.close();
            }
        }
        
		return null;
	}

	@Required
	public void setRepository(Repository repository) {
		this.repository = repository;
	}
}
