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

import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public abstract class DocumentReporter extends AbstractReporter {

    private int pageSize = DEFAULT_SEARCH_LIMIT;
    private Service manageService, reportService;
    private int backURL;

    protected abstract Search getSearch(String token, Resource currentResource, HttpServletRequest request);

    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("reportname", this.getName());

        /* Create back to diagram URL. TODO: more general */
        if (backURL > 0) {
            String backURLname;
            switch (backURL) {
              case 1: backURLname = "diagram";
                break;
              default: backURLname = "BACKURL_SET-BUT_UNKNOWN";
                break;
            }
            
            URL backURLConstructed = new URL(reportService.constructURL(resource)).addParameter(REPORT_TYPE_PARAM, backURLname);
            
            result.put("backURLname", backURLname);
            result.put("backURL", backURLConstructed);
        }

        Search search = this.getSearch(token, resource, request);
        if (search == null) {
            return result;
        }

        Position pos = Position.create(request, this.pageSize);
        if (pos.cursor >= Search.MAX_LIMIT) {
            return result;
        }
        search.setCursor(pos.cursor);
        search.setLimit(pageSize);

        ResultSet rs = this.searcher.execute(token, search);
        if (pos.cursor + Math.min(pageSize, rs.getSize()) >= rs.getTotalHits()) {
            pos.next = null;
        }
        if (pos.cursor + Math.min(pageSize, rs.getSize()) >= Search.MAX_LIMIT) {
            pos.next = null;
        }

        //result.put("result", rs.getAllResults());
        result.put("from", pos.cursor + 1);
        result.put("to", pos.cursor + Math.min(pageSize, rs.getSize()));
        result.put("total", rs.getTotalHits());
        result.put("next", pos.next);
        result.put("prev", pos.prev);

        boolean[] isReadRestricted = new boolean[rs.getSize()];
        URL[] viewURLs = new URL[rs.getSize()];
        List<PropertySet> list = new ArrayList<PropertySet>();
        for (int i = 0; i < rs.getSize(); i++) {            
            PropertySet p = rs.getResult(i);
            try {
                Resource r = this.repository.retrieve(token, p.getURI(), true);
                p = r; // fresh copy of resource
                isReadRestricted[i] = r.isReadRestricted();
                if (this.manageService != null) {
                    viewURLs[i] = this.manageService.constructURL(p.getURI()).setProtocol("http");
                }
                handleResult(r, result);
            } catch (Exception e) {
            }
            list.add(p);
        }
        result.put("result", list);
        result.put("isReadRestricted", isReadRestricted);
        result.put("viewURLs", viewURLs);
        return result;
    }
    
    protected void handleResult(Resource resource, Map<String, Object> model) {
    }

    public void setManageService(Service manageService) {
        this.manageService = manageService;
    }

    public void setReportService(Service reportService) {
        this.reportService = reportService;
    }
    
    public Service getReportService() {
        return this.reportService;
    }

    public void setBackURL(int backURL) {
        this.backURL = backURL;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    private static class Position {
        int cursor = 0;
        int limit = 0;
        URL next = null;
        URL prev = null;

        private Position() {
        }

        static Position create(HttpServletRequest req, int limit) {
            Position position = new Position();
            position.limit = limit;

            int page = 1;
            String pageParam = req.getParameter("page");
            if (pageParam != null) {
                try {
                    page = Integer.parseInt(pageParam.trim());
                } catch (Throwable t) {
                }
            }
            if (page <= 0) {
                page = 1;
            }
            int cursor = (page - 1) * position.limit;
            if (cursor < 0) {
                cursor = 0;
            }
            position.cursor = cursor;
            URL url = URL.create(req);
            position.next = new URL(url).setParameter("page", String.valueOf(page + 1));
            if (page > 1) {
                position.prev = new URL(url).setParameter("page", String.valueOf(page - 1));
            }
            if (page == 2) {
                position.prev.removeParameter("page");
            }
            return position;
        }
    }
}
