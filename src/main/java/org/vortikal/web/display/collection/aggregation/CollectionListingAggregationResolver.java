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
import org.vortikal.web.RequestContext;

public class CollectionListingAggregationResolver implements AggregationResolver {

    private static Log logger = LogFactory.getLog(CollectionListingAggregationResolver.class);

    private PropertyTypeDefinition aggregationPropDef;

    public final static int DEFAULT_LIMIT = 5;
    public final static int DEFAULT_RECURSIVE_DEPTH = 2;

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
