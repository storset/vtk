package org.vortikal.web.controller;

import javax.servlet.http.HttpServletRequest;

public class PageInfo {

    int page = 1;
    int offset;
    int limit;

    public PageInfo(HttpServletRequest request, int pageLimit) {
        if (request.getParameter("page") != null) {
            try {
                page = Integer.parseInt(request.getParameter("page"));
                if (page < 1) {
                    page = 1;
                }
            } catch (Throwable t) {
                // Ignore, using default
            }
        }

        offset = (page - 1) * pageLimit;
        limit = pageLimit;
    }

    public int getPage() {
        return page;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

}
