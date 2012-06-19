/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repository.search.query;

import org.apache.lucene.document.Fieldable;
import static org.vortikal.repository.search.query.TermOperator.EQ;
import static org.vortikal.repository.search.query.TermOperator.NE;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanFilter;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.FieldValueFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterClause;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermsFilter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.HierarchicalVocabulary;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.Vocabulary;
import org.vortikal.repository.index.mapping.FieldNames;
import org.vortikal.repository.index.mapping.FieldValueMapper;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Sorting;
import org.vortikal.repository.search.query.builders.ACLInheritedFromQueryBuilder;
import org.vortikal.repository.search.query.builders.ACLReadForAllQueryBuilder;
import org.vortikal.repository.search.query.builders.HierarchicalTermQueryBuilder;
import org.vortikal.repository.search.query.builders.NamePrefixQueryBuilder;
import org.vortikal.repository.search.query.builders.NameRangeQueryBuilder;
import org.vortikal.repository.search.query.builders.NameTermQueryBuilder;
import org.vortikal.repository.search.query.builders.NameWildcardQueryBuilder;
import org.vortikal.repository.search.query.builders.PropertyExistsQueryBuilder;
import org.vortikal.repository.search.query.builders.PropertyPrefixQueryBuilder;
import org.vortikal.repository.search.query.builders.PropertyRangeQueryBuilder;
import org.vortikal.repository.search.query.builders.PropertyTermQueryBuilder;
import org.vortikal.repository.search.query.builders.PropertyWildcardQueryBuilder;
import org.vortikal.repository.search.query.builders.QueryTreeBuilder;
import org.vortikal.repository.search.query.builders.TypeTermQueryBuilder;
import org.vortikal.repository.search.query.builders.UriDepthQueryBuilder;
import org.vortikal.repository.search.query.builders.UriPrefixQueryBuilder;
import org.vortikal.repository.search.query.builders.UriSetQueryBuilder;
import org.vortikal.repository.search.query.builders.UriTermQueryBuilder;
import org.vortikal.repository.search.query.filter.DeletedDocsFilter;
import org.vortikal.repository.search.query.security.QueryAuthorizationFilterFactory;

/**
 * Factory that helps in building different Lucene queries 
 * from our own query types.
 * 
 * @author oyviste
 */
public final class LuceneQueryBuilderImpl implements LuceneQueryBuilder, InitializingBean {

    Log logger = LogFactory.getLog(LuceneQueryBuilderImpl.class);
    
    private ResourceTypeTree resourceTypeTree;
    private FieldValueMapper fieldValueMapper;
    private QueryAuthorizationFilterFactory queryAuthorizationFilterFactory;
    private PropertyTypeDefinition publishedPropDef;

    private Filter onlyPublishedFilter;
    private Filter cachedDeletedDocsFilter;

    @Override
    public void afterPropertiesSet() {
        // Setup filter for published resources
        TermsFilter tf = new TermsFilter();
        String searchFieldName = FieldNames.getSearchFieldName(this.publishedPropDef, false);
        String searchFieldValue = this.fieldValueMapper.encodeIndexFieldValue("true", Type.BOOLEAN, false);
        Term publishedTrueTerm = new Term(searchFieldName, searchFieldValue);
        tf.addTerm(publishedTrueTerm);
        this.onlyPublishedFilter = new CachingWrapperFilter(tf);

        // Setup cached deleted docs filter
        this.cachedDeletedDocsFilter = new CachingWrapperFilter(new DeletedDocsFilter(), 
                                           CachingWrapperFilter.DeletesMode.RECACHE);

    }
    
