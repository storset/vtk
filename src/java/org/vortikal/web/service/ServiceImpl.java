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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.web.AuthenticationChallenge;


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
 *   service (see {@link Ordered}). Default is
 *   <code>Integer.MAX_VALUE</code> (unordered).
 *   <li><code>categories</code> - a {@link Set} of strings denoting
 *   the set of categories this service is a member of.
 * </ul>
 *
 */
public class ServiceImpl
  implements Service, BeanNameAware, InitializingBean, Ordered, ApplicationContextAware {

    private static Log logger = LogFactory.getLog(ServiceImpl.class);
    
    private AuthenticationChallenge authenticationChallenge;
    private Object handler;
    private List assertions = new ArrayList();
    private List services = new ArrayList();
    private Service parent;
    private String name;
    private List handlerInterceptors;
    private int order = Integer.MAX_VALUE; // Same as non ordered;
    private Set categories = null;
    private ApplicationContext applicationContext;
    


    // Duplicate:
    
    private LinkConstructionHelper linkConstructor = new LinkConstructionHelperImpl();
	
    
    public void setHandler(Object handler) {
        this.handler = handler;
    }
	
    public void setAssertions(List assertions) {
        this.assertions = assertions;
    }
	

    public void setServices(List services) {
        this.services = services;
    }


    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    

    public List getChildren() {
        return services;
    }

    public Object getHandler() {
        return handler;
    }
	



    /**
     * Mapping the tree
     **/
    protected void setParent(Service parent) throws BeanInitializationException {
        if (this.parent != null) 
            throw new BeanInitializationException(
                "Service " + getName() +  "has at least two parents ("
                + parent.getName() + " and" + this.parent.getName() + ")");
        this.parent = parent;
    }
	
    public List getAssertions() {
        return assertions;
    }
	
    private List getAllAssertions() {
        if (parent == null) 
            return getAssertions();
 
        List assertions = new ArrayList(((ServiceImpl) parent).getAllAssertions());
        assertions.addAll(getAssertions());
        return assertions;
    }
	
    public String getName() {
        return name;
    }
	
    public void setBeanName(String name) {
        this.name = name;
    }


    public Service getParent() {
        return parent;
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
        List assertions = getAllAssertions();
        return linkConstructor.construct(resource, principal, parameters, assertions,
                                         this, true);
    }

    public String constructLink(Resource resource, Principal principal,
                                Map parameters, boolean matchAssertions) {
        List assertions = getAllAssertions();
        return linkConstructor.construct(resource, principal, parameters, assertions, this,
                                         matchAssertions);
    }
	
    
    public void afterPropertiesSet() throws Exception {

        // Look up services that are of category <code>this.getName()</code>
        List childServices = ServiceCategoryResolver.getServicesOfCategory(
            this.applicationContext, this.getName());
        Collections.sort(childServices, new OrderComparator());

        for (int i = childServices.size() - 1; i > -1; i--) {
            ServiceImpl child = (ServiceImpl)childServices.get(i);
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
            if (! (o instanceof ServiceImpl)) {
                throw new BeanInitializationException(
                    "Only 'ServiceImpl' implementations of Service " +
                    "is supported ( check " + getName() +
                    "'s child services )");
            }
            ServiceImpl child = (ServiceImpl) o;
//             if (!services.contains(child)) {
//                 services.add(child);
//             }
            if (logger.isDebugEnabled()) {
                logger.debug("Initializing child service: " + child.getName());
            }

            validateAssertions(child);
            child.setParent(this);
        }
    }

    
    private void validateAssertions(ServiceImpl child) {
        List childAssertions = child.getAllAssertions();
		
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
        return handlerInterceptors;
    }

    
    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(getClass().getName()).append(": ").append(this.name);;
        return sb.toString();
    }

    public AuthenticationChallenge getAuthenticationChallenge() {
        return authenticationChallenge;
    }

    public void setAuthenticationChallenge(
        AuthenticationChallenge authenticationChallenge) {
        this.authenticationChallenge = authenticationChallenge;
    }
    
    /**
     * @return Returns the order.
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * @param order The order to set.
     */
    public void setOrder(int order) {
        this.order = order;
    }

    public Set getCategories() {
        return this.categories;
    }

    public void setCategories(Set categories) {
        this.categories = categories;
    }
}
