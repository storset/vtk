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
package org.vortikal.web.display.autocomplete;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import org.vortikal.repository.Path;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public abstract class AutoCompleteController implements Controller {

    protected static final char SUGGESTION_DELIMITER = '\n';
    protected static final char FIELD_SEPARATOR = ';';
    protected static final String PARAM_QUERY = "q";
    protected static final String PARAM_CONTEXT_URI_OVERRIDE = "context";
    protected static final String RESPONSE_CONTENT_TYPE = "text/plain;charset=utf-8";

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String token = SecurityContext.getSecurityContext().getToken();
        Path contextUri = getContextUri(request);

        String query = request.getParameter(PARAM_QUERY);
        if (query == null) {
            return null; // No query results in no auto-complete suggestions
        }

        String autoCompleteSuggestions = this.getAutoCompleteSuggestions(query, contextUri, token);
        autoCompleteSuggestions = autoCompleteSuggestions == null ? "" : autoCompleteSuggestions;

        response.setContentType(RESPONSE_CONTENT_TYPE);
        response.getWriter().print(autoCompleteSuggestions);

        return null;
    }

    protected Path getContextUri(HttpServletRequest request) {
        Path contextUri = null;
        try {
            // Try getting from overriding parameter first
            String contextParam = request.getParameter(PARAM_CONTEXT_URI_OVERRIDE);
            // Be gentle and snip trailing slash if necessary:
            if (contextParam != null && contextParam.endsWith("/") && contextParam.length() > 1) {
                contextParam = contextParam.substring(0, contextParam.length() - 1);
            }

            contextUri = Path.fromString(contextParam);
        } catch (IllegalArgumentException ie) {
            // Ignore.
        }

        if (contextUri == null) {
            // No luck (invalid path or missing override param), try request context ..
            RequestContext requestContext = RequestContext.getRequestContext();
            contextUri = requestContext.getResourceURI();
        }

        return contextUri;
    }

    /**
     * Optionally implemented by subclasses to provide autocomplete suggestions.
     * Only useful if handleRequest is *not* overridden.
     * 
     * @param query
     * @param contextUri
     * @param token
     * @return
     */
    protected abstract String getAutoCompleteSuggestions(String query, Path contextUri, String token);

}
