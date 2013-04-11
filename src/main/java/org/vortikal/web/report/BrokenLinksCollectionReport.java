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
package org.vortikal.web.report;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class BrokenLinksCollectionReport extends BrokenLinksReport {

    private int pageSize = 25;
    private Service brokenLinksToTsvReportService;

    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(REPORT_NAME, getName());

        populateMap(token, resource, result, request);

        Accumulator accumulator = getBrokenLinkAccumulator(token, resource, request, (String) result.get("linkType"));

        int page = 1;
        try {
            page = Integer.parseInt(request.getParameter("page"));
        } catch (Exception e) {
        }

        Map<String, CollectionStats> map = new LinkedHashMap<String, CollectionStats>();
        if ((pageSize * page) - pageSize < accumulator.map.size()) {
            URL currentPage = URL.create(request).removeParameter("page");
            if (page > 1) {
                result.put("prev", currentPage.addParameter("page", String.valueOf(page - 1)));
            }
            if (pageSize * page < accumulator.map.size()) {
                result.put("next", currentPage.addParameter("page", String.valueOf(page + 1)));
            }

            Iterator<Entry<String, CollectionStats>> it = accumulator.map.entrySet().iterator();
            int count = 0;
            Resource r;
            Path uri;
            CollectionStats cs;
            while (it.hasNext() && ++count <= pageSize * page) {
                Entry<String, CollectionStats> pairs = (Entry<String, CollectionStats>) it.next();
                if (count > (pageSize * page) - pageSize && count <= pageSize * page) {
                    cs = pairs.getValue();
                    uri = Path.fromString(pairs.getKey());
                    cs.url = getReportService().constructURL(uri).addParameter(REPORT_TYPE_PARAM, "broken-links");
                    try {
                        r = repository.retrieve(token, uri, false);
                        cs.title = r.getTitle();
                    } catch (Exception e) {
                        cs.title = uri.getName();
                    }
                    map.put(pairs.getKey(), cs);
                }
            }
            result.put("map", map);
        }

        result.put("sum", accumulator.sum);
        result.put("documentSum", accumulator.documentSum);

        if (accumulator.sum > 0) {
            String linkType = request.getParameter(FILTER_LINK_TYPE_PARAM_NAME);
            String published = request.getParameter(FILTER_PUBLISHED_PARAM_NAME);
            String readRestriction = request.getParameter(FILTER_READ_RESTRICTION_PARAM_NAME);

            if (linkType == null)
                linkType = FILTER_LINK_TYPE_PARAM_DEFAULT_VALUE;
            if (published == null)
                published = FILTER_PUBLISHED_PARAM_DEFAULT_VALUE;
            if (readRestriction == null)
                readRestriction = FILTER_READ_RESTRICTION_PARAM_DEFAULT_VALUE;

            Map<String, String> usedFilters = new LinkedHashMap<String, String>();
            usedFilters.put(FILTER_LINK_TYPE_PARAM_NAME, linkType);
            usedFilters.put(FILTER_PUBLISHED_PARAM_NAME, published);
            usedFilters.put(FILTER_READ_RESTRICTION_PARAM_NAME, readRestriction);

            URL exportURL = this.brokenLinksToTsvReportService.constructURL(resource, null, usedFilters, false);

            String[] exclude = request.getParameterValues(EXCLUDE_PATH_PARAM_NAME);
            if (exclude != null)
                for (String value : exclude)
                    exportURL.addParameter(EXCLUDE_PATH_PARAM_NAME, value);

            String[] include = request.getParameterValues(INCLUDE_PATH_PARAM_NAME);
            if (include != null)
                for (String value : include)
                    exportURL.addParameter(INCLUDE_PATH_PARAM_NAME, value);

            result.put("brokenLinksToTsvReportService", exportURL);
        }

        return result;
    }

    public Map<String, CollectionStats> getAccumulatorMap(String token, Resource currentResource,
            HttpServletRequest request) {
        String linkType = request.getParameter(FILTER_LINK_TYPE_PARAM_NAME);
        if (linkType == null)
            linkType = FILTER_LINK_TYPE_PARAM_DEFAULT_VALUE;

        return getBrokenLinkAccumulator(token, currentResource, request, linkType).map;
    }

    private Accumulator getBrokenLinkAccumulator(String token, Resource currentResource, HttpServletRequest request,
            final String linkType) {
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
            includeTypes = new String[] { "BROKEN_LINKS_ANCHOR", "BROKEN_LINKS_IMG" };
        } else if ("anchor".equals(linkType)) {
            includeTypes = new String[] { "BROKEN_LINKS_ANCHOR" };
        } else if ("img".equals(linkType)) {
            includeTypes = new String[] { "BROKEN_LINKS_IMG" };
        } else {
            includeTypes = new String[] { "BROKEN_LINKS" };
            excludeTypes = new String[] { "BROKEN_LINKS_IMG", "BROKEN_LINKS_ANCHOR" };
        }

        Map<String, CollectionStats> map = new TreeMap<String, CollectionStats>();
        for (Path uri : currentResource.getChildURIs()) {
            map.put(uri.toString(), new CollectionStats());
        }

        Accumulator accumulator = new Accumulator(includeTypes, excludeTypes, map,
                currentResource.getURI().getDepth() + 1);

        // No need to do search if no children
        if (map.isEmpty()) {
            return accumulator;
        }

        searcher.iterateMatching(token, search, accumulator);

        // Remove entries with value of 0 or less
        Iterator<Map.Entry<String, CollectionStats>> iter = accumulator.map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, CollectionStats> entry = iter.next();
            if (entry.getValue().linkCount <= 0) {
                iter.remove();
            }
        }

        return accumulator;
    }

    public class CollectionStats {
        int documentCount;
        int linkCount;
        String title;
        URL url;

        public CollectionStats() {
            this(0, 0);
        }

        public CollectionStats(int documentCount, int linkCount) {
            this.documentCount = documentCount;
            this.linkCount = linkCount;
        }

        public int getLinkCount() {
            return linkCount;
        }

        public int getDocumentCount() {
            return documentCount;
        }

        public String getTitle() {
            return title;
        }

        public URL getUrl() {
            return url;
        }
    }

    // Search callback which sums up broken link counts
    final class Accumulator implements Searcher.MatchCallback {
        int sum = 0, documentSum = 0;
        int depth, count, optInt;
        final String[] includeTypes;
        final String[] excludeTypes;
        Map<String, CollectionStats> map;

        Accumulator(String[] includeTypes, String[] excludeTypes, Map<String, CollectionStats> map, int depth) {
            this.depth = depth;
            this.includeTypes = includeTypes;
            this.excludeTypes = excludeTypes;
            this.map = map;
        }

        @Override
        public boolean matching(PropertySet propertySet) throws Exception {
            Property prop = propertySet.getProperty(brokenLinksCountPropDef);
            if (prop == null) {
                return true;
            }

            if (propertySet.getURI().getDepth() == depth) {
                return true;
            }

            CollectionStats cs = map.get(propertySet.getURI().getPath(depth).toString());
            if (cs == null) {
                return true;
            }

            count = 0;
            net.sf.json.JSONObject obj = prop.getJSONValue();
            for (String includeType : this.includeTypes) {
                optInt = obj.optInt(includeType);
                sum += optInt;
                cs.linkCount += optInt;
                count += optInt;
            }
            for (String excludeType : this.excludeTypes) {
                optInt = obj.optInt(excludeType);
                sum -= optInt;
                cs.linkCount -= optInt;
                count -= optInt;
            }
            if (count > 0) {
                cs.documentCount++;
                documentSum++;
            }

            return true;
        }
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Required
    public void setBrokenLinksToTsvReportService(Service brokenLinksToTsvReportService) {
        this.brokenLinksToTsvReportService = brokenLinksToTsvReportService;
    }

}
