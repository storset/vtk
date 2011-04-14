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
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.query.AbstractMultipleQuery;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.web.RequestContext;

public class CollectionListingAggregationResolver implements AggregationResolver {

    private static Log logger = LogFactory.getLog(CollectionListingAggregationResolver.class);

    private PropertyTypeDefinition aggregationPropDef;

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

    public List<Path> getAggregationPaths(Path pathToResource) {
        RequestContext requestContext = RequestContext.getRequestContext();
        Resource resource = getResource(requestContext, pathToResource);
        if (resource == null) {
            return null;
        }
        List<Path> paths = new ArrayList<Path>();
        getAggregationPaths(paths, pathToResource, resource, requestContext, 0);
        return paths;
    }

    public Query getAggregationQuery(Query originalQuery, Resource collection) throws IllegalArgumentException {
        return this.getAggregationQuery(originalQuery, collection, true);
    }

    public Query getAggregationQuery(Query originalQuery, Resource collection, boolean includeOriginalUriPrefix)
            throws IllegalArgumentException {

        // only extend simple UriPrefixQuery or instances of
        // AbstractMultipleQuery (AndQuery or OrQuery) containing UriPrefixQuery
        if (!(originalQuery instanceof AbstractMultipleQuery || originalQuery instanceof UriPrefixQuery)) {
            IllegalArgumentException up = new IllegalArgumentException("Unsupported query type: " + originalQuery);
            throw up;
        }

        Property aggregationProp = collection.getProperty(this.aggregationPropDef);
        if (aggregationProp != null) {

            // get an OrQuery or UriPrefixQuery containing valid path(s) to
            // aggregate from
            Query aggregatedQuery = createAggregationQuery(collection, aggregationProp);

            if (aggregatedQuery == null) {
                return originalQuery;
            }

            // originalQuery is a simple UriPrefixQuery and we aggregate from
            // only ONE other collection
            if (isExtendableUriPrefixQuery(originalQuery) && aggregatedQuery instanceof UriPrefixQuery) {
                return aggregatedQuery;
            } else {
                // iterate the original query and build a new one,
                // replacing any UriPrefixQuery you encounter
                AbstractMultipleQuery extendableQuery = getExtendableQuery(originalQuery);
                for (Query q : ((AbstractMultipleQuery) originalQuery).getQueries()) {
                    q = aggregate(q, aggregatedQuery, null);
                    extendableQuery.add(q);
                }
                return extendableQuery;
            }

        }
        return originalQuery;
    }

    private Query createAggregationQuery(Resource collection, Property aggregationProp) {

        // the actual paths to aggregate from
        // used to create an OrQuery to extend the original query with
        List<Path> paths = new ArrayList<Path>();
        RequestContext requestContext = RequestContext.getRequestContext();
        getAggregationPaths(paths, collection.getURI(), collection, requestContext, 0);

        // no valid paths to aggregate from were supplied
        if (paths.size() == 0) {
            return null;
        }

        if (paths.size() == 1) {
            return new UriPrefixQuery(paths.get(0).toString(), TermOperator.EQ, false);
        }

        OrQuery aggregatedFoldersQuery = new OrQuery();
        for (Path path : paths) {
            aggregatedFoldersQuery.add(new UriPrefixQuery(path.toString(), TermOperator.EQ, false));
        }
        return aggregatedFoldersQuery;
    }

    private void getAggregationPaths(List<Path> paths, Path startingPath, Resource collection,
            RequestContext requestContext, int depth) {
        List<Path> addedPaths = addToPaths(collection, paths, startingPath);
        if (depth < this.maxRecursiveDepth) {
            depth += 1;
            for (Path path : addedPaths) {
                Resource resource = getResource(requestContext, path);
                if (resource == null) {
                    paths.remove(path);
                    continue;
                }
                getAggregationPaths(paths, startingPath, resource, requestContext, depth);
            }
        } else {
            for (Path path : addedPaths) {
                Resource resource = getResource(requestContext, path);
                if (resource == null) {
                    paths.remove(path);
                    continue;
                }
            }
        }
    }

