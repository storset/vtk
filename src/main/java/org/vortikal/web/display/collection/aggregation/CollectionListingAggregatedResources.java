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
import java.util.Set;

import org.vortikal.repository.Path;
import org.vortikal.web.service.URL;

public class CollectionListingAggregatedResources implements Serializable {

    private static final long serialVersionUID = 1480429223516084662L;

    private Map<URL, Set<Path>> aggregationSet;
    private Set<URL> manuallyApprovedSet;

    public CollectionListingAggregatedResources(Map<URL, Set<Path>> aggregationSet, Set<URL> manuallyApprovedSet) {
        this.aggregationSet = aggregationSet;
        this.manuallyApprovedSet = manuallyApprovedSet;
    }

    public Map<URL, Set<Path>> getAggregationSet() {
        if (this.aggregationSet != null) {
            return Collections.unmodifiableMap(this.aggregationSet);
        }
        return null;
    }

    public Set<URL> getManuallyApproved() {
        if (this.manuallyApprovedSet != null) {
            return Collections.unmodifiableSet(this.manuallyApprovedSet);
        }
        return null;
    }

    public Set<Path> getHostAggregationSet(URL host) {
        if (this.aggregationSet != null) {
            Set<Path> set = this.aggregationSet.get(host);
            if (set != null) {
                return Collections.unmodifiableSet(this.aggregationSet.get(host));
            }
        }
        return null;
    }

    public Set<Path> getHostManuallyApprovedSet(URL host) {
        if (this.manuallyApprovedSet != null) {
            Set<Path> set = new HashSet<Path>();
            for (URL url : this.manuallyApprovedSet) {
                if (host.equals(url.relativeURL("/"))) {
                    set.add(url.getPath());
                }
            }
            if (!set.isEmpty()) {
                return Collections.unmodifiableSet(set);
            }
        }
        return null;
    }

    public boolean includesResourcesFromOtherHosts(URL currentURL) {
        if (this.aggregationSet != null) {
            for (URL url : this.aggregationSet.keySet()) {
                if (!url.equals(currentURL)) {
                    return true;
                }
            }
        }
        if (this.manuallyApprovedSet != null) {
            for (URL url : this.manuallyApprovedSet) {
                if (!url.relativeURL("/").equals(currentURL)) {
                    return true;
                }
            }
        }
        return false;
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
        if (this.aggregationSet != null) {
            sb.append("Aggregation set: ");
            sb.append(this.aggregationSet);
        }
        if (this.manuallyApprovedSet != null) {
            if (sb.toString().length() > 0) {
                sb.append("\n");
            }
            sb.append("Manually approved set: ");
            sb.append(this.manuallyApprovedSet);
        }
        return sb.toString();
    }

}
