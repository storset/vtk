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
package org.vortikal.repository.reporting;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.store.DataReportDAO;
import org.vortikal.util.cache.SimpleCache;

/**
 * Repository data report manager.
 * Handles:
 *  - Executing report queries
 *  - Caching of results (optional)
 * 
 * XXX: No authorization of any kind if currently done in database DAO.
 *      Index report DAO does proper ACL filtering for ResourceReadableACLScope scope.
 * 
 */
public class DataReportManagerImpl implements
        DataReportManager {

    private final Log logger = LogFactory.getLog(DataReportManagerImpl.class);
    
    private DataReportDAO dao;
    private SimpleCache<ReportQuery, ReportResult> cache = null;
    private List<ReportScope> defaultStaticScoping;

    public ReportResult executeReportQuery(ReportQuery query, String token) {
        if (query == null){
            throw new IllegalArgumentException("Query cannot be null");
        }

        // Add ACL scoping (dynamic on token)
        query.addScope(new ResourceReadableACLScope(token));

        // Add default static scoping (if any is configured)
        if (this.defaultStaticScoping != null) {
            for (ReportScope scope: this.defaultStaticScoping) {
                query.addScope(scope);
            }
        }
        
        if (this.cache != null) {
            ReportQuery key = (ReportQuery)query.clone();
            
            ReportResult result = this.cache.get(key);
            if (result != null) {
                this.logger.debug("Got report query result from cache.");
                return result;
            } else {
                this.logger.debug("No result found in cache for query, dispatching to DAO");
                result = dispatchQuery(query, token);
                this.cache.put(key, result);
            }
            
            return result;
        } else {
            return dispatchQuery(query, token);
        }
    }
    
    private ReportResult dispatchQuery(ReportQuery query, String token) {
        
        try {
            if (query instanceof PropertyValueFrequencyQuery) {
                List<Pair<Value, Integer>> result = 
                    this.dao.executePropertyFrequencyValueQuery((PropertyValueFrequencyQuery)query);
                
                PropertyValueFrequencyQueryResultImpl res = 
                    new PropertyValueFrequencyQueryResultImpl(
                            (PropertyValueFrequencyQuery)query);
                
                res.setValueFrequencyList(result);
                
                return res;
            }
        } catch (Exception e){
            this.logger.warn("Exception while dispatching report query", e);
            throw new DataReportException("Got an exception while dispatching query: ", e);
        }
        
        throw new DataReportException("Unsupported report query type: " 
                                                            + query.getClass());
    }

    /**
     * Default scopes that will be inserted in scope for all executed queries.
     * @param defaultScoping the defaultScoping to set.
     */
    public void setDefaultStaticScoping(List<ReportScope> defaultStaticScoping) {
        this.defaultStaticScoping = defaultStaticScoping;
    }
    
    @Required
    public void setDataReportDAO(DataReportDAO dao) {
        this.dao = dao;
    }

    public void setCache(SimpleCache<ReportQuery, ReportResult> cache) {
        this.cache = cache;
    }
}
