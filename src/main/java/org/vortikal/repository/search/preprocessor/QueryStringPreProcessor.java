package org.vortikal.repository.search.preprocessor;

import org.vortikal.repository.search.QueryException;

public interface QueryStringPreProcessor {

    public String process(String queryString) throws QueryException;

}