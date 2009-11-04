/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.web.display.collection.aggregation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.query.AbstractMultipleQuery;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.SecurityContext;

public class CollectionListingAggregationResolver implements AggregationReslover {

    private static Log logger = LogFactory.getLog(CollectionListingAggregationResolver.class);

    private Repository repository;
    private PropertyTypeDefinition aggregationPropDef;
    private PropertyTypeDefinition recursiveAggregationPropDef;

    private final static int DEFAULT_LIMIT = 5;
    private final static int DEFAULT_RECURSIVE_DEPTH = 2;

    /**
     * Limit the number of folders to aggregate from
     */
    private int limit = DEFAULT_LIMIT;

    /**
     * Limit depth of the aggregation, in cases where a folder to aggregate from
     * has it's own defined aggregation
     */
    private int maxRecursiveDepth = DEFAULT_RECURSIVE_DEPTH;

    public Query extend(Query query, Resource collection) throws IllegalArgumentException {

        // only extend simple UriPrefixQuery or instances of
        // AbstractMultipleQuery (AndQuery or OrQuery) containing UriPrefixQuery
        if (!(query instanceof AbstractMultipleQuery || query instanceof UriPrefixQuery)) {
            IllegalArgumentException up = new IllegalArgumentException("Unsupported query type: " + query);
            throw up;
        }

        Property aggregationProp = collection.getProperty(this.aggregationPropDef);
        if (aggregationProp != null) {

            // get an OrQuery containing all valid paths to aggregate from
            OrQuery aggregatedFoldersQuery = getAggregateFoldersQuery(collection, aggregationProp);

            if (aggregatedFoldersQuery.getQueries().size() > 0) {
                // a simple UriPrefixQuery -> just extend and return
                if (isExtendableUriPrefixQuery(query)) {
                    aggregatedFoldersQuery.add(query);
                    return aggregatedFoldersQuery;
                } else {
                    // iterate the original query and build a new one, extending
                    // any
                    // UriPrefixQuery you encounter
                    AbstractMultipleQuery extendableQuery = getExtendableQuery(query);
                    for (Query q : ((AbstractMultipleQuery) query).getQueries()) {
                        q = extend(q, aggregatedFoldersQuery, null);
                        extendableQuery.add(q);
                    }
                    return extendableQuery;
                }
            }

        }
        return query;
    }

    private OrQuery getAggregateFoldersQuery(Resource collection, Property aggregationProp) {

        // the actual paths to aggregate from
        // used to create an OrQuery to extend the original query with
        List<Path> paths = new ArrayList<Path>();

        // list of collections that have already been resolved for aggregation,
        // used to avoid loop in aggregation
        List<Path> resolvedCollectionPaths = new ArrayList<Path>();
        resolvedCollectionPaths.add(collection.getURI());
        resolveAggregatedFolderPaths(aggregationProp, paths, resolvedCollectionPaths);

        String token = SecurityContext.getSecurityContext().getToken();
        handleRecursiveAggregation(collection, token, paths, resolvedCollectionPaths, 0);

        OrQuery aggregatedFoldersQuery = new OrQuery();
        for (Path path : paths) {
            aggregatedFoldersQuery.add(new UriPrefixQuery(path.toString(), TermOperator.EQ, false));
        }
        return aggregatedFoldersQuery;
    }

    private void resolveAggregatedFolderPaths(Property aggregationProp, List<Path> paths,
            List<Path> resolvedCollectionPaths) {
        Value[] values = aggregationProp.getValues();
        int aggregationLimit = values.length > limit ? limit : values.length;
        for (int i = 0; i < aggregationLimit; i++) {
            String pathValue = values[i].getStringValue();
            if (isValidPath(pathValue, paths, resolvedCollectionPaths)) {
                paths.add(Path.fromString(pathValue));
            }
        }
    }

    /**
     * validate a value from an aggregationProp (pathValue); a pathValue is
     * considered valid to aggregate from if: (1) it's a valid Path object,(2)
     * not already among the set of paths to aggregate from, (3) not a path to
     * any actual collection along the aggregation path
     */
    private boolean isValidPath(String pathValue, List<Path> paths, List<Path> resolvedCollectionPaths) {
        try {
            Path path = Path.fromString(pathValue);
            return !resolvedCollectionPaths.contains(path) && !paths.contains(path);
        } catch (IllegalArgumentException e) {
            // Don't do anything, the path is invalid, just ignore it
            return false;
        }
    }

