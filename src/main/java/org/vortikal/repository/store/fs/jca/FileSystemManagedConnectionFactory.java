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

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;



public class FileSystemManagedConnectionFactory implements ManagedConnectionFactory {

    private transient PrintWriter logWriter;

    private String dataDirectory;
    private String workDirectory;
    

    public String getDataDirectory() {
        return this.dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String getWorkDirectory() {
        return this.workDirectory;
    }

    public void setWorkDirectory(String workDirectory) {
        this.workDirectory = workDirectory;
    }

    /*
     * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory(javax.resource.spi.ConnectionManager)
     *
     * Returns a EIS-specific connection factory (analogous to a javax.sql.DataSource)
     */
    public Object createConnectionFactory(ConnectionManager connectionManager)
            throws ResourceException {
        return new FileSystemConnectionFactoryImpl(this, connectionManager);
    }

    /* 
     * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory()
     */
    public Object createConnectionFactory() throws ResourceException {
        return new FileSystemConnectionFactoryImpl(this, new FileSystemConnectionManager());
    }

    /*
     * @see javax.resource.spi.ManagedConnectionFactory#createManagedConnection(javax.security.auth.Subject, javax.resource.spi.ConnectionRequestInfo)
     */
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo requestInfo)
            throws ResourceException {
        return new FileSystemManagedConnection(requestInfo, this.logWriter,
                                               new java.io.File(this.dataDirectory),
                                               new java.io.File(this.workDirectory));
    }

    /*
     * @see javax.resource.spi.ManagedConnectionFactory#matchManagedConnections(java.util.Set, javax.security.auth.Subject, javax.resource.spi.ConnectionRequestInfo)
     */
    public ManagedConnection matchManagedConnections(
            Set set, Subject subj, ConnectionRequestInfo conReqInfo)
            throws ResourceException {

        for (Iterator i = set.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof FileSystemManagedConnection) {
                return (ManagedConnection) o;
            }

        }


//         Iterator itr = set.iterator();
//         if (itr.hasNext()) {
//             Object obj = itr.next();
//             if (obj instanceof FileSystemManagedConnection) {
                
//                 return (FileSystemManagedConnection) obj;
//             }
//         }
        return null;
    }

    /*
     * @see javax.resource.spi.ManagedConnectionFactory#setLogWriter(java.io.PrintWriter)
     */
    public void setLogWriter(PrintWriter logWriter) throws ResourceException {
        this.logWriter = logWriter;
    }

    /*
     * @see javax.resource.spi.ManagedConnectionFactory#getLogWriter()
     */
    public PrintWriter getLogWriter() throws ResourceException {
        return this.logWriter;
    }
    
    /*
     * @see javax.resource.spi.ManagedConnectionFactory#hashCode()
     */
    public int hashCode() {
        if (this.dataDirectory == null || this.workDirectory == null) {
            return super.hashCode();
        } else {
            // XXX:
            return this.dataDirectory.hashCode() + this.workDirectory.hashCode();
        }
    }

    /*
     * @see javax.resource.spi.ManagedConnectionFactory#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileSystemManagedConnectionFactory)) {
            return false;
        }
        if (this.dataDirectory == null) {
            return false;
        }
        if (this.workDirectory == null) {
            return false;
        }
        FileSystemManagedConnectionFactory other = (FileSystemManagedConnectionFactory) o;

        return this.dataDirectory.equals(other.getDataDirectory())
            && this.workDirectory.equals(other.getWorkDirectory());
    }
    
    
    
}
