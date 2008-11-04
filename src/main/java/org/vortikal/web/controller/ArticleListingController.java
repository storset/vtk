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
package org.vortikal.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.URL;

public class ArticleListingController extends AbstractCollectionListingController {
	
    private SearchComponent featuredArticlesSearch;
    private SearchComponent defaultSearch;
        	
    protected void runSearch(HttpServletRequest request, Resource collection,
    		Map<String, Object> model) throws Exception {
    	
        int page = 0;
        if (request.getParameter("page") != null) {
            try {
                page = Integer.parseInt(request.getParameter("page"));
                if (page < 1) {
                    page = 1;
                }
            } catch (Throwable t) { }
        }

        if (page == 0) {
            page = 1;
        }

        int pageLimit = getPageLimit(collection);
        
        // TODO execute featuredArticleSearch
        // NB! paging will be different once other searches are performed
        
        List<Listing> results = new ArrayList<Listing>();
        Listing listing = defaultSearch.execute(request, collection, page, pageLimit, 0);
        if (listing.getFiles().size() > 0) {
            results.add(listing);
        }
        
        model.put("searchComponents", results);
        model.put("page", page);

        URL nextURL = null;
        URL prevURL = null;
        if (results.size() > 0) {
            Listing last = results.get(results.size() - 1);
            if (last.hasMoreResults()) {
                nextURL = URL.create(request);
                nextURL.setParameter("page", String.valueOf(page + 1));
            }
            if (page > 1) {
                prevURL = URL.create(request);
                if (page == 1) {
                    prevURL.removeParameter("page");
                } else {
                    prevURL.setParameter("page", String.valueOf(page - 1));
                }
            }
        }
        
        model.put("nextURL", nextURL);
        model.put("prevURL", prevURL);
    	
    }
    
    @Required
    public void setFeaturedArticlesSearch(SearchComponent featuredArticlesSearch) {
        this.featuredArticlesSearch = featuredArticlesSearch;
    }

    @Required
    public void setDefaultSearch(SearchComponent defaultSearch) {
        this.defaultSearch = defaultSearch;
    }

}