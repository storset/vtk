package org.vortikal.repository.resourcetype;

import org.vortikal.repository.RepositoryException;

/**
 * Exception thrown when an attempt is made to set a property value to an 
 * illegal or unconvertible type.
 * 
 *
 */
public class ValueFormatException extends RepositoryException {
    public ValueFormatException() {
        super();
    }

    public ValueFormatException(String message) {
        super(message);
    }

    public ValueFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValueFormatException(Throwable cause) {
        super(cause);
    }

}
