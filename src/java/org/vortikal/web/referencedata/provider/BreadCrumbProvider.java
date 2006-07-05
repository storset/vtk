/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.referencedata.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;


/**
 * Creates model data for a breadcrumb (list of URLs from the root
 * resource to the current).
 * 
 * <p>Description: creates an array of {@link BreadcrumbElement}
 * objects, which is the "breadcrumb trail" from the root resource
 * down to the collection containing the current resource.
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the content repository
 *   <li><code>service</code> - the service for which to construct breadcrumb URLs
 *   <li><code>breadcrumbName</code> - the name to publish the
 *   breadcrumb under (default <code>breadcrumb</code>
 *   <li><code>useDisplayNames</code> - a boolean indicating whether
 *   to use the resource's <code>displayname</code> property (rather
 *   than the default <code>name</code>) as breadcrumb element
 *   titles. Default is <code>false</code>
 *   <li><code>skippedURLs</code> - a list of resource URIs for which
 *   to skip URL generation. That is, {@link BreadcrumbElement#getURL}
 *   will return <code>null</code> for the resources included in this
 *   list.
 *   <li><code>ignoreProperty</code> - a resource property specified
 *   as <code>namespace:name</code> specifying whether to not include
 *   a given resource in the breadcrumb data model. Resources are
 *   ignored when the property exists.
 *   <li><code>titleOverrideProperties</code> - a {@link String} array
 *   of fully qualified property names (<code>namespace:name</code>)
 *   specifying properties that override the names of the resources in
 *   the breadcrumb when present. If such a property is present on a
 *   resource, the value of that property is used as the {@link
 *   BreadcrumbElement#getTitle title} of the breadcrumb
 *   element. Note: this setting overrides the
 *   <code>useDisplayNames</code> property.
 * </ul>
 *
 * <p>Model data published:
 * <ul>
 * <li><code>breadcrumb</code> (or, if the property
 * <code>breadcrumbName</code> is specified, the value of that
 * property): a {@link BreadcrumbElement} array constituting the
 * breadcrumb.
 * </ul>
 */
public class BreadCrumbProvider implements ReferenceDataProvider, InitializingBean {

    private static Log logger = LogFactory.getLog(BreadCrumbProvider.class);
    private Repository repository = null;
    private Service service = null;
    private String breadcrumbName = "breadcrumb";
    private boolean useDisplayNames = false;
    private String ignoreProperty = null;
    private String ignorePropertyNamespace = null;
    private String ignorePropertyName = null;
    private Namespace ignorePropertyNS = null;
    private String[] titleOverrideProperties = null;
    private String[] titleOverrideNamespaces = null;
    private String[] titleOverrideNames = null;
    private String[] skippedURLs = null;
    private Set skippedURLSet = null;
    
    
    public final Repository getRepository() {
        return this.repository;
    }


    public final void setRepository(final Repository newRepository) {
        this.repository = newRepository;
    }

    
    public void setService(Service service) {
        this.service = service;
    }
    

    public void setBreadcrumbName(String breadcrumbName) {
        this.breadcrumbName = breadcrumbName;
    }
    

    public void setUseDisplayNames(boolean useDisplayNames) {
        this.useDisplayNames = useDisplayNames;
    }
    

    public void setIgnoreProperty(String ignoreProperty) {
        this.ignoreProperty = ignoreProperty;
    }


    public void setTitleOverrideProperties(String[] titleOverrideProperties) {
        this.titleOverrideProperties = titleOverrideProperties;
    }
    
    
    public void setSkippedURLs(String[] skippedURLs) {
        this.skippedURLs = skippedURLs;
    }
    

