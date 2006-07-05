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

package org.vortikal.repositoryimpl.index.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Hits;
import org.vortikal.repositoryimpl.index.IndexConstants;
import org.vortikal.repositoryimpl.index.ModifiableResults;
import org.vortikal.repositoryimpl.index.ResultMetadata;


/**
 * Cached list of search results generated from a Lucene <code>Hits</code> object.
 * The <code>IndexReader</code> used to produce the hits can be safely closed after 
 * this object has been instantiated.
 *
 * @author oyviste
 */
public class CachedLuceneResults implements ModifiableResults {
    
    private Class resultClass = null;
    private List results = new ArrayList();
    private List resultsMetadata = new ArrayList();
        
    /**
     * Construct an empty result set.
     * @param resultClass Class of result objects.
     */
    public CachedLuceneResults(Class resultClass) {
        this.resultClass = resultClass;
    }
    
    /**
     * Construct a result set from a Lucene <code>Hits</code> object.
     * @param hits Lucene <code>Hits</code> object to obtain results from.
     * @param resultClass Class of result objects.
     */
    public CachedLuceneResults(Hits hits, Class resultClass) throws IOException {
        this(resultClass);
        generateResults(hits, resultClass, Integer.MAX_VALUE, 0);
    }
    
    public CachedLuceneResults(Hits hits, Class resultClass, int maxResults, 
                                int cursor) throws IOException {
        this(resultClass);
        generateResults(hits, resultClass, maxResults, cursor);
    }

    public Class getResultClass() {
        return this.resultClass;
    }
    
    public int getSize() {
        return this.results.size();
    }

    public ResultMetadata getResultMetadata(int index) {
        return (ResultMetadata) this.resultsMetadata.get(index);
    }
    
    /**
     * Generate and cache result set as instantiated beans with metadata.
     */
    private void generateResults(Hits hits, Class resultClass, int maxResults, 
        int cursor) throws IOException {
        
        if (cursor > hits.length()) {
            throw new IllegalArgumentException(
                    "Cursor is bigger than the number of hits."
                            + "The cursor is zero-based.");
        }

        if (maxResults < 0) maxResults = 0;
        int endPos = cursor + maxResults;
        for (int i = cursor; i < hits.length() && i < endPos; i++) {
            Document doc = hits.doc(i);
            Object indexBean = IndexDocumentUtil.createIndexBean(doc,
                    resultClass);
            if (indexBean != null) {
                this.results.add(indexBean);
                this.resultsMetadata.add(new ResultMetadata(doc.get(IndexConstants.URI_FIELD), 
                                    hits.score(i)));
            }
        }
    }

    public Object getResult(int index) {
        return this.results.get(index);
    }
    
    public List getResults(int maxResults) {
        int len = Math.min(Math.max(0, maxResults), this.results.size());
        List list = new ArrayList(len);
        
        for (int i = 0; i < len; i++) {
            list.add(this.results.get(i));
        }
        
        return list;
    }
    
    public List getAllResults() {
        return new ArrayList(this.results);
    }
    
    public List getAllResultsMetadata() {
        return new ArrayList(this.resultsMetadata);
    }
    
    public void removeResult(int index) {
        this.results.remove(index);
        this.resultsMetadata.remove(index);
    }
    
    public void appendHits(Hits hits, int maxResults, int cursor) throws IOException {
        generateResults(hits, this.resultClass, maxResults, cursor);
    }
    
    public void appendHits(Hits hits) throws IOException {
        generateResults(hits, this.resultClass, Integer.MAX_VALUE, 0);
    }
}

