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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.web.servlet.HandlerInterceptor;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.web.AuthenticationChallenge;
import org.vortikal.util.net.NetUtils;
import org.vortikal.web.RequestContext;
import org.vortikal.web.filter.HandlerFilter;
import org.vortikal.web.service.provider.ServiceNameProvider;


/**
 * Default implementation of the Service interface.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>parent</code> - the parent {@link Service} in the service tree
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
public class ServiceImpl implements Service, BeanNameAware {

    private static final String DEFAULT_HOST = NetUtils.guessHostName();
    
    // FIXME: Cache for all assertions, don't use directly!
    private volatile List<Assertion> allAssertions;

    private AuthenticationChallenge authenticationChallenge;
    private Object handler;
    private List<Assertion> assertions = new ArrayList<Assertion>();
    private Service parent;
    private String name;
    private Map<String, Object> attributes = new HashMap<String, Object>();
    private List<HandlerInterceptor> handlerInterceptors;
    private List<HandlerFilter> handlerFilters;
    private int order = 0;
    private Set<String> categories = null;
    private List<URLPostProcessor> urlPostProcessors = new ArrayList<URLPostProcessor>();
    private List<URLPostProcessor> accumulatedUrlPostProcessors = null;
    private ServiceNameProvider serviceNameProvider;
    
    public List<Assertion> getAllAssertions() {
        if (this.allAssertions == null) {
            synchronized (this) {
                if (this.allAssertions != null) {
                    return this.allAssertions;
                }
                this.allAssertions = new ArrayList<Assertion>();
                if (this.parent != null) {
                    this.allAssertions.addAll(parent.getAllAssertions());
                }
                this.allAssertions.addAll(this.assertions);
            }
        }
        
        return this.allAssertions;
    }

    public void setHandler(Object handler) {
        this.handler = handler;
    }
	

    public void setAssertions(List<Assertion> assertions) {
        this.assertions = assertions;
    }
	

    public Object getHandler() {
        return this.handler;
    }
	

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    

    public Object getAttribute(String name) {
        if (this.attributes == null) {
            return null;
        }
        return this.attributes.get(name);
    }
    

    public void setUrlPostProcessors(List<URLPostProcessor> urlPostProcessors) {
        this.urlPostProcessors = urlPostProcessors;
    }
    

    public void setParent(Service parent) {
        // Looking for infinite loops
        Service service = parent;
        while (service != null) {
            if (service == this) {
                throw new BeanInitializationException(
                "Trying to set parent service " + parent.getName() + " on service " 
                + getName() + " resulting in a infinite loop");
            }
            service = service.getParent();
        }
        
        this.parent = parent;
    }
	

    public List<Assertion> getAssertions() {
        return this.assertions;
    }
	

    private List<URLPostProcessor> getAllURLPostProcessors() {
        if (this.accumulatedUrlPostProcessors != null) {
            return this.accumulatedUrlPostProcessors;
        }

        List<URLPostProcessor> allPostProcessors = new ArrayList<URLPostProcessor>();
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

    public URL constructURL(Resource resource) {
        return constructURL(resource, null, null, false);
    }
    
    public URL constructURL(Resource resource, Principal principal) {
        return constructURL(resource, principal, null, true);
    }


    public String constructLink(Resource resource, Principal principal,
                                boolean matchAssertions) {
        return constructLink(resource, principal, null, matchAssertions);
    }


    public URL constructURL(Resource resource, Principal principal,
                                boolean matchAssertions) {
        return constructURL(resource, principal, null, matchAssertions);
    }

	
    public String constructLink(Resource resource, Principal principal,
                                Map<String, String> parameters) {
        return constructLink(resource, principal, parameters, true);
    }

    public URL constructURL(Resource resource, Principal principal,
                               Map<String, String> parameters) {
        return constructURL(resource, principal, parameters, true);
    }

    public String constructLink(Resource resource, Principal principal,
                                Map<String, String> parameters, boolean matchAssertions) {
        return constructURL(resource, principal, parameters, matchAssertions).toString();
    }
	
    public URL constructURL(Resource resource, Principal principal,
                                Map<String, String> parameters, boolean matchAssertions) {
        URL urlObject = 
            constructInternal(resource, principal, parameters, getAllAssertions(), 
                    matchAssertions);

        postProcess(urlObject, resource);

        return urlObject;
    }

    public String constructLink(Path uri) {
        return constructURL(uri).toString();
    }

    public URL constructURL(Path uri) {
        String protocol = "http";
        String host = DEFAULT_HOST;
        if (RequestContext.exists()) {
            RequestContext requestContext = RequestContext.getRequestContext();
            HttpServletRequest request = requestContext.getServletRequest();
            protocol = request.isSecure() ? "https" : "http"; 
            host = request.getServerName();
        }
        URL urlObject = new URL(protocol, host, uri);

        for (Assertion assertion: getAllAssertions()) {
            assertion.processURL(urlObject);
        }
       
        postProcess(urlObject, null);
        
        return urlObject;
    }

    public String constructLink(Path uri, Map<String, String> parameters) {
        return constructURL(uri, parameters).toString();
    }

    public URL constructURL(Path uri, Map<String, String> parameters) {
        String protocol = "http";
        String host = DEFAULT_HOST;
        if (RequestContext.exists()) {
            RequestContext requestContext = RequestContext.getRequestContext();
            HttpServletRequest request = requestContext.getServletRequest();
            protocol = request.isSecure() ? "https" : "http"; 
            host = request.getServerName();
        }
        URL urlObject = new URL(protocol, host, uri);

        if (parameters != null) {
            for (Map.Entry<String, String> entry: parameters.entrySet()) {
                urlObject.addParameter(entry.getKey(), entry.getValue());
            }
        }

        for (Assertion assertion: getAllAssertions()) {
            assertion.processURL(urlObject);
        }
       
        postProcess(urlObject, null);
        
        return urlObject;
    }
    
    public void setHandlerInterceptors(List<HandlerInterceptor> handlerInterceptors) {
        this.handlerInterceptors = handlerInterceptors;
    }
    

    public List<HandlerInterceptor> getHandlerInterceptors() {
        if (this.handlerInterceptors == null) {
            return null;
        }
        return Collections.unmodifiableList(this.handlerInterceptors);
    }

    public void setHandlerFilters(List<HandlerFilter> handlerFilters) {
        this.handlerFilters = handlerFilters;
    }
    
    @Override
    public List<HandlerFilter> getHandlerFilters() {
        if (this.handlerFilters == null) {
            return null;
        }
        return Collections.unmodifiableList(this.handlerFilters);
    }

    

    public String toString() {
        StringBuilder sb = new StringBuilder();
		
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


    public Set<String> getCategories() {
        return this.categories;
    }


    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }
  
    
    private void postProcess(URL urlObject, Resource resource) {
        List<URLPostProcessor> urlPostProcessors = getAllURLPostProcessors();

        if (urlPostProcessors != null) {
            for (URLPostProcessor urlProcessor: urlPostProcessors) {
                try {
                    if (resource != null) {
                        urlProcessor.processURL(urlObject, resource, this);
                    } else {
                        urlProcessor.processURL(urlObject, this);
                    }
                } catch (Exception e) {
                    throw new ServiceUnlinkableException("URL Post processor " + urlProcessor
                                                         + " threw exception", e);
                }
            }
        }
    }

    private URL constructInternal(Resource resource, Principal principal,
            Map<String, String> parameters, List<Assertion> assertions, boolean matchAssertions) {

        Path path = resource.getURI();

        String protocol = "http";
        String host = DEFAULT_HOST;
        if (RequestContext.exists()) {
            RequestContext requestContext = RequestContext.getRequestContext();
            HttpServletRequest request = requestContext.getServletRequest();
            protocol = request.isSecure() ? "https" : "http"; 
            host = request.getServerName();
        }
        URL urlObject = new URL(protocol, host, path);
        
        if (resource.isCollection()) {
            urlObject.setCollection(true);
        }
        
        if (parameters != null) {
            for (Map.Entry<String, String> entry: parameters.entrySet()) {
                urlObject.addParameter(entry.getKey(), entry.getValue());
            }
        }
        // urlObject.setQuery(parameters);

        for (Assertion assertion: assertions) {
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
    
    
    public void setServiceNameProvider(ServiceNameProvider serviceNameProvider) {
    	this.serviceNameProvider = serviceNameProvider;
    }
    
    
    public String getLocalizedName(Resource resource, HttpServletRequest request) {
    	if (this.serviceNameProvider != null) {
    		return this.serviceNameProvider.getLocalizedName(resource, request);
    	}
    	return null;
    }

}