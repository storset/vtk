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
package vtk.resourcemanagement.view.tl;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import vtk.text.tl.Context;
import vtk.text.tl.Symbol;
import vtk.text.tl.expr.Function;
import vtk.web.RequestContext;
import vtk.web.service.URL;

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
        result.put("headers", headersToMap(requestContext.getServletRequest()));
        result.put("principal", requestContext.getPrincipal());
        result.put("view-unauthenticated", requestContext.isViewUnauthenticated());
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
    
    @SuppressWarnings("unchecked")
    private Map<Object, Object> headersToMap(HttpServletRequest request) {
        Map<Object, Object> result = new HashMap<Object, Object>();
        for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            Enumeration<String> values = request.getHeaders(name);
            List<String> valueList = new ArrayList<String>();
            while (values.hasMoreElements()) valueList.add(values.nextElement());
            
            Map<String, Object> entry = new HashMap<String, Object>();
            if (!valueList.isEmpty()) {
                entry.put("values", valueList);
                entry.put("value", valueList.get(0));
            }
            result.put(name, entry);
        }
        return result;
    }

}
