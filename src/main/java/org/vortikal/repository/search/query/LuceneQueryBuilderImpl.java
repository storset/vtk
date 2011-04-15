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


import org.vortikal.repository.search.query.filter.DeletedDocsFilter;
import org.vortikal.repository.search.query.filter.InversionFilter;
import org.vortikal.repository.search.query.filter.TermExistsFilter;
import static org.vortikal.repository.search.query.TermOperator.EQ;
import static org.vortikal.repository.search.query.TermOperator.NE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterClause;
import org.apache.lucene.search.TermsFilter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.HierarchicalVocabulary;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.Vocabulary;
import org.vortikal.repository.index.mapping.FieldNameMapping;
import org.vortikal.repository.index.mapping.FieldValueMapper;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Sorting;
import org.vortikal.repository.search.query.builders.ACLInheritedFromQueryBuilder;
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
    private Filter onlyPublishedFilter;
    private PropertyTypeDefinition publishedPropDef;
    private PropertyTypeDefinition hiddenPropDef;
    private Filter cachedHiddenPropDefNotExistsFilter;
    private Filter cachedDeletedDocsFilter;

    @Override
    public void afterPropertiesSet() {
        // Setup filter for published resources
        TermsFilter tf = new TermsFilter();
        String searchFieldName = FieldNameMapping.getSearchFieldName(this.publishedPropDef, false);
        String searchFieldValue = this.fieldValueMapper.encodeIndexFieldValue("true", Type.BOOLEAN, false);
        Term publishedTrueTerm = new Term(searchFieldName, searchFieldValue);
        tf.addTerm(publishedTrueTerm);
        this.onlyPublishedFilter = new CachingWrapperFilter(tf);

        // Setup cached deleted docs filter
        this.cachedDeletedDocsFilter = new CachingWrapperFilter(new DeletedDocsFilter());

        if (this.hiddenPropDef != null) {
            // Special case caching for common "navigation:hidden !exists" query clause
            TermExistsFilter te = new TermExistsFilter(
                    FieldNameMapping.getSearchFieldName(this.hiddenPropDef, false));
            this.cachedHiddenPropDefNotExistsFilter =
                    new CachingWrapperFilter(new InversionFilter(te, this.cachedDeletedDocsFilter));
        }
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
            String uri = uriPrefixQuery.getUri();
            TermOperator operator = uriPrefixQuery.getOperator();
            List<Term> idTerms = new ArrayList<Term>();
            
            // XXX: Syntax parser does not really support IN operator for URI prefixes (/foo/*,/*), 
            //      query 'uri IN /*,/src/*' yields error: 'Encountered " <WILDVALUE> "/*,/src/ ""'
            if (TermOperator.IN.equals(operator)) {
            	String[] uris = uri.split(",");
            	for (String inUri : uris) {
            		idTerms.add(getPropertySetIdTermFromIndex(inUri, reader));
            	}
            } else {
                idTerms.add(getPropertySetIdTermFromIndex(uri, reader));
            }
            builder =  new UriPrefixQueryBuilder(uri, operator, idTerms, uriPrefixQuery.isInverted(), this.cachedDeletedDocsFilter);
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
                        ttq.getOperator(), FieldNameMapping.RESOURCETYPE_FIELD_NAME, ttq.getTerm(),
                        this.cachedDeletedDocsFilter);
            }
        } 
        
        else if (query instanceof ACLQuery) {
            builder = getACLQueryBuilder(query, reader);
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
                
                String fieldName = FieldNameMapping.getSearchFieldName(propDef, false);
                String fieldValue = this.fieldValueMapper.encodeIndexFieldValue(ptq.getTerm(), propDef.getType(), false);
                return new HierarchicalTermQueryBuilder<Value>(hv,
                                      ptq.getOperator(), fieldName,
                                      new Value(fieldValue, PropertyType.Type.STRING), this.cachedDeletedDocsFilter);
            }
            
            TermOperator op = ptq.getOperator();
            boolean lowercase = (op == TermOperator.EQ_IGNORECASE || op == TermOperator.NE_IGNORECASE);
            if (cva != null) {
                String fieldName = FieldNameMapping.getJSONSearchFieldName(propDef, cva, lowercase);
                String fieldValue = lowercase ? ptq.getTerm().toLowerCase() : ptq.getTerm();
                return new PropertyTermQueryBuilder(op, fieldName, fieldValue, this.cachedDeletedDocsFilter);
            } else {
                String fieldName = FieldNameMapping.getSearchFieldName(propDef, lowercase);
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
            if (peq.getPropertyDefinition() == this.hiddenPropDef  // XXX "Resource type def config pointer init racing" ..
                   && peq.isInverted()
                   && peq.getComplexValueAttributeSpecifier() == null) {

                // Special case optimization by caching of filter for common "navigation:hidden !exists" query.
                return new QueryBuilder() {
                    @Override
                    public org.apache.lucene.search.Query buildQuery() throws QueryBuilderException {
                        return new ConstantScoreQuery(LuceneQueryBuilderImpl.this.cachedHiddenPropDefNotExistsFilter);
                    }
                };
            }

            return new PropertyExistsQueryBuilder(peq, this.cachedDeletedDocsFilter);
        }
        
            throw new QueryBuilderException("Unsupported property query type: " 
                                        + query.getClass().getName());
    }
    
    // Lucene FieldSelector for only loading ID field
    private static final FieldSelector ID_FIELD_SELECTOR = new FieldSelector() {
        private static final long serialVersionUID = 3456052507152239972L;

        @Override
        public FieldSelectorResult accept(String fieldName) {
            if (FieldNameMapping.STORED_ID_FIELD_NAME == fieldName) { // Interned string comparison
                return FieldSelectorResult.LOAD;
            } 
                
            return FieldSelectorResult.NO_LOAD;
        }
    };
    
    private Term getPropertySetIdTermFromIndex(String uri, IndexReader reader) 
        throws QueryBuilderException {
        if (!"/".equals(uri) && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        Path p = Path.fromString(uri);
        return new Term(FieldNameMapping.ID_FIELD_NAME, 
                            String.valueOf(getResourceIdFromIndex(p, reader)));

    }
    
    private int getResourceIdFromIndex(Path uri, IndexReader reader) 
        throws QueryBuilderException {
        
        TermDocs td = null;
        try {
            td = reader.termDocs(new Term(FieldNameMapping.URI_FIELD_NAME, 
                                                uri.toString()));
            
            if (td.next()) {
                Field field= reader.document(td.doc(), ID_FIELD_SELECTOR).getField(
                                            FieldNameMapping.STORED_ID_FIELD_NAME);
                
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

    public void setHiddenPropDef(PropertyTypeDefinition hiddenPropDef) {
        this.hiddenPropDef = hiddenPropDef;
    }

}