    /* (non-Javadoc)
     * @see org.vortikal.repository.search.query.LuceneQueryBuilder#buildQuery(org.vortikal.repository.search.query.Query, org.apache.lucene.index.IndexReader)
     */
    @Override
    public org.apache.lucene.search.Query buildQuery(Query query, IndexReader reader) throws QueryBuilderException {
        
        QueryBuilder builder = null;

        if (query instanceof AbstractMultipleQuery) {
            builder = new QueryTreeBuilder(this, reader, (AbstractMultipleQuery)query);
        }

        else if (query instanceof AbstractPropertyQuery) {
            builder = getAbstractPropertyQueryBuilder(query);
        }
       
        else if (query instanceof UriTermQuery) {
            builder = new UriTermQueryBuilder((UriTermQuery)query, this.cachedDeletedDocsFilter);
        }

        else if (query instanceof UriPrefixQuery) {
        	UriPrefixQuery uriPrefixQuery = (UriPrefixQuery) query;
            builder = new UriPrefixQueryBuilder(uriPrefixQuery, this.cachedDeletedDocsFilter);
        }
        
        else if (query instanceof UriDepthQuery) {
            builder = new UriDepthQueryBuilder((UriDepthQuery)query);
        }
        
        else if (query instanceof UriSetQuery) {
            builder = new UriSetQueryBuilder((UriSetQuery)query);
        }

        else if (query instanceof NameTermQuery) {
            builder = new NameTermQueryBuilder((NameTermQuery)query, this.cachedDeletedDocsFilter);
        }

        else if (query instanceof NameRangeQuery) {
            builder = new NameRangeQueryBuilder((NameRangeQuery)query);
        }
       
        else if (query instanceof NamePrefixQuery) {
            builder = new NamePrefixQueryBuilder((NamePrefixQuery)query, this.cachedDeletedDocsFilter);
        }

        else if (query instanceof NameWildcardQuery) {
            builder = new NameWildcardQueryBuilder((NameWildcardQuery)query, this.cachedDeletedDocsFilter);
        }
       
        else if (query instanceof TypeTermQuery) {
            TypeTermQuery ttq = (TypeTermQuery)query;
            
            if (EQ == ttq.getOperator() || NE == ttq.getOperator()) {
                builder = new TypeTermQueryBuilder(ttq.getTerm(), ttq.getOperator(), this.cachedDeletedDocsFilter);
            } else {
                builder = new HierarchicalTermQueryBuilder<String>(this.resourceTypeTree, 
                        ttq.getOperator(), FieldNames.RESOURCETYPE_FIELD_NAME, ttq.getTerm(),
                        this.cachedDeletedDocsFilter);
            }
        } 
        
        else if (query instanceof ACLQuery) {
            builder = getACLQueryBuilder(query, reader);
        }
        
        else if (query instanceof MatchAllQuery) {
            return new MatchAllDocsQuery();
        }

        if (builder == null) {
            throw new QueryBuilderException("Unsupported query type: " 
                                            + query.getClass().getName());
        }

        return builder.buildQuery();
    }

    private QueryBuilder getACLQueryBuilder(Query query, IndexReader reader) {
        if (query instanceof ACLExistsQuery) {
            ACLExistsQuery aclExistsQuery = (ACLExistsQuery)query;
            
            return new ACLInheritedFromQueryBuilder(PropertySetImpl.NULL_RESOURCE_ID, 
                                                    aclExistsQuery.isInverted(), this.cachedDeletedDocsFilter);
        }
        
        if (query instanceof ACLInheritedFromQuery) {
            ACLInheritedFromQuery aclIHFQuery = (ACLInheritedFromQuery)query;
            
            return new ACLInheritedFromQueryBuilder(
                          getResourceIdFromIndex(aclIHFQuery.getUri(), reader), 
                              aclIHFQuery.isInverted(), this.cachedDeletedDocsFilter);
        }
        
        if (query instanceof ACLReadForAllQuery) {
            return new ACLReadForAllQueryBuilder(((ACLReadForAllQuery)query).isInverted(),
                                                  this.queryAuthorizationFilterFactory,
                                                  reader,
                                                  this.cachedDeletedDocsFilter);
        }

        return null;
    }
    