    public final void afterPropertiesSet() throws Exception {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Property 'repository' not set");
        }
        if (this.service == null) {
            throw new BeanInitializationException(
                "Property 'service' not set");
        }
        if (this.breadcrumbName == null) {
            throw new BeanInitializationException(
                "Property 'breadcrumbName' cannot be null");
        }
        if (this.ignoreProperty != null) {
            if (this.ignoreProperty.indexOf(":") == -1) {
                throw new BeanInitializationException(
                    "Bad property name: " + this.ignoreProperty);
            }
            this.ignorePropertyNamespace = this.ignoreProperty.substring(
                0, this.ignoreProperty.lastIndexOf(":"));
            this.ignorePropertyName = this.ignoreProperty.substring(
                this.ignoreProperty.lastIndexOf(":") + 1);
            this.ignorePropertyNS = Namespace.getNamespace(this.ignorePropertyNamespace);
        }
        if (this.titleOverrideProperties != null) {
            for (int i = 0; i < this.titleOverrideProperties.length; i++) {
                if (this.titleOverrideProperties[i].indexOf("") == -1) {
                    throw new BeanInitializationException(
                        "Title override property " + this.titleOverrideProperties[i]
                        + "is not a fully qualified property name");
                }
            }
            initTitleOverrideProperties();
        }

        if (this.skippedURLs != null) {
            this.skippedURLSet = new HashSet();
            for (int i = 0; i < this.skippedURLs.length; i++) {
                this.skippedURLSet.add(this.skippedURLs[i]);
            }
        }
    }


    public void referenceData(Map model, HttpServletRequest request) {
        
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();

        String[] path = URLUtil.splitUri(uri);
        String[] incrementalPath = URLUtil.splitUriIncrementally(uri);
        
        List breadCrumb = new ArrayList();

        for (int i = 0; i < path.length - 1; i++) {
            try {
                Resource r = this.repository.retrieve(token, incrementalPath[i], true);

                if (checkIgnore(r)) {
                    continue;
                }
                String title = getTitle(r);
                String url = null;
                if (this.skippedURLSet == null
                    || !this.skippedURLSet.contains(incrementalPath[i])) {
                    url = this.service.constructLink(r, principal, false);
                }
                breadCrumb.add(new BreadcrumbElement(url, title));
            } catch (Exception e) {
                breadCrumb.add(new BreadcrumbElement(null, path[i]));
                logger.warn("Unable to generate breadcrumb path element " + incrementalPath[i], e);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Generated breadcrumb path: " + breadCrumb);
        }

        model.put(this.breadcrumbName, breadCrumb.toArray((new BreadcrumbElement[0])));
    }


    private boolean checkIgnore(Resource resource) {
        if (this.ignoreProperty != null) {
            Property p = resource.getProperty(this.ignorePropertyNS,
                                              this.ignorePropertyName);
            if (p != null) {
                return true;
            }
        }
        return false;
    }
    

    private String getTitle(Resource resource) {
        if (this.titleOverrideProperties != null
            && this.titleOverrideProperties.length > 0) {

            // Check titleOverrideProperties in correct order
            for (int i = 0; i < this.titleOverrideProperties.length; i++) {

                Namespace namespace = Namespace.getNamespace(this.titleOverrideNamespaces[i]);
                String name = this.titleOverrideNames[i];
                
                Property property = resource.getProperty(namespace, name);
                if (property != null && property.getStringValue() != null) {
                    return property.getStringValue();
                }
            }
        }
        return (this.useDisplayNames) ?
            resource.getDisplayName() : resource.getName();
    }
    

    private void initTitleOverrideProperties() {

        this.titleOverrideNamespaces = new String[this.titleOverrideProperties.length];
        this.titleOverrideNames = new String[this.titleOverrideProperties.length];

        
        for (int i = 0; i < this.titleOverrideProperties.length; i++) {

            String namespace = this.titleOverrideProperties[i].substring(
                    0, this.titleOverrideProperties[i].lastIndexOf(":"));
            String name = this.titleOverrideProperties[i].substring(
                this.titleOverrideProperties[i].lastIndexOf(":") + 1);

            this.titleOverrideNamespaces[i] = namespace;
            this.titleOverrideNames[i] = name;
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(" [ ");
        sb.append("breadcrumbName = ").append(this.breadcrumbName);
        sb.append(" ]");
        return sb.toString();
    }

}

