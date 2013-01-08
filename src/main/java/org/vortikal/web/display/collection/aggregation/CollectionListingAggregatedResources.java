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
package org.vortikal.web.display.collection.aggregation;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.vortikal.repository.Path;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.repository.search.query.UriSetQuery;
import org.vortikal.web.search.VHostScopeQueryRestricter;
import org.vortikal.web.service.URL;

public class CollectionListingAggregatedResources implements Serializable {

    private static final long serialVersionUID = 1480429223516084662L;

    private Map<URL, Set<Path>> aggregationSet;
    private Map<URL, Set<Path>> manuallyApprovedSet;

    public Map<URL, Set<Path>> getAggregationSet() {
        if (this.aggregationSet != null) {
            return Collections.unmodifiableMap(this.aggregationSet);
        }
        return null;
    }

    public void setAggregationSet(Map<URL, Set<Path>> aggregationSet) {
        this.aggregationSet = aggregationSet;
    }

    public Map<URL, Set<Path>> getManuallyApproved() {
        if (this.manuallyApprovedSet != null) {
            return Collections.unmodifiableMap(this.manuallyApprovedSet);
        }
        return null;
    }

    public void setManuallyApprovedSet(Map<URL, Set<Path>> manuallyApprovedSet) {
        this.manuallyApprovedSet = manuallyApprovedSet;
    }

    public Set<Path> getHostAggregationSet(URL host) {
        return getHostResolvedSet(host, aggregationSet);
    }

    public Set<Path> getHostManuallyApprovedSet(URL host) {
        return getHostResolvedSet(host, manuallyApprovedSet);
    }

    private Set<Path> getHostResolvedSet(URL host, Map<URL, Set<Path>> aggregatedResourceMap) {
        if (aggregatedResourceMap != null) {
            Set<Path> result = new HashSet<Path>();
            for (Entry<URL, Set<Path>> entry : aggregatedResourceMap.entrySet()) {
                if (host.getHost().equals(entry.getKey().getHost())) {
                    result.addAll(entry.getValue());
                }
            }
            if (!result.isEmpty()) {
                return Collections.unmodifiableSet(result);
            }
        }
        return null;
    }

    public boolean includesResourcesFromOtherHosts(URL host) {
        return includesOtherHostRef(host, aggregationSet) || includesOtherHostRef(host, manuallyApprovedSet);
    }

    public int totalAggregatedResourceCount() {

        int count = 0;

        if ((aggregationSet == null || aggregationSet.size() == 0)
                && (manuallyApprovedSet == null || manuallyApprovedSet.size() == 0)) {
            return count;
        }

        count += aggregatedResourcesCount(null, aggregationSet);
        count += aggregatedResourcesCount(null, manuallyApprovedSet);

        return count;
    }

    public int aggregatedResourcesCount(URL host, Map<URL, Set<Path>> aggregatedResourceMap) {

        int count = 0;

        if (aggregatedResourceMap == null) {
            return count;
        }

        if (host != null) {
            Set<Path> set = getHostResolvedSet(host, aggregatedResourceMap);
            if (set != null) {
                return set.size();
            }
            return count;
        }

        for (Entry<URL, Set<Path>> entry : aggregatedResourceMap.entrySet()) {
            count += entry.getValue().size();
        }

        return count;
    }

