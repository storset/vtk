package org.vortikal.repositoryimpl.query.parser;

import org.vortikal.repository.RepositoryException;

public class QueryException extends RepositoryException {

    public QueryException() {
        super();
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryException(String message) {
        super(message);
    }

    public QueryException(Throwable cause) {
        super(cause);
    }

    
}
