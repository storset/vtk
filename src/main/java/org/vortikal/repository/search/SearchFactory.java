package org.vortikal.repository.search;

import org.vortikal.repository.search.query.Query;

public interface SearchFactory {

    public Search createSearch(String queryString) throws QueryException;

    public Search createSearch(Query query) throws QueryException;

}