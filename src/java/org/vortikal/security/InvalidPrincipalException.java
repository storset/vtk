package org.vortikal.security;

/**
 *	This exception is thrown when an invalid principal is encountered.
 */
public class InvalidPrincipalException extends RuntimeException {

    /**
     * @param arg0
     */
    public InvalidPrincipalException(String arg0) {
        super(arg0);
    }
    /**
     * @param arg0
     * @param arg1
     */
    public InvalidPrincipalException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
    /**
     * @param arg0
     */
    public InvalidPrincipalException(Throwable arg0) {
        super(arg0);
    }
}
