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

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.text.html.HtmlPage;


/**
 * Represents a decorator request for a component.
 */
public interface DecoratorRequest {

    
    /**
     * Gets the HTML page currently being decorated.
     * @return the HtmlPage object
     */
    public HtmlPage getHtmlPage();

    /**
     * Gets the current servlet request
     * @return the servlet request
     */
    public HttpServletRequest getServletRequest();
    
    /**
     * Gets the main MVC model of the current servlet request. 
     * (Should generally not be needed.)
     * @return the MVC model
     */
    public Map<String, Object> getMvcModel();

    /**
     * Gets the request locale
     * @return the request locale
     */
    public Locale getLocale();

    /**
     * Gets the document type of the output page
     * @return the doctype
     */
    public String getDoctype();

    /**
     * Gets a named parameter for this 
     * decorator component invocation. No processing 
     * (variable expansion) is performed on the parameter.
     * @param name the name of the parameter
     * @return the parameter value (or <code>null</code> 
     * if no such parameter exists)
     */
    public Object getRawParameter(String name);

    /**
     * Gets a named string parameter for this 
     * decorator component invocation. 
     * The parameter may be subject to variable 
     * expansion before returned.
     * @param name the name of the parameter
     * @return the parameter value, as a string
     */
    public String getStringParameter(String name);

    /**
     * Gets the names of all invocation parameters 
     * for this decorator component.
     * @return an iterator over the parameter names
     */
    public Iterator<String> getRequestParameterNames();
}
