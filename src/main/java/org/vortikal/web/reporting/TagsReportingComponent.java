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
package org.vortikal.web.reporting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.QueryException;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.web.display.collection.aggregation.AggregationResolver;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public class TagsReportingComponent {

    private Searcher searcher;
    private PropertyTypeDefinition tagsPropDef = null;
    private AggregationResolver aggregationResolver;
    private boolean caseInsensitive = true;
    private Ehcache cache;

    public static final class TagFrequency implements Serializable {

        private static final long serialVersionUID = -8618894865163460399L;
        private final String tag;
        private int frequency;

        private TagFrequency(String tag, int frequency) {
            this.tag = tag;
            this.frequency = frequency;
        }

        public String getTag() {
            return this.tag;
        }

        public int getFrequency() {
            return this.frequency;
        }

        @Override
        public String toString() {
            return tag + ":" + frequency;
        }

        private void increment() {
            ++this.frequency;
        }
    }

    // Tag frequency collector
    private class TagFrequencyCollector implements Searcher.MatchCallback {

        private final Map<String, TagFrequency> tagFreqMap = new HashMap<String, TagFrequency>();

        @Override
        public boolean matching(PropertySet propertySet) throws Exception {
            Property tags = propertySet.getProperty(tagsPropDef);
            if (tags != null) {
                if (tagsPropDef.isMultiple()) {
                    for (Value value : tags.getValues()) {
                        incrementTag(value.getStringValue());
                    }
                } else {
                    Value value = tags.getValue();
                    incrementTag(value.getStringValue());
                }
            }
            return true;
        }

        private void incrementTag(final String tagValue) {
            TagFrequency tf = this.tagFreqMap.get(tagValue);
            if (tf == null) {
                tf = new TagFrequency(tagValue, 1);
                this.tagFreqMap.put(tagValue, tf);
            } else {
                tf.increment();
            }
        }

        List<TagFrequency> getTagFreqList() {
            return new ArrayList<TagFrequency>(this.tagFreqMap.values());
        }
    }

    /**
     * Get list of TagFrequency instances for the given report criteria. The
     * list will always be sorted by frequency in descending order.
     * 
     * @param scopeUri
     * @param resourceTypeDefs
     * @param limit
     * @param tagOccurenceMin
     * @param token
     * @return
     * @throws QueryException
     */
    @SuppressWarnings("unchecked")
    public List<TagFrequency> getTags(Path scopeUri, List<ResourceTypeDefinition> resourceTypeDefs, int limit,
            int tagOccurenceMin, String token) throws QueryException {

        Set<String> rtNames = resourceTypeNames(resourceTypeDefs);
        final CacheKey cacheKey = new CacheKey(scopeUri, rtNames, limit, tagOccurenceMin, token);
        if (this.cache != null) {
            Element elem = this.cache.get(cacheKey);
            if (elem != null) {
                return (List<TagFrequency>) elem.getValue();
            }
        }

        OrQuery pathScopeQuery = null;
        if (scopeUri != null && !scopeUri.isRoot()) {
            if (pathScopeQuery == null) {
                pathScopeQuery = new OrQuery();
            }

            pathScopeQuery.add(new UriPrefixQuery(scopeUri.toString()));

            // If we have an aggregation resolver available, then include
            // whatever URIs the scope URI might aggregate from.
            if (this.aggregationResolver != null) {
                Set<Path> aggregationPaths = this.aggregationResolver.getAggregationPaths(scopeUri);
                if (aggregationPaths != null) {
                    for (Path p : aggregationPaths) {
                        pathScopeQuery.add(new UriPrefixQuery(p.toString()));
                    }
                }
            }
        }

        OrQuery typeScopeQuery = null;
        if (rtNames != null && !rtNames.isEmpty()) {
            if (typeScopeQuery == null)
                typeScopeQuery = new OrQuery();

            for (String rtName : rtNames) {
                // Consider TermOperator.IN to get hierarchical type support
                typeScopeQuery.add(new TypeTermQuery(rtName, TermOperator.EQ));
            }
        }

        Query masterScopeQuery = pathScopeQuery;
        if (typeScopeQuery != null) {
            if (masterScopeQuery != null) {
                AndQuery andQuery = new AndQuery();
                andQuery.add(masterScopeQuery);
                andQuery.add(typeScopeQuery);
                masterScopeQuery = andQuery;
            } else {
                masterScopeQuery = typeScopeQuery;
            }
        }

        // Set up index search
        Search search = new Search();
        search.setQuery(masterScopeQuery);
        search.setSorting(null);
        search.setLimit(Integer.MAX_VALUE);
        search.setOnlyPublishedResources(true);
        ConfigurablePropertySelect propSelect = new ConfigurablePropertySelect();
        propSelect.addPropertyDefinition(tagsPropDef);
        search.setPropertySelect(propSelect);

        TagFrequencyCollector tfc = new TagFrequencyCollector();
        // Execute index iteration and collect/aggregate tag frequencies
        this.searcher.iterateMatching(token, search, tfc);
        List<TagFrequency> tagFreqs = tfc.getTagFreqList();

        // Case insensitive value consolidation
        if (this.caseInsensitive) {
            tagFreqs = consolidateCaseVariations(tagFreqs);
        }

        // Minimum frequency criterium
        if (tagOccurenceMin > -1) {
            tagFreqs = filterBelowMinFreq(tagFreqs, tagOccurenceMin);
        }

        // Sort by highest frequency descending
        Collections.sort(tagFreqs, new Comparator<TagFrequency>() {
            @Override
            public int compare(TagFrequency o1, TagFrequency o2) {
                return o1.frequency < o2.frequency ? 1 : (o1.frequency == o2.frequency ? 0 : -1);
            }
        });

        // Apply any limit
        if (limit > -1) {
            limit = Math.min(limit, tagFreqs.size());
            tagFreqs = tagFreqs.subList(0, limit);
        }

        tagFreqs = Collections.unmodifiableList(tagFreqs);

        // Populate cache
        if (this.cache != null) {
            this.cache.put(new Element(cacheKey, tagFreqs));
        }

        return tagFreqs;
    }

    private Set<String> resourceTypeNames(List<ResourceTypeDefinition> defs) {
        if (defs == null)
            return null;
        Set<String> defNames = new HashSet<String>();
        for (ResourceTypeDefinition def : defs) {
            defNames.add(def.getName());
        }

        return defNames;
    }

    private List<TagFrequency> consolidateCaseVariations(List<TagFrequency> tagFreqs) {
        Map<String, List<TagFrequency>> m = new HashMap<String, List<TagFrequency>>(tagFreqs.size() + 50);
        for (TagFrequency tf : tagFreqs) {
            final String key = tf.tag.toLowerCase();
            List<TagFrequency> variations = m.get(key);
            if (variations == null) {
                variations = new ArrayList<TagFrequency>();
                m.put(key, variations);
            }
            variations.add(tf);
        }

        List<TagFrequency> retVal = new ArrayList<TagFrequency>();
        for (List<TagFrequency> variations : m.values()) {
            int sum = 0;
            int maxFreq = -1;
            String mostCommonVariant = null;
            for (TagFrequency variant : variations) {
                sum += variant.frequency;
                if (variant.frequency > maxFreq) {
                    maxFreq = variant.frequency;
                    mostCommonVariant = variant.tag;
                }
            }
            retVal.add(new TagFrequency(mostCommonVariant, sum));
        }
        return retVal;
    }

    private List<TagFrequency> filterBelowMinFreq(List<TagFrequency> tagFreqs, int minFreq) {
        for (Iterator<TagFrequency> it = tagFreqs.iterator(); it.hasNext();) {
            TagFrequency tf = it.next();
            if (tf.frequency < minFreq) {
                it.remove();
            }
        }
        return tagFreqs;
    }

    private static final class CacheKey implements Serializable {

        private static final long serialVersionUID = -8898229960525592912L;
        private final Path scopeUri;
        private final Set<String> resourceTypes;
        private final int limit;
        private final int minFreq;
        private final String token;

        CacheKey(Path scopeUri, Set<String> resourceTypes, int limit, int minFreq, String token) {
            this.scopeUri = scopeUri;
            this.resourceTypes = resourceTypes;
            this.limit = limit;
            this.minFreq = minFreq;
            this.token = token;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CacheKey other = (CacheKey) obj;
            if ((this.token == null) ? (other.token != null) : !this.token.equals(other.token)) {
                return false;
            }
            if (this.scopeUri != other.scopeUri && (this.scopeUri == null || !this.scopeUri.equals(other.scopeUri))) {
                return false;
            }
            if (this.resourceTypes != other.resourceTypes
                    && (this.resourceTypes == null || !this.resourceTypes.equals(other.resourceTypes))) {
                return false;
            }
            if (this.limit != other.limit) {
                return false;
            }
            if (this.minFreq != other.minFreq) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.scopeUri != null ? this.scopeUri.hashCode() : 0);
            hash = 29 * hash + (this.resourceTypes != null ? this.resourceTypes.hashCode() : 0);
            hash = 29 * hash + this.limit;
            hash = 29 * hash + this.minFreq;
            hash = 29 * hash + (this.token != null ? this.token.hashCode() : 0);
            return hash;
        }
    }

    @Required
    public void setTagsPropDef(PropertyTypeDefinition tagsPropDef) {
        this.tagsPropDef = tagsPropDef;
    }

    @Required
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    public void setAggregationResolver(AggregationResolver aggregationResolver) {
        this.aggregationResolver = aggregationResolver;
    }

    public void setCache(Ehcache cache) {
        this.cache = cache;
    }

    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }
}
