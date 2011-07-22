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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class DiagramReport extends AbstractReporter {

    private String name;
    private String viewName;

    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("reportname", this.getName());

        /* Create base URL. */
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        URL baseURL = new URL(service.constructURL(resource, securityContext.getPrincipal()));

        /* Get files and folders count. */
        try {
            int files = fileSearch("file", token, resource);
            result.put("files", files);

            int folders = fileSearch("collection", token, resource);
            result.put("folders", folders);

            result.put("firsttotal", files + folders);
        } catch (Exception e) {
        }

        /*
         * Get filetypes count and add URL to new search listing up the
         * filetype.
         */
        try {
            int total = 0;

            /*
             * Web pages needs to be handled alone since the search is
             * different.
             */
            int webpage = webSearch(token, resource);
            result.put("webpage", webpage);
            URL webpageURL = new URL(baseURL);
            webpageURL.addParameter(REPORT_TYPE_PARAM, "webpageReporter");
            result.put("webpageURL", webpageURL);
            total += webpage;

            /* All types except web pages. */
            String[] types = { "image", "audio", "video", "pdf", "doc", "ppt", "xls" };
            for (String type : types) {
                int count = fileSearch(type, token, resource);
                result.put(type, count);
                URL imageURL = new URL(baseURL);
                imageURL.addParameter(REPORT_TYPE_PARAM, type + "Reporter");
                result.put(type + "URL", imageURL);
                total += count;
            }

            result.put("secondtotal", total);
        } catch (Exception e) {
        }

        return result;
    }

    private int webSearch(String token, Resource resource) {
        Search search = new Search();
        AndQuery q = new AndQuery();
        OrQuery query = new OrQuery();

        query.add(new TypeTermQuery("apt-resource", TermOperator.IN));
        query.add(new TypeTermQuery("php", TermOperator.IN));
        query.add(new TypeTermQuery("html", TermOperator.IN));
        query.add(new TypeTermQuery("managed-xml", TermOperator.IN));
        query.add(new TypeTermQuery("json-resource", TermOperator.IN));
        q.add(query);

        /* In current resource but not in /vrtx. */
        q.add(new UriPrefixQuery(resource.getURI().toString()));
        q.add(new UriPrefixQuery("/vrtx", true));

        search.setQuery(q);
        search.setPropertySelect(null);
        
        return this.searcher.execute(token, search).getTotalHits();
    }

    private int fileSearch(String type, String token, Resource resource) {
        Search search = new Search();
        AndQuery query = new AndQuery();

        query.add(new TypeTermQuery(type, TermOperator.IN));

        /* In current resource but not in /vrtx. */
        query.add(new UriPrefixQuery(resource.getURI().toString()));
        query.add(new UriPrefixQuery("/vrtx", true));

        search.setQuery(query);
        search.setPropertySelect(null);

        return this.searcher.execute(token, search).getTotalHits();
    }

    @Override
    public String getName() {
        return name;
    }

    @Required
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getViewName() {
        return viewName;
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
}
