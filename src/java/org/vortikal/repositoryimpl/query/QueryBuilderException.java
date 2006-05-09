package org.vortikal.repositoryimpl.query;

public class QueryBuilderException extends QueryException {

    public QueryBuilderException() {
        super();
    }

    public QueryBuilderException(String message) {
        super(message);
    }

    public QueryBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryBuilderException(Throwable cause) {
        super(cause);
    }

}
