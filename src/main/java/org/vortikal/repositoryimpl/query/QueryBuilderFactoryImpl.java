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
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.query.AbstractMultipleQuery;
import org.vortikal.repository.query.AbstractPropertyQuery;
import org.vortikal.repository.query.NamePrefixQuery;
import org.vortikal.repository.query.NameRangeQuery;
import org.vortikal.repository.query.NameTermQuery;
import org.vortikal.repository.query.NameWildcardQuery;
import org.vortikal.repository.query.PropertyExistsQuery;
import org.vortikal.repository.query.PropertyPrefixQuery;
import org.vortikal.repository.query.PropertyRangeQuery;
import org.vortikal.repository.query.PropertyTermQuery;
import org.vortikal.repository.query.PropertyWildcardQuery;
import org.vortikal.repository.query.Query;
import org.vortikal.repository.query.TypeTermQuery;
import org.vortikal.repository.query.UriDepthQuery;
import org.vortikal.repository.query.UriPrefixQuery;
import org.vortikal.repository.query.UriTermQuery;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.query.builders.NamePrefixQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.NameRangeQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.NameTermQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.NameWildcardQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.PropertyExistsQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.PropertyPrefixQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.PropertyRangeQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.PropertyTermQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.PropertyWildcardQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.QueryTreeBuilder;
import org.vortikal.repositoryimpl.query.builders.TypeTermQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.UriDepthQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.UriPrefixQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.UriTermQueryBuilder;
import org.vortikal.util.repository.URIUtil;

/**
 * Factory that helps in building different Lucene queries 
 * from our own query types.
 * 
 * @author oyviste
 */
public final class QueryBuilderFactoryImpl implements QueryBuilderFactory, 
                                                      InitializingBean {

    Log logger = LogFactory.getLog(QueryBuilderFactoryImpl.class);
    
    private PropertyManager propertyManager;
    private LuceneIndexManager indexAccessor;
    private ResourceTypeTree resourceTypeTree;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.propertyManager == null) {
            throw new BeanInitializationException("Property 'propertyManager' not set.");
        }
        this.resourceTypeTree = this.propertyManager.getResourceTypeTree();
    }
    
    public QueryBuilder getBuilder(Query query) throws QueryBuilderException {
        
        QueryBuilder builder = null;

        if (query instanceof AbstractMultipleQuery) {
            builder = new QueryTreeBuilder(this, (AbstractMultipleQuery)query);
        }

        else if (query instanceof AbstractPropertyQuery) {
            builder = getAbstractPropertyQueryBuilder(query);
        }
       
        else if (query instanceof UriTermQuery) {
            builder = new UriTermQueryBuilder((UriTermQuery)query);
        }
       
        else if (query instanceof UriPrefixQuery) {
            String uri = ((UriPrefixQuery)query).getUri();
            Term idTerm = getPropertySetIdTermFromIndex(uri);
            builder =  new UriPrefixQueryBuilder(uri, idTerm);
        }
        
        else if (query instanceof UriDepthQuery) {
            builder = new UriDepthQueryBuilder((UriDepthQuery)query);
        }

        else if (query instanceof NameTermQuery) {
            builder = new NameTermQueryBuilder((NameTermQuery)query);
        }

        else if (query instanceof NameRangeQuery) {
            builder = new NameRangeQueryBuilder((NameRangeQuery)query);
        }
       
        else if (query instanceof NamePrefixQuery) {
            builder = new NamePrefixQueryBuilder((NamePrefixQuery)query);
        }

        else if (query instanceof NameWildcardQuery) {
            builder = new NameWildcardQueryBuilder((NameWildcardQuery)query);
        }
       
        else if (query instanceof TypeTermQuery) {
            builder = new TypeTermQueryBuilder(this.resourceTypeTree, 
                                               (TypeTermQuery)query);
        }
       
        if (builder == null) {
            throw new QueryBuilderException("Unsupported query type: " 
                                            + query.getClass().getName());
        }
        
        return builder;
    }
    
    private QueryBuilder getAbstractPropertyQueryBuilder(Query query)
        throws QueryBuilderException {

        QueryBuilder builder = null;
        
        if (query instanceof PropertyTermQuery) {
            builder = new PropertyTermQueryBuilder((PropertyTermQuery)query);
        }
        
        if (query instanceof PropertyPrefixQuery) {
            builder = new PropertyPrefixQueryBuilder((PropertyPrefixQuery)query);
        }
        
        if (query instanceof PropertyRangeQuery) {
            builder = new PropertyRangeQueryBuilder((PropertyRangeQuery)query);
        }
        
        if (query instanceof PropertyWildcardQuery) {
            builder = new PropertyWildcardQueryBuilder((PropertyWildcardQuery)query);
        }
        
        if (query instanceof PropertyExistsQuery) {
            builder = new PropertyExistsQueryBuilder((PropertyExistsQuery)query);
        }
        
        if (builder == null) {
            throw new QueryBuilderException("Unsupported property query type: " 
                                        + query.getClass().getName());
        }
        return builder;
    }
    
    private Term getPropertySetIdTermFromIndex(String uri) 
        throws QueryBuilderException {
        
        TermDocs td = null;
        IndexReader reader = null;
        try {
            reader = this.indexAccessor.getReadOnlyIndexReader();

            td = reader.termDocs(new Term(DocumentMapper.URI_FIELD_NAME, 
                                                URIUtil.stripTrailingSlash(uri)));
            
            if (td.next()) {
                Field field= reader.document(td.doc()).getField(
                                            DocumentMapper.STORED_ID_FIELD_NAME);
                
                String value = 
                    Integer.toString(
                            BinaryFieldValueMapper.getIntegerFromStoredBinaryField(field));
                
                return new Term(DocumentMapper.ID_FIELD_NAME, value);
                
            }
            // URI not found, so the query should produce zero hits.
            return new Term(DocumentMapper.ID_FIELD_NAME, String.valueOf(
                    PropertySetImpl.NULL_RESOURCE_ID));
        } catch (IOException io) {
            throw new QueryBuilderException("IOException while building query: " + io.getMessage());
        } finally {
            try {
                if (td != null) td.close();
                this.indexAccessor.releaseReadOnlyIndexReader(reader);
            } catch (IOException io) {}
        }
    }

    
    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }
    
    public void setIndexAccessor(LuceneIndexManager indexAccessor) {
        this.indexAccessor = indexAccessor;
    }

}
