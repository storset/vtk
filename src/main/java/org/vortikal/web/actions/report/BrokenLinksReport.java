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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.Parser;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.TypedSortField;
import org.vortikal.repository.search.query.ACLReadForAllQuery;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.web.service.URL;



public class BrokenLinksReport extends DocumentReporter {
    
    private PropertyTypeDefinition linkStatusPropDef;
    private PropertyTypeDefinition linkCheckPropDef;
    private PropertyTypeDefinition brokenLinksCountPropDef;
    private PropertyTypeDefinition sortPropDef;
    private PropertyTypeDefinition publishedPropDef;
    private SortFieldDirection sortOrder;
    private Parser parser;
    private String queryFilterExpression;

    private final static String   REPORT_TYPE_PARAM_NAME = "broken-links";
    
    private final static String   FILTER_READ_RESTRICTION_PARAM_NAME = "read-restriction";
    private final static String   FILTER_READ_RESTRICTION_PARAM_DEFAULT_VALUE = "all";
    private final static String[] FILTER_READ_RESTRICTION_PARAM_VALUES = { FILTER_READ_RESTRICTION_PARAM_DEFAULT_VALUE, "false", "true" };

    private final static String   FILTER_LINK_TYPE_PARAM_NAME = "link-type";
    private final static String   FILTER_LINK_TYPE_PARAM_DEFAULT_VALUE = "anchor";
    
