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
package org.vortikal.web.controller.search;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.fulltext.FulltextSearcher;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;


/**
 * Controller that performs a fulltext search and returns a
 * configurable view name.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>viewName</code> - the name of the view to return
 *   <li><code>searcher</code> - the {@link Searcher}
 * </ul>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>search</code>
 * </ul>
 *
 */
public class FulltextSearchController implements Controller {

    private FulltextSearcher searcher;
    private String viewName;
    private int pageSize = 20;

    public void setSearcher(FulltextSearcher searcher) {
        this.searcher = searcher;
    }
    

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }


    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) 
	throws Exception {
        
        Map<String, Object> mainModel = new HashMap<String, Object>();
        Map<String, Object> subModel = new HashMap<String, Object>();
        
        String query = request.getParameter("query");
        if (query != null) {
            subModel.put("query", query);

            String token = SecurityContext.getSecurityContext().getToken();
        
            ResultSet resultSet = searcher.execute(token, query);
            List<PropertySet> results = resultSet.getResults(this.pageSize);
            subModel.put("results", results);
            subModel.put("start", new Integer(0));
            subModel.put("end", new Integer(results.size()));
        }
        
        RequestContext requestContext = RequestContext.getRequestContext();

        Service currentService = requestContext.getService();
        URL searchURL = currentService.constructURL(requestContext.getResourceURI());
        subModel.put("url", searchURL);
        
        mainModel.put("search", subModel);
        return new ModelAndView(this.viewName, mainModel);
    }

}
