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
package org.vortikal.repository.query;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.index.IndexConstants;
import org.vortikal.repositoryimpl.index.ModifiableResults;
import org.vortikal.repositoryimpl.index.lucene.CachedLuceneResults;
import org.vortikal.repositoryimpl.index.util.IndexResourceIdHelper;

/**
 * Contains a few queries for listing URI contents from an index
 * efficiently using parent resource IDs.
 *
 * @author oyviste
 */
public class URIQueryHelper implements InitializingBean {
    
    private IndexResourceIdHelper indexResourceIdHelper;
    
    private Sort uriSorter = new Sort(new SortField(IndexConstants.URI_FIELD, 
                                         SortField.STRING, false));
    
    /** Creates a new instance of URIQueryHelper */
    public URIQueryHelper() {
    }

    public void afterPropertiesSet() {
        if (this.indexResourceIdHelper == null) {
            throw new BeanInitializationException("Property 'indexResourceIdHelper' not set.");
        }
    }
    
    /**
     * Perform an URI query using the given index searcher, with the given result class.
     * Returns all resources under URI ("recursively").
     *
     * Mostly useful with indexes that store collections as index documents, in addition to
     * resources, but should be usable with any index that has the URI and PARENTIDS fields
     * in its documents. 
     * NOTE: It really only makes sense to query on collection URI's. Querying on a plain resource
     * will only yield the resource itself as a result, as no other index documents will have its
     * id as a parent.
     *
     * It should have the same effect as a <code>PrefixQuery</code> on the string 'uri+/', 
     * only it will be faster.
     *
     * PrefixQueries are also, per default, limited to 1024 hits, upon which a 
     * BooleanQuery.TooManyClauses exception will be thrown (this is tunable, but requires lots
     * of memory with many hits). This is because a prefix query is actually expanded to lots of 
     * OR'ed term queries, which then are processed together (much like how a shell expands filenames
     * when using the asterisk).
     *
     * @param uri The URI to find the contents of.
     * @param searcher <code>IndexSearcher</code> to use for the search.
     * @param resultClass Class of result objects.
     * @return A <code>ModifiableResults</code> instance containing the search result.
     */
    public ModifiableResults queryURIContents(String uri, IndexSearcher searcher, Class resultClass) 
        throws IOException {
        
        CachedLuceneResults results = new CachedLuceneResults(resultClass);
        
        String resourceId = this.indexResourceIdHelper.getResourceId(uri);
        if (resourceId == null) {
            return results; // Resource (id) not found in repository.
        }
        
        TermQuery contentsTermq = new TermQuery(new Term(IndexConstants.PARENT_IDS_FIELD, resourceId));
        Hits hits = searcher.search(contentsTermq, this.uriSorter);
        results.appendHits(hits);
        
        return results;
    }
    
    public ModifiableResults queryURI(String uri, IndexSearcher searcher, Class resultClass) 
        throws IOException {
        TermQuery termQuery = new TermQuery(new Term(IndexConstants.URI_FIELD, uri));
        Hits hits = searcher.search(termQuery);
        return new CachedLuceneResults(hits, resultClass);
    }
    
    /**
     * List child resources of a URI from an index.
     *
     * TODO: This method is probably not terribly efficient, as it has to strip away all folder
     *       levers deeper than the given URI's children from the search result. Fix this.
     *
     */
    public ModifiableResults queryURIChildren(String uri, IndexSearcher searcher, Class resultClass) 
        throws IOException {
        ModifiableResults results = queryURIContents(uri, searcher, resultClass);
        
        // Strip away any subfolders of the URI's children.
        stripFolderLevels(results, uri, 1);
        return results;
    }
    
    public void setIndexResourceIdHelper(IndexResourceIdHelper indexResourceIdHelper) {
        this.indexResourceIdHelper = indexResourceIdHelper;
    }
    
    private void stripFolderLevels(ModifiableResults results, String baseURI, int keepLevels) {
        for (int i=0; i< results.getSize(); i++) {
            String uri = results.getResultMetadata(i).getUri();

            if (uri.length() <= baseURI.length()) continue;
            String baseStripped = uri.substring(baseURI.length()+1, uri.length());
            
            int level = 0;
            int index = -1;
            do {
                index = baseStripped.indexOf('/', index+1);
                if (++level > keepLevels) {
                    results.removeResult(i--);
                    break;
                }
            } while (index != -1);
        }
    }
}
