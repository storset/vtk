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
package org.vortikal.web.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class ReportHandler implements Controller {

    private Repository repository;
    private String viewName;

    // Primary reports, i.e. most visible and "important" reports
    protected List<Reporter> primaryReporters;

    // Set of simple reports, displayed as simple list to choose from
    protected List<Reporter> reporters;

    // Reports used as part of other reports, i.e. not explicit reports on
    // report flap, but only accessible via other reports (primarily via
    // "primaryReportes"
    protected List<Reporter> hiddenReporters;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();

        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Resource resource = this.repository.retrieve(token, uri, false);

        Service service = requestContext.getService();
        URL serviceURL = service.constructURL(resource, securityContext.getPrincipal());

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("serviceURL", serviceURL);

        String reportType = request.getParameter(AbstractReporter.REPORT_TYPE_PARAM);
        if (reportType != null && !"".equals(reportType.trim())) {
            Reporter reporter = getReporter(reportType);
            if (reporter != null) {
                model.put("report", reporter.getReportContent(token, resource, request));
                return new ModelAndView(reporter.getViewName(), model);
            }
        }

        this.addReports(this.primaryReporters, "primaryReporters", model, serviceURL);
        this.addReports(this.reporters, "reporters", model, serviceURL);

        return new ModelAndView(this.viewName, model);
    }

    private void addReports(List<Reporter> reportList, String modelKey, Map<String, Object> model, URL serviceURL) {
        if (reportList != null) {
            List<ReporterObject> reporterObjects = new ArrayList<ReporterObject>();
            for (Reporter reporter : reportList) {
                if (!reporter.isEnabled()) {
                    continue;
                }
                URL reporterURL = new URL(serviceURL);
                reporterURL.addParameter(AbstractReporter.REPORT_TYPE_PARAM, reporter.getName());
                reporterObjects.add(new ReporterObject(reporter.getName(), reporterURL));
            }
            model.put(modelKey, reporterObjects);
        }
    }

    private Reporter getReporter(String reportType) {
        Reporter reporter = this.getReporter(this.primaryReporters, reportType);
        reporter = reporter == null ? this.getReporter(this.reporters, reportType) : reporter;
        reporter = reporter == null ? this.getReporter(this.hiddenReporters, reportType) : reporter;
        return reporter;
    }

    private Reporter getReporter(List<Reporter> reportList, String reportType) {
        if (reportList != null) {
            for (Reporter reporter : reportList) {
                if (reporter.getName().equals(reportType)) {
                    return reporter;
                }
            }
        }
        return null;
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public class ReporterObject {

        private String name;
        private URL url;

        public ReporterObject(String name, URL url) {
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public URL getUrl() {
            return url;
        }

    }

    public void setPrimaryReporters(List<Reporter> primaryReporters) {
        this.primaryReporters = primaryReporters;
    }

    public void setReporters(List<Reporter> reporters) {
        this.reporters = reporters;
    }

    public void setHiddenReporters(List<Reporter> hiddenReporters) {
        this.hiddenReporters = hiddenReporters;
    }

}