    private final static String[] FILTER_LINK_TYPE_PARAM_VALUES = { FILTER_LINK_TYPE_PARAM_DEFAULT_VALUE, "img", "other" };

    
    private final static String   FILTER_PUBLISHED_PARAM_NAME = "published";
    private final static String   FILTER_PUBLISHED_PARAM_DEFAULT_VALUE = "true";
    private final static String[] FILTER_PUBLISHED_PARAM_VALUES = { FILTER_PUBLISHED_PARAM_DEFAULT_VALUE, "false" };

 
    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) { 
        Map<String, Object> result = super.getReportContent(token, resource, request);  

        URL reportURL = super.getReportService().constructURL(resource).addParameter(REPORT_TYPE_PARAM, REPORT_TYPE_PARAM_NAME);
        
        Map<String, List<FilterOption>> filters = new LinkedHashMap<String, List<FilterOption>>();

        String linkType = request.getParameter(FILTER_LINK_TYPE_PARAM_NAME);
        String published = request.getParameter(FILTER_PUBLISHED_PARAM_NAME);
        String readRestriction = request.getParameter(FILTER_READ_RESTRICTION_PARAM_NAME);
        
        if (linkType == null) linkType = FILTER_LINK_TYPE_PARAM_DEFAULT_VALUE;
        if (published == null) published = FILTER_PUBLISHED_PARAM_DEFAULT_VALUE;
        if (readRestriction == null) readRestriction = FILTER_READ_RESTRICTION_PARAM_DEFAULT_VALUE;
        
        result.put("linkType", linkType);

        // TODO: Refactor method and generalize for 1..infinity filters

        // Generate read restriction filter
        List<FilterOption> filterReadRestrictionOptions = new ArrayList<FilterOption>();
        for (String param : FILTER_READ_RESTRICTION_PARAM_VALUES) {
            URL filterOptionURL = new URL(reportURL);
            filterOptionURL.addParameter(FILTER_READ_RESTRICTION_PARAM_NAME, param);
            filterOptionURL.addParameter(FILTER_PUBLISHED_PARAM_NAME, published);
            filterOptionURL.addParameter(FILTER_LINK_TYPE_PARAM_NAME, linkType);
            filterReadRestrictionOptions.add(new FilterOption(param, filterOptionURL, param.equals(readRestriction) ? true : false));
        }
        
        // Generate link type filter
        List<FilterOption> filterLinkTypeOptions = new ArrayList<FilterOption>();
        for (String param : FILTER_LINK_TYPE_PARAM_VALUES) {
            URL filterOptionURL = new URL(reportURL);
            filterOptionURL.addParameter(FILTER_LINK_TYPE_PARAM_NAME, param);
            filterOptionURL.addParameter(FILTER_PUBLISHED_PARAM_NAME, published);
            filterOptionURL.addParameter(FILTER_READ_RESTRICTION_PARAM_NAME, readRestriction);
            filterLinkTypeOptions.add(new FilterOption(param, filterOptionURL, param.equals(linkType) ? true : false));
        }
        
        // Generate published filter
        List<FilterOption> filterPublishedOptions = new ArrayList<FilterOption>();
        for (String param : FILTER_PUBLISHED_PARAM_VALUES) {
            URL filterOptionURL = new URL(reportURL);
            filterOptionURL.addParameter(FILTER_PUBLISHED_PARAM_NAME, param);
            filterOptionURL.addParameter(FILTER_READ_RESTRICTION_PARAM_NAME, readRestriction); 
            filterOptionURL.addParameter(FILTER_LINK_TYPE_PARAM_NAME, linkType);
            filterPublishedOptions.add(new FilterOption(param, filterOptionURL, param.equals(published) ? true : false));
        }
        
        filters.put(FILTER_PUBLISHED_PARAM_NAME, filterPublishedOptions);
        filters.put(FILTER_LINK_TYPE_PARAM_NAME, filterLinkTypeOptions);
        filters.put(FILTER_READ_RESTRICTION_PARAM_NAME, filterReadRestrictionOptions);
        
        result.put("filters", filters);
        
        result.put("brokenLinkCount", getBrokenLinkCount(token, resource, request, linkType));
        
        return result;
    }
    
    private int getBrokenLinkCount(String token, Resource currentResource,
                                   HttpServletRequest request, final String linkType) {
        // Set up search
        Search search = getSearch(token, currentResource, request);
        search.setLimit(Integer.MAX_VALUE);
        ConfigurablePropertySelect cfg = new ConfigurablePropertySelect();
        cfg.addPropertyDefinition(this.brokenLinksCountPropDef);
        search.setPropertySelect(cfg);
        search.setSorting(null);

        // Set up include/exclude link types sum of broken links
        String[] includeTypes;
        String[] excludeTypes = new String[0];
        if (FILTER_LINK_TYPE_PARAM_DEFAULT_VALUE.equals(linkType) || linkType == null) {
            includeTypes = new String[]{"BROKEN_LINKS_ANCHOR"};
        } else if ("img".equals(linkType)) {
            includeTypes = new String[] {"BROKEN_LINKS_IMG"};
        } else {
            includeTypes = new String[] {"BROKEN_LINKS"};
            excludeTypes = new String[] {"BROKEN_LINKS_IMG", "BROKEN_LINKS_ANCHOR"};
        }

        // Search callback which sums up broken link counts
        final class Accumulator implements Searcher.MatchCallback {
            int sum = 0;
            final String[] includeTypes;
            final String[] excludeTypes;
            
            Accumulator(String[] includeTypes, String[] excludeTypes) {
                this.includeTypes = includeTypes;
                this.excludeTypes = excludeTypes;
            }
            
            @Override
            public boolean matching(PropertySet propertySet) throws Exception {
                Property prop = propertySet.getProperty(brokenLinksCountPropDef);
                if (prop == null) return true;
                
                net.sf.json.JSONObject obj = prop.getJSONValue();
                for (String includeType: this.includeTypes) {
                    sum += obj.optInt(includeType);
                }
                for (String excludeType: this.excludeTypes) {
                    sum -= obj.optInt(excludeType);
                }

                return true;
            }
        }

        Accumulator accumulator = new Accumulator(includeTypes, excludeTypes);
        searcher.iterateMatching(token, search, accumulator);
        return accumulator.sum;
    }

    @Override
    protected Search getSearch(String token, Resource currentResource, HttpServletRequest request) {
        OrQuery linkStatusCriteria = new OrQuery();
        String linkType = request.getParameter(FILTER_LINK_TYPE_PARAM_NAME);
        
        if (FILTER_LINK_TYPE_PARAM_DEFAULT_VALUE.equals(linkType) || linkType == null) {
            linkStatusCriteria.add(new PropertyTermQuery(this.linkStatusPropDef, "BROKEN_LINKS_ANCHOR", TermOperator.EQ));
        } else if ("img".equals(linkType)) {
            linkStatusCriteria.add(new PropertyTermQuery(this.linkStatusPropDef, "BROKEN_LINKS_IMG", TermOperator.EQ));
        } else {
            AndQuery and = new AndQuery();
            and.add(new PropertyTermQuery(this.linkStatusPropDef, "BROKEN_LINKS", TermOperator.EQ));
            and.add(new PropertyTermQuery(this.linkStatusPropDef, "BROKEN_LINKS_ANCHOR", TermOperator.NE));
            and.add(new PropertyTermQuery(this.linkStatusPropDef, "BROKEN_LINKS_IMG", TermOperator.NE));
            linkStatusCriteria.add(and);
        }
        linkStatusCriteria.add(new PropertyTermQuery(this.linkStatusPropDef, "AWAITING_LINKCHECK", TermOperator.EQ));

        AndQuery topLevelQ = new AndQuery();

        // Read restriction (all|true|false)
        String readRestriction = request.getParameter(FILTER_READ_RESTRICTION_PARAM_NAME);

        if (readRestriction != null) {
            if ("true".equals(readRestriction)) {
                ACLReadForAllQuery aclReadForAllQ = new ACLReadForAllQuery(true);
                topLevelQ.add(aclReadForAllQ);
            } else if ("false".equals(readRestriction)) {
                ACLReadForAllQuery aclReadForAllQ = new ACLReadForAllQuery();
                topLevelQ.add(aclReadForAllQ);
            }
        }
        
        // Add clauses for limiting to current folder and link status criteria
        topLevelQ.add(new UriPrefixQuery(currentResource.getURI().toString())).add(linkStatusCriteria);
        
        // Add clauses for any configured default filter query
        Query filterQ = getFilterQuery();
        if (filterQ != null) {
            topLevelQ.add(filterQ);
        }

        Search search = new Search();
        search.setQuery(topLevelQ);
        SortingImpl sorting = new SortingImpl();
        
        if (this.sortPropDef == null) {
            sorting.addSortField(new TypedSortField(PropertySet.URI_IDENTIFIER, this.sortOrder));
        } else {
            sorting.addSortField(new PropertySortField(this.sortPropDef, this.sortOrder));
        }
        search.setSorting(sorting);

        // Published (true|false)
        String published = request.getParameter(FILTER_PUBLISHED_PARAM_NAME);

        if (published != null && "false".equals(published)) {
            // ONLY those NOT published
            PropertyTermQuery ptq = new PropertyTermQuery(this.publishedPropDef, "true", TermOperator.NE);
            topLevelQ.add(ptq);
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

        Object obj;
        try {
            obj = JSONValue.parse(new InputStreamReader(binaryStream.getStream(), "utf-8"));
            map.put(resource.getURI().toString(), obj);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public class FilterOption {
        private String name;
        private URL url;
        private boolean active;
        
        public FilterOption(String name, URL url, boolean active) {
            this.name = name;
            this.url = url;
            this.active = active;
        }
        
        public String getName() {
            return this.name;
        }
        
        public URL getURL() {
            return this.url;
        }
        
        public boolean isActive() {
            return this.active;
        }
    }
    
    private Query getFilterQuery() {
        if (this.queryFilterExpression != null) {
            if (this.parser == null) {
                throw new IllegalStateException("parser must be configured when using queryFilterExpression");
            }
            return this.parser.parse(this.queryFilterExpression);
        }
        return null;
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
    public void setBrokenLinksCountPropDef(PropertyTypeDefinition def) {
        this.brokenLinksCountPropDef = def;
    }
    
    @Required
    public void setPublishedPropDef(PropertyTypeDefinition publishedPropDef) {
        this.publishedPropDef = publishedPropDef;
    }

    public void setSortPropDef(PropertyTypeDefinition sortPropDef) {
        this.sortPropDef = sortPropDef;
    }

    @Required
    public void setSortOrder(SortFieldDirection sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }
    
    public void setQueryFilterExpression(String exp) {
        this.queryFilterExpression = exp;
    }

}