    private Resource getResource(RequestContext requestContext, Path path) {
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        try {
            Resource resource = repository.retrieve(token, path, false);
            if (!resource.isCollection()) {
                return null;
            }
            return resource;
        } catch (ResourceNotFoundException rnfe) {
            // resource doesn'n exist, ignore
        } catch (Exception e) {
            logger.warn("An error occured while resolving recursive aggregation for path '" + path + "': "
                    + e.getMessage());
        }
        return null;
    }

    private List<Path> addToPaths(Resource collection, List<Path> paths, Path startingPath) {
        if (!paths.contains(collection.getURI()) && !collection.getURI().equals(startingPath)) {
            paths.add(collection.getURI());
        }
        List<Path> addedPaths = new ArrayList<Path>();
        Property aggregationProp = collection.getProperty(this.aggregationPropDef);
        if (aggregationProp != null) {
            Value[] values = aggregationProp.getValues();
            int aggregationLimit = values.length > this.limit ? this.limit : values.length;
            for (int i = 0; i < aggregationLimit; i++) {
                Path path = getValidPath(values[i].getStringValue());
                if (path != null && !paths.contains(path) && !path.equals(startingPath)) {
                    paths.add(path);
                    addedPaths.add(path);
                }
            }
        }
        return addedPaths;
    }

    private Path getValidPath(String pathValue) {
        try {
            if (!"/".equals(pathValue) && pathValue.endsWith("/")) {
                pathValue = pathValue.substring(0, pathValue.length() - 1);
            }
            return Path.fromString(pathValue);
        } catch (IllegalArgumentException e) {
            // Don't do anything, the path is invalid, just ignore it
        }
        return null;
    }

    private Query aggregate(Query query, Query aggregatedQuery, AbstractMultipleQuery extendable) {

        if (query instanceof AbstractMultipleQuery) {
            if (extendable == null) {
                extendable = getExtendableQuery(query);
            }
            for (Query q : ((AbstractMultipleQuery) query).getQueries()) {
                if (isExtendableUriPrefixQuery(q)) {
                    OrQuery uriPrefixOrQuery = null;
                    if (aggregatedQuery instanceof UriPrefixQuery) {
                        uriPrefixOrQuery = new OrQuery();
                        uriPrefixOrQuery.add(aggregatedQuery);
                    } else {
                        uriPrefixOrQuery = (OrQuery) aggregatedQuery;
                    }
                    uriPrefixOrQuery.add(q);
                    q = uriPrefixOrQuery;
                } else if (q instanceof AbstractMultipleQuery) {
                    q = aggregate(q, aggregatedQuery, getExtendableQuery(q));
                }
                extendable.add(q);
            }
            return extendable;
        } else if (isExtendableUriPrefixQuery(query)) {
            OrQuery uriPrefixOrQuery = new OrQuery();
            uriPrefixOrQuery.add(query);
            uriPrefixOrQuery.add(aggregatedQuery);
            return uriPrefixOrQuery;
        }

        return query;

    }

    private AbstractMultipleQuery getExtendableQuery(Query query) {
        if (query instanceof AndQuery) {
            return new AndQuery();
        }
        return new OrQuery();
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

    public void setAggregationPropDef(PropertyTypeDefinition aggregationPropDef) {
        this.aggregationPropDef = aggregationPropDef;
    }

    public void setLimit(int limit) {
        if (limit < 1) {
            logger.warn("Limit must be > 0, defaulting to " + DEFAULT_LIMIT);
            return;
        }
        this.limit = limit;
    }

    public void setMaxRecursiveDepth(int maxRecursiveDepth) {
        if (maxRecursiveDepth < 1) {
            logger.warn("Maximum depth for recursion must be > 0, defaulting to " + DEFAULT_RECURSIVE_DEPTH);
            return;
        }
        this.maxRecursiveDepth = maxRecursiveDepth;
    }

}
