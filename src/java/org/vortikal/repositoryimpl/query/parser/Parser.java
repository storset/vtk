package org.vortikal.repositoryimpl.query.parser;

import org.vortikal.repositoryimpl.query.query.Query;

public interface Parser {

    public Query parse(String query);

}
