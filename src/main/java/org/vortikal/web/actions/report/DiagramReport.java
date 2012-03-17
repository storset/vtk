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
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class DiagramReport extends AbstractReporter {

    private String name;
    private String viewName;
    private int total, totalWebpages, files;

    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("reportname", this.getName());

        /* Create base URL. */
        Principal p = SecurityContext.getSecurityContext().getPrincipal();
        Service service = RequestContext.getRequestContext().getService();
        URL baseURL = new URL(service.constructURL(resource, p));

        /* Get count and URL for file and folder. */
        try {
            files = doSearch("file", TermOperator.IN, token, resource);
            result.put("files", files);
            result.put("filesURL", new URL(baseURL).addParameter(REPORT_TYPE_PARAM, "fileReporter"));

            int folders = doSearch("collection", TermOperator.IN, token, resource);
            result.put("folders", folders);
            result.put("foldersURL", new URL(baseURL).addParameter(REPORT_TYPE_PARAM, "folderReporter"));

            result.put("firsttotal", files + folders);
        } catch (Exception e) {
            return result;
        }

        /*
         * Get filetypes count and add URL to new search listing up the
         * filetype.
         */
        try {
            total = 0;

            /*
             * This list can be appended to add file types. Do it after webpage
             * and before other unless it will be handled uniquely.
             */
            String[] types = { "webpage", "image", "audio", "video", "pdf", "doc", "ppt", "xls", "text", "other" };
            TermOperator[] t = { null, TermOperator.IN, TermOperator.IN, TermOperator.IN, TermOperator.IN,
                    TermOperator.IN, TermOperator.IN, TermOperator.IN, TermOperator.EQ, null };
            int typeCount[] = new int[types.length];
            URL typeURL[] = new URL[types.length];

            /*
             * Web pages needs to be handled alone since the search is
             * different.
             */
            typeCount[0] = totalWebpages = webSearch(token, resource);
            typeURL[0] = new URL(baseURL).addParameter(REPORT_TYPE_PARAM, types[0] + "Reporter");
            total += typeCount[0];

            /*
             * Starting on i = 1 since we have already done webpage and ending
             * on types.length - 1 since we will handle other unique.
             */
            for (int i = 1; i < types.length - 1; i++) {
                typeCount[i] = doSearch(types[i], t[i], token, resource);
                typeURL[i] = new URL(baseURL).addParameter(REPORT_TYPE_PARAM, types[i] + "Reporter");
                total += typeCount[i];
            }

            /* Other is handled unique as we do not need to search for it. */
            typeCount[types.length - 1] = files - total;
            typeURL[types.length - 1] = new URL(baseURL).addParameter(REPORT_TYPE_PARAM, types[types.length - 1]
                    + "Reporter");
            total += typeCount[types.length - 1];

            result.put("types", types);
            result.put("typeCount", typeCount);
            result.put("typeURL", typeURL);

            result.put("secondtotal", total);
        } catch (Exception e) {
        }

        /*
         * Get web page types count and add URL to new search listing up the
         * specific type.
         */
        try {
            total = 0;

            /*
             * This list can be appended to add file types. Do it before other.
             */
            String[] types = { "structured-article", "structured-event", "person", "structured-project",
                    "research-group", "organizational-unit", "contact-supervisor", "frontpage", "managed-xml", "html",
                    "php", "webOther" };
            int typeCount[] = new int[types.length];
            URL typeURL[] = new URL[types.length];

            for (int i = 0; i < types.length - 1; i++) {
                typeCount[i] = doSearch(types[i], TermOperator.IN, token, resource);
                typeURL[i] = new URL(baseURL).addParameter(REPORT_TYPE_PARAM, types[i] + "Reporter");
                total += typeCount[i];
            }

            /* webOther is handled unique as we do not need to search for it. */
            typeCount[types.length - 1] = totalWebpages - total;
            typeURL[types.length - 1] = new URL(baseURL).addParameter(REPORT_TYPE_PARAM, types[types.length - 1]
                    + "Reporter");
            total += typeCount[types.length - 1];

            result.put("webTypes", types);
            result.put("webTypeCount", typeCount);
            result.put("webTypeURL", typeURL);

            result.put("thirdtotal", total);
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
        q.add(new UriPrefixQuery(resource.getURI().toString(), false));
        q.add(new UriPrefixQuery("/vrtx", true));

        search.setQuery(q);
        search.setLimit(1);

        return this.searcher.execute(token, search).getTotalHits();
    }

    private int doSearch(String type, TermOperator t, String token, Resource resource) {
        Search search = new Search();
        AndQuery query = new AndQuery();

        query.add(new TypeTermQuery(type, t));

        /* In current resource but not in /vrtx. */
        query.add(new UriPrefixQuery(resource.getURI().toString(), false));
        query.add(new UriPrefixQuery("/vrtx", true));

        search.setQuery(query);
        search.setLimit(1);

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
