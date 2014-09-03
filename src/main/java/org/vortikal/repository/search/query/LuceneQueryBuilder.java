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
package org.vortikal.repository.search.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.FilterClause;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.FieldValueFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.HierarchicalVocabulary;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.Vocabulary;
import org.vortikal.repository.index.mapping.DocumentMapper;
import org.vortikal.repository.index.mapping.PropertyFields;
import org.vortikal.repository.index.mapping.ResourceFields;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Sorting;
import static org.vortikal.repository.search.query.TermOperator.EQ;
import static org.vortikal.repository.search.query.TermOperator.NE;
import org.vortikal.repository.search.query.builders.ACLInheritedFromQueryBuilder;
import org.vortikal.repository.search.query.builders.ACLReadForAllQueryBuilder;
import org.vortikal.repository.search.query.builders.AclPrivilegeQueryBuilder;
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
import org.vortikal.repository.search.query.builders.TermsQueryBuilder;
import org.vortikal.repository.search.query.builders.TypeTermQueryBuilder;
import org.vortikal.repository.search.query.builders.UriDepthQueryBuilder;
import org.vortikal.repository.search.query.builders.UriPrefixQueryBuilder;
import org.vortikal.repository.search.query.builders.UriSetQueryBuilder;
import org.vortikal.repository.search.query.builders.UriTermQueryBuilder;
import org.vortikal.repository.search.query.filter.FilterFactory;
import org.vortikal.repository.search.query.security.QueryAuthorizationFilterFactory;


/**
 * Build instances of {@link org.apache.lucene.search.Query} and
 * {@link org.apache.lucene.search.Filter} from our own query types.
 */
public class LuceneQueryBuilder implements InitializingBean {

    private final Log logger = LogFactory.getLog(LuceneQueryBuilder.class);

    private ResourceTypeTree resourceTypeTree;
    private DocumentMapper documentMapper;
    private QueryAuthorizationFilterFactory queryAuthorizationFilterFactory;
    private PropertyTypeDefinition publishedPropDef;
    private PropertyTypeDefinition unpublishedCollectionPropDef;
    private Filter cachedOnlyPublishedFilter;

    @Override
    public void afterPropertiesSet() {
        // Setup cached filter for published resources
        Term publishedTerm = documentMapper.getPropertyFields().queryTerm(PropertyFields.propertyFieldName(publishedPropDef, false), "true", PropertyType.Type.BOOLEAN, false);
        TermsFilter tf = new TermsFilter(publishedTerm);
        this.cachedOnlyPublishedFilter = FilterFactory.cacheWrapper(tf);
    }

