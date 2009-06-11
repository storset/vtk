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
package org.vortikal.web.http;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * Controller that puts a redirect URL in the model and returns a
 * configurable view name.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>viewName</code> - the name of the view to return
 *   (default is <code>redirect</code>)
 * </ul>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>redirectURL</code> - currently the requested URL (TODO:
 *   what is the purpose of this entry?)
 * </ul>
 *
 */
public class RedirectController implements Controller {

    public static final String DEFAULT_VIEW_NAME = "redirect";

    private String viewName = DEFAULT_VIEW_NAME;


    public void setViewName(String viewName) {
        this.viewName = viewName;
    }


    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) 
	throws Exception {
        StringBuffer redirectURL = request.getRequestURL();

        if (redirectURL.indexOf("?") != -1) {
            redirectURL.delete(redirectURL.indexOf("?"),
                               redirectURL.length());
        }
        if (redirectURL.charAt(redirectURL.length() - 1) != '/')
            redirectURL.append("/");
        
        if (request.getQueryString() != null) 
            redirectURL.append("?").append(request.getQueryString());
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("redirectURL", redirectURL.toString());
        return new ModelAndView(this.viewName, model);
    }

}
