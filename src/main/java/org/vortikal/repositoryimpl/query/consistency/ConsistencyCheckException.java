package org.vortikal.repositoryimpl.query.consistency;

import org.vortikal.repositoryimpl.query.IndexException;

public class ConsistencyCheckException extends IndexException {

    public ConsistencyCheckException() {
        super();
    }

    public ConsistencyCheckException(String message, Throwable cause) {
        super(message, cause);

    }

    public ConsistencyCheckException(String message) {
        super(message);

    }

    public ConsistencyCheckException(Throwable cause) {
        super(cause);

    }

}