    /**
     * Build a Lucene {@link org.apache.lucene.search.Query} 
     * for a given <code>{@link org.vortikal.repository.search.query.Query}</code>.
     * 
     * @param query
     * @param searcher
     * @return
     * @throws QueryBuilderException
     */
    public org.apache.lucene.search.Query buildQuery(Query query, IndexSearcher searcher) throws QueryBuilderException {

        QueryBuilder builder = null;

        if (query instanceof AbstractMultipleQuery) {
            builder = new QueryTreeBuilder(this, searcher, (AbstractMultipleQuery) query);
        }

        else if (query instanceof AbstractPropertyQuery) {
            builder = getAbstractPropertyQueryBuilder(query);
        }

        else if (query instanceof UriTermQuery) {
            builder = new UriTermQueryBuilder((UriTermQuery) query);
        }

        else if (query instanceof UriPrefixQuery) {
            UriPrefixQuery uriPrefixQuery = (UriPrefixQuery) query;
            builder = new UriPrefixQueryBuilder(uriPrefixQuery);
        }

        else if (query instanceof UriDepthQuery) {
            builder = new UriDepthQueryBuilder((UriDepthQuery) query, documentMapper.getPropertyFields());
        }

        else if (query instanceof UriSetQuery) {
            builder = new UriSetQueryBuilder((UriSetQuery) query);
        }

        else if (query instanceof NameTermQuery) {
            builder = new NameTermQueryBuilder((NameTermQuery) query);
        }

        else if (query instanceof NameRangeQuery) {
            builder = new NameRangeQueryBuilder((NameRangeQuery) query);
        }

        else if (query instanceof NamePrefixQuery) {
            builder = new NamePrefixQueryBuilder((NamePrefixQuery) query);
        }

        else if (query instanceof NameWildcardQuery) {
            builder = new NameWildcardQueryBuilder((NameWildcardQuery) query);
        }

        else if (query instanceof TypeTermQuery) {
            TypeTermQuery ttq = (TypeTermQuery) query;

            if (EQ == ttq.getOperator() || NE == ttq.getOperator()) {
                builder = new TypeTermQueryBuilder(ttq.getTerm(), ttq.getOperator());
            } else {
                List<String> values = new ArrayList<String>();
                values.add(ttq.getTerm());
                values.addAll(this.resourceTypeTree.getDescendants(ttq.getTerm()));
                builder = new TermsQueryBuilder(ResourceFields.RESOURCETYPE_FIELD_NAME, values,  
                        PropertyType.Type.STRING, ttq.getOperator(), documentMapper.getPropertyFields());
                
//                builder = new HierarchicalTermQueryBuilder<String>(this.resourceTypeTree, ttq.getOperator(),
//                        FieldNames.RESOURCETYPE_FIELD_NAME, ttq.getTerm());
            }
        }

        else if (query instanceof AbstractAclQuery) {
            builder = getACLQueryBuilder(query, searcher);
        }

        else if (query instanceof MatchAllQuery) {
            return new MatchAllDocsQuery();
        }

        if (builder == null) {
            throw new QueryBuilderException("Unsupported query type: " + query.getClass().getName());
        }

        return builder.buildQuery();
    }

    private QueryBuilder getACLQueryBuilder(Query query, IndexSearcher searcher) {
        if (query instanceof AclExistsQuery) {
            AclExistsQuery aclExistsQuery = (AclExistsQuery) query;

            return new ACLInheritedFromQueryBuilder(PropertySetImpl.NULL_RESOURCE_ID, aclExistsQuery.isInverted());
        }
        
        if (query instanceof AclPrivilegeQuery) {
            return new AclPrivilegeQueryBuilder((AclPrivilegeQuery)query);
        }

        if (query instanceof AclInheritedFromQuery) {
            AclInheritedFromQuery aclIHFQuery = (AclInheritedFromQuery) query;

            return new ACLInheritedFromQueryBuilder(getResourceIdFromIndex(aclIHFQuery.getUri(), searcher),
                    aclIHFQuery.isInverted());
        }

        if (query instanceof AclReadForAllQuery) {
            return new ACLReadForAllQueryBuilder(((AclReadForAllQuery) query).isInverted(),
                    this.queryAuthorizationFilterFactory, searcher);
        }

        return null;
    }

