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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;

public class BrokenLinksCollectionReport extends BrokenLinksReport {

    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) {
        Map<String, Object> result = super.getReportContent(token, resource, request);

        String linkType = request.getParameter(FILTER_LINK_TYPE_PARAM_NAME);
        if (linkType == null)
            linkType = FILTER_LINK_TYPE_PARAM_DEFAULT_VALUE;

        Accumulator accumulator = getBrokenLinkCount(token, resource, request, linkType);

        result.put("map", accumulator.map);
        result.put("sum", accumulator.sum);
        result.put("documentSum", accumulator.documentSum);

        return result;
    }

    private Accumulator getBrokenLinkCount(String token, Resource currentResource, HttpServletRequest request,
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
        Path uri;
        int documentCount;
        int linkCount;

        public CollectionStats() {
            this(null, 0, 0);
        }

        public CollectionStats(Path uri) {
            this(uri, 0, 0);
        }

        public CollectionStats(Path uri, int documentCount, int linkCount) {
            this.documentCount = documentCount;
            this.linkCount = linkCount;
        }

        public Path getUri() {
            return uri;
        }

        public int getLinkCount() {
            return linkCount;
        }

        public int getDocumentCount() {
            return documentCount;
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

}
