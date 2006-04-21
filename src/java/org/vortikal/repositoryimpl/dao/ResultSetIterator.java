package org.vortikal.repositoryimpl.dao;

import java.io.IOException;


/**
 * ResultSetIterator
 * 
 */
public interface ResultSetIterator {

    public Object next() throws IOException; 
    
    public boolean hasNext() throws IOException;

    public void close() throws IOException;
    
}
