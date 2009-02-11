/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.web.filter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class ExpiresCacheResponseFilter extends AbstractResponseFilter {

    private static final Set<String> DROPPED_HEADERS = new HashSet<String>();
    static {
        DROPPED_HEADERS.add("Expires");
        DROPPED_HEADERS.add("Cache-Control");
        DROPPED_HEADERS.add("Pragma");
        DROPPED_HEADERS.add("Last-Modified");
    }
    
    private Repository repository;
    private PropertyTypeDefinition expiresPropDef;
    private Service rootService;
    
    public HttpServletResponse filter(HttpServletRequest request,
            HttpServletResponse response) {

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();

        if (!requestContext.isInRepository()) {
            return response;
        }

        Service service = requestContext.getService();
        if (!service.isDescendantOf(this.rootService)) {
            return response;
        }
        
        boolean hasIndexFile = requestContext.getIndexFileURI() != null 
            && !requestContext.isIndexFile();
        if (hasIndexFile) {
            return response;
        }
        
        String token = securityContext.getToken();
        Path uri = requestContext.getResourceURI();

        try {
            Resource resource = this.repository.retrieve(token, uri, true);
            Property expiresProp = resource.getProperty(this.expiresPropDef);
            boolean anonymousReadable = 
                resource.isAuthorized(RepositoryAction.READ_PROCESSED, null);
            
            if (expiresProp != null && anonymousReadable) {
                long expiresMilliseconds = expiresProp.getLongValue() * 1000;
                Date expires = new Date(new Date().getTime() + expiresMilliseconds);
                return new ExpiresResponseWrapper(response, expires);
            }
        } catch (Throwable t) { }
        return response;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setExpiresPropDef(PropertyTypeDefinition expiresPropDef) {
        this.expiresPropDef = expiresPropDef;
    }

    public void setRootService(Service rootService) {
        this.rootService = rootService;
    }

    private class ExpiresResponseWrapper extends HttpServletResponseWrapper {

        private HttpServletResponse response;
        
        public ExpiresResponseWrapper(HttpServletResponse response, Date expires) {
            super(response);
            this.response = response;
            this.response.setDateHeader("Expires", expires.getTime());
        }

        @Override
        public void addDateHeader(String name, long date) {
            if (DROPPED_HEADERS.contains(name)) {
                return;
            }
            super.addDateHeader(name, date);
        }

        @Override
        public void addHeader(String name, String value) {
            if (DROPPED_HEADERS.contains(name)) {
                return;
            }
            super.addHeader(name, value);
        }

        @Override
        public void addIntHeader(String name, int value) {
            if (DROPPED_HEADERS.contains(name)) {
                return;
            }
            super.addIntHeader(name, value);
        }

        @Override
        public boolean containsHeader(String name) {
            if (DROPPED_HEADERS.contains(name)) {
                return false;
            }
            return super.containsHeader(name);
        }

        @Override
        public void setDateHeader(String name, long date) {
            if (DROPPED_HEADERS.contains(name)) {
                return;
            }
            super.setDateHeader(name, date);
        }

        @Override
        public void setHeader(String name, String value) {
            if (DROPPED_HEADERS.contains(name)) {
                return;
            }
            super.setHeader(name, value);
        }

        @Override
        public void setIntHeader(String name, int value) {
            if (DROPPED_HEADERS.contains(name)) {
                return;
            }
            super.setIntHeader(name, value);
        }
    }

}
