/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.web.actions.report;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.ACLReadForAllQuery;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.web.service.URL;

public class BrokenLinksReport extends DocumentReporter {
    
    private PropertyTypeDefinition linkStatusPropDef;
    private PropertyTypeDefinition linkCheckPropDef;
    private PropertyTypeDefinition sortPropDef;
    private PropertyTypeDefinition publishedPropDef;
    private SortFieldDirection sortOrder;
    
    private final static String   REPORT_TYPE_PARAM_NAME = "broken-links";
    
    // TODO: simplify all this when finished in FTL
    private final static String   READ_RESTRICTION_PARAM_NAME = "read-restriction";
    private final static String   READ_RESTRICTION_PARAM_DEFAULT_VALUE = "all";
    private final static String[] READ_RESTRICTION_PARAM_VALUES = {READ_RESTRICTION_PARAM_DEFAULT_VALUE, "true", "false"};
    
    private final static String   PUBLISHED_PARAM_NAME = "published";
    private final static String   PUBLISHED_PARAM_DEFAULT_VALUE = "true";
    private final static String[] PUBLISHED_PARAM_VALUES = {PUBLISHED_PARAM_DEFAULT_VALUE, "false"};
    
 
    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) { 
        Map<String, Object> model = super.getReportContent(token, resource, request);  

        URL reportURL = super.getReportService().constructURL(resource).addParameter(REPORT_TYPE_PARAM, REPORT_TYPE_PARAM_NAME);
        
        Map<String, List<URLState>> filtersURLs = new HashMap<String, List<URLState>>();

        String published = request.getParameter(PUBLISHED_PARAM_NAME);
        String readRestriction = request.getParameter(READ_RESTRICTION_PARAM_NAME);
        
        if(published == null) {
           published = PUBLISHED_PARAM_DEFAULT_VALUE;
        }
        if(readRestriction == null) {
           readRestriction = READ_RESTRICTION_PARAM_DEFAULT_VALUE;
        }
        
        // Generate published filter
        List<URLState> filterPublishedURLs = new ArrayList<URLState>();
        for (String param : PUBLISHED_PARAM_VALUES) {
            URL filterOptionURL = reportURL.addParameter(PUBLISHED_PARAM_NAME, param);
            filterOptionURL.addParameter(READ_RESTRICTION_PARAM_NAME, readRestriction);
            if(published.equals(param)) {    
               filterPublishedURLs.add(new URLState(filterOptionURL, true));
            } else {
               filterPublishedURLs.add(new URLState(filterOptionURL, false));  
            }
        }
        filtersURLs.put(PUBLISHED_PARAM_NAME, filterPublishedURLs);
        
        // Generate read restriction filter
        List<URLState> filterReadRestrictionURLs = new ArrayList<URLState>();
        for (String param : READ_RESTRICTION_PARAM_VALUES) {
            URL filterOptionURL = reportURL.addParameter(READ_RESTRICTION_PARAM_NAME, param);
            filterOptionURL.addParameter(PUBLISHED_PARAM_NAME, published);
            if(readRestriction.equals(param)) {    
                filterReadRestrictionURLs.add(new URLState(filterOptionURL, true));
            } else {
                filterReadRestrictionURLs.add(new URLState(filterOptionURL, false)); 
            }
        }
        filtersURLs.put(READ_RESTRICTION_PARAM_NAME, filterReadRestrictionURLs);
        
        model.put("filtersURLs", filtersURLs);
        
        return model;
    }

    @Override
    protected Search getSearch(String token, Resource currentResource, HttpServletRequest request) {
        OrQuery linkStatusCriteria = new OrQuery();
        linkStatusCriteria.add(new PropertyTermQuery(this.linkStatusPropDef, "BROKEN_LINKS", TermOperator.EQ))
        .add(new PropertyTermQuery(this.linkStatusPropDef, "AWAITING_LINKCHECK", TermOperator.EQ));

        AndQuery topLevel = new AndQuery();
        
        // Read restriction (all|true|false)
        String readRestriction = request.getParameter(READ_RESTRICTION_PARAM_NAME);

        if ("true".equals(readRestriction)) {
            ACLReadForAllQuery aclReadForAllQuery = new ACLReadForAllQuery(true);
            topLevel.add(aclReadForAllQuery);
        } else if ("false".equals(readRestriction)) {
            ACLReadForAllQuery aclReadForAllQuery = new ACLReadForAllQuery();
            topLevel.add(aclReadForAllQuery);
        }
          
        topLevel.add(new UriPrefixQuery(currentResource.getURI().toString())).add(linkStatusCriteria);
 
        SortingImpl sorting = new SortingImpl();
        sorting.addSortField(new PropertySortField(this.sortPropDef, this.sortOrder));
        Search search = new Search();
        search.setQuery(topLevel);
        search.setSorting(sorting);
        
        // Published (true|false)
        if("false".equals(request.getParameter(PUBLISHED_PARAM_NAME))) {
            // ONLY those NOT published
            PropertyTermQuery ptq = new PropertyTermQuery(this.publishedPropDef, "true", TermOperator.NE);
            topLevel.add(ptq);
        } else {
            search.setOnlyPublishedResources(true);
        }

        return search;
    }
    
    @Override
    protected void handleResult(Resource resource, Map<String, Object> model) {
        Property linkCheck = resource.getProperty(this.linkCheckPropDef);
        if (linkCheck == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) model.get("linkCheck");
        if (map == null) {
            map = new HashMap<String, Object>();
            model.put("linkCheck", map);
        } 
        
        ContentStream binaryStream = linkCheck.getBinaryStream();
        Object obj = JSONValue.parse(new InputStreamReader(binaryStream.getStream()));
        map.put(resource.getURI().toString(), obj);
    }
    
    private class URLState {
        private URL url;
        private boolean active;
        
        public URLState(URL url, boolean state) {
            this.url = url;
            this.active = state;
        }
        
        @SuppressWarnings("unused")
        public URL getURL() {
            return this.url;
        }
        
        @SuppressWarnings("unused")
        public boolean isActive() {
            return this.active;
        }
    }

    @Required
    public void setLinkStatusPropDef(PropertyTypeDefinition linkStatusPropDef) {
        this.linkStatusPropDef = linkStatusPropDef;
    }
    
    @Required
    public void setLinkCheckPropDef(PropertyTypeDefinition linkCheckPropDef) {
        this.linkCheckPropDef = linkCheckPropDef;
    }

    @Required
    public void setPublishedPropDef(PropertyTypeDefinition publishedPropDef) {
        this.publishedPropDef = publishedPropDef;
    }

    @Required
    public void setSortPropDef(PropertyTypeDefinition sortPropDef) {
        this.sortPropDef = sortPropDef;
    }

    @Required
    public void setSortOrder(SortFieldDirection sortOrder) {
        this.sortOrder = sortOrder;
    }

}
