/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.decorating;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.text.html.HtmlPage;

public class DecoratorRequestImpl implements DecoratorRequest {

    private HtmlPage html;

    private HttpServletRequest servletRequest;

    private Map<Object, Object> mvcModel;
    
    private Map<String, Object> decoratorParameters;
    
    private String doctype;

    private Locale locale;
    
    public DecoratorRequestImpl(HtmlPage html,
                                HttpServletRequest servletRequest,
                                Map<Object, Object> mvcModel,
                                Map<String, Object> decoratorParameters,
                                String doctype, Locale locale) {
        this.html = html;
        this.servletRequest = servletRequest;
        this.mvcModel = mvcModel;
        this.decoratorParameters = decoratorParameters;
        this.doctype = doctype;
        this.locale = locale;
    }
    
    public HtmlPage getHtmlPage() {
        return this.html;
    }
    
    public HttpServletRequest getServletRequest() {
        return this.servletRequest;
    }

    public Map<Object, Object> getMvcModel() {
        return Collections.unmodifiableMap(this.mvcModel);
    }
    
    public Object getParameter(String name) {
        Object value = null;
        if (this.decoratorParameters != null) {
            value = this.decoratorParameters.get(name);
        }
        return value;
    }
    
    public String getStringParameter(String name) {
        Object value = getParameter(name);
        if (value == null) {
            return null;
        }
        return value.toString();
    }
    
    public String getDoctype() {
        return this.doctype;
    }
        

    public Locale getLocale() {
        return this.locale;
    }
    
    public Iterator<String> getRequestParameterNames() {
        Set<String> s = new HashSet<String>();
        s.addAll(this.decoratorParameters.keySet());
        return s.iterator();
    }
}