    private boolean includesOtherHostRef(URL host, Map<URL, Set<Path>> aggregatedResourceMap) {
        if (aggregatedResourceMap != null) {
            for (URL url : aggregatedResourceMap.keySet()) {
                if (!url.getHost().equals(host.getHost())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * XXX Shouldn't be here, refactor to separate class
     */
    public Query getAggregationQuery(URL localVhost, boolean isMultiHostSearch) {

        // Nothing to generate query from
        if ((aggregationSet == null || aggregationSet.size() == 0)
                && (manuallyApprovedSet == null || manuallyApprovedSet.size() == 0)) {
            return null;
        }

        Query aggregationQuery = null;
        Query manuallyApprovedQuery = null;

        // Handle simplest case first: local repo query
        if (!isMultiHostSearch) {

            // Need ref to local host if not multi host search
            if (localVhost == null) {
                return null;
            }

            Set<Path> localHostAggregation = getHostAggregationSet(localVhost);
            Set<Path> localHostManuallyApproved = getHostManuallyApprovedSet(localVhost);

            if (localHostAggregation == null && localHostManuallyApproved == null) {
                return null;
            }

            aggregationQuery = aggregationQuery(null, localHostAggregation);
            manuallyApprovedQuery = manuallyApprovedQuery(null, localHostManuallyApproved);

        } else {

            if (aggregationSet != null && aggregationSet.size() > 0) {

                if (aggregationSet.size() == 1) {
                    Entry<URL, Set<Path>> entry = aggregationSet.entrySet().iterator().next();
                    aggregationQuery = aggregationQuery(entry.getKey(), entry.getValue());
                } else {
                    OrQuery aggOr = new OrQuery();
                    for (Entry<URL, Set<Path>> entry : aggregationSet.entrySet()) {
                        aggOr.add(aggregationQuery(entry.getKey(), entry.getValue()));
                    }
                    aggregationQuery = aggOr;
                }

            }

            if (manuallyApprovedSet != null && manuallyApprovedSet.size() > 0) {

                if (manuallyApprovedSet.size() == 1) {
                    Entry<URL, Set<Path>> entry = manuallyApprovedSet.entrySet().iterator().next();
                    manuallyApprovedQuery = manuallyApprovedQuery(entry.getKey(), entry.getValue());
                } else {
                    OrQuery manOr = new OrQuery();
                    for (Entry<URL, Set<Path>> entry : manuallyApprovedSet.entrySet()) {
                        manOr.add(manuallyApprovedQuery(entry.getKey(), entry.getValue()));
                    }
                    manuallyApprovedQuery = manOr;
                }

            }

        }

        if (aggregationQuery == null && manuallyApprovedQuery == null) {
            return null;
        }

        if (aggregationQuery != null && manuallyApprovedQuery == null) {
            return aggregationQuery;
        }

        if (aggregationQuery == null && manuallyApprovedQuery != null) {
            return manuallyApprovedQuery;
        }

        OrQuery or = new OrQuery();
        or.add(aggregationQuery);
        or.add(manuallyApprovedQuery);
        return or;

    }

    private Query aggregationQuery(URL vhost, Set<Path> aggregationSet) {

        if (aggregationSet == null) {
            return null;
        }

        Query query = null;
        if (aggregationSet.size() == 1) {
            query = new UriPrefixQuery(aggregationSet.iterator().next().toString());
        } else {
            OrQuery or = new OrQuery();
            for (Path path : aggregationSet) {
                or.add(new UriPrefixQuery(path.toString()));
            }
            query = or;
        }

        if (vhost == null) {
            return query;
        }

        return VHostScopeQueryRestricter.vhostRestrictedQuery(query, vhost);

    }

    private Query manuallyApprovedQuery(URL vhost, Set<Path> manuallyApprovedSet) {

        if (manuallyApprovedSet == null) {
            return null;
        }

        Query query = null;
        Set<String> uriSet = new HashSet<String>();
        for (Path manuallyApprovedPath : manuallyApprovedSet) {
            uriSet.add(manuallyApprovedPath.toString());
        }
        query = new UriSetQuery(uriSet);

        if (vhost == null) {
            return query;
        }

        return VHostScopeQueryRestricter.vhostRestrictedQuery(query, vhost);

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CollectionListingAggregatedResources)) {
            return false;
        }
        return this.toString().equals(((CollectionListingAggregatedResources) obj).toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.aggregationSet != null && this.aggregationSet.size() > 0) {
            sb.append("Aggregation set: ");
            sb.append(this.aggregationSet);
        }
        if (this.manuallyApprovedSet != null && this.manuallyApprovedSet.size() > 0) {
            if (sb.toString().length() > 0) {
                sb.append("\n");
            }
            sb.append("Manually approved set: ");
            sb.append(this.manuallyApprovedSet);
        }
        return sb.toString();
    }

}
