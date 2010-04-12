/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.filter;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

/**
 * {@link HandlerAdapter} that runs a chain of configured {@link HandlerFilter 
 * handler filters}. The chain of filters is invoked before control is handed to 
 * the {@link Controller}. The filters are looked up from the current 
 * {@link Service} (including ancestors).
 * 
 * @see HandlerFilter
 * @see HandlerFilterChain
 * @see Service#getHandlerFilters()
 */
public final class ServiceHandlerFilterAdapter implements HandlerAdapter {

    @Override
    public boolean supports(Object handler) {
        return handler instanceof Controller;
    }

    @Override
    public long getLastModified(HttpServletRequest request, Object handler) {
        Controller controller = (Controller) handler;
        if (controller instanceof LastModified) {
            return ((LastModified) controller).getLastModified(request);
        }
        return -1L;
    }

    @Override
    public ModelAndView handle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {

        Controller controller = (Controller) handler;
        RequestContext requestContext = RequestContext.getRequestContext();
        Service service = requestContext.getService();
        List<HandlerFilter> filters = getHandlerFilters(service);

        if (filters != null) {
            FilterChain chain = new FilterChain(filters.toArray(new HandlerFilter[filters.size()]), controller, response);
            chain.filter(request);
            return chain.getResult();
        }
        return controller.handleRequest(request, response);
    }

    private List<HandlerFilter> getHandlerFilters(Service service) {
        List<HandlerFilter> handlerFilters = new ArrayList<HandlerFilter>();
        
        if (service.getParent() != null) {
            List<HandlerFilter> parentFilters = getHandlerFilters(service.getParent());
            if (parentFilters != null) {
                handlerFilters.addAll(parentFilters);
            }
        }
        List<HandlerFilter> myHandlerFilters = service.getHandlerFilters();
        if (myHandlerFilters != null) { 
            handlerFilters.addAll(myHandlerFilters);
        }
        return handlerFilters;
    }
    
    private class FilterChain implements HandlerFilterChain {
        private HandlerFilter[] filters;
        private Controller controller;
        private HttpServletResponse response;
        private int filterIndex = 0;
        private ModelAndView result = null;

        public FilterChain(HandlerFilter[] filters, Controller controller, HttpServletResponse response) {
            this.filters = filters;
            this.controller = controller;
            this.response = response;
        }
        
        public HttpServletResponse getResponse() {
            return this.response;
        }
        
        public ModelAndView getResult() {
            if (this.filterIndex < this.filters.length) {
                return null;
            }
            return this.result;
        }
        
        @Override
        public void filter(HttpServletRequest request) throws Exception {
            if (this.filterIndex < this.filters.length) {
                HandlerFilter next = this.filters[this.filterIndex++];
                next.filter(request, this);
            } else {
                this.result = this.controller.handleRequest(request, this.response);
            }
        }
    }
}
