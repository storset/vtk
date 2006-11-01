package org.vortikal.repositoryimpl.query.consistency;

import org.vortikal.repositoryimpl.query.IndexException;

/**
 * Exception thrown in cases where index storage has been corrupted.
 * 
 * @author oyviste
 *
 */
public class StorageCorruptionException extends IndexException {

    public StorageCorruptionException(String message, Throwable cause) {
        super(message, cause);
    }

}
