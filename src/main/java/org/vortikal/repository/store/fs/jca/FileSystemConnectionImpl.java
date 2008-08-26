/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.store.fs.jca;


import java.io.InputStream;

import javax.resource.ResourceException;

import org.vortikal.repository.Path;


public class FileSystemConnectionImpl implements FileSystemConnection {
    
    private FileSystemManagedConnection managedConnection;

    
    public FileSystemConnectionImpl(FileSystemManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
    }
    
    public void setManagedConnection(FileSystemManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
    }

    public void createResource(Path uri, boolean isCollection) throws ResourceException {
        if (this.managedConnection == null) {
            throw new ResourceException("Connection is closed");
        }

        this.managedConnection.createResource(uri, isCollection);
    }
    

    public long getContentLength(Path uri) throws ResourceException {
        if (this.managedConnection == null) {
            throw new ResourceException("Connection is closed");
        }
        return this.managedConnection.getContentLength(uri);
    }
    

    public void deleteResource(Path uri) throws ResourceException {
        if (this.managedConnection == null) {
            throw new ResourceException("Connection is closed");
        }
        this.managedConnection.deleteResource(uri);
    }
    

    public InputStream getInputStream(Path uri) throws ResourceException {
        if (this.managedConnection == null) {
            throw new ResourceException("Connection is closed");
        }
        return this.managedConnection.getInputStream(uri);
    }
    

    public void storeContent(Path uri, InputStream inputStream) throws ResourceException {
        if (this.managedConnection == null) {
            throw new ResourceException("Connection is closed");
        }
        this.managedConnection.storeContent(uri, inputStream);
    }
    

    public void copy(Path srcURI, Path destURI) throws ResourceException {
        if (this.managedConnection == null) {
            throw new ResourceException("Connection is closed");
        }
        this.managedConnection.copy(srcURI, destURI);
    }
    
    public void move(Path srcURI, Path destURI) throws ResourceException {
        if (this.managedConnection == null) {
            throw new ResourceException("Connection is closed");
        }
        this.managedConnection.move(srcURI, destURI);
    }

    public void close() throws ResourceException {
        this.managedConnection.close(this);
        this.managedConnection = null;
    }
}
