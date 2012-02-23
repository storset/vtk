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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.reporting.DataReportException;
import org.vortikal.repository.reporting.DataReportManager;
import org.vortikal.repository.reporting.PropertyValueFrequencyQuery;
import org.vortikal.repository.reporting.PropertyValueFrequencyQueryResult;
import org.vortikal.repository.reporting.ResourceTypeScope;
import org.vortikal.repository.reporting.UriPrefixScope;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.web.display.collection.aggregation.AggregationResolver;

public class TagsReportingComponent {

    private DataReportManager dataReportManager;
    private PropertyTypeDefinition tagsPropDef = null;
    private AggregationResolver aggregationResolver;

    public PropertyValueFrequencyQueryResult getTags(Path scopeUri, List<ResourceTypeDefinition> resourceTypeDefs,
            int limit, int tagOccurenceMin, String token) throws DataReportException, IllegalArgumentException {

        PropertyValueFrequencyQuery query = new PropertyValueFrequencyQuery();
        query.setPropertyTypeDefinition(this.tagsPropDef);
        query.setCaseInsensitive(true);

        // Sort by highest frequency first.
        query.setOrdering(PropertyValueFrequencyQuery.Ordering.DESCENDING_BY_FREQUENCY);

        if (limit > -1)
            query.setLimit(limit);

        if (tagOccurenceMin > -1)
            query.setMinValueFrequency(tagOccurenceMin);

        if (scopeUri != null && !scopeUri.isRoot()) {
            UriPrefixScope scope = new UriPrefixScope();
            scope.addUriPrefix(scopeUri);

            // If we have an aggregation resolver available, then include whatever URIs
            // the scope URI might aggregate from.
            if (this.aggregationResolver != null) {
                Set<Path> aggregationPaths = this.aggregationResolver.getAggregationPaths(scopeUri);
                if (aggregationPaths != null) {
                    for (Path p : aggregationPaths) {
                        scope.addUriPrefix(p);
                    }
                }
            }

            query.addScope(scope);
        }

        if (resourceTypeDefs != null && resourceTypeDefs.size() > 0) {
            ResourceTypeScope rtscope = new ResourceTypeScope(
                        new HashSet<ResourceTypeDefinition>(resourceTypeDefs));
            query.addScope(rtscope);
        }
        
        return (PropertyValueFrequencyQueryResult) this.dataReportManager.executeReportQuery(query, token);
    }

    @Required
    public void setTagsPropDef(PropertyTypeDefinition tagsPropDef) {
        this.tagsPropDef = tagsPropDef;
    }

    @Required
    public void setDataReportManager(DataReportManager manager) {
        this.dataReportManager = manager;
    }

    /**
     * @param aggregationResolver the aggregationResolver to set
     */
    public void setAggregationResolver(AggregationResolver aggregationResolver) {
        this.aggregationResolver = aggregationResolver;
    }

}
