package org.vortikal.repositoryimpl.query;


/**
 * Closeable {@link java.util.Iterator} interface.
 * @author oyviste
 *
 */
interface CloseableIterator extends java.util.Iterator {
    
    public void close() throws Exception;

}
