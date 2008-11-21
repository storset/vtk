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
package org.vortikal.web.controller.article;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;

public class ArticleListingSearcher {
	
	private SearchComponent defaultSearch;
    private SearchComponent featuredArticlesSearch;
    private SearchComponent subfoldersSearch;
    private SearchComponent currentFolderSearch;
    
    public Listing getDefaultArticles(HttpServletRequest request, Resource collection,
			int page, int pageLimit, int upcomingOffset) throws Exception {
    	return this.defaultSearch.execute(request, collection, page, pageLimit, upcomingOffset);
    }
    
    public Listing getFeaturedArticles(HttpServletRequest request, Resource collection,
			int page, int pageLimit, int upcomingOffset) throws Exception {
    	return this.featuredArticlesSearch.execute(request, collection, page, pageLimit, upcomingOffset);
    }
    
    public Listing getSubfoldersArticles(HttpServletRequest request, Resource collection,
			int page, int pageLimit, int upcomingOffset) throws Exception {
    	return this.subfoldersSearch.execute(request, collection, page, pageLimit, upcomingOffset);
    }
    
    public Listing getCurrentFolderArticles(HttpServletRequest request, Resource collection,
			int page, int pageLimit, int upcomingOffset) throws Exception {
    	return this.currentFolderSearch.execute(request, collection, page, pageLimit, upcomingOffset);
    }
    
	public Listing getArticles(HttpServletRequest request, Resource collection,
			int page, int pageLimit, int upcomingOffset) throws Exception {
		
		Namespace namespace_al = Namespace.getNamespace("http://www.uio.no/resource-types/article-listing");
		Property recursiveListing = collection.getProperty(namespace_al, PropertyType.RECURSIVE_LISTING_PROP_NAME);
		Property subfolders = collection.getProperty(namespace_al, PropertyType.SUBFOLDERS_PROP_NAME);
		if ((recursiveListing != null && recursiveListing.getBooleanValue() == false)
				|| (subfolders == null || subfolders.getValues().length == 0)) {
			return this.defaultSearch.execute(request, collection, page, pageLimit, upcomingOffset);
		}
		
		/* Hm... This ain't good:
		 * 
		 * Should rather refactor searchcomponent to handle multiple queries returning one
		 * listing.
		 * 
		 */
		Listing mainResult = this.currentFolderSearch.execute(request, collection, page, pageLimit, upcomingOffset);
		Listing additionalArticlesFromSubfolders = this.subfoldersSearch.execute(request, collection, page, pageLimit, upcomingOffset);
		mainResult.getFiles().addAll(additionalArticlesFromSubfolders.getFiles());
		mainResult.getUrls().putAll(additionalArticlesFromSubfolders.getUrls());
		
		/* This is even worse:
		 * 
		 * Sorting is already done in the searchcomponent, but it doesn't work when search is
		 * comprised of two separate queries. Has to be handled in the searchcomponent, but
		 * TIME oh TIME...
		 * 
		 */
		Property sortPropDef = collection.getProperty(namespace_al, PropertyType.SORTING_PROP_NAME);
		Collections.sort(mainResult.getFiles(), new ResultListingComparator(sortPropDef));
		
		return mainResult;
	}

	public void removeFeaturedArticlesFromDefault(List<PropertySet> featuredArticles, List<PropertySet> defaultArticles) {
		List<PropertySet> duplicateArticles = new ArrayList<PropertySet>();
		for (PropertySet featuredArticle : featuredArticles) {
			for (PropertySet defaultArticle : defaultArticles) {
				if (defaultArticle.getURI().equals(featuredArticle.getURI())) {
					duplicateArticles.add(defaultArticle);
				}
			}
		}
		defaultArticles.removeAll(duplicateArticles);
	}
	
    @Required
    public void setDefaultSearch(SearchComponent defaultSearch) {
        this.defaultSearch = defaultSearch;
    }
	
	@Required
    public void setFeaturedArticlesSearch(SearchComponent featuredArticlesSearch) {
        this.featuredArticlesSearch = featuredArticlesSearch;
    }
    
    @Required
    public void setSubfoldersSearch(SearchComponent subfoldersSearch) {
        this.subfoldersSearch = subfoldersSearch;
    }
    
    @Required
    public void setCurrentFolderSearch(SearchComponent currentFolderSearch) {
        this.currentFolderSearch = currentFolderSearch;
    }
    
    private class ResultListingComparator implements Comparator<PropertySet> {
    	
    	private Property sortPropDef;
    	
    	public ResultListingComparator(Property sortPropDef) {
    		this.sortPropDef = sortPropDef;
    	}

		public int compare(PropertySet p1, PropertySet p2) {
			
			Property sp1;
			Property sp2;
			if (this.sortPropDef != null) {
				sp1 = p1.getProperty(Namespace.DEFAULT_NAMESPACE, sortPropDef.getStringValue());
				sp2 = p2.getProperty(Namespace.DEFAULT_NAMESPACE, sortPropDef.getStringValue());
				return sp1.getStringValue().compareTo(sp2.getStringValue());
			}
			
			sp1 = p1.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.PUBLISHED_DATE_PROP_NAME);
			sp2 = p2.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.PUBLISHED_DATE_PROP_NAME);
			
			return sp2.getDateValue().compareTo(sp1.getDateValue());
		}
    	
    }

}
