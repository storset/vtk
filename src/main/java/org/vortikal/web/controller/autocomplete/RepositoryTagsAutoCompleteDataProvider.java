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
package org.vortikal.web.controller.autocomplete;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.reporting.DataReportException;
import org.vortikal.repository.reporting.Pair;
import org.vortikal.repository.reporting.PropertyValueFrequencyQueryResult;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.reporting.TagsReportingComponent;


/**
 * Provide keywords completion data from repository.
 *
 */
public class RepositoryTagsAutoCompleteDataProvider implements
        AutoCompleteDataProvider {
    
    private final Log logger = LogFactory.getLog(getClass());
    
    private TagsReportingComponent tagsReporter;

    private boolean filterByPrefix = true;

    /**
     * @see AutoCompleteDataProvider#getPrefixCompletions(String, String)
     */
    public List<Object> getPrefixCompletions(String prefix, 
                                             Path contextUri, 
                                             String token) {
        
        List<Object> repositoryKeywords = getRepositoryKeywords(contextUri, token);
        
        if (this.filterByPrefix) {
            filterByPrefix(prefix, repositoryKeywords);
        }

        return repositoryKeywords;
    }
    
    // Fetch list of all existing unique repository keywords, sorted
    // by frequency, with most frequent on top.
    private List<Object> getRepositoryKeywords(Path scopeUri, String token) {
     
        // TODO might consider adding limit on number of unique keywords that are 
        //  fetched.
        try {
            PropertyValueFrequencyQueryResult pfqResult =
                tagsReporter.getTags(scopeUri, -1, -1, token);
                
            List<Pair<Value, Integer>> pfqList = pfqResult.getValueFrequencyList();

            List<Object> result = new ArrayList<Object>(pfqList.size());
            
            for (Pair<Value, Integer> pair: pfqList) {
                result.add(new Tag(pair.first().getStringValue()));
            }
            
            return result;
        } catch (DataReportException de) {
            logger.warn("Failed to execute data report query", de);
            
            // Return empty list when failed, for now.
            return new ArrayList<Object>(0);
        }
    }
    
    // Filter *case-insensitively* by prefix
    private void filterByPrefix(String prefix, List<Object> list) {
        List<Object> filteredKeywords = new ArrayList<Object>();
        for (Object obj : list) {
            String keyword = ((Tag) obj).getKeyword();
            if (!(prefix.length() <= keyword.length()
                    && keyword.substring(0, prefix.length()).equalsIgnoreCase(prefix))) {
                filteredKeywords.add(obj);
            }
        }
        list.removeAll(filteredKeywords);
    }

    public void setFilterByPrefix(boolean filterByPrefix) {
        this.filterByPrefix = filterByPrefix;
    }
    
    public class Tag {
        
        private String keyword;
        
        public Tag(String keyword) {
            this.keyword = keyword;
        }
        
        public String getKeyword() {
            return this.keyword;
        }
        
    }

    @Required
    public void setTagsReporter(TagsReportingComponent tagsReporter) {
        this.tagsReporter = tagsReporter;
    }

}