    private QueryBuilder getAbstractPropertyQueryBuilder(Query query)
        throws QueryBuilderException {

        AbstractPropertyQuery apq = (AbstractPropertyQuery)query;
        String cva = apq.getComplexValueAttributeSpecifier();
        Type type = apq.getPropertyDefinition().getType();
        if (!(cva == null ^ type == Type.JSON)) {
            throw new QueryBuilderException("Attribute specifier (..@attr) is required for JSON-property queries and forbidden for other types.");
        }

        if (query instanceof PropertyTermQuery) {
            PropertyTermQuery ptq = (PropertyTermQuery) query;
            PropertyTypeDefinition propDef = ptq.getPropertyDefinition();

            if (ptq.getOperator() == TermOperator.IN || ptq.getOperator() == TermOperator.NI) {
                if (type == Type.JSON) {
                    throw new QueryBuilderException("Operators IN or NI not supported for properties of type JSON");
                }

                Vocabulary<Value> vocabulary = propDef.getVocabulary();
                if (vocabulary == null || !(vocabulary instanceof HierarchicalVocabulary<?>)) {
                    throw new QueryBuilderException("Property type doesn't have a hierachical vocabulary: " + propDef);
                }
                HierarchicalVocabulary<Value> hv = (HierarchicalVocabulary<Value>) vocabulary;
                
                String fieldName = FieldNames.getSearchFieldName(propDef, false);
                String fieldValue = this.fieldValueMapper.encodeIndexFieldValue(ptq.getTerm(), propDef.getType(), false);
                return new HierarchicalTermQueryBuilder<Value>(hv,
                                      ptq.getOperator(), fieldName,
                                      new Value(fieldValue, PropertyType.Type.STRING), this.cachedDeletedDocsFilter);
            }
            
            TermOperator op = ptq.getOperator();
            boolean lowercase = (op == TermOperator.EQ_IGNORECASE || op == TermOperator.NE_IGNORECASE);
            if (cva != null) {
                Type dataType = FieldValueMapper.getJsonFieldDataType(propDef, cva);
                String fieldName = FieldNames.getJsonSearchFieldName(propDef, cva, lowercase);
                String fieldValue = this.fieldValueMapper.encodeIndexFieldValue(ptq.getTerm(), dataType, lowercase);
                return new PropertyTermQueryBuilder(op, fieldName, fieldValue, this.cachedDeletedDocsFilter);
            } else {
                String fieldName = FieldNames.getSearchFieldName(propDef, lowercase);
                String fieldValue = this.fieldValueMapper.encodeIndexFieldValue(ptq.getTerm(), propDef.getType(), lowercase);
                return new PropertyTermQueryBuilder(op, fieldName, fieldValue, this.cachedDeletedDocsFilter);
            }
        }
        
        if (query instanceof PropertyPrefixQuery) {
            return new PropertyPrefixQueryBuilder((PropertyPrefixQuery)query, this.cachedDeletedDocsFilter);
        }
        
        if (query instanceof PropertyRangeQuery) {
            return new PropertyRangeQueryBuilder((PropertyRangeQuery)query, this.fieldValueMapper);
        }
        
        if (query instanceof PropertyWildcardQuery) {
            return new PropertyWildcardQueryBuilder((PropertyWildcardQuery)query, this.cachedDeletedDocsFilter);
        }
        
        if (query instanceof PropertyExistsQuery) {

            PropertyExistsQuery peq = (PropertyExistsQuery)query;

            return new PropertyExistsQueryBuilder(peq);
        }
        
        throw new QueryBuilderException("Unsupported property query type: " + query.getClass().getName());
    }
    
    // Lucene FieldSelector for only loading ID field
    private static final FieldSelector ID_FIELD_SELECTOR = new FieldSelector() {
        private static final long serialVersionUID = 3456052507152239972L;

        @Override
        public FieldSelectorResult accept(String fieldName) {
            if (FieldNames.STORED_ID_FIELD_NAME == fieldName) { // Interned string comparison OK
                return FieldSelectorResult.LOAD_AND_BREAK;
            } 
                
            return FieldSelectorResult.NO_LOAD;
        }
    };
    
