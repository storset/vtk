package org.vortikal.repositoryimpl.queryparser;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMultipleQuery implements Query {

    private List queries = new ArrayList();

    public void add(Query query) {
        queries.add(query);
    }
    
    public List getQueries() {
        return queries;
    }

}
