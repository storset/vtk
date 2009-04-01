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
import org.vortikal.web.controller.article.ArticleListingSearcher;
import org.vortikal.web.search.Listing;
import org.vortikal.web.service.URL;

public class ArticleListingController extends AbstractCollectionListingController {
	
	private ArticleListingSearcher searcher;
        	
    protected void runSearch(HttpServletRequest request, Resource collection,
    		Map<String, Object> model, int pageLimit) throws Exception {
    	        
        int featuredArticlesPage = getPage(request, UPCOMING_PAGE_PARAM);
        int defaultArticlesPage = getPage(request, PREVIOUS_PAGE_PARAM);

        int userDisplayPage = defaultArticlesPage;

        URL nextURL = null;
        URL prevURL = null;

        boolean atLeastOneFeaturedArticle = false;
        if (collection.getProperty(searcher.getFeaturedArticlesPropDef()) != null) {
            atLeastOneFeaturedArticle = this.searcher.getFeaturedArticles(request, collection, 1, 1, 0).size() > 0;
        }

        List<Listing> results = new ArrayList<Listing>();
        Listing featuredArticles = null;
        if (request.getParameter(PREVIOUS_PAGE_PARAM) == null) {
            // Search featured articles
        	featuredArticles = this.searcher.getFeaturedArticles(request, collection, featuredArticlesPage, pageLimit, 0);
            if (featuredArticles.size() > 0) {
            	results.add(featuredArticles);
                if (featuredArticlesPage > 1) {
                    prevURL = URL.create(request);
                    prevURL.removeParameter(PREVIOUS_PAGE_PARAM);
                    prevURL.removeParameter(PREV_BASE_OFFSET_PARAM);
                    prevURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(featuredArticlesPage - 1));
                }
            }
            if (featuredArticles.hasMoreResults()) {
                nextURL = URL.create(request);
                nextURL.removeParameter(PREVIOUS_PAGE_PARAM);
                nextURL.removeParameter(PREV_BASE_OFFSET_PARAM);
                nextURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(featuredArticlesPage + 1));
            }
        }

        
        if (featuredArticles == null || featuredArticles.size() == 0) {
            // Searching only in default articles
            int upcomingOffset = getIntParameter(request, PREV_BASE_OFFSET_PARAM, 0);
            if (upcomingOffset > pageLimit) upcomingOffset = 0;
            Listing defaultArticles = this.searcher.getArticles(request, collection, defaultArticlesPage, pageLimit, upcomingOffset);
            if (defaultArticles.size() > 0) {
            	results.add(defaultArticles);
            }
            
            if (defaultArticlesPage > 1) {
                prevURL = URL.create(request);
                prevURL.setParameter(PREV_BASE_OFFSET_PARAM, String.valueOf(upcomingOffset));
                prevURL.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(defaultArticlesPage - 1));

            } else if (defaultArticlesPage == 1 && atLeastOneFeaturedArticle) {
                prevURL = URL.create(request);
                prevURL.removeParameter(PREVIOUS_PAGE_PARAM);
                prevURL.removeParameter(PREV_BASE_OFFSET_PARAM);
            }

            if (defaultArticles.hasMoreResults()) {
                nextURL = URL.create(request);
                nextURL.setParameter(PREV_BASE_OFFSET_PARAM, String.valueOf(upcomingOffset));
                nextURL.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(defaultArticlesPage + 1));
            }

            if (atLeastOneFeaturedArticle) {
                userDisplayPage += defaultArticlesPage;
            } else {
                userDisplayPage = defaultArticlesPage;
            }

        } else if (featuredArticles.size() < pageLimit) {
            // Fill up the rest of the page with default articles
            int upcomingOffset = pageLimit - featuredArticles.size();
            Listing defaultArticles = this.searcher.getArticles(request, collection, 1, upcomingOffset, 0);
            if (defaultArticles.size() > 0) {
            	results.add(defaultArticles);
            }
            
            if (featuredArticlesPage > 1) {
                prevURL = URL.create(request);
                prevURL.removeParameter(PREVIOUS_PAGE_PARAM);
                prevURL.removeParameter(PREV_BASE_OFFSET_PARAM);
                prevURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(featuredArticlesPage - 1));
            }
            
            if (defaultArticles.hasMoreResults()) {
                nextURL = URL.create(request);
                nextURL.setParameter(PREV_BASE_OFFSET_PARAM, String.valueOf(upcomingOffset));
                nextURL.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(defaultArticlesPage));
            }
        }
        
        model.put("searchComponents", results);
        model.put("page", userDisplayPage);
        model.put("hideNumberOfComments", getHideNumberOfComments(collection));
        
        cleanURL(nextURL);
        cleanURL(prevURL);

        model.put("nextURL", nextURL);
        model.put("prevURL", prevURL);
    	
    }
	
	@Required
	public void setSearcher(ArticleListingSearcher searcher) {
		this.searcher = searcher;
	}

}