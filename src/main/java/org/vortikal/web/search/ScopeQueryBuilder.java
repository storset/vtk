package org.vortikal.web.search;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.UriPrefixQuery;

public class ScopeQueryBuilder implements QueryBuilder {

    public Query build(Resource base, HttpServletRequest request) {
        if (base.getURI().isRoot()) {
            // Not needed
            return null;
        }
        
        return new UriPrefixQuery(base.getURI() + "/");
    }


}
