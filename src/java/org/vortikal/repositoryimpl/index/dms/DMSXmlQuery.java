/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.index.dms;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.query.Condition;
import org.vortikal.repository.query.ParsedQueryCondition;
import org.vortikal.repository.query.Query;
import org.vortikal.repository.query.QueryException;
import org.vortikal.repository.query.Searcher;
import org.vortikal.repository.query.Sort;
import org.vortikal.repository.query.SortField;
import org.vortikal.repositoryimpl.index.Results;
import org.vortikal.repositoryimpl.index.XmlResultsUtil;


/**
 * DMS queries with XML output.
 * Uses the new org.vortikal.index.Searcher interface, and is completely
 * Lucene indepedent. However, the query syntax is exactly like the one
 * used in Lucene (guess why).
 *  
 * @author oyviste
 *
 */
public class DMSXmlQuery implements InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    public static final String STATUS_OK = "OK";
    public static final String STATUS_FAIL = "ERROR";
    
    private Searcher searcher;
    
    public void afterPropertiesSet() {
        if (this.searcher == null) {
            throw new BeanInitializationException("Required property 'searcher' not set.");
        }
    }
    
    public DMSXmlQuery() {}
    
    public Document executeQuery(String queryString, String token) {
        return executeQuery(queryString, false, token, -1, null);
    }
    
    public Document executeQuery(String queryString, int maxResults) {
        return executeQuery(queryString, false, null, -1, null);
    }
    
    public Document executeQuery(String queryString, String token, int maxResults,
                                 String sortFields) {
        return executeQuery(queryString, false, token, maxResults, sortFields);
    }
    
    public Document executeQuery(String queryString, boolean multiField, String token) {
        return executeQuery(queryString, multiField, token, -1, null);
    }
    

    /**
     * Executes a search query.
     *
     * @param queryString the search string. The syntax is that of
     * {@link ParsedQueryCondition}.
     * @param multiField 
     * @param token
     * @param maxResults
     * @param sortFields specifies sorting. The syntax is
     * <code>field(:asc|:desc)?(,field(:asc|:desc)?)*</code>, or
     * <code>null</code> if the default sorting should be used.
     * @return a <code>Document</code>
     */
    public Document executeQuery(String queryString, boolean multiField, String token,
                                 int maxResults, String sortFields) {
     
        Condition parsedCondition = new ParsedQueryCondition(queryString, multiField);
        Query query = new Query(parsedCondition);
        
        if (sortFields != null) {
            Sort sort = parseSortFields(sortFields);
            if (sort != null) {
                query.setSort(sort);
            }
        }

        Element rootElement = new Element("query");
        Element statusElement = new Element("status");
        rootElement.addContent(statusElement);
        Document doc = new Document(rootElement);
        Element resultsElement = new Element("results");
        rootElement.addContent(resultsElement);
        
        Results results = null;
        try {
            
            if (maxResults > 0) {                
                results = searcher.execute(token, query, maxResults);
            } else {
                results = searcher.execute(token, query);
            }
            
        } catch (QueryException qe) {
            statusElement.setAttribute(new Attribute("code", STATUS_FAIL));
            statusElement.addContent(qe.getMessage());
            return doc;
        }
        
        Document xmlResults = null;
        try {
            xmlResults = XmlResultsUtil.generateXMLResults(results);
        } catch (Exception e) {
            statusElement.setAttribute(new Attribute("code", STATUS_FAIL));
            statusElement.addContent(e.getMessage());
            return doc;
        }
        
        Element xmlResultsRootElement = xmlResults.getRootElement();
        resultsElement.addContent(xmlResultsRootElement.cloneContent());
        resultsElement.setAttribute(new Attribute("hits", 
                Integer.toString(xmlResultsRootElement.getContentSize())));
        statusElement.setAttribute(new Attribute("code", STATUS_OK));

        if (logger.isDebugEnabled()) {
            logger.debug("Performed  search for query '" + query
                         + "', returning document '" + doc + "'");
        }


        return doc;
    }
    

    /**
     * Parses a sort specification of the syntax
     * <code>field(:asc|:desc)?(,field(:asc|:desc)?)*</code> and
     * produces a {@link Sort} object.
     *
     * @param sortFields the sort specification
     * @return a sort object, or <code>null</code> if the string does
     * not contain any valid sort fields.
     */
    public static Sort parseSortFields(String sortFields) {

        if (sortFields == null || "".equals(sortFields.trim())) {
            return null;
        }
        

        String[] fields = sortFields.split(",");
        List result = new ArrayList();
        
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i].trim();
            boolean invert = false;
            
            int separatorIdx = field.indexOf(":");
            if (separatorIdx != -1) {
                if (separatorIdx == 0 || separatorIdx == field.length() - 1) {
                    // Skip field
                    continue;
                }
                String modifier = field.substring(separatorIdx + 1).trim();
                field = field.substring(0, separatorIdx).trim();
                if ("descending".startsWith(modifier)) {
                    invert = true;
                }
            }
            
            SortField sortField = new SortField(field, invert);
            result.add(sortField);
        }

        if (result.isEmpty()) {
            return null;
        }

        Sort sort = new Sort((SortField[]) result.toArray(new SortField[result.size()]));
        return sort;
    }
    

    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }
    
}