    private int getResourceIdFromIndex(Path uri, IndexReader reader) 
        throws QueryBuilderException {
        
        TermDocs td = null;
        try {
            td = reader.termDocs(new Term(FieldNames.URI_FIELD_NAME, 
                                                uri.toString()));
            
            if (td.next()) {
                Fieldable field= reader.document(td.doc(), ID_FIELD_SELECTOR).getFieldable(
                                            FieldNames.STORED_ID_FIELD_NAME);
                
                return this.fieldValueMapper.getIntegerFromStoredBinaryField(field);
            }
            
            return PropertySetImpl.NULL_RESOURCE_ID; // URI not found in index
        } catch (IOException io) {
            throw new QueryBuilderException("IOException while building query: " + io.getMessage());
        } finally {
            try {
                if (td != null) td.close();
            } catch (IOException io) {}
        }
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.repository.search.query.LuceneQueryBuilder#buildSearchFilter(org.vortikal.repository.search.Search, org.apache.lucene.index.IndexReader)
     */
    @Override
    public Filter buildSearchFilter(String token, Search search, IndexReader reader)
            throws QueryBuilderException {
        
        Filter filter = null;
        
        // Get ACL filter
        Filter aclFilter = 
            this.queryAuthorizationFilterFactory.authorizationQueryFilter(token, reader);
        if (logger.isDebugEnabled()){
            if (aclFilter == null) {
                logger.debug("ACL filter null for token: " + token);
            } else {
                logger.debug("ACL filter: " +  aclFilter + " for token " + token);
            }
        }
        
        // Set filter to ACL filter initially
        filter = aclFilter;
        
        // Add published-filter if requested
        if (search.isOnlyPublishedResources()) {
            if (filter != null) {
                BooleanFilter bf = new BooleanFilter();
                bf.add(new FilterClause(filter, BooleanClause.Occur.MUST));
                bf.add(new FilterClause(this.onlyPublishedFilter, BooleanClause.Occur.MUST));
                filter = bf;
            } else {
                filter = this.onlyPublishedFilter;
            }
        }

        return filter;
    }
    
    @Override
    public Filter buildIterationFilter(String token, Search search, IndexReader reader) {
        Query query = search.getQuery();
        Filter filter = null;
        if (query != null && !(query instanceof MatchAllQuery)) {
            filter = new QueryWrapperFilter(buildQuery(query, reader));
        }

        Filter searchFilter = buildSearchFilter(token, search, reader);
        if (searchFilter != null) {
            if (filter == null) {
                filter = searchFilter;
            } else {
                BooleanFilter bf = new BooleanFilter();
                bf.add(new FilterClause(filter, BooleanClause.Occur.MUST));
                bf.add(new FilterClause(searchFilter, BooleanClause.Occur.MUST));
                filter = bf;
            }
        }
        
        return filter;
    }
    
    
    /* (non-Javadoc)
     * @see org.vortikal.repository.search.query.LuceneQueryBuilder#buildSort(org.vortikal.repository.search.Sorting)
     */
    @Override
    public org.apache.lucene.search.Sort buildSort(Sorting sort) {
        if (sort == null) return null;

        return new SortBuilderImpl().buildSort(sort);
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setFieldValueMapper(FieldValueMapper fieldValueMapper) {
        this.fieldValueMapper = fieldValueMapper;
    }

    @Required
    public void setQueryAuthorizationFilterFactory(
            QueryAuthorizationFilterFactory queryAuthorizationFilterFactory) {
        this.queryAuthorizationFilterFactory = queryAuthorizationFilterFactory;
    }

    @Required
    public void setPublishedPropDef(PropertyTypeDefinition publishedPropDef) {
        this.publishedPropDef = publishedPropDef;
    }

}
