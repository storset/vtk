package org.vortikal.index.management;

/**
 * Indexes might be unavailble for a short amount of time if for instance
 * a re-indexing operation has just begun. This is because the underlying
 * directory implementation is closed/re-opened. In these cases, this 
 * exception should be thrown.
 * 
 * @author oyviste
 *
 */
public class IndexUnavailableException extends ManagementException {

    public IndexUnavailableException(String msg) {
        super(msg);
    }
    
    public IndexUnavailableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
