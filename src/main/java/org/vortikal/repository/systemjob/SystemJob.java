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
package org.vortikal.repository.systemjob;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.context.BaseContext;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.PropertyExistsQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.SecurityContext;

public abstract class SystemJob implements InitializingBean {

    private Log logger = LogFactory.getLog(SystemJob.class);

    private static final int MAX_LIMIT = 2000;

    private String systemJobName;
    private Repository repository;
    private Searcher searcher;
    private int limit = MAX_LIMIT;
    private SecurityContext securityContext;
    private PropertyTypeDefinition systemJobStatusPropDef;
    private ResourceTypeTree resourceTypeTree;
    private Map<String, ?> config;

    /**
     * List of pointers to properties that are to be affected as a result of
     * this job. If none, all properties of the resource in question are to be
     * affected.
     */
    private List<String> affectedPropDefPointers;
    private List<PropertyTypeDefinition> affectedProperties;

    protected abstract Query getSearchQuery();

    // Whether or not to ignore unpublished resources. Set to 'true' means only
    // published resources will be handled
    protected abstract boolean handlePublishedOnly();

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.affectedPropDefPointers != null) {
            for (String pointer : this.affectedPropDefPointers) {
                PropertyTypeDefinition prop = this.resourceTypeTree.getPropertyDefinitionByPointer(pointer);
                if (this.affectedProperties == null) {
                    this.affectedProperties = new ArrayList<PropertyTypeDefinition>();
                }
                if (prop != null) {
                    this.affectedProperties.add(prop);
                }
            }
        }
    }

    public synchronized void execute() {

        if (this.repository.isReadOnly()) {
            return;
        }

        try {

            BaseContext.pushContext();
            SecurityContext.setSecurityContext(this.securityContext);
            String token = SecurityContext.getSecurityContext().getToken();

            Query query = getSearchQuery();
            // Add paths to ignore when processing (ignore resources that are
            // not relevant for system job maintenance)
            query = this.addIgnorePathsQuery(query);
            Search search = new Search();
            search.setQuery(query);
            search.setSorting(null);
            search.setLimit(this.limit);
            search.setOnlyPublishedResources(this.handlePublishedOnly());
            ResultSet results = this.searcher.execute(token, search);

            logger.info("Running job '" + this.systemJobName + "', " + results.getSize()
                    + " resource(s) to be affected");

            for (PropertySet propSet : results.getAllResults()) {

                try {
                    Resource resource = this.repository.retrieve(token, propSet.getURI(), true);
                    if (resource.getLock() == null) {

                        // XXX
                        // Explicit cast to ResourceImpl because we don't want
                        // to expose this further up the chain. This whole thing
                        // can suck monkey ballsack
                        String time = SystemJobContext.dateAsTimeString(Calendar.getInstance().getTime());
                        SystemJobContext systemJobContext = new SystemJobContext(this.systemJobName, time,
                                this.affectedProperties, this.systemJobStatusPropDef);
                        ((ResourceImpl) resource).setSystemJobContext(systemJobContext);

                        this.repository.store(token, resource);
                    }
                } catch (ResourceNotFoundException rnfe) {
                    // Resource is no longer there after search (deleted, moved
                    // or renamed)
                    logger.warn("A resource (" + propSet.getURI()
                            + ") that was to be affected by a systemjob was no longer available: " + rnfe.getMessage());
                }
            }

        } catch (Throwable t) {
            logger.error("An error occured while running job '" + this.systemJobName + "'", t);
        } finally {
            SecurityContext.setSecurityContext(null);
            BaseContext.popContext();
        }

    }

    protected Query getSystemJobQuery() {
        OrQuery orQuery = new OrQuery();

        PropertyExistsQuery systemJobPropertyExistsQuery = new PropertyExistsQuery(this.systemJobStatusPropDef, true);
        systemJobPropertyExistsQuery.setComplexValueAttributeSpecifier(this.systemJobName);
        orQuery.add(systemJobPropertyExistsQuery);

        String now = SystemJobContext.dateAsTimeString(Calendar.getInstance().getTime());
        PropertyTermQuery systemJobPropertyQuery = new PropertyTermQuery(this.systemJobStatusPropDef, now,
                TermOperator.LT);
        systemJobPropertyQuery.setComplexValueAttributeSpecifier(this.systemJobName);
        orQuery.add(systemJobPropertyQuery);

        return orQuery;
    }

    @SuppressWarnings("unchecked")
    private Query addIgnorePathsQuery(Query query) {

        // Check configuration file for other filters to add (e.g.
        // resourcetypes/paths to ignore)
        if (this.config != null) {
            Object obj = config.get(this.systemJobName);
            if (obj != null) {
                if (obj instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) obj;
                    Object ignore = m.get("ignore");
                    if (ignore != null && (ignore instanceof ArrayList)) {
                        AndQuery andQuery = new AndQuery();
                        andQuery.add(query);
                        List<String> ignoreList = (ArrayList<String>) ignore;
                        AndQuery ignoreQuery = new AndQuery();
                        for (String ignoreUri : ignoreList) {
                            ignoreQuery.add(new UriPrefixQuery(ignoreUri, true));
                        }
                        andQuery.add(ignoreQuery);
                        return andQuery;
                    }
                }
            }
        }

        // No configuration found, or no filters for current job
        return query;
    }

    @Required
    public void setSystemJobName(String systemJobName) {
        this.systemJobName = systemJobName;
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    public void setLimit(int limit) {
        if (limit < 1) {
            logger.warn("Limit must be > 0, defaulting to " + MAX_LIMIT);
            return;
        }
        this.limit = limit;
    }

    @Required
    public void setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Required
    public void setSystemJobStatusPropDef(PropertyTypeDefinition systemJobStatusPropDef) {
        this.systemJobStatusPropDef = systemJobStatusPropDef;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    public ResourceTypeTree getResourceTypeTree() {
        return resourceTypeTree;
    }

    public void setAffectedPropDefPointers(List<String> affectedPropDefPointers) {
        this.affectedPropDefPointers = affectedPropDefPointers;
    }

    public void setConfig(Map<String, ?> config) {
        this.config = config;
    }

}
