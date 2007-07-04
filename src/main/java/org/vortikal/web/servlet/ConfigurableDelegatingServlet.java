/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.web.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.core.OrderComparator;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.FrameworkServlet;

import org.vortikal.web.service.Assertion;

/**
 * A delegating servlet that uses a sequence of {@link ServletMapping}
 * objects to decide which servlet to forward the request to.
 *
 * <p>Servlet dispatching is done using a named dispatcher.
 *
 */
public class ConfigurableDelegatingServlet extends FrameworkServlet {

    private Log logger = LogFactory.getLog(this.getClass());
    private ServletContext servletContext = null;

    private List<ServletMapping> servletMappings;



    protected void initFrameworkServlet()
        throws ServletException, BeansException {
        super.initFrameworkServlet();

        WebApplicationContext context = getWebApplicationContext();
        
        Map<?, ServletMapping> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
                context, ServletMapping.class, true, false);
        
        this.servletMappings = new ArrayList<ServletMapping>(matchingBeans.values());
        Collections.sort(this.servletMappings, new OrderComparator());
        
    }

    

    protected void doService(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        for (ServletMapping mapping: this.servletMappings) {
            for (Assertion assertion: mapping.getAssertions()) {
                if (!assertion.matches(request, null, null)) {
                    continue;
                }
                doDispatch(mapping.getServletName(), request, response);
            }
        }
    }
    

    private void doDispatch(String servletName, HttpServletRequest request,
                            HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher rd = this.servletContext.getNamedDispatcher(servletName);
        if (rd == null) {
            throw new ServletException(
                "No request dispatcher available for servlet '" + servletName + "'");
        }
        
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Forwarding request '" + request.getRequestURL()
                              + "' to servlet '" + servletName + "'");
        }

        rd.forward(request, response);
    }
    

    
}

