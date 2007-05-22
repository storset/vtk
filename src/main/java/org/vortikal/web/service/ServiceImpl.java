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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.HandlerInterceptor;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.web.AuthenticationChallenge;
import org.vortikal.util.net.NetUtils;


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
public class ServiceImpl implements Service, BeanNameAware, InitializingBean {

    List<Assertion> allAssertions = new ArrayList<Assertion>();
    private AuthenticationChallenge authenticationChallenge;
    private Object handler;
    private List<Assertion> assertions = new ArrayList<Assertion>();
    private Service parent;
    private String name;
    private Map<String, Object> attributes = new HashMap<String, Object>();
    private List<HandlerInterceptor> handlerInterceptors;
    private int order = 0;
    private Set categories = null;
    private List urlPostProcessors = new ArrayList();
    private List accumulatedUrlPostProcessors = null;
    	
    
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
    

    public void setUrlPostProcessors(List urlPostProcessors) {
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
                                Map parameters) {
        return constructLink(resource, principal, parameters, true);
    }

    public URL constructURL(Resource resource, Principal principal,
                               Map parameters) {
        return constructURL(resource, principal, parameters, true);
    }

    public String constructLink(Resource resource, Principal principal,
                                Map parameters, boolean matchAssertions) {
        return constructURL(resource, principal, parameters, matchAssertions).toString();
    }
	
    public URL constructURL(Resource resource, Principal principal,
                                Map parameters, boolean matchAssertions) {
        URL urlObject = 
            constructInternal(resource, principal, parameters, this.allAssertions, 
                    matchAssertions);

        postProcess(urlObject);

        return urlObject;
    }

    public String constructLink(String uri) {
        return constructURL(uri).toString();
    }

    public URL constructURL(String uri) {
        URL urlObject = new URL("http", NetUtils.guessHostName(), uri);

        for (Iterator i = this.allAssertions.iterator(); i.hasNext();) {
            Assertion assertion = (Assertion) i.next();
            assertion.processURL(urlObject);
        }
       
        postProcess(urlObject);
        
        return urlObject;
    }

    public String constructLink(String uri, Map parameters) {
        return constructURL(uri, parameters).toString();
    }

    public URL constructURL(String uri, Map parameters) {
        URL urlObject = new URL("http", NetUtils.guessHostName(), uri);
        if (parameters != null) {
            for (Iterator iter = parameters.entrySet().iterator(); iter
                    .hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();

                String key = entry.getKey().toString();
                String value = entry.getValue().toString();

                urlObject.addParameter(key, value);
            }
        }

        for (Iterator i = this.allAssertions.iterator(); i.hasNext();) {
            Assertion assertion = (Assertion) i.next();
            assertion.processURL(urlObject);
        }
       
        postProcess(urlObject);
        
        return urlObject;
    }
    
    public void setHandlerInterceptors(List<HandlerInterceptor> handlerInterceptors) {
        this.handlerInterceptors = handlerInterceptors;
    }
    

    public List<HandlerInterceptor> getHandlerInterceptors() {
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


    public void afterPropertiesSet() throws Exception {
        Service service = this;
        do {
            allAssertions.addAll(0, service.getAssertions());
        } while ((service = service.getParent()) != null);
    }
        

}
