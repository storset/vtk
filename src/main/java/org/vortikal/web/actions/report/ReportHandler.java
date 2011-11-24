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
package org.vortikal.web.actions.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
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

public class ReportHandler implements Controller, BeanFactoryAware, InitializingBean {

    private Repository repository;
    private String viewName;
    private List<Reporter> primaryReporters, reporters, hiddenReporters;

    private BeanFactory beanFactory;

    private static final String REPORT_TYPE_PARAM = "report-type";

    @Required
    public void setPrimaryReporters(List<Reporter> primaryReporters) {
        this.primaryReporters = primaryReporters;
    }

    @Required
    public void setReporters(List<Reporter> reporters) {
        this.reporters = reporters;
    }

    public void setHiddenReporters(List<Reporter> hiddenReporters) {
        this.hiddenReporters = hiddenReporters;
    }

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

        String reportType = request.getParameter(REPORT_TYPE_PARAM);
        if (reportType != null && !"".equals(reportType.trim())) {
            Reporter reporter = getReporter(reportType);
            if (reporter != null) {
                model.put("report", reporter.getReportContent(token, resource, request));
                return new ModelAndView(reporter.getViewName(), model);
            }
        }

        List<ReporterObject> reporterObjects = new ArrayList<ReporterObject>();
        for (Reporter reporter : this.primaryReporters) {
            URL reporterURL = new URL(serviceURL);
            reporterURL.addParameter(REPORT_TYPE_PARAM, reporter.getName());
            reporterObjects.add(new ReporterObject(reporter.getName(), reporterURL));
        }
        model.put("primaryReporters", reporterObjects);

        reporterObjects = new ArrayList<ReporterObject>();
        for (Reporter reporter : this.reporters) {
            URL reporterURL = new URL(serviceURL);
            reporterURL.addParameter(REPORT_TYPE_PARAM, reporter.getName());
            reporterObjects.add(new ReporterObject(reporter.getName(), reporterURL));
        }
        model.put("reporters", reporterObjects);

        return new ModelAndView(this.viewName, model);
    }

    private Reporter getReporter(String reportType) {
        for (Reporter reporter : this.primaryReporters) {
            if (reporter.getName().equals(reportType)) {
                return reporter;
            }
        }
        for (Reporter reporter : this.reporters) {
            if (reporter.getName().equals(reportType)) {
                return reporter;
            }
        }
        for (Reporter reporter : this.hiddenReporters) {
            if (reporter.getName().equals(reportType)) {
                return reporter;
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

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            primaryReporters.add((Reporter) beanFactory.getBean("urchinVisitReport"));
        } catch (Exception e) {
        }
        try {
            primaryReporters.add((Reporter) beanFactory.getBean("urchinSearchReport"));
        } catch (Exception e) {
        }
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

}
