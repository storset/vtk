/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.resourcemanagement.view.tl;

import java.util.HashMap;
import java.util.Map;

import org.vortikal.security.SecurityContext;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Symbol;
import org.vortikal.text.tl.expr.Function;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;

public class RequestContextFunction extends Function {

    public RequestContextFunction(Symbol symbol) {
        super(symbol, 0);
    }

    @Override
    public Object eval(Context ctx, Object... args) {
        Map<Object, Object> result = new HashMap<Object, Object>();
        RequestContext requestContext = RequestContext.getRequestContext();
        result.put("current-collection", requestContext.getCurrentCollection());
        result.put("index-file", requestContext.isIndexFile());
        result.put("resource-uri", requestContext.getResourceURI());
        result.put("collection", requestContext.getCurrentCollection().equals(
                requestContext.getResourceURI()));
        URL url = URL.create(requestContext.getServletRequest());
        result.put("request-url", urlToMap(url));
        result.put("principal", SecurityContext.getSecurityContext().getPrincipal());
        return result;
    }
    
    private Map<Object, Object> urlToMap(URL url) {
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("base", url.getBase());
        map.put("protocol", url.getProtocol());
        map.put("host", url.getHost());
        map.put("port", url.getPort());
        map.put("path", url.getPath());
        
        Map<Object, Object> parameters = new HashMap<Object, Object>();
        for (String name: url.getParameterNames()) {
            parameters.put(name, url.getParameter(name));
        }
        map.put("parameters", parameters);
        map.put("full", url.toString());
        return map;
    }

}
