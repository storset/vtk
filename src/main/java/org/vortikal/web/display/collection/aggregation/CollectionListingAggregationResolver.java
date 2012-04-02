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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;

public class CollectionListingAggregationResolver implements AggregationResolver {

    private static Log logger = LogFactory.getLog(CollectionListingAggregationResolver.class);

    public final static int DEFAULT_LIMIT = 5;
    public final static int DEFAULT_RECURSIVE_DEPTH = 2;

    private Repository repository;
    private MultiHostSearcher multiHostSearcher;
    private PropertyTypeDefinition displayAggregationPropDef;
    private PropertyTypeDefinition aggregationPropDef;
    private PropertyTypeDefinition displayManuallyApprovedPropDef;
    private PropertyTypeDefinition manuallyApprovedPropDef;

    /**
     * Limit the number of folders to aggregate from
     */
    private int limit = DEFAULT_LIMIT;

    /**
     * Limit depth of the aggregation, in cases where a folder to aggregate from
     * has it's own defined aggregation
     */
    private int maxRecursiveDepth = DEFAULT_RECURSIVE_DEPTH;

    @Override
    public CollectionListingAggregatedResources getAggregatedResources(URL url) {

        PropertySet collection = this.getResource(url);

        // Resource not found
        if (collection == null) {
            return null;
        }

        return this.getAggregatedResources(collection);
    }

    @Override
    public CollectionListingAggregatedResources getAggregatedResources(PropertySet collection) {

        Map<URL, Set<Path>> aggregationSet = new HashMap<URL, Set<Path>>();
        Set<URL> manuallyApprovedSet = new HashSet<URL>();

        // Keep a reference to the starting point, avoid circular references
        // to self when resolving aggregation
        URL startCollectionURL = this.resolveCurrentCollectionURL(collection);

        // Current host url, key ref to all paths to aggregate from on that
        // particular host
        URL currentHostURL = startCollectionURL.relativeURL("/");

        // Resolve the aggregation
        this.resolveAggregatedResources(aggregationSet, manuallyApprovedSet, collection, currentHostURL,
                startCollectionURL, 0);

        CollectionListingAggregatedResources clar = new CollectionListingAggregatedResources(aggregationSet,
                manuallyApprovedSet);

        return clar;
    }

    private void resolveAggregatedResources(Map<URL, Set<Path>> aggregationSet, Set<URL> manuallyApprovedSet,
            PropertySet resource, URL currentHostURL, URL startCollectionURL, int depth) {

        // Include manually approved resources if any
        if (this.isDisplayManuallyApproved(resource)) {
            Property manuallyApprovedProp = resource.getProperty(this.manuallyApprovedPropDef);
            if (manuallyApprovedProp != null) {
                Value[] values = manuallyApprovedProp.getValues();
                for (Value manApp : values) {
                    try {
                        URL url = URL.parse(manApp.getStringValue());
                        manuallyApprovedSet.add(url);
                    } catch (Exception e) {
                        // Ignore invalid urls
                    }
                }
            }
        }

        // Resolve aggregation
        Set<PropertySet> set = this.resolveAggregation(resource, aggregationSet, currentHostURL, startCollectionURL);
        if (depth < this.maxRecursiveDepth) {
            depth += 1;
            for (PropertySet ps : set) {
                currentHostURL = this.resolveCurrentCollectionURL(ps).relativeURL("/");
                // Recursively repeat until depth is reached
                this.resolveAggregatedResources(aggregationSet, manuallyApprovedSet, ps, currentHostURL,
                        startCollectionURL, depth);
            }
        }

    }

    private Set<PropertySet> resolveAggregation(PropertySet resource, Map<URL, Set<Path>> aggregationSet,
            URL currentHostURL, URL startCollectionURL) {

        Set<PropertySet> resultSet = new HashSet<PropertySet>();
        if (this.isDisplayAggregation(resource)) {
            Property aggregationProp = resource.getProperty(this.aggregationPropDef);

            if (aggregationProp != null) {
                Value[] values = aggregationProp.getValues();

                // Only handle as many values as defined my limit
                int aggregationLimit = values.length > this.limit ? this.limit : values.length;

                for (int i = 0; i < aggregationLimit; i++) {
                    String aggStr = values[i].getStringValue();

                    URL aggregationURL = this.getAsURL(currentHostURL, aggStr);
                    if (aggregationURL == null) {
                        // Invalid ref, ignore and continue with next
                        continue;
                    }

                    // Do not include any references to starting point or
                    // already existing refs in aggregation set -> AVOID
                    // CIRCULAR RECURSION!!!
                    if (aggregationURL.equals(startCollectionURL)
                            || this.isAlreadyResolved(aggregationURL, aggregationSet)) {
                        continue;
                    }

                    // Valid ref, now make sure it points to an existing
                    // resource
                    PropertySet res = this.getResource(aggregationURL);
                    if (res != null) {

                        // Resource exists. Add it to return set for further
                        // aggregation resolving (until depth is reached) and
                        // add to set of hosts and paths to aggregate from
                        resultSet.add(res);

                        // Make sure currentHostURL is right
                        currentHostURL = aggregationURL.relativeURL("/");
                        Path path = aggregationURL.getPath();
                        Set<Path> paths = aggregationSet.get(currentHostURL);
                        if (paths != null) {
                            paths.add(path);
                        } else {
                            Set<Path> pathSet = new HashSet<Path>();
                            pathSet.add(path);
                            aggregationSet.put(currentHostURL, pathSet);
                        }

                    }

                }
            }
        }
        return resultSet;
    }

