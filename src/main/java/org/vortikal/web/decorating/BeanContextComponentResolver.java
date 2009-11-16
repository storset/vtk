/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.decorating;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.TypeInfo;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;


public class BeanContextComponentResolver
  implements ComponentResolver, ApplicationContextAware, InitializingBean {

    private static Log logger = LogFactory.getLog(BeanContextComponentResolver.class);
    
    private volatile boolean initialized = false;
    private ApplicationContext applicationContext;
    private Map<String, DecoratorComponent> components = new HashMap<String, DecoratorComponent>();
    private Set<String> availableComponentNamespaces = new HashSet<String>();
    private Set<String> prohibitedComponentNamespaces = new HashSet<String>();
    private ResourceTypeDefinition resourceType;
    private Repository repository;
    
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void setAvailableComponentNamespaces(Set<String> availableComponentNamespaces) {
        this.availableComponentNamespaces = availableComponentNamespaces;
    }

    public void setInhibitedComponentNamespaces(Set<String> prohibitedComponentNamespaces) {
    	this.prohibitedComponentNamespaces = prohibitedComponentNamespaces;
    }

    public void setResourceType(ResourceTypeDefinition resourceType) {
        this.resourceType = resourceType;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void afterPropertiesSet() {
        if (this.applicationContext == null) {
            throw new BeanInitializationException(
                "JavaBean property 'applicationContext' not specified");
        }
        if (this.availableComponentNamespaces == null) {
            throw new BeanInitializationException(
                "JavaBean property 'availableComponentNamespaces' not specified");
        }
        if (this.prohibitedComponentNamespaces == null) {
            throw new BeanInitializationException(
                "JavaBean property 'prohibitedComponentNamespaces' not specified");
        }
        if (this.resourceType != null && this.repository == null) {
            throw new BeanInitializationException(
                 "JavaBean property 'repository' required when 'resourceType' is specified");
        }
    }
    
    public DecoratorComponent resolveComponent(String namespace, String name) {
        if (!this.initialized) {
            init();
        }
        DecoratorComponent component = this.components.get(namespace + ":" + name);
        if (this.resourceType != null) {
            TypeInfo type = getResourceTypeInfo();
            if (type == null || !type.isOfType(this.resourceType)) {
                component = null;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved namespace: '" + namespace + "', name: '" + name
                         + "' to component:  " + component);
        }
        return component;
    }
    
    
    public List<DecoratorComponent> listComponents() {
        if (!this.initialized) {
            init();
        }
        List<DecoratorComponent> result = new ArrayList<DecoratorComponent>();
        for (DecoratorComponent component : this.components.values()) {
            String namespace = component.getNamespace();
            if (this.availableComponentNamespaces != null) {
                if (!this.availableComponentNamespaces.contains(namespace)) {
                    continue;
                }
            }
            if (this.prohibitedComponentNamespaces != null) {
                if (this.prohibitedComponentNamespaces.contains(namespace)) {
                    continue;
                }
            }
            result.add(component);
        }
        return result;
    }
    

    private synchronized void init() {
    	if (this.initialized) return;
    	
    	@SuppressWarnings("unchecked")
        Collection<DecoratorComponent> beans = 
            BeanFactoryUtils.beansOfTypeIncludingAncestors(
                this.applicationContext, 
                DecoratorComponent.class, false, false).values();        
        
        for (DecoratorComponent component: beans) {
            String ns = component.getNamespace();
            String name = component.getName();
            if (ns == null) {
                throw new IllegalStateException("Component " + component
                                                + " has invalid namespace (NULL)");
            }
            if (!this.availableComponentNamespaces.contains(component.getNamespace())) {
            	if (!this.availableComponentNamespaces.contains("*")) {
            		if (logger.isDebugEnabled()) {
            			logger.debug("Component " + component + " not added.");
                        continue;
            		}
            	}
            }
            if (this.prohibitedComponentNamespaces.contains(component.getNamespace())) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("Component " + component + " not added (prohibited namespace).");
        		}
        		continue;
            }
            if (name == null) {
                throw new IllegalStateException("Component " + component
                                                + " has invalid name (NULL)");
            }
            String key = ns + ":" + name;
            logger.info("Registering decorator component " + component);
            this.components.put(key, component);
        }
        
        this.initialized = true;
    }

    private TypeInfo getResourceTypeInfo() {
        try {
            RequestContext requestContext = RequestContext.getRequestContext();
            SecurityContext securityContext = SecurityContext.getSecurityContext();
            return this.repository.getTypeInfo(securityContext.getToken(), requestContext.getResourceURI()); 
        } catch (Throwable t) {
            return null;
        }
    }
    
    private Resource getCurrentResource() {
        try {
            RequestContext requestContext = RequestContext.getRequestContext();
            SecurityContext securityContext = SecurityContext.getSecurityContext();
            return this.repository.retrieve(securityContext.getToken(), requestContext.getResourceURI(), true); 
        } catch (Throwable t) {
            return null;
        }
    }
}
