/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.report;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.view.freemarker.MessageLocalizer;

public class BrokenLinksToTsvController implements Controller {

    private Reporter brokenLinksReporter;
    private String webHostName;

    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource resource = repository.retrieve(token, requestContext.getResourceURI(), false);

        Map<String, Object> result = this.brokenLinksReporter.getReportContent(token, resource, request);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        String filename = this.webHostName;
        filename += resource.getTitle().equals("/") ? "" : "_" + resource.getTitle();
        filename += "_" + df.format(new Date());
        filename += "_BrokenLinks";

        org.springframework.web.servlet.support.RequestContext springRequestContext = new org.springframework.web.servlet.support.RequestContext(
                request);
        String locTitle = new MessageLocalizer("property.title", "Title", null, springRequestContext).get(null)
                .toString();
        String locError = new MessageLocalizer("linkcheck.error", "Error", null, springRequestContext).get(null)
                .toString();

        StringBuilder tsv = new StringBuilder();
        tsv.append(locTitle + ":\tUri:\t" + locError + ":\n");

        List<PropertySet> list = (List<PropertySet>) result.get("result");
        Map<String, Object> map = (Map<String, Object>) result.get("linkCheck");
        for (PropertySet ps : list) {
            Property titleProp = ps.getProperty(Namespace.DEFAULT_NAMESPACE, "title");
            String title = titleProp.getFormattedValue();
            String uri = ps.getURI().toString();
            JSONObject obj = (JSONObject) map.get(uri);
            if (obj != null)
                tsv.append(title + "\t" + uri + "\t" + obj.get("index") + "\n");
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/tab-separated-values;charset=utf-8");
        response.setHeader("Content-Disposition", "filename=" + filename + ".tsv");

        ServletOutputStream out = response.getOutputStream();
        try {
            out.print(tsv.toString());
        } finally {
            out.close();
        }

        return null;

    }

    @Required
    public void setBrokenLinksReporter(Reporter brokenLinksReporter) {
        this.brokenLinksReporter = brokenLinksReporter;
    }

    @Required
    public void setWebHostName(String webHostName) {
        String[] names = webHostName.trim().split("\\s*,\\s*");
        this.webHostName = names[0];
    }

}
