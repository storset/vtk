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
package org.vortikal.web;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.service.Assertion;
import org.vortikal.web.service.Service;

/**
 * Request context initializer.
 *
 */
public class RequestContextInitializer
  implements ContextInitializer, ApplicationContextAware, InitializingBean {

    private static Log logger = LogFactory.getLog(RequestContextInitializer.class);
    private ApplicationContext context = null;
    private List rootServices = null;

    private String trustedToken;
    private Repository repository;
    private List uriPrefixes = new ArrayList();
    


    public void createContext(HttpServletRequest request) throws Exception {
        String uri = getResourceURI(request);
        Resource resource = null;

        try {
            resource = repository.retrieve(trustedToken, uri, false);
            
        } catch (ResourceNotFoundException e) {
            // Ignore, this is not an error
        } catch (ResourceLockedException e) {
            // Ignore, this is not an error
        } catch (RepositoryException e) {
            // TODO: What to do when unable to retrieve resource at
            // this point?
            String msg = "Unable to retrieve resource for service " +
                "matching: " + uri + ". A valid token is required.";
            logger.warn(msg, e);
            throw new ServletException(msg, e);
        }

        for (Iterator iter = rootServices.iterator(); iter.hasNext();) {
            Service service = (Service) iter.next();
            // Set an initial request context (with the resource, but
            // without the matched service)
            RequestContext.setRequestContext(
                new RequestContext(service, uri));
            
            // Resolve the request to a service:
            if (resolveService(service, request, resource)) {
                break;
            }
             
            RequestContext.setRequestContext(null);
        }

        //FIXME: What if no service is resolved?
    }



    public void destroyContext() {
        RequestContext.setRequestContext(null);
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append(": ").append(System.identityHashCode(this));
        return sb.toString();
    }
    

    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }
    
    public void afterPropertiesSet() {
        if (trustedToken == null) {
            throw new BeanInitializationException(
                "Required property 'trustedToken' not set");
        }

        if (repository == null) {
            throw new BeanInitializationException(
                "Required property 'repository' not set");
        }

        Map matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
                context, Service.class, true, false);
        
        List rootServices = new ArrayList(matchingBeans.values());
        List list = new ArrayList(rootServices);
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Service service = (Service) iter.next();
            if (service.getParent() != null) 
                rootServices.remove(service);
        }
        Collections.sort(rootServices, new OrderComparator());

        if (rootServices.isEmpty()) {
            throw new BeanInitializationException(
                    "No services defined in context.");
        }
        
        this.rootServices = rootServices;
        logger.info("Registered service tree root services in the following order: " 
                + rootServices);
        
        if (logger.isInfoEnabled()) {
            logger.info(printServiceTree());
        }
    }
    

    private StringBuffer printServiceTree() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("\nService tree:\n");
        printServiceList(rootServices, buffer, "->");
        return buffer;
    }

    private void printServiceList(List services, StringBuffer buffer, String indent) {
        for (Iterator iter = services.iterator(); iter.hasNext();) {
            Service service = (Service) iter.next();
            buffer.append(indent + service.getName() + "\n");
            printServiceList(service.getChildren(), buffer, "  " + indent);
        }
    }
    
    /**
     * Resolves a request recursively to a service and creates the
     * request context.
     * 
     * @param service the currently matched service. Should be set to
     * the root service initially.
     * @param request the <code>HttpServletRequest</code>
     * @param resource the resource (may be null)
     * @return If the service doesn't match the context,
     * <code>false</code> is returned. Otherwise the service' children
     * are queried for matches (recursivly) and the first match
     * returns <code>true</code>. If no children matches, this service
     * returns with <code>true</code>.  If there are assertions and
     * one (or more) assertions doesn't match, return null. Else
     * return this Service or the first matching child's result.
     * 
     */
    private boolean resolveService(Service service, HttpServletRequest request,
                                   Resource resource) {
		
        if (logger.isTraceEnabled()) {
            logger.trace("Matching for service " + service.getName() +
                         ", having assertions: " + service.getAssertions());
        }
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        try {
            for (Iterator iter = service.getAssertions().iterator(); iter.hasNext();) {
                Assertion assertion = (Assertion) iter.next();

                boolean match = true;

                if (!assertion.matches(request,resource,securityContext.getPrincipal()))
                    match = false;

                if (logger.isTraceEnabled()) {
                    if (match) {
                        logger.trace("Matched assertion: " + assertion +
                                     " for service " + service.getName());
                    } else {
                        logger.trace("Unmatched assertion: " + assertion +
                                     " for service " + service.getName());
                    }
                }
                if (!match) return false;
            }
        } catch (AuthenticationException e) {
            RequestContext.setRequestContext(
                new RequestContext(service, requestContext.getResourceURI()));
            throw(e);
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Currently matched service: " + service.getName() +
                         ", will check for child services: " + service.getChildren());
        }

        for (Iterator iter = service.getChildren().iterator(); iter.hasNext();) {
            Service child = (Service) iter.next();
            if (resolveService(child, request, resource))
                return true;
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Service matching produced result: " + service.getName());
        }

        RequestContext.setRequestContext(
            new RequestContext(service, requestContext.getResourceURI()));
        return true;
    }





    private String getResourceURI(HttpServletRequest req) {
        String uri = req.getRequestURI();

        for (Iterator i = uriPrefixes.iterator(); i.hasNext();) {
            String prefix = (String) i.next();
            if (uri.startsWith(prefix)) {
                uri = uri.substring(prefix.length());
                break;
            }
        }

	if (uri == null || uri.equals("")){
            uri="/";
	}

        if (uri.endsWith("/") && !uri.equals("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }

        
        try {

            uri = URLUtil.urlDecode(uri, "utf-8");

        } catch (UnsupportedEncodingException e) {
//            logger.warn("Unsupported encoding: utf-8", e);
        }
        
        return uri;
    }

    /**
     * @param repository The repository to set.
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
 
    
    /**
     * @param trustedToken The trustedToken to set.
     */
    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

    
    /**
     * @param uriPrefixes The uriPrefixes to set.
     */
    public void setUriPrefixes(List uriPrefixes) {
        this.uriPrefixes = uriPrefixes;
    }
}
