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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repositoryimpl.PropertyManagerImpl;
import org.vortikal.repositoryimpl.query.builders.NameRangeQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.NameTermQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.PropertyQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.QueryTreeBuilder;
import org.vortikal.repositoryimpl.query.builders.TypeTermQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.UriPrefixQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.UriTermQueryBuilder;
import org.vortikal.repositoryimpl.query.query.AbstractMultipleQuery;
import org.vortikal.repositoryimpl.query.query.AbstractPropertyQuery;
import org.vortikal.repositoryimpl.query.query.NameRangeQuery;
import org.vortikal.repositoryimpl.query.query.NameTermQuery;
import org.vortikal.repositoryimpl.query.query.Query;
import org.vortikal.repositoryimpl.query.query.TypeTermQuery;
import org.vortikal.repositoryimpl.query.query.UriPrefixQuery;
import org.vortikal.repositoryimpl.query.query.UriTermQuery;

/**
 * Factory that helps in building different Lucene queries 
 * from our own query types.
 * 
 * @author oyviste
 */
public final class QueryBuilderFactoryImpl implements QueryBuilderFactory, 
                                                      InitializingBean {

    Log logger = LogFactory.getLog(QueryBuilderFactoryImpl.class);
    
    private PropertyManagerImpl propertyManager;
    private LuceneIndex indexAccessor;

    /* Map resource type name to flat list of _all_ descendant resource type names.
     * (Supports fast lookup for 'IN'-resource-type queries)
     */
    private Map resourceTypeDescendantNames;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.propertyManager == null) {
            throw new BeanInitializationException("Property 'propertyManager' not set.");
        }
    
        initializeResourceTypeDescendants();
    }
    
    public QueryBuilder getBuilder(Query query) throws QueryBuilderException {
        
       if (query instanceof AbstractMultipleQuery) {
           return new QueryTreeBuilder(this, (AbstractMultipleQuery)query);
       }
        
       if (query instanceof NameTermQuery) {
           return new NameTermQueryBuilder((NameTermQuery)query);
       }

       if (query instanceof NameRangeQuery) {
           return new NameRangeQueryBuilder((NameRangeQuery)query);
       }
       
       if (query instanceof AbstractPropertyQuery) {
           return new PropertyQueryBuilder((AbstractPropertyQuery)query);
       }
       
       if (query instanceof TypeTermQuery) {
           return new TypeTermQueryBuilder(this.resourceTypeDescendantNames, 
                                          (TypeTermQuery)query);
       }
       
       if (query instanceof UriTermQuery) {
           return new UriTermQueryBuilder(((UriTermQuery)query).getUri());
       }
       
       if (query instanceof UriPrefixQuery) {
           Term idTerm = getPropertySetIdTermFromIndex(((UriPrefixQuery)query).getUri());
           return new UriPrefixQueryBuilder(idTerm);
       }
       
       throw new QueryBuilderException("Unsupported query type: " 
                                   + query.getClass().getSimpleName());
    }
    
    private Term getPropertySetIdTermFromIndex(String uri) 
        throws QueryBuilderException {
        
        TermDocs td = null;
        IndexSearcher searcher = null;
        IndexReader reader = null;
        try {
            searcher = indexAccessor.getIndexSearcher();
            reader = searcher.getIndexReader();
            
            td = reader.termDocs(new Term(DocumentMapper.URI_FIELD_NAME, uri));
            
            if (td.next()) {
                String fieldValue = reader.document(td.doc()).get(DocumentMapper.ID_FIELD_NAME);
                return new Term(DocumentMapper.ID_FIELD_NAME, fieldValue);
            } else {
                // URI not found, so the query should produce zero hits.
                return new Term(DocumentMapper.ID_FIELD_NAME, "-1"); 
            }
        } catch (IOException io) {
            throw new QueryBuilderException("IOException while building query: ", io);
        } finally {
            try {
                if (td != null) td.close();
                indexAccessor.releaseIndexSearcher(searcher);
            } catch (IOException io) {}
        }
    }

    /* Initialize map of resource type names to names of all descendants */
    private void initializeResourceTypeDescendants() {
        List definitions = propertyManager.getPrimaryResourceTypeDefinitions();
        
        this.resourceTypeDescendantNames = new HashMap();
        
        for (Iterator i = definitions.iterator(); i.hasNext();) {
            PrimaryResourceTypeDefinition def = (PrimaryResourceTypeDefinition)i.next();
            List descendantNames = new ArrayList();
            getAllDescendantNames(descendantNames, def);
            this.resourceTypeDescendantNames.put(def.getName(), descendantNames);
        }
        
        if (logger.isDebugEnabled()) {
            for (Iterator i=this.resourceTypeDescendantNames.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                String name = (String)entry.getKey();
                List descendantNames = (List)entry.getValue();
                
                StringBuffer buf = new StringBuffer("Descendant resource types of [" + name + "]: [");
                for (Iterator u = descendantNames.iterator();u.hasNext();) {
                    buf.append(u.next());
                    if (u.hasNext()) {
                        buf.append(", ");
                    }
                }
                buf.append("]");
                logger.debug(buf.toString());
            }
        }
    }
    
    /* Recursively get all descendant names for a given resource type */
    private void getAllDescendantNames(List names, PrimaryResourceTypeDefinition def) {
        List children = propertyManager.getResourceTypeDefinitionChildren(def);
        
        for (Iterator i=children.iterator();i.hasNext();) {
            PrimaryResourceTypeDefinition child = 
                (PrimaryResourceTypeDefinition)i.next();
            names.add(child.getName());
            getAllDescendantNames(names, child);
        }
    }

    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }
    
    public void setIndexAccessor(LuceneIndex indexAccessor) {
        this.indexAccessor = indexAccessor;
    }
}
