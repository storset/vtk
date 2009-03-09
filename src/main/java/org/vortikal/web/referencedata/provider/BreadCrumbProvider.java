/* Copyright (c) 2004, 2008, University of Oslo, Norway
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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
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
 *   <li><code>ignoreProperty</code> - a resource property definition specifying
 *   whether to not include a given resource in the breadcrumb data model. 
 *   Resources are ignored when the property exists.
 *   <li><code>titleOverrideProperties</code> - a {@link PropertyTypeDefinition} array
 *   specifying properties that override the names of the resources in
 *   the breadcrumb when present. If such a property is present on a
 *   resource, the value of that property is used as the {@link
 *   BreadcrumbElement#getTitle title} of the breadcrumb
 *   element.
 *   <li><code>skipCurrentResource</code> - whether to skip the last 
 *   element in the breadcrumb. Defaults to <code>false</code>. 
 * </ul>
 * 
 * <p>In addition to the <code>skipCurrentResource</code> config property,
 * this component looks in the model for an entry by the name 
 * <code>include-last-element</code>. If this entry exists and has the value 
 * <code>true</code>, the last breadcrumb element will be included regardless 
 * of the configuration. 
 *
 * <p>Model data published:
 * <ul>
 * <li><code>breadcrumb</code> (or, if the property
 * <code>breadcrumbName</code> is specified, the value of that
 * property): a {@link BreadcrumbElement} array constituting the
 * breadcrumb trail.
 * </ul>
 */
public class BreadCrumbProvider implements ReferenceDataProvider, InitializingBean {

    private Repository repository;
    private Service service;
    private String breadcrumbName = "breadcrumb";
    private PropertyTypeDefinition ignoreProperty;
    private PropertyTypeDefinition[] titleOverrideProperties;
    private boolean skipCurrentResource;
    private boolean skipIndexFile;
    private PropertyTypeDefinition navigationTitlePropDef;

	@Required
    public final void setRepository(final Repository newRepository) {
        this.repository = newRepository;
    }

	@Required
	public void setService(Service service) {
        this.service = service;
    }
    
    public void setBreadcrumbName(String breadcrumbName) {
        this.breadcrumbName = breadcrumbName;
    }
    
    public void setIgnoreProperty(PropertyTypeDefinition ignoreProperty) {
        this.ignoreProperty = ignoreProperty;
    }

    public void setTitleOverrideProperties(
        PropertyTypeDefinition[] titleOverrideProperties) {
        this.titleOverrideProperties = titleOverrideProperties;
    }
    
    public void setSkipIndexFile(boolean skipIndexFile) {
        this.skipIndexFile = skipIndexFile;
    }

    public void setSkipCurrentResource(boolean skipCurrentResource) {
        this.skipCurrentResource = skipCurrentResource;
    }
    
    public void setNavigationTitlePropDef(
			PropertyTypeDefinition navigationTitlePropDef) {
		this.navigationTitlePropDef = navigationTitlePropDef;
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
    }

    @SuppressWarnings("unchecked")
    public void referenceData(Map model, HttpServletRequest request) {
        
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();

        boolean skipLastElement = this.skipCurrentResource;
        Object includeLast = model.get("include-last-element");
        if (includeLast != null 
                && ("true".equals(includeLast) || Boolean.TRUE.equals(includeLast))) {
            skipLastElement = false;
        }
        
        List<BreadcrumbElement> breadCrumb = 
            generateBreadcrumb(token, principal, uri, skipLastElement, requestContext.isIndexFile());
        model.put(this.breadcrumbName, breadCrumb.toArray(new BreadcrumbElement[breadCrumb.size()]));
    }


    private List<BreadcrumbElement> generateBreadcrumb(String token, Principal principal, Path uri, boolean skipLastElement, boolean isIndexFile) {
        
        List<BreadcrumbElement> breadCrumb = new ArrayList<BreadcrumbElement>();
        if (uri.isRoot()) {
            return breadCrumb;
        }
        
        List<String> path = uri.getElements();
        List<Path> incrementalPath = uri.getPaths();
            
        int length = path.size();
        if (this.skipIndexFile && isIndexFile) {
            length--;
        }
        if (skipLastElement) {
            length--;
        }

        for (int i = 0; i < length; i++) {
            try {
                Resource r = this.repository.retrieve(token, incrementalPath.get(i), true);

                if (hasIgnoreProperty(r)) {
                    continue;
                }
                String title = getTitle(r);
                String navigationTitle = null;
                if (this.navigationTitlePropDef != null) {
            		navigationTitle = getNavigationTitle(r);
            	}
            	title = StringUtils.isBlank(navigationTitle) ? title : navigationTitle;
                String url = null;
                if (i < length - 1 || skipLastElement) {
                    url = this.service.constructLink(r, principal, false);
                }

                if (i == length - 1 && !skipLastElement) {
                    breadCrumb.add(new BreadcrumbElement(url, title, null));
                } else {
                    breadCrumb.add(new BreadcrumbElement(url, title));
                }
            } catch (Exception e) {
                breadCrumb.add(new BreadcrumbElement(null, path.get(i)));
                String msg = e.getMessage();
                if (msg == null) {
                    msg = e.getClass().getName();
                }
            }
        }
        return breadCrumb;
    }

    private boolean hasIgnoreProperty(Resource resource) {
        if (this.ignoreProperty != null) {
            Property p = resource.getProperty(this.ignoreProperty);
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
            for (PropertyTypeDefinition overridePropDef: this.titleOverrideProperties) {
                Property property = resource.getProperty(overridePropDef);
                if (property != null && property.getStringValue() != null) {
                    return property.getStringValue();
                }
            }
        }
        if (resource.getName().equals("/")) return this.repository.getId();
        return resource.getName();
    }
    
    private String getNavigationTitle(Resource resource) {
    	Property prop = resource.getProperty(this.navigationTitlePropDef);
    	if (prop != null) {
    		return prop.getStringValue();
    	}
    	return null;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName());
        sb.append(" [ ");
        sb.append("breadcrumbName = ").append(this.breadcrumbName);
        sb.append(" ]");
        return sb.toString();
    }
}

