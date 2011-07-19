/* Copyright (c) 2006, 2007, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.repository.store;

import java.io.IOException;
import java.io.InputStream;

import org.vortikal.repository.Path;
import org.vortikal.repository.RecoverableResource;
import org.vortikal.repository.content.InputStreamWrapper;


/**
 * Defines a pure content store. It is organized hierarchically and 
 * resource nodes are addressed by their URIs. The behaviour shall be equal
 * to that of a common file system. Every node must have an 
 * existing parent collection resource node, except the root node. 
 * The root collection node shall always exist, and should not need to be 
 * created upon initialization.
 * 
 */
public interface ContentStore {

    public void createResource(Path uri, boolean isCollection)
            throws DataAccessException;

    public long getContentLength(Path uri) throws DataAccessException;

    public void deleteResource(Path uri) throws DataAccessException;

    public InputStreamWrapper getInputStream(Path uri) throws DataAccessException;

    /**
     * Store content in the resource given by the URI.
     * The supplied <code>InputStream</code> should be closed by this
     * method, after it has been read.
     * 
     * @param uri
     * @param inputStream
     * @throws IOException
     */
    public void storeContent(Path uri, InputStream inputStream) throws DataAccessException;

    public void copy(Path srcURI, Path destURI) throws DataAccessException;
    
    public void move(Path srcURI, Path destURI) throws DataAccessException;
    
    public void moveToTrash(Path srcURI, final String trashIdDir) throws DataAccessException;
    
    public void recover(Path destURI, RecoverableResource recoverableResource) throws DataAccessException;
    
    public void deleteRecoverable(RecoverableResource recoverableResource) throws DataAccessException;
}
