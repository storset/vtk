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
package org.vortikal.web;

import org.vortikal.web.service.Service;



/**
 * Request context. Lives troughout one request, and contains the
 * current service, the requested URI, and if retrievable, the
 * requested resource. The request context can be obtained from
 * application code in the following way:
 *
 * <pre>RequestContext requestContext = RequestContext.getRequestContext();</pre>
 *
 * TODO: add a <code>reload()</code> method, to be used in cases where
 * application code modifies the current resource?
 * 
 * @version $Id$
 */
public class RequestContext {

    private static ThreadLocal threadLocal = new ThreadLocal();
    
    private final Service service;
    private final String resourceURI;
    
    
    public RequestContext(Service service, String resourceURI) {
        this.service = service;
        this.resourceURI = resourceURI;
    }
    
    public static void setRequestContext(RequestContext requestContext) {
        threadLocal.set(requestContext);
    }
    
    public static RequestContext getRequestContext() {
        return (RequestContext) threadLocal.get();
    }
    
    /**
     * @return Returns the service.
     */
    public Service getService() {
        return service;
    }


    /**
     * @return Returns URI of the requested resource.
     */
    public String getResourceURI() {
        return resourceURI;
    }
    

    
    public String toString() {
        StringBuffer sb = new StringBuffer(getClass().getName());
        sb.append(": ");
        sb.append("uri = ").append(this.service.getName());
        sb.append(",service = ").append(this.service.getName());
        return sb.toString();
    }
    
}