    private void handleRecursiveAggregation(Resource collection, String token, List<Path> paths,
            List<Path> resolvedCollectionPaths, int depthCount) {
        List<Path> pathList = new ArrayList<Path>(paths);
        Property recursiveAggregationProp = collection.getProperty(this.recursiveAggregationPropDef);
        boolean handleRecursion = recursiveAggregationProp != null && recursiveAggregationProp.getBooleanValue();
        if (handleRecursion) {
            for (Path path : pathList) {
                try {
                    if (resolvedCollectionPaths.contains(path)) {
                        continue;
                    }
                    Resource resource = this.repository.retrieve(token, path, false);
                    if (!resource.isCollection()) {
                        continue;
                    }
                    resolvedCollectionPaths.add(resource.getURI());

                    System.out.println("Aggregating " + resource.getURI() + " with count " + depthCount);

                    Property aggregationProp = resource.getProperty(this.aggregationPropDef);
                    if (aggregationProp != null) {
                        resolveAggregatedFolderPaths(aggregationProp, paths, resolvedCollectionPaths);
                        handleRecursiveAggregation(resource, token, paths, resolvedCollectionPaths, depthCount += 1);
                    }

                } catch (Exception e) {
                    logger.error("An error occured while resolving recursive aggregation: " + e.getMessage());
                }
            }
        }
    }

    private Query extend(Query query, OrQuery aggregatedFoldersQuery, AbstractMultipleQuery extendable) {

        if (query instanceof AbstractMultipleQuery) {
            if (extendable == null) {
                extendable = getExtendableQuery(query);
            }
            for (Query q : ((AbstractMultipleQuery) query).getQueries()) {
                if (isExtendableUriPrefixQuery(q)) {
                    q = getAggregatedUriQuery(q, aggregatedFoldersQuery);
                } else if (q instanceof AbstractMultipleQuery) {
                    q = extend(q, aggregatedFoldersQuery, getExtendableQuery(q));
                }
                extendable.add(q);
            }
            return extendable;
        } else if (isExtendableUriPrefixQuery(query)) {
            query = getAggregatedUriQuery(query, aggregatedFoldersQuery);
        }

        return query;

    }

    private AbstractMultipleQuery getExtendableQuery(Query query) {
        if (query instanceof AndQuery) {
            return new AndQuery();
        }
        return new OrQuery();
    }

    private Query getAggregatedUriQuery(Query query, OrQuery aggregatedFoldersQuery) {
        OrQuery aggregatedUriQuery = new OrQuery();
        aggregatedUriQuery.add(query);
        for (Query q : aggregatedFoldersQuery.getQueries()) {
            aggregatedUriQuery.add(q);
        }
        return aggregatedUriQuery;
    }

    private boolean isExtendableUriPrefixQuery(Query query) {
        if (query instanceof UriPrefixQuery) {
            UriPrefixQuery uriPrefixQuery = (UriPrefixQuery) query;
            if (uriPrefixQuery.getOperator() == null || uriPrefixQuery.getOperator() == TermOperator.EQ) {
                return true;
            }
        }
        return false;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setAggregationPropDef(PropertyTypeDefinition aggregationPropDef) {
        this.aggregationPropDef = aggregationPropDef;
    }

    public void setRecursiveAggregationPropDef(PropertyTypeDefinition recursiveAggregationPropDef) {
        this.recursiveAggregationPropDef = recursiveAggregationPropDef;
    }

    public void setLimit(int limit) {
        if (limit < 1) {
            logger.warn("Limit must be > 0, defaulting to " + DEFAULT_LIMIT);
        }
        this.limit = limit;
    }

    public void setMaxRecursiveDepth(int maxRecursiveDepth) {
        if (maxRecursiveDepth < 1) {
            logger.warn("Maximum depth for recursion must be > 0, defaulting to " + DEFAULT_RECURSIVE_DEPTH);
        }
        this.maxRecursiveDepth = maxRecursiveDepth;
    }

}
