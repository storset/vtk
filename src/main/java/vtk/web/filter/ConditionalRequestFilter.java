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
package vtk.web.filter;

import javax.servlet.http.HttpServletRequest;

import vtk.web.service.Assertion;


/**
 * Conditional request filter. Performs an {@link Assertion} match on
 * the request before conditionally invoking a target request filter.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>assertion</code> - the {@link Assertion} to match
 *   <li><code>requestFilter</code> - the target {@link RequestFilter
 *   request filter} which is invoked upon an assertion match
 * </ul>
 */
public class ConditionalRequestFilter extends AbstractRequestFilter {

    private RequestFilter requestFilter;
    private Assertion assertion;
    
    public void setRequestFilter(RequestFilter requestFilter) {
        this.requestFilter = requestFilter;
    }

    
    public void setAssertion(Assertion assertion) {
        this.assertion = assertion;
    }
    

    public HttpServletRequest filterRequest(HttpServletRequest request) {
        if (this.assertion.matches(request, null, null)) {
            return this.requestFilter.filterRequest(request);
        }
        return request;
    }
    
    public String toString() {
        return this.getClass().getName() + "(" + this.assertion 
            + ", " + this.requestFilter + ")";
    }
}
