/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.urchin;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public class UrchinResourceStatsController implements Controller {

    private UrchinResourceStats urs;
    private Repository repository;
    private String viewName;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();

        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Resource resource = this.repository.retrieve(token, uri, false);

        boolean recache = request.getParameter("recache") != null ? true : false;

        Map<String, Object> model = new HashMap<String, Object>();

        int visitsTotal = urs.visitsTotal(resource, token, recache);
        if (visitsTotal > 0) {
            model.put("ursTotalVisits", visitsTotal);
            model.put("thisMonth", urs.thisMonth());
            model.put("ursMonths", urs.months(resource, token, recache));
            model.put("ursSixtyTotal", urs.sixtyTotal(resource, token, recache));
            model.put("ursThirtyTotal", urs.thirtyTotal(resource, token, recache));
            model.put("ursTotalPages", urs.pagesTotal(resource, token, recache));
            model.put("ursNMonths", urs.nMonths());
        } else {
            // Whether or not it is a resource created within the last week
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(calendar.getTimeInMillis() - (86400000 * 7));

            boolean newResource = false;
            if (calendar.getTime().compareTo(resource.getCreationTime()) < 0) {
                newResource = true;
            }
            model.put("newResource", newResource);
        }

        return new ModelAndView(this.viewName, model);
    }

    @Required
    public void setUrchinResourceStats(UrchinResourceStats urs) {
        this.urs = urs;
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

}
