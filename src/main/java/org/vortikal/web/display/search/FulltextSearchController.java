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
package org.vortikal.web.display.search;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.search.Searcher;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.fulltext.FulltextSearcher;
import org.vortikal.security.AuthenticationException;
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
 *   <li><code>redirectViewName</code> - the name of an optional redirect view
 *   <li><code>hostName</code> - optional name for the root node (to be displayed in the title)
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
    private String redirectViewName;
    private int pageSize = 20;
    private String hostName;
    
    
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
                                      HttpServletResponse response) throws Exception {
        
        Map<String, Object> model = new HashMap<String, Object>();

        String token = SecurityContext.getSecurityContext().getToken();

        if (request.getParameter("login") != null) {
            if (token == null) {
                // The user wants to login
                throw new AuthenticationException();
            } 
            
            if (this.redirectViewName != null) {
                // Should send redirect without login parameter
                URL url = URL.create(request);
                url.removeParameter("login");
                model.put("redirectURL", url.toString());
                return new ModelAndView(this.redirectViewName, model);
            }
        }
        
        Map<String, Object> searchModel = new HashMap<String, Object>();
        model.put("search", searchModel);
        
        String query = request.getParameter("query");

        Service currentService = RequestContext.getRequestContext().getService();
        Path resourceURI = RequestContext.getRequestContext().getResourceURI();
        
        URL searchURL = currentService.constructURL(resourceURI);
        searchModel.put("url", searchURL);

        if (this.hostName != null) {
            searchModel.put("hostName", this.hostName);
        }
        
        if (query == null || query.length() == 0) {
            return new ModelAndView(this.viewName, model);
        }

        int page = getPage(request.getParameter("page"));
        int startIdx = page * this.pageSize;
        int endIdx = startIdx + this.pageSize;

        ResultSet resultSet = searcher.execute(token, query, resourceURI);

        // Check if last result of the current page exists 
        if (! resultSet.hasResult(endIdx-1)) {
            // Position endIdx one beyond the very last result (endIdx is exclusive)
            endIdx = resultSet.getAllResults().size();
        }

        // Since endIdx might have been repositioned above, we need to make sure
        // startIdx and page is sane.
        if (startIdx >= endIdx) {
            // In case of insane page number, don't roll back startIdx a 
            // whole page, just roll back to start of the last result page.
            if (endIdx % this.pageSize == 0) {
                startIdx = Math.max(endIdx - this.pageSize, 0);
            } else {
                startIdx = endIdx - endIdx % this.pageSize;
            }
            // Update page counter
            page = startIdx / this.pageSize;
        }

        List<PropertySet> results = resultSet.getResults(startIdx, endIdx);

        if (resultSet.hasResult(endIdx)) { // Check if there is another page of results
            int nextPage = page + 1;
            URL nextURL = currentService.constructURL(resourceURI);
            nextURL.removeParameter("query");
            nextURL.addParameter("query", query);
            nextURL.removeParameter("page");
            nextURL.addParameter("page", String.valueOf(nextPage + 1));
            searchModel.put("next", nextURL);
        }

        if (page > 0) {
            List<String> previousUrls = new ArrayList<String>();
            int prevPage = page;

            URL prevURL = currentService.constructURL(resourceURI);
            prevURL.removeParameter("query");
            prevURL.addParameter("query", query);
            while (prevPage > 0) {
                prevURL.removeParameter("page");
                prevURL.addParameter("page", String.valueOf(prevPage));
                previousUrls.add(0, prevURL.toString());
                prevPage--;
            }
            searchModel.put("previousPages", previousUrls);
        }

        searchModel.put("query", query);
        searchModel.put("results", results);
        searchModel.put("totalHits", resultSet.getSize());
        searchModel.put("start", startIdx+1);
        searchModel.put("end", endIdx);
        
        return new ModelAndView(this.viewName, model);
    }



    /**
     * Page number must be 0 or greater.
     * Max page number can't be greater than
     * pageNum * pageSize + pageSize <= Integer.MAX_VALUE
     */
    protected int getPage(String pageParam) {
        if (pageParam == null) {
            return 0;
        }

        int page = 0;
        try {
            page = Integer.parseInt(pageParam);
            page--;
        } catch (NumberFormatException e) { 
            return 0;
        }
                
        if (page < 0) {
            return 0;
        }

        int maxPages = (Integer.MAX_VALUE - this.pageSize)/ this.pageSize;
        if (page > maxPages) {
            return maxPages;
        }
        
        return page;
    }


    public void setRedirectViewName(String redirectViewName) {
        this.redirectViewName = redirectViewName;
    }


    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

}
