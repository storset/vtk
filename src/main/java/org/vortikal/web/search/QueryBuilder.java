package org.vortikal.web.search;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.repository.search.query.Query;

public interface QueryBuilder {

    public Query build(Resource base, HttpServletRequest request);
    
}