    private QueryBuilder getAbstractPropertyQueryBuilder(Query query) throws QueryBuilderException {

        AbstractPropertyQuery apq = (AbstractPropertyQuery) query;
        String cva = apq.getComplexValueAttributeSpecifier();
        PropertyType.Type type = apq.getPropertyDefinition().getType();
        if (!(cva == null ^ type == PropertyType.Type.JSON)) {
            throw new QueryBuilderException(
                    "Attribute specifier (..@attr) is required for JSON-property queries and forbidden for other types.");
        }

        if (query instanceof PropertyTermQuery) {
            PropertyTermQuery ptq = (PropertyTermQuery) query;
            PropertyTypeDefinition propDef = ptq.getPropertyDefinition();
            TermOperator op = ptq.getOperator();

            if (op == TermOperator.IN || op == TermOperator.NI) {
                if (type != PropertyType.Type.STRING) {
                    throw new QueryBuilderException("Operators IN or NI only supported for properties of type STRING");
                }
                
                // XXX not entirely sure this code path is in use.
                // (Only in theory do we have propdefs with a hierarchical value vocab)

                Vocabulary<Value> vocabulary = propDef.getVocabulary();
                if (!(vocabulary instanceof HierarchicalVocabulary<?>)) {
                    throw new QueryBuilderException("Property type doesn't have a hierachical vocabulary: " + propDef);
                }
                HierarchicalVocabulary<Value> hv = (HierarchicalVocabulary<Value>) vocabulary;
                String fieldName = PropertyFields.propertyFieldName(propDef, false);
                List<String> values = new ArrayList<String>();
                values.add(ptq.getTerm());
                for (Value v: hv.getDescendants(new Value(ptq.getTerm(), PropertyType.Type.STRING))) {
                    values.add(v.getStringValue());
                }

                return new TermsQueryBuilder(fieldName, values, PropertyType.Type.STRING, 
                        ptq.getOperator(), documentMapper.getPropertyFields());
            } else if (op == TermOperator.GE || op == TermOperator.GT) {
                // Convert to PropertyRangeQuery
                PropertyRangeQuery prq = new PropertyRangeQuery(ptq.getPropertyDefinition(), 
                        ptq.getTerm(), null, op == TermOperator.GE);
                prq.setComplexValueAttributeSpecifier(ptq.getComplexValueAttributeSpecifier());
                
                return new PropertyRangeQueryBuilder(prq, documentMapper.getPropertyFields());
            } else if (op == TermOperator.LE || op == TermOperator.LT) {
                // Convert to PropertyRangeQuery
                PropertyRangeQuery prq = new PropertyRangeQuery(ptq.getPropertyDefinition(), 
                        null, ptq.getTerm(), op == TermOperator.LE);
                prq.setComplexValueAttributeSpecifier(ptq.getComplexValueAttributeSpecifier());
                return new PropertyRangeQueryBuilder(prq, documentMapper.getPropertyFields());
                
            } else {
                return new PropertyTermQueryBuilder(ptq, documentMapper.getPropertyFields());
            }

//            TermOperator op = ptq.getOperator();
//            boolean lowercase = (op == TermOperator.EQ_IGNORECASE || op == TermOperator.NE_IGNORECASE);
//            if (cva != null) {
//                Type dataType = Field4ValueMapper.getJsonFieldDataType(propDef, cva);
//                String fieldName = FieldNames.getJsonSearchFieldName(propDef, cva, lowercase);
//                String fieldValue = fieldValueMapper.queryTerm(fieldName, ptq.getTerm(), dataType, lowercase);
//                return new PropertyTermQueryBuilder(op, fieldName, fieldValue);
//            } else {
//                String fieldName = FieldNames.getSearchFieldName(propDef, lowercase);
//                String fieldValue = fieldValueMapper.encodeIndexFieldValue(ptq.getTerm(), propDef.getType(),
//                        lowercase);
//                return new PropertyTermQueryBuilder(op, fieldName, fieldValue);
//            }
        }

        if (query instanceof PropertyPrefixQuery) {
            return new PropertyPrefixQueryBuilder((PropertyPrefixQuery) query);
        }

        if (query instanceof PropertyRangeQuery) {
            return new PropertyRangeQueryBuilder((PropertyRangeQuery) query, documentMapper.getPropertyFields());
        }

        if (query instanceof PropertyWildcardQuery) {
            return new PropertyWildcardQueryBuilder((PropertyWildcardQuery) query);
        }

        if (query instanceof PropertyExistsQuery) {
            PropertyExistsQuery peq = (PropertyExistsQuery) query;
            return new PropertyExistsQueryBuilder(peq);
        }

        throw new QueryBuilderException("Unsupported property query type: " + query.getClass().getName());
    }

    private int getResourceIdFromIndex(Path uri, IndexSearcher searcher) throws QueryBuilderException {
        TermQuery tq = new TermQuery(new Term(ResourceFields.URI_FIELD_NAME, uri.toString()));
        try {
            TopDocs docs = searcher.search(tq, 1);
            if (docs.scoreDocs.length == 1) {
                Set<String> fields = new HashSet<String>(2);
                fields.add(ResourceFields.ID_FIELD_NAME);
                Document doc = searcher.doc(docs.scoreDocs[0].doc, fields);
                String id = doc.get(ResourceFields.ID_FIELD_NAME);
                if (id != null) {
                    return Integer.parseInt(id);
                }
            }
            
            return PropertySetImpl.NULL_RESOURCE_ID;
            
        } catch (IOException io) {
            throw new QueryBuilderException("IOException while building query: " + io.getMessage());
        }
    }

