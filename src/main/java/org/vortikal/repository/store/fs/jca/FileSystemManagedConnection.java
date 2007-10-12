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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

public class FileSystemManagedConnection implements ManagedConnection {

    private PrintWriter logWriter;
    private List<ConnectionEventListener> listeners;
    private Set<FileSystemConnectionImpl> connections;
    private File dataDirectory;
    private File workDirectory;
    private Context context;
    
    public FileSystemManagedConnection(ConnectionRequestInfo requestInfo,
                                       PrintWriter logWriter, File dataDirectory, File workDirectory) {
        this.logWriter = logWriter;
        this.listeners = new ArrayList<ConnectionEventListener>();
        this.connections = new HashSet<FileSystemConnectionImpl>();
        this.dataDirectory = dataDirectory;
        this.workDirectory = workDirectory;
        this.context = new Context();
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo requestInfo)
        throws ResourceException {
        if (this.dataDirectory == null || !this.dataDirectory.exists()) {
            throw new ResourceException("The data directory does not exist");
        }
        if (this.workDirectory == null || !this.workDirectory.exists()) {
            throw new ResourceException("The work directory does not exist");
        }
        FileSystemConnectionImpl conn = new FileSystemConnectionImpl(this);
        synchronized(this.connections) {
            this.connections.add(conn);
        }
        return conn;
    }

    public void destroy() throws ResourceException {
        synchronized (this.connections) {
            this.connections.clear();
        }
        synchronized (this.listeners) {
            this.listeners = null;
        }
        this.dataDirectory = null;
        this.workDirectory = null;
    }

    public void cleanup() throws ResourceException {
    }

    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof FileSystemConnectionImpl)) {
            throw new ResourceException("Connection not of type '" + FileSystemConnectionImpl.class + "'");
        }
        FileSystemConnectionImpl fileSystemConn = (FileSystemConnectionImpl) connection;
        fileSystemConn.setManagedConnection(this);
        synchronized(this.connections) {
            this.connections.add(fileSystemConn);
        }
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
        synchronized (this.listeners) {
            this.listeners.add(listener);
        }
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        synchronized (this.listeners) {
            this.listeners.remove(listener);
        }
    }

    public XAResource getXAResource() throws ResourceException {
        return this.context;
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Local transactions are not supported");
    }

    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new FileSystemManagedConnectionMetaData();
    }

    public void setLogWriter(PrintWriter logWriter) throws ResourceException {
        this.logWriter = logWriter;
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return this.logWriter;
    }

    public void close(FileSystemConnectionImpl connection) {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(connection);
        synchronized (this.listeners) {
            for (ConnectionEventListener listener: this.listeners) {
                listener.connectionClosed(event);
            }
        }
        synchronized (this.connections) {
            connections.remove(connection);
        }
    }

    public void createResource(String uri, boolean isCollection) 
        throws ResourceException {
        try {
            FileMapper mapper = new CascadingFileMapper(
                this.dataDirectory, this.workDirectory, this.context.getCurrentFileMapper());
            CreateOperation op = new CreateOperation(uri, isCollection, mapper);
            this.context.pushOperation(op, mapper);
        } catch (IOException e) {
            throw new ResourceException("Unable to create [" + uri + "]", e);
        }
    }
    
    public long getContentLength(String uri) throws ResourceException {
        
        try {
            FileMapper mapper = this.context.getCurrentFileMapper();
            File file = null;
            if (mapper != null) {
                file = mapper.getFile(uri);
            } else {
                file = new File(this.dataDirectory.getAbsolutePath() + uri);
            }
            if (!file.exists()) {
                throw new ResourceException("File [" + uri + "] does not exist");
            }
            if (file.isDirectory()) {
                throw new ResourceException("File [" + uri + "] is a directory");
            }
            return file.length();
        } catch (IOException e) {
            throw new ResourceException("Failed to get content length for file [" + uri + "]", e);
        }
    }

    public InputStream getInputStream(String uri) throws ResourceException {
        try {
            FileMapper mapper = this.context.getCurrentFileMapper();
            File file = null;
            if (mapper != null) {
                file = mapper.getFile(uri);
            } else {
                file = new File(this.dataDirectory.getAbsolutePath() + uri);
            }
            if (!file.exists()) {
                throw new ResourceException("File [" + uri + "] does not exist");
            }
            if (file.isDirectory()) {
                throw new ResourceException("File [" + uri + "] is a directory");
            }
            return new FileInputStream(file);
        } catch (IOException e) {
            throw new ResourceException("Failed to get input stream for file [" + uri + "]", e);
        }
    }

    public void storeContent(String uri, InputStream inputStream)
        throws ResourceException {
        try {
            FileMapper mapper = new CascadingFileMapper(
                this.dataDirectory, this.workDirectory, this.context.getCurrentFileMapper());
            StoreContentOperation op = new StoreContentOperation(uri, inputStream, mapper);
            this.context.pushOperation(op, mapper);
        } catch (IOException e) {
            throw new ResourceException("Unable to create [" + uri + "]", e);
        }
    }
    
    public void deleteResource(String uri) throws ResourceException {

        try {
            FileMapper mapper = new CascadingFileMapper(
                this.dataDirectory, this.workDirectory, this.context.getCurrentFileMapper());
            DeleteOperation op = new DeleteOperation(uri, mapper);
            this.context.pushOperation(op, mapper);
        } catch (IOException e) {
            throw new ResourceException("Unable to delete [" + uri + "]", e);
        }
    }


    public void copy(String srcURI, String destURI) throws ResourceException {        
        try {
            FileMapper mapper = new CascadingFileMapper(
                this.dataDirectory, this.workDirectory, this.context.getCurrentFileMapper());
            CopyOperation op = new CopyOperation(srcURI, destURI, mapper);
            this.context.pushOperation(op, mapper);
        } catch (IOException e) {
            throw new ResourceException("Unable to copy [" + srcURI + ", " + destURI + "]", e);
        }
    }
}
