/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.query;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.query.QueryException;
import org.vortikal.repositoryimpl.query.query.Query;
import org.vortikal.repositoryimpl.query.security.QueryAuthorizationManager;
import org.vortikal.repositoryimpl.queryparser.ResultSet;
import org.vortikal.repositoryimpl.queryparser.ResultSetImpl;
import org.vortikal.repositoryimpl.queryparser.Searcher;

/**
 * @author oyviste
 *
 */
public class SearcherImpl implements Searcher, InitializingBean {

    Log logger = LogFactory.getLog(SearcherImpl.class);
    
    private LuceneIndex index;
    private DocumentMapper documentMapper;
    private QueryAuthorizationManager queryAuthorizationManager;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (index == null) {
            throw new BeanInitializationException("Property 'index' not set.");
        } else if (documentMapper == null) {
            throw new BeanInitializationException("Property 'documentMapper' not set.");
        }
        
        if (this.queryAuthorizationManager == null) {
            logger.warn("No authorization manager configured, queries will not be filtered with regard to"
                    + " permissions.");
        }
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.repositoryimpl.queryparser.Searcher#execute(java.lang.String, org.vortikal.repositoryimpl.query.query.Query)
     */
    public ResultSet execute(String token, Query query) throws QueryException {
        
        org.apache.lucene.search.Query q = new QueryTreeBuilder(query).buildQuery();
        IndexSearcher searcher = null;
        try {
            searcher = index.getIndexSearcher();
            
            Hits hits = searcher.search(q);
            
            ResultSet rs = buildResultSet(hits, token);
            
            return rs;
        } catch (IOException io) {
            logger.warn("IOException while performing query on index", io);
            throw new QueryException("IOException while performing query on index", io);
        } finally {
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (IOException io){}
            }
        }
    }

    /* (non-Javadoc)
     * @see org.vortikal.repositoryimpl.queryparser.Searcher#execute(java.lang.String, org.vortikal.repositoryimpl.query.query.Query, int)
     */
    public ResultSet execute(String token, Query query, int maxResults)
            throws QueryException {
        throw new UnsupportedOperationException("Searching with max results not yet implemented, coming soon.");
    }

    /* (non-Javadoc)
     * @see org.vortikal.repositoryimpl.queryparser.Searcher#execute(java.lang.String, org.vortikal.repositoryimpl.query.query.Query, int, int)
     */
    public ResultSet execute(String token, Query query, int maxResults, int cursor)
            throws QueryException {
        throw new UnsupportedOperationException("Searching with max results and cursor not yet implemented, coming soon.");
    }

    
    private ResultSet buildResultSet(Hits hits, String token) throws IOException {
        
        ResultSetImpl rs = new ResultSetImpl();
        for (int i=0; i<hits.length(); i++) {
            Document doc = hits.doc(i);
            if (this.queryAuthorizationManager != null) {
                try {
                    this.queryAuthorizationManager.authorizeQueryResult(token, 
                                        FieldMapper.getIntegerFromUnencodedField(doc.getField(DocumentMapper.ID_FIELD_NAME)),
                                        FieldMapper.getIntegerFromUnencodedField(doc.getField(DocumentMapper.ACL_INHERITED_FROM_FIELD_NAME)));
                } catch (AuthorizationException authEx) {
                    continue;
                }
            }
            
            PropertySet propSet = this.documentMapper.getPropertySet(doc);
            rs.addResult(propSet);
        }
        
        return rs;
    }

    public void setDocumentMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public void setIndex(LuceneIndex index) {
        this.index = index;
    }

    public void setQueryAuthorizationManager(QueryAuthorizationManager queryAuthorizationManager) {
        this.queryAuthorizationManager = queryAuthorizationManager;
    }
}