    /**
     * Build a {@link org.apache.lucene.search.Filter} that should be applied
     * for the given search and query.
     *
     * May return <code>null</code> if no filter should be applied for the given
     * search.
     *
     * @param token security token used for search
     * @param search search object
     * @param searcher index searcher
     * @return a {@link Filter} instance for the search
     */
    public Filter buildSearchFilter(String token, Search search, IndexSearcher searcher) throws QueryBuilderException {

        // Set filter to ACL filter initially. May be null.
        Filter filter = this.queryAuthorizationFilterFactory.authorizationQueryFilter(token, searcher);
        if (logger.isDebugEnabled()) {
            if (filter == null) {
                logger.debug("ACL filter null for token: " + token);
            } else {
                logger.debug("ACL filter: " + filter + " for token " + token);
            }
        }
        
        // Add filters for removing default excludes if requested
        if (search.hasFilterFlag(Search.FilterFlag.UNPUBLISHED)) {
            BooleanFilter bf = buildUnpublishedFilter();

            // Include ACL-filter if non-null:
            if (filter != null) {
                bf.add(filter, BooleanClause.Occur.MUST);
            }

            filter = bf;
        }

        if (search.hasFilterFlag(Search.FilterFlag.UNPUBLISHED_COLLECTIONS)) {
            filter = addUnpublishedCollectionFilter(filter);
        }

        return filter;

    }

    BooleanFilter buildUnpublishedFilter() {
        BooleanFilter bf = new BooleanFilter();

        // Filter to include only published resources:
        bf.add(this.cachedOnlyPublishedFilter, BooleanClause.Occur.MUST);

        return bf;
    }

    BooleanFilter addUnpublishedCollectionFilter(Filter filter) {
        BooleanFilter bf = new BooleanFilter();

        if (filter != null) {
            bf.add(filter, BooleanClause.Occur.MUST);
        }

        // Filter to exclude unpublishedCollection resources:
        // Avoid using cache-wrapper for FieldValueFilter, since that can
        // lead to memory leaks in Lucene.

        bf.add(new FieldValueFilter(PropertyFields.propertyFieldName(this.unpublishedCollectionPropDef, false), true), 
                BooleanClause.Occur.MUST);

        return bf;
    }

    /**
     * Build iteration filter based on search query, token and options.
     * @param token
     * @param search
     * @param searcher
     * @return 
     */
    public Filter buildIterationFilter(String token, Search search, IndexSearcher searcher) {
        Query query = search.getQuery();
        Filter filter = null;
        if (query != null && !(query instanceof MatchAllQuery)) {
            filter = new QueryWrapperFilter(buildQuery(query, searcher));
        }

        Filter searchFilter = buildSearchFilter(token, search, searcher);
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

    /**
     * Build a {@link org.apache.lucene.search.Sort} from given 
     * {@link org.vortikal.repository.search.Sorting}.
     * 
     * @param sort
     * @return
     */
    public org.apache.lucene.search.Sort buildSort(Sorting sort) {
        if (sort == null)
            return null;

        return new SortBuilder().buildSort(sort);
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setQueryAuthorizationFilterFactory(QueryAuthorizationFilterFactory queryAuthorizationFilterFactory) {
        this.queryAuthorizationFilterFactory = queryAuthorizationFilterFactory;
    }

    @Required
    public void setPublishedPropDef(PropertyTypeDefinition publishedPropDef) {
        this.publishedPropDef = publishedPropDef;
    }

    @Required
    public void setUnpublishedCollectionPropDef(PropertyTypeDefinition unpublishedCollectionPropDef) {
        this.unpublishedCollectionPropDef = unpublishedCollectionPropDef;
    }
    
    @Required
    public void setDocumentMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

}
