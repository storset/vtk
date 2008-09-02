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
package org.vortikal.web.controller.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.edit.editor.ResourceWrapper;
import org.vortikal.edit.editor.ResourceWrapperManager;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.QueryParser;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.SortField;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class SearchComponent {

    private String name;
    private String titleLocalizationKey;

    private boolean defaultRecursive = false;

    private Searcher searcher;
    private ResourceWrapperManager resourceManager;
    private Service viewService;

    private String query;
    private QueryParser queryParser;

    private PropertyTypeDefinition defaultSortPropDef;
    private Map<String, PropertyTypeDefinition> sortPropertyMapping;

    private SortFieldDirection defaultSortOrder;
    private Map<String, SortFieldDirection> sortOrderMapping;

    private PropertyTypeDefinition recursivePropDef;
    private PropertyTypeDefinition sortPropDef;

    private PropertyTypeDefinition authorDatePropDef;
    private PropertyTypeDefinition publishedDatePropDef;

    private List<PropertyDisplayConfig> listableProperties;


    public Listing execute(HttpServletRequest request, Resource collection, 
                           int offset, int limit) throws Exception {

        boolean recursive = this.defaultRecursive;
        if (collection.getProperty(this.recursivePropDef) != null) {
            recursive = collection.getProperty(this.recursivePropDef).getBooleanValue();
        }
        
        PropertyTypeDefinition sortProp = this.defaultSortPropDef;
        SortFieldDirection sortFieldDirection = this.defaultSortOrder;

        if (this.sortPropDef != null && collection.getProperty(this.sortPropDef) != null) {
            String sortString = collection.getProperty(this.sortPropDef).getStringValue();
            if (this.sortPropertyMapping.containsKey(sortString)) {
                sortProp = this.sortPropertyMapping.get(sortString);
            }
            if (this.sortOrderMapping != null && this.sortOrderMapping.containsKey(sortString)) {
                sortFieldDirection = this.sortOrderMapping.get(sortString);
            }
        }

        Search search = new Search();
        Query query = this.queryParser.parse(this.query);

        AndQuery andQuery = new AndQuery();
        andQuery.add(query);
        if (!recursive) {
            andQuery.add(new UriDepthQuery(collection.getURI().getDepth() + 1));
        }

        search.setQuery(andQuery);
        search.setLimit(limit + 1);
        search.setCursor(offset);

        String token = SecurityContext.getSecurityContext().getToken();
        List<SortField> sortFields = new ArrayList<SortField>();
        sortFields.add(new PropertySortField(sortProp, sortFieldDirection));
        search.setSorting(new SortingImpl(sortFields));

        ResultSet result = this.searcher.execute(token, search);

        boolean more = result.getSize() == limit + 1;
        int num = result.getSize();
        if (more) {
            num--;
        }

        Map<String, URL> urls = new HashMap<String, URL>();
        List<PropertySet> files = new ArrayList<PropertySet>();
        for (int i = 0; i < num; i++) {
            PropertySet res = result.getResult(i);
            files.add(res);
            URL url = this.viewService.constructURL(res.getURI());
            urls.put(res.getURI().toString(), url);
        }


        List<PropertyTypeDefinition> displayPropDefs = new ArrayList<PropertyTypeDefinition>();
        for (PropertyDisplayConfig config : this.listableProperties) {
            Property hide = null;
            if (config.getPreventDisplayProperty() != null) {
                hide = collection.getProperty(config.getPreventDisplayProperty());
            }
            if (hide == null) {
                displayPropDefs.add(config.getDisplayProperty());
            }
        }

        String title = null;
        if (this.titleLocalizationKey != null) {
            org.springframework.web.servlet.support.RequestContext springRequestContext = new org.springframework.web.servlet.support.RequestContext(
                    request);
            title = springRequestContext.getMessage(this.titleLocalizationKey, (String) null);
        }

        ResourceWrapper resourceWrapper = this.resourceManager.createResourceWrapper(collection.getURI());
 
        Listing listing = new Listing(resourceWrapper, title, name, offset);
        listing.setMore(more);
        listing.setFiles(files);
        listing.setUrls(urls);
        listing.setDisplayPropDefs(displayPropDefs);
        
        return listing;
    }

    @Required
    public void setName(String name) {
        this.name = name;
    }


    public String getName() {
        return this.name;
    }


    @Required
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }


    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }


    @Required
    public void setResourceManager(ResourceWrapperManager resourceManager) {
        this.resourceManager = resourceManager;
    }


    @Required
    public void setRecursivePropDef(PropertyTypeDefinition recursivePropDef) {
        this.recursivePropDef = recursivePropDef;
    }


    public void setSortPropDef(PropertyTypeDefinition sortPropDef) {
        this.sortPropDef = sortPropDef;
    }


    @Required
    public void setDefaultSortPropDef(PropertyTypeDefinition defaultSortPropDef) {
        this.defaultSortPropDef = defaultSortPropDef;
    }


    @Required
    public void setListableProperties(List<PropertyDisplayConfig> listableProperties) {
        this.listableProperties = listableProperties;
    }


    public void setSortPropertyMapping(Map<String, PropertyTypeDefinition> sortPropertyMapping) {
        this.sortPropertyMapping = sortPropertyMapping;
    }


    @Required
    public void setDefaultSortOrder(SortFieldDirection defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }


    public void setSortOrderMapping(Map<String, SortFieldDirection> sortOrderMapping) {
        this.sortOrderMapping = sortOrderMapping;
    }


    @Required
    public void setQuery(String query) {
        this.query = query;
    }


    @Required
    public void setQueryParser(QueryParser queryParser) {
        this.queryParser = queryParser;
    }


    public void setTitleLocalizationKey(String titleLocalizationKey) {
        this.titleLocalizationKey = titleLocalizationKey;
    }


    public String getTitleLocalizationKey() {
        return titleLocalizationKey;
    }


    public void setDefaultRecursive(boolean defaultRecursive) {
        this.defaultRecursive = defaultRecursive;
    }


    public void setPublishedDatePropDef(PropertyTypeDefinition publishedDatePropDef) {
        this.publishedDatePropDef = publishedDatePropDef;
    }


    public PropertyTypeDefinition getPublishedDatePropDef() {
        return this.publishedDatePropDef;
    }


    public void setAuthorPropDef(PropertyTypeDefinition authorPropDef) {
        this.authorDatePropDef = authorPropDef;
    }


    public PropertyTypeDefinition getAuthorPropDef() {
        return this.authorDatePropDef;
    }


    public class Listing {
        private ResourceWrapper resource;
        private String title;
        private String name;
        private int offset;
        private boolean more;
        private List<PropertySet> files = new ArrayList<PropertySet>();
        private Map<String, URL> urls = new HashMap<String, URL>();
        private List<PropertyTypeDefinition> displayPropDefs = new ArrayList<PropertyTypeDefinition>();

        public Listing(ResourceWrapper resource, String title, String name, int offset) {
            this.resource = resource;
            this.title = title;
            this.name = name;
            this.offset = offset;
        }

        public ResourceWrapper getResource() {
            return resource;
        }

        public String getTitle() {
            return title;
        }

        public String getName() {
            return name;
        }
        
        public int getOffset() {
            return this.offset;
        }

        public void setFiles(List<PropertySet> files) {
            this.files = files;
        }

        public List<PropertySet> getFiles() {
            return files;
        }

        public void setUrls(Map<String, URL> urls) {
            this.urls = urls;
        }

        public Map<String, URL> getUrls() {
            return urls;
        }

        public void setDisplayPropDefs(List<PropertyTypeDefinition> displayPropDefs) {
            this.displayPropDefs = displayPropDefs;
        }

        public List<PropertyTypeDefinition> getDisplayPropDefs() {
            return displayPropDefs;
        }

        public void setMore(boolean more) {
            this.more = more;
        }

        public boolean hasMoreResults() {
            return more;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder(this.getClass().getName());
            sb.append(": title: " + this.title);
            sb.append("; resource: ").append(this.resource.getURI());
            sb.append("; offset: " + this.offset);
            sb.append("; size: " + this.files.size());
            sb.append("; more:").append(this.more);
            return sb.toString();
        }
    }

}
