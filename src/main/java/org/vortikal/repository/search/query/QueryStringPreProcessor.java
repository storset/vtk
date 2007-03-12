package org.vortikal.repository.search.query;

import org.vortikal.repository.search.QueryException;

public interface QueryStringPreProcessor {

    public String processQueryString(String queryString) throws QueryException;

}