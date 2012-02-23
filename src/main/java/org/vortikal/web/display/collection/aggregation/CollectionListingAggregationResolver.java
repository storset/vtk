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
import org.vortikal.security.SecurityContext;
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

        PropertySet collection = null;
        URL currentURL = this.getLocalHostUrl();
        String token = SecurityContext.getSecurityContext().getToken();
        if (url.getHost().equals(currentURL.getHost())) {
            Path path = url.getPath();
            try {
                collection = this.repository.retrieve(token, path, false);
            } catch (Exception e) {
                // Ignore
            }
        } else {
            collection = this.multiHostSearcher.retrieve(token, url);
        }

        if (collection == null) {
            return null;
        }

        return this.getAggregatedResources(collection);
    }

    @Override
    public CollectionListingAggregatedResources getAggregatedResources(PropertySet collection) {

        Map<URL, Set<Path>> aggregationSet = new HashMap<URL, Set<Path>>();
        Set<URL> manuallyApprovedSet = new HashSet<URL>();

        URL currentURL = this.resolveCurrentURL(collection);
        this.resolveAggregatedResources(aggregationSet, manuallyApprovedSet, collection, currentURL, 0);
        CollectionListingAggregatedResources clar = new CollectionListingAggregatedResources(aggregationSet,
                manuallyApprovedSet);

        return clar;
    }

    private void resolveAggregatedResources(Map<URL, Set<Path>> aggregationSet, Set<URL> manuallyApprovedSet,
            PropertySet resource, URL currentURL, int depth) {
        if (this.isDisplayManuallyApproved(resource)) {
            Property manuallyApprovedProp = resource.getProperty(this.manuallyApprovedPropDef);
            if (manuallyApprovedProp != null) {
                Value[] values = manuallyApprovedProp.getValues();
                for (Value manApp : values) {
                    manuallyApprovedSet.add(URL.parse(manApp.getStringValue()));
                }
            }
        }
        Set<PropertySet> set = this.resolveAggregation(resource, aggregationSet, manuallyApprovedSet, currentURL);
        if (depth < this.maxRecursiveDepth) {
            depth += 1;
            for (PropertySet ps : set) {
                currentURL = this.resolveCurrentURL(ps);
                this.resolveAggregatedResources(aggregationSet, manuallyApprovedSet, ps, currentURL, depth);
            }
        }
    }

    private Set<PropertySet> resolveAggregation(PropertySet resource, Map<URL, Set<Path>> aggregationSet,
            Set<URL> manuallyApprovedSet, URL currentURL) {
        Set<PropertySet> set = new HashSet<PropertySet>();
        if (this.isDisplayAggregation(resource)) {
            Property aggregationProp = resource.getProperty(this.aggregationPropDef);
            if (aggregationProp != null) {
                Value[] values = aggregationProp.getValues();
                int aggregationLimit = values.length > this.limit ? this.limit : values.length;
                for (int i = 0; i < aggregationLimit; i++) {
                    String aggStr = values[i].getStringValue();
                    if (!currentURL.equals(this.getLocalHostUrl()) && this.isSimplePath(aggStr)) {
                        aggStr = currentURL.toString().concat(aggStr);
                    }
                    URL hostURL = currentURL;
                    PropertySet res = this.getResource(aggStr);
                    if (res != null) {
                        set.add(res);
                        URL url = this.resolveAsUrl(aggStr);
                        if (url != null) {
                            hostURL = url.relativeURL("/");
                        }
                        Path path = null;
                        if (url != null) {
                            path = url.getPath();
                        } else {
                            path = Path.fromString(aggStr);
                        }
                        Set<Path> paths = aggregationSet.get(hostURL);
                        if (paths != null) {
                            paths.add(path);
                        } else {
                            Set<Path> pathSet = new HashSet<Path>();
                            pathSet.add(path);
                            aggregationSet.put(hostURL, pathSet);
                        }
                    }
                }
            }
        }
        return set;
    }

    private boolean isSimplePath(String aggStr) {
        try {
            Path.fromString(aggStr);
            return true;
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }

    private boolean isDisplayAggregation(PropertySet resource) {
        Property displayAggregationProp = resource.getProperty(this.displayAggregationPropDef);
        return displayAggregationProp != null && displayAggregationProp.getBooleanValue();
    }

    private boolean isDisplayManuallyApproved(PropertySet resource) {
        Property displayManuallyApprovedProp = resource.getProperty(this.displayManuallyApprovedPropDef);
        return displayManuallyApprovedProp != null && displayManuallyApprovedProp.getBooleanValue();
    }

    private PropertySet getResource(String pathOrUrl) {
        String token = RequestContext.getRequestContext().getSecurityToken();
        try {

            PropertySet resource = null;
            if (this.isLocalPath(pathOrUrl)) {
                resource = this.repository.retrieve(token, Path.fromString(pathOrUrl), false);
            } else if (this.multiHostSearcher.isMultiHosSearchEnabled()) {
                URL url = this.resolveAsUrl(pathOrUrl);
                if (url != null) {
                    resource = multiHostSearcher.retrieve(token, url);
                }
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

    private URL resolveAsUrl(String pathOrUrl) {
        try {
            return URL.parse(pathOrUrl);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isLocalPath(String pathOrUrl) {
        try {
            Path.fromString(pathOrUrl);
        } catch (IllegalArgumentException iae) {
            return false;
        }
        return true;
    }

    private URL resolveCurrentURL(PropertySet collection) {
        URL currentURL = null;
        Property solrUrlProp = collection.getProperty(Namespace.DEFAULT_NAMESPACE, MultiHostSearcher.URL_PROP_NAME);
        if (solrUrlProp != null) {
            currentURL = URL.parse(solrUrlProp.getStringValue()).relativeURL("/");
        } else {
            currentURL = this.getLocalHostUrl();
        }
        return currentURL;
    }

    private URL getLocalHostUrl() {
        return RequestContext.getRequestContext().getRequestURL().relativeURL("/");
    }

    @Override
    public Set<Path> getAggregationPaths(Path pathToResource) {
        String token = RequestContext.getRequestContext().getSecurityToken();
        try {
            Resource collection = this.repository.retrieve(token, pathToResource, false);
            CollectionListingAggregatedResources clar = this.getAggregatedResources(collection);
            Map<URL, Set<Path>> aggregatedSet = clar.getAggregationSet();
            if (aggregatedSet != null) {
                return aggregatedSet.get(this.getLocalHostUrl());
            }
            return null;
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
