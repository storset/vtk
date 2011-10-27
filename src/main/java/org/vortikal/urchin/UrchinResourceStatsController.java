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
import org.vortikal.web.service.URL;

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

        Map<String, Object> model = new HashMap<String, Object>();

        try {
            String id;
            if ((id = request.getParameter("host")) == null)
                id = "www.uio.no";

            URL[] hosts = new URL[12];
            String[] host = { "www.uio.no", "www.hf.uio.no", "www.khm.uio.no", "www.odont.uio.no", "www.sv.uio.no",
                    "www.tf.uio.no", "www.ub.uio.no", "www.uv.uio.no", "www.jus.uio.no", "www.uniforum.uio.no",
                    "www.mn.uio.no", "www.med.uio.no" };
            String[] hostnames = { "UiO", "HF", "KHM", "Odont", "SV", "TF", "UB", "UV", "JUS", "Uniforum", "MN", "MED" };

            for (int i = 0; i < 12; i++) {
                URL base = URL.create(request).removeParameter("host");
                hosts[i] = base.addParameter("host", host[i]);
            }

            model.put("hosts", hosts);
            model.put("hostnames", hostnames);
            model.put("thisMonth", urs.thisMonth());
            model.put("ursMonths", urs.months(resource, token, id));
            model.put("ursTotal", urs.total(resource, token, id));
            model.put("ursWeekTotal", urs.weekTotal(resource, token, id));
            model.put("ursYesterdayTotal", urs.yesterdayTotal(resource, token, id));
        } catch (Exception e) {
            return null;
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
