package org.vortikal.security;

/**
 *	This exception is thrown when an invalid principal is encountered.
 */
public class InvalidPrincipalException extends RuntimeException {

    private static final long serialVersionUID = 3257004350076368948L;

    public InvalidPrincipalException(String message) {
        super(message);
    }

    public InvalidPrincipalException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public InvalidPrincipalException(Throwable throwable) {
        super(throwable);
    }
}
