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
package org.vortikal.web.display.collection.article;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;

public class ArticleListingSearcher {

    private SearchComponent defaultSearch;
    private SearchComponent featuredArticlesSearch;

    private PropertyTypeDefinition featuredArticlesPropDef;

    public Listing getDefaultArticles(HttpServletRequest request, Resource collection, int page, int pageLimit,
            int offset) throws Exception {
        return this.defaultSearch.execute(request, collection, page, pageLimit, offset);
    }

    public Listing getFeaturedArticles(HttpServletRequest request, Resource collection, int page, int pageLimit,
            int offset) throws Exception {

        Property featuredArticlesProp = collection.getProperty(featuredArticlesPropDef);
        if (featuredArticlesProp == null) {
            return null;
        }

        Listing result = this.featuredArticlesSearch.execute(request, collection, page, pageLimit, offset);
        if (result.size() > 1) {
            sortFeaturedArticles(result, featuredArticlesProp);
        }

        return result;
    }

    public Listing getArticles(HttpServletRequest request, Resource collection, int page, int pageLimit, int offset)
            throws Exception {
        return this.defaultSearch.execute(request, collection, page, pageLimit, offset);
    }

    // Sort the featured articles listing according to the propdef
    private void sortFeaturedArticles(Listing result, Property featuredArticlesProp) {
        Value[] featuredArticleURIs = featuredArticlesProp.getValues();
        List<PropertySet> sortedFiles = new ArrayList<PropertySet>();
        List<PropertySet> files = result.getFiles();
        for (Value featuredArticleURI : featuredArticleURIs) {
            for (PropertySet file : files) {
                if (StringUtils.equals(file.getURI().toString(), featuredArticleURI.getStringValue())) {
                    sortedFiles.add(file);
                }
            }
        }
        result.setFiles(sortedFiles);
    }

    @Required
    public void setDefaultSearch(SearchComponent defaultSearch) {
        this.defaultSearch = defaultSearch;
    }

    @Required
    public void setFeaturedArticlesSearch(SearchComponent featuredArticlesSearch) {
        this.featuredArticlesSearch = featuredArticlesSearch;
    }

    public void setFeaturedArticlesPropDef(PropertyTypeDefinition featuredArticlesPropDef) {
        this.featuredArticlesPropDef = featuredArticlesPropDef;
    }

    public PropertyTypeDefinition getFeaturedArticlesPropDef() {
        return this.featuredArticlesPropDef;
    }

}