    private URL getAsURL(URL currentHostURL, String strVal) {

        URL url = null;
        try {
            url = URL.parse(strVal);
        } catch (Exception e) {
            // Ignore, continue, assume valid path
        }
        try {
            // Be lenient on trailing slash
            String pathString = strVal.endsWith("/") && !strVal.equals("/") ? strVal
                    .substring(0, strVal.lastIndexOf("/")) : strVal;
            Path path = Path.fromString(pathString);
            url = new URL(currentHostURL);
            url.setPath(path);
        } catch (IllegalArgumentException iae) {
            // Ignore, invalid
        }
        if (url != null) {
            url.setCollection(true);
        }

        return url;
    }

    private boolean isAlreadyResolved(URL aggregationURL, Map<URL, Set<Path>> aggregationSet) {
        URL hostURL = aggregationURL.relativeURL("/");
        Path path = aggregationURL.getPath();
        Set<Path> set = aggregationSet.get(hostURL);
        if (set == null) {
            return false;
        }
        return set.contains(path);
    }

    private boolean isDisplayAggregation(PropertySet resource) {
        Property displayAggregationProp = resource.getProperty(this.displayAggregationPropDef);
        return displayAggregationProp != null && displayAggregationProp.getBooleanValue();
    }

    private boolean isDisplayManuallyApproved(PropertySet resource) {
        Property displayManuallyApprovedProp = resource.getProperty(this.displayManuallyApprovedPropDef);
        return displayManuallyApprovedProp != null && displayManuallyApprovedProp.getBooleanValue();
    }

    private PropertySet getResource(URL url) {
        String token = null;
        if (RequestContext.exists()) {
            token = RequestContext.getRequestContext().getSecurityToken();
        }
        try {

            PropertySet resource = null;
            Path path = null;
            if (this.getLocalHostUrl().equals(url.relativeURL("/"))) {
                path = url.getPath();
            }
            if (path != null) {
                resource = this.repository.retrieve(token, path, false);
            } else if (this.multiHostSearcher.isMultiHosSearchEnabled()) {
                resource = multiHostSearcher.retrieve(token, url);
            }

            if (resource == null) {
                return null;
            }
            return resource;
        } catch (ResourceNotFoundException rnfe) {
            // resource doesn'n exist, ignore
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private URL resolveCurrentCollectionURL(PropertySet collection) {
        URL currentCollectionURL = null;
        Property solrUrlProp = collection.getProperty(Namespace.DEFAULT_NAMESPACE, MultiHostSearcher.URL_PROP_NAME);
        if (solrUrlProp != null) {
            currentCollectionURL = URL.parse(solrUrlProp.getStringValue());
        } else {
            currentCollectionURL = this.getLocalHostUrl();
            currentCollectionURL.setPath(collection.getURI());
        }
        return currentCollectionURL;
    }

    private URL getLocalHostUrl() {
        return RequestContext.getRequestContext().getRequestURL().relativeURL("/");
    }

    @Override
    public Set<Path> getAggregationPaths(Path pathToResource) {
        String token = null;
        if (RequestContext.exists()) {
            token = RequestContext.getRequestContext().getSecurityToken();
        }
        try {
            Resource collection = this.repository.retrieve(token, pathToResource, false);
            CollectionListingAggregatedResources clar = this.getAggregatedResources(collection);
            return clar.getHostAggregationSet(this.getLocalHostUrl());
        } catch (Exception e) {
            return null;
        }
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setMultiHostSearcher(MultiHostSearcher multiHostSearcher) {
        this.multiHostSearcher = multiHostSearcher;
    }

    @Required
    public void setAggregationPropDef(PropertyTypeDefinition aggregationPropDef) {
        this.aggregationPropDef = aggregationPropDef;
    }

    @Required
    public void setDisplayAggregationPropDef(PropertyTypeDefinition displayAggregationPropDef) {
        this.displayAggregationPropDef = displayAggregationPropDef;
    }

    @Required
    public void setDisplayManuallyApprovedPropDef(PropertyTypeDefinition displayManuallyApprovedPropDef) {
        this.displayManuallyApprovedPropDef = displayManuallyApprovedPropDef;
    }

    @Required
    public void setManuallyApprovedPropDef(PropertyTypeDefinition manuallyApprovedPropDef) {
        this.manuallyApprovedPropDef = manuallyApprovedPropDef;
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
