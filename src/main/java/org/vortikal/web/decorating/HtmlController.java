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
package org.vortikal.web.decorating;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlNodeFilter;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.web.RequestContext;

public class HtmlController implements Controller {

    private Repository repository;
    private HtmlPageParser parser;
    private List<HtmlNodeFilter> parseFilters;
    private String viewName;
    private Map<String, String> exposedModelElements;
    
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        Path uri = requestContext.getResourceURI();

        Resource resource = this.repository.retrieve(token, uri, true);
        InputStream is = this.repository.getInputStream(token, uri, true);

        HtmlPage page = this.parser.parse(is, resource.getCharacterEncoding(), 
                    this.parseFilters);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("resource", resource);
        model.put("page", page);

        if (this.exposedModelElements != null) {
            for (String id : this.exposedModelElements.keySet()) {
                String select = this.exposedModelElements.get(id);
                HtmlElement elem = page.selectSingleElement(select);
                model.put(id, elem);
            }
        }
        return new ModelAndView(this.viewName, model);
    }

    @Required public void setParser(HtmlPageParser parser) {
        this.parser = parser;
    }

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setExposedModelElements(Map<String, String> exposedModelElements) {
        this.exposedModelElements = exposedModelElements;
    }

    public void setParseFilters(List<HtmlNodeFilter> parseFilters) {
        this.parseFilters = parseFilters;
    }
}
