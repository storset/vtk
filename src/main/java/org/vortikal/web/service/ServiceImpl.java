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
package org.vortikal.web.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.web.AuthenticationChallenge;
import org.vortikal.util.net.NetUtils;


/**
 * Default implementation of the Service interface.
 *
 * <p>Configurable properties:
 * <ul>
 *   <lI><code>services</code> a list of {@link ServiceImpl} objects:
 *   explicit definition of child services
 *   <li><code>assertions</code> - a list of {@link Assertion
 *   assertions}; the conditions that must hold for this service to
 *   match
 *   <li><code>handler</code> - a {@link
 *   org.springframework.web.servlet.mvc.Controller} that is executed
 *   when this service matches (see {@link ServiceHandlerMapping}).
 *   <li><code>handlerInterceptors</code> - list of
 *   {@link org.springframework.web.servlet.HandlerInterceptor
 *   interceptors} that are executed prior to (around) the controller
 *   invocation.
 *   <li><code>order</code> - integer specifying the order of this
 *   service (see {@link org.springframework.core.Ordered}). Default is
 *   <code>0</code>.
 *   <li><code>categories</code> - a {@link Set} of strings denoting
 *   the set of categories this service is a member of.
 * </ul>
 *
 */
public class ServiceImpl
  implements Service, BeanNameAware, InitializingBean,
             ApplicationContextAware {

    private AuthenticationChallenge authenticationChallenge;
    private Object handler;
    private List assertions = new ArrayList();
    private List services = new ArrayList();
    private Service parent;
    private String name;
    private Map attributes = new HashMap();
    private List handlerInterceptors;
    private int order = 0;
    private Set categories = null;
    private ApplicationContext applicationContext;
    private List urlPostProcessors = new ArrayList();
    private List accumulatedUrlPostProcessors = null;
    	
    
    public void setHandler(Object handler) {
        this.handler = handler;
    }
	

    public void setAssertions(List assertions) {
        this.assertions = assertions;
    }
	

    public void setServices(List services) {
        this.services = services;
    }


    public void addServices(List services) {
        this.services.addAll(services);
    }
    

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    

    public List getChildren() {
        return this.services;
    }


    public Object getHandler() {
        return this.handler;
    }
	

    public void setAttributes(Map attributes) {
        this.attributes = attributes;
    }
    

    public Object getAttribute(String name) {
        if (this.attributes == null) {
            return null;
        }
        return this.attributes.get(name);
    }
    

    public void setUrlPostProcessors(List urlPostProcessors) {
        this.urlPostProcessors = urlPostProcessors;
    }
    

    public void setParent(Service parent) {
        /**
         * Mapping the service tree:
         */
        if (this.parent != null && this.parent != parent) 
            throw new BeanInitializationException(
                "Service '" + getName() +  "' has at least two parents ('"
                + parent.getName() + "' and '" + this.parent.getName() + "')");
        if (parent == this) {
            throw new BeanInitializationException(
                "Trying to set parent of service to itself");
        }
        this.parent = parent;
    }
	

    public List getAssertions() {
        return this.assertions;
    }
	

    private static List getAllAssertions(Service service) {
        ArrayList assertions = new ArrayList();
        do {
            assertions.addAll(0, service.getAssertions());
        } while ((service = service.getParent()) != null);
        return assertions;
    }

    
    private List getAllURLPostProcessors() {
        if (this.accumulatedUrlPostProcessors != null) {
            return this.accumulatedUrlPostProcessors;
        }

        List allPostProcessors = new ArrayList();
        Service s = this;
        while (s != null) {
            if ((s instanceof ServiceImpl) && ((ServiceImpl) s).urlPostProcessors != null) {
                allPostProcessors.addAll(((ServiceImpl) s).urlPostProcessors);
            }
            s = s.getParent();
        }
        this.accumulatedUrlPostProcessors = allPostProcessors;
        return allPostProcessors;
    }
    

    public String getName() {
        return this.name;
    }

	
    public void setBeanName(String name) {
        this.name = name;
    }



    public Service getParent() {
        return this.parent;
    }
	

    public boolean isDescendantOf(Service service) {
        if (service == null) {
            throw new IllegalArgumentException("Services cannot be null");
        }

        Service s = this.parent;

        while (s != null) {
            if (s == service) {
                return true;
            }
            s = s.getParent();
        }
        return false;
    }


    public String constructLink(Resource resource, Principal principal) {
        return constructLink(resource, principal, null, true);
    }


    public String constructLink(Resource resource, Principal principal,
                                boolean matchAssertions) {
        return constructLink(resource, principal, null, matchAssertions);
    }

	
    public String constructLink(Resource resource, Principal principal,
                                Map parameters) {
        return constructLink(resource, principal, parameters, true);
    }

    public String constructLink(Resource resource, Principal principal,
                                Map parameters, boolean matchAssertions) {
        List assertions = getAllAssertions(this);
        URL urlObject = 
            constructInternal(resource, principal, parameters, assertions, 
                    matchAssertions);

        postProcess(urlObject);

        return urlObject.toString();
    }
	
    public String constructLink(String uri) {
        List assertions = getAllAssertions(this);
        URL urlObject = new URL("http", NetUtils.guessHostName(), uri);

        for (Iterator i = assertions.iterator(); i.hasNext();) {
            Assertion assertion = (Assertion) i.next();
            assertion.processURL(urlObject);
        }
       
        postProcess(urlObject);
        
        return urlObject.toString();
    }

    
    public void afterPropertiesSet() throws Exception {

        // Look up services that are of category <code>this.getName()</code>
        List childServices = getUnknownServiceChildren();

        for (int i = childServices.size() - 1; i > -1; i--) {
            Service child = (Service) childServices.get(i);
            if (!this.services.contains(child)) {
                
                if (child.getOrder() == Integer.MAX_VALUE) {
                    this.services.add(child);
                } else {
                    int index = Math.max(child.getOrder(), 0);
                    index = Math.min(index, this.services.size());
                    this.services.add(index, child);
                }
            }
        }

        for (Iterator iter = this.services.iterator(); iter.hasNext();) {
            Object o = iter.next();

            if (! (o instanceof Service)) {
                throw new BeanInitializationException(
                    "Only 'ServiceImpl' implementations of Service " +
                    "is supported ( check " + getName() +
                    "'s child services )");
            }

            Service child = (Service) o;
            validateAssertions(child);
            child.setParent(this);
        }

        // Sort all children:
        Collections.sort(this.services, new OrderComparator());

        
        

    }

    
    private void validateAssertions(Service child) {
        List childAssertions = getAllAssertions(child);
		
        for (Iterator iter = getAssertions().iterator(); iter.hasNext();) {
            Assertion assertion = (Assertion) iter.next();
			
            for (Iterator iterator = childAssertions.iterator(); iterator
                     .hasNext();) {
                Assertion childAssertion = (Assertion) iterator.next();
				
                if (childAssertion.conflicts(assertion)) {
                    throw new BeanInitializationException(
                        "Assertion " +  assertion + " for service " +
                        getName() + " is conflicting with assertion " +
                        childAssertion + " in descendant node of child " 
                        + child.getName());
                }
            }
        }
    }

    
    public void setHandlerInterceptors(List handlerInterceptors) {
        this.handlerInterceptors = handlerInterceptors;
    }
    

    public List getHandlerInterceptors() {
        return this.handlerInterceptors;
    }

    
    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(getClass().getName()).append(": ").append(this.name);;
        return sb.toString();
    }


    public AuthenticationChallenge getAuthenticationChallenge() {
        return this.authenticationChallenge;
    }


    public void setAuthenticationChallenge(
        AuthenticationChallenge authenticationChallenge) {
        this.authenticationChallenge = authenticationChallenge;
    }
    

    public int getOrder() {
        return this.order;
    }
    

    public void setOrder(int order) {
        this.order = order;
    }


    public Set getCategories() {
        return this.categories;
    }


    public void setCategories(Set categories) {
        this.categories = categories;
    }
  
    
    private List getUnknownServiceChildren() {
        // find all services, and sort out those of category 'category';
        Map matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            this.applicationContext, Service.class, true, false);
    
        List allServices = new ArrayList(matchingBeans.values());
        List list = new ArrayList(allServices);
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Service service = (Service) iter.next();
            if (service.getParent() != this) 
                allServices.remove(service);

        }
        Collections.sort(allServices, new OrderComparator());
        return allServices;
    }

    private void postProcess(URL urlObject) {
        List urlPostProcessors = getAllURLPostProcessors();

        if (urlPostProcessors != null) {
            for (Iterator i = urlPostProcessors.iterator(); i.hasNext();) {
                URLPostProcessor urlProcessor = (URLPostProcessor) i.next();
                try {
                    urlProcessor.processURL(urlObject);
                } catch (Exception e) {
                    throw new ServiceUnlinkableException("URL Post processor " + urlProcessor
                                                         + " threw exception", e);
                }
            }
        }
    }

    private URL constructInternal(Resource resource, Principal principal,
            Map parameters, List assertions, boolean matchAssertions) {

        String path = resource.getURI();
        if (resource.isCollection()) {
            path += "/";
        }
        URL urlObject = new URL("http", NetUtils.guessHostName(), path);
        if (parameters != null) {
            for (Iterator iter = parameters.entrySet().iterator(); iter
                    .hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();

                String key = entry.getKey().toString();
                String value = entry.getValue().toString();

                urlObject.addParameter(key, value);
            }
        }
        // urlObject.setQuery(parameters);

        for (Iterator i = assertions.iterator(); i.hasNext();) {
            Assertion assertion = (Assertion) i.next();
            boolean match = assertion.processURL(urlObject, resource,
                    principal, matchAssertions);
            if (match == false) {
                throw new ServiceUnlinkableException("Service "
                        + getName() + " cannot be applied to resource "
                        + resource.getURI() + ". Assertion " + assertion
                        + " false for resource.");
            }
        }

        return urlObject;
    }

}
